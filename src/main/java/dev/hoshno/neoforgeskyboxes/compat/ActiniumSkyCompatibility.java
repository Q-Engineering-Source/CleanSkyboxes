package dev.hoshno.neoforgeskyboxes.compat;

import net.minecraftforge.fml.common.Loader;

/** Keeps custom sky rendering inside Actinium's Iris custom-sky shader phase. */
public final class ActiniumSkyCompatibility {
    private ActiniumSkyCompatibility() {
    }

    public static boolean beginCustomSky() {
        return Loader.isModLoaded("actinium") && ActiniumAccess.begin();
    }

    public static void endCustomSky(boolean phaseStarted) {
        if (!phaseStarted) {
            return;
        }

        ActiniumAccess.end();
    }

    private static final class ActiniumAccess {
        private static boolean begin() {
            if (!net.coderbot.iris.Iris.enabled) {
                return false;
            }
            net.coderbot.iris.pipeline.WorldRenderingPipeline pipeline =
                    net.coderbot.iris.Iris.getPipelineManager().getPipelineNullable();
            if (pipeline == null) {
                return false;
            }
            pipeline.setPhase(net.coderbot.iris.pipeline.WorldRenderingPhase.CUSTOM_SKY);
            return true;
        }

        private static void end() {
            net.coderbot.iris.pipeline.WorldRenderingPipeline pipeline =
                    net.coderbot.iris.Iris.getPipelineManager().getPipelineNullable();
            if (pipeline != null) {
                pipeline.setPhase(net.coderbot.iris.pipeline.WorldRenderingPhase.NONE);
            }
        }
    }
}
