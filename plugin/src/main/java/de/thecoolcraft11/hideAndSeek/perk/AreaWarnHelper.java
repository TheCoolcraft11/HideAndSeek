package de.thecoolcraft11.hideAndSeek.perk;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AreaWarnHelper {

    private static final int WARN_INTERVAL_TICKS = 20;

    private final HideAndSeek plugin;
    private final Location center;
    private final double radius;
    private final int totalTicks;

    private final Map<UUID, BossBar> bossBars = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> borderWarningActive = new ConcurrentHashMap<>();
    private BukkitTask warnTask;
    private int ticksRemaining;

    public AreaWarnHelper(HideAndSeek plugin, Location center, double radius, int durationTicks) {
        this.plugin = plugin;
        this.center = center.clone();
        this.radius = radius;
        this.totalTicks = Math.max(1, durationTicks);
        this.ticksRemaining = Math.max(1, durationTicks);
    }

    public void start(Iterable<UUID> allAffectedPlayers) {
        refreshBossBars(allAffectedPlayers);
        warnTask = new BukkitRunnable() {
            @Override
            public void run() {
                ticksRemaining -= WARN_INTERVAL_TICKS;
                if (ticksRemaining <= 0) {
                    stop();
                    return;
                }
                refreshBossBars(allAffectedPlayers);
            }
        }.runTaskTimer(plugin, WARN_INTERVAL_TICKS, WARN_INTERVAL_TICKS);
    }

    public void stop() {
        if (warnTask != null) {
            warnTask.cancel();
            warnTask = null;
        }
        bossBars.values().forEach(BossBar::removeAll);
        bossBars.clear();

        for (UUID playerId : borderWarningActive.keySet()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline()) {
                plugin.getNmsAdapter().resetWarningBorder(player);
            }
        }
        borderWarningActive.clear();
    }

    private void refreshBossBars(Iterable<UUID> candidates) {

        if (safeWorld(center) == null) {
            stop();
            return;
        }

        for (UUID uid : candidates) {
            Player p = Bukkit.getPlayer(uid);
            if (p == null || !p.isOnline()) {
                continue;
            }

            if (!HideAndSeek.getDataController().getHiders().contains(uid)) {
                hideBossBar(p);
                clearBorderWarning(p);
                continue;
            }

            if (isInsideZone(p.getLocation())) {
                applyWarnEffect(p);
                showBossBar(p);
            } else {
                hideBossBar(p);
                clearBorderWarning(p);
            }
        }
    }

    private void applyWarnEffect(Player player) {
        World centerWorld = safeWorld(center);
        if (centerWorld == null) {
            stop();
            return;
        }

        int steps = 96;
        for (int i = 0; i < steps; i++) {
            double angle = (Math.PI * 2 / steps) * i;
            double x = center.getX() + Math.cos(angle) * radius;
            double z = center.getZ() + Math.sin(angle) * radius;
            Location ringPoint = new Location(centerWorld, x, player.getLocation().getY(), z);

            for (int j = 0; j < 10; j++) {
                player.spawnParticle(Particle.DUST, ringPoint.clone().add(0, j - 5, 0), 1, 0, 0, 0, 0,
                        new Particle.DustOptions(Color.RED, 1.5f));
            }

        }

        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 0.5f, 0.5f);

        int secondsRemaining = ticksRemaining / 20;
        player.showTitle(Title.title(
                Component.empty(),
                Component.text("DANGER ZONE - leave in " + secondsRemaining + "s", NamedTextColor.RED)
                        .decoration(TextDecoration.BOLD, true),
                Title.Times.times(Duration.ZERO, Duration.ofMillis(1200), Duration.ofMillis(150))
        ));

        if (plugin.getNmsAdapter().hasCapability(de.thecoolcraft11.hideAndSeek.nms.NmsCapabilities.CLIENT_FAKE_BORDER_WARNING)) {
            int warningDistance = (int) Math.ceil(Math.max(6.0, 1 + (double) ((totalTicks - ticksRemaining) * (6 - 1)) / totalTicks));
            plugin.getNmsAdapter().showWarningBorder(player, warningDistance);
            borderWarningActive.put(player.getUniqueId(), true);
        }
    }

    private void showBossBar(Player player) {
        BossBar bar = bossBars.computeIfAbsent(player.getUniqueId(), key ->
                Bukkit.createBossBar("Danger Zone", BarColor.RED, BarStyle.SOLID));

        double progress = Math.clamp((double) ticksRemaining / totalTicks, 0.0, 1.0);
        bar.setProgress(progress);
        bar.setTitle("Leave the danger zone! " + ticksRemaining / 20 + "s remaining");

        if (!bar.getPlayers().contains(player)) {
            bar.addPlayer(player);
        }
    }

    private void hideBossBar(Player player) {
        BossBar bar = bossBars.remove(player.getUniqueId());
        if (bar != null) {
            bar.removePlayer(player);
            bar.removeAll();
        }
    }

    private void clearBorderWarning(Player player) {
        if (borderWarningActive.remove(player.getUniqueId()) != null) {
            plugin.getNmsAdapter().resetWarningBorder(player);
        }
    }

    public void clearForPlayer(UUID playerId) {
        if (playerId == null) {
            return;
        }

        Player player = Bukkit.getPlayer(playerId);
        if (player != null) {
            hideBossBar(player);
            clearBorderWarning(player);
            return;
        }

        BossBar bar = bossBars.remove(playerId);
        if (bar != null) {
            bar.removeAll();
        }
        borderWarningActive.remove(playerId);
    }

    public boolean isInsideZone(Location loc) {
        World locWorld = safeWorld(loc);
        World centerWorld = safeWorld(center);
        if (locWorld == null || centerWorld == null) {
            return false;
        }
        if (!locWorld.equals(centerWorld)) {
            return false;
        }
        double dx = loc.getX() - center.getX();
        double dz = loc.getZ() - center.getZ();
        return (dx * dx + dz * dz) <= (radius * radius);
    }

    private @org.jspecify.annotations.Nullable World safeWorld(@org.jspecify.annotations.Nullable Location location) {
        if (location == null) {
            return null;
        }
        try {
            return location.getWorld();
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public int getTicksRemaining() {
        return ticksRemaining;
    }
}



