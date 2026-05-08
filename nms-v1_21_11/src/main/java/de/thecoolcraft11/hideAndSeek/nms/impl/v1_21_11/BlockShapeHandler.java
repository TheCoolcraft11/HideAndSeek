package de.thecoolcraft11.hideAndSeek.nms.impl.v1_21_11;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.craftbukkit.block.data.CraftBlockData;
import org.bukkit.util.BoundingBox;

import java.util.ArrayList;
import java.util.List;

public final class BlockShapeHandler {

    public List<BoundingBox> getBoundingBoxes(BlockData blockData, Location loc) {
        BlockState nmsState = ((CraftBlockData) blockData).getState();

        BlockPos pos = new BlockPos(
                loc.getBlockX(),
                loc.getBlockY(),
                loc.getBlockZ()
        );

        var world = ((CraftWorld) loc.getWorld()).getHandle();


        var shape = nmsState.getShape(world, pos, CollisionContext.empty());

        List<BoundingBox> result = new ArrayList<>();

        for (AABB aabb : shape.toAabbs()) {


            result.add(new BoundingBox(
                    loc.getX() + aabb.minX,
                    loc.getY() + aabb.minY,
                    loc.getZ() + aabb.minZ,
                    loc.getX() + aabb.maxX,
                    loc.getY() + aabb.maxY,
                    loc.getZ() + aabb.maxZ
            ));
        }

        return result;
    }
}
