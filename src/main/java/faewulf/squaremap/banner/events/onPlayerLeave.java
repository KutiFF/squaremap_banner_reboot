package faewulf.squaremap.banner.events;

import faewulf.squaremap.banner.modules.bannerTitle;
import faewulf.squaremap.banner.utils.RegisterCooldown;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

import java.util.UUID;

import static faewulf.squaremap.banner.Squaremapbanner.modConfigs;

public class onPlayerLeave {
    public static void load() {
        //always registered: announce_when_near_banner can be switched on later with /squaremapbanner reload
        ServerPlayConnectionEvents.DISCONNECT.register(((handler, server) -> {
            UUID id = handler.getPlayer().getUUID();

            bannerTitle.forget(id);
            RegisterCooldown.forgetIfExpired(id, modConfigs.registerCooldownSeconds * 1000L);
        }));
    }
}