package de.thecoolcraft11.hideAndSeek.items.hider;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.items.ItemSkinSelectionService;
import de.thecoolcraft11.hideAndSeek.items.api.GameItem;
import de.thecoolcraft11.hideAndSeek.model.SpeedBoostType;
import de.thecoolcraft11.minigameframework.items.CustomItemBuilder;
import de.thecoolcraft11.minigameframework.items.ItemActionType;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.TooltipDisplay;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class SpeedBoostItem implements GameItem {
    public static final String ID = "has_hider_speed_boost";

    private static final Map<UUID, Integer> speedLevels = new HashMap<>();

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public List<String> getAllIds() {
        List<String> ids = new ArrayList<>();
        for (int i = 0; i <= 5; i++) {
            ids.add(ID + "_" + i);
        }
        return ids;
    }

    @SuppressWarnings("UnstableApiUsage")
    public static ItemStack createSpeedBoostItem(int level, HideAndSeek plugin) {
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
            meta.displayName(MiniMessage.miniMessage().deserialize(
                            plugin.trText(null, "item.speed_boost.name", java.util.Map.of("level", level + 1)))
                    .decoration(TextDecoration.ITALIC, false));
            String loreStr = plugin.trText(null, "item.speed_boost.lore", java.util.Map.of("level", level + 1));
            java.util.List<Component> lore = new java.util.ArrayList<>();
            for (String line : loreStr.split("\n")) {
                lore.add(MiniMessage.miniMessage().deserialize(line).decoration(TextDecoration.ITALIC, false));
            }
            meta.lore(lore);
            meta.setUnbreakable(true);
            item.setItemMeta(meta);
        }
        item.setData(DataComponentTypes.TOOLTIP_DISPLAY,
                TooltipDisplay.tooltipDisplay().addHiddenComponents(DataComponentTypes.UNBREAKABLE).build());

        return item;
    }

    @Override
    public String getDescription(HideAndSeek plugin, @Nullable Player player) {
        Number duration = plugin.getSettingRegistry().get("hider-items.speed-boost.duration", 5);
        Object boostTypeObj = plugin.getSettingRegistry().get("hider-items.speed-boost.type");
        String boostMode = (boostTypeObj instanceof Enum) ? boostTypeObj.toString() : "SPEED_EFFECT";

        if ("VELOCITY_BOOST".equals(boostMode)) {
            return plugin.trText(player, "item.speed_boost.description.velocity_boost");
        } else {
            return plugin.trText(player, "item.speed_boost.description.speed_effect",
                    java.util.Map.of("duration", String.valueOf(duration.intValue())));
        }
    }

    public static void upgradeSpeedItem(Player player, HideAndSeek plugin) {
        int level = Math.min(5, getSpeedLevel(player.getUniqueId()) + 1);
        speedLevels.put(player.getUniqueId(), level);

        ItemStack upgradedItem = null;
        String selectedVariant = null;
        if (plugin != null) {
            String runtimeItemId = ID + "_" + level;
            var customItem = plugin.getCustomItemManager().getItem(runtimeItemId);
            if (customItem != null) {
                upgradedItem = customItem.getIdentifiedItemStack(plugin);

                selectedVariant = ItemSkinSelectionService.getSelectedVariant(player, ID);
                if (selectedVariant != null) {
                    var variant = plugin.getCustomItemManager().getVariantManager().getVariant(runtimeItemId,
                            selectedVariant);
                    if (variant == null) {
                        selectedVariant = null;
                    }
                }
            }
        }

        if (upgradedItem == null) {
            upgradedItem = createSpeedBoostItem(level, plugin);
        }

        if (plugin == null) return;

        removeSpeedItems(player, plugin);
        player.getInventory().addItem(upgradedItem);
        if (selectedVariant != null) {
            plugin.getCustomItemManager().getVariantManager().switchVariant(player, ID + "_" + level, selectedVariant);
        }
        player.sendMessage(plugin.trText(player, "item.speed_boost.messages.upgraded",
                java.util.Map.of("level", String.valueOf(level + 1))));
    }

    @Override
    public ItemStack createItem(HideAndSeek plugin) {
        return createSpeedBoostItem(0, plugin);
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
        boolean rocketBoots = ItemSkinSelectionService.isSelected(player, ID, "skin_rocket_boots");
        boolean sugarRush = ItemSkinSelectionService.isSelected(player, ID, "skin_sugar_rush");

        if (boostType == SpeedBoostType.VELOCITY_BOOST) {
            double boostPower = plugin.getSettingRegistry().get("hider-items.speed-boost.boost-power", 0.5);
            boostPower += (amplifier * amplifierBonus);

            Vector direction = player.getLocation().getDirection().normalize().multiply(boostPower);
            player.setVelocity(player.getVelocity().add(direction));
            player.sendMessage(plugin.trText(player, "item.speed_boost.messages.velocity_activated",
                    java.util.Map.of("level", String.valueOf(amplifier + 1))));
            if (rocketBoots) {
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 0.8f, 1.3f);
            } else if (sugarRush) {
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_BURP, 0.6f, 1.8f);
                player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, player.getLocation().add(0, 1, 0), 10, 0.2,
                        0.25, 0.2, 0.03);
            }

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
                    if (rocketBoots) {
                        player.getWorld().spawnParticle(Particle.FLAME, loc, 3, 0.1, 0.1, 0.1, 0.02);
                        player.getWorld().spawnParticle(Particle.CLOUD, loc, 2, 0.1, 0.1, 0.1, 0.05);
                    } else if (sugarRush) {
                        player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, loc, 2, 0.15, 0.15, 0.15, 0.02);
                        player.getWorld().spawnParticle(Particle.END_ROD, loc, 1, 0.03, 0.03, 0.03, 0.0);
                    } else {
                        player.getWorld().spawnParticle(Particle.CLOUD, loc, 2, 0.1, 0.1, 0.1, 0.05);
                    }
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
            player.sendMessage(plugin.trText(player, "item.speed_boost.messages.speed_activated",
                    java.util.Map.of("level", String.valueOf(amplifier + 1))));
            if (rocketBoots) {
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_BREEZE_SHOOT, 0.8f, 1.2f);
            } else if (sugarRush) {
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_GENERIC_DRINK, 0.7f, 1.4f);
                player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, player.getLocation().add(0, 1, 0), 10, 0.2,
                        0.25, 0.2, 0.03);
            }

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
                        if (rocketBoots) {
                            player.getWorld().spawnParticle(Particle.FLAME, loc.add(0.5, 0.1, 0.5), 2, 0.15, 0.05, 0.15,
                                    0.01);
                        } else if (sugarRush) {
                            player.getWorld().spawnParticle(Particle.CHERRY_LEAVES, loc.add(0.5, 0.1, 0.5), 2, 0.15,
                                    0.05, 0.15, 0.02);
                            player.getWorld().spawnParticle(Particle.END_ROD, loc, 1, 0.03, 0.03, 0.03, 0.0);
                        } else {
                            player.getWorld().spawnParticle(Particle.CLOUD, loc.add(0.5, 0.1, 0.5), 1, 0.15, 0.05, 0.15,
                                    0.02);
                        }
                    }

                    ticks++;
                }
            }.runTaskTimer(plugin, 1L, 1L);
        }
    }

    @Override
    public void register(HideAndSeek plugin) {
        int speedBoostCooldown = plugin.getSettingRegistry().get("hider-items.speed-boost.cooldown", 10);

        for (int level = 0; level <= 5; level++) {
            String levelId = ID + "_" + level;
            plugin.getCustomItemManager().registerItem(
                    new CustomItemBuilder(createSpeedBoostItem(level, plugin), levelId)
                    .withAction(ItemActionType.RIGHT_CLICK_AIR, context -> speedBoost(context.getPlayer(), plugin))
                    .withAction(ItemActionType.RIGHT_CLICK_BLOCK, context -> speedBoost(context.getPlayer(), plugin))
                    .withAction(ItemActionType.SHIFT_RIGHT_CLICK_AIR,
                            context -> speedBoost(context.getPlayer(), plugin))
                    .withAction(ItemActionType.SHIFT_RIGHT_CLICK_BLOCK,
                            context -> speedBoost(context.getPlayer(), plugin))
                    .withVanillaCooldown(speedBoostCooldown * 20)
                    .withCustomCooldown(speedBoostCooldown * 1000L)
                    .withVanillaCooldownDisplay(true)
                    .withDescription(getDescription(plugin, null))
                    .withNameKey("item.speed_boost.name", Map.of("level", level + 1))
                    .withLoreKey("item.speed_boost.lore", Map.of("level", level + 1))
                    .withDropPrevention(true)
                    .withCraftPrevention(true)
                    .allowOffHand(false)
                    .allowArmor(false)
                    .cancelDefaultAction(true)
                    .build());
        }
    }

    public static int getSpeedLevel(UUID playerId) {
        return speedLevels.getOrDefault(playerId, 0);
    }

    public static void resetSpeedLevel(UUID playerId) {
        speedLevels.put(playerId, 0);
    }

    private static void removeSpeedItems(Player player, HideAndSeek plugin) {
        var manager = plugin.getCustomItemManager();
        var inv = player.getInventory();
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);
            if (item != null) {
                String id = manager.getCustomItemId(item);
                if (id != null && id.startsWith(ID)) {
                    inv.setItem(i, null);
                }
            }
        }
        ItemStack[] armor = inv.getArmorContents();
        for (int i = 0; i < armor.length; i++) {
            ItemStack item = armor[i];
            if (item != null) {
                String id = manager.getCustomItemId(item);
                if (id != null && id.startsWith(ID)) {
                    armor[i] = null;
                }
            }
        }
        inv.setArmorContents(armor);
        ItemStack offHand = inv.getItemInOffHand();
        String id = manager.getCustomItemId(offHand);
        if (id != null && id.startsWith(ID)) {
            inv.setItemInOffHand(null);
        }
    }

    @Override
    public Set<String> getConfigKeys() {
        return Set.of("hider-items.speed-boost.cooldown");
    }
}
