package cz.parizmat.gitcraft.core.domain.element

import cz.parizmat.gitcraft.core.domain.element.enums.FileStatus
import java.nio.file.Path

data class GitChange(
    val path: Path,
    val oldPath: Path?,
    val indexStatus: FileStatus,
    val workingTreeStatus: FileStatus,
)

