package faewulf.squaremap.banner.events;

import faewulf.squaremap.banner.utils.DebugLog;
import faewulf.squaremap.banner.utils.bannerManager;
import faewulf.squaremap.banner.utils.bannerRegister;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Items;

public class onLeftClick {
    public static void load() {
        AttackBlockCallback.EVENT.register(((player, world, hand, pos, direction) -> {

            //cheap checks first: this callback runs for every block attack of every player
            if (world.isClientSide())
                return InteractionResult.PASS;

            //if not holding filled map
            if (player.getItemInHand(hand).getItem() != Items.FILLED_MAP)
                return InteractionResult.PASS;

            //if not sneaking
            if (!player.isShiftKeyDown())
                return InteractionResult.PASS;

            if (!Permissions.check(player, faewulf.squaremap.banner.dataType.Permissions.USE, 1)) {
                DebugLog.log("AttackBlock: skipped, no squaremap.banner.use permission");
                return InteractionResult.PASS;
            }

            //save only when a marker was actually removed
            boolean removed = bannerRegister.tryRemoveBanner(player, world, pos);
            DebugLog.log(() -> "AttackBlock (shift + map) at " + pos + ": marker removed=" + removed);

            if (removed)
                bannerManager.save();

            return InteractionResult.PASS;
        }));
    }
}
