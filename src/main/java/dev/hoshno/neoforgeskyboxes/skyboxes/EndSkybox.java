package dev.hoshno.neoforgeskyboxes.skyboxes;

import com.mojang.blaze3d.vertex.*;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.hoshno.neoforgeskyboxes.api.skyboxes.Skybox;
import dev.hoshno.neoforgeskyboxes.mixin.skybox.WorldRendererAccess;
import dev.hoshno.neoforgeskyboxes.util.object.Conditions;
import dev.hoshno.neoforgeskyboxes.util.object.Decorations;
import dev.hoshno.neoforgeskyboxes.util.object.Properties;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import org.joml.Matrix4f;

public class EndSkybox extends AbstractSkybox {
    public static Codec<EndSkybox> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Properties.CODEC.fieldOf("properties").forGetter(AbstractSkybox::getProperties),
            Conditions.CODEC.optionalFieldOf("conditions", Conditions.DEFAULT).forGetter(AbstractSkybox::getConditions),
            Decorations.CODEC.optionalFieldOf("decorations", Decorations.DEFAULT).forGetter(AbstractSkybox::getDecorations)
    ).apply(instance, EndSkybox::new));

    public EndSkybox(Properties properties, Conditions conditions, Decorations decorations) {
        super(properties, conditions, decorations);
    }

    @Override
    public SkyboxType<? extends Skybox> getType() {
        return SkyboxType.END_SKYBOX;
    }

    @Override
    public void render(WorldRendererAccess worldRendererAccess, PoseStack matrices, Matrix4f projectionMatrix, float tickDelta, Camera camera, boolean thickFog, Runnable fogCallback) {
        Minecraft client = Minecraft.getInstance();
        assert client.level != null;

        RenderSystem.enableBlend();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.setShaderTexture(0, WorldRendererAccess.getEndSky());
        BufferBuilder bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);

        for (int i = 0; i < 6; ++i) {
            matrices.pushPose();
            if (i == 1) {
                matrices.mulPose(Axis.XP.rotationDegrees(90.0F));
            }

            if (i == 2) {
                matrices.mulPose(Axis.XP.rotationDegrees(-90.0F));
            }

            if (i == 3) {
                matrices.mulPose(Axis.XP.rotationDegrees(180.0F));
            }

            if (i == 4) {
                matrices.mulPose(Axis.ZP.rotationDegrees(90.0F));
            }

            if (i == 5) {
                matrices.mulPose(Axis.ZP.rotationDegrees(-90.0F));
            }

            Matrix4f matrix4f = matrices.last().pose();
            bufferBuilder.addVertex(matrix4f, -100.0F, -100.0F, -100.0F).setUv(0.0F, 0.0F).setColor(40, 40, 40, (int) (255 * this.alpha));
            bufferBuilder.addVertex(matrix4f, -100.0F, -100.0F, 100.0F).setUv(0.0F, 16.0F).setColor(40, 40, 40, (int) (255 * this.alpha));
            bufferBuilder.addVertex(matrix4f, 100.0F, -100.0F, 100.0F).setUv(16.0F, 16.0F).setColor(40, 40, 40, (int) (255 * this.alpha));
            bufferBuilder.addVertex(matrix4f, 100.0F, -100.0F, -100.0F).setUv(16.0F, 0.0F).setColor(40, 40, 40, (int) (255 * this.alpha));
            matrices.popPose();
        }
        BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());

        this.renderDecorations(worldRendererAccess, matrices, projectionMatrix, tickDelta, this.alpha, fogCallback);

        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }
}
