package dev.mcclient.hud;

import dev.mcclient.core.BooleanSetting;
import dev.mcclient.core.ChoiceSetting;
import dev.mcclient.core.Corner;
import dev.mcclient.core.Module;
import dev.mcclient.core.OptionSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Active potion effects and their remaining time.
 *
 * <p>Vanilla 1.8.9 only shows this inside the inventory screen, which is exactly where you cannot
 * look mid-fight. The data is the player's own effect list -- nothing about anyone else.
 */
public final class PotionHudModule extends Module {

    private static final int NORMAL = 0xFFFFFFFF;
    private static final int EXPIRING = 0xFFFF5555;
    private static final int EXPIRING_TICKS = 5 * 20;

    private final OptionSetting corner;
    private final ChoiceSetting scale;
    private final BooleanSetting hideAmbient;

    public PotionHudModule() {
        super("potion-hud", "Potion HUD", "Active effects with time remaining.", true);
        corner = add(new OptionSetting("corner", "Position", Corner.NAMES, 1));
        scale = add(new ChoiceSetting("scale", "Scale", new float[] {0.75f, 1.0f, 1.25f, 1.5f}, 1.0f, "x"));
        hideAmbient = add(new BooleanSetting("hideBeacon", "Hide beacon effects", false));
    }

    public void render(MinecraftClient client) {
        if (client.player == null) {
            return;
        }
        Collection<StatusEffectInstance> effects = client.player.getStatusEffectInstances();
        if (effects == null || effects.isEmpty()) {
            return;
        }

        List<StatusEffectInstance> sorted = new ArrayList<StatusEffectInstance>(effects);
        if (hideAmbient.get()) {
            for (int i = sorted.size() - 1; i >= 0; i--) {
                if (sorted.get(i).isAmbient()) {
                    sorted.remove(i);
                }
            }
        }
        if (sorted.isEmpty()) {
            return;
        }
        Collections.sort(sorted, new Comparator<StatusEffectInstance>() {
            @Override
            public int compare(StatusEffectInstance a, StatusEffectInstance b) {
                // Soonest to run out at the top -- that is the one you need to act on.
                return a.getDuration() - b.getDuration();
            }
        });

        List<String> rows = new ArrayList<String>();
        List<Integer> colours = new ArrayList<Integer>();
        for (int i = 0; i < sorted.size(); i++) {
            StatusEffectInstance effect = sorted.get(i);
            String numeral = Format.amplifier(effect.getAmplifier());
            String name = name(effect.getEffectId());
            rows.add((numeral.isEmpty() ? name : name + " " + numeral) + "  " + Format.ticksToTime(effect.getDuration()));
            colours.add(Integer.valueOf(effect.getDuration() <= EXPIRING_TICKS ? EXPIRING : NORMAL));
        }
        Panel.draw(client, corner.index(), scale.get(), rows, colours);
    }

    /** Vanilla's own name for the effect, derived from its translation key so no lang file is needed. */
    private static String name(int effectId) {
        if (effectId >= 0 && effectId < StatusEffect.STATUS_EFFECTS.length) {
            StatusEffect effect = StatusEffect.STATUS_EFFECTS[effectId];
            if (effect != null) {
                return Format.prettifyKey(effect.getTranslationKey());
            }
        }
        return "Effect " + effectId;
    }
}
