package dev.pilltracker

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import dev.pilltracker.platform.NotificationNotifier
import javax.inject.Inject

@HiltAndroidApp
class PillTrackerApp : Application() {
    @Inject lateinit var notifier: NotificationNotifier

    override fun onCreate() {
        super.onCreate()
        notifier.createChannel()
    }
}
