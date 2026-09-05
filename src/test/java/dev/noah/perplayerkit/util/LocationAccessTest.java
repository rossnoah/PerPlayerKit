package dev.noah.perplayerkit.util;

import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.MockedStatic;

import java.util.List;
import java.util.logging.Logger;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LocationAccessTest {
    Plugin plugin;
    Player player;
    YamlConfiguration config;
    @BeforeEach void setup() {
        plugin = mock(Plugin.class, RETURNS_DEEP_STUBS);
        player = mock(Player.class);
        World world = mock(World.class);
        when(world.getName()).thenReturn("pvp"); when(player.getWorld()).thenReturn(world);
        config = new YamlConfiguration(); when(plugin.getConfig()).thenReturn(config);
        when(plugin.getLogger()).thenReturn(mock(Logger.class));
    }
    void rule(LocationFeature feature, String mode, String... entries) {
        config.set("locations." + feature.key() + ".mode", mode);
        config.set("locations." + feature.key() + ".entries", List.of(entries));
    }
    @ParameterizedTest @EnumSource(LocationFeature.class)
    void globalDenialCannotBeOverriddenByAnyFeature(LocationFeature feature) {
        rule(feature, "allow", "pvp"); rule(LocationFeature.GLOBAL, "deny", "pvp");
        var result = new LocationAccess(plugin).check(player, feature);
        assertFalse(result.allowed()); assertEquals("locations.global", result.rule());
    }
    @Test void manualKitsCanBeDeniedWithoutDisablingAutomaticRekit() {
        rule(LocationFeature.KITS, "deny", "pvp");
        LocationAccess access = new LocationAccess(plugin);
        assertFalse(access.check(player, LocationFeature.KITS).allowed());
        assertTrue(access.check(player, LocationFeature.REKIT_RESPAWN).allowed());
        assertTrue(access.check(player, LocationFeature.REKIT_KILL).allowed());
    }
    @Test void missingWorldGuardDeniesOnlyActionsThatNeedRegions() {
        rule(LocationFeature.REGEAR, "deny", "pvp:arena");
        LocationAccess access = new LocationAccess(plugin);
        var result = access.check(player, LocationFeature.REGEAR);
        assertFalse(result.allowed()); assertTrue(result.unavailable());
        assertTrue(access.check(player, LocationFeature.HEAL).allowed());
        access.check(player, LocationFeature.REGEAR);
        verify(plugin.getLogger(), times(1)).warning(anyString());
        when(player.getWorld().getName()).thenReturn("survival");
        assertTrue(access.check(player, LocationFeature.REGEAR).allowed());
    }
    @Test void globalAndFeatureShareOneQueryButNextActionUsesNewLocation() {
        rule(LocationFeature.GLOBAL, "allow", "pvp:arena");
        rule(LocationFeature.REGEAR, "deny", "pvp:spawn");
        when(plugin.getServer().getPluginManager().isPluginEnabled("WorldGuard")).thenReturn(true);
        try (MockedStatic<WorldGuardSupport> wg = mockStatic(WorldGuardSupport.class)) {
            wg.when(() -> WorldGuardSupport.getRegionIdsByPriority(player)).thenReturn(List.of("arena"), List.of("spawn"));
            LocationAccess access = new LocationAccess(plugin);
            assertTrue(access.check(player, LocationFeature.REGEAR).allowed());
            wg.verify(() -> WorldGuardSupport.getRegionIdsByPriority(player), times(1));
            assertFalse(access.check(player, LocationFeature.REGEAR).allowed());
        }
    }
    @Test void brokenWorldGuardFailsClosedAndRecoversWithoutRestart() {
        rule(LocationFeature.REPAIR, "deny", "pvp:spawn");
        when(plugin.getServer().getPluginManager().isPluginEnabled("WorldGuard")).thenReturn(true);
        try (MockedStatic<WorldGuardSupport> wg = mockStatic(WorldGuardSupport.class)) {
            wg.when(() -> WorldGuardSupport.getRegionIdsByPriority(player))
                    .thenThrow(new NoSuchMethodError("Incompatible build")).thenReturn(List.of("arena"));
            LocationAccess access = new LocationAccess(plugin);
            assertTrue(access.check(player, LocationFeature.REPAIR).unavailable());
            assertTrue(access.check(player, LocationFeature.REPAIR).allowed());
        }
    }
}
