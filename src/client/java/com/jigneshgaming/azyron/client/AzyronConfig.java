package com.jigneshgaming.azyron.client;

import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.EnumMap;
import java.util.Map;
import java.util.Properties;

/** Small, synchronous client config; no polling thread or external config library. */
public final class AzyronConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("AZYRON CLIENT/config");
    private static final int SCHEMA_VERSION = 1;
    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("azyron-client.properties");

    private static final EnumMap<HudModule, Boolean> HUD_ENABLED = new EnumMap<>(HudModule.class);
    private static final EnumMap<HudModule, HudPosition> HUD_POSITIONS = new EnumMap<>(HudModule.class);

    private static Profile profile = Profile.POTATO;
    private static boolean defaultsApplied;

    private AzyronConfig() {
    }

    public enum Profile {
        POTATO("Potato", "2 render / 2 simulation", 2, 2, 60),
        BALANCED("Balanced", "6 render / 4 simulation", 6, 4, 120),
        SMOOTH("Smooth FPS", "4 render / 3 simulation", 4, 3, 60);

        private final String displayName;
        private final String summary;
        private final int renderDistance;
        private final int simulationDistance;
        private final int maxFps;

        Profile(String displayName, String summary, int renderDistance, int simulationDistance, int maxFps) {
            this.displayName = displayName;
            this.summary = summary;
            this.renderDistance = renderDistance;
            this.simulationDistance = simulationDistance;
            this.maxFps = maxFps;
        }

        public String displayName() {
            return displayName;
        }

        public String summary() {
            return summary;
        }

        public int renderDistance() {
            return renderDistance;
        }

        public int simulationDistance() {
            return simulationDistance;
        }

        public int maxFps() {
            return maxFps;
        }
    }

    public enum HudModule {
        FPS("FPS", 0.015f, 0.025f),
        CPS("CPS", 0.015f, 0.075f),
        PING("PING", 0.015f, 0.125f),
        COORDINATES("COORDINATES", 0.015f, 0.175f),
        KEYSTROKES("KEYSTROKES", 0.015f, 0.27f),
        ARMOR("ARMOR", 0.78f, 0.82f);

        private final String displayName;
        private final float defaultX;
        private final float defaultY;

        HudModule(String displayName, float defaultX, float defaultY) {
            this.displayName = displayName;
            this.defaultX = defaultX;
            this.defaultY = defaultY;
        }

        public String displayName() {
            return displayName;
        }

        public String configKey() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    public record HudPosition(float x, float y) {
    }

    public static void load() {
        resetHudDefaults();
        profile = Profile.POTATO;
        defaultsApplied = false;

        if (!Files.isRegularFile(CONFIG_PATH)) {
            return;
        }

        Properties properties = new Properties();
        try (InputStream input = Files.newInputStream(CONFIG_PATH)) {
            properties.load(input);
            profile = parseProfile(properties.getProperty("profile"), Profile.POTATO);
            defaultsApplied = Boolean.parseBoolean(properties.getProperty("defaults_applied", "false"));

            for (HudModule module : HudModule.values()) {
                String prefix = "hud." + module.configKey() + ".";
                HUD_ENABLED.put(module, Boolean.parseBoolean(
                        properties.getProperty(prefix + "enabled", "true")));
                HudPosition fallback = HUD_POSITIONS.get(module);
                float x = parseFloat(properties.getProperty(prefix + "x"), fallback.x());
                float y = parseFloat(properties.getProperty(prefix + "y"), fallback.y());
                HUD_POSITIONS.put(module, new HudPosition(clamp01(x), clamp01(y)));
            }
        } catch (IOException | RuntimeException exception) {
            LOGGER.warn("Could not read AZYRON CLIENT settings; using safe defaults.", exception);
            resetHudDefaults();
            profile = Profile.POTATO;
            defaultsApplied = false;
        }
    }

    public static synchronized void save() {
        Properties properties = new Properties();
        properties.setProperty("schema", Integer.toString(SCHEMA_VERSION));
        properties.setProperty("profile", profile.name());
        properties.setProperty("defaults_applied", Boolean.toString(defaultsApplied));

        for (HudModule module : HudModule.values()) {
            String prefix = "hud." + module.configKey() + ".";
            HudPosition position = HUD_POSITIONS.get(module);
            properties.setProperty(prefix + "enabled", Boolean.toString(isHudEnabled(module)));
            properties.setProperty(prefix + "x", Float.toString(position.x()));
            properties.setProperty(prefix + "y", Float.toString(position.y()));
        }

        Path temporaryPath = CONFIG_PATH.resolveSibling(CONFIG_PATH.getFileName() + ".tmp");
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (OutputStream output = Files.newOutputStream(temporaryPath)) {
                properties.store(output, "AZYRON CLIENT 1.0 - Minecraft Java 1.21.11 only");
            }
            try {
                Files.move(temporaryPath, CONFIG_PATH,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporaryPath, CONFIG_PATH, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            LOGGER.warn("Could not save AZYRON CLIENT settings.", exception);
            try {
                Files.deleteIfExists(temporaryPath);
            } catch (IOException ignored) {
                // Best effort cleanup; the original config remains untouched.
            }
        }
    }

    public static Profile getProfile() {
        return profile;
    }

    public static void setProfile(Profile nextProfile) {
        profile = nextProfile == null ? Profile.POTATO : nextProfile;
    }

    public static boolean areDefaultsApplied() {
        return defaultsApplied;
    }

    public static void setDefaultsApplied(boolean applied) {
        defaultsApplied = applied;
    }

    public static boolean isHudEnabled(HudModule module) {
        return HUD_ENABLED.getOrDefault(module, true);
    }

    public static void toggleHud(HudModule module) {
        HUD_ENABLED.put(module, !isHudEnabled(module));
    }

    public static HudPosition getHudPosition(HudModule module) {
        return HUD_POSITIONS.getOrDefault(module,
                new HudPosition(module.defaultX, module.defaultY));
    }

    public static void nudgeHudPosition(HudModule module, float deltaX, float deltaY) {
        HudPosition old = getHudPosition(module);
        HUD_POSITIONS.put(module, new HudPosition(
                clamp01(old.x() + deltaX),
                clamp01(old.y() + deltaY)));
    }

    public static void resetHudPosition(HudModule module) {
        HUD_POSITIONS.put(module, new HudPosition(module.defaultX, module.defaultY));
    }

    private static void resetHudDefaults() {
        HUD_ENABLED.clear();
        HUD_POSITIONS.clear();
        for (HudModule module : HudModule.values()) {
            HUD_ENABLED.put(module, true);
            HUD_POSITIONS.put(module, new HudPosition(module.defaultX, module.defaultY));
        }
    }

    private static Profile parseProfile(String value, Profile fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return Profile.valueOf(value.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return fallback;
        }
    }

    private static float parseFloat(String value, float fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private static float clamp01(float value) {
        if (!Float.isFinite(value)) {
            return 0.0f;
        }
        return Math.max(0.0f, Math.min(1.0f, value));
    }
}
