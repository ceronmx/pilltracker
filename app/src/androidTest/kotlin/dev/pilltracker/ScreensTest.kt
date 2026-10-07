package dev.pilltracker

import androidx.compose.ui.test.DeviceConfigurationOverride
import androidx.compose.ui.test.FontScale
import androidx.compose.ui.test.ForcedSize
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.then
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import dev.pilltracker.domain.Settings
import dev.pilltracker.domain.rules.DayKind
import dev.pilltracker.domain.rules.PackType
import dev.pilltracker.ui.preview.Sample
import dev.pilltracker.ui.screens.HomeScreen
import dev.pilltracker.ui.screens.HomeUiState
import dev.pilltracker.ui.screens.SettingsSheet
import dev.pilltracker.ui.screens.SettingsUiState
import dev.pilltracker.ui.screens.SetupScreen
import dev.pilltracker.ui.screens.SetupUiState
import dev.pilltracker.ui.theme.PillTrackerTheme
import org.junit.Rule
import org.junit.Test
import java.time.DayOfWeek.MONDAY
import java.time.LocalTime

/** Each screen renders static state in en and es (AC5.5). */
class ScreensTest {
    @get:Rule val rule = createComposeRule()

    private fun setup(
        lang: String,
        denied: Boolean = false,
    ) = rule.setContentIn(lang) {
        PillTrackerTheme { SetupScreen(SetupUiState(permissionDenied = denied), {}, {}, {}) }
    }

    private fun home(
        lang: String,
        taken: Boolean,
    ) = rule.setContentIn(lang) { PillTrackerTheme { HomeScreen(sampleHome.copy(taken = taken), {}, {}) } }

    private val sampleHome = HomeUiState(Sample.today, month = Sample.month, weeks = Sample.weeks(MONDAY))

    @Test fun setupEn() {
        setup("en")
        rule.onNodeWithText("Enable reminders").assertExists()
    }

    @Test fun setupEs() {
        setup("es")
        rule.onNodeWithText("Activar recordatorios").assertExists()
    }

    @Test fun setupDeniedEs() {
        setup("es", denied = true)
        rule.onNodeWithText("Reintentar").assertExists()
        rule.onNodeWithText("Continuar sin recordatorios").assertExists()
    }

    @Test fun setupAsksPackAndPillEn() {
        setup("en")
        rule.onNodeWithText("28 pills").assertIsSelected()
        rule.onNodeWithText("21 + 7 break").assertExists()
        rule.onNodeWithText("Pill 1 of 28").assertExists()
    }

    @Test fun setupAsksPackAndPillEs() {
        setup("es")
        rule.onNodeWithText("¿Qué pastilla toca hoy?").assertExists()
        rule.onNodeWithText("Pastilla 1 de 28").assertExists()
    }

    /** Small phone at 2x font: the form scrolls, the action button stays on screen (sv7). */
    @Test fun setupDeniedFitsSmallScreenLargeFont() {
        rule.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.FontScale(2f) then
                    DeviceConfigurationOverride.ForcedSize(DpSize(320.dp, 480.dp)),
            ) { PillTrackerTheme { SetupScreen(SetupUiState(permissionDenied = true), {}, {}, {}) } }
        }
        rule.onNodeWithText("Continue without reminders").assertIsDisplayed()
    }

    @Test fun homeBreakDayEn() {
        rule.setContentIn("en") {
            PillTrackerTheme { HomeScreen(sampleHome.copy(dayKind = DayKind.BREAK), {}, {}) }
        }
        rule.onNodeWithText("Break day").assertExists()
        rule.onNodeWithText("Taken").assertDoesNotExist()
    }

    @Test fun homePlaceboDayEs() {
        rule.setContentIn("es") {
            PillTrackerTheme { HomeScreen(sampleHome.copy(dayKind = DayKind.PLACEBO), {}, {}) }
        }
        rule.onNodeWithText("Día de placebo").assertExists()
        rule.onNodeWithText("Tomada").assertExists()
    }

    @Test fun homeEn() {
        home("en", taken = false)
        rule.onNodeWithText("Taken").assertExists()
    }

    @Test fun homeTakenEs() {
        home("es", taken = true)
        rule.onNodeWithText("Registrado hoy").assertExists()
    }

    /** Taken button and month grid on the same screen (u9o). */
    @Test fun homeShowsTakenAndMonthEn() {
        home("en", taken = false)
        rule.onNodeWithText("Taken").assertExists()
        rule.onNodeWithText("October 2026").assertExists()
        rule.onNodeWithContentDescription("October 2, 2026, not taken").assertExists()
    }

    @Test fun homeShowsTakenAndMonthEs() {
        home("es", taken = true)
        rule.onNodeWithText("Registrado hoy").assertExists()
        rule.onNodeWithText("Octubre 2026").assertExists()
        rule.onNodeWithContentDescription("1 de octubre de 2026, tomada").assertExists()
        rule.onNodeWithContentDescription("22 de octubre de 2026, día de descanso").assertExists()
        rule.onNodeWithContentDescription("29 de octubre de 2026, placebo").assertExists()
    }

    private val saved = Settings(PackType.TWENTY_ONE_PLUS_SEVEN, Sample.today, LocalTime.of(9, 0))

    /** Settings sheet: time, read-only pack, reset with confirmation (AC7.1-7.3). */
    @Test fun settingsSheetEn() {
        rule.setContentIn("en") {
            PillTrackerTheme {
                HomeScreen(sampleHome, {}, {})
                SettingsSheet(SettingsUiState(saved, confirmReset = true), {}, {})
            }
        }
        rule.onNodeWithContentDescription("Settings").assertExists()
        rule.onNodeWithText("21 + 7 break").assertExists()
        rule.onNodeWithText("Delete all data?").assertExists()
        rule.onNodeWithText("Delete").assertExists()
    }

    @Test fun settingsIconEs() {
        rule.setContentIn("es") {
            PillTrackerTheme {
                HomeScreen(sampleHome, {}, {})
                SettingsSheet(SettingsUiState(saved), {}, {})
            }
        }
        // The sheet is its own window, which the test-only locale override doesn't reach (it shows en);
        // the real app follows the system language everywhere, and lint MissingTranslation covers es keys.
        rule.onNodeWithContentDescription("Ajustes").assertExists()
    }
}
