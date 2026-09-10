package cz.parizmat.gitcraft.feature.history.model

import cz.parizmat.gitcraft.core.domain.element.Commit
import java.time.ZonedDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CommitGraphLayoutTest {
    @Test
    fun `linear history stays in one lane`() {
        val rows = CommitGraphLayout.layout(
            listOf(
                commit("c3", "c2"),
                commit("c2", "c1"),
                commit("c1"),
            ),
        )

        assertTrue(rows.all { it.nodeLane == 0 })
        assertEquals(listOf(1, 2, 1), rows.map { it.edges.size })
        assertEquals(GraphEdgePosition.CENTER, rows.first().edges.single().fromPosition)
        assertEquals(GraphEdgePosition.TOP, rows.last().edges.single().fromPosition)
        assertEquals(GraphEdgePosition.CENTER, rows.last().edges.single().toPosition)
    }

    @Test
    fun `merge commit connects both parent lanes`() {
        val rows = CommitGraphLayout.layout(
            listOf(
                commit("merge", "main", "feature"),
                commit("feature", "base"),
                commit("main", "base"),
                commit("base"),
            ),
        )

        assertEquals(2, rows.first().edges.size)
        assertTrue(rows.first().edges.map { it.toLane }.distinct().size == 2)
        assertTrue(rows.any { it.nodeLane > 0 })
    }

    @Test
    fun `layout clips connections to commits outside the rendered page`() {
        val rows = CommitGraphLayout.layout(
            listOf(
                commit("visible-tip", "hidden-parent"),
                commit("other-visible-root"),
            ),
        )

        assertTrue(rows.all { it.edges.isEmpty() })
    }

    @Test
    fun `disconnected tip does not shift active lanes between rows`() {
        val rows = CommitGraphLayout.layout(
            listOf(
                commit("first-tip", "first-root"),
                commit("second-tip", "second-root"),
                commit("first-root"),
                commit("second-root"),
            ),
        )

        val disconnectedTip = rows[1]
        assertEquals(1, disconnectedTip.nodeLane)
        assertTrue(disconnectedTip.edges.any { it.fromLane == 0 && it.toLane == 0 })
    }

    private fun commit(hash: String, vararg parents: String) = Commit(
        hash = hash,
        shortHash = hash,
        authorName = "Test",
        authorEmail = "test@example.invalid",
        date = ZonedDateTime.parse("2026-09-09T12:00:00Z"),
        message = hash,
        parentsHashes = parents.toList(),
        tags = emptyList(),
        branch = emptyList(),
    )
}
