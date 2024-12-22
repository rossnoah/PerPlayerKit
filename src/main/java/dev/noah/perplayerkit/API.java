package dev.noah.perplayerkit;

import org.bukkit.entity.Player;

import java.util.List;

public class API {

    private static API instance;

    private API() {
        instance = this;
    }

    public static API getInstance() {
        if (instance == null) {
            instance = new API();
        }
        return instance;
    }


    public List<PublicKit> getPublicKits() {
        return PerPlayerKit.publicKitList;

    }

    public void loadPublicKit(Player player, PublicKit kit) {
        KitManager.loadPublicKitSilent(player, kit.id);
    }


}
