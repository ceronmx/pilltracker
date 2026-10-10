package dev.pilltracker.domain.rules

import dev.pilltracker.domain.Settings
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

class RepeatRulesTest {
    private val madrid = ZoneId.of("Europe/Madrid")
    private val nine = LocalTime.of(9, 0)

    // 21 pills from Oct 1: Oct 6 is a pill day, Oct 22 a break day.
    private val pack = Settings(PackType.WITH_BREAK, LocalDate.of(2026, 10, 1), nine)

    private fun at(
        dateTime: String,
        zone: ZoneId = madrid,
    ) = LocalDateTime.parse(dateTime).atZone(zone).toInstant()

    private fun next(
        now: String,
        taken: Boolean = false,
        settings: Settings = pack,
    ) = nextRepeatAt(settings, at(now), madrid, taken)

    @Test
    fun repeatsFiveMinutesAfterTheReminder() {
        assertEquals(at("2026-10-06T09:05"), next("2026-10-06T09:00"))
        assertEquals(at("2026-10-06T09:12"), next("2026-10-06T09:07"))
    }

    @Test
    fun atMostTwelveRepeatsWithinTheHour() {
        var now = at("2026-10-06T09:00")
        var repeats = 0
        while (true) {
            now = nextRepeatAt(pack, now, madrid, taken = false) ?: break
            repeats++
        }
        assertEquals(12, repeats)
        assertEquals(at("2026-10-06T10:00"), now)
        assertNull(next("2026-10-06T09:56"))
    }

    @Test
    fun noRepeatWhenLoggedOnABreakDayOrBeforeTheReminder() {
        assertNull(next("2026-10-06T09:00", taken = true))
        assertNull(next("2026-10-22T09:00"))
        assertNull(next("2026-10-06T08:59"))
    }

    @Test
    fun repeatsStopAtMidnight() {
        val late = pack.copy(reminderTime = LocalTime.of(23, 50))
        assertEquals(at("2026-10-06T23:59:59"), next("2026-10-06T23:54:59", settings = late))
        assertNull(next("2026-10-06T23:55", settings = late)) // 00:00 is a new pill day
        assertNull(next("2026-10-07T00:00", settings = late))
    }

    @Test
    fun inWindowOnlyDuringTheHourAfterTheReminder() {
        assertFalse(inRepeatWindow(nine, at("2026-10-06T08:59"), madrid))
        assertTrue(inRepeatWindow(nine, at("2026-10-06T09:00"), madrid))
        assertTrue(inRepeatWindow(nine, at("2026-10-06T10:00"), madrid))
        assertFalse(inRepeatWindow(nine, at("2026-10-06T10:01"), madrid))
    }

    @Test
    fun repeatWindowIsOneRealHourAcrossDstFallBack() {
        // 2026-10-25 Madrid falls back 03:00 -> 02:00; a 02:30 reminder is the first 02:30 (CEST).
        val dst = pack.copy(packType = PackType.WITH_PLACEBO, reminderTime = LocalTime.of(2, 30))
        val due = at("2026-10-25T02:30")
        assertEquals(due.plusSeconds(300), nextRepeatAt(dst, due, madrid, taken = false))
        // 56 real minutes later the wall clock reads 02:26 (CET), yet the hour is nearly over.
        assertEquals(due.plusSeconds(3600), nextRepeatAt(dst, due.plusSeconds(3300), madrid, taken = false))
        assertNull(nextRepeatAt(dst, due.plusSeconds(3360), madrid, taken = false))
    }

    @Test
    fun repeatWindowFollowsTheCurrentZone() {
        // 09:00 Madrid is 03:00 New York: before the New York reminder, so no repeat there.
        val ny = ZoneId.of("America/New_York")
        val now = at("2026-10-06T09:00")
        assertNull(nextRepeatAt(pack, now, ny, taken = false))
        assertEquals(now.plusSeconds(300), nextRepeatAt(pack, now, madrid, taken = false))
    }

    @Test
    fun hoursLateFromTheFirstFullHour() {
        assertNull(hoursLate(nine, at("2026-10-06T08:00"), madrid))
        assertNull(hoursLate(nine, at("2026-10-06T09:59"), madrid))
        assertEquals(1, hoursLate(nine, at("2026-10-06T10:00"), madrid))
        assertEquals(14, hoursLate(nine, at("2026-10-06T23:59"), madrid))
    }

    @Test
    fun hoursLateResetsAtMidnight() {
        assertNull(hoursLate(nine, at("2026-10-07T00:00"), madrid))
    }

    @Test
    fun hoursLateCountsRealHoursAcrossDst() {
        // Fall back: 09:00 -> 12:00 wall clock on 2026-10-25 is 3 real hours; the extra hour is at 02:00-03:00.
        assertEquals(3, hoursLate(nine, at("2026-10-25T12:00"), madrid))
        // Spring forward: a 02:30 reminder falls in the gap and moves to 03:30.
        assertNull(hoursLate(LocalTime.of(2, 30), at("2027-03-28T04:29"), madrid))
        assertEquals(1, hoursLate(LocalTime.of(2, 30), at("2027-03-28T04:30"), madrid))
    }

    @Test
    fun hoursLateFollowsTheCurrentZone() {
        // 12:00 Madrid is 06:00 New York: three hours late in Madrid, before the reminder in New York.
        assertEquals(3, hoursLate(nine, at("2026-10-06T12:00"), madrid))
        assertNull(hoursLate(nine, at("2026-10-06T12:00"), ZoneId.of("America/New_York")))
    }
}
