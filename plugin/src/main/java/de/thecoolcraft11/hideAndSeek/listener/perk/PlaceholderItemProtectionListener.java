package de.thecoolcraft11.hideAndSeek.listener.perk;

import de.thecoolcraft11.hideAndSeek.perk.PerkShopUI;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;

public class PlaceholderItemProtectionListener implements Listener {

    private final PerkShopUI perkShopUI;

    public PlaceholderItemProtectionListener(PerkShopUI perkShopUI) {
        this.perkShopUI = perkShopUI;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        ItemStack item = event.getItemDrop().getItemStack();
        if (perkShopUI.isProtectedShopLight(item)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        ItemStack cursor = event.getCursor();
        ItemStack current = event.getCurrentItem();

        if (perkShopUI.isProtectedShopLight(cursor)) {
            event.setCancelled(true);
        }
        if (perkShopUI.isProtectedShopLight(current)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        ItemStack cursor = event.getCursor();
        if (perkShopUI.isProtectedShopLight(cursor)) {
            event.setCancelled(true);
        }

        for (ItemStack item : event.getNewItems().values()) {
            if (perkShopUI.isProtectedShopLight(item)) {
                event.setCancelled(true);
                break;
            }
        }
    }
}
