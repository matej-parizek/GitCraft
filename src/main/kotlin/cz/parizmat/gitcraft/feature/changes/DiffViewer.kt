package cz.parizmat.gitcraft.feature.changes

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Difference
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import cz.parizmat.gitcraft.core.domain.element.DiffRow
import cz.parizmat.gitcraft.core.domain.element.DiffRowType
import cz.parizmat.gitcraft.core.ui.theme.*

@Composable
fun DiffViewer(state: ChangesUiState, modifier: Modifier = Modifier) {
    val selected = state.selection
    Column(modifier) {
        if (selected == null) {
            Column(
                Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Outlined.Difference,
                    null,
                    Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))
                Text("Review before you commit", style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.height(8.dp))
                GuidedInfo("Select a changed file to view its diff.")
            }
            return@Column
        }
        val status = if (selected.staged) selected.change.indexStatus else selected.change.workingTreeStatus
        Column(
            Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            SelectionContainer {
                Text(
                    selected.change.path.toString(),
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "${status.letter()}  ${status.name.lowercase().replaceFirstChar { it.uppercase() }}",
                    color = status.color(),
                    style = MaterialTheme.typography.bodySmall
                )
                GuidedInfo(if (selected.staged) "Staged · included in next commit" else "Working tree · not staged")
                Spacer(Modifier.weight(1f))
                GuidedInfo("Read only")
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        when {
            state.diffLoading -> Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator(Modifier.size(24.dp)) }

            state.diffError != null -> Text(
                state.diffError,
                Modifier.padding(24.dp),
                color = MaterialTheme.colorScheme.error
            )

            state.diff?.isBinary == true -> GuidedInfo(
                "Binary file changed. A text diff is not available.",
                Modifier.padding(24.dp)
            )

            state.diff != null -> {
                val diff = state.diff
                if (diff.isTruncated) GuidedInfo(
                    "Preview limited for this large file. Staging includes the entire file.",
                    Modifier.padding(12.dp)
                )
                if (diff.rows.isEmpty()) GuidedInfo(
                    "No text changes to display. This may be an empty file, rename or file mode change.",
                    Modifier.padding(24.dp)
                )
                key(selected.change.path, selected.staged) {
                    val vertical = rememberLazyListState()
                    val horizontal = rememberScrollState()
                    Box(Modifier.weight(1f).fillMaxWidth()) {
                        LazyColumn(
                            state = vertical,
                            modifier = Modifier.fillMaxSize().padding(bottom = 12.dp, end = 12.dp)
                                .horizontalScroll(horizontal)
                        ) {
                            items(diff.rows) { row -> DiffLine(row) }
                        }
                        VerticalScrollbar(
                            rememberScrollbarAdapter(vertical),
                            Modifier.align(Alignment.CenterEnd).fillMaxHeight()
                        )
                        HorizontalScrollbar(
                            rememberScrollbarAdapter(horizontal),
                            Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(end = 12.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DiffLine(row: DiffRow) {
    val background = when (row.type) {
        DiffRowType.ADDED -> DiffAddedBackground
        DiffRowType.DELETED -> DiffDeletedBackground
        DiffRowType.HEADER -> MaterialTheme.colorScheme.surfaceVariant
        else -> Color.Transparent
    }
    val color = when (row.type) {
        DiffRowType.ADDED -> GitAdded
        DiffRowType.DELETED -> GitDeleted
        DiffRowType.HEADER -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> MaterialTheme.colorScheme.onSurface
    }
    Row(Modifier.fillMaxWidth().background(background).padding(vertical = 2.dp), verticalAlignment = Alignment.Top) {
        Text(
            row.oldLine?.toString() ?: "",
            Modifier.width(48.dp).padding(start = 8.dp),
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            row.newLine?.toString() ?: "",
            Modifier.width(48.dp),
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            when (row.type) {
                DiffRowType.ADDED -> "+"; DiffRowType.DELETED -> "−"; else -> " "
            },
            Modifier.width(20.dp),
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = color
        )
        SelectionContainer {
            Text(
                row.text.ifEmpty { " " },
                Modifier.padding(end = 24.dp),
                softWrap = false,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = color
            )
        }
    }
}
