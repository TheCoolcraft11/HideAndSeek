package de.thecoolcraft11.hideAndSeek.nms.impl.v1_21_11;

import de.thecoolcraft11.hideAndSeek.nms.NmsAdapter;
import de.thecoolcraft11.hideAndSeek.nms.NmsCapabilities;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.block.data.BlockData;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Predicate;

@SuppressWarnings("unused")
public class NmsAdapterImpl implements NmsAdapter {

    private static final Set<NmsCapabilities> CAPS = EnumSet.of(
            NmsCapabilities.BLOCK_VOXEL_SHAPE,
            NmsCapabilities.MOB_PATHFINDING,
            NmsCapabilities.CLIENT_GAMEMODE_SPOOFING,
            NmsCapabilities.NO_CLIP_MOB,
            NmsCapabilities.CLIENT_LIGHTNING_PACKET,
            NmsCapabilities.PROJECTILE_ENTITY_RAYCAST,

            NmsCapabilities.ANTI_CHEAT_PACKET_FILTER,
            NmsCapabilities.CHANNEL_PACKET_INTERCEPTION,

            NmsCapabilities.CLIENT_ENTITY_SPAWNING,
            NmsCapabilities.CLIENT_ENTITY_REMOVAL,
            NmsCapabilities.CLIENT_ENTITY_VISIBILITY,
            NmsCapabilities.CLIENT_ENTITY_GLOWING,

            NmsCapabilities.CLIENT_CAMERA_SPOOFING,
            NmsCapabilities.CLIENT_CAMERA_ENTITY,

            NmsCapabilities.CLIENT_FAKE_BORDER_WARNING,

            NmsCapabilities.CLIENT_BLOCK_ENTITY_SPOOFING,
            NmsCapabilities.CLIENT_TEST_BLOCK_BEAM,

            NmsCapabilities.CUSTOM_ENTITY_GOALS,
            NmsCapabilities.PER_PLAYER_DIALOG_REGISTRY
    );

    private final Map<UUID, Set<Integer>> blockedEntityIdsByViewer = new ConcurrentHashMap<>();

    private final PacketFilterHandler packetFilter;
    private final EntityVisibilityHandler visibility;
    private final CameraHandler camera;
    private final GlowHandler glow;
    private final de.thecoolcraft11.hideAndSeek.nms.impl.v1_21_11.GameModeHandler gameMode;
    private final WorldBorderHandler worldBorder;
    private final LightningHandler lightning;
    private final RaycastHandler raycast;
    private final BlockShapeHandler blockShape;
    private final PathfindingHandler pathfinding;
    private final BeamHandler beam;
    private final DialogFilterHandler dialogFilter;
    private final SeekerAssistantHandler seekerAssistantHandler;

    public NmsAdapterImpl() {
        Map<UUID, Boolean> hadAllowedFlight = new ConcurrentHashMap<>();

        Map<UUID, Map<Integer, net.minecraft.world.entity.Entity>> clientCameraEntities = new ConcurrentHashMap<>();
        this.packetFilter = new PacketFilterHandler(
                blockedEntityIdsByViewer,
                clientCameraEntities,
                uuid -> false
        );
        this.visibility = new EntityVisibilityHandler(blockedEntityIdsByViewer, packetFilter);
        this.camera = new CameraHandler(clientCameraEntities, hadAllowedFlight);
        this.glow = new GlowHandler();
        this.gameMode = new GameModeHandler();
        this.worldBorder = new WorldBorderHandler();
        this.lightning = new LightningHandler();
        this.raycast = new RaycastHandler();
        this.blockShape = new BlockShapeHandler();
        this.pathfinding = new PathfindingHandler();
        this.beam = new BeamHandler();
        this.dialogFilter = new DialogFilterHandler();
        this.seekerAssistantHandler = new SeekerAssistantHandler();
    }

    @Override
    public String name() {
        return "v1_21_11";
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public Set<NmsCapabilities> capabilities() {
        return CAPS;
    }

    @Override
    public boolean hasNmsCapabilities() {
        return true;
    }

    @Override
    public List<BoundingBox> getBoundingBoxes(BlockData blockData, Location location) {
        return blockShape.getBoundingBoxes(blockData, location);
    }

    @Override
    public boolean canPathfind(Mob mob, Location start, Location end) {
        return pathfinding.canPathfind(mob, start, end);
    }

    @Override
    public void setServerGameModeSpectator(Player player) {
        gameMode.setServerSpectator(player);
    }

    @Override
    public void spoofClientGameMode(Player player, GameMode mode) {
        gameMode.spoofClientGameMode(player, mode);
    }

    @Override
    public void setNoClipForEntity(Entity entity, boolean noClip) {
        net.minecraft.world.entity.Entity serverEntity =
                ((org.bukkit.craftbukkit.entity.CraftEntity) entity).getHandle();
        serverEntity.noPhysics = noClip;
    }

    @Override
    public boolean spawnClientLightning(Player viewer, Location location) {
        return lightning.spawnLightning(viewer, location);
    }

    @Override
    public Entity raycastEntityHit(Player shooter, Location start, Vector direction,
                                   double distance, double hitboxInflation, Predicate<Entity> filter) {
        return raycast.raycast(shooter, start, direction, distance, hitboxInflation, filter);
    }

    @Override
    public boolean setEntityVisibilityForViewer(Player viewer, Player target, boolean visible) {
        return visibility.setVisible(viewer, target, visible);
    }

    @Override
    public int spawnClientCameraEntity(Player viewer, Location location, float yaw, float pitch,
                                       EntityType entityType) {
        return camera.spawnClientEntity(viewer, location, yaw, pitch, entityType);
    }

    @Override
    public void removeClientEntity(Player viewer, int entityId) {
        camera.removeClientEntity(viewer, entityId);
    }

    @Override
    public void setCameraEntity(Player viewer, int entityId) {
        camera.setCamera(viewer, entityId);
    }

    @Override
    public void resetCamera(Player viewer) {
        camera.resetCamera(viewer);
    }

    @Override
    public void setEntityGlowingForViewer(Player viewer, Entity target, boolean glowing) {
        glow.setGlowing(viewer, target, glowing);
    }

    @Override
    public void setEntityGlowingForViewer(Player viewer, Player target, boolean glowing) {
        glow.setGlowing(viewer, target, glowing);
    }

    @Override
    public void showWarningBorder(Player viewer, float strength) {
        worldBorder.showWarning(viewer, strength);
    }

    @Override
    public void resetWarningBorder(Player viewer) {
        worldBorder.resetWarning(viewer);
    }

    @Override
    public void sendAssistantBeamToAll(Plugin plugin, Location hiderLocation, String color) {
        beam.sendToAll(plugin, hiderLocation, color);
    }

    @Override
    public void clearVisibilityFilters() {
        Set<UUID> viewers = new HashSet<>(blockedEntityIdsByViewer.keySet());
        visibility.clearAll();
        camera.clearAll();

        for (UUID viewerId : viewers) {
            Player viewer = Bukkit.getPlayer(viewerId);
            if (viewer == null || !viewer.isOnline()) {
                continue;
            }
            try {
                packetFilter.remove(((CraftPlayer) viewer).getHandle(), viewerId);
            } catch (Throwable ignored) {
            }
        }
    }

    @Override
    public void setCameraSessionChecker(Predicate<UUID> checker) {
        packetFilter.setCameraSessionChecker(checker);
    }

    @Override
    public void injectDialogFilter(UUID playerUuid, Plugin plugin,
                                   BiFunction<String, OfflinePlayer, Boolean> permissionChecker,
                                   BiFunction<String, String, String> translationResolver) {
        dialogFilter.inject(playerUuid, plugin, permissionChecker, translationResolver);
    }

    @Override
    public void removeAllAssistants(Plugin plugin, UUID seekerId) {
        if (plugin == null) {
            return;
        }
        if (seekerId != null) {
            seekerAssistantHandler.removeAssistantsForSeeker(plugin, seekerId);
            return;
        }
        for (UUID id : new HashSet<>(seekerAssistantHandler.getAssistantIdsBySeeker().keySet())) {
            seekerAssistantHandler.removeAssistantsForSeeker(plugin, id);
        }
    }

    @Override
    public Entity spawnSeekerAssistant(Plugin plugin, Player seeker, Location location, String assistantSkin) {
        return seekerAssistantHandler.spawnSeekerAssistant(plugin, seeker, location, assistantSkin);
    }

    @Override
    public void injectSpectatorInventoryHandler(Player player,
                                                java.util.function.IntConsumer onSlotClick) {
        SpectatorInventoryHandler.inject(player, onSlotClick);
    }

    @Override
    public void removeSpectatorInventoryHandler(Player player) {
        SpectatorInventoryHandler.remove(player);
    }

}
