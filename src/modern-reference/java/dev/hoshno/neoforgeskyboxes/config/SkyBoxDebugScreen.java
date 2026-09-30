package dev.hoshno.neoforgeskyboxes.config;

import dev.hoshno.neoforgeskyboxes.NeoforgeSkyboxes;
import dev.hoshno.neoforgeskyboxes.SkyboxManager;
import dev.hoshno.neoforgeskyboxes.api.skyboxes.FSBSkybox;
import dev.hoshno.neoforgeskyboxes.api.skyboxes.Skybox;
import net.minecraft.client.Minecraft;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;

public class SkyBoxDebugScreen extends Screen {
    public SkyBoxDebugScreen(Component title) {
        super(title);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics context, int mouseX, int mouseY, float delta) {
        this.onHudRender(context, Minecraft.getInstance().getTimer());
    }

    public void onHudRender(GuiGraphics GuiGraphics, DeltaTracker DeltaTracker) {
        if (NeoforgeSkyboxes.config().generalSettings.debugHud || Minecraft.getInstance().screen == this) {
            int yPadding = 2;
            for (Map.Entry<ResourceLocation, Skybox> identifierSkyboxEntry : SkyboxManager.getInstance().getSkyboxMap().entrySet()) {
                Skybox activeSkybox = identifierSkyboxEntry.getValue();
                if (activeSkybox instanceof FSBSkybox fsbSkybox && fsbSkybox.isActive()) {
                    GuiGraphics.drawString(Minecraft.getInstance().font, identifierSkyboxEntry.getKey() + " " + activeSkybox.getPriority() + " " + fsbSkybox.getAlpha(), 2, yPadding, 0xffffffff, false);
                    yPadding += 14;
                }
            }
        }
    }
}
