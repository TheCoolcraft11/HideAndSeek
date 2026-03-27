package de.thecoolcraft11.hideAndSeek.listener.player;

import net.kyori.adventure.text.Component;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class EnvironmentalDeathMessageListener implements Listener {
    private final PlayerHitListener playerHitListener;

    public EnvironmentalDeathMessageListener(PlayerHitListener playerHitListener) {
        this.playerHitListener = playerHitListener;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        PlayerHitListener.EnvironmentalDeathCause cause = playerHitListener.peekEnvironmentalDeathCause(event.getEntity().getUniqueId());
        if (cause != PlayerHitListener.EnvironmentalDeathCause.CAMPING) {
            return;
        }

        event.deathMessage(Component.text(event.getEntity().getName() + " was struck down for camping too long."));
    }
}
