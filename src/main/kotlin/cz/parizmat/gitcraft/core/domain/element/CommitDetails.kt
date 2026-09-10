package cz.parizmat.gitcraft.core.domain.element

import java.nio.file.Path

data class CommitFileChange(
    val path: Path,
    val additions: Int?,
    val deletions: Int?,
)

data class CommitDetails(
    val commit: Commit,
    val files: List<CommitFileChange>,
) {
    val additions: Int = files.sumOf { it.additions ?: 0 }
    val deletions: Int = files.sumOf { it.deletions ?: 0 }
}
