package de.thecoolcraft11.hideAndSeek.items;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.block.BlockAppearanceConfig;
import de.thecoolcraft11.hideAndSeek.gui.AppearanceGUI;
import de.thecoolcraft11.hideAndSeek.gui.BlockSelectorGUI;
import de.thecoolcraft11.hideAndSeek.listener.player.HiderEquipmentChangeListener;
import de.thecoolcraft11.hideAndSeek.model.LoadoutItemType;
import de.thecoolcraft11.hideAndSeek.model.SpeedBoostType;
import de.thecoolcraft11.hideAndSeek.util.points.PointAction;
import de.thecoolcraft11.minigameframework.items.CustomItemBuilder;
import de.thecoolcraft11.minigameframework.items.ItemActionType;
import de.thecoolcraft11.minigameframework.items.ItemInteractionContext;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Consumable;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.type.Candle;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;

public final class HiderItems {
    public static final String SOUND_ITEM_ID = "has_hider_sound";
    public static final String EXPLOSION_ITEM_ID = "has_hider_explosion";
    public static final String RANDOM_BLOCK_ITEM_ID = "has_hider_random_block";
    public static final String SPEED_BOOST_ITEM_ID = "has_hider_speed_boost";
    public static final String TRACKER_CROSSBOW_ITEM_ID = "has_hider_crossbow";
    public static final String APPEARANCE_ITEM_ID = "has_hider_appearance";
    public static final String BLOCK_SELECTOR_ITEM_ID = "has_hider_block_selector";
    public static final String KNOCKBACK_STICK_ITEM_ID = "has_hider_knockback_stick";
    public static final String BLOCK_SWAP_ITEM_ID = "has_hider_block_swap";
    public static final String BIG_FIRECRACKER_ITEM_ID = "has_hider_big_firecracker";
    public static final String FIREWORK_ROCKET_ITEM_ID = "has_hider_firework_rocket";
    public static final String MEDKIT_ITEM_ID = "has_hider_medkit";
    public static final String TOTEM_ITEM_ID = "has_hider_totem";
    public static final String INK_FACE_ID = "has_hider_ink_face";
    public static final String INVISIBILITY_CLOAK_ITEM_ID = "has_hider_invisibility_cloak";
    public static final String SLOWNESS_BALL_ITEM_ID = "has_hider_slowness_ball";
    public static final String SMOKE_BOMB_ITEM_ID = "has_hider_smoke_bomb";

    private static final Map<UUID, Integer> speedLevels = new HashMap<>();
    private static final Map<UUID, Integer> knockbackLevels = new HashMap<>();
    private static final Map<UUID, Integer> trackerHits = new HashMap<>();
    private static final Map<UUID, Long> totemActiveUntil = new HashMap<>();


    private HiderItems() {
    }

    public static void registerItems(HideAndSeek plugin) {
        registerCooldownItems(plugin);

        BlockSelectorGUI blockSelectorGUI = plugin.getBlockSelectorGUI();
        plugin.getCustomItemManager().registerItem(new CustomItemBuilder(createBlockSelectorItem(), BLOCK_SELECTOR_ITEM_ID)
                .withAction(ItemActionType.RIGHT_CLICK_AIR, context -> openBlockSelectorUnhidden(context.getPlayer(), blockSelectorGUI))
                .withAction(ItemActionType.RIGHT_CLICK_BLOCK, context -> openBlockSelectorUnhidden(context.getPlayer(), blockSelectorGUI)).
                withAction(ItemActionType.SHIFT_RIGHT_CLICK_AIR, context -> openBlockSelectorUnhidden(context.getPlayer(), blockSelectorGUI))
                .withAction(ItemActionType.SHIFT_RIGHT_CLICK_BLOCK, context -> openBlockSelectorUnhidden(context.getPlayer(), blockSelectorGUI))
                .withDescription("Choose your block")
                .withDropPrevention(true)
                .withCraftPrevention(true)
                .allowOffHand(false)
                .allowArmor(false)
                .cancelDefaultAction(true)
                .build());


        AppearanceGUI appearanceGUI = new AppearanceGUI(plugin, blockSelectorGUI);
        plugin.getCustomItemManager().registerItem(new CustomItemBuilder(createAppearanceItem(), APPEARANCE_ITEM_ID)
                .withAction(ItemActionType.RIGHT_CLICK_AIR, context -> openAppearanceUnhidden(context.getPlayer(), appearanceGUI))
                .withAction(ItemActionType.RIGHT_CLICK_BLOCK, context -> openAppearanceUnhidden(context.getPlayer(), appearanceGUI))
                .withAction(ItemActionType.SHIFT_RIGHT_CLICK_AIR, context -> openAppearanceUnhidden(context.getPlayer(), appearanceGUI))
                .withAction(ItemActionType.SHIFT_RIGHT_CLICK_BLOCK, context -> openAppearanceUnhidden(context.getPlayer(), appearanceGUI))
                .withDescription("Customize your appearance")
                .withDropPrevention(true)
                .withCraftPrevention(true)
                .allowOffHand(false)
                .allowArmor(false)
                .cancelDefaultAction(true)
                .build());

        registerInkFace(plugin);
    }

    public static void reregisterCooldownItems(HideAndSeek plugin) {
        unregisterCooldownItems(plugin);
        registerCooldownItems(plugin);
    }

    private static void unregisterCooldownItems(HideAndSeek plugin) {
        plugin.getCustomItemManager().unregisterItem(SOUND_ITEM_ID);
        plugin.getCustomItemManager().unregisterItem(EXPLOSION_ITEM_ID);
        plugin.getCustomItemManager().unregisterItem(RANDOM_BLOCK_ITEM_ID);
        plugin.getCustomItemManager().unregisterItem(TRACKER_CROSSBOW_ITEM_ID);
        plugin.getCustomItemManager().unregisterItem(BLOCK_SWAP_ITEM_ID);
        plugin.getCustomItemManager().unregisterItem(BIG_FIRECRACKER_ITEM_ID);
        plugin.getCustomItemManager().unregisterItem(FIREWORK_ROCKET_ITEM_ID);
        plugin.getCustomItemManager().unregisterItem(MEDKIT_ITEM_ID);
        plugin.getCustomItemManager().unregisterItem(INVISIBILITY_CLOAK_ITEM_ID);
        plugin.getCustomItemManager().unregisterItem(SLOWNESS_BALL_ITEM_ID);
        plugin.getCustomItemManager().unregisterItem(SMOKE_BOMB_ITEM_ID);

        for (int level = 0; level <= 5; level++) {
            plugin.getCustomItemManager().unregisterItem(SPEED_BOOST_ITEM_ID + "_" + level);
        }

        for (int level = 1; level <= 5; level++) {
            plugin.getCustomItemManager().unregisterItem(KNOCKBACK_STICK_ITEM_ID + "_" + level);
        }
    }

    private static void registerCooldownItems(HideAndSeek plugin) {
        int soundCooldown = plugin.getSettingRegistry().get("hider-items.sound.cooldown", 4);
        int explosionCooldown = plugin.getSettingRegistry().get("hider-items.explosion.cooldown", 8);
        int randomBlockCooldown = plugin.getSettingRegistry().get("hider-items.random-block.cooldown", 3);
        int crossbowCooldown = plugin.getSettingRegistry().get("hider-items.crossbow.cooldown", 5);

        plugin.getCustomItemManager().registerItem(new CustomItemBuilder(createSoundItem(), SOUND_ITEM_ID)
                .withAction(ItemActionType.RIGHT_CLICK_BLOCK, context -> playSoundForAll(context.getLocation(), plugin, context.getPlayer()))
                .withAction(ItemActionType.SHIFT_RIGHT_CLICK_BLOCK, context -> playSoundForAll(context.getLocation(), plugin, context.getPlayer()))
                .withVanillaCooldown(soundCooldown * 20)
                .withCustomCooldown(soundCooldown * 1000L)
                .withVanillaCooldownDisplay(true)
                .withDescription("Play a sound for everyone")
                .withDropPrevention(true)
                .withCraftPrevention(true)
                .allowOffHand(false)
                .allowArmor(false)
                .cancelDefaultAction(true)
                .build());

        plugin.getCustomItemManager().registerItem(new CustomItemBuilder(createExplosionItem(), EXPLOSION_ITEM_ID)
                .withAction(ItemActionType.RIGHT_CLICK_BLOCK, context -> spawnExplosionForAll(context, plugin))
                .withAction(ItemActionType.SHIFT_RIGHT_CLICK_BLOCK, context -> spawnExplosionForAll(context, plugin))
                .withVanillaCooldown(explosionCooldown * 20)
                .withCustomCooldown(explosionCooldown * 1000L)
                .withVanillaCooldownDisplay(true)
                .withDescription("Play an explosion particle for everyone")
                .withDropPrevention(true)
                .withCraftPrevention(true)
                .allowOffHand(false)
                .allowArmor(false)
                .cancelDefaultAction(true)
                .build());


        int randomUses = plugin.getSettingRegistry().get("hider-items.random-block.uses", 5);

        plugin.getCustomItemManager().registerItem(new CustomItemBuilder(createRandomBlockItem(randomUses), RANDOM_BLOCK_ITEM_ID)
                .withAction(ItemActionType.RIGHT_CLICK_AIR, context -> randomizeBlockWithContext(context, plugin))
                .withAction(ItemActionType.RIGHT_CLICK_BLOCK, context -> randomizeBlockWithContext(context, plugin))
                .withAction(ItemActionType.SHIFT_RIGHT_CLICK_AIR, context -> randomizeBlockWithContext(context, plugin))
                .withAction(ItemActionType.SHIFT_RIGHT_CLICK_BLOCK, context -> randomizeBlockWithContext(context, plugin))
                .withMaxPlayerUses(randomUses)
                .withVanillaCooldown(randomBlockCooldown * 20)
                .withCustomCooldown(randomBlockCooldown * 1000L)
                .withVanillaCooldownDisplay(true)
                .withDescription("Transform into a random allowed block")
                .withDropPrevention(true)
                .withCraftPrevention(true)
                .allowOffHand(false)
                .allowArmor(false)
                .cancelDefaultAction(true)
                .withUsesExhaustedHandler((context, isTeamLimit) -> context.getPlayer().sendMessage(Component.text("You ran out of random block uses!", NamedTextColor.RED)))
                .withAppearanceProvider((player, item, context) -> {
                    ItemStack itemStack = item.getItemStack();
                    ItemMeta meta = itemStack.getItemMeta();
                    meta.displayName(Component.text("Random Block", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD)
                            .append(Component.space())
                            .append(Component.text("(" + context.getPlayerRemainingUses() + "/" + context.getMaxPlayerUses() + ")", NamedTextColor.GRAY)
                                    .decoration(TextDecoration.ITALIC, false)));
                    itemStack.setItemMeta(meta);
                    return itemStack;
                })
                .build());

        registerSpeedBoostItems(plugin);


        plugin.getCustomItemManager().registerItem(new CustomItemBuilder(createTrackerCrossbowItem(), TRACKER_CROSSBOW_ITEM_ID)
                .withAction(ItemActionType.SHOOT, context -> {
                })
                .withDescription("Hit seekers to upgrade your speed boost")
                .withDropPrevention(true)
                .withCraftPrevention(true)
                .withVanillaCooldown(crossbowCooldown * 20)
                .withCustomCooldown(crossbowCooldown * 1000L)
                .withVanillaCooldownDisplay(true)
                .allowOffHand(false)
                .allowArmor(false)
                .cancelDefaultAction(false)
                .build());


        registerKnockbackStickItems(plugin);

        int blockSwapCooldown = plugin.getSettingRegistry().get("hider-items.block-swap.cooldown", 15);
        plugin.getCustomItemManager().registerItem(new CustomItemBuilder(createBlockSwapItem(), BLOCK_SWAP_ITEM_ID)
                .withAction(ItemActionType.RIGHT_CLICK_AIR, context -> blockSwap(context, plugin))
                .withAction(ItemActionType.RIGHT_CLICK_BLOCK, context -> blockSwap(context, plugin))
                .withDescription("Swap blocks with another hider")
                .withDropPrevention(true)
                .withCraftPrevention(true)
                .withVanillaCooldown(blockSwapCooldown * 20)
                .withCustomCooldown(blockSwapCooldown * 1000L)
                .withVanillaCooldownDisplay(true)
                .allowOffHand(false)
                .allowArmor(false)
                .cancelDefaultAction(true)
                .build());

        int bigFirecrackerCooldown = plugin.getSettingRegistry().get("hider-items.big-firecracker.cooldown", 12);
        plugin.getCustomItemManager().registerItem(new CustomItemBuilder(createBigFirecrackerItem(), BIG_FIRECRACKER_ITEM_ID)
                .withAction(ItemActionType.RIGHT_CLICK_BLOCK, context -> spawnBigFirecracker(context, plugin))
                .withAction(ItemActionType.SHIFT_RIGHT_CLICK_BLOCK, context -> spawnBigFirecracker(context, plugin))
                .withDescription("Place a big firecracker")
                .withDropPrevention(true)
                .withCraftPrevention(true)
                .withVanillaCooldown(bigFirecrackerCooldown * 20)
                .withCustomCooldown(bigFirecrackerCooldown * 1000L)
                .withVanillaCooldownDisplay(true)
                .allowOffHand(false)
                .allowArmor(false)
                .cancelDefaultAction(true)
                .build());

        int fireworkCooldown = plugin.getSettingRegistry().get("hider-items.firework-rocket.cooldown", 10);
        plugin.getCustomItemManager().registerItem(new CustomItemBuilder(createFireworkRocketItem(), FIREWORK_ROCKET_ITEM_ID)
                .withAction(ItemActionType.RIGHT_CLICK_AIR, context -> launchFirework(context.getPlayer(), plugin))
                .withAction(ItemActionType.RIGHT_CLICK_BLOCK, context -> launchFirework(context.getPlayer(), plugin))
                .withDescription("Launch a firework into the sky")
                .withDropPrevention(true)
                .withCraftPrevention(true)
                .withVanillaCooldown(fireworkCooldown * 20)
                .withCustomCooldown(fireworkCooldown * 1000L)
                .withVanillaCooldownDisplay(true)
                .allowOffHand(false)
                .allowArmor(false)
                .cancelDefaultAction(true)
                .build());

        int medkitCooldown = plugin.getSettingRegistry().get("hider-items.medkit.cooldown", 30);
        plugin.getCustomItemManager().registerItem(new CustomItemBuilder(createMedkitItem(plugin), MEDKIT_ITEM_ID)
                .withAction(ItemActionType.CONSUME_START, context -> startMedkitConsume(context, plugin))
                .withAction(ItemActionType.CONSUME_FINISH, context -> useMedkit(context, plugin))
                .withCustomCooldown(medkitCooldown * 1000L)
                .withVanillaCooldown(medkitCooldown * 20)
                .withVanillaCooldownDisplay(true)
                .withConsumptionPrevention(true)
                .withDropPrevention(true)
                .withCraftPrevention(true)
                .allowOffHand(false)
                .allowArmor(false)
                .build());

        int totemUses = plugin.getSettingRegistry().get("hider-items.totem.max-uses", 1);
        plugin.getCustomItemManager().registerItem(new CustomItemBuilder(createTotemItem(), TOTEM_ITEM_ID)
                .withAction(ItemActionType.RIGHT_CLICK_AIR, context -> activateTotem(context.getPlayer(), plugin))
                .withAction(ItemActionType.RIGHT_CLICK_BLOCK, context -> activateTotem(context.getPlayer(), plugin))
                .withDescription("Activate revive mode (one-time use)")
                .withDropPrevention(true)
                .withCraftPrevention(true)
                .withMaxPlayerUses(totemUses)
                .allowOffHand(false)
                .allowArmor(false)
                .cancelDefaultAction(true)
                .withUsesExhaustedHandler((context, isTeamLimit) -> context.getPlayer().sendMessage(Component.text("You've already used your totem!", NamedTextColor.RED)))
                .build());

        int invisibilityCloakCooldown = plugin.getSettingRegistry().get("hider-items.invisibility-cloak.cooldown", 20);
        plugin.getCustomItemManager().registerItem(new CustomItemBuilder(createInvisibilityCloakItem(), INVISIBILITY_CLOAK_ITEM_ID)
                .withAction(ItemActionType.RIGHT_CLICK_AIR, context -> useInvisibilityCloak(context.getPlayer(), plugin))
                .withAction(ItemActionType.RIGHT_CLICK_BLOCK, context -> useInvisibilityCloak(context.getPlayer(), plugin))
                .withDescription("Make yourself invisible for a short time")
                .withDropPrevention(true)
                .withCraftPrevention(true)
                .withVanillaCooldown(invisibilityCloakCooldown * 20)
                .withCustomCooldown(invisibilityCloakCooldown * 1000L)
                .withVanillaCooldownDisplay(true)
                .allowOffHand(false)
                .allowArmor(false)
                .cancelDefaultAction(true)
                .build());

        int slownessBallCooldown = plugin.getSettingRegistry().get("hider-items.slowness-ball.cooldown", 10);
        plugin.getCustomItemManager().registerItem(new CustomItemBuilder(createSlownessBallItem(), SLOWNESS_BALL_ITEM_ID)
                .withAction(ItemActionType.RIGHT_CLICK_AIR, context -> throwSlownessBall(context, plugin))
                .withAction(ItemActionType.RIGHT_CLICK_BLOCK, context -> throwSlownessBall(context, plugin))
                .withDescription("Throw a snowball that slows seekers")
                .withDropPrevention(true)
                .withCraftPrevention(true)
                .withVanillaCooldown(slownessBallCooldown * 20)
                .withCustomCooldown(slownessBallCooldown * 1000L)
                .withVanillaCooldownDisplay(true)
                .allowOffHand(false)
                .allowArmor(false)
                .cancelDefaultAction(true)
                .build());

        int smokeBombCooldown = plugin.getSettingRegistry().get("hider-items.smoke-bomb.cooldown", 15);
        plugin.getCustomItemManager().registerItem(new CustomItemBuilder(createSmokeBombItem(), SMOKE_BOMB_ITEM_ID)
                .withAction(ItemActionType.RIGHT_CLICK_AIR, context -> throwSmokeBomb(context, plugin))
                .withAction(ItemActionType.RIGHT_CLICK_BLOCK, context -> throwSmokeBomb(context, plugin))
                .withDescription("Throw a smoke bomb to create cover")
                .withDropPrevention(true)
                .withCraftPrevention(true)
                .withVanillaCooldown(smokeBombCooldown * 20)
                .withCustomCooldown(smokeBombCooldown * 1000L)
                .withVanillaCooldownDisplay(true)
                .allowOffHand(false)
                .allowArmor(false)
                .cancelDefaultAction(true)
                .build());
    }

    private static void openBlockSelectorUnhidden(Player player, BlockSelectorGUI blockSelectorGUI) {
        if (HideAndSeek.getDataController().isHidden(player.getUniqueId())) return;
        blockSelectorGUI.open(player);
    }

    private static void openAppearanceUnhidden(Player player, AppearanceGUI appearanceGUI) {
        if (HideAndSeek.getDataController().isHidden(player.getUniqueId())) return;
        appearanceGUI.open(player);
    }

    private static void registerSpeedBoostItems(HideAndSeek plugin) {
        int speedBoostCooldown = plugin.getSettingRegistry().get("hider-items.speed-boost.cooldown", 10);

        for (int level = 0; level <= 5; level++) {
            String levelId = SPEED_BOOST_ITEM_ID + "_" + level;
            plugin.getCustomItemManager().registerItem(new CustomItemBuilder(createSpeedBoostItem(level), levelId)
                    .withAction(ItemActionType.RIGHT_CLICK_AIR, context -> speedBoost(context.getPlayer(), plugin))
                    .withAction(ItemActionType.RIGHT_CLICK_BLOCK, context -> speedBoost(context.getPlayer(), plugin))
                    .withAction(ItemActionType.SHIFT_RIGHT_CLICK_AIR, context -> speedBoost(context.getPlayer(), plugin))
                    .withAction(ItemActionType.SHIFT_RIGHT_CLICK_BLOCK, context -> speedBoost(context.getPlayer(), plugin))
                    .withVanillaCooldown(speedBoostCooldown * 20)
                    .withCustomCooldown(speedBoostCooldown * 1000L)
                    .withVanillaCooldownDisplay(true)
                    .withDescription("Quick speed boost")
                    .withDropPrevention(true)
                    .withCraftPrevention(true)
                    .allowOffHand(false)
                    .allowArmor(false)
                    .cancelDefaultAction(true)
                    .build());
        }
    }

    private static void registerKnockbackStickItems(HideAndSeek plugin) {
        int knockbackCooldown = plugin.getSettingRegistry().get("hider-items.knockback-stick.cooldown", 5);

        for (int level = 1; level <= 5; level++) {
            String levelId = KNOCKBACK_STICK_ITEM_ID + "_" + level;
            plugin.getCustomItemManager().registerItem(new CustomItemBuilder(createKnockbackStickItem(level), levelId)
                    .withAction(ItemActionType.LEFT_CLICK_ENTITY, HiderItems::knockbackHit)
                    .withDescription("Knock away seekers")
                    .withDropPrevention(true)
                    .withCraftPrevention(true)
                    .withVanillaCooldown(knockbackCooldown * 20)
                    .withCustomCooldown(knockbackCooldown * 1000L)
                    .withVanillaCooldownDisplay(true)
                    .allowOffHand(false)
                    .allowArmor(false)
                    .cancelDefaultAction(true)
                    .build());
        }
    }

    private static void knockbackHit(ItemInteractionContext context) {
        Player attacker = context.getPlayer();
        if (context.getEntity() instanceof Player victim) {
            Location victimLoc = victim.getLocation().add(0, 1, 0);
            Vector fromAttacker = victimLoc.toVector().subtract(attacker.getLocation().add(0, 1, 0).toVector());
            if (fromAttacker.lengthSquared() < 0.01) {
                return;
            }


            Vector backDir = fromAttacker.normalize().multiply(-0.25);
            for (int i = 0; i < 5; i++) {
                Location point = victimLoc.clone().add(backDir.clone().multiply(i));
                victim.getWorld().spawnParticle(org.bukkit.Particle.CLOUD, point, 2, 0.05, 0.05, 0.05, 0.02);
            }
        }
    }

    public static void registerInkFace(HideAndSeek plugin) {
        plugin.getCustomItemManager().registerItem(new CustomItemBuilder(createInkFace(), INK_FACE_ID)
                .withDropPrevention(true)
                .withCraftPrevention(true)
                .allowOffHand(false)
                .allowArmor(true)
                .withInventoryMovePrevention(true)
                .cancelDefaultAction(false)
                .build());
    }

    private static ItemStack createInkFace() {
        ItemStack item = new ItemStack(Material.CARVED_PUMPKIN);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.displayName(Component.text("Ink", NamedTextColor.DARK_BLUE, TextDecoration.BOLD)
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

    public static void applyMask(Player player, HideAndSeek plugin) {
        player.getInventory().setHelmet(plugin.getCustomItemManager().getIdentifiedItemStack(INK_FACE_ID, player));
    }

    public static void giveItems(Player player, HideAndSeek plugin, boolean isHiding) {
        removeItems(player);

        var gameModeResult = plugin.getSettingService().getSetting("game.gametype");
        Object gameModeObj = gameModeResult.isSuccess() ? gameModeResult.getValue() : null;

        if (!isHiding) {

            giveLoadoutItems(player, plugin);
            ensureArrow(player);
        }


        if (gameModeObj != null && gameModeObj.toString().equals("BLOCK")) {
            int appearanceSlot = isHiding ? 7 : 8;
            if (hasCustomizableBlock(player, plugin)) {

                player.getInventory().setItem(appearanceSlot, plugin.getCustomItemManager().getIdentifiedItemStack(APPEARANCE_ITEM_ID, player));
            } else {

                player.getInventory().setItem(appearanceSlot, new ItemStack(Material.AIR));
            }
        }

        if (isHiding && (gameModeObj != null && gameModeObj.toString().equals("BLOCK")))
            player.getInventory().setItem(8, plugin.getCustomItemManager().getIdentifiedItemStack(BLOCK_SELECTOR_ITEM_ID, player));
        HiderEquipmentChangeListener.hideHandItem(player, EquipmentSlot.HAND);
        HiderEquipmentChangeListener.hideHandItem(player, EquipmentSlot.OFF_HAND);
    }

    public static void giveLoadoutItems(Player player, HideAndSeek plugin) {
        var loadout = plugin.getLoadoutManager().getLoadout(player.getUniqueId());


        int slot = 0;

        Set<LoadoutItemType> itemsToGive = loadout.getHiderItems();


        if (itemsToGive.isEmpty()) {
            plugin.getLogger().info("No loadout selected for " + player.getName() + ", using defaults");
            itemsToGive = Set.of(
                    LoadoutItemType.CAT_SOUND,
                    LoadoutItemType.FIRECRACKER,
                    LoadoutItemType.SPEED_BOOST
            );
        } else {
            plugin.getLogger().info(player.getName() + " has custom loadout with " + itemsToGive.size() + " items");
        }


        boolean hasValidItems = false;
        for (LoadoutItemType itemType : itemsToGive) {
            String itemId = itemType.getItemId();
            if (itemType == LoadoutItemType.SPEED_BOOST) {
                itemId = SPEED_BOOST_ITEM_ID + "_" + getSpeedLevel(player.getUniqueId());
            } else if (itemType == LoadoutItemType.KNOCKBACK_STICK) {
                itemId = KNOCKBACK_STICK_ITEM_ID + "_" + getKnockbackLevel(player.getUniqueId());
            }
            if (plugin.getCustomItemManager().getIdentifiedItemStack(itemId, player) != null) {
                hasValidItems = true;
                break;
            }
        }


        if (!hasValidItems && !itemsToGive.isEmpty()) {
            plugin.getLogger().warning("All selected items for " + player.getName() + " are not implemented yet! Using default loadout instead.");
            player.sendMessage(Component.text("Some items you selected are not implemented yet. Using default items instead.", NamedTextColor.YELLOW));
            itemsToGive = Set.of(
                    LoadoutItemType.CAT_SOUND,
                    LoadoutItemType.FIRECRACKER,
                    LoadoutItemType.SPEED_BOOST
            );
        }

        for (LoadoutItemType itemType : itemsToGive) {
            String itemId = itemType.getItemId();


            if (itemType == LoadoutItemType.SPEED_BOOST) {
                itemId = SPEED_BOOST_ITEM_ID + "_" + getSpeedLevel(player.getUniqueId());
            } else if (itemType == LoadoutItemType.KNOCKBACK_STICK) {
                itemId = KNOCKBACK_STICK_ITEM_ID + "_" + getKnockbackLevel(player.getUniqueId());
            }

            plugin.getLogger().info("Giving " + player.getName() + " item: " + itemType.name() + " (ID: " + itemId + ") in slot " + slot);
            ItemStack item = plugin.getCustomItemManager().getIdentifiedItemStack(itemId, player);
            if (item != null) {
                player.getInventory().setItem(slot++, item);
                plugin.getLogger().info("  Item placed successfully");

                plugin.getCustomItemManager().resetPlayerUses(RANDOM_BLOCK_ITEM_ID, player.getUniqueId());
                plugin.getCustomItemManager().resetPlayerUses(TOTEM_ITEM_ID, player.getUniqueId());
            } else {
                plugin.getLogger().warning("  Item is NULL! Item not registered: " + itemId + " (skipping)");
            }
        }

        plugin.getLogger().info("Finished giving loadout items to " + player.getName() + " (" + (slot) + " items placed)");
    }

    private static boolean hasCustomizableBlock(Player player, HideAndSeek plugin) {
        Material chosenBlock = HideAndSeek.getDataController().getChosenBlock(player.getUniqueId());

        String currentMap = HideAndSeek.getDataController().getCurrentMapName();
        if (currentMap == null || currentMap.isEmpty()) {
            return false;
        }

        List<String> allowedBlocks = plugin.getMapManager().getAllowedBlocksForMap(currentMap);
        if (allowedBlocks == null || allowedBlocks.isEmpty()) {
            return false;
        }


        if (chosenBlock == null) {
            return mapHasCustomizableBlocks(allowedBlocks);
        }


        BlockAppearanceConfig config = plugin.getBlockSelectorGUI().resolveConfigForMaterial(allowedBlocks, chosenBlock);
        if (config == null) {
            return false;
        }

        return isConfigCustomizable(config);
    }

    private static boolean mapHasCustomizableBlocks(List<String> allowedBlocks) {
        for (String pattern : allowedBlocks) {
            BlockAppearanceConfig config = BlockAppearanceConfig.parse(pattern);
            if (config != null && isConfigCustomizable(config)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isConfigCustomizable(BlockAppearanceConfig config) {
        return config.isAllowAllVariants() ||
                config.hasVariantGroup() ||
                config.isAllowAllBlockStates() ||
                !config.getAllowedProperties().isEmpty() ||
                !config.getAllowedStates().isEmpty();
    }

    public static void updateAppearanceItem(Player player, HideAndSeek plugin) {
        String currentPhase = plugin.getStateManager().getCurrentPhaseId();
        boolean isHiding = "hiding".equals(currentPhase);
        int appearanceSlot = isHiding ? 7 : 8;

        if (hasCustomizableBlock(player, plugin)) {
            player.getInventory().setItem(appearanceSlot, plugin.getCustomItemManager().getIdentifiedItemStack(APPEARANCE_ITEM_ID, player));
        } else {
            player.getInventory().setItem(appearanceSlot, new ItemStack(Material.AIR));
        }
    }


    public static void ensureArrow(Player player) {
        if (player == null) {
            return;
        }
        if (!player.getInventory().contains(Material.ARROW)) {
            player.getInventory().setItem(9, new ItemStack(Material.ARROW, 1));
        }
    }

    public static void removeItems(Player player) {
        player.getInventory().remove(Material.CAT_SPAWN_EGG);
        player.getInventory().remove(Material.RED_CANDLE);
        player.getInventory().remove(Material.BLAZE_POWDER);
        player.getInventory().remove(Material.COMMAND_BLOCK);
        player.getInventory().remove(Material.COMPARATOR);
        player.getInventory().remove(Material.WOODEN_HOE);
        player.getInventory().remove(Material.STONE_HOE);
        player.getInventory().remove(Material.IRON_HOE);
        player.getInventory().remove(Material.GOLDEN_HOE);
        player.getInventory().remove(Material.DIAMOND_HOE);
        player.getInventory().remove(Material.NETHERITE_HOE);
        player.getInventory().remove(Material.CROSSBOW);
        player.getInventory().remove(Material.ARROW);
    }

    public static void removeFromAllPlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            removeItems(player);
        }
    }

    public static void onTrackerHit(Player hider, HideAndSeek plugin) {
        if (hider == null) {
            return;
        }

        int hitPoints = plugin.getPointService().award(hider.getUniqueId(), PointAction.HIDER_SHARPSHOOTER);
        hider.sendMessage(Component.text("Crossbow hit! +" + hitPoints + " points", NamedTextColor.GOLD));

        int hits = trackerHits.getOrDefault(hider.getUniqueId(), 0) + 1;
        trackerHits.put(hider.getUniqueId(), hits);

        int hitsPerUpgrade = plugin.getSettingRegistry().get("hider-items.crossbow.hits-per-upgrade", 3);
        if (hits >= hitsPerUpgrade) {
            trackerHits.put(hider.getUniqueId(), 0);
            upgradeLoadoutItems(hider, plugin);
        } else {
            hider.sendMessage(Component.text("Hit seeker! " + hits + "/" + hitsPerUpgrade, NamedTextColor.GREEN));
        }
    }

    private static void upgradeLoadoutItems(Player player, HideAndSeek plugin) {

        var loadout = plugin.getLoadoutManager().getLoadout(player.getUniqueId());
        Set<LoadoutItemType> hiderItems = loadout.getHiderItems();

        boolean hasSpeedBoost = hiderItems.contains(LoadoutItemType.SPEED_BOOST);
        boolean hasKnockbackStick = hiderItems.contains(LoadoutItemType.KNOCKBACK_STICK);


        if (hasSpeedBoost) {
            upgradeSpeedItem(player);
        }


        if (hasKnockbackStick) {
            upgradeKnockbackItem(player);
        }


        if (!hasSpeedBoost && !hasKnockbackStick) {
            player.sendMessage(Component.text("You don't have Speed Boost or Knockback Stick selected!", NamedTextColor.YELLOW));
        }
    }

    private static void randomizeBlock(Player player, HideAndSeek plugin) {
        if (player == null) {
            return;
        }
        if (!HideAndSeek.getDataController().getHiders().contains(player.getUniqueId())) {
            player.sendMessage(Component.text("Only hiders can use this item.", NamedTextColor.RED));
            return;
        }
        if (SeekerItems.isHiderCursed(player.getUniqueId())) {
            player.sendMessage(Component.text("You are cursed and cannot change blocks right now!", NamedTextColor.RED));
            return;
        }

        if (HideAndSeek.getDataController().isHidden(player.getUniqueId())) {
            player.sendMessage(Component.text("You cant transform into a new block while being hidden!", NamedTextColor.RED));
            return;
        }


        player.getInventory().remove(Material.COMPARATOR);

        String currentMap = HideAndSeek.getDataController().getCurrentMapName();
        if (currentMap == null || currentMap.isEmpty()) {
            player.sendMessage(Component.text("No active map found.", NamedTextColor.RED));
            return;
        }

        List<String> allowedBlocks = plugin.getMapManager().getAllowedBlocksForMap(currentMap);
        if (allowedBlocks.isEmpty()) {
            player.sendMessage(Component.text("No allowed blocks configured for this map.", NamedTextColor.RED));
            return;
        }

        List<String> possibleBlocks = allowedBlocks.stream()
                .filter(block -> !block.equalsIgnoreCase(HideAndSeek.getDataController().getChosenBlock(player.getUniqueId()).name()))
                .toList();


        String chosenPattern = possibleBlocks.get(new Random().nextInt(allowedBlocks.size()));


        BlockAppearanceConfig config = BlockAppearanceConfig.parse(chosenPattern);

        if (config == null) {
            player.sendMessage(Component.text("Invalid block pattern in config: " + chosenPattern, NamedTextColor.RED));
            return;
        }

        Material chosenMaterial;


        if (config.getDefaultVariant() != null) {
            try {
                chosenMaterial = Material.valueOf(config.getDefaultVariant());
            } catch (IllegalArgumentException e) {
                player.sendMessage(Component.text("Invalid default variant in config: " + config.getDefaultVariant(), NamedTextColor.RED));
                return;
            }
        } else {

            try {
                chosenMaterial = Material.valueOf(config.getBaseBlockType());
            } catch (IllegalArgumentException e) {
                player.sendMessage(Component.text("Invalid block type in config: " + config.getBaseBlockType(), NamedTextColor.RED));
                return;
            }
        }


        HideAndSeek.getDataController().setChosenBlock(player.getUniqueId(), chosenMaterial);


        BlockData blockData = chosenMaterial.createBlockData();
        HideAndSeek.getDataController().setChosenBlockData(player.getUniqueId(), blockData);


        plugin.getBlockSelectorGUI().setPlayerConfig(player.getUniqueId(), config);


        var display = HideAndSeek.getDataController().getBlockDisplay(player.getUniqueId());
        if (display != null && display.isValid()) {
            display.setBlock(blockData);
            display.setRotation(player.getLocation().getYaw(), 0f);
        }


        updateAppearanceItem(player, plugin);

        player.sendMessage(Component.text("Transformed into ", NamedTextColor.GREEN)
                .append(Component.text(formatName(chosenMaterial.name()), NamedTextColor.GOLD)));
    }

    private static void speedBoost(Player player, HideAndSeek plugin) {
        if (player == null) {
            return;
        }

        int durationSeconds = plugin.getSettingRegistry().get("hider-items.speed-boost.duration", 5);
        Object boostTypeObj = plugin.getSettingRegistry().get("hider-items.speed-boost.type");
        SpeedBoostType boostType = (boostTypeObj instanceof SpeedBoostType) ? (SpeedBoostType) boostTypeObj : SpeedBoostType.SPEED_EFFECT;
        double amplifierBonus = plugin.getSettingRegistry().get("hider-items.speed-boost.amplifier-bonus", 0.2);

        int amplifier = Math.max(0, getSpeedLevel(player.getUniqueId()));

        if (boostType == SpeedBoostType.VELOCITY_BOOST) {

            double boostPower = plugin.getSettingRegistry().get("hider-items.speed-boost.boost-power", 0.5);
            boostPower += (amplifier * amplifierBonus);

            Vector direction = player.getLocation().getDirection().normalize().multiply(boostPower);
            player.setVelocity(player.getVelocity().add(direction));
            player.sendMessage(Component.text("Velocity boost activated! ", NamedTextColor.YELLOW)
                    .append(Component.text("(Level " + (amplifier + 1) + ")", NamedTextColor.GOLD)));


            new BukkitRunnable() {
                @Override
                public void run() {
                    if (!player.isOnline()) {
                        cancel();
                        return;
                    }


                    if (player.isOnGround()) {
                        cancel();
                        return;
                    }

                    Location loc = player.getLocation();
                    player.getWorld().spawnParticle(Particle.CLOUD, loc, 2, 0.1, 0.1, 0.1, 0.05);
                }
            }.runTaskTimer(plugin, 1L, 2L);

        } else {

            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.SPEED,
                    durationSeconds * 20,
                    amplifier,
                    false,
                    true,
                    true
            ));
            player.sendMessage(Component.text("Speed boost activated! ", NamedTextColor.YELLOW)
                    .append(Component.text("(Level " + (amplifier + 1) + ")", NamedTextColor.GOLD)));


            new BukkitRunnable() {
                int ticks = 0;
                final int maxTicks = durationSeconds * 20;

                @Override
                public void run() {
                    if (!player.isOnline() || ticks >= maxTicks) {
                        cancel();
                        return;
                    }

                    Location loc = player.getLocation();

                    if (ticks % 4 == 0) {
                        player.getWorld().spawnParticle(Particle.CLOUD, loc.add(0.5, 0.1, 0.5), 1, 0.15, 0.05, 0.15, 0.02);
                    }

                    ticks++;
                }
            }.runTaskTimer(plugin, 1L, 1L);
        }
    }

    private static void upgradeSpeedItem(Player player) {
        int level = Math.min(5, getSpeedLevel(player.getUniqueId()) + 1);
        speedLevels.put(player.getUniqueId(), level);
        removeSpeedItems(player);
        player.getInventory().addItem(createSpeedBoostItem(level));
        player.sendMessage(Component.text("Speed boost upgraded!", NamedTextColor.GOLD));
    }

    private static void upgradeKnockbackItem(Player player) {
        int level = Math.min(5, getKnockbackLevel(player.getUniqueId()) + 1);
        knockbackLevels.put(player.getUniqueId(), level);
        removeKnockbackItems(player);
        player.getInventory().addItem(createKnockbackStickItem(level));
        player.sendMessage(Component.text("Knockback stick upgraded!", NamedTextColor.GOLD));
    }

    private static int getSpeedLevel(UUID playerId) {
        return speedLevels.getOrDefault(playerId, 0);
    }

    private static int getKnockbackLevel(UUID playerId) {
        return knockbackLevels.getOrDefault(playerId, 1);
    }

    private static void removeSpeedItems(Player player) {
        player.getInventory().remove(Material.WOODEN_HOE);
        player.getInventory().remove(Material.STONE_HOE);
        player.getInventory().remove(Material.IRON_HOE);
        player.getInventory().remove(Material.GOLDEN_HOE);
        player.getInventory().remove(Material.DIAMOND_HOE);
        player.getInventory().remove(Material.NETHERITE_HOE);
    }

    private static void removeKnockbackItems(Player player) {
        player.getInventory().remove(Material.STICK);
    }

    private static void playSoundForAll(Location location, HideAndSeek plugin, Player hider) {
        int tauntPoints = plugin.getPointService().award(hider.getUniqueId(), PointAction.HIDER_TAUNT_SMALL);
        double volume = plugin.getSettingRegistry().get("hider-items.sound.volume", 0.75);
        double pitch = plugin.getSettingRegistry().get("hider-items.sound.pitch", 0.8);

        hider.sendMessage(Component.text("You have used the taunt ", NamedTextColor.GREEN).append(Component.text("\"Cat\"", NamedTextColor.YELLOW)));
        hider.sendMessage(Component.text("+" + tauntPoints + " points", NamedTextColor.GOLD));


        Location particleLoc = hider.getEyeLocation();
        hider.getWorld().spawnParticle(Particle.NOTE, particleLoc, 8, 0.3, 0.3, 0.3, 1.0);
        hider.getWorld().spawnParticle(Particle.HEART, particleLoc, 4, 0.2, 0.2, 0.2, 0);

        for (Player target : Bukkit.getOnlinePlayers()) {
            target.playSound(location, Sound.ENTITY_CAT_AMBIENT, (float) volume, (float) pitch);
        }
    }

    private static void spawnExplosionForAll(ItemInteractionContext context, HideAndSeek plugin) {

        Location location = context.getLocation();
        Player hider = context.getPlayer();

        Block block = location.clone().add(0, 1, 0).getBlock();
        if (!block.getType().isAir()) {
            context.skipCooldown();
            return;
        }

        block.setType(Material.RED_CANDLE);

        Candle candle = (Candle) block.getBlockData();
        candle.setLit(true);
        candle.setCandles(1);
        block.setBlockData(candle);

        var tauntPoints = plugin.getPointService().award(hider.getUniqueId(), PointAction.HIDER_TAUNT_SMALL);
        double volume = plugin.getSettingRegistry().get("hider-items.explosion.volume", 0.65);
        double pitch = plugin.getSettingRegistry().get("hider-items.explosion.pitch", 1.5);
        int smokeParticles = plugin.getSettingRegistry().get("hider-items.explosion.smoke-particles", 3);
        int fuseTime = plugin.getSettingRegistry().get("hider-items.explosion.fuse-time", 40);

        hider.sendMessage(
                Component.text("You have used the taunt ", NamedTextColor.GREEN)
                        .append(Component.text("\"Firecracker\"", NamedTextColor.YELLOW))
        );
        hider.sendMessage(
                Component.text("+" + tauntPoints + " points", NamedTextColor.GOLD)
        );

        int smokeTaskId = Bukkit.getScheduler().runTaskTimer(
                plugin,
                () -> {
                    Location smokeLoc = location.clone().add(0.5, 1.6, 0.5);
                    location.getWorld().spawnParticle(
                            Particle.SMOKE,
                            smokeLoc,
                            smokeParticles,
                            0.05, 0.1, 0.05,
                            0.01
                    );

                    location.getWorld().spawnParticle(
                            Particle.FLAME,
                            smokeLoc.clone().add(0, -0.2, 0),
                            1,
                            0.05, 0.05, 0.05,
                            0.05
                    );
                },
                0L,
                4L
        ).getTaskId();

        Bukkit.getScheduler().runTaskLater(
                plugin,
                () -> {
                    Bukkit.getScheduler().cancelTask(smokeTaskId);

                    block.setType(Material.AIR);

                    Location explosionLoc = location.clone().add(0.5, 1.5, 0.5);
                    for (Player target : Bukkit.getOnlinePlayers()) {
                        target.getWorld().spawnParticle(
                                Particle.EXPLOSION,
                                explosionLoc,
                                1,
                                0, 0, 0, 0
                        );

                        target.getWorld().spawnParticle(
                                Particle.DUST,
                                explosionLoc,
                                15,
                                0.3, 0.3, 0.3,
                                new Particle.DustOptions(Color.fromARGB(255, 255, 100, 0), 1.0f)
                        );
                        target.playSound(
                                location,
                                Sound.ENTITY_GENERIC_EXPLODE,
                                (float) volume,
                                (float) pitch
                        );
                    }
                },
                fuseTime
        );
    }

    private static ItemStack createSoundItem() {
        ItemStack item = new ItemStack(Material.CAT_SPAWN_EGG);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.displayName(Component.text("Cat Sound", NamedTextColor.AQUA, TextDecoration.BOLD)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.text("Right click to play a sound", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false)
            ));
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);
        }

        return item;
    }

    private static ItemStack createExplosionItem() {
        ItemStack item = new ItemStack(Material.RED_CANDLE);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.displayName(Component.text("Firecracker", NamedTextColor.RED, TextDecoration.BOLD)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.text("Right click to place a firecracker that will explode", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false)
            ));
            item.setItemMeta(meta);
        }

        return item;
    }

    private static ItemStack createRandomBlockItem(int maxUses) {
        ItemStack item = new ItemStack(Material.BLAZE_POWDER);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.displayName(Component.text("Random Block", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD).append(Component.space()).append(Component.text("(" + maxUses + "/" + maxUses + ")", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
            meta.lore(List.of(
                    Component.text("Right click to randomize", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false)
            ));
            item.setItemMeta(meta);
        }

        return item;
    }

    private static ItemStack createSpeedBoostItem(int level) {
        Material material = switch (level) {
            case 0 -> Material.WOODEN_HOE;
            case 1 -> Material.STONE_HOE;
            case 2 -> Material.IRON_HOE;
            case 3 -> Material.GOLDEN_HOE;
            case 4 -> Material.DIAMOND_HOE;
            default -> Material.NETHERITE_HOE;
        };

        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.displayName(Component.text("Speed Boost", NamedTextColor.YELLOW, TextDecoration.BOLD)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.text("Right click for a speed boost", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false),
                    Component.text("Level: " + (level + 1), NamedTextColor.GOLD)
                            .decoration(TextDecoration.ITALIC, false)
            ));
            meta.setUnbreakable(true);
            item.setItemMeta(meta);
        }

        return item;
    }

    private static ItemStack createTrackerCrossbowItem() {
        ItemStack item = new ItemStack(Material.CROSSBOW);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.displayName(Component.text("Tracker Crossbow", NamedTextColor.AQUA, TextDecoration.BOLD)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.text("Hit seekers to upgrade speed", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false)
            ));
            meta.setUnbreakable(true);
            item.setItemMeta(meta);
        }

        return item;
    }

    private static ItemStack createBlockSelectorItem() {
        ItemStack item = new ItemStack(Material.COMMAND_BLOCK);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.displayName(Component.text("Block Selector", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.text("Right click to choose your block", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false)
            ));
            item.setItemMeta(meta);
        }

        return item;
    }

    private static ItemStack createAppearanceItem() {
        ItemStack item = new ItemStack(Material.COMPARATOR);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.displayName(Component.text("Appearance Editor", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.text("Right click to customize appearance", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false),
                    Component.empty(),
                    Component.text("Available during Hiding & Seeking phases", NamedTextColor.DARK_GRAY)
                            .decoration(TextDecoration.ITALIC, false)
            ));
            item.setItemMeta(meta);
        }

        return item;
    }

    private static String formatName(String name) {
        StringBuilder result = new StringBuilder();
        for (String part : name.toLowerCase().split("_")) {
            if (!result.isEmpty()) result.append(" ");
            result.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return result.toString();
    }


    private static void blockSwap(ItemInteractionContext context, HideAndSeek plugin) {
        Player player = context.getPlayer();
        if (!HideAndSeek.getDataController().getHiders().contains(player.getUniqueId())) {
            player.sendMessage(Component.text("Only hiders can use this item.", NamedTextColor.RED));
            context.skipCooldown();
            return;
        }
        if (!"BLOCK".equals(String.valueOf(plugin.getSettingService().getSetting("game.gametype").getValue()))) {
            player.sendMessage(Component.text("Block swap is only available in BLOCK mode.", NamedTextColor.RED));
            context.skipCooldown();
            return;
        }
        if (SeekerItems.isHiderCursed(player.getUniqueId())) {
            player.sendMessage(Component.text("You are cursed and cannot swap blocks!", NamedTextColor.RED));
            context.skipCooldown();
            return;
        }

        blockSwap(player, plugin);
    }

    private static void blockSwap(Player player, HideAndSeek plugin) {
        double range = plugin.getSettingRegistry().get("hider-items.block-swap.range", 50.0);
        Player target = null;
        double closest = range;

        for (UUID hiderId : HideAndSeek.getDataController().getHiders()) {
            if (hiderId.equals(player.getUniqueId())) {
                continue;
            }
            Player other = Bukkit.getPlayer(hiderId);
            if (other == null || !other.isOnline()) {
                continue;
            }
            double dist = other.getLocation().distance(player.getLocation());
            if (dist <= closest) {
                closest = dist;
                target = other;
            }
        }

        if (target == null) {
            player.sendMessage(Component.text("No hider nearby to swap with!", NamedTextColor.RED));
            return;
        }

        final Player finalTarget = target;

        Material playerMat = HideAndSeek.getDataController().getChosenBlock(player.getUniqueId());
        Material targetMat = HideAndSeek.getDataController().getChosenBlock(finalTarget.getUniqueId());
        BlockData playerData = HideAndSeek.getDataController().getChosenBlockData(player.getUniqueId());
        BlockData targetData = HideAndSeek.getDataController().getChosenBlockData(finalTarget.getUniqueId());

        if (playerMat == null || targetMat == null || playerData == null || targetData == null) {
            player.sendMessage(Component.text("Swap failed (missing block data)", NamedTextColor.RED));
            return;
        }


        boolean playerWasHidden = HideAndSeek.getDataController().isHidden(player.getUniqueId());
        boolean targetWasHidden = HideAndSeek.getDataController().isHidden(finalTarget.getUniqueId());

        if (playerWasHidden) {
            plugin.getBlockModeListener().forceUnhide(player);
        }
        if (targetWasHidden) {
            plugin.getBlockModeListener().forceUnhide(finalTarget);
        }


        Bukkit.getScheduler().runTaskLater(plugin, () -> {

            player.getInventory().remove(Material.COMPARATOR);
            finalTarget.getInventory().remove(Material.COMPARATOR);

            HideAndSeek.getDataController().setChosenBlock(player.getUniqueId(), targetMat);
            HideAndSeek.getDataController().setChosenBlock(finalTarget.getUniqueId(), playerMat);
            HideAndSeek.getDataController().setChosenBlockData(player.getUniqueId(), targetData);
            HideAndSeek.getDataController().setChosenBlockData(finalTarget.getUniqueId(), playerData);

            var playerDisplay = HideAndSeek.getDataController().getBlockDisplay(player.getUniqueId());
            if (playerDisplay != null && playerDisplay.isValid()) {
                playerDisplay.setBlock(targetData);
                playerDisplay.setRotation(player.getLocation().getYaw(), 0f);
            }
            var targetDisplay = HideAndSeek.getDataController().getBlockDisplay(finalTarget.getUniqueId());
            if (targetDisplay != null && targetDisplay.isValid()) {
                targetDisplay.setBlock(playerData);
                targetDisplay.setRotation(finalTarget.getLocation().getYaw(), 0f);
            }


            player.getWorld().spawnParticle(Particle.PORTAL, player.getLocation(), 20, 0.2, 0.5, 0.2, 1.0);
            finalTarget.getWorld().spawnParticle(Particle.PORTAL, finalTarget.getLocation(), 20, 0.2, 0.5, 0.2, 1.0);
            player.getWorld().spawnParticle(Particle.DRAGON_BREATH, player.getLocation(), 10, 0.3, 0.3, 0.3, 0.05);
            finalTarget.getWorld().spawnParticle(Particle.DRAGON_BREATH, finalTarget.getLocation(), 10, 0.3, 0.3, 0.3, 0.05);


            player.getWorld().spawnParticle(Particle.GLOW, player.getLocation().add(0, 1, 0), 15, 0.25, 0.4, 0.25, 0.08);
            finalTarget.getWorld().spawnParticle(Particle.GLOW, finalTarget.getLocation().add(0, 1, 0), 15, 0.25, 0.4, 0.25, 0.08);


            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
            finalTarget.getWorld().playSound(finalTarget.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);

            updateAppearanceItem(player, plugin);
            updateAppearanceItem(finalTarget, plugin);

            player.sendMessage(Component.text("Swapped blocks with " + finalTarget.getName() + "!", NamedTextColor.GREEN));
            finalTarget.sendMessage(Component.text("Swapped blocks with " + player.getName() + "!", NamedTextColor.GREEN));
        }, 2L);
    }

    private static void spawnBigFirecracker(ItemInteractionContext context, HideAndSeek plugin) {
        Location location = context.getLocation().clone().add(0, 1, 0);
        Player hider = context.getPlayer();

        Block block = location.getBlock();
        if (!block.getType().isAir()) {
            context.skipCooldown();
            return;
        }

        block.setType(Material.RED_CANDLE);
        Candle candle = (Candle) block.getBlockData();
        candle.setLit(true);
        candle.setCandles(4);
        block.setBlockData(candle);

        int tauntPoints = plugin.getPointService().award(hider.getUniqueId(), PointAction.HIDER_TAUNT_LARGE);
        double volume = plugin.getSettingRegistry().get("hider-items.big-firecracker.volume", 1.2);
        double pitch = plugin.getSettingRegistry().get("hider-items.big-firecracker.pitch", 0.5);
        int fuseTime = plugin.getSettingRegistry().get("hider-items.big-firecracker.fuse-time", 60);
        int miniFuse = plugin.getSettingRegistry().get("hider-items.big-firecracker.mini-fuse-time", 30);
        int miniCount = plugin.getSettingRegistry().get("hider-items.big-firecracker.mini-count", 3);

        hider.sendMessage(Component.text("Big Firecracker placed! +" + tauntPoints + " points", NamedTextColor.GOLD));

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            block.setType(Material.AIR);
            for (Player target : Bukkit.getOnlinePlayers()) {
                Location explosionLoc = location.clone().add(0.5, 0.5, 0.5);
                target.getWorld().spawnParticle(Particle.EXPLOSION, explosionLoc, 3, 0.3, 0.3, 0.3, 0.05);

                target.getWorld().spawnParticle(Particle.FLAME, explosionLoc, 15, 0.4, 0.4, 0.4, 0.08);

                target.getWorld().spawnParticle(Particle.DUST, explosionLoc, 20, 0.4, 0.4, 0.4,
                        new Particle.DustOptions(Color.fromARGB(255, 255, 80, 0), 1.5f));
                target.playSound(location, Sound.ENTITY_GENERIC_EXPLODE, (float) volume, (float) pitch);
            }

            for (int i = 0; i < miniCount; i++) {
                spawnMiniFirecracker(location, plugin, miniFuse);
            }
        }, fuseTime);
    }

    private static void spawnMiniFirecracker(Location origin, HideAndSeek plugin, int miniFuse) {
        Location spawnLoc = origin.clone().add(0.5, 1.0, 0.5);
        ArmorStand stand = origin.getWorld().spawn(spawnLoc, ArmorStand.class, s -> {
            s.setInvisible(true);
            s.setSmall(true);
            s.setGravity(true);
            s.setCollidable(false);
            s.getPersistentDataContainer().set(new NamespacedKey(plugin, "firecracker"), PersistentDataType.BOOLEAN, true);
        });

        Vector velocity = new Vector(
                (Math.random() - 0.5) * 0.8,
                0.3 + Math.random() * 0.5,
                (Math.random() - 0.5) * 0.8
        );
        stand.setVelocity(velocity);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!stand.isValid()) {
                    cancel();
                    return;
                }

                Location loc = stand.getLocation();
                World world = loc.getWorld();


                world.spawnParticle(
                        Particle.ELECTRIC_SPARK,
                        loc.getX(), loc.getY(), loc.getZ(),
                        5,
                        0.1, 0.1, 0.1,
                        0.02
                );


                Vector downward = new Vector(0, -0.1, 0);

                var hit = world.rayTraceBlocks(loc, downward, 0.3);

                if (hit != null && hit.getHitBlock() != null && hit.getHitBlock().getType().isSolid()) {

                    stand.remove();

                    Location land = hit.getHitBlock().getLocation().add(0, 1, 0);
                    if (land.getBlock().getType().isAir()) {
                        land.getBlock().setType(Material.RED_CANDLE);
                        Candle candle = (Candle) land.getBlock().getBlockData();
                        candle.setLit(true);
                        candle.setCandles(1);
                        land.getBlock().setBlockData(candle);

                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            land.getBlock().setType(Material.AIR);
                            land.getWorld().spawnParticle(
                                    Particle.EXPLOSION,
                                    land.clone().add(0.5, 0.5, 0.5),
                                    1, 0, 0, 0, 0
                            );
                            land.getWorld().playSound(land, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.2f);
                        }, miniFuse);
                    }
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    private static void launchFirework(Player player, HideAndSeek plugin) {
        Location launchLocation = player.getLocation().clone().add(0, 1.5, 0);
        int targetY = plugin.getSettingRegistry().get("hider-items.firework-rocket.target-y", 128);
        int points = plugin.getPointService().award(player.getUniqueId(), PointAction.HIDER_TAUNT_LARGE);
        double volume = plugin.getSettingRegistry().get("hider-items.firework-rocket.volume", 10.0);

        Firework firework = (Firework) launchLocation.getWorld().spawnEntity(launchLocation, EntityType.FIREWORK_ROCKET);
        FireworkMeta meta = firework.getFireworkMeta();
        meta.setPower(3);
        meta.addEffect(FireworkEffect.builder()
                .with(FireworkEffect.Type.BALL_LARGE)
                .withColor(Color.RED, Color.YELLOW, Color.ORANGE)
                .withFade(Color.WHITE)
                .flicker(true)
                .trail(true)
                .build());
        firework.setFireworkMeta(meta);

        player.sendMessage(Component.text("Firework launched! +" + points + " points", NamedTextColor.GOLD));

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!firework.isValid()) {
                    cancel();
                    return;
                }

                Location loc = firework.getLocation();
                firework.setTicksToDetonate(100);

                if (loc.getY() >= targetY) {
                    firework.detonate();
                    for (Player p : loc.getNearbyPlayers(200)) {
                        p.playSound(p.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, (float) volume, 0.9f);
                        p.playSound(p.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, (float) volume, 0.9f);
                    }
                    cancel();
                    return;
                }

                World world = loc.getWorld();

                Vector up = new Vector(0, 1, 0);
                double maxCheckDistance = 2.0;
                var rayResult = world.rayTraceBlocks(loc, up, maxCheckDistance);

                if (rayResult != null) {
                    Block blockHit = rayResult.getHitBlock();
                    if (blockHit != null) {
                        Location safeLoc = blockHit.getWorld().getHighestBlockAt(blockHit.getLocation()).getLocation().add(0, 1, 0);
                        firework.teleport(safeLoc);
                    }
                } else {
                    firework.setVelocity(new Vector(0, 1.2, 0));
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    private static void startMedkitConsume(ItemInteractionContext context, HideAndSeek plugin) {
        Player player = context.getPlayer();
        plugin.getLogger().info("Consuming medkit for " + player.getName());
        int channelTime = plugin.getSettingRegistry().get("hider-items.medkit.channel-time", 5);


        new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = channelTime * 20;

            @Override
            public void run() {
                if (!player.isOnline() || ticks >= maxTicks) {
                    cancel();
                    return;
                }

                Location loc = player.getLocation().add(0, 1, 0);

                player.getWorld().spawnParticle(Particle.HEART, loc, 2, 0.2, 0.3, 0.2, 0.05);
                player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, loc, 3, 0.15, 0.25, 0.15, 0.02);

                ticks++;
            }
        }.runTaskTimer(plugin, 1L, 2L);
    }

    private static void useMedkit(ItemInteractionContext context, HideAndSeek plugin) {
        Player player = context.getPlayer();
        if (!HideAndSeek.getDataController().getHiders().contains(player.getUniqueId())) {
            player.sendMessage(Component.text("Only hiders can use this item.", NamedTextColor.RED));
            context.skipCooldown();
            return;
        }

        double healAmount = plugin.getSettingRegistry().get("hider-items.medkit.heal-amount", 20.0);

        double maxHealth = Objects.requireNonNull(player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH)).getValue();
        double newHealth = Math.min(maxHealth, player.getHealth() + healAmount);
        player.setHealth(newHealth);
        player.sendMessage(Component.text("Healed!", NamedTextColor.GREEN));
    }

    private static void activateTotem(Player player, HideAndSeek plugin) {
        if (!HideAndSeek.getDataController().getHiders().contains(player.getUniqueId())) {
            player.sendMessage(Component.text("Only hiders can use this item.", NamedTextColor.RED));
            return;
        }

        int duration = plugin.getSettingRegistry().get("hider-items.totem.effect-duration", 30);
        long expiresAt = System.currentTimeMillis() + (duration * 1000L);
        totemActiveUntil.put(player.getUniqueId(), expiresAt);

        player.getInventory().removeItem(new ItemStack(Material.TOTEM_OF_UNDYING, 1));
        player.sendMessage(Component.text("Revive mode activated for " + duration + " seconds!", NamedTextColor.GOLD));


        new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = duration * 20;

            @Override
            public void run() {
                if (!player.isOnline() || ticks >= maxTicks || !isTotemActive(player.getUniqueId())) {
                    cancel();
                    return;
                }

                Location loc = player.getLocation().add(0, 1, 0);

                player.getWorld().spawnParticle(Particle.GLOW, loc, 8, 0.4, 0.4, 0.4, 0.05);


                if (ticks % 5 == 0) {
                    player.getWorld().spawnParticle(Particle.DUST, loc, 5, 0.3, 0.3, 0.3,
                            new Particle.DustOptions(Color.fromARGB(255, 255, 200, 0), 1.0f));
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 1L, 2L);
    }

    public static boolean isTotemActive(UUID playerId) {
        Long expiresAt = totemActiveUntil.get(playerId);
        if (expiresAt == null) {
            return false;
        }
        if (System.currentTimeMillis() > expiresAt) {
            totemActiveUntil.remove(playerId);
            return false;
        }
        return true;
    }

    public static void clearTotem(UUID playerId) {
        totemActiveUntil.remove(playerId);
    }

    public static void reviveWithTotem(Player player) {
        if (player == null) {
            return;
        }
        clearTotem(player.getUniqueId());

        player.playEffect(EntityEffect.PROTECTED_FROM_DEATH);
        player.setHealth(Math.max(1.0, Objects.requireNonNull(player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH)).getValue()));
        player.setFoodLevel(20);


        org.bukkit.Location roundSpawn = HideAndSeek.getDataController().getRoundSpawnPoint();
        if (roundSpawn != null) {
            player.teleport(roundSpawn);
        }

        player.sendMessage(Component.text("You were revived!", NamedTextColor.GOLD));
    }

    public static void randomizeBlockFor(Player player, HideAndSeek plugin, boolean forceUnhide) {
        if (player == null) {
            return;
        }
        if (forceUnhide && HideAndSeek.getDataController().isHidden(player.getUniqueId())) {
            plugin.getBlockModeListener().forceUnhide(player);

            Bukkit.getScheduler().runTaskLater(plugin, () -> randomizeBlock(player, plugin), 1L);
        } else {
            randomizeBlock(player, plugin);
        }
    }

    private static ItemStack createKnockbackStickItem(int level) {
        ItemStack item = new ItemStack(Material.STICK);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Knockback Stick", NamedTextColor.DARK_RED, TextDecoration.BOLD)
                    .append(Component.space())
                    .append(Component.text("(Level " + level + ")", NamedTextColor.GOLD)
                            .decoration(TextDecoration.ITALIC, false))
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.text("Left click to knock seekers away", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false)
            ));
            meta.addEnchant(Enchantment.KNOCKBACK, Math.max(1, Math.min(5, level)), true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            meta.setUnbreakable(true);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack createBlockSwapItem() {
        ItemStack item = new ItemStack(Material.ENDER_PEARL);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Block Swap", NamedTextColor.DARK_PURPLE, TextDecoration.BOLD)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.text("Right click to swap blocks with another hider", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false)
            ));
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack createBigFirecrackerItem() {
        ItemStack item = new ItemStack(Material.TNT);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Big Firecracker", NamedTextColor.RED, TextDecoration.BOLD)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.text("Right click to place a big firecracker", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false)
            ));
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack createFireworkRocketItem() {
        ItemStack item = new ItemStack(Material.FIREWORK_ROCKET);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Firework Rocket", NamedTextColor.GOLD, TextDecoration.BOLD)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.text("Right click to launch a firework", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false)
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    private static ItemStack createMedkitItem(HideAndSeek plugin) {
        ItemStack item = new ItemStack(Material.GOLDEN_APPLE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Medkit", NamedTextColor.GREEN, TextDecoration.BOLD)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.text("Right click to heal yourself", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false)
            ));

            item.setItemMeta(meta);

            int standStillSeconds = plugin.getSettingRegistry().get("hider-items.medkit.channel-time", 5);

            item.setData(DataComponentTypes.CONSUMABLE, Consumable.consumable().consumeSeconds(standStillSeconds).build());
        }
        return item;
    }

    private static ItemStack createTotemItem() {
        ItemStack item = new ItemStack(Material.TOTEM_OF_UNDYING);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Totem of Undying", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.text("Right click to activate revive mode", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false)
            ));
            meta.setUnbreakable(true);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static void randomizeBlockWithContext(ItemInteractionContext context, HideAndSeek plugin) {
        Player player = context.getPlayer();
        if (player == null) {
            context.skipCooldown();
            return;
        }
        if (!HideAndSeek.getDataController().getHiders().contains(player.getUniqueId())) {
            player.sendMessage(Component.text("Only hiders can use this item.", NamedTextColor.RED));
            context.skipCooldown();
            return;
        }
        if (SeekerItems.isHiderCursed(player.getUniqueId())) {
            player.sendMessage(Component.text("You are cursed and cannot change blocks right now!", NamedTextColor.RED));
            context.skipCooldown();
            return;
        }

        randomizeBlock(player, plugin);
    }

    private static ItemStack createInvisibilityCloakItem() {
        ItemStack item = new ItemStack(Material.PHANTOM_MEMBRANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Invisibility Cloak", NamedTextColor.DARK_PURPLE, TextDecoration.BOLD)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.text("Right click to become invisible", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false)
            ));
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static void useInvisibilityCloak(Player player, HideAndSeek plugin) {
        if (!HideAndSeek.getDataController().getHiders().contains(player.getUniqueId())) {
            player.sendMessage(Component.text("Only hiders can use this item.", NamedTextColor.RED));
            return;
        }

        int duration = plugin.getSettingRegistry().get("hider-items.invisibility-cloak.duration", 8);

        var gameModeResult = plugin.getSettingService().getSetting("game.gametype");
        Object gameModeObj = gameModeResult.isSuccess() ? gameModeResult.getValue() : null;
        String gameMode = gameModeObj != null ? gameModeObj.toString() : "";

        if ("BLOCK".equals(gameMode)) {

            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.INVISIBILITY,
                    duration * 20,
                    0,
                    false,
                    true,
                    true
            ));


            BlockDisplay display = HideAndSeek.getDataController().getBlockDisplay(player.getUniqueId());
            if (display != null && display.isValid()) {
                display.setVisibleByDefault(false);
            }
        } else {

            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.INVISIBILITY,
                    duration * 20,
                    0,
                    false,
                    true,
                    true
            ));
        }

        player.sendMessage(Component.text("You are now invisible!", NamedTextColor.AQUA));


        Location loc = player.getLocation().add(0, 1, 0);
        player.getWorld().spawnParticle(Particle.POOF, loc, 30, 0.5, 0.5, 0.5, 0.15);
        player.getWorld().spawnParticle(Particle.GLOW, loc, 15, 0.4, 0.4, 0.4, 0.1);
        player.playSound(player.getLocation(), Sound.ENTITY_PHANTOM_FLAP, 1.0f, 1.5f);


        new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = duration * 20;

            @Override
            public void run() {
                if (!player.isOnline() || ticks >= maxTicks) {
                    cancel();

                    if ("BLOCK".equals(gameMode)) {
                        BlockDisplay display = HideAndSeek.getDataController().getBlockDisplay(player.getUniqueId());
                        if (display != null && display.isValid()) {
                            display.setVisibleByDefault(true);
                        }
                    }
                    return;
                }


                if (ticks % 10 == 0) {
                    Location particleLoc = player.getLocation().add(0, 1, 0);
                    player.getWorld().spawnParticle(Particle.SOUL, particleLoc, 2, 0.2, 0.2, 0.2, 0.02);
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    private static ItemStack createSlownessBallItem() {
        ItemStack item = new ItemStack(Material.SNOWBALL);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Slowness Ball", NamedTextColor.AQUA, TextDecoration.BOLD)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.text("Right click to throw at seekers", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false)
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    private static void throwSlownessBall(ItemInteractionContext context, HideAndSeek plugin) {
        Player player = context.getPlayer();
        if (!HideAndSeek.getDataController().getHiders().contains(player.getUniqueId())) {
            player.sendMessage(Component.text("Only hiders can use this item.", NamedTextColor.RED));
            return;
        }

        int duration = plugin.getSettingRegistry().get("hider-items.slowness-ball.duration", 6);
        int amplifier = plugin.getSettingRegistry().get("hider-items.slowness-ball.amplifier", 1);

        org.bukkit.entity.Snowball snowball = player.launchProjectile(org.bukkit.entity.Snowball.class);
        snowball.setItem(new ItemStack(Material.ICE));
        snowball.setVelocity(snowball.getVelocity().multiply(1.5));
        snowball.getPersistentDataContainer().set(new NamespacedKey(plugin, "slowness_ball"), PersistentDataType.BOOLEAN, true);
        snowball.getPersistentDataContainer().set(new NamespacedKey(plugin, "slowness_ball_duration"), PersistentDataType.INTEGER, duration);
        snowball.getPersistentDataContainer().set(new NamespacedKey(plugin, "slowness_ball_amplifier"), PersistentDataType.INTEGER, amplifier);


        new BukkitRunnable() {
            @Override
            public void run() {
                if (!snowball.isValid()) {
                    cancel();
                    return;
                }

                snowball.getWorld().spawnParticle(Particle.SNOWFLAKE, snowball.getLocation(), 3, 0.1, 0.1, 0.1, 0.05);
            }
        }.runTaskTimer(plugin, 1L, 2L);


        Bukkit.getScheduler().runTaskLater(plugin, snowball::remove, 200L);
    }

    private static ItemStack createSmokeBombItem() {
        ItemStack item = new ItemStack(Material.GRAY_DYE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Smoke Bomb", NamedTextColor.DARK_GRAY, TextDecoration.BOLD)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.text("Right click to throw", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false),
                    Component.text("Creates a smoke cloud for cover", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false)
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    private static void throwSmokeBomb(ItemInteractionContext context, HideAndSeek plugin) {
        Player player = context.getPlayer();
        if (!HideAndSeek.getDataController().getHiders().contains(player.getUniqueId())) {
            player.sendMessage(Component.text("Only hiders can use this item.", NamedTextColor.RED));
            return;
        }

        int duration = plugin.getSettingRegistry().get("hider-items.smoke-bomb.duration", 8);
        int radius = plugin.getSettingRegistry().get("hider-items.smoke-bomb.radius", 4);

        org.bukkit.entity.Snowball smokeBomb = player.launchProjectile(org.bukkit.entity.Snowball.class);
        smokeBomb.setItem(new ItemStack(Material.BLACK_CONCRETE_POWDER));
        smokeBomb.setVelocity(smokeBomb.getVelocity().multiply(1.2));
        smokeBomb.getPersistentDataContainer().set(new NamespacedKey(plugin, "smoke_bomb"), PersistentDataType.BOOLEAN, true);
        smokeBomb.getPersistentDataContainer().set(new NamespacedKey(plugin, "smoke_bomb_duration"), PersistentDataType.INTEGER, duration);
        smokeBomb.getPersistentDataContainer().set(new NamespacedKey(plugin, "smoke_bomb_radius"), PersistentDataType.INTEGER, radius);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!smokeBomb.isValid()) {
                    cancel();
                    return;
                }

                smokeBomb.getWorld().spawnParticle(Particle.SMOKE, smokeBomb.getLocation(), 2, 0.1, 0.1, 0.1, 0.02);
            }
        }.runTaskTimer(plugin, 1L, 2L);

        Bukkit.getScheduler().runTaskLater(plugin, smokeBomb::remove, 200L);
    }
}
