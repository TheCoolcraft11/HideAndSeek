package de.thecoolcraft11.hideAndSeek.nms;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.VoxelShape;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class NoopNmsAdapter implements NmsAdapter {

    @Override
    public String name() {
        return "NOOP";
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public boolean isCompatible(String version) {
        return true;
    }

    @Override
    public Set<NmsCapabilities> capabilities() {
        return Collections.emptySet();
    }

    @Override
    public List<BoundingBox> getBoundingBoxes(BlockData blockData, Location location) {
        VoxelShape shape = blockData.getCollisionShape(location);
        return List.copyOf(shape.getBoundingBoxes());
    }


    @Override
    public boolean canPathfind(Mob mob, Location start, Location end) {
        return false;
    }

    @Override
    public void setServerGameModeSpectator(Player player) {
    }

    @Override
    public void spoofClientGameMode(Player player, GameMode mode) {
    }

    @Override
    public void setNoClipForEntity(org.bukkit.entity.Entity entity, boolean noClip) {
    }
}