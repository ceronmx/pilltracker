package dev.pilltracker.platform

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.pilltracker.domain.Notifier
import dev.pilltracker.domain.ReminderScheduler
import dev.pilltracker.domain.rules.nextReminderAt
import java.time.Clock
import java.time.Instant
import java.time.LocalTime
import javax.inject.Inject

internal const val ACTION_REMIND = "dev.pilltracker.REMIND"
internal const val ACTION_REPEAT = "dev.pilltracker.REPEAT"

private const val WINDOW_MILLIS = 10 * 60 * 1000L

/** Exact alarms that fire in Doze too, via the auto-granted USE_EXACT_ALARM (ADR 0006). */
class AlarmReminderScheduler
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val clock: Clock,
        private val notifier: Notifier,
    ) : ReminderScheduler {
        private val alarms get() = context.getSystemService(AlarmManager::class.java)

        // Same PendingIntent per action every time, so a new alarm replaces the old one and cancel() finds it.
        private fun broadcast(action: String) =
            PendingIntent.getBroadcast(
                context,
                0,
                Intent(context, ReminderReceiver::class.java).setAction(action),
                PendingIntent.FLAG_IMMUTABLE,
            )

        private fun remind() = broadcast(ACTION_REMIND)

        private fun repeat() = broadcast(ACTION_REPEAT)

        // API 31-32 grant SCHEDULE_EXACT_ALARM by default but the user can revoke it; then fall back to inexact.
        private fun set(
            at: Instant,
            intent: PendingIntent,
        ) = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarms.canScheduleExactAlarms()) {
            alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at.toEpochMilli(), intent)
        } else {
            alarms.setWindow(AlarmManager.RTC_WAKEUP, at.toEpochMilli(), WINDOW_MILLIS, intent)
        }

        override fun scheduleNext(time: LocalTime) = set(nextReminderAt(time, clock.instant(), clock.zone), remind())

        override fun scheduleRepeat(at: Instant) = set(at, repeat())

        override fun cancelRepeat() = alarms.cancel(repeat())

        override fun cancel() {
            alarms.cancel(remind())
            cancelRepeat()
            notifier.clearReminder()
        }
    }
