package dev.hoshno.neoforgeskyboxes.resource;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import dev.hoshno.neoforgeskyboxes.NeoforgeSkyboxes;
import dev.hoshno.neoforgeskyboxes.SkyboxManager;
import dev.hoshno.neoforgeskyboxes.skyboxes.SkyboxDefinition;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.util.ResourceLocation;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Set;

public final class SkyboxResourceListener implements IResourceManagerReloadListener {
    private static final Gson GSON = new GsonBuilder().serializeNulls().setLenient().create();

    @Override
    public void onResourceManagerReload(IResourceManager resourceManager) {
        SkyboxManager skyboxManager = SkyboxManager.getInstance();
        skyboxManager.clearSkyboxes();

        Set<ResourceLocation> resources = ResourcePackScanner.findSkyboxResources(resourceManager);
        NeoforgeSkyboxes.getLogger().info("Loading {} FabricSkyBoxes skybox resources", resources.size());
        for (ResourceLocation id : resources) {
            try (IResource resource = resourceManager.getResource(id);
                 InputStreamReader reader = new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)) {
                JsonObject json = GSON.fromJson(reader, JsonObject.class);
                if (json != null) {
                    skyboxManager.addSkybox(SkyboxDefinition.parse(id, json));
                }
            } catch (Exception exception) {
                NeoforgeSkyboxes.getLogger().error("Error reading skybox {}", id, exception);
            }
        }
    }
}
