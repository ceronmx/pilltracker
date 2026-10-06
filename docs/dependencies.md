# Dependencies

Every dependency needs a reason here. No networking, analytics or crash-reporting libraries (ADR 0001).

| Dependency | Why | Added in |
|---|---|---|
| Compose BOM + material3 | UI toolkit | j2a |
| activity-compose | `ComponentActivity` + `setContent` | j2a |
| JUnit 5 | Domain unit tests (CLAUDE.md) | j2a |
| Hilt (`hilt-android`, compiler) + KSP | Constructor injection, required by CLAUDE.md; KSP over kapt for build speed | c3v |
