# Open questions

Android behavior we are unsure about across OS versions (CLAUDE.md workflow rule 7).

- **Inexact alarm delay on real devices (ADR 0004).** Resolved on the API 36 emulator: `setWindow` = +10 min in use, deferred to the Doze maintenance window when idle; chose `setWindow`. Still unmeasured: older APIs (26-30) and OEM battery managers (Samsung, Xiaomi) that may kill or delay alarms further.
- **Notification permission on API 26-32.** Notifications are on by default there, but the user can switch them off in Settings without a runtime permission. Resolved: yes, Home shows the notice whenever they are off (AC2.4, checked on every resume).
- **Zone update vs. `TIMEZONE_CHANGED` delivery.** `DeviceClock` relies on Android clearing the process's cached default `TimeZone` when the zone changes, so `ZoneId.systemDefault()` follows travel. Unverified: whether that always happens before `ReminderReceiver` gets `TIMEZONE_CHANGED` in an already-running process; if not, that one reschedule uses the old zone until the next alarm. Check on an emulator by changing zone with the app open.
