package de.thecoolcraft11.hideAndSeek.gui;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
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
import io.papermc.paper.datacomponent.DataComponentType;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.TooltipDisplay;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
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
            player.sendMessage(Component.text("Loadouts can only be modified in the lobby!", NamedTextColor.RED));
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
                        .title(hiderView ? "Hider Loadout" : "Seeker Loadout")
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
            InventoryItem lockedInfo = new InventoryItem(createRestrictedInfoItem(role));
            lockedInfo.setClickHandler((p, item, event, slot) -> event.setCancelled(true));
            lockedInfo.setAllowTakeout(false);
            lockedInfo.setAllowInsert(false);
            inv.setItem(4, lockedInfo);

            renderAdminPresetChoices(inv, player, role);
            renderBottomTabs(inv, hiderView ? GuiTab.HIDER : GuiTab.SEEKER);
            plugin.getInventoryFramework().openInventory(player, inv);
            return;
        }


        InventoryItem infoItem = new InventoryItem(createInfoItem(selected.size(), maxItems, usedTokens, maxTokens));
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
            InventoryItem catalogItem = new InventoryItem(createItemStack(item, cost, isSelected, usedTokens, maxTokens, selected.size(), maxItems));
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
            InventoryItem selectedItem = new InventoryItem(createSelectedItemDisplay(item, cost));
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


        InventoryItem slotPrefsButton = new InventoryItem(createUtilityItem(Material.HOPPER,
                "Slot Preferences", NamedTextColor.LIGHT_PURPLE,
                List.of(
                        Component.text("Configure item placement", NamedTextColor.YELLOW).decoration(
                                TextDecoration.ITALIC, false),
                        Component.text("Choose which ItemTypes go in which slots", NamedTextColor.YELLOW).decoration(
                                TextDecoration.ITALIC, false)
                )));
        slotPrefsButton.setClickHandler((p, item, event, s) -> {
            SlotPreferencesGUI gui = new SlotPreferencesGUI(loadoutManager, plugin);
            gui.open(p, hiderView);
            event.setCancelled(true);
        });
        slotPrefsButton.setAllowTakeout(false);
        slotPrefsButton.setAllowInsert(false);
        inv.setItem(53, slotPrefsButton);

        renderBottomTabs(inv, hiderView ? GuiTab.HIDER : GuiTab.SEEKER);

        plugin.getInventoryFramework().openInventory(player, inv);
    }

    private void openPresetsView(Player player) {
        FrameworkInventory inv = new de.thecoolcraft11.minigameframework.inventory.InventoryBuilder(plugin.getInventoryFramework())
                .id("loadout_presets_" + player.getUniqueId() + "_combined")
                .title("Loadout Presets")
                .rows(6)
                .allowOutsideClicks(false)
                .allowDrag(false)
                .allowPlayerInventoryInteraction(false)
                .build();

        PlayerLoadout loadout = loadoutManager.getLoadout(player.getUniqueId());
        int totalCount = loadout.getHiderItems().size() + loadout.getSeekerItems().size();

        InventoryItem info = new InventoryItem(createPresetInfoItem(totalCount));
        info.setClickHandler((p, item, event, slot) -> event.setCancelled(true));
        info.setAllowTakeout(false);
        info.setAllowInsert(false);
        inv.setItem(4, info);

        int[] presetSlots = {19, 20, 22, 24, 25};
        for (int presetSlot = 1; presetSlot <= PlayerLoadout.MAX_PRESETS; presetSlot++) {
            int guiSlot = presetSlots[presetSlot - 1];
            boolean hasPreset = loadoutManager.hasPreset(player.getUniqueId(), presetSlot);
            LoadoutManager.PresetLoadResult analysis = loadoutManager.analyzePresetLoad(player.getUniqueId(), presetSlot);

            InventoryItem presetItem = new InventoryItem(createPresetItem(player, presetSlot, hasPreset, analysis));
            int finalPresetSlot = presetSlot;
            presetItem.setClickHandler((p, item, event, slot) -> {
                if (isCustomEditingBlocked(LoadoutRole.HIDER) || isCustomEditingBlocked(LoadoutRole.SEEKER)) {
                    p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    p.sendMessage(Component.text("Custom loadouts are disabled while admin presets are enforced.", NamedTextColor.RED));
                    openPresetsView(p);
                    event.setCancelled(true);
                    return;
                }

                if (loadoutManager.isGlobalLoadoutLocked()) {
                    p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    p.sendMessage(Component.text("Loadouts are globally locked by an admin.", NamedTextColor.RED));
                    openPresetsView(p);
                    event.setCancelled(true);
                    return;
                }

                if (event.getClick() == ClickType.SHIFT_LEFT) {
                    loadoutManager.savePreset(p.getUniqueId(), finalPresetSlot);
                    p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
                    p.sendMessage(Component.text("Saved current loadout to preset #" + finalPresetSlot + ".", NamedTextColor.GREEN));
                    openPresetsView(p);
                    event.setCancelled(true);
                    return;
                }

                if (event.getClick().isRightClick()) {
                    boolean removed = loadoutManager.deletePreset(p.getUniqueId(), finalPresetSlot);
                    p.playSound(p.getLocation(), removed ? Sound.ENTITY_ITEM_BREAK : Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    p.sendMessage(Component.text(removed
                            ? "Deleted preset #" + finalPresetSlot + "."
                            : "Preset #" + finalPresetSlot + " is already empty.", removed ? NamedTextColor.YELLOW : NamedTextColor.RED));
                    openPresetsView(p);
                    event.setCancelled(true);
                    return;
                }

                LoadoutManager.PresetLoadResult result = loadoutManager.loadPreset(p.getUniqueId(), finalPresetSlot);
                if (!result.presetExists()) {
                    p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    p.sendMessage(Component.text("Preset #" + finalPresetSlot + " is empty.", NamedTextColor.RED));
                    openPresetsView(p);
                    event.setCancelled(true);
                    return;
                }

                if (result.isEmptyPreset()) {
                    p.sendMessage(Component.text("Loaded preset #" + finalPresetSlot + " (empty preset).", NamedTextColor.YELLOW));
                } else if (result.isFullyApplied()) {
                    p.sendMessage(Component.text("Loaded preset #" + finalPresetSlot + " successfully.", NamedTextColor.GREEN));
                } else {
                    p.sendMessage(Component.text("Loaded preset #" + finalPresetSlot + " partially: "
                            + result.blockedByPolicy() + " blocked by admin policy, "
                            + result.blockedByLimits() + " blocked by limits.", NamedTextColor.YELLOW));
                }

                p.playSound(p.getLocation(), result.isFullyApplied() ? Sound.ENTITY_PLAYER_LEVELUP : Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                openPresetsView(p);
                event.setCancelled(true);
            });
            presetItem.setAllowTakeout(false);
            presetItem.setAllowInsert(false);
            inv.setItem(guiSlot, presetItem);
        }

        InventoryItem roleToggle = new InventoryItem(createUtilityItem(Material.COMPASS,
                "Editing: All Presets", NamedTextColor.AQUA,
                List.of(
                        Component.text("Both hider & seeker items in one preset", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false),
                        Component.text("Shift+Left preset = save", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                        Component.text("Left preset = load, Right preset = delete", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                )));
        roleToggle.setClickHandler((p, item, event, slot) -> event.setCancelled(true));
        roleToggle.setAllowTakeout(false);
        roleToggle.setAllowInsert(false);
        inv.setItem(49, roleToggle);

        renderBottomTabs(inv, GuiTab.PRESETS);
        plugin.getInventoryFramework().openInventory(player, inv);
    }

    private void handleSelectedRemoval(Player player, boolean isHiderView, LoadoutItemType itemToRemove) {
        LoadoutRole role = isHiderView ? LoadoutRole.HIDER : LoadoutRole.SEEKER;
        if (isCustomEditingBlocked(role)) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            player.sendMessage(Component.text("This role is restricted to admin presets.", NamedTextColor.RED));
            openView(player, isHiderView);
            return;
        }

        if (loadoutManager.isGlobalLoadoutLocked()) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            player.sendMessage(Component.text("Loadouts are globally locked by an admin.", NamedTextColor.RED));
            openView(player, isHiderView);
            return;
        }

        if (loadoutManager.isRoleLocked(player.getUniqueId(), role)) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            player.sendMessage(Component.text("Your " + (isHiderView ? "hider" : "seeker") + " loadout is locked by an admin.", NamedTextColor.RED));
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
        player.sendMessage(Component.text("Removed ", NamedTextColor.RED)
                .append(Component.text(formatName(itemToRemove.name()), getRarityColor(itemToRemove.getRarity())))
                .append(Component.text(" (+" + cost + " tokens)", NamedTextColor.GOLD)));

        openView(player, isHiderView);
    }

    private void handleCatalogSelection(Player player, boolean isHiderView, LoadoutItemType clickedItem) {
        LoadoutRole role = isHiderView ? LoadoutRole.HIDER : LoadoutRole.SEEKER;
        if (isCustomEditingBlocked(role)) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            player.sendMessage(Component.text("This role is restricted to admin presets.", NamedTextColor.RED));
            openView(player, isHiderView);
            return;
        }

        if (loadoutManager.isGlobalLoadoutLocked()) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            player.sendMessage(Component.text("Loadouts are globally locked by an admin.", NamedTextColor.RED));
            openView(player, isHiderView);
            return;
        }

        if (loadoutManager.isRoleLocked(player.getUniqueId(), role)) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            player.sendMessage(Component.text("Your " + (isHiderView ? "hider" : "seeker") + " loadout is locked by an admin.", NamedTextColor.RED));
            openView(player, isHiderView);
            return;
        }
        if (!loadoutManager.isItemAvailableForRole(role, clickedItem)) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            player.sendMessage(Component.text("This item is disabled by an admin.", NamedTextColor.RED));
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
                player.sendMessage(Component.text("Removed ", NamedTextColor.RED)
                        .append(Component.text(formatName(clickedItem.name()), getRarityColor(clickedItem.getRarity())))
                        .append(Component.text(" (+" + cost + " tokens)", NamedTextColor.GOLD)));
            } else {
                if (loadout.addHiderItem(clickedItem, loadoutManager.getMaxHiderItems(),
                        loadoutManager.getMaxHiderTokens(), cost)) {
                    loadoutManager.clearPlayerSelectedAdminPreset(player.getUniqueId(), LoadoutRole.HIDER);
                    loadoutManager.saveLoadout(player.getUniqueId());
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
                    player.sendMessage(Component.text("Added ", NamedTextColor.GREEN)
                            .append(Component.text(formatName(clickedItem.name()), getRarityColor(clickedItem.getRarity())))
                            .append(Component.text(" (-" + cost + " tokens)", NamedTextColor.GOLD)));
                } else {
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    if (loadout.getHiderItems().size() >= loadoutManager.getMaxHiderItems()) {
                        player.sendMessage(Component.text("Maximum items reached!", NamedTextColor.RED));
                    } else {
                        player.sendMessage(Component.text("Not enough tokens!", NamedTextColor.RED));
                    }
                }
            }
        } else {
            if (loadout.getSeekerItems().contains(clickedItem)) {
                loadout.removeSeekerItem(clickedItem);
                loadoutManager.clearPlayerSelectedAdminPreset(player.getUniqueId(), LoadoutRole.SEEKER);
                loadoutManager.saveLoadout(player.getUniqueId());
                player.playSound(player.getLocation(), Sound.ENTITY_ITEM_PICKUP, 1.0f, 0.8f);
                player.sendMessage(Component.text("Removed ", NamedTextColor.RED)
                        .append(Component.text(formatName(clickedItem.name()), getRarityColor(clickedItem.getRarity())))
                        .append(Component.text(" (+" + cost + " tokens)", NamedTextColor.GOLD)));
            } else {
                if (loadout.addSeekerItem(clickedItem, loadoutManager.getMaxSeekerItems(),
                        loadoutManager.getMaxSeekerTokens(), cost)) {
                    loadoutManager.clearPlayerSelectedAdminPreset(player.getUniqueId(), LoadoutRole.SEEKER);
                    loadoutManager.saveLoadout(player.getUniqueId());
                    player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
                    player.sendMessage(Component.text("Added ", NamedTextColor.GREEN)
                            .append(Component.text(formatName(clickedItem.name()), getRarityColor(clickedItem.getRarity())))
                            .append(Component.text(" (-" + cost + " tokens)", NamedTextColor.GOLD)));
                } else {
                    player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    if (loadout.getSeekerItems().size() >= loadoutManager.getMaxSeekerItems()) {
                        player.sendMessage(Component.text("Maximum items reached!", NamedTextColor.RED));
                    } else {
                        player.sendMessage(Component.text("Not enough tokens!", NamedTextColor.RED));
                    }
                }
            }
        }

        openView(player, isHiderView);
    }

    private void renderBottomTabs(FrameworkInventory inv, GuiTab activeTab) {
        inv.setItem(50, buildTabButton(GuiTab.HIDER, activeTab));
        inv.setItem(51, buildTabButton(GuiTab.SEEKER, activeTab));
        inv.setItem(52, buildTabButton(GuiTab.PRESETS, activeTab));
    }


    private ItemStack createInfoItem(int usedSlots, int maxSlots, int usedTokens, int maxTokens) {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Loadout Info", NamedTextColor.AQUA, TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(Arrays.asList(
                Component.text("Items: " + usedSlots + "/" + maxSlots, usedSlots >= maxSlots ? NamedTextColor.RED : NamedTextColor.GREEN)
                        .decoration(TextDecoration.ITALIC, false),
                Component.text("Tokens: " + usedTokens + "/" + maxTokens, usedTokens >= maxTokens ? NamedTextColor.RED : NamedTextColor.GREEN)
                        .decoration(TextDecoration.ITALIC, false)
        ));
        item.setItemMeta(meta);
        return item;
    }

    private InventoryItem buildTabButton(GuiTab tab, GuiTab activeTab) {
        Material material = switch (tab) {
            case HIDER -> Material.BLUE_CONCRETE;
            case SEEKER -> Material.RED_CONCRETE;
            case PRESETS -> Material.BOOKSHELF;
        };
        String label = switch (tab) {
            case HIDER -> "Hider";
            case SEEKER -> "Seeker";
            case PRESETS -> "Presets";
        };
        boolean active = tab == activeTab;

        ItemStack stack = createUtilityItem(material,
                label + (active ? " (Selected)" : ""),
                active ? NamedTextColor.GREEN : NamedTextColor.YELLOW,
                List.of(Component.text(active ? "Current tab" : "Click to open", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));

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

    private ItemStack createRestrictedInfoItem(LoadoutRole role) {
        int forcedSlot = loadoutManager.getForcedRolePresetSlot(role);
        List<Component> lore = new ArrayList<>();
        if (forcedSlot > 0) {
            lore.add(Component.text("Role uses forced preset #" + forcedSlot, NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
        } else {
            lore.add(Component.text("Custom loadouts are disabled", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Pick one enabled admin preset", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
        }
        lore.add(Component.text("Changes are managed in admin presets", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        return createUtilityItem(Material.BOOK, role.name() + " Preset Mode", NamedTextColor.AQUA, lore);
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
                stack = createUtilityItem(Material.GRAY_STAINED_GLASS_PANE, "Preset #" + slot + " (Empty)", NamedTextColor.GRAY,
                        List.of(Component.text("Admin has not configured this preset", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
            } else if (!enabled) {
                stack = createUtilityItem(Material.BARRIER, "Preset #" + slot + " (Disabled)", NamedTextColor.RED,
                        List.of(Component.text("Disabled by admin", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false)));
            } else {
                LoadoutItemType preview = preset.getItems().stream().findFirst().orElse(null);
                stack = preview == null ? new ItemStack(Material.CHEST) : getPreviewItemStack(preview);
                ItemMeta meta = stack.getItemMeta();
                if (meta != null) {
                    NamedTextColor titleColor = forced ? NamedTextColor.RED : (selected ? NamedTextColor.GREEN : NamedTextColor.YELLOW);
                    meta.displayName(Component.text("Preset #" + slot + (forced ? " (Forced)" : selected ? " (Selected)" : ""), titleColor, TextDecoration.BOLD)
                            .decoration(TextDecoration.ITALIC, false));
                    List<Component> lore = new ArrayList<>();
                    lore.add(Component.text("Items: " + preset.getItems().size(), NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
                    lore.add(Component.text(forced ? "This preset is enforced" : "Click to apply", forced ? NamedTextColor.RED : NamedTextColor.YELLOW)
                            .decoration(TextDecoration.ITALIC, false));
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
                    p.sendMessage(Component.text("A forced preset is active for this role.", NamedTextColor.RED));
                    openView(p, role == LoadoutRole.HIDER);
                    event.setCancelled(true);
                    return;
                }
                if (!loadoutManager.isAdminPresetEnabled(role, targetSlot)) {
                    p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    p.sendMessage(Component.text("That preset is disabled.", NamedTextColor.RED));
                    openView(p, role == LoadoutRole.HIDER);
                    event.setCancelled(true);
                    return;
                }
                boolean changed = loadoutManager.applyAdminPresetToPlayer(p.getUniqueId(), role, targetSlot);
                p.playSound(p.getLocation(), changed ? Sound.ENTITY_EXPERIENCE_ORB_PICKUP : Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
                openView(p, role == LoadoutRole.HIDER);
                event.setCancelled(true);
            });
            invItem.setAllowTakeout(false);
            invItem.setAllowInsert(false);
            inv.setItem(guiSlot, invItem);
        }
    }

    private ItemStack createPresetInfoItem(int totalCount) {
        return createUtilityItem(Material.WRITABLE_BOOK, "Combined Presets", NamedTextColor.AQUA,
                List.of(
                        Component.text("5 preset slots with both roles", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                        Component.text("Current items: " + totalCount + " total", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                        Component.text("Load validates limits and admin restrictions", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false)
                ));
    }

    private ItemStack createPresetItem(Player player, int presetSlot, boolean hasPreset, LoadoutManager.PresetLoadResult analysis) {
        if (!hasPreset) {
            return createUtilityItem(Material.GRAY_STAINED_GLASS_PANE, "Preset #" + presetSlot + " (Empty)", NamedTextColor.GRAY,
                    List.of(
                            Component.text("Shift+Left click to save current loadout", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false),
                            Component.text("Left click to load", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                            Component.text("Right click to delete", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                    ));
        }

        PlayerLoadout.Preset preset = loadoutManager.getPreset(player.getUniqueId(), presetSlot);
        LoadoutItemType preview = preset.hiderItems.stream().findFirst().orElse(preset.seekerItems.stream().findFirst().orElse(null));
        ItemStack item = preview == null ? new ItemStack(Material.CHEST) : getPreviewItemStack(preview);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        NamedTextColor statusColor = analysis.isFullyApplied() || analysis.isEmptyPreset() ? NamedTextColor.GREEN : NamedTextColor.YELLOW;
        meta.displayName(Component.text("Preset #" + presetSlot, statusColor, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        int totalItems = preset.hiderItems.size() + preset.seekerItems.size();
        lore.add(Component.text("Items saved: " + totalItems, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        if (!preset.hiderItems.isEmpty()) {
            lore.add(Component.text("  Hider: " + preset.hiderItems.size(), NamedTextColor.DARK_AQUA).decoration(TextDecoration.ITALIC, false));
        }
        if (!preset.seekerItems.isEmpty()) {
            lore.add(Component.text("  Seeker: " + preset.seekerItems.size(), NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
        }
        if (analysis.blockedByPolicy() > 0 || analysis.blockedByLimits() > 0) {
            lore.add(Component.text("Would load partially", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
            if (analysis.blockedByPolicy() > 0) {
                lore.add(Component.text("Blocked by admin policy: " + analysis.blockedByPolicy(), NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
            }
            if (analysis.blockedByLimits() > 0) {
                lore.add(Component.text("Blocked by limits/tokens: " + analysis.blockedByLimits(), NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
            }
        } else {
            lore.add(Component.text("Ready to load", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
        }
        lore.add(Component.empty());
        lore.add(Component.text("Left click: Load", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Shift+Left click: Save current", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Right click: Delete", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));

        meta.lore(lore);
        item.setItemMeta(meta);
        if (preview != null) {
            CustomModelDataUtil.setCustomModelData(item, resolveRuntimeItemId(preview), null);
        }
        return item;
    }

    private ItemStack createUtilityItem(Material material, String title, NamedTextColor color, List<Component> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        meta.displayName(Component.text(title, color, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createItemStack(LoadoutItemType type, int cost, boolean selected, int usedTokens, int maxTokens, int usedSlots, int maxSlots) {
        ItemStack item = getPreviewItemStack(type);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        NamedTextColor rarityColor = getRarityColor(type.getRarity());
        meta.displayName(Component.text(formatName(type.name()), rarityColor, TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();


        String description = getItemDescription(type);
        if (!description.isEmpty()) {
            lore.add(Component.text(description, NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.empty());
        }

        lore.add(Component.text("Cost: " + cost + " tokens", NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Type: " + type.getItemType().name(), NamedTextColor.DARK_GRAY)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Rarity: " + type.getRarity().name(), rarityColor)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());

        if (selected) {
            lore.add(Component.text("Selected", NamedTextColor.GREEN, TextDecoration.BOLD)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Click to remove", NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));

        } else {
            boolean canAfford = usedTokens + cost <= maxTokens;
            boolean hasSlot = usedSlots < maxSlots;

            if (canAfford && hasSlot) {
                lore.add(Component.text("Click to select", NamedTextColor.YELLOW)
                        .decoration(TextDecoration.ITALIC, false));
            } else if (!canAfford) {
                lore.add(Component.text("Not enough tokens", NamedTextColor.RED)
                        .decoration(TextDecoration.ITALIC, false));
            } else {
                lore.add(Component.text("No slots available", NamedTextColor.RED)
                        .decoration(TextDecoration.ITALIC, false));
            }
        }
        meta.setEnchantmentGlintOverride(selected);

        meta.lore(lore);
        item.setItemMeta(meta);
        CustomModelDataUtil.setCustomModelData(item, resolveRuntimeItemId(type), null);
        item.setData(DataComponentTypes.TOOLTIP_DISPLAY, TooltipDisplay.tooltipDisplay().hiddenComponents(ALL_TOOLTIP_COMPONENTS).build());
        return item;
    }

    private ItemStack createSelectedItemDisplay(LoadoutItemType type, int cost) {
        ItemStack item = getPreviewItemStack(type);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }


        NamedTextColor rarityColor = getRarityColor(type.getRarity());
        meta.displayName(Component.text(formatName(type.name()), rarityColor, TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Cost: " + cost + " tokens", NamedTextColor.GOLD)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Rarity: " + type.getRarity().name(), rarityColor)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text("Click to remove", NamedTextColor.RED)
                .decoration(TextDecoration.ITALIC, false));

        meta.lore(lore);



        item.setItemMeta(meta);
        CustomModelDataUtil.setCustomModelData(item, resolveRuntimeItemId(type), null);
        item.setData(DataComponentTypes.TOOLTIP_DISPLAY, TooltipDisplay.tooltipDisplay().hiddenComponents(ALL_TOOLTIP_COMPONENTS).build());
        return item;
    }

    private String getItemDescription(LoadoutItemType type) {
        GameItem item = SeekerItems.getItem(type.getItemId());
        if (item == null) {
            item = HiderItems.getItem(type.getItemId());
        }

        return (item != null) ? item.getDescription(plugin) : "No description available";
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
            return new ItemStack(Material.BARRIER);
        }

        ItemStack stack = item.createItem(plugin);
        return stack == null ? new ItemStack(Material.BARRIER) : stack.clone();
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

    private NamedTextColor getRarityColor(ItemRarity rarity) {
        return switch (rarity) {
            case COMMON -> NamedTextColor.WHITE;
            case UNCOMMON -> NamedTextColor.GREEN;
            case RARE -> NamedTextColor.BLUE;
            case EPIC -> NamedTextColor.LIGHT_PURPLE;
            case LEGENDARY -> NamedTextColor.GOLD;
        };
    }

    private String formatName(String name) {
        StringBuilder result = new StringBuilder();
        for (String part : name.split("_")) {
            if (!result.isEmpty()) result.append(" ");
            result.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1).toLowerCase());
        }
        return result.toString();
    }
}
