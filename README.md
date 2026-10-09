# PillTracker

A private, fully offline Android app that reminds you to take your daily pill and keeps a simple history.

**Privacy by construction:** the app declares no network permissions, disables cloud backup, and contains no analytics or third-party network SDKs. CI fails the build if that ever changes. See [ADR 0001](docs/adr/0001-local-only-no-network.md).

## Status
MVP: setup, daily reminder with a Taken action, one-tap logging with the month grid on Home, and a settings sheet (reminder time, pack realignment, delete all data). English and Spanish.

## Docs
- [Spec](docs/spec.md): features and acceptance criteria
- [CLAUDE.md](CLAUDE.md): rules the coding agent follows
- [ADRs](docs/adr/): architecture decisions
- [Open questions](docs/open-questions.md)

## Architecture (planned)
```
:app (Compose, ViewModels, Hilt)  ->  :domain (pure Kotlin rules/use cases)  <-  :data (Room, DataStore)
                                              ^
                                       :platform (AlarmManager, notifications, Clock)
```

## Release build
The signing key never goes in git. Put it in `~/.gradle/gradle.properties`:
```
pilltracker.storeFile=/absolute/path/to/release.jks
pilltracker.storePassword=...
pilltracker.keyAlias=...
pilltracker.keyPassword=...
```
Then `./gradlew assembleRelease` gives a signed, R8-shrunk `app/build/outputs/apk/release/app-release.apk`. Without the properties it builds unsigned. Keep a backup of the keystore: updates must be signed with the same key.

## Verify the privacy claim
```
bash scripts/check-no-network-permission.sh .
```
After building, inspect the merged manifest or run `aapt dump permissions app-release.apk`: no `INTERNET`.
