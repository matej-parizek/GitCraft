package cz.parizmat.gitcraft.core.domain.command

data class CommandResult(
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
)
