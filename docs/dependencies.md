# Dependencies

Every dependency needs a reason here. No networking, analytics or crash-reporting libraries (ADR 0001).

| Dependency | Why | Added in |
|---|---|---|
| Compose BOM + material3 | UI toolkit | j2a |
| activity-compose | `ComponentActivity` + `setContent` | j2a |
| JUnit 5 | Domain unit tests (CLAUDE.md) | j2a |
| Hilt (`hilt-android`, compiler) + KSP | Constructor injection, required by CLAUDE.md; KSP over kapt for build speed | c3v |
| ktlint (jlleitschuh gradle plugin) | Code style gate in `check` (CLAUDE.md) | qpi |
| detekt | Static analysis gate in `check` (CLAUDE.md) | qpi |
| Compose `ui-test-junit4` + `ui-test-manifest` (debug), `androidx.test:runner` | Instrumented Compose UI tests; `DeviceConfigurationOverride.Locales` for en/es coverage (AC5.5). Test/debug only, not in release | hex |
| Compose `ui-tooling-preview` (+ `ui-tooling`, debug only) | `@Preview` for every component state and screen, light/dark × en/es | 7bb |
| `navigation-compose` | Screen navigation (Setup → Home) with back stack and saved state; chosen by the user over hand-rolled state | 2fg |
| `hilt-navigation-compose` | `hiltViewModel()` scoped to each nav destination | 2fg |
| Room (`room-runtime`, `room-ktx`, compiler via KSP, `room-testing`) + `androidx.room` Gradle plugin | Local storage with exported schemas for migration tests (ADR 0003) | r5w |
| `kotlinx-coroutines-core` (+ `-test`) | `Flow` in domain repository interfaces; `setMain` in ViewModel tests | r5w |
| `androidx.test:rules` | `GrantPermissionRule` for `POST_NOTIFICATIONS` in instrumented tests (test only) | gcc |

## Supply-chain verification
- `gradle/verification-metadata.xml` pins a SHA-256 for every resolved artifact (including build plugins). A tampered or swapped dependency fails the build. Regenerate after any dependency change: `./gradlew --write-verification-metadata sha256 check assembleDebug assembleRelease assembleDebugAndroidTest`, then review the diff before committing.
- `distributionSha256Sum` in `gradle-wrapper.properties` pins the Gradle distribution.
- Limit: hashes are trust-on-first-use. They stop later tampering, not a malicious artifact present when first recorded.
