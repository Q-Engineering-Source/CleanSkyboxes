package dev.hoshno.neoforgeskyboxes;

import net.minecraftforge.fml.relauncher.IFMLLoadingPlugin;

import java.util.Map;

@IFMLLoadingPlugin.Name("CleanSkyboxes")
@IFMLLoadingPlugin.MCVersion("1.12.2")
public final class NeoforgeSkyboxesCoremod implements IFMLLoadingPlugin {
    @Override
    public String[] getASMTransformerClass() {
        return null;
    }

    @Override
    public String getModContainerClass() {
        return null;
    }

    @Override
    public String getSetupClass() {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data) {
    }

    @Override
    public String getAccessTransformerClass() {
        return null;
    }
}
