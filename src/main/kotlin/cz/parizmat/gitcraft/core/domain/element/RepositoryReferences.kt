package cz.parizmat.gitcraft.core.domain.element

data class RepositoryReferences(
    val localBranches: List<String>,
    val remoteBranches: List<String>,
    val tags: List<String>,
)
