# mc-client-1.8.9

Privacy-first, telemetry-free Minecraft client. Standalone launcher + curated mod bundle, built on Legacy Fabric. MC 1.8.9 priority. See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

Status: milestone 1 written — launcher downloads vanilla 1.8.9 from Mojang (sha1-verified) and launches it in offline/dev mode, no auth or mods yet. **Untested — no JDK on this machine yet.**

## Building

Needs JDK 17+ (not installed here as of 2026-08-26). No Gradle wrapper checked in yet — once a JDK is present, run `gradle wrapper` once to generate `gradlew`, then:

```
./gradlew :launcher:run
```

## Next up
- Generate the Gradle wrapper + do a real build/run once a JDK is installed
- Microsoft OAuth device-code flow (`auth/MicrosoftAuth.java` is stubbed)
- Legacy Fabric loader integration
- First utility mod
