package dev.hoshno.neoforgeskyboxes.mixin.skybox;

import dev.hoshno.neoforgeskyboxes.SkyboxManager;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SideOnly(Side.CLIENT)
@Mixin(EntityRenderer.class)
public abstract class EntityRendererFogMixin {
    @Shadow
    private float fogColorRed;

    @Shadow
    private float fogColorGreen;

    @Shadow
    private float fogColorBlue;

    @Unique
    private float cleanskyboxes$fogDensity = -1.0F;

    @Inject(method = "updateFogColor(F)V", at = @At("TAIL"))
    private void cleanskyboxes$blendSkyboxFog(float partialTicks, CallbackInfo ci) {
        SkyboxManager.FogOverride fog = SkyboxManager.getInstance().blendFog(
                this.fogColorRed, this.fogColorGreen, this.fogColorBlue);
        if (fog == null) {
            this.cleanskyboxes$fogDensity = -1.0F;
            return;
        }

        this.fogColorRed = fog.red;
        this.fogColorGreen = fog.green;
        this.fogColorBlue = fog.blue;
        this.cleanskyboxes$fogDensity = fog.changesDensity ? fog.density : -1.0F;
        GlStateManager.clearColor(this.fogColorRed, this.fogColorGreen, this.fogColorBlue, 0.0F);
    }

    @ModifyArg(
            method = "setupFog(IF)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;setFogDensity(F)V"),
            index = 0
    )
    private float cleanskyboxes$overrideFogDensity(float vanillaDensity) {
        return this.cleanskyboxes$fogDensity >= 0.0F ? this.cleanskyboxes$fogDensity : vanillaDensity;
    }
}
