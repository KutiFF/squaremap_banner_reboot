package faewulf.squaremap.banner.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Per-player cooldown for registering banners. Only touched from the server thread.
 */
public class RegisterCooldown {
    private static final Map<UUID, Long> LAST_REGISTER = new HashMap<>();

    /**
     * @return milliseconds the player still has to wait, 0 if registering is allowed now
     */
    public static long remainingMs(UUID player, long cooldownMs) {
        Long last = LAST_REGISTER.get(player);
        if (last == null)
            return 0;

        long elapsedMs = (System.nanoTime() - last) / 1_000_000L;
        return Math.max(0, cooldownMs - elapsedMs);
    }

    public static void mark(UUID player) {
        LAST_REGISTER.put(player, System.nanoTime());
    }

    /**
     * Called when a player disconnects: drops the entry only if it no longer matters,
     * so relogging does not reset a running cooldown but the map cannot grow forever.
     */
    public static void forgetIfExpired(UUID player, long cooldownMs) {
        if (remainingMs(player, cooldownMs) == 0)
            LAST_REGISTER.remove(player);
    }
}