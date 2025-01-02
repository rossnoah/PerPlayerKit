package dev.noah.perplayerkit.util;

import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

public class BroadcastManager {

    private static final int LINE_LENGTH = 60; // Length of the strikethrough line
    private static final String FIGURE_SPACE = "\u2007"; // A whitespace character of consistent width
    private static BroadcastManager instance;
    private final int broadcastDistance = 200;
    private final Plugin plugin;
    private final CooldownManager repairBroadcastCooldown = new CooldownManager(5);
    private final CooldownManager kitroomBroadcastCooldown = new CooldownManager(15);
    private final BukkitAudiences audience;
    private final Component prefix;

    public BroadcastManager(Plugin plugin) {
        this.plugin = plugin;
        audience = BukkitAudiences.create(plugin);
        prefix = MiniMessage.miniMessage().deserialize(plugin.getConfig().getString("prefix", "<gray>[<aqua>Kits</aqua>]</gray> "));
        instance = this;
    }

    public static BroadcastManager get() {
        if (instance == null) {
            throw new IllegalStateException("BroadcastManager has not been initialized yet!");
        }
        return instance;
    }

    public static Component generateBroadcastComponent(String message) {
        String strikeThroughLine = "<gray>" + " ".repeat(3) + "<st>" + FIGURE_SPACE.repeat(LINE_LENGTH) + "</st>";

        int messageLength = MiniMessage.miniMessage().stripTags(message).length();

        int padding = (LINE_LENGTH - messageLength) / 2;

        String formattedMessage = strikeThroughLine + "\n\n" + " ".repeat(3) + FIGURE_SPACE.repeat(Math.max(padding, 0)) + message + "\n\n" + strikeThroughLine;

        return MiniMessage.miniMessage().deserialize(formattedMessage);
    }

    private void broadcastMessage(Player player, String message) {
        World world = player.getWorld();

        for (Player broadcastPlayer : world.getPlayers()) {
            if (broadcastPlayer.getLocation().distance(player.getLocation()) < broadcastDistance) {
                audience.player(broadcastPlayer).sendMessage(prefix.append(MiniMessage.miniMessage().deserialize(message)));
            }
        }
    }

    private void broadcastMessage(Player player, BroadcastManager.MessageKey key, CooldownManager cooldownManager) {
        if (cooldownManager != null && cooldownManager.isOnCooldown(player)) {
            return;
        }

        String message = plugin.getConfig().getString(key.getKey(), "<gray><aqua>%player%</aqua> " +
                "performed an action.</gray>");
        message = message.replace("%player%", player.getName());

        broadcastMessage(player, message);

        if (cooldownManager != null) {
            cooldownManager.setCooldown(player);
        }
    }

    public void broadcastPlayerRepaired(Player player) {
        broadcastMessage(player, MessageKey.PLAYER_REPAIRED, repairBroadcastCooldown);
    }

    public void broadcastPlayerOpenedKitRoom(Player player) {
        broadcastMessage(player, MessageKey.PLAYER_OPENED_KIT_ROOM, kitroomBroadcastCooldown);
    }

    public void broadcastPlayerLoadedPrivateKit(Player player) {
        broadcastMessage(player, MessageKey.PLAYER_LOADED_PRIVATE_KIT, null);
    }

    public void broadcastPlayerLoadedPublicKit(Player player) {
        broadcastMessage(player, MessageKey.PLAYER_LOADED_PUBLIC_KIT, null);
    }

    public void broadcastPlayerLoadedEnderChest(Player player) {
        broadcastMessage(player, MessageKey.PLAYER_LOADED_ENDER_CHEST, null);
    }

    public void broadcastPlayerCopiedKit(Player player) {
        broadcastMessage(player, MessageKey.PLAYER_COPIED_KIT, null);
    }

    public void broadcastPlayerRegeared(Player player) {
        broadcastMessage(player, MessageKey.PLAYER_REGEARED, null);
    }
    public void startScheduledBroadcast() {
        List<Component> messages = new ArrayList<>();
        plugin.getConfig().getStringList("scheduled-broadcast.messages").forEach(message -> messages.add(generateBroadcastComponent(message)));

        int[] index = {0};

        if (plugin.getConfig().getBoolean("scheduled-broadcast.enabled")) {
            Bukkit.getScheduler().runTaskTimer(plugin, () -> {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    audience.player(player).sendMessage(messages.get(index[0]));
                }
                index[0] = (index[0] + 1) % messages.size();
            }, 0, plugin.getConfig().getInt("scheduled-broadcast.period") * 20L);
        }
    }

    public void sendComponentMessage(Player player, Component message) {
        audience.player(player).sendMessage(message);
    }

    public enum MessageKey {
        PLAYER_REPAIRED("messages.player-repaired"), PLAYER_OPENED_KIT_ROOM("messages.player-opened-kit-room"), PLAYER_LOADED_PRIVATE_KIT("messages.player-loaded-private-kit"), PLAYER_LOADED_PUBLIC_KIT("messages.player-loaded-public-kit"), PLAYER_LOADED_ENDER_CHEST("messages.player-loaded-enderchest"), PLAYER_COPIED_KIT("messages.player-copied-kit"), PLAYER_REGEARED("messages.player-regeared");

        private final String key;

        MessageKey(String key) {
            this.key = key;
        }

        public String getKey() {
            return key;
        }
    }
}
