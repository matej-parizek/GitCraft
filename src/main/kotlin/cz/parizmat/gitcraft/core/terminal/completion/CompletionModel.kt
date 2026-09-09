package cz.parizmat.gitcraft.core.terminal.completion

import java.nio.file.Path

enum class CompletionType(val label: String) {
    COMMAND("Command"),
    BRANCH("Branch"),
    TAG("Tag"),
    OPTION("Option"),
    FILE("File"),
    HISTORY("History"),
    POWERSHELL("PowerShell"),
}

data class CompletionCandidate(
    val text: String,
    val type: CompletionType,
    val description: String,
    val score: Int = 0,
    val replacementMode: ReplacementMode = ReplacementMode.ACTIVE_TOKEN,
)

enum class ReplacementMode { ACTIVE_TOKEN, WHOLE_LINE }

data class CompletionRequest(
    val commandLine: String,
    val cursorOffset: Int,
    val repositoryRoot: Path,
) {
    init {
        require(cursorOffset in 0..commandLine.length)
    }

    val activeToken: String
        get() = commandLine.substring(activeTokenStart(), cursorOffset)

    fun replaceActiveToken(replacement: String): String =
        commandLine.replaceRange(activeTokenStart(), cursorOffset, replacement)

    private fun activeTokenStart(): Int {
        var quote: Char? = null
        var tokenStart = 0
        for (index in 0 until cursorOffset) {
            val character = commandLine[index]
            when {
                quote != null && character == quote -> quote = null
                quote == null && character in QUOTES -> {
                    quote = character
                    tokenStart = index + 1
                }
                quote == null && character.isWhitespace() -> tokenStart = index + 1
            }
        }
        return tokenStart
    }

    private companion object {
        val QUOTES = setOf('\'', '"')
    }
}

data class GitReferences(
    val localBranches: List<String> = emptyList(),
    val remoteBranches: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
)

fun interface CompletionProvider {
    suspend fun complete(request: CompletionRequest): List<CompletionCandidate>
}
