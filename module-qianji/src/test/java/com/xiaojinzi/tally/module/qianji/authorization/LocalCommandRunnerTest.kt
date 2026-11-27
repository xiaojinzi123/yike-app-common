package com.xiaojinzi.tally.module.qianji.authorization

import java.io.File
import java.io.RandomAccessFile
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import kotlin.system.exitProcess

class LocalCommandRunnerTest {
    @Test
    fun sendsStdinAndCollectsBothStreamsWithTheExitCode() {
        val result = LocalCommandRunner.run(command("copy"), stdin = "line 1\n第二行\n")
        assertEquals(7, result.exitCode)
        assertFalse(result.timedOut)
        assertEquals("OUT:line 1\n第二行\nERR:complete\n", result.output)
    }

    @Test
    fun nullStdinClosesThePipeSoAReaderCanFinish() {
        val result = LocalCommandRunner.run(command("copy"), timeoutMillis = 5_000L)
        assertFalse(result.timedOut)
        assertEquals(7, result.exitCode)
        assertEquals("OUT:ERR:complete\n", result.output)
    }

    @Test
    fun argumentsArePassedLiterallyWithoutAShell() {
        val argument = "spaces ' \" ; ${'$'}(must_not_run) `also_not_run`"
        // Windows passes a command line to the Java launcher, which consumes quote escapes.
        val launcherArgument = if (System.getProperty("os.name", "").startsWith("Windows", true)) {
            argument.replace("\"", "\\\"")
        } else argument
        val result = LocalCommandRunner.run(command("argument") + launcherArgument)
        assertEquals(0, result.exitCode)
        assertEquals(argument, result.output)
        assertFalse(result.timedOut)
    }

    @Test
    fun consumesOutputBeyondTheCaptureLimitWithoutDeadlocking() {
        val result = LocalCommandRunner.run(command("large-output"), timeoutMillis = 5_000L)
        assertFalse(result.timedOut)
        assertEquals(0, result.exitCode)
        assertEquals(64 * 1024, result.output.length)
        assertTrue(result.output.all { it == 'x' })
    }

    @Test
    fun capturesTheFinalSuccessTokenOfAFastProcess() {
        repeat(5) {
            val result = LocalCommandRunner.run(command("token"))
            assertEquals(0, result.exitCode)
            assertFalse(result.timedOut)
            assertEquals("QIANJI_ACCESSIBILITY_ENABLED\n", result.output)
        }
    }

    @Test
    fun timeoutKillsTheProcessAndReleasesReaderAndWriterThreads() {
        val originalThreads = runnerThreads()
        val sentinel = File.createTempFile("qianji-wait-sentinel-", ".tmp").apply { deleteOnExit() }
        try {
            val started = System.nanoTime()
            val result = LocalCommandRunner.run(
                command("wait") + sentinel.absolutePath,
                stdin = "x".repeat(2 * 1024 * 1024),
                timeoutMillis = 1_500L,
            )
            val elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started)
            assertTrue(result.timedOut)
            assertNull(result.exitCode)
            assertEquals("READY\n", result.output)
            assertTrue("Runner must return after a bounded cleanup", elapsedMillis < 6_000L)
            RandomAccessFile(sentinel, "rw").channel.use { channel ->
                val lock = channel.tryLock()
                assertTrue("Timed-out child must release its file lock", lock != null)
                lock?.release()
            }
            assertEquals(originalThreads, runnerThreads())
        } finally {
            sentinel.delete()
        }
    }

    @Test
    fun missingExecutableDoesNotExposeTheCommandOrEnvironment() {
        val commandName = "qianji-nonexistent-command-sensitive-value-6d9542a1"
        val result = LocalCommandRunner.run(listOf(commandName))
        assertNull(result.exitCode)
        assertFalse(result.timedOut)
        assertEquals("", result.output)
    }

    @Test
    fun rejectsInvalidExecutionBoundsBeforeStarting() {
        expectIllegalArgument { LocalCommandRunner.run(emptyList()) }
        expectIllegalArgument { LocalCommandRunner.run(command("token"), timeoutMillis = 0) }
        expectIllegalArgument { LocalCommandRunner.run(command("token"), timeoutMillis = -1) }
    }

    private fun command(mode: String): List<String> {
        val executableName = if (System.getProperty("os.name").startsWith("Windows", true)) {
            "java.exe"
        } else {
            "java"
        }
        val java = File(File(System.getProperty("java.home"), "bin"), executableName).absolutePath
        val classPath = listOf(
            RunnerTestProcess::class.java,
            Unit::class.java,
        ).map { File(it.protectionDomain.codeSource.location.toURI()).absolutePath }
            .distinct()
            .joinToString(File.pathSeparator)
        return listOf(java, "-Dfile.encoding=UTF-8", "-cp", classPath, RunnerTestProcess::class.java.name, mode)
    }

    private fun runnerThreads(): Set<Long> = Thread.getAllStackTraces().keys
        .filter { it.isAlive && it.name.startsWith("qianji-command-") }
        .map { it.id }
        .toSet()

    private fun expectIllegalArgument(block: () -> Unit) {
        try {
            block()
            fail("Expected IllegalArgumentException")
        } catch (_: IllegalArgumentException) {
            // Expected.
        }
    }
}

/** A real child JVM exercises pipe pressure, EOF and process termination without Android. */
object RunnerTestProcess {
    @JvmStatic
    fun main(args: Array<String>) {
        when (args[0]) {
            "copy" -> {
                val text = System.`in`.bufferedReader(Charsets.UTF_8).readText()
                System.out.write("OUT:$text".toByteArray(Charsets.UTF_8))
                System.out.flush()
                System.err.write("ERR:complete\n".toByteArray(Charsets.UTF_8))
                System.err.flush()
                exitProcess(7)
            }
            "argument" -> System.out.print(args[1])
            "large-output" -> {
                val block = ByteArray(8 * 1024) { 'x'.code.toByte() }
                repeat(32) { System.out.write(block) }
                repeat(32) { System.err.write(block) }
            }
            "token" -> System.out.print("QIANJI_ACCESSIBILITY_ENABLED\n")
            "wait" -> {
                RandomAccessFile(File(args[1]), "rw").channel.use { channel ->
                    channel.lock()
                    System.out.print("READY\n")
                    System.out.flush()
                    while (true) Thread.sleep(1_000L)
                }
            }
            else -> error("Unknown test mode")
        }
    }
}
