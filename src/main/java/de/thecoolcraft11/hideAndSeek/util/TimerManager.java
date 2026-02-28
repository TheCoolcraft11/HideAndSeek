package de.thecoolcraft11.hideAndSeek.util;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.timer.AnimationType;
import de.thecoolcraft11.timer.MultiTimerManager.TimerType;
import de.thecoolcraft11.timer.TimerInstance;
import de.thecoolcraft11.timer.api.TimerBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public class TimerManager {

    public static void cleanupTimers(HideAndSeek plugin) {
        if (plugin.getTimerApi() == null) {
            return;
        }

        try {

            plugin.getTimerApi().deleteTimer("Hiding");
            plugin.getLogger().info("Deleted hiding timer");
        } catch (Exception ignored) {

        }

        try {

            plugin.getTimerApi().deleteTimer("Seeking");
            plugin.getLogger().info("Deleted seeking timer");
        } catch (Exception ignored) {

        }
    }

    public static void cleanupTimer(HideAndSeek plugin, String timerName) {
        if (plugin.getTimerApi() == null) {
            return;
        }

        try {
            plugin.getTimerApi().deleteTimer(timerName);
            plugin.getLogger().info("Deleted timer: " + timerName);
        } catch (Exception ignored) {

        }

    }

    public static void startHidingTimer(HideAndSeek plugin) {

        if (plugin.getTimerApi() == null || plugin.getTimerPlugin() == null) {

            startHidingTimerFallback(plugin);
            return;
        }

        try {
            Number hidingTime = plugin.getSettingRegistry().get("game.hiding_time");
            String color1 = plugin.getSettingRegistry().get("timer.hiding_color1");
            String color2 = plugin.getSettingRegistry().get("timer.hiding_color2");
            Object animationTypeObj = plugin.getSettingRegistry().get("timer.animation_type");
            Number animationSpeed = plugin.getSettingRegistry().get("timer.animation_speed");


            if (color1 == null) color1 = "#FF0000";
            if (color2 == null) color2 = "#0000FF";
            AnimationType animationType = (animationTypeObj instanceof AnimationType) ? (AnimationType) animationTypeObj : AnimationType.WAVE;
            if (animationSpeed == null) animationSpeed = 0.5;

            TimerInstance timer = new TimerBuilder(plugin.getTimerApi(), plugin.getTimerPlugin())
                    .name("Hiding")
                    .targetId("had_hiding_timer")
                    .color1(color1)
                    .color2(color2)
                    .countingUp(false)
                    .maxTime(hidingTime.longValue())
                    .initialTime(hidingTime.longValue())
                    .visible(true)
                    .showName(false)
                    .animationType(animationType)
                    .animationSpeed(animationSpeed.doubleValue())
                    .animationDurationTicks(10)
                    .type(TimerType.GLOBAL)
                    .build();


            if (timer == null) {
                plugin.getLogger().warning("TimerBuilder returned null, falling back to custom timer");
                startHidingTimerFallback(plugin);
                return;
            }


            timer.addTarget("had_hiding_complete", 0, () -> {
                try {

                    timer.stop();
                    timer.setVisible(false);


                    plugin.getStateManager().setPhase("seeking");


                    Bukkit.getScheduler().runTaskLater(plugin, () -> cleanupTimer(plugin, "Hiding"), 5L);
                } catch (Exception e) {
                    plugin.getLogger().warning("Error transitioning to seeking phase: " + e.getMessage());
                }
            });

            timer.start();
            plugin.getLogger().info("Hiding timer started with TimerAPI");
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to create hiding timer using Timer API, falling back to custom timer: " + e.getMessage());
            startHidingTimerFallback(plugin);
        }
    }

    public static void startSeekingTimer(HideAndSeek plugin) {

        if (plugin.getTimerApi() == null || plugin.getTimerPlugin() == null) {

            startSeekingTimerFallback(plugin);
            return;
        }

        try {
            Number seekingTime = plugin.getSettingRegistry().get("game.seeking_time");
            String color1 = plugin.getSettingRegistry().get("timer.seeking_color1");
            String color2 = plugin.getSettingRegistry().get("timer.seeking_color2");
            Object animationTypeObj = plugin.getSettingRegistry().get("timer.animation_type");
            Number animationSpeed = plugin.getSettingRegistry().get("timer.animation_speed");


            if (color1 == null) color1 = "#FFFF00";
            if (color2 == null) color2 = "#00FFFF";
            AnimationType animationType = (animationTypeObj instanceof AnimationType) ? (AnimationType) animationTypeObj : AnimationType.WAVE;
            if (animationSpeed == null) animationSpeed = 0.5;

            TimerInstance timer = new TimerBuilder(plugin.getTimerApi(), plugin.getTimerPlugin())
                    .name("Seeking")
                    .targetId("had_seeking_timer")
                    .color1(color1)
                    .color2(color2)
                    .countingUp(false)
                    .maxTime(seekingTime.longValue())
                    .initialTime(seekingTime.longValue())
                    .visible(true)
                    .showName(false)
                    .animationType(animationType)
                    .animationSpeed(animationSpeed.doubleValue())
                    .animationDurationTicks(10)
                    .type(TimerType.GLOBAL)
                    .build();


            if (timer == null) {
                plugin.getLogger().warning("TimerBuilder returned null, falling back to custom timer");
                startSeekingTimerFallback(plugin);
                return;
            }


            timer.addTarget("had_seeking_complete", 0, () -> {
                try {

                    timer.stop();
                    timer.setVisible(false);


                    plugin.getStateManager().setPhase("ended");


                    Bukkit.getScheduler().runTaskLater(plugin, () -> cleanupTimer(plugin, "Seeking"), 5L);
                } catch (Exception e) {
                    plugin.getLogger().warning("Error transitioning to ended phase: " + e.getMessage());
                }
            });

            timer.start();
            plugin.getLogger().info("Seeking timer started with TimerAPI");
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to create seeking timer using Timer API, falling back to custom timer: " + e.getMessage());
            startSeekingTimerFallback(plugin);
        }
    }


    private static void startHidingTimerFallback(HideAndSeek plugin) {
        Number hidingTime = plugin.getSettingRegistry().get("game.hiding_time");
        int timeRemaining = hidingTime.intValue();

        BukkitTask[] task = new BukkitTask[1];
        task[0] = new BukkitRunnable() {
            int time = timeRemaining;

            @Override
            public void run() {
                time--;

                if (time > 0) {
                    if (time <= 5 || time == 10 || time == 30 || time % 15 == 0) {
                        Component message = Component.text(time + " seconds remaining!", NamedTextColor.YELLOW);
                        for (var player : Bukkit.getOnlinePlayers()) {
                            player.sendActionBar(message);
                        }
                    }
                } else {
                    plugin.getStateManager().setPhase("seeking");
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    private static void startSeekingTimerFallback(HideAndSeek plugin) {
        Number seekingTime = plugin.getSettingRegistry().get("game.seeking_time");
        int timeRemaining = seekingTime.intValue();

        BukkitTask[] task = new BukkitTask[1];
        task[0] = new BukkitRunnable() {
            int time = timeRemaining;

            @Override
            public void run() {
                time--;

                if (time > 0) {
                    if (time <= 10 || time == 30 || time == 60 || time % 60 == 0) {
                        Component message = Component.text("Time remaining: " + formatTime(time), NamedTextColor.YELLOW);
                        for (var player : Bukkit.getOnlinePlayers()) {
                            player.sendActionBar(message);
                        }
                    }
                } else {
                    plugin.getStateManager().setPhase("ended");
                    cancel();
                }
            }

            private String formatTime(int seconds) {
                int minutes = seconds / 60;
                int secs = seconds % 60;
                return String.format("%d:%02d", minutes, secs);
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }
}
