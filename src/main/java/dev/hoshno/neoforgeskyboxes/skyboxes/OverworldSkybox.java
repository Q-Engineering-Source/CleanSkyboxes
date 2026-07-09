package dev.hoshno.neoforgeskyboxes.skyboxes;

import com.mojang.blaze3d.vertex.*;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.hoshno.neoforgeskyboxes.SkyboxManager;
import dev.hoshno.neoforgeskyboxes.api.skyboxes.Skybox;
import dev.hoshno.neoforgeskyboxes.mixin.skybox.WorldRendererAccess;
import dev.hoshno.neoforgeskyboxes.util.object.Conditions;
import dev.hoshno.neoforgeskyboxes.util.object.Decorations;
import dev.hoshno.neoforgeskyboxes.util.object.Properties;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderInstance;
import com.mojang.blaze3d.vertex.VertexBuffer;
import net.minecraft.client.renderer.*;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import com.mojang.math.Axis;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public class OverworldSkybox extends AbstractSkybox {
    public static Codec<OverworldSkybox> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Properties.CODEC.fieldOf("properties").forGetter(AbstractSkybox::getProperties),
            Conditions.CODEC.optionalFieldOf("conditions", Conditions.DEFAULT).forGetter(AbstractSkybox::getConditions),
            Decorations.CODEC.optionalFieldOf("decorations", Decorations.DEFAULT).forGetter(AbstractSkybox::getDecorations)
    ).apply(instance, OverworldSkybox::new));

    public OverworldSkybox(Properties properties, Conditions conditions, Decorations decorations) {
        super(properties, conditions, decorations);
    }

    @Override
    public SkyboxType<? extends Skybox> getType() {
        return SkyboxType.OVERWORLD_SKYBOX;
    }

    @Override
    public void render(WorldRendererAccess worldRendererAccess, PoseStack matrices, Matrix4f projectionMatrix, float tickDelta, Camera camera, boolean thickFog, Runnable fogCallback) {
        fogCallback.run();
        Minecraft client = Minecraft.getInstance();
        ClientLevel world = client.level;
        assert client.level != null;

        Vec3 Vec3 = world.getSkyColor(client.gameRenderer.getMainCamera().getPosition(), tickDelta);
        float f = (float) Vec3.x;
        float g = (float) Vec3.y;
        float h = (float) Vec3.z;
        FogRenderer.levelFogColor();
        RenderSystem.depthMask(false);

        // Light Sky
        RenderSystem.setShaderColor(f, g, h, this.alpha);
        ShaderInstance ShaderInstance = RenderSystem.getShader();
        worldRendererAccess.getLightSkyBuffer().bind();
        worldRendererAccess.getLightSkyBuffer().drawWithShader(matrices.last().pose(), projectionMatrix, ShaderInstance);
        VertexBuffer.unbind();


        RenderSystem.enableBlend();
        float skyAngle = world.getTimeOfDay(tickDelta);
        float skyAngleRadian = world.getSunAngle(tickDelta);

        if (SkyboxManager.getInstance().isEnabled() && SkyboxManager.getInstance().getActiveSkyboxes().stream().anyMatch(skybox -> skybox instanceof AbstractSkybox abstractSkybox && abstractSkybox.getDecorations().getRotation().getSkyboxRotation())) {
            skyAngle = Mth.frac(world.getDayTime() / 24000F + 0.75F);
            skyAngleRadian = skyAngle * (float) (Math.PI * 2);
        }

        float[] fs = world.effects().getSunriseColor(skyAngle, tickDelta);
        if (fs != null) {
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            matrices.pushPose();
            matrices.mulPose(Axis.XP.rotationDegrees(90.0F));
            float i = Mth.sin(skyAngleRadian) < 0.0F ? 180.0F : 0.0F;
            matrices.mulPose(Axis.ZP.rotationDegrees(i));
            matrices.mulPose(Axis.ZP.rotationDegrees(90.0F));
            float j = fs[0];
            float k = fs[1];
            float l = fs[2];
            Matrix4f matrix4f = matrices.last().pose();
            BufferBuilder bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
            bufferBuilder.addVertex(matrix4f, 0.0F, 100.0F, 0.0F).setColor(j, k, l, fs[3] * this.alpha);

            for (int n = 0; n <= 16; ++n) {
                float o = (float) n * (float) (Math.PI * 2) / 16.0F;
                float p = Mth.sin(o);
                float q = Mth.cos(o);
                bufferBuilder.addVertex(matrix4f, p * 120.0F, q * 120.0F, -q * 40.0F * fs[3]).setColor(fs[0], fs[1], fs[2], 0.0F);
            }

            BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());
            matrices.popPose();
        }


        this.renderDecorations(worldRendererAccess, matrices, projectionMatrix, tickDelta, this.alpha, fogCallback);

        // Dark Sky
        RenderSystem.setShaderColor(0.0F, 0.0F, 0.0F, 1.0F);
        double d = client.player.getEyePosition(tickDelta).y - world.getLevelData().getHorizonHeight(world);
        if (d < 0.0) {
            matrices.pushPose();
            matrices.translate(0.0F, 12.0F, 0.0F);
            worldRendererAccess.getDarkSkyBuffer().bind();
            worldRendererAccess.getDarkSkyBuffer().drawWithShader(matrices.last().pose(), projectionMatrix, ShaderInstance);
            VertexBuffer.unbind();
            matrices.popPose();
        }
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);


        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }
}
