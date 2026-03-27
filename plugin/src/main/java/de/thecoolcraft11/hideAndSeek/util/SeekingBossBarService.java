package de.thecoolcraft11.hideAndSeek.util;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.model.SeekingBossBarColorMode;
import de.thecoolcraft11.hideAndSeek.model.SeekingBossBarNameLayout;
import de.thecoolcraft11.hideAndSeek.model.SeekingBossBarProgressMode;
import de.thecoolcraft11.hideAndSeek.model.SeekingBossBarStaticColor;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public class SeekingBossBarService {
    private static final String GLOBAL_BAR_ID = "seeking_round_status";
    private static final long LIVE_UPDATE_PERIOD_TICKS = 10L;

    private final HideAndSeek plugin;
    private BukkitTask liveUpdateTask;
    private BukkitTask deathAnimationTask;
    private int maxHidersAtSeekingStart = 1;

    public SeekingBossBarService(HideAndSeek plugin) {
        this.plugin = plugin;
    }

    public void startSeekingSession() {
        stopSeekingSession();
        if (isDisabled()) {
            return;
        }

        maxHidersAtSeekingStart = Math.max(1, countHidersLeft());
        plugin.getTeamBossBarManager().createGlobalBossBar(
                GLOBAL_BAR_ID,
                buildTitle(countHidersLeft(), countSeekers()),
                resolveConfiguredColor(countHidersLeft()),
                BarStyle.SOLID
        );

        refreshNow();
        startLiveUpdater();
    }

    public void stopSeekingSession() {
        cancelLiveUpdateTask();
        cancelDeathAnimationTask();
        plugin.getTeamBossBarManager().removeGlobalBossBar(GLOBAL_BAR_ID);
    }

    public void reloadSettings() {
        if (isNotSeekingPhase()) {
            stopSeekingSession();
            return;
        }

        if (isDisabled()) {
            stopSeekingSession();
            return;
        }

        if (!plugin.getTeamBossBarManager().hasGlobalBossBar(GLOBAL_BAR_ID)) {
            startSeekingSession();
            return;
        }

        refreshNow();
    }

    public void refreshNow() {
        if (isNotSeekingPhase() || isDisabled()) {
            stopSeekingSession();
            return;
        }

        if (!plugin.getTeamBossBarManager().hasGlobalBossBar(GLOBAL_BAR_ID)) {
            startSeekingSession();
            return;
        }

        int hidersLeft = countHidersLeft();
        int seekers = countSeekers();
        float progress = resolveProgress(hidersLeft);

        plugin.getTeamBossBarManager().updateGlobalBossBar(GLOBAL_BAR_ID, progress, buildTitle(hidersLeft, seekers));
        plugin.getTeamBossBarManager().updateGlobalColor(GLOBAL_BAR_ID, resolveConfiguredColor(hidersLeft));
        plugin.getTeamBossBarManager().setGlobalVisible(GLOBAL_BAR_ID, true);

        if (deathAnimationTask == null) {
            plugin.getTeamBossBarManager().updateGlobalStyle(GLOBAL_BAR_ID, BarStyle.SOLID);
        }

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            plugin.getTeamBossBarManager().addPlayerToGlobalBossBar(GLOBAL_BAR_ID, onlinePlayer);
        }
    }

    public void onHiderEliminated() {
        refreshNow();

        boolean animationEnabled = plugin.getSettingRegistry().get("game.seeking-bossbar.animation.enabled", true);
        if (!animationEnabled || !plugin.getTeamBossBarManager().hasGlobalBossBar(GLOBAL_BAR_ID)) {
            return;
        }

        startDeathAnimation();
    }

    public void onPlayerJoin(Player player) {
        if (isNotSeekingPhase() || isDisabled()) {
            return;
        }

        if (!plugin.getTeamBossBarManager().hasGlobalBossBar(GLOBAL_BAR_ID)) {
            startSeekingSession();
            return;
        }

        plugin.getTeamBossBarManager().addPlayerToGlobalBossBar(GLOBAL_BAR_ID, player);
        refreshNow();
    }

    public void onPlayerQuit() {
        if (isNotSeekingPhase() || isDisabled()) {
            return;
        }
        refreshNow();
    }

    private void startLiveUpdater() {
        cancelLiveUpdateTask();
        liveUpdateTask = new BukkitRunnable() {
            @Override
            public void run() {
                refreshNow();
            }
        }.runTaskTimer(plugin, LIVE_UPDATE_PERIOD_TICKS, LIVE_UPDATE_PERIOD_TICKS);
    }

    private void cancelLiveUpdateTask() {
        if (liveUpdateTask != null) {
            liveUpdateTask.cancel();
            liveUpdateTask = null;
        }
    }

    private void startDeathAnimation() {
        cancelDeathAnimationTask();

        final BarStyle[] animationFrames = {
                BarStyle.SEGMENTED_20,
                BarStyle.SEGMENTED_10,
                BarStyle.SEGMENTED_6,
                BarStyle.SEGMENTED_10,
                BarStyle.SEGMENTED_20
        };
        final long speedTicks = Math.max(1L, plugin.getSettingRegistry().get("game.seeking-bossbar.animation.speed-ticks", 3));

        deathAnimationTask = new BukkitRunnable() {
            private int frameIndex = 0;

            @Override
            public void run() {
                if (isNotSeekingPhase() || isDisabled() || !plugin.getTeamBossBarManager().hasGlobalBossBar(GLOBAL_BAR_ID)) {
                    cancelDeathAnimationTask();
                    return;
                }

                if (frameIndex >= animationFrames.length) {
                    plugin.getTeamBossBarManager().updateGlobalStyle(GLOBAL_BAR_ID, BarStyle.SOLID);
                    cancelDeathAnimationTask();
                    return;
                }

                plugin.getTeamBossBarManager().updateGlobalStyle(GLOBAL_BAR_ID, animationFrames[frameIndex]);
                frameIndex++;
            }
        }.runTaskTimer(plugin, 0L, speedTicks);
    }

    private void cancelDeathAnimationTask() {
        if (deathAnimationTask != null) {
            deathAnimationTask.cancel();
            deathAnimationTask = null;
        }
    }

    private float resolveProgress(int hidersLeft) {
        Object progressModeObj = plugin.getSettingRegistry().get("game.seeking-bossbar.progress-mode");
        SeekingBossBarProgressMode progressMode = (progressModeObj instanceof SeekingBossBarProgressMode mode)
                ? mode
                : SeekingBossBarProgressMode.PROGRESS;

        if (progressMode == SeekingBossBarProgressMode.FULL) {
            return 1.0f;
        }

        return Math.clamp(hidersLeft / (float) Math.max(1, maxHidersAtSeekingStart), 0.0f, 1.0f);
    }

    private BarColor resolveConfiguredColor(int hidersLeft) {
        Object colorModeObj = plugin.getSettingRegistry().get("game.seeking-bossbar.color.mode");
        SeekingBossBarColorMode colorMode = (colorModeObj instanceof SeekingBossBarColorMode mode)
                ? mode
                : SeekingBossBarColorMode.DYNAMIC;

        if (colorMode == SeekingBossBarColorMode.STATIC) {
            Object staticColorObj = plugin.getSettingRegistry().get("game.seeking-bossbar.color.static-color");
            SeekingBossBarStaticColor staticColor = (staticColorObj instanceof SeekingBossBarStaticColor color)
                    ? color
                    : SeekingBossBarStaticColor.GREEN;
            return staticColor.getBarColor();
        }

        if (hidersLeft <= 0) {
            return BarColor.RED;
        }

        float ratio = hidersLeft / (float) Math.max(1, maxHidersAtSeekingStart);
        if (ratio > 0.66f) {
            return BarColor.GREEN;
        }
        if (ratio > 0.33f) {
            return BarColor.YELLOW;
        }
        return BarColor.RED;
    }

    private Component buildTitle(int hidersLeft, int seekers) {
        Object layoutObj = plugin.getSettingRegistry().get("game.seeking-bossbar.name-layout");
        SeekingBossBarNameLayout layout = (layoutObj instanceof SeekingBossBarNameLayout nameLayout)
                ? nameLayout
                : SeekingBossBarNameLayout.HIDERS_AND_SEEKERS;

        return switch (layout) {
            case HIDERS_ONLY -> Component.text("Hiders Left: " + hidersLeft + "/" + maxHidersAtSeekingStart);
            case SEEKERS_ONLY -> Component.text("Seekers: " + seekers);
            case HIDERS_AND_SEEKERS ->
                    Component.text("Hiders: " + hidersLeft + "/" + maxHidersAtSeekingStart + " | Seekers: " + seekers);
        };
    }

    private int countHidersLeft() {
        int count = 0;
        for (var hiderId : HideAndSeek.getDataController().getHiders()) {
            Player player = Bukkit.getPlayer(hiderId);
            if (player != null && player.isOnline()) {
                count++;
            }
        }
        return count;
    }

    private int countSeekers() {
        int count = 0;
        for (var seekerId : HideAndSeek.getDataController().getSeekers()) {
            Player player = Bukkit.getPlayer(seekerId);
            if (player != null && player.isOnline()) {
                count++;
            }
        }
        return count;
    }

    private boolean isDisabled() {
        return plugin.getSettingRegistry().get("game.seeking-bossbar.enabled", true);
    }

    private boolean isNotSeekingPhase() {
        return !"seeking".equals(plugin.getStateManager().getCurrentPhaseId());
    }
}
