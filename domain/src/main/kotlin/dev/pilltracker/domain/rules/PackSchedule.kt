package dev.pilltracker.domain.rules

import java.time.LocalDate
import java.time.temporal.ChronoUnit.DAYS

/** Days in a pack cycle, for both pack types. */
const val PACK_DAYS = 28
private const val ACTIVE_DAYS = 21

/** "21 + 7" = 21 active + 7 placebo pills; "21 pills" = 21 active pills, then 7 days without pills. */
enum class PackType { WITH_PLACEBO, WITH_BREAK }

enum class DayKind { ACTIVE, PLACEBO, BREAK }

/** Day of pill 1 of the current pack, from the pill number taken [today] (1-based). */
fun packStart(
    today: LocalDate,
    pillNumber: Int,
): LocalDate = today.minusDays(pillNumber - 1L)

/** Day 1-28 of the pack that [date] falls in; inverse of [packStart]. */
fun dayOfPack(
    start: LocalDate,
    date: LocalDate,
): Int = dayInCycle(start, date).toInt() + 1

private fun dayInCycle(
    start: LocalDate,
    date: LocalDate,
) = Math.floorMod(DAYS.between(start, date), PACK_DAYS.toLong())

/** What [date] is in a pack cycle starting at [start]; the cycle repeats every 28 days, both directions. */
fun dayKind(
    type: PackType,
    start: LocalDate,
    date: LocalDate,
): DayKind {
    val dayInCycle = dayInCycle(start, date)
    return when {
        dayInCycle < ACTIVE_DAYS -> DayKind.ACTIVE
        type == PackType.WITH_PLACEBO -> DayKind.PLACEBO
        else -> DayKind.BREAK
    }
}

/**
 * Calendar days from [lastTaken] to [today] if a pill day (not a break day) in between went unlogged (AC4.3);
 * null when nothing was ever logged or nothing was missed.
 */
fun daysSinceLastPill(
    type: PackType,
    start: LocalDate,
    lastTaken: LocalDate?,
    today: LocalDate,
): Int? {
    if (lastTaken == null) return null
    val missed =
        generateSequence(lastTaken.plusDays(1)) { it.plusDays(1) }
            .takeWhile { it < today }
            .any { dayKind(type, start, it) != DayKind.BREAK }
    return if (missed) DAYS.between(lastTaken, today).toInt() else null
}
