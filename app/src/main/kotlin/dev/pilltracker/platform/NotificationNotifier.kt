package dev.pilltracker.platform

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.pilltracker.MainActivity
import dev.pilltracker.R
import dev.pilltracker.domain.Notifier
import javax.inject.Inject

// High importance so the reminder pops up as a banner. Importance can't be raised on an existing channel,
// hence a new id; the old default-importance "daily" channel is deleted.
private const val CHANNEL_ID = "daily_reminder"
private const val OLD_CHANNEL_ID = "daily"
private const val NOTIFICATION_ID = 1

class NotificationNotifier
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : Notifier {
        private val manager get() = context.getSystemService(NotificationManager::class.java)

        /** Once per process start (PillTrackerApp), not per reminder; also refreshes the localized name. */
        fun createChannel() {
            manager.deleteNotificationChannel(OLD_CHANNEL_ID)
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_HIGH,
                ).apply { lockscreenVisibility = Notification.VISIBILITY_SECRET },
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
