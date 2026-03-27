package de.thecoolcraft11.hideAndSeek.items.effects.impl;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.items.effects.KillEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.entity.WitherSkeleton;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

public class ShadowyReaperKillEffect implements KillEffect {

    @Override
    public void execute(Player killer, Player victim, Location killLocation, HideAndSeek plugin) {
        if (killLocation == null || killLocation.getWorld() == null) {
            return;
        }

        Location center = killLocation.clone().add(0, 0.1, 0);

        center.getWorld().playSound(center, Sound.ENTITY_WITHER_DEATH, 0.75f, 0.82f);
        center.getWorld().playSound(center, Sound.ENTITY_PHANTOM_DEATH, 0.68f, 0.94f);
        center.getWorld().playSound(center, Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 0.4f, 0.7f);

        WitherSkeleton reaper = center.getWorld().spawn(center.clone().add(0, 0.2, 0), WitherSkeleton.class, skeleton -> {
            skeleton.setAI(false);
            skeleton.setSilent(true);
            skeleton.setInvulnerable(true);
            skeleton.setGravity(false);
            skeleton.setCollidable(false);
            skeleton.setRemoveWhenFarAway(true);
            skeleton.getEquipment().setItemInMainHand(new ItemStack(Material.NETHERITE_HOE));
            skeleton.getEquipment().setHelmet(new ItemStack(Material.BLACK_STAINED_GLASS));
        });

        new BukkitRunnable() {
            private int tick;

            @Override
            public void run() {
                if (center.getWorld() == null) {
                    if (reaper.isValid()) {
                        reaper.remove();
                    }
                    cancel();
                    return;
                }

                if (reaper.isValid()) {
                    double orbit = tick * 0.23;
                    Location pose = center.clone().add(Math.cos(orbit) * 0.45, 0.15 + Math.sin(tick * 0.25) * 0.08, Math.sin(orbit) * 0.45);
                    reaper.teleport(pose);
                }

                int smokeCount = 7 + (tick % 3);
                center.getWorld().spawnParticle(Particle.SMOKE, center.clone().add(0, 0.8, 0), smokeCount, 0.8, 0.55, 0.8, 0.06);
                center.getWorld().spawnParticle(Particle.SOUL, center.clone().add(0, 1.0, 0), 5, 0.6, 0.45, 0.6, 0.03);

                if (tick % 5 == 0) {
                    for (int i = 0; i < 16; i++) {
                        double angle = (Math.PI * 2.0) * i / 16.0 + (tick * 0.07);
                        Location sweep = center.clone().add(Math.cos(angle) * 1.45, 0.7, Math.sin(angle) * 1.45);
                        center.getWorld().spawnParticle(Particle.SCULK_SOUL, sweep, 1, 0.01, 0.01, 0.01, 0.02);
                    }
                    center.getWorld().playSound(center, Sound.ENTITY_WITHER_SKELETON_STEP, 0.38f, 0.6f + (tick * 0.01f));
                }

                if (tick++ >= 24) {
                    center.getWorld().playSound(center, Sound.ENTITY_WITHER_SHOOT, 0.7f, 1.2f);
                    center.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, center.clone().add(0, 1.0, 0), 30, 0.8, 0.7, 0.8, 0.04);
                    center.getWorld().spawnParticle(Particle.EXPLOSION, center.clone().add(0, 0.9, 0), 2, 0.2, 0.2, 0.2, 0.01);
                    if (reaper.isValid()) {
                        reaper.remove();
                    }
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }
}
