package dev.hoshno.neoforgeskyboxes.interop.mixin;

import dev.hoshno.neoforgeskyboxes.resource.SkyboxResourceListener;
import dev.hoshno.neoforgeskyboxes.interop.FSBInterop;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = SkyboxResourceListener.class, remap = false)
public class MixinSkyboxResourceListener {

    @Inject(method = "onResourceManagerReload", at = @At(value = "TAIL"))
    public void onResourceManagerReload(ResourceManager manager, CallbackInfo ci) {
        FSBInterop.getInstance().inject(manager);
    }
}

