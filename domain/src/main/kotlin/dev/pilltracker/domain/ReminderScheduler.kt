package dev.pilltracker.domain

import java.time.LocalTime

/** Schedules the next daily reminder at [time]; implemented by the platform layer. */
interface ReminderScheduler {
    fun scheduleNext(time: LocalTime)

    /** Drops the scheduled reminder and clears one already shown (AC7.3). */
    fun cancel()
}
