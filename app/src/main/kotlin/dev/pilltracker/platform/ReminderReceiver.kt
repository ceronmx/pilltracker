package dev.pilltracker.platform

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import dev.pilltracker.domain.IntakeRepository
import dev.pilltracker.domain.Notifier
import dev.pilltracker.domain.ReminderScheduler
import dev.pilltracker.domain.SettingsRepository
import dev.pilltracker.domain.rules.shouldRemind
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

/** Fires the daily reminder, and reschedules after reboot, app update, time or time-zone change (AC2.1). */
@AndroidEntryPoint
class ReminderReceiver : BroadcastReceiver() {
    @Inject lateinit var settings: SettingsRepository

    @Inject lateinit var intakes: IntakeRepository

    @Inject lateinit var scheduler: ReminderScheduler

    @Inject lateinit var notifier: Notifier

    @Inject lateinit var clock: Clock

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val done = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val s = settings.settings.first() ?: return@launch // setup not done: nothing to remind
                if (intent.action == ACTION_REMIND) {
                    val today = LocalDate.now(clock)
                    val taken = today in intakes.takenDays(today, today).first()
                    if (shouldRemind(s, today, taken)) notifier.showReminder()
                }
                scheduler.scheduleNext(s.reminderTime)
            } finally {
                done.finish()
            }
        }
    }
}
