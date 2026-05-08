package de.thecoolcraft11.hideAndSeek.nms.impl.v1_21_10;

import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetCameraPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.phys.Vec3;
import org.bukkit.Location;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftEntityType;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class CameraHandler {


    private final Map<UUID, Map<Integer, net.minecraft.world.entity.Entity>> clientCameraEntities;

    private final Map<UUID, Boolean> hadAllowedFlight;

    public CameraHandler(
            Map<UUID, Map<Integer, net.minecraft.world.entity.Entity>> clientCameraEntities,
            Map<UUID, Boolean> hadAllowedFlight) {
        this.clientCameraEntities = clientCameraEntities;
        this.hadAllowedFlight = hadAllowedFlight;
    }

    public int spawnClientEntity(Player viewer, Location location, float yaw, float pitch, EntityType entityType) {
        if (viewer == null || location == null || location.getWorld() == null || !viewer.isOnline()) {
            return Integer.MIN_VALUE;
        }
        if (entityType == null || entityType == EntityType.UNKNOWN) {
            return Integer.MIN_VALUE;
        }

        try {
            ServerPlayer viewerHandle = ((CraftPlayer) viewer).getHandle();
            ServerLevel level = ((CraftWorld) location.getWorld()).getHandle();
            double targetEyeY = location.getY() + 0.16;

            net.minecraft.world.entity.EntityType<?> nmsType = CraftEntityType.bukkitToMinecraft(entityType);
            if (nmsType == null) {
                return Integer.MIN_VALUE;
            }

            net.minecraft.world.entity.Entity fake = nmsType.create(
                    level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
            if (fake == null) {
                return Integer.MIN_VALUE;
            }

            fake.setYRot(yaw);
            fake.setXRot(pitch);
            fake.setYHeadRot(yaw);
            fake.setNoGravity(true);
            fake.setInvulnerable(true);


            if (fake instanceof Creeper creeper) {
                creeper.setNoAi(true);
                creeper.setSilent(true);
                creeper.setInvisible(true);
            }
            if (fake instanceof EnderMan enderMan) {
                enderMan.setNoAi(true);
                enderMan.setSilent(true);
                enderMan.setInvisible(true);
            }
            if (fake instanceof ArmorStand stand) {
                stand.setInvisible(true);
                stand.setMarker(true);
            }


            double eyeHeight = fake.getEyeHeight(fake.getPose());
            double spawnY = targetEyeY - eyeHeight;
            fake.setPos(location.getX(), spawnY, location.getZ());


            ClientboundAddEntityPacket addPacket = new ClientboundAddEntityPacket(
                    fake.getId(),
                    fake.getUUID(),
                    location.getX(),
                    spawnY,
                    location.getZ(),
                    pitch,
                    yaw,
                    fake.getType(),
                    0,
                    Vec3.ZERO,
                    yaw
            );

            List<SynchedEntityData.DataValue<?>> values = fake.getEntityData().getNonDefaultValues();
            if (values == null) {
                values = List.of();
            }

            viewerHandle.connection.send(addPacket);
            viewerHandle.connection.send(new ClientboundSetEntityDataPacket(fake.getId(), values));


            clientCameraEntities
                    .computeIfAbsent(viewer.getUniqueId(), ignored -> new ConcurrentHashMap<>())
                    .put(fake.getId(), fake);

            return fake.getId();
        } catch (Throwable ignored) {
            return Integer.MIN_VALUE;
        }
    }

    public void removeClientEntity(Player viewer, int entityId) {
        if (viewer == null || !viewer.isOnline() || entityId == Integer.MIN_VALUE) {
            return;
        }

        try {
            ServerPlayer viewerHandle = ((CraftPlayer) viewer).getHandle();
            viewerHandle.connection.send(new ClientboundRemoveEntitiesPacket(entityId));

            Map<Integer, net.minecraft.world.entity.Entity> byId = clientCameraEntities.get(viewer.getUniqueId());
            if (byId != null) {
                byId.remove(entityId);
                if (byId.isEmpty()) {
                    clientCameraEntities.remove(viewer.getUniqueId());
                }
            }
        } catch (Throwable ignored) {
        }
    }

    public void setCamera(Player viewer, int entityId) {
        if (viewer == null || !viewer.isOnline() || entityId == Integer.MIN_VALUE) {
            return;
        }

        try {
            Map<Integer, net.minecraft.world.entity.Entity> byId = clientCameraEntities.get(viewer.getUniqueId());
            if (byId == null) {
                return;
            }
            net.minecraft.world.entity.Entity cameraEntity = byId.get(entityId);
            if (cameraEntity == null) {
                return;
            }

            ServerPlayer viewerHandle = ((CraftPlayer) viewer).getHandle();
            viewerHandle.connection.send(new ClientboundSetCameraPacket(cameraEntity));
        } catch (Throwable ignored) {
        }


        hadAllowedFlight.putIfAbsent(viewer.getUniqueId(), viewer.getAllowFlight());
        viewer.setAllowFlight(true);
    }

    public void resetCamera(Player viewer) {
        if (viewer == null || !viewer.isOnline()) {
            return;
        }

        try {
            ServerPlayer serverPlayer = ((CraftPlayer) viewer).getHandle();
            serverPlayer.connection.send(new ClientboundSetCameraPacket(serverPlayer));


            Map<Integer, net.minecraft.world.entity.Entity> byId =
                    clientCameraEntities.remove(viewer.getUniqueId());
            if (byId != null && !byId.isEmpty()) {
                int[] ids = byId.keySet().stream().mapToInt(Integer::intValue).toArray();
                serverPlayer.connection.send(new ClientboundRemoveEntitiesPacket(ids));
            }
        } catch (Throwable ignored) {
        }

        Boolean hadFlight = hadAllowedFlight.remove(viewer.getUniqueId());
        if (hadFlight != null) {
            viewer.setAllowFlight(hadFlight);
            if (!hadFlight) {
                viewer.setFlying(false);
            }
        }
    }

    public void clearAll() {
        clientCameraEntities.clear();
        hadAllowedFlight.clear();
    }
}
