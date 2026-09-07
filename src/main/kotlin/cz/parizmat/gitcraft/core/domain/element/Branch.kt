package cz.parizmat.gitcraft.core.domain.element

import cz.parizmat.gitcraft.core.domain.element.enums.BranchType

data class Branch(
    val name: String,
    val type: BranchType,
    val isCurrent: Boolean,
    val isMerged: Boolean,
    val ahead: Int,
    val behind: Int,
    val lastCommit: String,
) {
}