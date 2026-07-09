package dev.hoshno.neoforgeskyboxes.mixin.skybox;

import com.mojang.blaze3d.platform.DisplayData;
import com.mojang.blaze3d.platform.ScreenManager;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.platform.WindowEventHandler;
import dev.hoshno.neoforgeskyboxes.NeoforgeSkyboxes;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Window.class)
public class WindowMixin {
    @Inject(method = "<init>", at = @At(value = "TAIL"))
    private void postWindowHints(WindowEventHandler eventHandler, ScreenManager screenManager, DisplayData displayData, String videoMode, String title, CallbackInfo ci) {
        if (NeoforgeSkyboxes.config().generalSettings.debugMode) {
            int maxTextureSize = GL11.glGetInteger(GL11.GL_MAX_TEXTURE_SIZE);
            NeoforgeSkyboxes.getLogger().info("Max Texture Size: {}x{}", maxTextureSize, maxTextureSize);
            NeoforgeSkyboxes.getLogger().info("Extension EXT_blend_func_extended supported: {}", GLFW.glfwExtensionSupported("EXT_blend_func_extended"));
            NeoforgeSkyboxes.getLogger().info("Extension GL_KHR_blend_equation_advanced supported: {}", GLFW.glfwExtensionSupported("GL_KHR_blend_equation_advanced"));
            NeoforgeSkyboxes.getLogger().info("Extension GL_KHR_blend_equation_advanced_coherent supported: {}", GLFW.glfwExtensionSupported("GL_KHR_blend_equation_advanced_coherent"));
        }
    }
}
