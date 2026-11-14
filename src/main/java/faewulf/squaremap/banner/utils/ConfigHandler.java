package faewulf.squaremap.banner.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import faewulf.squaremap.banner.Squaremapbanner;
import faewulf.squaremap.banner.dataType.ModConfigs;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;

public class ConfigHandler {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_FILE = Path.of("config", "squaremap_banner.json");

    /**
     * Startup loading: never fails. A missing file gives defaults, an unreadable file is backed up
     * and replaced by defaults.
     */
    public static ModConfigs loadConfig() {
        if (!Files.exists(CONFIG_FILE))
            return new ModConfigs();

        try {
            return readConfig();
        } catch (Exception e) {
            Squaremapbanner.LOGGER.error("Could not read " + CONFIG_FILE + ", falling back to the default config", e);
            FileUtil.backupBroken(CONFIG_FILE);
            return new ModConfigs();
        }
    }

    /**
     * Strict loading used by /squaremapbanner reload: throws instead of falling back to defaults,
     * so a typo in the file never silently resets the running config.
     */
    public static ModConfigs readConfig() throws IOException {
        try (Reader reader = Files.newBufferedReader(CONFIG_FILE, StandardCharsets.UTF_8)) {
            ModConfigs config = GSON.fromJson(reader, ModConfigs.class);

            if (config == null)
                throw new IOException("the config file is empty");

            sanitize(config);
            return config;
        } catch (JsonParseException e) {
            throw new IOException("invalid JSON: " + e.getMessage(), e);
        }
    }

    public static void saveConfig(ModConfigs config) {
        try {
            FileUtil.writeAtomic(CONFIG_FILE, GSON.toJson(config));
        } catch (IOException e) {
            Squaremapbanner.LOGGER.error("Could not save " + CONFIG_FILE, e);
        }
    }

    /**
     * Replaces values that would break the mod (null blacklist, blank blacklist entries that match
     * every name, non-positive radius, absurd intervals) with safe ones.
     */
    private static void sanitize(ModConfigs config) {
        ModConfigs defaults = new ModConfigs();

        if (config.radius < 1)
            config.radius = defaults.radius;

        config.registerCooldownSeconds = Math.max(0, Math.min(config.registerCooldownSeconds, 86_400));

        if (config.cleanupIntervalSeconds < 0)
            config.cleanupIntervalSeconds = defaults.cleanupIntervalSeconds;
        config.cleanupIntervalSeconds = Math.min(config.cleanupIntervalSeconds, 86_400);

        if (config.cleanupBannersPerTick < 1)
            config.cleanupBannersPerTick = defaults.cleanupBannersPerTick;

        HashSet<String> cleaned = new HashSet<>();
        if (config.blacklist != null) {
            for (String word : config.blacklist) {
                if (word != null && !word.isBlank())
                    cleaned.add(word.trim());
            }
        }
        config.blacklist = cleaned;
    }
}