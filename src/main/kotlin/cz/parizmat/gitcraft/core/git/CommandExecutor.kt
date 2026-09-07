package cz.parizmat.gitcraft.core.git

import arrow.core.Either
import arrow.core.left
import cz.parizmat.gitcraft.core.domain.base.ProcessError
import cz.parizmat.gitcraft.core.domain.command.CommandResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.io.IOException
import java.nio.file.Path

class CommandExecutor(
    private val gitExecuter: String = "git"
) {
    suspend fun execute(
        workingDirPath: Path? = null,
        args: List<String> = emptyList(),
    ): Either<ProcessError, CommandResult> = withContext(Dispatchers.IO) {
        val command = buildList {
            add(gitExecuter)
            addAll(args)
        }

        try {
            val processBuilder = ProcessBuilder(*command.toTypedArray())
            workingDirPath?.let { processBuilder.directory(it.toFile()) }
            val process = processBuilder.start()

            coroutineScope {
                val exitCode = process.waitFor()
                val output = process.inputStream.bufferedReader().readText()
                val errorOutput = process.errorStream.bufferedReader().readText()

                val result = CommandResult(exitCode, output, errorOutput)

                if (exitCode == 0) {
                    Either.Right(result)
                } else {
                    Either.Left(
                        ProcessError.CommandFailed(
                            command, exitCode, errorOutput
                        )
                    )
                }
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

}