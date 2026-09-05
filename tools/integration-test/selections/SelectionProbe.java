package ppk.selectionprobe;

import dev.noah.perplayerkit.KitManager;
import dev.noah.perplayerkit.PerPlayerKit;
import dev.noah.perplayerkit.kitdata.KitDataService;
import dev.noah.perplayerkit.storage.StorageMigrator;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/** Uses real item serialization and storage across separate server processes. */
public final class SelectionProbe extends JavaPlugin {
    private static final UUID PRIVATE = UUID.fromString("77102cca-dee0-4c98-89b5-9ac05c4bfd00");
    private static final UUID PUBLIC = UUID.fromString("c6b50c6a-2f6e-43be-8c3e-4a171c101f13");
    @Override public void onEnable() {
        Bukkit.getScheduler().runTask(this, () -> {
            try {
                String phase = Files.readString(Path.of("selection-phase.txt")).trim();
                if (phase.equals("seed")) seed(); else verifyAfterJoin();
            } catch (Throwable error) { fail(error); }
        });
    }
    private void seed() {
        KitManager kits = KitManager.get();
        check(PerPlayerKit.storageManager.getAllKitIDs().isEmpty(), "Expected an empty test database");
        ItemStack[] personal = new ItemStack[41]; personal[0] = new ItemStack(Material.STONE, 32);
        ItemStack[] shared = new ItemStack[41]; shared[0] = new ItemStack(Material.DIRT, 16);
        ItemStack[] ec = new ItemStack[27]; ec[0] = new ItemStack(Material.GRAVEL, 8);
        check(kits.savekit(PRIVATE, 3, personal, true), "save personal kit");
        check(kits.savePublicKit("persisted", shared), "save public kit"); kits.savePublicKitToDB("persisted");
        check(kits.saveECSilent(PRIVATE, 2, ec) && kits.saveECSilent(PUBLIC, 2, ec), "save enderchests");
        Player privatePlayer = player(PRIVATE), publicPlayer = player(PUBLIC);
        check(kits.loadKit(privatePlayer, 3), "manual private load");
        check(kits.loadPublicKit(publicPlayer, "persisted"), "manual public load");
        check(kits.loadEnderchest(privatePlayer, 2) && kits.loadEnderchest(publicPlayer, 2), "manual enderchest loads");
        // Only the test waits here, to ensure the seed reached storage before the process exits.
        kits.storageWork().run(() -> {}).join();
        kits.unloadPlayer(PRIVATE); kits.unloadPlayer(PUBLIC);
        var snapshot = new KitDataService(PerPlayerKit.storageManager, kits).buildSnapshot(KitDataService.Scope.PLAYERKITS);
        check(snapshot.getPlayerKits().size() == 3, "Preference records leaked into inventory exports");
        if (PerPlayerKit.getPlugin().getConfig().getString("storage.type").equals("sqlite")) {
            var migration = new StorageMigrator(PerPlayerKit.getPlugin()).migrate("sqlite", "yaml", null);
            check(migration.isSuccess() && migration.getFailedCount() == 0 && migration.getMigratedCount() == 8,
                    "Storage migration did not copy inventories and selections");
        }
        getLogger().info("PPK_SELECTION_PASS seed");
    }
    private void verifyAfterJoin() {
        KitManager kits = KitManager.get();
        kits.loadPlayerDataAsync(PRIVATE); kits.loadPlayerDataAsync(PUBLIC);
        new org.bukkit.scheduler.BukkitRunnable() {
            private int ticks;
            @Override public void run() {
                if (++ticks > 200) { cancel(); fail(new AssertionError("Player data did not finish loading")); return; }
                if (kits.isLoading(PRIVATE) || kits.isLoading(PUBLIC)) return;
                cancel();
                try {
                    check(kits.getLastKitReference(PRIVATE).slot() == 3, "Private slot lost across processes");
                    check("persisted".equals(kits.getLastKitReference(PUBLIC).publicId()), "Public selection lost across processes");
                    for (UUID id : new UUID[]{PRIVATE, PUBLIC}) {
                        Player player = player(id); Material expected = id.equals(PRIVATE) ? Material.STONE : Material.DIRT;
                        check(kits.regearLastKit(player), "Regear has no remembered kit");
                        check(player.getInventory().getContents()[0].getType() == expected, "Regear used wrong kit");
                        check(kits.loadLastKit(player), "Respawn restore has no remembered kit");
                        check(player.getInventory().getContents()[0].getType() == expected, "Respawn restore used wrong kit");
                        kits.restoreLastEnderchest(player);
                        check(player.getEnderChest().getItem(0).getType() == Material.GRAVEL, "Enderchest selection lost");
                    }
                    getLogger().info("PPK_SELECTION_PASS verify");
                } catch (Throwable error) { fail(error); }
            }
        }.runTaskTimer(this, 1, 1);
    }
    private Player player(UUID uuid) {
        ItemStack[][] contents = {new ItemStack[41]};
        PlayerInventory inventory = (PlayerInventory) Proxy.newProxyInstance(getClassLoader(), new Class<?>[]{PlayerInventory.class}, (proxy, method, args) -> {
            if (method.getName().equals("getContents")) return contents[0].clone();
            if (method.getName().equals("setContents")) { contents[0] = ((ItemStack[]) args[0]).clone(); return null; }
            return defaultValue(proxy, method.getName(), method.getReturnType(), args);
        });
        Inventory enderchest = Bukkit.createInventory(null, 27);
        return (Player) Proxy.newProxyInstance(getClassLoader(), new Class<?>[]{Player.class}, (proxy, method, args) -> switch (method.getName()) {
            case "getUniqueId" -> uuid;
            case "getName", "getDisplayName" -> "SelectionProbe";
            case "getInventory" -> inventory;
            case "getEnderChest" -> enderchest;
            case "getWorld" -> Bukkit.getWorlds().get(0);
            case "getLocation" -> Bukkit.getWorlds().get(0).getSpawnLocation();
            case "isOnline", "hasPermission", "isOp" -> true;
            default -> defaultValue(proxy, method.getName(), method.getReturnType(), args);
        });
    }
    private static Object defaultValue(Object proxy, String name, Class<?> type, Object[] args) {
        if (name.equals("hashCode")) return System.identityHashCode(proxy);
        if (name.equals("equals")) return proxy == args[0];
        if (name.equals("toString")) return "SelectionProbe";
        if (type == boolean.class) return false;
        if (type == int.class) return 0;
        if (type == float.class) return 0f;
        if (type == double.class) return 0d;
        if (type == long.class) return 0L;
        return null;
    }
    private static void check(boolean result, String reason) { if (!result) throw new AssertionError(reason); }
    private void fail(Throwable error) { error.printStackTrace(); getLogger().severe("PPK_SELECTION_FAIL " + error); }
}
