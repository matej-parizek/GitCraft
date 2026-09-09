package cz.parizmat.gitcraft.feature.sidebar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import cz.parizmat.gitcraft.feature.changes.ChangesModel
import cz.parizmat.gitcraft.feature.changes.GuidedInfo
import cz.parizmat.gitcraft.feature.sidebar.domain.enums.Destinations
import org.koin.compose.koinInject

@Composable
fun MainSidebar(model: ChangesModel, onOpenRepository: () -> Unit, modifier: Modifier = Modifier, viewModel: SidebarModelView = koinInject()) {
    val navigation by viewModel.state.collectAsState()
    val state by model.state.collectAsState()
    Column(modifier.background(MaterialTheme.colorScheme.surface).padding(12.dp)) {
        Text("WORKSPACE", Modifier.padding(start = 8.dp, top = 16.dp, bottom = 12.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(state.repository?.rootPath?.fileName?.toString() ?: "No repository", Modifier.padding(horizontal = 8.dp), maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium)
        GuidedInfo(state.repository?.currentBranch ?: if (state.hasCommits) "Detached HEAD" else "Local repository", Modifier.padding(start = 8.dp, top = 4.dp, bottom = 20.dp))
        val entries = listOf(
            Triple(Destinations.REPOSITORY, "Repository", Icons.Outlined.FolderOpen),
            Triple(Destinations.CHANGES, "Changes", Icons.Outlined.Difference),
            Triple(Destinations.HISTORY, "History", Icons.Outlined.History),
            Triple(Destinations.BRANCHES, "Branches", Icons.Outlined.AccountTree),
            Triple(Destinations.STASHES, "Stashes", Icons.Outlined.Inventory2),
            Triple(Destinations.REMOTES, "Remotes", Icons.Outlined.Cloud),
        )
        entries.forEach { (destination, label, icon) ->
            SideBarItem(label = label, icon = icon, selected = navigation.currentDestination == destination, onClick = { viewModel.selectDestination(destination) })
            Spacer(Modifier.height(4.dp))
        }
        Spacer(Modifier.weight(1f))
        OutlinedButton(onClick = onOpenRepository, enabled = !state.busy, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.small) {
            Text("Open repository", style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(12.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        Spacer(Modifier.height(8.dp))
        SideBarItem(label = "Settings", icon = Icons.Outlined.Settings, selected = navigation.currentDestination == Destinations.SETTINGS, onClick = { viewModel.selectDestination(Destinations.SETTINGS) })
    }
}
