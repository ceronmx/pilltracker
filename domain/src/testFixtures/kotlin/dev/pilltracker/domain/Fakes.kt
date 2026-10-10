package dev.pilltracker.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

class InMemoryIntakeRepository : IntakeRepository {
    val intakes = MutableStateFlow(mapOf<LocalDate, Instant>())

    override fun takenDays(
        from: LocalDate,
        to: LocalDate,
    ): Flow<Set<LocalDate>> = intakes.map { all -> all.keys.filter { it in from..to }.toSet() }

    override suspend fun markTaken(
        day: LocalDate,
        at: Instant,
    ) = intakes.update { if (day in it) it else it + (day to at) }

    override suspend fun markNotTaken(day: LocalDate) = intakes.update { it - day }

    override fun lastTakenDay(day: LocalDate): Flow<LocalDate?> =
        intakes.map { all -> all.keys.filter { it <= day }.maxOrNull() }
}

class InMemorySettingsRepository(initial: Settings? = null) : SettingsRepository {
    override val settings = MutableStateFlow(initial)

    override suspend fun save(settings: Settings) {
        this.settings.value = settings
    }

    override suspend fun deleteAll() {
        settings.value = null
    }
}

class FakeReminderScheduler : ReminderScheduler {
    val scheduled = mutableListOf<LocalTime>()

    var cancelled = false

    /** The pending repeat, like the platform's single repeat alarm; null once cancelled. */
    var repeatAt: Instant? = null

    var repeatsCancelled = 0

    override fun scheduleNext(time: LocalTime) {
        scheduled += time
    }

    override fun scheduleRepeat(at: Instant) {
        repeatAt = at
    }

    override fun cancelRepeat() {
        repeatAt = null
        repeatsCancelled++
    }

    override fun cancel() {
        cancelled = true
        repeatAt = null
    }
}

class FakeNotifier : Notifier {
    var shown = 0

    var cleared = 0

    override fun showReminder() {
        shown++
    }

    override fun clearReminder() {
        cleared++
    }
}
