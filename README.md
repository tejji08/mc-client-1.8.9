# mc-client-1.8.9

Privacy-first, telemetry-free Minecraft client. Standalone launcher + curated mod bundle, built on Legacy Fabric. MC 1.8.9 priority. See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

Status: milestone 1 **verified working** (2026-08-26) — launcher resolves 1.8.9 from Mojang's manifest, downloads + sha1-verifies the client jar/libraries/natives/assets, and launches straight to a live "Minecraft 1.8.9" window in offline/dev mode. No auth or mods yet.

## Building

Needs JDK 21 (Temurin, installed via winget). Gradle wrapper is checked in:

```
./gradlew :launcher:run
```

## Next up
- Generate the Gradle wrapper + do a real build/run once a JDK is installed
- Microsoft OAuth device-code flow (`auth/MicrosoftAuth.java` is stubbed)
- Legacy Fabric loader integration
- First utility mod
