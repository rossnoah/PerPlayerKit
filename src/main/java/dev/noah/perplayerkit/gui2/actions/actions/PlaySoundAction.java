/*
 * Copyright 2022-2025 Noah Ross
 */
package dev.noah.perplayerkit.gui2.actions.actions;

import dev.noah.perplayerkit.gui2.actions.ActionHandler;
import dev.noah.perplayerkit.gui2.data.DataContext;
import dev.noah.perplayerkit.util.SoundManager;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

/**
 * Action to play a sound to the player
 */
public class PlaySoundAction extends ActionHandler {
    private final String soundName;
    private final float volume;
    private final float pitch;
    
    public PlaySoundAction(String soundName, float volume, float pitch) {
        this.soundName = soundName;
        this.volume = volume;
        this.pitch = pitch;
    }
    
    public PlaySoundAction(String soundName) {
        this(soundName, 1.0f, 1.0f);
    }
    
    @Override
    public void execute(Player player, DataContext context, Object clickInfo) {
        String resolvedSound = context.resolve(soundName);
        
        try {
            Sound sound = Sound.valueOf(resolvedSound);
            player.playSound(player.getLocation(), sound, volume, pitch);
        } catch (IllegalArgumentException e) {
            // Invalid sound name, use SoundManager as fallback
            switch (resolvedSound.toLowerCase()) {
                case "click":
                    SoundManager.playClick(player);
                    break;
                case "success":
                    SoundManager.playSuccess(player);
                    break;
                case "failure":
                    SoundManager.playFailure(player);
                    break;
                case "open_gui":
                    SoundManager.playOpenGui(player);
                    break;
                case "close_gui":
                    SoundManager.playCloseGui(player);
                    break;
                default:
                    // Unknown sound, ignore
                    break;
            }
        }
    }
}