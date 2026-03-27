package de.thecoolcraft11.hideAndSeek.items.effects.impl;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.items.effects.KillEffect;
import org.bukkit.*;
import org.bukkit.entity.Display;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

public class BanHammerKillEffect implements KillEffect {

    @Override
    public void execute(Player killer, Player victim, Location killLocation, HideAndSeek plugin) {
        if (killLocation == null || killLocation.getWorld() == null) {
            return;
        }

        Location base = killLocation.clone().add(0, 0.15, 0).setRotation(0, 0);
        Location start = base.clone().add(0, 5.5, 0).setRotation(0, 0);

        ItemDisplay hammer = base.getWorld().spawn(start, ItemDisplay.class, display -> {
            display.setItemStack(new ItemStack(Material.MACE));
            display.setBrightness(new Display.Brightness(15, 15));
            display.setViewRange(36.0f);
            display.setInterpolationDuration(1);
            display.setTransformation(new Transformation(
                    new Vector3f(0f, 0f, 0f),
                    new AxisAngle4f((float) Math.toRadians(18), 0f, 0f, 1f),
                    new Vector3f(2.6f, 2.6f, 2.6f),
                    new AxisAngle4f((float) Math.toRadians(-45), 0f, 1f, 0f)
            ));
        });

        TextDisplay bannedText = base.getWorld().spawn(base.clone().add(0, 2.4, 0), TextDisplay.class, display -> {
            display.text(net.kyori.adventure.text.Component.text("BANNED"));
            display.setBillboard(Display.Billboard.CENTER);
            display.setSeeThrough(false);
            display.setShadowed(true);
            display.setBrightness(new Display.Brightness(15, 15));
        });

        base.getWorld().playSound(base, Sound.BLOCK_NOTE_BLOCK_BASS, 0.75f, 0.45f);
        base.getWorld().playSound(base, Sound.BLOCK_ANVIL_PLACE, 0.8f, 1.2f);

        new BukkitRunnable() {
            private int tick = 0;
            private boolean impacted = false;

            @Override
            public void run() {
                if (base.getWorld() == null || !hammer.isValid() || !bannedText.isValid()) {
                    if (hammer.isValid()) {
                        hammer.remove();
                    }
                    if (bannedText.isValid()) {
                        bannedText.remove();
                    }
                    cancel();
                    return;
                }

                if (!impacted) {
                    double progress = Math.min(1.0, tick / 11.0);
                    double eased = progress * progress;
                    Location target = start.clone().add(0, -5.4 * eased, 0);
                    hammer.teleport(target);
                    bannedText.teleport(base.clone().add(0, 2.4 + Math.sin(tick * 0.35) * 0.08, 0));

                    if (tick % 2 == 0) {
                        base.getWorld().spawnParticle(Particle.CRIT, hammer.getLocation(), 6, 0.2, 0.2, 0.2, 0.2);
                        base.getWorld().spawnParticle(Particle.DUST,
                                hammer.getLocation(),
                                4,
                                0.15,
                                0.15,
                                0.15,
                                0.0,
                                new Particle.DustOptions(Color.fromRGB(210, 216, 230), 1.1f));
                    }

                    if (progress >= 1.0) {
                        impacted = true;
                        base.getWorld().playSound(base, Sound.BLOCK_ANVIL_LAND, 1.4f, 0.72f);
                        base.getWorld().playSound(base, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 1.0f, 0.58f);
                        base.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, base.clone().add(0, 0.15, 0), 2, 0.15, 0.1, 0.15, 0.01);

                        for (int i = 0; i < 24; i++) {
                            double angle = (Math.PI * 2.0) * i / 24.0;
                            Location ring = base.clone().add(Math.cos(angle) * 1.9, 0.12, Math.sin(angle) * 1.9);
                            base.getWorld().spawnParticle(Particle.BLOCK, ring, 1, 0.02, 0.02, 0.02, 0.08, Material.ANVIL.createBlockData());
                        }
                    }
                } else {
                    double rise = (tick - 12) * 0.06;
                    bannedText.teleport(base.clone().add(0, 2.45 + rise, 0));
                    bannedText.setTextOpacity((byte) Math.max(30, 255 - (tick - 12) * 28));

                    if ((tick - 12) % 2 == 0) {
                        base.getWorld().spawnParticle(Particle.SMOKE, base.clone().add(0, 0.2, 0), 8, 0.4, 0.1, 0.4, 0.05);
                    }

                    if (tick >= 20) {
                        hammer.remove();
                        bannedText.remove();
                        cancel();
                    }
                }

                tick++;
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }
}
