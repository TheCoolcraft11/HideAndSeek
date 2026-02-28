package de.thecoolcraft11.hideAndSeek.items;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.gui.BlockStatsGUI;
import de.thecoolcraft11.hideAndSeek.listener.HiderEquipmentChangeListener;
import de.thecoolcraft11.minigameframework.items.CustomItemBuilder;
import de.thecoolcraft11.minigameframework.items.ItemActionType;
import de.thecoolcraft11.minigameframework.items.ItemInteractionContext;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
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
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.util.*;

public final class SeekerItems {
    public static final String GRAPPLING_HOOK_ITEM_ID = "had_seeker_grappling_hook";
    public static final String BLOCK_STATS_ITEM_ID = "had_seeker_block_stats";
    public static final String SEEKERS_MASK_ITEM_ID = "had_seekers_mask";
    public static final String SEEKERS_SWORD_ID = "had_seeker_sword";
    public static final String INK_SPLASH_ITEM_ID = "had_seeker_ink_splash";
    public static final String LIGHTNING_FREEZE_ITEM_ID = "had_seeker_lightning_freeze";
    public static final String GLOWING_COMPASS_ITEM_ID = "had_seeker_glowing_compass";
    public static final String CURSE_SPELL_ITEM_ID = "had_seeker_curse_spell";
    public static final String BLOCK_RANDOMIZER_ITEM_ID = "had_seeker_block_randomizer";
    public static final String CHAIN_PULL_ITEM_ID = "had_seeker_chain_pull";

    private static final Map<UUID, Long> grapplingHookCooldowns = new HashMap<>();
    private static final Map<UUID, Long> seekerCurseActiveUntil = new HashMap<>();
    private static final Map<UUID, Long> hiderCursedUntil = new HashMap<>();
    private static final Map<UUID, ItemStack> inkHelmetBackup = new HashMap<>();

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
        plugin.getCustomItemManager().registerItem(new CustomItemBuilder(createSeekerSword(), SEEKERS_SWORD_ID)
                .withCraftPrevention(true)
                .withDropPrevention(true)
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

    public static void resetTracking() {
        grapplingHookCooldowns.clear();
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

                if (!seeker.isOnline() || (ticks > 5 && seeker.isOnGround()) || ticks > 30) {

                    if (seeker.getLocation().distance(hookLoc) < 3) {
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

    private static void setOnCooldown(UUID playerId) {
        grapplingHookCooldowns.put(playerId, System.currentTimeMillis());
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
        int radius = plugin.getSettingRegistry().get("seeker-items.ink-splash.radius", 5);
        int duration = plugin.getSettingRegistry().get("seeker-items.ink-splash.duration", 5);

        for (Player hider : Bukkit.getOnlinePlayers()) {
            if (!HideAndSeek.getDataController().getHiders().contains(hider.getUniqueId())) continue;
            if (hider.getLocation().distance(seeker.getLocation()) > radius) continue;

            ItemStack previous = hider.getInventory().getHelmet();
            inkHelmetBackup.put(hider.getUniqueId(), previous);
            HiderItems.applyMask(hider, plugin);
            HiderEquipmentChangeListener.hideHelmet(hider);
            BukkitTask task = Bukkit.getScheduler().runTaskTimer(
                    plugin,
                    () -> {
                        HiderEquipmentChangeListener.hideHelmet(hider);
                    },
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
            final Player finalNearest = nearest;
            UUID hiderId = finalNearest.getUniqueId();

            var gameModeResult = plugin.getSettingService().getSetting("game.gametype");
            Object gameModeObj = gameModeResult.isSuccess() ? gameModeResult.getValue() : null;
            boolean isBlockMode = gameModeObj != null && gameModeObj.toString().equals("BLOCK");


            HideAndSeek.getDataController().setGlowing(hiderId, true);

            if (isBlockMode) {

                BlockDisplay display = HideAndSeek.getDataController().getBlockDisplay(hiderId);
                if (display != null && display.isValid()) {
                    display.setGlowing(true);
                    Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
                    Team hiderTeam = scoreboard.getEntryTeam(finalNearest.getName());
                    display.setGlowColorOverride(chatColorToBukkitColor(hiderTeam.getColor()));
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
                            Team hiderTeam = scoreboard.getEntryTeam(finalNearest.getName());
                            bd.setGlowColorOverride(chatColorToBukkitColor(hiderTeam.getColor()));
                            bd.getPersistentDataContainer().set(new NamespacedKey(plugin, "temp_glow"), PersistentDataType.STRING, hiderId.toString());
                        });


                        HideAndSeek.getDataController().setBlockDisplay(hiderId, tempDisplay);
                    }
                }
            } else {

                finalNearest.setGlowing(true);
            }


            Bukkit.getScheduler().runTaskLater(plugin, () -> {
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
                    finalNearest.setGlowing(false);
                }
            }, duration * 20L);

            seeker.sendMessage(Component.text(finalNearest.getName() + " is now glowing!", NamedTextColor.GOLD));
        } else {
            seeker.sendMessage(Component.text("No hiders found nearby!", NamedTextColor.RED));
        }
    }

    private static void activateCurseSpell(Player seeker, HideAndSeek plugin) {
        int duration = plugin.getSettingRegistry().get("seeker-items.curse-spell.active-duration", 10);
        long until = System.currentTimeMillis() + (duration * 1000L);
        seekerCurseActiveUntil.put(seeker.getUniqueId(), until);

        ItemStack sword = seeker.getInventory().getItemInMainHand();
        if (sword != null) {
            ItemMeta meta = sword.getItemMeta();
            if (meta != null) {
                meta.addEnchant(Enchantment.SHARPNESS, 1, true);
                
                sword.setItemMeta(meta);
            }
        }

        seeker.sendMessage(Component.text("Curse spell activated! (" + duration + "s)", NamedTextColor.DARK_PURPLE));

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            seekerCurseActiveUntil.remove(seeker.getUniqueId());
            ItemStack s = seeker.getInventory().getItemInMainHand();
            if (s != null) {
                ItemMeta m = s.getItemMeta();
                if (m != null) {
                    m.removeEnchant(Enchantment.SHARPNESS);
                    s.setItemMeta(m);
                }
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

        int slownessDuration = plugin.getSettingRegistry().get("seeker-items.chain-pull.slowness-duration", 3);
        int pullTicks = 8;

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!finalTarget.isOnline() || !finalTarget.getWorld().equals(seeker.getWorld())) {
                    cancel();
                    return;
                }

                
                drawChainParticles(seeker, finalTarget);

                Location current = finalTarget.getLocation();
                Vector toTarget = targetLocation.toVector().subtract(current.toVector());
                double distance = toTarget.length();

                if (distance < 0.6 || ticks >= pullTicks) {
                    finalTarget.teleport(targetLocation);
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

    private static void removeChainDisplays(List<BlockDisplay> displays) {
        for (BlockDisplay d : displays) {
            if (d != null && d.isValid()) d.remove();
        }
        displays.clear();
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

    private static Color chatColorToBukkitColor(ChatColor chatColor) {
        return switch (chatColor) {
            case BLACK -> Color.fromRGB(0, 0, 0);
            case DARK_BLUE -> Color.fromRGB(0, 0, 170);
            case DARK_GREEN -> Color.fromRGB(0, 170, 0);
            case DARK_AQUA -> Color.fromRGB(0, 170, 170);
            case DARK_RED -> Color.fromRGB(170, 0, 0);
            case DARK_PURPLE -> Color.fromRGB(170, 0, 170);
            case GOLD -> Color.fromRGB(255, 170, 0);
            case GRAY -> Color.fromRGB(170, 170, 170);
            case DARK_GRAY -> Color.fromRGB(85, 85, 85);
            case BLUE -> Color.fromRGB(85, 85, 255);
            case GREEN -> Color.fromRGB(85, 255, 85);
            case AQUA -> Color.fromRGB(85, 255, 255);
            case RED -> Color.fromRGB(255, 85, 85);
            case LIGHT_PURPLE -> Color.fromRGB(255, 85, 255);
            case YELLOW -> Color.fromRGB(255, 255, 85);
            case WHITE -> Color.fromRGB(255, 255, 255);
            default -> Color.fromRGB(255, 255, 255);
        };
    }
}

