package de.thecoolcraft11.hideAndSeek.phase;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.items.ItemSkinSelectionService;
import de.thecoolcraft11.hideAndSeek.items.effects.win.WinSkinService;
import de.thecoolcraft11.hideAndSeek.items.hider.RemoteGatewayItem;
import de.thecoolcraft11.hideAndSeek.items.seeker.CameraItem;
import de.thecoolcraft11.hideAndSeek.util.DataController;
import de.thecoolcraft11.hideAndSeek.util.PlayerStateResetUtil;
import de.thecoolcraft11.hideAndSeek.util.TimerManager;
import de.thecoolcraft11.minigameframework.FrameworkPlugin;
import de.thecoolcraft11.minigameframework.MinigameFramework;
import de.thecoolcraft11.minigameframework.game.GamePhase;
import de.thecoolcraft11.minigameframework.progression.GameResult;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jspecify.annotations.NonNull;

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
        RemoteGatewayItem.clearAllGateways();
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
        updateGlobalRoundStats(hideAndSeekPlugin, hidersWin, activeHiders);

        announceWinner(plugin, hidersWin, coinGains);

        WinSkinService winSkinService = new WinSkinService(hideAndSeekPlugin);

        if (hidersWin) {
            for (UUID hiderId : activeHiders) {
                Player hider = Bukkit.getPlayer(hiderId);
                if (hider != null && hider.isOnline()) {
                    winSkinService.triggerWinSkin(hider, true);
                }
            }
        } else {
            List<UUID> winningSeekers = new ArrayList<>(HideAndSeek.getDataController().getSeekers());
            winningSeekers.removeAll(DataController.getInstance().getAllHiders());
            for (UUID seekerId : winningSeekers) {
                Player seeker = Bukkit.getPlayer(seekerId);
                if (seeker != null && seeker.isOnline()) {
                    winSkinService.triggerWinSkin(seeker, false);
                }
            }
        }


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
            }.runTaskLater(plugin, 120L);
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
        RemoteGatewayItem.clearAllGateways();


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

    private static int getMaxPoints() {
        Map<UUID, Integer> allPoints = DataController.getInstance().getAllPoints();
        int max = 1;
        for (int points : allPoints.values()) {
            if (points > max) max = points;
        }
        return max;
    }

    private static @NonNull Map<UUID, Integer> getUuidIntegerMap(List<Map.Entry<UUID, Integer>> sortedPoints) {
        Map<UUID, Integer> placements = new HashMap<>();
        int currentPlacement = 1;
        int lastPoints = Integer.MIN_VALUE;
        for (int i = 0; i < sortedPoints.size(); i++) {
            int points = sortedPoints.get(i).getValue();
            UUID uuid = sortedPoints.get(i).getKey();
            if (points != lastPoints) {
                currentPlacement = i + 1;
                lastPoints = points;
            }
            placements.put(uuid, currentPlacement);
        }
        return placements;
    }

    private void updateGlobalRoundStats(HideAndSeek plugin, boolean hidersWin, List<UUID> activeHiders) {
        Set<UUID> winners = new HashSet<>();
        if (hidersWin) {
            winners.addAll(activeHiders);
        } else {
            winners.addAll(HideAndSeek.getDataController().getSeekers());
        }

        Set<UUID> participants = new HashSet<>();
        participants.addAll(HideAndSeek.getDataController().getHiders());
        participants.addAll(HideAndSeek.getDataController().getSeekers());

        int totalPlayers = participants.size();
        long roundMillis = DataController.getInstance().getRoundEndTime() - DataController.getInstance().getRoundStartTime();
        int durationSeconds = (int) (roundMillis / 100);

        List<Map.Entry<UUID, Integer>> sortedPoints = new ArrayList<>(
                HideAndSeek.getDataController().getAllPoints().entrySet());
        sortedPoints.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));

        Map<UUID, Integer> placements = getUuidIntegerMap(sortedPoints);

        for (UUID playerId : participants) {
            boolean winner = winners.contains(playerId);
            int placement = placements.getOrDefault(playerId, totalPlayers);
            int points = HideAndSeek.getDataController().getAllPoints().getOrDefault(playerId, 0);
            double performance = (double) points / Math.max(1, getMaxPoints());
            GameResult result = new GameResult(placement, totalPlayers, performance,
                    durationSeconds > 0 ? durationSeconds : 1);
            Player player = Bukkit.getPlayer(playerId);
            if (winner) {
                plugin.getPlayerDataStore().addWin(playerId)
                        .exceptionally(ex -> {
                            plugin.getLogger().warning("Failed to update win/xp stats for " + playerId + ": " + ex.getMessage());
                            return null;
                        });
            } else {
                plugin.getPlayerDataStore().addLoss(playerId)
                        .exceptionally(ex -> {
                            plugin.getLogger().warning("Failed to update loss stats for " + playerId + ": " + ex.getMessage());
                            return null;
                        });
            }
            if (player != null) {
                FrameworkPlugin.getLevelManager().processResult(player.getUniqueId(), result);
            }
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
