package cz.parizmat.gitcraft.core.terminal.completion

import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope

class TerminalCompletionService(
    private val providers: List<CompletionProvider>,
) {
    suspend fun complete(request: CompletionRequest): List<CompletionCandidate> = supervisorScope {
        providers
            .map { provider -> async { runCatching { provider.complete(request) }.getOrDefault(emptyList()) } }
            .flatMap { it.await() }
            .groupBy { it.text.lowercase() }
            .map { (_, matches) -> matches.maxBy { it.score } }
            .sortedWith(compareByDescending<CompletionCandidate> { it.score }.thenBy { it.text })
            .take(MAX_SUGGESTIONS)
    }

    private companion object {
        const val MAX_SUGGESTIONS = 8
    }
}
