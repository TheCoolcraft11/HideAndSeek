package de.thecoolcraft11.hideAndSeek.items;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.items.api.GameItem;
import de.thecoolcraft11.hideAndSeek.items.seeker.*;
import de.thecoolcraft11.hideAndSeek.model.LoadoutItemType;
import de.thecoolcraft11.hideAndSeek.model.SlotPreference;
import de.thecoolcraft11.hideAndSeek.util.CustomModelDataUtil;
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
import java.util.concurrent.atomic.AtomicInteger;
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
        add(new CameraItem());
        add(new PhantomViewerItem());
        add(new CageTrapItem());
        add(new SeekerAssistantItem());
        add(new SeekersSwordItem());
        add(new SeekersMaskItem());
        add(new BlockStatsItem());
        add(new CrowbarItem());
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
                registered.incrementAndGet();
            } else {
                plugin.getLogger().warning("Failed to register item: " + item.getClass().getSimpleName() + " - Invalid ID or null ItemStack");
                failed.incrementAndGet();
            }
        });
        plugin.getLogger().info("Registered " + registered.get() + " seeker items");
        if (failed.get() > 0)
            plugin.getLogger().warning("Failed to register " + failed.get() + " seeker items. Check previous warnings for details.");
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

    public static void giveItems(Player player, HideAndSeek plugin) {
        removeItems(player);
        giveLoadoutItems(player, plugin);
        ItemSkinSelectionService.applySelectedVariants(player, plugin);
        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, Integer.MAX_VALUE, 4, false, false, false));
    }


    public static void giveLoadoutItems(Player player, HideAndSeek plugin) {

        plugin.getLoadoutManager().sanitizePlayerLoadout(player.getUniqueId());
        var loadout = plugin.getLoadoutManager().getLoadout(player.getUniqueId());

        var gameModeResult = plugin.getSettingService().getSetting("game.mode");
        Object gameModeObj = gameModeResult.isSuccess() ? gameModeResult.getValue() : null;
        var blockSmallResult = plugin.getSettingService().getSetting("game.block-form.scale-to-block");
        Object blockSmallObj = blockSmallResult.isSuccess() ? blockSmallResult.getValue() : null;
        var blockStatsEnabledResult = plugin.getSettingService().getSetting("game.blockstats.enabled");
        Object blockStatsEnabledObj = blockStatsEnabledResult.isSuccess() ? blockStatsEnabledResult.getValue() : null;
        var crowBarEnabledResult = plugin.getSettingService().getSetting("seeker-items.crowbar.enabled");
        Object crowbarEnabledObj = crowBarEnabledResult.isSuccess() ? crowBarEnabledResult.getValue() : null;
        boolean isBlockMode = gameModeObj != null && gameModeObj.toString().equals("BLOCK");
        boolean isSmallMode = gameModeObj != null && gameModeObj.toString().equals("SMALL");
        boolean isBlockSmall = blockSmallObj != null && blockSmallObj.toString().equals("true");
        boolean blockStatsEnabled = blockStatsEnabledObj != null && blockStatsEnabledObj.toString().equals("true");
        boolean crowBarEnabled = crowbarEnabledObj != null && crowbarEnabledObj.toString().equals("true");


        ItemStack sword = plugin.getCustomItemManager().getIdentifiedItemStack(SeekersSwordItem.ID, player);
        if (sword != null) {
            String selectedVariant = ItemSkinSelectionService.getSelectedVariant(player,
                    ItemSkinSelectionService.normalizeLogicalItemId(SeekersSwordItem.ID));
            CustomModelDataUtil.setCustomModelData(sword, SeekersSwordItem.ID, selectedVariant);
            player.getInventory().setItem(0, sword);
        }


        Set<LoadoutItemType> itemsToGive = loadout.getSeekerItems().stream()
                .filter(item -> !SeekersSwordItem.ID.equals(item.getItemId()))
                .collect(java.util.stream.Collectors.toSet());


        if (itemsToGive.isEmpty()) {
            if (plugin.getDebugSettings().isVerboseLoggingEnabled()) {
                plugin.getLogger().info("No loadout selected for " + player.getName() + ", using defaults");
            }

            itemsToGive = Set.of(LoadoutItemType.GRAPPLING_HOOK);
        } else {
            if (plugin.getDebugSettings().isVerboseLoggingEnabled()) {
                plugin.getLogger().info(player.getName() + " has custom loadout with " + itemsToGive.size() + " items");
            }
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
            itemsToGive = Set.of(LoadoutItemType.GRAPPLING_HOOK);
        }


        java.util.Map<Integer, SlotPreference> slotPreferences = loadout.getSeekerSlotPreferences();
        java.util.Set<LoadoutItemType> placedItems = new java.util.HashSet<>();


        if (!slotPreferences.isEmpty()) {
            for (int currentSlot = 1; currentSlot < 7; currentSlot++) {
                SlotPreference preference = slotPreferences.get(currentSlot);
                if (preference == null) continue;

                for (LoadoutItemType itemType : itemsToGive) {
                    if (placedItems.contains(itemType)) continue;
                    if (itemType.getItemType() != preference.primary()) continue;
                    String itemId = itemType.getItemId();
                    ItemStack item = plugin.getCustomItemManager().getIdentifiedItemStack(itemId, player);
                    if (item != null) {
                        if (plugin.getDebugSettings().isVerboseLoggingEnabled()) {
                            plugin.getLogger().info(
                                    "Giving " + player.getName() + " item: " + itemType.name() + " (ID: " + itemId + ") in slot " + currentSlot + " (primary preference match)");
                        }
                        String selectedVariant = ItemSkinSelectionService.getSelectedVariant(player,
                                ItemSkinSelectionService.normalizeLogicalItemId(itemId));
                        CustomModelDataUtil.setCustomModelData(item, itemId, selectedVariant);
                        player.getInventory().setItem(currentSlot, item);
                        placedItems.add(itemType);
                        break;
                    }
                }

                if (preference.hasFallback() && (player.getInventory().getItem(
                        currentSlot) == null || player.getInventory().getItem(currentSlot).getType().isAir())) {
                    for (LoadoutItemType itemType : itemsToGive) {
                        if (placedItems.contains(itemType)) continue;
                        if (itemType.getItemType() != preference.fallback()) continue;
                        String itemId = itemType.getItemId();
                        ItemStack item = plugin.getCustomItemManager().getIdentifiedItemStack(itemId, player);
                        if (item != null) {
                            if (plugin.getDebugSettings().isVerboseLoggingEnabled()) {
                                plugin.getLogger().info(
                                        "Giving " + player.getName() + " item: " + itemType.name() + " (ID: " + itemId + ") in slot " + currentSlot + " (fallback preference match)");
                            }
                            String selectedVariant = ItemSkinSelectionService.getSelectedVariant(player,
                                    ItemSkinSelectionService.normalizeLogicalItemId(itemId));
                            CustomModelDataUtil.setCustomModelData(item, itemId, selectedVariant);
                            player.getInventory().setItem(currentSlot, item);
                            placedItems.add(itemType);
                            break;
                        }
                    }
                }
            }
        }


        int slot = 1;
        for (LoadoutItemType itemType : itemsToGive) {
            if (placedItems.contains(itemType)) continue;
            String itemId = itemType.getItemId();
            while (slot < 7) {
                ItemStack slotItem = player.getInventory().getItem(slot);
                if (slotItem == null || slotItem.getType().isAir()) {
                    break;
                }
                slot++;
            }
            if (slot >= 7) break;
            if (plugin.getDebugSettings().isVerboseLoggingEnabled()) {
                plugin.getLogger().info("Giving " + player.getName() + " item: " + itemType.name() + " (ID: " + itemId + ") in slot " + slot);
            }
            ItemStack item = plugin.getCustomItemManager().getIdentifiedItemStack(itemId, player);
            if (item != null) {
                String selectedVariant = ItemSkinSelectionService.getSelectedVariant(player, ItemSkinSelectionService.normalizeLogicalItemId(itemId));
                CustomModelDataUtil.setCustomModelData(item, itemId, selectedVariant);
                player.getInventory().setItem(slot++, item);
                if (plugin.getDebugSettings().isVerboseLoggingEnabled()) {
                    plugin.getLogger().info(" Item placed successfully");
                }
            } else {
                plugin.getLogger().warning(" Item is NULL! Item not registered: " + itemId + " (skipping)");
            }
        }


        if (isBlockMode && blockStatsEnabled) {

            ItemStack blockStats = plugin.getCustomItemManager().getIdentifiedItemStack(BlockStatsItem.ID, player);

            if (blockStats != null) {
                CustomModelDataUtil.setCustomModelData(blockStats, BlockStatsItem.ID);

                player.getInventory().setItem(8, blockStats);

                if (plugin.getDebugSettings().isVerboseLoggingEnabled()) {
                    plugin.getLogger().info("Gave permanent BlockStats item to " + player.getName() + " in slot 8");
                }

            }

        }

        if (isSmallMode || (isBlockMode && isBlockSmall)) {
            if (!crowBarEnabled) return;
            ItemStack slot7Item = player.getInventory().getItem(7);
            if (slot7Item == null || slot7Item.getType().isAir()) {
                ItemStack blockStats = plugin.getCustomItemManager().getIdentifiedItemStack(CrowbarItem.ID, player);
                if (blockStats != null) {
                    CustomModelDataUtil.setCustomModelData(blockStats, CrowbarItem.ID);
                    player.getInventory().setItem(7, blockStats);
                    if (plugin.getDebugSettings().isVerboseLoggingEnabled()) {
                        plugin.getLogger().info("Gave Crowbar to " + player.getName() + " in slot 7");
                    }
                }
            }
        }


        if (plugin.getDebugSettings().isVerboseLoggingEnabled()) {
            plugin.getLogger().info("Finished giving loadout items to " + player.getName() + " (" + (slot - 1) + " items placed)");
        }

    }


    public static void applyMask(Player player, HideAndSeek plugin) {

        player.getInventory().setHelmet(plugin.getCustomItemManager().getIdentifiedItemStack(SeekersMaskItem.ID, player));
        CustomModelDataUtil.setForInventorySlot(player.getInventory(), 39, SeekersMaskItem.ID, null);

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

    public static void giveItemsWithLoadout(Player player, HideAndSeek plugin) {
        removeItems(player);



        giveLoadoutItems(player, plugin);


        ItemSkinSelectionService.applySelectedVariants(player, plugin);

        var gameModeResult = plugin.getSettingService().getSetting("game.mode");
        Object gameModeObj = gameModeResult.isSuccess() ? gameModeResult.getValue() : null;
        var blockSmallResult = plugin.getSettingService().getSetting("game.block-form.scale-to-block");
        Object blockSmallObj = blockSmallResult.isSuccess() ? blockSmallResult.getValue() : null;
        var blockStatsEnabledResult = plugin.getSettingService().getSetting("game.blockstats.enabled");
        Object blockStatsEnabledObj = blockStatsEnabledResult.isSuccess() ? blockStatsEnabledResult.getValue() : null;
        var crowBarEnabledResult = plugin.getSettingService().getSetting("seeker-items.crowbar.enabled");
        Object crowbarEnabledObj = crowBarEnabledResult.isSuccess() ? crowBarEnabledResult.getValue() : null;
        boolean isBlockMode = gameModeObj != null && gameModeObj.toString().equals("BLOCK");
        boolean isSmallMode = gameModeObj != null && gameModeObj.toString().equals("SMALL");
        boolean isBlockSmall = blockSmallObj != null && blockSmallObj.toString().equals("true");
        boolean blockStatsEnabled = blockStatsEnabledObj != null && blockStatsEnabledObj.toString().equals("true");
        boolean crowBarEnabled = crowbarEnabledObj != null && crowbarEnabledObj.toString().equals("true");



        if (isBlockMode) {
            if (blockStatsEnabled) {

                ItemStack slot8Item = player.getInventory().getItem(8);
                if (slot8Item == null || slot8Item.getType().isAir()) {
                    ItemStack blockStats = plugin.getCustomItemManager().getIdentifiedItemStack(BlockStatsItem.ID, player);
                    if (blockStats != null) {
                        CustomModelDataUtil.setCustomModelData(blockStats, BlockStatsItem.ID);
                        player.getInventory().setItem(8, blockStats);
                        if (plugin.getDebugSettings().isVerboseLoggingEnabled()) {
                            plugin.getLogger().info("Gave BlockStats to " + player.getName() + " in slot 8");
                        }
                    }
                }
            }
        }

        if (isSmallMode || (isBlockMode && isBlockSmall)) {
            if (!crowBarEnabled) return;
            ItemStack slot7Item = player.getInventory().getItem(7);
            if (slot7Item == null || slot7Item.getType().isAir()) {
                ItemStack blockStats = plugin.getCustomItemManager().getIdentifiedItemStack(CrowbarItem.ID, player);
                if (blockStats != null) {
                    CustomModelDataUtil.setCustomModelData(blockStats, CrowbarItem.ID);
                    player.getInventory().setItem(7, blockStats);
                    if (plugin.getDebugSettings().isVerboseLoggingEnabled()) {
                        plugin.getLogger().info("Gave Crowbar to " + player.getName() + " in slot 7");
                    }
                }
            }
        }


        player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, Integer.MAX_VALUE, 4, false, false, false));
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
