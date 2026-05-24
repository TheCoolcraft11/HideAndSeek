package de.thecoolcraft11.hideAndSeek.listener.player;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.gui.SpectatorTeleportGUI;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;

public class SpectatorTeleportListener implements Listener {

    private final HideAndSeek plugin;

    public SpectatorTeleportListener(HideAndSeek plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (SpectatorTeleportGUI.isGuiItem(plugin, event.getItemDrop().getItemStack())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        if (SpectatorTeleportGUI.isGuiItem(plugin, event.getCursor())) {
            event.setCancelled(true);
            return;
        }
        for (ItemStack item : event.getNewItems().values()) {
            if (SpectatorTeleportGUI.isGuiItem(plugin, item)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onSwapHandItems(PlayerSwapHandItemsEvent event) {
        if (SpectatorTeleportGUI.isGuiItem(plugin, event.getMainHandItem())
                || SpectatorTeleportGUI.isGuiItem(plugin, event.getOffHandItem())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();


        if (SpectatorTeleportGUI.isSpectatorWithGui(player.getUniqueId())) {
            SpectatorTeleportGUI.removeOnQuit(plugin, player);
        }
    }
}