package dev.mcclient.hud;

/** Small display helpers shared by the HUD modules. Pure, so they're covered by tests. */
public final class Format {

    private static final String[] ROMAN = {"", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X"};

    private Format() {}

    /** Ticks to "m:ss", or "0:0X" style seconds. Effects below a second still read "0:01". */
    public static String ticksToTime(int ticks) {
        if (ticks <= 0) {
            return "0:00";
        }
        int totalSeconds = (ticks + 19) / 20;
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return minutes + ":" + (seconds < 10 ? "0" : "") + seconds;
    }

    /**
     * Potion amplifier to the numeral players expect: amplifier 0 is level I and is written
     * without a numeral, matching how the game itself labels "Speed" vs "Speed II".
     */
    public static String amplifier(int amplifier) {
        if (amplifier <= 0) {
            return "";
        }
        return amplifier < ROMAN.length ? ROMAN[amplifier] : String.valueOf(amplifier + 1);
    }

    /**
     * Turns a vanilla effect translation key into something readable without a language file:
     * "potion.moveSpeed" becomes "Move Speed".
     */
    public static String prettifyKey(String translationKey) {
        if (translationKey == null || translationKey.isEmpty()) {
            return "Effect";
        }
        String tail = translationKey;
        int dot = tail.lastIndexOf('.');
        if (dot >= 0 && dot < tail.length() - 1) {
            tail = tail.substring(dot + 1);
        }
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < tail.length(); i++) {
            char c = tail.charAt(i);
            if (i == 0) {
                out.append(Character.toUpperCase(c));
            } else if (Character.isUpperCase(c)) {
                out.append(' ').append(c);
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    /** Fuse ticks to a one-decimal countdown, e.g. "2.4s". */
    public static String fuse(int ticks) {
        if (ticks < 0) {
            return "0.0s";
        }
        return String.format("%.1fs", ticks / 20.0f);
    }
}
