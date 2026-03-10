package de.thecoolcraft11.hideAndSeek.nms;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;

import java.util.List;
import java.util.Set;

public interface NmsAdapter {

    String name();

    boolean isAvailable();

    boolean isCompatible(String version);

    Set<NmsCapabilities> capabilities();

    default boolean hasCapability(NmsCapabilities capability) {
        return capabilities().contains(capability);
    }

    List<BoundingBox> getBoundingBoxes(BlockData blockData, Location location);


    boolean canPathfind(Mob mob, Location start, Location end);

    void setServerGameModeSpectator(Player player);

    void spoofClientGameMode(Player player, GameMode mode);


}