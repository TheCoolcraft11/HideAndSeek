package de.thecoolcraft11.hideAndSeek.nms.impl.v1_21_11;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.util.function.IntConsumer;


public final class SpectatorInventoryHandler {

    private static final String HANDLER_PREFIX = "has_spec_inv_";

    private SpectatorInventoryHandler() {
    }

    public static void inject(Player player,
                              IntConsumer onSlotClick) {
        Channel channel = getChannel(player);
        if (channel == null || !channel.isActive()) {
            return;
        }

        String name = handlerName(player.getUniqueId());
        channel.eventLoop().submit(() -> {
            if (channel.pipeline().get(name) != null) {
                return;
            }
            channel.pipeline().addBefore("packet_handler", name, new ChannelDuplexHandler() {
                @Override
                public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                    try {
                        if (msg instanceof ServerboundContainerClickPacket click) {

                            if (click.containerId() == 0) {
                                onSlotClick.accept(click.slotNum());
                            }
                        }
                    } catch (Throwable ignored) {

                    }
                    super.channelRead(ctx, msg);
                }
            });
        });
    }


    public static void remove(Player player) {
        Channel channel = getChannel(player);
        if (channel == null) {
            return;
        }
        String name = handlerName(player.getUniqueId());
        channel.eventLoop().submit(() -> {
            if (channel.pipeline().get(name) != null) {
                channel.pipeline().remove(name);
            }
        });
    }


    private static String handlerName(java.util.UUID playerId) {
        return HANDLER_PREFIX + playerId;
    }

    private static Channel getChannel(Player player) {
        try {
            ServerPlayer handle = ((CraftPlayer) player).getHandle();


            Field connectionField = handle.connection.getClass()
                    .getSuperclass()
                    .getDeclaredField("connection");
            connectionField.setAccessible(true);
            Connection connection = (Connection) connectionField.get(handle.connection);

            Field channelField = Connection.class.getDeclaredField("channel");
            channelField.setAccessible(true);
            return (Channel) channelField.get(connection);
        } catch (Throwable t) {
            return null;
        }
    }
}