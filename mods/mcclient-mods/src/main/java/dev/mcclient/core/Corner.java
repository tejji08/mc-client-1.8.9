package dev.mcclient.core;

/** Shared helper for anchoring a HUD panel to a screen corner. */
public final class Corner {

    public static final String[] NAMES = {"Top Left", "Top Right", "Bottom Left", "Bottom Right"};

    private Corner() {}

    public static int x(int cornerIndex, int screenWidth, int panelWidth, int margin) {
        boolean right = cornerIndex == 1 || cornerIndex == 3;
        return right ? screenWidth - panelWidth - margin : margin;
    }

    public static int y(int cornerIndex, int screenHeight, int panelHeight, int margin) {
        boolean bottom = cornerIndex == 2 || cornerIndex == 3;
        return bottom ? screenHeight - panelHeight - margin : margin;
    }
}
