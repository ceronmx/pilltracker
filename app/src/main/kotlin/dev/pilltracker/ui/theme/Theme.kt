package dev.pilltracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.pilltracker.domain.GuineaPig

// Guinea pig palette: chestnut fur, cream, hay green. Fixed on every API level (no dynamic color) so the theme
// survives Android 12+ wallpaper colors. Containers are set too: M3 cards, sheets and dialogs read them.
private val LightColors =
    lightColorScheme(
        primary = Color(0xFF8B5A2B),
        onPrimary = Color(0xFFFFFFFF),
        primaryContainer = Color(0xFFFFDCC0),
        onPrimaryContainer = Color(0xFF2F1500),
        secondary = Color(0xFF765844),
        secondaryContainer = Color(0xFFFFDCC6),
        onSecondaryContainer = Color(0xFF2C1607),
        tertiary = Color(0xFF5E6236),
        tertiaryContainer = Color(0xFFE3E7AF),
        onTertiaryContainer = Color(0xFF1B1D00),
        background = Color(0xFFFFF8F3),
        onBackground = Color(0xFF221A14),
        surface = Color(0xFFFFF8F3),
        onSurface = Color(0xFF221A14),
        surfaceVariant = Color(0xFFF3DFD1),
        onSurfaceVariant = Color(0xFF52443A),
        outline = Color(0xFF84746A),
        outlineVariant = Color(0xFFD6C3B6),
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerLow = Color(0xFFFEF1E8),
        surfaceContainer = Color(0xFFF8EBE2),
        surfaceContainerHigh = Color(0xFFF2E6DC),
        surfaceContainerHighest = Color(0xFFECE0D7),
    )

private val DarkColors =
    darkColorScheme(
        primary = Color(0xFFFFB77A),
        onPrimary = Color(0xFF4A2800),
        primaryContainer = Color(0xFF6A3C14),
        onPrimaryContainer = Color(0xFFFFDCC0),
        secondary = Color(0xFFE5BFA8),
        secondaryContainer = Color(0xFF5D4130),
        onSecondaryContainer = Color(0xFFFFDCC6),
        tertiary = Color(0xFFC7CB95),
        tertiaryContainer = Color(0xFF464A20),
        onTertiaryContainer = Color(0xFFE3E7AF),
        background = Color(0xFF19120D),
        onBackground = Color(0xFFEFE0D5),
        surface = Color(0xFF19120D),
        onSurface = Color(0xFFEFE0D5),
        surfaceVariant = Color(0xFF52443A),
        onSurfaceVariant = Color(0xFFD6C3B6),
        outline = Color(0xFF9F8D81),
        outlineVariant = Color(0xFF52443A),
        surfaceContainerLowest = Color(0xFF140D08),
        surfaceContainerLow = Color(0xFF221A14),
        surfaceContainer = Color(0xFF261E18),
        surfaceContainerHigh = Color(0xFF312822),
        surfaceContainerHighest = Color(0xFF3C332C),
    )

// Large radii: cards 20dp. Buttons are pill-shaped by M3 default; extraLarge (dialogs, sheets) stays at M3's 28dp.
private val IosShapes =
    Shapes(
        small = RoundedCornerShape(12.dp),
        medium = RoundedCornerShape(20.dp),
        large = RoundedCornerShape(28.dp),
    )

@Composable
fun PillTrackerTheme(
    dark: Boolean = isSystemInDarkTheme(),
    guineaPig: GuineaPig = GuineaPig.TRICOLOR,
    content: @Composable () -> Unit,
) {
    // ponytail: system font + default typography; add a Typography only if a design needs it.
    CompositionLocalProvider(LocalGuineaPig provides guineaPig) {
        MaterialTheme(colorScheme = if (dark) DarkColors else LightColors, shapes = IosShapes, content = content)
    }
}
