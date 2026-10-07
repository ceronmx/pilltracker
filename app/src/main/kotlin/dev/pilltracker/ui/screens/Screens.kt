package dev.pilltracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.pilltracker.R
import dev.pilltracker.domain.rules.DayKind
import dev.pilltracker.ui.components.MonthGrid
import dev.pilltracker.ui.components.PackTypeSelector
import dev.pilltracker.ui.components.PermissionNotice
import dev.pilltracker.ui.components.PillNumberStepper
import dev.pilltracker.ui.components.ReminderTimePickerDialog
import dev.pilltracker.ui.components.TakenButton
import dev.pilltracker.ui.components.TimeSelector
import dev.pilltracker.ui.preview.PillPreviews
import dev.pilltracker.ui.preview.Sample
import dev.pilltracker.ui.theme.PillTrackerTheme
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.WeekFields

/** Shared screen frame: system-bar insets, page padding, large title. */
@Composable
private fun ScreenFrame(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Scaffold { insets ->
        Column(
            Modifier.fillMaxSize().padding(insets).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(title, style = MaterialTheme.typography.headlineLarge)
            content()
        }
    }
}

/** First launch: preset time, picker, one Enable action (AC1.1-1.3). */
@Composable
fun SetupScreen(
    state: SetupUiState,
    onEvent: (SetupUiEvent) -> Unit,
    onEnable: () -> Unit,
    onContinueWithout: () -> Unit,
) {
    ScreenFrame(stringResource(R.string.setup_title)) {
        // Only the form scrolls, so the action button stays on screen on small phones / large fonts.
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            PackTypeSelector(state.packType, { onEvent(SetupUiEvent.PackTypeChosen(it)) })
            PillNumberStepper(state.pillNumber, state.maxPillNumber, { onEvent(SetupUiEvent.PillNumberChanged(it)) })
            TimeSelector(state.time, { onEvent(SetupUiEvent.TimeClicked) })
            if (state.permissionDenied) PermissionNotice(onEnable)
        }
        if (state.permissionDenied) {
            TextButton(onContinueWithout, Modifier.align(Alignment.CenterHorizontally)) {
                Text(stringResource(R.string.continue_without_reminders))
            }
        } else {
            Button(onEnable, Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                Text(stringResource(R.string.enable_reminders), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
    if (state.showTimePicker) {
        ReminderTimePickerDialog(
            state.time,
            { onEvent(SetupUiEvent.TimePicked(it)) },
            { onEvent(SetupUiEvent.PickerDismissed) },
        )
    }
}

/** Today + one-tap Taken (AC3.1). */
@Composable
fun HomeScreen(
    state: HomeUiState,
    onEvent: (HomeUiEvent) -> Unit,
    onHistory: () -> Unit,
) {
    val locale = LocalConfiguration.current.locales[0]
    val dateLabel = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale).format(state.today)
    ScreenFrame(dateLabel.replaceFirstChar { it.titlecase(locale) }) {
        Spacer(Modifier.weight(1f))
        if (state.dayKind == DayKind.PLACEBO) {
            Text(
                stringResource(R.string.placebo_day),
                Modifier.align(Alignment.CenterHorizontally),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (state.dayKind == DayKind.BREAK) {
            Text(
                stringResource(R.string.break_day),
                Modifier.align(Alignment.CenterHorizontally),
                style = MaterialTheme.typography.headlineSmall,
            )
        } else {
            TakenButton(state.taken, { onEvent(HomeUiEvent.Taken) })
        }
        Spacer(Modifier.weight(1f))
        TextButton(onHistory, Modifier.align(Alignment.CenterHorizontally)) { Text(stringResource(R.string.history)) }
    }
}

/** Month grid of taken / not-taken days (AC4.1). Records only, no advice (AC4.2). */
@Composable
fun HistoryScreen(
    state: HistoryUiState,
    onEvent: (HistoryUiEvent) -> Unit,
) {
    ScreenFrame(stringResource(R.string.history)) {
        MonthGrid(
            state.month,
            state.firstDayOfWeek,
            state.weeks,
            { onEvent(HistoryUiEvent.Previous) },
            { onEvent(HistoryUiEvent.Next) },
        )
    }
}

@PillPreviews
@Composable
private fun SetupPreview() = PillTrackerTheme { SetupScreen(SetupUiState(), {}, {}, {}) }

@PillPreviews
@Composable
private fun SetupDeniedPreview() = PillTrackerTheme { SetupScreen(SetupUiState(permissionDenied = true), {}, {}, {}) }

@PillPreviews
@Composable
private fun HomeUntakenPreview() = PillTrackerTheme { HomeScreen(HomeUiState(Sample.today), {}, {}) }

@PillPreviews
@Composable
private fun HomeTakenPreview() = PillTrackerTheme { HomeScreen(HomeUiState(Sample.today, taken = true), {}, {}) }

@PillPreviews
@Composable
private fun HomePlaceboPreview() =
    PillTrackerTheme { HomeScreen(HomeUiState(Sample.today, dayKind = DayKind.PLACEBO), {}, {}) }

@PillPreviews
@Composable
private fun HomeBreakPreview() =
    PillTrackerTheme {
        HomeScreen(HomeUiState(Sample.today, dayKind = DayKind.BREAK), {}, {})
    }

@PillPreviews
@Composable
private fun HistoryPreview() =
    PillTrackerTheme {
        val first = WeekFields.of(LocalConfiguration.current.locales[0]).firstDayOfWeek
        HistoryScreen(HistoryUiState(Sample.month, first, Sample.weeks(first)), {})
    }
