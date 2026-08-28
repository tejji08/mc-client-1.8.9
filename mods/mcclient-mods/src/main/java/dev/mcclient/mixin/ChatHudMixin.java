package dev.mcclient.mixin;

import dev.mcclient.McClientMods;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Feeds chat to the match-stats module. Read-only: it observes messages the client already
 * received and is about to draw, and never sends or suppresses anything.
 */
@Mixin(ChatHud.class)
public class ChatHudMixin {

    @Inject(method = "addMessage(Lnet/minecraft/text/Text;)V", at = @At("HEAD"))
    private void mcclient$watchChat(Text message, CallbackInfo ci) {
        if (message == null || !McClientMods.MATCH_STATS.isEnabled()) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) {
            return;
        }
        McClientMods.MATCH_STATS.onChatLine(message.asUnformattedString(), client.player.getGameProfile().getName());
    }
}
