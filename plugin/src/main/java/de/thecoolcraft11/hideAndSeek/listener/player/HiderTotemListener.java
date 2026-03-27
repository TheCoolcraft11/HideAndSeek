package de.thecoolcraft11.hideAndSeek.listener.player;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityResurrectEvent;

public class HiderTotemListener implements Listener {


    @EventHandler(ignoreCancelled = true)
    public void onResurrect(EntityResurrectEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        if (!HideAndSeek.getDataController().getHiders().contains(player.getUniqueId())) {
            return;
        }


        event.setCancelled(true);
    }
}

