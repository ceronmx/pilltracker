# ADR 0006: Exact reminder alarms via USE_EXACT_ALARM

- Status: Accepted
- Date: 2026-10-20
- Supersedes: [ADR 0004](0004-inexact-reminder-alarm.md); the alarm type in [ADR 0005](0005-repeat-reminders.md)

## Context
Reminders arrive too late for a pill that has to be taken daily: `setWindow` adds up to 10 minutes in use and waits for the Doze maintenance window (+1h measured) on an idle phone; `setAndAllowWhileIdle` repeats got a +1h window. The notification channel is already `IMPORTANCE_HIGH`, so the delay is the alarm. Setup must stay one tap, so a Settings trip for `SCHEDULE_EXACT_ALARM` is out.

## Decision
- Daily reminder and repeats use `AlarmManager.setExactAndAllowWhileIdle`.
- Declare `USE_EXACT_ALARM` (API 33+, granted at install, no prompt, can't be revoked) and `SCHEDULE_EXACT_ALARM` with `maxSdkVersion="32"` (API 31-32, granted by default). API 26-30 need no permission.
- If `canScheduleExactAlarms()` is false (user revoked it on API 31-32), fall back to the ADR 0004 `setWindow` instead of crashing.
- Rejected: `setAlarmClock`; it shows an alarm icon and the next alarm time in the status bar and on the lock screen.

## Consequences
- Setup is unchanged: one system dialog at most.
- Reminders fire at the chosen time, also in Doze. Exact allow-while-idle alarms are quota-limited in Doze, so repeats may be spaced a little further apart there (open question).
- Play restricts `USE_EXACT_ALARM` to apps whose core function is an alarm or calendar. A daily medication reminder is an alarm, but a Play listing would have to declare it; if refused, switch to `SCHEDULE_EXACT_ALARM` plus a Settings trip in setup, in a new ADR.
