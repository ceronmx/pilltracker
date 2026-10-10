# ADR 0005: Repeat reminders every 5 minutes, guinea pig sound

- Status: Accepted
- Date: 2026-10-09
- Supersedes: [ADR 0004](0004-inexact-reminder-alarm.md) for the repeats only; the daily alarm stays `setWindow`.

## Context
One daily notification is easy to swipe away and forget. She asked for repeats until the pill is logged, a guinea pig prefix and sound, and how late she is. Setup must stay one tap: no exact-alarm permission (ADR 0004).

## Decision
- After the daily reminder fires on an unlogged pill day, schedule a repeat 5 minutes later with `AlarmManager.setAndAllowWhileIdle`. Each repeat re-posts the same notification and schedules the next. No new permission.
- Cap: 12 repeats (about an hour). Stop when today is logged (notification Taken or Home), on break days, and once the pill day changes (midnight). **Mark as not taken** cancels any pending repeat and does not restart the chain.
- The chain survives reboot, app update and time-zone change: the existing reschedule path restarts it while today is still unlogged and the cap is not reached.
- Notification title: "Squeak! Squeak!" / "¡Cuy! ¡Cuy!" before the neutral text. Hours late are whole hours since today's reminder time, shown from the first full hour.
- Sound: a short guinea pig clip from `res/raw` on a new notification channel (a channel's sound can't change after creation), on every notification including repeats.

## Consequences
- In use, a repeat arrives within its inexact window after 5 minutes. On an idle phone in Doze, allow-while-idle alarms are rate-limited and were given a +1h window on API 36 (ADR 0004 table), so repeats on an idle phone are not a clean 5 minutes; they may slip by up to about an hour.
- The sound is audible to bystanders. The title and sound are playful, not medical; the lock screen still shows nothing (`SECRET`). She can silence or change the sound in the system channel settings.
- Up to 13 notifications a day on unlogged days; fewer on logged days.
- Revisit with a new ADR if repeats prove too late on idle phones (exact alarms) or too noisy (first-only sound).
