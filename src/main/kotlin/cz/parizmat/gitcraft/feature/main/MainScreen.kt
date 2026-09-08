package cz.parizmat.gitcraft.feature.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cz.parizmat.gitcraft.feature.changes.ChangesModel
import cz.parizmat.gitcraft.feature.sidebar.ui.MainSidebar

@Composable
fun MainScreen(model: ChangesModel, onOpenRepository: () -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        MainSidebar(model = model, onOpenRepository = onOpenRepository, modifier = Modifier.width(200.dp).fillMaxHeight())
        VerticalDivider(color = MaterialTheme.colorScheme.outline)
        MainContent(model = model, onOpenRepository = onOpenRepository, modifier = Modifier.weight(1f).fillMaxHeight())
    }
}
