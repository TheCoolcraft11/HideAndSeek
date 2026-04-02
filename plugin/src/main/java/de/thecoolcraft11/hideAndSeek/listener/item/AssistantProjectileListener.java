package de.thecoolcraft11.hideAndSeek.listener.item;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.items.api.ItemStateManager;
import de.thecoolcraft11.hideAndSeek.items.seeker.assistant.AssistantProjectile;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public class AssistantProjectileListener implements Listener {

    private final HideAndSeek plugin;

    public AssistantProjectileListener(HideAndSeek plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!(event.getEntity() instanceof Snowball snowball)) {
            return;
        }

        Boolean tagged = snowball.getPersistentDataContainer().get(
                AssistantProjectile.projectileKey(plugin),
                PersistentDataType.BOOLEAN
        );
        if (!Boolean.TRUE.equals(tagged)) {
            return;
        }

        event.setCancelled(true);

        String seekerStr = snowball.getPersistentDataContainer().get(
                AssistantProjectile.seekerKey(plugin),
                PersistentDataType.STRING
        );
        String assistantStr = snowball.getPersistentDataContainer().get(
                AssistantProjectile.assistantKey(plugin),
                PersistentDataType.STRING
        );

        UUID seekerUUID = parseUuid(seekerStr);
        UUID assistantUUID = parseUuid(assistantStr);

        Location impact = snowball.getLocation();
        snowball.remove();

        double directThreshold = plugin.getSettingRegistry().get("seeker-items.assistant.hit-direct-threshold", 0.6);
        double nearThreshold = plugin.getSettingRegistry().get("seeker-items.assistant.hit-near-threshold", 2.5);
        double stationaryDirect = plugin.getSettingRegistry().get("seeker-items.assistant.hit-direct-threshold-stationary", 1.0);
        double movingDirect = plugin.getSettingRegistry().get("seeker-items.assistant.hit-direct-threshold-moving", 0.35);
        double movingSpeedThreshold = plugin.getSettingRegistry().get("seeker-items.assistant.hit-moving-speed-threshold", 0.08);
        double luckyMovingDirectChance = plugin.getSettingRegistry().get("seeker-items.assistant.hit-moving-lucky-direct-chance", 0.2);

        if (event.getHitEntity() instanceof Player hider
                && HideAndSeek.getDataController().getHiders().contains(hider.getUniqueId())) {
            boolean moving = horizontalSpeed(hider) > movingSpeedThreshold;
            double effectiveDirect = moving ? Math.min(directThreshold, movingDirect) : Math.max(directThreshold, stationaryDirect);
            double dist = impact.distance(hider.getLocation().add(0.0, 1.0, 0.0));
            if (!moving || dist < effectiveDirect || ThreadLocalRandom.current().nextDouble() < luckyMovingDirectChance) {
                handleDirectHit(hider, seekerUUID, assistantUUID, impact);
            } else if (dist < nearThreshold) {
                handleNearHit(hider, seekerUUID, assistantUUID, dist, nearThreshold, directThreshold);
            } else {
                AssistantProjectile.spawnMissEffect(impact);
            }
            return;
        }

        Player nearestHider = null;
        double nearestDist = Double.MAX_VALUE;
        for (UUID hiderId : HideAndSeek.getDataController().getHiders()) {
            Player h = Bukkit.getPlayer(hiderId);
            if (h == null || !h.isOnline()) {
                continue;
            }
            if (!h.getWorld().equals(impact.getWorld())) {
                continue;
            }

            double d = h.getLocation().add(0.0, 1.0, 0.0).distance(impact);
            if (d < nearestDist) {
                nearestDist = d;
                nearestHider = h;
            }
        }

        if (nearestHider != null) {
            boolean moving = horizontalSpeed(nearestHider) > movingSpeedThreshold;
            double effectiveDirect = moving ? Math.min(directThreshold, movingDirect) : Math.max(directThreshold, stationaryDirect);

            if (nearestDist < effectiveDirect) {
                handleDirectHit(nearestHider, seekerUUID, assistantUUID, impact);
                return;
            }
            if (nearestDist < nearThreshold) {
                if (moving && ThreadLocalRandom.current().nextDouble() < luckyMovingDirectChance) {
                    handleDirectHit(nearestHider, seekerUUID, assistantUUID, impact);
                } else {
                    handleNearHit(nearestHider, seekerUUID, assistantUUID, nearestDist, nearThreshold, directThreshold);
                }
                return;
            }
        }

        AssistantProjectile.spawnMissEffect(impact);
    }

    private UUID parseUuid(String value) {
        if (value == null) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private double horizontalSpeed(Player player) {
        var vel = player.getVelocity();
        return Math.sqrt((vel.getX() * vel.getX()) + (vel.getZ() * vel.getZ()));
    }

    private void handleDirectHit(Player hider, UUID seekerUUID, UUID assistantUUID, Location impactLoc) {
        int slowDuration = plugin.getSettingRegistry().get("seeker-items.assistant.hit-direct-slowness-duration", 120);
        int nauseaDuration = plugin.getSettingRegistry().get("seeker-items.assistant.hit-direct-nausea-duration", 100);

        hider.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, slowDuration, 2, false, true, true));
        hider.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, nauseaDuration, 0, false, true, true));


        plugin.getNmsAdapter().sendAssistantBeamToAll(plugin, hider.getLocation(), "alert");


        Location hLoc = hider.getLocation();
        if (hLoc.getWorld() != null) {
            hLoc.getWorld().playSound(hLoc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 0.5f, 1.5f);
        }

        Component directMsg = Component.text()
                .append(Component.text("DIRECT HIT! ", NamedTextColor.RED).decorate(TextDecoration.BOLD))
                .append(Component.text("Assistant projectile hit ", NamedTextColor.GRAY))
                .append(Component.text(hider.getName(), NamedTextColor.RED))
                .append(Component.text("!", NamedTextColor.GRAY))
                .build();

        for (UUID sid : HideAndSeek.getDataController().getSeekers()) {
            Player seeker = Bukkit.getPlayer(sid);
            if (seeker != null && seeker.isOnline()) {
                seeker.sendMessage(directMsg);
            }
        }

        hider.sendMessage(Component.text()
                .append(Component.text("You were hit ", NamedTextColor.RED).decorate(TextDecoration.BOLD))
                .append(Component.text("by the Seeker's Assistant!", NamedTextColor.GRAY))
                .build());

        if (seekerUUID != null) {
            int points = plugin.getSettingRegistry().get("seeker-items.assistant.hit-direct-points", 60);
            HideAndSeek.getDataController().addPoints(seekerUUID, points);
        }

        incrementHitCounter(seekerUUID, assistantUUID, impactLoc);
    }

    private void handleNearHit(Player hider, UUID seekerUUID, UUID assistantUUID, double dist, double nearThreshold, double directThreshold) {
        int slowBase = plugin.getSettingRegistry().get("seeker-items.assistant.hit-near-slowness-base", 80);
        int nauseaBase = plugin.getSettingRegistry().get("seeker-items.assistant.hit-near-nausea-base", 60);

        double t = (dist - directThreshold) / (nearThreshold - directThreshold);
        double factor = 1.0 - (t * 0.8);

        int slowDur = (int) (slowBase * factor);
        int nauseaDur = (int) (nauseaBase * factor);

        hider.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, slowDur, 1, false, true, true));
        hider.addPotionEffect(new PotionEffect(PotionEffectType.NAUSEA, nauseaDur, 0, false, true, true));

        Location hLoc = hider.getLocation();
        hLoc.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, hLoc, 16, 0.4, 0.6, 0.4, 0.04);
        hLoc.getWorld().spawnParticle(Particle.SMOKE, hLoc, 12, 0.3, 0.3, 0.3, 0.02);
        hLoc.getWorld().playSound(hLoc, Sound.ENTITY_BLAZE_HURT, 0.8f, 1.0f);
        hLoc.getWorld().playSound(hLoc, Sound.ENTITY_GENERIC_EXPLODE, 0.3f, 1.5f);

        if (seekerUUID != null) {
            Player seeker = Bukkit.getPlayer(seekerUUID);
            if (seeker != null && seeker.isOnline()) {
                seeker.sendMessage(Component.text()
                        .append(Component.text("NEAR HIT! ", NamedTextColor.YELLOW).decorate(TextDecoration.BOLD))
                        .append(Component.text("Assistant projectile grazed ", NamedTextColor.GRAY))
                        .append(Component.text(hider.getName(), NamedTextColor.YELLOW))
                        .append(Component.text("!", NamedTextColor.GRAY))
                        .build());
            }

            int points = plugin.getSettingRegistry().get("seeker-items.assistant.hit-near-points", 20);
            HideAndSeek.getDataController().addPoints(seekerUUID, points);
        }

        incrementHitCounter(seekerUUID, assistantUUID, hLoc);
    }

    private void incrementHitCounter(UUID seekerUUID, UUID assistantUUID, Location effectLoc) {
        if (assistantUUID == null) {
            if (seekerUUID == null) {
                return;
            }
            List<UUID> assistants = ItemStateManager.activeAssistants.get(seekerUUID);
            if (assistants == null || assistants.isEmpty()) {
                return;
            }
            assistantUUID = assistants.getFirst();
        }

        int hits = ItemStateManager.assistantHitCounts.merge(assistantUUID, 1, Integer::sum);
        int maxHits = plugin.getSettingRegistry().get("seeker-items.assistant.max-hits", 4);
        if (hits < maxHits) {
            return;
        }

        Entity entity = Bukkit.getEntity(assistantUUID);
        if (entity != null) {
            Location loc = entity.getLocation();
            loc.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, loc, 24, 0.4, 0.6, 0.4, 0.05);
            loc.getWorld().spawnParticle(Particle.SMOKE, loc, 18, 0.3, 0.3, 0.3, 0.02);
            loc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, loc, 12, 0.3, 0.3, 0.3, 0.03);
            loc.getWorld().playSound(loc, Sound.ENTITY_WITHER_DEATH, 0.6f, 1.4f);
            loc.getWorld().playSound(loc, Sound.ENTITY_BLAZE_DEATH, 0.5f, 0.8f);
            entity.remove();
        } else if (effectLoc != null && effectLoc.getWorld() != null) {
            effectLoc.getWorld().spawnParticle(Particle.SMOKE, effectLoc, 8, 0.2, 0.2, 0.2, 0.01);
        }

        ItemStateManager.removeAssistant(assistantUUID);

        Component msg = Component.text()
                .append(Component.text("Seeker's Assistant", NamedTextColor.RED).decorate(TextDecoration.BOLD))
                .append(Component.text(" was destroyed (" + maxHits + " hits)!", NamedTextColor.GRAY))
                .build();
        Bukkit.getOnlinePlayers().forEach(player -> player.sendMessage(msg));
    }

}
