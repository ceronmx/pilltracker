package dev.pilltracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
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
import dev.pilltracker.ui.components.DayCell
import dev.pilltracker.ui.components.MonthGrid
import dev.pilltracker.ui.components.PermissionNotice
import dev.pilltracker.ui.components.ReminderTimePickerDialog
import dev.pilltracker.ui.components.TakenButton
import dev.pilltracker.ui.components.TimeSelector
import dev.pilltracker.ui.preview.PillPreviews
import dev.pilltracker.ui.preview.Sample
import dev.pilltracker.ui.theme.PillTrackerTheme
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
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
    time: LocalTime,
    showTimePicker: Boolean,
    permissionDenied: Boolean,
    onTimeClick: () -> Unit,
    onTimePicked: (LocalTime) -> Unit,
    onTimePickerDismiss: () -> Unit,
    onEnable: () -> Unit,
    onContinueWithout: () -> Unit,
) {
    ScreenFrame(stringResource(R.string.setup_title)) {
        TimeSelector(time, onTimeClick)
        if (permissionDenied) PermissionNotice(onEnable)
        Spacer(Modifier.weight(1f))
        if (permissionDenied) {
            TextButton(onContinueWithout, Modifier.align(Alignment.CenterHorizontally)) {
                Text(stringResource(R.string.continue_without_reminders))
            }
        } else {
            Button(onEnable, Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                Text(stringResource(R.string.enable_reminders), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
    if (showTimePicker) ReminderTimePickerDialog(time, onTimePicked, onTimePickerDismiss)
}

/** Today + one-tap Taken (AC3.1). */
@Composable
fun HomeScreen(
    today: LocalDate,
    taken: Boolean,
    onTaken: () -> Unit,
    onHistory: () -> Unit,
) {
    val locale = LocalConfiguration.current.locales[0]
    val dateLabel = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale).format(today)
    ScreenFrame(dateLabel.replaceFirstChar { it.titlecase(locale) }) {
        Spacer(Modifier.weight(1f))
        TakenButton(taken, onTaken)
        Spacer(Modifier.weight(1f))
        TextButton(onHistory, Modifier.align(Alignment.CenterHorizontally)) { Text(stringResource(R.string.history)) }
    }
}

/** Month grid of taken / not-taken days (AC4.1). Records only, no advice (AC4.2). */
@Composable
fun HistoryScreen(
    month: YearMonth,
    firstDayOfWeek: DayOfWeek,
    weeks: List<List<DayCell?>>,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    ScreenFrame(stringResource(R.string.history)) {
        MonthGrid(month, firstDayOfWeek, weeks, onPrevious, onNext)
    }
}

@PillPreviews
@Composable
private fun SetupPreview() = PillTrackerTheme { SetupScreen(Sample.time, false, false, {}, {}, {}, {}, {}) }

@PillPreviews
@Composable
private fun SetupDeniedPreview() = PillTrackerTheme { SetupScreen(Sample.time, false, true, {}, {}, {}, {}, {}) }

@PillPreviews
@Composable
private fun HomeUntakenPreview() = PillTrackerTheme { HomeScreen(Sample.today, taken = false, {}, {}) }

@PillPreviews
@Composable
private fun HomeTakenPreview() = PillTrackerTheme { HomeScreen(Sample.today, taken = true, {}, {}) }

@PillPreviews
@Composable
private fun HistoryPreview() =
    PillTrackerTheme {
        val first = WeekFields.of(LocalConfiguration.current.locales[0]).firstDayOfWeek
        HistoryScreen(Sample.month, first, Sample.weeks(first), {}, {})
    }
