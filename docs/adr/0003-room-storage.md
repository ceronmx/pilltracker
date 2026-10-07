# ADR 0003: Room for local storage

- Status: Accepted
- Date: 2026-10-06

## Context
Intakes (and next, setup answers) must survive restarts, stay on the device, and evolve with schema migrations that are tested (CLAUDE.md testing rules).

## Decision
- One Room database in a new `:data` Android library module, implementing repository interfaces declared in `:domain` (`IntakeRepository`).
- An intake row stores the pill day as `LocalDate` (epoch day, primary key) and the instant taken (epoch millis). The pill day is never re-derived from the instant (AC3.2). The primary key makes a second log on the same pill day a no-op (AC3.1).
- Schemas are exported to `data/schemas/` so every bump gets a `MigrationTestHelper` test.
- Setup answers will go in the same database (one settings row), not DataStore: one storage technology, one backup exclusion, one migration story.
- Backup exclusion already covers `database` (ADR 0001, `data_extraction_rules.xml`).

## Consequences
- Repositories expose `Flow`, so `:domain` depends on `kotlinx-coroutines-core` (pure Kotlin, no Android).
- DAO tests are instrumented (in-memory Room on a device/emulator); `./gradlew check` compiles them but does not run them.
