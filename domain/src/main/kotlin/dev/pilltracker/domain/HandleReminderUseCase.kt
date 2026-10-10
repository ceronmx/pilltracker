package dev.pilltracker.domain

import dev.pilltracker.domain.rules.hoursLate
import dev.pilltracker.domain.rules.inRepeatWindow
import dev.pilltracker.domain.rules.nextRepeatAt
import dev.pilltracker.domain.rules.shouldRemind
import kotlinx.coroutines.flow.first
import java.time.Clock
import java.time.LocalDate

enum class ReminderTrigger {
    /** The daily alarm. */
    DAILY,

    /** A 5-minute repeat (ADR 0005). */
    REPEAT,

    /** Reboot, app update, time or zone change: reschedule only, never notify. */
    SYSTEM,
}

/** Shows the reminder when due, chains its repeats, and always schedules the next daily one (AC2.1). */
class HandleReminderUseCase(
    private val settings: SettingsRepository,
    private val intakes: IntakeRepository,
    private val scheduler: ReminderScheduler,
    private val notifier: Notifier,
    private val clock: Clock,
) {
    /**
     * A failure while reminding still schedules tomorrow's alarm, then rethrows; without settings there is no
     * time to schedule.
     */
    suspend operator fun invoke(trigger: ReminderTrigger) {
        val s = settings.settings.first() ?: return // setup not done: nothing to remind
        try {
            val now = clock.instant()
            val today = LocalDate.now(clock)
            val taken = today in intakes.takenDays(today, today).first()
            val notify =
                when (trigger) {
                    // Even when Doze held it past the repeat hour.
                    ReminderTrigger.DAILY -> true
                    ReminderTrigger.REPEAT -> inRepeatWindow(s.reminderTime, now, clock.zone)
                    ReminderTrigger.SYSTEM -> false
                }
            if (notify && shouldRemind(s, today, taken)) {
                notifier.showReminder(
                    hoursLate(s.reminderTime, now, clock.zone),
                )
            }
            // A SYSTEM trigger inside the hour restarts the chain a reboot or update dropped.
            nextRepeatAt(s, now, clock.zone, taken)?.let(scheduler::scheduleRepeat)
        } finally {
            scheduler.scheduleNext(s.reminderTime)
        }
    }
}
