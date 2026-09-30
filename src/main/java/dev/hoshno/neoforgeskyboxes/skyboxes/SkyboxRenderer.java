package dev.hoshno.neoforgeskyboxes.skyboxes;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.VertexBuffer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import dev.hoshno.neoforgeskyboxes.mixin.skybox.RenderGlobalSkyAccessor;
import org.lwjgl.opengl.GL11;

/** Immediate-mode renderer for the first Cleanroom ported skybox types. */
public final class SkyboxRenderer {
    private static final float HALF_WIDTH = 100.0F;

    private SkyboxRenderer() {
    }

    public static void render(Minecraft minecraft, SkyboxDefinition skybox, float partialTicks, RenderGlobalSkyAccessor renderGlobal) {
        float alpha = skybox.getAlpha();
        if (alpha <= 0.0F) {
            return;
        }

        GlStateManager.pushMatrix();
        applyRotation(minecraft, skybox);
        GlStateManager.depthMask(false);
        GlStateManager.disableAlpha();
        GlStateManager.disableCull();
        GlStateManager.enableBlend();
        applyBlend(skybox.getBlend());

        if (skybox.getType() == SkyboxDefinition.Type.MONOCOLOR) {
            GlStateManager.disableTexture2D();
            renderMonocolor(skybox, alpha);
        } else if (skybox.getType() == SkyboxDefinition.Type.SQUARE_TEXTURED
                || skybox.getType() == SkyboxDefinition.Type.SINGLE_SPRITE_SQUARE_TEXTURED
                || skybox.getType() == SkyboxDefinition.Type.ANIMATED_SQUARE_TEXTURED
                || skybox.getType() == SkyboxDefinition.Type.SINGLE_SPRITE_ANIMATED_SQUARE_TEXTURED) {
            GlStateManager.enableTexture2D();
            renderSquareTextured(minecraft, skybox, alpha);
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

    private static void renderMonocolor(SkyboxDefinition skybox, float alpha) {
        float vertexAlpha = alpha * skybox.getColorAlpha();
        for (int face = 0; face < 6; face++) {
            GlStateManager.pushMatrix();
            rotateFace(face);
            BufferBuilder buffer = Tessellator.getInstance().getBuffer();
            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
            addColorVertex(buffer, -HALF_WIDTH, -HALF_WIDTH, -HALF_WIDTH, skybox, vertexAlpha);
            addColorVertex(buffer, -HALF_WIDTH, -HALF_WIDTH, HALF_WIDTH, skybox, vertexAlpha);
            addColorVertex(buffer, HALF_WIDTH, -HALF_WIDTH, HALF_WIDTH, skybox, vertexAlpha);
            addColorVertex(buffer, HALF_WIDTH, -HALF_WIDTH, -HALF_WIDTH, skybox, vertexAlpha);
            Tessellator.getInstance().draw();
            GlStateManager.popMatrix();
        }
    }

    private static void addColorVertex(BufferBuilder buffer, double x, double y, double z, SkyboxDefinition skybox, float alpha) {
        buffer.pos(x, y, z)
                .color(skybox.getRed(), skybox.getGreen(), skybox.getBlue(), alpha)
                .endVertex();
    }

    private static void renderSquareTextured(Minecraft minecraft, SkyboxDefinition skybox, float alpha) {
        ResourceLocation[] textures = skybox.getTexturesAt(System.currentTimeMillis());
        for (int face = 0; face < textures.length; face++) {
            float[] uv = skybox.getTextureUv(face);
            minecraft.getTextureManager().bindTexture(textures[face]);
            GlStateManager.pushMatrix();
            rotateFace(face);
            BufferBuilder buffer = Tessellator.getInstance().getBuffer();
            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
            addTextureVertex(buffer, -HALF_WIDTH, -HALF_WIDTH, -HALF_WIDTH, uv[0], uv[1], alpha);
            addTextureVertex(buffer, -HALF_WIDTH, -HALF_WIDTH, HALF_WIDTH, uv[0], uv[3], alpha);
            addTextureVertex(buffer, HALF_WIDTH, -HALF_WIDTH, HALF_WIDTH, uv[2], uv[3], alpha);
            addTextureVertex(buffer, HALF_WIDTH, -HALF_WIDTH, -HALF_WIDTH, uv[2], uv[1], alpha);
            Tessellator.getInstance().draw();
            GlStateManager.popMatrix();
        }
    }

    private static void addTextureVertex(BufferBuilder buffer, double x, double y, double z, double u, double v, float alpha) {
        buffer.pos(x, y, z)
                .tex(u, v)
                .color(1.0F, 1.0F, 1.0F, alpha)
                .endVertex();
    }

    private static void renderDecorations(Minecraft minecraft, SkyboxDefinition skybox, float alpha,
                                          float partialTicks, RenderGlobalSkyAccessor renderGlobal) {
        if (!skybox.isSunEnabled() && !skybox.isMoonEnabled() && !skybox.areStarsEnabled()) {
            return;
        }

        float weatherAlpha = 1.0F - minecraft.world.getRainStrength(partialTicks);
        GlStateManager.pushMatrix();
        GlStateManager.enableTexture2D();
        GlStateManager.enableBlend();
        applyDecorationBlend(skybox.getDecorationBlend());
        GlStateManager.rotate(-90.0F, 0.0F, 1.0F, 0.0F);
        GlStateManager.rotate(minecraft.world.getCelestialAngle(partialTicks) * 360.0F, 1.0F, 0.0F, 0.0F);

        if (skybox.isSunEnabled()) {
            drawCelestialQuad(minecraft, skybox.getSunTexture(), alpha * weatherAlpha,
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
            drawMoon(minecraft, skybox.getMoonTexture(), alpha * weatherAlpha, minU, minV, maxU, maxV);
        }
        if (skybox.areStarsEnabled()) {
            renderStars(minecraft, alpha, partialTicks, renderGlobal);
        }

        GlStateManager.popMatrix();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }

    private static void drawCelestialQuad(Minecraft minecraft, ResourceLocation texture, float alpha,
                                          float halfWidth, float y, float minU, float minV, float maxU, float maxV) {
        minecraft.getTextureManager().bindTexture(texture);
        GlStateManager.color(1.0F, 1.0F, 1.0F, alpha);
        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        buffer.pos(-halfWidth, y, -halfWidth).tex(minU, minV).color(1.0F, 1.0F, 1.0F, alpha).endVertex();
        buffer.pos(halfWidth, y, -halfWidth).tex(maxU, minV).color(1.0F, 1.0F, 1.0F, alpha).endVertex();
        buffer.pos(halfWidth, y, halfWidth).tex(maxU, maxV).color(1.0F, 1.0F, 1.0F, alpha).endVertex();
        buffer.pos(-halfWidth, y, halfWidth).tex(minU, maxV).color(1.0F, 1.0F, 1.0F, alpha).endVertex();
        Tessellator.getInstance().draw();
    }

    private static void drawMoon(Minecraft minecraft, ResourceLocation texture, float alpha,
                                 float minU, float minV, float maxU, float maxV) {
        minecraft.getTextureManager().bindTexture(texture);
        GlStateManager.color(1.0F, 1.0F, 1.0F, alpha);
        BufferBuilder buffer = Tessellator.getInstance().getBuffer();
        buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        buffer.pos(-20.0F, -100.0F, 20.0F).tex(maxU, maxV).color(1.0F, 1.0F, 1.0F, alpha).endVertex();
        buffer.pos(20.0F, -100.0F, 20.0F).tex(minU, maxV).color(1.0F, 1.0F, 1.0F, alpha).endVertex();
        buffer.pos(20.0F, -100.0F, -20.0F).tex(minU, minV).color(1.0F, 1.0F, 1.0F, alpha).endVertex();
        buffer.pos(-20.0F, -100.0F, -20.0F).tex(maxU, minV).color(1.0F, 1.0F, 1.0F, alpha).endVertex();
        Tessellator.getInstance().draw();
    }

    private static void renderStars(Minecraft minecraft, float skyboxAlpha, float partialTicks,
                                    RenderGlobalSkyAccessor renderGlobal) {
        float rainAlpha = 1.0F - minecraft.world.getRainStrength(partialTicks);
        float brightness = minecraft.world.getStarBrightness(partialTicks) * rainAlpha * skyboxAlpha;
        if (brightness <= 0.0F) {
            return;
        }

        GlStateManager.disableTexture2D();
        GlStateManager.color(brightness, brightness, brightness, brightness);
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

    private static void applyDecorationBlend(String blend) {
        if ("alpha".equals(blend)) {
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        } else if ("disable".equals(blend)) {
            GlStateManager.disableBlend();
        } else {
            GlStateManager.tryBlendFuncSeparate(770, 1, 1, 0);
        }
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
        float[] axis = skybox.getAxisRotation();
        float[] speed = skybox.getRotationSpeed();
        int[] shift = skybox.getTimeShift();
        float[] fixed = skybox.getStaticRotation();
        long time = minecraft.world.getWorldTime();
        float[] timeRotation = new float[3];
        for (int i = 0; i < timeRotation.length; i++) {
            if (speed[i] != 0.0F) {
                double rotationFraction = (time + shift[i]) * speed[i] / 24000.0D;
                timeRotation[i] = (float) ((rotationFraction - Math.floor(rotationFraction)) * 360.0D);
            }
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

    private static void applyBlend(String blend) {
        if ("disable".equals(blend)) {
            GlStateManager.disableBlend();
        } else if ("add".equals(blend)) {
            GlStateManager.tryBlendFuncSeparate(770, 1, 1, 0);
        } else if ("subtract".equals(blend)) {
            GlStateManager.tryBlendFuncSeparate(775, 0, 1, 0);
        } else if ("multiply".equals(blend)) {
            GlStateManager.tryBlendFuncSeparate(774, 771, 1, 0);
        } else if ("screen".equals(blend)) {
            GlStateManager.tryBlendFuncSeparate(1, 769, 1, 0);
        } else if ("replace".equals(blend)) {
            GlStateManager.tryBlendFuncSeparate(0, 1, 1, 0);
        } else if ("burn".equals(blend)) {
            GlStateManager.tryBlendFuncSeparate(0, 769, 1, 0);
        } else if ("dodge".equals(blend)) {
            GlStateManager.tryBlendFuncSeparate(774, 1, 1, 0);
        } else {
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        }
    }
}
