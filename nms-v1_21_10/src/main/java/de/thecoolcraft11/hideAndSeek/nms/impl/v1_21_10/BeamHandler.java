package de.thecoolcraft11.hideAndSeek.nms.impl.v1_21_10;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import org.bukkit.Location;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Objects;

public final class BeamHandler {

    public void sendToAll(Plugin plugin, Location hiderLocation, String color) {
        if (plugin == null || hiderLocation == null || hiderLocation.getWorld() == null) {
            return;
        }


        BlockPos pos = new BlockPos(
                hiderLocation.getBlockX(),
                findBeamY(hiderLocation),
                hiderLocation.getBlockZ()
        );

        if ("alert".equalsIgnoreCase(color)) {
            sendAlertBeam(plugin, hiderLocation, pos);
            return;
        }

        broadcastBeam(hiderLocation, pos, color);
        scheduleRemoval(plugin, hiderLocation, pos);
    }

    private int findBeamY(Location loc) {
        if (loc == null || loc.getWorld() == null) {
            return Objects.requireNonNull(loc).getBlockY();
        }

        int startY = loc.getBlockY() - 2;
        int endY = Math.max(loc.getBlockY() - 15, loc.getWorld().getMinHeight());
        int x = loc.getBlockX();
        int z = loc.getBlockZ();

        for (int y = startY; y >= endY; y--) {
            var block = loc.getWorld().getBlockAt(x, y, z);
            var above = loc.getWorld().getBlockAt(x, y + 1, z);

            if (!block.isEmpty() && above.isEmpty()) {
                return y;
            }
        }

        return endY;
    }

    private void sendAlertBeam(Plugin plugin, Location hiderLocation, BlockPos pos) {
        int switches = 6;
        int interval = 4;

        for (int i = 0; i < switches; i++) {
            final String stepColor = (i % 2 == 0) ? "red" : "green";
            final long delay = (long) i * interval;
            plugin.getServer().getScheduler().runTaskLater(plugin, () ->
                    broadcastBeam(hiderLocation, pos, stepColor), delay);
        }


        plugin.getServer().getScheduler().runTaskLater(plugin, () ->
                removeFromAll(hiderLocation, pos), (long) switches * interval + 8L);
    }

    private void broadcastBeam(Location worldRef, BlockPos pos, String color) {
        for (Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
            if (player.isOnline() && player.getWorld().equals(worldRef.getWorld())) {
                sendBeamPackets(player, pos, color);
            }
        }
    }

    private void scheduleRemoval(Plugin plugin, Location worldRef, BlockPos pos) {
        plugin.getServer().getScheduler().runTaskLater(plugin, () ->
                removeFromAll(worldRef, pos), 40L);
    }

    private void removeFromAll(Location worldRef, BlockPos pos) {
        for (Player player : org.bukkit.Bukkit.getOnlinePlayers()) {
            if (player.isOnline() && player.getWorld().equals(worldRef.getWorld())) {
                removeBeam(player, pos);
            }
        }
    }

    private void sendBeamPackets(Player player, BlockPos pos, String color) {
        var conn = ((CraftPlayer) player).getHandle().connection;

        conn.send(new ClientboundBlockUpdatePacket(pos, Blocks.TEST_INSTANCE_BLOCK.defaultBlockState()));

        CompoundTag data = buildBeamNbt(color);
        CompoundTag root = new CompoundTag();
        root.put("data", data);

        conn.send(new ClientboundBlockEntityDataPacket(pos, BlockEntityType.TEST_INSTANCE_BLOCK, root));
    }


    private void removeBeam(Player player, BlockPos pos) {
        var conn = ((CraftPlayer) player).getHandle().connection;
        var serverLevel = ((CraftWorld) player.getWorld()).getHandle();
        conn.send(new ClientboundBlockUpdatePacket(serverLevel, pos));
    }

    private CompoundTag buildBeamNbt(String color) {
        CompoundTag data = new CompoundTag();
        data.putString("rotation", "none");
        data.putByte("ignore_entities", (byte) 0);
        data.putIntArray("size", new int[]{1, 1, 1});

        switch (color.toLowerCase()) {
            case "green" -> data.putString("status", "finished");
            case "red" -> {
                data.putString("status", "finished");
                data.put("error_message", buildErrorMessageNbt());
            }
            case "gray" -> data.putString("status", "running");
            default -> data.putString("status", "cleared");
        }

        return data;
    }

    private CompoundTag buildErrorMessageNbt() {
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
}
