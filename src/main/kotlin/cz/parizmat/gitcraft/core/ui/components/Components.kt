package cz.parizmat.gitcraft.core.ui.components

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import cz.parizmat.gitcraft.resources.Res
import cz.parizmat.gitcraft.resources.gitcraft_logo
import org.jetbrains.compose.resources.painterResource

@Composable
fun Logo(
    modifier: Modifier = Modifier,
){
    Icon(
        painter = painterResource(Res.drawable.gitcraft_logo),
        contentDescription = null,
        tint = Color.Unspecified,
        modifier = modifier
    )
}