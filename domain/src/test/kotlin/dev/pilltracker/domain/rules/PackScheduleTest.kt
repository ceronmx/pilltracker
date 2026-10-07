package dev.pilltracker.domain.rules

import dev.pilltracker.domain.rules.DayKind.ACTIVE
import dev.pilltracker.domain.rules.DayKind.BREAK
import dev.pilltracker.domain.rules.DayKind.PLACEBO
import dev.pilltracker.domain.rules.PackType.TWENTY_EIGHT
import dev.pilltracker.domain.rules.PackType.TWENTY_ONE_PLUS_SEVEN
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
        assertEquals(ACTIVE, dayKind(TWENTY_EIGHT, start, day(1)))
        assertEquals(ACTIVE, dayKind(TWENTY_EIGHT, start, day(21)))
        assertEquals(PLACEBO, dayKind(TWENTY_EIGHT, start, day(22)))
        assertEquals(PLACEBO, dayKind(TWENTY_EIGHT, start, day(28)))
    }

    @Test
    fun twentyOnePlusSevenHasActiveThenBreak() {
        assertEquals(ACTIVE, dayKind(TWENTY_ONE_PLUS_SEVEN, start, day(21)))
        assertEquals(BREAK, dayKind(TWENTY_ONE_PLUS_SEVEN, start, day(22)))
        assertEquals(BREAK, dayKind(TWENTY_ONE_PLUS_SEVEN, start, day(28)))
    }

    @Test
    fun cycleRepeatsEvery28Days() {
        assertEquals(ACTIVE, dayKind(TWENTY_ONE_PLUS_SEVEN, start, day(29)))
        assertEquals(BREAK, dayKind(TWENTY_ONE_PLUS_SEVEN, start, day(28 * 10 + 22)))
    }

    @Test
    fun datesBeforeStartFollowTheSameCycle() {
        // The day before pill 1 is day 28 of the previous pack.
        assertEquals(BREAK, dayKind(TWENTY_ONE_PLUS_SEVEN, start, start.minusDays(1)))
        assertEquals(BREAK, dayKind(TWENTY_ONE_PLUS_SEVEN, start, start.minusDays(7))) // day 22 of previous pack
        assertEquals(ACTIVE, dayKind(TWENTY_ONE_PLUS_SEVEN, start, start.minusDays(8))) // pill 21 of previous pack
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
        assertEquals(ACTIVE, dayKind(TWENTY_ONE_PLUS_SEVEN, start, before))
        assertEquals(BREAK, dayKind(TWENTY_ONE_PLUS_SEVEN, start, after))

        // 2026-10-25 is the European DST fall-back day, still day 25 of the pack.
        assertEquals(BREAK, dayKind(TWENTY_ONE_PLUS_SEVEN, start, LocalDate.of(2026, 10, 25)))

        // One instant, two zones: New York is still on pill 21 while Madrid is on day 22.
        val instant = after.atStartOfDay(madrid).plusHours(2).toInstant()
        val nyDate = instant.atZone(ZoneId.of("America/New_York")).toLocalDate()
        assertEquals(ACTIVE, dayKind(TWENTY_ONE_PLUS_SEVEN, start, nyDate))
    }
}
