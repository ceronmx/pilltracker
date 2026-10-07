# ADR 0002: Injected Clock

- Status: Accepted
- Date: 2026-10-06

## Context
Reminders and history depend on "today", which changes at midnight, across DST, and when the user travels. Direct system-time calls make this untestable.

## Decision
- Inject `java.time.Clock` (JDK, so usable in pure `:domain`) instead of a custom interface. Only `DeviceClock` (platform) may call `System.currentTimeMillis()`, `Instant.now()`, `LocalDate.now()` or `ZoneId.systemDefault()`; it re-reads the zone on every call so travel changes "today" (`Clock.systemDefaultZone()` would freeze it). CI greps for violations.
- A pill day is a `LocalDate` in the user's current zone, stored alongside the instant taken; never re-derived from the instant.
- Tests use `Clock.fixed(instant, zone)` as the fake and cover day boundary, DST change, and time-zone change.

## Consequences
- Slight indirection for every time read.
- Time rules are deterministic and unit-testable in pure JVM.

## Module layout
Start with only `:app` and `:domain` (JVM). Add `:data` and `:platform` with the first Room/alarm code, not before.

## Amendments
- 2026-10-06 (2fg): switched from a custom `Clock` interface + `FakeClock` to `java.time.Clock` + `Clock.fixed`; same guarantees, no extra types.
