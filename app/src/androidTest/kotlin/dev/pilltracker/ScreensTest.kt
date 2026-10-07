package dev.pilltracker

import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import dev.pilltracker.domain.rules.DayKind
import dev.pilltracker.ui.preview.Sample
import dev.pilltracker.ui.screens.HistoryScreen
import dev.pilltracker.ui.screens.HistoryUiState
import dev.pilltracker.ui.screens.HomeScreen
import dev.pilltracker.ui.screens.HomeUiState
import dev.pilltracker.ui.screens.SetupScreen
import dev.pilltracker.ui.screens.SetupUiState
import dev.pilltracker.ui.theme.PillTrackerTheme
import org.junit.Rule
import org.junit.Test
import java.time.DayOfWeek.MONDAY

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
    ) = rule.setContentIn(lang) { PillTrackerTheme { HomeScreen(HomeUiState(Sample.today, taken), {}, {}) } }

    private fun history(lang: String) =
        rule.setContentIn(lang) {
            PillTrackerTheme { HistoryScreen(HistoryUiState(Sample.month, MONDAY, Sample.weeks(MONDAY)), {}) }
        }

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

    @Test fun homeBreakDayEn() {
        rule.setContentIn("en") {
            PillTrackerTheme { HomeScreen(HomeUiState(Sample.today, dayKind = DayKind.BREAK), {}, {}) }
        }
        rule.onNodeWithText("Break day").assertExists()
        rule.onNodeWithText("Taken").assertDoesNotExist()
    }

    @Test fun homePlaceboDayEs() {
        rule.setContentIn("es") {
            PillTrackerTheme { HomeScreen(HomeUiState(Sample.today, dayKind = DayKind.PLACEBO), {}, {}) }
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

    @Test fun historyEn() {
        history("en")
        rule.onNodeWithText("October 2026").assertExists()
        rule.onNodeWithContentDescription("October 2, 2026, not taken").assertExists()
    }

    @Test fun historyEs() {
        history("es")
        rule.onNodeWithText("Octubre 2026").assertExists()
        rule.onNodeWithContentDescription("1 de octubre de 2026, tomada").assertExists()
        rule.onNodeWithContentDescription("22 de octubre de 2026, día de descanso").assertExists()
        rule.onNodeWithContentDescription("29 de octubre de 2026, placebo").assertExists()
    }
}
