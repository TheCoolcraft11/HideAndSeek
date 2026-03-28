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

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

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
    public boolean hasNmsCapabilities() {
        return false;
    }

    @Override
    public List<BoundingBox> getBoundingBoxes(BlockData blockData, Location location) {
        return Collections.emptyList();
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

    @Override
    public boolean spawnClientLightning(Player viewer, Location location) {
        return false;
    }

    @Override
    public Entity raycastEntityHit(Player shooter, Location start, Vector direction, double distance, double hitboxInflation, Predicate<Entity> filter) {
        return null;
    }

    @Override
    public boolean setEntityVisibilityForViewer(Player viewer, Player target, boolean visible) {
        return false;
    }

    @Override
    public int spawnClientCameraEntity(Player viewer, Location location, float yaw, float pitch, EntityType entityType) {
        return Integer.MIN_VALUE;
    }

    @Override
    public boolean removeClientEntity(Player viewer, int entityId) {
        return false;
    }

    @Override
    public boolean setCameraEntity(Player viewer, int entityId) {
        return false;
    }

    @Override
    public boolean resetCamera(Player viewer) {
        return false;
    }

    @Override
    public boolean setEntityGlowingForViewer(Player viewer, Player target, boolean glowing) {
        return false;
    }

    @Override
    public Entity spawnSeekerAssistant(Plugin plugin, Player seeker, Location location) {
        return null;
    }

    @Override
    public void removeAllAssistants(Plugin plugin, UUID seekerId) {
    }

    @Override
    public void sendAssistantBeamToAll(Plugin plugin, Location hiderLocation, String color) {
    }

    @Override
    public void clearVisibilityFilters() {
    }
}
