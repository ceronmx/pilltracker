# Open questions

Android behavior we are unsure about across OS versions (CLAUDE.md workflow rule 7).

- **Inexact alarm delay on real devices (ADR 0004).** Resolved on the API 36 emulator: `setWindow` = +10 min in use, deferred to the Doze maintenance window when idle; chose `setWindow`. Still unmeasured: older APIs (26-30) and OEM battery managers (Samsung, Xiaomi) that may kill or delay alarms further.
- **Notification permission on API 26-32.** Notifications are on by default there, but the user can switch them off in Settings without a runtime permission. Should Home show the denied notice if `NotificationManagerCompat.areNotificationsEnabled()` is false later?
