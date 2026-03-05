package de.thecoolcraft11.hideAndSeek.listener.item;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import org.bukkit.*;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

public class SmokeBombListener implements Listener {

    private final HideAndSeek plugin;

    public SmokeBombListener(HideAndSeek plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {

        if (!(event.getEntity() instanceof Snowball snowball)) {
            return;
        }

        PersistentDataContainer container = snowball.getPersistentDataContainer();

        Boolean isSmokeBomb = container.get(
                new NamespacedKey(plugin, "smoke_bomb"),
                PersistentDataType.BOOLEAN
        );

        if (isSmokeBomb == null || !isSmokeBomb) {
            return;
        }

        int duration = 8;
        int radius = 4;

        Integer storedDuration = container.get(
                new NamespacedKey(plugin, "smoke_bomb_duration"),
                PersistentDataType.INTEGER
        );

        Integer storedRadius = container.get(
                new NamespacedKey(plugin, "smoke_bomb_radius"),
                PersistentDataType.INTEGER
        );

        if (storedDuration != null) duration = storedDuration;
        if (storedRadius != null) radius = storedRadius;

        Location impactLocation;

        if (event.getHitBlock() != null) {
            impactLocation = event.getHitBlock().getLocation().add(0.5, 1, 0.5);
        } else if (event.getHitEntity() != null) {
            impactLocation = event.getHitEntity().getLocation();
        } else {
            impactLocation = snowball.getLocation();
        }

        World world = impactLocation.getWorld();
        if (world == null) return;

        world.playSound(impactLocation, Sound.BLOCK_FIRE_EXTINGUISH, 1.5f, 0.5f);


        world.spawnParticle(
                Particle.CLOUD,
                impactLocation,
                500,
                1.2, 1.0, 1.2,
                0.02
        );

        final int finalDuration = duration;
        final int finalRadius = radius;

        new BukkitRunnable() {

            int ticks = 0;
            final int maxTicks = finalDuration * 20;

            @Override
            public void run() {

                if (ticks >= maxTicks) {
                    cancel();
                    return;
                }

                double progress = (double) ticks / maxTicks;


                double densityMultiplier = 1.0 - (progress * 0.6);

                int coreAmount = (int) (350 * densityMultiplier);
                int swirlAmount = (int) (200 * densityMultiplier);


                world.spawnParticle(
                        Particle.CLOUD,
                        impactLocation,
                        coreAmount,
                        finalRadius * 0.5,
                        1.5,
                        finalRadius * 0.5,
                        0.01f
                );

                world.spawnParticle(
                        Particle.DUST,
                        impactLocation,
                        coreAmount / 2,
                        finalRadius * 0.5,
                        1.5,
                        finalRadius * 0.5,
                        0,
                        new Particle.DustOptions(Color.fromRGB(120, 120, 120), 2.0f)
                );

                world.spawnParticle(
                        Particle.DUST,
                        impactLocation,
                        coreAmount / 2,
                        finalRadius * 0.5,
                        1.5,
                        finalRadius * 0.5,
                        0,
                        new Particle.DustOptions(Color.fromRGB(160, 160, 160), 5.0f)
                );

                for (int i = 0; i < 20; i++) {

                    double offsetX = (Math.random() - 0.5) * finalRadius;
                    double offsetY = Math.random() * 2;
                    double offsetZ = (Math.random() - 0.5) * finalRadius;

                    Location particleLoc = impactLocation.clone().add(offsetX, offsetY, offsetZ);

                    world.spawnParticle(
                            Particle.CAMPFIRE_COSY_SMOKE,
                            particleLoc,
                            swirlAmount,
                            0.05,
                            0.05,
                            0.05,
                            0.01
                    );
                }

                ticks++;
            }

        }.runTaskTimer(plugin, 1L, 1L);

        snowball.remove();
    }
}