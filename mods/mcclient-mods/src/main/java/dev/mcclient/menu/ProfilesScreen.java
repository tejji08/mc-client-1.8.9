package dev.mcclient.menu;

import dev.mcclient.core.ModuleRegistry;
import dev.mcclient.core.ProfileStore;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;

import java.util.List;

/**
 * Switch, create and delete HUD profiles.
 *
 * <p>Each profile is a plain properties file, so anything made here can be copied, shared or kept
 * in version control without the client being involved.
 */
public final class ProfilesScreen extends Screen {

    private static final int ID_BACK = 900;
    private static final int ID_CREATE = 901;
    private static final int ID_DELETE_BASE = 400;
    private static final int ID_SELECT_BASE = 200;

    private static final int ROW = 22;
    private static final int TOP = 52;
    private static final int VISIBLE = 7;

    private TextFieldWidget nameField;
    private String notice = "";

    @Override
    public void init() {
        this.buttons.clear();

        nameField = new TextFieldWidget(0, this.textRenderer, this.width / 2 - 100, this.height - 56, 140, 20);
        nameField.setMaxLength(32);

        List<String> names = ProfileStore.names();
        int shown = Math.min(VISIBLE, names.size());
        for (int i = 0; i < shown; i++) {
            String name = names.get(i);
            boolean isActive = name.equals(ProfileStore.active());
            this.buttons.add(new ButtonWidget(ID_SELECT_BASE + i, this.width / 2 - 130, TOP + i * ROW, 200, 20,
                    (isActive ? "> " : "  ") + name));
            ButtonWidget delete = new ButtonWidget(ID_DELETE_BASE + i, this.width / 2 + 76, TOP + i * ROW, 54, 20,
                    "Delete");
            // The default profile is the fallback when another is deleted; it has to survive.
            delete.active = !ProfileStore.DEFAULT_NAME.equals(name);
            this.buttons.add(delete);
        }

        this.buttons.add(new ButtonWidget(ID_CREATE, this.width / 2 + 44, this.height - 56, 86, 20, "New profile"));
        this.buttons.add(new ButtonWidget(ID_BACK, this.width / 2 - 100, this.height - 28, 200, 20, "Back"));
    }

    @Override
    protected void buttonClicked(ButtonWidget button) {
        if (!button.active) {
            return;
        }
        List<String> names = ProfileStore.names();

        if (button.id == ID_BACK) {
            this.client.setScreen(new ModMenuScreen());
            return;
        }
        if (button.id == ID_CREATE) {
            String wanted = ProfileStore.sanitise(nameField.getText());
            if (ProfileStore.exists(wanted)) {
                notice = "A profile called " + wanted + " already exists.";
                return;
            }
            // A new profile starts as a copy of what is on screen, which is nearly always what
            // you want -- you are branching a layout you already like.
            ModuleRegistry.createProfile(wanted);
            nameField.setText("");
            notice = "Created " + wanted + " from the current layout.";
            this.init();
            return;
        }
        if (button.id >= ID_DELETE_BASE) {
            int index = button.id - ID_DELETE_BASE;
            if (index < names.size()) {
                String name = names.get(index);
                ModuleRegistry.deleteProfile(name);
                notice = "Deleted " + name + ".";
                this.init();
            }
            return;
        }
        if (button.id >= ID_SELECT_BASE) {
            int index = button.id - ID_SELECT_BASE;
            if (index < names.size()) {
                ModuleRegistry.switchProfile(names.get(index));
                notice = "Now using " + ProfileStore.active() + ".";
                this.init();
            }
        }
    }

    @Override
    public void render(int mouseX, int mouseY, float delta) {
        this.renderBackground();
        this.drawCenteredString(this.textRenderer, "HUD Profiles", this.width / 2, 20, 0xFFFFFF);
        this.drawCenteredString(this.textRenderer,
                "Switching saves the profile you are leaving first", this.width / 2, 34, 0xA0A0A0);
        if (!notice.isEmpty()) {
            this.drawCenteredString(this.textRenderer, notice, this.width / 2, this.height - 72, 0xFFD54F);
        }
        nameField.render();
        super.render(mouseX, mouseY, delta);
    }

    @Override
    protected void keyPressed(char character, int keyCode) {
        if (nameField.keyPressed(character, keyCode)) {
            return;
        }
        if (keyCode == 1) {
            this.client.setScreen(new ModMenuScreen());
            return;
        }
        super.keyPressed(character, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) {
        nameField.mouseClicked(mouseX, mouseY, button);
        super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void tick() {
        nameField.tick();
        super.tick();
    }

    @Override
    public boolean shouldPauseGame() {
        return false;
    }
}
