package dev.pilltracker.domain

import java.time.Clock
import java.time.LocalDate

/** The notification's Taken action: logs today's pill day, then dismisses the reminder (AC2.3). */
class LogTakenUseCase(
    private val intakes: IntakeRepository,
    private val notifier: Notifier,
    private val clock: Clock,
) {
    suspend operator fun invoke() {
        intakes.markTaken(LocalDate.now(clock), clock.instant())
        notifier.clearReminder()
    }
}
