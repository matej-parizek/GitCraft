package cz.parizmat.gitcraft.core.terminal.completion

import java.util.concurrent.CopyOnWriteArrayList

class CommandHistoryCompletionProvider : CompletionProvider {
    private val entries = CopyOnWriteArrayList<String>()

    fun record(command: String) {
        val normalized = command.trim()
        if (normalized.isEmpty()) return
        entries.remove(normalized)
        entries.add(0, normalized)
        while (entries.size > MAX_ENTRIES) entries.removeLast()
    }

    override suspend fun complete(request: CompletionRequest): List<CompletionCandidate> {
        val query = request.commandLine.substring(0, request.cursorOffset).trimStart()
        if (query.isEmpty()) return emptyList()
        return entries.asSequence()
            .filter { it.startsWith(query, ignoreCase = true) && !it.equals(query, ignoreCase = true) }
            .take(MAX_RESULTS)
            .mapIndexed { index, command ->
                CompletionCandidate(
                    text = command,
                    type = CompletionType.HISTORY,
                    description = "Previously run in this terminal",
                    score = HISTORY_SCORE - index,
                    replacementMode = ReplacementMode.WHOLE_LINE,
                )
            }.toList()
    }

    private companion object {
        const val MAX_ENTRIES = 200
        const val MAX_RESULTS = 5
        const val HISTORY_SCORE = 75
    }
}
