package net.vanillapractice.perplayerkit;

import org.bukkit.entity.Player;

import java.util.List;

import static net.vanillapractice.perplayerkit.PerPlayerKit.publicKitList;

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


        return publicKitList;

    }

    public void loadPublicKit(Player player, PublicKit kit) {
        KitManager.loadPublicKitSilent(player, kit.id);
    }


}
