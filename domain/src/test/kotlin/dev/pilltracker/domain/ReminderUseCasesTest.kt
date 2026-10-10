package dev.pilltracker.domain

import dev.pilltracker.domain.rules.PackType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.io.IOException
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class ReminderUseCasesTest {
    private val madrid = ZoneId.of("Europe/Madrid")
    private val newYork = ZoneId.of("America/New_York")
    private val oct6 = LocalDate.of(2026, 10, 6)
    private val nine = LocalTime.of(9, 0)

    // 21 pills from Oct 1: Oct 6 is a pill day, Oct 22-28 are break days.
    private val settings = InMemorySettingsRepository(Settings(PackType.WITH_BREAK, LocalDate.of(2026, 10, 1), nine))
    private val intakes = InMemoryIntakeRepository()
    private val scheduler = FakeReminderScheduler()
    private val notifier = FakeNotifier()

    private fun clock(
        dateTime: String,
        zone: ZoneId = madrid,
    ) = Clock.fixed(LocalDateTime.parse(dateTime).atZone(zone).toInstant(), zone)

    private fun handle(
        clock: Clock,
        trigger: ReminderTrigger = ReminderTrigger.DAILY,
    ) = runBlocking { HandleReminderUseCase(settings, intakes, scheduler, notifier, clock)(trigger) }

    private fun logTaken(clock: Clock) = runBlocking { LogTakenUseCase(intakes, notifier, scheduler, clock)() }

    @Test
    fun alarmOnAnUnloggedPillDayRemindsAndReschedules() {
        handle(clock("2026-10-06T09:00"))
        assertEquals(1, notifier.shown)
        assertEquals(listOf(nine), scheduler.scheduled)
    }

    @Test
    fun alarmOnALoggedDayOnlyReschedules() {
        intakes.intakes.value = mapOf(oct6 to Instant.EPOCH)
        handle(clock("2026-10-06T09:00"))
        assertEquals(0, notifier.shown)
        assertEquals(listOf(nine), scheduler.scheduled)
    }

    @Test
    fun alarmOnABreakDayOnlyReschedules() {
        handle(clock("2026-10-22T09:00"))
        assertEquals(0, notifier.shown)
        assertEquals(listOf(nine), scheduler.scheduled)
    }

    @Test
    fun aFailingReadStillSchedulesTheNextReminder() {
        val broken =
            object : IntakeRepository by intakes {
                override fun takenDays(
                    from: LocalDate,
                    to: LocalDate,
                ): Flow<Set<LocalDate>> = flow { throw IOException("disk") }
            }
        assertThrows(IOException::class.java) {
            runBlocking {
                HandleReminderUseCase(
                    settings,
                    broken,
                    scheduler,
                    notifier,
                    clock("2026-10-06T09:00"),
                )(ReminderTrigger.DAILY)
            }
        }
        assertEquals(listOf(nine), scheduler.scheduled)
    }

    @Test
    fun rebootOrTimeChangeOnlyReschedules() {
        handle(clock("2026-10-06T08:00"), ReminderTrigger.SYSTEM)
        assertEquals(0, notifier.shown)
        assertEquals(listOf(nine), scheduler.scheduled)
    }

    @Test
    fun beforeSetupDoesNothing() {
        settings.settings.value = null
        handle(clock("2026-10-06T09:00"))
        assertEquals(0, notifier.shown)
        assertEquals(emptyList<LocalTime>(), scheduler.scheduled)
    }

    @Test
    fun yesterdaysLogDoesNotCountAfterMidnight() {
        intakes.intakes.value = mapOf(oct6 to Instant.EPOCH)
        handle(clock("2026-10-06T23:59"))
        assertEquals(0, notifier.shown)
        handle(clock("2026-10-07T00:00"))
        assertEquals(1, notifier.shown)
    }

    @Test
    fun pillDayFollowsTheCurrentZoneWhenTraveling() {
        // Oct 7 02:00 in Madrid is still Oct 6 in New York, where Oct 6 is already logged.
        intakes.intakes.value = mapOf(oct6 to Instant.EPOCH)
        val instant = LocalDateTime.parse("2026-10-07T02:00").atZone(madrid).toInstant()
        handle(Clock.fixed(instant, newYork))
        assertEquals(0, notifier.shown)
        handle(Clock.fixed(instant, madrid))
        assertEquals(1, notifier.shown)
    }

    @Test
    fun remindsOnTheDstChangeDay() {
        // Europe falls back on 2026-10-25 at 03:00 -> 02:00; the 02:30 alarm is still Oct 25, a placebo day.
        settings.settings.value = Settings(PackType.WITH_PLACEBO, LocalDate.of(2026, 10, 1), LocalTime.of(2, 30))
        handle(clock("2026-10-25T02:30"))
        assertEquals(1, notifier.shown)
    }

    @Test
    fun dailyAlarmOnAnUnloggedPillDayStartsTheRepeats() {
        val clock = clock("2026-10-06T09:03")
        handle(clock)
        assertEquals(clock.instant().plusSeconds(300), scheduler.repeatAt)
    }

    @Test
    fun aRepeatWithinTheHourRemindsAgainAndChainsTheNext() {
        val clock = clock("2026-10-06T09:30")
        handle(clock, ReminderTrigger.REPEAT)
        assertEquals(1, notifier.shown)
        assertEquals(clock.instant().plusSeconds(300), scheduler.repeatAt)
        assertEquals(listOf(nine), scheduler.scheduled)
    }

    @Test
    fun theNotificationSaysHowLateOnceAnHourHasPassed() {
        handle(clock("2026-10-06T09:55"), ReminderTrigger.REPEAT)
        assertEquals(null, notifier.lastHoursLate)
        handle(clock("2026-10-06T10:00"), ReminderTrigger.REPEAT)
        assertEquals(1, notifier.lastHoursLate)
    }

    @Test
    fun aRepeatAfterTheHourIsSilentAndEndsTheChain() {
        handle(clock("2026-10-06T10:20"), ReminderTrigger.REPEAT)
        assertEquals(0, notifier.shown)
        assertEquals(null, scheduler.repeatAt)
    }

    @Test
    fun aLateDailyAlarmStillRemindsButDoesNotRepeat() {
        // Doze held the daily alarm past the repeat hour (ADR 0004).
        handle(clock("2026-10-06T11:00"))
        assertEquals(1, notifier.shown)
        assertEquals(null, scheduler.repeatAt)
    }

    @Test
    fun aRepeatAfterTakenOrOnABreakDayIsSilentAndEndsTheChain() {
        intakes.intakes.value = mapOf(oct6 to Instant.EPOCH)
        handle(clock("2026-10-06T09:30"), ReminderTrigger.REPEAT)
        handle(clock("2026-10-22T09:30"), ReminderTrigger.REPEAT)
        assertEquals(0, notifier.shown)
        assertEquals(null, scheduler.repeatAt)
    }

    @Test
    fun rebootDuringTheHourRestartsTheRepeatsWithoutNotifying() {
        val clock = clock("2026-10-06T09:30")
        handle(clock, ReminderTrigger.SYSTEM)
        assertEquals(0, notifier.shown)
        assertEquals(clock.instant().plusSeconds(300), scheduler.repeatAt)
    }

    @Test
    fun aRepeatJustBeforeMidnightDoesNotChainIntoTheNextPillDay() {
        settings.settings.value = settings.settings.value?.copy(reminderTime = LocalTime.of(23, 50))
        handle(clock("2026-10-06T23:56"), ReminderTrigger.REPEAT)
        assertEquals(1, notifier.shown)
        assertEquals(null, scheduler.repeatAt)
    }

    @Test
    fun takenLogsTheCurrentPillDayAndInstantAndClearsTheReminder() {
        val clock = clock("2026-10-07T00:00:30")
        scheduler.repeatAt = clock.instant()
        logTaken(clock)
        assertEquals(mapOf(LocalDate.of(2026, 10, 7) to clock.instant()), intakes.intakes.value)
        assertEquals(1, notifier.cleared)
        assertEquals(null, scheduler.repeatAt) // Taken ends the repeats (AC2.1)
    }

    @Test
    fun takenWhileTravelingLogsTheLocalPillDay() {
        val instant = LocalDateTime.parse("2026-10-07T02:00").atZone(madrid).toInstant()
        logTaken(Clock.fixed(instant, newYork))
        assertEquals(setOf(oct6), intakes.intakes.value.keys)
    }
}
