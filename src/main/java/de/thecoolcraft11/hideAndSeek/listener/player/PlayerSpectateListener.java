package de.thecoolcraft11.hideAndSeek.listener.player;

import com.destroystokyo.paper.event.player.PlayerStartSpectatingEntityEvent;
import de.thecoolcraft11.hideAndSeek.util.DataController;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;

public class PlayerSpectateListener implements Listener {

    @EventHandler
    public void onStartSpectate(PlayerStartSpectatingEntityEvent event) {
        if (DataController.getInstance().getHiders().contains(event.getPlayer().getUniqueId())) {
            event.setCancelled(true);
            event.getPlayer().setSpectatorTarget(null);
        }
    }

    @EventHandler
    public void onTeleport(PlayerTeleportEvent event) {
        if (DataController.getInstance().getHiders().contains(event.getPlayer().getUniqueId())) {
            if (event.getCause() == PlayerTeleportEvent.TeleportCause.SPECTATE) {
                event.setCancelled(true);
            }
        }
    }
}
