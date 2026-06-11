package de.thecoolcraft11.hideAndSeek.listener.player;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.items.effects.death.DeathMessageService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class EnvironmentalDeathMessageListener implements Listener {
    private final HideAndSeek plugin;
    private final PlayerHitListener playerHitListener;
    private final DeathMessageService deathMessageService;

    public EnvironmentalDeathMessageListener(HideAndSeek plugin, PlayerHitListener playerHitListener, DeathMessageService deathMessageService) {
        this.plugin = plugin;
        this.playerHitListener = playerHitListener;
        this.deathMessageService = deathMessageService;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerDeath(PlayerDeathEvent event) {
        var victim = event.getEntity();
        PlayerHitListener.EnvironmentalDeathCause cause = playerHitListener.peekEnvironmentalDeathCause(victim.getUniqueId());
        if (cause == null) {
            return;
        }

        java.util.UUID attributorId = playerHitListener.peekEnvironmentalDeathAttributor(victim.getUniqueId());
        org.bukkit.entity.Player attributor = attributorId == null ? null : org.bukkit.Bukkit.getPlayer(attributorId);

        Component customMessage = (attributor != null)
                ? deathMessageService.getSeekerPerkDeathMessage(attributor, victim, cause)
                : deathMessageService.getEnvironmentalDeathMessage(victim, cause, victim);

        if (customMessage != null) {
            event.deathMessage(customMessage);
            return;
        }

        String key = switch (cause) {
            case CAMPING -> "listeners.environmental_death.camping";
            case WORLD_BORDER -> "listeners.environmental_death.world_border";
            case DROWNING -> "listeners.environmental_death.drowning";
            case FIRE -> "listeners.environmental_death.fire";
            case LAVA -> "listeners.environmental_death.lava";
            case SUFFOCATION -> "listeners.environmental_death.suffocation";
            case FREEZING -> "listeners.environmental_death.freezing";
            case HOT_FLOOR -> "listeners.environmental_death.hot_floor";
            case CONTACT -> "listeners.environmental_death.contact";
            case PERK_DEATH_ZONE -> "listeners.environmental_death.death_zone";
            case PERK_RELOCATE -> "listeners.environmental_death.relocate";
            default -> "listeners.environmental_death.generic";
        };
        Component fallback = Component.text(victim.getName(), NamedTextColor.GREEN)
                .append(plugin.tr(victim, key));
        event.deathMessage(fallback);
    }
}
