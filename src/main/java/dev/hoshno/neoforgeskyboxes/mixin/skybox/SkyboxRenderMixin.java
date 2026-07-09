package dev.hoshno.neoforgeskyboxes.mixin.skybox;

import dev.hoshno.neoforgeskyboxes.NeoforgeSkyboxes;
import dev.hoshno.neoforgeskyboxes.SkyboxManager;
import net.minecraft.world.level.material.FogType;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LevelRenderer.class, priority = 900)
public abstract class SkyboxRenderMixin {
    /**
     * Contains the logic for when skyboxes should be rendered.
     */
    @Inject(method = "renderSky(Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;FLnet/minecraft/client/Camera;ZLjava/lang/Runnable;)V", at = @At("HEAD"), cancellable = true)
    private void renderCustomSkyboxes(Matrix4f matrix4f, Matrix4f projectionMatrix, float tickDelta, Camera camera, boolean thickFog, Runnable fogCallback, CallbackInfo ci) {
        SkyboxManager skyboxManager = SkyboxManager.getInstance();
        if (skyboxManager.isEnabled() && !skyboxManager.getActiveSkyboxes().isEmpty()) {
            FogType FogType = camera.getFluidInCamera();
            boolean cameraObscured = FogType == FogType.POWDER_SNOW
                    || FogType == FogType.LAVA
                    || FogType == FogType.WATER
                    || hasBlindnessOrDarkness(camera);
            boolean renderSky = !NeoforgeSkyboxes.config().generalSettings.keepVanillaBehaviour || !cameraObscured;
            if (renderSky) {
                PoseStack PoseStack = new PoseStack();
                PoseStack.mulPose(matrix4f);
                skyboxManager.renderSkyboxes((WorldRendererAccess) this, PoseStack, projectionMatrix, tickDelta, camera, thickFog, fogCallback);
            }
            ci.cancel();
        }
    }

    private static boolean hasBlindnessOrDarkness(Camera camera) {
        return camera.getEntity() instanceof LivingEntity livingEntity
                && (livingEntity.hasEffect(MobEffects.BLINDNESS) || livingEntity.hasEffect(MobEffects.DARKNESS));
    }
}
