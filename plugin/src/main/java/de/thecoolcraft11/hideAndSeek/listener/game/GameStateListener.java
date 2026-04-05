package de.thecoolcraft11.hideAndSeek.listener.game;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.items.ItemSkinSelectionService;
import de.thecoolcraft11.hideAndSeek.util.PlayerStateResetUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import static de.thecoolcraft11.hideAndSeek.items.hider.MedkitItem.cleanupMedkitCharge;
import static de.thecoolcraft11.hideAndSeek.items.seeker.SeekersSwordItem.cleanupSwordCharge;

public class GameStateListener implements Listener {
    private final HideAndSeek plugin;

    public GameStateListener(HideAndSeek plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        ItemSkinSelectionService.loadPlayer(plugin, player.getUniqueId());
        String currentPhase = plugin.getStateManager().getCurrentPhaseId();

        plugin.getSeekingBossBarService().onPlayerJoin(player);

        if (currentPhase.equals("seeking") || currentPhase.equals("hiding")) {

            PlayerStateResetUtil.resetPlayerForSpectator(player, false);
            player.sendMessage(Component.text("A game is in progress. You're spectating.", NamedTextColor.YELLOW));
            teleportNextTick(player, resolveIngameJoinSpawn());
        } else {

            PlayerStateResetUtil.resetPlayerCompletely(player, false);
            plugin.getVoteManager().setReady(player.getUniqueId(), false);
            teleportNextTick(player, resolveLobbySpawn());
            scheduleLobbyStartRecheck();
        }
    }

    private void teleportNextTick(Player player, Location target) {
        if (target == null) {
            return;
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            if (!player.isOnline()) {
                return;
            }
            player.teleport(target);
        });
    }

    private Location resolveIngameJoinSpawn() {
        Location roundSpawn = HideAndSeek.getDataController().getRoundSpawnPoint();
        if (roundSpawn != null && roundSpawn.getWorld() != null) {
            return roundSpawn.clone();
        }

        String currentMapName = HideAndSeek.getDataController().getCurrentMapName();
        if (currentMapName != null && !currentMapName.isBlank()) {
            World workingWorld = Bukkit.getWorld("has_" + currentMapName);
            if (workingWorld != null) {
                return workingWorld.getSpawnLocation();
            }
        }

        return resolveLobbySpawn();
    }

    private Location resolveLobbySpawn() {
        String lobbyWorldName = plugin.getMapManager().getLobbyWorld();
        World lobbyWorld = Bukkit.getWorld(lobbyWorldName);
        if (lobbyWorld != null) {
            return lobbyWorld.getSpawnLocation();
        }
        return null;
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        ItemSkinSelectionService.savePlayer(plugin, player.getUniqueId());
        cleanupSwordCharge(player.getUniqueId());
        cleanupMedkitCharge(player.getUniqueId());
        HideAndSeek.getDataController().removeAllowedSpectator(player.getUniqueId());
        plugin.getVoteManager().clearVotes(player.getUniqueId());

        plugin.getSeekingBossBarService().onPlayerQuit();

        if (HideAndSeek.getDataController().getHiders().contains(player.getUniqueId())) {
            HideAndSeek.getDataController().removeHider(player.getUniqueId());
            if (plugin.getDebugSettings().isVerboseLoggingEnabled()) {
                plugin.getLogger().info(player.getName() + " (hider) left the game");
            }
        }

        if (HideAndSeek.getDataController().getSeekers().contains(player.getUniqueId())) {
            HideAndSeek.getDataController().removeSeeker(player.getUniqueId());
            if (plugin.getDebugSettings().isVerboseLoggingEnabled()) {
                plugin.getLogger().info(player.getName() + " (seeker) left the game");
            }
        }

        scheduleLobbyStartRecheck();
    }

    private void scheduleLobbyStartRecheck() {
        Bukkit.getScheduler().runTask(plugin, () -> plugin.getVoteManager().tryAutoStartIfEveryoneReady());
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
