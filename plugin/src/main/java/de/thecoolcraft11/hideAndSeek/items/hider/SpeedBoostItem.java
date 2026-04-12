package de.thecoolcraft11.hideAndSeek.items.hider;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.items.ItemSkinSelectionService;
import de.thecoolcraft11.hideAndSeek.items.api.GameItem;
import de.thecoolcraft11.hideAndSeek.model.SpeedBoostType;
import de.thecoolcraft11.minigameframework.items.CustomItemBuilder;
import de.thecoolcraft11.minigameframework.items.ItemActionType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
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

    @Override
    public ItemStack createItem(HideAndSeek plugin) {
        return createSpeedBoostItem(0);
    }

    @Override
    public String getDescription(HideAndSeek plugin) {
        Number duration = plugin.getSettingRegistry().get("hider-items.speed-boost.duration", 5);
        Object boostTypeObj = plugin.getSettingRegistry().get("hider-items.speed-boost.type");
        String boostMode = (boostTypeObj instanceof Enum) ? boostTypeObj.toString() : "SPEED_EFFECT";

        if ("VELOCITY_BOOST".equals(boostMode)) {
            return "Launch yourself forward with a velocity boost.";
        } else {
            return String.format("Gain speed effect for %ds to move faster.", duration.intValue());
        }
    }

    @Override
    public void register(HideAndSeek plugin) {
        int speedBoostCooldown = plugin.getSettingRegistry().get("hider-items.speed-boost.cooldown", 10);

        for (int level = 0; level <= 5; level++) {
            String levelId = ID + "_" + level;
            plugin.getCustomItemManager().registerItem(new CustomItemBuilder(createSpeedBoostItem(level), levelId)
                    .withAction(ItemActionType.RIGHT_CLICK_AIR, context -> speedBoost(context.getPlayer(), plugin))
                    .withAction(ItemActionType.RIGHT_CLICK_BLOCK, context -> speedBoost(context.getPlayer(), plugin))
                    .withAction(ItemActionType.SHIFT_RIGHT_CLICK_AIR, context -> speedBoost(context.getPlayer(), plugin))
                    .withAction(ItemActionType.SHIFT_RIGHT_CLICK_BLOCK, context -> speedBoost(context.getPlayer(), plugin))
                    .withVanillaCooldown(speedBoostCooldown * 20)
                    .withCustomCooldown(speedBoostCooldown * 1000L)
                    .withVanillaCooldownDisplay(true)
                    .withDescription(getDescription(plugin))
                    .withDropPrevention(true)
                    .withCraftPrevention(true)
                    .allowOffHand(false)
                    .allowArmor(false)
                    .cancelDefaultAction(true)
                    .build());
        }
    }


    public static ItemStack createSpeedBoostItem(int level) {
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
                    .append(Component.space())
                    .append(Component.text("(Level " + (level + 1) + ")", NamedTextColor.GOLD)
                            .decoration(TextDecoration.ITALIC, false))
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
            player.sendMessage(Component.text("Velocity boost activated! ", NamedTextColor.YELLOW)
                    .append(Component.text("(Level " + (amplifier + 1) + ")", NamedTextColor.GOLD)));
            if (rocketBoots) {
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 0.8f, 1.3f);
            } else if (sugarRush) {
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_PLAYER_BURP, 0.6f, 1.8f);
                player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, player.getLocation().add(0, 1, 0), 10, 0.2, 0.25, 0.2, 0.03);
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
            player.sendMessage(Component.text("Speed boost activated! ", NamedTextColor.YELLOW)
                    .append(Component.text("(Level " + (amplifier + 1) + ")", NamedTextColor.GOLD)));
            if (rocketBoots) {
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_BREEZE_SHOOT, 0.8f, 1.2f);
            } else if (sugarRush) {
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_GENERIC_DRINK, 0.7f, 1.4f);
                player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, player.getLocation().add(0, 1, 0), 10, 0.2, 0.25, 0.2, 0.03);
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
                            player.getWorld().spawnParticle(Particle.FLAME, loc.add(0.5, 0.1, 0.5), 2, 0.15, 0.05, 0.15, 0.01);
                        } else if (sugarRush) {
                            player.getWorld().spawnParticle(Particle.CHERRY_LEAVES, loc.add(0.5, 0.1, 0.5), 2, 0.15, 0.05, 0.15, 0.02);
                            player.getWorld().spawnParticle(Particle.END_ROD, loc, 1, 0.03, 0.03, 0.03, 0.0);
                        } else {
                            player.getWorld().spawnParticle(Particle.CLOUD, loc.add(0.5, 0.1, 0.5), 1, 0.15, 0.05, 0.15, 0.02);
                        }
                    }

                    ticks++;
                }
            }.runTaskTimer(plugin, 1L, 1L);
        }
    }

    public static void upgradeSpeedItem(Player player, HideAndSeek plugin) {
        int level = Math.min(5, getSpeedLevel(player.getUniqueId()) + 1);
        speedLevels.put(player.getUniqueId(), level);

        ItemStack upgradedItem = null;
        if (plugin != null) {
            String runtimeItemId = ID + "_" + level;
            var customItem = plugin.getCustomItemManager().getItem(runtimeItemId);
            if (customItem != null) {
                upgradedItem = customItem.getItemStack();

                String selectedVariant = ItemSkinSelectionService.getSelectedVariant(player, ID);
                if (selectedVariant != null) {
                    var variant = plugin.getCustomItemManager().getVariantManager().getVariant(runtimeItemId, selectedVariant);
                    if (variant != null && variant.getItemStack() != null) {
                        upgradedItem = variant.getItemStack().clone();
                    }
                }
            }
        }

        if (upgradedItem == null) {
            upgradedItem = createSpeedBoostItem(level);
        }

        removeSpeedItems(player);
        player.getInventory().addItem(upgradedItem);
        player.sendMessage(Component.text("Upgraded to Level " + (level + 1) + "!", NamedTextColor.GOLD));
    }

    public static int getSpeedLevel(UUID playerId) {
        return speedLevels.getOrDefault(playerId, 0);
    }

    private static void removeSpeedItems(Player player) {
        player.getInventory().remove(Material.WOODEN_HOE);
        player.getInventory().remove(Material.STONE_HOE);
        player.getInventory().remove(Material.IRON_HOE);
        player.getInventory().remove(Material.GOLDEN_HOE);
        player.getInventory().remove(Material.DIAMOND_HOE);
        player.getInventory().remove(Material.NETHERITE_HOE);
    }

    @Override
    public Set<String> getConfigKeys() {
        return Set.of("hider-items.speed-boost.cooldown");
    }
}
