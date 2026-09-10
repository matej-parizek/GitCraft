package cz.parizmat.gitcraft.core.domain.element

import java.time.ZonedDateTime

data class Commit(
    val hash: String,
    val shortHash: String,
    val authorName: String,
    val authorEmail: String,
    val date: ZonedDateTime,
    val message: String,
    val body: String = "",
    val parentsHashes: List<String>,
    val tags: List<String>,
    val branch: List<Branch>,
)
