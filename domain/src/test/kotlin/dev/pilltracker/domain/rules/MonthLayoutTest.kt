package dev.pilltracker.domain.rules

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.SUNDAY
import java.time.LocalDate
import java.time.YearMonth

class MonthLayoutTest {
    @Test
    fun monthStartingMidWeekGetsLeadingAndTrailingBlanks() {
        // October 2026 starts on a Thursday, has 31 days.
        val weeks = monthLayout(YearMonth.of(2026, 10), MONDAY)
        assertEquals(5, weeks.size)
        assertEquals(listOf(null, null, null), weeks.first().take(3))
        assertEquals(LocalDate.of(2026, 10, 1), weeks.first()[3])
        assertEquals(LocalDate.of(2026, 10, 31), weeks.last()[5])
        assertEquals(null, weeks.last()[6])
    }

    @Test
    fun firstDayOfWeekShiftsColumns() {
        val weeks = monthLayout(YearMonth.of(2026, 10), SUNDAY)
        assertEquals(LocalDate.of(2026, 10, 1), weeks.first()[4])
    }

    @Test
    fun monthStartingOnFirstDayHasNoBlanks() {
        // February 2027 starts on a Monday and has 28 days: exactly four full weeks.
        val weeks = monthLayout(YearMonth.of(2027, 2), MONDAY)
        assertEquals(4, weeks.size)
        assertEquals(28, weeks.flatten().filterNotNull().size)
        assertEquals(LocalDate.of(2027, 2, 1), weeks.first().first())
    }

    @Test
    fun everyWeekHasSevenCellsAndDaysAreInOrder() {
        val weeks = monthLayout(YearMonth.of(2028, 2), SUNDAY) // leap year
        weeks.forEach { assertEquals(7, it.size) }
        val days = weeks.flatten().filterNotNull()
        assertEquals((1..29).toList(), days.map { it.dayOfMonth })
    }
}
