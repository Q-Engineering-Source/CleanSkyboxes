package dev.hoshno.neoforgeskyboxes;

import dev.hoshno.neoforgeskyboxes.skyboxes.SkyboxDefinition;
import dev.hoshno.neoforgeskyboxes.skyboxes.SkyboxRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SkyboxManager {
    private static final SkyboxManager INSTANCE = new SkyboxManager();

    private final Map<ResourceLocation, SkyboxDefinition> skyboxes = new LinkedHashMap<>();
    private boolean enabled = true;

    private SkyboxManager() {
    }

    public static SkyboxManager getInstance() {
        return INSTANCE;
    }

    public synchronized void clearSkyboxes() {
        this.skyboxes.clear();
    }

    public synchronized void addSkybox(SkyboxDefinition skybox) {
        this.skyboxes.put(skybox.getId(), skybox);
        if (skybox.getType() == SkyboxDefinition.Type.SQUARE_TEXTURED) {
            for (ResourceLocation texture : skybox.getTextures()) {
                Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
            }
        }
    }

    public synchronized void tick(Minecraft minecraft) {
        for (SkyboxDefinition skybox : this.skyboxes.values()) {
            skybox.tick(minecraft);
        }
    }

    public void renderSkyboxes(Minecraft minecraft) {
        if (!this.isEnabled()) {
            return;
        }
        List<SkyboxDefinition> ordered = new ArrayList<>(this.getSkyboxes().values());
        ordered.sort(Comparator.comparingInt(SkyboxDefinition::getPriority));
        for (SkyboxDefinition skybox : ordered) {
            if (skybox.getAlpha() > 0.0F) {
                SkyboxRenderer.render(minecraft, skybox);
            }
        }
    }

    public synchronized Map<ResourceLocation, SkyboxDefinition> getSkyboxes() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(this.skyboxes));
    }

    public synchronized boolean isEnabled() {
        return this.enabled;
    }

    public synchronized void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
