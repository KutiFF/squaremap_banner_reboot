package faewulf.squaremap.banner.events;

import faewulf.squaremap.banner.Squaremapbanner;
import faewulf.squaremap.banner.utils.DebugLog;
import faewulf.squaremap.banner.utils.bannerManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

public class onServerStarted {
    public static void load() {
        ServerLifecycleEvents.SERVER_STARTED.register((server -> {
            Squaremapbanner.LOGGER.info("Restoring banner data if exist...");
            DebugLog.log(() -> "SERVER_STARTED: tracked dimensions before restoring = " + bannerManager.bannerManager.keySet());

            bannerManager.load(server);

            DebugLog.log(() -> {
                StringBuilder sb = new StringBuilder("restored banners:");
                bannerManager.bannerManager.forEach((key, w) -> sb.append(' ').append(key).append('=').append(w.getData().size()));
                return sb.toString();
            });

            //reload save file
            boolean saved = bannerManager.save();
            DebugLog.log("initial save ok=" + saved);
        }));
    }
}
