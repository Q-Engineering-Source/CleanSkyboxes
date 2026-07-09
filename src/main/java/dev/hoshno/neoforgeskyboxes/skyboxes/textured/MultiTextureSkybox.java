package dev.hoshno.neoforgeskyboxes.skyboxes.textured;

import com.mojang.blaze3d.vertex.*;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.hoshno.neoforgeskyboxes.api.skyboxes.Skybox;
import dev.hoshno.neoforgeskyboxes.mixin.skybox.WorldRendererAccess;
import dev.hoshno.neoforgeskyboxes.skyboxes.AbstractSkybox;
import dev.hoshno.neoforgeskyboxes.skyboxes.SkyboxType;
import dev.hoshno.neoforgeskyboxes.util.Utils;
import dev.hoshno.neoforgeskyboxes.util.object.*;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.*;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.Util;
import com.mojang.math.Axis;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

public class MultiTextureSkybox extends TexturedSkybox {
    public static Codec<MultiTextureSkybox> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Properties.CODEC.fieldOf("properties").forGetter(AbstractSkybox::getProperties),
            Conditions.CODEC.optionalFieldOf("conditions", Conditions.DEFAULT).forGetter(AbstractSkybox::getConditions),
            Decorations.CODEC.optionalFieldOf("decorations", Decorations.DEFAULT).forGetter(AbstractSkybox::getDecorations),
            Blend.CODEC.optionalFieldOf("blend", Blend.DEFAULT).forGetter(TexturedSkybox::getBlend),
            Animation.CODEC.listOf().optionalFieldOf("animations", new ArrayList<>()).forGetter(MultiTextureSkybox::getAnimations)
    ).apply(instance, MultiTextureSkybox::new));
    protected final List<Animation> animations;
    private final UVRanges uvRanges;

    private final float quadSize = 100F;
    private final UVRange quad = new UVRange(-this.quadSize, -this.quadSize, this.quadSize, this.quadSize);

    public MultiTextureSkybox(Properties properties, Conditions conditions, Decorations decorations, Blend blend, List<Animation> animations) {
        super(properties, conditions, decorations, blend);
        this.animations = animations;
        this.uvRanges = Util.make(() -> new UVRanges(
                new UVRange(1.0F / 3.0F, 1.0F / 2.0F, 2.0F / 3.0F, 1),
                new UVRange(2.0F / 3.0F, 0, 1, 1.0F / 2.0F),
                new UVRange(2.0F / 3.0F, 1.0F / 2.0F, 1, 1),
                new UVRange(0, 1.0F / 2.0F, 1.0F / 3.0F, 1),
                new UVRange(1.0F / 3.0F, 0, 2.0F / 3.0F, 1.0F / 2.0F),
                new UVRange(0, 0, 1.0F / 3.0F, 1.0F / 2.0F)
        ));
    }

    @Override
    public SkyboxType<? extends Skybox> getType() {
        return SkyboxType.MULTI_TEXTURE_SKYBOX;
    }

    @Override
    public void renderSkybox(WorldRendererAccess worldRendererAccess, PoseStack matrices, float tickDelta, Camera camera, boolean thickFog, Runnable runnable) {
        for (int i = 0; i < 6; ++i) {
            // 0 = bottom
            // 1 = north
            // 2 = south
            // 3 = top
            // 4 = east
            // 5 = west
            UVRange faceUVRange = this.uvRanges.byId(i);
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

            // animations
            for (Animation animation : this.animations) {
                animation.tick();
                UVRange intersect = Utils.findUVIntersection(faceUVRange, animation.getUvRanges());
                if (intersect != null && animation.getCurrentFrame() != null) {
                    BufferBuilder bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
                    UVRange intersectionOnCurrentTexture = Utils.mapUVRanges(faceUVRange, this.quad, intersect);
                    UVRange intersectionOnCurrentFrame = Utils.mapUVRanges(animation.getUvRanges(), animation.getCurrentFrame(), intersect);

                    // Render the quad at the calculated position
                    RenderSystem.setShaderTexture(0, animation.getTexture().getTextureId());

                    bufferBuilder.addVertex(matrix4f, intersectionOnCurrentTexture.getMinU(), -this.quadSize, intersectionOnCurrentTexture.getMinV()).setUv(intersectionOnCurrentFrame.getMinU(), intersectionOnCurrentFrame.getMinV());
                    bufferBuilder.addVertex(matrix4f, intersectionOnCurrentTexture.getMinU(), -this.quadSize, intersectionOnCurrentTexture.getMaxV()).setUv(intersectionOnCurrentFrame.getMinU(), intersectionOnCurrentFrame.getMaxV());
                    bufferBuilder.addVertex(matrix4f, intersectionOnCurrentTexture.getMaxU(), -this.quadSize, intersectionOnCurrentTexture.getMaxV()).setUv(intersectionOnCurrentFrame.getMaxU(), intersectionOnCurrentFrame.getMaxV());
                    bufferBuilder.addVertex(matrix4f, intersectionOnCurrentTexture.getMaxU(), -this.quadSize, intersectionOnCurrentTexture.getMinV()).setUv(intersectionOnCurrentFrame.getMaxU(), intersectionOnCurrentFrame.getMinV());
                    BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());
                }
            }

            matrices.popPose();
        }
    }

    public List<Animation> getAnimations() {
        return this.animations;
    }
}
