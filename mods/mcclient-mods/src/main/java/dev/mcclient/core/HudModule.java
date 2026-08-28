package dev.mcclient.core;

import net.minecraft.client.MinecraftClient;

/**
 * A module that draws a draggable panel on screen.
 *
 * <p>Position, size and styling are standard here rather than re-declared by every module, so the
 * HUD editor can move any of them and the settings menu can restyle any of them without knowing
 * what they draw.
 */
public abstract class HudModule extends Module {

    private final PositionSetting position;
    private final NumberSetting scale;
    private final ColorSetting textColour;
    private final BooleanSetting background;
    private final NumberSetting backgroundOpacity;
    private final BooleanSetting shadow;

    protected HudModule(String id, String name, String description, boolean enabledByDefault,
                        float defaultX, float defaultY) {
        super(id, name, description, enabledByDefault);
        position = add(new PositionSetting("position", "Position", defaultX, defaultY));
        scale = add(new NumberSetting("scale", "Size", 0.5f, 3.0f, 0.05f, 1.0f, "x", 2));
        textColour = add(new ColorSetting("colour", "Text colour", 0xFFFFFFFF));
        background = add(new BooleanSetting("background", "Background", true));
        backgroundOpacity = add(new NumberSetting("bgOpacity", "Background opacity", 0f, 100f, 5f, 56f, "%", 0));
        shadow = add(new BooleanSetting("shadow", "Text shadow", true));
    }

    public PositionSetting position() {
        return position;
    }

    public float scale() {
        return scale.get();
    }

    /**
     * The configured text colour. Modules whose rows carry meaning in their colour -- team red,
     * an expiring effect -- pass their own and ignore this.
     */
    public int textColour() {
        return textColour.get();
    }

    public boolean hasBackground() {
        return background.get();
    }

    /** Background as ARGB, alpha taken from the opacity setting. */
    public int backgroundColour() {
        int alpha = Math.round(backgroundOpacity.get() / 100.0f * 255.0f);
        return (alpha << 24);
    }

    public boolean hasShadow() {
        return shadow.get();
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
