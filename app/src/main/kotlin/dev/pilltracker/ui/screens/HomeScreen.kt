package dev.pilltracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.pilltracker.R
import dev.pilltracker.domain.rules.DayKind
import dev.pilltracker.ui.components.MonthGrid
import dev.pilltracker.ui.components.PermissionNotice
import dev.pilltracker.ui.components.TakenButton
import dev.pilltracker.ui.preview.PillPreviews
import dev.pilltracker.ui.preview.Sample
import dev.pilltracker.ui.theme.GuineaPigImage
import dev.pilltracker.ui.theme.PillTrackerTheme
import java.time.DayOfWeek
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Today's one-tap Taken (AC3.1) above the month grid (AC4.1), all visible at first glance. */
@Composable
fun HomeScreen(
    state: HomeUiState,
    onEvent: (HomeUiEvent) -> Unit,
    onSettings: () -> Unit,
    onFixNotifications: () -> Unit = {},
) {
    val locale = LocalConfiguration.current.locales[0]
    val dateLabel = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale).format(state.today)
    val settingsButton = @Composable {
        IconButton(onSettings) { Icon(Icons.Filled.Settings, stringResource(R.string.settings)) }
    }
    ScreenFrame(dateLabel.replaceFirstChar { it.titlecase(locale) }, settingsButton) {
        Column(
            Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            if (state.notificationsOff) PermissionNotice(onFixNotifications)
            state.daysSinceLastPill?.let { days ->
                Card(Modifier.fillMaxWidth()) {
                    Text(
                        pluralStringResource(R.plurals.days_since_last_pill, days, days),
                        Modifier.padding(20.dp),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
            }
            state.hoursLate?.let { Text(pluralStringResource(R.plurals.hours_since_reminder, it, it)) }
            if (state.dayKind == DayKind.PLACEBO) {
                Text(stringResource(R.string.placebo_day), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (state.dayKind == DayKind.BREAK) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GuineaPigImage(Modifier.size(48.dp))
                    Spacer(Modifier.width(12.dp))
                    Text(stringResource(R.string.break_day), style = MaterialTheme.typography.headlineSmall)
                }
            } else {
                TakenButton(state.taken, { onEvent(HomeUiEvent.Taken) })
                if (state.taken) {
                    TextButton({ onEvent(HomeUiEvent.NotTaken) }, Modifier.align(Alignment.CenterHorizontally)) {
                        Text(stringResource(R.string.mark_not_taken))
                    }
                }
            }
            MonthGrid(
                state.month,
                state.firstDayOfWeek,
                state.weeks,
                { onEvent(HomeUiEvent.PreviousMonth) },
                { onEvent(HomeUiEvent.NextMonth) },
            )
            // A little herd in the space under the calendar; decorative.
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.Bottom,
            ) {
                GuineaPigImage(Modifier.size(36.dp))
                GuineaPigImage(Modifier.size(28.dp))
                GuineaPigImage(Modifier.size(36.dp).graphicsLayer(scaleX = -1f))
            }
        }
    }
}

private val sampleHome = HomeUiState(Sample.today, month = Sample.month, weeks = Sample.weeks(DayOfWeek.MONDAY))

@PillPreviews
@Composable
private fun HomeUntakenPreview() = PillTrackerTheme { HomeScreen(sampleHome, {}, {}) }

@PillPreviews
@Composable
private fun HomeNotificationsOffPreview() =
    PillTrackerTheme {
        HomeScreen(sampleHome.copy(notificationsOff = true), {}, {})
    }

@PillPreviews
@Composable
private fun HomeTakenPreview() = PillTrackerTheme { HomeScreen(sampleHome.copy(taken = true), {}, {}) }

@PillPreviews
@Composable
private fun HomePlaceboPreview() = PillTrackerTheme { HomeScreen(sampleHome.copy(dayKind = DayKind.PLACEBO), {}, {}) }

@PillPreviews
@Composable
private fun HomeBreakPreview() =
    PillTrackerTheme {
        HomeScreen(sampleHome.copy(dayKind = DayKind.BREAK), {}, {})
    }

@PillPreviews
@Composable
private fun HomeMissedPreview() = PillTrackerTheme { HomeScreen(sampleHome.copy(daysSinceLastPill = 3), {}, {}) }

@PillPreviews
@Composable
private fun HomeLatePreview() = PillTrackerTheme { HomeScreen(sampleHome.copy(hoursLate = 3), {}, {}) }
