package dev.noah.perplayerkit.util;

import dev.noah.perplayerkit.PerPlayerKit;
import dev.noah.perplayerkit.ConfigManager;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

/**
 * Manages playing configurable sounds for plugin feedback (success, error, UI
 * interactions).
 */
public class SoundManager {
    private static final String BASE_KEY = "sounds.";

    private static Sound getSound(String key, String defaultName) {
        String path = BASE_KEY + key;
        String soundName = ConfigManager.get().getSound(path, defaultName);
        try {
            return Sound.valueOf(soundName);
        } catch (IllegalArgumentException ex) {
            PerPlayerKit.getPlugin().getLogger()
                    .warning("Invalid sound '" + soundName + "' for config key '" + path + "'. Using default '"
                            + defaultName + "'.");
            return Sound.valueOf(defaultName);
        }
    }

    /**
     * Play a success sound (e.g. confirmation) to the player.
     */
    public static void playSuccess(Player player) {
        play(player, getSound("success", "ENTITY_PLAYER_LEVELUP"));
    }

    /**
     * Play a failure sound (e.g. error) to the player.
     */
    public static void playFailure(Player player) {
        play(player, getSound("failure", "ENTITY_ITEM_BREAK"));
    }

    /**
     * Play a generic UI click sound to the player.
     */
    public static void playClick(Player player) {
        play(player, getSound("click", "UI_BUTTON_CLICK"));
    }

    /**
     * Play a GUI open sound to the player.
     */
    public static void playOpenGui(Player player) {
        play(player, getSound("open_gui", "UI_BUTTON_CLICK"));
    }

    /**
     * Play a GUI close sound to the player.
     */
    public static void playCloseGui(Player player) {
        play(player, getSound("close_gui", "UI_BUTTON_CLICK"));
    }

    private static void play(Player player, Sound sound) {
        player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
    }
}
