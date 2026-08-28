package dev.mcclient.mixin;

import dev.mcclient.McClientMods;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Draws the HUD modules after the vanilla HUD, so they sit on top of it. */
@Mixin(InGameHud.class)
public class InGameHudMixin {

    @Inject(method = "render(F)V", at = @At("TAIL"))
    private void mcclient$renderModules(float tickDelta, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null || client.options == null) {
            return;
        }
        // Respect the player's own "hide HUD" (F1) choice rather than drawing over a clean screen.
        if (client.options.hudHidden) {
            return;
        }
        McClientMods.ensureReady();
        McClientMods.drawHuds(client, false);
    }
}
