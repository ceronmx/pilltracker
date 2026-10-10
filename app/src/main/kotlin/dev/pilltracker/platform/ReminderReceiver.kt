package dev.pilltracker.platform

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
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

    @Suppress("TooGenericExceptionCaught")
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val done = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                handleReminder(alarmFired = intent.action == ACTION_REMIND)
            } catch (e: Exception) {
                // An uncaught error here kills the process; log it and let the receiver finish.
                Log.e("ReminderReceiver", "failed", e)
            } finally {
                done.finish()
            }
        }
    }
}
