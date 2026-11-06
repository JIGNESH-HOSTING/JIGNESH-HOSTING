package com.jigneshgaming.azyron.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.option.GraphicsMode;
import net.minecraft.particle.ParticlesMode;

/** Applies reversible vanilla video options only; the performance mods do their own work. */
public final class PerformanceProfiles {
    private PerformanceProfiles() {
    }

    public static void apply(MinecraftClient client, AzyronConfig.Profile profile) {
        if (client == null || client.options == null) {
            return;
        }

        AzyronConfig.Profile selected = profile == null ? AzyronConfig.Profile.POTATO : profile;
        GameOptions options = client.options;
        int renderDistance = selected.renderDistance();
        int simulationDistance = selected.simulationDistance();
        int maxFps = selected.maxFps();
        double entityDistance = 0.50;
        int mipmapLevels = 0;
        boolean smoothLighting = false;
        ParticlesMode particles = ParticlesMode.MINIMAL;

        switch (selected) {
            case POTATO -> {
                // Requested AZYRON first-run defaults.
                renderDistance = 2;
                simulationDistance = 2;
                maxFps = 60;
            }
            case BALANCED -> {
                renderDistance = 6;
                simulationDistance = 4;
                maxFps = 120;
                entityDistance = 0.75;
                mipmapLevels = 2;
                smoothLighting = true;
                particles = ParticlesMode.DECREASED;
            }
            case SMOOTH -> {
                // A conservative frame-time profile rather than an unbounded FPS claim.
                renderDistance = 4;
                simulationDistance = 3;
                maxFps = 60;
                entityDistance = 0.60;
            }
        }

        // Apply the vanilla Fast preset first, then make the values explicit so the
        // results are deterministic and independent of the previous graphics preset.
        options.applyGraphicsMode(GraphicsMode.FAST);
        options.getViewDistance().setValue(renderDistance);
        options.getSimulationDistance().setValue(simulationDistance);
        options.getCloudRenderMode().setValue(CloudRenderMode.OFF);
        options.getParticles().setValue(particles);
        options.getAo().setValue(smoothLighting);
        options.getEntityDistanceScaling().setValue(entityDistance);
        options.getMipmapLevels().setValue(mipmapLevels);
        options.getEnableVsync().setValue(false);
        options.getMaxFps().setValue(maxFps);
        options.getBiomeBlendRadius().setValue(0);
        options.getEntityShadows().setValue(false);
        options.write();

        AzyronConfig.setProfile(selected);
        AzyronConfig.setDefaultsApplied(true);
        AzyronConfig.save();
    }
}
