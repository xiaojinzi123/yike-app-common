package com.xiaojinzi.tally.module.qianji.authorization

/** Builds the fixed, own-service accessibility grant script. */
object AccessibilityGrantCommand {
    private const val SERVICE_CLASS =
        "com.xiaojinzi.tally.module.qianji.QianjiAutoRecordService"
    private const val MAX_PACKAGE_NAME_LENGTH = 255
    private const val MAX_USER_ID = 100_000
    private val PACKAGE_NAME =
        Regex("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+")

    /** Returns the full ComponentName form for this app's fixed accessibility service. */
    fun component(packageName: String): String {
        requirePackageName(packageName)
        return "$packageName/$SERVICE_CLASS"
    }

    /** Builds the computer-side ADB command that grants this app secure-settings access. */
    fun adbGrantCommand(packageName: String, userId: Int): String {
        requirePackageName(packageName)
        requireUserId(userId)
        return "adb shell pm grant --user $userId $packageName android.permission.WRITE_SECURE_SETTINGS"
    }

    /** Merges only our fixed service; malformed reads must never become an empty list. */
    fun mergeServices(current: String?, packageName: String): String {
        val full = component(packageName)
        val short = if (SERVICE_CLASS.startsWith("$packageName.")) {
            "$packageName/" + SERVICE_CLASS.removePrefix(packageName)
        } else full
        val value = current.orEmpty()
        val entries = if (value.isEmpty()) emptyList() else value.split(':')
        require(entries.all { entry ->
            val parts = entry.split('/')
            parts.size == 2 && PACKAGE_NAME.matches(parts[0]) &&
                Regex("\\.?[A-Za-z_${'$'}][A-Za-z0-9_${'$'}]*(\\.[A-Za-z_${'$'}][A-Za-z0-9_${'$'}]*)*").matches(parts[1])
        }) { "Invalid accessibility services setting" }
        return if (entries.any { it == full || it == short }) value
        else (entries + full).joinToString(":")
    }

    /**
     * Returns a POSIX shell script for enabling only this app's accessibility service.
     *
     * The package name and user id are validated before being inserted into quoted shell
     * values. The script never prints the service list; failures are represented by exit codes.
     */
    fun script(packageName: String, userId: Int): String {
        requirePackageName(packageName)
        requireUserId(userId)

        val fullComponent = component(packageName)
        val shortComponent = if (SERVICE_CLASS.startsWith("$packageName.")) {
            packageName + "/" + SERVICE_CLASS.removePrefix(packageName)
        } else {
            fullComponent
        }
        return """
            #!/system/bin/sh
            # Privileged processes may start with a minimal environment.
            PATH=/system/bin:/system/xbin:/vendor/bin:/product/bin:${'$'}PATH
            export PATH
            # Disable glob expansion while treating settings values as data.
            set -f

            # A zero exit code is not enough: some settings failures print an exception.
            valid_services() {
                case "${'$'}1" in
                    ""|null) return 0 ;;
                    *[!A-Za-z0-9_.${'$'}/:]*|:*|*:|*::*) return 1 ;;
                esac
                REMAINING="${'$'}1"
                while [ -n "${'$'}REMAINING" ]; do
                    ENTRY="${'$'}{REMAINING%%:*}"
                    case "${'$'}ENTRY" in
                        */*) ;;
                        *) return 1 ;;
                    esac
                    ENTRY_PACKAGE="${'$'}{ENTRY%%/*}"
                    ENTRY_CLASS="${'$'}{ENTRY#*/}"
                    case "${'$'}ENTRY_PACKAGE" in
                        ""|.*|*.|*..*|*[!A-Za-z0-9_.]*) return 1 ;;
                    esac
                    case "${'$'}ENTRY_CLASS" in
                        ""|.|*.|*..*|*[!A-Za-z0-9_.${'$'}]*) return 1 ;;
                    esac
                    case "${'$'}REMAINING" in
                        *:*) REMAINING="${'$'}{REMAINING#*:}" ;;
                        *) break ;;
                    esac
                done
            }

            # Read once and stop if settings cannot be read.
            FIRST=${'$'}(settings --user $userId get secure enabled_accessibility_services 2>&1)
            STATUS=${'$'}?
            if [ "${'$'}STATUS" -ne 0 ] || ! valid_services "${'$'}FIRST"; then
                exit 10
            fi
            if [ "${'$'}FIRST" = "null" ] || [ -z "${'$'}FIRST" ]; then
                FIRST=""
            fi

            # Read again just before the merge to reduce lost concurrent updates.
            SECOND=${'$'}(settings --user $userId get secure enabled_accessibility_services 2>&1)
            STATUS=${'$'}?
            if [ "${'$'}STATUS" -ne 0 ] || ! valid_services "${'$'}SECOND"; then
                exit 11
            fi
            if [ "${'$'}SECOND" = "null" ] || [ -z "${'$'}SECOND" ]; then
                SECOND=""
            fi

            # Preserve the latest list, including any explicit removals by another writer.
            CURRENT="${'$'}SECOND"

            TARGET_FULL='$fullComponent'
            TARGET_SHORT='$shortComponent'
            case ":${'$'}CURRENT:" in
                *":${'$'}TARGET_FULL:"*|*":${'$'}TARGET_SHORT:"*)
                    ;;
                *)
                    if [ -n "${'$'}CURRENT" ]; then
                        CURRENT="${'$'}CURRENT:${'$'}TARGET_FULL"
                    else
                        CURRENT="${'$'}TARGET_FULL"
                    fi
                    ;;
            esac

            # Keep every existing entry while writing the merged list.
            settings --user $userId put secure enabled_accessibility_services "${'$'}CURRENT" >/dev/null 2>&1
            STATUS=${'$'}?
            if [ "${'$'}STATUS" -ne 0 ]; then
                exit 20
            fi
            settings --user $userId put secure accessibility_enabled 1 >/dev/null 2>&1
            STATUS=${'$'}?
            if [ "${'$'}STATUS" -ne 0 ]; then
                exit 21
            fi

            # Verify the exact merged list, then verify the enabled flag.
            CHECKED=${'$'}(settings --user $userId get secure enabled_accessibility_services 2>&1)
            STATUS=${'$'}?
            if [ "${'$'}STATUS" -ne 0 ] || ! valid_services "${'$'}CHECKED"; then
                exit 30
            fi
            if [ "${'$'}CHECKED" = "null" ] || [ -z "${'$'}CHECKED" ]; then
                CHECKED=""
            fi
            if [ "${'$'}CHECKED" != "${'$'}CURRENT" ]; then
                exit 31
            fi
            case ":${'$'}CHECKED:" in
                *":${'$'}TARGET_FULL:"*|*":${'$'}TARGET_SHORT:"*)
                    ;;
                *)
                    exit 32
                    ;;
            esac

            ENABLED=${'$'}(settings --user $userId get secure accessibility_enabled 2>&1)
            STATUS=${'$'}?
            if [ "${'$'}STATUS" -ne 0 ]; then
                exit 33
            fi
            if [ "${'$'}ENABLED" != "1" ]; then
                exit 34
            fi

            printf '%s\n' 'QIANJI_ACCESSIBILITY_ENABLED'
            exit 0
        """.trimIndent() + "\n"
    }

    private fun requirePackageName(packageName: String) {
        require(packageName.isNotEmpty()) { "packageName must not be empty" }
        require(packageName.length <= MAX_PACKAGE_NAME_LENGTH) {
            "packageName is too long"
        }
        require(PACKAGE_NAME.matches(packageName)) {
            "Invalid Android package name"
        }
    }

    private fun requireUserId(userId: Int) {
        require(userId in 0..MAX_USER_ID) { "Invalid userId" }
    }
}
