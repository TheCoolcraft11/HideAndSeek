package de.thecoolcraft11.hideAndSeek.util;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public final class XpProgressHelper {

    public enum Mode {
        COUNTDOWN,
        COUNTUP
    }

    public record SavedXp(float exp, int level) {
    }

    public static void applyCountdown(Player player, long elapsedMs, long totalMs) {
        long safeTotalMs = Math.max(1L, totalMs);
        double remaining = Math.max(0.0, 1.0 - (double) elapsedMs / safeTotalMs);
        int secondsLeft = (int) Math.ceil((safeTotalMs - elapsedMs) / 1000.0);
        player.setLevel(Math.max(0, secondsLeft));
        player.setExp((float) Math.max(0.0, Math.min(0.9999, remaining)));
    }

    public static void applyCountup(Player player, long elapsedMs, long totalMs, int maxLevel) {
        long safeTotalMs = Math.max(1L, totalMs);
        double progress = Math.min(1.0, (double) elapsedMs / safeTotalMs);
        player.setExp((float) Math.min(0.9999, progress));
        player.setLevel((int) (progress * maxLevel));
    }


    public static SavedXp saveXp(Player player) {
        return new SavedXp(player.getExp(), player.getLevel());
    }

    public static BukkitTask start(HideAndSeek plugin, Player player, long durationTicks, Mode mode, int maxLevel) {
        long safeDurationTicks = Math.max(1L, durationTicks);

        if (mode == Mode.COUNTDOWN) {
            player.setLevel(maxLevel);
            player.setExp(1.0f);
        } else {
            player.setLevel(0);
            player.setExp(0.0f);
        }

        return new BukkitRunnable() {
            long tick = 0;

            @Override
            public void run() {
                if (!player.isOnline()) {
                    cancel();
                    return;
                }

                if (tick >= safeDurationTicks) {
                    if (mode == Mode.COUNTDOWN) {
                        player.setLevel(0);
                        player.setExp(0.0f);
                    } else {
                        player.setLevel(maxLevel);
                        player.setExp(0.9999f);
                    }
                    cancel();
                    return;
                }

                double progress = Math.max(0.0, Math.min(1.0, (double) tick / safeDurationTicks));

                if (mode == Mode.COUNTDOWN) {
                    double remaining = 1.0 - progress;
                    int secondsLeft = (int) Math.ceil((safeDurationTicks - tick) / 20.0);
                    player.setLevel(Math.max(0, secondsLeft));
                    player.setExp((float) Math.max(0.0, Math.min(0.9999, remaining)));
                } else {
                    player.setExp((float) Math.min(0.9999, progress));
                    player.setLevel((int) (progress * maxLevel));
                }

                tick++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    public static BukkitTask start(HideAndSeek plugin, Player player, long durationTicks, Mode mode) {
        int maxLevel = (mode == Mode.COUNTDOWN) ? (int) Math.ceil(Math.max(1L, durationTicks) / 20.0) : 10;
        return start(plugin, player, durationTicks, mode, maxLevel);
    }

    public static void stopAndRestore(Player player, BukkitTask task, SavedXp savedXp) {
        if (task != null) {
            task.cancel();
        }
        if (savedXp != null && player.isOnline()) {
            player.setExp(savedXp.exp());
            player.setLevel(savedXp.level());
        }
    }

    public static void stopAndClear(Player player, BukkitTask task) {
        if (task != null) {
            task.cancel();
        }
        if (player.isOnline()) {
            player.setExp(0.0f);
            player.setLevel(0);
        }
    }

    private XpProgressHelper() {
    }
}
