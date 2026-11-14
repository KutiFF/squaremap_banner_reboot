package faewulf.squaremap.banner.modules;

import faewulf.squaremap.banner.dataType.Data;
import faewulf.squaremap.banner.dataType.Position;
import faewulf.squaremap.banner.utils.DebugLog;
import faewulf.squaremap.banner.utils.bannerManager;
import faewulf.squaremap.banner.utils.world;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

import static faewulf.squaremap.banner.Squaremapbanner.modConfigs;

/**
 * Removes markers of banners that no longer exist in the world (exploded, burned, pushed by a piston,
 * washed away ...). Players cannot remove markers themselves, so this is the only way they disappear
 * besides breaking the banner and the admin commands.
 * <p>
 * Designed to cost next to nothing:
 * - a pass starts every cleanup_interval_seconds (measured from the end of the previous pass),
 * - a pass checks at most cleanup_banners_per_tick banners per tick, spread over many ticks,
 * - chunks are never loaded for the check, banners in unloaded chunks are simply skipped,
 * - a marker is removed only when the banner is missing in two consecutive passes.
 */
public class bannerCleanup {

    private record Entry(String worldKey, Position pos) {
    }

    private static final ArrayDeque<Entry> queue = new ArrayDeque<>();
    private static final Set<Entry> suspects = new HashSet<>();

    private static int ticksUntilNextPass = 20 * 30;
    private static boolean dirty = false;

    public static void load() {
        ServerTickEvents.END_SERVER_TICK.register(bannerCleanup::tick);

        //do not lose a removal that was not saved yet
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> flush());
    }

    private static void tick(MinecraftServer server) {
        int intervalSeconds = modConfigs.cleanupIntervalSeconds;

        //disabled in the config
        if (intervalSeconds <= 0) {
            if (!queue.isEmpty()) {
                queue.clear();
                suspects.clear();
            }
            flush();
            return;
        }

        if (queue.isEmpty()) {
            //waiting for the next pass (only counts down while no pass is running)
            if (--ticksUntilNextPass > 0)
                return;

            ticksUntilNextPass = intervalSeconds * 20;
            snapshot();
            DebugLog.log(() -> "cleanup pass started, banners to check: " + queue.size());

            if (queue.isEmpty())
                return;
        }

        int budget = modConfigs.cleanupBannersPerTick;
        while (budget-- > 0 && !queue.isEmpty())
            check(server, queue.poll());

        //pass finished -> one save for all removals of this pass
        if (queue.isEmpty())
            flush();
    }

    private static void snapshot() {
        bannerManager.bannerManager.forEach((worldKey, worldObj) -> {
            for (Position pos : worldObj.getBanners())
                queue.add(new Entry(worldKey, pos));
        });
    }

    private static void check(MinecraftServer server, Entry entry) {
        world worldObj = bannerManager.bannerManager.get(entry.worldKey());
        if (worldObj == null)
            return;

        Data data = worldObj.getData().get(entry.pos());

        //already removed since the snapshot was taken
        if (data == null) {
            suspects.remove(entry);
            return;
        }

        ServerLevel level = bannerManager.getLevel(server, entry.worldKey());
        BlockPos pos = entry.pos().loc();

        //never load a chunk just to check a banner; try again in a later pass
        if (level == null || !level.hasChunkAt(pos)) {
            suspects.remove(entry);
            return;
        }

        if (isSameBanner(level, pos, data)) {
            suspects.remove(entry);
            return;
        }

        //missing: first time only marks it, second consecutive time removes it
        if (suspects.remove(entry)) {
            DebugLog.log(() -> "cleanup: REMOVING marker \"" + data.name() + "\" at " + pos + " in " + entry.worldKey()
                    + " (banner missing in two passes)");
            worldObj.removeBanner(entry.pos());
            dirty = true;
        } else {
            DebugLog.log(() -> "cleanup: banner \"" + data.name() + "\" at " + pos + " in " + entry.worldKey()
                    + " not found, will be removed if still missing in the next pass");
            suspects.add(entry);
        }
    }

    /**
     * The registered banner is still there: a banner block entity with the same custom name.
     */
    private static boolean isSameBanner(ServerLevel level, BlockPos pos, Data data) {
        BlockEntity blockEntity = level.getBlockEntity(pos);

        if (!(blockEntity instanceof BannerBlockEntity banner))
            return false;

        Component customName = banner.getCustomName();
        return customName != null && customName.getString().equals(data.name());
    }

    private static void flush() {
        if (dirty) {
            dirty = false;
            bannerManager.save();
        }
    }
}