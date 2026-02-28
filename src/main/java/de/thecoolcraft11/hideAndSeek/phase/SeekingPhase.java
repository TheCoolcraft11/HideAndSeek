package de.thecoolcraft11.hideAndSeek.phase;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.items.HiderItems;
import de.thecoolcraft11.hideAndSeek.items.SeekerItems;
import de.thecoolcraft11.hideAndSeek.util.TimerManager;
import de.thecoolcraft11.minigameframework.MinigameFramework;
import de.thecoolcraft11.minigameframework.game.GamePhase;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class SeekingPhase implements GamePhase {
    private BukkitTask checkTask;
    private BukkitTask pointsTask;

    @Override
    public String getId() {
        return "seeking";
    }

    @Override
    public String getDisplayName() {
        return "Seeking";
    }

    @Override
    public void onStart(MinigameFramework plugin) {

        HideAndSeek hideAndSeekPlugin = (HideAndSeek) plugin;

        TimerManager.cleanupTimers(hideAndSeekPlugin);

        var seekingTimeResult = plugin.getSettingService().getSetting("game.seeking_time");
        Object seekingTimeObj = seekingTimeResult.isSuccess() ? seekingTimeResult.getValue() : 300;
        int timeRemaining = (seekingTimeObj instanceof Integer) ? (Integer) seekingTimeObj : 300;

        plugin.getLogger().info("Seeking phase started - " + timeRemaining + " seconds");


        var gameModeResult = plugin.getSettingService().getSetting("game.gametype");
        Object gameModeObj = gameModeResult.isSuccess() ? gameModeResult.getValue() : null;
        boolean isBlockMode = gameModeObj != null && gameModeObj.toString().equals("BLOCK");


        de.thecoolcraft11.hideAndSeek.util.GameModeEnum gameMode = (gameModeObj instanceof de.thecoolcraft11.hideAndSeek.util.GameModeEnum) ?
                (de.thecoolcraft11.hideAndSeek.util.GameModeEnum) gameModeObj : de.thecoolcraft11.hideAndSeek.util.GameModeEnum.NORMAL;
        double seekerSize = 1.0;
        if (gameMode == de.thecoolcraft11.hideAndSeek.util.GameModeEnum.SMALL) {
            var seekerSizeResult = plugin.getSettingService().getSetting("game.small_mode_seeker_size");
            Object seekerSizeObj = seekerSizeResult.isSuccess() ? seekerSizeResult.getValue() : 1.0;
            seekerSize = (seekerSizeObj instanceof Number) ? ((Number) seekerSizeObj).doubleValue() : 1.0;
        }


        for (UUID playerId : HideAndSeek.getDataController().getHiders()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline()) {
                player.setGameMode(GameMode.SURVIVAL);
                player.sendMessage(Component.text("Seekers are coming! Run and hide!", NamedTextColor.GREEN));


                player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                        org.bukkit.potion.PotionEffectType.SATURATION,
                        Integer.MAX_VALUE,
                        0,
                        false,
                        false,
                        false
                ));

                if (isBlockMode) {
                    player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                            org.bukkit.potion.PotionEffectType.INVISIBILITY,
                            Integer.MAX_VALUE,
                            0,
                            false,
                            false,
                            false
                    ));
                }


                HiderItems.giveItems(player, (HideAndSeek) plugin, false);
            }
        }

        for (UUID playerId : HideAndSeek.getDataController().getSeekers()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline()) {
                player.setGameMode(GameMode.SURVIVAL);
                player.sendMessage(Component.text("Go find the hiders!", NamedTextColor.RED));


                player.removePotionEffect(org.bukkit.potion.PotionEffectType.BLINDNESS);
                player.removePotionEffect(org.bukkit.potion.PotionEffectType.SLOWNESS);


                player.getInventory().setHelmet(null);


                player.setWalkSpeed(0.2f);

                player.addPotionEffect(new org.bukkit.potion.PotionEffect(
                        org.bukkit.potion.PotionEffectType.SATURATION,
                        Integer.MAX_VALUE,
                        0,
                        false,
                        false,
                        false
                ));


                if (gameMode == de.thecoolcraft11.hideAndSeek.util.GameModeEnum.SMALL && seekerSize != 1.0) {
                    var scaleAttribute = player.getAttribute(org.bukkit.attribute.Attribute.SCALE);
                    if (scaleAttribute != null) {
                        scaleAttribute.setBaseValue(seekerSize);
                    }
                }

                SeekerItems.giveItems(player, (HideAndSeek) plugin);
            }
        }

        startCountdown(plugin);

        startWinConditionCheck(plugin);

        startHiderPointsTask(plugin);
    }

    @Override
    public void onEnd(MinigameFramework plugin) {

        HideAndSeek hideAndSeekPlugin = (HideAndSeek) plugin;
        if (checkTask != null) {
            checkTask.cancel();
        }
        if (pointsTask != null) {
            pointsTask.cancel();
        }

        for (UUID hiderId : HideAndSeek.getDataController().getHiders()) {
            Player hider = Bukkit.getPlayer(hiderId);
            if (hider != null && hider.isOnline()) {
                hider.customName(null);
                hider.setCustomNameVisible(false);
            }
        }

        ScoreboardManager scoreboardManager = Bukkit.getScoreboardManager();
        Team dummyTeam = scoreboardManager.getMainScoreboard().getTeam("hiders_display");
        if (dummyTeam != null) dummyTeam.unregister();

        HiderItems.removeFromAllPlayers();
        SeekerItems.removeFromAllPlayers();

        plugin.getLogger().info("Seeking phase ended");

        TimerManager.cleanupTimer(hideAndSeekPlugin, "Seeking");
    }

    @Override
    public List<String> getAllowedTransitions() {
        return List.of("ended");
    }

    private void startCountdown(MinigameFramework plugin) {
        HideAndSeek hideAndSeekPlugin = (HideAndSeek) plugin;
        TimerManager.startSeekingTimer(hideAndSeekPlugin);
    }

    private void startWinConditionCheck(MinigameFramework plugin) {
        checkTask = new BukkitRunnable() {
            @Override
            public void run() {
                checkWinConditions(plugin);
            }
        }.runTaskTimer(plugin, 20L, 20L);
    }

    private void checkWinConditions(MinigameFramework plugin) {
        List<UUID> activeHiders = new ArrayList<>();


        for (UUID hiderId : HideAndSeek.getDataController().getHiders()) {
            Player hider = Bukkit.getPlayer(hiderId);
            if (hider != null && hider.isOnline() && hider.getGameMode() != GameMode.SPECTATOR) {
                activeHiders.add(hiderId);
            }
        }


        if (activeHiders.isEmpty()) {

            plugin.getStateManager().setPhase("ended");
        }
    }

    private String formatTime(int seconds) {
        int minutes = seconds / 60;
        int secs = seconds % 60;
        return String.format("%d:%02d", minutes, secs);
    }

    @Override
    public boolean allowBlockBreak() {
        return false;
    }

    @Override
    public boolean allowBlockPlace() {
        return false;
    }

    private void startHiderPointsTask(MinigameFramework plugin) {
        var hiderPointsResult = plugin.getSettingService().getSetting("game.hider-points");
        Object hiderPointsObj = hiderPointsResult.isSuccess() ? hiderPointsResult.getValue() : 1;
        int hiderPoints = (hiderPointsObj instanceof Integer) ? (Integer) hiderPointsObj : 1;

        pointsTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (UUID hiderId : HideAndSeek.getDataController().getHiders()) {
                    Player hider = Bukkit.getPlayer(hiderId);
                    if (hider != null && hider.isOnline() && hider.getGameMode() != GameMode.SPECTATOR) {
                        HideAndSeek.getDataController().addPoints(hiderId, hiderPoints);
                    }
                }
            }
        }.runTaskTimer(plugin, 100L, 100L);
    }

    @Override
    public boolean canBreakBlock(Block block, Player player, MinigameFramework plugin) {
        HideAndSeek hideAndSeekPlugin = (HideAndSeek) plugin;
        Material blockType = block.getType();
        if (HideAndSeek.getDataController().getSeekers().contains(player.getUniqueId())) {
            if (hideAndSeekPlugin.getCustomItemManager().hasItemInMainHand(player, SeekerItems.SEEKERS_SWORD_ID)) {
                List<String> blockBreaks = hideAndSeekPlugin.getConfig().getStringList("seeker-break-blocks");
                return blockBreaks.contains(blockType.name());
            }
        }
        return GamePhase.super.canBreakBlock(block, player, plugin);
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
    public List<Material> getBlockInteractionExceptions() {
        return new ArrayList<>(
                Arrays.stream(Material.values())
                        .filter(material -> material.name().endsWith("_DOOR") || material.name().endsWith("_FENCE_GATE") || material.name().endsWith("_TRAPDOOR"))
                        .toList()
        );
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
    public List<Material> getBlockPhysicsExceptions() {
        return new ArrayList<>(
                Arrays.stream(Material.values())
                        .filter(material -> material.name().endsWith("_DOOR") || material.name().endsWith("_FENCE_GATE") || material.name().endsWith("_TRAPDOOR"))
                        .toList()
        );
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
}
