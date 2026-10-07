# PillTracker spec

A 100% local Android app: remind one person to take a daily pill, log it in one tap, show a month history. No accounts, network, analytics or medical advice. Rules for implementers: [CLAUDE.md](../CLAUDE.md).

## 1. Setup (minimal taps)
- AC1.1 First launch shows **one screen** with: pack type (**28**, preselected: 21 active + 7 placebo pills; or **21+7**: 21 active pills, then 7 days without pills), which pill of the pack is today (1-28 or 1-21, default 1 = starting a new pack today), a preset reminder time (default 09:00) with a time picker, and one **Enable reminders** button. With the defaults, setup is one tap.
- AC1.2 That button also requests notification permission (API 33+) when needed; nothing else (no exact-alarm access, see ADR 0004). No tutorial, wizard, or account step.
- AC1.3 If notifications are denied, the screen says so neutrally and offers a retry (opens the app's notification settings; setup continues on its own once allowed) and "Continue without reminders"; the app stays usable for manual logging.
- AC1.4 Answers are saved; later launches open Home. The pack repeats every 28 days from the day of pill 1.

## 2. Reminder
- AC2.1 A notification fires daily within 10 minutes after the chosen time while the phone is in use; on an idle phone it may wait until the phone wakes (inexact alarm, ADR 0004), except on break days (21+7, days 22-28) (local zone; re-scheduled after reboot, time-zone change, and DST change).
- AC2.2 Notification text is neutral ("Time for your daily check-in"), visibility `SECRET`.
- AC2.3 The notification has a **Taken** action: one tap logs today's pill, dismisses the notification, no app launch.

## 3. Logging
- AC3.1 Home is the single main screen: today's **Taken** button above the month history (section 4), all visible at first glance. One **Taken** button for today. One tap logs it; logging again the same pill day is a no-op.
- AC3.2 A record stores the pill day (`LocalDate` in the zone at the time) and the instant taken. The date is never re-derived from the instant.
- AC3.3 Placebo days (28, days 22-28) work like active days; Home labels them neutrally as placebo.
- AC3.4 Break days (21+7, days 22-28) show "Break day" instead of the Taken button.

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

## Out of scope
Accounts, sync, cloud backup, analytics, medical guidance, widgets, wear, multiple users.
