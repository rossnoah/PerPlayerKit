package dev.noah.perplayerkit.listeners;

import dev.noah.perplayerkit.KitManager;
import dev.noah.perplayerkit.util.*;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import java.util.*;
import java.util.logging.Logger;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AutoRekitListenerTest {
    Plugin plugin;
    YamlConfiguration config;
    Player player;
    KitManager kits;
    AutoRekitListener listener;
    List<Runnable> scheduled;
    BukkitTask task;
    MockedStatic<KitManager> manager;
    MockedStatic<WorldGuardSupport> worldGuard;
    @BeforeEach void setup() {
        plugin = mock(Plugin.class, RETURNS_DEEP_STUBS);
        config = new YamlConfiguration(); when(plugin.getConfig()).thenReturn(config);
        when(plugin.getLogger()).thenReturn(mock(Logger.class));
        player = mock(Player.class);
        when(player.getUniqueId()).thenReturn(UUID.randomUUID());
        when(player.isOnline()).thenReturn(true);
        when(player.hasPermission(anyString())).thenReturn(true);
        World world = mock(World.class); when(world.getName()).thenReturn("pvp"); when(player.getWorld()).thenReturn(world);
        when(plugin.getServer().getPlayer(player.getUniqueId())).thenReturn(player);
        config.set("rekit.kill.enabled", true);
        new LocationAccess(plugin);
        kits = mock(KitManager.class); manager = mockStatic(KitManager.class); manager.when(KitManager::get).thenReturn(kits);
        when(kits.getLastKitReference(player.getUniqueId())).thenReturn(new KitManager.KitReference(1, null));
        when(kits.getPublicKitList()).thenReturn(List.of());
        when(kits.hasPublicKit(anyString())).thenReturn(true);
        when(kits.loadPublicKitSilent(eq(player), anyString())).thenReturn(true);
        scheduled = new ArrayList<>(); task = mock(BukkitTask.class);
        var scheduler = plugin.getServer().getScheduler();
        doAnswer(i -> { scheduled.add(i.getArgument(1)); return task; })
                .when(scheduler).runTaskLater(eq(plugin), any(Runnable.class), anyLong());
        worldGuard = mockStatic(WorldGuardSupport.class);
        when(plugin.getServer().getPluginManager().isPluginEnabled("WorldGuard")).thenReturn(true);
        worldGuard.when(() -> WorldGuardSupport.getRegionIdsByPriority(player)).thenReturn(List.of("inner", "outer"));
        listener = new AutoRekitListener(plugin);
    }
    @AfterEach void close() { if (worldGuard != null) worldGuard.close(); if (manager != null) manager.close(); }
    void fire(boolean respawn) {
        if (respawn) { queueRespawn(); scheduled.get(scheduled.size() - 1).run(); }
        else {
            PlayerDeathEvent death = mock(PlayerDeathEvent.class); Player victim = mock(Player.class);
            when(death.getEntity()).thenReturn(victim); when(victim.getKiller()).thenReturn(player);
            listener.onPlayerKill(death);
        }
    }
    void queueRespawn() {
        PlayerRespawnEvent event = mock(PlayerRespawnEvent.class); when(event.getPlayer()).thenReturn(player);
        listener.onRespawn(event);
    }
    String path(boolean respawn) { return "rekit." + (respawn ? "respawn" : "kill"); }
    @ParameterizedTest @ValueSource(booleans={true, false})
    void bothTriggersUseHighestPriorityConfiguredRegionBeforeWorld(boolean respawn) {
        config.set(path(respawn) + ".kits.pvp", "world-kit");
        config.set(path(respawn) + ".kits.pvp:outer", "outer-kit");
        config.set(path(respawn) + ".kits.pvp:inner", "inner-kit");
        config.set(path(respawn) + ".restore-enderchest", true);
        fire(respawn);
        verify(kits).loadPublicKitSilent(player, "inner-kit");
        verify(kits, never()).loadLastKit(any());
        verify(kits).restoreLastEnderchest(player);
    }
    @ParameterizedTest @ValueSource(booleans={true, false})
    void unavailableRegionsDoNotGiveWrongWorldOrRememberedKit(boolean respawn) {
        config.set(path(respawn) + ".kits.pvp", "world-kit");
        config.set(path(respawn) + ".kits.pvp:arena", "arena-kit");
        config.set(path(respawn) + ".restore-enderchest", true);
        when(plugin.getServer().getPluginManager().isPluginEnabled("WorldGuard")).thenReturn(false);
        fire(respawn);
        verify(kits, never()).loadPublicKitSilent(any(), anyString());
        verify(kits, never()).loadLastKit(any());
        verify(kits, never()).restoreLastEnderchest(any());
    }
    @ParameterizedTest @ValueSource(booleans={true, false})
    void worldMappingsWorkWithoutWorldGuardDespiteRegionsInAnotherWorld(boolean respawn) {
        config.set(path(respawn) + ".kits.pvp", "world-kit");
        config.set(path(respawn) + ".kits.other:arena", "arena-kit");
        when(plugin.getServer().getPluginManager().isPluginEnabled("WorldGuard")).thenReturn(false);
        fire(respawn);
        verify(kits).loadPublicKitSilent(player, "world-kit");
    }
    @ParameterizedTest @ValueSource(booleans={true, false})
    void featureLocationDenialPreventsInventoryAndEnderchestRestore(boolean respawn) {
        String feature = respawn ? "rekit-respawn" : "rekit-kill";
        config.set("locations." + feature + ".mode", "deny");
        config.set("locations." + feature + ".entries", List.of("pvp:inner"));
        config.set(path(respawn) + ".restore-enderchest", true);
        new LocationAccess(plugin); fire(respawn);
        verify(kits, never()).loadLastKit(any()); verify(kits, never()).restoreLastEnderchest(any());
    }
    @Test void delayedRespawnChecksDestinationAndCurrentPermissions() {
        config.set("locations.rekit-respawn.mode", "allow"); config.set("locations.rekit-respawn.entries", List.of("spawn"));
        new LocationAccess(plugin);
        queueRespawn(); when(player.getWorld().getName()).thenReturn("spawn"); scheduled.get(0).run();
        verify(kits).loadLastKit(player); clearInvocations(kits);
        queueRespawn(); when(player.hasPermission("perplayerkit.rekitonrespawn")).thenReturn(false); scheduled.get(1).run();
        verify(kits, never()).loadLastKit(any());
    }
    @Test void quitAndNewRespawnCancelOldTasksAndOldPlayerObjectCannotBeRestored() {
        queueRespawn(); queueRespawn(); verify(task).cancel();
        PlayerQuitEvent quit = mock(PlayerQuitEvent.class); when(quit.getPlayer()).thenReturn(player);
        listener.onQuit(quit); verify(task, times(2)).cancel();
        when(plugin.getServer().getPlayer(player.getUniqueId())).thenReturn(mock(Player.class));
        scheduled.forEach(Runnable::run);
        verify(kits, never()).loadLastKit(any());
    }
    @ParameterizedTest @ValueSource(booleans={true, false})
    void missingFixedKitFallsBackToRememberedSelection(boolean respawn) {
        config.set(path(respawn) + ".kits.pvp", "missing"); when(kits.hasPublicKit("missing")).thenReturn(false);
        fire(respawn); verify(kits).loadLastKit(player);
    }
}
