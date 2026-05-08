package de.thecoolcraft11.hideAndSeek.nms.impl.v1_21_11;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.server.level.ServerPlayer;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

public final class PacketFilterHandler {

    private static final String FILTER_PREFIX = "has_anticheat_filter_";

    private final Map<UUID, Set<Integer>> blockedEntityIdsByViewer;

    private final Map<UUID, Map<Integer, net.minecraft.world.entity.Entity>> clientCameraEntities;
    private Predicate<UUID> cameraSessionChecker;

    public PacketFilterHandler(
            Map<UUID, Set<Integer>> blockedEntityIdsByViewer,
            Map<UUID, Map<Integer, net.minecraft.world.entity.Entity>> clientCameraEntities,
            Predicate<UUID> cameraSessionChecker) {
        this.blockedEntityIdsByViewer = blockedEntityIdsByViewer;
        this.clientCameraEntities = clientCameraEntities;
        this.cameraSessionChecker = cameraSessionChecker;
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

    private static int extractEntityId(ServerboundInteractPacket packet) {
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

    public static Channel getChannel(ServerPlayer viewerHandle) {
        try {

            Field serverConnectionField = viewerHandle.connection.getClass()
                    .getSuperclass()
                    .getDeclaredField("connection");
            serverConnectionField.setAccessible(true);
            Connection connection = (Connection) serverConnectionField.get(viewerHandle.connection);

            Field channelField = Connection.class.getDeclaredField("channel");
            channelField.setAccessible(true);
            return (Channel) channelField.get(connection);
        } catch (Throwable ignored) {
            return null;
        }
    }

    public void setCameraSessionChecker(Predicate<UUID> checker) {
        this.cameraSessionChecker = checker != null ? checker : uuid -> false;
    }

    public boolean ensureInstalled(ServerPlayer player, UUID viewerId) {
        Channel channel = getChannel(player);
        if (channel == null || !channel.isActive()) {
            return false;
        }

        String handlerName = FILTER_PREFIX + viewerId;
        if (channel.pipeline().get(handlerName) != null) {
            return true;
        }

        try {
            channel.pipeline().addBefore("packet_handler", handlerName, new ChannelDuplexHandler() {

                @Override
                public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {

                    try {
                        if (msg instanceof ServerboundInteractPacket packet) {
                            int targetId = extractEntityId(packet);


                            if (targetId == player.getId()) {
                                return;
                            }


                            if (cameraSessionChecker.test(player.getUUID())) {
                                return;
                            }


                            Map<Integer, net.minecraft.world.entity.Entity> cameraMap =
                                    clientCameraEntities.get(player.getUUID());
                            if (cameraMap != null && cameraMap.containsKey(targetId)) {
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

            return channel.pipeline().get(handlerName) != null;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public void remove(ServerPlayer viewerHandle, UUID viewerId) {
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
