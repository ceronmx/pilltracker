# CLAUDE.md: Agent rules for PillTracker (Android)

Read this file fully before writing any code. Rules here override defaults.

## Product in one paragraph
A 100% local Android app that reminds one person to take a daily birth-control pill, lets her log it in one tap, and shows a month history. No accounts, no network, no analytics, no medical advice. Spec: `docs/spec.md`.

## Top priority: effortless setup and use
1. **Minimal taps.** First launch to a working daily reminder in as few taps as possible: one screen, sensible defaults (e.g. a preset time), one "Enable reminders" action that also handles the notification/exact-alarm permission. No accounts, no tutorial carousel, no multi-step wizard.
2. Logging a pill is one tap (from the notification action or the home screen). Any proposed flow that adds a tap or screen needs a stated reason.
3. When a design trade-off pits simplicity of setup/use against a feature, simplicity wins.

## Localization (required)
- The app ships in **English and Spanish**; **every** user-visible string (UI, notifications, permission rationales, errors, dates/times formatting, accessibility labels) lives in `values/strings.xml` (en, default) and `values-es/strings.xml`. No hardcoded text in Kotlin/Compose.
- Follow the system language; no in-app language picker (saves a setup step). Fall back to English.
- Use plurals/format args (`%1$s`) instead of string concatenation; format dates with the locale-aware `java.time` formatters.
- Neutral notification wording applies in both languages (e.g. "Time for your daily check-in" / "Es hora de tu registro diario").
- CI/`check` must fail if a string key exists in one locale and not the other (Android lint `MissingTranslation` as error).
- UI tests cover both locales for both screens (setup, home with month grid).

## Hard constraints (never violate)
1. **No network.** The manifest must NOT declare `android.permission.INTERNET` (or any network-state permission). Do not add networking libraries (OkHttp, Retrofit, Ktor, Firebase, analytics, crash reporters). CI enforces this.
2. **No cloud backup of data.** `android:allowBackup="false"` and data extraction rules exclude the database.
3. **No medical advice in the UI.** The app records and reminds; it never tells her what to do about a missed pill.
4. **Sensitive data stays off the lock screen.** Notification visibility is `SECRET`; wording is neutral (e.g. "Time for your daily check-in").

## Architecture (strict)
Layers, dependencies point inward only:

```
ui (Compose, ViewModels) -> domain (use cases, models, rules) <- data (Room, repositories impl)
                                      ^
                          platform (notifications, alarms, clock impl)
```

- `domain` is **pure Kotlin**: no `android.*`, no Room, no Compose imports. Enforced by a Gradle module boundary (`:domain` is a JVM module).
- `data` implements repository interfaces declared in `domain`.
- `platform` implements `ReminderScheduler`, `Notifier` interfaces declared in `domain`, and provides the `java.time.Clock` (`DeviceClock`, ADR 0002).
- ViewModels expose a single immutable `UiState` via `StateFlow` and accept `UiEvent`s (unidirectional data flow). No logic in composables.
- Dependency injection: Hilt. Constructor injection only.

## Time rules
- **Never call** `System.currentTimeMillis()`, `Instant.now()`, `LocalDate.now()` or `ZoneId.systemDefault()` outside the `Clock` implementation. Inject `Clock`.
- A "pill day" is a `LocalDate` in the user's current zone. Store the date plus the instant taken; do not derive the date from the instant later.
- Every time-dependent rule must have tests for: day boundary (23:59 to 00:00), DST change, time-zone change while traveling.

## Testing rules
- Domain: plain JUnit5 tests, written **before** the implementation. Target 100% branch coverage on `domain/rules`.
- Data: Room in-memory DB tests plus migration tests (`MigrationTestHelper`) for every schema bump.
- UI: a few Compose UI tests for the two screens (setup, home). No UI test for logic that belongs in the domain.
- Use fakes (`Clock.fixed`, `FakeReminderScheduler`, `InMemoryIntakeRepository`), not mocking frameworks, for our own interfaces.
- A task is not done until `./gradlew check` passes.

## Code style
- Kotlin, official style, ktlint + detekt must pass.
- Immutable data classes, sealed types for results/state; no `!!`, no `lateinit` outside DI/Android entry points.
- Names: `<Verb><Noun>UseCase`, `<Noun>Repository`, `<Screen>ViewModel`, `<Screen>UiState`.
- Public domain functions return `Result`-like sealed types for expected failures; do not throw for control flow.

## Workflow rules for the agent
1. Work from a spec section in `docs/spec.md`; if it is missing or ambiguous, ask or propose a spec edit first.
2. Tests first for domain logic. Show the failing test, then make it pass.
3. One concern per commit/PR; keep diffs small and reviewable.
4. Any decision that changes architecture, storage or permissions needs an ADR in `docs/adr/` (copy the template in ADR 0001's format).
5. Do not add dependencies without stating why in the PR description and updating `docs/dependencies.md`.
6. Before finishing: run `./gradlew check` and `scripts/check-no-network-permission.sh`, then summarize what changed and what is still unverified.
7. If you are unsure about Android notification/alarm behavior across OS versions, say so and add it to `docs/open-questions.md` rather than guessing.

## Out of scope (do not build)
Accounts, sync, cloud backup, analytics, medical guidance, widgets, wear support, multiple users.


<!-- BEGIN BEADS INTEGRATION v:1 profile:minimal hash:1105d646 -->
## Beads Issue Tracker

This project uses **bd (beads)** for issue tracking. Run `bd prime` to see full workflow context and commands.

### Quick Reference

```bash
bd ready              # Find available work
bd show <id>          # View issue details
bd update <id> --claim  # Claim work
bd close <id>         # Complete work
```

### Rules

- Use `bd` for ALL task tracking — do NOT use TodoWrite, TaskCreate, or markdown TODO lists
- Run `bd prime` for detailed command reference and session close protocol
- Use `bd remember` for persistent knowledge — do NOT use MEMORY.md files

**Architecture in one line:** issues live in a local Dolt DB; sync uses `refs/dolt/data` on your git remote; `.beads/issues.jsonl` is a passive export. See https://github.com/gastownhall/beads/blob/main/docs/core-concepts/sync-concepts.md for details and anti-patterns.

## Agent Context Profiles

The managed Beads block is task-tracking guidance, not permission to override repository, user, or orchestrator instructions.

- **Conservative (default)**: Use `bd` for task tracking. Do not run git commits, git pushes, or Dolt remote sync unless explicitly asked. At handoff, report changed files, validation, and suggested next commands.
- **Minimal**: Keep tool instruction files as pointers to `bd prime`; use the same conservative git policy unless active instructions say otherwise.
- **Team-maintainer**: Only when the repository explicitly opts in, agents may close beads, run quality gates, commit, and push as part of session close. A current "do not commit" or "do not push" instruction still wins.

## Session Completion

This protocol applies when ending a Beads implementation workflow. It is subordinate to explicit user, repository, and orchestrator instructions.

1. **File issues for remaining work** - Create beads for anything that needs follow-up
2. **Run quality gates** (if code changed) - Tests, linters, builds
3. **Update issue status** - Close finished work, update in-progress items
4. **Handle git/sync by active profile**:
   ```bash
   # Conservative/minimal/default: report status and proposed commands; wait for approval.
   git status

   # Team-maintainer opt-in only, unless current instructions forbid it:
   git pull --rebase
   git push
   git status
   ```
5. **Hand off** - Summarize changes, validation, issue status, and any blocked sync/commit/push step

**Critical rules:**
- Explicit user or orchestrator instructions override this Beads block.
- Do not commit or push without clear authority from the active profile or the current user request.
- If a required sync or push is blocked, stop and report the exact command and error.
<!-- END BEADS INTEGRATION -->
