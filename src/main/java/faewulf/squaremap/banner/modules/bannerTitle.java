package faewulf.squaremap.banner.modules;

import faewulf.squaremap.banner.dataType.Data;
import faewulf.squaremap.banner.dataType.Position;
import faewulf.squaremap.banner.dataType.icon;
import faewulf.squaremap.banner.utils.bannerManager;
import faewulf.squaremap.banner.utils.world;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static faewulf.squaremap.banner.Squaremapbanner.modConfigs;

public class bannerTitle {

    //player -> key of the banner area the player is currently in.
    //Keyed by UUID (not by the Player object) so disconnected players are never kept alive.
    private static final Map<UUID, String> playerList = new HashMap<>();

    public static void forget(UUID player) {
        playerList.remove(player);
    }

    /**
     * Called at most once per second and only after the player changed block (see PlayerEntityMixin).
     * One pass over the banners of the player's world: squared distances, no sqrt, no allocations.
     */
    public static void check(Level playerWorld, Player player) {

        if (playerWorld.isClientSide())
            return;

        //get worldObj from bannerManager
        world worldObj = bannerManager.bannerManager.get(playerWorld.dimension().identifier().toString());

        //safe check
        if (worldObj == null)
            return;

        UUID id = player.getUUID();
        Map<Position, Data> banners = worldObj.getData();

        if (banners.isEmpty()) {
            playerList.remove(id);
            return;
        }

        //banner title radius
        double radius = modConfigs.radius;
        double radiusSq = radius * radius;

        double playerX = player.getX();
        double playerZ = player.getZ();

        //find the nearest banner whose area the player is in
        Data nearest = null;
        double nearestSq = Double.MAX_VALUE;

        for (Map.Entry<Position, Data> entry : banners.entrySet()) {
            BlockPos loc = entry.getKey().loc();

            double dx = loc.getX() - playerX;
            double dz = loc.getZ() - playerZ;
            double distSq = dx * dx + dz * dz;

            if (distSq <= radiusSq && distSq < nearestSq) {
                nearestSq = distSq;
                nearest = entry.getValue();
            }
        }

        //player is outside of every banner area -> the next entered area will be announced again
        if (nearest == null) {
            playerList.remove(id);
            return;
        }

        String selectedKey = nearest.key().getKey();

        //still in the area that was already announced
        if (selectedKey.equals(playerList.get(id)))
            return;

        playerList.put(id, selectedKey);

        DyeColor dye = icon.toDye(nearest.keyIconType());
        TextColor color = icon.getTextColor(dye);

        //then sendMessage
        Component sendString = Component.literal("Entering ")
                .append(Component.literal(nearest.name()).withStyle(style -> style.withColor(color)));

        player.sendOverlayMessage(sendString);
    }
}