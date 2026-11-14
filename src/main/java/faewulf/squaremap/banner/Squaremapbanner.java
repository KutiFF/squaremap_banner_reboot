package faewulf.squaremap.banner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import faewulf.squaremap.banner.commands.BannerCommand;
import faewulf.squaremap.banner.modules.bannerCleanup;
import faewulf.squaremap.banner.dataType.ModConfigs;
import faewulf.squaremap.banner.events.onLeftClick;
import faewulf.squaremap.banner.events.onPlayerBreakBlock;
import faewulf.squaremap.banner.events.onPlayerLeave;
import faewulf.squaremap.banner.events.onRightClick;
import faewulf.squaremap.banner.events.onServerStarted;
import faewulf.squaremap.banner.events.onWorldLoad;
import faewulf.squaremap.banner.utils.ConfigHandler;
import faewulf.squaremap.banner.utils.DebugLog;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;
import java.util.stream.Collectors;

public class Squaremapbanner implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("squaremap-banner");

    /** Shown at startup and in /squaremapbanner status, to be sure which build is really running. */
    public static final String BUILD_STAMP = "1.1.0";

    public static ModConfigs modConfigs;

    public static String MODID = "squaremap-banner";

    @Override
    public void onInitialize() {
        LOGGER.info("Cool! Now loading modules and events... (build {})", BUILD_STAMP);

        //load config file
        modConfigs = ConfigHandler.loadConfig();

        ConfigHandler.saveConfig(modConfigs);

        logStartupDiagnostics();

        this.loadEvents();
    }

    private void loadEvents() {
        onWorldLoad.load();
        onLeftClick.load();
        onRightClick.load();
        onPlayerBreakBlock.load();
        onServerStarted.load();
        onPlayerLeave.load();
        BannerCommand.load();
        bannerCleanup.load();
    }

    private static void logStartupDiagnostics() {
        if (!DebugLog.enabled())
            return;

        FabricLoader loader = FabricLoader.getInstance();

        DebugLog.log("=== squaremap-banner build " + BUILD_STAMP + " starting ===");
        DebugLog.log("environment=" + loader.getEnvironmentType() + ", java=" + System.getProperty("java.version")
                + ", workingDir=" + Path.of("").toAbsolutePath());
        DebugLog.log("squaremap=" + loader.getModContainer("squaremap")
                .map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("NOT LOADED"));
        DebugLog.log("config: radius=" + modConfigs.radius + ", announce=" + modConfigs.bannerEnterAnnouncer
                + ", cooldown=" + modConfigs.registerCooldownSeconds + "s, cleanup=" + modConfigs.cleanupIntervalSeconds
                + "s, blacklist=" + modConfigs.blacklist);
        DebugLog.log("mods: " + loader.getAllMods().stream()
                .map(c -> c.getMetadata().getId() + "@" + c.getMetadata().getVersion().getFriendlyString())
                .sorted().collect(Collectors.joining(", ")));
    }
}
