# ADR 0002: Injected Clock

- Status: Accepted
- Date: 2026-10-06

## Context
Reminders and history depend on "today", which changes at midnight, across DST, and when the user travels. Direct system-time calls make this untestable.

## Decision
- `domain` declares a `Clock` interface; only its platform implementation may call `System.currentTimeMillis()`, `Instant.now()`, `LocalDate.now()` or `ZoneId.systemDefault()`. CI greps for violations.
- A pill day is a `LocalDate` in the user's current zone, stored alongside the instant taken; never re-derived from the instant.
- Tests use `FakeClock` and cover day boundary, DST change, and time-zone change.

## Consequences
- Slight indirection for every time read.
- Time rules are deterministic and unit-testable in pure JVM.

## Module layout
Start with only `:app` and `:domain` (JVM). Add `:data` and `:platform` with the first Room/alarm code, not before.
