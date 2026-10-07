package dev.pilltracker.data

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class IntakeRepositoryTest {
    private val db =
        Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            PillDatabase::class.java,
        )
            .build()
    private val repo = RoomIntakeRepository(db.intakeDao())
    private val oct6 = LocalDate.of(2026, 10, 6)

    @After fun close() = db.close()

    @Test fun markTakenIsIdempotentPerPillDay() =
        runTest {
            repo.markTaken(oct6, Instant.parse("2026-10-06T07:00:00Z"))
            repo.markTaken(oct6, Instant.parse("2026-10-06T19:00:00Z"))
            assertEquals(setOf(oct6), repo.takenDays(oct6, oct6).first())
        }

    @Test fun pillDayIsStoredAsGivenNotDerivedFromInstant() =
        runTest {
            // Logged just after local midnight in UTC+2: the instant is still Oct 5 in UTC.
            repo.markTaken(oct6, Instant.parse("2026-10-05T22:30:00Z"))
            assertEquals(setOf(oct6), repo.takenDays(oct6.minusDays(1), oct6).first())
        }

    @Test fun takenDaysIsInclusiveRange() =
        runTest {
            listOf(1L, 15L, 31L).forEach { repo.markTaken(LocalDate.of(2026, 10, it.toInt()), Instant.EPOCH) }
            repo.markTaken(LocalDate.of(2026, 11, 1), Instant.EPOCH)
            assertEquals(
                setOf(1, 15, 31).map { LocalDate.of(2026, 10, it) }.toSet(),
                repo.takenDays(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31)).first(),
            )
        }
}
