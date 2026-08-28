package dev.mcclient.mixin;

import dev.mcclient.McClientMods;
import dev.mcclient.menu.ModMenuScreen;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Opens the mod menu on Right Shift and keeps tick-driven modules asserted. */
@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {

    private boolean mcclient$menuKeyWasDown;

    @Inject(method = "tick()V", at = @At("TAIL"))
    private void mcclient$tickModules(CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return;
        }
        McClientMods.ensureReady();

        // Edge-triggered: hold-to-repeat would reopen the screen the moment it closed.
        boolean down = Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
        if (down && !mcclient$menuKeyWasDown && client.currentScreen == null) {
            client.setScreen(new ModMenuScreen());
        }
        mcclient$menuKeyWasDown = down;

        if (McClientMods.FULLBRIGHT.isEnabled()) {
            McClientMods.FULLBRIGHT.tick(client);
        }
    }
}
