package cz.parizmat.gitcraft.core.domain.element

import java.nio.file.Path

data class FileDiff(
    val path: Path,
    val rows: List<DiffRow>,
    val isBinary: Boolean = false,
    val isTruncated: Boolean = false,
)

data class DiffRow(
    val type: DiffRowType,
    val text: String,
    val oldLine: Int? = null,
    val newLine: Int? = null,
)

enum class DiffRowType { CONTEXT, ADDED, DELETED, HEADER }
