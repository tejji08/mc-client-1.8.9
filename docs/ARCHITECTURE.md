# mc-client-1.8.9 — architecture

Privacy-first, telemetry-free alternative to Lunar Client. MC 1.8.9 first (Hypixel/PvP version), open-source top to bottom.

## Why not build the mod loader from scratch

1.8.9 predates modern Fabric. Rather than hand-roll a LaunchWrapper + Mixin tweaker (what Lunar/Badlion/old cheat clients did), use **Legacy Fabric** (legacyfabric.net) — a community backport of Fabric Loader + Mixin to 1.7–1.13. This gives us a maintained, auditable mod-loading layer for free and lets us write mods as normal Fabric mods.

## Three components

1. **Loader**: Legacy Fabric Loader (unmodified, pinned version) — handles jar patching, mixin injection, mod discovery.
2. **Launcher** (`launcher/`): our own thin Java app.
   - Microsoft OAuth device-code flow for auth (no third-party auth relay — talk to Mojang/Xbox APIs directly)
   - Downloads vanilla 1.8.9 client jar + libraries + assets from Mojang's own manifest
   - Installs/pins Legacy Fabric loader
   - Launches with our curated mod set from `mods/`
   - **Zero telemetry**: no analytics SDK, no phone-home, no remote config fetch. Logs stay local.
3. **Mods** (`mods/`): curated bundle, each either written by us or a vetted open-source jar (license + source link recorded in `mods/MANIFEST.md`).
   - Performance: chunk/render optimizations compatible with 1.8.9's renderer (evaluate what Legacy Fabric ecosystem already has before writing new)
   - Utility: minimap, keystrokes, HUD, FPS counter — no cosmetics backend for v1 (that needs a server; skip until core is solid)

## Non-goals for v1

- No cosmetics/cape system (requires a backend + accounts — later, if at all)
- No anti-cheat-adjacent "cheat" modules (reach, killaura, etc.) — utility only
- No multi-version support yet — get 1.8.9 solid first

## Build order

1. Launcher: auth + vanilla download + Legacy Fabric install + plain launch (no mods yet) — prove the pipeline
2. Mod manifest system: launcher fetches/verifies pinned mod jars, drops into `mods/`
3. First utility mod (keystrokes or FPS overlay) written against Legacy Fabric to validate the mod-dev loop
4. Performance pass: survey existing Legacy Fabric perf mods before writing our own
