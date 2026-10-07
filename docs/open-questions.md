# Open questions

Android behavior we are unsure about across OS versions (CLAUDE.md workflow rule 7).

- **Inexact alarm delay under Doze (ADR 0004).** `setWindow` alarms don't fire during Doze; `setAndAllowWhileIdle` does but gives no window guarantee. Which one keeps a 9:00 reminder within ~10 minutes on an idle phone, on API 26-36 and with battery saver / OEM battery managers? Measure on devices in bead 54c.
- **Notification permission on API 26-32.** Notifications are on by default there, but the user can switch them off in Settings without a runtime permission. Should Home show the denied notice if `NotificationManagerCompat.areNotificationsEnabled()` is false later?
