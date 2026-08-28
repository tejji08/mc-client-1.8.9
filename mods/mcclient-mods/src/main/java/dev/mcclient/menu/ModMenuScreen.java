package dev.mcclient.menu;

import dev.mcclient.core.Module;
import dev.mcclient.core.ModuleRegistry;
import dev.mcclient.core.Setting;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * The Right Shift menu: modules on the left, the selected module's settings on the right.
 *
 * <p>Built from plain {@link ButtonWidget}s so it looks and behaves like a vanilla screen. 1.8.9
 * has no slider or checkbox widget, which is why every setting is a button that cycles to its next
 * value -- the same trick the vanilla Options screen uses for its own toggles.
 */
public final class ModMenuScreen extends Screen {

    private static final int ID_DONE = 900;
    private static final int ID_TOGGLE = 800;
    private static final int ID_SETTING_BASE = 200;

    /** Mods we did not write, so the list of "everything installed" is honest about what it can edit. */
    private static final List<String> IGNORED_IDS =
            java.util.Arrays.asList("minecraft", "java", "fabricloader", "mcclient-mods");

    private int selected;
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
            this.buttons.add(new ButtonWidget(ID_TOGGLE, rightX, top, 160, 20,
                    module.isEnabled() ? "Enabled: ON" : "Enabled: OFF"));
            List<Setting> settings = module.settings();
            for (int j = 0; j < settings.size(); j++) {
                Setting setting = settings.get(j);
                this.buttons.add(new ButtonWidget(ID_SETTING_BASE + j, rightX, top + 22 + j * 22, 160, 20,
                        setting.name() + ": " + setting.valueLabel()));
            }
        }

        this.buttons.add(new ButtonWidget(ID_DONE, this.width / 2 - 100, this.height - 28, 200, 20, "Done"));
    }

    private String label(Module module, int index) {
        String mark = index == selected ? "> " : "  ";
        return mark + module.name() + (module.isEnabled() ? "" : " (off)");
    }

    @Override
    protected void buttonClicked(ButtonWidget button) {
        if (!button.active) {
            return;
        }
        List<Module> modules = ModuleRegistry.modules();

        if (button.id == ID_DONE) {
            this.client.setScreen(null);
            return;
        }
        if (button.id == ID_TOGGLE && selected < modules.size()) {
            Module module = modules.get(selected);
            module.setEnabled(!module.isEnabled());
        } else if (button.id >= ID_SETTING_BASE && selected < modules.size()) {
            List<Setting> settings = modules.get(selected).settings();
            int index = button.id - ID_SETTING_BASE;
            if (index < settings.size()) {
                settings.get(index).cycle();
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
            this.textRenderer.draw(modules.get(selected).description(),
                    this.width / 2 + 10, 32, 0xA0A0A0);
        }

        // Everything else the launcher installed. We can list it, but it is configured in the
        // launcher (or by its own author), so it is shown read-only rather than faked as editable.
        int y = this.height - 52;
        if (!otherMods.isEmpty()) {
            this.textRenderer.draw("Also loaded: " + join(otherMods), this.width / 2 - 170, y, 0x707070);
        }

        super.render(mouseX, mouseY, delta);
    }

    @Override
    protected void keyPressed(char character, int keyCode) {
        // Escape (1) and Right Shift (54) both close, so the key that opened it also shuts it.
        if (keyCode == 1 || keyCode == 54) {
            ModuleRegistry.save();
            this.client.setScreen(null);
            return;
        }
        super.keyPressed(character, keyCode);
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
