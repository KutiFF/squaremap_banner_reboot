package faewulf.squaremap.banner.dataType;

import com.google.gson.annotations.SerializedName;

import java.util.Arrays;
import java.util.HashSet;

public class ModConfigs {
    @SerializedName("banner_radius")
    public int radius = 80;

    @SerializedName("announce_when_near_banner")
    public boolean bannerEnterAnnouncer = true;

    @SerializedName("register_cooldown_seconds")
    public int registerCooldownSeconds = 10;

    @SerializedName("cleanup_interval_seconds")
    public int cleanupIntervalSeconds = 30;

    @SerializedName("cleanup_banners_per_tick")
    public int cleanupBannersPerTick = 20;

    /** Writes a step-by-step trace to squaremap/banner-debug.log. Also switchable with /squaremapbanner debug on|off. */
    @SerializedName("debug")
    public boolean debug = false;

    public HashSet<String> blacklist = new HashSet<>(Arrays.asList("nigga", "slut"));
}
