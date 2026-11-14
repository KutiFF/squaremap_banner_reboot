package faewulf.squaremap.banner.utils;

import com.google.common.collect.Maps;
import faewulf.squaremap.banner.Squaremapbanner;
import faewulf.squaremap.banner.dataType.icon;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import java.util.Map;


public class bannerManager {
    public static Map<String, world> bannerManager = Maps.newHashMap();

    /**
     * @return true if the data file was written
     */
    public static boolean save() {
        return saveDataHandler.saveConfig();
    }

    /**
     * @return the loaded level for a world id like "minecraft:overworld", or null
     */
    public static ServerLevel getLevel(MinecraftServer server, String worldKey) {
        Identifier id = Identifier.tryParse(worldKey);
        if (id == null)
            return null;

        return server.getLevel(ResourceKey.create(Registries.DIMENSION, id));
    }

    public static void load(MinecraftServer server) {
        Map<String, world> savedWorlds = saveDataHandler.loadData();

        savedWorlds.forEach((worldId, savedWorld) -> {
            if (savedWorld == null || savedWorld.getData() == null)
                return;

            //get world based on data file's identifier
            world liveWorld = bannerManager.get(savedWorld.identifier);
            if (liveWorld == null)
                return;

            ServerLevel serverWorld = getLevel(server, savedWorld.identifier);

            //then forEach every banner in the data
            savedWorld.getData().forEach((position, data) -> {
                //damaged entry (see DataTypeAdapter)
                if (position == null || data == null)
                    return;

                //Drop a banner only when we can *see* that it is gone. Chunks that are not loaded are not
                //touched (asking for a block entity there would load the chunk, which is slow at startup
                //with many banners); the periodic cleanup checks those banners once their chunk is loaded.
                try {
                    if (serverWorld != null && serverWorld.hasChunkAt(position.loc()) && !position.isBanner(serverWorld))
                        return;
                } catch (Exception e) {
                    Squaremapbanner.LOGGER.error(e.toString());
                }

                try {
                    liveWorld.addBanner(position, icon.getIcon(icon.toDye(data.keyIconType())), data.name(), data.key(), data.owner(), data.ownerName());
                } catch (Exception e) {
                    Squaremapbanner.LOGGER.error("Could not restore banner " + data.name() + " at " + position, e);
                }
            });
        });
    }

}