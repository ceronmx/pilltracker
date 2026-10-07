package dev.pilltracker.domain.rules

import dev.pilltracker.domain.Settings
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class ReminderRulesTest {
    private val madrid = ZoneId.of("Europe/Madrid")
    private val nine = LocalTime.of(9, 0)

    private fun at(
        dateTime: String,
        zone: ZoneId = madrid,
    ) = LocalDateTime.parse(dateTime).atZone(zone).toInstant()

    @Test
    fun laterTodayIfTheTimeHasNotPassed() {
        assertEquals(at("2026-10-06T09:00"), nextReminderAt(nine, at("2026-10-06T08:59"), madrid))
    }

    @Test
    fun tomorrowOnceTheTimeHasPassedOrIsNow() {
        assertEquals(at("2026-10-07T09:00"), nextReminderAt(nine, at("2026-10-06T09:00"), madrid))
        assertEquals(at("2026-10-07T09:00"), nextReminderAt(nine, at("2026-10-06T23:59"), madrid))
    }

    @Test
    fun dayBoundaryJustAfterMidnight() {
        assertEquals(at("2026-10-07T09:00"), nextReminderAt(nine, at("2026-10-07T00:00"), madrid))
    }

    @Test
    fun dstFallBackKeepsWallClockTime() {
        // 2026-10-25 Europe falls back 03:00 -> 02:00; the next reminder is still 09:00 local.
        val next = nextReminderAt(nine, at("2026-10-24T10:00"), madrid)
        assertEquals(LocalDateTime.parse("2026-10-25T09:00"), next.atZone(madrid).toLocalDateTime())
    }

    @Test
    fun dstSpringForwardGapMovesLater() {
        // 2027-03-28 02:00 -> 03:00 in Madrid: a 02:30 reminder lands at 03:30 that day.
        val next = nextReminderAt(LocalTime.of(2, 30), at("2027-03-28T00:00"), madrid)
        assertEquals(LocalDateTime.parse("2027-03-28T03:30"), next.atZone(madrid).toLocalDateTime())
    }

    @Test
    fun travelUsesTheNewZone() {
        // 08:00 in Madrid is 02:00 in New York: next 09:00 is the New York one, 7 hours later.
        val ny = ZoneId.of("America/New_York")
        assertEquals(at("2026-10-06T09:00", ny), nextReminderAt(nine, at("2026-10-06T08:00"), ny))
    }

    @Test
    fun remindOnActiveAndPlaceboDaysNotOnBreakOrWhenTaken() {
        val pack21 = Settings(PackType.TWENTY_ONE_PLUS_SEVEN, LocalDate.of(2026, 10, 1), nine)
        val pack28 = pack21.copy(packType = PackType.TWENTY_EIGHT)
        assertTrue(shouldRemind(pack21, LocalDate.of(2026, 10, 21), taken = false))
        assertFalse(shouldRemind(pack21, LocalDate.of(2026, 10, 22), taken = false))
        assertTrue(shouldRemind(pack28, LocalDate.of(2026, 10, 22), taken = false))
        assertFalse(shouldRemind(pack28, LocalDate.of(2026, 10, 22), taken = true))
    }
}
