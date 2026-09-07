package cz.parizmat.gitcraft.feature.sidebar.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Commit
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Timer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import cz.parizmat.gitcraft.feature.sidebar.domain.enums.Destinations
import org.koin.compose.koinInject

@Composable
fun MainSidebar(
    viewModel: SidebarModelView = koinInject(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.state.collectAsState()
    Column(modifier) {
        SideBarItem(
            label = Destinations.CHANGES.name,
            selected = uiState.currentDestination == Destinations.CHANGES,
            icon = Icons.Default.Newspaper,
            onClick = { viewModel.selectDestination(Destinations.CHANGES) },
        )
        SideBarItem(
            label = Destinations.BRANCHES.name,
            selected = uiState.currentDestination == Destinations.BRANCHES,
            icon = Icons.Default.Commit,
             onClick = { viewModel.selectDestination(Destinations.BRANCHES) },
        )

        SideBarItem(
            label = Destinations.HISTORY.name,
            selected = uiState.currentDestination == Destinations.HISTORY,
            icon = Icons.Default.Timer,
             onClick = { viewModel.selectDestination(Destinations.HISTORY) },
        )

    }

}