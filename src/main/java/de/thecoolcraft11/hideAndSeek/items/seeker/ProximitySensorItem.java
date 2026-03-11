package de.thecoolcraft11.hideAndSeek.items.seeker;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.items.api.GameItem;
import de.thecoolcraft11.hideAndSeek.util.points.PointAction;
import de.thecoolcraft11.minigameframework.items.CustomItemBuilder;
import de.thecoolcraft11.minigameframework.items.ItemActionType;
import de.thecoolcraft11.minigameframework.items.ItemInteractionContext;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Lightable;
import org.bukkit.block.data.type.RedstoneWallTorch;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.*;

import static de.thecoolcraft11.hideAndSeek.items.api.ItemStateManager.sensorDisplays;

public class ProximitySensorItem implements GameItem {
    public static final String ID = "has_seeker_proximity_sensor";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public ItemStack createItem(HideAndSeek plugin) {
        ItemStack item = new ItemStack(Material.REDSTONE_TORCH);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Proximity Sensor", NamedTextColor.DARK_RED, TextDecoration.BOLD)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.text("Right click to place a sensor", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false)
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    public String getDescription(HideAndSeek plugin) {
        Number range = plugin.getSettingRegistry().get("seeker-items.proximity-sensor.range", 8.0);
        int points = plugin.getPointService().getInt("points.seeker.utility-success.amount", 40);
        return String.format("Place a sensor revealing hiders within %.0f blocks, grants %d points per detection.", range.doubleValue(), points);
    }

    @Override
    public void register(HideAndSeek plugin) {
        int proximityCooldown = plugin.getSettingRegistry().get("seeker-items.proximity-sensor.cooldown", 20);
        plugin.getCustomItemManager().registerItem(new CustomItemBuilder(createItem(plugin), getId())
                .withAction(ItemActionType.RIGHT_CLICK_BLOCK, context -> placeProximitySensor(context, plugin))
                .withDescription(getDescription(plugin))
                .withDropPrevention(true)
                .withCraftPrevention(true)
                .withVanillaCooldown(proximityCooldown * 20)
                .withCustomCooldown(proximityCooldown * 1000L)
                .withVanillaCooldownDisplay(true)
                .allowOffHand(false)
                .allowArmor(false)
                .cancelDefaultAction(true)
                .build());
    }

    private static void placeProximitySensor(ItemInteractionContext context, HideAndSeek plugin) {
        double range = plugin.getSettingRegistry().get("seeker-items.proximity-sensor.range", 8.0);
        int sensorDuration = plugin.getSettingRegistry().get("seeker-items.proximity-sensor.duration", 60);
        double fovAngle = plugin.getSettingRegistry().get("seeker-items.proximity-sensor.fov-angle", 160.0);


        Block clickedBlock = context.getLocation().getBlock();
        Player player = context.getPlayer();


        if (!clickedBlock.getType().isSolid()) {
            player.sendMessage(Component.text("Cannot place sensor - need solid block!", NamedTextColor.RED));
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


        Block torchBlock = clickedBlock.getRelative(clickedFace);


        if (!torchBlock.getType().isAir()) {
            player.sendMessage(Component.text("Cannot place sensor here - space is occupied!", NamedTextColor.RED));
            context.skipCooldown();
            return;
        }


        Material torchType;
        BlockData torchData;

        if (clickedFace == BlockFace.DOWN) {

            player.sendMessage(Component.text("Cannot place sensor on ceiling!", NamedTextColor.RED));
            context.skipCooldown();
            return;
        } else if (clickedFace == BlockFace.UP) {

            torchType = Material.REDSTONE_TORCH;
            torchData = torchType.createBlockData();
            if (torchData instanceof Lightable lightable) {
                lightable.setLit(false);
            }
            torchBlock.setBlockData(torchData, false);
            BlockDisplay display = (BlockDisplay) torchBlock.getLocation().getWorld().spawnEntity(torchBlock.getLocation(), EntityType.BLOCK_DISPLAY);
            display.getPersistentDataContainer().set(new NamespacedKey(plugin, "sensor-type"), PersistentDataType.STRING, "proximity");
            display.getPersistentDataContainer().set(new NamespacedKey(plugin, "sensor-block"), PersistentDataType.STRING, torchBlock.getLocation().toString());
            display.getPersistentDataContainer().set(new NamespacedKey(plugin, "sensor-facing"), PersistentDataType.STRING, "UP");
            BlockData data = Material.SCULK_SENSOR.createBlockData();

            display.setBlock(data);

            display.setTransformation(new Transformation(
                    new Vector3f(0.375f, 0.425f, 0.375f),
                    new AxisAngle4f(0, 0, 0, 0),
                    new Vector3f(0.25f, 0.465f, 0.25f),
                    new AxisAngle4f(0, 0, 0, 0)
            ));
            sensorDisplays.put(torchBlock.getLocation(), display);

        } else {

            torchType = Material.REDSTONE_WALL_TORCH;
            torchData = torchType.createBlockData();
            if (torchData instanceof Lightable lightable) {
                lightable.setLit(false);
            }

            if (torchData instanceof RedstoneWallTorch wallTorch) {
                wallTorch.setFacing(clickedFace);
                torchBlock.setBlockData(wallTorch, false);


                BlockDisplay display = (BlockDisplay) torchBlock.getLocation().getWorld().spawnEntity(torchBlock.getLocation(), EntityType.BLOCK_DISPLAY);
                display.getPersistentDataContainer().set(new NamespacedKey(plugin, "sensor-type"), PersistentDataType.STRING, "proximity");
                display.getPersistentDataContainer().set(new NamespacedKey(plugin, "sensor-block"), PersistentDataType.STRING, torchBlock.getLocation().toString());
                display.getPersistentDataContainer().set(new NamespacedKey(plugin, "sensor-facing"), PersistentDataType.STRING, clickedFace.name());
                BlockData data = Material.SCULK_SENSOR.createBlockData();

                display.setBlock(data);


                display.setTransformation(getWallTorchTransformation(clickedFace));
                sensorDisplays.put(torchBlock.getLocation(), display);
            } else {
                torchBlock.setType(torchType);
            }
        }

        Location sensorLocation = torchBlock.getLocation().clone().add(0.5, 0.5, 0.5);

        BlockDisplay sensorDisplay = sensorDisplays.get(torchBlock.getLocation());
        String facingStr = sensorDisplay != null ?
                sensorDisplay.getPersistentDataContainer().get(new NamespacedKey(plugin, "sensor-facing"), PersistentDataType.STRING) :
                "UP";
        BlockFace sensorFacing = facingStr != null ? BlockFace.valueOf(facingStr) : BlockFace.UP;

        Map<UUID, Long> glowingHiders = new HashMap<>();


        new BukkitRunnable() {
            final long startTime = System.currentTimeMillis();
            final long durationMs = sensorDuration == -1 ? Long.MAX_VALUE : (long) sensorDuration * 1000L;

            @Override
            public void run() {

                if (torchBlock.getType() != Material.REDSTONE_TORCH && torchBlock.getType() != Material.REDSTONE_WALL_TORCH) {
                    cancel();

                    if (torchBlock.getBlockData() instanceof Lightable lightable) {
                        plugin.getLogger().info("Removing power from " + torchBlock.getLocation());
                        lightable.setLit(false);
                    }

                    BlockDisplay display = sensorDisplays.remove(torchBlock.getLocation());
                    if (display != null && display.isValid()) {
                        display.remove();
                    }

                    for (UUID hiderId : glowingHiders.keySet()) {
                        Player hider = Bukkit.getPlayer(hiderId);
                        if (hider != null) {
                            removeProximitySensorGlow(hider, plugin);
                        }
                    }
                    return;
                }

                if (sensorDuration != -1 && System.currentTimeMillis() - startTime > durationMs) {
                    torchBlock.setType(Material.AIR);
                    cancel();

                    BlockDisplay display = sensorDisplays.remove(torchBlock.getLocation());
                    if (display != null && display.isValid()) {
                        display.remove();
                    }

                    for (UUID hiderId : glowingHiders.keySet()) {
                        Player hider = Bukkit.getPlayer(hiderId);
                        if (hider != null) {
                            removeProximitySensorGlow(hider, plugin);
                        }
                    }
                    return;
                }

                Set<UUID> hidersInRange = new HashSet<>();


                for (UUID hiderId : HideAndSeek.getDataController().getHiders()) {
                    Player hider = Bukkit.getPlayer(hiderId);
                    if (hider == null || !hider.isOnline() || !hider.getWorld().equals(sensorLocation.getWorld()))
                        continue;

                    double distance = hider.getLocation().distance(sensorLocation);
                    if (distance < range) {
                        if (sensorFacing != BlockFace.UP) {
                            org.bukkit.util.Vector toHider = hider.getLocation().toVector().subtract(sensorLocation.toVector()).normalize();
                            Vector sensorDirection = getSensorDirection(sensorFacing);

                            double angle = Math.toDegrees(Math.acos(sensorDirection.dot(toHider)));

                            if (angle > fovAngle / 2) {
                                continue;
                            }
                        }

                        Location hiderEyeLocation = hider.getEyeLocation();
                        RayTraceResult rayTrace = sensorLocation.getWorld().rayTraceBlocks(
                                sensorLocation,
                                hiderEyeLocation.toVector().subtract(sensorLocation.toVector()).normalize(),
                                distance,
                                org.bukkit.FluidCollisionMode.NEVER,
                                true
                        );

                        if (rayTrace != null && rayTrace.getHitBlock() != null) {
                            continue;
                        }

                        hidersInRange.add(hiderId);

                        if (!glowingHiders.containsKey(hiderId)) {
                            sensorLocation.getWorld().playSound(sensorLocation, Sound.BLOCK_SCULK_SENSOR_CLICKING, 1.0f, 1.0f);
                        }


                        Location hiderLoc = hider.getLocation().add(0, 1, 0);

                        hider.getWorld().spawnParticle(
                                Particle.VIBRATION,
                                sensorLocation,
                                0,
                                0, 0, 0,
                                1.0,
                                new Vibration(new Vibration.Destination.BlockDestination(hiderLoc), 5)
                        );

                        HideAndSeek.getDataController().getSeekers().forEach(seekerUUID -> {
                            Player seeker = Bukkit.getPlayer(seekerUUID);
                            if (seeker != null) {
                                seeker.playSound(seeker.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 2.0f);
                            }
                        });

                        if (!glowingHiders.containsKey(hiderId)) {
                            applyProximitySensorGlow(hider, plugin);
                            plugin.getPointService().award(context.getPlayer().getUniqueId(), PointAction.SEEKER_UTILITY_SUCCESS);
                            plugin.getPointService().markUtilitySpotted(hiderId);
                        }
                        glowingHiders.put(hiderId, System.currentTimeMillis());
                    }
                }


                List<UUID> hidersToRemove = new ArrayList<>();
                for (UUID hiderId : glowingHiders.keySet()) {
                    if (!hidersInRange.contains(hiderId)) {
                        Player hider = Bukkit.getPlayer(hiderId);
                        if (hider != null) {
                            removeProximitySensorGlow(hider, plugin);
                        }
                        hidersToRemove.add(hiderId);
                    }
                }
                for (UUID hiderId : hidersToRemove) {
                    glowingHiders.remove(hiderId);
                }
            }
        }.runTaskTimer(plugin, 0L, 4L);

        String durationMsg = sensorDuration == -1 ? "until round ends" : sensorDuration + " seconds";
        context.getPlayer().sendMessage(Component.text("Proximity sensor placed! (" + durationMsg + ")", NamedTextColor.GREEN));
    }


    private static void applyProximitySensorGlow(Player hider, HideAndSeek plugin) {
        UUID hiderId = hider.getUniqueId();

        var gameModeResult = plugin.getSettingService().getSetting("game.gametype");
        Object gameModeObj = gameModeResult.isSuccess() ? gameModeResult.getValue() : null;
        boolean isBlockMode = gameModeObj != null && gameModeObj.toString().equals("BLOCK");


        HideAndSeek.getDataController().setGlowing(hiderId, true);

        if (isBlockMode) {

            BlockDisplay display = HideAndSeek.getDataController().getBlockDisplay(hiderId);
            if (display != null && display.isValid()) {
                display.setGlowing(true);
                Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
                Team hiderTeam = scoreboard.getEntryTeam(hider.getName());
                if (hiderTeam != null) {
                    display.setGlowColorOverride(textColorToColor(hiderTeam.color()));
                }
            } else if (HideAndSeek.getDataController().isHidden(hiderId)) {

                var hiddenBlock = HideAndSeek.getDataController().getHiddenBlock(hiderId);
                var lastLoc = HideAndSeek.getDataController().getLastLocation(hiderId);
                var blockData = HideAndSeek.getDataController().getChosenBlockData(hiderId);
                if ((hiddenBlock != null || lastLoc != null) && blockData != null) {
                    Location spawnLoc = hiddenBlock != null
                            ? hiddenBlock.getLocation().clone().add(0.5, 0, 0.5)
                            : lastLoc.getBlock().getLocation().clone().add(0.5, 0, 0.5);
                    BlockDisplay tempDisplay = spawnLoc.getWorld().spawn(spawnLoc, BlockDisplay.class, bd -> {
                        bd.setBlock(blockData);
                        bd.setGlowing(true);
                        bd.setTransformation(new Transformation(
                                new Vector3f(-0.5f, 0.001f, -0.5f),
                                new AxisAngle4f(0, 0, 0, 0),
                                new Vector3f(1.001f, 1.001f, 1.001f),
                                new AxisAngle4f(0, 0, 0, 0)
                        ));
                        bd.setBrightness(new Display.Brightness(15, 15));
                        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
                        Team hiderTeam = scoreboard.getEntryTeam(hider.getName());
                        if (hiderTeam != null) {
                            bd.setGlowColorOverride(textColorToColor(hiderTeam.color()));
                        }
                        bd.getPersistentDataContainer().set(new NamespacedKey(plugin, "temp_glow"), PersistentDataType.STRING, hiderId.toString());
                    });

                    HideAndSeek.getDataController().setBlockDisplay(hiderId, tempDisplay);
                }
            }
        } else {

            hider.setGlowing(true);
        }

    }

    private static void removeProximitySensorGlow(Player hider, HideAndSeek plugin) {
        UUID hiderId = hider.getUniqueId();
        HideAndSeek.getDataController().removeGlowing(hiderId);

        var gameModeResult = plugin.getSettingService().getSetting("game.gametype");
        Object gameModeObj = gameModeResult.isSuccess() ? gameModeResult.getValue() : null;
        boolean isBlockMode = gameModeObj != null && gameModeObj.toString().equals("BLOCK");

        if (isBlockMode) {
            BlockDisplay display = HideAndSeek.getDataController().getBlockDisplay(hiderId);
            if (display != null && display.isValid()) {

                String tempGlowId = display.getPersistentDataContainer().get(new NamespacedKey(plugin, "temp_glow"), PersistentDataType.STRING);
                if (tempGlowId != null && tempGlowId.equals(hiderId.toString())) {

                    display.remove();
                } else {

                    display.setGlowing(false);
                    display.setGlowColorOverride(null);
                }
            }
        } else {

            hider.setGlowing(false);
        }
    }

    private static Transformation getWallTorchTransformation(BlockFace face) {
        return switch (face) {
            case NORTH -> new Transformation(
                    new Vector3f(0.375f, 0.5715f, 0.715f),
                    new AxisAngle4f(0.4f, -1f, 0f, 0f),
                    new Vector3f(0.25f, 0.465f, 0.25f),
                    new AxisAngle4f(0, 1, 0, 0)
            );
            case SOUTH -> new Transformation(
                    new Vector3f(0.375f, 0.660f, 0.05f),
                    new AxisAngle4f(0.4f, 1f, 0f, 0f),
                    new Vector3f(0.25f, 0.465f, 0.25f),
                    new AxisAngle4f(0, 1, 0, 0)
            );
            case EAST -> new Transformation(
                    new Vector3f(0.05f, 0.660f, 0.375f),
                    new AxisAngle4f(0.4f, 0f, 0f, -1f),
                    new Vector3f(0.25f, 0.465f, 0.25f),
                    new AxisAngle4f(0, 1, 0, 0)
            );
            case WEST -> new Transformation(
                    new Vector3f(0.715f, 0.5715f, 0.375f),
                    new AxisAngle4f(0.4f, 0f, 0f, 1f),
                    new Vector3f(0.25f, 0.465f, 0.25f),
                    new AxisAngle4f(0, 1, 0, 0)
            );
            default -> new Transformation(
                    new Vector3f(0f, 0f, 0f),
                    new AxisAngle4f(0f, 0f, 0f, 0f),
                    new Vector3f(0, 0f, 0f),
                    new AxisAngle4f(0, 0f, 0f, 0f)
            );
        };
    }

    private static Vector getSensorDirection(BlockFace face) {
        return switch (face) {
            case NORTH -> new Vector(0, 0, -1);
            case SOUTH -> new Vector(0, 0, 1);
            case EAST -> new Vector(1, 0, 0);
            case WEST -> new Vector(-1, 0, 0);
            case DOWN -> new Vector(0, -1, 0);
            default -> new Vector(0, 1, 0);
        };
    }

    private static Color textColorToColor(TextColor chatColor) {
        return chatColor != null ? Color.fromRGB(chatColor.red(), chatColor.green(), chatColor.blue()) : Color.WHITE;
    }
}
