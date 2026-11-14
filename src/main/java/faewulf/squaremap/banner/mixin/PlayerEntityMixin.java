package faewulf.squaremap.banner.mixin;

import faewulf.squaremap.banner.modules.bannerTitle;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static faewulf.squaremap.banner.Squaremapbanner.modConfigs;

@Mixin(Player.class)
public class PlayerEntityMixin {
    @Unique
    private BlockPos lastBlock;

    @Unique
    private int delayTick = 0;

    @Inject(at = @At("TAIL"), method = "aiStep")
    private void tickMovementMixin(CallbackInfo cif) {

        Player player = (Player) (Object) this;

        //only the server announces banner areas
        if (player.level().isClientSide())
            return;

        if (!modConfigs.bannerEnterAnnouncer)
            return;

        delayTick++;
        if (delayTick < 20)
            return;
        delayTick = 0;

        BlockPos currentBlock = player.blockPosition();

        //only look at the banners when the player changed block horizontally (height is irrelevant for the area)
        boolean movedToOtherBlock = lastBlock != null
                && (currentBlock.getX() != lastBlock.getX() || currentBlock.getZ() != lastBlock.getZ());

        lastBlock = currentBlock;

        if (movedToOtherBlock)
            bannerTitle.check(player.level(), player);
    }
}