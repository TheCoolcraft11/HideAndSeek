package de.thecoolcraft11.hideAndSeek.items.seeker;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.items.ItemSkinSelectionService;
import de.thecoolcraft11.hideAndSeek.items.api.GameItem;
import de.thecoolcraft11.minigameframework.items.CustomItemBuilder;
import de.thecoolcraft11.minigameframework.items.ItemActionType;
import de.thecoolcraft11.minigameframework.items.ItemInteractionContext;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.List;
import java.util.UUID;

public class CageTrapItem implements GameItem {
    public static final String ID = "has_seeker_cage_trap";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public ItemStack createItem(HideAndSeek plugin) {
        ItemStack item = new ItemStack(Material.IRON_BARS);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Cage Trap", NamedTextColor.GRAY, TextDecoration.BOLD)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.text("Right click to place a trap", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false)
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    public String getDescription(HideAndSeek plugin) {
        Number duration = plugin.getSettingRegistry().get("seeker-items.cage-trap.paralyze-duration", 5);
        return String.format("Place a hidden trap that cages and immobilizes a hider for %ds.", duration.intValue());
    }

    @Override
    public void register(HideAndSeek plugin) {
        int cageCooldown = plugin.getSettingRegistry().get("seeker-items.cage-trap.cooldown", 20);
        plugin.getCustomItemManager().registerItem(new CustomItemBuilder(createItem(plugin), getId())
                .withAction(ItemActionType.RIGHT_CLICK_BLOCK, context -> placeCageTrap(context, plugin))
                .withDescription(getDescription(plugin))
                .withDropPrevention(true)
                .withCraftPrevention(true)
                .withVanillaCooldown(cageCooldown * 20)
                .withCustomCooldown(cageCooldown * 1000L)
                .withVanillaCooldownDisplay(true)
                .allowOffHand(false)
                .allowArmor(false)
                .cancelDefaultAction(true)
                .build());
    }

    private static void placeCageTrap(ItemInteractionContext context, HideAndSeek plugin) {
        Location location = context.getLocation().clone().add(0.5, 1, 0.5);
        double range = plugin.getSettingRegistry().get("seeker-items.cage-trap.range", 3.0);
        int paralyzeDuration = plugin.getSettingRegistry().get("seeker-items.cage-trap.paralyze-duration", 5);
        int trapDuration = plugin.getSettingRegistry().get("seeker-items.cage-trap.duration", -1);
        int setupTime = plugin.getSettingRegistry().get("seeker-items.cage-trap.setup-time", 5);
        boolean laserGrid = ItemSkinSelectionService.isSelected(context.getPlayer(), ID, "skin_laser_grid");
        boolean iceBlockSkin = ItemSkinSelectionService.isSelected(context.getPlayer(), ID, "skin_ice_block");

        final Player trapOwner = context.getPlayer();
        ItemStack cageBarItem = getCageBarItem(trapOwner);

        ItemDisplay[] trapIndicators = new ItemDisplay[3];
        Location indicatorLoc = location.clone().add(0, 0, 0);
        ItemDisplay trapIndicator1 = indicatorLoc.getWorld().spawn(indicatorLoc, ItemDisplay.class, display -> {
            display.setItemStack(cageBarItem.clone());
            display.setVisibleByDefault(false);
            display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
            display.setTransformation(new Transformation(
                    new Vector3f(0, 0, 0),
                    new AxisAngle4f((float) Math.toRadians(90), 1, 0, 0),
                    new Vector3f(1.2f, 1.2f, 1.2f),
                    new AxisAngle4f(0, 0, 0, 0)
            ));
            display.getPersistentDataContainer().set(new NamespacedKey(plugin, "cage_trap_indicator"), PersistentDataType.BOOLEAN, true);
        });

        ItemDisplay trapIndicator2 = indicatorLoc.getWorld().spawn(indicatorLoc, ItemDisplay.class, display -> {
            display.setItemStack(cageBarItem.clone());
            display.setVisibleByDefault(false);
            display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
            display.setTransformation(new Transformation(
                    new Vector3f(0f, 0.001f, 0f),
                    new Quaternionf(0.5f, -0.5f, 0.5f, 0.5f),
                    new Vector3f(1.2f, 1.2f, 1.2f),
                    new Quaternionf(0, 0, 0, 1)
            ));

            display.getPersistentDataContainer().set(new NamespacedKey(plugin, "cage_trap_indicator"), PersistentDataType.BOOLEAN, true);
        });

        ItemDisplay trapIndicator3 = indicatorLoc.getWorld().spawn(indicatorLoc, ItemDisplay.class, display -> {
            ItemStack ironBars = new ItemStack(Material.HEAVY_WEIGHTED_PRESSURE_PLATE);
            display.setItemStack(ironBars);
            display.setVisibleByDefault(false);
            display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
            display.setTransformation(new Transformation(
                    new Vector3f(0f, 0.28f, 0f),
                    new AxisAngle4f((float) Math.toRadians(90), 0, 1, 0),
                    new Vector3f(1.2f, 1.2f, 1.2f),
                    new AxisAngle4f(0, 0, 0, 0)
            ));

        });

        trapIndicators[0] = trapIndicator1;
        trapIndicators[1] = trapIndicator2;
        trapIndicators[2] = trapIndicator3;

        for (UUID seekerId : HideAndSeek.getDataController().getSeekers()) {
            Player seeker = Bukkit.getPlayer(seekerId);
            if (seeker != null && seeker.isOnline()) {
                for (ItemDisplay indicator : trapIndicators) {
                    seeker.showEntity(plugin, indicator);
                }
            }
        }

        if (laserGrid || iceBlockSkin) {
            location.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, location, 12, 0.22, 0.12, 0.22, 0.02);
            location.getWorld().spawnParticle(Particle.WAX_ON, location, 10, 0.2, 0.1, 0.2, 0.01);
            context.getPlayer().playSound(location, Sound.BLOCK_BEACON_POWER_SELECT, 0.4f, 1.25f);
        }

        new BukkitRunnable() {
            final long startTime = System.currentTimeMillis();
            final long durationMs = trapDuration == -1 ? Long.MAX_VALUE : (long) trapDuration * 1000L;
            boolean triggered = false;
            boolean readyToTrigger = false;
            long lastPulseAt = 0L;

            @Override
            public void run() {
                long elapsedTime = System.currentTimeMillis() - startTime;
                long setupTimeMs = setupTime * 1000L;

                if (!readyToTrigger && elapsedTime >= setupTimeMs) {
                    readyToTrigger = true;
                }

                if ((laserGrid || iceBlockSkin) && readyToTrigger && System.currentTimeMillis() - lastPulseAt >= 1000L) {
                    lastPulseAt = System.currentTimeMillis();
                    location.getWorld().spawnParticle(Particle.END_ROD, location, 4, 0.16, 0.05, 0.16, 0.01);
                }

                if (trapDuration != -1 && elapsedTime > durationMs) {
                    if (laserGrid || iceBlockSkin) {
                        location.getWorld().spawnParticle(Particle.SMOKE, location, 10, 0.2, 0.1, 0.2, 0.02);
                        location.getWorld().playSound(location, Sound.BLOCK_GLASS_BREAK, 0.3f, 0.9f);
                    }
                    for (ItemDisplay trapIndicator : trapIndicators) {
                        if (trapIndicator != null && trapIndicator.isValid()) {
                            trapIndicator.remove();
                        }
                    }
                    cancel();
                    return;
                }

                if (!readyToTrigger) {
                    return;
                }

                for (UUID hiderId : HideAndSeek.getDataController().getHiders()) {
                    Player hider = Bukkit.getPlayer(hiderId);
                    if (hider == null || !hider.isOnline() || !hider.getWorld().equals(location.getWorld()))
                        continue;

                    double distance = hider.getLocation().distance(location);
                    if (distance < range && !triggered) {
                        triggered = true;
                        triggerCageTrap(hider, trapOwner, plugin, paralyzeDuration);
                        for (ItemDisplay trapIndicator : trapIndicators) {
                            if (trapIndicator != null && trapIndicator.isValid()) {
                                trapIndicator.remove();
                            }
                        }
                        cancel();
                        return;
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 5L);

        String durationMsg = trapDuration == -1 ? "until round ends" : trapDuration + " seconds";
        context.getPlayer().sendMessage(Component.text("Cage trap placed! (Ready in 5s, lasts " + durationMsg + ")", NamedTextColor.GREEN));
    }

    private static void triggerCageTrap(Player hider, Player seeker, HideAndSeek plugin, int paralyzeDuration) {

        boolean iceBlock = ItemSkinSelectionService.isSelected(seeker, ID, "skin_ice_block");

        Location hiderLoc = hider.getLocation().getBlock().getLocation().add(0.5, 0, 0.5);
        hider.teleport(hiderLoc);

        if (iceBlock) {
            hiderLoc.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, hiderLoc.clone().add(0, 1, 0), 14, 0.3, 0.35, 0.3, 0.03);
            hiderLoc.getWorld().spawnParticle(Particle.WAX_ON, hiderLoc.clone().add(0, 1, 0), 12, 0.28, 0.32, 0.28, 0.01);
            hiderLoc.getWorld().playSound(hiderLoc, Sound.BLOCK_BEACON_DEACTIVATE, 0.45f, 0.85f);
        }

        Material wallMaterial = getCageWallMaterial(seeker);
        Material floorCeilingMaterial = getCageFloorMaterial(seeker);

        int cageSize = 3;
        int halfSize = cageSize / 2;

        for (int y = 0; y < 3; y++) {
            for (int x = -halfSize; x <= halfSize; x++) {
                for (int z = -halfSize; z <= halfSize; z++) {

                    boolean isEdge = Math.abs(x) == halfSize || Math.abs(z) == halfSize;
                    boolean isFloor = (y == 0);
                    boolean isCeiling = (y == 2);


                    if (isEdge || isFloor || isCeiling) {
                        Location blockLoc = hiderLoc.clone().add(x, y, z).getBlock().getLocation();
                        Material blockMaterial;


                        if (isEdge) {
                            blockMaterial = wallMaterial;
                        } else {
                            blockMaterial = floorCeilingMaterial;
                        }

                        final int fx = x;
                        final int fz = z;

                        BlockDisplay blockDisplay = blockLoc.getWorld().spawn(blockLoc, BlockDisplay.class, display -> {
                            org.bukkit.block.data.BlockData blockData = blockMaterial.createBlockData();


                            if (blockData instanceof org.bukkit.block.data.type.Fence bars) {
                                bars.setFace(org.bukkit.block.BlockFace.NORTH, isCageBar(fx, fz - 1, halfSize));
                                bars.setFace(org.bukkit.block.BlockFace.SOUTH, isCageBar(fx, fz + 1, halfSize));
                                bars.setFace(org.bukkit.block.BlockFace.EAST, isCageBar(fx + 1, fz, halfSize));
                                bars.setFace(org.bukkit.block.BlockFace.WEST, isCageBar(fx - 1, fz, halfSize));
                            } else if (blockData instanceof org.bukkit.block.data.type.GlassPane pane) {
                                pane.setFace(org.bukkit.block.BlockFace.NORTH, isCageBar(fx, fz - 1, halfSize));
                                pane.setFace(org.bukkit.block.BlockFace.SOUTH, isCageBar(fx, fz + 1, halfSize));
                                pane.setFace(org.bukkit.block.BlockFace.EAST, isCageBar(fx + 1, fz, halfSize));
                                pane.setFace(org.bukkit.block.BlockFace.WEST, isCageBar(fx - 1, fz, halfSize));
                            }

                            display.setBlock(blockData);
                            display.setVisibleByDefault(false);

                            display.setTransformation(new Transformation(
                                    new Vector3f(0, 0, 0),
                                    new AxisAngle4f(0, 0, 0, 0),
                                    new Vector3f(1, 1, 1),
                                    new AxisAngle4f(0, 0, 0, 0)
                            ));
                        });


                        hider.showEntity(plugin, blockDisplay);


                        Bukkit.getScheduler().runTaskLater(plugin, blockDisplay::remove, paralyzeDuration * 20L);
                    }
                }
            }
        }


        hider.setVelocity(new Vector(0, 0, 0));


        hider.addPotionEffect(new PotionEffect(
                PotionEffectType.SLOWNESS,
                paralyzeDuration * 20,
                10,
                false, false, false
        ));


        hider.addPotionEffect(new PotionEffect(
                PotionEffectType.JUMP_BOOST,
                paralyzeDuration * 20,
                250,
                false, false, false
        ));

        hider.sendMessage(Component.text("You've been trapped by a cage!", NamedTextColor.DARK_RED));
        hider.playSound(hiderLoc, Sound.BLOCK_IRON_DOOR_CLOSE, 1.0f, 0.8f);
        hider.playSound(hiderLoc, Sound.BLOCK_CHAIN_PLACE, 1.0f, 1.2f);
    }

    private static ItemStack getCageBarItem(Player seeker) {
        if (ItemSkinSelectionService.isSelected(seeker, CageTrapItem.ID, "skin_laser_grid")) {
            return new ItemStack(Material.MAGENTA_STAINED_GLASS_PANE);
        } else if (ItemSkinSelectionService.isSelected(seeker, CageTrapItem.ID, "skin_ice_block")) {
            return new ItemStack(Material.BLUE_ICE);
        }
        return new ItemStack(Material.IRON_BARS);
    }

    private static Material getCageWallMaterial(Player seeker) {
        if (seeker != null && ItemSkinSelectionService.isSelected(seeker, CageTrapItem.ID, "skin_laser_grid")) {
            return Material.MAGENTA_STAINED_GLASS_PANE;
        } else if (seeker != null && ItemSkinSelectionService.isSelected(seeker, CageTrapItem.ID, "skin_ice_block")) {
            return Material.BLUE_ICE;
        }
        return Material.IRON_BARS;
    }

    private static Material getCageFloorMaterial(Player seeker) {
        if (seeker != null && ItemSkinSelectionService.isSelected(seeker, CageTrapItem.ID, "skin_ice_block")) {

            return Material.PACKED_ICE;
        }

        return Material.BARRIER;
    }

    private static boolean isCageBar(int x, int z, int halfSize) {
        return (Math.abs(x) == halfSize || Math.abs(z) == halfSize);
    }
}
