package cz.parizmat.gitcraft.feature.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cz.parizmat.gitcraft.feature.changes.ChangesModel
import cz.parizmat.gitcraft.feature.history.viewmodel.HistoryViewModel
import cz.parizmat.gitcraft.feature.sidebar.ui.MainSidebar
import cz.parizmat.gitcraft.feature.toolwindow.ui.TerminalToolWindow
import cz.parizmat.gitcraft.feature.toolwindow.viewmodel.TerminalToolWindowViewModel

@Composable
fun MainScreen(
    model: ChangesModel,
    historyViewModel: HistoryViewModel,
    terminalViewModel: TerminalToolWindowViewModel,
    onOpenRepository: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        MainSidebar(model = model, onOpenRepository = onOpenRepository, modifier = Modifier.width(200.dp).fillMaxHeight())
        VerticalDivider(color = MaterialTheme.colorScheme.outline)
        Column(Modifier.weight(1f).fillMaxHeight()) {
            MainContent(model = model, historyViewModel = historyViewModel, onOpenRepository = onOpenRepository, modifier = Modifier.weight(1f).fillMaxWidth())
            HorizontalDivider(color = MaterialTheme.colorScheme.outline)
            TerminalToolWindow(terminalViewModel)
        }
    }
}
