package cz.parizmat.gitcraft.core.terminal.completion

class GitCommandCompletionProvider(
    private val references: suspend (CompletionRequest) -> GitReferences,
) : CompletionProvider {
    override suspend fun complete(request: CompletionRequest): List<CompletionCandidate> {
        val beforeCursor = request.commandLine.substring(0, request.cursorOffset)
        val tokens = beforeCursor.trimStart().split(WHITESPACE).filter(String::isNotEmpty)
        if (tokens.firstOrNull()?.lowercase() != GIT) return emptyList()
        val endsWithSpace = beforeCursor.lastOrNull()?.isWhitespace() == true
        if (tokens.size == 1 && endsWithSpace) return commands("")
        if (tokens.size == 2 && !endsWithSpace) return commands(tokens[1])
        val command = tokens.getOrNull(1)?.lowercase() ?: return emptyList()
        val query = if (endsWithSpace) "" else tokens.lastOrNull().orEmpty()
        if (query.startsWith("-")) return options(command, query)
        if (command !in REFERENCE_COMMANDS) return emptyList()
        val refs = references(request)
        return buildList {
            addAll(refs.localBranches.map { candidate(it, CompletionType.BRANCH, "Local branch", query, 80) })
            if (command != SWITCH) {
                addAll(refs.remoteBranches.map { candidate(it, CompletionType.BRANCH, "Remote branch", query, 70) })
                addAll(refs.tags.map { candidate(it, CompletionType.TAG, "Repository tag", query, 60) })
            }
        }.filter { it.score > 0 }
    }

    private fun commands(query: String): List<CompletionCandidate> =
        COMMANDS.mapNotNull { (name, description) ->
            candidate(name, CompletionType.COMMAND, description, query, 100).takeIf { it.score > 0 }
        }

    private fun options(command: String, query: String): List<CompletionCandidate> =
        (GLOBAL_OPTIONS + OPTIONS_BY_COMMAND[command].orEmpty()).distinctBy { it.first }.mapNotNull { (name, description) ->
            candidate(name, CompletionType.OPTION, description, query, 90).takeIf { it.score > 0 }
        }

    private fun candidate(
        text: String,
        type: CompletionType,
        description: String,
        query: String,
        baseScore: Int,
    ): CompletionCandidate {
        val matchScore = fuzzyScore(text, query)
        return CompletionCandidate(text, type, description, if (matchScore < 0) 0 else baseScore + matchScore)
    }

    private fun fuzzyScore(text: String, query: String): Int {
        if (query.isEmpty()) return 1
        val normalizedText = text.lowercase()
        val normalizedQuery = query.lowercase()
        if (normalizedText.startsWith(normalizedQuery)) return 40 - (text.length - query.length).coerceAtMost(20)
        var cursor = 0
        for (character in normalizedQuery) {
            cursor = normalizedText.indexOf(character, cursor)
            if (cursor < 0) return if (normalizedText.firstOrNull() == normalizedQuery.firstOrNull()) 1 else -1
            cursor++
        }
        return 10
    }

    private companion object {
        const val GIT = "git"
        const val SWITCH = "switch"
        val WHITESPACE = Regex("\\s+")
        val REFERENCE_COMMANDS = setOf("switch", "checkout", "merge", "rebase", "show")
        val COMMANDS = linkedMapOf(
            "switch" to "Switch to a different branch",
            "show" to "Show information about Git objects",
            "status" to "Show working tree status",
            "add" to "Stage file contents",
            "commit" to "Record staged changes",
            "restore" to "Restore working tree files",
            "diff" to "Show changes between states",
            "log" to "Show commit history",
            "branch" to "List or manage branches",
            "checkout" to "Switch branches or restore files",
            "merge" to "Join development histories",
            "rebase" to "Reapply commits on another base",
            "fetch" to "Download refs from a remote",
            "pull" to "Fetch and integrate remote changes",
            "push" to "Update remote refs",
            "stash" to "Temporarily store local changes",
        )
        val GLOBAL_OPTIONS = listOf(
            "--help" to "Show Git help",
            "--version" to "Show the Git version",
            "--no-pager" to "Do not pipe output into a pager",
        )
        val OPTIONS_BY_COMMAND = mapOf(
            "status" to listOf("--short" to "Use compact status output", "--branch" to "Show branch information"),
            "commit" to listOf("--amend" to "Replace the latest commit", "--no-verify" to "Skip commit hooks"),
            "switch" to listOf("--create" to "Create and switch to a branch", "--detach" to "Switch to detached HEAD"),
            "log" to listOf("--oneline" to "Show one line per commit", "--graph" to "Draw the commit graph"),
            "diff" to listOf("--staged" to "Compare staged changes", "--stat" to "Show a change summary"),
        )
    }
}
