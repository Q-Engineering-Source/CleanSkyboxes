package dev.hoshno.neoforgeskyboxes.skyboxes.textured;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.hoshno.neoforgeskyboxes.api.skyboxes.RotatableSkybox;
import dev.hoshno.neoforgeskyboxes.mixin.skybox.WorldRendererAccess;
import dev.hoshno.neoforgeskyboxes.skyboxes.AbstractSkybox;
import dev.hoshno.neoforgeskyboxes.skyboxes.TextureRegistrar;
import dev.hoshno.neoforgeskyboxes.util.Utils;
import dev.hoshno.neoforgeskyboxes.util.object.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.multiplayer.ClientLevel;
import com.mojang.math.Axis;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.Objects;

public abstract class TexturedSkybox extends AbstractSkybox implements RotatableSkybox, TextureRegistrar {
    public Rotation rotation;
    public Blend blend;

    protected TexturedSkybox() {
    }

    protected TexturedSkybox(Properties properties, Conditions conditions, Decorations decorations, Blend blend) {
        super(properties, conditions, decorations);
        this.blend = blend;
        this.rotation = properties.getRotation();
    }

    /**
     * Overrides and makes final here as there are options that should always be respected in a textured skybox.
     *
     * @param worldRendererAccess Access to the LevelRenderer as skyboxes often require it.
     * @param PoseStack            The current PoseStack.
     * @param tickDelta           The current tick delta.
     */
    @Override
    public final void render(WorldRendererAccess worldRendererAccess, PoseStack PoseStack, Matrix4f projectionMatrix, float tickDelta, Camera camera, boolean thickFog, Runnable fogCallback) {
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        this.blend.applyBlendFunc(this.alpha);

        ClientLevel world = Objects.requireNonNull(Minecraft.getInstance().level);

        Vector3f rotationStatic = this.rotation.getStatic();

        PoseStack.pushPose();

        // axis + time rotation
        double timeRotationX = Utils.calculateRotation(this.rotation.getRotationSpeedX(), this.rotation.getTimeShift().x(), this.rotation.getSkyboxRotation(), world);
        double timeRotationY = Utils.calculateRotation(this.rotation.getRotationSpeedY(), this.rotation.getTimeShift().y(), this.rotation.getSkyboxRotation(), world);
        double timeRotationZ = Utils.calculateRotation(this.rotation.getRotationSpeedZ(), this.rotation.getTimeShift().z(), this.rotation.getSkyboxRotation(), world);
        this.applyTimeRotation(PoseStack, (float) timeRotationX, (float) timeRotationY, (float) timeRotationZ);
        // static
        PoseStack.mulPose(Axis.XP.rotationDegrees(rotationStatic.x()));
        PoseStack.mulPose(Axis.YP.rotationDegrees(rotationStatic.y()));
        PoseStack.mulPose(Axis.ZP.rotationDegrees(rotationStatic.z()));
        this.renderSkybox(worldRendererAccess, PoseStack, tickDelta, camera, thickFog, fogCallback);
        PoseStack.popPose();

        this.renderDecorations(worldRendererAccess, PoseStack, projectionMatrix, tickDelta, this.alpha, fogCallback);

        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /**
     * Override this method instead of render if you are extending this skybox.
     */
    public abstract void renderSkybox(WorldRendererAccess worldRendererAccess, PoseStack PoseStack, float tickDelta, Camera camera, boolean thickFog, Runnable runnable);

    private void applyTimeRotation(PoseStack PoseStack, float timeRotationX, float timeRotationY, float timeRotationZ) {
        Vector3f timeRotationAxis = this.rotation.getAxis();
        PoseStack.mulPose(Axis.XP.rotationDegrees(timeRotationAxis.x()));
        PoseStack.mulPose(Axis.YP.rotationDegrees(timeRotationAxis.y()));
        PoseStack.mulPose(Axis.ZP.rotationDegrees(timeRotationAxis.z()));
        PoseStack.mulPose(Axis.XP.rotationDegrees(timeRotationX));
        PoseStack.mulPose(Axis.YP.rotationDegrees(timeRotationY));
        PoseStack.mulPose(Axis.ZP.rotationDegrees(timeRotationZ));
        PoseStack.mulPose(Axis.ZN.rotationDegrees(timeRotationAxis.z()));
        PoseStack.mulPose(Axis.YN.rotationDegrees(timeRotationAxis.y()));
        PoseStack.mulPose(Axis.XN.rotationDegrees(timeRotationAxis.x()));
    }

    public Blend getBlend() {
        return this.blend;
    }

    public Rotation getRotation() {
        return this.rotation;
    }
}
