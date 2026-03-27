package de.thecoolcraft11.hideAndSeek.items.effects.impl;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.items.effects.KillEffect;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.scheduler.BukkitRunnable;

public class VitalityWaveKillEffect implements KillEffect {

    private static void spawnWaveLayer(Location center, double radius, double yOffset, int count, Particle.DustOptions dustOptions) {
        if (center.getWorld() == null || count <= 0) {
            return;
        }

        for (int i = 0; i < count; i++) {
            double angle = (Math.PI * 2.0) * i / count;
            double wobble = Math.sin(i * 0.7) * 0.1;
            Location point = center.clone().add(Math.cos(angle) * (radius + wobble), yOffset + Math.cos(angle * 2.0) * 0.14, Math.sin(angle) * (radius + wobble));
            center.getWorld().spawnParticle(Particle.DUST, point, 1, 0.01, 0.01, 0.01, 0.0, dustOptions);
        }
    }

    private static double clamp01(double value) {
        return Math.clamp(value, 0.0, 1.0);
    }

    private static Color mix(Color first, Color second, double t) {
        double clamped = clamp01(t);
        int red = (int) Math.round(first.getRed() + (second.getRed() - first.getRed()) * clamped);
        int green = (int) Math.round(first.getGreen() + (second.getGreen() - first.getGreen()) * clamped);
        int blue = (int) Math.round(first.getBlue() + (second.getBlue() - first.getBlue()) * clamped);
        return Color.fromRGB(red, green, blue);
    }

    @Override
    public void execute(Player killer, Player victim, Location killLocation, HideAndSeek plugin) {
        if (killLocation == null || killLocation.getWorld() == null) {
            return;
        }

        int points = HideAndSeek.getDataController().getAllPoints().getOrDefault(killer.getUniqueId(), 0);
        double scoreFactor = Math.clamp(points / 2500.0, 0.0, 1.0);
        double speedFactor = Math.min(1.2, killer.getVelocity().length());

        int nearbyPlayers = (int) killLocation.getWorld().getNearbyEntities(killLocation, 9.0, 5.0, 9.0).stream()
                .filter(Player.class::isInstance)
                .map(Player.class::cast)
                .filter(player -> !player.getUniqueId().equals(killer.getUniqueId()))
                .count();
        int nearbyProjectiles = (int) killLocation.getWorld().getNearbyEntities(killLocation, 10.0, 5.0, 10.0).stream()
                .filter(Projectile.class::isInstance)
                .count();

        double pressureFactor = Math.min(1.0, (nearbyPlayers * 0.3) + (nearbyProjectiles * 0.12));
        double intensity = 1.0 + (pressureFactor * 1.15) + (speedFactor * 0.55) + (scoreFactor * 0.7);

        float pitch = (float) Math.min(2.0, 0.9 + (pressureFactor * 0.35) + (scoreFactor * 0.32));
        killLocation.getWorld().playSound(killLocation, Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 0.9f, pitch);
        killLocation.getWorld().playSound(killLocation, Sound.ENTITY_WARDEN_HEARTBEAT, 0.45f, 1.0f + (float) (pressureFactor * 0.2));
        killLocation.getWorld().playSound(killLocation, Sound.BLOCK_BEACON_ACTIVATE, 0.55f, 1.18f + (float) (scoreFactor * 0.2));

        AreaEffectCloud innerWave = killLocation.getWorld().spawn(killLocation.clone().add(0, 0.15, 0), AreaEffectCloud.class, cloud -> {
            cloud.setRadius(0.7f);
            cloud.setRadiusPerTick(0.075f);
            cloud.setDuration(26);
            cloud.setParticle(Particle.ENTITY_EFFECT, Color.fromRGB(78, 250, 198));
            cloud.setSource(killer);
        });
        AreaEffectCloud outerWave = killLocation.getWorld().spawn(killLocation.clone().add(0, 0.1, 0), AreaEffectCloud.class, cloud -> {
            cloud.setRadius(1.2f);
            cloud.setRadiusPerTick(0.11f);
            cloud.setDuration(30);
            cloud.setColor(Color.fromRGB(124, 116, 255));
            cloud.setParticle(Particle.TOTEM_OF_UNDYING);
            cloud.setSource(killer);
        });

        int baseBursts = 20 + (int) Math.round(pressureFactor * 11.0) + (int) Math.round(scoreFactor * 12.0);
        Color core = mix(Color.fromRGB(74, 252, 198), Color.fromRGB(104, 136, 255), clamp01(pressureFactor));
        Color edge = mix(Color.fromRGB(255, 92, 163), Color.fromRGB(128, 74, 255), clamp01(scoreFactor));

        spawnWaveLayer(killLocation, 0.95 + intensity * 0.35, 0.35, baseBursts, new Particle.DustOptions(core, 1.15f));
        spawnWaveLayer(killLocation, 1.45 + intensity * 0.45, 0.6, baseBursts + 8, new Particle.DustOptions(edge, 1.25f));

        int sparks = 12 + (int) Math.round(pressureFactor * 10) + (int) (speedFactor * 8);
        killLocation.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, killLocation.clone().add(0, 1.0, 0), sparks, 0.6, 0.45, 0.6, 0.12);
        killLocation.getWorld().spawnParticle(Particle.END_ROD, killLocation.clone().add(0, 1.1, 0), 8 + nearbyPlayers, 0.45, 0.55, 0.45, 0.04);


        new BukkitRunnable() {
            private int pulse;

            @Override
            public void run() {
                if (killLocation.getWorld() == null) {
                    cancel();
                    return;
                }

                double pulseRadius = 1.15 + pulse * 0.65 + intensity * 0.25;
                int pulseCount = 14 + (int) (pressureFactor * 9.0) + pulse * 4;
                float size = (float) (1.0 + pulse * 0.22);
                Particle.DustOptions pulseDust = new Particle.DustOptions(edge, size);

                spawnWaveLayer(killLocation, pulseRadius, 0.45 + pulse * 0.2, pulseCount, pulseDust);
                killLocation.getWorld().spawnParticle(Particle.CRIT, killLocation.clone().add(0, 0.9 + pulse * 0.2, 0), 8 + nearbyPlayers, 0.35, 0.25, 0.35, 0.08);
                if (pulse == 1) {
                    killLocation.getWorld().playSound(killLocation, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, 0.35f, 1.25f);
                }

                if (++pulse >= 3) {
                    if (innerWave.isValid()) {
                        innerWave.remove();
                    }
                    if (outerWave.isValid()) {
                        outerWave.remove();
                    }
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 4L, 4L);
    }

}
