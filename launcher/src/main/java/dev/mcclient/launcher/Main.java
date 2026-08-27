package dev.mcclient.launcher;

import dev.mcclient.launcher.auth.SessionResolver;
import dev.mcclient.launcher.auth.model.MinecraftSession;
import dev.mcclient.launcher.gui.LauncherWindow;
import dev.mcclient.launcher.mods.ModManager;

import java.net.http.HttpClient;

/**
 * Entry point. Opens the launcher window by default; pass {@code --cli} to run the old
 * headless path instead, which is what the automated checks and scripts use.
 */
public final class Main {

    public static void main(String[] args) throws Exception {
        boolean cli = false;
        for (String arg : args) {
            if (arg.equals("--cli") || arg.equals("--nogui")) {
                cli = true;
            }
        }

        if (cli) {
            runHeadless();
        } else {
            LauncherWindow.open();
        }
    }

    private static void runHeadless() throws Exception {
        HttpClient http = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build();
        ModManager mods = new ModManager(http);
        MinecraftSession session = new SessionResolver(http).resolve();
        Process game = new GameLauncher(http, mods).launch(session, new LauncherSettings(), Progress.CONSOLE);
        game.waitFor();
    }
}
