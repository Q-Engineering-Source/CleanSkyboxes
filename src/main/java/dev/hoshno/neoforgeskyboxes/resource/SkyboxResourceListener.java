package dev.hoshno.neoforgeskyboxes.resource;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import dev.hoshno.neoforgeskyboxes.NeoforgeSkyboxes;
import dev.hoshno.neoforgeskyboxes.SkyboxManager;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.resources.ResourceLocation;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class SkyboxResourceListener implements ResourceManagerReloadListener {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().serializeNulls().setLenient().create();

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        SkyboxManager skyboxManager = SkyboxManager.getInstance();

        // clear registered skyboxes on reload
        skyboxManager.clearSkyboxes();

        // load new skyboxes
        Map<ResourceLocation, Resource> resources = manager.listResources("sky", resourceLocation ->
                resourceLocation.getNamespace().equals(NeoforgeSkyboxes.FABRIC_SKYBOXES_NAMESPACE)
                        && resourceLocation.getPath().endsWith(".json"));
        NeoforgeSkyboxes.getLogger().info("Loading {} FabricSkyBoxes skybox resources", resources.size());

        resources.forEach((ResourceLocation, resource) -> {
            try {
                JsonObject json = GSON.fromJson(new InputStreamReader(resource.open(), StandardCharsets.UTF_8), JsonObject.class);
                skyboxManager.addSkybox(ResourceLocation, json);
            } catch (Exception e) {
                NeoforgeSkyboxes.getLogger().error("Error reading skybox {}", ResourceLocation.toString(), e);
            }
        });
    }
}
