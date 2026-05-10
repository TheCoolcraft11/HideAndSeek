package de.thecoolcraft11.hideAndSeek.listener.game;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.util.AdrenalineRushService;
import de.thecoolcraft11.timer.api.events.TimerTickEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class AdrenalineRushListener implements Listener {

    private final HideAndSeek plugin;
    private final AdrenalineRushService rushService;

    private int fallbackTaskId = -1;

    public AdrenalineRushListener(HideAndSeek plugin, AdrenalineRushService rushService) {
        this.plugin = plugin;
        this.rushService = rushService;

        if (plugin.getTimerPlugin() == null) {
            startFallbackLoop();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!"seeking".equals(plugin.getStateManager().getCurrentPhaseId())) {
            return;
        }
        rushService.evaluate(-1);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onTimerTick(TimerTickEvent event) {
        if (!"seeking".equals(plugin.getStateManager().getCurrentPhaseId())) {
            return;
        }

        if (!"Seeking".equals(event.getTimerName())) {
            return;
        }
        int secondsRemaining = -1;
        if (event.getTimer() != null) {
            if (!event.getTimer().isCountingUp()) {
                secondsRemaining = Math.toIntExact(event.getCurrentTime());
            } else {
                if (event.getTimer().getMaxTime() > 0) {
                    secondsRemaining = event.getTimer().getMaxTime() > event.getCurrentTime() ? Math.toIntExact(
                            (event.getTimer().getMaxTime() - event.getCurrentTime())) : -1;
                }
            }
        }
        if (secondsRemaining != -1) rushService.evaluate(secondsRemaining);
    }

    private void startFallbackLoop() {
        fallbackTaskId = org.bukkit.Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (!"seeking".equals(plugin.getStateManager().getCurrentPhaseId())) {
                return;
            }
            rushService.evaluate(-1);
        }, 20L, 20L);
    }

    public void shutdown() {
        if (fallbackTaskId >= 0) {
            org.bukkit.Bukkit.getScheduler().cancelTask(fallbackTaskId);
            fallbackTaskId = -1;
        }
    }
}