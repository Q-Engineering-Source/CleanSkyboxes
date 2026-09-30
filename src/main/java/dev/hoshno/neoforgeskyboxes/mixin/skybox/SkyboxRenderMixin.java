package dev.hoshno.neoforgeskyboxes.mixin.skybox;

import dev.hoshno.neoforgeskyboxes.NeoforgeSkyboxes;
import dev.hoshno.neoforgeskyboxes.SkyboxManager;
import dev.hoshno.neoforgeskyboxes.compat.ActiniumSkyCompatibility;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.init.MobEffects;
import net.minecraft.block.material.Material;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SideOnly(Side.CLIENT)
@Mixin(RenderGlobal.class)
public abstract class SkyboxRenderMixin {
    @Inject(method = "renderSky(FI)V", at = @At("TAIL"))
    private void cleanskyboxes$renderCustomSky(float partialTicks, int pass, CallbackInfo ci) {
        Minecraft minecraft = Minecraft.getMinecraft();
        SkyboxManager manager = SkyboxManager.getInstance();
        if (!manager.isEnabled() || minecraft.world == null || minecraft.player == null || manager.getSkyboxes().isEmpty()) {
            return;
        }
        if (NeoforgeSkyboxes.config().generalSettings.keepVanillaBehaviour
                && (minecraft.player.isInsideOfMaterial(Material.WATER)
                || minecraft.player.isInsideOfMaterial(Material.LAVA)
                || minecraft.player.isPotionActive(MobEffects.BLINDNESS))) {
            return;
        }

        boolean actiniumPhaseStarted = ActiniumSkyCompatibility.beginCustomSky();
        try {
            manager.renderSkyboxes(minecraft);
        } finally {
            ActiniumSkyCompatibility.endCustomSky(actiniumPhaseStarted);
        }
    }
}
