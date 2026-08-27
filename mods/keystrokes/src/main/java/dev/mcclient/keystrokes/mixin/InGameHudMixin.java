package dev.mcclient.keystrokes.mixin;

import dev.mcclient.keystrokes.KeystrokesHud;
import dev.mcclient.keystrokes.KeystrokesMod;
import net.minecraft.client.gui.hud.InGameHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Draws the keystroke overlay after the vanilla HUD, so it sits on top of it. */
@Mixin(InGameHud.class)
public class InGameHudMixin {

    @Inject(method = "render(F)V", at = @At("TAIL"))
    private void mcclient$renderKeystrokes(float tickDelta, CallbackInfo ci) {
        KeystrokesHud hud = KeystrokesMod.hud();
        if (hud != null) {
            hud.render();
        }
    }
}
