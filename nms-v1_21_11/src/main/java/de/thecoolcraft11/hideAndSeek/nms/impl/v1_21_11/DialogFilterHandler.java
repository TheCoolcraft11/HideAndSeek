package de.thecoolcraft11.hideAndSeek.nms.impl.v1_21_11;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.configuration.ClientboundRegistryDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.BiFunction;


public final class DialogFilterHandler {

    private static UUID extractUuid(Object listener) {
        try {
            for (Method m : listener.getClass().getMethods()) {
                if (m.getReturnType() == com.mojang.authlib.GameProfile.class
                        && m.getParameterCount() == 0) {
                    com.mojang.authlib.GameProfile profile =
                            (com.mojang.authlib.GameProfile) m.invoke(listener);
                    if (profile != null) {
                        return profile.id();
                    }
                }
            }
            for (Field f : listener.getClass().getDeclaredFields()) {
                if (f.getType() == com.mojang.authlib.GameProfile.class) {
                    f.setAccessible(true);
                    com.mojang.authlib.GameProfile profile =
                            (com.mojang.authlib.GameProfile) f.get(listener);
                    if (profile != null) {
                        return profile.id();
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static Channel extractChannel(Connection connection) {
        try {
            Field f = Connection.class.getDeclaredField("channel");
            f.setAccessible(true);
            return (Channel) f.get(connection);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object extractRegistryKey(ClientboundRegistryDataPacket packet) {
        try {
            for (Method m : packet.getClass().getDeclaredMethods()) {
                if (m.getParameterCount() == 0
                        && ResourceKey.class.isAssignableFrom(m.getReturnType())) {
                    m.setAccessible(true);
                    return m.invoke(packet);
                }
            }
            for (Field f : packet.getClass().getDeclaredFields()) {
                if (ResourceKey.class.isAssignableFrom(f.getType())) {
                    f.setAccessible(true);
                    return f.get(packet);
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static Object extractResourceKey(Object obj) {
        try {
            for (Field f : obj.getClass().getDeclaredFields()) {
                if (ResourceKey.class.isAssignableFrom(f.getType())) {
                    f.setAccessible(true);
                    return f.get(obj);
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static boolean isDialogRegistry(Object registryKey) {
        return registryKey.toString().contains("minecraft:dialog");
    }

    private static List<?> extractListField(Object obj) {
        try {
            for (Field f : obj.getClass().getDeclaredFields()) {
                if (List.class.isAssignableFrom(f.getType())) {
                    f.setAccessible(true);
                    return (List<?>) f.get(obj);
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static Optional<?> extractOptionalField(Object obj) {
        try {
            for (Field f : obj.getClass().getDeclaredFields()) {
                if (Optional.class.isAssignableFrom(f.getType())) {
                    f.setAccessible(true);
                    return (Optional<?>) f.get(obj);
                }
            }
        } catch (Throwable ignored) {
        }
        return Optional.empty();
    }

    private static Identifier extractIdentifier(Object entry) {
        try {
            for (Method m : entry.getClass().getDeclaredMethods()) {
                if (m.getParameterCount() == 0
                        && Identifier.class.isAssignableFrom(m.getReturnType())) {
                    m.setAccessible(true);
                    return (Identifier) m.invoke(entry);
                }
            }
            for (Field f : entry.getClass().getDeclaredFields()) {
                if (Identifier.class.isAssignableFrom(f.getType())) {
                    f.setAccessible(true);
                    return (Identifier) f.get(entry);
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    public void inject(UUID playerUuid, Plugin plugin,
                       BiFunction<String, OfflinePlayer, Boolean> permissionChecker) {
        try {
            MinecraftServer server = MinecraftServer.getServer();
            List<Connection> connections = server.getConnection().getConnections();

            for (Connection connection : connections) {
                Channel channel = extractChannel(connection);
                if (channel == null || !channel.isActive()) {
                    continue;
                }

                Object packetListener = connection.getPacketListener();
                if (packetListener == null) {
                    continue;
                }

                UUID listenerUuid = extractUuid(packetListener);
                if (listenerUuid == null || !listenerUuid.equals(playerUuid)) {
                    continue;
                }

                injectPipelineHandler(channel, playerUuid, plugin, permissionChecker);
                return;
            }
        } catch (Throwable t) {
            plugin.getLogger().warning("Failed to inject dialog filter for " + playerUuid + ": " + t.getMessage());
        }
    }

    private void injectPipelineHandler(Channel channel, UUID playerUuid, Plugin plugin,
                                       BiFunction<String, OfflinePlayer, Boolean> permissionChecker) {
        String handlerName = "has_dialog_filter_" + playerUuid;
        if (channel.pipeline().get(handlerName) != null) {
            return;
        }

        channel.pipeline().addBefore("packet_handler", handlerName, new ChannelDuplexHandler() {
            @Override
            public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise)
                    throws Exception {
                if (msg instanceof ClientboundRegistryDataPacket registryPacket) {
                    try {
                        Object registryKey = extractRegistryKey(registryPacket);
                        if (registryKey != null && isDialogRegistry(registryKey)) {
                            Object modified = rebuildDialogRegistry(
                                    registryPacket, permissionChecker, playerUuid);
                            if (modified != null) {

                                channel.pipeline().remove(handlerName);
                                super.write(ctx, modified, promise);
                                return;
                            }
                        }
                    } catch (Throwable t) {
                        plugin.getLogger().warning(
                                "Dialog registry intercept failed for " + playerUuid + ": " + t.getMessage());
                    }
                }
                super.write(ctx, msg, promise);
            }
        });
    }

    private Object rebuildDialogRegistry(ClientboundRegistryDataPacket original,
                                         BiFunction<String, OfflinePlayer, Boolean> permissionChecker,
                                         UUID playerUuid) {
        try {
            List<?> entries = extractListField(original);
            if (entries == null) {
                return null;
            }

            Identifier targetKey = Identifier.fromNamespaceAndPath("hideandseek", "main");
            List<Object> newEntries = new ArrayList<>();

            for (Object entry : entries) {
                Identifier entryId = extractIdentifier(entry);
                if (entryId != null && entryId.equals(targetKey)) {
                    Object rebuilt = rebuildMainDialogEntry(entry, permissionChecker, playerUuid);
                    newEntries.add(rebuilt != null ? rebuilt : entry);
                } else {
                    newEntries.add(entry);
                }
            }


            Object registryKey = extractResourceKey(original);
            if (registryKey == null) {
                return null;
            }

            for (Constructor<?> c : ClientboundRegistryDataPacket.class.getConstructors()) {
                if (c.getParameterCount() == 2) {
                    c.setAccessible(true);
                    return c.newInstance(registryKey, newEntries);
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private Object rebuildMainDialogEntry(Object originalEntry,
                                          BiFunction<String, OfflinePlayer, Boolean> permissionChecker,
                                          UUID playerUuid) {
        try {
            Optional<?> dataOpt = extractOptionalField(originalEntry);
            if (Objects.requireNonNull(dataOpt).isEmpty()) {
                return null;
            }

            CompoundTag dialogNbt = (CompoundTag) dataOpt.get();
            CompoundTag modified = dialogNbt.copy();

            ListTag actions = modified.getListOrEmpty("actions");
            ListTag filteredActions = new ListTag();

            for (int i = 0; i < actions.size(); i++) {
                if (actions.getCompound(i).isEmpty()) {
                    continue;
                }
                CompoundTag action = actions.getCompound(i).get();

                if (!lacksPermission(action, permissionChecker, playerUuid)) {
                    filteredActions.add(action);
                }
            }

            modified.put("actions", filteredActions);

            Identifier entryId = extractIdentifier(originalEntry);
            if (entryId == null) {
                return null;
            }

            for (Constructor<?> c : originalEntry.getClass().getDeclaredConstructors()) {
                if (c.getParameterCount() == 2) {
                    c.setAccessible(true);
                    return c.newInstance(entryId, Optional.of(modified));
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private boolean lacksPermission(CompoundTag actionTag,
                                    BiFunction<String, @NotNull OfflinePlayer, Boolean> permissionChecker,
                                    UUID playerUuid) {
        try {
            if (actionTag.getCompound("action").isEmpty()) {
                return false;
            }
            CompoundTag action = actionTag.getCompound("action").get();
            if (action.isEmpty()) {
                return false;
            }
            if (action.getString("type").isEmpty()) {
                return false;
            }
            String type = action.getString("type").get();
            if (!"minecraft:run_command".equals(type)) {
                return false;
            }
            if (action.getString("command").isEmpty()) {
                return false;
            }
            String command = action.getString("command").get();

            return !permissionChecker.apply(command, org.bukkit.Bukkit.getOfflinePlayer(playerUuid));
        } catch (Throwable ignored) {
            return false;
        }
    }
}
