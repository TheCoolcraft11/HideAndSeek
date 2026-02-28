package de.thecoolcraft11.hideAndSeek.listener;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.items.HiderItems;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerPickupArrowEvent;

public class CrossbowTrackerListener implements Listener {
    private final HideAndSeek plugin;

    public CrossbowTrackerListener(HideAndSeek plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onProjectileShoot(EntityShootBowEvent event) {
        if (!(event.getEntity() instanceof Player shooter)) {
            return;
        }

        if (!HideAndSeek.getDataController().getHiders().contains(shooter.getUniqueId())) {
            return;
        }

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> HiderItems.ensureArrow(shooter), 1L);
    }

    @EventHandler
    public void onArrowHit(EntityDamageByEntityEvent event) {
        if (!(event.getDamager() instanceof Arrow arrow)) {
            return;
        }
        if (!(event.getEntity() instanceof Player hit)) {
            return;
        }
        if (!(arrow.getShooter() instanceof Player shooter)) {
            return;
        }

        if (!HideAndSeek.getDataController().getHiders().contains(shooter.getUniqueId())) {
            return;
        }
        if (!HideAndSeek.getDataController().getSeekers().contains(hit.getUniqueId())) {
            return;
        }

        HiderItems.onTrackerHit(shooter, plugin);
    }

    @EventHandler
    public void onArrowPickup(PlayerPickupArrowEvent event) {
        Player player = event.getPlayer();
        if (!HideAndSeek.getDataController().getHiders().contains(player.getUniqueId())) {
            return;
        }

        if (player.getInventory().contains(org.bukkit.Material.ARROW)) {
            event.setCancelled(true);
        }
    }
}
