package de.thecoolcraft11.hideAndSeek.nms.impl.v1_21_R7;

import de.thecoolcraft11.hideAndSeek.nms.NmsAdapter;
import de.thecoolcraft11.hideAndSeek.nms.NmsCapabilities;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import net.minecraft.core.BlockPos;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.*;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.block.data.CraftBlockData;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.craftbukkit.entity.CraftEntityType;
import org.bukkit.craftbukkit.entity.CraftMob;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

public class NmsAdapterImpl implements NmsAdapter {

    private static final AtomicInteger CLIENT_ONLY_ENTITY_ID = new AtomicInteger(2_000_000_000);
    private static final String FILTER_PREFIX = "has_anticheat_filter_";
    private static final Set<NmsCapabilities> CAPS =
            EnumSet.of(
                    NmsCapabilities.BLOCK_VOXEL_SHAPE,
                    NmsCapabilities.MOB_PATHFINDING,
                    NmsCapabilities.CLIENT_GAMEMODE_SPOOFING,
                    NmsCapabilities.NO_CLIP_MOB,
                    NmsCapabilities.CLIENT_LIGHTNING_PACKET,
                    NmsCapabilities.PROJECTILE_ENTITY_RAYCAST,
                    NmsCapabilities.ANTI_CHEAT_PACKET_FILTER,
                    NmsCapabilities.CLIENT_ENTITY_SPAWNING,
                    NmsCapabilities.CLIENT_ENTITY_GLOWING,
                    NmsCapabilities.CLIENT_CAMERA_SPOOFING
            );
    private final Map<UUID, Set<Integer>> blockedEntityIdsByViewer = new ConcurrentHashMap<>();
    private final Map<UUID, Map<Integer, net.minecraft.world.entity.Entity>> clientCameraEntities = new ConcurrentHashMap<>();

    @Override
    public String name() {
        return "v1_21_R7";
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public boolean isCompatible(String version) {
        return version.equals("1.21.10") || version.equals("1.21.11");
    }

    @Override
    public Set<NmsCapabilities> capabilities() {
        return CAPS;
    }

    @Override
    public List<BoundingBox> getBoundingBoxes(BlockData blockData, Location loc) {

        BlockState nmsState = ((CraftBlockData) blockData).getState();

        BlockPos pos = new BlockPos(
                loc.getBlockX(),
                loc.getBlockY(),
                loc.getBlockZ()
        );

        var world = ((CraftWorld) loc.getWorld()).getHandle();

        var shape = nmsState.getShape(
                world,
                pos,
                net.minecraft.world.phys.shapes.CollisionContext.empty()
        );

        List<BoundingBox> result = new ArrayList<>();

        for (AABB aabb : shape.toAabbs()) {

            BoundingBox bb = new BoundingBox(
                    loc.getX() + aabb.minX,
                    loc.getY() + aabb.minY,
                    loc.getZ() + aabb.minZ,
                    loc.getX() + aabb.maxX,
                    loc.getY() + aabb.maxY,
                    loc.getZ() + aabb.maxZ
            );

            result.add(bb);
        }

        return result;
    }


    @Override
    public boolean canPathfind(org.bukkit.entity.Mob mob, Location start, Location end) {

        if (!start.getWorld().equals(end.getWorld())) {
            return false;
        }

        CraftMob craftMob = (CraftMob) mob;

        try {

            craftMob.getHandle().getNavigation().recomputePath();

            var pathfinder = craftMob.getPathfinder();

            var path = pathfinder.findPath(end);

            if (path == null || path.getFinalPoint() == null) {
                return false;
            }

            return path.getFinalPoint().distanceSquared(end) < 6.0;

        } catch (Exception ignored) {
            return false;
        }
    }


    @Override
    public void setServerGameModeSpectator(Player player) {

        ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
        serverPlayer.setGameMode(GameType.SPECTATOR);
    }

    @Override
    public void spoofClientGameMode(Player player, GameMode mode) {

        ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();

        float value;

        switch (mode) {
            case CREATIVE -> value = 1f;
            case ADVENTURE -> value = 2f;
            case SPECTATOR -> value = 3f;
            default -> value = 0f;
        }

        serverPlayer.connection.send(
                new ClientboundGameEventPacket(
                        ClientboundGameEventPacket.CHANGE_GAME_MODE,
                        value
                )
        );
    }

    @Override
    public void setNoClipForEntity(Entity entity, boolean noClip) {
        net.minecraft.world.entity.Entity serverEntity = ((CraftEntity) entity).getHandle();

        serverEntity.noPhysics = noClip;
    }

    private static Object tryProjectileUtilHit(Object level, net.minecraft.world.entity.Entity shooterHandle, Vec3 from, Vec3 to,
                                               AABB box, Predicate<Entity> filter, double hitboxInflation) {
        Predicate<net.minecraft.world.entity.Entity> nmsFilter = entity -> {
            Entity bukkit = entity.getBukkitEntity();
            return filter.test(bukkit);
        };

        for (Method method : ProjectileUtil.class.getDeclaredMethods()) {
            if (!method.getName().equals("getEntityHitResult")) {
                continue;
            }

            Class<?>[] parameterTypes = method.getParameterTypes();
            Object[] args = new Object[parameterTypes.length];
            int vecIndex = 0;
            boolean valid = true;

            for (int i = 0; i < parameterTypes.length; i++) {
                Class<?> type = parameterTypes[i];

                if (type.isAssignableFrom(level.getClass())) {
                    args[i] = level;
                } else if (type.isAssignableFrom(shooterHandle.getClass()) || type == net.minecraft.world.entity.Entity.class) {
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

    private static Object buildClientLightningPacket(Location location) {
        int entityId = CLIENT_ONLY_ENTITY_ID.incrementAndGet();
        UUID uuid = UUID.randomUUID();
        Vec3 velocity = Vec3.ZERO;
        Object entityType = net.minecraft.world.entity.EntityType.LIGHTNING_BOLT;

        for (Constructor<?> constructor : ClientboundAddEntityPacket.class.getConstructors()) {
            Class<?>[] parameterTypes = constructor.getParameterTypes();

            try {
                if (parameterTypes.length == 11
                        && parameterTypes[0] == int.class
                        && parameterTypes[1] == UUID.class
                        && parameterTypes[2] == double.class
                        && parameterTypes[3] == double.class
                        && parameterTypes[4] == double.class
                        && parameterTypes[5] == float.class
                        && parameterTypes[6] == float.class
                        && parameterTypes[8] == int.class
                        && parameterTypes[9] == Vec3.class
                        && parameterTypes[10] == double.class) {
                    return constructor.newInstance(
                            entityId,
                            uuid,
                            location.getX(),
                            location.getY(),
                            location.getZ(),
                            0f,
                            0f,
                            entityType,
                            0,
                            velocity,
                            0d
                    );
                }

                if (parameterTypes.length == 10
                        && parameterTypes[0] == int.class
                        && parameterTypes[1] == UUID.class
                        && parameterTypes[2] == double.class
                        && parameterTypes[3] == double.class
                        && parameterTypes[4] == double.class
                        && parameterTypes[5] == float.class
                        && parameterTypes[6] == float.class
                        && parameterTypes[8] == int.class
                        && parameterTypes[9] == Vec3.class) {
                    return constructor.newInstance(
                            entityId,
                            uuid,
                            location.getX(),
                            location.getY(),
                            location.getZ(),
                            0f,
                            0f,
                            entityType,
                            0,
                            velocity
                    );
                }
            } catch (Throwable ignored) {
            }
        }

        return null;
    }

    private static boolean shouldDropPacket(Object msg, Set<Integer> blockedEntityIds) {
        if (!(msg instanceof Packet<?> packet)) {
            return false;
        }

        if (packet instanceof ClientboundAddEntityPacket addEntityPacket) {
            return blockedEntityIds.contains(addEntityPacket.getId());
        }

        try {
            Method subPackets = packet.getClass().getMethod("subPackets");
            Object nested = subPackets.invoke(packet);
            if (nested instanceof Iterable<?> iterable) {
                for (Object child : iterable) {
                    if (shouldDropPacket(child, blockedEntityIds)) {
                        return true;
                    }
                }
            }
        } catch (Throwable ignored) {
        }

        return false;
    }

    private static Channel getChannel(ServerPlayer viewerHandle) {
        try {
            Field serverConnectionField = viewerHandle.connection.getClass().getSuperclass().getDeclaredField("connection");
            serverConnectionField.setAccessible(true);
            Connection connection = (Connection) serverConnectionField.get(viewerHandle.connection);

            Field channelField = Connection.class.getDeclaredField("channel");
            channelField.setAccessible(true);
            return (Channel) channelField.get(connection);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static boolean sendPairingData(ServerPlayer viewerHandle, ServerPlayer targetHandle) {
        try {
            ServerLevel level = targetHandle.level();
            Object trackedEntity = getTrackedEntity(level, targetHandle.getId());
            if (trackedEntity == null) {
                return false;
            }

            Field serverEntityField = trackedEntity.getClass().getDeclaredField("serverEntity");
            serverEntityField.setAccessible(true);
            ServerEntity serverEntity = (ServerEntity) serverEntityField.get(trackedEntity);
            if (serverEntity == null) {
                return false;
            }

            serverEntity.sendPairingData(viewerHandle, packet -> viewerHandle.connection.send(packet));
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static Object getTrackedEntity(ServerLevel level, int entityId) {
        try {
            Object chunkMap = level.getChunkSource().chunkMap;
            Field entityMapField = chunkMap.getClass().getDeclaredField("entityMap");
            entityMapField.setAccessible(true);
            Object entityMap = entityMapField.get(chunkMap);
            Method getMethod = entityMap.getClass().getMethod("get", int.class);
            return getMethod.invoke(entityMap, entityId);
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Override
    public boolean spawnClientLightning(Player viewer, Location location) {
        if (viewer == null || location == null || location.getWorld() == null) {
            return false;
        }

        try {
            ServerPlayer serverPlayer = ((CraftPlayer) viewer).getHandle();
            Object packet = buildClientLightningPacket(location);
            if (packet == null) {
                return false;
            }
            serverPlayer.connection.send((Packet<?>) packet);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    @Override
    public Entity raycastEntityHit(Player shooter, Location start, Vector direction, double distance, double hitboxInflation, Predicate<Entity> filter) {
        if (shooter == null || start == null || start.getWorld() == null || direction == null || filter == null || distance <= 0) {
            return null;
        }

        try {
            net.minecraft.world.entity.Entity shooterHandle = ((CraftPlayer) shooter).getHandle();
            Vec3 from = new Vec3(start.getX(), start.getY(), start.getZ());
            Vec3 to = from.add(direction.getX() * distance, direction.getY() * distance, direction.getZ() * distance);
            AABB box = shooterHandle.getBoundingBox().expandTowards(to.subtract(from)).inflate(hitboxInflation);

            Object result = tryProjectileUtilHit(
                    ((CraftWorld) start.getWorld()).getHandle(),
                    shooterHandle,
                    from,
                    to,
                    box,
                    filter,
                    hitboxInflation
            );

            if (result == null) {
                return null;
            }

            Method getEntityMethod = result.getClass().getMethod("getEntity");
            Object nmsEntity = getEntityMethod.invoke(result);
            if (nmsEntity instanceof net.minecraft.world.entity.Entity nms) {
                return nms.getBukkitEntity();
            }
        } catch (Throwable ignored) {
            return null;
        }

        return null;
    }

    @Override
    public boolean setEntityVisibilityForViewer(Player viewer, Player target, boolean visible) {
        if (viewer == null || target == null || !viewer.isOnline() || !target.isOnline()) {
            return false;
        }

        if (viewer.getUniqueId().equals(target.getUniqueId())) {
            return true;
        }

        try {
            ServerPlayer viewerHandle = ((CraftPlayer) viewer).getHandle();
            int targetEntityId = ((CraftPlayer) target).getHandle().getId();

            ensurePacketFilterInstalled(viewerHandle, viewer.getUniqueId());
            Set<Integer> blocked = blockedEntityIdsByViewer.computeIfAbsent(viewer.getUniqueId(), ignored -> ConcurrentHashMap.newKeySet());

            if (visible) {
                boolean changed = blocked.remove(targetEntityId);
                if (blocked.isEmpty()) {
                    blockedEntityIdsByViewer.remove(viewer.getUniqueId());
                }

                if (changed) {
                    return sendPairingData(viewerHandle, ((CraftPlayer) target).getHandle());
                }
                return true;
            }

            boolean changed = blocked.add(targetEntityId);
            if (changed) {
                viewerHandle.connection.send(new ClientboundRemoveEntitiesPacket(targetEntityId));
            }
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private static EntityDataAccessor<Byte> getSharedFlagsAccessor() {
        try {
            Field field = net.minecraft.world.entity.Entity.class.getDeclaredField("DATA_SHARED_FLAGS_ID");
            field.setAccessible(true);
            return (EntityDataAccessor<Byte>) field.get(null);
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Override
    public int spawnClientCameraEntity(Player viewer, Location location, float yaw, float pitch, EntityType entityType) {
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

            net.minecraft.world.entity.Entity fake = nmsType.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
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

    @Override
    public boolean removeClientEntity(Player viewer, int entityId) {
        if (viewer == null || !viewer.isOnline() || entityId == Integer.MIN_VALUE) {
            return false;
        }

        try {
            ServerPlayer viewerHandle = ((CraftPlayer) viewer).getHandle();
            viewerHandle.connection.send(new ClientboundRemoveEntitiesPacket(entityId));

            Map<Integer, net.minecraft.world.entity.Entity> byEntityId = clientCameraEntities.get(viewer.getUniqueId());
            if (byEntityId != null) {
                byEntityId.remove(entityId);
                if (byEntityId.isEmpty()) {
                    clientCameraEntities.remove(viewer.getUniqueId());
                }
            }

            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    @Override
    public boolean setCameraEntity(Player viewer, int entityId) {
        if (viewer == null || !viewer.isOnline() || entityId == Integer.MIN_VALUE) {
            return false;
        }

        try {
            Map<Integer, net.minecraft.world.entity.Entity> byEntityId = clientCameraEntities.get(viewer.getUniqueId());
            if (byEntityId == null) {
                return false;
            }

            net.minecraft.world.entity.Entity cameraEntity = byEntityId.get(entityId);
            if (cameraEntity == null) {
                return false;
            }

            ServerPlayer viewerHandle = ((CraftPlayer) viewer).getHandle();
            viewerHandle.connection.send(new ClientboundSetCameraPacket(cameraEntity));
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    @Override
    public boolean resetCamera(Player viewer) {
        if (viewer == null || !viewer.isOnline()) {
            return false;
        }

        try {
            ServerPlayer serverPlayer = ((CraftPlayer) viewer).getHandle();
            serverPlayer.connection.send(new ClientboundSetCameraPacket(serverPlayer));

            Map<Integer, net.minecraft.world.entity.Entity> byEntityId = clientCameraEntities.remove(viewer.getUniqueId());
            if (byEntityId != null && !byEntityId.isEmpty()) {
                int[] ids = byEntityId.keySet().stream().mapToInt(Integer::intValue).toArray();
                serverPlayer.connection.send(new ClientboundRemoveEntitiesPacket(ids));
            }
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    @Override
    public boolean setEntityGlowingForViewer(Player viewer, Player target, boolean glowing) {
        if (viewer == null || target == null || !viewer.isOnline() || !target.isOnline()) {
            return false;
        }

        try {
            ServerPlayer viewerHandle = ((CraftPlayer) viewer).getHandle();
            ServerPlayer targetHandle = ((CraftPlayer) target).getHandle();

            EntityDataAccessor<Byte> sharedFlagsAccessor = getSharedFlagsAccessor();
            if (sharedFlagsAccessor == null) {
                return false;
            }

            byte flags = targetHandle.getEntityData().get(sharedFlagsAccessor);
            byte updated = glowing ? (byte) (flags | 0x40) : (byte) (flags & ~0x40);

            SynchedEntityData.DataValue<Byte> value = SynchedEntityData.DataValue.create(
                    sharedFlagsAccessor,
                    updated
            );

            viewerHandle.connection.send(new ClientboundSetEntityDataPacket(targetHandle.getId(), List.of(value)));
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    @Override
    public void clearVisibilityFilters() {
        Set<UUID> viewers = new HashSet<>(blockedEntityIdsByViewer.keySet());
        blockedEntityIdsByViewer.clear();
        clientCameraEntities.clear();

        for (UUID viewerId : viewers) {
            Player viewer = org.bukkit.Bukkit.getPlayer(viewerId);
            if (viewer == null || !viewer.isOnline()) {
                continue;
            }

            try {
                removePacketFilter(((CraftPlayer) viewer).getHandle(), viewerId);
            } catch (Throwable ignored) {
            }
        }
    }

    private void ensurePacketFilterInstalled(ServerPlayer viewerHandle, UUID viewerId) {
        Channel channel = getChannel(viewerHandle);
        if (channel == null) {
            return;
        }

        String handlerName = FILTER_PREFIX + viewerId;
        if (channel.pipeline().get(handlerName) != null) {
            return;
        }

        channel.pipeline().addBefore("packet_handler", handlerName, new ChannelDuplexHandler() {
            @Override
            public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
                Set<Integer> blocked = blockedEntityIdsByViewer.get(viewerId);
                if (blocked != null && !blocked.isEmpty() && shouldDropPacket(msg, blocked)) {
                    return;
                }
                super.write(ctx, msg, promise);
            }
        });
    }

    private void removePacketFilter(ServerPlayer viewerHandle, UUID viewerId) {
        Channel channel = getChannel(viewerHandle);
        if (channel == null) {
            return;
        }

        String handlerName = FILTER_PREFIX + viewerId;
        if (channel.pipeline().get(handlerName) != null) {
            channel.pipeline().remove(handlerName);
        }
    }
}
