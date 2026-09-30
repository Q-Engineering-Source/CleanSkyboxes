package dev.hoshno.neoforgeskyboxes;

import dev.hoshno.neoforgeskyboxes.skyboxes.SkyboxDefinition;
import dev.hoshno.neoforgeskyboxes.skyboxes.SkyboxRenderer;
import dev.hoshno.neoforgeskyboxes.mixin.skybox.RenderGlobalSkyAccessor;
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
        if (skybox.getType() == SkyboxDefinition.Type.SQUARE_TEXTURED
                || skybox.getType() == SkyboxDefinition.Type.SINGLE_SPRITE_SQUARE_TEXTURED) {
            for (ResourceLocation texture : skybox.getTextures()) {
                Minecraft.getMinecraft().getTextureManager().bindTexture(texture);
            }
        }
        if (skybox.isSunEnabled()) {
            Minecraft.getMinecraft().getTextureManager().bindTexture(skybox.getSunTexture());
        }
        if (skybox.isMoonEnabled()) {
            Minecraft.getMinecraft().getTextureManager().bindTexture(skybox.getMoonTexture());
        }
    }

    public synchronized void tick(Minecraft minecraft) {
        for (SkyboxDefinition skybox : this.skyboxes.values()) {
            skybox.tick(minecraft);
        }
    }

    public void renderSkyboxes(Minecraft minecraft, float partialTicks, RenderGlobalSkyAccessor renderGlobal) {
        if (!this.isEnabled()) {
            return;
        }
        List<SkyboxDefinition> ordered = new ArrayList<>(this.getSkyboxes().values());
        ordered.sort(Comparator.comparingInt(SkyboxDefinition::getPriority));
        for (SkyboxDefinition skybox : ordered) {
            if (skybox.getAlpha() > 0.0F) {
                SkyboxRenderer.render(minecraft, skybox, partialTicks, renderGlobal);
            }
        }
    }

    public FogOverride blendFog(float red, float green, float blue) {
        if (!this.isEnabled()) {
            return null;
        }

        List<SkyboxDefinition> ordered = new ArrayList<>(this.getSkyboxes().values());
        ordered.sort(Comparator.comparingInt(SkyboxDefinition::getPriority));
        float resultRed = red;
        float resultGreen = green;
        float resultBlue = blue;
        float density = 1.0F;
        boolean changed = false;
        boolean changesDensity = false;

        for (SkyboxDefinition skybox : ordered) {
            if (!skybox.changesFog() || skybox.getAlpha() <= 0.0F || skybox.getMaxAlpha() <= 0.0F) {
                continue;
            }
            changed = true;
            float weight = Math.min(1.0F, skybox.getAlpha() / skybox.getMaxAlpha());
            float inverse = 1.0F - weight;
            resultRed = skybox.getFogRed() * weight + resultRed * inverse;
            resultGreen = skybox.getFogGreen() * weight + resultGreen * inverse;
            resultBlue = skybox.getFogBlue() * weight + resultBlue * inverse;
            if (skybox.changesFogDensity()) {
                density = skybox.getFogDensity() * weight + density * inverse;
            }
            changesDensity = skybox.changesFogDensity();
        }

        return changed ? new FogOverride(resultRed, resultGreen, resultBlue, density, changesDensity) : null;
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

    public static final class FogOverride {
        public final float red;
        public final float green;
        public final float blue;
        public final float density;
        public final boolean changesDensity;

        private FogOverride(float red, float green, float blue, float density, boolean changesDensity) {
            this.red = red;
            this.green = green;
            this.blue = blue;
            this.density = density;
            this.changesDensity = changesDensity;
        }
    }
}
