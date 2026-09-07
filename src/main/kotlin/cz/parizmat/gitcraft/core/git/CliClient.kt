package cz.parizmat.gitcraft.core.git

import arrow.core.Either
import arrow.core.flatMap
import cz.parizmat.gitcraft.core.domain.base.GitError
import cz.parizmat.gitcraft.core.domain.base.ProcessError
import cz.parizmat.gitcraft.core.domain.element.Branch
import cz.parizmat.gitcraft.core.domain.element.Commit
import cz.parizmat.gitcraft.core.domain.element.GitChange
import cz.parizmat.gitcraft.core.domain.element.Repository
import java.nio.file.Path

class CliClient(
    private val commandExecutor: CommandExecutor,
) : GitClient {
    override suspend fun openRepository(path: Path): Either<ProcessError, Repository> {
        return commandExecutor.execute(
            workingDirPath = path,
            args = listOf(
                "rev-parse",
                "--show-toplevel",
            ),
        ).flatMap { rootResult ->

            commandExecutor.execute(
                workingDirPath = path,
                args = listOf(
                    "branch",
                    "--show-current",
                ),
            ).map { branchResult ->

                Repository(
                    rootPath = Path.of(
                        rootResult.stdout.trim()
                    ),
                    currentBranch = branchResult.stdout
                        .trim()
                        .takeIf { it.isNotBlank() },
                )
            }

        }
    }

    override suspend fun status(repository: Repository): Either<GitError, List<GitChange>> {
        TODO("Not yet implemented")
    }

    override suspend fun branches(repository: Repository): Either<GitError, List<Branch>> {
        TODO("Not yet implemented")
    }

    override suspend fun commits(repository: Repository): Either<GitError, List<Commit>> {
        TODO("Not yet implemented")
    }

    private companion object {

        const val COMMAND_REV_PARSE = "rev-parse"
        const val OPTION_SHOW_TOPLEVEL = "--show-toplevel"

        const val COMMAND_BRANCH = "branch"
        const val OPTION_SHOW_CURRENT = "--show-current"

        const val COMMAND_REMOTE = "remote"
        const val COMMAND_GET_URL = "get-url"

        const val REMOTE_ORIGIN = "origin"

        const val COMMAND_STATUS = "status"
        const val OPTION_PORCELAIN_V2 = "--porcelain=v2"

        const val COMMAND_LOG = "log"
        const val OPTION_ONELINE = "--oneline"
    }
}