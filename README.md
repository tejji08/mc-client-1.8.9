# mc-client-1.8.9

Privacy-first, telemetry-free Minecraft client. Standalone launcher + curated mod bundle, built on Legacy Fabric. MC 1.8.9 priority. See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

Status: milestones 1–3 **verified working** (2026-08-26). Launcher resolves 1.8.9 from Mojang's manifest, downloads + sha1-verifies the client jar/libraries/natives/assets, and launches. Real Microsoft sign-in (device-code flow → Xbox Live → XSTS → Minecraft token → profile) is implemented and cached locally (`auth-cache.json`, never leaves the machine) — falls back to offline/dev mode until an Azure client ID is configured. The game now boots through the **Legacy Fabric** loader (Knot client entrypoint) instead of vanilla's main class, and a proof-of-life mod in `mods/example-mod` confirms mods actually get scanned and initialized.

### Modding
Each subproject under `mods/` is a normal Fabric mod (`fabric.mod.json` + a `ModInitializer`), compiled against `net.fabricmc:fabric-loader`. `./gradlew :launcher:run` auto-copies every built `mods/*/build/libs/*.jar` into the game's `mods/` folder before each launch — build a mod, run the launcher, it's loaded. See `mods/example-mod` for the minimal shape.

### Enabling real sign-in
The launcher needs its own Azure AD app registration (public client, personal Microsoft accounts, device-code flow enabled) — see the instructions at the top of `auth/Config.java`. Once you have a client ID, either set env var `MC_CLIENT_AZURE_APP_ID` or drop it into `%APPDATA%\mc-client-1.8.9\azure-client-id.txt`.

## Building

Needs JDK 21 (Temurin, installed via winget). Gradle wrapper is checked in:

```
./gradlew :launcher:run
```

## Next up
- First *real* utility mod (example-mod is just a log line)
- Mod manifest/verification system
- Real end-to-end test of the Microsoft sign-in flow (needs an Azure client ID from you)
- Performance-mod survey of the Legacy Fabric ecosystem before writing anything new
