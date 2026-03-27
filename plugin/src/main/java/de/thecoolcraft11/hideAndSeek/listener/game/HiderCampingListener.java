package de.thecoolcraft11.hideAndSeek.listener.game;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.listener.player.PlayerHitListener;
import de.thecoolcraft11.hideAndSeek.nms.NmsCapabilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.*;

public class HiderCampingListener implements Listener {
    private static final long CHECK_INTERVAL_TICKS = 5L;
    private static final long WARNING_SIGNAL_INTERVAL_TICKS = 5L;
    private static final long PUNISH_SIGNAL_INTERVAL_TICKS = 5L;

    private final HideAndSeek plugin;
    private final PlayerHitListener playerHitListener;
    private final Map<UUID, CampingState> states = new HashMap<>();
    private final NamespacedKey safeLightningKey;
    private BukkitTask monitorTask;

    public HiderCampingListener(HideAndSeek plugin, PlayerHitListener playerHitListener) {
        this.plugin = plugin;
        this.playerHitListener = playerHitListener;
        this.safeLightningKey = new NamespacedKey(plugin, "freezeLightning");
        this.monitorTask = Bukkit.getScheduler().runTaskTimer(plugin, this::checkCamping, CHECK_INTERVAL_TICKS, CHECK_INTERVAL_TICKS);
    }

    private static double horizontalDistanceSquared(Location a, Location b) {
        double dx = a.getX() - b.getX();
        double dz = a.getZ() - b.getZ();
        return dx * dx + dz * dz;
    }

    public void shutdown() {
        if (monitorTask != null) {
            monitorTask.cancel();
            monitorTask = null;
        }
        clearAllState();
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        clearPlayerState(event.getPlayer().getUniqueId(), false);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        clearPlayerState(event.getEntity().getUniqueId(), true);
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        UUID playerId = event.getPlayer().getUniqueId();
        if (!HideAndSeek.getDataController().getHiders().contains(playerId)) {
            return;
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Player player = event.getPlayer();
            if (player.isOnline()) {
                resetStateFor(playerId, player);
            }
        }, 1L);
    }

    private void checkCamping() {
        if (!plugin.getSettingRegistry().get("anticheat.enabled", true)
                || !plugin.getSettingRegistry().get("anticheat.hider-camping.enabled", true)
                || !"seeking".equals(plugin.getStateManager().getCurrentPhaseId())) {
            clearAllState();
            return;
        }

        double spotRadius = Math.max(0.25, plugin.getSettingRegistry().get("anticheat.hider-camping.spot-radius", 2.5));
        double spotRadiusSq = spotRadius * spotRadius;
        int maxDurationSeconds = Math.max(1, plugin.getSettingRegistry().get("anticheat.hider-camping.max-duration", 90));
        int warnSeconds = Math.max(0, plugin.getSettingRegistry().get("anticheat.hider-camping.warn-time", 15));
        int damageCooldownTicks = Math.max(1, plugin.getSettingRegistry().get("anticheat.hider-camping.damage-cooldown-ticks", 20));
        double damageAmount = Math.max(0.1, plugin.getSettingRegistry().get("anticheat.hider-camping.damage-amount", 1.0));

        long maxDurationTicks = maxDurationSeconds * 20L;
        long warnStartTicks = Math.max(0L, maxDurationTicks - (warnSeconds * 20L));
        Set<UUID> activeHiders = new HashSet<>(HideAndSeek.getDataController().getHiders());
        states.keySet().removeIf(id -> {
            if (activeHiders.contains(id)) {
                return false;
            }
            Player player = Bukkit.getPlayer(id);
            if (player != null && player.isOnline()) {
                player.resetTitle();
            }
            return true;
        });

        for (UUID hiderId : activeHiders) {
            Player hider = Bukkit.getPlayer(hiderId);
            if (hider == null || !hider.isOnline()) {
                states.remove(hiderId);
                continue;
            }

            CampingState state = states.computeIfAbsent(hiderId, id -> new CampingState(hider.getLocation()));
            Location location = hider.getLocation();

            if (state.anchor.getWorld() == null || location.getWorld() == null || !state.anchor.getWorld().equals(location.getWorld())) {
                resetStateFor(hiderId, hider);
                continue;
            }

            if (horizontalDistanceSquared(state.anchor, location) > spotRadiusSq) {
                resetStateFor(hiderId, hider);
                continue;
            }

            state.stationaryTicks += CHECK_INTERVAL_TICKS;
            if (state.stationaryTicks >= maxDurationTicks) {
                handlePunishment(hider, state, damageAmount, damageCooldownTicks);
                continue;
            }

            if (state.stationaryTicks >= warnStartTicks) {
                int remainingSeconds = (int) Math.ceil((maxDurationTicks - state.stationaryTicks) / 20.0);
                sendWarning(hider, state, remainingSeconds);
            }
        }
    }

    private void handlePunishment(Player hider, CampingState state, double damageAmount, int damageCooldownTicks) {
        if (!state.punishing) {
            state.punishing = true;
            state.lastPunishSignalTicks = -PUNISH_SIGNAL_INTERVAL_TICKS;
            state.lastDamageTicks = state.stationaryTicks - Math.max(1, damageCooldownTicks);
            spawnStrikeForHider(hider);
        }

        long currentTicks = state.stationaryTicks;
        if (currentTicks - state.lastPunishSignalTicks >= PUNISH_SIGNAL_INTERVAL_TICKS) {
            state.lastPunishSignalTicks = currentTicks;
            hider.showTitle(Title.title(
                    Component.text("STOP CAMPING", NamedTextColor.RED, TextDecoration.BOLD)
                            .decoration(TextDecoration.ITALIC, false),
                    Component.text("Move now or keep taking damage", NamedTextColor.GOLD)
                            .decoration(TextDecoration.ITALIC, false),
                    Title.Times.times(Duration.ofMillis(120), Duration.ofMillis(700), Duration.ofMillis(120))
            ));
            hider.spawnParticle(Particle.ELECTRIC_SPARK, hider.getLocation().add(0, 1.0, 0), 12, 0.25, 0.25, 0.25, 0.03);
            hider.playSound(hider.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 0.8f, 1.5f);
        }

        if (currentTicks - state.lastDamageTicks < Math.max(1, damageCooldownTicks)) {
            return;
        }

        state.lastDamageTicks = currentTicks;

        if (damageAmount <= 0.0) {
            return;
        }

        if (hider.getHealth() - damageAmount <= 0.0) {
            playerHitListener.markEnvironmentalDeath(hider.getUniqueId(), PlayerHitListener.EnvironmentalDeathCause.CAMPING);
        }

        hider.setNoDamageTicks(0);
        hider.damage(damageAmount, DamageSource.builder(DamageType.OUTSIDE_BORDER).build());
    }

    private void sendWarning(Player hider, CampingState state, int remainingSeconds) {
        long currentTicks = state.stationaryTicks;
        if (currentTicks - state.lastWarningSignalTicks < WARNING_SIGNAL_INTERVAL_TICKS) {
            return;
        }

        state.lastWarningSignalTicks = currentTicks;
        hider.showTitle(Title.title(
                Component.text("MOVE", NamedTextColor.YELLOW, TextDecoration.BOLD)
                        .decoration(TextDecoration.ITALIC, false),
                Component.text("Camping punish in " + Math.max(0, remainingSeconds) + "s", NamedTextColor.RED)
                        .decoration(TextDecoration.ITALIC, false),
                Title.Times.times(Duration.ofMillis(120), Duration.ofMillis(700), Duration.ofMillis(120))
        ));
        hider.spawnParticle(Particle.ELECTRIC_SPARK, hider.getLocation().add(0, 1.0, 0), 8, 0.2, 0.2, 0.2, 0.03);
        hider.playSound(hider.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.7f, 1.8f);
    }

    private void spawnStrikeForHider(Player hider) {
        Location strikeLocation = hider.getLocation();
        boolean sentClientLightning = false;
        if (plugin.getNmsAdapter().hasCapability(NmsCapabilities.CLIENT_LIGHTNING_PACKET)) {
            sentClientLightning = plugin.getNmsAdapter().spawnClientLightning(hider, strikeLocation);
        }

        if (sentClientLightning) {
            return;
        }

        Entity lightning = hider.getWorld().spawnEntity(strikeLocation, EntityType.LIGHTNING_BOLT);
        lightning.getPersistentDataContainer().set(safeLightningKey, PersistentDataType.BOOLEAN, true);
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.getUniqueId().equals(hider.getUniqueId())) {
                player.hideEntity(plugin, lightning);
            }
        }
    }

    private void resetStateFor(UUID playerId, Player player) {
        CampingState state = states.computeIfAbsent(playerId, id -> new CampingState(player.getLocation()));
        state.anchor = player.getLocation();
        state.stationaryTicks = 0;
        state.lastDamageTicks = Long.MIN_VALUE / 4;
        state.lastWarningSignalTicks = -WARNING_SIGNAL_INTERVAL_TICKS;
        state.lastPunishSignalTicks = -PUNISH_SIGNAL_INTERVAL_TICKS;
        state.punishing = false;
        player.resetTitle();
    }

    private void clearPlayerState(UUID playerId, boolean resetTitle) {
        states.remove(playerId);
        if (!resetTitle) {
            return;
        }

        Player player = Bukkit.getPlayer(playerId);
        if (player != null && player.isOnline()) {
            player.resetTitle();
        }
    }

    private void clearAllState() {
        for (UUID playerId : states.keySet()) {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null && player.isOnline()) {
                player.resetTitle();
            }
        }
        states.clear();
    }

    private static final class CampingState {
        private Location anchor;
        private long stationaryTicks;
        private long lastDamageTicks;
        private long lastWarningSignalTicks;
        private long lastPunishSignalTicks;
        private boolean punishing;

        private CampingState(Location anchor) {
            this.anchor = anchor;
            this.stationaryTicks = 0;
            this.lastDamageTicks = Long.MIN_VALUE / 4;
            this.lastWarningSignalTicks = -WARNING_SIGNAL_INTERVAL_TICKS;
            this.lastPunishSignalTicks = -PUNISH_SIGNAL_INTERVAL_TICKS;
            this.punishing = false;
        }
    }
}
