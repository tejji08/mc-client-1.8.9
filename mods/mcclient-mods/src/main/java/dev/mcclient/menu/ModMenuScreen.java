package dev.mcclient.menu;

import dev.mcclient.core.ClientModule;
import dev.mcclient.core.KeybindSetting;
import dev.mcclient.core.Module;
import dev.mcclient.core.ModuleRegistry;
import dev.mcclient.core.NumberSetting;
import dev.mcclient.core.PositionSetting;
import dev.mcclient.core.ProfileStore;
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
 * <p>Built from plain widgets so it looks and behaves like a vanilla screen. Numeric settings get a
 * real slider, since "a bit bigger" is a reasonable thing to ask of a HUD and a handful of preset
 * steps cannot answer it. Keybinds capture the next key pressed rather than cycling, because
 * stepping through a hundred key codes a click at a time would be useless.
 *
 * <p>Both columns scroll: sixteen modules, several with eight settings each, stopped fitting on one
 * screen a while ago.
 */
public final class ModMenuScreen extends Screen {

    private static final int ID_DONE = 900;
    private static final int ID_TOGGLE = 800;
    private static final int ID_EDIT_HUD = 801;
    private static final int ID_RELOAD = 802;
    private static final int ID_SCROLL_UP = 803;
    private static final int ID_SCROLL_DOWN = 804;
    private static final int ID_SET_UP = 805;
    private static final int ID_SET_DOWN = 806;
    private static final int ID_PROFILES = 807;
    private static final int ID_SETTING_BASE = 200;

    private static final int ROW = 22;
    private static final int TOP = 46;
    private static final int VISIBLE_ROWS = 9;

    /** Mods we did not write, so the list of "everything installed" is honest about what it can edit. */
    private static final List<String> IGNORED_IDS =
            Arrays.asList("minecraft", "java", "fabricloader", "mcclient-mods");

    private static int selected;
    private static int moduleScroll;
    private static int settingScroll;

    private KeybindSetting capturing;
    private List<String> otherMods = Collections.emptyList();

    @Override
    public void init() {
        this.buttons.clear();
        otherMods = collectOtherMods();

        List<Module> modules = ModuleRegistry.modules();
        clampScroll(modules.size());

        int leftX = this.width / 2 - 176;
        int rightX = this.width / 2 + 12;

        int shown = Math.min(VISIBLE_ROWS, modules.size() - moduleScroll);
        for (int i = 0; i < shown; i++) {
            int index = moduleScroll + i;
            this.buttons.add(new ButtonWidget(index, leftX, TOP + i * ROW, 160, 20,
                    label(modules.get(index), index)));
        }
        if (modules.size() > VISIBLE_ROWS) {
            this.buttons.add(new ButtonWidget(ID_SCROLL_UP, leftX - 22, TOP, 20, 20, "^"));
            this.buttons.add(new ButtonWidget(ID_SCROLL_DOWN, leftX - 22, TOP + (shown - 1) * ROW, 20, 20, "v"));
        }

        if (selected >= 0 && selected < modules.size()) {
            Module module = modules.get(selected);
            List<Setting> settings = module.settings();

            // The enable toggle is a pseudo-row at the top of the same scrollable column, so a
            // module with more settings than fit on screen still reaches every one of them.
            int totalRows = settings.size() + (module.canDisable() ? 1 : 0);
            int maxSettingScroll = Math.max(0, totalRows - VISIBLE_ROWS);
            if (settingScroll > maxSettingScroll) {
                settingScroll = maxSettingScroll;
            }
            if (settingScroll < 0) {
                settingScroll = 0;
            }

            int drawn = 0;
            for (int rowIndex = settingScroll; rowIndex < totalRows && drawn < VISIBLE_ROWS; rowIndex++, drawn++) {
                int y = TOP + drawn * ROW;
                if (module.canDisable() && rowIndex == 0) {
                    this.buttons.add(new ButtonWidget(ID_TOGGLE, rightX, y, 168, 20,
                            module.isEnabled() ? "Enabled: ON" : "Enabled: OFF"));
                    continue;
                }
                int j = module.canDisable() ? rowIndex - 1 : rowIndex;
                Setting setting = settings.get(j);
                if (setting instanceof PositionSetting) {
                    this.buttons.add(new ButtonWidget(ID_EDIT_HUD, rightX, y, 168, 20, "Position: edit HUD..."));
                } else if (setting instanceof NumberSetting) {
                    this.buttons.add(new SettingSlider(ID_SETTING_BASE + j, rightX, y, 168, 20,
                            (NumberSetting) setting));
                } else {
                    this.buttons.add(new ButtonWidget(ID_SETTING_BASE + j, rightX, y, 168, 20,
                            setting.name() + ": " + setting.valueLabel()));
                }
            }
            if (totalRows > VISIBLE_ROWS) {
                this.buttons.add(new ButtonWidget(ID_SET_UP, rightX + 172, TOP, 20, 20, "^"));
                this.buttons.add(new ButtonWidget(ID_SET_DOWN, rightX + 172, TOP + (drawn - 1) * ROW, 20, 20, "v"));
            }
        }

        this.buttons.add(new ButtonWidget(ID_PROFILES, this.width / 2 - 176, this.height - 28, 110, 20,
                "Profile: " + ProfileStore.active()));
        this.buttons.add(new ButtonWidget(ID_RELOAD, this.width / 2 - 62, this.height - 28, 110, 20, "Reload"));
        this.buttons.add(new ButtonWidget(ID_DONE, this.width / 2 + 52, this.height - 28, 128, 20, "Done"));
    }

    private void clampScroll(int count) {
        int maxScroll = Math.max(0, count - VISIBLE_ROWS);
        if (moduleScroll > maxScroll) {
            moduleScroll = maxScroll;
        }
        if (moduleScroll < 0) {
            moduleScroll = 0;
        }
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

        // A slider is adjusted by dragging, not by clicking through; only refresh its label.
        if (button instanceof SettingSlider) {
            ModuleRegistry.save();
            return;
        }
        cancelCapture();

        if (button.id == ID_DONE) {
            ModuleRegistry.save();
            this.client.setScreen(null);
            return;
        }
        if (button.id == ID_RELOAD) {
            // Discards unsaved fiddling and re-reads the file, including hand edits.
            ModuleRegistry.reload();
            this.init();
            return;
        }
        if (button.id == ID_PROFILES) {
            ModuleRegistry.save();
            this.client.setScreen(new ProfilesScreen());
            return;
        }
        if (button.id == ID_EDIT_HUD) {
            ModuleRegistry.save();
            this.client.setScreen(new HudEditorScreen());
            return;
        }
        if (button.id == ID_SCROLL_UP) {
            moduleScroll--;
        } else if (button.id == ID_SCROLL_DOWN) {
            moduleScroll++;
        } else if (button.id == ID_SET_UP) {
            settingScroll--;
        } else if (button.id == ID_SET_DOWN) {
            settingScroll++;
        } else if (button.id == ID_TOGGLE && selected < modules.size()) {
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
            settingScroll = 0;
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
            this.textRenderer.draw(modules.get(selected).description(), this.width / 2 + 12, 32, 0xA0A0A0);
        }
        if (capturing != null) {
            this.drawCenteredString(this.textRenderer,
                    "Press a key to bind, or Escape to clear", this.width / 2, this.height - 44, 0xFFD54F);
        } else if (!otherMods.isEmpty()) {
            // Everything else the launcher installed. We can list it, but it is configured in the
            // launcher (or by its own author), so it is shown read-only rather than faked as editable.
            this.textRenderer.draw("Also loaded: " + join(otherMods),
                    this.width / 2 - 176, this.height - 44, 0x707070);
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
        Module clientModule = ModuleRegistry.byId("client");
        int menuKey = clientModule instanceof ClientModule
                ? ((ClientModule) clientModule).menuKey().keyCode()
                : KeybindSetting.NONE;
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
}
