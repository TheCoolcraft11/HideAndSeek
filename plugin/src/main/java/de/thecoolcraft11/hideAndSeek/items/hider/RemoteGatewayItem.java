package de.thecoolcraft11.hideAndSeek.items.hider;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.items.ItemSkinSelectionService;
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
import org.bukkit.block.data.type.Door;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class RemoteGatewayItem implements GameItem {
    public static final String ID = "has_hider_remote_gateway";
    private static final double DEFAULT_TRAVEL_COOLDOWN_SECONDS = 1.5;
    private static final String SKIN_END_RIFT = "skin_end_rift";
    private static final String SKIN_PHASE_DOOR = "skin_phase_door";
    private static final String SKIN_DEMATERIALIZER = "skin_dematerializer";
    private static final String SKIN_VOID_LATTICE = "skin_void_lattice";
    private static final int VOID_LATTICE_RINGS = 5;
    private static final int VOID_LATTICE_BASE_SEGMENTS = 5;
    private static final Map<UUID, String> channelPortalKeyByPlayer = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> channelStartedAtByPlayer = new ConcurrentHashMap<>();
    private static BukkitTask activeGatewayTask;

    private static void placeGateway(ItemInteractionContext context, HideAndSeek plugin) {
        Player player = context.getPlayer();
        if (!HideAndSeek.getDataController().getHiders().contains(player.getUniqueId())) {
            player.sendMessage(Component.text("Only hiders can use this item.", NamedTextColor.RED));
            context.skipCooldown();
            return;
        }

        ensureGatewayTask(plugin);

        if (context.getLocation() == null) {
            player.sendMessage(Component.text("Aim at a block to place a gateway anchor.", NamedTextColor.RED));
            context.skipCooldown();
            return;
        }

        Location location = resolvePlacementLocation(context.getLocation());
        if (location.getWorld() == null) {
            context.skipCooldown();
            return;
        }

        if (!location.getBlock().getType().isAir() || !location.clone().add(0, 1, 0).getBlock().getType().isAir()) {
            player.sendMessage(Component.text("Need clear space to place gateway.", NamedTextColor.RED));
            context.skipCooldown();
            return;
        }

        String skinVariant = resolveGatewaySkin(player.getUniqueId());
        ItemStateManager.GatewayData placed = spawnGatewayDisplay(plugin, location, skinVariant);
        UUID ownerId = player.getUniqueId();

        ItemStateManager.GatewayData pending = ItemStateManager.pendingGatewayByOwner.remove(ownerId);
        if (pending == null) {
            ItemStateManager.pendingGatewayByOwner.put(ownerId, placed);
            player.sendMessage(Component.text("Gateway anchor A placed. Place another anchor to link.", NamedTextColor.GREEN));
            location.getWorld().playSound(location, Sound.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, 0.6f, 1.15f);
            return;
        }

        ItemStateManager.GatewayPairData pair = new ItemStateManager.GatewayPairData(pending, placed, System.currentTimeMillis());
        ItemStateManager.hiderGatewayPairs.computeIfAbsent(ownerId, ignored -> new LinkedList<>()).add(pair);

        int maxPairs = Math.max(1, plugin.getSettingRegistry().get("hider-items.remote-gateway.max-pairs", 1));
        LinkedList<ItemStateManager.GatewayPairData> pairs = ItemStateManager.hiderGatewayPairs.get(ownerId);
        while (pairs != null && pairs.size() > maxPairs) {
            removePair(pairs.removeFirst());
        }

        player.sendMessage(Component.text("Gateway linked.", NamedTextColor.AQUA));
        location.getWorld().spawnParticle(Particle.PORTAL, location.clone().add(0, 0.5, 0), 40, 0.22, 0.6, 0.22, 0.04);
        location.getWorld().playSound(location, Sound.BLOCK_PORTAL_TRIGGER, 0.7f, 1.0f);
    }

    private static ItemStateManager.GatewayData spawnGatewayDisplay(HideAndSeek plugin, Location center, String skinVariant) {
        List<Display> displays = spawnGatewayVisuals(plugin, center, skinVariant);
        return new ItemStateManager.GatewayData(center.clone(), displays, skinVariant, System.currentTimeMillis(), ConcurrentHashMap.newKeySet());
    }

    private static void ensureGatewayTask(HideAndSeek plugin) {
        if (activeGatewayTask != null && !activeGatewayTask.isCancelled()) {
            return;
        }

        activeGatewayTask = Bukkit.getScheduler().runTaskTimer(plugin, () -> tickGateways(plugin), 2L, 3L);
    }

    private static void tickGateways(HideAndSeek plugin) {
        long now = System.currentTimeMillis();
        int durationSeconds = plugin.getSettingRegistry().get("hider-items.remote-gateway.duration-seconds", 120);
        long maxAgeMs = durationSeconds == -1 ? Long.MAX_VALUE : Math.max(1, durationSeconds) * 1000L;
        boolean seekerCanUse = plugin.getSettingRegistry().get("hider-items.remote-gateway.seeker-can-use", false);
        double travelCooldownSeconds = plugin.getSettingRegistry().get("hider-items.remote-gateway.travel-cooldown-seconds", DEFAULT_TRAVEL_COOLDOWN_SECONDS);
        long travelCooldownMs = Math.max(0L, Math.round(travelCooldownSeconds * 1000.0));
        double standSeconds = plugin.getSettingRegistry().get("hider-items.remote-gateway.portal-stand-seconds", 0.0);
        long standMs = Math.max(0L, Math.round(standSeconds * 1000.0));
        Set<UUID> playersInsidePortalThisTick = new HashSet<>();

        ItemStateManager.gatewayTeleportCooldownUntil.entrySet().removeIf(entry -> entry.getValue() <= now);

        for (Map.Entry<UUID, LinkedList<ItemStateManager.GatewayPairData>> entry : ItemStateManager.hiderGatewayPairs.entrySet()) {
            LinkedList<ItemStateManager.GatewayPairData> pairs = entry.getValue();
            Iterator<ItemStateManager.GatewayPairData> iterator = pairs.iterator();

            while (iterator.hasNext()) {
                ItemStateManager.GatewayPairData pair = iterator.next();
                if (!isPairValid(pair) || now - pair.createdAtMs() >= maxAgeMs) {
                    removePair(pair);
                    iterator.remove();
                    continue;
                }

                tickPair(plugin, pair, seekerCanUse, now, travelCooldownMs, standMs, playersInsidePortalThisTick);
            }

            ItemStateManager.GatewayData pending = ItemStateManager.pendingGatewayByOwner.get(entry.getKey());
            if (pending != null) {
                if (!isGatewayValid(pending) || now - pending.placedAtMs() >= maxAgeMs) {
                    removeGateway(pending);
                    ItemStateManager.pendingGatewayByOwner.remove(entry.getKey());
                }
            }
        }

        for (Map.Entry<UUID, ItemStateManager.GatewayData> pendingEntry : ItemStateManager.pendingGatewayByOwner.entrySet()) {
            ItemStateManager.GatewayData pending = pendingEntry.getValue();
            if (!isGatewayValid(pending) || now - pending.placedAtMs() >= maxAgeMs) {
                removeGateway(pending);
                ItemStateManager.pendingGatewayByOwner.remove(pendingEntry.getKey());
                continue;
            }
            tickGatewayParticle(pending, pending.skinVariant());
        }

        ItemStateManager.hiderGatewayPairs.entrySet().removeIf(e -> e.getValue() == null || e.getValue().isEmpty());
        channelPortalKeyByPlayer.entrySet().removeIf(entry -> !playersInsidePortalThisTick.contains(entry.getKey()));
        channelStartedAtByPlayer.entrySet().removeIf(entry -> !playersInsidePortalThisTick.contains(entry.getKey()));
    }

    private static void tickPair(HideAndSeek plugin, ItemStateManager.GatewayPairData pair, boolean seekerCanUse, long nowMs, long travelCooldownMs, long standMs, Set<UUID> playersInsidePortalThisTick) {
        String skinVariant = pair.first().skinVariant() != null ? pair.first().skinVariant() : pair.second().skinVariant();
        if (isAnimatedSkin(skinVariant)) {
            animateGatewayVisuals(pair.first(), nowMs);
            animateGatewayVisuals(pair.second(), nowMs);
        }
        tickGatewayParticle(pair.first(), skinVariant);
        tickGatewayParticle(pair.second(), skinVariant);

        tryTeleport(plugin, pair.first(), pair.second(), seekerCanUse, nowMs, travelCooldownMs, standMs, playersInsidePortalThisTick, skinVariant);
        tryTeleport(plugin, pair.second(), pair.first(), seekerCanUse, nowMs, travelCooldownMs, standMs, playersInsidePortalThisTick, skinVariant);
    }

    private static void tryTeleport(HideAndSeek plugin, ItemStateManager.GatewayData source, ItemStateManager.GatewayData target, boolean seekerCanUse, long nowMs, long travelCooldownMs, long standMs, Set<UUID> playersInsidePortalThisTick, String skinVariant) {
        Location sourceCenter = source.center();
        World world = sourceCenter.getWorld();
        if (world == null) {
            return;
        }

        for (Player player : world.getPlayers()) {
            if (!canUseGateway(player, seekerCanUse)) {
                continue;
            }

            Location feet = player.getLocation();
            double horizontalDistSq = Math.pow(feet.getX() - sourceCenter.getX(), 2) + Math.pow(feet.getZ() - sourceCenter.getZ(), 2);
            double verticalDist = Math.abs(feet.getY() - sourceCenter.getY());
            if (horizontalDistSq > (1.05 * 1.05) || verticalDist > 1.75) {
                continue;
            }

            UUID playerId = player.getUniqueId();
            playersInsidePortalThisTick.add(playerId);

            String sourcePortalKey = gatewayKey(sourceCenter);
            String currentPortalKey = channelPortalKeyByPlayer.get(playerId);
            if (!sourcePortalKey.equals(currentPortalKey)) {
                channelPortalKeyByPlayer.put(playerId, sourcePortalKey);
                channelStartedAtByPlayer.put(playerId, nowMs);
            }
            long channelStartedAt = channelStartedAtByPlayer.getOrDefault(playerId, nowMs);
            if (nowMs - channelStartedAt < standMs) {
                playChargingEffects(sourceCenter, target.center(), nowMs - channelStartedAt, standMs, skinVariant, plugin);
                continue;
            }

            float chargeProgress = standMs <= 0L
                    ? 1.0f
                    : Math.min(1.0f, (float) (nowMs - channelStartedAt) / (float) standMs);

            long cooldownUntil = ItemStateManager.gatewayTeleportCooldownUntil.getOrDefault(playerId, 0L);
            if (cooldownUntil > nowMs || source.teleportCooldowns().contains(playerId)) {
                continue;
            }

            if (HideAndSeek.getDataController().isHidden(player.getUniqueId())) {
                plugin.getBlockModeListener().forceUnhide(player);
            }

            Location destination = target.center().clone().add(0, 0.1, 0);
            destination.setYaw(player.getLocation().getYaw());
            destination.setPitch(player.getLocation().getPitch());
            player.teleport(destination);
            player.setVelocity(player.getLocation().getDirection().normalize().multiply(0.12));

            source.teleportCooldowns().add(playerId);
            target.teleportCooldowns().add(playerId);
            ItemStateManager.gatewayTeleportCooldownUntil.put(playerId, nowMs + travelCooldownMs);
            channelPortalKeyByPlayer.remove(playerId);
            channelStartedAtByPlayer.remove(playerId);

            long releaseTicks = Math.max(1L, (travelCooldownMs + 49L) / 50L);

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                source.teleportCooldowns().remove(playerId);
                target.teleportCooldowns().remove(playerId);
            }, releaseTicks);

            playTeleportEffects(sourceCenter, target.center(), skinVariant, plugin, chargeProgress);
            sourceCenter.getWorld().playSound(sourceCenter, Sound.BLOCK_PORTAL_TRAVEL, 0.1f, 1.2f);
            target.center().getWorld().playSound(target.center(), Sound.BLOCK_PORTAL_TRAVEL, 0.1f, 1.2f);
            break;
        }
    }

    private static String gatewayKey(Location location) {
        return location.getWorld().getUID() + ":" + location.getBlockX() + ":" + location.getBlockY() + ":" + location.getBlockZ();
    }

    private static boolean canUseGateway(Player player, boolean seekerCanUse) {
        if (HideAndSeek.getDataController().getHiders().contains(player.getUniqueId())) {
            return true;
        }

        return seekerCanUse && HideAndSeek.getDataController().getSeekers().contains(player.getUniqueId());
    }

    private static void tickGatewayParticle(ItemStateManager.GatewayData gateway, String skinVariant) {
        if (gateway == null || gateway.center().getWorld() == null) {
            return;
        }

        Color color = skinColor(skinVariant);
        gateway.center().getWorld().spawnParticle(Particle.DUST_COLOR_TRANSITION, gateway.center().clone().add(0, 0.6, 0), 4, 0.18, 0.22, 0.18, 0.0,
                new Particle.DustTransition(color, lighter(color), 1.2f));
        gateway.center().getWorld().spawnParticle(Particle.PORTAL, gateway.center().clone().add(0, 0.5, 0), 3, 0.18, 0.25, 0.18, 0.01);
    }

    private static void animateGatewayVisuals(ItemStateManager.GatewayData gateway, long nowMs) {
        if (gateway == null || gateway.displays() == null || gateway.displays().isEmpty()) {
            return;
        }

        double elapsed = Math.max(0L, nowMs - gateway.placedAtMs()) / 1000.0;
        String skinVariant = gateway.skinVariant();

        for (int i = 0; i < gateway.displays().size(); i++) {
            Display display = gateway.displays().get(i);
            if (display == null || !display.isValid()) {
                continue;
            }

            display.setInterpolationDuration(2);
            display.setInterpolationDelay(0);

            animateBlockLayer((BlockDisplay) display, skinVariant, elapsed, i);
        }
    }

    private static void animateBlockLayer(BlockDisplay display, String skinVariant, double elapsed, int layerIndex) {

        if (SKIN_END_RIFT.equals(skinVariant)) {

            display.setTransformation(new Transformation(
                    new Vector3f(-0.42f, -0.02f, -0.42f),
                    new AxisAngle4f(0, 0, 0, 0),
                    new Vector3f(0.84f, 0.12f, 0.84f),
                    new AxisAngle4f(0, 0, 0, 0)
            ));
            return;
        }

        if (SKIN_PHASE_DOOR.equals(skinVariant)) {

            float side = (layerIndex % 2 == 0) ? -0.18f : 0.18f;
            display.setTransformation(new Transformation(
                    new Vector3f(side, -0.04f, 0.0f),
                    new AxisAngle4f((float) Math.toRadians(layerIndex % 2 == 0 ? -14 : 14), 0f, 1f, 0f),
                    new Vector3f(0.20f, 1.05f, 0.16f),
                    new AxisAngle4f(0, 0, 0, 0)
            ));
            return;
        }


        float bob = (float) (Math.sin(elapsed * 1.7 + layerIndex * 0.9) * 0.06);
        float spin = (float) ((elapsed * 18.0 + layerIndex * 45.0) % 360.0);

        if (SKIN_DEMATERIALIZER.equals(skinVariant)) {

            double orbitRadius = 0.28 + Math.sin(elapsed * 2.3 + layerIndex) * 0.04;
            double angle = elapsed * 1.8 + (layerIndex * (Math.PI / 2.0));
            float x = (float) (Math.cos(angle) * orbitRadius);
            float z = (float) (Math.sin(angle) * orbitRadius);
            boolean glow = layerIndex % 3 == 0;
            display.setTransformation(new Transformation(
                    new Vector3f(x, 0.16f + bob, z),
                    new AxisAngle4f((float) Math.toRadians(spin * 1.2f), 0f, 1f, 0f),
                    glow ? new Vector3f(0.16f, 0.16f, 0.16f) : new Vector3f(0.11f, 0.11f, 0.11f),
                    new AxisAngle4f((float) Math.toRadians(spin * 0.45f), 0f, 1f, 0f)
            ));
            return;
        }

        if (SKIN_VOID_LATTICE.equals(skinVariant)) {

            if (layerIndex == 0) {
                float pulseCore = 0.28f + (float) ((Math.sin(elapsed * 2.2) + 1.0) * 0.04);
                display.setTransformation(new Transformation(
                        new Vector3f(-pulseCore / 2.0f, 0.38f + bob, -pulseCore / 2.0f),
                        new AxisAngle4f((float) Math.toRadians(spin * 0.7f), 0f, 1f, 0f),
                        new Vector3f(pulseCore, pulseCore, pulseCore),
                        new AxisAngle4f((float) Math.toRadians(spin * 0.4f), 0f, 1f, 0f)
                ));
                return;
            }

            if (layerIndex == 1) {
                float shellPulse = 0.36f + (float) ((Math.cos(elapsed * 1.9) + 1.0) * 0.05);
                display.setTransformation(new Transformation(
                        new Vector3f(-shellPulse / 2.0f, 0.24f + bob, -shellPulse / 2.0f),
                        new AxisAngle4f((float) Math.toRadians(-spin * 1.1f), 0f, 1f, 0f),
                        new Vector3f(shellPulse, shellPulse, shellPulse),
                        new AxisAngle4f((float) Math.toRadians(spin * 0.8f), 0f, 1f, 0f)
                ));
                return;
            }

            int rem = layerIndex - 2;
            int ring = 0;
            int inRing = rem;
            int acc = 0;
            for (int r = 0; r < VOID_LATTICE_RINGS; r++) {
                int segments = voidLatticeSegmentsForRing(r);
                if (rem < acc + segments) {
                    ring = r;
                    inRing = rem - acc;
                    break;
                }
                acc += segments;
            }

            int segments = voidLatticeSegmentsForRing(ring);
            double direction = ring % 2 == 0 ? 1.0 : -1.0;
            double baseAngle = ((double) inRing / (double) segments) * Math.PI * 2.0;
            double ringSpeed = (1.05 + ring * 0.24) * direction;
            double angle = baseAngle + (elapsed * ringSpeed) + (ring * 0.38);

            double ringRadius = 0.42 + ring * 0.19;
            double distortion = Math.sin(elapsed * 2.1 + ring * 0.7) * 0.09;
            double warpedRadius = ringRadius + distortion * Math.cos(angle * 2.0);

            float x = (float) (Math.cos(angle) * warpedRadius);
            float z = (float) (Math.sin(angle) * warpedRadius);
            float y = (float) (0.16 + ring * 0.16 + Math.sin(angle * 3.0 + elapsed * 2.0) * 0.08);
            float scale = (float) (0.11 + (VOID_LATTICE_RINGS - ring) * 0.01 + Math.sin(elapsed * 3.3 + inRing * 0.5) * 0.01);

            display.setTransformation(new Transformation(
                    new Vector3f(x, y, z),
                    new AxisAngle4f((float) Math.toRadians(spin * 2.2f + ring * 15f), 0f, 1f, 0f),
                    new Vector3f(scale, scale, scale),
                    new AxisAngle4f((float) Math.toRadians((spin * 1.1f) * (direction > 0 ? 1 : -1)), 0f, 1f, 0f)
            ));
            return;
        }


        display.setTransformation(new Transformation(
                new Vector3f(-0.45f, -0.03f, -0.45f),
                new AxisAngle4f(0, 0, 0, 0),
                new Vector3f(0.90f, 0.16f, 0.90f),
                new AxisAngle4f(0, 0, 0, 0)
        ));
    }

    private static List<Display> spawnGatewayVisuals(HideAndSeek plugin, Location center, String skinVariant) {
        List<Display> displays = new ArrayList<>();

        if (SKIN_VOID_LATTICE.equals(skinVariant)) {
            displays.add(spawnBlockDisplay(plugin, center.clone(), Material.CRYING_OBSIDIAN,
                    new Vector3f(-0.14f, 0.34f, -0.14f), new Vector3f(0.28f, 0.28f, 0.28f)));
            displays.add(spawnBlockDisplay(plugin, center.clone(), Material.SCULK_CATALYST,
                    new Vector3f(-0.18f, 0.20f, -0.18f), new Vector3f(0.36f, 0.36f, 0.36f)));

            Material[] ringMaterials = new Material[]{
                    Material.SCULK,
                    Material.SHROOMLIGHT,
                    Material.OBSIDIAN,
                    Material.AMETHYST_BLOCK,
                    Material.SCULK_VEIN
            };

            for (int ring = 0; ring < VOID_LATTICE_RINGS; ring++) {
                int segments = voidLatticeSegmentsForRing(ring);
                double radius = 0.42 + ring * 0.19;
                double baseY = 0.16 + ring * 0.16;
                Material ringMaterial = ringMaterials[ring % ringMaterials.length];

                for (int i = 0; i < segments; i++) {
                    double angle = ((double) i / (double) segments) * Math.PI * 2.0 + ring * 0.38;
                    float x = (float) (Math.cos(angle) * radius);
                    float z = (float) (Math.sin(angle) * radius);
                    float y = (float) (baseY + Math.sin(angle * 3.0) * 0.08);
                    float scale = (float) (0.11 + (VOID_LATTICE_RINGS - ring) * 0.01);
                    displays.add(spawnBlockDisplay(plugin, center.clone(), ringMaterial,
                            new Vector3f(x, y, z), new Vector3f(scale, scale, scale)));
                }
            }

            for (Display display : displays) {
                if (display != null) {
                    ItemStateManager.remoteGatewayEntities.add(display.getUniqueId());
                }
            }
            return displays;
        }

        if (SKIN_END_RIFT.equals(skinVariant)) {
            displays.add(spawnBlockDisplay(plugin, center.clone(), Material.END_PORTAL_FRAME,
                    new Vector3f(-0.50f, -0.02f, -0.50f), new Vector3f(1.00f, 0.16f, 1.00f)));
            for (Display display : displays) {
                if (display != null) {
                    ItemStateManager.remoteGatewayEntities.add(display.getUniqueId());
                }
            }
            return displays;
        }

        if (SKIN_PHASE_DOOR.equals(skinVariant)) {
            Door doorBlock = (Door) Material.WARPED_DOOR.createBlockData();
            doorBlock.setHalf(Door.Half.BOTTOM);
            displays.add(spawnBlockDisplay(plugin, center.clone(), doorBlock,
                    new Vector3f(-0.50f, 0f, -0.05f), new Vector3f(1.00f, 1.00f, 0.10f)));

            Door doorBlock2 = (Door) Material.WARPED_DOOR.createBlockData();
            doorBlock2.setHalf(Door.Half.TOP);
            displays.add(spawnBlockDisplay(plugin, center.clone(), doorBlock2,
                    new Vector3f(-0.50f, 1f, -0.05f), new Vector3f(1.00f, 1.00f, 0.10f)));

            for (Display display : displays) {
                if (display != null) {
                    ItemStateManager.remoteGatewayEntities.add(display.getUniqueId());
                }
            }
            return displays;
        }

        if (skinVariant == null || skinVariant.isBlank()) {
            displays.add(spawnBlockDisplay(plugin, center.clone(), Material.NETHER_PORTAL,
                    new Vector3f(-0.50f, 0f, -0.02f), new Vector3f(1.00f, 2.00f, 0.04f)));
            for (Display display : displays) {
                if (display != null) {
                    ItemStateManager.remoteGatewayEntities.add(display.getUniqueId());
                }
            }
            return displays;
        }

        Material ringMaterial = (SKIN_DEMATERIALIZER.equals(skinVariant)) ? Material.LIGHT_BLUE_STAINED_GLASS : Material.PURPLE_STAINED_GLASS;

        Material coreMaterial = (SKIN_DEMATERIALIZER.equals(skinVariant)) ? Material.SEA_LANTERN : Material.OBSIDIAN;

        Material accentMaterial = (SKIN_DEMATERIALIZER.equals(skinVariant)) ? Material.GLASS : Material.AMETHYST_BLOCK;

        displays.add(spawnBlockDisplay(plugin, center, ringMaterial, new Vector3f(-0.52f, -0.02f, -0.52f), new Vector3f(1.04f, 1.96f, 1.04f)));
        displays.add(spawnBlockDisplay(plugin, center.clone().add(0, 0.28, 0), coreMaterial, new Vector3f(-0.18f, -0.18f, -0.18f), new Vector3f(0.36f, 0.36f, 0.36f)));
        displays.add(spawnBlockDisplay(plugin, center.clone().add(0, 0.88, 0), accentMaterial, new Vector3f(-0.10f, -0.10f, -0.10f), new Vector3f(0.20f, 0.20f, 0.20f)));

        if (SKIN_DEMATERIALIZER.equals(skinVariant)) {
            displays.add(spawnBlockDisplay(plugin, center.clone().add(0.26, 0.52, 0.0), Material.SEA_LANTERN, new Vector3f(-0.08f, -0.08f, -0.08f), new Vector3f(0.16f, 0.16f, 0.16f)));
            displays.add(spawnBlockDisplay(plugin, center.clone().add(-0.24, 0.96, 0.0), Material.GLASS, new Vector3f(-0.07f, -0.07f, -0.07f), new Vector3f(0.14f, 0.14f, 0.14f)));
            displays.add(spawnBlockDisplay(plugin, center.clone().add(0.0, 1.18, 0.24), Material.AMETHYST_BLOCK, new Vector3f(-0.09f, -0.09f, -0.09f), new Vector3f(0.18f, 0.18f, 0.18f)));
            displays.add(spawnBlockDisplay(plugin, center.clone().add(0.0, 1.42, -0.22), Material.LIGHT_BLUE_STAINED_GLASS, new Vector3f(-0.06f, -0.06f, -0.06f), new Vector3f(0.12f, 0.12f, 0.12f)));
        }

        for (Display display : displays) {
            if (display != null) {
                ItemStateManager.remoteGatewayEntities.add(display.getUniqueId());
            }
        }

        return displays;
    }

    private static void playChargingEffects(Location sourceCenter, Location targetCenter, long elapsedMs, long standMs, String skinVariant, HideAndSeek plugin) {
        World world = sourceCenter.getWorld();
        if (world == null) {
            return;
        }

        Color color = skinColor(skinVariant);
        float progress = standMs <= 0 ? 1.0f : Math.min(1.0f, (float) elapsedMs / (float) standMs);
        int trailDuration = Math.max(3, 4 + (int) Math.round(progress * 3.0));
        int particleCount = 2 + (int) Math.round(progress * 6.0);
        double spread = 0.12 + (progress * 0.08);

        new BukkitRunnable() {
            final int maxTicks = 4;
            int ticks;

            @Override
            public void run() {
                if (ticks >= maxTicks) {
                    cancel();
                    return;
                }

                spawnTrail(world, sourceCenter.clone().add(0, 0.72, 0), targetCenter.clone().add(0, 0.72, 0), darker(darker(color)), trailDuration * 2, particleCount);
                spawnTrail(world, targetCenter.clone().add(0, 0.72, 0), sourceCenter.clone().add(0, 0.72, 0), lighter(lighter(color)), trailDuration * 2 - 2, particleCount);
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 2L);

        world.spawnParticle(Particle.PORTAL, sourceCenter.clone().add(0, 0.7, 0), particleCount, spread, 0.15 + (progress * 0.1), spread, 0.08 + (progress * 0.06));
        world.spawnParticle(Particle.DUST_COLOR_TRANSITION, targetCenter.clone().add(0, 0.7, 0), particleCount / 2, spread * 0.8, 0.12 + (progress * 0.08), spread * 0.8, 0.0,
                new Particle.DustTransition(darker(color), lighter(color), 1.0f));

    }

    private static void playTeleportEffects(Location sourceCenter, Location targetCenter, String skinVariant, HideAndSeek plugin, float chargeProgress) {
        World world = sourceCenter.getWorld();
        if (world == null) {
            return;
        }

        Color color = skinColor(skinVariant);
        float clampedProgress = Math.clamp(chargeProgress, 0.0f, 1.0f);
        int trailRepeats = 2 + Math.round(clampedProgress * 4.0f);
        int shortTrailDuration = 12 + Math.round(clampedProgress * 10.0f);
        int longTrailDuration = 22 + Math.round(clampedProgress * 12.0f);
        int portalBurst = 12 + Math.round(clampedProgress * 30.0f);
        double spread = 0.2 + (clampedProgress * 0.18);

        new BukkitRunnable() {
            final int maxTicks = trailRepeats;
            int ticks;

            @Override
            public void run() {
                if (ticks >= maxTicks) {
                    cancel();
                    return;
                }

                int perTickCount = 2 + Math.round(clampedProgress * 4.0f);
                spawnTrail(world, sourceCenter.clone().add(0, 0.8, 0), targetCenter.clone().add(0, 0.8, 0), color, longTrailDuration, perTickCount);
                spawnTrail(world, targetCenter.clone().add(0, 0.8, 0), sourceCenter.clone().add(0, 0.8, 0), lighter(color), shortTrailDuration, perTickCount);
                spawnTrail(world, sourceCenter.clone().add(0, 0.95, 0), targetCenter.clone().add(0, 0.95, 0), lighter(color), shortTrailDuration + 4, perTickCount);
                spawnTrail(world, sourceCenter.clone().add(0, 0.62, 0), targetCenter.clone().add(0, 0.62, 0), color, shortTrailDuration + 4, perTickCount);
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 2L);
        world.spawnParticle(Particle.PORTAL, sourceCenter.clone().add(0, 0.7, 0), portalBurst, spread, 0.24 + (clampedProgress * 0.22), spread, 0.18 + (clampedProgress * 0.12));
        world.spawnParticle(Particle.PORTAL, targetCenter.clone().add(0, 0.7, 0), portalBurst, spread, 0.24 + (clampedProgress * 0.22), spread, 0.18 + (clampedProgress * 0.12));
    }

    private static void spawnTrail(World world, Location from, Location to, Color color, int duration, int count) {
        world.spawnParticle(Particle.TRAIL, from, count, 0.25, 0.5, 0.25, new Particle.Trail(to, color, Math.max(1, duration)));
    }

    private static int voidLatticeSegmentsForRing(int ring) {
        return VOID_LATTICE_BASE_SEGMENTS + ring;
    }

    private static Color lighter(Color color) {
        int r = Math.min(255, color.getRed() + 45);
        int g = Math.min(255, color.getGreen() + 45);
        int b = Math.min(255, color.getBlue() + 45);
        return Color.fromRGB(r, g, b);
    }

    private static Color darker(Color color) {
        int r = Math.max(0, color.getRed() - 45);
        int g = Math.max(0, color.getGreen() - 45);
        int b = Math.max(0, color.getBlue() - 45);
        return Color.fromRGB(r, g, b);
    }

    private static String resolveGatewaySkin(UUID ownerId) {
        Player owner = Bukkit.getPlayer(ownerId);
        if (owner == null) {
            return null;
        }
        String selected = ItemSkinSelectionService.getSelectedVariant(owner, ID);
        if (selected == null || selected.isBlank()) {
            return null;
        }
        return ItemSkinSelectionService.isUnlocked(ownerId, ID, selected) ? selected : null;
    }

    private static boolean isAnimatedSkin(String skinVariant) {
        return SKIN_DEMATERIALIZER.equals(skinVariant) || SKIN_VOID_LATTICE.equals(skinVariant);
    }

    private static BlockDisplay spawnBlockDisplay(HideAndSeek plugin, Location location, Material material, Vector3f translation, Vector3f scale) {
        return location.getWorld().spawn(location, BlockDisplay.class, display -> {
            display.setBlock(material.createBlockData());
            display.setBrightness(new org.bukkit.entity.Display.Brightness(15, 15));
            display.setInterpolationDuration(2);
            display.setInterpolationDelay(0);
            display.setTransformation(new Transformation(
                    translation,
                    new AxisAngle4f(0, 0, 0, 0),
                    scale,
                    new AxisAngle4f(0, 0, 0, 0)
            ));
            display.setPersistent(true);
            display.getPersistentDataContainer().set(new NamespacedKey(plugin, "remote_gateway"), org.bukkit.persistence.PersistentDataType.BOOLEAN, true);
        });
    }

    private static BlockDisplay spawnBlockDisplay(HideAndSeek plugin, Location location, BlockData blockData, Vector3f translation, Vector3f scale) {
        return location.getWorld().spawn(location, BlockDisplay.class, display -> {
            display.setBlock(blockData);
            display.setBrightness(new org.bukkit.entity.Display.Brightness(15, 15));
            display.setInterpolationDuration(2);
            display.setInterpolationDelay(0);
            display.setTransformation(new Transformation(
                    translation,
                    new AxisAngle4f(0, 0, 0, 0),
                    scale,
                    new AxisAngle4f(0, 0, 0, 0)
            ));
            display.setPersistent(true);
            display.getPersistentDataContainer().set(new NamespacedKey(plugin, "remote_gateway"), org.bukkit.persistence.PersistentDataType.BOOLEAN, true);
        });
    }

    private static Color skinColor(String skinVariant) {
        if (SKIN_END_RIFT.equals(skinVariant)) {
            return Color.fromRGB(120, 60, 255);
        }
        if (SKIN_PHASE_DOOR.equals(skinVariant)) {
            return Color.fromRGB(255, 170, 70);
        }
        if (SKIN_DEMATERIALIZER.equals(skinVariant)) {
            return Color.fromRGB(95, 235, 255);
        }
        if (SKIN_VOID_LATTICE.equals(skinVariant)) {
            return Color.fromRGB(35, 205, 150);
        }
        return Color.fromRGB(180, 70, 255);
    }

    private static Location resolvePlacementLocation(Location clickedLocation) {
        return clickedLocation.getBlock().getLocation().add(0.5, 1.0, 0.5);
    }

    private static boolean isPairValid(ItemStateManager.GatewayPairData pair) {
        return isGatewayValid(pair.first()) && isGatewayValid(pair.second());
    }

    private static boolean isGatewayValid(ItemStateManager.GatewayData gateway) {
        return gateway != null
                && gateway.displays() != null
                && !gateway.displays().isEmpty()
                && gateway.displays().stream().anyMatch(display -> display != null && display.isValid())
                && gateway.center() != null
                && gateway.center().getWorld() != null;
    }

    private static void removePair(ItemStateManager.GatewayPairData pair) {
        if (pair == null) {
            return;
        }
        removeGateway(pair.first());
        removeGateway(pair.second());
    }

    private static void removeGateway(ItemStateManager.GatewayData gateway) {
        if (gateway == null) {
            return;
        }

        if (gateway.displays() != null) {
            for (Display display : gateway.displays()) {
                if (display == null || !display.isValid()) {
                    continue;
                }
                ItemStateManager.remoteGatewayEntities.remove(display.getUniqueId());
                display.remove();
            }
        }
    }

    public static void clearGatewaysForOwner(UUID ownerId) {
        if (ownerId == null) {
            return;
        }

        ItemStateManager.GatewayData pending = ItemStateManager.pendingGatewayByOwner.remove(ownerId);
        removeGateway(pending);

        LinkedList<ItemStateManager.GatewayPairData> pairs = ItemStateManager.hiderGatewayPairs.remove(ownerId);
        if (pairs == null) {
            return;
        }

        for (ItemStateManager.GatewayPairData pair : pairs) {
            removePair(pair);
        }
    }

    public static void clearAllGateways() {
        for (UUID ownerId : new ArrayList<>(ItemStateManager.hiderGatewayPairs.keySet())) {
            clearGatewaysForOwner(ownerId);
        }

        for (ItemStateManager.GatewayData pending : new ArrayList<>(ItemStateManager.pendingGatewayByOwner.values())) {
            removeGateway(pending);
        }
        ItemStateManager.pendingGatewayByOwner.clear();
        ItemStateManager.gatewayTeleportCooldownUntil.clear();
        channelPortalKeyByPlayer.clear();
        channelStartedAtByPlayer.clear();

        if (activeGatewayTask != null) {
            activeGatewayTask.cancel();
            activeGatewayTask = null;
        }
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public ItemStack createItem(HideAndSeek plugin) {
        ItemStack item = new ItemStack(Material.ENDER_EYE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Remote Gateway", NamedTextColor.DARK_PURPLE, TextDecoration.BOLD)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.text("Right click to place your gateway anchors", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false),
                    Component.text("Two anchors form a teleport pair", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false)
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    public String getDescription(HideAndSeek plugin) {
        int duration = plugin.getSettingRegistry().get("hider-items.remote-gateway.duration-seconds", 120);
        if (duration == -1) {
            return "Place paired gateways that teleport between anchors until the round ends.";
        }
        return "Place paired gateways that teleport between anchors for " + duration + " seconds.";
    }

    @Override
    public void register(HideAndSeek plugin) {
        int cooldown = plugin.getSettingRegistry().get("hider-items.remote-gateway.cooldown", 8);

        plugin.getCustomItemManager().registerItem(new CustomItemBuilder(createItem(plugin), getId())
                .withAction(ItemActionType.RIGHT_CLICK_AIR, context -> placeGateway(context, plugin))
                .withAction(ItemActionType.RIGHT_CLICK_BLOCK, context -> placeGateway(context, plugin))
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

        ensureGatewayTask(plugin);
    }

    @Override
    public Set<String> getConfigKeys() {
        return Set.of(
                "hider-items.remote-gateway.cooldown",
                "hider-items.remote-gateway.duration-seconds",
                "hider-items.remote-gateway.travel-cooldown-seconds",
                "hider-items.remote-gateway.portal-stand-seconds"
        );
    }
}
















