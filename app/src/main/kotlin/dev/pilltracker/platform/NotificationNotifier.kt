package dev.pilltracker.platform

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.media.AudioAttributes
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.pilltracker.MainActivity
import dev.pilltracker.R
import dev.pilltracker.domain.Notifier
import javax.inject.Inject

// High importance so the reminder pops up as a banner, with the guinea pig sound (ADR 0005). A channel's
// importance and sound can't change once created, hence a new id; earlier channels are deleted.
internal const val CHANNEL_ID = "daily_reminder_v2"
internal val OLD_CHANNEL_IDS = listOf("daily", "daily_reminder")
private const val NOTIFICATION_ID = 1

class NotificationNotifier
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : Notifier {
        private val manager get() = context.getSystemService(NotificationManager::class.java)

        /** Once per process start (PillTrackerApp), not per reminder; also refreshes the localized name. */
        fun createChannel() {
            OLD_CHANNEL_IDS.forEach(manager::deleteNotificationChannel)
            val sound = Uri.parse("android.resource://${context.packageName}/${R.raw.guinea_pig}")
            val usage = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION).build()
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply {
                    lockscreenVisibility = Notification.VISIBILITY_SECRET
                    // Every notification, repeats included (ADR 0005).
                    setSound(sound, usage)
                },
            )
        }

        override fun showReminder(hoursLate: Int?) {
            if (!manager.areNotificationsEnabled()) return
            val open =
                PendingIntent.getActivity(
                    context,
                    0,
                    Intent(context, MainActivity::class.java),
                    PendingIntent.FLAG_IMMUTABLE,
                )
            val taken =
                PendingIntent.getBroadcast(
                    context,
                    0,
                    Intent(context, TakenReceiver::class.java),
                    PendingIntent.FLAG_IMMUTABLE,
                )
            val icon = Icon.createWithResource(context, R.drawable.ic_notification)
            val notification =
                Notification.Builder(context, CHANNEL_ID)
                    .setSmallIcon(icon)
                    .setColor(context.getColor(R.color.brand))
                    .setContentTitle(context.getString(R.string.notification_title))
                    .setContentText(
                        hoursLate?.let { context.resources.getQuantityString(R.plurals.hours_since_reminder, it, it) },
                    )
                    .setVisibility(Notification.VISIBILITY_SECRET)
                    .setContentIntent(open)
                    .setAutoCancel(true)
                    .addAction(Notification.Action.Builder(icon, context.getString(R.string.taken), taken).build())
                    .build()
            manager.notify(NOTIFICATION_ID, notification)
        }

        override fun clearReminder() = manager.cancel(NOTIFICATION_ID)
    }
