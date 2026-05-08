package de.thecoolcraft11.hideAndSeek.nms.impl.v1_21_11;

import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Location;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.lang.reflect.Method;
import java.util.function.Predicate;


public final class RaycastHandler {


    private static Object invokeGetEntityHitResult(
            Object level,
            net.minecraft.world.entity.Entity shooterHandle,
            Vec3 from, Vec3 to,
            AABB box,
            Predicate<Entity> filter,
            double hitboxInflation) {


        Predicate<net.minecraft.world.entity.Entity> nmsFilter = entity ->
                filter.test(entity.getBukkitEntity());

        for (Method method : ProjectileUtil.class.getDeclaredMethods()) {
            if (!method.getName().equals("getEntityHitResult")) {
                continue;
            }

            Class<?>[] paramTypes = method.getParameterTypes();
            Object[] args = new Object[paramTypes.length];
            int vecIndex = 0;
            boolean valid = true;

            for (int i = 0; i < paramTypes.length; i++) {
                Class<?> type = paramTypes[i];

                if (type.isAssignableFrom(level.getClass())) {
                    args[i] = level;
                } else if (type.isAssignableFrom(shooterHandle.getClass())
                        || type == net.minecraft.world.entity.Entity.class) {
                    args[i] = shooterHandle;
                } else if (type == Vec3.class) {

                    args[i] = vecIndex++ == 0 ? from : to;
                } else if (type == AABB.class) {
                    args[i] = box;
                } else if (Predicate.class.isAssignableFrom(type)) {
                    args[i] = nmsFilter;
                } else if (type == float.class || type == Float.class) {
                    args[i] = (float) hitboxInflation;
                } else if (type == double.class || type == Double.class) {
                    args[i] = hitboxInflation;
                } else {

                    valid = false;
                    break;
                }
            }


            if (!valid || vecIndex < 2) {
                continue;
            }

            try {
                return method.invoke(null, args);
            } catch (Throwable ignored) {
            }
        }

        return null;
    }

    public Entity raycast(
            Player shooter,
            Location start,
            Vector direction,
            double distance,
            double hitboxInflation,
            Predicate<Entity> filter) {

        if (shooter == null || start == null || start.getWorld() == null
                || direction == null || filter == null || distance <= 0) {
            return null;
        }

        try {
            net.minecraft.world.entity.Entity shooterHandle = ((CraftPlayer) shooter).getHandle();
            Vec3 from = new Vec3(start.getX(), start.getY(), start.getZ());
            Vec3 to = from.add(
                    direction.getX() * distance,
                    direction.getY() * distance,
                    direction.getZ() * distance);


            AABB box = shooterHandle.getBoundingBox()
                    .expandTowards(to.subtract(from))
                    .inflate(hitboxInflation);

            Object result = invokeGetEntityHitResult(
                    ((CraftWorld) start.getWorld()).getHandle(),
                    shooterHandle,
                    from, to, box,
                    filter,
                    hitboxInflation
            );

            if (result == null) {
                return null;
            }

            Method getEntity = result.getClass().getMethod("getEntity");
            Object nmsEntity = getEntity.invoke(result);
            if (nmsEntity instanceof net.minecraft.world.entity.Entity nms) {
                return nms.getBukkitEntity();
            }
        } catch (Throwable ignored) {
        }

        return null;
    }
}
