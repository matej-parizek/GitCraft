package cz.parizmat.gitcraft.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cz.parizmat.gitcraft.resources.Res
import cz.parizmat.gitcraft.resources.gitcraft_logo
import org.jetbrains.compose.resources.painterResource

@Composable
fun Logo(
    modifier: Modifier = Modifier,
) {
    Icon(
        painter = painterResource(Res.drawable.gitcraft_logo),
        contentDescription = null,
        tint = Color.Unspecified,
        modifier = modifier
    )
}

@Composable
fun Watermark(
    waterMarkSize: Dp = 480.dp,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Logo(
            modifier = Modifier
                .size(waterMarkSize)
                .alpha(0.05f),
        )
    }
}
