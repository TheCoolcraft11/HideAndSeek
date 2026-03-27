package de.thecoolcraft11.hideAndSeek.listener.player;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import io.papermc.paper.event.entity.EntityEquipmentChangedEvent;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public class HiderEquipmentChangeListener implements Listener {

    private final HideAndSeek plugin;

    public HiderEquipmentChangeListener(HideAndSeek plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityEquipmentChanged(EntityEquipmentChangedEvent event) {
        if (event.getEntity() instanceof Player player) {
            if (HideAndSeek.getDataController().getHiders().contains(player.getUniqueId())) {
                hideHandItem(player, EquipmentSlot.HAND);
                hideHandItem(player, EquipmentSlot.OFF_HAND);
                Bukkit.getScheduler().runTask(
                        plugin,
                        () -> {
                            hideHandItem(player, EquipmentSlot.HAND);
                            hideHandItem(player, EquipmentSlot.OFF_HAND);
                        }
                );
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();

        if (!HideAndSeek.getDataController().getHiders().contains(player.getUniqueId())) {
            return;
        }

        hideHandItem(player, EquipmentSlot.HAND);
        hideHandItem(player, EquipmentSlot.OFF_HAND);
        Bukkit.getScheduler().runTask(
                plugin,
                () -> {
                    hideHandItem(player, EquipmentSlot.HAND);
                    hideHandItem(player, EquipmentSlot.OFF_HAND);
                }
        );
    }


    public static void hideHandItem(Player target, EquipmentSlot slot) {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (viewer.equals(target)) continue;

            viewer.sendEquipmentChange(
                    target,
                    slot,
                    new ItemStack(Material.AIR)
            );
        }
    }

    public static void hideHelmet(Player target) {
        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (viewer.equals(target)) continue;

            viewer.sendEquipmentChange(
                    target,
                    EquipmentSlot.HEAD,
                    new ItemStack(Material.AIR)
            );
        }
    }


}
