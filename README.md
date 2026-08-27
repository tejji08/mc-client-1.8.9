# mc-client-1.8.9

Privacy-first, telemetry-free Minecraft client. Standalone launcher + curated mod bundle, built on Legacy Fabric. MC 1.8.9 priority. See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

Status: milestones 1–4 **verified working** (2026-08-27). Launcher resolves 1.8.9 from Mojang's manifest, downloads + sha1-verifies the client jar/libraries/natives/assets, and launches. Real Microsoft sign-in (device-code flow → Xbox Live → XSTS → Minecraft token → profile) is implemented and cached locally (`auth-cache.json`, never leaves the machine) — falls back to offline/dev mode until an Azure client ID is configured. Sign-in against a *real* account is currently blocked on Microsoft's side — see **Minecraft API approval** below. The game now boots through the **Legacy Fabric** loader (Knot client entrypoint) instead of vanilla's main class, and a proof-of-life mod in `mods/example-mod` confirms mods actually get scanned and initialized.

### Launcher GUI
`./gradlew :launcher:run` opens a windowed launcher (Play / Mods / Settings) — plain Swing, so the launcher keeps its zero-runtime-dependency property. Play streams the whole download/verify/launch pipeline into a log view; Mods lists the bundle with live verification badges; Settings holds heap size and window size. Pass `--cli` for the old headless path (what scripts and the approval checker use).

### Mod manifest + verification
The bundle is described by `launcher/src/main/resources/mods-manifest.json`, **shipped inside the jar** — never fetched from a server, because a remote config fetch is a phone-home by another name. Every entry pins a `sha256`; `ModEntry` refuses to construct without one, so an unpinned mod cannot exist in the catalog.

The invariant the design turns on: **a jar whose sha256 does not match the manifest never reaches the game directory.** It is checked on download and re-checked on every launch — "verified once, months ago" is not the same claim as "verified now". A tampered jar is reported as `Hash mismatch` in the GUI, blocked from the game folder, and offered a one-click repair.

Locally built jars from `mods/` cannot be pinned (they change every build), so they are self-hashed and flagged `Local build` rather than pretending to carry the bundle's guarantee. A user-supplied `%APPDATA%\mc-client-1.8.9\mods-manifest.json` overrides the bundled one.

### Minecraft API approval
Since 2022 every new Azure app must be manually allowlisted before `api.minecraftservices.com` will accept it — until then the final auth leg returns HTTP 403 `Invalid app registration`, no matter how correct the config is. Submit the app at <https://aka.ms/mce-reviewappid>. `./gradlew :launcher:checkApproval` probes the current state headlessly (exit 0 approved / 1 failed / 2 pending) using the cached refresh token, with no interactive prompt.

### The mods
Two real client mods ship in-tree, both built with Legacy Fabric's Loom toolchain and both Hypixel-legal (they reformat information the client already has; neither automates input nor reveals anything hidden):

- **`mods/keystrokes`** — WASD + mouse display with a rolling CPS counter. Config: `config/keystrokes.properties`.
- **`mods/bedwars-hud`** — condenses the Hypixel Bed Wars sidebar into one colour-coded row per team (bed up / players left / eliminated). Config: `config/bedwars-hud.properties`.

The Bed Wars scoreboard parser is deliberately free of Minecraft types so it can be unit-tested against real sidebar text (`./gradlew :mods:bedwars-hud:test`) — the only other place to exercise it is a live Hypixel game.

### Modding
Each subproject under `mods/` is a normal Fabric mod (`fabric.mod.json` + a `ModInitializer`), compiled against `net.fabricmc:fabric-loader`. `./gradlew :launcher:run` auto-copies every built `mods/*/build/libs/*.jar` into the game's `mods/` folder before each launch — build a mod, run the launcher, it's loaded. See `mods/example-mod` for the minimal shape.

### Enabling real sign-in
The launcher needs its own Azure AD app registration (public client, personal Microsoft accounts, device-code flow enabled) — see the instructions at the top of `auth/Config.java`. Once you have a client ID, either set env var `MC_CLIENT_AZURE_APP_ID` or drop it into `%APPDATA%\mc-client-1.8.9\azure-client-id.txt`.

## Building

Needs JDK 21 (Temurin, installed via winget). Gradle wrapper is checked in (9.7.1 — Legacy Fabric's Loom requires Gradle 9.4+):

```
./gradlew :launcher:run
```

## Next up
- Real end-to-end test of the Microsoft sign-in flow (blocked on the approval above)
- Performance-mod survey of the Legacy Fabric ecosystem before writing anything new
