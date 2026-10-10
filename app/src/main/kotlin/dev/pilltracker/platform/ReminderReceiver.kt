package dev.pilltracker.platform

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import dev.pilltracker.domain.HandleReminderUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Fires the daily reminder, and reschedules after reboot, app update, time or time-zone change (AC2.1). */
@AndroidEntryPoint
class ReminderReceiver : BroadcastReceiver() {
    @Inject lateinit var handleReminder: HandleReminderUseCase

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val done = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                handleReminder(alarmFired = intent.action == ACTION_REMIND)
            } finally {
                done.finish()
            }
        }
    }
}
