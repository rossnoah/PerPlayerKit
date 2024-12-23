package dev.noah.perplayerkit;

import org.bukkit.entity.Player;

import java.util.ArrayList;
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
        List<PublicKit> originalList = KitManager.get().getPublicKitList();
        return new ArrayList<>(originalList);
    }

    public void loadPublicKit(Player player, PublicKit kit) {
        KitManager.get().loadPublicKitSilent(player, kit.id);
    }


}
