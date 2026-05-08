package de.thecoolcraft11.hideAndSeek.nms.impl.v1_21_11;

import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class EntityVisibilityHandler {

    private final Map<UUID, Set<Integer>> blockedEntityIdsByViewer;

    private final PacketFilterHandler filterHandler;

    public EntityVisibilityHandler(
            Map<UUID, Set<Integer>> blockedEntityIdsByViewer,
            PacketFilterHandler filterHandler) {
        this.blockedEntityIdsByViewer = blockedEntityIdsByViewer;
        this.filterHandler = filterHandler;
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

    public boolean setVisible(Player viewer, Player target, boolean visible) {
        if (viewer == null || target == null || !viewer.isOnline() || !target.isOnline()) {
            return false;
        }

        if (viewer.getUniqueId().equals(target.getUniqueId())) {
            return true;
        }

        try {
            ServerPlayer viewerHandle = ((CraftPlayer) viewer).getHandle();
            int targetEntityId = ((CraftPlayer) target).getHandle().getId();

            boolean filterReady = filterHandler.ensureInstalled(viewerHandle, viewer.getUniqueId());
            Set<Integer> blocked = blockedEntityIdsByViewer.computeIfAbsent(
                    viewer.getUniqueId(), ignored -> ConcurrentHashMap.newKeySet());

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

    public void clearAll() {
        blockedEntityIdsByViewer.clear();
    }
}
