package de.thecoolcraft11.hideAndSeek.nms;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

public interface NmsAdapter {

    String name();

    boolean isAvailable();

    boolean isCompatible(String version);

    Set<NmsCapabilities> capabilities();

    boolean hasNmsCapabilities();

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

    void removeClientEntity(Player viewer, int entityId);

    void setCameraEntity(Player viewer, int entityId);

    void resetCamera(Player viewer);

    void setEntityGlowingForViewer(Player viewer, Entity target, boolean glowing);

    void setEntityGlowingForViewer(Player viewer, Player target, boolean glowing);

    void showWarningBorder(Player viewer, float strength);

    void resetWarningBorder(Player viewer);

    Entity spawnSeekerAssistant(Plugin plugin, Player seeker, Location location, String assistantSkin);

    void removeAllAssistants(Plugin plugin, UUID seekerId);

    void sendAssistantBeamToAll(Plugin plugin, Location hiderLocation, String color);

    void clearVisibilityFilters();

    void setCameraSessionChecker(Predicate<UUID> checker);

}
