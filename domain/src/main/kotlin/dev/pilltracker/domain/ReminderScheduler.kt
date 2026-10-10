package dev.pilltracker.domain

import java.time.Instant
import java.time.LocalTime

/** Schedules the next daily reminder at [time]; implemented by the platform layer. */
interface ReminderScheduler {
    fun scheduleNext(time: LocalTime)

    /** One pending repeat of an unlogged reminder (ADR 0005); a new one replaces it. */
    fun scheduleRepeat(at: Instant)

    fun cancelRepeat()

    /** Drops the scheduled reminder and any repeat, and clears one already shown (AC7.3). */
    fun cancel()
}
