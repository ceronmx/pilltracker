package dev.pilltracker

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import dev.pilltracker.platform.showReminder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** AC2.2 neutral + SECRET, AC2.3 Taken action dismisses without opening the app. */
class ReminderNotificationTest {
    @get:Rule val notifications: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val manager = context.getSystemService(NotificationManager::class.java)

    private fun active() = manager.activeNotifications.map { it.notification }

    /** Posting and cancelling are asynchronous in the system server. */
    private fun waitUntil(condition: () -> Boolean) {
        val deadline = System.nanoTime() + 5_000_000_000
        while (!condition() && System.nanoTime() < deadline) Thread.sleep(100)
    }

    @Test fun neutralSecretNotificationWithTakenActionThatDismissesIt() {
        showReminder(context)
        waitUntil { active().isNotEmpty() }
        val n = active().single()
        assertEquals(context.getString(R.string.notification_title), n.extras.getString(Notification.EXTRA_TITLE))
        assertEquals(Notification.VISIBILITY_SECRET, n.visibility)
        assertEquals(context.getString(R.string.taken), n.actions.single().title)

        n.actions.single().actionIntent.send()
        waitUntil { active().isEmpty() }
        assertTrue(active().isEmpty())
    }
}
