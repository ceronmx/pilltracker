package dev.pilltracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.pilltracker.R
import dev.pilltracker.domain.rules.PackType
import dev.pilltracker.ui.components.GuineaPigBackground
import dev.pilltracker.ui.components.GuineaPigPicker
import dev.pilltracker.ui.components.PillNumberStepper
import dev.pilltracker.ui.components.ReminderTimePickerDialog
import dev.pilltracker.ui.components.TimeSelector
import dev.pilltracker.ui.theme.GuineaPigImage

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
