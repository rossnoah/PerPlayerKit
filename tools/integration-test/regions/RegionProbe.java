package ppk.regionprobe;

import dev.noah.perplayerkit.util.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/** Real WorldGuard geometry, priority, and optional-dependency checks on a throwaway server. */
public final class RegionProbe extends JavaPlugin {
    @Override public void onEnable() {
        Bukkit.getScheduler().runTask(this, () -> {
            try { runChecks(); getLogger().info("PPK_REGIONS_PASS"); }
            catch (Throwable error) { error.printStackTrace(); getLogger().severe("PPK_REGIONS_FAIL " + error); }
        });
    }

    private void runChecks() throws Exception {
        Plugin plugin = Bukkit.getPluginManager().getPlugin("PerPlayerKit");
        World world = Bukkit.getWorlds().get(0);
        String name = world.getName();
        AtomicReference<Location> position = new AtomicReference<>(new Location(world, 15, 75, 15));
        Player player = (Player) Proxy.newProxyInstance(getClassLoader(), new Class<?>[]{Player.class}, (proxy, method, args) -> switch (method.getName()) {
            case "getLocation" -> position.get();
            case "getWorld" -> world;
            case "getName" -> "RegionProbe";
            case "getUniqueId" -> UUID.fromString("19d355ca-e068-4296-b876-6a83927568a9");
            case "hasPermission", "isOnline" -> true;
            case "isDead" -> false;
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy == args[0];
            case "toString" -> "RegionProbePlayer";
            default -> throw new UnsupportedOperationException(method.toString());
        });
        plugin.getConfig().set("locations", null);
        rule(plugin, "kit-room", "deny", name + ":outer");
        LocationAccess access = new LocationAccess(plugin);
        boolean expected = Boolean.parseBoolean(java.nio.file.Files.readString(java.nio.file.Path.of("expect-worldguard.txt")));
        require(Bukkit.getPluginManager().isPluginEnabled("WorldGuard") == expected, "WorldGuard did not reach the expected enabled state");
        if (!expected) {
            require(access.check(player, LocationFeature.KIT_ROOM).unavailable(), "Missing WorldGuard allowed a region denial");
            require(access.check(player, LocationFeature.HEAL).allowed(), "Unrelated action required WorldGuard");
            getLogger().info("Checked startup without WorldGuard");
            return;
        }
        WorldGuardFixture.create(world);
        List<String> regions = access.regions(player);
        require(regions.equals(List.of("template", "alpha", "inner", "outer")), "Wrong priority/tie order: " + regions);
        require(!access.check(player, LocationFeature.KIT_ROOM).allowed(), "Higher priority region overrode deny filter");
        rule(plugin, "kit-room", "allow", name + ":inner");
        access = new LocationAccess(plugin);
        require(access.check(player, LocationFeature.KIT_ROOM).allowed(), "Inside allow region was denied");
        position.set(new Location(world, 5, 75, 5));
        require(!access.check(player, LocationFeature.KIT_ROOM).allowed(), "Outside inner region was allowed");
        position.set(new Location(world, 15, 150, 15));
        require(!access.check(player, LocationFeature.KIT_ROOM).allowed(), "Region height was ignored");
        position.set(new Location(world, 15, 75, 15));
        for (String trigger : List.of("respawn", "kill")) {
            YamlConfiguration config = new YamlConfiguration();
            String path = "rekit." + trigger + ".kits";
            config.set(path + "." + name, "world-kit");
            config.set(path + "." + name + ":inner", "inner-kit");
            config.set(path + "." + name + ":alpha", "alpha-kit");
            RekitKitResolver.validate(config);
            require("alpha-kit".equals(RekitKitResolver.resolveKit(config.getConfigurationSection(path), name, regions)), "Wrong " + trigger + " kit");
        }
        rule(plugin, "kit-room", "deny", name + ":template");
        require(!new LocationAccess(plugin).check(player, LocationFeature.KIT_ROOM).allowed(), "Inherited parent did not match filter");
        rule(plugin, "global", "deny", name);
        access = new LocationAccess(plugin);
        require(!access.check(player, LocationFeature.KIT_ROOM).allowed(), "Feature overrode global denial");
        plugin.getConfig().set("locations.global", null);
        WorldGuardFixture.unload(world);
        require(new LocationAccess(plugin).check(player, LocationFeature.KIT_ROOM).unavailable(), "Unavailable region data was treated as empty space");
        Bukkit.getPluginManager().disablePlugin(Bukkit.getPluginManager().getPlugin("WorldGuard"));
        access = new LocationAccess(plugin);
        require(access.check(player, LocationFeature.KIT_ROOM).unavailable(), "Disabled WorldGuard permitted a region action");
        require(access.check(player, LocationFeature.REGEAR).allowed(), "Disabled WorldGuard blocked unrelated action");
        rule(plugin, "kit-room", "allow", name, name + ":inner");
        require(new LocationAccess(plugin).check(player, LocationFeature.KIT_ROOM).allowed(), "World match unnecessarily required WorldGuard");
    }
    private static void rule(Plugin plugin, String feature, String mode, String... entries) {
        plugin.getConfig().set("locations." + feature + ".mode", mode);
        plugin.getConfig().set("locations." + feature + ".entries", List.of(entries));
    }
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    /** Kept separate so the main probe can load with no WorldGuard classes installed. */
    private static final class WorldGuardFixture {
        static void create(World world) {
            var manager = com.sk89q.worldguard.WorldGuard.getInstance().getPlatform().getRegionContainer()
                    .get(com.sk89q.worldedit.bukkit.BukkitAdapter.adapt(world));
            if (manager == null) throw new AssertionError("No region manager");
            for (String id : List.of("outer", "inner", "alpha")) {
                int min = id.equals("outer") ? 0 : 10;
                int max = id.equals("outer") ? 30 : 20;
                var region = new com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion(id,
                        com.sk89q.worldedit.math.BlockVector3.at(min, 50, min),
                        com.sk89q.worldedit.math.BlockVector3.at(max, 100, max));
                region.setPriority(id.equals("outer") ? 0 : 10);
                manager.addRegion(region);
            }
            var parent = new com.sk89q.worldguard.protection.regions.GlobalProtectedRegion("template");
            parent.setPriority(20);
            manager.addRegion(parent);
            try { manager.getRegion("inner").setParent(parent); }
            catch (com.sk89q.worldguard.protection.regions.ProtectedRegion.CircularInheritanceException e) { throw new AssertionError(e); }
        }
        static void unload(World world) {
            com.sk89q.worldguard.WorldGuard.getInstance().getPlatform().getRegionContainer()
                    .unload(com.sk89q.worldedit.bukkit.BukkitAdapter.adapt(world));
        }
    }
}
