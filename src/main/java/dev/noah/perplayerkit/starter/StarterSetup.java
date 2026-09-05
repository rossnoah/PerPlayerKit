/*
 * Copyright 2026 Noah Ross
 *
 * This file is part of PerPlayerKit.
 *
 * PerPlayerKit is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Affero General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * PerPlayerKit is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for
 * more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with PerPlayerKit. If not, see <https://www.gnu.org/licenses/>.
 */
package dev.noah.perplayerkit.starter;

import dev.noah.perplayerkit.KitManager;
import dev.noah.perplayerkit.KitRoomDataManager;
import dev.noah.perplayerkit.PublicKit;
import dev.noah.perplayerkit.util.BroadcastManager;
import dev.noah.perplayerkit.util.Lang;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Offers a brand new server a ready-made setup instead of leaving an admin to
 * fill five empty kit room pages by hand.
 *
 * <p>Nothing is written on its own, and nothing here leaves a file in the
 * plugin folder - the defaults are read straight out of the jar and what has
 * already been offered is kept in memory. While the kit room has never been
 * saved, admins are pointed at {@code /perplayerkit autosetup}, which fills the
 * kit room and any unassigned public kits from those bundled defaults. The
 * offer stops as soon as the kit room has been saved, however it was saved.
 *
 * <p>Autosetup only ever fills content that has never been saved on this server,
 * so it cannot overwrite an existing setup or undo an admin who deliberately
 * emptied a page. {@code autosetup reset confirm} is the explicit way to replace
 * what is already there.
 */
public final class StarterSetup {

    public static final String DOCS_URL = "https://perplayerkit.com";

    private static StarterSetup instance;

    private final Plugin plugin;

    /**
     * Whether the kit room has never been saved on this server. Held in memory
     * so the join check costs nothing; a configured server never becomes
     * unconfigured again, so this only ever flips one way.
     */
    private volatile boolean kitRoomUnconfigured;

    /** Whether an admin has already had the long version of the offer. */
    private volatile boolean welcomeShown;

    public StarterSetup(Plugin plugin) {
        this.plugin = plugin;
        instance = this;
    }

    public static StarterSetup get() {
        if (instance == null) {
            throw new IllegalStateException("StarterSetup has not been initialized yet!");
        }
        return instance;
    }

    /** Explicitly opt into starter definitions on an existing server. Custom entries win. */
    public int addMissingPublicKits() throws java.io.IOException {
        org.bukkit.configuration.file.YamlConfiguration updated = new org.bukkit.configuration.file.YamlConfiguration();
        try { updated.loadFromString(plugin.getConfig().saveToString()); }
        catch (org.bukkit.configuration.InvalidConfigurationException e) { throw new java.io.IOException(e); }
        org.bukkit.configuration.file.YamlConfiguration defaults;
        try (java.io.InputStream in = plugin.getResource("config.yml")) {
            if (in == null) throw new java.io.IOException("Bundled config.yml is missing");
            defaults = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8));
        }
        List<PublicKit> added = new ArrayList<>();
        for (String id : defaults.getConfigurationSection("publickits").getKeys(false)) {
            if (updated.contains("publickits." + id)) continue;
            updated.set("publickits." + id, defaults.get("publickits." + id));
            org.bukkit.Material icon = org.bukkit.Material.matchMaterial(defaults.getString("publickits." + id + ".icon", ""));
            if (icon != null) added.add(new PublicKit(id, defaults.getString("publickits." + id + ".name"), icon));
        }
        dev.noah.perplayerkit.ConfigFiles.write(updated, plugin.getDataFolder().toPath().resolve("config.yml"));
        plugin.reloadConfig();
        KitManager.get().getPublicKitList().addAll(added);
        return added.size();
    }

    /** What an autosetup run actually filled in. */
    public record Result(List<Integer> kitRoomPages, List<String> publicKits) {

        public boolean isEmpty() {
            return kitRoomPages.isEmpty() && publicKits.isEmpty();
        }
    }

    /**
     * Works out whether this server has a kit room yet and, while it has none,
     * prints a short orientation in the console.
     *
     * <p>The guide is tied to the kit room being empty rather than to a flag
     * saved somewhere, so it keeps offering itself until the server is set up
     * and then costs a configured server nothing, forever. Nothing about it is
     * written to disk.
     */
    public void checkOnStartup() {
        // loadFromDB() has just read every page, so reuse what it found.
        kitRoomUnconfigured = !KitRoomDataManager.get().loadedAnyPage();

        if (!kitRoomUnconfigured) {
            return;
        }

        plugin.getLogger().info("Getting started:");
        plugin.getLogger().info("  /kit          - the kit menu, and the way into the kit room");
        plugin.getLogger().info("  /publickit    - the server kits players can load");
        plugin.getLogger().info("Your kit room is empty. Run /perplayerkit autosetup to fill it and the");
        plugin.getLogger().info("public kits with a ready-made crystal room and the bundled starter kits.");
        plugin.getLogger().info("Permissions:");
        plugin.getLogger().info("  perplayerkit.use    - give this one to your default group");
        plugin.getLogger().info("  perplayerkit.staff  - inspect other players' kits");
        plugin.getLogger().info("  perplayerkit.admin  - kit room editing and admin commands");
        plugin.getLogger().info("Players need perplayerkit.use before /kit works for them.");
        plugin.getLogger().info("Storage is " + plugin.getConfig().getString("storage.type", "sqlite")
                + " - see config.yml to change it. Docs: " + DOCS_URL);
    }

    /** Whether admins should still be offered {@code /perplayerkit autosetup}. */
    public boolean needsAutoSetup() {
        return kitRoomUnconfigured;
    }

    /**
     * Called whenever kit room pages are written, so the offer stops as soon as
     * the server has a kit room - whether autosetup made it or an admin did.
     * A no-op before the plugin has finished enabling.
     */
    public static void notifyKitRoomSaved() {
        if (instance != null) {
            instance.kitRoomUnconfigured = false;
        }
    }

    /**
     * Fills anything that has never been configured on this server.
     *
     * @param overwrite true to replace existing kit room pages and public kits too
     */
    public Result apply(boolean overwrite) {
        List<Integer> seededPages = fillKitRoom(overwrite);
        List<String> seededKits = fillPublicKits(overwrite);
        return new Result(seededPages, seededKits);
    }

    private List<Integer> fillKitRoom(boolean overwrite) {
        List<ItemStack[]> defaults = StarterDefaults.loadKitRoomPages(plugin);
        List<Integer> filled = new ArrayList<>();

        int pages = Math.min(defaults.size(), KitRoomDataManager.PAGE_COUNT);
        for (int page = 0; page < pages; page++) {
            if (!overwrite && KitRoomDataManager.hasStoredPage(page)) {
                continue;
            }
            KitRoomDataManager.get().setKitRoom(page, defaults.get(page));
            filled.add(page);
        }

        if (!filled.isEmpty()) {
            KitRoomDataManager.get().savePagesToDBAsync(filled);
        }
        return filled;
    }

    private List<String> fillPublicKits(boolean overwrite) {
        List<PublicKit> listed = KitManager.get().getPublicKitList();
        Set<String> wanted = listed.stream()
                .map(kit -> kit.id.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());

        Map<String, ItemStack[]> defaults = StarterDefaults.loadPublicKits(plugin, wanted);
        List<String> filled = new ArrayList<>();

        for (PublicKit kit : listed) {
            ItemStack[] contents = defaults.get(kit.id.toLowerCase(Locale.ROOT));
            if (contents == null) {
                continue;
            }
            if (!overwrite && KitManager.get().hasPublicKit(kit.id)) {
                continue;
            }
            if (!KitManager.get().savePublicKit(kit.id, contents.clone())) {
                plugin.getLogger().warning("Starter public kit " + kit.id + " was empty, skipping");
                continue;
            }
            filled.add(kit.id);
        }

        for (String id : filled) {
            KitManager.get().savePublicKitToDB(id);
        }
        return filled;
    }

    /**
     * Offers autosetup to an admin who just joined. The first admin gets the
     * full introduction and everyone after that gets a single line, so the
     * offer stays visible without becoming join spam.
     */
    public void offerAutoSetup(Player player) {
        if (!player.isOnline() || !kitRoomUnconfigured) {
            return;
        }

        String key = "welcome.first-admin";
        if (welcomeShown) {
            key = "welcome.setup-reminder";
        } else {
            welcomeShown = true;
        }

        for (String line : Lang.get().rawList(key)) {
            BroadcastManager.get().sendComponentMessage(player, MiniMessage.miniMessage().deserialize(line));
        }
    }
}
