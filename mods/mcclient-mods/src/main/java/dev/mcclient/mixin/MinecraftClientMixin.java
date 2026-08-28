package dev.mcclient.mixin;

import dev.mcclient.McClientMods;
import dev.mcclient.core.KeybindSetting;
import dev.mcclient.core.Module;
import dev.mcclient.core.ModuleRegistry;
import dev.mcclient.menu.HudEditorScreen;
import dev.mcclient.menu.ModMenuScreen;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Drives the client-wide keys and the tick-based modules. */
@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {

    /** Keys held on the previous tick, so a binding fires once per press instead of every tick. */
    private final Set<Integer> mcclient$heldLastTick = new HashSet<Integer>();

    @Inject(method = "tick()V", at = @At("TAIL"))
    private void mcclient$tickModules(CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return;
        }
        McClientMods.ensureReady();

        if (client.currentScreen == null) {
            if (pressed(McClientMods.CLIENT.menuKey())) {
                client.setScreen(new ModMenuScreen());
            } else if (pressed(McClientMods.CLIENT.editorKey())) {
                client.setScreen(new HudEditorScreen());
            } else {
                List<Module> modules = ModuleRegistry.modules();
                for (int i = 0; i < modules.size(); i++) {
                    Module module = modules.get(i);
                    if (module.canDisable() && pressed(module.toggleKey())) {
                        module.setEnabled(!module.isEnabled());
                        ModuleRegistry.save();
                    }
                }
            }
        }
        // Track held state even with a screen open, so closing one cannot fire a stale press.
        refreshHeld();

        if (McClientMods.ZOOM.isEnabled()) {
            McClientMods.ZOOM.tick(client);
        }
        if (McClientMods.FULLBRIGHT.isEnabled()) {
            McClientMods.FULLBRIGHT.tick(client);
        }
    }

    private boolean pressed(KeybindSetting binding) {
        if (!binding.isBound()) {
            return false;
        }
        return binding.isDown() && !mcclient$heldLastTick.contains(Integer.valueOf(binding.keyCode()));
    }

    private void refreshHeld() {
        mcclient$heldLastTick.clear();
        record(McClientMods.CLIENT.menuKey());
        record(McClientMods.CLIENT.editorKey());
        List<Module> modules = ModuleRegistry.modules();
        for (int i = 0; i < modules.size(); i++) {
            record(modules.get(i).toggleKey());
        }
    }

    private void record(KeybindSetting binding) {
        if (binding.isBound() && binding.isDown()) {
            mcclient$heldLastTick.add(Integer.valueOf(binding.keyCode()));
        }
    }
}
