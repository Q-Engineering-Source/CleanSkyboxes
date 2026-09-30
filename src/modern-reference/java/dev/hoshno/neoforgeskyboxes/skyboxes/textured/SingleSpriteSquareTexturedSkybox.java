package dev.hoshno.neoforgeskyboxes.skyboxes.textured;

import com.mojang.blaze3d.vertex.*;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.hoshno.neoforgeskyboxes.api.skyboxes.Skybox;
import dev.hoshno.neoforgeskyboxes.mixin.skybox.WorldRendererAccess;
import dev.hoshno.neoforgeskyboxes.skyboxes.AbstractSkybox;
import dev.hoshno.neoforgeskyboxes.skyboxes.SkyboxType;
import dev.hoshno.neoforgeskyboxes.util.object.*;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.*;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import org.joml.Matrix4f;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

public class SingleSpriteSquareTexturedSkybox extends TexturedSkybox {
    public static Codec<SingleSpriteSquareTexturedSkybox> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Properties.CODEC.fieldOf("properties").forGetter(AbstractSkybox::getProperties),
            Conditions.CODEC.optionalFieldOf("conditions", Conditions.DEFAULT).forGetter(AbstractSkybox::getConditions),
            Decorations.CODEC.optionalFieldOf("decorations", Decorations.DEFAULT).forGetter(AbstractSkybox::getDecorations),
            Blend.CODEC.optionalFieldOf("blend", Blend.DEFAULT).forGetter(TexturedSkybox::getBlend),
            Texture.CODEC.fieldOf("texture").forGetter(SingleSpriteSquareTexturedSkybox::getTexture)
    ).apply(instance, SingleSpriteSquareTexturedSkybox::new));
    private final UVRanges uvRanges;
    protected Texture texture;

    public SingleSpriteSquareTexturedSkybox(Properties properties, Conditions conditions, Decorations decorations, Blend blend, Texture texture) {
        super(properties, conditions, decorations, blend);
        this.texture = texture;
        this.uvRanges = new UVRanges(
                texture.withUV(1.0F / 3.0F, 1.0F / 2.0F, 2.0F / 3.0F, 1),
                texture.withUV(2.0F / 3.0F, 0, 1, 1.0F / 2.0F),
                texture.withUV(2.0F / 3.0F, 1.0F / 2.0F, 1, 1),
                texture.withUV(0, 1.0F / 2.0F, 1.0F / 3.0F, 1),
                texture.withUV(1.0F / 3.0F, 0, 2.0F / 3.0F, 1.0F / 2.0F),
                texture.withUV(0, 0, 1.0F / 3.0F, 1.0F / 2.0F)
        );
    }

    @Override
    public SkyboxType<? extends Skybox> getType() {
        return SkyboxType.SINGLE_SPRITE_SQUARE_TEXTURED_SKYBOX;
    }

    public Texture getTexture() {
        return this.texture;
    }

    @Override
    public List<ResourceLocation> getTexturesToRegister() {
        return List.of(this.texture.getTextureId());
    }

    @Override
    public void renderSkybox(WorldRendererAccess worldRendererAccess, PoseStack matrices, float tickDelta, Camera camera, boolean thickFog, Runnable runnable) {
        BufferBuilder bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        RenderSystem.setShaderTexture(0, texture.getTextureId());
        for (int i = 0; i < 6; ++i) {
            // 0 = bottom
            // 1 = north
            // 2 = south
            // 3 = top
            // 4 = east
            // 5 = west
            UVRange tex = this.uvRanges.byId(i);
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
            bufferBuilder.addVertex(matrix4f, -100.0F, -100.0F, -100.0F).setUv(tex.getMinU(), tex.getMinV());
            bufferBuilder.addVertex(matrix4f, -100.0F, -100.0F, 100.0F).setUv(tex.getMinU(), tex.getMaxV());
            bufferBuilder.addVertex(matrix4f, 100.0F, -100.0F, 100.0F).setUv(tex.getMaxU(), tex.getMaxV());
            bufferBuilder.addVertex(matrix4f, 100.0F, -100.0F, -100.0F).setUv(tex.getMaxU(), tex.getMinV());
            matrices.popPose();
        }
        BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());
    }
}
