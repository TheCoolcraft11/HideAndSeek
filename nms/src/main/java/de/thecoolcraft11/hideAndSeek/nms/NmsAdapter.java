package de.thecoolcraft11.hideAndSeek.nms;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

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

    void setNoClipForEntity(Entity entity, boolean noClip);

    boolean spawnClientLightning(Player viewer, Location location);

    Entity raycastEntityHit(Player shooter, Location start, Vector direction, double distance, double hitboxInflation, Predicate<Entity> filter);

    boolean setEntityVisibilityForViewer(Player viewer, Player target, boolean visible);

    int spawnClientCameraEntity(Player viewer, Location location, float yaw, float pitch, EntityType entityType);

    boolean removeClientEntity(Player viewer, int entityId);

    boolean setCameraEntity(Player viewer, int entityId);

    boolean resetCamera(Player viewer);

    boolean setEntityGlowingForViewer(Player viewer, Player target, boolean glowing);

    void clearVisibilityFilters();

}
