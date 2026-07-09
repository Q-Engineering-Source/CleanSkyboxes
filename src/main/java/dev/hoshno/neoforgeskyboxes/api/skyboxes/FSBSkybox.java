package dev.hoshno.neoforgeskyboxes.api.skyboxes;

import dev.hoshno.neoforgeskyboxes.util.object.Conditions;
import dev.hoshno.neoforgeskyboxes.util.object.Decorations;
import dev.hoshno.neoforgeskyboxes.util.object.Properties;

public interface FSBSkybox extends Skybox {
    float getAlpha();

    float updateAlpha();

    Properties getProperties();

    Conditions getConditions();

    Decorations getDecorations();
}
