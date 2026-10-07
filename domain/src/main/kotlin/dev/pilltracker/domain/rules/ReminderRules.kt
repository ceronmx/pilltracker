package dev.pilltracker.domain.rules

import dev.pilltracker.domain.Settings
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * Next instant the wall clock in [zone] reads [time], strictly after [now]. Computed one day at a time,
 * so DST and zone changes apply on the next scheduling; a time inside a DST gap moves forward by the gap.
 */
fun nextReminderAt(
    time: LocalTime,
    now: Instant,
    zone: ZoneId,
): Instant {
    val date = now.atZone(zone).toLocalDate()
    val today = ZonedDateTime.of(date, time, zone).toInstant()
    return if (today > now) today else ZonedDateTime.of(date.plusDays(1), time, zone).toInstant()
}

/** Remind on active and placebo days that aren't logged yet; never on break days (AC2.1, AC3.3). */
fun shouldRemind(
    settings: Settings,
    date: LocalDate,
    taken: Boolean,
): Boolean = !taken && dayKind(settings.packType, settings.packStart, date) != DayKind.BREAK
