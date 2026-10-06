# PillTracker spec

A 100% local Android app: remind one person to take a daily pill, log it in one tap, show a month history. No accounts, network, analytics or medical advice. Rules for implementers: [CLAUDE.md](../CLAUDE.md).

## 1. Setup (minimal taps)
- AC1.1 First launch shows **one screen** with a preset reminder time (default 09:00), a time picker, and one **Enable reminders** button.
- AC1.2 That button also requests notification permission (API 33+) and exact-alarm access (API 31+) as needed. No tutorial, wizard, or account step.
- AC1.3 If a permission is denied, the screen says so neutrally and offers a retry; the app stays usable for manual logging.

## 2. Reminder
- AC2.1 A notification fires daily at the chosen time (local zone; re-scheduled after reboot, time-zone change, and DST change).
- AC2.2 Notification text is neutral ("Time for your daily check-in"), visibility `SECRET`.
- AC2.3 The notification has a **Taken** action: one tap logs today's pill, dismisses the notification, no app launch.

## 3. Logging
- AC3.1 Home screen has one **Taken** button for today. One tap logs it; logging again the same pill day is a no-op.
- AC3.2 A record stores the pill day (`LocalDate` in the zone at the time) and the instant taken. The date is never re-derived from the instant.

## 4. History
- AC4.1 Month grid shows taken / not-taken per day, with previous/next month navigation.
- AC4.2 Records only; no advice about missed pills.

## 5. Localization (en, es)
- AC5.1 Every user-visible string (UI, notifications, permission rationales, errors, accessibility labels) is in `values/strings.xml` (en, default) and `values-es/strings.xml`. No hardcoded text.
- AC5.2 Follows the system language; no in-app picker; falls back to English.
- AC5.3 Plurals and format args, never concatenation; dates/times use locale-aware `java.time` formatters.
- AC5.4 Neutral notification wording in both languages ("Time for your daily check-in" / "Es hora de tu registro diario").
- AC5.5 Lint `MissingTranslation` is an error; UI tests cover both locales for the three screens (setup, home, history).

## 6. Privacy
- AC6.1 No `INTERNET` or network-state permission ([ADR 0001](adr/0001-local-only-no-network.md)).
- AC6.2 `allowBackup="false"`; data extraction rules exclude the database.

## Out of scope
Accounts, sync, cloud backup, analytics, medical guidance, widgets, wear, multiple users.
