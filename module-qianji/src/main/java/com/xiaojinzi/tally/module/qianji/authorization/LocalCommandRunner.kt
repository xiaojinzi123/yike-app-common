package com.xiaojinzi.tally.module.qianji.authorization

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

/** Runs a trusted command with bounded output and lifetime. */
object LocalCommandRunner {
    data class Result(
        val exitCode: Int?,
        val output: String,
        val timedOut: Boolean,
    )

    private const val MAX_OUTPUT_BYTES = 64 * 1024
    private const val IO_JOIN_MILLIS = 1_000L
    private const val KILL_WAIT_MILLIS = 1_000L

    /**
     * Runs a trusted executable directly. Arguments are never passed through a shell.
     * Standard output and error are merged and capped before being returned.
     */
    fun run(
        command: List<String>,
        stdin: String? = null,
        timeoutMillis: Long = 15_000L,
    ): Result {
        require(command.isNotEmpty()) { "command must not be empty" }
        require(timeoutMillis > 0L) { "timeoutMillis must be positive" }

        val process = try {
            ProcessBuilder(command)
                .redirectErrorStream(true)
                .start()
        } catch (_: IOException) {
            return Result(exitCode = null, output = "", timedOut = false)
        } catch (_: SecurityException) {
            return Result(exitCode = null, output = "", timedOut = false)
        }

        val collector = OutputCollector(process)
        val outputThread = Thread(collector, "qianji-command-output")
        outputThread.isDaemon = true
        outputThread.start()

        val inputThread = stdin?.let { input ->
            Thread(
                {
                    try {
                        process.outputStream.use { stream ->
                            stream.write(input.toByteArray(StandardCharsets.UTF_8))
                            stream.flush()
                        }
                    } catch (_: IOException) {
                        // The command may exit before consuming stdin.
                    }
                },
                "qianji-command-input",
            ).also {
                it.isDaemon = true
                it.start()
            }
        } ?: run {
            closeQuietly(process.outputStream)
            null
        }

        var timedOut = false
        var interrupted = false
        try {
            try {
                if (!process.waitFor(timeoutMillis, TimeUnit.MILLISECONDS)) {
                    timedOut = true
                    terminate(process)
                }
            } catch (_: InterruptedException) {
                interrupted = true
                timedOut = true
                terminate(process)
            }
        } finally {
            // A successful process can exit before the reader has consumed its final token.
            if (!timedOut) {
                joinQuietly(outputThread)
            }
            collector.stop()
            closeQuietly(process.outputStream)
            closeQuietly(process.inputStream)
            closeQuietly(process.errorStream)
            inputThread?.interrupt()
            outputThread.interrupt()
            joinQuietly(inputThread)
            joinQuietly(outputThread)
            if (interrupted) {
                Thread.currentThread().interrupt()
            }
        }

        val exitCode = if (timedOut) null else runCatching { process.exitValue() }.getOrNull()
        return Result(
            exitCode = exitCode,
            output = collector.text(),
            timedOut = timedOut,
        )
    }

    private fun terminate(process: Process) {
        runCatching { process.destroy() }
        runCatching {
            if (process.isAlive) {
                process.destroyForcibly()
            }
        }
        runCatching { process.waitFor(KILL_WAIT_MILLIS, TimeUnit.MILLISECONDS) }
        runCatching { process.destroyForcibly() }
    }

    private fun joinQuietly(thread: Thread?) {
        if (thread == null) return
        try {
            thread.join(IO_JOIN_MILLIS)
        } catch (_: InterruptedException) {
            thread.interrupt()
            Thread.currentThread().interrupt()
        }
    }

    private fun closeQuietly(stream: java.io.Closeable) {
        runCatching { stream.close() }
    }

    private class OutputCollector(private val process: Process) : Runnable {
        private val input: InputStream = process.inputStream
        private val bytes = ByteArrayOutputStream()
        @Volatile private var stopped = false

        override fun run() {
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (!stopped) {
                var available = try {
                    input.available()
                } catch (_: IOException) {
                    break
                }
                if (available == 0) {
                    if (process.isAlive) {
                        try {
                            Thread.sleep(10L)
                        } catch (_: InterruptedException) {
                            break
                        }
                        continue
                    }
                    // Recheck after observing exit: output may arrive after the first check.
                    available = try {
                        input.available()
                    } catch (_: IOException) {
                        break
                    }
                    if (available == 0) break
                }
                val count = try {
                    // Never block on a pipe inherited by a descendant of the command.
                    input.read(buffer, 0, minOf(buffer.size, available))
                } catch (_: IOException) {
                    break
                }
                if (count < 0) break
                synchronized(bytes) {
                    val remaining = MAX_OUTPUT_BYTES - bytes.size()
                    if (remaining > 0) {
                        bytes.write(buffer, 0, minOf(count, remaining))
                    }
                }
            }
        }

        fun stop() {
            stopped = true
        }

        fun text(): String = synchronized(bytes) {
            bytes.toByteArray().toString(StandardCharsets.UTF_8)
        }
    }
}
