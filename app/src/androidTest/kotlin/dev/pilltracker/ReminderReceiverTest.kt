package dev.pilltracker

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import dev.pilltracker.data.PillDatabase
import dev.pilltracker.data.RoomSettingsRepository
import dev.pilltracker.domain.Settings
import dev.pilltracker.domain.rules.PackType
import dev.pilltracker.platform.ACTION_REMIND
import dev.pilltracker.platform.ACTION_REPEAT
import dev.pilltracker.platform.DeviceClock
import dev.pilltracker.platform.ReminderReceiver
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime

/** The alarm broadcast through the real Hilt graph and database: notify on pill days, not on break days. */
class ReminderReceiverTest {
    @get:Rule val notifications: GrantPermissionRule = GrantPermissionRule.grant(Manifest.permission.POST_NOTIFICATIONS)

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val manager = context.getSystemService(NotificationManager::class.java)
    private val db = PillDatabase.create(context)

    // Other tests (the notification's Taken action) write to this same app database.
    @Before fun emptyDatabase() = db.clearAllTables()

    private fun repeatAlarm(flags: Int = 0) =
        PendingIntent.getBroadcast(
            context,
            0,
            Intent(context, ReminderReceiver::class.java).setAction(ACTION_REPEAT),
            flags or PendingIntent.FLAG_IMMUTABLE,
        )

    @After fun cleanUp() {
        context.getSystemService(AlarmManager::class.java).cancel(repeatAlarm())
        repeatAlarm().cancel()
        manager.cancelAll()
        db.clearAllTables()
        db.close()
    }

    private fun fireWith(
        settings: Settings,
        action: String = ACTION_REMIND,
    ): Int {
        runBlocking { RoomSettingsRepository(db).save(settings) }
        context.sendBroadcast(Intent(context, ReminderReceiver::class.java).setAction(action))
        // The receiver works on a background coroutine: poll, since a loaded emulator can take seconds.
        // ponytail: the break-day case always waits the full deadline to prove silence.
        val deadline = System.nanoTime() + 5_000_000_000L
        while (manager.activeNotifications.isEmpty() && System.nanoTime() < deadline) Thread.sleep(100)
        return manager.activeNotifications.size
    }

    @Test fun remindsOnAPillDay() {
        // Device date: pill 1 of a 21-pill pack starting today.
        assertEquals(1, fireWith(Settings(PackType.WITH_BREAK, LocalDate.now(DeviceClock), LocalTime.NOON)))
    }

    @Test fun aRepeatInsideTheHourRemindsAndSchedulesTheNext() {
        val now = LocalTime.now(DeviceClock)
        // ponytail: skipped near midnight, where the next repeat would fall on the next pill day.
        assumeTrue(now in LocalTime.of(0, 5)..LocalTime.of(23, 50))
        val started = Settings(PackType.WITH_BREAK, LocalDate.now(DeviceClock), now.minusMinutes(1))
        assertEquals(1, fireWith(started, ACTION_REPEAT))
        assertNotNull(repeatAlarm(PendingIntent.FLAG_NO_CREATE))
    }

    @Test fun silentOnABreakDay() {
        // Day 22 of the pack: first break day.
        assertEquals(
            0,
            fireWith(
                Settings(PackType.WITH_BREAK, LocalDate.now(DeviceClock).minusDays(21), LocalTime.NOON),
            ),
        )
    }
}
