package dev.mcclient.keystrokes;

import com.mojang.blaze3d.platform.GlStateManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;

/**
 * Draws the WASD / mouse key display. Everything here is read from the local client's own input
 * state -- nothing is queried from the server and nothing is sent to it.
 */
public final class KeystrokesHud {

    private static final int KEY = 22;
    private static final int GAP = 2;

    private static final int BG_IDLE = 0x80000000;
    private static final int BG_HELD = 0xC0FFFFFF;
    private static final int FG_IDLE = 0xFFFFFFFF;
    private static final int FG_HELD = 0xFF000000;

    private final KeystrokesConfig config;
    private final ClickCounter leftClicks = new ClickCounter();
    private final ClickCounter rightClicks = new ClickCounter();

    public KeystrokesHud(KeystrokesConfig config) {
        this.config = config;
    }

    public void render() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null || client.options == null) {
            return;
        }
        // Respect the player's own "hide HUD" (F1) choice rather than drawing over a clean screen.
        if (client.options.hudHidden) {
            return;
        }

        GameOptions options = client.options;
        long now = System.currentTimeMillis();
        leftClicks.update(options.attackKey.isPressed(), now);
        rightClicks.update(options.useKey.isPressed(), now);

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.disableLighting();
        GlStateManager.scale(config.scale, config.scale, 1.0f);

        int x = config.x;
        int y = config.y;
        TextRenderer font = client.textRenderer;

        // Row 1: W centred over the ASD row.
        key(font, x + KEY + GAP, y, "W", options.forwardKey);

        // Row 2: A S D
        int row2 = y + KEY + GAP;
        key(font, x, row2, "A", options.leftKey);
        key(font, x + KEY + GAP, row2, "S", options.backKey);
        key(font, x + (KEY + GAP) * 2, row2, "D", options.rightKey);

        // Row 3: the two mouse buttons, labelled with CPS when enabled.
        int row3 = row2 + KEY + GAP;
        int wide = KEY + (KEY + GAP) / 2;
        String left = config.showCps ? leftClicks.cps(now) + " CPS" : "LMB";
        String right = config.showCps ? rightClicks.cps(now) + " CPS" : "RMB";
        wideKey(font, x, row3, wide, left, leftClicks.isDown());
        wideKey(font, x + wide + GAP, row3, wide, right, rightClicks.isDown());

        // Row 4: spacebar as a single bar.
        if (config.showSpacebar) {
            int row4 = row3 + KEY + GAP;
            int full = (KEY + GAP) * 3 - GAP;
            wideKey(font, x, row4, full, "___", options.jumpKey.isPressed());
        }

        GlStateManager.popMatrix();
    }

    private void key(TextRenderer font, int x, int y, String label, KeyBinding binding) {
        wideKey(font, x, y, KEY, label, binding.isPressed());
    }

    private void wideKey(TextRenderer font, int x, int y, int width, String label, boolean held) {
        DrawableHelper.fill(x, y, x + width, y + KEY, held ? BG_HELD : BG_IDLE);
        int textX = x + (width - font.getStringWidth(label)) / 2;
        int textY = y + (KEY - font.fontHeight) / 2 + 1;
        font.draw(label, textX, textY, held ? FG_HELD : FG_IDLE);
    }
}
