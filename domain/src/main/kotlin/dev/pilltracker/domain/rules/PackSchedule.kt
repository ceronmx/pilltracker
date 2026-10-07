package dev.pilltracker.domain.rules

import java.time.LocalDate
import java.time.temporal.ChronoUnit.DAYS

private const val CYCLE_DAYS = 28
private const val ACTIVE_DAYS = 21

/** 28 = 21 active + 7 placebo pills; 21+7 = 21 active pills + 7 days without pills. */
enum class PackType { TWENTY_EIGHT, TWENTY_ONE_PLUS_SEVEN }

enum class DayKind { ACTIVE, PLACEBO, BREAK }

/** Day of pill 1 of the current pack, from the pill number taken [today] (1-based). */
fun packStart(
    today: LocalDate,
    pillNumber: Int,
): LocalDate = today.minusDays(pillNumber - 1L)

/** What [date] is in a pack cycle starting at [start]; the cycle repeats every 28 days, both directions. */
fun dayKind(
    type: PackType,
    start: LocalDate,
    date: LocalDate,
): DayKind {
    val dayInCycle = Math.floorMod(DAYS.between(start, date), CYCLE_DAYS.toLong())
    return when {
        dayInCycle < ACTIVE_DAYS -> DayKind.ACTIVE
        type == PackType.TWENTY_EIGHT -> DayKind.PLACEBO
        else -> DayKind.BREAK
    }
}
