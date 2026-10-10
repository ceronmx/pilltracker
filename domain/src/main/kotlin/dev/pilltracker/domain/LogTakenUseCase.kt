package dev.pilltracker.domain

import java.time.Clock
import java.time.LocalDate

/** Logs today's pill day, then ends the reminder and its repeats (AC2.1, AC2.3, AC3.1). */
class LogTakenUseCase(
    private val intakes: IntakeRepository,
    private val notifier: Notifier,
    private val scheduler: ReminderScheduler,
    private val clock: Clock,
) {
    suspend operator fun invoke() {
        intakes.markTaken(LocalDate.now(clock), clock.instant())
        scheduler.cancelRepeat()
        notifier.clearReminder()
    }
}
