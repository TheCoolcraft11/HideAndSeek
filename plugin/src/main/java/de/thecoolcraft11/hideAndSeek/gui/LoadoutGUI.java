package de.thecoolcraft11.hideAndSeek.gui;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.gui.config.GUIItems;
import de.thecoolcraft11.hideAndSeek.gui.config.GUINames;
import de.thecoolcraft11.hideAndSeek.items.HiderItems;
import de.thecoolcraft11.hideAndSeek.items.SeekerItems;
import de.thecoolcraft11.hideAndSeek.items.api.GameItem;
import de.thecoolcraft11.hideAndSeek.loadout.AdminRolePreset;
import de.thecoolcraft11.hideAndSeek.loadout.LoadoutManager;
import de.thecoolcraft11.hideAndSeek.loadout.LoadoutRole;
import de.thecoolcraft11.hideAndSeek.loadout.PlayerLoadout;
import de.thecoolcraft11.hideAndSeek.model.ItemRarity;
import de.thecoolcraft11.hideAndSeek.model.LoadoutItemType;
import de.thecoolcraft11.hideAndSeek.util.CustomModelDataUtil;
import de.thecoolcraft11.minigameframework.inventory.FrameworkInventory;
import de.thecoolcraft11.minigameframework.inventory.InventoryItem;
import de.thecoolcraft11.minigameframework.translation.TranslationArguments;
import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.TooltipDisplay;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;


@SuppressWarnings("UnstableApiUsage")
public class LoadoutGUI {
    private final Map<UUID, GuiTab> currentTab = new HashMap<>();

    private final LoadoutManager loadoutManager;
    private final HideAndSeek plugin;

    public void open(Player player) {
        if (!loadoutManager.canModifyLoadout()) {
            player.sendMessage(plugin.tr(player, "gui.loadout.errors.only_lobby"));
            return;
        }

        currentTab.putIfAbsent(player.getUniqueId(), GuiTab.HIDER);
        openTab(player, currentTab.get(player.getUniqueId()));
    }

    private final Set<DataComponentType> ALL_TOOLTIP_COMPONENTS = Set.of(
            DataComponentTypes.ENCHANTMENTS,
            DataComponentTypes.STORED_ENCHANTMENTS,
            DataComponentTypes.ATTRIBUTE_MODIFIERS,
            DataComponentTypes.UNBREAKABLE,
            DataComponentTypes.CAN_BREAK,
            DataComponentTypes.CAN_PLACE_ON,
            DataComponentTypes.DYED_COLOR,
            DataComponentTypes.TRIM,
            DataComponentTypes.JUKEBOX_PLAYABLE,

            DataComponentTypes.BANNER_PATTERNS,
            DataComponentTypes.BLOCK_DATA,
            DataComponentTypes.BUNDLE_CONTENTS,
            DataComponentTypes.CHARGED_PROJECTILES,
            DataComponentTypes.CONTAINER,
            DataComponentTypes.CONTAINER_LOOT,
            DataComponentTypes.FIREWORK_EXPLOSION,
            DataComponentTypes.FIREWORKS,
            DataComponentTypes.INSTRUMENT,
            DataComponentTypes.MAP_ID,
            DataComponentTypes.PAINTING_VARIANT,
            DataComponentTypes.POT_DECORATIONS,
            DataComponentTypes.POTION_CONTENTS,
            DataComponentTypes.TROPICAL_FISH_PATTERN,
            DataComponentTypes.WRITTEN_BOOK_CONTENT
    );

    public LoadoutGUI(LoadoutManager loadoutManager, HideAndSeek plugin) {
        this.loadoutManager = loadoutManager;
        this.plugin = plugin;
    }

    private void openTab(Player player, GuiTab tab) {
        currentTab.put(player.getUniqueId(), tab);
        if (tab == GuiTab.PRESETS) {
            openPresetsView(player);
            return;
        }
        boolean hiderView = tab == GuiTab.HIDER;
        openView(player, hiderView);
    }

    private void openView(Player player, boolean hiderView) {
        de.thecoolcraft11.minigameframework.inventory.InventoryBuilder builder =
                new de.thecoolcraft11.minigameframework.inventory.InventoryBuilder(plugin.getInventoryFramework())
                        .id("loadout_" + player.getUniqueId() + "_" + (hiderView ? "hider" : "seeker"))
                        .title(hiderView ? plugin.trText(player, "gui.loadout.title.hider") : plugin.trText(player,
                                "gui.loadout.title.seeker"))
                        .rows(6)
                        .allowOutsideClicks(false)
                        .allowDrag(false)
                        .allowPlayerInventoryInteraction(false);

        FrameworkInventory inv = builder.build();

        PlayerLoadout loadout = loadoutManager.getLoadout(player.getUniqueId());
        int maxItems = hiderView ? loadoutManager.getMaxHiderItems() : loadoutManager.getMaxSeekerItems();
        int maxTokens = hiderView ? loadoutManager.getMaxHiderTokens() : loadoutManager.getMaxSeekerTokens();
        int usedTokens = hiderView ? loadout.getHiderTokensUsed() : loadout.getSeekerTokensUsed();
        Set<LoadoutItemType> selected = hiderView ? loadout.getHiderItems() : loadout.getSeekerItems();
        LoadoutRole role = hiderView ? LoadoutRole.HIDER : LoadoutRole.SEEKER;

        if (isCustomEditingBlocked(role)) {
            InventoryItem lockedInfo = new InventoryItem(createRestrictedInfoItem(player, role));
            lockedInfo.setClickHandler((p, item, event, slot) -> event.setCancelled(true));
            lockedInfo.setAllowTakeout(false);
            lockedInfo.setAllowInsert(false);
            inv.setItem(4, lockedInfo);

            renderAdminPresetChoices(inv, player, role);
            renderBottomTabs(inv, player, hiderView ? GuiTab.HIDER : GuiTab.SEEKER);
            plugin.getInventoryFramework().openInventory(player, inv);
            return;
        }


        InventoryItem infoItem = new InventoryItem(
                createInfoItem(player, selected.size(), maxItems, usedTokens, maxTokens));
        infoItem.setClickHandler((p, item, event, slot) -> event.setCancelled(true));
        infoItem.setAllowTakeout(false);
        infoItem.setAllowInsert(false);
        infoItem.setMetadata("type", "info");
        inv.setItem(4, infoItem);


        int slot = 9;
        for (LoadoutItemType item : Arrays.stream(LoadoutItemType.values()).toList()) {
            if (hiderView && !item.isForHiders()) continue;
            if (!hiderView && !item.isForSeekers()) continue;
            if (!loadoutManager.isItemAvailableForRole(role, item)) continue;

            int cost = loadoutManager.getItemCost(item);
            boolean isSelected = selected.contains(item);
            InventoryItem catalogItem = new InventoryItem(
                    createItemStack(player, item, cost, isSelected, usedTokens, maxTokens, selected.size(), maxItems));
            catalogItem.setClickHandler((p, invItem, event, s) -> {
                handleCatalogSelection(p, hiderView, item);
                event.setCancelled(true);
            });
            catalogItem.setAllowTakeout(false);
            catalogItem.setAllowInsert(false);
            catalogItem.setMetadata("item_type", item.name());
            catalogItem.setMetadata("item_cost", cost);
            catalogItem.setMetadata("is_selected", isSelected);
            inv.setItem(slot++, catalogItem);
        }


        int selectedSlot = 45;
        int displayedCount = 0;
        for (LoadoutItemType item : new ArrayList<>(selected)) {
            if (displayedCount >= 5) break;
            int cost = loadoutManager.getItemCost(item);
            InventoryItem selectedItem = new InventoryItem(createSelectedItemDisplay(player, item, cost));
            selectedItem.setClickHandler((p, invItem, event, s) -> {
                handleSelectedRemoval(p, hiderView, item);
                event.setCancelled(true);
            });
            selectedItem.setAllowTakeout(false);
            selectedItem.setAllowInsert(false);
            selectedItem.setMetadata("selected_item", item.name());
            selectedItem.setMetadata("removable", true);
            inv.setItem(selectedSlot++, selectedItem);
            displayedCount++;
        }


        InventoryItem slotPrefsButton = new InventoryItem(createUtilityItem(GUIItems.L_SLOT_PREFS,
                plugin.tr(player, "gui.loadout.slotprefs.title"),
                List.of(
                        plugin.tr(player, "gui.loadout.slotprefs.hint1").decoration(TextDecoration.ITALIC, false),
                        plugin.tr(player, "gui.loadout.slotprefs.hint2").decoration(TextDecoration.ITALIC, false)
                ), new ItemStack(Material.HOPPER)));
        slotPrefsButton.setClickHandler((p, item, event, s) -> {
            SlotPreferencesGUI gui = new SlotPreferencesGUI(loadoutManager, plugin);
            gui.open(p, hiderView);
            event.setCancelled(true);
        });
        slotPrefsButton.setAllowTakeout(false);
        slotPrefsButton.setAllowInsert(false);
        inv.setItem(53, slotPrefsButton);

        renderBottomTabs(inv, player, hiderView ? GuiTab.HIDER : GuiTab.SEEKER);

        plugin.getInventoryFramework().openInventory(player, inv);
    }

    private void openPresetsView(Player player) {
        FrameworkInventory inv = new de.thecoolcraft11.minigameframework.inventory.InventoryBuilder(
                plugin.getInventoryFramework())
                .id("loadout_presets_" + player.getUniqueId() + "_combined")
                .title(plugin.trText(player, "gui.loadout.presets.title"))
                .rows(6)
                .allowOutsideClicks(false)
                .allowDrag(false)
                .allowPlayerInventoryInteraction(false)
                .build();

        PlayerLoadout loadout = loadoutManager.getLoadout(player.getUniqueId());
        int totalCount = loadout.getHiderItems().size() + loadout.getSeekerItems().size();

        InventoryItem info = new InventoryItem(createPresetInfoItem(player, totalCount));
        info.setClickHandler((p, item, event, slot) -> event.setCancelled(true));
        info.setAllowTakeout(false);
        info.setAllowInsert(false);
        inv.setItem(4, info);

        int[] presetSlots = {19, 20, 22, 24, 25};
        for (int presetSlot = 1; presetSlot <= PlayerLoadout.MAX_PRESETS; presetSlot++) {
            int guiSlot = presetSlots[presetSlot - 1];
            boolean hasPreset = loadoutManager.hasPreset(player.getUniqueId(), presetSlot);
            LoadoutManager.PresetLoadResult analysis = loadoutManager.analyzePresetLoad(player.getUniqueId(),
                    presetSlot);

            InventoryItem presetItem = new InventoryItem(createPresetItem(player, presetSlot, hasPreset, analysis));
            int finalPresetSlot = presetSlot;
            presetItem.setClickHandler((p, item, event, slot) -> {
                if (isCustomEditingBlocked(LoadoutRole.HIDER) || isCustomEditingBlocked(LoadoutRole.SEEKER)) {
                    p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    p.sendMessage(plugin.tr(p, "gui.loadout.errors.custom_blocked"));
                    openPresetsView(p);
                    event.setCancelled(true);
                    return;
                }

                if (loadoutManager.isGlobalLoadoutLocked()) {
                    p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    p.sendMessage(plugin.tr(p, "gui.loadout.errors.global_locked"));
                    openPresetsView(p);
                    event.setCancelled(true);
                    return;
                }

                if (event.getClick() == ClickType.SHIFT_LEFT) {
                    loadoutManager.savePreset(p.getUniqueId(), finalPresetSlot);
                    p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
                    p.sendMessage(
                            plugin.tr(p, "gui.loadout.presets.saved", Map.of("slot", String.valueOf(finalPresetSlot))));
                    openPresetsView(p);
                    event.setCancelled(true);
                    return;
                }

                if (event.getClick().isRightClick()) {
                    boolean removed = loadoutManager.deletePreset(p.getUniqueId(), finalPresetSlot);
                    p.playSound(p.getLocation(), removed ? Sound.ENTITY_ITEM_BREAK : Sound.ENTITY_VILLAGER_NO, 1.0f,
                            1.0f);
                    p.sendMessage(
                            plugin.tr(p, removed ? "gui.loadout.presets.deleted" : "gui.loadout.presets.already_empty",
                                    Map.of("slot", String.valueOf(finalPresetSlot))));
                    openPresetsView(p);
                    event.setCancelled(true);
                    return;
                }

                LoadoutManager.PresetLoadResult result = loadoutManager.loadPreset(p.getUniqueId(), finalPresetSlot);
                if (!result.presetExists()) {
                    p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    p.sendMessage(plugin.tr(p, "gui.loadout.presets.load_empty",
                            Map.of("slot", String.valueOf(finalPresetSlot))));
                    openPresetsView(p);
                    event.setCancelled(true);
                    return;
                }

                if (result.isEmptyPreset()) {
                    p.sendMessage(plugin.tr(p, "gui.loadout.presets.load_empty",
                            Map.of("slot", String.valueOf(finalPresetSlot))));
                } else if (result.isFullyApplied()) {
                    p.sendMessage(plugin.tr(p, "gui.loadout.presets.load_success",
                            Map.of("slot", String.valueOf(finalPresetSlot))));
                } else {
                    p.sendMessage(plugin.tr(p, "gui.loadout.presets.load_partial", Map.of(
                            "slot", String.valueOf(finalPresetSlot),
                            "policy", String.valueOf(result.blockedByPolicy()),
                            "limits", String.valueOf(result.blockedByLimits())
                    )));
                }

                p.playSound(p.getLocation(),
                        result.isFullyApplied() ? Sound.ENTITY_PLAYER_LEVELUP : Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                openPresetsView(p);
                event.setCancelled(true);
            });
            presetItem.setAllowTakeout(false);
            presetItem.setAllowInsert(false);
            inv.setItem(guiSlot, presetItem);
        }

        InventoryItem roleToggle = new InventoryItem(createUtilityItem(GUIItems.L_PRESET_INFO,
                plugin.tr(player, "gui.loadout.presets.editing_all"),
                List.of(
                        plugin.tr(player, "gui.loadout.presets.hint_both_roles").decoration(TextDecoration.ITALIC,
                                false),
                        plugin.tr(player, "gui.loadout.presets.hint_shift_save").decoration(TextDecoration.ITALIC,
                                false),
                        plugin.tr(player, "gui.loadout.presets.hint_actions").decoration(TextDecoration.ITALIC, false)
                ), new ItemStack(Material.COMPASS)));
        roleToggle.setClickHandler((p, item, event, slot) -> event.setCancelled(true));
        roleToggle.setAllowTakeout(false);
        roleToggle.setAllowInsert(false);
        inv.setItem(49, roleToggle);

        renderBottomTabs(inv, player, GuiTab.PRESETS);
        plugin.getInventoryFramework().openInventory(player, inv);
    }

    private void handleSelectedRemoval(Player player, boolean isHiderView, LoadoutItemType itemToRemove) {
        LoadoutRole role = isHiderView ? LoadoutRole.HIDER : LoadoutRole.SEEKER;
        if (isCustomEditingBlocked(role)) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            player.sendMessage(plugin.tr(player, "gui.loadout.errors.restricted_role"));
            openView(player, isHiderView);
            return;
        }

        if (loadoutManager.isGlobalLoadoutLocked()) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            player.sendMessage(plugin.tr(player, "gui.loadout.errors.global_locked"));
            openView(player, isHiderView);
            return;
        }

        if (loadoutManager.isRoleLocked(player.getUniqueId(), role)) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            player.sendMessage(plugin.tr(player, "gui.loadout.errors.role_locked",
                    Map.of("role", isHiderView ? "hider" : "seeker")));
            openView(player, isHiderView);
            return;
        }

        PlayerLoadout loadout = loadoutManager.getLoadout(player.getUniqueId());
        if (isHiderView) {
            loadout.removeHiderItem(itemToRemove);
        } else {
            loadout.removeSeekerItem(itemToRemove);
        }
        loadoutManager.saveLoadout(player.getUniqueId());

        int cost = loadoutManager.getItemCost(itemToRemove);
        player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 0.8f);
        player.sendMessage(plugin.tr(player, "gui.loadout.item.removed", Map.of(
                "name", getItemDisplayName(player, itemToRemove.getItemId()),
                "rarity", getRarityTag(itemToRemove.getRarity()),
                "cost", String.valueOf(cost)
        )));

        openView(player, isHiderView);
    }

    private void handleCatalogSelection(Player player, boolean isHiderView, LoadoutItemType clickedItem) {
        LoadoutRole role = isHiderView ? LoadoutRole.HIDER : LoadoutRole.SEEKER;
        if (isCustomEditingBlocked(role)) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            player.sendMessage(plugin.tr(player, "gui.loadout.errors.restricted_role"));
            openView(player, isHiderView);
            return;
        }

        if (loadoutManager.isGlobalLoadoutLocked()) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            player.sendMessage(plugin.tr(player, "gui.loadout.errors.global_locked"));
            openView(player, isHiderView);
            return;
        }

        if (loadoutManager.isRoleLocked(player.getUniqueId(), role)) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            player.sendMessage(plugin.tr(player, "gui.loadout.errors.role_locked",
                    Map.of("role", isHiderView ? "hider" : "seeker")));
            openView(player, isHiderView);
            return;
        }
        if (!loadoutManager.isItemAvailableForRole(role, clickedItem)) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            player.sendMessage(plugin.tr(player, "gui.loadout.errors.item_disabled"));
            openView(player, isHiderView);
            return;
        }

        PlayerLoadout loadout = loadoutManager.getLoadout(player.getUniqueId());
        int cost = loadoutManager.getItemCost(clickedItem);

        if (isHiderView) {
            if (loadout.getHiderItems().contains(clickedItem)) {
                loadout.removeHiderItem(clickedItem);
                loadoutManager.clearPlayerSelectedAdminPreset(player.getUniqueId(), LoadoutRole.HIDER);
                loadoutManager.saveLoadout(player.getUniqueId());
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 0.8f);
                player.sendMessage(plugin.tr(player, "gui.loadout.item.removed", Map.of(
                        "name", getItemDisplayName(player, clickedItem.getItemId()),
                        "rarity", getRarityTag(clickedItem.getRarity()),
                        "cost", String.valueOf(cost)
                )));
            } else {
                if (loadout.addHiderItem(clickedItem, loadoutManager.getMaxHiderItems(),
                        loadoutManager.getMaxHiderTokens(), cost)) {
                    loadoutManager.clearPlayerSelectedAdminPreset(player.getUniqueId(), LoadoutRole.HIDER);
                    loadoutManager.saveLoadout(player.getUniqueId());
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
                    player.sendMessage(plugin.tr(player, "gui.loadout.item.added", Map.of(
                            "name", getItemDisplayName(player, clickedItem.getItemId()),
                            "rarity", getRarityTag(clickedItem.getRarity()),
                            "cost", String.valueOf(cost)
                    )));
                } else {
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    if (loadout.getHiderItems().size() >= loadoutManager.getMaxHiderItems()) {
                        player.sendMessage(plugin.tr(player, "gui.loadout.item.max_reached"));
                    } else {
                        player.sendMessage(plugin.tr(player, "gui.loadout.item.not_enough_tokens"));
                    }
                }
            }
        } else {
            if (loadout.getSeekerItems().contains(clickedItem)) {
                loadout.removeSeekerItem(clickedItem);
                loadoutManager.clearPlayerSelectedAdminPreset(player.getUniqueId(), LoadoutRole.SEEKER);
                loadoutManager.saveLoadout(player.getUniqueId());
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 0.8f);
                player.sendMessage(plugin.tr(player, "gui.loadout.item.removed", Map.of(
                        "name", getItemDisplayName(player, clickedItem.getItemId()),
                        "rarity", getRarityTag(clickedItem.getRarity()),
                        "cost", String.valueOf(cost)
                )));
            } else {
                if (loadout.addSeekerItem(clickedItem, loadoutManager.getMaxSeekerItems(),
                        loadoutManager.getMaxSeekerTokens(), cost)) {
                    loadoutManager.clearPlayerSelectedAdminPreset(player.getUniqueId(), LoadoutRole.SEEKER);
                    loadoutManager.saveLoadout(player.getUniqueId());
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
                    player.sendMessage(plugin.tr(player, "gui.loadout.item.added", Map.of(
                            "name", getItemDisplayName(player, clickedItem.getItemId()),
                            "rarity", getRarityTag(clickedItem.getRarity()),
                            "cost", String.valueOf(cost)
                    )));
                } else {
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    if (loadout.getSeekerItems().size() >= loadoutManager.getMaxSeekerItems()) {
                        player.sendMessage(plugin.tr(player, "gui.loadout.item.max_reached"));
                    } else {
                        player.sendMessage(plugin.tr(player, "gui.loadout.item.not_enough_tokens"));
                    }
                }
            }
        }

        openView(player, isHiderView);
    }

    private void renderBottomTabs(FrameworkInventory inv, Player player, GuiTab activeTab) {
        inv.setItem(50, buildTabButton(player, GuiTab.HIDER, activeTab));
        inv.setItem(51, buildTabButton(player, GuiTab.SEEKER, activeTab));
        inv.setItem(52, buildTabButton(player, GuiTab.PRESETS, activeTab));
    }


    private ItemStack createInfoItem(Player player, int usedSlots, int maxSlots, int usedTokens, int maxTokens) {
        ItemStack item = item(GUIItems.KEY_INFO, new ItemStack(Material.BOOK));
        ItemMeta meta = item.getItemMeta();
        meta.displayName(plugin.tr(player, "gui.loadout.info.title").decoration(TextDecoration.ITALIC, false));
        meta.lore(Arrays.asList(
                plugin.tr(player, "gui.loadout.info.items",
                        Map.of("used", String.valueOf(usedSlots), "max", String.valueOf(maxSlots), "color",
                                usedSlots >= maxSlots ? "red" : "green")).decoration(TextDecoration.ITALIC, false),
                plugin.tr(player, "gui.loadout.info.tokens",
                        Map.of("used", String.valueOf(usedTokens), "max", String.valueOf(maxTokens), "color",
                                usedTokens >= maxTokens ? "red" : "green")).decoration(TextDecoration.ITALIC, false)
        ));
        item.setItemMeta(meta);
        return item;
    }

    private InventoryItem buildTabButton(Player player, GuiTab tab, GuiTab activeTab) {
        Material material = switch (tab) {
            case HIDER -> Material.BLUE_CONCRETE;
            case SEEKER -> Material.RED_CONCRETE;
            case PRESETS -> Material.BOOKSHELF;
        };
        String key = switch (tab) {
            case HIDER -> "hider";
            case SEEKER -> "seeker";
            case PRESETS -> "presets";
        };
        boolean active = tab == activeTab;

        Component titleComp = plugin.tr(player, "gui.loadout.tabs." + key);
        if (active) titleComp = titleComp.append(plugin.tr(player, "gui.loadout.tabs.selected"));

        ItemStack stack = createUtilityItem(GUIItems.L_TAB_BASE + key,
                titleComp,
                List.of(plugin.tr(player, "gui.loadout.tabs.click_to_switch").decoration(TextDecoration.ITALIC,
                        false)),
                new ItemStack(material));

        if (active) {
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                meta.setEnchantmentGlintOverride(true);
                stack.setItemMeta(meta);
            }
        }

        InventoryItem button = new InventoryItem(stack);
        button.setClickHandler((p, item, event, slot) -> {
            openTab(p, tab);
            event.setCancelled(true);
        });
        button.setAllowTakeout(false);
        button.setAllowInsert(false);
        return button;
    }

    private boolean isCustomEditingBlocked(LoadoutRole role) {
        return loadoutManager.isRoleRestrictedToAdminPresets(role) || loadoutManager.getForcedRolePresetSlot(role) > 0;
    }

    private ItemStack createRestrictedInfoItem(Player player, LoadoutRole role) {
        int forcedSlot = loadoutManager.getForcedRolePresetSlot(role);
        List<Component> lore = new ArrayList<>();
        if (forcedSlot > 0) {
            lore.add(plugin.tr(player, "gui.loadout.restricted.forced",
                    Map.of("slot", String.valueOf(forcedSlot))).decoration(TextDecoration.ITALIC, false));
        } else {
            lore.add(plugin.tr(player, "gui.loadout.restricted.disabled").decoration(TextDecoration.ITALIC, false));
            lore.add(plugin.tr(player, "gui.loadout.restricted.pick_preset").decoration(TextDecoration.ITALIC, false));
        }
        lore.add(plugin.tr(player, "gui.loadout.restricted.note").decoration(TextDecoration.ITALIC, false));
        return createUtilityItem(GUIItems.L_RESTRICTED_ITEM_INFO,
                plugin.tr(player, "gui.loadout.restricted.title", Map.of("role", role.name())), lore,
                new ItemStack(Material.BOOK));
    }

    private void renderAdminPresetChoices(FrameworkInventory inv, Player player, LoadoutRole role) {
        int[] presetSlots = {19, 20, 22, 24, 25};
        int selectedSlot = loadoutManager.getLoadout(player.getUniqueId()).getSelectedAdminPresetSlot(role);
        int forcedSlot = loadoutManager.getForcedRolePresetSlot(role);

        for (int slot = 1; slot <= PlayerLoadout.MAX_PRESETS; slot++) {
            int guiSlot = presetSlots[slot - 1];
            AdminRolePreset preset = loadoutManager.getAdminPreset(role, slot);
            boolean enabled = loadoutManager.isAdminPresetEnabled(role, slot);
            boolean selected = selectedSlot == slot;
            boolean forced = forcedSlot == slot;

            ItemStack stack;
            if (preset.getItems().isEmpty()) {
                stack = createUtilityItem(GUIItems.L_ADMIN_PRESET_EMPTY,
                        plugin.tr(player, "gui.loadout.presets.slot_empty", Map.of("slot", String.valueOf(slot))),
                        List.of(plugin.tr(player, "gui.loadout.presets.empty_hint").decoration(TextDecoration.ITALIC,
                                false)),
                        new ItemStack(Material.GRAY_STAINED_GLASS_PANE));
            } else if (!enabled) {
                stack = createUtilityItem(GUIItems.L_ADMIN_PRESET_DISABLED,
                        plugin.tr(player, "gui.loadout.presets.slot_disabled", Map.of("slot", String.valueOf(slot))),
                        List.of(plugin.tr(player, "gui.loadout.presets.disabled_hint").decoration(TextDecoration.ITALIC,
                                false)),
                        new ItemStack(Material.BARRIER));
            } else {
                LoadoutItemType preview = preset.getItems().stream().findFirst().orElse(null);
                stack = preview == null ? item(GUIItems.L_ADMIN_PRESET_FALLBACK,
                        new ItemStack(Material.CHEST)) : getPreviewItemStack(preview);
                ItemMeta meta = stack.getItemMeta();
                if (meta != null) {
                    Component titleComp = plugin.tr(player,
                            forced ? "gui.loadout.presets.slot_forced" : selected ? "gui.loadout.presets.slot_selected" : "gui.loadout.presets.slot",
                            Map.of("slot", String.valueOf(slot)));
                    meta.displayName(titleComp.decoration(TextDecoration.ITALIC, false));
                    List<Component> lore = new ArrayList<>();
                    lore.add(plugin.tr(player, "gui.loadout.presets.items",
                            Map.of("count", String.valueOf(preset.getItems().size()))).decoration(TextDecoration.ITALIC,
                            false));
                    lore.add((forced ? plugin.tr(player, "gui.loadout.presets.forced_note") : plugin.tr(player,
                            "gui.loadout.presets.click_to_apply")).decoration(TextDecoration.ITALIC, false));
                    meta.lore(lore);

                    meta.setEnchantmentGlintOverride(selected || forced);

                    stack.setItemMeta(meta);
                }
                if (preview != null) {
                    CustomModelDataUtil.setCustomModelData(stack, resolveRuntimeItemId(preview), null);
                }
            }

            InventoryItem invItem = new InventoryItem(stack);
            int targetSlot = slot;
            invItem.setClickHandler((p, item, event, s) -> {
                if (forcedSlot > 0 && forcedSlot != targetSlot) {
                    p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    p.sendMessage(plugin.tr(p, "gui.loadout.presets.forced_active"));
                    openView(p, role == LoadoutRole.HIDER);
                    event.setCancelled(true);
                    return;
                }
                if (!loadoutManager.isAdminPresetEnabled(role, targetSlot)) {
                    p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    p.sendMessage(plugin.tr(p, "gui.loadout.presets.disabled_notify"));
                    openView(p, role == LoadoutRole.HIDER);
                    event.setCancelled(true);
                    return;
                }
                boolean changed = loadoutManager.applyAdminPresetToPlayer(p.getUniqueId(), role, targetSlot);
                p.playSound(p.getLocation(), changed ? Sound.ENTITY_EXPERIENCE_ORB_PICKUP : Sound.UI_BUTTON_CLICK, 1.0f,
                        1.0f);
                openView(p, role == LoadoutRole.HIDER);
                event.setCancelled(true);
            });
            invItem.setAllowTakeout(false);
            invItem.setAllowInsert(false);
            inv.setItem(guiSlot, invItem);
        }
    }

    private ItemStack createPresetInfoItem(Player player, int totalCount) {
        return createUtilityItem(GUIItems.L_PRESET_TITLE, plugin.tr(player, "gui.loadout.presets.info.title"),
                List.of(
                        plugin.tr(player, "gui.loadout.presets.info.slots").decoration(TextDecoration.ITALIC, false),
                        plugin.tr(player, "gui.loadout.presets.info.current_items",
                                Map.of("count", String.valueOf(totalCount))).decoration(TextDecoration.ITALIC, false),
                        plugin.tr(player, "gui.loadout.presets.info.validate").decoration(TextDecoration.ITALIC, false)
                ),
                new ItemStack(Material.WRITABLE_BOOK));
    }

    private ItemStack createPresetItem(Player player, int presetSlot, boolean hasPreset, LoadoutManager.PresetLoadResult analysis) {
        if (!hasPreset) {
            return createUtilityItem(GUIItems.L_PRESET_EMPTY,
                    plugin.tr(player, "gui.loadout.presets.slot_empty", Map.of("slot", String.valueOf(presetSlot))),
                    List.of(
                            plugin.tr(player, "gui.loadout.presets.hint_shift_save").decoration(TextDecoration.ITALIC,
                                    false),
                            plugin.tr(player, "gui.loadout.presets.hint_left_load").decoration(TextDecoration.ITALIC,
                                    false),
                            plugin.tr(player, "gui.loadout.presets.hint_right_delete").decoration(TextDecoration.ITALIC,
                                    false)
                    ),
                    new ItemStack(Material.GRAY_STAINED_GLASS_PANE));
        }

        PlayerLoadout.Preset preset = loadoutManager.getPreset(player.getUniqueId(), presetSlot);
        LoadoutItemType preview = preset.hiderItems.stream().findFirst().orElse(
                preset.seekerItems.stream().findFirst().orElse(null));
        ItemStack item = preview == null ? item(GUIItems.L_PRESET_FALLBACK,
                new ItemStack(Material.CHEST)) : getPreviewItemStack(preview);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        String statusTag = (analysis.isFullyApplied() || analysis.isEmptyPreset()) ? "green" : "yellow";
        Component titleComp = plugin.tr(player, "gui.loadout.presets.preset_title",
                Map.of("slot", String.valueOf(presetSlot), "color", statusTag));

        meta.displayName(titleComp.decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        int totalItems = preset.hiderItems.size() + preset.seekerItems.size();
        lore.add(plugin.tr(player, "gui.loadout.presets.items_saved",
                Map.of("count", String.valueOf(totalItems))).decoration(TextDecoration.ITALIC, false));
        if (!preset.hiderItems.isEmpty()) {
            lore.add(plugin.tr(player, "gui.loadout.presets.hider_count",
                    Map.of("count", String.valueOf(preset.hiderItems.size()))).decoration(TextDecoration.ITALIC,
                    false));
        }
        if (!preset.seekerItems.isEmpty()) {
            lore.add(plugin.tr(player, "gui.loadout.presets.seeker_count",
                    Map.of("count", String.valueOf(preset.seekerItems.size()))).decoration(TextDecoration.ITALIC,
                    false));
        }
        if (analysis.blockedByPolicy() > 0 || analysis.blockedByLimits() > 0) {
            lore.add(plugin.tr(player, "gui.loadout.presets.would_partial").decoration(TextDecoration.ITALIC, false));
            if (analysis.blockedByPolicy() > 0) {
                lore.add(plugin.tr(player, "gui.loadout.presets.blocked_policy",
                        Map.of("count", String.valueOf(analysis.blockedByPolicy()))).decoration(TextDecoration.ITALIC,
                        false));
            }
            if (analysis.blockedByLimits() > 0) {
                lore.add(plugin.tr(player, "gui.loadout.presets.blocked_limits",
                        Map.of("count", String.valueOf(analysis.blockedByLimits()))).decoration(TextDecoration.ITALIC,
                        false));
            }
        } else {
            lore.add(plugin.tr(player, "gui.loadout.presets.ready").decoration(TextDecoration.ITALIC, false));
        }
        lore.add(Component.empty());
        lore.add(plugin.tr(player, "gui.loadout.presets.hint_left_load").decoration(TextDecoration.ITALIC, false));
        lore.add(plugin.tr(player, "gui.loadout.presets.hint_shift_save").decoration(TextDecoration.ITALIC, false));
        lore.add(plugin.tr(player, "gui.loadout.presets.hint_right_delete").decoration(TextDecoration.ITALIC, false));

        meta.lore(lore);
        item.setItemMeta(meta);
        if (preview != null) {
            CustomModelDataUtil.setCustomModelData(item, resolveRuntimeItemId(preview), null);
        }
        return item;
    }


    private ItemStack createUtilityItem(String key, Component title, List<Component> lore, ItemStack fallback) {
        ItemStack item = plugin.getGuiItemRegistry().getOrDefault(GUINames.LOADOUT, key, fallback).clone();
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        meta.displayName(title.decoration(TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false));
        meta.lore(lore.stream().map(line -> line.decoration(TextDecoration.ITALIC, false)).toList());
        item.setItemMeta(meta);
        return item;
    }

    private String getRarityTag(ItemRarity rarity) {
        return switch (rarity) {
            case COMMON -> "white";
            case UNCOMMON -> "green";
            case RARE -> "blue";
            case EPIC -> "light_purple";
            case LEGENDARY -> "gold";
        };
    }

    private ItemStack createItemStack(Player player, LoadoutItemType type, int cost, boolean selected, int usedTokens, int maxTokens, int usedSlots, int maxSlots) {
        ItemStack item = getPreviewItemStack(type);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }


        meta.displayName(plugin.tr(player, "gui.loadout.item.display_name", Map.of(
                "name", getItemDisplayName(player, type.getItemId()),
                "color", getRarityTag(type.getRarity())
        )).decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();


        String description = getItemDescription(player, type);
        if (!description.isEmpty()) {

            lore.add(plugin.tr(player, "gui.loadout.item.description", Map.of("desc", description)).decoration(
                    TextDecoration.ITALIC, false));
            lore.add(Component.empty());
        }

        lore.add(plugin.tr(player, "gui.loadout.item.cost", Map.of("cost", String.valueOf(cost))).decoration(
                TextDecoration.ITALIC, false));
        lore.add(plugin.tr(player, "gui.loadout.item.type", Map.of("type", type.getItemType().name())).decoration(
                TextDecoration.ITALIC, false));
        lore.add(plugin.tr(player, "gui.loadout.item.rarity",
                Map.of("rarity", type.getRarity().name(), "color", getRarityTag(type.getRarity()))).decoration(
                TextDecoration.ITALIC, false));
        lore.add(Component.empty());

        if (selected) {
            lore.add(plugin.tr(player, "gui.loadout.item.selected").decoration(TextDecoration.ITALIC, false));
            lore.add(plugin.tr(player, "gui.loadout.item.click_to_remove").decoration(TextDecoration.ITALIC, false));

        } else {
            boolean canAfford = usedTokens + cost <= maxTokens;
            boolean hasSlot = usedSlots < maxSlots;

            if (canAfford && hasSlot) {
                lore.add(
                        plugin.tr(player, "gui.loadout.item.click_to_select").decoration(TextDecoration.ITALIC, false));
            } else if (!canAfford) {
                lore.add(plugin.tr(player, "gui.loadout.item.not_enough_tokens").decoration(TextDecoration.ITALIC,
                        false));
            } else {
                lore.add(plugin.tr(player, "gui.loadout.item.no_slots").decoration(TextDecoration.ITALIC, false));
            }
        }
        meta.setEnchantmentGlintOverride(selected);

        meta.lore(lore);
        item.setItemMeta(meta);
        CustomModelDataUtil.setCustomModelData(item, resolveRuntimeItemId(type), null);
        item.setData(DataComponentTypes.TOOLTIP_DISPLAY,
                TooltipDisplay.tooltipDisplay().hiddenComponents(ALL_TOOLTIP_COMPONENTS).build());
        return item;
    }

    private ItemStack createSelectedItemDisplay(Player player, LoadoutItemType type, int cost) {
        ItemStack item = getPreviewItemStack(type);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }


        meta.displayName(plugin.tr(player, "gui.loadout.item.display_name", Map.of(
                "name", getItemDisplayName(player, type.getItemId()),
                "color", getRarityTag(type.getRarity())
        )).decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(plugin.tr(player, "gui.loadout.item.cost", Map.of("cost", String.valueOf(cost))).decoration(
                TextDecoration.ITALIC, false));
        lore.add(plugin.tr(player, "gui.loadout.item.rarity",
                Map.of("rarity", type.getRarity().name(), "color", getRarityTag(type.getRarity()))).decoration(
                TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(plugin.tr(player, "gui.loadout.item.click_to_remove_selected").decoration(TextDecoration.ITALIC,
                false));

        meta.lore(lore);


        item.setItemMeta(meta);
        CustomModelDataUtil.setCustomModelData(item, resolveRuntimeItemId(type), null);
        item.setData(DataComponentTypes.TOOLTIP_DISPLAY,
                TooltipDisplay.tooltipDisplay().hiddenComponents(ALL_TOOLTIP_COMPONENTS).build());
        return item;
    }

    private String getItemDescription(Player player, LoadoutItemType type) {
        GameItem item = SeekerItems.getItem(type.getItemId());
        if (item == null) {
            item = HiderItems.getItem(type.getItemId());
        }

        return (item != null) ? item.getDescription(plugin, player) : plugin.trText(player,
                "gui.loadout.item.no_description");
    }

    private enum GuiTab {
        HIDER,
        SEEKER,
        PRESETS
    }

    private ItemStack getPreviewItemStack(LoadoutItemType type) {
        GameItem item = SeekerItems.getItem(type.getItemId());
        if (item == null) {
            item = HiderItems.getItem(type.getItemId());
        }

        if (item == null) {
            return item(GUIItems.L_NO_PREVIEW, new ItemStack(Material.BARRIER));
        }

        ItemStack stack = item.createItem(plugin);
        return stack == null ? item(GUIItems.L_NO_PREVIEW, new ItemStack(Material.BARRIER)) : stack.clone();
    }

    private String resolveRuntimeItemId(LoadoutItemType type) {
        if (type == LoadoutItemType.SPEED_BOOST) {
            return de.thecoolcraft11.hideAndSeek.items.hider.SpeedBoostItem.ID + "_0";
        }
        if (type == LoadoutItemType.KNOCKBACK_STICK) {
            return de.thecoolcraft11.hideAndSeek.items.hider.KnockbackStickItem.ID + "_1";
        }
        return type.getItemId();
    }


    private String getItemNameKey(String itemId) {
        if (itemId.startsWith("has_hider_")) {
            return "item." + itemId.substring("has_hider_".length()) + ".name";
        }
        if (itemId.startsWith("has_seeker_")) {
            return "item." + itemId.substring("has_seeker_".length()) + ".name";
        }
        return "item." + itemId + ".name";
    }

    private String getItemDisplayName(Player player, String itemId) {
        String key = getItemNameKey(itemId);
        switch (itemId) {
            case de.thecoolcraft11.hideAndSeek.items.hider.SpeedBoostItem.ID -> {
                return MiniMessage.miniMessage().stripTags(plugin.trText(player, key,
                        TranslationArguments.ofNamed(Map.of("level", String.valueOf(
                                de.thecoolcraft11.hideAndSeek.items.hider.SpeedBoostItem.getSpeedLevel(
                                        player.getUniqueId()))))));
            }
            case de.thecoolcraft11.hideAndSeek.items.hider.KnockbackStickItem.ID -> {
                return MiniMessage.miniMessage().stripTags(plugin.trText(player, key,
                        TranslationArguments.ofNamed(Map.of("level", String.valueOf(
                                de.thecoolcraft11.hideAndSeek.items.hider.KnockbackStickItem.getKnockbackLevel(
                                        player.getUniqueId()))))));
            }
            case de.thecoolcraft11.hideAndSeek.items.hider.RandomBlockItem.ID -> {
                int uses = plugin.getSettingRegistry().get("hider-items.random-block.uses", 5);
                return MiniMessage.miniMessage().stripTags(plugin.trText(player, key,
                        TranslationArguments.ofNamed(
                                Map.of("uses", String.valueOf(uses), "maxUses", String.valueOf(uses)))));
            }
        }
        return MiniMessage.miniMessage().stripTags(plugin.trText(player, key));
    }

    private ItemStack item(String key, ItemStack fallback) {
        return plugin.getGuiItemRegistry().getOrDefault(GUINames.LOADOUT, key, fallback);
    }
}
