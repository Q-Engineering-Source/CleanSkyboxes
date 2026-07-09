package dev.hoshno.neoforgeskyboxes.skyboxes;

import com.mojang.blaze3d.vertex.*;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.hoshno.neoforgeskyboxes.api.skyboxes.Skybox;
import dev.hoshno.neoforgeskyboxes.mixin.skybox.WorldRendererAccess;
import dev.hoshno.neoforgeskyboxes.util.object.*;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.*;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import org.joml.Matrix4f;

public class MonoColorSkybox extends AbstractSkybox {
    public static Codec<MonoColorSkybox> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Properties.CODEC.fieldOf("properties").forGetter(AbstractSkybox::getProperties),
            Conditions.CODEC.optionalFieldOf("conditions", Conditions.DEFAULT).forGetter(AbstractSkybox::getConditions),
            Decorations.CODEC.optionalFieldOf("decorations", Decorations.DEFAULT).forGetter(AbstractSkybox::getDecorations),
            RGBA.CODEC.optionalFieldOf("color", RGBA.DEFAULT).forGetter(MonoColorSkybox::getColor),
            Blend.CODEC.optionalFieldOf("blend", Blend.DEFAULT).forGetter(MonoColorSkybox::getBlend)
    ).apply(instance, MonoColorSkybox::new));
    public RGBA color = RGBA.DEFAULT;
    public Blend blend = Blend.DEFAULT;

    public MonoColorSkybox() {
    }

    public MonoColorSkybox(Properties properties, Conditions conditions, Decorations decorations, RGBA color, Blend blend) {
        super(properties, conditions, decorations);
        this.color = color;
        this.blend = blend;
    }

    @Override
    public SkyboxType<? extends Skybox> getType() {
        return SkyboxType.MONO_COLOR_SKYBOX;
    }

    @Override
    public void render(WorldRendererAccess worldRendererAccess, PoseStack matrices, Matrix4f projectionMatrix, float tickDelta, Camera camera, boolean thickFog, Runnable fogCallback) {
        if (this.alpha > 0) {
            RenderSystem.depthMask(false);
            RenderSystem.enableBlend();
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            this.blend.applyBlendFunc(this.alpha);
            BufferBuilder bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

            for (int i = 0; i < 6; ++i) {
                matrices.pushPose();
                if (i == 1) {
                    matrices.mulPose(Axis.XP.rotationDegrees(90.0F));
                } else if (i == 2) {
                    matrices.mulPose(Axis.XP.rotationDegrees(-90.0F));
                    matrices.mulPose(Axis.YP.rotationDegrees(180.0F));
                } else if (i == 3) {
                    matrices.mulPose(Axis.XP.rotationDegrees(180.0F));
                } else if (i == 4) {
                    matrices.mulPose(Axis.ZP.rotationDegrees(90.0F));
                    matrices.mulPose(Axis.YP.rotationDegrees(-90.0F));
                } else if (i == 5) {
                    matrices.mulPose(Axis.ZP.rotationDegrees(-90.0F));
                    matrices.mulPose(Axis.YP.rotationDegrees(90.0F));
                }

                Matrix4f matrix4f = matrices.last().pose();
                bufferBuilder.addVertex(matrix4f, -100.0F, -100.0F, -100.0F).setColor(this.color.getRed(), this.color.getGreen(), this.color.getBlue(), this.alpha);
                bufferBuilder.addVertex(matrix4f, -100.0F, -100.0F, 100.0F).setColor(this.color.getRed(), this.color.getGreen(), this.color.getBlue(), this.alpha);
                bufferBuilder.addVertex(matrix4f, 100.0F, -100.0F, 100.0F).setColor(this.color.getRed(), this.color.getGreen(), this.color.getBlue(), this.alpha);
                bufferBuilder.addVertex(matrix4f, 100.0F, -100.0F, -100.0F).setColor(this.color.getRed(), this.color.getGreen(), this.color.getBlue(), this.alpha);
                matrices.popPose();
            }
            BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());

            this.renderDecorations(worldRendererAccess, matrices, projectionMatrix, tickDelta, this.alpha, fogCallback);

            RenderSystem.disableBlend();
            RenderSystem.depthMask(true);
        }
    }

    public RGBA getColor() {
        return this.color;
    }

    public Blend getBlend() {
        return this.blend;
    }
}
