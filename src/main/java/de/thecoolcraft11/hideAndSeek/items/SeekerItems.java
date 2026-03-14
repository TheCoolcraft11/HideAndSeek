package de.thecoolcraft11.hideAndSeek.items;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.items.api.GameItem;
import de.thecoolcraft11.hideAndSeek.items.seeker.*;
import de.thecoolcraft11.hideAndSeek.model.LoadoutItemType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class SeekerItems {


    private static final Map<String, GameItem> ITEM_REGISTRY = new LinkedHashMap<>();

    static {

        add(new GrapplingHookItem());
        add(new InkSplashItem());
        add(new LightningFreezeItem());
        add(new GlowingCompassItem());
        add(new CurseSpellItem());
        add(new BlockRandomizerItem());
        add(new ChainPullItem());
        add(new ProximitySensorItem());
        add(new CageTrapItem());
        add(new SeekersSwordItem());
        add(new SeekersMaskItem());
        add(new BlockStatsItem());
    }

    private static void add(GameItem item) {
        ITEM_REGISTRY.put(item.getId(), item);
    }

    public static void registerItems(HideAndSeek plugin) {
        ITEM_REGISTRY.values().forEach(item -> {
            if (item.getId() != null && !item.getId().isEmpty() && item.createItem(plugin) != null) {
                item.register(plugin);
            } else {
                plugin.getLogger().warning("Failed to register item: " + item.getClass().getSimpleName() + " - Invalid ID or null ItemStack");
            }
        });
    }


    public static void reregisterSpecificItem(String configKey, HideAndSeek plugin) {
        ITEM_REGISTRY.values().stream()
                .filter(item -> item.getConfigKeys().contains(configKey))
                .forEach(item -> {
                    item.getAllIds().forEach(id -> plugin.getCustomItemManager().unregisterItem(id));

                    item.register(plugin);
                    plugin.getLogger().info("Targeted refresh for: " + item.getId());
                });
    }

    public static void giveItems(Player player, HideAndSeek plugin) {
        removeItems(player);

        ItemStack sword = plugin.getCustomItemManager().getIdentifiedItemStack(SeekersSwordItem.ID, player);
        player.getInventory().setItem(0, sword);

        giveLoadoutItems(player, plugin);
        ItemSkinSelectionService.applySelectedVariants(player, plugin);

        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, Integer.MAX_VALUE, 4, false, false, false));
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

                plugin.getLogger().info(" Item placed successfully");

            } else {

                plugin.getLogger().warning(" Item is NULL! Item not registered: " + itemId + " (skipping)");

            }

        }


        if (isBlockMode && blockStatsEnabled) {

            ItemStack blockStats = plugin.getCustomItemManager().getIdentifiedItemStack(BlockStatsItem.ID, player);

            if (blockStats != null) {

                player.getInventory().setItem(8, blockStats);

                plugin.getLogger().info("Gave permanent BlockStats item to " + player.getName() + " in slot 8");

            }

        }


        plugin.getLogger().info("Finished giving loadout items to " + player.getName() + " (" + (slot - 1) + " items placed)");

    }


    public static void applyMask(Player player, HideAndSeek plugin) {

        player.getInventory().setHelmet(plugin.getCustomItemManager().getIdentifiedItemStack(SeekersMaskItem.ID, player));

    }


    public static void removeItems(Player player) {

        player.getInventory().clear();


        player.removePotionEffect(PotionEffectType.REGENERATION);

    }


    public static void removeFromAllPlayers() {

        for (Player player : Bukkit.getOnlinePlayers()) {

            removeItems(player);

        }

    }

    public static void giveGrapplingHook(Player player, HideAndSeek plugin) {
        player.getInventory().setItem(1, plugin.getCustomItemManager().getIdentifiedItemStack(GrapplingHookItem.ID, player));
    }

    public static void giveBlockStats(Player player, HideAndSeek plugin) {
        boolean blockStatsEnabled = plugin.getSettingRegistry().get("blockstats.enabled", true);
        if (blockStatsEnabled) {
            player.getInventory().setItem(2, plugin.getCustomItemManager().getIdentifiedItemStack(BlockStatsItem.ID, player));
        }
    }

    public static Set<String> getAllConfigKeys() {
        return ITEM_REGISTRY.values().stream()
                .flatMap(item -> item.getConfigKeys().stream())
                .collect(Collectors.toSet());
    }

    public static GameItem getItem(String id) {
        return ITEM_REGISTRY.get(id);
    }
}