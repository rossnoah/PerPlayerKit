package dev.noah.perplayerkit;

import dev.noah.perplayerkit.util.Serializer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.IOException;

public class KitRoomDataManager {

    private static final PerPlayerKit plugin = PerPlayerKit.getPlugin(PerPlayerKit.class);


    public static void setKitRoom(int page, ItemStack[] data) {

        PerPlayerKit.kitroomData.set(page, data);
        Filter.createWhitelist();


    }

    public static void saveToSQL() {
        new BukkitRunnable() {

            @Override
            public void run() {

                for (int i = 0; i < 5; i++) {
                    ItemStack[] pagedata = PerPlayerKit.kitroomData.get(i);
                    String output = Serializer.itemStackArrayToBase64(pagedata);
                    PerPlayerKit.dbManager.saveMySQLKit("kitroom" + i, output);
                }
            }

        }.runTaskAsynchronously(plugin);


    }

    public static void loadFromSQL() {


        for (int i = 0; i < 5; i++) {


            String input = PerPlayerKit.dbManager.getMySQLKit("kitroom" + i);
            if (!input.equalsIgnoreCase("error")) {
                try {
                    ItemStack[] pagedata = Serializer.itemStackArrayFromBase64(input);
                    PerPlayerKit.kitroomData.set(i, pagedata);
                    Filter.createWhitelist();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    public static void syncLoadFromSQL() {


        for (int i = 0; i < 5; i++) {


            String input = PerPlayerKit.dbManager.getMySQLKit("kitroom" + i);
            if (!input.equalsIgnoreCase("error")) {
                try {
                    ItemStack[] pagedata = Serializer.itemStackArrayFromBase64(input);
                    PerPlayerKit.kitroomData.set(i, pagedata);
                    Filter.createWhitelist();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }


    }

}







