package dev.mcclient.core;

import net.minecraft.client.MinecraftClient;

/**
 * A module that draws a draggable panel on screen.
 *
 * <p>Position and scale are standard here rather than re-declared by every module, so the HUD
 * editor can move any of them without knowing what they draw.
 */
public abstract class HudModule extends Module {

    private final PositionSetting position;
    private final ChoiceSetting scale;

    protected HudModule(String id, String name, String description, boolean enabledByDefault,
                        float defaultX, float defaultY) {
        super(id, name, description, enabledByDefault);
        position = add(new PositionSetting("position", "Position", defaultX, defaultY));
        scale = add(new ChoiceSetting("scale", "Scale", new float[] {0.75f, 1.0f, 1.25f, 1.5f}, 1.0f, "x"));
    }

    public PositionSetting position() {
        return position;
    }

    public float scale() {
        return scale.get();
    }

    /**
     * Draws the panel.
     *
     * @param preview true when the HUD editor is open. Modules whose real content only exists in a
     *                Bed Wars game draw sample rows instead, otherwise there would be nothing on
     *                screen to drag.
     */
    public abstract void draw(MinecraftClient client, boolean preview);
}
