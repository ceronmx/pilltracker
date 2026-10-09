# PillTracker spec

A 100% local Android app: remind one person to take a daily pill, log it in one tap, show a month history. No accounts, network, analytics or medical advice. Rules for implementers: [CLAUDE.md](../CLAUDE.md).

## 1. Setup (minimal taps)
- AC1.1 First launch shows **one screen** with: pack type (**21 + 7**, preselected: 21 active + 7 placebo pills; or **21 pills**: 21 active pills, then 7 days without pills), which day of the pack is today (1-28 for both; for 21 pills, days 22-28 are labeled as break days, so setup works during the break week; default 1 = starting a new pack today), a preset reminder time (default 09:00) with a time picker, and one **Enable reminders** button. With the defaults, setup is one tap.
- AC1.2 That button also requests notification permission (API 33+) when needed; nothing else (no exact-alarm access, see ADR 0004). No tutorial, wizard, or account step.
- AC1.3 If notifications are denied, the screen says so neutrally and offers a retry (opens the app's notification settings; setup continues on its own once allowed) and "Continue without reminders"; the app stays usable for manual logging.
- AC1.4 Answers are saved; later launches open Home. The pack repeats every 28 days from the day of pill 1.

## 2. Reminder
- AC2.1 A notification fires daily within 10 minutes after the chosen time while the phone is in use; on an idle phone it may wait until the phone wakes (inexact alarm, ADR 0004), except on break days (21 pills, days 22-28) (local zone; re-scheduled after reboot, time-zone change, and DST change).
- AC2.2 Notification text is neutral ("Time for your daily check-in"), visibility `SECRET`.
- AC2.3 The notification has a **Taken** action: one tap logs today's pill, dismisses the notification, no app launch.
- AC2.4 Whenever notifications are not allowed (switched off after setup, or never allowed), Home shows the same neutral notice as setup (AC1.3) above the Taken button; its retry opens the app's notification settings. It disappears once notifications are allowed again. Logging keeps working either way.

## 3. Logging
- AC3.1 Home is the single main screen: today's **Taken** button above the month history (section 4), all visible at first glance. One **Taken** button for today. One tap logs it; logging again the same pill day is a no-op.
- AC3.2 A record stores the pill day (`LocalDate` in the zone at the time) and the instant taken. The date is never re-derived from the instant.
- AC3.3 Placebo days (21 + 7, days 22-28) work like active days; Home labels them neutrally as placebo.
- AC3.4 Break days (21 pills, days 22-28) show "Break day" instead of the Taken button.
- AC3.5 Once today is logged, a **Mark as not taken** text button under it removes today's record in one tap (undo for a wrong tap, from Home or the notification). No confirmation: Taken logs it again in one tap. Past days stay as recorded.

## 4. History (on Home)
- AC4.1 Month grid on Home, below today's Taken button, shows taken / not-taken / break per day (placebo days marked), never "not taken" for a break day, with previous/next month navigation; returning to the app shows the current month.
- AC4.2 Records only; no advice about missed pills.

## 5. Localization (en, es)
- AC5.1 Every user-visible string (UI, notifications, permission rationales, errors, accessibility labels) is in `values/strings.xml` (en, default) and `values-es/strings.xml`. No hardcoded text.
- AC5.2 Follows the system language; no in-app picker; falls back to English.
- AC5.3 Plurals and format args, never concatenation; dates/times use locale-aware `java.time` formatters.
- AC5.4 Neutral notification wording in both languages ("Time for your daily check-in" / "Es hora de tu registro diario").
- AC5.5 Lint `MissingTranslation` is an error; UI tests cover both locales for both screens (setup, home).

## 6. Privacy
- AC6.1 No `INTERNET` or network-state permission ([ADR 0001](adr/0001-local-only-no-network.md)).
- AC6.2 `allowBackup="false"`; data extraction rules exclude the database.

## 7. Settings
- AC7.1 A settings icon in the top corner of Home (accessibility label in en and es) opens a bottom sheet, not a new screen.
- AC7.2 The sheet shows the reminder time, editable with the same picker as setup; a new time replaces the scheduled reminder at once. It shows the pack type read-only (changing it mid-pack would shift past break days; use Reset instead). It shows today's day of the pack with the setup stepper; changing it realigns the pack (e.g. a pack started late) by moving only the pack start: intake records are kept, past placebo/break marks follow the new start.
- AC7.3 A **Reset** button asks for confirmation ("Delete all data?", Cancel / Delete). On Delete: all intakes and settings are deleted, the reminder alarm is cancelled, any shown notification is cleared, and the app returns to Setup with no way back to Home.
- AC7.4 Neutral wording; no advice.
- AC7.5 The sheet offers a choice of guinea pig (tricolor, preselected; ginger; black and white; cream), shown as pictures with accessible names in en and es. The choice changes every in-app guinea pig and the app's accent colors (buttons, highlights, calendar marks) at once; backgrounds stay shared and is kept with the settings (deleted by Reset). Purely cosmetic: launcher and notification icons stay fixed, and setup does not ask (AC1.1 stays one tap).

## Out of scope
Accounts, sync, cloud backup, analytics, medical guidance, widgets, wear, multiple users.
