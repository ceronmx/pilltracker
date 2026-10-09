package dev.pilltracker.domain.rules

import dev.pilltracker.domain.rules.DayKind.ACTIVE
import dev.pilltracker.domain.rules.DayKind.BREAK
import dev.pilltracker.domain.rules.DayKind.PLACEBO
import dev.pilltracker.domain.rules.PackType.WITH_BREAK
import dev.pilltracker.domain.rules.PackType.WITH_PLACEBO
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class PackScheduleTest {
    private val start = LocalDate.of(2026, 10, 1)

    private fun day(n: Long) = start.plusDays(n - 1) // pill number n of the first pack

    @Test
    fun twentyEightHasActiveThenPlacebo() {
        assertEquals(ACTIVE, dayKind(WITH_PLACEBO, start, day(1)))
        assertEquals(ACTIVE, dayKind(WITH_PLACEBO, start, day(21)))
        assertEquals(PLACEBO, dayKind(WITH_PLACEBO, start, day(22)))
        assertEquals(PLACEBO, dayKind(WITH_PLACEBO, start, day(28)))
    }

    @Test
    fun twentyOnePlusSevenHasActiveThenBreak() {
        assertEquals(ACTIVE, dayKind(WITH_BREAK, start, day(21)))
        assertEquals(BREAK, dayKind(WITH_BREAK, start, day(22)))
        assertEquals(BREAK, dayKind(WITH_BREAK, start, day(28)))
    }

    @Test
    fun dayOfPackInvertsPackStartAndRepeats() {
        assertEquals(1, dayOfPack(start, day(1)))
        assertEquals(28, dayOfPack(start, day(28)))
        assertEquals(1, dayOfPack(start, day(29))) // next pack
        assertEquals(28, dayOfPack(start, day(0))) // day before pack 1: last day of the previous pack
        (1..28).forEach { n -> assertEquals(n, dayOfPack(packStart(day(40), n), day(40))) }
    }

    @Test
    fun cycleRepeatsEvery28Days() {
        assertEquals(ACTIVE, dayKind(WITH_BREAK, start, day(29)))
        assertEquals(BREAK, dayKind(WITH_BREAK, start, day(28 * 10 + 22)))
    }

    @Test
    fun datesBeforeStartFollowTheSameCycle() {
        // The day before pill 1 is day 28 of the previous pack.
        assertEquals(BREAK, dayKind(WITH_BREAK, start, start.minusDays(1)))
        assertEquals(BREAK, dayKind(WITH_BREAK, start, start.minusDays(7))) // day 22 of previous pack
        assertEquals(ACTIVE, dayKind(WITH_BREAK, start, start.minusDays(8))) // pill 21 of previous pack
    }

    @Test
    fun packStartFromTodaysPillNumber() {
        assertEquals(start, packStart(today = start, pillNumber = 1))
        assertEquals(start, packStart(today = day(22), pillNumber = 22))
    }

    @Test
    fun dayBoundaryDstAndTravelOnlyChangeTheDateNotTheRule() {
        // The rule takes the pill day as a LocalDate; these show the date callers pass in each case.
        val madrid = ZoneId.of("Europe/Madrid")
        val before = LocalDateTime.of(2026, 10, 21, 23, 59).atZone(madrid).toLocalDate() // pill 21
        val after = LocalDateTime.of(2026, 10, 22, 0, 0).atZone(madrid).toLocalDate() // pill 22
        assertEquals(ACTIVE, dayKind(WITH_BREAK, start, before))
        assertEquals(BREAK, dayKind(WITH_BREAK, start, after))

        // 2026-10-25 is the European DST fall-back day, still day 25 of the pack.
        assertEquals(BREAK, dayKind(WITH_BREAK, start, LocalDate.of(2026, 10, 25)))

        // One instant, two zones: New York is still on pill 21 while Madrid is on day 22.
        val instant = after.atStartOfDay(madrid).plusHours(2).toInstant()
        val nyDate = instant.atZone(ZoneId.of("America/New_York")).toLocalDate()
        assertEquals(ACTIVE, dayKind(WITH_BREAK, start, nyDate))
    }

    @Test
    fun noMissedDaysWhenNothingLoggedOrNoGap() {
        assertEquals(null, daysSinceLastPill(WITH_PLACEBO, start, lastTaken = null, today = day(5)))
        assertEquals(null, daysSinceLastPill(WITH_PLACEBO, start, lastTaken = day(5), today = day(5)))
        // 23:59 to 00:00: yesterday logged, today not yet: nothing missed.
        assertEquals(null, daysSinceLastPill(WITH_PLACEBO, start, lastTaken = day(4), today = day(5)))
        // Logged a later pill day (zone travel westwards): not a gap.
        assertEquals(null, daysSinceLastPill(WITH_PLACEBO, start, lastTaken = day(6), today = day(5)))
    }

    @Test
    fun missedActiveOrPlaceboDayCountsCalendarDaysSinceLastPill() {
        assertEquals(2, daysSinceLastPill(WITH_PLACEBO, start, lastTaken = day(3), today = day(5)))
        assertEquals(3, daysSinceLastPill(WITH_PLACEBO, start, lastTaken = day(21), today = day(24))) // placebo gap
        // The gap spans the 2026-10-25 DST fall-back day; still calendar days.
        assertEquals(4, daysSinceLastPill(WITH_PLACEBO, start, lastTaken = day(23), today = day(27)))
    }

    @Test
    fun breakDaysAreNeverMissed() {
        assertEquals(null, daysSinceLastPill(WITH_BREAK, start, lastTaken = day(21), today = day(28)))
        assertEquals(null, daysSinceLastPill(WITH_BREAK, start, lastTaken = day(21), today = day(29))) // new pack
        // Pill 1 of the new pack missed: counts back to pill 21.
        assertEquals(9, daysSinceLastPill(WITH_BREAK, start, lastTaken = day(21), today = day(30)))
    }
}
