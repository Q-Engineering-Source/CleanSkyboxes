package dev.hoshno.neoforgeskyboxes.skyboxes;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;

/** Immediate-mode renderer for the first Cleanroom ported skybox types. */
public final class SkyboxRenderer {
    private static final float HALF_WIDTH = 100.0F;

    private SkyboxRenderer() {
    }

    public static void render(Minecraft minecraft, SkyboxDefinition skybox) {
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
        } else if (skybox.getType() == SkyboxDefinition.Type.SQUARE_TEXTURED) {
            GlStateManager.enableTexture2D();
            renderSquareTextured(minecraft, skybox, alpha);
        }

        GlStateManager.popMatrix();
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
        ResourceLocation[] textures = skybox.getTextures();
        for (int face = 0; face < textures.length; face++) {
            minecraft.getTextureManager().bindTexture(textures[face]);
            GlStateManager.pushMatrix();
            rotateFace(face);
            BufferBuilder buffer = Tessellator.getInstance().getBuffer();
            buffer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
            addTextureVertex(buffer, -HALF_WIDTH, -HALF_WIDTH, -HALF_WIDTH, 0.0D, 0.0D, alpha);
            addTextureVertex(buffer, -HALF_WIDTH, -HALF_WIDTH, HALF_WIDTH, 0.0D, 1.0D, alpha);
            addTextureVertex(buffer, HALF_WIDTH, -HALF_WIDTH, HALF_WIDTH, 1.0D, 1.0D, alpha);
            addTextureVertex(buffer, HALF_WIDTH, -HALF_WIDTH, -HALF_WIDTH, 1.0D, 0.0D, alpha);
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
