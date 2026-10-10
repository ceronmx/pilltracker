package dev.pilltracker.domain

/** Shows and clears the daily reminder notification; implemented by the platform layer. */
interface Notifier {
    /** Neutral wording, hidden on the lock screen (AC2.2). */
    fun showReminder()

    fun clearReminder()
}
