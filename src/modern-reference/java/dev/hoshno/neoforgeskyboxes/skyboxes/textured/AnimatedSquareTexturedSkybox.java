package dev.hoshno.neoforgeskyboxes.skyboxes.textured;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.hoshno.neoforgeskyboxes.api.skyboxes.Skybox;
import dev.hoshno.neoforgeskyboxes.mixin.skybox.WorldRendererAccess;
import dev.hoshno.neoforgeskyboxes.skyboxes.AbstractSkybox;
import dev.hoshno.neoforgeskyboxes.skyboxes.SkyboxType;
import dev.hoshno.neoforgeskyboxes.util.object.*;
import net.minecraft.client.Camera;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public class AnimatedSquareTexturedSkybox extends SquareTexturedSkybox {
    public static final Codec<AnimatedSquareTexturedSkybox> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Properties.CODEC.fieldOf("properties").forGetter(AbstractSkybox::getProperties),
            Conditions.CODEC.optionalFieldOf("conditions", Conditions.DEFAULT).forGetter(AbstractSkybox::getConditions),
            Decorations.CODEC.optionalFieldOf("decorations", Decorations.DEFAULT).forGetter(AbstractSkybox::getDecorations),
            Blend.CODEC.optionalFieldOf("blend", Blend.DEFAULT).forGetter(TexturedSkybox::getBlend),
            Textures.CODEC.listOf().fieldOf("animationTextures").forGetter(AnimatedSquareTexturedSkybox::getAnimationTextures),
            Codec.FLOAT.fieldOf("fps").forGetter(AnimatedSquareTexturedSkybox::getFps)
    ).apply(instance, AnimatedSquareTexturedSkybox::new));
    private final List<Textures> animationTextures;
    private final float fps;
    private final long frameTimeMillis;
    private int count = 0;
    private long lastTime = 0L;

    public AnimatedSquareTexturedSkybox(Properties properties, Conditions conditions, Decorations decorations, Blend blend, List<Textures> animationTextures, float fps) {
        super(properties, conditions, decorations, blend, null);
        this.animationTextures = animationTextures;
        this.fps = fps;
        if (fps > 0 && fps <= 360) {
            this.frameTimeMillis = (long) (1000F / fps);
        } else {
            this.frameTimeMillis = 16L;
        }
    }

    @Override
    public SkyboxType<? extends Skybox> getType() {
        return SkyboxType.ANIMATED_SQUARE_TEXTURED_SKYBOX;
    }

    @Override
    public void renderSkybox(WorldRendererAccess worldRendererAccess, PoseStack matrices, float tickDelta, Camera camera, boolean thickFog, Runnable runnable) {
        if (this.lastTime == 0L) this.lastTime = System.currentTimeMillis();
        this.textures = this.getAnimationTextures().get(this.count);

        super.renderSkybox(worldRendererAccess, matrices, tickDelta, camera, thickFog, runnable);

        if (System.currentTimeMillis() >= (this.lastTime + this.frameTimeMillis)) {
            if (this.count < this.getAnimationTextures().size()) {
                if (this.count + 1 == this.getAnimationTextures().size()) {
                    this.count = 0;
                } else {
                    this.count++;
                }
            }
            this.lastTime = System.currentTimeMillis();
        }
    }

    public List<Textures> getAnimationTextures() {
        return this.animationTextures;
    }

    public float getFps() {
        return this.fps;
    }

    @Override
    public List<ResourceLocation> getTexturesToRegister() {
        return this.animationTextures.stream()
                .flatMap(textures -> List.of(
                        textures.getNorth(), textures.getSouth(), textures.getEast(),
                        textures.getWest(), textures.getTop(), textures.getBottom()
                ).stream())
                .map(Texture::getTextureId)
                .distinct()
                .toList();
    }
}
