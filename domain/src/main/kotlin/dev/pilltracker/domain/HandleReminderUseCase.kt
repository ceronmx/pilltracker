package dev.pilltracker.domain

import dev.pilltracker.domain.rules.shouldRemind
import kotlinx.coroutines.flow.first
import java.time.Clock
import java.time.LocalDate

/** Shows the daily reminder when due, and always schedules the next one (AC2.1). */
class HandleReminderUseCase(
    private val settings: SettingsRepository,
    private val intakes: IntakeRepository,
    private val scheduler: ReminderScheduler,
    private val notifier: Notifier,
    private val clock: Clock,
) {
    /** [alarmFired] is false after a reboot, app update or time change: only reschedule. */
    suspend operator fun invoke(alarmFired: Boolean) {
        val s = settings.settings.first() ?: return // setup not done: nothing to remind
        if (alarmFired) {
            val today = LocalDate.now(clock)
            val taken = today in intakes.takenDays(today, today).first()
            if (shouldRemind(s, today, taken)) notifier.showReminder()
        }
        scheduler.scheduleNext(s.reminderTime)
    }
}
