package dev.hoshno.neoforgeskyboxes.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.hoshno.neoforgeskyboxes.NeoforgeSkyboxes;
import dev.hoshno.neoforgeskyboxes.SkyboxManager;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Modifier;

public class FabricSkyBoxesConfig {
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .excludeFieldsWithModifiers(Modifier.PRIVATE)
            .create();
    public final GeneralSettings generalSettings = new GeneralSettings();
    private final KeyBindingImpl keyBinding = new KeyBindingImpl();
    private File file;

    public static FabricSkyBoxesConfig load(File file) {
        FabricSkyBoxesConfig config;
        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                config = GSON.fromJson(reader, FabricSkyBoxesConfig.class);
            } catch (Exception e) {
                NeoforgeSkyboxes.getLogger().error("Could not parse config, falling back to defaults!", e);
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
        File dir = this.file.getParentFile();

        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                throw new RuntimeException("Could not create parent directories");
            }
        } else if (!dir.isDirectory()) {
            throw new RuntimeException("The parent file is not a directory");
        }

        try (FileWriter writer = new FileWriter(this.file)) {
            GSON.toJson(this, writer);
        } catch (IOException e) {
            throw new RuntimeException("Could not save configuration file", e);
        }
    }

    public static class GeneralSettings {
        public boolean enable = true;
        public int unexpectedTransitionDuration = 20;
        public boolean keepVanillaBehaviour = true;

        public boolean debugMode = false;
        public boolean debugHud = false;
    }


    public static class KeyBindingImpl {

        public final KeyMapping toggleFabricSkyBoxes;
        public final KeyMapping toggleSkyboxDebugHud;

        public KeyBindingImpl() {
            this.toggleFabricSkyBoxes = new KeyMapping("key.fabricskyboxes.toggle", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, "category.fabricskyboxes");
            this.toggleSkyboxDebugHud = new KeyMapping("key.fabricskyboxes.toggle.debug_hud", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, "category.fabricskyboxes");
        }

        public void onEndTick(Minecraft client) {
            while (this.toggleFabricSkyBoxes.consumeClick()) {
                NeoforgeSkyboxes.config().generalSettings.enable = !NeoforgeSkyboxes.config().generalSettings.enable;
                NeoforgeSkyboxes.config().save();
                SkyboxManager.getInstance().setEnabled(NeoforgeSkyboxes.config().generalSettings.enable);

                assert client.player != null;
                if (SkyboxManager.getInstance().isEnabled()) {
                    client.player.displayClientMessage(Component.translatable("fabricskyboxes.message.enabled"), false);
                } else {
                    client.player.displayClientMessage(Component.translatable("fabricskyboxes.message.disabled"), false);
                }
            }
            while (this.toggleSkyboxDebugHud.consumeClick()) {
                NeoforgeSkyboxes.config().generalSettings.debugHud = !NeoforgeSkyboxes.config().generalSettings.debugHud;
                NeoforgeSkyboxes.config().save();
            }
        }
    }
}
