package dev.hoshno.neoforgeskyboxes.skyboxes;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.VertexBuffer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import dev.hoshno.neoforgeskyboxes.mixin.skybox.RenderGlobalSkyAccessor;
import org.lwjgl.opengl.GL11;

/** Immediate-mode renderer for the first Cleanroom ported skybox types. */
public final class SkyboxRenderer {
    private static final float HALF_WIDTH = 100.0F;
    private static final ResourceLocation END_SKY = new ResourceLocation("textures/environment/end_sky.png");

    private SkyboxRenderer() {
    }

    public static void render(Minecraft minecraft, SkyboxDefinition skybox, float partialTicks, RenderGlobalSkyAccessor renderGlobal) {
        float alpha = skybox.getAlpha();
        if (alpha <= 0.0F) {
            return;
        }

        GlStateManager.pushMatrix();
        if (skybox.getType() != SkyboxDefinition.Type.END) {
            if (skybox.isOptifineLayer()) {
                applyOptifineRotation(minecraft.world, skybox, partialTicks);
            } else {
                applyRotation(minecraft, skybox);
            }
        }
        GlStateManager.depthMask(false);
        GlStateManager.disableAlpha();
        GlStateManager.disableCull();
        GlStateManager.enableBlend();
        float[] colorModulation = applyBlend(skybox.getBlendSettings(), alpha);

        if (skybox.getType() == SkyboxDefinition.Type.MONOCOLOR) {
            GlStateManager.disableTexture2D();
            renderMonocolor(skybox, colorModulation);
        } else if (skybox.getType() == SkyboxDefinition.Type.END) {
            GlStateManager.enableTexture2D();
            renderEndSky(minecraft, colorModulation);
        } else if (skybox.getType() == SkyboxDefinition.Type.OVERWORLD) {
            renderOverworldSky(minecraft, skybox.getBlendSettings(), colorModulation, alpha, partialTicks, renderGlobal);
        } else if (skybox.getType() == SkyboxDefinition.Type.SQUARE_TEXTURED
                || skybox.getType() == SkyboxDefinition.Type.SINGLE_SPRITE_SQUARE_TEXTURED
                || skybox.getType() == SkyboxDefinition.Type.ANIMATED_SQUARE_TEXTURED
                || skybox.getType() == SkyboxDefinition.Type.SINGLE_SPRITE_ANIMATED_SQUARE_TEXTURED) {
            GlStateManager.enableTexture2D();
            renderSquareTextured(minecraft, skybox, colorModulation);
        } else if (skybox.getType() == SkyboxDefinition.Type.MULTI_TEXTURE) {
            GlStateManager.enableTexture2D();
            renderMultiTexture(minecraft, skybox, colorModulation);
        }

        GlStateManager.popMatrix();
        renderDecorations(minecraft, skybox, alpha, partialTicks, renderGlobal);

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.enableTexture2D();
        GlStateManager.enableCull();
        GlStateManager.disableBlend();
        GlStateManager.depthMask(true);
        GlStateManager.enableAlpha();
        GlStateManager.enableFog();
    }

    private static void renderEndSky(Minecraft minecraft, float[] colorModulation) {
        minecraft.getTextureManager().bindTexture(END_SKY);
        for (int face = 0; face < 6; face++) {
            GlStateManager.pushMatrix();
            rotateEndFace(face);
            BufferBuilder buffer = Tessellator.getInstance().getBuffer();
            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
            addEndSkyVertex(buffer, -HALF_WIDTH, -HALF_WIDTH, -HALF_WIDTH, 0.0D, 0.0D, colorModulation);
            addEndSkyVertex(buffer, -HALF_WIDTH, -HALF_WIDTH, HALF_WIDTH, 0.0D, 16.0D, colorModulation);
            addEndSkyVertex(buffer, HALF_WIDTH, -HALF_WIDTH, HALF_WIDTH, 16.0D, 16.0D, colorModulation);
            addEndSkyVertex(buffer, HALF_WIDTH, -HALF_WIDTH, -HALF_WIDTH, 16.0D, 0.0D, colorModulation);
            Tessellator.getInstance().draw();
            GlStateManager.popMatrix();
        }
    }

    private static void renderOverworldSky(Minecraft minecraft, BlendSettings blendSettings, float[] colorModulation,
                                           float alpha, float partialTicks, RenderGlobalSkyAccessor renderGlobal) {
        World world = minecraft.world;
        Entity camera = minecraft.getRenderViewEntity();
        if (camera == null) {
            return;
        }

        Vec3d skyColor = world.getSkyColor(camera, partialTicks);
        GlStateManager.depthMask(false);
        GlStateManager.enableFog();
        GlStateManager.disableTexture2D();
        GlStateManager.color((float) skyColor.x * colorModulation[0], (float) skyColor.y * colorModulation[1],
                (float) skyColor.z * colorModulation[2], colorModulation[3]);
        drawSkyMesh(renderGlobal.getSkyVBO(), renderGlobal.getSkyDisplayList(), renderGlobal.isVboEnabled());
        GlStateManager.disableFog();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        RenderHelper.disableStandardItemLighting();

        float skyAngle = world.getCelestialAngle(partialTicks);
        float[] sunrise = world.provider.calcSunriseSunsetColors(skyAngle, partialTicks);
        if (sunrise != null) {
            GlStateManager.disableTexture2D();
            GlStateManager.shadeModel(GL11.GL_SMOOTH);
            colorModulation = applyBlend(blendSettings, alpha);
            GlStateManager.pushMatrix();
            GlStateManager.rotate(90.0F, 1.0F, 0.0F, 0.0F);
            GlStateManager.rotate(Math.sin(world.getCelestialAngleRadians(partialTicks)) < 0.0D ? 180.0F : 0.0F,
                    0.0F, 0.0F, 1.0F);
            GlStateManager.rotate(90.0F, 0.0F, 0.0F, 1.0F);

            BufferBuilder buffer = Tessellator.getInstance().getBuffer();
            buffer.begin(GL11.GL_TRIANGLE_FAN, DefaultVertexFormats.POSITION_COLOR);
            buffer.pos(0.0D, 100.0D, 0.0D)
                    .color(sunrise[0] * colorModulation[0], sunrise[1] * colorModulation[1],
                            sunrise[2] * colorModulation[2], sunrise[3] * colorModulation[3])
                    .endVertex();
            for (int i = 0; i <= 16; i++) {
                float angle = i * (float) (Math.PI * 2.0D) / 16.0F;
                float sin = (float) Math.sin(angle);
                float cos = (float) Math.cos(angle);
                buffer.pos(sin * 120.0F, cos * 120.0F, -cos * 40.0F * sunrise[3])
                        .color(sunrise[0] * colorModulation[0], sunrise[1] * colorModulation[1],
                                sunrise[2] * colorModulation[2], 0.0F)
                        .endVertex();
            }
            Tessellator.getInstance().draw();
            GlStateManager.popMatrix();
            GlStateManager.shadeModel(GL11.GL_FLAT);
            GlStateManager.enableTexture2D();
        }

        double eyeY = camera.lastTickPosY + (camera.posY - camera.lastTickPosY) * partialTicks + camera.getEyeHeight();
        if (eyeY < world.getHorizon()) {
            colorModulation = applyBlend(blendSettings, alpha);
            GlStateManager.color(0.0F, 0.0F, 0.0F, colorModulation[3]);
            GlStateManager.pushMatrix();
            GlStateManager.translate(0.0F, 12.0F, 0.0F);
            drawSkyMesh(renderGlobal.getDarkSkyVBO(), renderGlobal.getDarkSkyDisplayList(), renderGlobal.isVboEnabled());
            GlStateManager.popMatrix();
        }

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.depthMask(true);
        GlStateManager.disableBlend();
    }

    private static void drawSkyMesh(VertexBuffer buffer, int displayList, boolean vboEnabled) {
        if (vboEnabled && buffer != null) {
            buffer.bindBuffer();
            GlStateManager.glEnableClientState(GL11.GL_VERTEX_ARRAY);
            GlStateManager.glVertexPointer(3, GL11.GL_FLOAT, 12, 0);
            buffer.drawArrays(GL11.GL_QUADS);
            buffer.unbindBuffer();
            GlStateManager.glDisableClientState(GL11.GL_VERTEX_ARRAY);
        } else if (displayList >= 0) {
            GlStateManager.callList(displayList);
        }
    }

    private static void addEndSkyVertex(BufferBuilder buffer, double x, double y, double z, double u, double v,
                                        float[] colorModulation) {
        buffer.pos(x, y, z).tex(u, v)
                .color(40.0F / 255.0F * colorModulation[0], 40.0F / 255.0F * colorModulation[1],
                        40.0F / 255.0F * colorModulation[2], colorModulation[3]).endVertex();
    }

    private static void rotateEndFace(int face) {
        switch (face) {
            case 1:
                GlStateManager.rotate(90.0F, 1.0F, 0.0F, 0.0F);
                break;
            case 2:
                GlStateManager.rotate(-90.0F, 1.0F, 0.0F, 0.0F);
                break;
            case 3:
                GlStateManager.rotate(180.0F, 1.0F, 0.0F, 0.0F);
                break;
            case 4:
                GlStateManager.rotate(90.0F, 0.0F, 0.0F, 1.0F);
                break;
            case 5:
                GlStateManager.rotate(-90.0F, 0.0F, 0.0F, 1.0F);
                break;
            default:
                break;
        }
    }

    private static void renderMonocolor(SkyboxDefinition skybox, float[] colorModulation) {
        float vertexAlpha = colorModulation[3] * skybox.getColorAlpha();
        for (int face = 0; face < 6; face++) {
            GlStateManager.pushMatrix();
            rotateFace(face);
            BufferBuilder buffer = Tessellator.getInstance().getBuffer();
            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
            addColorVertex(buffer, -HALF_WIDTH, -HALF_WIDTH, -HALF_WIDTH, skybox, vertexAlpha, colorModulation);
            addColorVertex(buffer, -HALF_WIDTH, -HALF_WIDTH, HALF_WIDTH, skybox, vertexAlpha, colorModulation);
            addColorVertex(buffer, HALF_WIDTH, -HALF_WIDTH, HALF_WIDTH, skybox, vertexAlpha, colorModulation);
            addColorVertex(buffer, HALF_WIDTH, -HALF_WIDTH, -HALF_WIDTH, skybox, vertexAlpha, colorModulation);
            Tessellator.getInstance().draw();
            GlStateManager.popMatrix();
        }
    }

    private static void addColorVertex(BufferBuilder buffer, double x, double y, double z, SkyboxDefinition skybox,
                                       float alpha, float[] colorModulation) {
        buffer.pos(x, y, z)
                .color(skybox.getRed() * colorModulation[0], skybox.getGreen() * colorModulation[1],
                        skybox.getBlue() * colorModulation[2], alpha)
                .endVertex();
    }

    private static void renderSquareTextured(Minecraft minecraft, SkyboxDefinition skybox, float[] colorModulation) {
        ResourceLocation[] textures = skybox.getTexturesAt(System.currentTimeMillis());
        for (int face = 0; face < textures.length; face++) {
            float[] uv = skybox.getTextureUv(face);
            minecraft.getTextureManager().bindTexture(textures[face]);
            GlStateManager.pushMatrix();
            rotateFace(face);
            BufferBuilder buffer = Tessellator.getInstance().getBuffer();
            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
            addTextureVertex(buffer, -HALF_WIDTH, -HALF_WIDTH, -HALF_WIDTH, uv[0], uv[1], colorModulation);
            addTextureVertex(buffer, -HALF_WIDTH, -HALF_WIDTH, HALF_WIDTH, uv[0], uv[3], colorModulation);
            addTextureVertex(buffer, HALF_WIDTH, -HALF_WIDTH, HALF_WIDTH, uv[2], uv[3], colorModulation);
            addTextureVertex(buffer, HALF_WIDTH, -HALF_WIDTH, -HALF_WIDTH, uv[2], uv[1], colorModulation);
            Tessellator.getInstance().draw();
            GlStateManager.popMatrix();
        }
    }

    private static void renderMultiTexture(Minecraft minecraft, SkyboxDefinition skybox, float[] colorModulation) {
        float[][] faceRanges = atlasFaceRanges();
        float[] quad = new float[]{-HALF_WIDTH, -HALF_WIDTH, HALF_WIDTH, HALF_WIDTH};
        long now = System.currentTimeMillis();
        for (int face = 0; face < faceRanges.length; face++) {
            float[] faceRange = faceRanges[face];
            GlStateManager.pushMatrix();
            rotateFace(face);
            for (TextureAnimation animation : skybox.getTextureAnimations()) {
                float[] animationRange = animation.getUvRange();
                float[] overlap = intersect(faceRange, animationRange);
                if (overlap == null) {
                    continue;
                }
                float[] position = mapRange(faceRange, quad, overlap);
                float[] frame = animation.getCurrentFrame(now);
                if (frame == null) {
                    continue;
                }
                float[] uv = mapRange(animationRange, frame, overlap);
                minecraft.getTextureManager().bindTexture(animation.getTexture());
                BufferBuilder buffer = Tessellator.getInstance().getBuffer();
                buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
                addTextureVertex(buffer, position[0], -HALF_WIDTH, position[1], uv[0], uv[1], colorModulation);
                addTextureVertex(buffer, position[0], -HALF_WIDTH, position[3], uv[0], uv[3], colorModulation);
                addTextureVertex(buffer, position[2], -HALF_WIDTH, position[3], uv[2], uv[3], colorModulation);
                addTextureVertex(buffer, position[2], -HALF_WIDTH, position[1], uv[2], uv[1], colorModulation);
                Tessellator.getInstance().draw();
            }
            GlStateManager.popMatrix();
        }
    }

    private static float[] intersect(float[] first, float[] second) {
        float minU = Math.max(first[0], second[0]);
        float minV = Math.max(first[1], second[1]);
        float maxU = Math.min(first[2], second[2]);
        float maxV = Math.min(first[3], second[3]);
        return maxU >= minU && maxV >= minV ? new float[]{minU, minV, maxU, maxV} : null;
    }

    private static float[] mapRange(float[] input, float[] output, float[] intersection) {
        float u1 = (intersection[0] - input[0]) / (input[2] - input[0]) * (output[2] - output[0]) + output[0];
        float v1 = (intersection[1] - input[1]) / (input[3] - input[1]) * (output[3] - output[1]) + output[1];
        float u2 = (intersection[2] - input[0]) / (input[2] - input[0]) * (output[2] - output[0]) + output[0];
        float v2 = (intersection[3] - input[1]) / (input[3] - input[1]) * (output[3] - output[1]) + output[1];
        return new float[]{u1, v1, u2, v2};
    }

    private static float[][] atlasFaceRanges() {
        return new float[][]{
                {0.0F, 0.0F, 1.0F / 3.0F, 0.5F},
                {1.0F / 3.0F, 0.5F, 2.0F / 3.0F, 1.0F},
                {2.0F / 3.0F, 0.0F, 1.0F, 0.5F},
                {1.0F / 3.0F, 0.0F, 2.0F / 3.0F, 0.5F},
                {2.0F / 3.0F, 0.5F, 1.0F, 1.0F},
                {0.0F, 0.5F, 1.0F / 3.0F, 1.0F}
        };
    }

    private static void addTextureVertex(BufferBuilder buffer, double x, double y, double z, double u, double v,
                                         float[] colorModulation) {
        buffer.pos(x, y, z)
                .tex(u, v)
                .color(colorModulation[0], colorModulation[1], colorModulation[2], colorModulation[3])
                .endVertex();
    }

    private static void renderDecorations(Minecraft minecraft, SkyboxDefinition skybox, float alpha,
                                          float partialTicks, RenderGlobalSkyAccessor renderGlobal) {
        if (!skybox.isSunEnabled() && !skybox.isMoonEnabled() && !skybox.areStarsEnabled()) {
            return;
        }

        GlStateManager.pushMatrix();
        GlStateManager.enableTexture2D();
        GlStateManager.enableBlend();
        float[] colorModulation = applyBlend(skybox.getDecorationBlendSettings(), alpha);
        applyRotation(minecraft.world, skybox.isDecorationSkyboxRotation(), skybox.getDecorationStaticRotation(),
                skybox.getDecorationAxisRotation(), skybox.getDecorationTimeShift(), skybox.getDecorationRotationSpeed());

        if (skybox.isSunEnabled()) {
            drawCelestialQuad(minecraft, skybox.getSunTexture(), colorModulation,
                    30.0F, 100.0F, 0.0F, 0.0F, 1.0F, 1.0F);
        }
        if (skybox.isMoonEnabled()) {
            int phase = minecraft.world.getMoonPhase();
            int column = phase % 4;
            int row = phase / 4 % 2;
            float minU = column / 4.0F;
            float minV = row / 2.0F;
            float maxU = (column + 1) / 4.0F;
            float maxV = (row + 1) / 2.0F;
            drawMoon(minecraft, skybox.getMoonTexture(), colorModulation, minU, minV, maxU, maxV);
        }
        if (skybox.areStarsEnabled()) {
            renderStars(minecraft, colorModulation, partialTicks, renderGlobal);
        }

        GlStateManager.popMatrix();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    private static void drawCelestialQuad(Minecraft minecraft, ResourceLocation texture, float[] colorModulation,
                                          float halfWidth, float y, float minU, float minV, float maxU, float maxV) {
        minecraft.getTextureManager().bindTexture(texture);
        GlStateManager.color(colorModulation[0], colorModulation[1], colorModulation[2], colorModulation[3]);
        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        buffer.pos(-halfWidth, y, -halfWidth).tex(minU, minV)
                .color(colorModulation[0], colorModulation[1], colorModulation[2], colorModulation[3]).endVertex();
        buffer.pos(halfWidth, y, -halfWidth).tex(maxU, minV)
                .color(colorModulation[0], colorModulation[1], colorModulation[2], colorModulation[3]).endVertex();
        buffer.pos(halfWidth, y, halfWidth).tex(maxU, maxV)
                .color(colorModulation[0], colorModulation[1], colorModulation[2], colorModulation[3]).endVertex();
        buffer.pos(-halfWidth, y, halfWidth).tex(minU, maxV)
                .color(colorModulation[0], colorModulation[1], colorModulation[2], colorModulation[3]).endVertex();
        Tessellator.getInstance().draw();
    }

    private static void drawMoon(Minecraft minecraft, ResourceLocation texture, float[] colorModulation,
                                 float minU, float minV, float maxU, float maxV) {
        minecraft.getTextureManager().bindTexture(texture);
        GlStateManager.color(colorModulation[0], colorModulation[1], colorModulation[2], colorModulation[3]);
        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        buffer.pos(-20.0F, -100.0F, 20.0F).tex(maxU, maxV)
                .color(colorModulation[0], colorModulation[1], colorModulation[2], colorModulation[3]).endVertex();
        buffer.pos(20.0F, -100.0F, 20.0F).tex(minU, maxV)
                .color(colorModulation[0], colorModulation[1], colorModulation[2], colorModulation[3]).endVertex();
        buffer.pos(20.0F, -100.0F, -20.0F).tex(minU, minV)
                .color(colorModulation[0], colorModulation[1], colorModulation[2], colorModulation[3]).endVertex();
        buffer.pos(-20.0F, -100.0F, -20.0F).tex(maxU, minV)
                .color(colorModulation[0], colorModulation[1], colorModulation[2], colorModulation[3]).endVertex();
        Tessellator.getInstance().draw();
    }

    private static void renderStars(Minecraft minecraft, float[] colorModulation, float partialTicks,
                                    RenderGlobalSkyAccessor renderGlobal) {
        float rainAlpha = 1.0F - minecraft.world.getRainStrength(partialTicks);
        float brightness = minecraft.world.getStarBrightness(partialTicks) * rainAlpha;
        if (brightness <= 0.0F) {
            return;
        }

        GlStateManager.disableTexture2D();
        GlStateManager.color(brightness * colorModulation[0], brightness * colorModulation[1],
                brightness * colorModulation[2], brightness * colorModulation[3]);
        if (renderGlobal.isVboEnabled()) {
            VertexBuffer stars = renderGlobal.getStarVBO();
            if (stars != null) {
                stars.bindBuffer();
                GlStateManager.glEnableClientState(GL11.GL_VERTEX_ARRAY);
                GlStateManager.glVertexPointer(3, GL11.GL_FLOAT, 12, 0);
                stars.drawArrays(GL11.GL_QUADS);
                stars.unbindBuffer();
                GlStateManager.glDisableClientState(GL11.GL_VERTEX_ARRAY);
            }
        } else {
            int starList = renderGlobal.getStarGLCallList();
            if (starList >= 0) {
                GlStateManager.callList(starList);
            }
        }
        GlStateManager.enableTexture2D();
    }

    private static void rotateFace(int face) {
        switch (face) {
            case 1:
                GlStateManager.rotate(90.0F, 1.0F, 0.0F, 0.0F);
                break;
            case 2:
                GlStateManager.rotate(-90.0F, 1.0F, 0.0F, 0.0F);
                GlStateManager.rotate(180.0F, 0.0F, 1.0F, 0.0F);
                break;
            case 3:
                GlStateManager.rotate(180.0F, 1.0F, 0.0F, 0.0F);
                break;
            case 4:
                GlStateManager.rotate(90.0F, 0.0F, 0.0F, 1.0F);
                GlStateManager.rotate(-90.0F, 0.0F, 1.0F, 0.0F);
                break;
            case 5:
                GlStateManager.rotate(-90.0F, 0.0F, 0.0F, 1.0F);
                GlStateManager.rotate(90.0F, 0.0F, 1.0F, 0.0F);
                break;
            default:
                break;
        }
    }

    private static void applyRotation(Minecraft minecraft, SkyboxDefinition skybox) {
        applyRotation(minecraft.world, skybox.isSkyboxRotation(), skybox.getStaticRotation(),
                skybox.getAxisRotation(), skybox.getTimeShift(), skybox.getRotationSpeed());
    }

    private static void applyRotation(World world, boolean skyboxRotation, float[] fixed, float[] axis,
                                      int[] shift, float[] speed) {
        float[] timeRotation = new float[3];
        for (int i = 0; i < timeRotation.length; i++) {
            timeRotation[i] = calculateRotation(world, skyboxRotation, speed[i], shift[i]);
        }
        GlStateManager.rotate(axis[0], 1.0F, 0.0F, 0.0F);
        GlStateManager.rotate(axis[1], 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(axis[2], 0.0F, 0.0F, 1.0F);
        GlStateManager.rotate(timeRotation[0], 1.0F, 0.0F, 0.0F);
        GlStateManager.rotate(timeRotation[1], 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(timeRotation[2], 0.0F, 0.0F, 1.0F);
        GlStateManager.rotate(-axis[2], 0.0F, 0.0F, 1.0F);
        GlStateManager.rotate(-axis[1], 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(-axis[0], 1.0F, 0.0F, 0.0F);
        GlStateManager.rotate(fixed[0], 1.0F, 0.0F, 0.0F);
        GlStateManager.rotate(fixed[1], 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(fixed[2], 0.0F, 0.0F, 1.0F);
    }

    private static float calculateRotation(World world, boolean skyboxRotation, float speed, int timeShift) {
        if (speed == 0.0F) {
            return 0.0F;
        }
        double fraction = (world.getWorldTime() + timeShift) * speed / 24000.0D;
        double wrappedFraction = fraction - Math.floor(fraction);
        if (skyboxRotation) {
            return (float) (wrappedFraction * 360.0D);
        }
        long timeOfDay = (long) (24000.0D * wrappedFraction);
        return world.provider.calculateCelestialAngle(timeOfDay, 0.0F) * 360.0F;
    }

    private static void applyOptifineRotation(World world, SkyboxDefinition skybox, float partialTicks) {
        if (!skybox.isOptifineRotate()) {
            return;
        }
        float speed = skybox.getOptifineSpeed();
        float dayStart = 0.0F;
        if (speed != Math.round(speed)) {
            long currentLevelDay = (world.getWorldTime() + 18000L) / 24000L;
            double anglePerDay = speed % 1.0F;
            dayStart = (float) ((currentLevelDay * anglePerDay) % 1.0D);
        }
        float angle = -360.0F * (dayStart + world.getCelestialAngle(partialTicks) * speed);
        float[] axis = skybox.getOptifineAxis();
        GlStateManager.rotate(angle, axis[0], axis[1], axis[2]);
    }

    private static float[] applyBlend(BlendSettings blend, float alpha) {
        if (blend != null && blend.isCustom()) {
            GlStateManager.enableBlend();
            if (blend.isSeparateFunction()) {
                GlStateManager.tryBlendFuncSeparate(blend.getSourceFactor(), blend.getDestinationFactor(),
                        blend.getSourceFactorAlpha(), blend.getDestinationFactorAlpha());
            } else {
                GlStateManager.blendFunc(blend.getSourceFactor(), blend.getDestinationFactor());
            }
            GlStateManager.glBlendEquation(blend.getEquation());
            return new float[]{
                    blend.isRedAlphaEnabled() ? alpha : 1.0F,
                    blend.isGreenAlphaEnabled() ? alpha : 1.0F,
                    blend.isBlueAlphaEnabled() ? alpha : 1.0F,
                    blend.isAlphaEnabled() ? alpha : 1.0F
            };
        }

        String type = blend == null ? "" : blend.getType();
        if (type.startsWith("optifine_")) {
            return applyOptiFineBlend(type.substring("optifine_".length()), alpha);
        }

        GlStateManager.glBlendEquation(32774);
        switch (type) {
            case "disable":
                GlStateManager.disableBlend();
                return new float[]{1.0F, 1.0F, 1.0F, alpha};
            case "add":
                setBlendFunc(770, 1);
                return new float[]{1.0F, 1.0F, 1.0F, alpha};
            case "subtract":
                setBlendFunc(775, 0);
                return new float[]{alpha, alpha, alpha, 1.0F};
            case "multiply":
                setBlendFunc(774, 771);
                return new float[]{alpha, alpha, alpha, alpha};
            case "screen":
                setBlendFunc(1, 769);
                return new float[]{alpha, alpha, alpha, 1.0F};
            case "replace":
                setBlendFunc(0, 1);
                return new float[]{1.0F, 1.0F, 1.0F, alpha};
            case "burn":
                setBlendFunc(0, 769);
                return new float[]{alpha, alpha, alpha, 1.0F};
            case "dodge":
                setBlendFunc(774, 1);
                return new float[]{alpha, alpha, alpha, 1.0F};
            case "decorations":
                GlStateManager.enableBlend();
                GlStateManager.tryBlendFuncSeparate(770, 1, 1, 0);
                return new float[]{1.0F, 1.0F, 1.0F, alpha};
            case "alpha":
            case "":
            default:
                GlStateManager.enableBlend();
                GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
                return new float[]{1.0F, 1.0F, 1.0F, alpha};
        }
    }

    private static void setBlendFunc(int source, int destination) {
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(source, destination);
    }

    private static float[] applyOptiFineBlend(String blend, float alpha) {
        GlStateManager.glBlendEquation(32774);
        switch (blend) {
            case "add":
                setBlendFunc(770, 1);
                return new float[]{1.0F, 1.0F, 1.0F, alpha};
            case "subtract":
                setBlendFunc(775, 0);
                return new float[]{alpha, alpha, alpha, 1.0F};
            case "multiply":
                setBlendFunc(774, 771);
                return new float[]{alpha, alpha, alpha, alpha};
            case "dodge":
                setBlendFunc(1, 1);
                return new float[]{alpha, alpha, alpha, 1.0F};
            case "burn":
                setBlendFunc(0, 769);
                return new float[]{alpha, alpha, alpha, 1.0F};
            case "screen":
                setBlendFunc(1, 769);
                return new float[]{alpha, alpha, alpha, 1.0F};
            case "overlay":
                setBlendFunc(774, 768);
                return new float[]{alpha, alpha, alpha, 1.0F};
            case "replace":
                GlStateManager.disableBlend();
                return new float[]{1.0F, 1.0F, 1.0F, alpha};
            case "alpha":
            default:
                GlStateManager.enableBlend();
                GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
                return new float[]{1.0F, 1.0F, 1.0F, alpha};
        }
    }

}
