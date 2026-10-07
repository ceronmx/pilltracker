package dev.pilltracker.ui.components

import android.text.format.DateFormat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import dev.pilltracker.R
import dev.pilltracker.ui.format.rememberTimeFormatter
import dev.pilltracker.ui.preview.PillPreviews
import dev.pilltracker.ui.preview.Sample
import dev.pilltracker.ui.theme.PillTrackerTheme
import java.time.LocalTime

/** Shows the reminder time; tapping opens the picker. */
@Composable
fun TimeSelector(
    time: LocalTime,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier.fillMaxWidth()) {
        Row(
            Modifier
                .clickable(role = Role.Button, onClick = onClick)
                .padding(20.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.reminder_time_label), style = MaterialTheme.typography.bodyLarge)
            Text(
                rememberTimeFormatter().format(time),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderTimePickerDialog(
    time: LocalTime,
    onConfirm: (LocalTime) -> Unit,
    onDismiss: () -> Unit,
) {
    val state = rememberTimePickerState(time.hour, time.minute, DateFormat.is24HourFormat(LocalContext.current))
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton({ onConfirm(LocalTime.of(state.hour, state.minute)) }) {
                Text(stringResource(R.string.time_picker_confirm))
            }
        },
        dismissButton = { TextButton(onDismiss) { Text(stringResource(R.string.time_picker_cancel)) } },
        text = { TimePicker(state) },
    )
}

/** Neutral notice shown when notification or exact-alarm access is denied (AC1.3). */
@Composable
fun PermissionNotice(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.permission_denied), style = MaterialTheme.typography.bodyMedium)
            FilledTonalButton(onRetry) { Text(stringResource(R.string.retry)) }
        }
    }
}

/** One-tap log for today; disabled once taken (AC3.1). */
@Composable
fun TakenButton(
    taken: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Button(
        onClick = onClick,
        enabled = !taken,
        modifier = modifier.fillMaxWidth().heightIn(min = 64.dp),
    ) {
        Text(
            stringResource(if (taken) R.string.taken_today else R.string.taken),
            style = MaterialTheme.typography.titleLarge,
        )
    }
}

@PillPreviews
@Composable
private fun TimeSelectorPreview() = PillTrackerTheme { TimeSelector(Sample.time, {}) }

@PillPreviews
@Composable
private fun TimePickerDialogPreview() = PillTrackerTheme { ReminderTimePickerDialog(Sample.time, {}, {}) }

@PillPreviews
@Composable
private fun PermissionNoticePreview() = PillTrackerTheme { PermissionNotice({}) }

@PillPreviews
@Composable
private fun TakenButtonUntakenPreview() = PillTrackerTheme { TakenButton(taken = false, onClick = {}) }

@PillPreviews
@Composable
private fun TakenButtonTakenPreview() = PillTrackerTheme { TakenButton(taken = true, onClick = {}) }
