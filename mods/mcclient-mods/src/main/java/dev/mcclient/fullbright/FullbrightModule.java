package dev.mcclient.fullbright;

import dev.mcclient.core.Module;
import dev.mcclient.core.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;

/**
 * Brightness override.
 *
 * <p>Vanilla's slider stops at 1.0; the underlying {@code gamma} field is just a float and happily
 * goes further, which is all "fullbright" has ever been. No rendering is replaced and nothing
 * hidden becomes visible -- caves are lit, players behind walls are not. That distinction is what
 * separates this from an x-ray or ESP mod.
 */
public final class FullbrightModule extends Module {

    private final NumberSetting brightness;

    private float originalGamma = 1.0f;
    private boolean haveOriginal;

    public FullbrightModule() {
        super("fullbright", "Fullbright", "Lights the world past the brightness slider's limit.", false);
        // Vanilla's own slider stops at 1.0; anything above that is the override, and 100 is
        // the conventional "as bright as it goes" value.
        brightness = add(new NumberSetting("brightness", "Brightness", 1.0f, 100.0f, 1.0f, 100.0f, "", 0));
    }

    private float bright() {
        return brightness.get();
    }

    @Override
    public void onClientReady() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client != null && client.options != null && isEnabled()) {
            remember(client.options);
            client.options.gamma = bright();
        }
    }

    @Override
    protected void onToggled(boolean on) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.options == null) {
            return;
        }
        if (on) {
            remember(client.options);
            client.options.gamma = bright();
        } else {
            // Put the player's own brightness back rather than guessing at a default.
            client.options.gamma = haveOriginal ? originalGamma : 1.0f;
        }
    }

    /** Re-asserts the override; the options screen can write gamma back underneath us. */
    public void tick(MinecraftClient client) {
        if (client.options != null && client.options.gamma != bright()) {
            client.options.gamma = bright();
        }
    }

    private void remember(GameOptions options) {
        if (!haveOriginal) {
            // If we were already forcing it (restored from config), 1.0 is the sane thing to go back to.
            originalGamma = options.gamma >= bright() ? 1.0f : options.gamma;
            haveOriginal = true;
        }
    }
}
