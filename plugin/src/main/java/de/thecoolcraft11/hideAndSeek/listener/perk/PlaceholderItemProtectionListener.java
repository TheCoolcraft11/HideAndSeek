package de.thecoolcraft11.hideAndSeek.listener.perk;

import de.thecoolcraft11.hideAndSeek.gui.PerkShopGUI;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;

public class PlaceholderItemProtectionListener implements Listener {

    private final PerkShopGUI perkShopGUI;

    public PlaceholderItemProtectionListener(PerkShopGUI perkShopGUI) {
        this.perkShopGUI = perkShopGUI;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        ItemStack item = event.getItemDrop().getItemStack();
        if (perkShopGUI.isProtectedShopLight(item)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        ItemStack cursor = event.getCursor();
        ItemStack current = event.getCurrentItem();

        if (perkShopGUI.isProtectedShopLight(cursor)) {
            event.setCancelled(true);
        }
        if (perkShopGUI.isProtectedShopLight(current)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        ItemStack cursor = event.getCursor();
        if (perkShopGUI.isProtectedShopLight(cursor)) {
            event.setCancelled(true);
        }

        for (ItemStack item : event.getNewItems().values()) {
            if (perkShopGUI.isProtectedShopLight(item)) {
                event.setCancelled(true);
                break;
            }
        }
    }
}
