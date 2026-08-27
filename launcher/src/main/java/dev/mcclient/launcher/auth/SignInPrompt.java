package dev.mcclient.launcher.auth;

/**
 * How the device-code sign-in instructions reach the user. The console launcher prints them; the
 * GUI shows a dialog with a copy button. The auth code itself stays unaware of either.
 */
@FunctionalInterface
public interface SignInPrompt {

    /**
     * Called once, as soon as Microsoft issues a code. Must not block for the whole sign-in --
     * polling starts the moment this returns.
     */
    void show(String verificationUri, String userCode);

    /** Called when polling ends, so a GUI can dismiss whatever {@link #show} put on screen. */
    default void dismiss() {}

    SignInPrompt CONSOLE = (uri, code) -> {
        System.out.println();
        System.out.println("  Go to: " + uri);
        System.out.println("  Enter code: " + code);
        System.out.println();
    };
}
