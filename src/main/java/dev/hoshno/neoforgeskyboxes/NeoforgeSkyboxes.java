package dev.hoshno.neoforgeskyboxes;

import dev.hoshno.neoforgeskyboxes.config.FabricSkyBoxesConfig;
import dev.hoshno.neoforgeskyboxes.config.SkyBoxDebugScreen;
import dev.hoshno.neoforgeskyboxes.resource.SkyboxResourceListener;
import dev.hoshno.neoforgeskyboxes.skyboxes.LegacyDeserializer;
import dev.hoshno.neoforgeskyboxes.skyboxes.SkyboxType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

@Mod(value = NeoforgeSkyboxes.MOD_ID, dist = Dist.CLIENT)
public final class NeoforgeSkyboxes {
    public static final String MOD_ID = "neoforgeskyboxes";
    public static final String FABRIC_SKYBOXES_NAMESPACE = "fabricskyboxes";
    public static final String MODID = FABRIC_SKYBOXES_NAMESPACE;
    private static final Logger LOGGER = LogManager.getLogger("NeoforgeSkyboxes");
    private static FabricSkyBoxesConfig CONFIG;
    private final SkyBoxDebugScreen debugScreen = new SkyBoxDebugScreen(Component.literal("Skybox Debug Screen"));

    public NeoforgeSkyboxes(IEventBus modBus) {
        LOGGER.info("Initializing NeoforgeSkyboxes");

        SkyboxType.SKYBOX_TYPE.register(modBus);
        LegacyDeserializer.DESERIALIZER.register(modBus);

        modBus.addListener(this::registerReloadListeners);
        modBus.addListener(this::registerKeyMappings);

        NeoForge.EVENT_BUS.addListener(this::onClientTick);
        NeoForge.EVENT_BUS.addListener(this::onLevelTick);
        NeoForge.EVENT_BUS.addListener(this::onRenderGui);
    }

    public static Logger getLogger() {
        return LOGGER;
    }

    public static FabricSkyBoxesConfig config() {
        if (CONFIG == null) {
            CONFIG = FabricSkyBoxesConfig.load(FMLPaths.CONFIGDIR.get().resolve("neoforgeskyboxes-config.json").toFile());
        }

        return CONFIG;
    }

    private void registerReloadListeners(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener(new SkyboxResourceListener());
        SkyboxManager.getInstance().setEnabled(config().generalSettings.enable);
    }

    private void registerKeyMappings(RegisterKeyMappingsEvent event) {
        FabricSkyBoxesConfig.KeyBindingImpl keyMappings = config().getKeyBinding();
        event.register(keyMappings.toggleFabricSkyBoxes);
        event.register(keyMappings.toggleSkyboxDebugHud);
    }

    private void onClientTick(ClientTickEvent.Post event) {
        Minecraft client = Minecraft.getInstance();
        config().getKeyBinding().onEndTick(client);
    }

    private void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel().isClientSide() && event.getLevel() instanceof net.minecraft.client.multiplayer.ClientLevel clientLevel) {
            SkyboxManager.getInstance().onEndTick(clientLevel);
        }
    }

    private void onRenderGui(RenderGuiEvent.Post event) {
        debugScreen.onHudRender(event.getGuiGraphics(), event.getPartialTick());
    }

    public static ResourceLocation fabricSkyboxesId(String path) {
        return ResourceLocation.fromNamespaceAndPath(FABRIC_SKYBOXES_NAMESPACE, path);
    }
}
