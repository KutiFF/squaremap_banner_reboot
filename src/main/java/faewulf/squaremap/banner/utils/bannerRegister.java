package faewulf.squaremap.banner.utils;

import static faewulf.squaremap.banner.Squaremapbanner.modConfigs;

import java.util.UUID;

import faewulf.squaremap.banner.dataType.Data;
import faewulf.squaremap.banner.dataType.Position;
import faewulf.squaremap.banner.dataType.icon;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import xyz.jpenilla.squaremap.api.Key;

public class bannerRegister {

    /**
     * @return true if the banner was added or updated on the map (the caller should then save the data)
     */
    public static boolean tryAddBanner(Player player, Level world, BlockPos pos) {
        BlockEntity blockEntity = world.getBlockEntity(pos);

        DebugLog.log(() -> "tryAddBanner at " + pos + ": block=" + world.getBlockState(pos).getBlock()
                + ", block entity=" + (blockEntity == null ? "null" : blockEntity.getClass().getName()));

        if (!(blockEntity instanceof BannerBlockEntity bannerEntity)) {
            DebugLog.log("  -> not a banner block entity");
            return false;
        }

        if (bannerEntity.getCustomName() == null) {
            DebugLog.log("  -> the banner has no custom name (it must be named, e.g. in an anvil, before it is placed)");
            return false;
        }

        // Get the base color of the banner
        DyeColor baseColor = bannerEntity.getBaseColor();

        //get name
        String name = bannerEntity.getCustomName().getString();

        //bad word check
        String badWord = findBlacklistedWord(name);
        if (badWord != null) {
            DebugLog.log("  -> name \"" + name + "\" contains the blacklisted word \"" + badWord + "\"");
            player.sendOverlayMessage(Component.literal("This banner's name contains a blacklist word: \"" + badWord + "\", please rename it and try again"));
            return false;
        }

        //get world id
        String worldKey = world.dimension().identifier().toString();

        //get provider based on the world id
        faewulf.squaremap.banner.utils.world w = bannerManager.bannerManager.get(worldKey);
        if (w == null) {
            DebugLog.log(() -> "  -> dimension " + worldKey + " is not tracked, tracked = " + bannerManager.bannerManager.keySet());
            player.sendOverlayMessage(Component.literal("The banner map is not active in this dimension (" + worldKey + ")"));
            return false;
        }

        //cooldown (admins bypass it)
        long cooldownMs = modConfigs.registerCooldownSeconds * 1000L;
        if (cooldownMs > 0 && !Permissions.check(player, faewulf.squaremap.banner.dataType.Permissions.ADMIN, 2)) {
            long remainingMs = RegisterCooldown.remainingMs(player.getUUID(), cooldownMs);
            if (remainingMs > 0) {
                DebugLog.log("  -> cooldown, " + remainingMs + " ms left");
                long seconds = (remainingMs + 999) / 1000;
                player.sendOverlayMessage(Component.literal("Please wait " + seconds + "s before registering another banner"));
                return false;
            }
        }

        Position position = Position.of(pos);

        //updating an already registered banner keeps its original owner
        Data existing = w.getData().get(position);
        UUID owner = player.getUUID();
        String ownerName = player.getName().getString();
        if (existing != null && existing.owner() != null) {
            owner = existing.owner();
            ownerName = existing.ownerName();
        }

        //generate new random unique key for banner
        UUID uuid = UUID.randomUUID();

        //then just add banner to the world
        w.addBanner(position, icon.getIcon(baseColor), name, Key.of(uuid.toString()), owner, ownerName);

        RegisterCooldown.mark(player.getUUID());

        DebugLog.log("  -> added \"" + name + "\" at " + pos + " in " + worldKey + ", owner " + ownerName
                + ", banners in this dimension now: " + w.getData().size());

        player.sendOverlayMessage(Component.literal("Banner added to the map: " + name));
        return true;
    }

    /**
     * Shift + left click with a filled map on a registered banner removes its marker.
     *
     * @return true if a marker was removed (the caller should then save the data)
     */
    public static boolean tryRemoveBanner(Player player, Level world, BlockPos pos) {
        if (!(world.getBlockEntity(pos) instanceof BannerBlockEntity bannerEntity))
            return false;

        if (bannerEntity.getCustomName() == null)
            return false;

        //get world id
        String worldKey = world.dimension().identifier().toString();

        //get provider based on the world id
        faewulf.squaremap.banner.utils.world w = bannerManager.bannerManager.get(worldKey);
        if (w == null)
            return false;

        Position position = Position.of(pos);
        Data existing = w.getData().get(position);
        if (existing == null) {
            DebugLog.log("tryRemoveBanner: no registered banner at " + pos);
            return false;
        }

        w.removeBanner(position);

        player.sendOverlayMessage(Component.literal("Banner removed from the map: " + existing.name()));
        return true;
    }
    
    /**
     * @return the first blacklisted word contained in the name, or null
     */
    public static String findBlacklistedWord(String name) {
        for (String word : modConfigs.blacklist) {
            if (wordCheck.containsWholeWord(name, word))
                return word;
        }
        return null;
    }

}