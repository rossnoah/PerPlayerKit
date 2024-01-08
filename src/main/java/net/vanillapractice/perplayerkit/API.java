package net.vanillapractice.perplayerkit;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class API {

    private static API instance;

    private API(){
        instance = this;
    }

    public static API getInstance(){
        if(instance==null){
            instance = new API();
        }
        return instance;
    }


    public List<PublicKit> getPublicKits(){

        List<PublicKit> publicKitList = new ArrayList<>();

        //generate list of public kits from the config
        PerPlayerKit.getPlugin().getConfig().getConfigurationSection("publickits").getKeys(false).forEach(key -> {
            String name = PerPlayerKit.getPlugin().getConfig().getString("publickits."+key+".name");
            Material icon = Material.valueOf(PerPlayerKit.getPlugin().getConfig().getString("publickits."+key+".icon"));
            PublicKit kit = new PublicKit(key,name,icon);
            publicKitList.add(kit);
        });

        return publicKitList;

    }

    public void loadPublicKit(Player player, PublicKit kit){
        KitManager.loadPublicKit(player,kit.id);
    }


}
