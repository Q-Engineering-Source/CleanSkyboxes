package dev.hoshno.neoforgeskyboxes.skyboxes;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import dev.hoshno.neoforgeskyboxes.NeoforgeSkyboxes;
import dev.hoshno.neoforgeskyboxes.api.skyboxes.Skybox;
import dev.hoshno.neoforgeskyboxes.skyboxes.textured.SquareTexturedSkybox;
import dev.hoshno.neoforgeskyboxes.util.JsonObjectWrapper;
import dev.hoshno.neoforgeskyboxes.util.object.*;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.joml.Vector3f;
import org.joml.Vector3i;

import java.util.List;
import java.util.function.BiConsumer;

public class LegacyDeserializer<T extends Skybox> {
    public static final DeferredRegister<LegacyDeserializer<? extends Skybox>> DESERIALIZER = DeferredRegister.create(ResourceKey.createRegistryKey(ResourceLocation.fromNamespaceAndPath(NeoforgeSkyboxes.FABRIC_SKYBOXES_NAMESPACE, "legacy_skybox_deserializer")), NeoforgeSkyboxes.FABRIC_SKYBOXES_NAMESPACE);
    public static final Registry<LegacyDeserializer<? extends Skybox>> REGISTRY = DESERIALIZER.makeRegistry(builder -> {});
    public static final LegacyDeserializer<MonoColorSkybox> MONO_COLOR_SKYBOX_DESERIALIZER = register(new LegacyDeserializer<>(LegacyDeserializer::decodeMonoColor, MonoColorSkybox.class), "mono_color_skybox_legacy_deserializer");
    public static final LegacyDeserializer<SquareTexturedSkybox> SQUARE_TEXTURED_SKYBOX_DESERIALIZER = register(new LegacyDeserializer<>(LegacyDeserializer::decodeSquareTextured, SquareTexturedSkybox.class), "square_textured_skybox_legacy_deserializer");
    private final BiConsumer<JsonObjectWrapper, Skybox> deserializer;

    private LegacyDeserializer(BiConsumer<JsonObjectWrapper, Skybox> deserializer, Class<T> clazz) {
        this.deserializer = deserializer;
    }

    private static void decodeSquareTextured(JsonObjectWrapper wrapper, Skybox skybox) {
        decodeSharedData(wrapper, skybox);
        ((SquareTexturedSkybox) skybox).rotation = new Rotation(true, new Vector3f(0f, 0f, 0f), new Vector3f(wrapper.getOptionalArrayFloat("axis", 0, 0), wrapper.getOptionalArrayFloat("axis", 1, 0), wrapper.getOptionalArrayFloat("axis", 2, 0)), new Vector3i(0, 0, 0), 0, 1, 0);
        ((SquareTexturedSkybox) skybox).blend = new Blend(wrapper.getOptionalBoolean("shouldBlend", false) ? "add" : "", Blender.DEFAULT);
        ((SquareTexturedSkybox) skybox).textures = new Textures(
                new Texture(wrapper.getJsonStringAsId("texture_north")),
                new Texture(wrapper.getJsonStringAsId("texture_south")),
                new Texture(wrapper.getJsonStringAsId("texture_east")),
                new Texture(wrapper.getJsonStringAsId("texture_west")),
                new Texture(wrapper.getJsonStringAsId("texture_top")),
                new Texture(wrapper.getJsonStringAsId("texture_bottom"))
        );
    }

    private static void decodeMonoColor(JsonObjectWrapper wrapper, Skybox skybox) {
        decodeSharedData(wrapper, skybox);
        ((MonoColorSkybox) skybox).color = new RGBA(wrapper.get("red").getAsFloat(), wrapper.get("green").getAsFloat(), wrapper.get("blue").getAsFloat());
    }

    private static void decodeSharedData(JsonObjectWrapper wrapper, Skybox skybox) {
        float maxAlpha = wrapper.getOptionalFloat("maxAlpha", 1f);
        ((AbstractSkybox) skybox).properties = new Properties.Builder()
                .fade(new Fade(
                        wrapper.get("startFadeIn").getAsInt(),
                        wrapper.get("endFadeIn").getAsInt(),
                        wrapper.get("startFadeOut").getAsInt(),
                        wrapper.get("endFadeOut").getAsInt(),
                        false
                ))
                .maxAlpha(maxAlpha)
                .transitionInDuration((int) (maxAlpha / wrapper.getOptionalFloat("transitionSpeed", 0.05f)))
                .transitionOutDuration((int) (maxAlpha / wrapper.getOptionalFloat("transitionSpeed", 0.05f)))
                .changeFog(wrapper.getOptionalBoolean("changeFog", false))
                .fogColors(new RGBA(
                        wrapper.getOptionalFloat("fogRed", 0f),
                        wrapper.getOptionalFloat("fogGreen", 0f),
                        wrapper.getOptionalFloat("fogBlue", 0f)
                ))
                .build();
        // decorations
        ((AbstractSkybox) skybox).decorations = Decorations.DEFAULT;
        // environment specifications
        JsonElement element;
        element = wrapper.getOptionalValue("weather").orElse(null);
        if (element != null) {
            if (element.isJsonArray()) {
                for (JsonElement jsonElement : element.getAsJsonArray()) {
                    ((AbstractSkybox) skybox).conditions.getWeathers().add(Weather.fromString(jsonElement.getAsString()));
                }
            } else if (GsonHelper.isStringValue(element)) {
                ((AbstractSkybox) skybox).conditions.getWeathers().add(Weather.fromString(element.getAsString()));
            }
        }
        element = wrapper.getOptionalValue("biomes").orElse(null);
        processIds(element, ((AbstractSkybox) skybox).conditions.getBiomes());
        element = wrapper.getOptionalValue("dimensions").orElse(null);
        processIds(element, ((AbstractSkybox) skybox).conditions.getWorlds());
        element = wrapper.getOptionalValue("heightRanges").orElse(null);
        if (element != null) {
            JsonArray array = element.getAsJsonArray();
            for (JsonElement jsonElement : array) {
                JsonArray insideArray = jsonElement.getAsJsonArray();
                float low = insideArray.get(0).getAsFloat();
                float high = insideArray.get(1).getAsFloat();
                ((AbstractSkybox) skybox).conditions.getYRanges().add(new MinMaxEntry(low, high));
            }
        }
    }

    private static void processIds(JsonElement element, List<ResourceLocation> list) {
        if (element != null) {
            if (element.isJsonArray()) {
                for (JsonElement jsonElement : element.getAsJsonArray()) {
                    list.add(ResourceLocation.parse(jsonElement.getAsString()));
                }
            } else if (GsonHelper.isStringValue(element)) {
                list.add(ResourceLocation.parse(element.getAsString()));
            }
        }
    }

    private static <T extends Skybox> LegacyDeserializer<T> register(LegacyDeserializer<T> deserializer, String name) {
        DESERIALIZER.register(name, () -> deserializer);
        return deserializer;
    }

    public BiConsumer<JsonObjectWrapper, Skybox> getDeserializer() {
        return this.deserializer;
    }
}
