package de.thecoolcraft11.hideAndSeek.listener.game;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.listener.player.PlayerHitListener;
import org.bukkit.Bukkit;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EnvironmentalDamageListener implements Listener {
    private static final long DAMAGE_STREAK_TIMEOUT_MS = 2500L;

    private final HideAndSeek plugin;
    private final PlayerHitListener playerHitListener;
    private final Map<UUID, DamageStreakState> damageStreaks = new HashMap<>();

    public EnvironmentalDamageListener(HideAndSeek plugin, PlayerHitListener playerHitListener) {
        this.plugin = plugin;
        this.playerHitListener = playerHitListener;
    }

    public void shutdown() {
        damageStreaks.clear();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEnvironmentalDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        UUID playerId = player.getUniqueId();
        if (!"seeking".equals(plugin.getStateManager().getCurrentPhaseId())
                || !HideAndSeek.getDataController().getHiders().contains(playerId)) {
            damageStreaks.remove(playerId);
            return;
        }

        TrackedSource source = TrackedSource.fromCause(event.getCause());
        if (source == null || !source.isEnabled(plugin)) {
            return;
        }

        long now = System.currentTimeMillis();
        DamageStreakState state = damageStreaks.computeIfAbsent(playerId,
                ignored -> new DamageStreakState(source, now));
        if (state.source != source || now - state.lastVanillaDamageAtMs > DAMAGE_STREAK_TIMEOUT_MS) {
            state.source = source;
            state.firstVanillaDamageAtMs = now;
            state.lastPluginDamageAtMs = Long.MIN_VALUE / 4;
        }

        state.lastVanillaDamageAtMs = now;

        long safeDurationMs = Math.max(0, source.getSafeDurationSeconds(plugin)) * 1000L;
        if (now - state.firstVanillaDamageAtMs < safeDurationMs) {
            return;
        }


        event.setCancelled(true);

        int cooldownTicks = Math.max(1, source.getDamageCooldownTicks(plugin));
        long cooldownMs = cooldownTicks * 50L;
        if (now - state.lastPluginDamageAtMs < cooldownMs) {
            return;
        }

        double damageAmount = Math.max(0.1, source.getDamageAmount(plugin));
        state.lastPluginDamageAtMs = now;
        UUID victimId = player.getUniqueId();
        Bukkit.getScheduler().runTask(plugin, () -> {
            Player victim = Bukkit.getPlayer(victimId);
            if (victim == null || !victim.isOnline() || victim.isDead()) {
                return;
            }
            applyPluginDamage(victim, source, damageAmount);
        });
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        damageStreaks.remove(event.getEntity().getUniqueId());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        damageStreaks.remove(event.getPlayer().getUniqueId());
    }

    private void applyPluginDamage(Player player, TrackedSource source, double damageAmount) {
        if (player.getHealth() - damageAmount <= 0) {
            playerHitListener.markEnvironmentalDeath(player.getUniqueId(), source.getDeathCause());
        }

        player.setNoDamageTicks(0);
        player.damage(damageAmount, DamageSource.builder(DamageType.OUTSIDE_BORDER).build());
    }

    private enum TrackedSource {
        DROWNING(
                "drowning",
                PlayerHitListener.EnvironmentalDeathCause.DROWNING,
                EntityDamageEvent.DamageCause.DROWNING
        ),
        FIRE(
                "fire",
                PlayerHitListener.EnvironmentalDeathCause.FIRE,
                EntityDamageEvent.DamageCause.FIRE,
                EntityDamageEvent.DamageCause.FIRE_TICK
        ),
        LAVA(
                "lava",
                PlayerHitListener.EnvironmentalDeathCause.LAVA,
                EntityDamageEvent.DamageCause.LAVA
        ),
        SUFFOCATION(
                "suffocation",
                PlayerHitListener.EnvironmentalDeathCause.SUFFOCATION,
                EntityDamageEvent.DamageCause.SUFFOCATION
        ),
        FREEZING(
                "freezing",
                PlayerHitListener.EnvironmentalDeathCause.FREEZING,
                EntityDamageEvent.DamageCause.FREEZE
        ),
        HOT_FLOOR(
                "hot-floor",
                PlayerHitListener.EnvironmentalDeathCause.HOT_FLOOR,
                EntityDamageEvent.DamageCause.HOT_FLOOR
        ),
        CONTACT(
                "contact",
                PlayerHitListener.EnvironmentalDeathCause.CONTACT,
                EntityDamageEvent.DamageCause.CONTACT
        );

        private static final Map<EntityDamageEvent.DamageCause, TrackedSource> BY_CAUSE = new EnumMap<>(
                EntityDamageEvent.DamageCause.class);

        static {
            for (TrackedSource source : values()) {
                for (EntityDamageEvent.DamageCause cause : source.causes) {
                    BY_CAUSE.put(cause, source);
                }
            }
        }

        private final String settingKeyPrefix;
        private final PlayerHitListener.EnvironmentalDeathCause deathCause;
        private final EntityDamageEvent.DamageCause[] causes;

        TrackedSource(String settingKeyPrefix, PlayerHitListener.EnvironmentalDeathCause deathCause, EntityDamageEvent.DamageCause... causes) {
            this.settingKeyPrefix = settingKeyPrefix;
            this.deathCause = deathCause;
            this.causes = causes;
        }

        static TrackedSource fromCause(EntityDamageEvent.DamageCause cause) {
            return BY_CAUSE.get(cause);
        }

        private String setting(String suffix) {
            return "game.environmental-damage." + settingKeyPrefix + "." + suffix;
        }

        boolean isEnabled(HideAndSeek plugin) {
            return plugin.getSettingRegistry().get(setting("enabled"), true);
        }

        int getSafeDurationSeconds(HideAndSeek plugin) {
            return plugin.getSettingRegistry().get(setting("safe-duration-seconds"), 8);
        }

        double getDamageAmount(HideAndSeek plugin) {
            return plugin.getSettingRegistry().get(setting("damage-amount"), 1.0);
        }

        int getDamageCooldownTicks(HideAndSeek plugin) {
            return plugin.getSettingRegistry().get(setting("damage-cooldown-ticks"), 20);
        }

        PlayerHitListener.EnvironmentalDeathCause getDeathCause() {
            return deathCause;
        }
    }

    private static final class DamageStreakState {
        private TrackedSource source;
        private long firstVanillaDamageAtMs;
        private long lastVanillaDamageAtMs;
        private long lastPluginDamageAtMs;

        private DamageStreakState(TrackedSource source, long timestampMs) {
            this.source = source;
            this.firstVanillaDamageAtMs = timestampMs;
            this.lastVanillaDamageAtMs = timestampMs;
            this.lastPluginDamageAtMs = Long.MIN_VALUE / 4;
        }
    }
}

