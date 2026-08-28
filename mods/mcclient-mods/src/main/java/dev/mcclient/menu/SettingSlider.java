package dev.mcclient.menu;

import dev.mcclient.core.NumberSetting;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ButtonWidget;

/**
 * A slider bound to a {@link NumberSetting}.
 *
 * <p>Vanilla's own {@code SliderWidget} wants an options-list listener this menu does not have, so
 * this is a plain button that paints and drags itself, following the same hooks vanilla uses:
 * {@code isMouseOver} starts the drag, {@code mouseDragged} continues it, {@code mouseReleased}
 * ends it. It reads straight from the setting, so there is no copy of the value to drift.
 */
final class SettingSlider extends ButtonWidget {

    private final NumberSetting setting;
    private boolean dragging;

    SettingSlider(int id, int x, int y, int width, int height, NumberSetting setting) {
        super(id, x, y, width, height, "");
        this.setting = setting;
        updateMessage();
    }

    NumberSetting setting() {
        return setting;
    }

    private void updateMessage() {
        this.message = setting.name() + ": " + setting.valueLabel();
    }

    /** Hovering must not swap the button face mid-drag, the way a normal button would. */
    @Override
    protected int getYImage(boolean hovered) {
        return 0;
    }

    @Override
    public void render(MinecraftClient client, int mouseX, int mouseY) {
        super.render(client, mouseX, mouseY);
        if (!this.visible) {
            return;
        }
        int knob = this.x + Math.round(setting.progress() * (this.getWidth() - 8));
        fill(knob, this.y, knob + 8, this.y + this.height, 0xFFCCCCCC);
        fill(knob + 1, this.y + 1, knob + 7, this.y + this.height - 1, 0xFF8B93A3);
    }

    @Override
    protected void mouseDragged(MinecraftClient client, int mouseX, int mouseY) {
        if (this.visible && dragging) {
            setFromMouse(mouseX);
        }
    }

    @Override
    public boolean isMouseOver(MinecraftClient client, int mouseX, int mouseY) {
        boolean over = super.isMouseOver(client, mouseX, mouseY);
        if (over) {
            setFromMouse(mouseX);
            dragging = true;
        }
        return over;
    }

    @Override
    public void mouseReleased(int mouseX, int mouseY) {
        dragging = false;
        super.mouseReleased(mouseX, mouseY);
    }

    private void setFromMouse(int mouseX) {
        float progress = (float) (mouseX - (this.x + 4)) / (float) (this.getWidth() - 8);
        setting.setProgress(clamp(progress));
        updateMessage();
    }

    private static float clamp(float value) {
        if (value < 0.0f) {
            return 0.0f;
        }
        return value > 1.0f ? 1.0f : value;
    }
}
