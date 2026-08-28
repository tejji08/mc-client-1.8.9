package dev.mcclient.keystrokes;

import com.mojang.blaze3d.platform.GlStateManager;
import dev.mcclient.core.BooleanSetting;
import dev.mcclient.core.ChoiceSetting;
import dev.mcclient.core.Corner;
import dev.mcclient.core.Module;
import dev.mcclient.core.OptionSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.Window;

/**
 * WASD and mouse key display with a rolling CPS counter.
 *
 * <p>Reads the local client's own input state. Nothing is queried from the server, and the click
 * counter is fed key states rather than hooking input, so it is structurally incapable of
 * generating a click.
 */
public final class KeystrokesModule extends Module {

    private static final int KEY = 22;
    private static final int GAP = 2;
    private static final int BG_IDLE = 0x80000000;
    private static final int BG_HELD = 0xC0FFFFFF;
    private static final int FG_IDLE = 0xFFFFFFFF;
    private static final int FG_HELD = 0xFF000000;

    private final OptionSetting corner;
    private final ChoiceSetting scale;
    private final BooleanSetting showCps;
    private final BooleanSetting showSpacebar;

    private final ClickCounter leftClicks = new ClickCounter();
    private final ClickCounter rightClicks = new ClickCounter();

    public KeystrokesModule() {
        super("keystrokes", "Keystrokes", "WASD and mouse keys with a CPS counter.", true);
        corner = add(new OptionSetting("corner", "Position", Corner.NAMES, 2));
        scale = add(new ChoiceSetting("scale", "Scale", new float[] {0.75f, 1.0f, 1.25f, 1.5f}, 1.0f, "x"));
        showCps = add(new BooleanSetting("showCps", "Show CPS", true));
        showSpacebar = add(new BooleanSetting("showSpacebar", "Show spacebar", true));
    }

    public void render(MinecraftClient client) {
        GameOptions options = client.options;
        long now = System.currentTimeMillis();
        leftClicks.update(options.attackKey.isPressed(), now);
        rightClicks.update(options.useKey.isPressed(), now);

        TextRenderer font = client.textRenderer;
        float s = scale.get();
        Window window = new Window(client);

        int panelWidth = (KEY + GAP) * 3 - GAP;
        int rows = showSpacebar.get() ? 4 : 3;
        int panelHeight = (KEY + GAP) * rows - GAP;

        int screenWidth = (int) (window.getScaledWidth() / s);
        int screenHeight = (int) (window.getScaledHeight() / s);
        int x = Corner.x(corner.index(), screenWidth, panelWidth, 6);
        int y = Corner.y(corner.index(), screenHeight, panelHeight, 6);

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.disableLighting();
        GlStateManager.scale(s, s, 1.0f);

        key(font, x + KEY + GAP, y, "W", options.forwardKey);

        int row2 = y + KEY + GAP;
        key(font, x, row2, "A", options.leftKey);
        key(font, x + KEY + GAP, row2, "S", options.backKey);
        key(font, x + (KEY + GAP) * 2, row2, "D", options.rightKey);

        int row3 = row2 + KEY + GAP;
        int wide = KEY + (KEY + GAP) / 2;
        String left = showCps.get() ? leftClicks.cps(now) + " CPS" : "LMB";
        String right = showCps.get() ? rightClicks.cps(now) + " CPS" : "RMB";
        wideKey(font, x, row3, wide, left, leftClicks.isDown());
        wideKey(font, x + wide + GAP, row3, wide, right, rightClicks.isDown());

        if (showSpacebar.get()) {
            wideKey(font, x, row3 + KEY + GAP, panelWidth, "___", options.jumpKey.isPressed());
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
