package dev.mcclient.hud;

import dev.mcclient.core.ChoiceSetting;
import dev.mcclient.core.KeybindSetting;
import dev.mcclient.core.Module;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.GameOptions;

/**
 * Hold-to-zoom, the way OptiFine trained everyone to expect.
 *
 * <p>Just the field-of-view value, restored the moment the key is released. It renders nothing new
 * and reveals nothing the camera could not already see; it is the same view, narrower.
 */
public final class ZoomModule extends Module {

    /** LWJGL keycode for C, the conventional zoom key. */
    private static final int DEFAULT_KEY = 46;

    private final KeybindSetting zoomKey;
    private final ChoiceSetting amount;

    private float originalFov;
    private boolean zooming;

    public ZoomModule() {
        super("zoom", "Zoom", "Hold a key to narrow the field of view.", true);
        zoomKey = add(new KeybindSetting("zoomKey", "Zoom key", DEFAULT_KEY));
        amount = add(new ChoiceSetting("amount", "Amount", new float[] {2.0f, 3.0f, 4.0f, 6.0f}, 4.0f, "x"));
    }

    public void tick(MinecraftClient client) {
        GameOptions options = client.options;
        if (options == null) {
            return;
        }
        boolean wanted = zoomKey.isDown() && client.currentScreen == null;

        if (wanted && !zooming) {
            originalFov = options.fov;
            zooming = true;
        }
        if (zooming) {
            if (wanted) {
                options.fov = originalFov / amount.get();
            } else {
                // Always hand the player's own FOV back; leaving it narrowed would be a nasty bug.
                options.fov = originalFov;
                zooming = false;
            }
        }
    }

    @Override
    protected void onToggled(boolean on) {
        if (!on && zooming) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null && client.options != null) {
                client.options.fov = originalFov;
            }
            zooming = false;
        }
    }
}
