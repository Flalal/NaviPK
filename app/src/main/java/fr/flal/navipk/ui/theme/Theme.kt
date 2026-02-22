package fr.flal.navipk.ui.theme

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val NaviPKShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

private fun navipkDarkScheme(seed: Color? = null): ColorScheme {
    val primary = seed ?: AccentBlue
    val primaryDark = seed?.copy(alpha = 0.7f) ?: AccentBlueDark
    return darkColorScheme(
        primary = primary,
        onPrimary = Color.White,
        primaryContainer = primaryDark,
        onPrimaryContainer = Color.White,
        secondary = primary.copy(alpha = 0.8f),
        onSecondary = Color.White,
        secondaryContainer = SurfaceVariantDark,
        onSecondaryContainer = Color.White,
        tertiary = primary.copy(alpha = 0.6f),
        onTertiary = Color.White,
        background = NearBlack,
        onBackground = Color.White,
        surface = SurfaceDark,
        onSurface = Color.White,
        surfaceVariant = SurfaceVariantDark,
        onSurfaceVariant = Color(0xFFB0B0B0),
        surfaceContainerLowest = NearBlack,
        surfaceContainerLow = SurfaceDark,
        surfaceContainer = SurfaceVariantDark,
        surfaceContainerHigh = CardDark,
        surfaceContainerHighest = Color(0xFF2D333B),
        outline = Color(0xFF444C56),
        outlineVariant = Color(0xFF30363D)
    )
}

@Composable
private fun animateScheme(target: ColorScheme): ColorScheme {
    val duration = 500
    val spec = tween<Color>(duration)
    return target.copy(
        primary = animateColorAsState(target.primary, spec, label = "primary").value,
        onPrimary = animateColorAsState(target.onPrimary, spec, label = "onPrimary").value,
        primaryContainer = animateColorAsState(target.primaryContainer, spec, label = "primaryContainer").value,
        onPrimaryContainer = animateColorAsState(target.onPrimaryContainer, spec, label = "onPrimaryContainer").value,
        secondary = animateColorAsState(target.secondary, spec, label = "secondary").value,
        onSecondary = animateColorAsState(target.onSecondary, spec, label = "onSecondary").value,
        secondaryContainer = animateColorAsState(target.secondaryContainer, spec, label = "secondaryContainer").value,
        onSecondaryContainer = animateColorAsState(target.onSecondaryContainer, spec, label = "onSecondaryContainer").value,
        tertiary = animateColorAsState(target.tertiary, spec, label = "tertiary").value,
        onTertiary = animateColorAsState(target.onTertiary, spec, label = "onTertiary").value,
        background = animateColorAsState(target.background, spec, label = "background").value,
        onBackground = animateColorAsState(target.onBackground, spec, label = "onBackground").value,
        surface = animateColorAsState(target.surface, spec, label = "surface").value,
        onSurface = animateColorAsState(target.onSurface, spec, label = "onSurface").value,
        surfaceVariant = animateColorAsState(target.surfaceVariant, spec, label = "surfaceVariant").value,
        onSurfaceVariant = animateColorAsState(target.onSurfaceVariant, spec, label = "onSurfaceVariant").value,
        outline = animateColorAsState(target.outline, spec, label = "outline").value,
        outlineVariant = animateColorAsState(target.outlineVariant, spec, label = "outlineVariant").value,
        surfaceContainerLowest = animateColorAsState(target.surfaceContainerLowest, spec, label = "surfaceContainerLowest").value,
        surfaceContainerLow = animateColorAsState(target.surfaceContainerLow, spec, label = "surfaceContainerLow").value,
        surfaceContainer = animateColorAsState(target.surfaceContainer, spec, label = "surfaceContainer").value,
        surfaceContainerHigh = animateColorAsState(target.surfaceContainerHigh, spec, label = "surfaceContainerHigh").value,
        surfaceContainerHighest = animateColorAsState(target.surfaceContainerHighest, spec, label = "surfaceContainerHighest").value
    )
}

@Composable
fun NaviPKTheme(
    seedColor: Color? = null,
    content: @Composable () -> Unit
) {
    val baseScheme = when {
        seedColor != null -> navipkDarkScheme(seedColor)
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> dynamicDarkColorScheme(LocalContext.current)
        else -> navipkDarkScheme()
    }

    val animatedScheme = animateScheme(baseScheme)

    MaterialTheme(
        colorScheme = animatedScheme,
        typography = NaviPKTypography,
        shapes = NaviPKShapes,
        content = content
    )
}
