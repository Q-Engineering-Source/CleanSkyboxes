package dev.hoshno.neoforgeskyboxes.skyboxes;

import com.google.gson.JsonObject;

/** Blend factors and alpha modulation from a skybox's blend object. */
public final class BlendSettings {
    private final String type;
    private final boolean custom;
    private final boolean separateFunction;
    private final int sourceFactor;
    private final int destinationFactor;
    private final int equation;
    private final int sourceFactorAlpha;
    private final int destinationFactorAlpha;
    private final boolean redAlphaEnabled;
    private final boolean greenAlphaEnabled;
    private final boolean blueAlphaEnabled;
    private final boolean alphaEnabled;

    private BlendSettings(String type, boolean custom, boolean separateFunction, int sourceFactor,
                          int destinationFactor, int equation, int sourceFactorAlpha,
                          int destinationFactorAlpha, boolean redAlphaEnabled,
                          boolean greenAlphaEnabled, boolean blueAlphaEnabled, boolean alphaEnabled) {
        this.type = type;
        this.custom = custom;
        this.separateFunction = separateFunction;
        this.sourceFactor = sourceFactor;
        this.destinationFactor = destinationFactor;
        this.equation = equation;
        this.sourceFactorAlpha = sourceFactorAlpha;
        this.destinationFactorAlpha = destinationFactorAlpha;
        this.redAlphaEnabled = redAlphaEnabled;
        this.greenAlphaEnabled = greenAlphaEnabled;
        this.blueAlphaEnabled = blueAlphaEnabled;
        this.alphaEnabled = alphaEnabled;
    }

    public static BlendSettings from(JsonObject blendObject, String fallbackType) {
        String type = blendObject != null && blendObject.has("type")
                ? blendObject.get("type").getAsString() : fallbackType;
        if ("decorations".equals(type)) {
            return new BlendSettings(type, true, true, 770, 1, 32774, 1, 0,
                    false, false, false, true);
        }
        JsonObject blender = blendObject != null && blendObject.has("blender")
                && blendObject.get("blender").isJsonObject() ? blendObject.getAsJsonObject("blender") : null;
        boolean custom = "custom".equals(type);
        if (!custom) {
            return new BlendSettings(type, false, false, 770, 771, 32774, 1, 0,
                    false, false, false, true);
        }

        boolean separate = bool(blender, "separateFunction", false);
        int source = integer(blender, "sourceFactor", 770);
        int destination = integer(blender, "destinationFactor", 1);
        int equation = integer(blender, "equation", 32774);
        int sourceAlpha = integer(blender, "sourceFactorAlpha", 0);
        int destinationAlpha = integer(blender, "destinationFactorAlpha", 0);
        boolean redAlpha = bool(blender, "redAlphaEnabled", false);
        boolean greenAlpha = bool(blender, "greenAlphaEnabled", false);
        boolean blueAlpha = bool(blender, "blueAlphaEnabled", false);
        boolean alpha = bool(blender, "alphaEnabled", true);
        if (!isValidFactor(source) || !isValidFactor(destination) || !isValidEquation(equation)
                || (separate && (!isValidFactor(sourceAlpha) || !isValidFactor(destinationAlpha)))) {
            return new BlendSettings("", false, false, 770, 771, 32774, 1, 0,
                    false, false, false, true);
        }
        return new BlendSettings(type, true, separate, source, destination, equation,
                sourceAlpha, destinationAlpha, redAlpha, greenAlpha, blueAlpha, alpha);
    }

    private static boolean isValidFactor(int value) {
        switch (value) {
            case 0: case 1: case 768: case 769: case 770: case 771: case 772: case 773:
            case 774: case 775: case 776: case 32769: case 32770: case 32771: case 32772:
                return true;
            default:
                return false;
        }
    }

    private static boolean isValidEquation(int value) {
        return value == 32774 || value == 32778 || value == 32779 || value == 32775 || value == 32776;
    }

    private static boolean bool(JsonObject json, String key, boolean fallback) {
        return json != null && json.has(key) ? json.get(key).getAsBoolean() : fallback;
    }

    private static int integer(JsonObject json, String key, int fallback) {
        return json != null && json.has(key) ? json.get(key).getAsInt() : fallback;
    }

    public String getType() { return this.type; }
    public boolean isCustom() { return this.custom; }
    public boolean isSeparateFunction() { return this.separateFunction; }
    public int getSourceFactor() { return this.sourceFactor; }
    public int getDestinationFactor() { return this.destinationFactor; }
    public int getEquation() { return this.equation; }
    public int getSourceFactorAlpha() { return this.sourceFactorAlpha; }
    public int getDestinationFactorAlpha() { return this.destinationFactorAlpha; }
    public boolean isRedAlphaEnabled() { return this.redAlphaEnabled; }
    public boolean isGreenAlphaEnabled() { return this.greenAlphaEnabled; }
    public boolean isBlueAlphaEnabled() { return this.blueAlphaEnabled; }
    public boolean isAlphaEnabled() { return this.alphaEnabled; }
}
