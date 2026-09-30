package dev.hoshno.neoforgeskyboxes;

import dev.hoshno.neoforgeskyboxes.proxy.IProxy;
import dev.hoshno.neoforgeskyboxes.config.FabricSkyBoxesConfig;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(modid = Reference.MOD_ID, name = Reference.MOD_NAME, version = Reference.VERSION, clientSideOnly = true)
public final class NeoforgeSkyboxes {
    public static final String MOD_ID = Reference.MOD_ID;
    public static final String FABRIC_SKYBOXES_NAMESPACE = "fabricskyboxes";

    private static final Logger LOGGER = LogManager.getLogger(Reference.MOD_NAME);
    private static FabricSkyBoxesConfig config;

    @SidedProxy(
            modId = MOD_ID,
            clientSide = "dev.hoshno.neoforgeskyboxes.proxy.ClientProxy",
            serverSide = "dev.hoshno.neoforgeskyboxes.proxy.CommonProxy"
    )
    public static IProxy proxy;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        LOGGER.info("Initializing {} for Minecraft 1.12.2", Reference.MOD_NAME);
        proxy.preInit(event);
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        proxy.init(event);
    }

    public static Logger getLogger() {
        return LOGGER;
    }

    public static FabricSkyBoxesConfig config() {
        return config;
    }

    public static void setConfig(FabricSkyBoxesConfig config) {
        NeoforgeSkyboxes.config = config;
    }
}
