package cz.parizmat.gitcraft.feature.main

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cz.parizmat.gitcraft.feature.changes.ChangesModel
import cz.parizmat.gitcraft.feature.changes.ChangesScreen
import cz.parizmat.gitcraft.feature.changes.GuidedInfo
import cz.parizmat.gitcraft.feature.sidebar.domain.enums.Destinations
import cz.parizmat.gitcraft.feature.sidebar.ui.SidebarModelView
import org.koin.compose.koinInject

@Composable
fun MainContent(model: ChangesModel, onOpenRepository: () -> Unit, modifier: Modifier, view: SidebarModelView = koinInject()) {
    val navigation by view.state.collectAsState()
    val state by model.state.collectAsState()
    Box(modifier) {
        when (navigation.currentDestination) {
            Destinations.CHANGES -> ChangesScreen(model, onOpenRepository)
            Destinations.REPOSITORY -> Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Repository", style = MaterialTheme.typography.headlineMedium)
                Text(state.repository?.rootPath?.toString() ?: "No repository open", style = MaterialTheme.typography.bodyMedium)
                GuidedInfo(state.repository?.currentBranch?.let { "Current branch: $it" } ?: "Open a local repository to get started.")
                OutlinedButton(onClick = onOpenRepository, enabled = !state.busy) { Text("Open repository") }
                Button(onClick = { view.selectDestination(Destinations.CHANGES) }) { Text("Review changes") }
            }
            else -> Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Text(navigation.currentDestination.name.lowercase().replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(8.dp))
                GuidedInfo("This workspace is not implemented yet.")
                TextButton(onClick = { view.selectDestination(Destinations.CHANGES) }) { Text("Back to Changes") }
            }
        }
    }
}
