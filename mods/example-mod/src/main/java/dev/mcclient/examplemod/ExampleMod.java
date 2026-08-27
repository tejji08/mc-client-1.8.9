package dev.mcclient.examplemod;

import net.fabricmc.api.ModInitializer;

/** Proof-of-life mod: if this line shows up in the log, mods/ is actually being scanned and loaded. */
public final class ExampleMod implements ModInitializer {
    @Override
    public void onInitialize() {
        System.out.println("[example-mod] Legacy Fabric mod loading works. Hello from mods/.");
    }
}
