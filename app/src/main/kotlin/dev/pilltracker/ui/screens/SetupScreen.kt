package dev.pilltracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.pilltracker.R
import dev.pilltracker.domain.rules.DayKind
import dev.pilltracker.ui.components.PackTypeSelector
import dev.pilltracker.ui.components.PermissionNotice
import dev.pilltracker.ui.components.PillNumberStepper
import dev.pilltracker.ui.components.ReminderTimePickerDialog
import dev.pilltracker.ui.components.TimeSelector
import dev.pilltracker.ui.preview.PillPreviews
import dev.pilltracker.ui.theme.GuineaPigImage
import dev.pilltracker.ui.theme.PillTrackerTheme

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

@PillPreviews
@Composable
private fun SetupPreview() = PillTrackerTheme { SetupScreen(SetupUiState(), {}, {}, {}) }

@PillPreviews
@Composable
private fun SetupDeniedPreview() = PillTrackerTheme { SetupScreen(SetupUiState(permissionDenied = true), {}, {}, {}) }
