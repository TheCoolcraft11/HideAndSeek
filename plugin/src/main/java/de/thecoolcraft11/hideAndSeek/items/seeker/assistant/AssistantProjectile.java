package de.thecoolcraft11.hideAndSeek.items.seeker.assistant;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import org.bukkit.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.UUID;

public final class AssistantProjectile {

    public static final String KEY_NAME = "assistant_projectile";
    public static final String SEEKER_KEY_NAME = "assistant_projectile_seeker";
    public static final String ASSISTANT_KEY_NAME = "assistant_projectile_assistant";
    private AssistantProjectile() {
    }

    public static NamespacedKey projectileKey(HideAndSeek plugin) {
        return new NamespacedKey(plugin, KEY_NAME);
    }

    public static NamespacedKey seekerKey(HideAndSeek plugin) {
        return new NamespacedKey(plugin, SEEKER_KEY_NAME);
    }

    public static NamespacedKey assistantKey(HideAndSeek plugin) {
        return new NamespacedKey(plugin, ASSISTANT_KEY_NAME);
    }

    public static void spawn(
            HideAndSeek plugin,
            LivingEntity shooter,
            UUID assistantId,
            Location launchLoc,
            Vector initialVel,
            double gravity,
            double homingDeg,
            double homingRange,
            int lifetimeTicks,
            UUID targetId,
            UUID seekerUUID
    ) {
        if (plugin == null || shooter == null || launchLoc == null || launchLoc.getWorld() == null) {
            return;
        }

        Snowball snowball = launchLoc.getWorld().spawn(launchLoc, Snowball.class, sb -> {
            sb.setShooter(shooter);
            sb.setVelocity(initialVel);
            sb.setGravity(false);
            sb.setItem(new ItemStack(Material.SOUL_TORCH));
            sb.getPersistentDataContainer().set(projectileKey(plugin), PersistentDataType.BOOLEAN, true);
            sb.getPersistentDataContainer().set(seekerKey(plugin), PersistentDataType.STRING, seekerUUID.toString());
            sb.getPersistentDataContainer().set(assistantKey(plugin), PersistentDataType.STRING, assistantId.toString());
        });

        new BukkitRunnable() {
            int ticksAlive = 0;
            int sparkCounter = 0;

            @Override
            public void run() {
                if (!snowball.isValid()) {
                    cancel();
                    return;
                }

                if (++ticksAlive >= lifetimeTicks) {
                    spawnMissEffect(snowball.getLocation());
                    snowball.remove();
                    cancel();
                    return;
                }

                Vector vel = snowball.getVelocity();
                vel.setY(vel.getY() - gravity);
                snowball.setVelocity(vel);

                applyHoming(snowball, targetId, homingDeg, homingRange);

                Location loc = snowball.getLocation();
                loc.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, loc, 2, 0.05, 0.05, 0.05, 0.01);
                loc.getWorld().spawnParticle(Particle.SMOKE, loc, 1, 0.03, 0.03, 0.03, 0.005);

                if (++sparkCounter % 5 == 0) {
                    loc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, loc, 3, 0.08, 0.08, 0.08, 0.02);
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private static void applyHoming(Snowball snowball, UUID targetId, double homingDeg, double homingRange) {
        if (HideAndSeek.getDataController().isHidden(targetId)) {
            return;
        }

        Player target = Bukkit.getPlayer(targetId);
        if (target == null || !target.isOnline()) {
            return;
        }

        Vector toTarget = target.getLocation().add(0.0, 1.0, 0.0).toVector().subtract(snowball.getLocation().toVector());
        if (toTarget.length() > homingRange) {
            return;
        }

        Vector currentVel = snowball.getVelocity();
        double speed = currentVel.length();
        if (speed < 1.0E-6) {
            return;
        }

        double angle = currentVel.angle(toTarget);
        double maxRotation = Math.toRadians(homingDeg);
        if (angle < 0.01) {
            return;
        }

        double t = Math.min(1.0, maxRotation / angle);
        Vector newDir = currentVel.clone().normalize()
                .multiply(1.0 - t)
                .add(toTarget.normalize().multiply(t))
                .normalize()
                .multiply(speed);

        snowball.setVelocity(newDir);
    }

    public static void spawnMissEffect(Location loc) {
        if (loc == null || loc.getWorld() == null) {
            return;
        }

        loc.getWorld().spawnParticle(Particle.SMOKE, loc, 8, 0.2, 0.2, 0.2, 0.01);
        loc.getWorld().spawnParticle(Particle.SOUL, loc, 4, 0.15, 0.15, 0.15, 0.01);
        loc.getWorld().playSound(loc, Sound.BLOCK_GRAVEL_BREAK, 0.5f, 1.2f);
    }
}


