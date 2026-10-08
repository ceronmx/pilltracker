package dev.pilltracker.platform

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.pilltracker.MainActivity
import dev.pilltracker.R
import dev.pilltracker.domain.IntakeRepository
import dev.pilltracker.domain.ReminderScheduler
import dev.pilltracker.domain.SettingsRepository
import dev.pilltracker.domain.rules.nextReminderAt
import dev.pilltracker.domain.rules.shouldRemind
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import javax.inject.Inject

internal const val ACTION_REMIND = "dev.pilltracker.REMIND"

// High importance so the reminder pops up as a banner. Importance can't be raised on an existing channel,
// hence a new id; the old default-importance "daily" channel is deleted.
private const val CHANNEL_ID = "daily_reminder"
private const val OLD_CHANNEL_ID = "daily"
private const val NOTIFICATION_ID = 1
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
            context.getSystemService(NotificationManager::class.java).cancel(NOTIFICATION_ID)
        }
    }

/** Fires the daily reminder, and reschedules after reboot, app update, time or time-zone change (AC2.1). */
@AndroidEntryPoint
class ReminderReceiver : BroadcastReceiver() {
    @Inject lateinit var settings: SettingsRepository

    @Inject lateinit var intakes: IntakeRepository

    @Inject lateinit var scheduler: ReminderScheduler

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
                    if (shouldRemind(s, today, taken)) showReminder(context)
                }
                scheduler.scheduleNext(s.reminderTime)
            } finally {
                done.finish()
            }
        }
    }
}

/** The notification's Taken action: logs today without opening the app, then dismisses (AC2.3). */
@AndroidEntryPoint
class TakenReceiver : BroadcastReceiver() {
    @Inject lateinit var intakes: IntakeRepository

    @Inject lateinit var clock: Clock

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val done = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                intakes.markTaken(LocalDate.now(clock), clock.instant())
                context.getSystemService(NotificationManager::class.java).cancel(NOTIFICATION_ID)
            } finally {
                done.finish()
            }
        }
    }
}

/** Neutral wording, hidden on the lock screen (AC2.2). */
fun showReminder(context: Context) {
    val manager = context.getSystemService(NotificationManager::class.java)
    if (!manager.areNotificationsEnabled()) return
    val channel =
        NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply { lockscreenVisibility = Notification.VISIBILITY_SECRET }
    manager.deleteNotificationChannel(OLD_CHANNEL_ID)
    manager.createNotificationChannel(channel)
    val open =
        PendingIntent.getActivity(context, 0, Intent(context, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
    val taken =
        PendingIntent.getBroadcast(context, 0, Intent(context, TakenReceiver::class.java), PendingIntent.FLAG_IMMUTABLE)
    val icon = Icon.createWithResource(context, R.drawable.ic_notification)
    val notification =
        Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(icon)
            .setColor(context.getColor(R.color.brand))
            .setContentTitle(context.getString(R.string.notification_title))
            .setVisibility(Notification.VISIBILITY_SECRET)
            .setContentIntent(open)
            .setAutoCancel(true)
            .addAction(Notification.Action.Builder(icon, context.getString(R.string.taken), taken).build())
            .build()
    manager.notify(NOTIFICATION_ID, notification)
}
