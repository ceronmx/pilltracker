package dev.pilltracker.domain

import dev.pilltracker.domain.rules.PackType
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
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
        alarmFired: Boolean = true,
    ) = runBlocking { HandleReminderUseCase(settings, intakes, scheduler, notifier, clock)(alarmFired) }

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
    fun rebootOrTimeChangeOnlyReschedules() {
        handle(clock("2026-10-06T09:00"), alarmFired = false)
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
    fun takenLogsTheCurrentPillDayAndInstantAndClearsTheReminder() {
        val clock = clock("2026-10-07T00:00:30")
        runBlocking { LogTakenUseCase(intakes, notifier, clock)() }
        assertEquals(mapOf(LocalDate.of(2026, 10, 7) to clock.instant()), intakes.intakes.value)
        assertEquals(1, notifier.cleared)
    }

    @Test
    fun takenWhileTravelingLogsTheLocalPillDay() {
        val instant = LocalDateTime.parse("2026-10-07T02:00").atZone(madrid).toInstant()
        runBlocking { LogTakenUseCase(intakes, notifier, Clock.fixed(instant, newYork))() }
        assertEquals(setOf(oct6), intakes.intakes.value.keys)
    }
}
