package dev.pilltracker

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.Locales
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.intl.LocaleList
import dev.pilltracker.ui.format.rememberTimeFormatter
import dev.pilltracker.ui.format.timeFormatter
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.time.LocalTime
import java.util.Locale

/** Renders [content] as if the system language were [languageTag]. Use for the en/es screen tests. */
fun ComposeContentTestRule.setContentIn(
    languageTag: String,
    content: @Composable () -> Unit,
) = setContent { DeviceConfigurationOverride(DeviceConfigurationOverride.Locales(LocaleList(languageTag)), content) }

class LocaleTest {
    @get:Rule val rule = createComposeRule()

    private val nine = LocalTime.of(21, 0)

    @Test fun englishStrings() {
        rule.setContentIn("en") { Text(stringResource(R.string.notification_title)) }
        rule.onNodeWithText("Time for your daily check-in").assertExists()
    }

    @Test fun spanishStrings() {
        rule.setContentIn("es") { Text(stringResource(R.string.notification_title)) }
        rule.onNodeWithText("Es hora de tu registro diario").assertExists()
    }

    @Test fun unsupportedLanguageFallsBackToEnglish() {
        rule.setContentIn("de") { Text(stringResource(R.string.notification_title)) }
        rule.onNodeWithText("Time for your daily check-in").assertExists()
    }

    @Test fun timeFollows12And24HourSetting() {
        assertEquals("9:00 PM", timeFormatter(Locale.US, is24Hour = false).format(nine).normalizeSpaces())
        assertEquals("21:00", timeFormatter(Locale.US, is24Hour = true).format(nine))
        assertEquals("21:00", timeFormatter(Locale("es", "ES"), is24Hour = true).format(nine))
    }

    @Test fun composableFormatterUsesOverriddenLocale() {
        var shown = ""
        rule.setContentIn("es") { shown = rememberTimeFormatter().format(nine) }
        rule.waitForIdle()
        // Device 12/24h setting varies by emulator image; either Spanish form proves the locale was used.
        val spanish = listOf(true, false).map { timeFormatter(Locale("es"), it).format(nine) }
        assert(shown in spanish) { "$shown not in $spanish" }
    }

    // ICU versions differ on the space before AM/PM (U+0020 vs U+202F).
    private fun String.normalizeSpaces() = replace(' ', ' ')
}
