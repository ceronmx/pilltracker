package dev.pilltracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
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

/** The accent roles that follow the chosen guinea pig; neutrals (background, surfaces) stay shared. */
private class Accent(
    val primary: Long,
    val onPrimary: Long,
    val primaryContainer: Long,
    val onPrimaryContainer: Long,
    val secondaryContainer: Long,
    val onSecondaryContainer: Long,
)

// Light, dark per coat. Tricolor is the base palette above; ThemeTest checks AA contrast for all.
private val accents =
    mapOf(
        GuineaPig.GINGER to
            (
                Accent(0xFFA04F00, 0xFFFFFFFF, 0xFFFFDCC2, 0xFF351000, 0xFFFFE0C2, 0xFF2E1500) to
                    Accent(0xFFFFB783, 0xFF552100, 0xFF793100, 0xFFFFDBC8, 0xFF6B3A16, 0xFFFFDCC2)
            ),
        GuineaPig.BLACK_WHITE to
            (
                Accent(0xFF3A3330, 0xFFFFFFFF, 0xFFE6E1DE, 0xFF1E1B19, 0xFFE8E2DE, 0xFF1F1B18) to
                    Accent(0xFFD6CFCA, 0xFF2B2522, 0xFF4A4542, 0xFFEDE7E3, 0xFF47423F, 0xFFEDE7E3)
            ),
        GuineaPig.CREAM to
            (
                Accent(0xFF8F4B4E, 0xFFFFFFFF, 0xFFFFD9D9, 0xFF3A0A10, 0xFFF6DDCB, 0xFF2C1810) to
                    Accent(0xFFFFB2B5, 0xFF561D23, 0xFF723338, 0xFFFFD9D9, 0xFF5A4335, 0xFFF6DDCB)
            ),
    )

/** The app's colors for [pig] (spec AC7.5): shared neutrals, coat accents. */
internal fun colorSchemeFor(
    pig: GuineaPig,
    dark: Boolean,
): ColorScheme {
    val base = if (dark) DarkColors else LightColors
    val a = accents[pig]?.let { if (dark) it.second else it.first } ?: return base
    return base.copy(
        primary = Color(a.primary),
        onPrimary = Color(a.onPrimary),
        primaryContainer = Color(a.primaryContainer),
        onPrimaryContainer = Color(a.onPrimaryContainer),
        secondaryContainer = Color(a.secondaryContainer),
        onSecondaryContainer = Color(a.onSecondaryContainer),
    )
}

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
        MaterialTheme(colorScheme = colorSchemeFor(guineaPig, dark), shapes = IosShapes, content = content)
    }
}
