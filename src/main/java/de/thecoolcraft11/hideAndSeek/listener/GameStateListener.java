package de.thecoolcraft11.hideAndSeek.listener;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.GameMode;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Objects;

public class GameStateListener implements Listener {
    private final HideAndSeek plugin;

    public GameStateListener(HideAndSeek plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String currentPhase = plugin.getStateManager().getCurrentPhaseId();


        if (currentPhase.equals("seeking") || currentPhase.equals("hiding")) {

            player.setGameMode(GameMode.SPECTATOR);
            player.sendMessage(Component.text("A game is in progress. You're spectating.", NamedTextColor.YELLOW));
        } else {

            player.setGameMode(GameMode.SURVIVAL);
            Objects.requireNonNull(player.getAttribute(Attribute.MAX_HEALTH)).setBaseValue(20.0);
            player.setHealth(20.0);
            player.setFoodLevel(20);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();


        if (HideAndSeek.getDataController().getHiders().contains(player.getUniqueId())) {
            HideAndSeek.getDataController().removeHider(player.getUniqueId());
            plugin.getLogger().info(player.getName() + " (hider) left the game");
        }

        if (HideAndSeek.getDataController().getSeekers().contains(player.getUniqueId())) {
            HideAndSeek.getDataController().removeSeeker(player.getUniqueId());
            plugin.getLogger().info(player.getName() + " (seeker) left the game");
        }
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        String currentPhase = plugin.getStateManager().getCurrentPhaseId();


        if (currentPhase.equals("hiding") && HideAndSeek.getDataController().getSeekers().contains(player.getUniqueId())) {

            if (event.getFrom().getX() != event.getTo().getX() || event.getFrom().getZ() != event.getTo().getZ()) {

                event.getTo().setX(event.getFrom().getX());
                event.getTo().setZ(event.getFrom().getZ());
            }
        }
    }
}

