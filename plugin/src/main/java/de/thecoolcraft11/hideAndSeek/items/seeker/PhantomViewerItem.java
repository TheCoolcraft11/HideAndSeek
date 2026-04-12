package de.thecoolcraft11.hideAndSeek.items.seeker;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.items.api.GameItem;
import de.thecoolcraft11.hideAndSeek.items.api.ItemStateManager;
import de.thecoolcraft11.minigameframework.items.CustomItemBuilder;
import de.thecoolcraft11.minigameframework.items.ItemActionType;
import de.thecoolcraft11.minigameframework.items.ItemInteractionContext;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.awt.Color;
import java.lang.reflect.Method;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class PhantomViewerItem implements GameItem {
    public static final String ID = "has_seeker_phantom_viewer";
    private static final int MAP_SIZE = 128;
    private static final double FOV_DEGREES = 68.0;
    private static final double STEP_SIZE = 0.4;
    private static final String SNAPSHOT_TOKEN_KEY = "phantom_viewer_snapshot_token";
    private static final String SNAPSHOT_EXPIRE_KEY = "phantom_viewer_snapshot_expire";
    private static final String SNAPSHOT_APPLIED_KEY = "phantom_viewer_snapshot_applied";
    private static final Map<String, Color> MAP_COLOR_CACHE = new ConcurrentHashMap<>();

    private static void use(ItemInteractionContext context, HideAndSeek plugin) {
        Player seeker = context.getPlayer();
        if (!HideAndSeek.getDataController().getSeekers().contains(seeker.getUniqueId())) {
            seeker.sendMessage(Component.text("Only seekers can use this item.", NamedTextColor.RED));
            context.skipCooldown();
            return;
        }

        int rayDistance = plugin.getSettingRegistry().get("seeker-items.phantom-viewer.ray-distance", 24);
        boolean targetRandom = plugin.getSettingRegistry().get("seeker-items.phantom-viewer.target-random", true);
        boolean showPlayerName = plugin.getSettingRegistry().get("seeker-items.phantom-viewer.show-player-name", false);
        int mapDuration = plugin.getSettingRegistry().get("seeker-items.phantom-viewer.map-duration-seconds", 60);
        boolean applyToViewerItem = plugin.getSettingRegistry().get("seeker-items.phantom-viewer.apply-to-item-map-data", false);
        boolean targetNameInLore = plugin.getSettingRegistry().get("seeker-items.phantom-viewer.target-name-in-lore", false);

        Player target = pickTargetHider(seeker, targetRandom);
        if (target == null) {
            seeker.sendMessage(Component.text("No valid hider target found.", NamedTextColor.RED));
            context.skipCooldown();
            return;
        }

        SnapshotContext snapshotContext = captureSnapshotContext(target, rayDistance);
        if (snapshotContext == null) {
            seeker.sendMessage(Component.text("Could not capture phantom snapshot right now.", NamedTextColor.RED));
            context.skipCooldown();
            return;
        }

        seeker.sendMessage(Component.text("Capturing phantom snapshot...", NamedTextColor.YELLOW));

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            Color[] pixels = renderSnapshotPixels(snapshotContext);
            Bukkit.getScheduler().runTask(plugin, () -> giveSnapshotMap(plugin, seeker, target, pixels, mapDuration, showPlayerName, applyToViewerItem, targetNameInLore));
        });
    }

    private static Player pickTargetHider(Player seeker, boolean targetRandom) {
        List<Player> candidates = new ArrayList<>();
        for (UUID hiderId : HideAndSeek.getDataController().getHiders()) {
            Player hider = Bukkit.getPlayer(hiderId);
            if (hider == null || !hider.isOnline() || !hider.getWorld().equals(seeker.getWorld())) {
                continue;
            }
            candidates.add(hider);
        }

        if (candidates.isEmpty()) {
            return null;
        }

        if (targetRandom) {
            return candidates.get(new Random().nextInt(candidates.size()));
        }

        candidates.sort(Comparator.comparingDouble(h -> h.getLocation().distanceSquared(seeker.getLocation())));
        return candidates.getFirst();
    }

    private static SnapshotContext captureSnapshotContext(Player target, int rayDistance) {
        Location eyeLocation = target.getEyeLocation().clone();
        World world = eyeLocation.getWorld();
        if (world == null) {
            return null;
        }

        Vector forward = eyeLocation.getDirection().normalize();
        Vector worldUp = new Vector(0, 1, 0);
        Vector right = forward.clone().crossProduct(worldUp);
        if (right.lengthSquared() < 1.0E-6) {
            right = new Vector(1, 0, 0);
        }
        right.normalize();
        Vector up = right.clone().crossProduct(forward).normalize();

        int minChunkX = floorToChunk(eyeLocation.getX() - rayDistance - 2);
        int maxChunkX = floorToChunk(eyeLocation.getX() + rayDistance + 2);
        int minChunkZ = floorToChunk(eyeLocation.getZ() - rayDistance - 2);
        int maxChunkZ = floorToChunk(eyeLocation.getZ() + rayDistance + 2);

        Map<Long, ChunkSnapshot> snapshots = new HashMap<>();
        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                if (!world.isChunkLoaded(cx, cz)) {
                    continue;
                }
                ChunkSnapshot snapshot = world.getChunkAt(cx, cz).getChunkSnapshot(true, false, false);
                snapshots.put(chunkKey(cx, cz), snapshot);
            }
        }

        if (snapshots.isEmpty()) {
            return null;
        }

        return new SnapshotContext(
                eyeLocation,
                forward,
                right,
                up,
                rayDistance,
                world.getMinHeight(),
                world.getMaxHeight(),
                snapshots
        );
    }

    private static Color[] renderSnapshotPixels(SnapshotContext context) {
        Color[] pixels = new Color[MAP_SIZE * MAP_SIZE];
        double fovScale = Math.tan(Math.toRadians(FOV_DEGREES / 2.0));

        for (int y = 0; y < MAP_SIZE; y++) {
            double ny = 1.0 - ((y + 0.5) / MAP_SIZE) * 2.0;
            for (int x = 0; x < MAP_SIZE; x++) {
                double nx = ((x + 0.5) / MAP_SIZE) * 2.0 - 1.0;

                Vector ray = context.forward().clone()
                        .add(context.right().clone().multiply(nx * fovScale))
                        .add(context.up().clone().multiply(ny * fovScale))
                        .normalize();

                BlockData hit = traceMaterial(context, ray);
                Color color = toLoFiColor(hit);
                pixels[y * MAP_SIZE + x] = color;
            }
        }


        return pixels;
    }

    private static BlockData traceMaterial(SnapshotContext context, Vector ray) {
        Location start = context.eyeLocation();
        for (double t = 0.0; t <= context.maxDistance(); t += STEP_SIZE) {
            double x = start.getX() + ray.getX() * t;
            double y = start.getY() + ray.getY() * t;
            double z = start.getZ() + ray.getZ() * t;

            int blockY = (int) Math.floor(y);
            if (blockY < context.minY() || blockY >= context.maxY()) {
                continue;
            }

            BlockData blockData = getSnapshotMaterial(context.snapshots(), x, blockY, z);
            if (blockData != null && blockData.getMaterial().isOccluding()) {
                return blockData;
            }
        }

        return null;
    }

    private static BlockData getSnapshotMaterial(Map<Long, ChunkSnapshot> snapshots, double worldX, int worldY, double worldZ) {
        int blockX = (int) Math.floor(worldX);
        int blockZ = (int) Math.floor(worldZ);
        int chunkX = floorToChunk(blockX);
        int chunkZ = floorToChunk(blockZ);

        ChunkSnapshot snapshot = snapshots.get(chunkKey(chunkX, chunkZ));
        if (snapshot == null) {
            return null;
        }

        int localX = Math.floorMod(blockX, 16);
        int localZ = Math.floorMod(blockZ, 16);
        return snapshot.getBlockData(localX, worldY, localZ);
    }

    private static boolean isVisualHitMaterial(Material material) {
        String name = material.name();
        return name.contains("WATER")
                || name.contains("LAVA")
                || name.contains("LEAVES")
                || name.contains("VINE")
                || name.contains("KELP")
                || name.contains("SEAGRASS")
                || name.contains("MOSS")
                || name.contains("GRASS")
                || name.contains("FERN")
                || name.contains("FLOWER");
    }

    private static Color toLoFiColor(BlockData blockData) {
        if (blockData == null || blockData.getMaterial().isAir()) {
            return new Color(100, 150, 230);
        }

        String cacheKey = blockData.getAsString(false);
        return MAP_COLOR_CACHE.computeIfAbsent(cacheKey, ignored -> resolveMapColor(blockData));
    }

    private static Color resolveMapColor(BlockData blockData) {
        Color mapColor = extractMapColor(blockData);
        if (mapColor != null) {
            return mapColor;
        }

        Material material = blockData.getMaterial();
        if (material.name().contains("WATER") || material.name().contains("SEAGRASS") || material.name().contains("KELP")) {
            return new Color(65, 110, 210);
        } else if (material.name().contains("LAVA")) {
            return new Color(230, 100, 20);
        } else if (material.name().contains("LEAVES") || material.name().contains("MOSS") || material.name().contains("GRASS") || material.name().contains("FERN") || material.name().contains("VINE") || material.name().contains("FLOWER")) {
            return new Color(75, 145, 70);
        } else if (material.name().contains("SAND") || material.name().contains("END_STONE")) {
            return new Color(218, 206, 143);
        } else if (material.name().contains("WOOD") || material.name().contains("LOG") || material.name().contains("PLANK")) {
            return new Color(143, 104, 65);
        } else if (material.name().contains("NETHER")) {
            return new Color(130, 48, 48);
        } else {
            return new Color(130, 130, 130);
        }
    }

    private static Color extractMapColor(BlockData blockData) {
        try {
            Method getMapColorMethod = blockData.getClass().getMethod("getMapColor");
            Object mapColorObj = getMapColorMethod.invoke(blockData);
            if (mapColorObj == null) {
                return null;
            }

            if (mapColorObj instanceof Color awtColor) {
                return awtColor;
            }

            Object rgbObject = tryInvokeNoArgs(mapColorObj, "asRGB");
            if (rgbObject instanceof Number rgb) {
                return colorFromRgbInt(rgb.intValue());
            }

            Object red = tryInvokeNoArgs(mapColorObj, "red");
            Object green = tryInvokeNoArgs(mapColorObj, "green");
            Object blue = tryInvokeNoArgs(mapColorObj, "blue");
            if (red instanceof Number r && green instanceof Number g && blue instanceof Number b) {
                return new Color(clampColor(r.intValue()), clampColor(g.intValue()), clampColor(b.intValue()));
            }

            Object getColor = tryInvokeNoArgs(mapColorObj, "getColor");
            if (getColor instanceof Color awtColor) {
                return awtColor;
            }
            if (getColor != null) {
                Object nestedRgb = tryInvokeNoArgs(getColor, "asRGB");
                if (nestedRgb instanceof Number rgb) {
                    return colorFromRgbInt(rgb.intValue());
                }
            }
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
        return null;
    }

    private static Object tryInvokeNoArgs(Object target, String methodName) {
        try {
            Method method = target.getClass().getMethod(methodName);
            return method.invoke(target);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static Color colorFromRgbInt(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        return new Color(r, g, b);
    }

    private static int clampColor(int value) {
        return Math.clamp(value, 0, 255);
    }

    private static void giveSnapshotMap(HideAndSeek plugin, Player seeker, Player target, Color[] pixels, int mapDurationSeconds, boolean showPlayerName, boolean applyToViewerItem, boolean targetNameInLore) {
        if (!seeker.isOnline()) {
            return;
        }

        MapView mapView = Bukkit.createMap(target.getWorld());
        mapView.setScale(MapView.Scale.CLOSE);
        mapView.setTrackingPosition(false);
        mapView.setUnlimitedTracking(false);
        mapView.setLocked(true);
        mapView.getRenderers().clear();
        mapView.addRenderer(new PhantomSnapshotRenderer(pixels));

        long expiryAt = System.currentTimeMillis() + (Math.max(1, mapDurationSeconds) * 1000L);
        String token = UUID.randomUUID().toString();

        if (applyToViewerItem && plugin.getCustomItemManager().hasItemInMainHand(seeker, ID)) {
            ItemStack held = seeker.getInventory().getItemInMainHand();
            if (held.getType() == Material.FILLED_MAP && held.getItemMeta() instanceof MapMeta heldMeta) {
                heldMeta.setMapView(mapView);
                heldMeta.displayName(Component.text("Phantom Viewer", NamedTextColor.DARK_AQUA, TextDecoration.BOLD)
                        .decoration(TextDecoration.ITALIC, false));
                List<Component> lore = new ArrayList<>();
                lore.add(Component.text("Right click to capture a lo-fi phantom snapshot", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false));
                if (targetNameInLore) {
                    lore.add(Component.text("Snapshot: " + target.getName(), NamedTextColor.AQUA)
                            .decoration(TextDecoration.ITALIC, false));
                }
                heldMeta.lore(lore);
                tagSnapshot(plugin, heldMeta.getPersistentDataContainer(), token, expiryAt, true);
                held.setItemMeta(heldMeta);

                Bukkit.getScheduler().runTaskLater(plugin, () -> expireTokenFromPlayerInventory(plugin, seeker, token), Math.max(1, mapDurationSeconds) * 20L);

                if (showPlayerName) {
                    seeker.sendMessage(Component.text("PhantomViewer captured " + target.getName() + "'s perspective.", NamedTextColor.GREEN));
                } else {
                    seeker.sendMessage(Component.text("PhantomViewer captured a phantom perspective.", NamedTextColor.GREEN));
                }
                return;
            }
        }

        ItemStack mapItem = new ItemStack(Material.FILLED_MAP);
        MapMeta mapMeta = (MapMeta) mapItem.getItemMeta();
        if (mapMeta == null) {
            return;
        }

        mapMeta.setMapView(mapView);
        mapMeta.displayName(Component.text("Phantom Snapshot", NamedTextColor.AQUA)
                .decoration(TextDecoration.ITALIC, false));
        tagSnapshot(plugin, mapMeta.getPersistentDataContainer(), token, expiryAt, false);
        mapItem.setItemMeta(mapMeta);

        seeker.getInventory().addItem(mapItem);

        if (showPlayerName) {
            seeker.sendMessage(Component.text("PhantomViewer captured " + target.getName() + "'s perspective.", NamedTextColor.GREEN));
        } else {
            seeker.sendMessage(Component.text("PhantomViewer captured a phantom perspective.", NamedTextColor.GREEN));
        }

        ItemStateManager.phantomViewerMapExpiry.put(seeker.getUniqueId(), expiryAt);

        Bukkit.getScheduler().runTaskLater(plugin, () -> expireTokenFromPlayerInventory(plugin, seeker, token), Math.max(1, mapDurationSeconds) * 20L);
    }

    private static void tagSnapshot(HideAndSeek plugin, PersistentDataContainer container, String token, long expiryAt, boolean appliedToViewerItem) {
        container.set(new NamespacedKey(plugin, SNAPSHOT_TOKEN_KEY), PersistentDataType.STRING, token);
        container.set(new NamespacedKey(plugin, SNAPSHOT_EXPIRE_KEY), PersistentDataType.LONG, expiryAt);
        container.set(new NamespacedKey(plugin, SNAPSHOT_APPLIED_KEY), PersistentDataType.BOOLEAN, appliedToViewerItem);
    }

    public static boolean isSnapshotNotTagged(HideAndSeek plugin, ItemStack stack) {
        if (plugin == null || stack == null || stack.getType() != Material.FILLED_MAP) {
            return true;
        }
        if (!(stack.getItemMeta() instanceof MapMeta mapMeta)) {
            return true;
        }
        String token = mapMeta.getPersistentDataContainer().get(new NamespacedKey(plugin, SNAPSHOT_TOKEN_KEY), PersistentDataType.STRING);
        return token == null || token.isBlank();
    }

    public static long getSnapshotExpiryMs(HideAndSeek plugin, ItemStack stack) {
        if (plugin == null || stack == null || stack.getType() != Material.FILLED_MAP) {
            return 0L;
        }
        if (!(stack.getItemMeta() instanceof MapMeta mapMeta)) {
            return 0L;
        }
        Long expiry = mapMeta.getPersistentDataContainer().get(new NamespacedKey(plugin, SNAPSHOT_EXPIRE_KEY), PersistentDataType.LONG);
        return expiry == null ? 0L : expiry;
    }

    public static boolean expireSnapshotStack(HideAndSeek plugin, ItemStack stack) {
        if (plugin == null || stack == null || stack.getType() != Material.FILLED_MAP) {
            return false;
        }
        if (!(stack.getItemMeta() instanceof MapMeta mapMeta)) {
            return true;
        }

        NamespacedKey appliedKey = new NamespacedKey(plugin, SNAPSHOT_APPLIED_KEY);
        NamespacedKey tokenKey = new NamespacedKey(plugin, SNAPSHOT_TOKEN_KEY);
        NamespacedKey expireKey = new NamespacedKey(plugin, SNAPSHOT_EXPIRE_KEY);
        boolean applied = Boolean.TRUE.equals(mapMeta.getPersistentDataContainer().get(appliedKey, PersistentDataType.BOOLEAN));

        if (!applied) {
            return true;
        }

        mapMeta.setMapView(null);
        mapMeta.displayName(Component.text("Phantom Viewer", NamedTextColor.DARK_AQUA, TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));
        mapMeta.lore(List.of(
                Component.text("Right click to capture a lo-fi phantom snapshot", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)
        ));
        mapMeta.getPersistentDataContainer().remove(tokenKey);
        mapMeta.getPersistentDataContainer().remove(appliedKey);
        mapMeta.getPersistentDataContainer().remove(expireKey);
        stack.setItemMeta(mapMeta);
        return false;
    }

    private static void expireTokenFromPlayerInventory(HideAndSeek plugin, Player player, String token) {
        if (player == null || !player.isOnline()) {
            return;
        }

        NamespacedKey tokenKey = new NamespacedKey(plugin, SNAPSHOT_TOKEN_KEY);
        NamespacedKey appliedKey = new NamespacedKey(plugin, SNAPSHOT_APPLIED_KEY);

        for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack == null || stack.getType() != Material.FILLED_MAP) {
                continue;
            }

            if (!(stack.getItemMeta() instanceof MapMeta mapMeta)) {
                continue;
            }

            String found = mapMeta.getPersistentDataContainer().get(tokenKey, PersistentDataType.STRING);
            if (!token.equals(found)) {
                continue;
            }

            boolean applied = Boolean.TRUE.equals(mapMeta.getPersistentDataContainer().get(appliedKey, PersistentDataType.BOOLEAN));
            if (applied) {
                expireSnapshotStack(plugin, stack);
            } else {
                player.getInventory().setItem(slot, null);
            }
        }
    }

    public static void clearPlayerState(UUID playerId) {
        if (playerId != null) {
            ItemStateManager.phantomViewerMapExpiry.remove(playerId);
        }
    }

    private static long chunkKey(int chunkX, int chunkZ) {
        return (((long) chunkX) << 32) ^ (chunkZ & 0xFFFFFFFFL);
    }

    private static int floorToChunk(double blockCoordinate) {
        return (int) Math.floor(blockCoordinate) >> 4;
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public ItemStack createItem(HideAndSeek plugin) {
        ItemStack item = new ItemStack(Material.FILLED_MAP);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Phantom Viewer", NamedTextColor.DARK_AQUA, TextDecoration.BOLD)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.text("Right click to capture a lo-fi phantom snapshot", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false)
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    public String getDescription(HideAndSeek plugin) {
        int rayDistance = plugin.getSettingRegistry().get("seeker-items.phantom-viewer.ray-distance", 24);
        return "Capture a rough map snapshot from a hider perspective (" + rayDistance + " blocks).";
    }

    @Override
    public void register(HideAndSeek plugin) {
        int cooldown = plugin.getSettingRegistry().get("seeker-items.phantom-viewer.cooldown", 45);
        plugin.getCustomItemManager().registerItem(new CustomItemBuilder(createItem(plugin), getId())
                .withAction(ItemActionType.RIGHT_CLICK_AIR, context -> use(context, plugin))
                .withAction(ItemActionType.RIGHT_CLICK_BLOCK, context -> use(context, plugin))
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

    @Override
    public Set<String> getConfigKeys() {
        return Set.of(
                "seeker-items.phantom-viewer.cooldown",
                "seeker-items.phantom-viewer.apply-to-item-map-data",
                "seeker-items.phantom-viewer.target-name-in-lore"
        );
    }

    private record SnapshotContext(
            Location eyeLocation,
            Vector forward,
            Vector right,
            Vector up,
            int maxDistance,
            int minY,
            int maxY,
            Map<Long, ChunkSnapshot> snapshots
    ) {
    }

    private static final class PhantomSnapshotRenderer extends MapRenderer {
        private final Color[] pixels;
        private final Set<UUID> renderedFor = ConcurrentHashMap.newKeySet();

        private PhantomSnapshotRenderer(Color[] pixels) {
            super(true);
            this.pixels = pixels;
        }

        @Override
        public void render(@NotNull MapView map, @NotNull MapCanvas canvas, @NotNull Player player) {
            if (renderedFor.contains(player.getUniqueId())) {
                return;
            }
            for (int y = 0; y < MAP_SIZE; y++) {
                for (int x = 0; x < MAP_SIZE; x++) {
                    canvas.setPixelColor(x, y, pixels[y * MAP_SIZE + x]);
                }
            }
            renderedFor.add(player.getUniqueId());
        }
    }
}
