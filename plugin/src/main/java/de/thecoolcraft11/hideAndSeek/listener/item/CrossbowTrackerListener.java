package de.thecoolcraft11.hideAndSeek.listener.item;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.items.HiderItems;
import de.thecoolcraft11.hideAndSeek.items.ItemSkinSelectionService;
import de.thecoolcraft11.hideAndSeek.items.hider.TrackerCrossbowItem;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.player.PlayerPickupArrowEvent;
import org.bukkit.scheduler.BukkitRunnable;

import static de.thecoolcraft11.hideAndSeek.items.hider.TrackerCrossbowItem.onTrackerHit;

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

        if (event.getProjectile() instanceof Arrow arrow) {
            boolean laserTag = ItemSkinSelectionService.isSelected(shooter, TrackerCrossbowItem.ID, "skin_laser_tag");
            if (laserTag) {

                arrow.getWorld().spawnParticle(org.bukkit.Particle.ELECTRIC_SPARK, arrow.getLocation(), 24, 0.18, 0.18, 0.18, 0.05);
                arrow.getWorld().playSound(arrow.getLocation(), org.bukkit.Sound.BLOCK_BEACON_POWER_SELECT, 0.6f, 1.6f);

                new BukkitRunnable() {
                    int ticks = 0;

                    @Override
                    public void run() {
                        if (!arrow.isValid() || arrow.isInBlock() || ticks++ > 80) {
                            cancel();
                            return;
                        }
                        arrow.getWorld().spawnParticle(org.bukkit.Particle.END_ROD, arrow.getLocation(), 4, 0.05, 0.05, 0.05, 0.0);
                        arrow.getWorld().spawnParticle(org.bukkit.Particle.ELECTRIC_SPARK, arrow.getLocation(), 3, 0.03, 0.03, 0.03, 0.0);
                    }
                }.runTaskTimer(plugin, 1L, 1L);
            }
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

        onTrackerHit(shooter, plugin);
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
