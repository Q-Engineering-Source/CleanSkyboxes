package dev.hoshno.neoforgeskyboxes.skyboxes;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.hoshno.neoforgeskyboxes.NeoforgeSkyboxes;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.Potion;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** A decoded 1.12.2 skybox definition with the original JSON kept for forward porting. */
public final class SkyboxDefinition {
    private static final int MAX_SUPPORTED_SCHEMA = 2;

    public enum Type {
        MONOCOLOR,
        END,
        OVERWORLD,
        SQUARE_TEXTURED,
        SINGLE_SPRITE_SQUARE_TEXTURED,
        ANIMATED_SQUARE_TEXTURED,
        SINGLE_SPRITE_ANIMATED_SQUARE_TEXTURED,
        MULTI_TEXTURE
    }

    private final ResourceLocation id;
    private final int schemaVersion;
    private final ResourceLocation typeId;
    private final Type type;
    private final JsonObject payload;

    private final int priority;
    private final boolean changeFog;
    private final boolean changeFogDensity;
    private final float fogRed;
    private final float fogGreen;
    private final float fogBlue;
    private final float fogDensity;
    private final float minAlpha;
    private final float maxAlpha;
    private final int transitionInDuration;
    private final int transitionOutDuration;
    private final int startFadeIn;
    private final int endFadeIn;
    private final int startFadeOut;
    private final int endFadeOut;
    private final boolean alwaysOn;

    private final float red;
    private final float green;
    private final float blue;
    private final float colorAlpha;
    private final BlendSettings blendSettings;
    private final ResourceLocation[] textures;
    private final ResourceLocation[][] textureFrames;
    private final List<TextureAnimation> textureAnimations;
    private final long animationStartedAtMillis;
    private final long animationFrameMillis;
    private final float[][] textureUvs;
    private final boolean showSun;
    private final boolean showMoon;
    private final boolean showStars;
    private final ResourceLocation sunTexture;
    private final ResourceLocation moonTexture;
    private final BlendSettings decorationBlendSettings;

    private final Set<ResourceLocation> biomes;
    private final boolean biomeInclusion;
    private final Set<ResourceLocation> dimensions;
    private final Set<ResourceLocation> worlds;
    private final Set<ResourceLocation> effects;
    private final Set<String> weather;
    private final List<ValueRange> xRanges;
    private final List<ValueRange> yRanges;
    private final List<ValueRange> zRanges;
    private final double loopDays;
    private final List<ValueRange> loopRanges;

    private final boolean skyboxRotation;
    private final float[] staticRotation;
    private final float[] axisRotation;
    private final int[] timeShift;
    private final float[] rotationSpeed;
    private final boolean optifineLayer;
    private final Set<String> optifineWeathers;
    private final boolean optifineRotate;
    private final float optifineSpeed;
    private final float[] optifineAxis;
    private final boolean decorationSkyboxRotation;
    private final float[] decorationStaticRotation;
    private final float[] decorationAxisRotation;
    private final int[] decorationTimeShift;
    private final float[] decorationRotationSpeed;

    private int lastTime = -2;
    private float conditionAlpha;
    private float alpha;

    private SkyboxDefinition(ResourceLocation id, int schemaVersion, ResourceLocation typeId, Type type,
                            JsonObject payload, JsonObject properties, JsonObject conditions, JsonObject decorations,
                            float red, float green, float blue, float colorAlpha, BlendSettings blendSettings,
                            ResourceLocation[] textures, float[][] textureUvs, List<TextureAnimation> textureAnimations,
                            List<ResourceLocation[]> textureFrames, float fps) {
        this.id = id;
        this.schemaVersion = schemaVersion;
        this.typeId = typeId;
        this.type = type;
        this.payload = payload.deepCopy();

        this.priority = getInt(properties, "priority", 0);
        this.changeFog = getBoolean(properties, "changeFog", false);
        this.changeFogDensity = getBoolean(properties, "changeFogDensity", false);
        this.minAlpha = clamp(getFloat(properties, "minAlpha", 0.0F), 0.0F, 1.0F);
        this.maxAlpha = clamp(getFloat(properties, "maxAlpha", 1.0F), this.minAlpha, 1.0F);
        if (schemaVersion == 1) {
            float transitionSpeed = getFloat(properties, "transitionSpeed", 0.05F);
            int legacyDuration = transitionSpeed > 0.0F ? (int) (this.maxAlpha / transitionSpeed) : 0;
            this.transitionInDuration = Math.max(0, legacyDuration);
            this.transitionOutDuration = Math.max(0, legacyDuration);
        } else {
            this.transitionInDuration = Math.max(0, getInt(properties, "transitionInDuration", 20));
            this.transitionOutDuration = Math.max(0, getInt(properties, "transitionOutDuration", 20));
        }

        JsonObject fade = getObject(properties, "fade");
        if (schemaVersion == 1) {
            fade = properties;
        }
        this.startFadeIn = normalizeTime(getLong(fade, "startFadeIn", 0L));
        this.endFadeIn = normalizeTime(getLong(fade, "endFadeIn", 0L));
        this.startFadeOut = normalizeTime(getLong(fade, "startFadeOut", 0L));
        this.endFadeOut = normalizeTime(getLong(fade, "endFadeOut", 0L));
        this.alwaysOn = getBoolean(fade, "alwaysOn", false);

        this.red = clamp(red, 0.0F, 1.0F);
        this.green = clamp(green, 0.0F, 1.0F);
        this.blue = clamp(blue, 0.0F, 1.0F);
        this.colorAlpha = clamp(colorAlpha, 0.0F, 1.0F);
        this.blendSettings = blendSettings;
        this.textures = textures == null ? new ResourceLocation[0] : textures.clone();
        this.textureFrames = copyTextureFrames(textureFrames, this.textures);
        this.textureAnimations = textureAnimations == null ? new ArrayList<>() : new ArrayList<>(textureAnimations);
        this.animationStartedAtMillis = System.currentTimeMillis();
        this.animationFrameMillis = fps > 0.0F && fps <= 360.0F ? (long) (1000.0F / fps) : 16L;
        this.textureUvs = copyUvs(textureUvs == null ? fullTextureUvs() : textureUvs);

        JsonObject fogColors = getObject(properties, "fogColors");
        if (schemaVersion == 1) {
            this.fogRed = clamp(getFloat(properties, "fogRed", 0.0F), 0.0F, 1.0F);
            this.fogGreen = clamp(getFloat(properties, "fogGreen", 0.0F), 0.0F, 1.0F);
            this.fogBlue = clamp(getFloat(properties, "fogBlue", 0.0F), 0.0F, 1.0F);
            this.fogDensity = 1.0F;
        } else {
            this.fogRed = clamp(getFloat(fogColors, "red", 0.0F), 0.0F, 1.0F);
            this.fogGreen = clamp(getFloat(fogColors, "green", 0.0F), 0.0F, 1.0F);
            this.fogBlue = clamp(getFloat(fogColors, "blue", 0.0F), 0.0F, 1.0F);
            this.fogDensity = clamp(getFloat(fogColors, "alpha", 0.0F), 0.0F, 1.0F);
        }

        this.showSun = getBoolean(decorations, "showSun", false);
        this.showMoon = getBoolean(decorations, "showMoon", false);
        this.showStars = getBoolean(decorations, "showStars", false);
        this.sunTexture = parseLocation(getString(decorations, "sun", "textures/environment/sun.png"));
        this.moonTexture = parseLocation(getString(decorations, "moon", "textures/environment/moon_phases.png"));
        JsonObject decorationBlend = getObject(decorations, "blend");
        this.decorationBlendSettings = BlendSettings.from(decorationBlend, decorationBlend == null ? "decorations" : "");

        this.biomes = readLocations(conditions, "biomes");
        this.biomeInclusion = getBoolean(payload, "biomeInclusion", true);
        this.dimensions = readLocations(conditions, "dimensions");
        this.worlds = readLocations(conditions, "worlds");
        if (schemaVersion == 1 && this.worlds.isEmpty()) {
            this.worlds.addAll(readLocations(conditions, "dimensions"));
        }
        this.effects = readLocations(conditions, "effects");
        this.weather = readStrings(conditions, "weather");
        this.xRanges = readRanges(conditions, "xRanges", null);
        this.yRanges = readRanges(conditions, "yRanges", schemaVersion == 1 ? "heightRanges" : null);
        this.zRanges = readRanges(conditions, "zRanges", null);

        JsonObject loop = getObject(conditions, "loop");
        this.loopDays = Math.max(1.0D, getDouble(loop, "days", 7.0D));
        this.loopRanges = readRanges(loop, "ranges", null);

        JsonObject rotation = schemaVersion == 1 ? conditions : getObject(properties, "rotation");
        this.skyboxRotation = getBoolean(rotation, "skyboxRotation", true);
        this.staticRotation = schemaVersion == 1 ? new float[]{0, 0, 0} : readVector(rotation, "static", 0.0F);
        this.axisRotation = readVector(rotation, "axis", 0.0F);
        this.timeShift = readIntVector(rotation, "timeShift");
        this.rotationSpeed = new float[]{
                getFloat(rotation, "rotationSpeedX", 0.0F),
                getFloat(rotation, "rotationSpeedY", 0.0F),
                getFloat(rotation, "rotationSpeedZ", 0.0F)
        };

        JsonObject optifine = getObject(payload, "optifine");
        this.optifineLayer = optifine != null;
        this.optifineWeathers = readStrings(optifine, "weathers");
        this.optifineRotate = getBoolean(optifine, "rotate", false);
        this.optifineSpeed = getFloat(optifine, "speed", 1.0F);
        this.optifineAxis = readVector(optifine, "axis", new float[]{1.0F, 0.0F, 0.0F});

        JsonObject decorationRotation = getObject(decorations, "rotation");
        boolean hasDecorations = decorations != null;
        this.decorationSkyboxRotation = getBoolean(decorationRotation, "skyboxRotation", !hasDecorations);
        this.decorationStaticRotation = readVector(decorationRotation, "static", 0.0F);
        this.decorationAxisRotation = readVector(decorationRotation, "axis", 0.0F);
        this.decorationTimeShift = readIntVector(decorationRotation, "timeShift");
        this.decorationRotationSpeed = new float[]{
                getFloat(decorationRotation, "rotationSpeedX", 0.0F),
                getFloat(decorationRotation, "rotationSpeedY", 0.0F),
                getFloat(decorationRotation, "rotationSpeedZ", hasDecorations ? 1.0F : 0.0F)
        };
    }

    public static SkyboxDefinition parse(ResourceLocation id, JsonObject json) {
        JsonElement versionElement = json.get("schemaVersion");
        JsonElement typeElement = json.get("type");
        if (versionElement == null || !versionElement.isJsonPrimitive()
                || !versionElement.getAsJsonPrimitive().isNumber()
                || typeElement == null || !typeElement.isJsonPrimitive()
                || !typeElement.getAsJsonPrimitive().isString()) {
            throw new IllegalArgumentException("Missing numeric 'schemaVersion' or string 'type'");
        }

        int schemaVersion = versionElement.getAsInt();
        if (schemaVersion < 1 || schemaVersion > MAX_SUPPORTED_SCHEMA) {
            throw new IllegalArgumentException("Unsupported schemaVersion " + schemaVersion);
        }

        ResourceLocation typeId = parseType(typeElement.getAsString());
        if (typeId == null || !NeoforgeSkyboxes.FABRIC_SKYBOXES_NAMESPACE.equals(typeId.getNamespace())) {
            throw new IllegalArgumentException("Unsupported skybox type '" + typeElement.getAsString() + "'");
        }
        Type type;
        switch (typeId.getPath()) {
            case "monocolor":
            case "mono_color":
                type = Type.MONOCOLOR;
                break;
            case "end":
                type = Type.END;
                break;
            case "overworld":
                type = Type.OVERWORLD;
                break;
            case "square_textured":
                type = Type.SQUARE_TEXTURED;
                break;
            case "single_sprite_square_textured":
                type = Type.SINGLE_SPRITE_SQUARE_TEXTURED;
                break;
            case "animated_square_textured":
                type = Type.ANIMATED_SQUARE_TEXTURED;
                break;
            case "single_sprite_animated_square_textured":
                type = Type.SINGLE_SPRITE_ANIMATED_SQUARE_TEXTURED;
                break;
            case "multi_texture":
                type = Type.MULTI_TEXTURE;
                break;
            default:
                throw new IllegalArgumentException("Skybox type is not ported yet: " + typeId);
        }

        JsonObject properties = schemaVersion == 1 ? json : getObject(json, "properties");
        if (properties == null) {
            throw new IllegalArgumentException("Missing 'properties' object");
        }
        JsonObject conditions = schemaVersion == 1 ? json : getObject(json, "conditions");
        if (conditions == null) {
            conditions = new JsonObject();
        }

        float red = 1.0F;
        float green = 1.0F;
        float blue = 1.0F;
        float colorAlpha = 1.0F;
        String blend = "";
        ResourceLocation[] textures = null;
        float[][] textureUvs = null;
        List<ResourceLocation[]> textureFrames = new ArrayList<>();
        List<TextureAnimation> textureAnimations = new ArrayList<>();
        float fps = 20.0F;

        if (type == Type.MONOCOLOR) {
            if (schemaVersion == 1) {
                red = getRequiredFloat(json, "red");
                green = getRequiredFloat(json, "green");
                blue = getRequiredFloat(json, "blue");
                colorAlpha = getFloat(json, "alpha", 1.0F);
                blend = getBoolean(json, "shouldBlend", false) ? "add" : "";
            } else {
                JsonObject color = getObject(json, "color");
                if (color == null) {
                    red = green = blue = colorAlpha = 0.0F;
                } else {
                    red = getRequiredFloat(color, "red");
                    green = getRequiredFloat(color, "green");
                    blue = getRequiredFloat(color, "blue");
                    colorAlpha = getFloat(color, "alpha", 1.0F);
                }
                blend = getString(getObject(json, "blend"), "type", "");
            }
        } else if (type == Type.END || type == Type.OVERWORLD) {
            if (schemaVersion == 1) {
                throw new IllegalArgumentException("dimension skyboxes require schemaVersion 2");
            }
        } else if (type == Type.SQUARE_TEXTURED) {
            JsonObject textureObject = schemaVersion == 1 ? json : getObject(json, "textures");
            if (textureObject == null) {
                throw new IllegalArgumentException("Missing 'textures' object");
            }
            String[] keys = schemaVersion == 1
                    ? new String[]{"texture_bottom", "texture_north", "texture_south", "texture_top", "texture_east", "texture_west"}
                    : new String[]{"bottom", "north", "south", "top", "east", "west"};
            textures = new ResourceLocation[6];
            for (int index = 0; index < keys.length; index++) {
                textures[index] = parseLocation(getRequiredString(textureObject, keys[index]));
            }
            textureUvs = fullTextureUvs();
            textureFrames.add(textures.clone());
            if (schemaVersion == 1) {
                blend = getBoolean(json, "shouldBlend", false) ? "add" : "";
            } else {
                blend = getString(getObject(json, "blend"), "type", "");
            }
        } else if (type == Type.SINGLE_SPRITE_SQUARE_TEXTURED) {
            if (schemaVersion == 1) {
                throw new IllegalArgumentException("single-sprite skyboxes require schemaVersion 2");
            }
            ResourceLocation texture = parseLocation(getRequiredString(json, "texture"));
            textures = new ResourceLocation[]{texture, texture, texture, texture, texture, texture};
            textureUvs = singleSpriteTextureUvs();
            textureFrames.add(textures.clone());
            blend = getString(getObject(json, "blend"), "type", "");
        } else if (type == Type.ANIMATED_SQUARE_TEXTURED) {
            if (schemaVersion == 1) {
                throw new IllegalArgumentException("animated skyboxes require schemaVersion 2");
            }
            JsonElement animationElement = json.get("animationTextures");
            if (animationElement == null || !animationElement.isJsonArray() || animationElement.getAsJsonArray().size() == 0) {
                throw new IllegalArgumentException("Missing non-empty 'animationTextures' array");
            }
            for (JsonElement frame : animationElement.getAsJsonArray()) {
                if (!frame.isJsonObject()) {
                    throw new IllegalArgumentException("Animated square frame must be a texture object");
                }
                textureFrames.add(parseSixTextures(frame.getAsJsonObject()));
            }
            textures = textureFrames.get(0).clone();
            textureUvs = fullTextureUvs();
            fps = getRequiredFloat(json, "fps");
            blend = getString(getObject(json, "blend"), "type", "");
        } else if (type == Type.MULTI_TEXTURE) {
            if (schemaVersion == 1) {
                throw new IllegalArgumentException("multi-texture skyboxes require schemaVersion 2");
            }
            JsonElement animations = json.get("animations");
            if (animations != null && animations.isJsonArray()) {
                for (JsonElement animation : animations.getAsJsonArray()) {
                    if (!animation.isJsonObject()) {
                        throw new IllegalArgumentException("Multi-texture animation must be an object");
                    }
                    textureAnimations.add(TextureAnimation.parse(animation.getAsJsonObject()));
                }
            }
            blend = getString(getObject(json, "blend"), "type", "");
        } else {
            if (schemaVersion == 1) {
                throw new IllegalArgumentException("animated skyboxes require schemaVersion 2");
            }
            JsonElement animationElement = json.get("animationTextures");
            if (animationElement == null || !animationElement.isJsonArray() || animationElement.getAsJsonArray().size() == 0) {
                throw new IllegalArgumentException("Missing non-empty 'animationTextures' array");
            }
            for (JsonElement frame : animationElement.getAsJsonArray()) {
                ResourceLocation texture = parseLocation(frame.getAsString());
                textureFrames.add(new ResourceLocation[]{texture, texture, texture, texture, texture, texture});
            }
            textures = textureFrames.get(0).clone();
            textureUvs = singleSpriteTextureUvs();
            fps = getRequiredFloat(json, "fps");
            blend = getString(getObject(json, "blend"), "type", "");
        }

        JsonObject decorations = schemaVersion == 1 ? null : getObject(json, "decorations");
        BlendSettings blendSettings = BlendSettings.from(
                schemaVersion == 1 ? null : getObject(json, "blend"), blend);
        return new SkyboxDefinition(id, schemaVersion, typeId, type, json,
                properties, conditions, decorations, red, green, blue, colorAlpha, blendSettings,
                textures, textureUvs, textureAnimations, textureFrames, fps);
    }

    public boolean tick(Minecraft minecraft) {
        if (minecraft.world == null || minecraft.player == null) {
            this.alpha = 0.0F;
            return false;
        }

        World world = minecraft.world;
        int currentTime = normalizeTime(world.getWorldTime());
        boolean conditionsMatch = this.matchesConditions(world, minecraft.player, currentTime);
        float conditionTarget = conditionsMatch ? 1.0F : 0.0F;
        int transitionDuration = conditionsMatch ? this.transitionInDuration : this.transitionOutDuration;
        if (!this.alwaysOn && this.lastTime != currentTime - 1 && this.lastTime != currentTime) {
            transitionDuration = NeoforgeSkyboxes.config().generalSettings.unexpectedTransitionDuration;
        }
        this.conditionAlpha = transition(this.conditionAlpha, conditionTarget, Math.max(0, transitionDuration));

        float timeAlpha = this.alwaysOn ? 1.0F : calculateFadeAlpha(currentTime);
        float optifineWeatherAlpha = this.optifineLayer ? calculateOptifineWeatherAlpha(world, minecraft.player) : 1.0F;
        this.alpha = clamp(timeAlpha * this.conditionAlpha * (this.maxAlpha - this.minAlpha) + this.minAlpha,
                this.minAlpha, this.maxAlpha) * optifineWeatherAlpha;
        this.lastTime = currentTime;
        return this.alpha > 0.0F;
    }

    private boolean matchesConditions(World world, EntityPlayer player, int currentTime) {
        ResourceLocation dimension = getDimensionLocation(world);
        int dimensionId = world.provider.getDimensionType().getId();
        if (!this.dimensions.isEmpty() && !matchesLocation(this.dimensions, dimension, dimensionId)) {
            return false;
        }
        if (!this.worlds.isEmpty() && !matchesLocation(this.worlds, dimension, dimensionId)) {
            return false;
        }
        if (!this.biomes.isEmpty()) {
            Biome biome = world.getBiome(player.getPosition());
            ResourceLocation biomeId = biome.getRegistryName();
            boolean matches = biomeId != null && matchesLocation(this.biomes, biomeId);
            if (matches != this.biomeInclusion) {
                return false;
            }
        }
        if (!this.effects.isEmpty()) {
            for (ResourceLocation effect : this.effects) {
                Potion potion = Potion.getPotionFromResourceLocation(effect.toString());
                if (potion != null && player.isPotionActive(potion)) {
                    return false;
                }
            }
        }
        if (!this.optifineLayer && !this.weather.isEmpty() && !matchesWeather(world, player, this.weather)) {
            return false;
        }
        if (!contains(this.xRanges, player.posX) || !contains(this.yRanges, player.posY) || !contains(this.zRanges, player.posZ)) {
            return false;
        }
        if (!this.loopRanges.isEmpty() && this.loopDays > 0.0D) {
            double loopTime = world.getWorldTime() - this.startFadeIn;
            while (loopTime < 0.0D) {
                loopTime += 24000.0D * this.loopDays;
            }
            double loopDay = (loopTime / 24000.0D) % this.loopDays;
            if (!contains(this.loopRanges, loopDay)) {
                return false;
            }
        }
        return true;
    }

    private static boolean matchesWeather(World world, EntityPlayer player, Set<String> weather) {
        Biome biome = world.getBiome(player.getPosition());
        boolean raining = world.isRaining();
        boolean thunder = world.isThundering();
        boolean snowyBiome = biome.isSnowyBiome();
        return (weather.contains("thunder") && thunder)
                || (weather.contains("rain") && raining && !thunder)
                || (weather.contains("snow") && raining && snowyBiome)
                || (weather.contains("rain_biome") && raining && !snowyBiome)
                || (weather.contains("clear") && !raining && !thunder);
    }

    private float calculateOptifineWeatherAlpha(World world, EntityPlayer player) {
        float rain = world.getRainStrength(1.0F);
        float thunder = world.getThunderStrength(1.0F);
        float alpha = this.optifineWeathers.contains("clear") ? 1.0F - rain : 0.0F;
        if (this.optifineWeathers.contains("thunder")) {
            alpha += thunder;
        }

        float rainOnly = Math.max(0.0F, rain - thunder);
        boolean snowyBiome = world.getBiome(player.getPosition()).isSnowyBiome();
        boolean precipitationMatches = snowyBiome
                ? this.optifineWeathers.contains("snow") || this.optifineWeathers.contains("rain")
                : this.optifineWeathers.contains("rain") || this.optifineWeathers.contains("rain_biome");
        if (precipitationMatches) {
            alpha += rainOnly;
        }
        return clamp(alpha, 0.0F, 1.0F);
    }

    private float calculateFadeAlpha(int currentTime) {
        if (isInInterval(currentTime, this.endFadeIn, this.startFadeOut)) {
            return this.maxAlpha;
        }
        if (isInInterval(currentTime, this.startFadeIn, this.endFadeIn)) {
            int duration = cyclicDistance(this.startFadeIn, this.endFadeIn);
            if (duration == 0) {
                return this.maxAlpha;
            }
            return this.minAlpha + (float) cyclicDistance(this.startFadeIn, currentTime) / duration * (this.maxAlpha - this.minAlpha);
        }
        if (isInInterval(currentTime, this.startFadeOut, this.endFadeOut)) {
            int duration = cyclicDistance(this.startFadeOut, this.endFadeOut);
            if (duration == 0) {
                return this.minAlpha;
            }
            return this.maxAlpha + (float) cyclicDistance(this.startFadeOut, currentTime) / duration * (this.minAlpha - this.maxAlpha);
        }
        return this.minAlpha;
    }

    private static float transition(float current, float target, int duration) {
        if (duration == 0) {
            return target;
        }
        float step = 1.0F / duration;
        return target > current ? Math.min(target, current + step) : Math.max(target, current - step);
    }

    private static boolean isInInterval(int value, int start, int end) {
        return start <= end ? value >= start && value <= end : value >= start || value <= end;
    }

    private static int cyclicDistance(int start, int end) {
        return (end - start + 24000) % 24000;
    }

    private static int normalizeTime(long time) {
        return (int) Math.floorMod(time, 24000L);
    }

    private static boolean matchesLocation(Set<ResourceLocation> choices, ResourceLocation actual, int dimensionId) {
        if (choices.contains(actual)) {
            return true;
        }
        for (ResourceLocation choice : choices) {
            if (choice.getPath().equals(Integer.toString(dimensionId))) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesLocation(Set<ResourceLocation> choices, ResourceLocation actual) {
        return choices.contains(actual);
    }

    private static ResourceLocation getDimensionLocation(World world) {
        int id = world.provider.getDimensionType().getId();
        switch (id) {
            case -1: return new ResourceLocation("minecraft", "the_nether");
            case 0: return new ResourceLocation("minecraft", "overworld");
            case 1: return new ResourceLocation("minecraft", "the_end");
            default:
                String name = world.provider.getDimensionType().getName().toLowerCase().replace(' ', '_');
                try {
                    return new ResourceLocation(name.contains(":") ? name : "minecraft:" + name);
                } catch (RuntimeException exception) {
                    return new ResourceLocation("dimension", Integer.toString(id));
                }
        }
    }

    private static boolean contains(List<ValueRange> ranges, double value) {
        if (ranges.isEmpty()) {
            return true;
        }
        for (ValueRange range : ranges) {
            if (range.contains(value)) {
                return true;
            }
        }
        return false;
    }

    private static ResourceLocation parseType(String typeName) {
        String normalized = typeName.replace('-', '_');
        try {
            return normalized.indexOf(':') < 0
                    ? new ResourceLocation(NeoforgeSkyboxes.FABRIC_SKYBOXES_NAMESPACE, normalized)
                    : new ResourceLocation(normalized);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private static ResourceLocation parseLocation(String location) {
        return new ResourceLocation(location);
    }

    private static Set<ResourceLocation> readLocations(JsonObject object, String name) {
        Set<ResourceLocation> values = new HashSet<>();
        if (object == null || !object.has(name)) {
            return values;
        }
        JsonElement element = object.get(name);
        if (element.isJsonArray()) {
            for (JsonElement entry : element.getAsJsonArray()) {
                values.add(parseLocation(entry.getAsString()));
            }
        } else if (element.isJsonPrimitive()) {
            values.add(parseLocation(element.getAsString()));
        }
        return values;
    }

    private static Set<String> readStrings(JsonObject object, String name) {
        Set<String> values = new HashSet<>();
        if (object == null || !object.has(name)) {
            return values;
        }
        JsonElement element = object.get(name);
        if (element.isJsonArray()) {
            for (JsonElement entry : element.getAsJsonArray()) {
                values.add(entry.getAsString());
            }
        } else if (element.isJsonPrimitive()) {
            values.add(element.getAsString());
        }
        return values;
    }

    private static List<ValueRange> readRanges(JsonObject object, String name, String legacyName) {
        List<ValueRange> ranges = new ArrayList<>();
        String key = object != null && object.has(name) ? name : legacyName;
        if (object == null || key == null || !object.has(key)) {
            return ranges;
        }
        JsonElement element = object.get(key);
        if (!element.isJsonArray()) {
            return ranges;
        }
        JsonArray array = element.getAsJsonArray();
        for (JsonElement entry : array) {
            if (entry.isJsonArray() && entry.getAsJsonArray().size() >= 2) {
                ranges.add(new ValueRange(entry.getAsJsonArray().get(0).getAsDouble(), entry.getAsJsonArray().get(1).getAsDouble()));
            } else if (entry.isJsonObject()) {
                JsonObject range = entry.getAsJsonObject();
                ranges.add(new ValueRange(getDouble(range, "min", 0.0D), getDouble(range, "max", 0.0D)));
            }
        }
        return ranges;
    }

    private static JsonObject getObject(JsonObject object, String name) {
        return object != null && object.has(name) && object.get(name).isJsonObject() ? object.getAsJsonObject(name) : null;
    }

    private static String getRequiredString(JsonObject object, String name) {
        if (object == null || !object.has(name)) {
            throw new IllegalArgumentException("Missing '" + name + "'");
        }
        return object.get(name).getAsString();
    }

    private static float getRequiredFloat(JsonObject object, String name) {
        if (object == null || !object.has(name)) {
            throw new IllegalArgumentException("Missing '" + name + "'");
        }
        return object.get(name).getAsFloat();
    }

    private static int getInt(JsonObject object, String name, int fallback) {
        return object != null && object.has(name) ? object.get(name).getAsInt() : fallback;
    }

    private static long getLong(JsonObject object, String name, long fallback) {
        return object != null && object.has(name) ? object.get(name).getAsLong() : fallback;
    }

    private static float getFloat(JsonObject object, String name, float fallback) {
        return object != null && object.has(name) ? object.get(name).getAsFloat() : fallback;
    }

    private static double getDouble(JsonObject object, String name, double fallback) {
        return object != null && object.has(name) ? object.get(name).getAsDouble() : fallback;
    }

    private static boolean getBoolean(JsonObject object, String name, boolean fallback) {
        return object != null && object.has(name) ? object.get(name).getAsBoolean() : fallback;
    }

    private static String getString(JsonObject object, String name, String fallback) {
        return object != null && object.has(name) ? object.get(name).getAsString() : fallback;
    }

    private static float[] readVector(JsonObject object, String name, float fallback) {
        return readVector(object, name, new float[]{fallback, fallback, fallback});
    }

    private static float[] readVector(JsonObject object, String name, float[] fallback) {
        float[] vector = fallback.clone();
        if (object == null || !object.has(name) || !object.get(name).isJsonArray()) {
            return vector;
        }
        JsonArray array = object.getAsJsonArray(name);
        for (int i = 0; i < vector.length && i < array.size(); i++) {
            vector[i] = array.get(i).getAsFloat();
        }
        return vector;
    }

    private static int[] readIntVector(JsonObject object, String name) {
        int[] vector = new int[]{0, 0, 0};
        if (object == null || !object.has(name) || !object.get(name).isJsonArray()) {
            return vector;
        }
        JsonArray array = object.getAsJsonArray(name);
        for (int i = 0; i < vector.length && i < array.size(); i++) {
            vector[i] = array.get(i).getAsInt();
        }
        return vector;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static float[][] fullTextureUvs() {
        float[][] uvs = new float[6][4];
        for (int face = 0; face < uvs.length; face++) {
            uvs[face] = new float[]{0.0F, 0.0F, 1.0F, 1.0F};
        }
        return uvs;
    }

    private static float[][] singleSpriteTextureUvs() {
        return new float[][]{
                {0.0F, 0.0F, 1.0F / 3.0F, 0.5F},
                {1.0F / 3.0F, 0.5F, 2.0F / 3.0F, 1.0F},
                {2.0F / 3.0F, 0.0F, 1.0F, 0.5F},
                {1.0F / 3.0F, 0.0F, 2.0F / 3.0F, 0.5F},
                {2.0F / 3.0F, 0.5F, 1.0F, 1.0F},
                {0.0F, 0.5F, 1.0F / 3.0F, 1.0F}
        };
    }

    private static ResourceLocation[] parseSixTextures(JsonObject object) {
        String[] keys = new String[]{"bottom", "north", "south", "top", "east", "west"};
        ResourceLocation[] frame = new ResourceLocation[keys.length];
        for (int i = 0; i < keys.length; i++) {
            frame[i] = parseLocation(getRequiredString(object, keys[i]));
        }
        return frame;
    }

    private static ResourceLocation[][] copyTextureFrames(List<ResourceLocation[]> frames, ResourceLocation[] fallback) {
        if (frames == null || frames.isEmpty()) {
            if (fallback == null) {
                return new ResourceLocation[0][];
            }
            return new ResourceLocation[][]{fallback.clone()};
        }
        ResourceLocation[][] copy = new ResourceLocation[frames.size()][];
        for (int i = 0; i < frames.size(); i++) {
            copy[i] = frames.get(i).clone();
        }
        return copy;
    }

    private static float[][] copyUvs(float[][] source) {
        float[][] copy = new float[source.length][];
        for (int i = 0; i < source.length; i++) {
            copy[i] = source[i].clone();
        }
        return copy;
    }

    public ResourceLocation getId() {
        return this.id;
    }

    public int getSchemaVersion() {
        return this.schemaVersion;
    }

    public ResourceLocation getTypeId() {
        return this.typeId;
    }

    public Type getType() {
        return this.type;
    }

    public JsonObject getPayload() {
        return this.payload.deepCopy();
    }

    public int getPriority() {
        return this.priority;
    }

    public boolean changesFog() {
        return this.changeFog;
    }

    public boolean changesFogDensity() {
        return this.changeFogDensity;
    }

    public float getFogRed() {
        return this.fogRed;
    }

    public float getFogGreen() {
        return this.fogGreen;
    }

    public float getFogBlue() {
        return this.fogBlue;
    }

    public float getFogDensity() {
        return this.fogDensity;
    }

    public float getMaxAlpha() {
        return this.maxAlpha;
    }

    public float getAlpha() {
        return this.alpha;
    }

    public float getRed() {
        return this.red;
    }

    public float getGreen() {
        return this.green;
    }

    public float getBlue() {
        return this.blue;
    }

    public float getColorAlpha() {
        return this.colorAlpha;
    }

    public BlendSettings getBlendSettings() {
        return this.blendSettings;
    }

    public ResourceLocation[] getTextures() {
        return this.textures.clone();
    }

    public ResourceLocation[] getTexturesAt(long nowMillis) {
        if (this.textureFrames.length == 0) {
            return new ResourceLocation[0];
        }
        if (this.textureFrames.length <= 1) {
            return this.textureFrames[0].clone();
        }
        long elapsed = Math.max(0L, nowMillis - this.animationStartedAtMillis);
        int frame = (int) ((elapsed / Math.max(1L, this.animationFrameMillis)) % this.textureFrames.length);
        return this.textureFrames[frame].clone();
    }

    public List<ResourceLocation> getAllTextures() {
        Set<ResourceLocation> unique = new HashSet<>();
        for (ResourceLocation[] frame : this.textureFrames) {
            Collections.addAll(unique, frame);
        }
        for (TextureAnimation animation : this.textureAnimations) {
            unique.add(animation.getTexture());
        }
        return new ArrayList<>(unique);
    }

    public List<TextureAnimation> getTextureAnimations() {
        return this.textureAnimations;
    }

    public float[] getTextureUv(int face) {
        return this.textureUvs[face].clone();
    }

    public float[] getStaticRotation() {
        return this.staticRotation.clone();
    }

    public float[] getAxisRotation() {
        return this.axisRotation.clone();
    }

    public int[] getTimeShift() {
        return this.timeShift.clone();
    }

    public float[] getRotationSpeed() {
        return this.rotationSpeed.clone();
    }

    public boolean isSkyboxRotation() {
        return this.skyboxRotation;
    }

    public boolean isSunEnabled() {
        return this.showSun;
    }

    public boolean isMoonEnabled() {
        return this.showMoon;
    }

    public boolean areStarsEnabled() {
        return this.showStars;
    }

    public ResourceLocation getSunTexture() {
        return this.sunTexture;
    }

    public ResourceLocation getMoonTexture() {
        return this.moonTexture;
    }

    public BlendSettings getDecorationBlendSettings() {
        return this.decorationBlendSettings;
    }

    public boolean isOptifineLayer() {
        return this.optifineLayer;
    }

    public boolean isOptifineRotate() {
        return this.optifineRotate;
    }

    public float getOptifineSpeed() {
        return this.optifineSpeed;
    }

    public float[] getOptifineAxis() {
        return this.optifineAxis.clone();
    }

    public boolean isDecorationSkyboxRotation() {
        return this.decorationSkyboxRotation;
    }

    public float[] getDecorationStaticRotation() {
        return this.decorationStaticRotation.clone();
    }

    public float[] getDecorationAxisRotation() {
        return this.decorationAxisRotation.clone();
    }

    public int[] getDecorationTimeShift() {
        return this.decorationTimeShift.clone();
    }

    public float[] getDecorationRotationSpeed() {
        return this.decorationRotationSpeed.clone();
    }

    private static final class ValueRange {
        private final double min;
        private final double max;

        private ValueRange(double min, double max) {
            this.min = Math.min(min, max);
            this.max = Math.max(min, max);
        }

        private boolean contains(double value) {
            return value >= this.min && value <= this.max;
        }
    }
}
