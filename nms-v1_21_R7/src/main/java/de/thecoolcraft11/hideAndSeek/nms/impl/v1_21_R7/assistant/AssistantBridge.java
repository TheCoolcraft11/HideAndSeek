package de.thecoolcraft11.hideAndSeek.nms.impl.v1_21_R7.assistant;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

public final class AssistantBridge {

    private AssistantBridge() {
    }

    public static double getDoubleSetting(Plugin plugin, String key, double fallback) {
        try {
            Object registry = plugin.getClass().getMethod("getSettingRegistry").invoke(plugin);
            Method get = registry.getClass().getMethod("get", String.class, Object.class);
            Object value = get.invoke(registry, key, fallback);
            if (value instanceof Number number) {
                return number.doubleValue();
            }
        } catch (Throwable ignored) {
        }
        return fallback;
    }

    public static int getIntSetting(Plugin plugin, String key, int fallback) {
        try {
            Object registry = plugin.getClass().getMethod("getSettingRegistry").invoke(plugin);
            Method get = registry.getClass().getMethod("get", String.class, Object.class);
            Object value = get.invoke(registry, key, fallback);
            if (value instanceof Number number) {
                return number.intValue();
            }
        } catch (Throwable ignored) {
        }
        return fallback;
    }

    @SuppressWarnings("unchecked")
    public static List<UUID> getHiders() {
        Object controller = getDataController();
        if (controller == null) {
            return Collections.emptyList();
        }

        try {
            Method method = controller.getClass().getMethod("getHiders");
            Object value = method.invoke(controller);
            if (value instanceof List<?> list) {
                return (List<UUID>) list;
            }
        } catch (Throwable ignored) {
        }
        return Collections.emptyList();
    }

    @SuppressWarnings("unchecked")
    public static List<UUID> getSeekers() {
        Object controller = getDataController();
        if (controller == null) {
            return Collections.emptyList();
        }

        try {
            Method method = controller.getClass().getMethod("getSeekers");
            Object value = method.invoke(controller);
            if (value instanceof List<?> list) {
                return (List<UUID>) list;
            }
        } catch (Throwable ignored) {
        }
        return Collections.emptyList();
    }

    public static boolean isHidden(UUID playerId) {
        Object controller = getDataController();
        if (controller == null || playerId == null) {
            return false;
        }

        try {
            Method method = controller.getClass().getMethod("isHidden", UUID.class);
            Object result = method.invoke(controller, playerId);
            if (result instanceof Boolean bool) {
                return bool;
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    public static Player getOnlinePlayer(UUID playerId) {
        return Bukkit.getPlayer(playerId);
    }

    public static void sendBeam(Plugin plugin, Location location, String color) {
        if (plugin == null || location == null) {
            return;
        }

        try {
            Object nmsAdapter = plugin.getClass().getMethod("getNmsAdapter").invoke(plugin);
            Method sendBeam = nmsAdapter.getClass().getMethod("sendAssistantBeamToAll", Plugin.class, Location.class, String.class);
            sendBeam.invoke(nmsAdapter, plugin, location, color);
        } catch (Throwable ignored) {
        }
    }

    @SuppressWarnings("JavaReflectionInvocation")
    public static void shootProjectile(Plugin plugin, LivingEntity shooter, UUID assistantId, UUID targetId, UUID seekerId) {
        if (plugin == null || shooter == null || targetId == null || seekerId == null) {
            return;
        }

        Player target = getOnlinePlayer(targetId);
        if (target == null || !target.isOnline()) {
            return;
        }

        Location launchLoc = shooter.getEyeLocation();
        Location targetLoc = target.getLocation().add(0.0, 1.0, 0.0);

        double speed = getDoubleSetting(plugin, "seeker-items.assistant.projectile-speed", 0.55);
        double gravity = getDoubleSetting(plugin, "seeker-items.assistant.projectile-gravity", 0.025);
        double homing = getDoubleSetting(plugin, "seeker-items.assistant.projectile-homing", 8.0);
        double homingRange = getDoubleSetting(plugin, "seeker-items.assistant.projectile-homing-range", 25.0);
        int lifetime = getIntSetting(plugin, "seeker-items.assistant.projectile-lifetime", 80);

        Vector initialVelocity = targetLoc.toVector().subtract(launchLoc.toVector()).normalize().multiply(speed);
        double movingSpeedThreshold = getDoubleSetting(plugin, "seeker-items.assistant.hit-moving-speed-threshold", 0.08);
        double horizontalSpeed = Math.sqrt((target.getVelocity().getX() * target.getVelocity().getX())
                + (target.getVelocity().getZ() * target.getVelocity().getZ()));
        boolean moving = horizontalSpeed > movingSpeedThreshold;

        double defaultSpread = getDoubleSetting(plugin, "seeker-items.assistant.projectile-aim-spread", 0.12);
        double stationarySpread = getDoubleSetting(plugin, "seeker-items.assistant.projectile-aim-spread-stationary", 0.04);
        double movingSpread = getDoubleSetting(plugin, "seeker-items.assistant.projectile-aim-spread-moving", 0.16);
        double spread = Math.max(0.0, moving ? movingSpread : stationarySpread);
        if (spread <= 0.0) {
            spread = Math.max(0.0, defaultSpread);
        }
        if (spread > 0.0) {
            Vector jitter = new Vector(
                    ThreadLocalRandom.current().nextGaussian() * spread,
                    ThreadLocalRandom.current().nextGaussian() * (spread * 0.65),
                    ThreadLocalRandom.current().nextGaussian() * spread
            );
            initialVelocity.add(jitter);
            if (initialVelocity.lengthSquared() < 1.0E-6) {
                initialVelocity = targetLoc.toVector().subtract(launchLoc.toVector()).normalize().multiply(speed);
            } else {
                initialVelocity = initialVelocity.normalize().multiply(speed);
            }
        }

        try {
            Class<?> projectileClass = Class.forName("de.thecoolcraft11.hideAndSeek.items.seeker.assistant.AssistantProjectile");
            Method spawn = projectileClass.getMethod(
                    "spawn",
                    Class.forName("de.thecoolcraft11.hideAndSeek.HideAndSeek"),
                    LivingEntity.class,
                    UUID.class,
                    Location.class,
                    Vector.class,
                    double.class,
                    double.class,
                    double.class,
                    int.class,
                    UUID.class,
                    UUID.class
            );

            spawn.invoke(null, plugin, shooter, assistantId, launchLoc, initialVelocity, gravity, homing, homingRange, lifetime, targetId, seekerId);
            return;
        } catch (Throwable ignored) {
        }


        Snowball snowball = launchLoc.getWorld().spawn(launchLoc, Snowball.class);
        snowball.setShooter(shooter);
        snowball.setGravity(true);
        snowball.setVelocity(initialVelocity);
        snowball.getPersistentDataContainer().set(new NamespacedKey(plugin, "assistant_projectile"), PersistentDataType.BOOLEAN, true);
        snowball.getPersistentDataContainer().set(new NamespacedKey(plugin, "assistant_projectile_seeker"), PersistentDataType.STRING, seekerId.toString());
        snowball.getPersistentDataContainer().set(new NamespacedKey(plugin, "assistant_projectile_assistant"), PersistentDataType.STRING, assistantId.toString());

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (snowball.isValid()) {
                snowball.remove();
            }
        }, Math.max(1, lifetime));
    }

    private static Object getDataController() {
        try {
            Class<?> clazz = Class.forName("de.thecoolcraft11.hideAndSeek.HideAndSeek");
            Method method = clazz.getMethod("getDataController");
            return method.invoke(null);
        } catch (Throwable ignored) {
            return null;
        }
    }
}
