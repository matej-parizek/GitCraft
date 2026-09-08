package cz.parizmat.gitcraft.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = GitCraftPurple,
    onPrimary = DarkTextPrimary,

    secondary = GitCraftBlue,
    onSecondary = DarkTextPrimary,

    background = DarkBackground,
    onBackground = DarkTextPrimary,

    surface = DarkSurface,
    onSurface = DarkTextPrimary,

    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,

    outline = DarkBorder,
    outlineVariant = DarkBorder,

    error = GitDeleted,
)

private val LightColorScheme = lightColorScheme(
    primary = GitCraftPurpleDark,
    onPrimary = LightSurface,

    secondary = GitCraftBlue,
    onSecondary = LightSurface,

    background = LightBackground,
    onBackground = LightTextPrimary,

    surface = LightSurface,
    onSurface = LightTextPrimary,

    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,

    outline = LightBorder,
    outlineVariant = LightBorder,

    error = GitDeleted,
)

@Composable
fun GitCraftTheme(
    content: @Composable () -> Unit,
) {
    val isDark = isSystemInDarkTheme()
    MaterialTheme(
        colorScheme = if (isDark) {
            DarkColorScheme
        } else {
            LightColorScheme
        },
        typography = GitCraftTypography,
        content = content,
    )
}
