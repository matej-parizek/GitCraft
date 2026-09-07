package cz.parizmat.gitcraft.core.domain.base

sealed interface GitError {
    data object NotGitRepository : GitError

    data class CommandFailed(
        val command: String,
        val exitCode: Int,
        val message: String
    ) : GitError

    data class Unexpected(val cause: Throwable) : GitError
}

sealed interface ProcessError {

    data class CommandFailed(
        val command: List<String>,
        val exitCode: Int,
        val stderr: String,
    ) : ProcessError

    data class ProcessStartFailed(val cause: Throwable) : ProcessError

    data class ProcessInterrupted(val cause: Throwable) : ProcessError
}