package dev.pilltracker.data

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import dev.pilltracker.domain.GuineaPig
import dev.pilltracker.domain.Settings
import dev.pilltracker.domain.rules.PackType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

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

class SettingsRepositoryTest {
    private val db =
        Room.inMemoryDatabaseBuilder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            PillDatabase::class.java,
        )
            .build()
    private val repo = RoomSettingsRepository(db)

    @After fun close() = db.close()

    @Test fun nullUntilSavedThenRoundTripsAndOverwrites() =
        runTest {
            assertEquals(null, repo.settings.first())
            val first = Settings(PackType.WITH_PLACEBO, LocalDate.of(2026, 10, 1), LocalTime.of(9, 0))
            repo.save(first)
            assertEquals(first, repo.settings.first())
            val second = Settings(PackType.WITH_BREAK, LocalDate.of(2026, 9, 20), LocalTime.of(21, 45), GuineaPig.CREAM)
            repo.save(second)
            assertEquals(second, repo.settings.first())
        }

    @Test fun deleteAllEmptiesSettingsAndIntakes() =
        runTest {
            val intakes = RoomIntakeRepository(db.intakeDao())
            val day = LocalDate.of(2026, 10, 6)
            repo.save(Settings(PackType.WITH_PLACEBO, day, LocalTime.of(9, 0)))
            intakes.markTaken(day, Instant.EPOCH)
            repo.deleteAll()
            assertEquals(null, repo.settings.first())
            assertEquals(emptySet<LocalDate>(), intakes.takenDays(LocalDate.MIN, LocalDate.MAX).first())
        }
}
