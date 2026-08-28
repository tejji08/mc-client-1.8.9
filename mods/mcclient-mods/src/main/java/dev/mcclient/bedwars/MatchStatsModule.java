package dev.mcclient.bedwars;

import com.mojang.blaze3d.platform.GlStateManager;
import dev.mcclient.core.BooleanSetting;
import dev.mcclient.core.ChoiceSetting;
import dev.mcclient.core.Corner;
import dev.mcclient.core.Module;
import dev.mcclient.core.OptionSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.util.Window;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Your stats for the match you are actually in: kills, final kills, beds broken, deaths, K/D.
 *
 * <p>Entirely local. Kills, final kills and beds come off the sidebar the server already sent;
 * deaths come from chat the client already displayed. No Hypixel API key, no network call, nothing
 * leaves the machine -- which is the whole reason this is a different feature from a stats overlay
 * that looks up other players' careers.
 */
public final class MatchStatsModule extends Module {

    private static final int PANEL_BG = 0x90000000;
    private static final int ROW_HEIGHT = 11;
    private static final int LABEL = 0xFFAAAAAA;
    private static final int VALUE = 0xFFFFFFFF;

    private final OptionSetting corner;
    private final ChoiceSetting scale;
    private final BooleanSetting showKd;

    private int deaths;
    private int finalDeaths;
    private int lastSidebarTotal = -1;

    public MatchStatsModule() {
        super("match-stats", "Match Stats", "Your kills, finals, beds and deaths this game.", true);
        corner = add(new OptionSetting("corner", "Position", Corner.NAMES, 0));
        scale = add(new ChoiceSetting("scale", "Scale", new float[] {0.75f, 1.0f, 1.25f, 1.5f}, 1.0f, "x"));
        showKd = add(new BooleanSetting("showKd", "Show K/D", true));
    }

    /** Fed every chat line by the mixin. */
    public void onChatLine(String line, String username) {
        DeathWatcher.Event event = DeathWatcher.classify(line, username);
        if (event == DeathWatcher.Event.DEATH) {
            deaths++;
        } else if (event == DeathWatcher.Event.FINAL_DEATH) {
            deaths++;
            finalDeaths++;
        }
    }

    public void render(MinecraftClient client) {
        if (client.world == null) {
            return;
        }
        Map<String, Integer> stats = SidebarSource.stats(client);
        if (stats.isEmpty()) {
            return;
        }

        int kills = value(stats, "Kills");
        int finals = value(stats, "Final Kills");
        int beds = value(stats, "Beds Broken");
        trackMatchReset(kills + finals + beds);

        List<String> labels = new ArrayList<String>();
        List<String> values = new ArrayList<String>();
        labels.add("Kills");
        values.add(String.valueOf(kills));
        labels.add("Finals");
        values.add(String.valueOf(finals));
        labels.add("Beds");
        values.add(String.valueOf(beds));
        labels.add("Deaths");
        values.add(finalDeaths > 0 ? deaths + " (" + finalDeaths + "F)" : String.valueOf(deaths));
        if (showKd.get()) {
            labels.add("K/D");
            values.add(ratio(kills, deaths));
        }

        TextRenderer font = client.textRenderer;
        float s = scale.get();
        Window window = new Window(client);

        int labelWidth = 0;
        int valueWidth = 0;
        for (int i = 0; i < labels.size(); i++) {
            labelWidth = Math.max(labelWidth, font.getStringWidth(labels.get(i)));
            valueWidth = Math.max(valueWidth, font.getStringWidth(values.get(i)));
        }
        int width = labelWidth + 8 + valueWidth;
        int height = labels.size() * ROW_HEIGHT;

        int screenWidth = (int) (window.getScaledWidth() / s);
        int screenHeight = (int) (window.getScaledHeight() / s);
        int x = Corner.x(corner.index(), screenWidth, width, 10);
        int y = Corner.y(corner.index(), screenHeight, height, 10);

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.disableLighting();
        GlStateManager.scale(s, s, 1.0f);

        DrawableHelper.fill(x - 4, y - 4, x + width + 4, y + height + 2, PANEL_BG);
        for (int i = 0; i < labels.size(); i++) {
            int rowY = y + i * ROW_HEIGHT;
            font.draw(labels.get(i), x, rowY, LABEL);
            font.draw(values.get(i), x + width - font.getStringWidth(values.get(i)), rowY, VALUE);
        }

        GlStateManager.popMatrix();
    }

    /**
     * Deaths are ours to count, so they need clearing between games. The sidebar resetting to all
     * zeroes after having been non-zero is the signal a new match started.
     */
    private void trackMatchReset(int sidebarTotal) {
        if (sidebarTotal == 0 && lastSidebarTotal > 0) {
            deaths = 0;
            finalDeaths = 0;
        }
        lastSidebarTotal = sidebarTotal;
    }

    private static String ratio(int kills, int deaths) {
        if (deaths == 0) {
            return kills == 0 ? "0.00" : String.format("%.2f", (float) kills);
        }
        return String.format("%.2f", (float) kills / (float) deaths);
    }

    private static int value(Map<String, Integer> stats, String key) {
        Integer found = stats.get(key);
        return found == null ? 0 : found.intValue();
    }
}
