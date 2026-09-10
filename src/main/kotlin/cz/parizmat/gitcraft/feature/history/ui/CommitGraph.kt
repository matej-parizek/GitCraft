package cz.parizmat.gitcraft.feature.history.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import cz.parizmat.gitcraft.core.ui.theme.BranchBlue
import cz.parizmat.gitcraft.core.ui.theme.BranchGreen
import cz.parizmat.gitcraft.core.ui.theme.BranchOrange
import cz.parizmat.gitcraft.core.ui.theme.BranchPink
import cz.parizmat.gitcraft.core.ui.theme.BranchPurple
import cz.parizmat.gitcraft.core.ui.theme.HistoryDimensions
import cz.parizmat.gitcraft.core.ui.theme.GitModified
import cz.parizmat.gitcraft.feature.history.model.CommitGraphRow
import cz.parizmat.gitcraft.feature.history.model.GraphEdgePosition

@Composable
fun CommitGraph(row: CommitGraphRow, modifier: Modifier = Modifier) {
    val colors = graphColors()
    Canvas(modifier) {
        fun laneX(lane: Int): Float = HistoryDimensions.graphLeftPadding.toPx() +
            lane.coerceAtMost(MAX_VISIBLE_LANE) * HistoryDimensions.graphLaneSpacing.toPx()
        fun positionY(position: GraphEdgePosition): Float = when (position) {
            GraphEdgePosition.TOP -> 0f
            GraphEdgePosition.CENTER -> size.height / 2f
            GraphEdgePosition.BOTTOM -> size.height
        }
        row.edges.forEach { edge ->
            drawLine(
                color = colors[edge.colorIndex.mod(colors.size)],
                start = Offset(laneX(edge.fromLane), positionY(edge.fromPosition)),
                end = Offset(laneX(edge.toLane), positionY(edge.toPosition)),
                strokeWidth = HistoryDimensions.graphStrokeWidth.toPx(),
                cap = StrokeCap.Round,
            )
        }
        val node = Offset(laneX(row.nodeLane), size.height / 2f)
        drawCircle(colors[row.nodeColorIndex.mod(colors.size)], radius = HistoryDimensions.graphNodeRadius.toPx(), center = node)
        drawCircle(Color.White.copy(alpha = 0.78f), radius = HistoryDimensions.graphNodeCoreRadius.toPx(), center = node)
    }
}

@Composable
private fun graphColors(): List<Color> = listOf(BranchPink, BranchBlue, BranchPurple, BranchGreen, BranchOrange, GitModified)

private const val MAX_VISIBLE_LANE = 5
