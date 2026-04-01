package de.thecoolcraft11.hideAndSeek.listener.perk;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.ItemStack;

public class PlaceholderItemProtectionListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        ItemStack item = event.getItemDrop().getItemStack();
        if (item.getType() == Material.LIGHT) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        ItemStack cursor = event.getCursor();
        ItemStack current = event.getCurrentItem();

        if (cursor.getType() == Material.LIGHT) {
            event.setCancelled(true);
        }
        if (current != null && current.getType() == Material.LIGHT) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        ItemStack cursor = event.getCursor();
        if (cursor != null && cursor.getType() == Material.LIGHT) {
            event.setCancelled(true);
        }

        for (ItemStack item : event.getNewItems().values()) {
            if (item != null && item.getType() == Material.LIGHT) {
                event.setCancelled(true);
                break;
            }
        }
    }
}


