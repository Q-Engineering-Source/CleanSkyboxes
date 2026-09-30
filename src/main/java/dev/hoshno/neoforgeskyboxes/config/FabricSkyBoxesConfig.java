package dev.hoshno.neoforgeskyboxes.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.hoshno.neoforgeskyboxes.NeoforgeSkyboxes;
import net.minecraft.client.settings.KeyBinding;
import org.lwjgl.input.Keyboard;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Modifier;

public final class FabricSkyBoxesConfig {
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .excludeFieldsWithModifiers(Modifier.PRIVATE)
            .create();

    public final GeneralSettings generalSettings = new GeneralSettings();
    private final KeyBindingImpl keyBinding = new KeyBindingImpl();
    private File file;

    public static FabricSkyBoxesConfig load(File file) {
        FabricSkyBoxesConfig config;
        if (file.isFile()) {
            try (FileReader reader = new FileReader(file)) {
                config = GSON.fromJson(reader, FabricSkyBoxesConfig.class);
                if (config == null) {
                    config = new FabricSkyBoxesConfig();
                }
            } catch (Exception exception) {
                NeoforgeSkyboxes.getLogger().error("Could not parse config; using defaults", exception);
                config = new FabricSkyBoxesConfig();
            }
        } else {
            config = new FabricSkyBoxesConfig();
        }

        config.file = file;
        config.save();
        return config;
    }

    public KeyBindingImpl getKeyBinding() {
        return this.keyBinding;
    }

    public void save() {
        File parent = this.file.getParentFile();
        if (!parent.exists() && !parent.mkdirs()) {
            throw new IllegalStateException("Could not create config directory " + parent);
        }
        if (!parent.isDirectory()) {
            throw new IllegalStateException("Config parent is not a directory: " + parent);
        }

        try (FileWriter writer = new FileWriter(this.file)) {
            GSON.toJson(this, writer);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not save configuration file " + this.file, exception);
        }
    }

    public static final class GeneralSettings {
        public boolean enable = true;
        public int unexpectedTransitionDuration = 20;
        public boolean keepVanillaBehaviour = true;
        public boolean enableOptiFineInterop = true;
        public boolean debugMode;
        public boolean debugHud;
    }

    public static final class KeyBindingImpl {
        public final KeyBinding toggleFabricSkyBoxes = new KeyBinding(
                "key.fabricskyboxes.toggle", Keyboard.KEY_NONE, "key.categories.fabricskyboxes");
    }
}
