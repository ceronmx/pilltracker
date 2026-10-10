package dev.pilltracker.domain.rules

import dev.pilltracker.domain.Settings
import java.time.Duration
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

private const val REPEAT_MINUTES = 5L
private val REPEAT_INTERVAL: Duration = Duration.ofMinutes(REPEAT_MINUTES)
private val REPEAT_WINDOW: Duration = Duration.ofHours(1)

/** Today's [time] in [zone]; a time inside a DST gap moves forward by the gap. */
private fun dueToday(
    time: LocalTime,
    now: Instant,
    zone: ZoneId,
): Instant = ZonedDateTime.of(now.atZone(zone).toLocalDate(), time, zone).toInstant()

/** [now] lies in the real hour after today's reminder [time]: a repeat firing now may notify (ADR 0005). */
fun inRepeatWindow(
    time: LocalTime,
    now: Instant,
    zone: ZoneId,
): Boolean {
    val due = dueToday(time, now, zone)
    return now >= due && now <= due + REPEAT_WINDOW
}

/**
 * Next repeat of an unlogged reminder, 5 minutes after [now], or null to stop: logged, break day, before the
 * reminder time, past its hour (so at most 12 repeats, AC2.1), or past midnight (the next pill day).
 */
fun nextRepeatAt(
    settings: Settings,
    now: Instant,
    zone: ZoneId,
    taken: Boolean,
): Instant? {
    val next = now + REPEAT_INTERVAL
    val today = now.atZone(zone).toLocalDate()
    val due = shouldRemind(settings, today, taken) && inRepeatWindow(settings.reminderTime, now, zone)
    // next's own day decides its window, so a repeat after midnight falls before tomorrow's reminder: stop.
    return next.takeIf { due && inRepeatWindow(settings.reminderTime, next, zone) }
}

/** Whole real hours since today's reminder [time] (AC2.2, AC3.6); null until the first full hour. */
fun hoursLate(
    time: LocalTime,
    now: Instant,
    zone: ZoneId,
): Int? = Duration.between(dueToday(time, now, zone), now).toHours().toInt().takeIf { it >= 1 }
