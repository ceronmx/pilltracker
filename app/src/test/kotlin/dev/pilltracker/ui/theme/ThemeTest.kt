package dev.pilltracker.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import dev.pilltracker.domain.GuineaPig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ThemeTest {
    private fun contrast(
        a: Color,
        b: Color,
    ): Double {
        val (hi, lo) = listOf(a.luminance(), b.luminance()).sortedDescending()
        return (hi + 0.05) / (lo + 0.05)
    }

    @Test
    fun everyCoatsAccentTextMeetsWcagAa() {
        for (pig in GuineaPig.entries) for (dark in listOf(false, true)) {
            val c = colorSchemeFor(pig, dark)
            listOf(
                "primary" to contrast(c.primary, c.onPrimary),
                "primaryContainer" to contrast(c.primaryContainer, c.onPrimaryContainer),
                "secondaryContainer" to contrast(c.secondaryContainer, c.onSecondaryContainer),
            ).forEach { (role, ratio) -> assertTrue(ratio >= 4.5, "$pig dark=$dark $role: $ratio") }
        }
    }

    @Test
    fun coatsDifferAndTricolorKeepsTheBrown() {
        assertEquals(Color(0xFF8B5A2B), colorSchemeFor(GuineaPig.TRICOLOR, dark = false).primary)
        assertEquals(4, GuineaPig.entries.map { colorSchemeFor(it, dark = false).primary }.toSet().size)
    }
}
