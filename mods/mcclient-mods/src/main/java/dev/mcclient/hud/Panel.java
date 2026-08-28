package dev.mcclient.hud;

import com.mojang.blaze3d.platform.GlStateManager;
import dev.mcclient.core.Corner;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.util.Window;

import java.util.List;

/** Draws a corner-anchored list of coloured rows. Shared so the HUD modules look identical. */
public final class Panel {

    public static final int BG = 0x90000000;
    private static final int ROW_HEIGHT = 11;

    private Panel() {}

    public static void draw(MinecraftClient client, int cornerIndex, float scale,
                            List<String> rows, List<Integer> colours) {
        if (rows.isEmpty()) {
            return;
        }
        TextRenderer font = client.textRenderer;
        Window window = new Window(client);

        int width = 0;
        for (int i = 0; i < rows.size(); i++) {
            width = Math.max(width, font.getStringWidth(rows.get(i)));
        }
        int height = rows.size() * ROW_HEIGHT;

        int screenWidth = (int) (window.getScaledWidth() / scale);
        int screenHeight = (int) (window.getScaledHeight() / scale);
        int x = Corner.x(cornerIndex, screenWidth, width, 10);
        int y = Corner.y(cornerIndex, screenHeight, height, 10);

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.disableLighting();
        GlStateManager.scale(scale, scale, 1.0f);

        DrawableHelper.fill(x - 4, y - 4, x + width + 4, y + height + 2, BG);
        for (int i = 0; i < rows.size(); i++) {
            font.draw(rows.get(i), x, y + i * ROW_HEIGHT, colours.get(i).intValue());
        }

        GlStateManager.popMatrix();
    }
}
