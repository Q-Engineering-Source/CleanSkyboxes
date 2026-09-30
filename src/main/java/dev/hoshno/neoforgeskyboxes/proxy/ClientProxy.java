package dev.hoshno.neoforgeskyboxes.proxy;

import dev.hoshno.neoforgeskyboxes.NeoforgeSkyboxes;
import dev.hoshno.neoforgeskyboxes.SkyboxManager;
import dev.hoshno.neoforgeskyboxes.config.FabricSkyBoxesConfig;
import dev.hoshno.neoforgeskyboxes.resource.SkyboxResourceListener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.io.File;

@SideOnly(Side.CLIENT)
public class ClientProxy extends CommonProxy {
    @Override
    public void preInit(FMLPreInitializationEvent event) {
        File configFile = new File(event.getSuggestedConfigurationFile().getParentFile(), NeoforgeSkyboxes.MOD_ID + "-config.json");
        FabricSkyBoxesConfig config = FabricSkyBoxesConfig.load(configFile);
        NeoforgeSkyboxes.setConfig(config);
        SkyboxManager.getInstance().setEnabled(config.generalSettings.enable);
        ClientRegistry.registerKeyBinding(config.getKeyBinding().toggleFabricSkyBoxes);
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Override
    public void init(FMLInitializationEvent event) {
        IResourceManager resourceManager = Minecraft.getMinecraft().getResourceManager();
        if (resourceManager instanceof IReloadableResourceManager) {
            ((IReloadableResourceManager) resourceManager).registerReloadListener(new SkyboxResourceListener());
        } else {
            NeoforgeSkyboxes.getLogger().error("The active resource manager cannot register reload listeners");
        }
    }

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        FabricSkyBoxesConfig config = NeoforgeSkyboxes.config();
        KeyBinding keyBinding = config.getKeyBinding().toggleFabricSkyBoxes;
        while (keyBinding.isPressed()) {
            config.generalSettings.enable = !config.generalSettings.enable;
            config.save();
            SkyboxManager.getInstance().setEnabled(config.generalSettings.enable);

            Minecraft minecraft = Minecraft.getMinecraft();
            if (minecraft.player != null) {
                String message = config.generalSettings.enable
                        ? "FabricSkyBoxes skyboxes enabled"
                        : "FabricSkyBoxes skyboxes disabled";
                minecraft.player.sendMessage(new TextComponentString(message));
            }
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            SkyboxManager.getInstance().tick(Minecraft.getMinecraft());
        }
    }
}
