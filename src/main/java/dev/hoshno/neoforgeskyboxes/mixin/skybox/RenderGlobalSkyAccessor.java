package dev.hoshno.neoforgeskyboxes.mixin.skybox;

import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.vertex.VertexBuffer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RenderGlobal.class)
public interface RenderGlobalSkyAccessor {
    @Accessor("skyVBO")
    VertexBuffer getSkyVBO();

    @Accessor("glSkyList")
    int getSkyDisplayList();

    @Accessor("sky2VBO")
    VertexBuffer getDarkSkyVBO();

    @Accessor("glSkyList2")
    int getDarkSkyDisplayList();

    @Accessor("starVBO")
    VertexBuffer getStarVBO();

    @Accessor("starGLCallList")
    int getStarGLCallList();

    @Accessor("vboEnabled")
    boolean isVboEnabled();
}
