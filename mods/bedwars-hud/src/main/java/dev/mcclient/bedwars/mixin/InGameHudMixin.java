package dev.mcclient.bedwars.mixin;

import dev.mcclient.bedwars.BedwarsHud;
import dev.mcclient.bedwars.BedwarsMod;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Draws the team panel after the vanilla HUD, so it sits on top of it. */
@Mixin(InGameHud.class)
public class InGameHudMixin {

    @Inject(method = "render(F)V", at = @At("TAIL"))
    private void mcclient$renderBedwarsPanel(float tickDelta, CallbackInfo ci) {
        BedwarsHud hud = BedwarsMod.hud();
        if (hud != null) {
            hud.render();
        }
    }
}
