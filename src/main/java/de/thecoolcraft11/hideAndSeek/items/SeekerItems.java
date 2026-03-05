package de.thecoolcraft11.hideAndSeek.items;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.gui.BlockStatsGUI;
import de.thecoolcraft11.hideAndSeek.listener.player.HiderEquipmentChangeListener;
import de.thecoolcraft11.hideAndSeek.model.LoadoutItemType;
import de.thecoolcraft11.hideAndSeek.util.points.PointAction;
import de.thecoolcraft11.minigameframework.items.CustomItemBuilder;
import de.thecoolcraft11.minigameframework.items.ItemActionType;
import de.thecoolcraft11.minigameframework.items.ItemInteractionContext;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.BlocksAttacks;
import io.papermc.paper.datacomponent.item.blocksattacks.DamageReduction;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.RedstoneWallTorch;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.*;

public final class SeekerItems {
    public static final String GRAPPLING_HOOK_ITEM_ID = "has_seeker_grappling_hook";
    public static final String BLOCK_STATS_ITEM_ID = "has_seeker_block_stats";
    public static final String SEEKERS_MASK_ITEM_ID = "has_seekers_mask";
    public static final String SEEKERS_SWORD_ID = "has_seeker_sword";
    public static final String INK_SPLASH_ITEM_ID = "has_seeker_ink_splash";
    public static final String LIGHTNING_FREEZE_ITEM_ID = "has_seeker_lightning_freeze";
    public static final String GLOWING_COMPASS_ITEM_ID = "has_seeker_glowing_compass";
    public static final String CURSE_SPELL_ITEM_ID = "has_seeker_curse_spell";
    public static final String BLOCK_RANDOMIZER_ITEM_ID = "has_seeker_block_randomizer";
    public static final String CHAIN_PULL_ITEM_ID = "has_seeker_chain_pull";
    public static final String PROXIMITY_SENSOR_ITEM_ID = "has_seeker_proximity_sensor";
    public static final String CAGE_TRAP_ITEM_ID = "has_seeker_cage_trap";

    private static final Map<UUID, Long> seekerCurseActiveUntil = new HashMap<>();
    private static final Map<UUID, Long> hiderCursedUntil = new HashMap<>();
    private static final Map<UUID, ItemStack> inkHelmetBackup = new HashMap<>();
    private static final Map<Location, BlockDisplay> sensorDisplays = new HashMap<>();
    private static final Map<UUID, SwordChargeState> swordChargeStates = new HashMap<>();
    private static final Map<UUID, BukkitTask> swordChargeTasks = new HashMap<>();

    private record SwordChargeState(long startedAtMs, float previousExp, int previousLevel) {
    }

    public static void registerItems(HideAndSeek plugin) {
        registerCooldownItems(plugin);
        registerBlockStats(plugin);
        registerSeekersMask(plugin);
        registerSeekersSword(plugin);
    }

    public static void reregisterCooldownItems(HideAndSeek plugin) {
        unregisterCooldownItems(plugin);
        registerCooldownItems(plugin);
    }

    private static void unregisterCooldownItems(HideAndSeek plugin) {
        plugin.getCustomItemManager().unregisterItem(GRAPPLING_HOOK_ITEM_ID);
        plugin.getCustomItemManager().unregisterItem(INK_SPLASH_ITEM_ID);
        plugin.getCustomItemManager().unregisterItem(LIGHTNING_FREEZE_ITEM_ID);
        plugin.getCustomItemManager().unregisterItem(GLOWING_COMPASS_ITEM_ID);
        plugin.getCustomItemManager().unregisterItem(CURSE_SPELL_ITEM_ID);
        plugin.getCustomItemManager().unregisterItem(BLOCK_RANDOMIZER_ITEM_ID);
        plugin.getCustomItemManager().unregisterItem(CHAIN_PULL_ITEM_ID);
        plugin.getCustomItemManager().unregisterItem(PROXIMITY_SENSOR_ITEM_ID);
        plugin.getCustomItemManager().unregisterItem(CAGE_TRAP_ITEM_ID);
    }

    private static void registerCooldownItems(HideAndSeek plugin) {
        registerGrapplingHook(plugin);


        int inkCooldown = plugin.getSettingRegistry().get("seeker-items.ink-splash.cooldown", 20);
        plugin.getCustomItemManager().registerItem(new CustomItemBuilder(createInkSplashItem(), INK_SPLASH_ITEM_ID)
                .withAction(ItemActionType.RIGHT_CLICK_AIR, context -> spawnInkSplash(context, plugin))
                .withAction(ItemActionType.RIGHT_CLICK_BLOCK, context -> spawnInkSplash(context, plugin))
                .withDescription("Throw ink to blind hiders")
                .withDropPrevention(true)
                .withCraftPrevention(true)
                .withVanillaCooldown(inkCooldown * 20)
                .withCustomCooldown(inkCooldown * 1000L)
                .withVanillaCooldownDisplay(true)
                .allowOffHand(false)
                .allowArmor(false)
                .cancelDefaultAction(true)
                .build());


        int lightningCooldown = plugin.getSettingRegistry().get("seeker-items.lightning-freeze.cooldown", 60);
        plugin.getCustomItemManager().registerItem(new CustomItemBuilder(createLightningFreezeItem(), LIGHTNING_FREEZE_ITEM_ID)
                .withAction(ItemActionType.RIGHT_CLICK_AIR, context -> castLightningFreeze(context.getPlayer(), plugin))
                .withAction(ItemActionType.RIGHT_CLICK_BLOCK, context -> castLightningFreeze(context.getPlayer(), plugin))
                .withDescription("Freeze all hiders")
                .withDropPrevention(true)
                .withCraftPrevention(true)
                .withVanillaCooldown(lightningCooldown * 20)
                .withCustomCooldown(lightningCooldown * 1000L)
                .withVanillaCooldownDisplay(true)
                .allowOffHand(false)
                .allowArmor(false)
                .cancelDefaultAction(true)
                .build());


        int glowCooldown = plugin.getSettingRegistry().get("seeker-items.glowing-compass.cooldown", 25);
        plugin.getCustomItemManager().registerItem(new CustomItemBuilder(createGlowingCompassItem(), GLOWING_COMPASS_ITEM_ID)
                .withAction(ItemActionType.RIGHT_CLICK_AIR, context -> glowHider(context.getPlayer(), plugin))
                .withAction(ItemActionType.RIGHT_CLICK_BLOCK, context -> glowHider(context.getPlayer(), plugin))
                .withDescription("Make nearest hider glow")
                .withDropPrevention(true)
                .withCraftPrevention(true)
                .withVanillaCooldown(glowCooldown * 20)
                .withCustomCooldown(glowCooldown * 1000L)
                .withVanillaCooldownDisplay(true)
                .allowOffHand(false)
                .allowArmor(false)
                .cancelDefaultAction(true)
                .build());


        int curseCooldown = plugin.getSettingRegistry().get("seeker-items.curse-spell.cooldown", 30);
        plugin.getCustomItemManager().registerItem(new CustomItemBuilder(createCurseSpellItem(), CURSE_SPELL_ITEM_ID)
                .withAction(ItemActionType.RIGHT_CLICK_AIR, context -> activateCurseSpell(context.getPlayer(), plugin))
                .withAction(ItemActionType.RIGHT_CLICK_BLOCK, context -> activateCurseSpell(context.getPlayer(), plugin))
                .withDescription("Curse hiders when hitting them")
                .withDropPrevention(true)
                .withCraftPrevention(true)
                .withVanillaCooldown(curseCooldown * 20)
                .withCustomCooldown(curseCooldown * 1000L)
                .withVanillaCooldownDisplay(true)
                .allowOffHand(false)
                .allowArmor(false)
                .cancelDefaultAction(true)
                .build());


        int randCooldown = plugin.getSettingRegistry().get("seeker-items.block-randomizer.cooldown", 45);
        plugin.getCustomItemManager().registerItem(new CustomItemBuilder(createBlockRandomizerItem(), BLOCK_RANDOMIZER_ITEM_ID)
                .withAction(ItemActionType.RIGHT_CLICK_AIR, context -> randomizeAllBlocks(context.getPlayer(), plugin))
                .withAction(ItemActionType.RIGHT_CLICK_BLOCK, context -> randomizeAllBlocks(context.getPlayer(), plugin))
                .withDescription("Randomize all hider blocks")
                .withDropPrevention(true)
                .withCraftPrevention(true)
                .withVanillaCooldown(randCooldown * 20)
                .withCustomCooldown(randCooldown * 1000L)
                .withVanillaCooldownDisplay(true)
                .allowOffHand(false)
                .allowArmor(false)
                .cancelDefaultAction(true)
                .build());


        int chainCooldown = plugin.getSettingRegistry().get("seeker-items.chain-pull.cooldown", 12);
        plugin.getCustomItemManager().registerItem(new CustomItemBuilder(createChainPullItem(), CHAIN_PULL_ITEM_ID)
                .withAction(ItemActionType.RIGHT_CLICK_AIR, context -> chainPull(context.getPlayer(), plugin))
                .withAction(ItemActionType.RIGHT_CLICK_BLOCK, context -> chainPull(context.getPlayer(), plugin))
                .withDescription("Pull hiders back to you")
                .withDropPrevention(true)
                .withCraftPrevention(true)
                .withVanillaCooldown(chainCooldown * 20)
                .withCustomCooldown(chainCooldown * 1000L)
                .withVanillaCooldownDisplay(true)
                .allowOffHand(false)
                .allowArmor(false)
                .build());
        int proximityCooldown = plugin.getSettingRegistry().get("seeker-items.proximity-sensor.cooldown", 20);
        plugin.getCustomItemManager().registerItem(new CustomItemBuilder(createProximitySensorItem(), PROXIMITY_SENSOR_ITEM_ID)
                .withAction(ItemActionType.RIGHT_CLICK_BLOCK, context -> placeProximitySensor(context, plugin))
                .withDescription("Place a proximity sensor")
                .withDropPrevention(true)
                .withCraftPrevention(true)
                .withVanillaCooldown(proximityCooldown * 20)
                .withCustomCooldown(proximityCooldown * 1000L)
                .withVanillaCooldownDisplay(true)
                .allowOffHand(false)
                .allowArmor(false)
                .cancelDefaultAction(true)
                .build());

        int cageCooldown = plugin.getSettingRegistry().get("seeker-items.cage-trap.cooldown", 20);
        plugin.getCustomItemManager().registerItem(new CustomItemBuilder(createCageTrapItem(), CAGE_TRAP_ITEM_ID)
                .withAction(ItemActionType.RIGHT_CLICK_BLOCK, context -> placeCageTrap(context, plugin))
                .withDescription("Place an invisible cage trap")
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

    public static void giveItems(Player player, HideAndSeek plugin) {
        removeItems(player);

        player.getInventory().setItem(0, plugin.getCustomItemManager().getIdentifiedItemStack(SEEKERS_SWORD_ID, player));


        giveLoadoutItems(player, plugin);

        player.addPotionEffect(new PotionEffect(
                PotionEffectType.REGENERATION,
                Integer.MAX_VALUE,
                4,
                false,
                false,
                false
        ));
    }

    public static void giveLoadoutItems(Player player, HideAndSeek plugin) {
        var loadout = plugin.getLoadoutManager().getLoadout(player.getUniqueId());
        int slot = 1;


        var gameModeResult = plugin.getSettingService().getSetting("game.gametype");
        Object gameModeObj = gameModeResult.isSuccess() ? gameModeResult.getValue() : null;
        boolean isBlockMode = gameModeObj != null && gameModeObj.toString().equals("BLOCK");
        boolean blockStatsEnabled = plugin.getSettingRegistry().get("blockstats.enabled", true);

        Set<LoadoutItemType> itemsToGive = loadout.getSeekerItems();


        if (itemsToGive.isEmpty()) {
            plugin.getLogger().info("No loadout selected for " + player.getName() + ", using defaults");
            itemsToGive = Set.of(
                    LoadoutItemType.GRAPPLING_HOOK
            );
        } else {
            plugin.getLogger().info(player.getName() + " has custom loadout with " + itemsToGive.size() + " items");
        }


        boolean hasValidItems = false;
        for (LoadoutItemType itemType : itemsToGive) {
            String itemId = itemType.getItemId();
            if (plugin.getCustomItemManager().getIdentifiedItemStack(itemId, player) != null) {
                hasValidItems = true;
                break;
            }
        }


        if (!hasValidItems && !itemsToGive.isEmpty()) {
            plugin.getLogger().warning("All selected items for " + player.getName() + " are not implemented yet! Using default loadout instead.");
            player.sendMessage(Component.text("Some items you selected are not implemented yet. Using default items instead.", NamedTextColor.YELLOW));
            itemsToGive = Set.of(
                    LoadoutItemType.GRAPPLING_HOOK
            );
        }


        for (LoadoutItemType itemType : itemsToGive) {
            String itemId = itemType.getItemId();
            plugin.getLogger().info("Giving " + player.getName() + " item: " + itemType.name() + " (ID: " + itemId + ") in slot " + slot);
            ItemStack item = plugin.getCustomItemManager().getIdentifiedItemStack(itemId, player);
            if (item != null) {
                player.getInventory().setItem(slot++, item);
                plugin.getLogger().info("  Item placed successfully");
            } else {
                plugin.getLogger().warning("  Item is NULL! Item not registered: " + itemId + " (skipping)");
            }
        }


        if (isBlockMode && blockStatsEnabled) {
            ItemStack blockStats = plugin.getCustomItemManager().getIdentifiedItemStack(BLOCK_STATS_ITEM_ID, player);
            if (blockStats != null) {
                player.getInventory().setItem(8, blockStats);
                plugin.getLogger().info("Gave permanent BlockStats item to " + player.getName() + " in slot 8");
            }
        }

        plugin.getLogger().info("Finished giving loadout items to " + player.getName() + " (" + (slot - 1) + " items placed)");
    }

    public static void applyMask(Player player, HideAndSeek plugin) {
        player.getInventory().setHelmet(plugin.getCustomItemManager().getIdentifiedItemStack(SEEKERS_MASK_ITEM_ID, player));
    }

    public static void removeItems(Player player) {
        player.getInventory().remove(Material.IRON_SWORD);


        player.removePotionEffect(PotionEffectType.REGENERATION);
    }

    public static void removeFromAllPlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            removeItems(player);
        }
    }

    public static void registerGrapplingHook(HideAndSeek plugin) {

        int grapplingHookCooldown = plugin.getSettingRegistry().get("seeker-items.grappling-hook.cooldown", 2);

        plugin.getCustomItemManager().registerItem(new CustomItemBuilder(createGrapplingHookItem(), GRAPPLING_HOOK_ITEM_ID)
                .withAction(ItemActionType.FISHING_REEL, context -> {
                    FishHook hook = context.getFishHook();
                    if (hook != null && hook.isValid()) {
                        pullGrapplingHook(context.getPlayer(), hook, plugin);
                    }
                })
                .withVanillaCooldown(grapplingHookCooldown * 20)
                .withCustomCooldown(grapplingHookCooldown * 1000L)
                .withVanillaCooldownDisplay(true)
                .withDescription("Cast the hook, then pull to launch yourself")
                .withDropPrevention(true)
                .withCraftPrevention(true)
                .allowOffHand(false)
                .allowArmor(false)
                .cancelDefaultAction(false)
                .build());
    }

    public static void registerSeekersMask(HideAndSeek plugin) {
        plugin.getCustomItemManager().registerItem(new CustomItemBuilder(createHidersMaskItem(), SEEKERS_MASK_ITEM_ID)
                .withDropPrevention(true)
                .withCraftPrevention(true)
                .allowOffHand(false)
                .allowArmor(true)
                .withInventoryMovePrevention(true)
                .cancelDefaultAction(false)
                .build());
    }

    public static void registerBlockStats(HideAndSeek plugin) {
        BlockStatsGUI gui = new BlockStatsGUI(plugin);
        plugin.getCustomItemManager().registerItem(new CustomItemBuilder(createBlockStatsItem(), BLOCK_STATS_ITEM_ID)
                .withAction(ItemActionType.RIGHT_CLICK_AIR, context -> gui.open(context.getPlayer()))
                .withAction(ItemActionType.RIGHT_CLICK_BLOCK, context -> gui.open(context.getPlayer()))
                .withDescription("Open block statistics menu")
                .build());
    }

    public static void registerSeekersSword(HideAndSeek plugin) {
        int swordCooldown = plugin.getSettingRegistry().get("seeker-items.sword-of-seeking.cooldown", 5);

        plugin.getCustomItemManager().registerItem(new CustomItemBuilder(createSeekerSword(), SEEKERS_SWORD_ID)
                .withCraftPrevention(true)
                .withDropPrevention(true)
                .cancelDefaultAction(false)
                .withVanillaCooldown(swordCooldown * 20)
                .withCustomCooldown(swordCooldown * 1000L)
                .cancelAttackOnCooldown(false)
                .withAction(ItemActionType.BLOCK_START, context -> startSwordCharge(context, plugin))
                .withAction(ItemActionType.BLOCK_RELEASE, context -> releaseSwordCharge(context, plugin))
                .build());
    }

    public static void giveGrapplingHook(Player player, HideAndSeek plugin) {
        player.getInventory().setItem(1, plugin.getCustomItemManager().getIdentifiedItemStack(GRAPPLING_HOOK_ITEM_ID, player));
    }

    public static void giveBlockStats(Player player, HideAndSeek plugin) {
        boolean blockStatsEnabled = plugin.getSettingRegistry().get("blockstats.enabled", true);
        if (blockStatsEnabled) {
            player.getInventory().setItem(2, plugin.getCustomItemManager().getIdentifiedItemStack(BLOCK_STATS_ITEM_ID, player));
        }
    }


    public static boolean isCurseActive(UUID seekerId) {
        Long until = seekerCurseActiveUntil.get(seekerId);
        if (until == null) {
            return false;
        }
        if (System.currentTimeMillis() > until) {
            seekerCurseActiveUntil.remove(seekerId);
            return false;
        }
        return true;
    }

    public static boolean isHiderCursed(UUID hiderId) {
        Long until = hiderCursedUntil.get(hiderId);
        if (until == null) {
            return false;
        }
        if (System.currentTimeMillis() > until) {
            hiderCursedUntil.remove(hiderId);
            return false;
        }
        return true;
    }

    public static void applyCurseToHider(Player hider, HideAndSeek plugin) {
        int duration = plugin.getSettingRegistry().get("seeker-items.curse-spell.curse-duration", 8);
        long until = System.currentTimeMillis() + (duration * 1000L);
        hiderCursedUntil.put(hider.getUniqueId(), until);

        var gameModeResult = plugin.getSettingService().getSetting("game.gametype");
        Object gameModeObj = gameModeResult.isSuccess() ? gameModeResult.getValue() : null;
        String mode = gameModeObj != null ? gameModeObj.toString() : "";

        if ("BLOCK".equals(mode)) {
            if (HideAndSeek.getDataController().isHidden(hider.getUniqueId())) {
                plugin.getBlockModeListener().forceUnhide(hider);
            }
        } else {
            hider.removePotionEffect(PotionEffectType.INVISIBILITY);
        }

        if ("SMALL".equals(mode)) {
            var smallSizeResult = plugin.getSettingService().getSetting("game.small_mode_size");
            Object sizeObj = smallSizeResult.isSuccess() ? smallSizeResult.getValue() : 0.5;
            double smallSize = (sizeObj instanceof Number) ? ((Number) sizeObj).doubleValue() : 0.5;

            int shrinkDelay = plugin.getSettingRegistry().get("seeker-items.curse-spell.small-shrink-delay", 8);
            var scale = hider.getAttribute(Attribute.SCALE);
            if (scale != null) {
                scale.setBaseValue(1.0);
                Bukkit.getScheduler().runTaskLater(plugin, () -> scale.setBaseValue(smallSize), shrinkDelay * 20L);
            }
        }

        hider.sendMessage(Component.text("You have been cursed!", NamedTextColor.DARK_PURPLE));


        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!hider.isOnline() || !isHiderCursed(hider.getUniqueId())) {
                    cancel();
                    return;
                }

                Location loc = hider.getLocation().add(0, 1, 0);

                hider.getWorld().spawnParticle(Particle.SOUL, loc, 8, 0.3, 0.5, 0.3, 0.1);

                hider.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, loc, 5, 0.2, 0.3, 0.2, 0.05);

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 5L);
    }

    private static void pullGrapplingHook(Player seeker, FishHook hook, HideAndSeek plugin) {
        if (seeker == null || !hook.isValid()) return;


        seeker.setCooldown(Material.FISHING_ROD, 100);

        Location playerLoc = seeker.getEyeLocation();
        Location hookLoc = hook.getLocation();
        Vector travelVec = hookLoc.toVector().subtract(playerLoc.toVector());
        double distance = travelVec.length();

        if (distance < 1.5) return;


        Vector direction = travelVec.normalize();
        double baseSpeed = plugin.getSettingRegistry().get("seeker-items.grappling-hook.speed", 1.3);


        double distanceFactor = Math.min(Math.sqrt(distance) * 0.25, 2.0);
        double gravityCompensation = (direction.getY() > 0) ? (1.0 + direction.getY() * 0.8) : 1.0;
        double speed = (baseSpeed + distanceFactor) * gravityCompensation;

        Vector finalVel = direction.multiply(speed);


        if (seeker.isOnGround()) {
            finalVel.setY(finalVel.getY() + 0.25);
        }

        seeker.setVelocity(seeker.getVelocity().multiply(0.3).add(finalVel.multiply(0.7)));

        World world = seeker.getWorld();
        world.spawnParticle(Particle.GUST_EMITTER_LARGE, seeker.getLocation(), 2);
        world.spawnParticle(Particle.SONIC_BOOM, seeker.getLocation(), 1, 0, 0, 0, 0);
        world.playSound(seeker.getLocation(), Sound.ENTITY_WIND_CHARGE_WIND_BURST, 1.2f, 1.2f);


        drawGrappleLine(seeker, hookLoc);


        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {

                if (!seeker.isOnline() || !seeker.getWorld().equals(hookLoc.getWorld()) || (ticks > 5 && seeker.isOnGround()) || ticks > 30) {

                    if (seeker.isOnline() && seeker.getWorld().equals(hookLoc.getWorld()) && seeker.getLocation().distance(hookLoc) < 3) {
                        seeker.getWorld().playSound(seeker.getLocation(), Sound.ENTITY_WIND_CHARGE_THROW, 1f, 0.5f);
                    }
                    this.cancel();
                    return;
                }


                seeker.getWorld().spawnParticle(Particle.CLOUD, seeker.getLocation(), 3, 0.2, 0.2, 0.2, 0.05);
                if (ticks % 2 == 0) {
                    seeker.getWorld().spawnParticle(Particle.GUST, seeker.getLocation(), 1, 0, 0, 0, 0);
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 1L, 1L);

        hook.remove();
    }

    private static void drawGrappleLine(Player seeker, Location hookLoc) {
        Location start = seeker.getEyeLocation().subtract(0, 0.3, 0);
        Vector direction = hookLoc.toVector().subtract(start.toVector());
        double distance = direction.length();
        direction.normalize();


        for (double i = 0; i < distance; i += 0.4) {
            Location point = start.clone().add(direction.clone().multiply(i));

            seeker.getWorld().spawnParticle(Particle.TRIAL_SPAWNER_DETECTION, point, 1, 0, 0, 0, 0);
            if (i % 0.8 == 0) {
                seeker.getWorld().spawnParticle(Particle.WHITE_SMOKE, point, 1, 0.01, 0.01, 0.01, 0);
            }
        }
    }


    private static ItemStack createGrapplingHookItem() {
        ItemStack item = new ItemStack(Material.FISHING_ROD);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.displayName(Component.text("Grappling Hook", NamedTextColor.AQUA, TextDecoration.BOLD)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.text("Right click to launch yourself forward", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false),
                    Component.text("toward where you're looking", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false)
            ));
            meta.setUnbreakable(true);
            item.setItemMeta(meta);
        }

        return item;
    }

    private static ItemStack createHidersMaskItem() {
        ItemStack item = new ItemStack(Material.CARVED_PUMPKIN);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.displayName(Component.text("Seekers Mask", NamedTextColor.GRAY, TextDecoration.BOLD)
                    .decoration(TextDecoration.ITALIC, false));
            meta.setUnbreakable(true);
            meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            meta.setEnchantmentGlintOverride(false);
            meta.addEnchant(Enchantment.BINDING_CURSE, 1, true);
            meta.setItemModel(new NamespacedKey("minecraft", "air"));
            item.setItemMeta(meta);
        }

        return item;
    }


    private static ItemStack createSeekerSword() {
        ItemStack sword = new ItemStack(Material.IRON_SWORD);
        ItemMeta meta = sword.getItemMeta();

        if (meta != null) {
            meta.displayName(Component.text("Seeker's Blade", NamedTextColor.RED, TextDecoration.BOLD)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.text("Hunt down the hiders!", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false)
            ));
            meta.setUnbreakable(true);
            meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
            sword.setItemMeta(meta);
            sword.setData(DataComponentTypes.BLOCKS_ATTACKS, BlocksAttacks.blocksAttacks().addDamageReduction(DamageReduction.damageReduction().base(0).factor(0).horizontalBlockingAngle(0.1f).build()).build());
        }

        return sword;
    }

    private static ItemStack createBlockStatsItem() {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.displayName(Component.text("Block Statistics", NamedTextColor.AQUA, TextDecoration.BOLD)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.text("Right click to view block stats", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false)
            ));
            item.setItemMeta(meta);
        }

        return item;
    }


    private static void spawnInkSplash(ItemInteractionContext context, HideAndSeek plugin) {
        Player seeker = context.getPlayer();
        int radius = plugin.getSettingRegistry().get("seeker-items.ink-splash.radius", 25);
        int duration = plugin.getSettingRegistry().get("seeker-items.ink-splash.duration", 7);

        for (Player hider : Bukkit.getOnlinePlayers()) {
            if (!HideAndSeek.getDataController().getHiders().contains(hider.getUniqueId())) continue;
            if (hider.getLocation().distance(seeker.getLocation()) > radius) continue;

            plugin.getPointService().award(seeker.getUniqueId(), PointAction.SEEKER_UTILITY_SUCCESS);
            plugin.getPointService().markUtilitySpotted(hider.getUniqueId());

            ItemStack previous = hider.getInventory().getHelmet();
            inkHelmetBackup.put(hider.getUniqueId(), previous);
            HiderItems.applyMask(hider, plugin);
            HiderEquipmentChangeListener.hideHelmet(hider);
            BukkitTask task = Bukkit.getScheduler().runTaskTimer(
                    plugin,
                    () -> HiderEquipmentChangeListener.hideHelmet(hider),
                    0L, 1L
            );


            hider.getWorld().spawnParticle(Particle.SQUID_INK, hider.getEyeLocation(), 10, 0.3, 0.3, 0.3, 0.1);

            hider.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, duration * 20, 255, false, false));
            hider.sendMessage(Component.text("You've been hit with ink!", NamedTextColor.DARK_AQUA));

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                task.cancel();
                ItemStack restore = inkHelmetBackup.remove(hider.getUniqueId());
                hider.getInventory().setHelmet(restore);
                HiderEquipmentChangeListener.hideHelmet(hider);
            }, duration * 20L);
        }
    }

    private static void castLightningFreeze(Player seeker, HideAndSeek plugin) {
        int duration = plugin.getSettingRegistry().get("seeker-items.lightning-freeze.duration", 5);

        for (Player hider : Bukkit.getOnlinePlayers()) {
            if (!HideAndSeek.getDataController().getHiders().contains(hider.getUniqueId())) continue;
            if (!hider.getWorld().equals(seeker.getWorld())) continue;

            plugin.getPointService().award(seeker.getUniqueId(), PointAction.SEEKER_UTILITY_SUCCESS);
            plugin.getPointService().markUtilitySpotted(hider.getUniqueId());

            hider.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, duration * 20, 10, false, false));
            hider.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, duration * 20, 250, false, false));

            hider.spawnParticle(Particle.ELECTRIC_SPARK, hider.getLocation().add(0, 1.0, 0), 20, 0.3, 0.3, 0.3, 0.05);
            hider.spawnParticle(Particle.FLASH, hider.getLocation().add(0, 1.0, 0), 1, 0, 0, 0, Color.fromARGB(0xFFFFFF));
            hider.playSound(hider.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.0f);
            Entity entity = hider.getWorld().spawnEntity(hider.getLocation(), EntityType.LIGHTNING_BOLT);
            entity.getPersistentDataContainer().set(new NamespacedKey(plugin, "freezeLightning"), PersistentDataType.BOOLEAN, true);
            Bukkit.getOnlinePlayers().stream().filter(player -> !player.getUniqueId().equals(hider.getUniqueId())).forEach(p -> p.hideEntity(plugin, entity));
        }

        seeker.sendMessage(Component.text("All hiders frozen!", NamedTextColor.AQUA));
        seeker.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, seeker.getLocation().add(0, 1.0, 0), 15, 0.5, 0.5, 0.5, 0.1);
    }

    private static void glowHider(Player seeker, HideAndSeek plugin) {
        double range = plugin.getSettingRegistry().get("seeker-items.glowing-compass.range", 50.0);
        int duration = plugin.getSettingRegistry().get("seeker-items.glowing-compass.duration", 10);

        Player nearest = null;
        double distance = range;

        for (UUID hiderId : HideAndSeek.getDataController().getHiders()) {
            Player hider = Bukkit.getPlayer(hiderId);
            if (hider == null) continue;
            if (!hider.getWorld().equals(seeker.getWorld())) continue;

            double dist = hider.getLocation().distance(seeker.getLocation());
            if (dist < distance) {
                distance = dist;
                nearest = hider;
            }
        }

        if (nearest != null) {
            applyGlowEffect(nearest, duration, plugin);
            plugin.getPointService().award(seeker.getUniqueId(), PointAction.SEEKER_UTILITY_SUCCESS);
            plugin.getPointService().markUtilitySpotted(nearest.getUniqueId());
            seeker.sendMessage(Component.text(nearest.getName() + " is now glowing!", NamedTextColor.GOLD));
        } else {
            seeker.sendMessage(Component.text("No hiders found nearby!", NamedTextColor.RED));
        }
    }

    private static void applyGlowEffect(Player hider, int duration, HideAndSeek plugin) {
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


        Bukkit.getScheduler().runTaskLater(plugin, () -> removeGlowEffect(hider, isBlockMode, plugin), duration * 20L);
    }

    private static void removeGlowEffect(Player hider, boolean isBlockMode, HideAndSeek plugin) {
        UUID hiderId = hider.getUniqueId();
        HideAndSeek.getDataController().removeGlowing(hiderId);

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

    private static void activateCurseSpell(Player seeker, HideAndSeek plugin) {
        int duration = plugin.getSettingRegistry().get("seeker-items.curse-spell.active-duration", 10);
        long until = System.currentTimeMillis() + (duration * 1000L);
        seekerCurseActiveUntil.put(seeker.getUniqueId(), until);

        ItemStack sword = seeker.getInventory().getItemInMainHand();
        ItemMeta meta = sword.getItemMeta();
        if (meta != null) {
            meta.addEnchant(Enchantment.SHARPNESS, 1, true);

            sword.setItemMeta(meta);
        }

        seeker.sendMessage(Component.text("Curse spell activated! (" + duration + "s)", NamedTextColor.DARK_PURPLE));

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            seekerCurseActiveUntil.remove(seeker.getUniqueId());
            ItemStack s = seeker.getInventory().getItemInMainHand();
            ItemMeta m = s.getItemMeta();
            if (m != null) {
                m.removeEnchant(Enchantment.SHARPNESS);
                s.setItemMeta(m);
            }
        }, duration * 20L);
    }

    private static void randomizeAllBlocks(Player seeker, HideAndSeek plugin) {
        var gameModeResult = plugin.getSettingService().getSetting("game.gametype");
        Object gameModeObj = gameModeResult.isSuccess() ? gameModeResult.getValue() : null;
        if (gameModeObj == null || !gameModeObj.toString().equals("BLOCK")) {
            seeker.sendMessage(Component.text("Block Randomizer is only available in BLOCK mode.", NamedTextColor.RED));
            return;
        }

        int count = 0;
        for (UUID hiderId : HideAndSeek.getDataController().getHiders()) {
            Player hider = Bukkit.getPlayer(hiderId);
            if (hider == null) continue;
            HiderItems.randomizeBlockFor(hider, plugin, true);

            hider.getWorld().spawnParticle(Particle.ENCHANT, hider.getLocation().add(0, 1, 0), 15, 0.4, 0.4, 0.4, 0.1);
            hider.playSound(hider.getLocation(), Sound.ITEM_ARMOR_EQUIP_GENERIC, 1.0f, 1.5f);
            count++;
        }
        seeker.sendMessage(Component.text("Blocks randomized! (" + count + " hiders)", NamedTextColor.GREEN));
    }

    private static Location findSafeLandingLocation(Location loc, World world) {
        Location checkLoc = loc.clone();


        Block block;


        int maxAttempts = 10;
        for (int i = 0; i < maxAttempts; i++) {
            block = world.getBlockAt(checkLoc);
            if (!block.getType().isSolid() && (i == 0 || world.getBlockAt(checkLoc.clone().add(0, -1, 0)).getType().isSolid())) {
                return checkLoc;
            }
            checkLoc.add(0, 1, 0);
        }


        checkLoc = loc.clone();
        Block below = world.getBlockAt(checkLoc.clone().add(0, -0.5, 0));
        if (!below.getType().isSolid()) {

            while (checkLoc.getY() > world.getMinHeight()) {
                below = world.getBlockAt(checkLoc.clone().add(0, -1, 0));
                if (below.getType().isSolid()) {
                    checkLoc.add(0, 1, 0);
                    break;
                }
                checkLoc.add(0, -1, 0);
            }
        }

        return checkLoc;
    }

    private static void chainPull(Player seeker, HideAndSeek plugin) {
        double range = plugin.getSettingRegistry().get("seeker-items.chain-pull.range", 30.0);
        double power = plugin.getSettingRegistry().get("seeker-items.chain-pull.pull-power", 2.0);
        double cosThreshold = Math.cos(Math.toRadians(30));

        Vector dir = seeker.getLocation().getDirection().normalize();
        Player target = null;
        double closest = range;

        for (UUID hiderId : HideAndSeek.getDataController().getHiders()) {
            Player hider = Bukkit.getPlayer(hiderId);
            if (hider == null) continue;

            Vector toHider = hider.getLocation().toVector().subtract(seeker.getLocation().toVector());
            double dist = toHider.length();
            if (dist > range) continue;

            double cos = dir.dot(toHider.normalize());
            if (cos < cosThreshold) continue;

            if (dist < closest) {
                closest = dist;
                target = hider;
            }
        }

        if (target == null) {
            seeker.sendMessage(Component.text("No hider in range.", NamedTextColor.RED));
            return;
        }

        final Player finalTarget = target;

        Vector direction = seeker.getLocation().getDirection().normalize();
        Location targetLocation = seeker.getLocation().add(direction.multiply(1.5));
        targetLocation.setYaw(seeker.getLocation().getYaw());
        targetLocation.setPitch(0);


        final Location finalTargetLocation = findSafeLandingLocation(targetLocation, seeker.getWorld());

        int slownessDuration = plugin.getSettingRegistry().get("seeker-items.chain-pull.slowness-duration", 3);
        int pullTicks = 8;

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                try {
                    if (!finalTarget.isOnline() || !finalTarget.getWorld().equals(seeker.getWorld())) {
                        cancel();
                        return;
                    }


                    drawChainParticles(seeker, finalTarget);

                    Location current = finalTarget.getLocation();
                    Vector toTarget = finalTargetLocation.toVector().subtract(current.toVector());
                    double distance = toTarget.length();

                    if (distance < 0.6 || ticks >= pullTicks) {
                        Location safeLanding = findSafeLandingLocation(finalTarget.getLocation(), seeker.getWorld());
                        finalTarget.teleport(safeLanding);
                        finalTarget.setVelocity(new Vector(0, 0, 0));
                        finalTarget.addPotionEffect(new PotionEffect(
                                PotionEffectType.SLOWNESS,
                                slownessDuration * 20,
                                2,
                                false,
                                false,
                                false
                        ));
                        seeker.sendMessage(Component.text("Pulled " + finalTarget.getName() + "!", NamedTextColor.GREEN));
                        finalTarget.sendMessage(Component.text("You've been pulled by a chain!", NamedTextColor.DARK_GRAY));
                        cancel();
                        return;
                    }

                    Vector velocity = toTarget.normalize().multiply(Math.min(power, distance));
                    finalTarget.setVelocity(velocity);
                    ticks++;
                } catch (Exception e) {

                    cancel();
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    private static void drawChainParticles(Player seeker, Player hider) {
        Location start = seeker.getEyeLocation().subtract(0, 0.3, 0);
        Location end = hider.getEyeLocation().subtract(0, 0.3, 0);

        double dx = end.getX() - start.getX();
        double dy = end.getY() - start.getY();
        double dz = end.getZ() - start.getZ();
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (distance < 0.1) return;

        World world = start.getWorld();
        int steps = (int) (distance / 0.35);

        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;

            Location point = new Location(world,
                    start.getX() + dx * t,
                    start.getY() + dy * t,
                    start.getZ() + dz * t);


            int r = (int) (60 + (255 - 60) * t);
            int g = (int) (140 + (80 - 140) * t);
            int b = (int) (255 + (10 - 255) * t);
            world.spawnParticle(Particle.DUST, point, 1, 0, 0, 0, 0,
                    new Particle.DustOptions(Color.fromRGB(r, g, b), 0.75f));


            if (i % 2 == 0) {
                world.spawnParticle(Particle.DUST, point, 1, 0, 0, 0, 0,
                        new Particle.DustOptions(Color.fromRGB(255, 255, 255), 0.3f));
            }


            if (Math.random() < 0.07) {
                world.spawnParticle(Particle.CRIT, point, 1, 0.03, 0.03, 0.03, 0.01);
            }
        }


        world.spawnParticle(Particle.DUST, start, 4, 0.08, 0.08, 0.08, 0,
                new Particle.DustOptions(Color.fromRGB(60, 140, 255), 1.1f));
        world.spawnParticle(Particle.END_ROD, start, 1, 0.04, 0.04, 0.04, 0.003);

        world.spawnParticle(Particle.DUST, end, 4, 0.08, 0.08, 0.08, 0,
                new Particle.DustOptions(Color.fromRGB(255, 80, 10), 1.1f));
        world.spawnParticle(Particle.END_ROD, end, 1, 0.04, 0.04, 0.04, 0.003);
    }


    private static ItemStack createInkSplashItem() {
        ItemStack item = new ItemStack(Material.INK_SAC);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Ink Splash", NamedTextColor.DARK_AQUA, TextDecoration.BOLD)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.text("Right click to blind hiders", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false)
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack createLightningFreezeItem() {
        ItemStack item = new ItemStack(Material.LIGHTNING_ROD);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Lightning Freeze", NamedTextColor.YELLOW, TextDecoration.BOLD)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.text("Right click to freeze all hiders", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false)
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack createGlowingCompassItem() {
        ItemStack item = new ItemStack(Material.COMPASS);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Glowing Compass", NamedTextColor.GOLD, TextDecoration.BOLD)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.text("Right click to glow the nearest hider", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false)
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack createCurseSpellItem() {
        ItemStack item = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Curse Spell", NamedTextColor.DARK_PURPLE, TextDecoration.BOLD)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.text("Right click to curse hiders", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false)
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack createBlockRandomizerItem() {
        ItemStack item = new ItemStack(Material.BLAZE_POWDER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Block Randomizer", NamedTextColor.RED, TextDecoration.BOLD)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.text("Right click to randomize all blocks", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false)
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack createChainPullItem() {
        ItemStack item = new ItemStack(Material.LEAD);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Chain Pull", NamedTextColor.DARK_GRAY, TextDecoration.BOLD)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.text("Right click to pull hiders", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false)
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    private static Color textColorToColor(TextColor chatColor) {
        return chatColor != null ? Color.fromRGB(chatColor.red(), chatColor.green(), chatColor.blue()) : Color.WHITE;
    }

    private static ItemStack createProximitySensorItem() {
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
            torchBlock.setBlockData(torchData);
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


            if (torchData instanceof RedstoneWallTorch wallTorch) {
                wallTorch.setFacing(clickedFace);
                torchBlock.setBlockData(wallTorch);


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
                            Vector toHider = hider.getLocation().toVector().subtract(sensorLocation.toVector()).normalize();
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

    private static ItemStack createCageTrapItem() {
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

    private static void placeCageTrap(ItemInteractionContext context, HideAndSeek plugin) {
        Location location = context.getLocation().clone().add(0.5, 1, 0.5);
        double range = plugin.getSettingRegistry().get("seeker-items.cage-trap.range", 3.0);
        int paralyzeDuration = plugin.getSettingRegistry().get("seeker-items.cage-trap.paralyze-duration", 5);
        int trapDuration = plugin.getSettingRegistry().get("seeker-items.cage-trap.duration", -1);
        int setupTime = plugin.getSettingRegistry().get("seeker-items.cage-trap.setup-time", 5);

        ItemDisplay[] trapIndicators = new ItemDisplay[3];
        Location indicatorLoc = location.clone().add(0, 0, 0);
        ItemDisplay trapIndicator1 = indicatorLoc.getWorld().spawn(indicatorLoc, ItemDisplay.class, display -> {
            ItemStack ironBars = new ItemStack(Material.IRON_BARS);
            display.setItemStack(ironBars);
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
            ItemStack ironBars = new ItemStack(Material.IRON_BARS);
            display.setItemStack(ironBars);
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

        new BukkitRunnable() {
            final long startTime = System.currentTimeMillis();
            final long durationMs = trapDuration == -1 ? Long.MAX_VALUE : (long) trapDuration * 1000L;
            boolean triggered = false;
            boolean readyToTrigger = false;

            @Override
            public void run() {
                long elapsedTime = System.currentTimeMillis() - startTime;
                long setupTimeMs = setupTime * 1000L;

                if (!readyToTrigger && elapsedTime >= setupTimeMs) {
                    readyToTrigger = true;
                }

                if (trapDuration != -1 && elapsedTime > durationMs) {
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
                        triggerCageTrap(hider, plugin, paralyzeDuration);
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

    private static void triggerCageTrap(Player hider, HideAndSeek plugin, int paralyzeDuration) {

        Location hiderLoc = hider.getLocation().getBlock().getLocation().add(0.5, 0, 0.5);
        hider.teleport(hiderLoc);

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
                            blockMaterial = Material.IRON_BARS;
                        } else {
                            blockMaterial = Material.BARRIER;
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

    private static boolean isCageBar(int x, int z, int halfSize) {
        return (Math.abs(x) == halfSize || Math.abs(z) == halfSize);
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

    private static void startSwordCharge(ItemInteractionContext context, HideAndSeek plugin) {
        context.skipCooldown();
        Player seeker = context.getPlayer();
        int maxChargeSeconds = Math.max(1, plugin.getSettingRegistry().get("seeker-items.seeker-sword-throw.max-charge-seconds", 5));
        long maxChargeMs = maxChargeSeconds * 1000L;

        clearSwordCharge(seeker);
        swordChargeStates.put(seeker.getUniqueId(), new SwordChargeState(System.currentTimeMillis(), seeker.getExp(), seeker.getLevel()));


        seeker.setLevel(0);
        seeker.setExp(0f);

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!seeker.isOnline() || !plugin.getCustomItemManager().hasItemInMainHand(seeker, SEEKERS_SWORD_ID)) {
                clearSwordCharge(seeker);
                return;
            }

            SwordChargeState chargeState = swordChargeStates.get(seeker.getUniqueId());
            if (chargeState == null) {
                clearSwordCharge(seeker);
                return;
            }

            long elapsed = Math.min(System.currentTimeMillis() - chargeState.startedAtMs, maxChargeMs);
            seeker.setLevel(0);
            seeker.setExp((float) elapsed / (float) maxChargeMs);
        }, 1L, 1L);

        swordChargeTasks.put(seeker.getUniqueId(), task);
    }

    private static void releaseSwordCharge(ItemInteractionContext context, HideAndSeek plugin) {
        Player seeker = context.getPlayer();
        int maxChargeSeconds = Math.max(1, plugin.getSettingRegistry().get("seeker-items.seeker-sword-throw.max-charge-seconds", 5));
        long maxChargeMs = maxChargeSeconds * 1000L;

        SwordChargeState state = swordChargeStates.get(seeker.getUniqueId());
        long measuredHoldMs = Math.max(0L, context.getHoldDurationMs());
        if (state != null) {
            measuredHoldMs = Math.max(measuredHoldMs, System.currentTimeMillis() - state.startedAtMs);
        }

        clearSwordCharge(seeker);

        double chargeRatio = Math.min(1.0, measuredHoldMs / (double) maxChargeMs);
        throwChargedSword(seeker, plugin, chargeRatio);
    }

    private static void clearSwordCharge(Player player) {
        BukkitTask existingTask = swordChargeTasks.remove(player.getUniqueId());
        if (existingTask != null) {
            existingTask.cancel();
        }

        SwordChargeState state = swordChargeStates.remove(player.getUniqueId());
        if (state != null && player.isOnline()) {
            player.setLevel(state.previousLevel);
            player.setExp(state.previousExp);
        }
    }

    public static void cleanupSwordCharge(UUID playerId) {
        Player player = Bukkit.getPlayer(playerId);
        if (player != null) {
            clearSwordCharge(player);
            return;
        }

        BukkitTask existingTask = swordChargeTasks.remove(playerId);
        if (existingTask != null) {
            existingTask.cancel();
        }
        swordChargeStates.remove(playerId);
    }

    private static void throwChargedSword(Player seeker, HideAndSeek plugin, double chargeRatio) {
        if (!seeker.isOnline()) {
            return;
        }

        double minSpeed = plugin.getSettingRegistry().get("seeker-items.seeker-sword-throw.min-speed", 0.8);
        double maxSpeed = plugin.getSettingRegistry().get("seeker-items.seeker-sword-throw.max-speed", 2.4);
        double gravity = plugin.getSettingRegistry().get("seeker-items.seeker-sword-throw.gravity", 0.035);
        int maxFlightSeconds = Math.max(1, plugin.getSettingRegistry().get("seeker-items.seeker-sword-throw.max-flight-seconds", 6));
        int stuckSeconds = Math.max(1, plugin.getSettingRegistry().get("seeker-items.seeker-sword-throw.stuck-seconds", 12));
        double hitbox = plugin.getSettingRegistry().get("seeker-items.seeker-sword-throw.hitbox", 0.4);

        double speed = minSpeed + (maxSpeed - minSpeed) * chargeRatio;
        Location start = seeker.getEyeLocation().add(seeker.getEyeLocation().getDirection().normalize().multiply(0.6)).add(0, -0.2, 0);
        Vector velocity = seeker.getEyeLocation().getDirection().normalize().multiply(speed);

        ItemDisplay swordDisplay = start.getWorld().spawn(start, ItemDisplay.class, display -> {
            display.setItemStack(createSeekerSword());
            display.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
            display.setInterpolationDuration(1);
            display.setTransformation(new Transformation(
                    new Vector3f(-0.15f, -0.12f, -0.15f),
                    new AxisAngle4f((float) Math.toRadians(90), 1f, 0f, 0f),
                    new Vector3f(0.7f, 0.7f, 0.7f),
                    new AxisAngle4f(0, 0, 0, 0)
            ));
        });

        seeker.getWorld().playSound(seeker.getLocation(), Sound.ITEM_TRIDENT_THROW, 1.0f, (float) (0.8 + chargeRatio * 0.5));

        new BukkitRunnable() {
            Location current = start.clone();
            int ticks = 0;
            float rotationAngle = 0f;

            @Override
            public void run() {
                if (!seeker.isOnline() || !swordDisplay.isValid()) {
                    removeDisplay();
                    cancel();
                    return;
                }

                if (ticks++ > maxFlightSeconds * 20) {
                    removeDisplay();
                    cancel();
                    return;
                }

                Location previous = current.clone();
                velocity.setY(velocity.getY() - gravity);
                current = current.add(velocity);

                World world = current.getWorld();
                Vector travel = current.toVector().subtract(previous.toVector());
                double distance = travel.length();
                if (distance <= 0.0001) {
                    return;
                }
                Vector direction = travel.clone().normalize();

                RayTraceResult entityTrace = world.rayTraceEntities(
                        previous,
                        direction,
                        distance,
                        hitbox,
                        entity -> entity instanceof Player target
                                && !target.getUniqueId().equals(seeker.getUniqueId())
                                && HideAndSeek.getDataController().getHiders().contains(target.getUniqueId())
                );

                if (entityTrace != null && entityTrace.getHitEntity() instanceof Player target) {
                    double damage = getThrownSwordDamage(seeker);
                    target.damage(damage, seeker);
                    target.getWorld().spawnParticle(Particle.CRIT, target.getLocation().add(0, 1, 0), 10, 0.25, 0.4, 0.25, 0.02);
                    target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_HURT, 1.0f, 1.0f);
                    seeker.playSound(seeker.getLocation(), Sound.ENTITY_ARROW_HIT_PLAYER, 1.0f, 1.1f);

                    removeDisplay();
                    cancel();
                    return;
                }

                RayTraceResult blockTrace = world.rayTraceBlocks(
                        previous,
                        direction,
                        distance,
                        FluidCollisionMode.NEVER,
                        true
                );

                if (blockTrace != null) {
                    Vector hitNormal = blockTrace.getHitBlockFace() != null ?
                            blockTrace.getHitBlockFace().getDirection() : new Vector(0, 1, 0);

                    Location impact = blockTrace.getHitPosition().toLocation(world);
                    impact.add(hitNormal.clone().multiply(0.175));
                    impact.setYaw(rotationAngle + 180f);
                    impact.setPitch(0);


                    Vector bladeDirection = hitNormal.clone().multiply(-1);


                    float targetPitch = (float) Math.toDegrees(Math.asin(-bladeDirection.getY()));
                    float targetYaw = (float) Math.toDegrees(Math.atan2(-bladeDirection.getX(), bladeDirection.getZ()));


                    Quaternionf stuckRotation = new Quaternionf()
                            .rotateX((float) Math.toRadians(90))
                            .rotateY((float) Math.toRadians(targetYaw))
                            .rotateX((float) Math.toRadians(targetPitch))
                            .rotateZ((float) Math.toRadians(rotationAngle));

                    swordDisplay.setTransformation(new Transformation(
                            new Vector3f(-0.15f, -0.12f, -0.15f),
                            new AxisAngle4f(stuckRotation),
                            new Vector3f(0.7f, 0.7f, 0.7f),
                            new AxisAngle4f(0, 0, 0, 0)
                    ));

                    swordDisplay.teleport(impact);
                    swordDisplay.setInterpolationDelay(0);
                    swordDisplay.setInterpolationDuration(3);
                    world.playSound(impact, Sound.ITEM_TRIDENT_HIT_GROUND, 1.0f, 0.9f);

                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        if (swordDisplay.isValid()) {
                            swordDisplay.remove();
                        }
                    }, stuckSeconds * 20L);

                    cancel();
                    return;
                }

                rotationAngle += 30f;
                if (rotationAngle >= 360f) {
                    rotationAngle -= 360f;
                }

                float pitch = (float) Math.toDegrees(Math.asin(-direction.getY()));
                float yaw = (float) Math.toDegrees(Math.atan2(-direction.getX(), direction.getZ()));

                Quaternionf rotation = new Quaternionf()
                        .rotateY((float) Math.toRadians(yaw))
                        .rotateX((float) Math.toRadians(pitch))
                        .rotateZ((float) Math.toRadians(rotationAngle));

                swordDisplay.setTransformation(new Transformation(
                        new Vector3f(-0.15f, -0.12f, -0.15f),
                        new AxisAngle4f(rotation),
                        new Vector3f(0.7f, 0.7f, 0.7f),
                        new AxisAngle4f(0, 0, 0, 0)
                ));

                swordDisplay.teleport(current);
                world.spawnParticle(Particle.SWEEP_ATTACK, current, 1, 0, 0, 0, 0);
            }

            private void removeDisplay() {
                if (swordDisplay.isValid()) {
                    swordDisplay.remove();
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    private static double getThrownSwordDamage(Player seeker) {
        double damage = 6.0;
        var attackAttr = seeker.getAttribute(Attribute.ATTACK_DAMAGE);
        if (attackAttr != null) {
            damage = attackAttr.getValue();
        }

        ItemStack hand = seeker.getInventory().getItemInMainHand();
        int sharpnessLevel = hand.getEnchantmentLevel(Enchantment.SHARPNESS);
        if (sharpnessLevel > 0) {
            damage += 0.5 * sharpnessLevel + 0.5;
        }

        return damage;
    }

}

