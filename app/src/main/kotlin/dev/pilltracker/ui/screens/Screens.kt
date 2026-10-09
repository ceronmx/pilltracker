package dev.pilltracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
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
import dev.pilltracker.domain.rules.PackType
import dev.pilltracker.ui.components.GuineaPigBackground
import dev.pilltracker.ui.components.GuineaPigPicker
import dev.pilltracker.ui.components.MonthGrid
import dev.pilltracker.ui.components.PackTypeSelector
import dev.pilltracker.ui.components.PermissionNotice
import dev.pilltracker.ui.components.PillNumberStepper
import dev.pilltracker.ui.components.ReminderTimePickerDialog
import dev.pilltracker.ui.components.TakenButton
import dev.pilltracker.ui.components.TimeSelector
import dev.pilltracker.ui.preview.PillPreviews
import dev.pilltracker.ui.preview.Sample
import dev.pilltracker.ui.theme.GuineaPigImage
import dev.pilltracker.ui.theme.PillTrackerTheme
import java.time.DayOfWeek
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Shared screen frame: system-bar insets, page padding, large title with an optional corner [action]. */
@Composable
private fun ScreenFrame(
    title: String,
    action: @Composable () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    Scaffold { insets ->
        GuineaPigBackground(Modifier.fillMaxSize()) {
            Column(
                Modifier.fillMaxSize().padding(insets).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row {
                    Text(title, Modifier.weight(1f), style = MaterialTheme.typography.headlineLarge)
                    action()
                }
                content()
            }
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
            // Decorative mascot; nothing to announce.
            GuineaPigImage(Modifier.size(96.dp).align(Alignment.CenterHorizontally))
            PackTypeSelector(state.packType, { onEvent(SetupUiEvent.PackTypeChosen(it)) })
            PillNumberStepper(
                state.pillNumber,
                state.dayKind == DayKind.BREAK,
                { onEvent(SetupUiEvent.PillNumberChanged(it)) },
            )
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

/** Reminder time (editable), pack type (read-only) and Reset (AC7.1-7.3). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    state: SettingsUiState,
    onEvent: (SettingsUiEvent) -> Unit,
    onClose: () -> Unit,
) {
    val settings = state.settings ?: return
    ModalBottomSheet(onClose) {
        GuineaPigBackground {
            Column(
                Modifier.padding(horizontal = 24.dp).padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(R.string.settings),
                        Modifier.weight(1f),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    GuineaPigImage(Modifier.size(48.dp))
                }
                TimeSelector(settings.reminderTime, { onEvent(SettingsUiEvent.TimeClicked) })
                PillNumberStepper(state.dayOfPack, state.breakDay, { onEvent(SettingsUiEvent.DayOfPackChanged(it)) })
                GuineaPigPicker(settings.guineaPig, { onEvent(SettingsUiEvent.GuineaPigChosen(it)) })
                val packLabel =
                    if (settings.packType == PackType.WITH_PLACEBO) R.string.pack_placebo else R.string.pack_break
                Row {
                    Text(stringResource(R.string.pack_type_label), Modifier.weight(1f))
                    Text(stringResource(packLabel))
                }
                TextButton(
                    { onEvent(SettingsUiEvent.ResetClicked) },
                    Modifier.align(Alignment.CenterHorizontally),
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text(stringResource(R.string.delete_all_data)) }
            }
        }
    }
    if (state.showTimePicker) {
        ReminderTimePickerDialog(
            settings.reminderTime,
            { onEvent(SettingsUiEvent.TimePicked(it)) },
            { onEvent(SettingsUiEvent.PickerDismissed) },
        )
    }
    if (state.confirmReset) {
        AlertDialog(
            onDismissRequest = { onEvent(SettingsUiEvent.ResetDismissed) },
            icon = { GuineaPigImage(Modifier.size(56.dp)) },
            title = { Text(stringResource(R.string.delete_all_data_title)) },
            text = { Text(stringResource(R.string.delete_all_data_body)) },
            confirmButton = {
                TextButton({ onEvent(SettingsUiEvent.ResetConfirmed) }) { Text(stringResource(R.string.delete)) }
            },
            dismissButton = {
                TextButton({ onEvent(SettingsUiEvent.ResetDismissed) }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@PillPreviews
@Composable
private fun SetupPreview() = PillTrackerTheme { SetupScreen(SetupUiState(), {}, {}, {}) }

@PillPreviews
@Composable
private fun SetupDeniedPreview() = PillTrackerTheme { SetupScreen(SetupUiState(permissionDenied = true), {}, {}, {}) }

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
