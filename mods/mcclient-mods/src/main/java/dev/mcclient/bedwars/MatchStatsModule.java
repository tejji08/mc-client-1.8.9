package dev.mcclient.bedwars;

import com.mojang.blaze3d.platform.GlStateManager;
import dev.mcclient.core.BooleanSetting;
import dev.mcclient.core.HudModule;
import dev.mcclient.hud.Panel;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;

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
public final class MatchStatsModule extends HudModule {

    private static final int ROW_HEIGHT = Panel.ROW_HEIGHT;
    private static final int LABEL = 0xFFAAAAAA;
    private static final int VALUE = 0xFFFFFFFF;
    private static final int GAP = 8;

    private final BooleanSetting showKd;

    private int deaths;
    private int finalDeaths;
    private int lastSidebarTotal = -1;

    public MatchStatsModule() {
        super("match-stats", "Match Stats", "Your kills, finals, beds and deaths this game.", true,
                0.02f, 0.30f);
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

    @Override
    public void draw(MinecraftClient client, boolean preview) {
        Map<String, Integer> stats = client.world == null ? null : SidebarSource.stats(client);

        List<String> labels = new ArrayList<String>();
        List<String> values = new ArrayList<String>();
        if (stats == null || stats.isEmpty()) {
            if (!preview) {
                return;
            }
            // Outside a Bed Wars game there is nothing to show, so the editor gets sample rows.
            fill(labels, values, 5, 2, 1, 1, 0);
        } else {
            int kills = value(stats, "Kills");
            int finals = value(stats, "Final Kills");
            int beds = value(stats, "Beds Broken");
            trackMatchReset(kills + finals + beds);
            fill(labels, values, kills, finals, beds, deaths, finalDeaths);
        }

        TextRenderer font = client.textRenderer;
        int labelWidth = 0;
        int valueWidth = 0;
        for (int i = 0; i < labels.size(); i++) {
            labelWidth = Math.max(labelWidth, font.getStringWidth(labels.get(i)));
            valueWidth = Math.max(valueWidth, font.getStringWidth(values.get(i)));
        }
        int width = labelWidth + GAP + valueWidth;
        int height = labels.size() * ROW_HEIGHT;

        float scale = scale();
        int[] xy = Panel.place(client, position(), width, height, scale);

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.disableLighting();
        GlStateManager.scale(scale, scale, 1.0f);

        DrawableHelper.fill(xy[0] - 4, xy[1] - 4, xy[0] + width + 4, xy[1] + height + 2, Panel.BG);
        for (int i = 0; i < labels.size(); i++) {
            int rowY = xy[1] + i * ROW_HEIGHT;
            font.draw(labels.get(i), xy[0], rowY, LABEL);
            font.draw(values.get(i), xy[0] + width - font.getStringWidth(values.get(i)), rowY, VALUE);
        }

        GlStateManager.popMatrix();
    }

    private void fill(List<String> labels, List<String> values,
                      int kills, int finals, int beds, int deathCount, int finalDeathCount) {
        labels.add("Kills");
        values.add(String.valueOf(kills));
        labels.add("Finals");
        values.add(String.valueOf(finals));
        labels.add("Beds");
        values.add(String.valueOf(beds));
        labels.add("Deaths");
        values.add(finalDeathCount > 0 ? deathCount + " (" + finalDeathCount + "F)" : String.valueOf(deathCount));
        if (showKd.get()) {
            labels.add("K/D");
            values.add(ratio(kills, deathCount));
        }
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
