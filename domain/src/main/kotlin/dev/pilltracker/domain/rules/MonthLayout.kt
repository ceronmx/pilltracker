package dev.pilltracker.domain.rules

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

private const val DAYS_IN_WEEK = 7

/** Weeks of [month] as rows of 7 cells starting at [firstDayOfWeek]; null = day outside the month. */
fun monthLayout(
    month: YearMonth,
    firstDayOfWeek: DayOfWeek,
): List<List<LocalDate?>> {
    val leading = (month.atDay(1).dayOfWeek.value - firstDayOfWeek.value + DAYS_IN_WEEK) % DAYS_IN_WEEK
    val cells = List<LocalDate?>(leading) { null } + (1..month.lengthOfMonth()).map(month::atDay)
    return cells.chunked(DAYS_IN_WEEK) { it + List(DAYS_IN_WEEK - it.size) { null } }
}
