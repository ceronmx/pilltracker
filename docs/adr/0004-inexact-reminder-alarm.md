# ADR 0004: Inexact reminder alarm, notification permission only

- Status: Accepted
- Date: 2026-10-06

## Context
Exact alarms need `SCHEDULE_EXACT_ALARM`, which is off by default on Android 14+ for new installs; granting it sends the user to a system Settings screen and back. `USE_EXACT_ALARM` is auto-granted but Play restricts it to alarm-clock and calendar apps. Setup simplicity is the top product priority (CLAUDE.md).

## Decision
- The daily reminder uses an inexact alarm with a ~10 minute window after the chosen time. No exact-alarm permission is declared or requested.
- Setup requests only `POST_NOTIFICATIONS` (API 33+). Retry after a denial opens the app's notification settings, which works even after the system stops showing the dialog.

## Consequences
- Setup is one system dialog at most.
- Reminders can arrive a few minutes late. How late under Doze/battery saver is an open question (docs/open-questions.md); revisit with a new ADR if it proves too late.
