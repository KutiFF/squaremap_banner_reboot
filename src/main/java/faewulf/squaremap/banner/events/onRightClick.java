package faewulf.squaremap.banner.events;

import faewulf.squaremap.banner.utils.DebugLog;
import faewulf.squaremap.banner.utils.bannerManager;
import faewulf.squaremap.banner.utils.bannerRegister;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Items;

public class onRightClick {
    public static void load() {
        UseBlockCallback.EVENT.register(((player, world, hand, hitResult) -> {

            //cheap checks first: this callback runs for every block interaction of every player
            if (world.isClientSide())
                return InteractionResult.PASS;

            DebugLog.log(() -> "UseBlock: player=" + player.getName().getString() + " hand=" + hand
                    + " item=" + player.getItemInHand(hand).getItem() + " sneaking=" + player.isShiftKeyDown()
                    + " pos=" + hitResult.getBlockPos());

            //if not holding filled map
            if (player.getItemInHand(hand).getItem() != Items.FILLED_MAP) {
                DebugLog.log("  -> skipped: not holding a filled map in this hand");
                return InteractionResult.PASS;
            }

            //if not sneaking
            if (!player.isShiftKeyDown()) {
                DebugLog.log("  -> skipped: not sneaking");
                return InteractionResult.PASS;
            }

            //maps in both hands: the main hand already handled this click
            if (hand == InteractionHand.OFF_HAND && player.getMainHandItem().getItem() == Items.FILLED_MAP) {
                DebugLog.log("  -> skipped: off hand, the main hand also holds a map");
                return InteractionResult.PASS;
            }

            boolean allowed = Permissions.check(player, faewulf.squaremap.banner.dataType.Permissions.USE, 1);
            if (!allowed) {
                DebugLog.log("  -> skipped: player has no squaremap.banner.use permission");
                return InteractionResult.PASS;
            }

            //save only when something actually changed
            if (bannerRegister.tryAddBanner(player, world, hitResult.getBlockPos())) {
                boolean saved = bannerManager.save();
                DebugLog.log("  -> registered, save ok=" + saved);

                if (!saved)
                    player.sendOverlayMessage(Component.literal("Banner added, but the data file could not be saved (admins: /squaremapbanner status)"));
            }

            return InteractionResult.PASS;
        }));
    }
}
