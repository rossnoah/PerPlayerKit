package dev.noah.perplayerkit.util;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Evaluates global and action rules against the location at the time of use. */
public final class LocationAccess {
    private static LocationAccess instance;
    private final Plugin plugin;
    private final Map<LocationFeature, LocationRules.Rule> rules;
    private final Set<String> warnings = new HashSet<>();

    public LocationAccess(Plugin plugin) {
        this.plugin = plugin;
        this.rules = LocationRules.parse(plugin.getConfig());
        instance = this;
    }
    public static LocationAccess get() { return instance; }

    public record Decision(boolean allowed, String rule, String reason, boolean unavailable) {}

    public Decision check(Player player, LocationFeature feature) {
        RegionLookup lookup = new RegionLookup(player);
        for (LocationFeature current : feature == LocationFeature.GLOBAL
                ? List.of(LocationFeature.GLOBAL) : List.of(LocationFeature.GLOBAL, feature)) {
            LocationRules.Rule rule = rules.get(current);
            if (rule == null) continue;
            String path = "locations." + current.key();
            try {
                if (!rule.permits(player.getWorld().getName(), lookup::get))
                    return new Decision(false, path, rule.allow() ? "No allow entry matched" : "A deny entry matched", false);
            } catch (RegionUnavailableException e) {
                warn(path, player, e);
                return new Decision(false, path, e.getMessage(), true);
            }
        }
        return new Decision(true, "locations." + feature.key(), "Global and feature rules allow this location", false);
    }

    public boolean require(Player player, LocationFeature feature) {
        Decision decision = check(player, feature);
        if (decision.allowed()) return true;
        String message = "error.disabled-at-location";
        LocationRules.Rule global = rules.get(LocationFeature.GLOBAL);
        if (decision.rule().equals("locations.global") && global != null
                && global.entries().stream().allMatch(entry -> entry.region() == null)) message = "error.disabled-in-world";
        Lang.get().sendNoPrefix(player, decision.unavailable() ? "error.regions-unavailable" : message);
        SoundManager.playFailure(player);
        return false;
    }

    /** Only callers with a relevant region entry need WorldGuard. */
    public List<String> regions(Player player) {
        if (!plugin.getServer().getPluginManager().isPluginEnabled("WorldGuard"))
            throw new RegionUnavailableException("WorldGuard is not enabled");
        try { return WorldGuardSupport.getRegionIdsByPriority(player); }
        catch (RuntimeException | LinkageError e) {
            throw new RegionUnavailableException("WorldGuard regions are unavailable: " + e.getMessage());
        }
    }

    public void warn(String path, Player player, RegionUnavailableException error) {
        String message = path + " in world '" + player.getWorld().getName() + "': " + error.getMessage();
        if (warnings.add(message)) plugin.getLogger().warning(message + ". Skipping affected actions until regions are available.");
    }

    public static final class RegionUnavailableException extends RuntimeException {
        public RegionUnavailableException(String message) { super(message); }
    }

    private final class RegionLookup {
        private final Player player;
        private List<String> ids;
        RegionLookup(Player player) { this.player = player; }
        List<String> get() {
            if (ids == null) ids = regions(player);
            return ids;
        }
    }
}
