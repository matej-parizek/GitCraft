package cz.parizmat.gitcraft.feature.history.model

import cz.parizmat.gitcraft.core.domain.element.Branch
import cz.parizmat.gitcraft.core.domain.element.Commit
import cz.parizmat.gitcraft.core.domain.element.CommitDetails
import cz.parizmat.gitcraft.core.domain.element.FileDiff
import cz.parizmat.gitcraft.core.domain.element.Repository

enum class HistoryRefType { BRANCH, TAG }

data class HistoryRef(
    val type: HistoryRefType,
    val name: String,
)

data class GraphEdge(
    val fromLane: Int,
    val toLane: Int,
    val colorIndex: Int,
    val fromPosition: GraphEdgePosition,
    val toPosition: GraphEdgePosition,
)

enum class GraphEdgePosition { TOP, CENTER, BOTTOM }

data class CommitGraphRow(
    val commit: Commit,
    val nodeLane: Int,
    val nodeColorIndex: Int,
    val edges: List<GraphEdge>,
)

data class HistoryUiState(
    val repository: Repository? = null,
    val rows: List<CommitGraphRow> = emptyList(),
    val visibleRows: List<CommitGraphRow> = emptyList(),
    val branches: List<Branch> = emptyList(),
    val selectedRef: HistoryRef? = null,
    val query: String = "",
    val selectedHash: String? = null,
    val details: CommitDetails? = null,
    val diff: FileDiff? = null,
    val diffOpen: Boolean = false,
    val loading: Boolean = false,
    val detailsLoading: Boolean = false,
    val diffLoading: Boolean = false,
    val error: String? = null,
)

object CommitGraphLayout {
    fun layout(commits: List<Commit>): List<CommitGraphRow> {
        val renderedHashes = commits.mapTo(hashSetOf()) { it.hash }
        val lanes = mutableListOf<String>()
        val colors = mutableMapOf<String, Int>()
        var nextColor = 0

        return commits.map { commit ->
            val hasIncomingEdge = commit.hash in lanes
            if (commit.hash !in lanes) lanes.add(commit.hash)
            val top = lanes.toList()
            val nodeLane = top.indexOf(commit.hash)
            val nodeColor = colors.getOrPut(commit.hash) { nextColor++.mod(GRAPH_COLOR_COUNT) }
            val bottom = top.toMutableList().apply { removeAt(nodeLane) }
            val visibleParents = commit.parentsHashes.filter(renderedHashes::contains)

            visibleParents.forEachIndexed { index, parent ->
                if (parent !in bottom) bottom.add((nodeLane + index).coerceAtMost(bottom.size), parent)
                colors.getOrPut(parent) {
                    if (index == 0) nodeColor else nextColor++.mod(GRAPH_COLOR_COUNT)
                }
            }

            val edges = buildList {
                top.forEachIndexed { fromLane, hash ->
                    if (hash != commit.hash) {
                        val toLane = bottom.indexOf(hash)
                        if (toLane >= 0) add(GraphEdge(fromLane, toLane, colors.getValue(hash), GraphEdgePosition.TOP, GraphEdgePosition.BOTTOM))
                    }
                }
                if (hasIncomingEdge) {
                    add(GraphEdge(nodeLane, nodeLane, nodeColor, GraphEdgePosition.TOP, GraphEdgePosition.CENTER))
                }
                visibleParents.forEach { parent ->
                    val toLane = bottom.indexOf(parent)
                    if (toLane >= 0) add(GraphEdge(nodeLane, toLane, colors.getValue(parent), GraphEdgePosition.CENTER, GraphEdgePosition.BOTTOM))
                }
            }
            lanes.clear()
            lanes.addAll(bottom)
            CommitGraphRow(commit, nodeLane, nodeColor, edges)
        }
    }

    private const val GRAPH_COLOR_COUNT = 6
}
