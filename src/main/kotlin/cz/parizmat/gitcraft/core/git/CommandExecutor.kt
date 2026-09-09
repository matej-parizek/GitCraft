package cz.parizmat.gitcraft.core.git

import arrow.core.Either
import arrow.core.left
import cz.parizmat.gitcraft.core.domain.base.ProcessError
import cz.parizmat.gitcraft.core.domain.command.CommandResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.withContext
import java.io.IOException
import java.nio.file.Path

class CommandExecutor(
    private val gitExecuter: String = "git",
    private val timeoutMillis: Long = 60_000,
) {
    suspend fun execute(
        workingDirPath: Path? = null,
        args: List<String> = emptyList(),
        acceptedExitCodes: Set<Int> = setOf(0),
    ): Either<ProcessError, CommandResult> = withContext(Dispatchers.IO) {
        val command = buildList {
            add(gitExecuter)
            add("--no-pager")
            add("--literal-pathspecs")
            addAll(args)
        }

        try {
            val processBuilder = ProcessBuilder(*command.toTypedArray())
            workingDirPath?.let { processBuilder.directory(it.toFile()) }
            processBuilder.environment()["GIT_TERMINAL_PROMPT"] = "0"
            val process = processBuilder.start()
            process.outputStream.close()
            try {
                coroutineScope {
                    val outputReader = async { readOutput(process.inputStream) }
                    val errorReader = async { readOutput(process.errorStream) }
                    try {
                        val completed = withTimeoutOrNull(timeoutMillis) {
                            while (process.isAlive) delay(25)
                            true
                        } ?: false
                        if (!completed) stop(process)
                        val exitCode = if (completed) process.exitValue() else -1
                        val output = outputReader.await()
                        val error = errorReader.await()
                        val errorOutput = if (completed) error.text
                        else "Git command timed out after ${timeoutMillis / 1000} seconds. ${error.text}"
                        val result = CommandResult(exitCode, output.text, errorOutput)
                        if (output.truncated || error.truncated) {
                            Either.Left(ProcessError.CommandFailed(command, -1, "Git output exceeded the 16 MB safety limit."))
                        } else if (exitCode in acceptedExitCodes) {
                            Either.Right(result)
                        } else {
                            Either.Left(ProcessError.CommandFailed(command, exitCode, errorOutput))
                        }
                    } finally {
                        if (process.isAlive) stop(process)
                    }
                }
            } finally {
                if (process.isAlive) stop(process)
                process.inputStream.close()
                process.errorStream.close()
            }
        } catch (exception: IOException) {
            ProcessError.ProcessStartFailed(
                cause = exception,
            ).left()
        } catch (exception: InterruptedException) {
            Thread.currentThread().interrupt()
            ProcessError.ProcessInterrupted(
                cause = exception,
            ).left()
        }

    }

    private fun stop(process: Process) {
        process.descendants().use { descendants -> descendants.forEach { it.destroyForcibly() } }
        process.destroyForcibly()
    }

    private data class Output(val text: String, val truncated: Boolean)

    private fun readOutput(stream: java.io.InputStream): Output = stream.bufferedReader(Charsets.UTF_8).use { reader ->
        val text = StringBuilder()
        val buffer = CharArray(8192)
        var truncated = false
        while (true) {
            val count = reader.read(buffer)
            if (count < 0) break
            val retained = minOf(count, 16 * 1024 * 1024 - text.length)
            text.append(buffer, 0, retained)
            if (retained < count) truncated = true
        }
        Output(text.toString(), truncated)
    }

}
