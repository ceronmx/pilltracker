package dev.pilltracker

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
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
    }
}
