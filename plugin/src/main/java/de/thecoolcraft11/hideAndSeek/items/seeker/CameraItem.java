package de.thecoolcraft11.hideAndSeek.items.seeker;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.items.api.GameItem;
import de.thecoolcraft11.hideAndSeek.items.api.ItemStateManager;
import de.thecoolcraft11.hideAndSeek.model.GameModeEnum;
import de.thecoolcraft11.minigameframework.items.CustomItemBuilder;
import de.thecoolcraft11.minigameframework.items.ItemActionType;
import de.thecoolcraft11.minigameframework.items.ItemInteractionContext;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Lightable;
import org.bukkit.block.data.type.RedstoneWallTorch;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static de.thecoolcraft11.hideAndSeek.items.api.ItemStateManager.activeCameraSessions;
import static de.thecoolcraft11.hideAndSeek.items.api.ItemStateManager.seekerCameras;

public class CameraItem implements GameItem {
    public static final String ID = "has_seeker_camera";
    private static final String HEAD_TEXTURE = "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvZWE3ZDJhN2ZiYjRkMzdiNGQ1M2ZlODc3NTcxMjhlNWVmNjZlYzIzZDdmZjRmZTk5NDQ1NDZkYmM4Y2U3NzcifX19";

    private static void enterCameraMode(ItemInteractionContext context, HideAndSeek plugin) {
        Player player = context.getPlayer();

        if (activeCameraSessions.containsKey(player.getUniqueId())) {
            context.skipCooldown();
            return;
        }

        if (!startCameraSession(player, plugin, 0)) {
            context.skipCooldown();
        }
    }

    private static void placeCamera(ItemInteractionContext context, HideAndSeek plugin) {
        Block clickedBlock = context.getLocation().getBlock();
        Player player = context.getPlayer();

        if (!plugin.getNmsAdapter().hasNmsCapabilities()) {
            player.sendMessage(Component.text("The Seeker's Assistant is not available on this server version.", NamedTextColor.RED));
            return;
        }

        if (!clickedBlock.getType().isSolid()) {
            player.sendMessage(Component.text("Cannot place camera - need solid block!", NamedTextColor.RED));
            context.skipCooldown();
            return;
        }

        RayTraceResult rayTrace = player.getWorld().rayTraceBlocks(
                player.getEyeLocation(),
                player.getLocation().getDirection(),
                5.0
        );

        BlockFace clickedFace = BlockFace.UP;
        if (rayTrace != null && rayTrace.getHitBlockFace() != null) {
            clickedFace = rayTrace.getHitBlockFace();
        }

        if (clickedFace == BlockFace.DOWN) {
            player.sendMessage(Component.text("Cannot place camera on ceiling!", NamedTextColor.RED));
            context.skipCooldown();
            return;
        }

        Block torchBlock = clickedBlock.getRelative(clickedFace);
        if (!torchBlock.getType().isAir()) {
            player.sendMessage(Component.text("Cannot place camera here - space is occupied!", NamedTextColor.RED));
            context.skipCooldown();
            return;
        }

        placeTorchBase(torchBlock, clickedFace);
        float placementYaw = normalizeYaw(player.getLocation().getYaw());
        ItemDisplay display = spawnCameraDisplay(plugin, torchBlock.getLocation(), clickedFace, placementYaw);

        ItemStateManager.PlacedCamera camera = new ItemStateManager.PlacedCamera(
                player.getUniqueId(),
                torchBlock.getLocation().clone(),
                clickedFace,
                placementYaw,
                display
        );

        removeCameraAt(torchBlock.getLocation());

        LinkedList<ItemStateManager.PlacedCamera> cameras = seekerCameras.computeIfAbsent(player.getUniqueId(), ignored -> new LinkedList<>());
        cameras.add(camera);

        int maxCameras = Math.max(1, plugin.getSettingRegistry().get("seeker-items.camera.max-placed", 5));
        while (cameras.size() > maxCameras) {
            removePlacedCamera(cameras.removeFirst());

            ItemStateManager.CameraSessionState state = activeCameraSessions.get(player.getUniqueId());
            if (state != null) {
                state.currentIndex(Math.max(0, state.currentIndex() - 1));
            }
        }

        player.sendMessage(Component.text("Camera placed! (" + cameras.size() + "/" + maxCameras + ")", NamedTextColor.GREEN));
    }

    private static void placeTorchBase(Block torchBlock, BlockFace clickedFace) {
        if (clickedFace == BlockFace.UP) {
            BlockData torchData = Material.REDSTONE_TORCH.createBlockData();
            if (torchData instanceof Lightable lightable) {
                lightable.setLit(false);
            }
            torchBlock.setBlockData(torchData, false);
            return;
        }

        BlockData torchData = Material.REDSTONE_WALL_TORCH.createBlockData();
        if (torchData instanceof Lightable lightable) {
            lightable.setLit(false);
        }
        if (torchData instanceof RedstoneWallTorch wallTorch) {
            wallTorch.setFacing(clickedFace);
        }
        torchBlock.setBlockData(torchData, false);
    }

    private static ItemDisplay spawnCameraDisplay(HideAndSeek plugin, Location baseLocation, BlockFace face, float placementYaw) {
        Location spawn = baseLocation.clone().add(0.5, 0.5, 0.5);
        return spawn.getWorld().spawn(spawn, ItemDisplay.class, d -> {
            d.setItemStack(createCameraHeadItem());
            d.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
            d.getPersistentDataContainer().set(new NamespacedKey(plugin, "camera-head"), PersistentDataType.BOOLEAN, true);
            d.getPersistentDataContainer().set(new NamespacedKey(plugin, "camera-face"), PersistentDataType.STRING, face.name());
            d.setTransformation(getHeadTransformation(face, placementYaw));
        });
    }

    private static Transformation getHeadTransformation(BlockFace face, float placementYaw) {
        AxisAngle4f placementRotation = new AxisAngle4f((float) Math.toRadians(placementYaw), 0f, 1f, 0f);
        AxisAngle4f wallRotation = new AxisAngle4f((float) Math.toRadians(placementYaw + 180f), 0f, 1f, 0f);

        if (face == BlockFace.UP) {
            return new Transformation(
                    new Vector3f(0f, -0.03f, 0f),
                    new AxisAngle4f(0, 0, 0, 0),
                    new Vector3f(0.58f, 0.58f, 0.58f),
                    placementRotation
            );
        }

        return switch (face) {
            case NORTH -> new Transformation(
                    addHeadLocalOffset(new Vector3f(0f, 0.02f, 0.26f), face),
                    new AxisAngle4f(0.4f, -1f, 0f, 0f),
                    new Vector3f(0.58f, 0.58f, 0.58f),
                    wallRotation
            );
            case SOUTH -> new Transformation(
                    addHeadLocalOffset(new Vector3f(0f, 0.02f, -0.26f), face),
                    new AxisAngle4f(0.4f, 1f, 0f, 0f),
                    new Vector3f(0.58f, 0.58f, 0.58f),
                    wallRotation
            );
            case EAST -> new Transformation(
                    addHeadLocalOffset(new Vector3f(-0.26f, 0.02f, 0f), face),
                    new AxisAngle4f(0.4f, 0f, 0f, -1f),
                    new Vector3f(0.58f, 0.58f, 0.58f),
                    wallRotation
            );
            case WEST -> new Transformation(
                    addHeadLocalOffset(new Vector3f(0.26f, 0.02f, 0f), face),
                    new AxisAngle4f(0.4f, 0f, 0f, 1f),
                    new Vector3f(0.58f, 0.58f, 0.58f),
                    wallRotation
            );
            default -> new Transformation(
                    new Vector3f(0f, -0.03f, 0f),
                    new AxisAngle4f(0, 0, 0, 0),
                    new Vector3f(0.58f, 0.58f, 0.58f),
                    placementRotation
            );
        };
    }

    private static Vector3f addHeadLocalOffset(Vector3f base, BlockFace face) {
        Vector3f adjusted = new Vector3f(base);
        adjusted.y += (float) 0.03;

        Vector3f forwardDir = new Vector3f(
                face.getOppositeFace().getModX(),
                face.getOppositeFace().getModY(),
                face.getOppositeFace().getModZ()
        );
        if (forwardDir.lengthSquared() > 0f) {
            forwardDir.normalize((float) 0.035);
            adjusted.add(forwardDir);
        }

        return adjusted;
    }

    private static ItemStack createCameraHeadItem() {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta == null) return head;

        UUID uuid = UUID.randomUUID();


        PlayerProfile paperProfile = Bukkit.createProfile(uuid, "camera_head");

        paperProfile.setProperty(new ProfileProperty("textures", HEAD_TEXTURE));

        meta.setPlayerProfile(paperProfile);

        head.setItemMeta(meta);
        return head;
    }

    public static LinkedList<ItemStateManager.PlacedCamera> getPlacedCameras(UUID seekerId) {
        return seekerCameras.computeIfAbsent(seekerId, ignored -> new LinkedList<>());
    }

    public static boolean startCameraSession(Player seeker, HideAndSeek plugin, int preferredIndex) {
        LinkedList<ItemStateManager.PlacedCamera> cameras = getPlacedCameras(seeker.getUniqueId());
        if (cameras.isEmpty()) {
            seeker.sendMessage(Component.text("You have no cameras placed.", NamedTextColor.RED));
            return false;
        }

        int idx = Math.clamp(preferredIndex, 0, cameras.size() - 1);
        ItemStateManager.PlacedCamera camera = cameras.get(idx);
        float baseYaw = getBaseYaw(camera);

        ItemStateManager.CameraSessionState state = new ItemStateManager.CameraSessionState(idx, baseYaw);
        activeCameraSessions.put(seeker.getUniqueId(), state);

        return attachToCurrentCamera(seeker, plugin);
    }

    public static boolean attachToCurrentCamera(Player seeker, HideAndSeek plugin) {
        ItemStateManager.CameraSessionState state = activeCameraSessions.get(seeker.getUniqueId());
        if (state == null) {
            return false;
        }

        LinkedList<ItemStateManager.PlacedCamera> cameras = getPlacedCameras(seeker.getUniqueId());
        if (cameras.isEmpty()) {
            stopCameraSession(seeker, plugin, true);
            seeker.sendMessage(Component.text("All your cameras are gone.", NamedTextColor.RED));
            return false;
        }

        int idx = Math.floorMod(state.currentIndex(), cameras.size());
        state.currentIndex(idx);
        ItemStateManager.PlacedCamera camera = cameras.get(idx);

        if (state.fakeEntityId() != Integer.MIN_VALUE) {
            plugin.getNmsAdapter().removeClientEntity(seeker, state.fakeEntityId());
            state.fakeEntityId(Integer.MIN_VALUE);
        }

        Location viewLocation = getViewLocation(camera);
        EntityType entityType = state.nightVision() ? EntityType.CREEPER : EntityType.ARMOR_STAND;

        int entityId = plugin.getNmsAdapter().spawnClientCameraEntity(
                seeker,
                viewLocation,
                state.rotationYaw(),
                0f,
                entityType
        );

        if (entityId == Integer.MIN_VALUE) {
            stopCameraSession(seeker, plugin, true);
            seeker.sendMessage(Component.text("Your client does not support camera mode on this server build.", NamedTextColor.RED));
            return false;
        }

        state.fakeEntityId(entityId);
        plugin.getNmsAdapter().setCameraEntity(seeker, entityId);
        updateTorchIndicators(seeker.getUniqueId());

        if (state.nightVision()) {
            setViewerGlow(seeker, plugin, true);
        }

        return true;
    }

    public static void stopCameraSession(Player seeker, HideAndSeek plugin, boolean resetCameraPacket) {
        ItemStateManager.CameraSessionState state = activeCameraSessions.remove(seeker.getUniqueId());
        if (state == null) {
            return;
        }

        if (state.fakeEntityId() != Integer.MIN_VALUE) {
            plugin.getNmsAdapter().removeClientEntity(seeker, state.fakeEntityId());
        }

        setViewerGlow(seeker, plugin, false);
        updateTorchIndicators(seeker.getUniqueId());

        if (resetCameraPacket) {
            plugin.getNmsAdapter().resetCamera(seeker);
        }
    }

    public static void removeCameraAt(Location torchLocation) {
        if (torchLocation == null) {
            return;
        }

        for (Map.Entry<UUID, LinkedList<ItemStateManager.PlacedCamera>> entry : seekerCameras.entrySet()) {
            LinkedList<ItemStateManager.PlacedCamera> cameras = entry.getValue();
            ItemStateManager.PlacedCamera matched = null;
            for (ItemStateManager.PlacedCamera camera : cameras) {
                if (sameBlock(camera.torchLocation(), torchLocation)) {
                    matched = camera;
                    break;
                }
            }

            if (matched != null) {
                cameras.remove(matched);
                removePlacedCamera(matched);
                break;
            }
        }
    }

    public static void clearPlacedCameras(UUID seekerId) {
        LinkedList<ItemStateManager.PlacedCamera> cameras = seekerCameras.remove(seekerId);
        if (cameras == null) {
            return;
        }

        for (ItemStateManager.PlacedCamera camera : cameras) {
            removePlacedCamera(camera);
        }
    }

    public static void clearAllCameraState(HideAndSeek plugin) {
        for (UUID viewerId : List.copyOf(activeCameraSessions.keySet())) {
            Player viewer = plugin.getServer().getPlayer(viewerId);
            if (viewer != null && viewer.isOnline()) {
                stopCameraSession(viewer, plugin, true);
            } else {
                activeCameraSessions.remove(viewerId);
            }
        }

        for (LinkedList<ItemStateManager.PlacedCamera> cameras : seekerCameras.values()) {
            for (ItemStateManager.PlacedCamera camera : cameras) {
                removePlacedCamera(camera);
            }
        }
        seekerCameras.clear();
    }

    public static void removePlacedCamera(ItemStateManager.PlacedCamera camera) {
        if (camera == null) {
            return;
        }

        Block block = camera.torchLocation().getBlock();
        if (block.getType() == Material.REDSTONE_TORCH || block.getType() == Material.REDSTONE_WALL_TORCH) {
            block.setType(Material.AIR, false);
        }

        if (camera.headDisplay() != null && camera.headDisplay().isValid()) {
            camera.headDisplay().remove();
        }
    }

    private static void updateTorchIndicators(UUID seekerId) {
        LinkedList<ItemStateManager.PlacedCamera> cameras = getPlacedCameras(seekerId);
        ItemStateManager.CameraSessionState state = activeCameraSessions.get(seekerId);
        int activeIndex = state == null ? -1 : Math.floorMod(state.currentIndex(), Math.max(1, cameras.size()));

        for (int i = 0; i < cameras.size(); i++) {
            setTorchLit(cameras.get(i).torchLocation(), i == activeIndex);
        }
    }

    private static void setTorchLit(Location torchLocation, boolean lit) {
        if (torchLocation == null) {
            return;
        }

        Block torchBlock = torchLocation.getBlock();
        Material type = torchBlock.getType();
        if (type != Material.REDSTONE_TORCH && type != Material.REDSTONE_WALL_TORCH) {
            return;
        }

        BlockData data = torchBlock.getBlockData();
        if (data instanceof Lightable lightable) {
            if (lightable.isLit() == lit) {
                return;
            }
            lightable.setLit(lit);
            torchBlock.setBlockData(lightable, false);
        }
    }

    public static Location getViewLocation(ItemStateManager.PlacedCamera camera) {
        Location loc = camera.torchLocation().clone().add(0.5, 0.66, 0.5);

        if (camera.facing() == BlockFace.UP) {
            loc.add(0, 0.08, 0);
            return loc;
        }


        loc.add(camera.facing().getOppositeFace().getDirection().multiply(0.26));
        loc.add(0, 0.06, 0);

        return loc;
    }

    public static float getBaseYaw(ItemStateManager.PlacedCamera camera) {
        return normalizeYaw(camera.placementYaw());
    }

    private static float normalizeYaw(float yaw) {
        float normalized = yaw % 360f;
        if (normalized < -180f) {
            normalized += 360f;
        }
        if (normalized > 180f) {
            normalized -= 360f;
        }
        return normalized;
    }

    private static boolean sameBlock(Location a, Location b) {
        if (a == null || b == null || a.getWorld() == null || b.getWorld() == null) {
            return false;
        }

        return a.getWorld().getUID().equals(b.getWorld().getUID())
                && a.getBlockX() == b.getBlockX()
                && a.getBlockY() == b.getBlockY()
                && a.getBlockZ() == b.getBlockZ();
    }

    public static void setViewerGlow(Player viewer, HideAndSeek plugin, boolean glowing) {
        var gameModeResult = plugin.getSettingService().getSetting("game.mode");
        Object gameModeObj = gameModeResult.isSuccess() ? gameModeResult.getValue() : GameModeEnum.NORMAL;
        GameModeEnum gameMode = (gameModeObj instanceof GameModeEnum) ?
                (GameModeEnum) gameModeObj : GameModeEnum.NORMAL;

        for (UUID hiderId : HideAndSeek.getDataController().getHiders()) {
            Player hider = plugin.getServer().getPlayer(hiderId);
            if (hider == null || !hider.isOnline() || !hider.getWorld().equals(viewer.getWorld())) {
                continue;
            }

            if (gameMode == GameModeEnum.BLOCK) {
                BlockDisplay display = HideAndSeek.getDataController().getBlockDisplay(hiderId);
                if (display != null && display.isValid()) {
                    display.setGlowing(glowing);
                }
            } else {
                plugin.getNmsAdapter().setEntityGlowingForViewer(viewer, hider, glowing);
            }
        }
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public ItemStack createItem(HideAndSeek plugin) {
        ItemStack item = createCameraHeadItem();
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Camera", NamedTextColor.DARK_AQUA, TextDecoration.BOLD)
                    .decoration(TextDecoration.ITALIC, false));

            List<Component> lore = new LinkedList<>(List.of(
                    Component.text("Shift + right click block to place", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.text("Right click to watch your cameras", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
            ));

            if (!plugin.getNmsAdapter().hasNmsCapabilities()) {
                lore.add(Component.text("Not available on this server version", NamedTextColor.DARK_RED)
                        .decoration(TextDecoration.ITALIC, false));
            }

            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    public String getDescription(HideAndSeek plugin) {
        int maxCameras = plugin.getSettingRegistry().get("seeker-items.camera.max-placed", 5);
        return String.format("Place up to %d cameras and spectate through them.", maxCameras);
    }

    @Override
    public void register(HideAndSeek plugin) {
        if (!plugin.getNmsAdapter().hasNmsCapabilities()) {
            return;
        }

        int cooldown = plugin.getSettingRegistry().get("seeker-items.camera.cooldown", 2);

        plugin.getCustomItemManager().registerItem(new CustomItemBuilder(createItem(plugin), getId())
                .withAction(ItemActionType.SHIFT_RIGHT_CLICK_BLOCK, context -> placeCamera(context, plugin))
                .withAction(ItemActionType.RIGHT_CLICK_BLOCK, context -> enterCameraMode(context, plugin))
                .withAction(ItemActionType.RIGHT_CLICK_AIR, context -> enterCameraMode(context, plugin))
                .withDescription(getDescription(plugin))
                .withDropPrevention(true)
                .withCraftPrevention(true)
                .withVanillaCooldown(cooldown * 20)
                .withCustomCooldown(cooldown * 1000L)
                .withVanillaCooldownDisplay(true)
                .allowOffHand(false)
                .allowArmor(false)
                .cancelDefaultAction(true)
                .build());
    }
}
