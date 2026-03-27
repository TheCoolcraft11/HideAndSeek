package de.thecoolcraft11.hideAndSeek.items.effects.impl;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.items.effects.KillEffect;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class ScoreCrownKillEffect implements KillEffect {

    @Override
    public void execute(Player killer, Player victim, Location killLocation, HideAndSeek plugin) {
        if (killLocation == null || killLocation.getWorld() == null) {
            return;
        }

        int points = HideAndSeek.getDataController().getAllPoints().getOrDefault(killer.getUniqueId(), 0);
        int cappedPoints = Math.clamp(points, 0, 5000);
        double scoreFactor = cappedPoints / 5000.0;
        double radius = 1.1 + scoreFactor * 1.9;
        int orbitPoints = 14 + (cappedPoints / 250);
        int durationTicks = 30 + (cappedPoints / 280);

        float pitch = 0.8f + (float) scoreFactor * 0.5f;
        killLocation.getWorld().playSound(killLocation, Sound.BLOCK_BEACON_POWER_SELECT, 1.0f, pitch);
        killLocation.getWorld().playSound(killLocation, Sound.BLOCK_AMETHYST_BLOCK_RESONATE, 0.8f, pitch + 0.25f);
        killLocation.getWorld().spawnParticle(Particle.FLASH, killLocation.clone().add(0, 1.2, 0), 1, 0, 0, 0, 0, Color.YELLOW);
        killLocation.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, killLocation.clone().add(0, 1.0, 0), 20, 0.7, 0.45, 0.7, 0.03);

        new BukkitRunnable() {
            private int tick;

            @Override
            public void run() {
                if (killLocation.getWorld() == null) {
                    cancel();
                    return;
                }

                double progress = tick / (double) Math.max(1, durationTicks);
                double rotation = tick * 0.34;
                double height = 1.15 + progress * 0.6 + Math.sin(tick * 0.22) * 0.08;

                Particle.DustOptions goldDust = new Particle.DustOptions(org.bukkit.Color.fromRGB(255, 208, 54), 1.2f);
                Particle.DustOptions royalDust = new Particle.DustOptions(org.bukkit.Color.fromRGB(166, 114, 255), 0.9f);

                for (int i = 0; i < orbitPoints; i++) {
                    double angle = ((Math.PI * 2.0) * i / orbitPoints) + rotation;
                    Location point = killLocation.clone().add(Math.cos(angle) * radius, height, Math.sin(angle) * radius);

                    killLocation.getWorld().spawnParticle(Particle.DUST, point, 1, 0.01, 0.01, 0.01, 0, goldDust);
                    if (i % 3 == 0) {
                        killLocation.getWorld().spawnParticle(Particle.ENCHANT, point, 1, 0, 0, 0, 0.01);
                    }
                }

                for (int i = 0; i < 5; i++) {
                    double angle = ((Math.PI * 2.0) * i / 5.0) - (rotation * 1.35);
                    Location jewel = killLocation.clone().add(Math.cos(angle) * (radius * 0.6), height + 0.28, Math.sin(angle) * (radius * 0.6));
                    killLocation.getWorld().spawnParticle(Particle.DUST, jewel, 1, 0.0, 0.0, 0.0, 0, royalDust);
                    killLocation.getWorld().spawnParticle(Particle.END_ROD, jewel, 1, 0.0, 0.0, 0.0, 0.01);
                }

                int beamSteps = 4 + (int) (progress * 8);
                for (int step = 0; step < beamSteps; step++) {
                    Location beam = killLocation.clone().add(0, 0.25 + (step * 0.27), 0);
                    killLocation.getWorld().spawnParticle(Particle.END_ROD, beam, 1, 0.02, 0.02, 0.02, 0.01);
                }

                if (tick % 7 == 0) {
                    killLocation.getWorld().playSound(killLocation, Sound.BLOCK_BELL_RESONATE, 0.45f, 1.4f + (float) progress * 0.3f);
                }

                if (tick++ >= durationTicks) {
                    double finaleRadius = radius * 1.25;
                    int finaleCount = 26 + (int) (scoreFactor * 34);
                    killLocation.getWorld().playSound(killLocation, Sound.ENTITY_ENDER_DRAGON_GROWL, 0.55f, 1.6f);
                    killLocation.getWorld().playSound(killLocation, Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, 1.0f, 1.05f);
                    killLocation.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, killLocation.clone().add(0, 1.2, 0), 2, 0.2, 0.2, 0.2, 0.01);

                    for (int i = 0; i < finaleCount; i++) {
                        double angle = (Math.PI * 2.0) * i / finaleCount;
                        Location ring = killLocation.clone().add(Math.cos(angle) * finaleRadius, 1.3, Math.sin(angle) * finaleRadius);
                        killLocation.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, ring, 1, 0.0, 0.02, 0.0, 0.02);
                        if (i % 2 == 0) {
                            killLocation.getWorld().spawnParticle(Particle.FIREWORK, ring, 1, 0.0, 0.0, 0.0, 0.01);
                        }
                    }

                    cancel();
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }
}
