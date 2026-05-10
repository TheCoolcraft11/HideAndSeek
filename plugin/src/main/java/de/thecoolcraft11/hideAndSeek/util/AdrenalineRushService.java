package de.thecoolcraft11.hideAndSeek.util;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.nms.NmsCapabilities;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


public final class AdrenalineRushService {
    private static final long PULSE_PERIOD_TICKS = 40L;
    private final HideAndSeek plugin;
    private final List<UUID> activeHiderRushPlayers = new ArrayList<>();
    private final List<UUID> activeSeekerRushPlayers = new ArrayList<>();
    private boolean hiderRushFired;
    private boolean seekerRushFired;
    private BukkitTask pulseTask;

    public AdrenalineRushService(HideAndSeek plugin) {
        this.plugin = plugin;
    }

    public void onSeekingStart() {
        reset();
        startPulseTask();
    }

    public void onSeekingEnd() {
        stopPulseTask();
        clearAllRushEffects();
        reset();
    }

    public void shutdown() {
        onSeekingEnd();
    }

    public void evaluate(int secondsRemaining) {
        if (!isEnabled()) {
            return;
        }
        if (!"seeking".equals(plugin.getStateManager().getCurrentPhaseId())) {
            return;
        }

        List<Player> activeHiders = getActiveHiders();
        List<Player> activeSeekers = getActiveSeekers();


        if (!hiderRushFired && shouldFireHiderRush(activeHiders)) {
            hiderRushFired = true;
            activateHiderRush(activeHiders);
        }


        if (!seekerRushFired && shouldFireSeekerRush(activeHiders, secondsRemaining)) {
            seekerRushFired = true;
            activateSeekerRush(activeSeekers);
        }
    }

    private boolean shouldFireHiderRush(List<Player> activeHiders) {
        if (!isHiderRushEnabled()) {
            return false;
        }
        int threshold = getHiderTriggerCount();
        return activeHiders.size() <= threshold && !activeHiders.isEmpty();
    }

    private boolean shouldFireSeekerRush(List<Player> activeHiders, int secondsRemaining) {
        if (!isSeekerRushEnabled()) {
            return false;
        }
        if (secondsRemaining < 0) {
            return false;
        }
        int timeThreshold = getSeekerTimeThresholdSeconds();
        int hiderThreshold = getSeekerHiderCountThreshold();
        return secondsRemaining <= timeThreshold && activeHiders.size() >= hiderThreshold;
    }

    private void activateHiderRush(List<Player> hiders) {
        int duration = getHiderDurationSeconds();

        for (Player hider : hiders) {
            applyHiderBuffs(hider, duration);
            applyBorderWarning(hider);
            activeHiderRushPlayers.add(hider.getUniqueId());
        }

        announceHiderRush(hiders);
        scheduleHiderRushExpiry(duration);
    }

    private void activateSeekerRush(List<Player> seekers) {
        int duration = getSeekerDurationSeconds();

        for (Player seeker : seekers) {
            applySeekerBuffs(seeker, duration);
            applyBorderWarning(seeker);
            activeSeekerRushPlayers.add(seeker.getUniqueId());
        }

        announceSeekerRush(seekers);
        scheduleSeekerRushExpiry(duration);
    }

    private void applyHiderBuffs(Player hider, int durationSeconds) {
        int ticks = durationSeconds * 20;

        if (getSettingBool("adrenaline-rush.hider.speed-enabled", true)) {
            int amp = getSettingInt("adrenaline-rush.hider.speed-amplifier", 1);
            hider.addPotionEffect(new PotionEffect(
                    PotionEffectType.SPEED, ticks, amp, false, false, true));
        }

        if (getSettingBool("adrenaline-rush.hider.resistance-enabled", true)) {
            int amp = getSettingInt("adrenaline-rush.hider.resistance-amplifier", 0);
            hider.addPotionEffect(new PotionEffect(
                    PotionEffectType.RESISTANCE, ticks, amp, false, false, true));
        }

        if (getSettingBool("adrenaline-rush.hider.regeneration-enabled", false)) {
            int amp = getSettingInt("adrenaline-rush.hider.regeneration-amplifier", 0);
            hider.addPotionEffect(new PotionEffect(
                    PotionEffectType.REGENERATION, ticks, amp, false, false, true));
        }
    }

    private void applySeekerBuffs(Player seeker, int durationSeconds) {
        int ticks = durationSeconds * 20;

        if (getSettingBool("adrenaline-rush.seeker.speed-enabled", true)) {
            int amp = getSettingInt("adrenaline-rush.seeker.speed-amplifier", 0);
            seeker.addPotionEffect(new PotionEffect(
                    PotionEffectType.SPEED, ticks, amp, false, false, true));
        }

        if (getSettingBool("adrenaline-rush.seeker.haste-enabled", true)) {
            int amp = getSettingInt("adrenaline-rush.seeker.haste-amplifier", 0);
            seeker.addPotionEffect(new PotionEffect(
                    PotionEffectType.HASTE, ticks, amp, false, false, true));
        }

        if (getSettingBool("adrenaline-rush.seeker.regeneration-enabled", false)) {
            int amp = getSettingInt("adrenaline-rush.seeker.regeneration-amplifier", 0);
            seeker.addPotionEffect(new PotionEffect(
                    PotionEffectType.REGENERATION, ticks, amp, false, false, true));
        }
    }

    private void applyBorderWarning(Player player) {
        if (!isBorderWarningEnabled()) {
            return;
        }
        if (!plugin.getNmsAdapter().hasCapability(NmsCapabilities.CLIENT_FAKE_BORDER_WARNING)) {
            return;
        }
        float strength = getBorderStrength();
        plugin.getNmsAdapter().showWarningBorder(player, strength);
    }

    private void clearBorderWarning(Player player) {
        if (!isBorderWarningEnabled()) {
            return;
        }
        if (!plugin.getNmsAdapter().hasCapability(NmsCapabilities.CLIENT_FAKE_BORDER_WARNING)) {
            return;
        }
        plugin.getNmsAdapter().resetWarningBorder(player);
    }

    private void startPulseTask() {
        stopPulseTask();
        pulseTask = new BukkitRunnable() {
            @Override
            public void run() {
                pulseActiveRushPlayers();
            }
        }.runTaskTimer(plugin, PULSE_PERIOD_TICKS, PULSE_PERIOD_TICKS);
    }

    private void stopPulseTask() {
        if (pulseTask != null) {
            pulseTask.cancel();
            pulseTask = null;
        }
    }

    private void pulseActiveRushPlayers() {
        if (!isBorderWarningEnabled()) {
            return;
        }
        if (!plugin.getNmsAdapter().hasCapability(NmsCapabilities.CLIENT_FAKE_BORDER_WARNING)) {
            return;
        }

        float strength = getBorderStrength();

        for (UUID id : activeHiderRushPlayers) {
            Player p = Bukkit.getPlayer(id);
            if (p != null && p.isOnline()) {
                plugin.getNmsAdapter().showWarningBorder(p, strength);
            }
        }

        for (UUID id : activeSeekerRushPlayers) {
            Player p = Bukkit.getPlayer(id);
            if (p != null && p.isOnline()) {
                plugin.getNmsAdapter().showWarningBorder(p, strength);
            }
        }
    }

    private void announceHiderRush(List<Player> hiders) {

        Title title = Title.title(
                Component.text("ADRENALINE RUSH!", NamedTextColor.RED, TextDecoration.BOLD)
                        .decoration(TextDecoration.ITALIC, false),
                Component.text("Survive until the end. Stay hidden!", NamedTextColor.GOLD)
                        .decoration(TextDecoration.ITALIC, false),
                Title.Times.times(
                        Duration.ofMillis(300),
                        Duration.ofMillis(3000),
                        Duration.ofMillis(500))
        );
        for (Player hider : hiders) {
            hider.showTitle(title);
            hider.playSound(hider.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.6f, 1.6f);
            hider.spawnParticle(Particle.SOUL_FIRE_FLAME, hider.getLocation().add(0, 1, 0),
                    20, 0.4, 0.5, 0.4, 0.05);
        }
    }

    private void announceSeekerRush(List<Player> seekers) {
        Title title = Title.title(
                Component.text("ADRENALINE RUSH!", NamedTextColor.YELLOW, TextDecoration.BOLD)
                        .decoration(TextDecoration.ITALIC, false),
                Component.text("Time is almost up - Find them!", NamedTextColor.RED)
                        .decoration(TextDecoration.ITALIC, false),
                Title.Times.times(
                        Duration.ofMillis(300),
                        Duration.ofMillis(3000),
                        Duration.ofMillis(500))
        );
        for (Player seeker : seekers) {
            seeker.showTitle(title);
            seeker.playSound(seeker.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 1.4f);
            seeker.spawnParticle(Particle.ELECTRIC_SPARK, seeker.getLocation().add(0, 1, 0),
                    20, 0.4, 0.5, 0.4, 0.05);
        }
    }

    private void scheduleHiderRushExpiry(int durationSeconds) {
        Bukkit.getScheduler().runTaskLater(plugin, this::expireHiderRush, durationSeconds * 20L);
    }

    private void scheduleSeekerRushExpiry(int durationSeconds) {
        Bukkit.getScheduler().runTaskLater(plugin, this::expireSeekerRush, durationSeconds * 20L);
    }

    private void expireHiderRush() {
        for (UUID id : new ArrayList<>(activeHiderRushPlayers)) {
            Player p = Bukkit.getPlayer(id);
            if (p != null && p.isOnline()) {
                clearBorderWarning(p);
                removeRushBuffsHider(p);
            }
        }
        activeHiderRushPlayers.clear();
    }

    private void expireSeekerRush() {
        for (UUID id : new ArrayList<>(activeSeekerRushPlayers)) {
            Player p = Bukkit.getPlayer(id);
            if (p != null && p.isOnline()) {
                clearBorderWarning(p);
                removeRushBuffsSeeker(p);
            }
        }
        activeSeekerRushPlayers.clear();
    }

    private void removeRushBuffsHider(Player player) {
        if (getSettingBool("adrenaline-rush.hider.speed-enabled", true)) {
            player.removePotionEffect(PotionEffectType.SPEED);
        }
        if (getSettingBool("adrenaline-rush.hider.resistance-enabled", true)) {
            player.removePotionEffect(PotionEffectType.RESISTANCE);
        }
        if (getSettingBool("adrenaline-rush.hider.regeneration-enabled", false)) {
            player.removePotionEffect(PotionEffectType.REGENERATION);
        }
    }

    private void removeRushBuffsSeeker(Player player) {
        if (getSettingBool("adrenaline-rush.seeker.speed-enabled", true)) {
            player.removePotionEffect(PotionEffectType.SPEED);
        }
        if (getSettingBool("adrenaline-rush.seeker.haste-enabled", true)) {
            player.removePotionEffect(PotionEffectType.HASTE);
        }
        if (getSettingBool("adrenaline-rush.seeker.regeneration-enabled", false)) {
            player.removePotionEffect(PotionEffectType.REGENERATION);
        }
    }

    private void clearAllRushEffects() {
        expireHiderRush();
        expireSeekerRush();
    }

    private void reset() {
        hiderRushFired = false;
        seekerRushFired = false;
        activeHiderRushPlayers.clear();
        activeSeekerRushPlayers.clear();
    }

    private List<Player> getActiveHiders() {
        List<Player> result = new ArrayList<>();
        for (UUID id : HideAndSeek.getDataController().getHiders()) {
            Player p = Bukkit.getPlayer(id);
            if (p != null && p.isOnline()
                    && p.getGameMode() != GameMode.SPECTATOR) {
                result.add(p);
            }
        }
        return result;
    }

    private List<Player> getActiveSeekers() {
        List<Player> result = new ArrayList<>();
        for (UUID id : HideAndSeek.getDataController().getSeekers()) {
            Player p = Bukkit.getPlayer(id);
            if (p != null && p.isOnline()
                    && p.getGameMode() != GameMode.SPECTATOR) {
                result.add(p);
            }
        }
        return result;
    }

    private boolean isEnabled() {
        return getSettingBool("adrenaline-rush.enabled", true);
    }

    private boolean isHiderRushEnabled() {
        return getSettingBool("adrenaline-rush.hider.enabled", true);
    }

    private boolean isSeekerRushEnabled() {
        return getSettingBool("adrenaline-rush.seeker.enabled", true);
    }

    private boolean isBorderWarningEnabled() {
        return getSettingBool("adrenaline-rush.border-warning-enabled", true);
    }

    private int getHiderTriggerCount() {
        return getSettingInt("adrenaline-rush.hider.trigger-hider-count", 1);
    }

    private int getHiderDurationSeconds() {
        return getSettingInt("adrenaline-rush.hider.duration-seconds", 30);
    }

    private int getSeekerTimeThresholdSeconds() {
        return getSettingInt("adrenaline-rush.seeker.trigger-time-threshold", 60);
    }

    private int getSeekerHiderCountThreshold() {
        return getSettingInt("adrenaline-rush.seeker.trigger-hider-count", 2);
    }

    private int getSeekerDurationSeconds() {
        return getSettingInt("adrenaline-rush.seeker.duration-seconds", 30);
    }

    private boolean getSettingBool(String key, boolean fallback) {
        Object v = plugin.getSettingRegistry().get(key, fallback);
        return v instanceof Boolean b ? b : fallback;
    }

    private int getSettingInt(String key, int fallback) {
        Object v = plugin.getSettingRegistry().get(key, fallback);
        return v instanceof Number n ? n.intValue() : fallback;
    }

    private float getBorderStrength() {
        Object v = plugin.getSettingRegistry().get("adrenaline-rush.border-warning-strength", (float) 1.0);
        return v instanceof Number n ? n.floatValue() : (float) 1.0;
    }
}