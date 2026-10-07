package dev.pilltracker.domain

import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate

interface IntakeRepository {
    /** Pill days logged between [from] and [to], inclusive. */
    fun takenDays(
        from: LocalDate,
        to: LocalDate,
    ): Flow<Set<LocalDate>>

    /** Logs [day] as taken at [at]; a second call for the same pill day is a no-op (AC3.1). */
    suspend fun markTaken(
        day: LocalDate,
        at: Instant,
    )
}
