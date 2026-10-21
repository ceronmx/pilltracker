# ADR 0004: Inexact reminder alarm, notification permission only

- Status: Superseded by [ADR 0006](0006-exact-reminder-alarm.md)
- Date: 2026-10-06

## Context
Exact alarms need `SCHEDULE_EXACT_ALARM`, which is off by default on Android 14+ for new installs; granting it sends the user to a system Settings screen and back. `USE_EXACT_ALARM` is auto-granted but Play restricts it to alarm-clock and calendar apps. Setup simplicity is the top product priority (CLAUDE.md).

## Decision
- The daily reminder uses `AlarmManager.setWindow` with a 10 minute window after the chosen time. No exact-alarm permission is declared or requested.
- Setup requests only `POST_NOTIFICATIONS` (API 33+). Retry after a denial opens the app's notification settings, which works even after the system stops showing the dialog.

## Consequences
- Setup is one system dialog at most.
- Reminders arrive within 10 minutes while the phone is in use. On an idle phone in deep Doze, `setWindow` alarms wait for the next maintenance window (measured +1h on API 36, growing with idle time) and fire at the latest when the screen turns on.
- Rejected: `setAndAllowWhileIdle` fires in Doze but the system gave it a +1h window (measured on API 36), so it is late even when the phone is in use.
- Revisit with a new ADR (exact alarm + Settings trip) if late reminders on idle phones prove a problem.

## Measurements (2026-10-06, Pixel 9 Pro emulator, API 36, `dumpsys alarm`)
| Call | Normal | `deviceidle force-idle` |
|---|---|---|
| `setAndAllowWhileIdle` | window=+1h | fires (allow-while-idle) |
| `setWindow(10 min)` | window=+10m | device_idle=+59m (next maintenance window) |

## Measurements (2026-10-09, same emulator): zone change with the app running
`cmd alarm set-timezone` while the app process stayed alive rescheduled the 09:00 alarm to 09:00 in each new zone (Mexico City -> Tokyo -> Los Angeles). No change to the decision. API 26-30 and OEM battery managers are still unmeasured (bead vix).
