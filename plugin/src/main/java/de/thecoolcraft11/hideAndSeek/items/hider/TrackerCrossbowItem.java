package de.thecoolcraft11.hideAndSeek.items.hider;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.items.ItemSkinSelectionService;
import de.thecoolcraft11.hideAndSeek.items.api.GameItem;
import de.thecoolcraft11.minigameframework.items.CustomItemBuilder;
import de.thecoolcraft11.minigameframework.items.ItemActionType;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.TooltipDisplay;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class TrackerCrossbowItem implements GameItem {
    public static final String ID = "has_hider_crossbow";

    private static final Map<UUID, Integer> trackerHits = new HashMap<>();

    @Override
    public String getId() {
        return ID;
    }

    public static void onTrackerHit(Player hider, HideAndSeek plugin) {
        if (hider == null) {
            return;
        }

        int hitPoints = plugin.getPointService().award(hider.getUniqueId(),
                de.thecoolcraft11.hideAndSeek.util.points.PointAction.HIDER_SHARPSHOOTER);
        hider.sendMessage(plugin.trText(hider, "item.crossbow.messages.hit",
                java.util.Map.of("points", String.valueOf(hitPoints))));

        if (ItemSkinSelectionService.isSelected(hider, ID, "skin_paintball_gun")) {
            hider.getWorld().spawnParticle(Particle.ENTITY_EFFECT, hider.getLocation().add(0, 1.1, 0), 14, 0.4, 0.3,
                    0.4, 1.0);
            hider.getWorld().spawnParticle(Particle.ITEM_SLIME, hider.getLocation().add(0, 1.0, 0), 6, 0.25, 0.25, 0.25,
                    0.01);
            hider.playSound(hider.getLocation(), Sound.ENTITY_SLIME_SQUISH_SMALL, 0.55f, 1.5f);
        } else if (ItemSkinSelectionService.isSelected(hider, ID, "skin_laser_tag")) {
            hider.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, hider.getLocation().add(0, 1.0, 0), 12, 0.3, 0.3,
                    0.3, 0.03);
            hider.getWorld().spawnParticle(Particle.END_ROD, hider.getLocation().add(0, 1.0, 0), 6, 0.15, 0.2, 0.15,
                    0.01);
            hider.playSound(hider.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 0.45f, 1.8f);
        }

        int hits = trackerHits.getOrDefault(hider.getUniqueId(), 0) + 1;
        trackerHits.put(hider.getUniqueId(), hits);

        int hitsPerUpgrade = plugin.getSettingRegistry().get("hider-items.crossbow.hits-per-upgrade", 3);
        if (hits >= hitsPerUpgrade) {
            trackerHits.put(hider.getUniqueId(), 0);
            upgradeLoadoutItems(hider, plugin);
        } else {
            hider.sendMessage(plugin.trText(hider, "item.crossbow.messages.hit_progress",
                    java.util.Map.of("current", String.valueOf(hits), "required", String.valueOf(hitsPerUpgrade))));
        }
    }

    @SuppressWarnings("UnstableApiUsage")
    @Override
    public ItemStack createItem(HideAndSeek plugin) {
        ItemStack item = new ItemStack(Material.CROSSBOW);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.displayName(MiniMessage.miniMessage().deserialize(plugin.trText(null, "item.crossbow.name"))
                    .decoration(TextDecoration.ITALIC, false));
            String loreStr = plugin.trText(null, "item.crossbow.lore");
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
    public void register(HideAndSeek plugin) {
        int crossbowCooldown = plugin.getSettingRegistry().get("hider-items.crossbow.cooldown", 5);
        plugin.getCustomItemManager().registerItem(new CustomItemBuilder(createItem(plugin), getId())
                .withAction(ItemActionType.SHOOT, context -> {
                })
                .withDescription(getDescription(plugin, null))
                .withNameKey("item.crossbow.name")
                .withLoreKey("item.crossbow.lore")
                .withDropPrevention(true)
                .withCraftPrevention(true)
                .withVanillaCooldown(crossbowCooldown * 20)
                .withCustomCooldown(crossbowCooldown * 1000L)
                .withVanillaCooldownDisplay(true)
                .allowOffHand(false)
                .allowArmor(false)
                .cancelDefaultAction(false)
                .build());
    }

    @Override
    public Set<String> getConfigKeys() {
        return Set.of("hider-items.crossbow.cooldown");
    }

    @Override
    public String getDescription(HideAndSeek plugin, @Nullable Player player) {
        int points = plugin.getPointService().getInt("points.hider.sharpshooter.amount", 20);
        return plugin.trText(player, "item.crossbow.description",
                java.util.Map.of("points", String.valueOf(points)));
    }

    private static void upgradeLoadoutItems(Player player, HideAndSeek plugin) {
        var loadout = plugin.getLoadoutManager().getLoadout(player.getUniqueId());
        var hiderItems = loadout.getHiderItems();

        boolean hasSpeedBoost = hiderItems.contains(de.thecoolcraft11.hideAndSeek.model.LoadoutItemType.SPEED_BOOST);
        boolean hasKnockbackStick = hiderItems.contains(
                de.thecoolcraft11.hideAndSeek.model.LoadoutItemType.KNOCKBACK_STICK);

        if (hasSpeedBoost) {
            SpeedBoostItem.upgradeSpeedItem(player, plugin);
        }

        if (hasKnockbackStick) {
            KnockbackStickItem.upgradeKnockbackItem(player, plugin);
        }

        if (!hasSpeedBoost && !hasKnockbackStick) {
            player.sendMessage(plugin.trText(player, "item.crossbow.messages.no_upgrade_items"));
        }
    }
}
