package cz.parizmat.gitcraft.core.terminal.completion

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.isDirectory

class RepositoryPathCompletionProvider : CompletionProvider {
    override suspend fun complete(request: CompletionRequest): List<CompletionCandidate> = withContext(Dispatchers.IO) {
        val token = request.activeToken.removePrefix("\"").removePrefix("'")
        if (token.startsWith("-")) return@withContext emptyList()
        val normalizedToken = token.replace('\\', '/')
        val separator = normalizedToken.lastIndexOf('/')
        val parentText = if (separator >= 0) normalizedToken.substring(0, separator + 1) else ""
        val namePrefix = if (separator >= 0) normalizedToken.substring(separator + 1) else normalizedToken
        val directory = request.repositoryRoot.resolve(parentText).normalize()
        if (!directory.startsWith(request.repositoryRoot.normalize()) || !Files.isDirectory(directory)) {
            return@withContext emptyList()
        }
        Files.list(directory).use { stream ->
            stream.limit(MAX_VISITED_ENTRIES).map { path ->
                val display = parentText + path.fileName.toString() + if (path.isDirectory()) "/" else ""
                val escaped = if (display.any(Char::isWhitespace)) "'$display'" else display
                CompletionCandidate(
                    text = escaped,
                    type = CompletionType.FILE,
                    description = if (path.isDirectory()) "Repository folder" else "Repository file",
                    score = PATH_SCORE + if (path.isDirectory()) 5 else 0,
                )
            }.filter { candidate -> candidate.text.trim('\'', '"').substringAfterLast('/').startsWith(namePrefix, ignoreCase = true) }
                .sorted(compareByDescending<CompletionCandidate> { it.description.endsWith("folder") }.thenBy { it.text })
                .limit(MAX_RESULTS)
                .toList()
        }
    }

    private companion object {
        const val MAX_VISITED_ENTRIES = 200L
        const val MAX_RESULTS = 8L
        const val PATH_SCORE = 50
    }
}
