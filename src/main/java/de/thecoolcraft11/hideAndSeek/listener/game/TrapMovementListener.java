package de.thecoolcraft11.hideAndSeek.listener.game;

import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import de.thecoolcraft11.hideAndSeek.items.seeker.CageTrapItem;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class TrapMovementListener implements Listener {

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (!CageTrapItem.trappedPlayers.contains(event.getPlayer().getUniqueId())) return;

        if (event.getFrom().distanceSquared(event.getTo()) > 0) {
            Location newLoc = event.getFrom().clone();
            newLoc.setY(event.getTo().getY());
            event.setTo(newLoc);
        }
    }

    @EventHandler
    public void onJump(PlayerJumpEvent event) {
        if (!CageTrapItem.trappedPlayers.contains(event.getPlayer().getUniqueId())) return;

        event.setCancelled(true);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        CageTrapItem.trappedPlayers.remove(event.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        CageTrapItem.trappedPlayers.remove(event.getEntity().getUniqueId());
    }
}
