package dev.pilltracker.platform

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.pilltracker.domain.Notifier
import dev.pilltracker.domain.ReminderScheduler
import dev.pilltracker.domain.rules.nextReminderAt
import java.time.Clock
import java.time.LocalTime
import javax.inject.Inject

internal const val ACTION_REMIND = "dev.pilltracker.REMIND"

private const val WINDOW_MILLIS = 10 * 60 * 1000L

/**
 * Inexact 10-minute window, no exact-alarm permission (ADR 0004). In deep Doze it waits for the next
 * maintenance window or screen-on; setAndAllowWhileIdle was rejected for its +1h window.
 */
class AlarmReminderScheduler
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val clock: Clock,
        private val notifier: Notifier,
    ) : ReminderScheduler {
        private val alarms get() = context.getSystemService(AlarmManager::class.java)

        // Same PendingIntent every time, so a new alarm replaces the old one and cancel() finds it.
        private fun remind() =
            PendingIntent.getBroadcast(
                context,
                0,
                Intent(context, ReminderReceiver::class.java).setAction(ACTION_REMIND),
                PendingIntent.FLAG_IMMUTABLE,
            )

        override fun scheduleNext(time: LocalTime) {
            val at = nextReminderAt(time, clock.instant(), clock.zone)
            alarms.setWindow(AlarmManager.RTC_WAKEUP, at.toEpochMilli(), WINDOW_MILLIS, remind())
        }

        override fun cancel() {
            alarms.cancel(remind())
            notifier.clearReminder()
        }
    }
