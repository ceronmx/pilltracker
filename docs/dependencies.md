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
