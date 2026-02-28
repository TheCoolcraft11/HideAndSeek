package de.thecoolcraft11.hideAndSeek.block;

import org.bukkit.Axis;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.Orientable;
import org.bukkit.block.data.Rotatable;
import org.bukkit.block.data.type.Slab;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.block.data.type.TrapDoor;
import org.bukkit.entity.Player;

public class BlockDirectionUtil {

    public static BlockData applyPlayerDirection(BlockData blockData, Player player) {
        Location loc = player.getLocation();
        float yaw = loc.getYaw();
        float pitch = loc.getPitch();


        if (blockData instanceof Directional directional) {
            applyFacing(directional, yaw, pitch);
        }

        if (blockData instanceof Orientable orientable) {
            applyAxis(orientable, yaw, pitch);
        }

        if (blockData instanceof Rotatable rotatable) {
            applyRotation(rotatable, yaw);
        }


        applyHalfProperty(blockData, pitch);

        return blockData;
    }

    private static void applyFacing(Directional directional, float yaw, float pitch) {
        var allowedFaces = directional.getFaces();


        if (allowedFaces.contains(BlockFace.UP) && pitch < -45) {
            directional.setFacing(BlockFace.UP);
            return;
        }
        if (allowedFaces.contains(BlockFace.DOWN) && pitch > 45) {
            directional.setFacing(BlockFace.DOWN);
            return;
        }


        BlockFace horizontal = getHorizontalFacing(yaw);
        if (allowedFaces.contains(horizontal)) {
            directional.setFacing(horizontal);
        } else if (allowedFaces.contains(horizontal.getOppositeFace())) {

            directional.setFacing(horizontal.getOppositeFace());
        } else if (!allowedFaces.isEmpty()) {

            directional.setFacing(allowedFaces.iterator().next());
        }
    }

    private static void applyAxis(Orientable orientable, float yaw, float pitch) {
        var allowedAxes = orientable.getAxes();


        if (allowedAxes.contains(Axis.Y) && (pitch < -45 || pitch > 45)) {
            orientable.setAxis(Axis.Y);
            return;
        }


        float normalizedYaw = (yaw + 360) % 360;


        if ((normalizedYaw >= 45 && normalizedYaw < 135) || (normalizedYaw >= 225 && normalizedYaw < 315)) {
            if (allowedAxes.contains(Axis.X)) {
                orientable.setAxis(Axis.X);
                return;
            }
        }


        if (allowedAxes.contains(Axis.Z)) {
            orientable.setAxis(Axis.Z);
            return;
        }


        if (!allowedAxes.isEmpty()) {
            orientable.setAxis(allowedAxes.iterator().next());
        }
    }

    private static void applyRotation(Rotatable rotatable, float yaw) {


        float normalizedYaw = (yaw + 360) % 360;
        int rotation = Math.round(normalizedYaw / 22.5f) % 16;


        BlockFace[] faces = {
                BlockFace.SOUTH,
                BlockFace.SOUTH_SOUTH_WEST,
                BlockFace.SOUTH_WEST,
                BlockFace.WEST_SOUTH_WEST,
                BlockFace.WEST,
                BlockFace.WEST_NORTH_WEST,
                BlockFace.NORTH_WEST,
                BlockFace.NORTH_NORTH_WEST,
                BlockFace.NORTH,
                BlockFace.NORTH_NORTH_EAST,
                BlockFace.NORTH_EAST,
                BlockFace.EAST_NORTH_EAST,
                BlockFace.EAST,
                BlockFace.EAST_SOUTH_EAST,
                BlockFace.SOUTH_EAST,
                BlockFace.SOUTH_SOUTH_EAST
        };

        rotatable.setRotation(faces[rotation]);
    }

    private static void applyHalfProperty(BlockData blockData, float pitch) {

        if (blockData instanceof Slab slab) {
            if (pitch > 0) {
                slab.setType(Slab.Type.BOTTOM);
            } else if (pitch < 0) {
                slab.setType(Slab.Type.TOP);
            }

        }

        if (blockData instanceof Stairs stairs) {
            if (pitch > 45) {
                stairs.setHalf(Stairs.Half.BOTTOM);
            } else if (pitch < -45) {
                stairs.setHalf(Stairs.Half.TOP);
            }
        }

        if (blockData instanceof TrapDoor trapDoor) {
            if (pitch > 45) {
                trapDoor.setHalf(TrapDoor.Half.BOTTOM);
            } else if (pitch < -45) {
                trapDoor.setHalf(TrapDoor.Half.TOP);
            }
        }
    }

    private static BlockFace getHorizontalFacing(float yaw) {
        float normalizedYaw = (yaw + 360) % 360;

        if (normalizedYaw >= 315 || normalizedYaw < 45) {
            return BlockFace.SOUTH;
        } else if (normalizedYaw >= 45 && normalizedYaw < 135) {
            return BlockFace.WEST;
        } else if (normalizedYaw >= 135 && normalizedYaw < 225) {
            return BlockFace.NORTH;
        } else {
            return BlockFace.EAST;
        }
    }

}
