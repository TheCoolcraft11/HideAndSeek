package de.thecoolcraft11.hideAndSeek.nms.impl.v1_21_10;

import de.thecoolcraft11.hideAndSeek.nms.NmsAdapter;
import de.thecoolcraft11.hideAndSeek.nms.NmsCapabilities;
import de.thecoolcraft11.hideAndSeek.nms.impl.v1_21_10.assistant.SeekerAssistantCreeperEntity;
import de.thecoolcraft11.hideAndSeek.nms.impl.v1_21_10.assistant.SeekerAssistantEntity;
import de.thecoolcraft11.hideAndSeek.nms.impl.v1_21_10.assistant.SeekerAssistantSkeletonEntity;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
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
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
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
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
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

    private static final String SKIN_GHOST_DRONE = "skin_ghost_drone";
    private static final String SKIN_BATTLE_MECH = "skin_battle_mech";
    private static final String SKIN_STEEL_GOLEM = "skin_steel_golem";


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
                    NmsCapabilities.ENTITY_GLIDE_FLAG,
                    NmsCapabilities.CLIENT_FAKE_BORDER_WARNING,
                    NmsCapabilities.CLIENT_CAMERA_SPOOFING,
                    NmsCapabilities.CLIENT_TEST_BLOCK_BEAM,
                    NmsCapabilities.CUSTOM_ENTITY_GOALS
            );
    private final Map<UUID, Set<Integer>> blockedEntityIdsByViewer = new ConcurrentHashMap<>();
    private final Map<UUID, Map<Integer, net.minecraft.world.entity.Entity>> clientCameraEntities = new ConcurrentHashMap<>();
    private final Map<UUID, Set<UUID>> assistantIdsBySeeker = new ConcurrentHashMap<>();
    private final Map<UUID, Boolean> hadAllowedFlight = new ConcurrentHashMap<>();
    private Predicate<UUID> cameraSessionChecker = uuid -> false;

    @Override
    public String name() {
        return "v1_21_10";
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public boolean isCompatible(String version) {
        return version.equals("1.21.10");
    }

    @Override
    public Set<NmsCapabilities> capabilities() {
        return CAPS;
    }

    @Override
    public boolean hasNmsCapabilities() {
        return true;
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

            boolean filterReady = ensurePacketFilterInstalled(viewerHandle, viewer.getUniqueId());
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

            if (!filterReady) {
                return false;
            }

            boolean changed = blocked.add(targetEntityId);

            if (changed || blocked.contains(targetEntityId)) {
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
    public void removeClientEntity(Player viewer, int entityId) {
        if (viewer == null || !viewer.isOnline() || entityId == Integer.MIN_VALUE) {
            return;
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

        } catch (Throwable ignored) {
        }
    }

    @Override
    public void setCameraEntity(Player viewer, int entityId) {
        if (viewer == null || !viewer.isOnline() || entityId == Integer.MIN_VALUE) {
            return;
        }

        try {
            Map<Integer, net.minecraft.world.entity.Entity> byEntityId = clientCameraEntities.get(viewer.getUniqueId());
            if (byEntityId == null) {
                return;
            }

            net.minecraft.world.entity.Entity cameraEntity = byEntityId.get(entityId);
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

    @Override
    public void resetCamera(Player viewer) {
        if (viewer == null || !viewer.isOnline()) {
            return;
        }

        try {
            ServerPlayer serverPlayer = ((CraftPlayer) viewer).getHandle();
            serverPlayer.connection.send(new ClientboundSetCameraPacket(serverPlayer));

            Map<Integer, net.minecraft.world.entity.Entity> byEntityId = clientCameraEntities.remove(viewer.getUniqueId());
            if (byEntityId != null && !byEntityId.isEmpty()) {
                int[] ids = byEntityId.keySet().stream().mapToInt(Integer::intValue).toArray();
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

    @Override
    public void setEntityGlowingForViewer(Player viewer, Entity target, boolean glowing) {
        if (viewer == null || target == null || !viewer.isOnline() || !target.isValid()) {
            return;
        }

        try {
            ServerPlayer viewerHandle = ((CraftPlayer) viewer).getHandle();
            net.minecraft.world.entity.Entity targetHandle = ((CraftEntity) target).getHandle();

            EntityDataAccessor<Byte> sharedFlagsAccessor = getSharedFlagsAccessor();
            if (sharedFlagsAccessor == null) {
                return;
            }

            byte flags = targetHandle.getEntityData().get(sharedFlagsAccessor);
            byte updated = glowing ? (byte) (flags | 0x40) : (byte) (flags & ~0x40);

            SynchedEntityData.DataValue<Byte> value = SynchedEntityData.DataValue.create(
                    sharedFlagsAccessor,
                    updated
            );

            viewerHandle.connection.send(new ClientboundSetEntityDataPacket(targetHandle.getId(), List.of(value)));
        } catch (Throwable ignored) {
        }
    }

    @Override
    public void setEntityGlowingForViewer(Player viewer, Player target, boolean glowing) {
        setEntityGlowingForViewer(viewer, (Entity) target, glowing);
    }

    @Override
    public void showWarningBorder(Player viewer, float strength) {
        if (viewer == null || !viewer.isOnline()) {
            return;
        }
        try {
            ServerPlayer handle = ((CraftPlayer) viewer).getHandle();
            WorldBorder worldBorder = handle.level().getWorldBorder();

            net.minecraft.world.level.border.WorldBorder border = new net.minecraft.world.level.border.WorldBorder();
            border.setCenter(worldBorder.getCenterX(), worldBorder.getCenterZ());
            border.setSize(worldBorder.getSize());
            border.setWarningBlocks((int) (worldBorder.getSize() * strength));
            border.setWarningTime(0);
            handle.connection.send(new ClientboundInitializeBorderPacket(border));
        } catch (Throwable ignored) {
        }
    }

    @Override
    public void resetWarningBorder(Player viewer) {
        if (viewer == null || !viewer.isOnline()) {
            return;
        }
        try {
            ServerPlayer handle = ((CraftPlayer) viewer).getHandle();
            handle.connection.send(new ClientboundInitializeBorderPacket(handle.level().getWorldBorder()));
        } catch (Throwable ignored) {
        }
    }

    @Override
    public Entity spawnSeekerAssistant(Plugin plugin, Player seeker, Location location, String assistantSkin) {
        if (plugin == null || seeker == null || location == null || location.getWorld() == null) {
            return null;
        }

        try {
            net.minecraft.world.level.Level nmsLevel = ((CraftWorld) location.getWorld()).getHandle();
            net.minecraft.world.entity.PathfinderMob assistant = createAssistantEntity(plugin, seeker, location, nmsLevel, assistantSkin);

            switch (assistant) {
                case SeekerAssistantEntity zombieAssistant -> zombieAssistant.injectGoals();
                case SeekerAssistantSkeletonEntity skeletonAssistant -> skeletonAssistant.injectGoals();
                case SeekerAssistantCreeperEntity creeperAssistant -> creeperAssistant.injectGoals();
                default -> {
                }
            }

            assistant.setPos(location.getX(), location.getY(), location.getZ());
            nmsLevel.addFreshEntity(assistant);

            Entity bukkit = assistant.getBukkitEntity();
            if (bukkit instanceof org.bukkit.entity.Mob mob) {
                mob.setCanPickupItems(false);
                mob.setSilent(false);
                mob.setRemoveWhenFarAway(false);
                mob.customName(net.kyori.adventure.text.Component.text("Seeker's Assistant"));
                mob.setCustomNameVisible(true);

                var speed = mob.getAttribute(org.bukkit.attribute.Attribute.MOVEMENT_SPEED);
                if (speed != null) {
                    speed.setBaseValue(0.38D);
                }
                var health = mob.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH);
                if (health != null) {
                    health.setBaseValue(6.0D);
                    mob.setHealth(6.0D);
                }
            }

            if (bukkit instanceof org.bukkit.entity.Creeper creeper) {
                creeper.setPowered(false);
                creeper.setExplosionRadius(0);
                creeper.setMaxFuseTicks(Short.MAX_VALUE);
            }

            bukkit.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(plugin, "assistant_entity"), PersistentDataType.BOOLEAN, true);
            bukkit.getPersistentDataContainer().set(new org.bukkit.NamespacedKey(plugin, "assistant_owner"), PersistentDataType.STRING, seeker.getUniqueId().toString());

            assistantIdsBySeeker
                    .computeIfAbsent(seeker.getUniqueId(), ignored -> ConcurrentHashMap.newKeySet())
                    .add(bukkit.getUniqueId());


            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                Entity entity = plugin.getServer().getEntity(bukkit.getUniqueId());
                if (entity != null && entity.isValid()) {
                    entity.remove();
                }
                Set<UUID> ids = assistantIdsBySeeker.get(seeker.getUniqueId());
                if (ids != null) {
                    ids.remove(bukkit.getUniqueId());
                    if (ids.isEmpty()) {
                        assistantIdsBySeeker.remove(seeker.getUniqueId());
                    }
                }
            }, 90L * 20L);

            return bukkit;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private net.minecraft.world.entity.PathfinderMob createAssistantEntity(Plugin plugin, Player seeker, Location location, net.minecraft.world.level.Level nmsLevel, String assistantSkin) {
        if (SKIN_GHOST_DRONE.equals(assistantSkin)) {
            return new SeekerAssistantSkeletonEntity(plugin, seeker.getUniqueId(), location, nmsLevel, assistantSkin);
        }
        if (SKIN_BATTLE_MECH.equals(assistantSkin)) {
            return new SeekerAssistantCreeperEntity(plugin, seeker.getUniqueId(), location, nmsLevel, assistantSkin);
        }
        if (SKIN_STEEL_GOLEM.equals(assistantSkin)) {
            return new SeekerAssistantSkeletonEntity(plugin, seeker.getUniqueId(), location, nmsLevel, assistantSkin);
        }
        return new SeekerAssistantEntity(plugin, seeker.getUniqueId(), location, nmsLevel, assistantSkin);
    }

    @Override
    public void removeAllAssistants(Plugin plugin, UUID seekerId) {
        if (plugin == null) {
            return;
        }

        if (seekerId != null) {
            removeAssistantsForSeeker(plugin, seekerId);
            return;
        }

        for (UUID id : new HashSet<>(assistantIdsBySeeker.keySet())) {
            removeAssistantsForSeeker(plugin, id);
        }
    }

    private void removeAssistantsForSeeker(Plugin plugin, UUID seekerId) {
        Set<UUID> ids = assistantIdsBySeeker.remove(seekerId);
        if (ids == null || ids.isEmpty()) {
            return;
        }

        for (UUID assistantId : ids) {
            Entity entity = plugin.getServer().getEntity(assistantId);
            if (entity != null) {
                Location loc = entity.getLocation();
                if (loc.getWorld() != null) {
                    loc.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, loc, 20, 0.4, 0.5, 0.4, 0.03);
                    loc.getWorld().playSound(loc, Sound.ENTITY_BLAZE_DEATH, 0.7f, 0.8f);
                }
                entity.remove();
            }
        }
    }

    @Override
    public void sendAssistantBeamToAll(Plugin plugin, Location hiderLocation, String color) {
        if (plugin == null || hiderLocation == null || hiderLocation.getWorld() == null) {
            return;
        }

        int minY = hiderLocation.getWorld().getMinHeight();
        BlockPos pos = new BlockPos(hiderLocation.getBlockX(), minY, hiderLocation.getBlockZ());

        if ("alert".equalsIgnoreCase(color)) {
            int switches = 6;
            int interval = 4;
            for (int i = 0; i < switches; i++) {
                final String stepColor = (i % 2 == 0) ? "red" : "green";
                final long delay = (long) i * interval;
                plugin.getServer().getScheduler().runTaskLater(plugin, () -> {

                    for (Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
                        if (player.isOnline() && player.getWorld().equals(hiderLocation.getWorld())) {
                            sendBeamPackets(player, pos, stepColor);
                        }
                    }
                }, delay);
            }

            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                for (Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
                    if (player.isOnline() && player.getWorld().equals(hiderLocation.getWorld())) {
                        removeBeam(player, pos);
                    }
                }
            }, (long) switches * interval + 8L);
            return;
        }


        for (Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
            if (player.isOnline() && player.getWorld().equals(hiderLocation.getWorld())) {
                sendBeamPackets(player, pos, color);
            }
        }

        int durationTicks = 40;
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            for (Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
                if (player.isOnline() && player.getWorld().equals(hiderLocation.getWorld())) {
                    removeBeam(player, pos);
                }
            }
        }, durationTicks);
    }

    private void sendBeamPackets(Player player, BlockPos pos, String color) {
        var conn = ((CraftPlayer) player).getHandle().connection;

        conn.send(new ClientboundBlockUpdatePacket(pos, Blocks.TEST_INSTANCE_BLOCK.defaultBlockState()));

        CompoundTag data = buildBeamData(color);
        CompoundTag root = new CompoundTag();
        root.put("data", data);

        conn.send(new ClientboundBlockEntityDataPacket(pos, BlockEntityType.TEST_INSTANCE_BLOCK, root));
    }

    private void removeBeam(Player player, BlockPos pos) {
        var conn = ((CraftPlayer) player).getHandle().connection;
        var serverLevel = ((CraftWorld) player.getWorld()).getHandle();
        conn.send(new ClientboundBlockUpdatePacket(serverLevel, pos));
    }

    private CompoundTag buildBeamData(String color) {
        CompoundTag data = new CompoundTag();
        data.putString("rotation", "none");
        data.putByte("ignore_entities", (byte) 0);
        data.putIntArray("size", new int[]{1, 1, 1});

        if ("green".equalsIgnoreCase(color)) {
            data.putString("status", "finished");
            return data;
        }

        if ("red".equalsIgnoreCase(color)) {
            data.putString("status", "finished");
            data.put("error_message", buildErrorMessage());
            return data;
        }

        if ("gray".equalsIgnoreCase(color)) {
            data.putString("status", "running");
            return data;
        }

        data.putString("status", "cleared");
        return data;
    }

    private CompoundTag buildErrorMessage() {
        CompoundTag innerInner = new CompoundTag();
        innerInner.putString("translate", "test_block.mode.accept");

        ListTag innerWith = new ListTag();
        innerWith.add(innerInner);

        CompoundTag inner = new CompoundTag();
        inner.putString("translate", "test_block.error.missing");
        inner.put("with", innerWith);

        ListTag outerWith = new ListTag();
        outerWith.add(inner);
        outerWith.add(IntTag.valueOf(0));

        CompoundTag error = new CompoundTag();
        error.putString("translate", "test.error.tick");
        error.put("with", outerWith);
        return error;
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

    private static int getEntityId(ServerboundInteractPacket packet) {
        try {
            for (Method m : packet.getClass().getMethods()) {
                if (m.getParameterCount() == 0 && m.getReturnType() == int.class) {
                    String name = m.getName().toLowerCase();
                    if (name.contains("entity") || name.contains("id")) {
                        return (int) m.invoke(packet);
                    }
                }
            }

            for (Field f : packet.getClass().getDeclaredFields()) {
                if (f.getType() == int.class) {
                    f.setAccessible(true);
                    return f.getInt(packet);
                }
            }

        } catch (Throwable ignored) {
        }

        return Integer.MIN_VALUE;
    }

    @Override
    public void setCameraSessionChecker(Predicate<UUID> checker) {
        this.cameraSessionChecker = checker != null ? checker : uuid -> false;
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

    private boolean ensurePacketFilterInstalled(ServerPlayer player, UUID viewerId) {
        Channel channel = getChannel(player);
        if (channel == null || !channel.isActive()) {
            return false;
        }

        String handlerName = FILTER_PREFIX + viewerId;
        if (channel.pipeline().get(handlerName) != null) {
            return true;
        }

        try {
            if (channel.pipeline().get(handlerName) == null) {
                channel.pipeline().addBefore("packet_handler", handlerName, new ChannelDuplexHandler() {

                    @Override
                    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

                        try {
                            if (msg instanceof ServerboundInteractPacket packet) {

                                int targetId = getEntityId(packet);

                                if (targetId == player.getId()) {
                                    return;
                                }

                                if (cameraSessionChecker.test(player.getUUID())) {
                                    return;
                                }

                                Map<Integer, net.minecraft.world.entity.Entity> map =
                                        clientCameraEntities.get(player.getUUID());

                                if (map != null && map.containsKey(targetId)) {
                                    return;
                                }
                            }
                        } catch (Throwable ignored) {
                        }

                        super.channelRead(ctx, msg);
                    }

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

            return channel.pipeline().get(handlerName) != null;
        } catch (Throwable ignored) {
            return false;
        }
    }

}
