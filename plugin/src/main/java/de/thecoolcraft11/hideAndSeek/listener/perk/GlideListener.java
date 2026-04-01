package de.thecoolcraft11.hideAndSeek.listener.perk;

import de.thecoolcraft11.hideAndSeek.perk.impl.seeker.ElytraRushPerk;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityToggleGlideEvent;

public class GlideListener implements Listener {

    @EventHandler
    public void onToggleGlide(EntityToggleGlideEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        if (ElytraRushPerk.hasNoFall(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }
}
