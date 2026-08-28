package dev.mcclient.menu;

import dev.mcclient.core.HudModule;
import dev.mcclient.core.Module;
import dev.mcclient.core.ModuleRegistry;
import dev.mcclient.core.PositionSetting;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;

import java.util.ArrayList;
import java.util.List;

/**
 * Drag HUD panels wherever you want them.
 *
 * <p>Each panel draws its real content (or a sample, when there is nothing live to show) and
 * records where it landed; this screen hit-tests those recorded bounds. Positions are stored as a
 * fraction of the screen, so a panel dragged here stays put when the window is resized or the GUI
 * scale changes -- pixels would not.
 */
public final class HudEditorScreen extends Screen {

    private static final int ID_DONE = 900;
    private static final int ID_RESET = 901;

    private static final int OUTLINE = 0x60FFFFFF;
    private static final int OUTLINE_ACTIVE = 0xFF3B82F6;

    private HudModule dragging;
    private int grabOffsetX;
    private int grabOffsetY;

    @Override
    public void init() {
        this.buttons.clear();
        this.buttons.add(new ButtonWidget(ID_DONE, this.width / 2 - 104, this.height - 28, 100, 20, "Done"));
        this.buttons.add(new ButtonWidget(ID_RESET, this.width / 2 + 4, this.height - 28, 100, 20, "Reset all"));
    }

    @Override
    public void render(int mouseX, int mouseY, float delta) {
        this.renderBackground();

        // Panels draw themselves in preview mode, which also refreshes their recorded bounds.
        List<HudModule> huds = huds();
        for (int i = 0; i < huds.size(); i++) {
            HudModule hud = huds.get(i);
            hud.position().clearDrawn();
            hud.draw(this.client, true);
        }

        for (int i = 0; i < huds.size(); i++) {
            HudModule hud = huds.get(i);
            PositionSetting position = hud.position();
            if (!position.wasDrawn()) {
                continue;
            }
            boolean active = hud == dragging || position.contains(mouseX, mouseY);
            outline(position, active ? OUTLINE_ACTIVE : OUTLINE);
        }

        this.drawCenteredString(this.textRenderer, "Drag a panel to move it", this.width / 2, 12, 0xFFFFFF);
        this.drawCenteredString(this.textRenderer,
                "Panels with nothing to show are previewed with sample values",
                this.width / 2, 26, 0xA0A0A0);

        super.render(mouseX, mouseY, delta);
    }

    private void outline(PositionSetting position, int colour) {
        int x = position.lastX();
        int y = position.lastY();
        int width = position.lastWidth();
        int height = position.lastHeight();
        fill(x, y, x + width, y + 1, colour);
        fill(x, y + height - 1, x + width, y + height, colour);
        fill(x, y, x + 1, y + height, colour);
        fill(x + width - 1, y, x + width, y + height, colour);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) {
        if (button == 0) {
            List<HudModule> huds = huds();
            // Reverse order so the panel drawn last (on top) is the one you grab.
            for (int i = huds.size() - 1; i >= 0; i--) {
                HudModule hud = huds.get(i);
                PositionSetting position = hud.position();
                if (position.wasDrawn() && position.contains(mouseX, mouseY)) {
                    dragging = hud;
                    grabOffsetX = mouseX - position.lastX();
                    grabOffsetY = mouseY - position.lastY();
                    return;
                }
            }
        }
        super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void mouseDragged(int mouseX, int mouseY, int button, long heldMillis) {
        if (dragging != null) {
            PositionSetting position = dragging.position();
            int newX = mouseX - grabOffsetX;
            int newY = mouseY - grabOffsetY;
            position.set((float) newX / (float) this.width, (float) newY / (float) this.height);
            return;
        }
        super.mouseDragged(mouseX, mouseY, button, heldMillis);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int button) {
        if (dragging != null) {
            dragging = null;
            ModuleRegistry.save();
            return;
        }
        super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    protected void buttonClicked(ButtonWidget button) {
        if (button.id == ID_DONE) {
            ModuleRegistry.save();
            this.client.setScreen(null);
        } else if (button.id == ID_RESET) {
            List<HudModule> huds = huds();
            for (int i = 0; i < huds.size(); i++) {
                huds.get(i).position().set(0.02f, 0.02f + i * 0.09f);
            }
            ModuleRegistry.save();
        }
    }

    @Override
    protected void keyPressed(char character, int keyCode) {
        if (keyCode == 1) {
            ModuleRegistry.save();
            this.client.setScreen(null);
            return;
        }
        super.keyPressed(character, keyCode);
    }

    @Override
    public boolean shouldPauseGame() {
        return false;
    }

    private static List<HudModule> huds() {
        List<HudModule> huds = new ArrayList<HudModule>();
        List<Module> modules = ModuleRegistry.modules();
        for (int i = 0; i < modules.size(); i++) {
            Module module = modules.get(i);
            if (module instanceof HudModule && module.isEnabled()) {
                huds.add((HudModule) module);
            }
        }
        return huds;
    }
}
