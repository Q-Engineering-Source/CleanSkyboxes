package dev.hoshno.neoforgeskyboxes.skyboxes;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.util.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/** One atlas animation layer used by the FabricSkyBoxes multi-texture type. */
public final class TextureAnimation {
    private final ResourceLocation texture;
    private final float[] uvRange;
    private final int gridColumns;
    private final int gridRows;
    private final int durationMillis;
    private final Map<String, Integer> frameDurations;

    private int frameIndex;
    private long nextFrameAt;
    private float[] currentFrame;

    private TextureAnimation(ResourceLocation texture, float[] uvRange, int gridColumns, int gridRows,
                             int durationMillis, Map<String, Integer> frameDurations) {
        this.texture = texture;
        this.uvRange = uvRange;
        this.gridColumns = gridColumns;
        this.gridRows = gridRows;
        this.durationMillis = durationMillis;
        this.frameDurations = frameDurations;
    }

    public static TextureAnimation parse(JsonObject json) {
        String textureName = requiredString(json, "texture");
        JsonObject uv = json.has("uvRanges") && json.get("uvRanges").isJsonObject()
                ? json.getAsJsonObject("uvRanges") : new JsonObject();
        float[] range = new float[]{
                number(uv, "minU", 0.0F), number(uv, "minV", 0.0F),
                number(uv, "maxU", 1.0F), number(uv, "maxV", 1.0F)
        };
        int columns = Math.max(1, requiredInt(json, "gridColumns"));
        int rows = Math.max(1, requiredInt(json, "gridRows"));
        int duration = Math.max(1, requiredInt(json, "duration"));
        Map<String, Integer> durations = new HashMap<>();
        if (json.has("frameDuration") && json.get("frameDuration").isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject("frameDuration").entrySet()) {
                durations.put(entry.getKey(), Math.max(1, entry.getValue().getAsInt()));
            }
        }
        return new TextureAnimation(new ResourceLocation(textureName), range, columns, rows, duration, durations);
    }

    public ResourceLocation getTexture() {
        return this.texture;
    }

    public float[] getUvRange() {
        return this.uvRange.clone();
    }

    public float[] getCurrentFrame(long nowMillis) {
        if (this.nextFrameAt <= nowMillis) {
            this.frameIndex = (this.frameIndex + 1) % (this.gridRows * this.gridColumns);
            this.currentFrame = this.calculateFrame(this.frameIndex);
            this.nextFrameAt = nowMillis + this.frameDurations.getOrDefault(
                    String.valueOf(this.frameIndex + 1), this.durationMillis);
        }
        return this.currentFrame == null ? null : this.currentFrame.clone();
    }

    private float[] calculateFrame(int index) {
        float frameWidth = 1.0F / this.gridColumns;
        float frameHeight = 1.0F / this.gridRows;
        float minU = (float) (index / this.gridRows) * frameWidth;
        float minV = (float) (index % this.gridRows) * frameHeight;
        return new float[]{minU, minV, minU + frameWidth, minV + frameHeight};
    }

    private static String requiredString(JsonObject json, String key) {
        if (!json.has(key) || !json.get(key).isJsonPrimitive()) {
            throw new IllegalArgumentException("Missing string '" + key + "'");
        }
        return json.get(key).getAsString();
    }

    private static int requiredInt(JsonObject json, String key) {
        if (!json.has(key) || !json.get(key).isJsonPrimitive()) {
            throw new IllegalArgumentException("Missing integer '" + key + "'");
        }
        return json.get(key).getAsInt();
    }

    private static float number(JsonObject json, String key, float fallback) {
        return json.has(key) ? json.get(key).getAsFloat() : fallback;
    }
}
