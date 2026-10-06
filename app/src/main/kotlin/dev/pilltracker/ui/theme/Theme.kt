package dev.pilltracker.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

// iOS-like fallback palette (used below Android 12, where dynamic color is unavailable).
private val LightColors =
    lightColorScheme(
        primary = Color(0xFF007AFF),
        background = Color(0xFFF2F2F7),
        surface = Color(0xFFFFFFFF),
        surfaceVariant = Color(0xFFE5E5EA),
        onBackground = Color(0xFF000000),
        onSurface = Color(0xFF000000),
    )

private val DarkColors =
    darkColorScheme(
        primary = Color(0xFF0A84FF),
        background = Color(0xFF000000),
        surface = Color(0xFF1C1C1E),
        surfaceVariant = Color(0xFF2C2C2E),
        onBackground = Color(0xFFFFFFFF),
        onSurface = Color(0xFFFFFFFF),
    )

// Large radii: cards 20dp, buttons fully pill-shaped.
private val IosShapes =
    Shapes(
        small = RoundedCornerShape(12.dp),
        medium = RoundedCornerShape(20.dp),
        large = RoundedCornerShape(28.dp),
        extraLarge = RoundedCornerShape(50),
    )

@Composable
fun PillTrackerTheme(
    dark: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors =
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
                val ctx = LocalContext.current
                if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
            }
            dark -> DarkColors
            else -> LightColors
        }
    // ponytail: system font + default typography; add a Typography only if a design needs it.
    MaterialTheme(colorScheme = colors, shapes = IosShapes, content = content)
}
