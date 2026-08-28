package dev.mcclient.menu;

import dev.mcclient.core.HudModule;
import dev.mcclient.core.KeybindSetting;
import dev.mcclient.core.Module;
import dev.mcclient.core.ModuleRegistry;
import dev.mcclient.core.PositionSetting;
import dev.mcclient.core.Setting;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * The settings menu: modules on the left, the selected module's settings on the right.
 *
 * <p>Built from plain {@link ButtonWidget}s so it looks and behaves like a vanilla screen. 1.8.9
 * has no slider or checkbox widget, which is why most settings are buttons that cycle -- the same
 * trick the vanilla Options screen uses. Keybinds are the exception: cycling a hundred key codes a
 * click at a time would be useless, so they capture the next key you press instead.
 */
public final class ModMenuScreen extends Screen {

    private static final int ID_DONE = 900;
    private static final int ID_TOGGLE = 800;
    private static final int ID_EDIT_HUD = 801;
    private static final int ID_SETTING_BASE = 200;

    /** Mods we did not write, so the list of "everything installed" is honest about what it can edit. */
    private static final List<String> IGNORED_IDS =
            Arrays.asList("minecraft", "java", "fabricloader", "mcclient-mods");

    private int selected;
    private KeybindSetting capturing;
    private List<String> otherMods = Collections.emptyList();

    @Override
    public void init() {
        this.buttons.clear();
        otherMods = collectOtherMods();

        List<Module> modules = ModuleRegistry.modules();
        int leftX = this.width / 2 - 170;
        int rightX = this.width / 2 + 10;
        int top = 46;

        for (int i = 0; i < modules.size(); i++) {
            this.buttons.add(new ButtonWidget(i, leftX, top + i * 22, 160, 20, label(modules.get(i), i)));
        }

        if (selected >= 0 && selected < modules.size()) {
            Module module = modules.get(selected);
            int row = 0;
            if (module.canDisable()) {
                this.buttons.add(new ButtonWidget(ID_TOGGLE, rightX, top, 160, 20,
                        module.isEnabled() ? "Enabled: ON" : "Enabled: OFF"));
                row++;
            }
            List<Setting> settings = module.settings();
            for (int j = 0; j < settings.size(); j++) {
                Setting setting = settings.get(j);
                int id = setting instanceof PositionSetting ? ID_EDIT_HUD : ID_SETTING_BASE + j;
                String text = setting instanceof PositionSetting
                        ? "Position: edit HUD..."
                        : setting.name() + ": " + setting.valueLabel();
                this.buttons.add(new ButtonWidget(id, rightX, top + (row + j) * 22, 160, 20, text));
            }
        }

        this.buttons.add(new ButtonWidget(ID_DONE, this.width / 2 - 100, this.height - 28, 200, 20, "Done"));
    }

    private String label(Module module, int index) {
        String mark = index == selected ? "> " : "  ";
        return mark + module.name() + (module.canDisable() && !module.isEnabled() ? " (off)" : "");
    }

    @Override
    protected void buttonClicked(ButtonWidget button) {
        if (!button.active) {
            return;
        }
        List<Module> modules = ModuleRegistry.modules();
        cancelCapture();

        if (button.id == ID_DONE) {
            ModuleRegistry.save();
            this.client.setScreen(null);
            return;
        }
        if (button.id == ID_EDIT_HUD) {
            ModuleRegistry.save();
            this.client.setScreen(new HudEditorScreen());
            return;
        }
        if (button.id == ID_TOGGLE && selected < modules.size()) {
            Module module = modules.get(selected);
            if (module.canDisable()) {
                module.setEnabled(!module.isEnabled());
            }
        } else if (button.id >= ID_SETTING_BASE && selected < modules.size()) {
            List<Setting> settings = modules.get(selected).settings();
            int index = button.id - ID_SETTING_BASE;
            if (index < settings.size()) {
                Setting setting = settings.get(index);
                setting.cycle();
                // A keybind's "cycle" arms capture rather than changing anything.
                if (setting instanceof KeybindSetting) {
                    capturing = (KeybindSetting) setting;
                }
            }
        } else if (button.id >= 0 && button.id < modules.size()) {
            selected = button.id;
        }

        ModuleRegistry.save();
        // Rebuild so every label reflects the new state.
        this.init();
    }

    @Override
    public void render(int mouseX, int mouseY, float delta) {
        this.renderBackground();
        this.drawCenteredString(this.textRenderer, "mc-client", this.width / 2, 16, 0xFFFFFF);

        List<Module> modules = ModuleRegistry.modules();
        if (selected >= 0 && selected < modules.size()) {
            this.textRenderer.draw(modules.get(selected).description(), this.width / 2 + 10, 32, 0xA0A0A0);
        }
        if (capturing != null) {
            this.drawCenteredString(this.textRenderer,
                    "Press a key to bind, or Escape to clear", this.width / 2, this.height - 44, 0xFFD54F);
        }

        // Everything else the launcher installed. We can list it, but it is configured in the
        // launcher (or by its own author), so it is shown read-only rather than faked as editable.
        if (!otherMods.isEmpty()) {
            this.textRenderer.draw("Also loaded: " + join(otherMods),
                    this.width / 2 - 170, this.height - 52, 0x707070);
        }

        super.render(mouseX, mouseY, delta);
    }

    @Override
    protected void keyPressed(char character, int keyCode) {
        if (capturing != null) {
            // Escape clears the binding rather than closing; that is the only way to unbind a key.
            capturing.set(keyCode == 1 ? KeybindSetting.NONE : keyCode);
            capturing = null;
            ModuleRegistry.save();
            this.init();
            return;
        }
        Module client = ModuleRegistry.byId("client");
        int menuKey = client == null ? 54 : ((dev.mcclient.core.ClientModule) client).menuKey().keyCode();
        if (keyCode == 1 || (menuKey != KeybindSetting.NONE && keyCode == menuKey)) {
            ModuleRegistry.save();
            this.client.setScreen(null);
            return;
        }
        super.keyPressed(character, keyCode);
    }

    private void cancelCapture() {
        if (capturing != null) {
            capturing.cancelCapture();
            capturing = null;
        }
    }

    @Override
    public boolean shouldPauseGame() {
        // Matches how client menus behave -- opening settings mid-game should not pause singleplayer.
        return false;
    }

    private static List<String> collectOtherMods() {
        List<String> names = new ArrayList<String>();
        try {
            Collection<ModContainer> mods = FabricLoader.getInstance().getAllMods();
            for (ModContainer mod : mods) {
                String id = mod.getMetadata().getId();
                if (IGNORED_IDS.contains(id) || id.startsWith("fabric-") || id.startsWith("legacy-fabric")) {
                    continue;
                }
                names.add(mod.getMetadata().getName());
            }
            Collections.sort(names);
        } catch (RuntimeException e) {
            // A missing loader API is not worth breaking the menu over.
        }
        return names;
    }

    private static String join(List<String> values) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                out.append(", ");
            }
            out.append(values.get(i));
        }
        return out.toString();
    }

    /** Unused hook kept so the compiler flags any future HudModule that forgets a position. */
    static boolean isHud(Module module) {
        return module instanceof HudModule;
    }
}
