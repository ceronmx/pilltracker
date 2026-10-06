# PillTracker

A private, fully offline Android app that reminds you to take your daily pill and keeps a simple history.

**Privacy by construction:** the app declares no network permissions, disables cloud backup, and contains no analytics or third-party network SDKs. CI fails the build if that ever changes. See [ADR 0001](docs/adr/0001-local-only-no-network.md).

## Status
Foundation phase: spec, architecture decisions and agent guardrails are in place; implementation follows the milestones in [docs/agent-prompts.md](docs/agent-prompts.md).

## Docs
- [Spec](docs/spec.md): features and acceptance criteria
- [CLAUDE.md](CLAUDE.md): rules the coding agent follows
- [ADRs](docs/adr/): architecture decisions
- [Agent workflow](docs/agent-workflow.md): how agents were used, and where they were wrong
- [Open questions](docs/open-questions.md)

## Architecture (planned)
```
:app (Compose, ViewModels, Hilt)  ->  :domain (pure Kotlin rules/use cases)  <-  :data (Room, DataStore)
                                              ^
                                       :platform (AlarmManager, notifications, Clock)
```

## Verify the privacy claim
```
bash scripts/check-no-network-permission.sh .
```
After building, inspect the merged manifest or run `aapt dump permissions app-release.apk`: no `INTERNET`.
