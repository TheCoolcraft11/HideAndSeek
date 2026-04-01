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
        if (cause == null) {
            return;
        }

        String message = switch (cause) {
            case CAMPING -> event.getEntity().getName() + " was struck down for camping too long.";
            case WORLD_BORDER -> event.getEntity().getName() + " was consumed by the world border.";
            case PERK_DEATH_ZONE -> event.getEntity().getName() + " failed to escape the Death Zone.";
            case PERK_RELOCATE -> event.getEntity().getName() + " did not relocate in time.";
            default -> event.getEntity().getName() + " was eliminated by the environment.";
        };
        event.deathMessage(Component.text(message));
    }
}
