package com.xiaojinzi.tally.module.qianji.authorization

import java.io.Closeable
import java.io.File
import java.nio.file.Files
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Assume.assumeNotNull
import org.junit.Test

class AccessibilityGrantCommandTest {
    @Test
    fun componentUsesOnlyTheFixedServiceAndRejectsUnsafePackageNames() {
        listOf("com.example.app", PACKAGE, "org.Example_v2.client2").forEach { packageName ->
            assertEquals("$packageName/$SERVICE", AccessibilityGrantCommand.component(packageName))
        }
        listOf(
            "", "android", ".com.app", "com.app.", "com..app", "com.1app", "_com.app",
            "com.ex-ample", "com.ex ample", "com.app\n", "com.app'", "com.app\"",
            "com.app;id", "com.app${'$'}(id)", "com.app`id`", "com.app/Service", "中文.app",
            "a".repeat(256) + ".app",
        ).forEach { packageName ->
            expectIllegalArgument { AccessibilityGrantCommand.component(packageName) }
            expectIllegalArgument { AccessibilityGrantCommand.script(packageName, 0) }
        }
    }

    @Test
    fun onlyNonNegativeBoundedUserIdsAreAccepted() {
        listOf(0, 10, 100_000).forEach { userId ->
            assertTrue(AccessibilityGrantCommand.script(PACKAGE, userId).contains("settings --user $userId "))
        }
        listOf(-1, Int.MIN_VALUE, 100_001, Int.MAX_VALUE).forEach { userId ->
            expectIllegalArgument { AccessibilityGrantCommand.script(PACKAGE, userId) }
        }
    }

    @Test
    fun adbGrantCommandUsesTheExactPackageAndUser() {
        assertEquals(
            "adb shell pm grant --user 10 $PACKAGE android.permission.WRITE_SECURE_SETTINGS",
            AccessibilityGrantCommand.adbGrantCommand(PACKAGE, 10),
        )
        listOf("", "com.example/app", "com.example;id").forEach { packageName ->
            expectIllegalArgument { AccessibilityGrantCommand.adbGrantCommand(packageName, 10) }
        }
        listOf(-1, 100_001).forEach { userId ->
            expectIllegalArgument { AccessibilityGrantCommand.adbGrantCommand(PACKAGE, userId) }
        }
    }

    @Test
    fun mergeServicesPreservesOtherEntriesAndIsIdempotent() {
        val other = "org.reader/.ReaderService"
        assertEquals(
            "$other:$FULL",
            AccessibilityGrantCommand.mergeServices(other, PACKAGE),
        )
        assertEquals(
            "$other:$FULL",
            AccessibilityGrantCommand.mergeServices("$other:$FULL", PACKAGE),
        )
        assertEquals(
            "$other:$PACKAGE/.QianjiAutoRecordService",
            AccessibilityGrantCommand.mergeServices("$other:$PACKAGE/.QianjiAutoRecordService", PACKAGE),
        )
        listOf("not_a_component", "org.reader//Service", "org.reader/.Service::org.other/.Other").forEach {
            expectIllegalArgument { AccessibilityGrantCommand.mergeServices(it, PACKAGE) }
        }
    }

    @Test
    fun nullAndEmptyListsBecomeOnlyOurService() {
        listOf("", "null").forEach { initial ->
            SettingsSandbox(initial).use { sandbox ->
                assertSuccess(sandbox.run())
                assertEquals(FULL, sandbox.services())
                assertEquals("1", sandbox.enabled())
                assertEquals(EXPECTED_CALLS, sandbox.calls())
            }
        }
    }

    @Test
    fun preservesOtherServicesAndUsesTheRequestedUser() {
        val otherServices = "org.reader/.ReaderService:org.assist/org.assist.Owner${'$'}Nested"
        SettingsSandbox(otherServices, userId = 10).use { sandbox ->
            assertSuccess(sandbox.run())
            assertEquals("$otherServices:$FULL", sandbox.services())
            assertEquals(EXPECTED_CALLS, sandbox.calls())
        }
    }

    @Test
    fun mergesConcurrentAdditionsFromTheSecondRead() {
        val first = "org.first/.FirstService"
        val second = "org.second/.SecondService:$first"
        SettingsSandbox(first, secondSnapshot = second).use { sandbox ->
            assertSuccess(sandbox.run())
            assertEquals("$second:$FULL", sandbox.services())
        }
    }

    @Test
    fun doesNotReenableAServiceRemovedBeforeTheLatestRead() {
        SettingsSandbox("org.first/.FirstService", secondSnapshot = "org.second/.SecondService").use { sandbox ->
            assertSuccess(sandbox.run())
            assertEquals("org.second/.SecondService:$FULL", sandbox.services())
        }
    }

    @Test
    fun fullAndValidShortComponentsAreIdempotent() {
        listOf(FULL, "$PACKAGE/.QianjiAutoRecordService").forEach { ownComponent ->
            val initial = "org.other/.OtherService:$ownComponent"
            SettingsSandbox(initial).use { sandbox ->
                assertSuccess(sandbox.run())
                assertSuccess(sandbox.run())
                assertEquals(initial, sandbox.services())
            }
        }
    }

    @Test
    fun runtimePackageShortNameMustNotBeMistakenForTheFixedLibraryService() {
        val runtimePackage = "com.example.ledger.debug"
        val unrelatedShort = "$runtimePackage/.QianjiAutoRecordService"
        SettingsSandbox(unrelatedShort, packageName = runtimePackage).use { sandbox ->
            assertSuccess(sandbox.run())
            assertEquals("$unrelatedShort:$runtimePackage/$SERVICE", sandbox.services())
        }
    }

    @Test
    fun serviceNamesAreMatchedAsWholeComponents() {
        val similar = "${FULL}Extra:org.other/$SERVICE"
        SettingsSandbox(similar).use { sandbox ->
            assertSuccess(sandbox.run())
            assertEquals("$similar:$FULL", sandbox.services())
        }
    }

    @Test
    fun eitherFailedInitialReadPreventsAllWrites() {
        listOf("fail_first" to 10, "fail_second" to 11).forEach { (mode, exitCode) ->
            SettingsSandbox("org.other/.OtherService", mode = mode).use { sandbox ->
                assertFailure(sandbox.run(), exitCode)
                assertFalse(sandbox.calls().any { it.startsWith("put ") })
                assertEquals("org.other/.OtherService", sandbox.services())
                assertEquals("0", sandbox.enabled())
            }
        }
    }

    @Test
    fun exitZeroWithAnExceptionOrInvalidListPreventsAllWrites() {
        listOf("exception_first" to 10, "exception_second" to 11).forEach { (mode, exitCode) ->
            SettingsSandbox("org.other/.OtherService", mode = mode).use { sandbox ->
                assertFailure(sandbox.run(), exitCode)
                assertFalse(sandbox.calls().any { it.startsWith("put ") })
            }
        }
        listOf(
            "java.lang.SecurityException: Permission denial",
            "org.other/.Service\norg.third/.Service",
            "org.other/.Service;id", "org.other/.Service::org.third/.Service",
            "not_a_component", "org.other//Service", "org.other/.",
        ).forEach { invalid ->
            SettingsSandbox(invalid).use { sandbox ->
                assertFailure(sandbox.run(), 10)
                assertFalse(sandbox.calls().any { it.startsWith("put ") })
            }
        }
    }

    @Test
    fun writeFailuresStopAtTheFailedOperation() {
        SettingsSandbox("", mode = "fail_put_list").use { sandbox ->
            assertFailure(sandbox.run(), 20)
            assertFalse(sandbox.calls().contains("put accessibility_enabled"))
        }
        SettingsSandbox("", mode = "fail_put_flag").use { sandbox ->
            assertFailure(sandbox.run(), 21)
            assertEquals(FULL, sandbox.services())
            assertEquals("0", sandbox.enabled())
        }
    }

    @Test
    fun failedOrConflictingReadbackNeverReportsSuccessOrRetriesTheWrite() {
        listOf(
            "fail_readback" to 30,
            "exception_readback" to 30,
            "conflict_readback" to 31,
            "fail_read_flag" to 33,
            "wrong_flag" to 34,
        ).forEach { (mode, exitCode) ->
            SettingsSandbox("org.other/.OtherService", mode = mode).use { sandbox ->
                assertFailure(sandbox.run(), exitCode)
                assertEquals(1, sandbox.calls().count { it == "put enabled_accessibility_services" })
            }
        }
    }

    private fun assertSuccess(result: LocalCommandRunner.Result) {
        assertFalse(result.timedOut)
        assertEquals(0, result.exitCode)
        assertEquals("QIANJI_ACCESSIBILITY_ENABLED\n", result.output)
    }

    private fun assertFailure(result: LocalCommandRunner.Result, exitCode: Int) {
        assertFalse(result.timedOut)
        assertEquals(exitCode, result.exitCode)
        assertEquals("", result.output)
    }

    private fun expectIllegalArgument(block: () -> Unit) {
        try {
            block()
            fail("Expected IllegalArgumentException")
        } catch (_: IllegalArgumentException) {
            // Expected: no unsafe value reaches a shell script.
        }
    }

    /** Executes the generated script against a stateful fake settings command, not a Kotlin mirror. */
    private class SettingsSandbox(
        initialServices: String,
        private val packageName: String = PACKAGE,
        private val userId: Int = 0,
        private val mode: String = "normal",
        secondSnapshot: String? = null,
    ) : Closeable {
        private val directory = Files.createTempDirectory("qianji-settings-test-").toFile()
        private val listFile = File(directory, "services").apply { writeText(initialServices) }
        private val enabledFile = File(directory, "enabled").apply { writeText("0") }
        private val callsFile = File(directory, "calls").apply { writeText("") }
        private val countFile = File(directory, "count").apply { writeText("0") }
        private val secondFile = secondSnapshot?.let { File(directory, "second").apply { writeText(it) } }

        fun run(): LocalCommandRunner.Result {
            val shell = findShell()
            assumeNotNull(shell)
            countFile.writeText("0")
            val fakeSettings = """
                SERVICES=${quote(listFile)}
                ENABLED_FILE=${quote(enabledFile)}
                CALLS=${quote(callsFile)}
                COUNT=${quote(countFile)}
                SECOND_FILE=${secondFile?.let { quote(it) } ?: "''"}
                MODE='$mode'
                settings() {
                    if [ "${'$'}1" != '--user' ] || [ "${'$'}2" != '$userId' ] || [ "${'$'}4" != 'secure' ]; then
                        return 98
                    fi
                    printf '%s %s\n' "${'$'}3" "${'$'}5" >> "${'$'}CALLS"
                    case "${'$'}3 ${'$'}5" in
                        'get enabled_accessibility_services')
                            COUNT_VALUE=${'$'}(cat "${'$'}COUNT")
                            COUNT_VALUE=${'$'}((COUNT_VALUE + 1))
                            printf '%s' "${'$'}COUNT_VALUE" > "${'$'}COUNT"
                            case "${'$'}MODE:${'$'}COUNT_VALUE" in
                                fail_first:1|fail_second:2|fail_readback:3)
                                    printf '%s\n' null
                                    return 7
                                    ;;
                                exception_first:1|exception_second:2|exception_readback:3)
                                    printf '%s\n' 'java.lang.SecurityException: Permission denial' >&2
                                    return 0
                                    ;;
                                conflict_readback:3)
                                    printf '%s' '$FULL'
                                    return 0
                                    ;;
                            esac
                            if [ "${'$'}COUNT_VALUE" = 2 ] && [ -n "${'$'}SECOND_FILE" ]; then
                                cat "${'$'}SECOND_FILE"
                            else
                                cat "${'$'}SERVICES"
                            fi
                            ;;
                        'get accessibility_enabled')
                            if [ "${'$'}MODE" = fail_read_flag ]; then return 7; fi
                            if [ "${'$'}MODE" = wrong_flag ]; then
                                printf '%s' 0
                            else
                                cat "${'$'}ENABLED_FILE"
                            fi
                            ;;
                        'put enabled_accessibility_services')
                            if [ "${'$'}MODE" = fail_put_list ]; then return 7; fi
                            printf '%s' "${'$'}6" > "${'$'}SERVICES"
                            printf '%s\n' 'must not be exposed' >&2
                            ;;
                        'put accessibility_enabled')
                            if [ "${'$'}MODE" = fail_put_flag ]; then return 7; fi
                            printf '%s' "${'$'}6" > "${'$'}ENABLED_FILE"
                            printf '%s\n' 'must not be exposed'
                            ;;
                        *) return 98 ;;
                    esac
                }
            """.trimIndent() + "\n"
            return LocalCommandRunner.run(
                command = listOf(shell!!),
                stdin = fakeSettings + AccessibilityGrantCommand.script(packageName, userId),
                timeoutMillis = 5_000L,
            )
        }

        fun services(): String = listFile.readText()
        fun enabled(): String = enabledFile.readText()
        fun calls(): List<String> = callsFile.readLines()

        override fun close() {
            directory.deleteRecursively()
        }

        private fun quote(file: File): String =
            "'" + file.absolutePath.replace('\\', '/').replace("'", "'\"'\"'") + "'"
    }

    companion object {
        private const val PACKAGE = "com.xiaojinzi.tally.module.qianji"
        private const val SERVICE = "com.xiaojinzi.tally.module.qianji.QianjiAutoRecordService"
        private const val FULL = "$PACKAGE/$SERVICE"
        private val EXPECTED_CALLS = listOf(
            "get enabled_accessibility_services",
            "get enabled_accessibility_services",
            "put enabled_accessibility_services",
            "put accessibility_enabled",
            "get enabled_accessibility_services",
            "get accessibility_enabled",
        )

        private fun findShell(): String? {
            val windows = System.getProperty("os.name").startsWith("Windows", ignoreCase = true)
            val executable = if (windows) "sh.exe" else "sh"
            val candidates = (System.getenv("PATH") ?: "").split(File.pathSeparator)
                .map { File(it, executable) } + listOf(
                File("/bin/sh"),
                File("C:/Program Files/Git/bin/sh.exe"),
                File("C:/Program Files/Git/usr/bin/sh.exe"),
                File("D:/Program Files/Git/bin/sh.exe"),
            )
            return candidates.firstOrNull { it.isFile }?.absolutePath
        }
    }
}
