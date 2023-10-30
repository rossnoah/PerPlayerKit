package net.vanillapractice.perplayerkit;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

public class SaveOnServerClose {

    public static void saveAll(){



   /*     for (Map.Entry<String, ItemStack[]> entry : PerPlayerKit.data.entrySet()){

            PerPlayerKit.sqldata.saveMySQLKit(entry.getKey(),
                    Serializer.itemStackArrayToBase64(entry.getValue()));
    }


    */
        for (Player p: Bukkit.getOnlinePlayers()){
            KitManager.saveToSQL(p.getUniqueId());

        }
    }

}
