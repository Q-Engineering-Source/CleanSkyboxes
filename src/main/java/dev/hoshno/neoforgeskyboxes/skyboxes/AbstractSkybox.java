package dev.hoshno.neoforgeskyboxes.skyboxes;

import com.mojang.blaze3d.vertex.*;

import net.minecraft.client.Camera;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.hoshno.neoforgeskyboxes.NeoforgeSkyboxes;
import dev.hoshno.neoforgeskyboxes.api.skyboxes.FSBSkybox;
import dev.hoshno.neoforgeskyboxes.api.skyboxes.Skybox;
import dev.hoshno.neoforgeskyboxes.mixin.skybox.WorldRendererAccess;
import dev.hoshno.neoforgeskyboxes.util.Utils;
import dev.hoshno.neoforgeskyboxes.util.object.Conditions;
import dev.hoshno.neoforgeskyboxes.util.object.Decorations;
import dev.hoshno.neoforgeskyboxes.util.object.Properties;
import dev.hoshno.neoforgeskyboxes.util.object.Weather;
import net.minecraft.world.level.material.FogType;
import net.minecraft.client.Minecraft;
import com.mojang.blaze3d.vertex.VertexBuffer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.*;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.Mth;
import com.mojang.math.Axis;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.resources.ResourceKey;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.Objects;

/**
 * All classes that implement {@link AbstractSkybox} should
 * have a default constructor as it is required when checking
 * the type of the skybox.
 */
public abstract class AbstractSkybox implements FSBSkybox {

    /**
     * The current alpha for the skybox. Expects all skyboxes extending this to accommodate this.
     * This variable is responsible for fading in/out skyboxes.
     */
    public transient float alpha;
    protected Properties properties = Properties.DEFAULT;
    protected Conditions conditions = Conditions.DEFAULT;
    protected Decorations decorations = Decorations.DEFAULT;

    private int lastTime = -2;
    private float conditionAlpha = 0f;


    protected AbstractSkybox() {
    }

    protected AbstractSkybox(Properties properties, Conditions conditions, Decorations decorations) {
        this.properties = properties;
        this.conditions = conditions;
        this.decorations = decorations;
    }

    @Override
    public void tick(ClientLevel ClientLevel) {
        this.updateAlpha();
    }

    /**
     * Calculates the alpha value for the current time and conditions and returns it.
     *
     * @return The new alpha value.
     */
    @Override
    public final float updateAlpha() {
        int currentTime = (int) (Objects.requireNonNull(Minecraft.getInstance().level).getDayTime() % 24000);

        boolean condition = this.checkConditions();

        float fadeAlpha = 1f;
        if (this.properties.getFade().isAlwaysOn()) {
            this.conditionAlpha = Utils.calculateConditionAlphaValue(1f, 0f, this.conditionAlpha, condition ? this.properties.getTransitionInDuration() : this.properties.getTransitionOutDuration(), condition);
        } else {
            fadeAlpha = Utils.calculateFadeAlphaValue(1f, 0f, currentTime, this.properties.getFade().getStartFadeIn(), this.properties.getFade().getEndFadeIn(), this.properties.getFade().getStartFadeOut(), this.properties.getFade().getEndFadeOut());

            if (this.lastTime == currentTime - 1 || this.lastTime == currentTime) { // Check if time is ticking or if time is same (doDaylightCycle gamerule)
                this.conditionAlpha = Utils.calculateConditionAlphaValue(1f, 0f, this.conditionAlpha, condition ? this.properties.getTransitionInDuration() : this.properties.getTransitionOutDuration(), condition);
            } else {
                this.conditionAlpha = Utils.calculateConditionAlphaValue(1f, 0f, this.conditionAlpha, NeoforgeSkyboxes.config().generalSettings.unexpectedTransitionDuration, condition);
            }
        }

        this.alpha = (fadeAlpha * this.conditionAlpha) * (this.properties.getMaxAlpha() - this.properties.getMinAlpha()) + this.properties.getMinAlpha();

        this.alpha = Mth.clamp(this.alpha, this.properties.getMinAlpha(), this.properties.getMaxAlpha());
        this.lastTime = currentTime;

        return this.alpha;
    }

    /**
     * @return Whether all conditions were met
     */
    protected boolean checkConditions() {
        return this.checkDimensions() && this.checkWorlds() && this.checkBiomes() && this.checkXRanges() &&
                this.checkYRanges() && this.checkZRanges() && this.checkWeather() && this.checkEffects() &&
                this.checkLoop();
    }

    /**
     * @return Whether the current biomes and dimensions are valid for this skybox.
     */
    protected boolean checkBiomes() {
        Minecraft client = Minecraft.getInstance();
        Objects.requireNonNull(client.level);
        Objects.requireNonNull(client.player);
        return this.conditions.getBiomes().isEmpty() || this.conditions.getBiomes().contains(client.level.registryAccess().registryOrThrow(Registries.BIOME).getKey(client.level.getBiome(client.player.blockPosition()).value()));
    }

    /**
     * @return Whether the current dimension ResourceLocation is valid for this skybox
     */
    protected boolean checkDimensions() {
        Minecraft client = Minecraft.getInstance();
        Objects.requireNonNull(client.level);
        return this.conditions.getDimensions().isEmpty() || this.conditions.getDimensions().contains(client.level.dimension().location());
    }

    /**
     * @return Whether the current dimension sky effect is valid for this skybox
     */
    protected boolean checkWorlds() {
        Minecraft client = Minecraft.getInstance();
        Objects.requireNonNull(client.level);
        return this.conditions.getWorlds().isEmpty() || this.conditions.getWorlds().contains(client.level.dimensionType().effectsLocation());
    }

    /*
		Check if an effect that should prevent skybox from showing
     */
    protected boolean checkEffects() {
        Minecraft client = Minecraft.getInstance();
        Objects.requireNonNull(client.level);

        Camera camera = client.gameRenderer.getMainCamera();

        if (this.conditions.getEffects().isEmpty()) {
            // Vanilla checks
            boolean thickFog = client.level.effects().isFoggyAt(Mth.floor(camera.getPosition().x()), Mth.floor(camera.getPosition().y())) || client.gui.getBossOverlay().shouldCreateWorldFog();
            if (thickFog) {
                // Render skybox in thick fog, enabled by default
                return this.properties.isRenderInThickFog();
            }

            FogType FogType = camera.getFluidInCamera();
            if (FogType == FogType.POWDER_SNOW || FogType == FogType.LAVA)
                return false;

            return !(camera.getEntity() instanceof LivingEntity livingEntity) || (!livingEntity.hasEffect(MobEffects.BLINDNESS) && !livingEntity.hasEffect(MobEffects.DARKNESS));

        } else {
            if (camera.getEntity() instanceof LivingEntity livingEntity) {
                return this.conditions.getEffects().stream().noneMatch(effectId -> client.level.registryAccess().registryOrThrow(Registries.MOB_EFFECT).getHolder(ResourceKey.create(Registries.MOB_EFFECT, effectId)).filter(livingEntity::hasEffect).isPresent());
            }
        }
        return true;
    }

    /**
     * @return Whether the current x values are valid for this skybox.
     */
    protected boolean checkXRanges() {
        double playerX = Objects.requireNonNull(Minecraft.getInstance().player).getX();
        return Utils.checkRanges(playerX, this.conditions.getXRanges());
    }

    /**
     * @return Whether the current y values are valid for this skybox.
     */
    protected boolean checkYRanges() {
        double playerY = Objects.requireNonNull(Minecraft.getInstance().player).getY();
        return Utils.checkRanges(playerY, this.conditions.getYRanges());
    }

    /**
     * @return Whether the current z values are valid for this skybox.
     */
    protected boolean checkZRanges() {
        double playerZ = Objects.requireNonNull(Minecraft.getInstance().player).getZ();
        return Utils.checkRanges(playerZ, this.conditions.getZRanges());
    }

    /**
     * @return Whether the current loop is valid for this skybox.
     */
    protected boolean checkLoop() {
        if (!this.conditions.getLoop().getRanges().isEmpty() && this.conditions.getLoop().getDays() > 0) {
            double currentTime = Objects.requireNonNull(Minecraft.getInstance().level).getDayTime() - this.properties.getFade().getStartFadeIn();
            while (currentTime < 0) {
                currentTime += 24000 * this.conditions.getLoop().getDays();
            }

            double currentDay = (currentTime / 24000D) % this.conditions.getLoop().getDays();

            return Utils.checkRanges(currentDay, this.conditions.getLoop().getRanges());
        }
        return true;
    }

    /**
     * @return Whether the current weather is valid for this skybox.
     */
    protected boolean checkWeather() {
        ClientLevel world = Objects.requireNonNull(Minecraft.getInstance().level);
        LocalPlayer player = Objects.requireNonNull(Minecraft.getInstance().player);
        Biome.Precipitation precipitation = world.getBiome(player.blockPosition()).value().getPrecipitationAt(player.blockPosition());
        if (!this.conditions.getWeathers().isEmpty()) {
            if (this.conditions.getWeathers().contains(Weather.THUNDER) && world.isThundering()) {
                return true;
            }
            if (this.conditions.getWeathers().contains(Weather.RAIN) && world.isRaining() && !world.isThundering()) {
                return true;
            }
            if (this.conditions.getWeathers().contains(Weather.SNOW) && world.isRaining() && precipitation == Biome.Precipitation.SNOW) {
                return true;
            }
            if (this.conditions.getWeathers().contains(Weather.BIOME_RAIN) && world.isRaining() && precipitation == Biome.Precipitation.RAIN) {
                return true;
            }
            return this.conditions.getWeathers().contains(Weather.CLEAR) && !world.isRaining() && !world.isThundering();
        } else {
            return true;
        }
    }

    public abstract SkyboxType<? extends Skybox> getType();

    public void renderDecorations(WorldRendererAccess worldRendererAccess, PoseStack PoseStack, Matrix4f projectionMatrix, float tickDelta, float alpha, Runnable fogCallback) {
        RenderSystem.enableBlend();
        Vector3f rotationStatic = this.decorations.getRotation().getStatic();
        Vector3f rotationAxis = this.decorations.getRotation().getAxis();
        ClientLevel world = Minecraft.getInstance().level;
        assert world != null;

        // Custom Blender
        this.decorations.getBlend().applyBlendFunc(alpha);
        PoseStack.pushPose();

        // axis rotation
        PoseStack.mulPose(Axis.XP.rotationDegrees(rotationAxis.x()));
        PoseStack.mulPose(Axis.YP.rotationDegrees(rotationAxis.y()));
        PoseStack.mulPose(Axis.ZP.rotationDegrees(rotationAxis.z()));

        // Vanilla rotation
        //PoseStack.mulPose(Axis.YP.rotationDegrees(-90.0F));
        // Iris Compat
        //PoseStack.mulPose(Axis.ZP.rotationDegrees(IrisCompat.getSunPathRotation()));
        //PoseStack.mulPose(Axis.XP.rotationDegrees(world.getTimeOfDay(tickDelta) * 360.0F * this.decorations.getRotation().getRotationSpeed()));

        // Custom rotation
        double timeRotationX = Utils.calculateRotation(this.decorations.getRotation().getRotationSpeedX(), this.decorations.getRotation().getTimeShift().x(), this.decorations.getRotation().getSkyboxRotation(), world);
        double timeRotationY = Utils.calculateRotation(this.decorations.getRotation().getRotationSpeedY(), this.decorations.getRotation().getTimeShift().y(), this.decorations.getRotation().getSkyboxRotation(), world);
        double timeRotationZ = Utils.calculateRotation(this.decorations.getRotation().getRotationSpeedZ(), this.decorations.getRotation().getTimeShift().z(), this.decorations.getRotation().getSkyboxRotation(), world);
        PoseStack.mulPose(Axis.XP.rotationDegrees((float) timeRotationX));
        PoseStack.mulPose(Axis.YP.rotationDegrees((float) timeRotationY));
        PoseStack.mulPose(Axis.ZP.rotationDegrees((float) timeRotationZ));

        // axis rotation
        PoseStack.mulPose(Axis.ZN.rotationDegrees(rotationAxis.z()));
        PoseStack.mulPose(Axis.YN.rotationDegrees(rotationAxis.y()));
        PoseStack.mulPose(Axis.XN.rotationDegrees(rotationAxis.x()));

        // static rotation
        PoseStack.mulPose(Axis.XP.rotationDegrees(rotationStatic.x()));
        PoseStack.mulPose(Axis.YP.rotationDegrees(rotationStatic.y()));
        PoseStack.mulPose(Axis.ZP.rotationDegrees(rotationStatic.z()));

        Matrix4f matrix4f2 = PoseStack.last().pose();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        // Sun
        if (this.decorations.isSunEnabled()) {
            RenderSystem.setShaderTexture(0, this.decorations.getSunTexture());
            BufferBuilder bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
            bufferBuilder.addVertex(matrix4f2, -30.0F, 100.0F, -30.0F).setUv(0.0F, 0.0F);
            bufferBuilder.addVertex(matrix4f2, 30.0F, 100.0F, -30.0F).setUv(1.0F, 0.0F);
            bufferBuilder.addVertex(matrix4f2, 30.0F, 100.0F, 30.0F).setUv(1.0F, 1.0F);
            bufferBuilder.addVertex(matrix4f2, -30.0F, 100.0F, 30.0F).setUv(0.0F, 1.0F);
            BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());
        }
        // Moon
        if (this.decorations.isMoonEnabled()) {
            RenderSystem.setShaderTexture(0, this.decorations.getMoonTexture());
            int moonPhase = world.getMoonPhase();
            int xCoord = moonPhase % 4;
            int yCoord = moonPhase / 4 % 2;
            float startX = xCoord / 4.0F;
            float startY = yCoord / 2.0F;
            float endX = (xCoord + 1) / 4.0F;
            float endY = (yCoord + 1) / 2.0F;
            BufferBuilder bufferBuilder = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
            bufferBuilder.addVertex(matrix4f2, -20.0F, -100.0F, 20.0F).setUv(endX, endY);
            bufferBuilder.addVertex(matrix4f2, 20.0F, -100.0F, 20.0F).setUv(startX, endY);
            bufferBuilder.addVertex(matrix4f2, 20.0F, -100.0F, -20.0F).setUv(startX, startY);
            bufferBuilder.addVertex(matrix4f2, -20.0F, -100.0F, -20.0F).setUv(endX, startY);
            BufferUploader.drawWithShader(bufferBuilder.buildOrThrow());
        }
        // Stars
        if (this.decorations.isStarsEnabled()) {
            float i = 1.0F - world.getRainLevel(tickDelta);
            float brightness = world.getStarBrightness(tickDelta) * i;
            if (brightness > 0.0F) {
                RenderSystem.setShaderColor(brightness, brightness, brightness, brightness);
                FogRenderer.setupNoFog();
                worldRendererAccess.getStarsBuffer().bind();
                worldRendererAccess.getStarsBuffer().drawWithShader(PoseStack.last().pose(), projectionMatrix, GameRenderer.getPositionShader());
                VertexBuffer.unbind();
                fogCallback.run();
            }
        }
        PoseStack.popPose();

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
    }

    @Override
    public Decorations getDecorations() {
        return this.decorations;
    }

    @Override
    public Properties getProperties() {
        return this.properties;
    }

    @Override
    public Conditions getConditions() {
        return this.conditions;
    }

    @Override
    public float getAlpha() {
        return this.alpha;
    }

    @Override
    public int getPriority() {
        return this.properties.getPriority();
    }

    @Override
    public boolean isActive() {
        return this.getAlpha() != 0F;
    }
}
