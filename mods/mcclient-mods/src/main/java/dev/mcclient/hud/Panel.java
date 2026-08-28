package dev.mcclient.hud;

import com.mojang.blaze3d.platform.GlStateManager;
import dev.mcclient.core.HudModule;
import dev.mcclient.core.PositionSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.util.Window;

import java.util.ArrayList;
import java.util.List;

/**
 * Draws a positioned list of rows, and records where it landed so the HUD editor can hit-test and
 * drag it. Shared so every HUD module looks identical, restyles identically, and moves identically.
 */
public final class Panel {

    public static final int ROW_HEIGHT = 11;
    private static final int PAD = 4;

    private Panel() {}

    /** Rows in the module's own configured colour. */
    public static void draw(MinecraftClient client, HudModule module, List<String> rows) {
        List<Integer> colours = new ArrayList<Integer>(rows.size());
        Integer colour = Integer.valueOf(module.textColour());
        for (int i = 0; i < rows.size(); i++) {
            colours.add(colour);
        }
        draw(client, module, rows, colours);
    }

    /** Rows whose colours carry meaning of their own -- a team's red, an expiring effect. */
    public static void draw(MinecraftClient client, HudModule module,
                            List<String> rows, List<Integer> colours) {
        if (rows.isEmpty()) {
            return;
        }
        TextRenderer font = client.textRenderer;
        int width = 0;
        for (int i = 0; i < rows.size(); i++) {
            width = Math.max(width, font.getStringWidth(rows.get(i)));
        }
        int height = rows.size() * ROW_HEIGHT;

        float scale = module.scale();
        int[] xy = place(client, module.position(), width, height, scale);

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.disableLighting();
        GlStateManager.scale(scale, scale, 1.0f);

        if (module.hasBackground()) {
            DrawableHelper.fill(xy[0] - PAD, xy[1] - PAD, xy[0] + width + PAD, xy[1] + height + 2,
                    module.backgroundColour());
        }
        for (int i = 0; i < rows.size(); i++) {
            int y = xy[1] + i * ROW_HEIGHT;
            int colour = colours.get(i).intValue();
            if (module.hasShadow()) {
                font.drawWithShadow(rows.get(i), xy[0], y, colour);
            } else {
                font.draw(rows.get(i), xy[0], y, colour);
            }
        }

        GlStateManager.popMatrix();
    }

    /**
     * Turns the stored screen fraction into scaled-space coordinates, clamps the panel fully on
     * screen, and records the real-pixel bounds for the editor.
     *
     * @return {x, y} in the module's own scaled coordinate space
     */
    public static int[] place(MinecraftClient client, PositionSetting position,
                              int width, int height, float scale) {
        Window window = new Window(client);
        int screenWidth = (int) (window.getScaledWidth() / scale);
        int screenHeight = (int) (window.getScaledHeight() / scale);

        int x = Math.round(position.x() * screenWidth);
        int y = Math.round(position.y() * screenHeight);
        x = clamp(x, PAD, Math.max(PAD, screenWidth - width - PAD));
        y = clamp(y, PAD, Math.max(PAD, screenHeight - height - PAD));

        position.recordBounds(
                Math.round((x - PAD) * scale),
                Math.round((y - PAD) * scale),
                Math.round((width + PAD * 2) * scale),
                Math.round((height + PAD * 2) * scale));
        return new int[] {x, y};
    }

    private static int clamp(int value, int min, int max) {
        if (value < min) {
            return min;
        }
        return value > max ? max : value;
    }
}
