package faewulf.squaremap.banner.dataType;

import xyz.jpenilla.squaremap.api.Key;
import xyz.jpenilla.squaremap.api.marker.Icon;

import java.util.UUID;

/**
 * @param owner     UUID of the player who first registered the banner; null for banners saved by older versions
 * @param ownerName last known name of the owner, used by "/squaremapbanner list <player>" (works for offline players)
 */
public record Data(Icon marker, Key keyIconType, String name, Key key, UUID owner, String ownerName) {
}