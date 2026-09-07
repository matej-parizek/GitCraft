package cz.parizmat.gitcraft.core.domain.element

import java.nio.file.Path

data class Repository(
    val rootPath: Path,
    val currentBranch: String?,
)