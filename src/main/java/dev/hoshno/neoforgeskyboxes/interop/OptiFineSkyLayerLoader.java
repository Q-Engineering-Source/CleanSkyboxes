package dev.hoshno.neoforgeskyboxes.interop;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.hoshno.neoforgeskyboxes.NeoforgeSkyboxes;
import dev.hoshno.neoforgeskyboxes.SkyboxManager;
import dev.hoshno.neoforgeskyboxes.resource.ResourcePackScanner;
import dev.hoshno.neoforgeskyboxes.skyboxes.SkyboxDefinition;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.util.ResourceLocation;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Converts the common OptiFine/MCPatcher sky-layer properties format into skybox layers. */
public final class OptiFineSkyLayerLoader {
    private static final Pattern PROPERTY_PATH = Pattern.compile("(?:optifine|mcpatcher)/sky/world(-?\\d+)/sky(\\d+)\\.properties$");
    private static final Pattern RANGE = Pattern.compile("^\\s*(-?\\d+)\\s*-\\s*(-?\\d+)\\s*$");

    private OptiFineSkyLayerLoader() {
    }

    public static void load(IResourceManager manager) {
        loadDirectory(manager, "optifine/sky");
        loadDirectory(manager, "mcpatcher/sky");
    }

    private static void loadDirectory(IResourceManager manager, String directory) {
        Set<ResourceLocation> resources = ResourcePackScanner.findResources(manager, "minecraft", directory, ".properties");
        List<ResourceLocation> ordered = new ArrayList<>(resources);
        ordered.sort(Comparator.comparingInt(OptiFineSkyLayerLoader::getSkyNumber)
                .thenComparing(ResourceLocation::getPath));
        for (ResourceLocation id : ordered) {
            Matcher matcher = PROPERTY_PATH.matcher(id.getPath());
            if (!matcher.matches()) {
                continue;
            }
            try {
                Properties properties = readProperties(manager, id);
                JsonObject definition = convert(manager, id, properties, matcher.group(1), matcher.group(2));
                if (definition != null) {
                    SkyboxManager.getInstance().addSkybox(SkyboxDefinition.parse(
                            new ResourceLocation("fabricskyboxes", "interop/" + id.getPath()), definition));
                }
            } catch (Exception exception) {
                NeoforgeSkyboxes.getLogger().warn("Skipping OptiFine sky layer {}", id, exception);
            }
        }
    }

    private static Properties readProperties(IResourceManager manager, ResourceLocation id) throws IOException {
        Properties properties = new Properties();
        try (IResource resource = manager.getResource(id); InputStream input = resource.getInputStream()) {
            properties.load(input);
        }
        return properties;
    }

    private static JsonObject convert(IResourceManager manager, ResourceLocation propertiesId,
                                      Properties properties, String worldName, String skyNumber) {
        ResourceLocation source = resolveSource(properties.getProperty("source"), propertiesId);
        if (source == null) {
            return null;
        }
        try (IResource ignored = manager.getResource(source)) {
            // Confirm that the layer's texture resolves in the active pack stack.
        } catch (IOException exception) {
            NeoforgeSkyboxes.getLogger().debug("Skipping OptiFine layer with missing texture {}", source);
            return null;
        }

        JsonObject definition = new JsonObject();
        definition.addProperty("schemaVersion", 2);
        definition.addProperty("type", "single-sprite-square-textured");
        definition.addProperty("texture", source.toString());
        JsonObject blend = new JsonObject();
        blend.addProperty("type", "optifine_" + properties.getProperty("blend", "add"));
        definition.add("blend", blend);
        definition.add("optifine", makeOptifineSettings(properties));

        JsonObject layerProperties = new JsonObject();
        layerProperties.addProperty("priority", parseInt(skyNumber, 0));
        layerProperties.add("fade", makeFade(properties));
        int transition = Math.max(0, Math.round(parseFloat(properties.getProperty("transition", "1"), 1.0F) * 20.0F));
        layerProperties.addProperty("transitionInDuration", transition);
        layerProperties.addProperty("transitionOutDuration", transition);
        definition.add("properties", layerProperties);

        JsonObject conditions = new JsonObject();
        JsonArray worlds = new JsonArray();
        worlds.add(toDimensionId(worldName));
        conditions.add("worlds", worlds);

        String biomeValue = properties.getProperty("biomes", "").trim();
        if (!biomeValue.isEmpty()) {
            if (biomeValue.startsWith("!")) {
                definition.addProperty("biomeInclusion", false);
                biomeValue = biomeValue.substring(1).trim();
            }
            JsonArray biomes = new JsonArray();
            for (String value : biomeValue.split("\\s+")) {
                if (!value.isEmpty()) {
                    biomes.add(value);
                }
            }
            conditions.add("biomes", biomes);
        }

        String heights = properties.getProperty("heights", "").trim();
        if (!heights.isEmpty()) {
            JsonArray ranges = makeRanges(heights);
            if (!ranges.isEmpty()) {
                conditions.add("yRanges", ranges);
            }
        }

        String days = properties.getProperty("days", "").trim();
        if (!days.isEmpty()) {
            JsonArray ranges = makeRanges(days);
            if (!ranges.isEmpty()) {
                JsonObject loop = new JsonObject();
                loop.addProperty("days", Math.max(1, parseInt(properties.getProperty("daysLoop", "8"), 8)));
                loop.add("ranges", ranges);
                conditions.add("loop", loop);
            }
        }
        definition.add("conditions", conditions);
        return definition;
    }

    private static JsonObject makeFade(Properties properties) {
        JsonObject fade = new JsonObject();
        if (properties.containsKey("startFadeIn") && properties.containsKey("endFadeIn") && properties.containsKey("endFadeOut")) {
            int startIn = toTickTime(properties.getProperty("startFadeIn"));
            int endIn = toTickTime(properties.getProperty("endFadeIn"));
            int endOut = toTickTime(properties.getProperty("endFadeOut"));
            int startOut;
            if (properties.containsKey("startFadeOut")) {
                startOut = toTickTime(properties.getProperty("startFadeOut"));
            } else {
                startOut = endOut - (endIn - startIn);
                if (startIn <= startOut && endIn >= startOut) {
                    startOut = endOut;
                }
            }
            fade.addProperty("startFadeIn", normalizeTime(startIn));
            fade.addProperty("endFadeIn", normalizeTime(endIn));
            fade.addProperty("startFadeOut", normalizeTime(startOut));
            fade.addProperty("endFadeOut", normalizeTime(endOut));
        } else {
            fade.addProperty("alwaysOn", true);
        }
        return fade;
    }

    private static JsonObject makeOptifineSettings(Properties properties) {
        JsonObject settings = new JsonObject();
        settings.addProperty("rotate", Boolean.parseBoolean(properties.getProperty("rotate", "false")));
        settings.addProperty("speed", parseFloat(properties.getProperty("speed", "1"), 1.0F));

        JsonArray axis = new JsonArray();
        String axisText = properties.getProperty("axis", "0 1 0").trim();
        String[] components = axisText.split("\\s+");
        if (components.length != 3) {
            components = new String[]{"0", "1", "0"};
        }
        for (String component : components) {
            axis.add(parseFloat(component, 0.0F));
        }
        settings.add("axis", axis);

        JsonArray weathers = new JsonArray();
        String weatherValue = properties.getProperty("weather", "clear").trim();
        for (String value : weatherValue.split("\\s+")) {
            if (!value.isEmpty()) {
                weathers.add(value.toLowerCase());
            }
        }
        settings.add("weathers", weathers);
        return settings;
    }

    private static JsonArray makeRanges(String text) {
        JsonArray ranges = new JsonArray();
        for (String entry : text.split("(?:\\s*,\\s*|\\s+)")) {
            String value = entry.trim();
            Matcher matcher = RANGE.matcher(value);
            int min;
            int max;
            if (matcher.matches()) {
                min = parseInt(matcher.group(1), Integer.MIN_VALUE);
                max = parseInt(matcher.group(2), Integer.MIN_VALUE);
            } else {
                min = parseInt(value, Integer.MIN_VALUE);
                max = min;
            }
            if (min != Integer.MIN_VALUE && max != Integer.MIN_VALUE) {
                JsonObject range = new JsonObject();
                range.addProperty("min", Math.min(min, max));
                range.addProperty("max", Math.max(min, max));
                ranges.add(range);
            }
        }
        return ranges;
    }

    private static ResourceLocation resolveSource(String source, ResourceLocation propertiesId) {
        String namespace = propertiesId.getNamespace();
        String path;
        if (source == null || source.trim().isEmpty()) {
            path = propertiesId.getPath().replaceFirst("\\.properties$", ".png");
        } else {
            source = source.trim().replace('\\', '/');
            if (source.startsWith("./")) {
                String resourcePath = propertiesId.getPath();
                int lastSlash = resourcePath.lastIndexOf('/');
                path = (lastSlash < 0 ? "" : resourcePath.substring(0, lastSlash + 1)) + source.substring(2);
            } else if (source.startsWith("assets/")) {
                String[] parts = source.split("/", 3);
                if (parts.length < 3) {
                    return null;
                }
                namespace = parts[1];
                path = parts[2];
            } else if (source.startsWith("/")) {
                path = source.substring(1);
            } else {
                try {
                    ResourceLocation location = new ResourceLocation(source);
                    namespace = location.getNamespace();
                    path = location.getPath();
                } catch (RuntimeException exception) {
                    path = source;
                }
            }
        }

        try {
            return new ResourceLocation(namespace, path);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private static String toDimensionId(String worldName) {
        switch (worldName) {
            case "world-1": return "minecraft:the_nether";
            case "world0": return "minecraft:overworld";
            case "world1": return "minecraft:the_end";
            default:
                return worldName.startsWith("world") ? worldName.substring("world".length()) : worldName;
        }
    }

    private static int toTickTime(String value) {
        String[] parts = value.split(":");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid OptiFine time: " + value);
        }
        int hours = Integer.parseInt(parts[0]);
        int minutes = Integer.parseInt(parts[1]);
        return normalizeTime(hours * 1000 + Math.round(minutes / 0.06F) - 6000);
    }

    private static int normalizeTime(int time) {
        return Math.floorMod(time, 24000);
    }

    private static int getSkyNumber(ResourceLocation id) {
        Matcher matcher = PROPERTY_PATH.matcher(id.getPath());
        return matcher.matches() ? parseInt(matcher.group(2), Integer.MAX_VALUE) : Integer.MAX_VALUE;
    }

    private static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (RuntimeException exception) {
            return fallback;
        }
    }

    private static float parseFloat(String value, float fallback) {
        try {
            return Float.parseFloat(value);
        } catch (RuntimeException exception) {
            return fallback;
        }
    }
}
