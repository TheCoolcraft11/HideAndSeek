package de.thecoolcraft11.hideAndSeek.items.hider;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.items.api.GameItem;
import de.thecoolcraft11.minigameframework.items.CustomItemBuilder;
import de.thecoolcraft11.minigameframework.items.ItemActionType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class TrackerCrossbowItem implements GameItem {
    public static final String ID = "has_hider_crossbow";

    private static final Map<UUID, Integer> trackerHits = new HashMap<>();

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public ItemStack createItem(HideAndSeek plugin) {
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

    @Override
    public String getDescription(HideAndSeek plugin) {
        int points = plugin.getPointService().getInt("points.hider.sharpshooter.amount", 20);
        return String.format("Hit seekers with arrows to upgrade items, grants %d points per hit.", points);
    }

    @Override
    public void register(HideAndSeek plugin) {
        int crossbowCooldown = plugin.getSettingRegistry().get("hider-items.crossbow.cooldown", 5);
        plugin.getCustomItemManager().registerItem(new CustomItemBuilder(createItem(plugin), getId())
                .withAction(ItemActionType.SHOOT, context -> {
                })
                .withDescription(getDescription(plugin))
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

    public static void onTrackerHit(Player hider, HideAndSeek plugin) {
        if (hider == null) {
            return;
        }

        int hitPoints = plugin.getPointService().award(hider.getUniqueId(), de.thecoolcraft11.hideAndSeek.util.points.PointAction.HIDER_SHARPSHOOTER);
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
        var hiderItems = loadout.getHiderItems();

        boolean hasSpeedBoost = hiderItems.contains(de.thecoolcraft11.hideAndSeek.model.LoadoutItemType.SPEED_BOOST);
        boolean hasKnockbackStick = hiderItems.contains(de.thecoolcraft11.hideAndSeek.model.LoadoutItemType.KNOCKBACK_STICK);

        if (hasSpeedBoost) {
            SpeedBoostItem.upgradeSpeedItem(player);
        }

        if (hasKnockbackStick) {
            KnockbackStickItem.upgradeKnockbackItem(player);
        }

        if (!hasSpeedBoost && !hasKnockbackStick) {
            player.sendMessage(Component.text("You don't have Speed Boost or Knockback Stick selected!", NamedTextColor.YELLOW));
        }
    }
}
