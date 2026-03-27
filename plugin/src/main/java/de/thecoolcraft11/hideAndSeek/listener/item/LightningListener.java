package de.thecoolcraft11.hideAndSeek.listener.item;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LightningStrike;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.entity.CreeperPowerEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.persistence.PersistentDataType;

public class LightningListener implements Listener {
    private final HideAndSeek plugin;

    public LightningListener(HideAndSeek plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockIgnite(BlockIgniteEvent event) {
        if (event.getCause() != BlockIgniteEvent.IgniteCause.LIGHTNING) return;

        if (event.getIgnitingEntity() instanceof LightningStrike lightning) {
            if (lightning.getPersistentDataContainer().has(new NamespacedKey(plugin, "freezeLightning"), PersistentDataType.BOOLEAN)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onCreeperPower(CreeperPowerEvent event) {
        if (event.getCause() != CreeperPowerEvent.PowerCause.LIGHTNING) return;

        if (event.getLightning() instanceof LightningStrike lightning) {
            if (lightning.getPersistentDataContainer().has(new NamespacedKey(plugin, "freezeLightning"), PersistentDataType.BOOLEAN)) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onLightningDamage(EntityDamageEvent event) {
        if (event.getCause() != EntityDamageEvent.DamageCause.LIGHTNING) return;
        if (event.getDamageSource().getCausingEntity() instanceof LightningStrike lightning) {
            if (lightning.getPersistentDataContainer().has(new NamespacedKey(plugin, "freezeLightning"), PersistentDataType.BOOLEAN)) {
                event.setDamage(0);
                event.getEntity().setFireTicks(0);
                event.setCancelled(true);
            }
        }
    }
}
