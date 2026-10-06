# ADR 0001: Local-only, no network

- Status: Accepted
- Date: 2026-10-06

## Context
The app stores sensitive health-adjacent data (pill intake). Any network path creates a leak surface and a trust question.

## Decision
- The manifest declares no `INTERNET` or network-state permission.
- No networking, analytics, crash-reporting or Firebase libraries.
- `android:allowBackup="false"`; data extraction rules exclude the database.
- CI runs `scripts/check-no-network-permission.sh` on sources and on the merged release manifest.

## Consequences
- No sync, accounts, or remote config; losing the device loses the history.
- Anyone can verify the claim with the script or `aapt dump permissions`.
- Changing this requires a new ADR superseding this one.
