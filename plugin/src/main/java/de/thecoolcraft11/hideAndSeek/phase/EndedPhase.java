package de.thecoolcraft11.hideAndSeek.phase;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.items.ItemSkinSelectionService;
import de.thecoolcraft11.hideAndSeek.items.seeker.CameraItem;
import de.thecoolcraft11.hideAndSeek.util.PlayerStateResetUtil;
import de.thecoolcraft11.hideAndSeek.util.TimerManager;
import de.thecoolcraft11.minigameframework.MinigameFramework;
import de.thecoolcraft11.minigameframework.game.GamePhase;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Duration;
import java.util.*;

public class EndedPhase implements GamePhase {
    @Override
    public String getId() {
        return "ended";
    }

    @Override
    public String getDisplayName() {
        return "Ended";
    }

    @Override
    public void onStart(MinigameFramework plugin) {
        HideAndSeek hideAndSeekPlugin = (HideAndSeek) plugin;

        hideAndSeekPlugin.getAntiCheatVisibilityListener().resetNow();
        CameraItem.clearAllCameraState(hideAndSeekPlugin);
        if (hideAndSeekPlugin.getDebugSettings().isVerboseLoggingEnabled()) {
            plugin.getLogger().info("Game ended");
        }

        TimerManager.cleanupTimers(hideAndSeekPlugin);

        List<UUID> activeHiders = new ArrayList<>();
        for (UUID hiderId : HideAndSeek.getDataController().getHiders()) {
            Player hider = Bukkit.getPlayer(hiderId);
            if (hider != null && hider.isOnline() && hider.getGameMode() != GameMode.SPECTATOR) {
                activeHiders.add(hiderId);
            }
        }

        hideAndSeekPlugin.getPointService().awardRoundEndBonuses(activeHiders);

        int pointsPerCoin = Math.max(1, plugin.getSettingRegistry().get("skin-shop.points-per-coin", 50));
        Map<UUID, Integer> coinGains = new HashMap<>();
        for (Map.Entry<UUID, Integer> entry : HideAndSeek.getDataController().getAllPoints().entrySet()) {
            int points = Math.max(0, entry.getValue());
            int gainedCoins = points / pointsPerCoin;
            if (gainedCoins > 0) {
                ItemSkinSelectionService.addCoins(hideAndSeekPlugin, entry.getKey(), gainedCoins);
            }
            coinGains.put(entry.getKey(), gainedCoins);
        }

        boolean hidersWin = !activeHiders.isEmpty();

        announceWinner(plugin, hidersWin, coinGains);


        boolean autoCleanup = plugin.getSettingRegistry().get("game.round.auto-cleanup", true);

        if (autoCleanup) {

            new BukkitRunnable() {
                @Override
                public void run() {
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        String lobbyWorldName = hideAndSeekPlugin.getMapManager().getLobbyWorld();
                        org.bukkit.World lobbyWorld = Bukkit.getWorld(lobbyWorldName);
                        if (lobbyWorld != null) {
                            org.bukkit.Location lobbySpawn = lobbyWorld.getSpawnLocation();
                            player.teleport(lobbySpawn);
                        }
                    }


                    String currentMapName = HideAndSeek.getDataController().getCurrentMapName();
                    if (currentMapName != null && !currentMapName.isEmpty()) {
                        hideAndSeekPlugin.getMapManager().deleteWorkingWorld(currentMapName);
                        HideAndSeek.getDataController().setCurrentMapName(null, false);
                    }


                    plugin.getStateManager().setPhase("lobby", true);
                }
            }.runTaskLater(plugin, 60L);
        } else {

            new BukkitRunnable() {
                @Override
                public void run() {
                    plugin.getStateManager().setPhase("lobby", true);
                }
            }.runTaskLater(plugin, 200L);
        }
    }

    @Override
    public void onEnd(MinigameFramework plugin) {


        HideAndSeek hideAndSeekPlugin = (HideAndSeek) plugin;
        String currentMapName = HideAndSeek.getDataController().getCurrentMapName();
        if (currentMapName != null && !currentMapName.isEmpty()) {
            hideAndSeekPlugin.getMapManager().deleteWorkingWorld(currentMapName);
            HideAndSeek.getDataController().setCurrentMapName(null, false);
        }

        for (Player player : Bukkit.getOnlinePlayers()) {

            org.bukkit.entity.BlockDisplay display = HideAndSeek.getDataController().getBlockDisplay(player.getUniqueId());
            if (display != null && display.isValid()) {
                display.remove();
            }


            org.bukkit.entity.Entity sittingEntity = HideAndSeek.getDataController().getSittingEntity(player.getUniqueId());
            if (sittingEntity != null && sittingEntity.isValid()) {

                if (player.isInsideVehicle() && Objects.equals(player.getVehicle(), sittingEntity)) {
                    player.leaveVehicle();
                }
                sittingEntity.remove();
            }


            org.bukkit.Location lastLoc = HideAndSeek.getDataController().getLastLocation(player.getUniqueId());
            if (lastLoc != null) {
                org.bukkit.block.Block block = lastLoc.getBlock();
                org.bukkit.Material chosenBlock = HideAndSeek.getDataController().getChosenBlock(player.getUniqueId());
                if (chosenBlock != null && block.getType() == chosenBlock) {
                    block.setType(org.bukkit.Material.AIR);
                }
            }
        }


        HideAndSeek.getDataController().reset();


        for (Player player : Bukkit.getOnlinePlayers()) {
            PlayerStateResetUtil.resetPlayerCompletely(player, true);

            var maxHealth = player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
            if (maxHealth != null) {
                maxHealth.setBaseValue(20.0);
            }

            player.removePotionEffect(org.bukkit.potion.PotionEffectType.INVISIBILITY);

            player.removePotionEffect(org.bukkit.potion.PotionEffectType.SLOWNESS);
        }
    }

    @Override
    public List<String> getAllowedTransitions() {
        return List.of("lobby");
    }

    private void announceWinner(MinigameFramework plugin, boolean hidersWin, Map<UUID, Integer> coinGains) {
        Component winnerTitle;
        Component winnerSubtitle;
        NamedTextColor color;

        if (hidersWin) {
            winnerTitle = Component.text("HIDERS WIN!", NamedTextColor.GREEN, TextDecoration.BOLD);
            winnerSubtitle = Component.text("They survived the seekers!", NamedTextColor.YELLOW);
            color = NamedTextColor.GREEN;
        } else {
            winnerTitle = Component.text("SEEKERS WIN!", NamedTextColor.RED, TextDecoration.BOLD);
            winnerSubtitle = Component.text("They found all the hiders!", NamedTextColor.YELLOW);
            color = NamedTextColor.RED;
        }

        Title title = Title.title(
                winnerTitle,
                winnerSubtitle,
                Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(5), Duration.ofSeconds(1))
        );


        for (Player player : Bukkit.getOnlinePlayers()) {
            player.showTitle(title);
            player.sendMessage(Component.empty());
            player.sendMessage(Component.text("═══════════════════════════════", color));
            player.sendMessage(winnerTitle);
            player.sendMessage(Component.text("═══════════════════════════════", color));
            player.sendMessage(Component.empty());


            player.sendMessage(Component.text("POINTS:", NamedTextColor.GOLD, TextDecoration.BOLD));
            HideAndSeek.getDataController().getAllPoints().entrySet().stream()
                    .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                    .forEach(entry -> {
                        Player scoredPlayer = Bukkit.getPlayer(entry.getKey());
                        if (scoredPlayer != null) {
                            player.sendMessage(Component.text(scoredPlayer.getName() + ": " + entry.getValue() + " points", NamedTextColor.YELLOW));
                        }
                    });
            int gainedCoins = coinGains.getOrDefault(player.getUniqueId(), 0);
            int totalCoins = ItemSkinSelectionService.getCoins(player.getUniqueId());
            player.sendMessage(Component.text("COINS:", NamedTextColor.GOLD, TextDecoration.BOLD));
            player.sendMessage(Component.text("+" + gainedCoins + " coins this round", NamedTextColor.YELLOW));
            player.sendMessage(Component.text("Balance: " + totalCoins + " coins", NamedTextColor.AQUA));
            player.sendMessage(Component.empty());
        }

        HideAndSeek hideAndSeekPlugin = (HideAndSeek) plugin;
        if (hideAndSeekPlugin.getDebugSettings().isVerboseLoggingEnabled()) {
            plugin.getLogger().info((hidersWin ? "Hiders" : "Seekers") + " won the game!");
        }
    }

    @Override
    public boolean allowDamage() {
        return false;
    }

    @Override
    public boolean allowBlockBreak() {
        return false;
    }

    @Override
    public boolean allowBlockPlace() {
        return false;
    }

    @Override
    public boolean allowBlockInteraction() {
        return false;
    }

    @Override
    public boolean allowEntityInteraction() {
        return false;
    }

    @Override
    public boolean allowBlockDetection() {
        return false;
    }

    @Override
    public boolean allowEntityDetection() {
        return false;
    }

    @Override
    public boolean allowBlockPhysics() {
        return false;
    }

    @Override
    public boolean allowEntityChangeBlock() {
        return false;
    }

    @Override
    public boolean allowBlockExplosions() {
        return false;
    }

    @Override
    public boolean allowEntityExplosions() {
        return false;
    }

    @Override
    public boolean allowBlockDrops() {
        return false;
    }

    @Override
    public boolean allowBlockExperienceDrop() {
        return false;
    }

    @Override
    public boolean allowEntityDrops() {
        return false;
    }

    @Override
    public boolean allowEntityExperienceDrop() {
        return false;
    }

    @Override
    public boolean allowHunger() {
        return false;
    }

    @Override
    public boolean allowEntityPortals() {
        return false;
    }
}
