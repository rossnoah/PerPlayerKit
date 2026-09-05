package dev.noah.perplayerkit.commands.core;

import dev.noah.perplayerkit.KitManager;
import dev.noah.perplayerkit.util.LocationAccess;
import dev.noah.perplayerkit.util.LocationFeature;
import dev.noah.perplayerkit.util.Lang;
import dev.noah.perplayerkit.util.SoundManager;
import org.bukkit.entity.Player;

/** Player actions use the same policy whether invoked by a command, menu, or special item. */
public final class ActionGuards {
    private ActionGuards() {}

    public static boolean allowed(Player player, String permission) {
        return allowed(player, permission, LocationFeature.forPermission(permission));
    }

    public static boolean allowed(Player player, String permission, LocationFeature feature) {
        if (!player.hasPermission(permission)) {
            Lang.get().send(player, "error.no-permission");
            SoundManager.playFailure(player);
            return false;
        }
        return feature == null || LocationAccess.get().require(player, feature);
    }

    public static boolean dataReady(Player player) {
        if (!KitManager.isPlayerDataLoading(player.getUniqueId())) return true;
        Lang.get().send(player, "error.kit-data-loading");
        return false;
    }
}
