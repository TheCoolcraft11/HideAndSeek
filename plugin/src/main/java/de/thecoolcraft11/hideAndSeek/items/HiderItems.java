package de.thecoolcraft11.hideAndSeek.items;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.items.api.GameItem;
import de.thecoolcraft11.hideAndSeek.items.hider.*;
import de.thecoolcraft11.hideAndSeek.listener.player.HiderEquipmentChangeListener;
import de.thecoolcraft11.hideAndSeek.model.LoadoutItemType;
import de.thecoolcraft11.hideAndSeek.util.CustomModelDataUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public final class HiderItems {


    private static final Map<String, GameItem> ITEM_REGISTRY = new LinkedHashMap<>();

    static {

        add(new SoundItem());
        add(new ExplosionItem());
        add(new RandomBlockItem());
        add(new SpeedBoostItem());
        add(new TrackerCrossbowItem());
        add(new BlockSelectorItem());
        add(new AppearanceItem());
        add(new KnockbackStickItem());
        add(new BlockSwapItem());
        add(new BigFirecrackerItem());
        add(new FireworkRocketItem());
        add(new MedkitItem());
        add(new TotemItem());
        add(new InkFaceItem());
        add(new InvisibilityCloakItem());
        add(new SlownessBallItem());
        add(new SmokeBombItem());
        add(new GhostEssenceItem());
        add(new RemoteGatewayItem());
    }

    private static void add(GameItem item) {
        ITEM_REGISTRY.put(item.getId(), item);
    }

    public static void registerItems(HideAndSeek plugin) {
        AtomicInteger registered = new AtomicInteger(0);
        AtomicInteger failed = new AtomicInteger(0);
        ITEM_REGISTRY.values().forEach(item -> {
            if (item.getId() != null && !item.getId().isEmpty() && item.createItem(plugin) != null) {
                item.register(plugin);
                registered.getAndIncrement();
            } else {
                plugin.getLogger().warning("Failed to register item: " + item.getClass().getSimpleName() + " - Invalid ID or null ItemStack");
                failed.incrementAndGet();
            }
        });
        plugin.getLogger().info("Registered " + registered.get() + " hider items");
        if (failed.get() > 0)
            plugin.getLogger().warning("Failed to register " + failed.get() + " hider items. Check previous warnings for details.");
    }

    public static void reregisterSpecificItem(String configKey, HideAndSeek plugin) {
        ITEM_REGISTRY.values().stream()
                .filter(item -> item.getConfigKeys().contains(configKey))
                .forEach(item -> {
                    item.getAllIds().forEach(id -> plugin.getCustomItemManager().unregisterItem(id));

                    item.register(plugin);
                    if (plugin.getDebugSettings().isVerboseLoggingEnabled()) {
                        plugin.getLogger().info("Targeted refresh for: " + item.getId());
                    }
                });
    }

    public static void giveItems(Player player, HideAndSeek plugin, boolean isHiding) {
        removeItems(player);

        var gameModeResult = plugin.getSettingService().getSetting("game.mode");
        Object gameModeObj = gameModeResult.isSuccess() ? gameModeResult.getValue() : null;

        if (!isHiding) {
            giveLoadoutItems(player, plugin);
            ensureArrow(player);
        }

        if (gameModeObj != null && gameModeObj.toString().equals("BLOCK")) {
            int appearanceSlot = isHiding ? 7 : 8;
            if (HiderItemUtil.hasCustomizableBlock(player, plugin)) {
                ItemStack appearance = plugin.getCustomItemManager().getIdentifiedItemStack(AppearanceItem.ID, player);
                CustomModelDataUtil.setCustomModelData(appearance, AppearanceItem.ID);
                player.getInventory().setItem(appearanceSlot, appearance);
            } else {
                player.getInventory().setItem(appearanceSlot, new ItemStack(Material.AIR));
            }
        }

        if (isHiding && (gameModeObj != null && gameModeObj.toString().equals("BLOCK"))) {
            ItemStack selector = plugin.getCustomItemManager().getIdentifiedItemStack(BlockSelectorItem.ID, player);
            CustomModelDataUtil.setCustomModelData(selector, BlockSelectorItem.ID);
            player.getInventory().setItem(8, selector);
        }

        HiderEquipmentChangeListener.hideHandItem(player, EquipmentSlot.HAND);
        HiderEquipmentChangeListener.hideHandItem(player, EquipmentSlot.OFF_HAND);
    }

    public static void giveLoadoutItems(Player player, HideAndSeek plugin) {
        plugin.getLoadoutManager().sanitizePlayerLoadout(player.getUniqueId());
        var loadout = plugin.getLoadoutManager().getLoadout(player.getUniqueId());

        Set<LoadoutItemType> itemsToGive = loadout.getHiderItems();

        if (itemsToGive.isEmpty()) {
            if (plugin.getDebugSettings().isVerboseLoggingEnabled()) {
                plugin.getLogger().info("No loadout selected for " + player.getName() + ", using defaults");
            }
            itemsToGive = Set.of(
                    LoadoutItemType.CAT_SOUND,
                    LoadoutItemType.FIRECRACKER,
                    LoadoutItemType.SPEED_BOOST
            );
        } else {
            if (plugin.getDebugSettings().isVerboseLoggingEnabled()) {
                plugin.getLogger().info(player.getName() + " has custom loadout with " + itemsToGive.size() + " items");
            }
        }

        boolean hasValidItems = false;
        for (LoadoutItemType itemType : itemsToGive) {
            String itemId = itemType.getItemId();
            if (itemType == LoadoutItemType.SPEED_BOOST) {
                itemId = SpeedBoostItem.ID + "_" + SpeedBoostItem.getSpeedLevel(player.getUniqueId());
            } else if (itemType == LoadoutItemType.KNOCKBACK_STICK) {
                itemId = KnockbackStickItem.ID + "_" + KnockbackStickItem.getKnockbackLevel(player.getUniqueId());
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


        int slot = 0;
        java.util.Map<Integer, de.thecoolcraft11.hideAndSeek.model.ItemType> slotPreferences = loadout.getHiderSlotPreferences();
        java.util.Set<LoadoutItemType> placedItems = new java.util.HashSet<>();

        if (!slotPreferences.isEmpty()) {
            for (int currentSlot = 0; currentSlot < 9; currentSlot++) {
                de.thecoolcraft11.hideAndSeek.model.ItemType preference = slotPreferences.get(currentSlot);
                if (preference == null) continue;


                for (LoadoutItemType itemType : itemsToGive) {
                    if (placedItems.contains(itemType)) continue;
                    if (itemType.getItemType() != preference) continue;

                    String itemId = itemType.getItemId();
                    if (itemType == LoadoutItemType.SPEED_BOOST) {
                        itemId = SpeedBoostItem.ID + "_" + SpeedBoostItem.getSpeedLevel(player.getUniqueId());
                    } else if (itemType == LoadoutItemType.KNOCKBACK_STICK) {
                        itemId = KnockbackStickItem.ID + "_" + KnockbackStickItem.getKnockbackLevel(
                                player.getUniqueId());
                    }

                    ItemStack item = plugin.getCustomItemManager().getIdentifiedItemStack(itemId, player);
                    if (item != null) {
                        if (plugin.getDebugSettings().isVerboseLoggingEnabled()) {
                            plugin.getLogger().info(
                                    "Giving " + player.getName() + " item: " + itemType.name() + " (ID: " + itemId + ") in slot " + currentSlot + " (preference match)");
                        }
                        String selectedVariant = ItemSkinSelectionService.getSelectedVariant(player,
                                ItemSkinSelectionService.normalizeLogicalItemId(itemId));
                        CustomModelDataUtil.setCustomModelData(item, itemId, selectedVariant);
                        player.getInventory().setItem(currentSlot, item);
                        placedItems.add(itemType);
                        plugin.getCustomItemManager().resetPlayerUses(RandomBlockItem.ID, player.getUniqueId());
                        plugin.getCustomItemManager().resetPlayerUses(TotemItem.ID, player.getUniqueId());
                        break;
                    }
                }
            }
        }


        for (LoadoutItemType itemType : itemsToGive) {
            if (placedItems.contains(itemType)) continue;

            String itemId = itemType.getItemId();

            if (itemType == LoadoutItemType.SPEED_BOOST) {
                itemId = SpeedBoostItem.ID + "_" + SpeedBoostItem.getSpeedLevel(player.getUniqueId());
            } else if (itemType == LoadoutItemType.KNOCKBACK_STICK) {
                itemId = KnockbackStickItem.ID + "_" + KnockbackStickItem.getKnockbackLevel(player.getUniqueId());
            }


            while (slot < 9 && player.getInventory().getItem(slot) != null && !Objects.requireNonNull(
                    player.getInventory().getItem(
                            slot)).getType().isAir()) {
                slot++;
            }

            if (slot >= 9) break;

            if (plugin.getDebugSettings().isVerboseLoggingEnabled()) {
                plugin.getLogger().info("Giving " + player.getName() + " item: " + itemType.name() + " (ID: " + itemId + ") in slot " + slot);
            }
            ItemStack item = plugin.getCustomItemManager().getIdentifiedItemStack(itemId, player);
            if (item != null) {
                String selectedVariant = ItemSkinSelectionService.getSelectedVariant(player, ItemSkinSelectionService.normalizeLogicalItemId(itemId));
                CustomModelDataUtil.setCustomModelData(item, itemId, selectedVariant);
                player.getInventory().setItem(slot++, item);
                if (plugin.getDebugSettings().isVerboseLoggingEnabled()) {
                    plugin.getLogger().info("  Item placed successfully");
                }

                plugin.getCustomItemManager().resetPlayerUses(RandomBlockItem.ID, player.getUniqueId());
                plugin.getCustomItemManager().resetPlayerUses(TotemItem.ID, player.getUniqueId());
            } else {
                plugin.getLogger().warning("  Item is NULL! Item not registered: " + itemId + " (skipping)");
            }
        }

        ItemSkinSelectionService.applySelectedVariants(player, plugin);
        if (plugin.getDebugSettings().isVerboseLoggingEnabled()) {
            plugin.getLogger().info("Finished giving loadout items to " + player.getName() + " (" + (slot) + " items placed)");
        }
    }

    public static void updateAppearanceItem(Player player, HideAndSeek plugin) {
        HiderItemUtil.updateAppearanceItem(player, plugin);
    }

    public static void ensureArrow(Player player) {
        if (player == null) {
            return;
        }
        if (!player.getInventory().contains(Material.ARROW)) {
            player.getInventory().setItem(18, new ItemStack(Material.ARROW, 1));
        }
    }

    public static void removeItems(Player player) {
        player.getInventory().clear();
    }

    public static void removeFromAllPlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            removeItems(player);
        }
    }

    public static void applyMask(Player player, HideAndSeek plugin) {
        InkFaceItem.applyMask(player, plugin);
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
