package dev.mcclient.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;

/**
 * Shared helpers for the modules that read what you are carrying.
 *
 * <p>Everything here comes from the local player's own inventory, which the client already has in
 * full -- no other player's equipment is involved.
 */
final class EquipmentModules {

    private EquipmentModules() {}

    /** Item name plus remaining durability, e.g. "Diamond Sword  87%". */
    static String describe(ItemStack stack, boolean showDurability) {
        if (stack == null) {
            return null;
        }
        String name = stack.hasCustomName() ? stack.getCustomName() : prettify(stack);
        if (!showDurability || stack.getMaxDamage() <= 0) {
            return stack.count > 1 ? name + "  x" + stack.count : name;
        }
        return name + "  " + durabilityPercent(stack) + "%";
    }

    static int durabilityPercent(ItemStack stack) {
        int max = stack.getMaxDamage();
        if (max <= 0) {
            return 100;
        }
        int remaining = max - stack.getDamage();
        return Math.max(0, Math.min(100, remaining * 100 / max));
    }

    /** Colours durability the way a player reads it: fine, getting low, about to break. */
    static int durabilityColour(ItemStack stack, int fallback) {
        if (stack == null || stack.getMaxDamage() <= 0) {
            return fallback;
        }
        int percent = durabilityPercent(stack);
        if (percent <= 10) {
            return 0xFFFF5555;
        }
        return percent <= 30 ? 0xFFFFAA00 : fallback;
    }

    /**
     * Turns a vanilla translation key into something readable without a language file:
     * "item.swordDiamond" becomes "Sword Diamond". Not perfect English, but honest and offline.
     */
    private static String prettify(ItemStack stack) {
        String key = stack.getItem() == null ? "" : stack.getItem().getTranslationKey();
        return Format.prettifyKey(key);
    }

    static ItemStack held(MinecraftClient client) {
        if (client.player == null || client.player.inventory == null) {
            return null;
        }
        return client.player.inventory.getMainHandStack();
    }
}
