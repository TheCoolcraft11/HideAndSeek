package de.thecoolcraft11.hideAndSeek.nms.impl.v1_21_R7;

import de.thecoolcraft11.hideAndSeek.nms.NmsAdapter;
import de.thecoolcraft11.hideAndSeek.nms.NmsCapabilities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.block.data.CraftBlockData;
import org.bukkit.craftbukkit.entity.CraftMob;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class NmsAdapterImpl implements NmsAdapter {

    private static final Set<NmsCapabilities> CAPS =
            EnumSet.of(
                    NmsCapabilities.BLOCK_VOXEL_SHAPE,
                    NmsCapabilities.MOB_PATHFINDING,
                    NmsCapabilities.CLIENT_GAMEMODE_SPOOFING
            );

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
}