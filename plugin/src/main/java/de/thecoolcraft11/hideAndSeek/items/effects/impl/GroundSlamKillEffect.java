package de.thecoolcraft11.hideAndSeek.items.effects.impl;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.items.effects.KillEffect;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Display;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class GroundSlamKillEffect implements KillEffect {

    private static final int DEBRIS_COUNT = 18;
    private static final int SLAM_TICKS = 12;
    private static final int DEBRIS_LIFETIME_TICKS = 275;
    private static final int MIN_GROUNDED_DESPAWN_TICK = SLAM_TICKS + 90;
    private static final int TOTAL_TICKS = SLAM_TICKS + DEBRIS_LIFETIME_TICKS + 8;
    private static final Set<UUID> VISUAL_DEBRIS_IDS = ConcurrentHashMap.newKeySet();
    private static volatile boolean listenerRegistered;

    private static void playImpact(World world, Location center) {
        world.playSound(center, Sound.BLOCK_ANVIL_LAND, 1.35f, 0.56f);
        world.playSound(center, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 0.74f);
        world.playSound(center, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 0.75f, 0.62f);
        world.spawnParticle(Particle.EXPLOSION_EMITTER, center.clone().add(0, 0.18, 0), 2, 0.18, 0.1, 0.18, 0.01);

        for (int i = 0; i < 30; i++) {
            double angle = (Math.PI * 2.0) * i / 30.0;
            Location ring = center.clone().add(Math.cos(angle) * 2.15, 0.12, Math.sin(angle) * 2.15);
            world.spawnParticle(Particle.BLOCK, ring, 1, 0.03, 0.02, 0.03, 0.03, Material.STONE.createBlockData());
            if (i % 2 == 0) {
                world.spawnParticle(Particle.CLOUD, ring, 1, 0.02, 0.02, 0.02, 0.02);
            }
        }
    }

    private static void spawnDebris(World world, Location impactCenter, List<Debris> debris) {
        ThreadLocalRandom random = ThreadLocalRandom.current();

        for (int i = 0; i < DEBRIS_COUNT; i++) {
            BlockData sampled = sampleNearbyBlockData(world, impactCenter, random);
            double angle = random.nextDouble() * Math.PI * 2.0;
            double speed = 0.24 + random.nextDouble() * 0.22;
            Vector velocity = new Vector(Math.cos(angle) * speed, 0.33 + random.nextDouble() * 0.20, Math.sin(angle) * speed);
            Location start = impactCenter.clone().add(random.nextDouble(-0.35, 0.35), 0.35, random.nextDouble(-0.35, 0.35));

            FallingBlock fallingBlock = world.spawn(start, FallingBlock.class, entity -> {
                entity.setBlockData(sampled);
                entity.setDropItem(false);
                entity.setHurtEntities(false);
                entity.setPersistent(false);
                entity.setVelocity(velocity);
            });
            VISUAL_DEBRIS_IDS.add(fallingBlock.getUniqueId());

            debris.add(new Debris(fallingBlock, sampled));
        }
    }

    private static synchronized void ensureVisualDebrisListener(HideAndSeek plugin) {
        if (listenerRegistered) {
            return;
        }

        Bukkit.getPluginManager().registerEvents(new Listener() {
            @EventHandler(ignoreCancelled = true)
            public void onVisualDebrisChangeBlock(EntityChangeBlockEvent event) {
                UUID entityId = event.getEntity().getUniqueId();
                if (!VISUAL_DEBRIS_IDS.remove(entityId)) {
                    return;
                }

                event.setCancelled(true);
                event.getEntity().remove();
            }
        }, plugin);

        listenerRegistered = true;
    }

    private static BlockData sampleNearbyBlockData(World world, Location center, ThreadLocalRandom random) {
        int centerX = center.getBlockX();
        int centerY = center.getBlockY();
        int centerZ = center.getBlockZ();

        for (int attempt = 0; attempt < 14; attempt++) {
            int x = centerX + random.nextInt(-3, 4);
            int y = centerY - random.nextInt(0, 2);
            int z = centerZ + random.nextInt(-3, 4);
            Block block = world.getBlockAt(x, y, z);
            if (block.getType().isAir() || !block.getType().isSolid()) {
                continue;
            }
            return block.getBlockData();
        }

        return Material.STONE.createBlockData();
    }

    @Override
    public void execute(Player killer, Player victim, Location killLocation, HideAndSeek plugin) {
        if (killLocation == null || killLocation.getWorld() == null) {
            return;
        }

        World world = killLocation.getWorld();
        Location impactCenter = killLocation.clone().add(0, 0.05, 0);
        ensureVisualDebrisListener(plugin);

        ItemDisplay slamMass = world.spawn(impactCenter.clone().add(0, 6.0, 0), ItemDisplay.class, display -> {
            display.setItemStack(new ItemStack(Material.ANVIL));
            display.setViewRange(36.0f);
            display.setBrightness(new Display.Brightness(15, 15));
            display.setInterpolationDuration(1);
            display.setTransformation(new Transformation(
                    new Vector3f(),
                    new AxisAngle4f((float) Math.toRadians(8.0), 0f, 0f, 1f),
                    new Vector3f(1f, 1f, 1f),
                    new AxisAngle4f((float) Math.toRadians(-28.0), 0f, 1f, 0f)
            ));
        });

        world.playSound(impactCenter, Sound.BLOCK_ANVIL_PLACE, 0.95f, 0.72f);
        world.playSound(impactCenter, Sound.ENTITY_IRON_GOLEM_STEP, 0.7f, 0.75f);

        new BukkitRunnable() {
            private final List<Debris> debris = new ArrayList<>();
            private int tick;
            private boolean impacted;

            @Override
            public void run() {
                if (impactCenter.getWorld() == null || (!impacted && !slamMass.isValid())) {
                    cleanup();
                    cancel();
                    return;
                }

                if (!impacted) {
                    double progress = Math.min(1.0, tick / (double) SLAM_TICKS);
                    double eased = progress * progress;
                    Location next = impactCenter.clone().add(0, 6.0 - (5.86 * eased), 0);
                    slamMass.teleport(next);

                    if (tick % 2 == 0) {
                        world.spawnParticle(Particle.CAMPFIRE_COSY_SMOKE, next, 4, 0.2, 0.15, 0.2, 0.02);
                        world.spawnParticle(Particle.CRIT, next, 4, 0.18, 0.18, 0.18, 0.16);
                    }

                    if (progress >= 1.0) {
                        impacted = true;
                        playImpact(world, impactCenter);
                        spawnDebris(world, impactCenter, debris);
                    }
                } else {
                    animateDebris();
                }

                if (++tick >= TOTAL_TICKS) {
                    cleanup();
                    cancel();
                }
            }

            private void animateDebris() {
                for (Debris piece : debris) {
                    if (!piece.entity().isValid()) {
                        continue;
                    }


                    Vector velocity = piece.entity().getVelocity();
                    if (tick % 3 == 0) {
                        velocity.add(new Vector((Math.random() - 0.5) * 0.03, 0, (Math.random() - 0.5) * 0.03));
                        piece.entity().setVelocity(velocity);
                    }

                    if (tick % 2 == 0) {
                        Location loc = piece.entity().getLocation();
                        world.spawnParticle(Particle.BLOCK, loc, 2, 0.05, 0.05, 0.05, 0.01, piece.data());
                        world.spawnParticle(Particle.CLOUD, loc, 1, 0.02, 0.02, 0.02, 0.01);
                    }

                    if (tick >= SLAM_TICKS + DEBRIS_LIFETIME_TICKS || (piece.entity().isOnGround() && tick >= MIN_GROUNDED_DESPAWN_TICK)) {
                        VISUAL_DEBRIS_IDS.remove(piece.entity().getUniqueId());
                        piece.entity().remove();
                    }
                }

                if (tick == SLAM_TICKS + 1) {
                    slamMass.remove();
                }
            }

            private void cleanup() {
                if (slamMass.isValid()) {
                    slamMass.remove();
                }

                for (Debris piece : debris) {
                    if (piece.entity().isValid()) {
                        VISUAL_DEBRIS_IDS.remove(piece.entity().getUniqueId());
                        piece.entity().remove();
                    }
                }
                debris.clear();
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    private record Debris(FallingBlock entity, BlockData data) {
    }
}
