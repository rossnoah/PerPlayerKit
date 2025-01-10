package dev.noah.perplayerkit.listeners.features;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

public class OldDeathDropListener implements Listener {

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        Location loc = e.getEntity().getLocation();
        World world = loc.getWorld();
        for(ItemStack item : e.getDrops()){
            world.dropItem(loc, item);
        }
        e.getDrops().clear();
    }

}
