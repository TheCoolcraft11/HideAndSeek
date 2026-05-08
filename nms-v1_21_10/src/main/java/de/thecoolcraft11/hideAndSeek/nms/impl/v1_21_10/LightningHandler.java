package de.thecoolcraft11.hideAndSeek.nms.impl.v1_21_10;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Location;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.lang.reflect.Constructor;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public final class LightningHandler {


    private static final AtomicInteger CLIENT_ONLY_ENTITY_ID = new AtomicInteger(2_000_000_000);

    private static Object buildPacket(Location location) {
        int entityId = CLIENT_ONLY_ENTITY_ID.incrementAndGet();
        UUID uuid = UUID.randomUUID();
        Vec3 velocity = Vec3.ZERO;
        Object entityType = net.minecraft.world.entity.EntityType.LIGHTNING_BOLT;

        for (Constructor<?> constructor : ClientboundAddEntityPacket.class.getConstructors()) {
            Class<?>[] p = constructor.getParameterTypes();

            try {

                if (p.length == 11
                        && p[0] == int.class
                        && p[1] == UUID.class
                        && p[2] == double.class
                        && p[3] == double.class
                        && p[4] == double.class
                        && p[5] == float.class
                        && p[6] == float.class
                        && p[8] == int.class
                        && p[9] == Vec3.class
                        && p[10] == double.class) {
                    return constructor.newInstance(
                            entityId, uuid,
                            location.getX(), location.getY(), location.getZ(),
                            0f, 0f,
                            entityType,
                            0, velocity,
                            0d
                    );
                }

                if (p.length == 10
                        && p[0] == int.class
                        && p[1] == UUID.class
                        && p[2] == double.class
                        && p[3] == double.class
                        && p[4] == double.class
                        && p[5] == float.class
                        && p[6] == float.class
                        && p[8] == int.class
                        && p[9] == Vec3.class) {
                    return constructor.newInstance(
                            entityId, uuid,
                            location.getX(), location.getY(), location.getZ(),
                            0f, 0f,
                            entityType,
                            0, velocity
                    );
                }
            } catch (Throwable ignored) {
            }
        }

        return null;
    }

    public boolean spawnLightning(Player viewer, Location location) {
        if (viewer == null || location == null || location.getWorld() == null) {
            return false;
        }

        try {
            ServerPlayer serverPlayer = ((CraftPlayer) viewer).getHandle();
            Object packet = buildPacket(location);
            if (packet == null) {
                return false;
            }
            serverPlayer.connection.send((Packet<?>) packet);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }
}
