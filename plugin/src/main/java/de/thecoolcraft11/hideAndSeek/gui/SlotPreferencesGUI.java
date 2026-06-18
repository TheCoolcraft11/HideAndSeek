package de.thecoolcraft11.hideAndSeek.gui;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.gui.config.GUIItems;
import de.thecoolcraft11.hideAndSeek.gui.config.GUINames;
import de.thecoolcraft11.hideAndSeek.loadout.LoadoutManager;
import de.thecoolcraft11.hideAndSeek.loadout.PlayerLoadout;
import de.thecoolcraft11.hideAndSeek.model.ItemType;
import de.thecoolcraft11.hideAndSeek.model.SlotPreference;
import de.thecoolcraft11.minigameframework.inventory.FrameworkInventory;
import de.thecoolcraft11.minigameframework.inventory.InventoryItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SlotPreferencesGUI {
    private final LoadoutManager loadoutManager;
    private final HideAndSeek plugin;

    public SlotPreferencesGUI(LoadoutManager loadoutManager, HideAndSeek plugin) {
        this.loadoutManager = loadoutManager;
        this.plugin = plugin;
    }

    public void open(Player player, boolean isHider) {
        if (!loadoutManager.canModifyLoadout()) {
            player.sendMessage(plugin.tr(player, "gui.slotprefs.cant-modify"));
            return;
        }

        int maxItems = isHider ? loadoutManager.getMaxHiderItems() : loadoutManager.getMaxSeekerItems();

        FrameworkInventory inv = new de.thecoolcraft11.minigameframework.inventory.InventoryBuilder(
                plugin.getInventoryFramework())
                .id("slot_prefs_" + player.getUniqueId() + "_" + (isHider ? "hider" : "seeker"))
                .title(plugin.trText(player, isHider ? "gui.slotprefs.hider.title" : "gui.slotprefs.seeker.title"))
                .rows(6)
                .allowOutsideClicks(false)
                .allowDrag(false)
                .allowPlayerInventoryInteraction(false)
                .build();

        PlayerLoadout loadout = loadoutManager.getLoadout(player.getUniqueId());


        InventoryItem infoItem = new InventoryItem(createInfoItem(player, maxItems));
        infoItem.setClickHandler((p, item, event, slot) -> event.setCancelled(true));
        infoItem.setAllowTakeout(false);
        infoItem.setAllowInsert(false);
        inv.setItem(4, infoItem);



        int startSlot = isHider ? 0 : 1;
        for (int i = 0; i < maxItems; i++) {
            final int slotNumber = startSlot + i;
            SlotPreference preference = isHider ? loadout.getHiderSlotPreference(
                    slotNumber) : loadout.getSeekerSlotPreference(slotNumber);

            InventoryItem slotItem = new InventoryItem(createPrimarySlotItem(player, i, preference));
            slotItem.setClickHandler((p, item, event, s) -> {
                if (event.isRightClick()) {

                    if (isHider) {
                        loadout.setHiderSlotPreference(slotNumber, null, null);
                    } else {
                        loadout.setSeekerSlotPreference(slotNumber, null, null);
                    }
                    loadoutManager.saveLoadout(p.getUniqueId());
                    p.playSound(p.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
                    p.sendMessage(plugin.tr(player, "gui.slotprefs.cleared.slot",
                            java.util.Map.of("slot", String.valueOf(slotNumber))));
                    open(p, isHider);
                } else {

                    openPreferenceSelector(p, slotNumber, isHider, false);
                }
                event.setCancelled(true);
            });
            slotItem.setAllowTakeout(false);
            slotItem.setAllowInsert(false);
            inv.setItem(9 + i, slotItem);
        }


        for (int i = 0; i < maxItems; i++) {
            final int slotNumber = startSlot + i;
            SlotPreference preference = isHider ? loadout.getHiderSlotPreference(
                    slotNumber) : loadout.getSeekerSlotPreference(slotNumber);

            InventoryItem slotItem = new InventoryItem(createFallbackSlotItem(player, i, preference));
            slotItem.setClickHandler((p, item, event, s) -> {
                if (event.isRightClick()) {

                    SlotPreference current = isHider ? loadout.getHiderSlotPreference(
                            slotNumber) : loadout.getSeekerSlotPreference(slotNumber);
                    if (current != null && current.hasFallback()) {
                        if (isHider) {
                            loadout.setHiderSlotPreference(slotNumber, current.primary(), null);
                        } else {
                            loadout.setSeekerSlotPreference(slotNumber, current.primary(), null);
                        }
                        loadoutManager.saveLoadout(p.getUniqueId());
                        p.playSound(p.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
                        p.sendMessage(plugin.tr(player, "gui.slotprefs.cleared.fallback",
                                java.util.Map.of("slot", String.valueOf(slotNumber))));
                    }
                    open(p, isHider);
                } else {

                    SlotPreference current = isHider ? loadout.getHiderSlotPreference(
                            slotNumber) : loadout.getSeekerSlotPreference(slotNumber);
                    if (current != null) {
                        openPreferenceSelector(p, slotNumber, isHider, true);
                    } else {
                        p.sendMessage(plugin.tr(player, "gui.slotprefs.error.set-primary-first"));
                        p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    }
                }
                event.setCancelled(true);
            });
            slotItem.setAllowTakeout(false);
            slotItem.setAllowInsert(false);
            inv.setItem(18 + i, slotItem);
        }


        InventoryItem clearAllButton = new InventoryItem(
                createUtilityItem(GUIItems.KEY_CLEAR, plugin.tr(player, "gui.slotprefs.clear_all.title"),
                        List.of(plugin.tr(player, "gui.slotprefs.clear_all.hint").decoration(TextDecoration.ITALIC,
                                false))
                        , new ItemStack(Material.BARRIER)));
        clearAllButton.setClickHandler((p, item, event, slot) -> {
            if (isHider) {
                loadout.clearHiderSlotPreferences();
            } else {
                loadout.clearSeekerSlotPreferences();
            }
            loadoutManager.saveLoadout(p.getUniqueId());
            p.playSound(p.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 0.8f);
            p.sendMessage(plugin.tr(player, "gui.slotprefs.cleared.all"));
            open(p, isHider);
            event.setCancelled(true);
        });
        clearAllButton.setAllowTakeout(false);
        clearAllButton.setAllowInsert(false);
        inv.setItem(45, clearAllButton);


        InventoryItem backButton = new InventoryItem(
                createUtilityItem(GUIItems.SP_BACK_LOADOUT, plugin.tr(player, "gui.slotprefs.back_to_loadout"),
                        List.of(plugin.tr(player, "gui.slotprefs.back_to_loadout.hint").decoration(
                                TextDecoration.ITALIC, false)),
                        new ItemStack(Material.ARROW)));
        backButton.setClickHandler((p, item, event, slot) -> {
            LoadoutGUI gui = new LoadoutGUI(loadoutManager, plugin);
            gui.open(p);
            event.setCancelled(true);
        });
        backButton.setAllowTakeout(false);
        backButton.setAllowInsert(false);
        inv.setItem(49, backButton);

        plugin.getInventoryFramework().openInventory(player, inv);
    }

    private void openPreferenceSelector(Player player, int slot, boolean isHider, boolean isFallback) {
        FrameworkInventory inv = new de.thecoolcraft11.minigameframework.inventory.InventoryBuilder(
                plugin.getInventoryFramework())
                .id("pref_selector_" + player.getUniqueId() + "_" + slot + "_" + (isFallback ? "fb" : "primary"))
                .title(plugin.trText(player, "gui.slotprefs.selector.title", java.util.Map.of(
                        "mode", isFallback ? "Fallback" : "Primary",
                        "slot", String.valueOf(slot)
                )))
                .rows(3)
                .allowOutsideClicks(false)
                .allowDrag(false)
                .allowPlayerInventoryInteraction(false)
                .build();

        int slotIndex = 9;
        for (ItemType itemType : ItemType.values()) {
            InventoryItem typeItem = new InventoryItem(createItemTypeItem(player, itemType));
            typeItem.setClickHandler((p, item, event, s) -> {
                PlayerLoadout loadout = loadoutManager.getLoadout(p.getUniqueId());
                SlotPreference existing = isFallback ?
                        (isHider ? loadout.getHiderSlotPreference(slot) : loadout.getSeekerSlotPreference(slot)) : null;

                if (isFallback && existing != null) {

                    if (isHider) {
                        loadout.setHiderSlotPreference(slot, existing.primary(), itemType);
                    } else {
                        loadout.setSeekerSlotPreference(slot, existing.primary(), itemType);
                    }
                } else {

                    if (isHider) {
                        loadout.setHiderSlotPreference(slot, itemType, null);
                    } else {
                        loadout.setSeekerSlotPreference(slot, itemType, null);
                    }
                }
                loadoutManager.saveLoadout(p.getUniqueId());
                p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);


                if (!isFallback) {
                    p.sendMessage(plugin.tr(player, "gui.slotprefs.set.primary", java.util.Map.of(
                            "slot", String.valueOf(slot),
                            "type", formatName(itemType.name())
                    )));
                    p.sendMessage(plugin.tr(player, "gui.slotprefs.primary.hint"));
                    open(p, isHider);
                } else {
                    p.sendMessage(plugin.tr(player, "gui.slotprefs.set.fallback", java.util.Map.of(
                            "slot", String.valueOf(slot),
                            "type", formatName(itemType.name())
                    )));
                    open(p, isHider);
                }
                event.setCancelled(true);
            });
            typeItem.setAllowTakeout(false);
            typeItem.setAllowInsert(false);
            inv.setItem(slotIndex++, typeItem);
        }


        InventoryItem backButton = new InventoryItem(
                createUtilityItem(GUIItems.SP_BACK_PREFS,
                        plugin.tr(player, "gui.slotprefs.back_to_preferences.title"),
                        List.of(plugin.tr(player, "gui.slotprefs.back_to_preferences.hint").decoration(
                                TextDecoration.ITALIC, false)),
                        new ItemStack(Material.ARROW)));
        backButton.setClickHandler((p, item, event, s) -> {
            open(p, isHider);
            event.setCancelled(true);
        });
        backButton.setAllowTakeout(false);
        backButton.setAllowInsert(false);
        inv.setItem(22, backButton);

        plugin.getInventoryFramework().openInventory(player, inv);
    }

    private ItemStack createInfoItem(Player player, int maxItems) {
        ItemStack item = item(GUIItems.KEY_INFO, new ItemStack(Material.BOOK));
        ItemMeta meta = item.getItemMeta();
        meta.displayName(plugin.tr(player, "gui.slotprefs.info.title").color(NamedTextColor.AQUA)
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(Arrays.asList(
                plugin.tr(player, "gui.slotprefs.info.configure", java.util.Map.of("count", String.valueOf(maxItems)))
                        .color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                plugin.tr(player, "gui.slotprefs.info.top_row").color(NamedTextColor.GREEN).decoration(
                        TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false),
                plugin.tr(player, "gui.slotprefs.info.bottom_row").color(NamedTextColor.YELLOW).decoration(
                        TextDecoration.BOLD, true).decoration(TextDecoration.ITALIC, false),
                Component.empty(),
                plugin.tr(player, "gui.slotprefs.info.primary").color(NamedTextColor.GRAY).decoration(
                        TextDecoration.ITALIC, false),
                plugin.tr(player, "gui.slotprefs.info.fallback").color(NamedTextColor.GRAY).decoration(
                        TextDecoration.ITALIC, false),
                Component.empty(),
                plugin.tr(player, "gui.slotprefs.info.click_primary").color(NamedTextColor.YELLOW).decoration(
                        TextDecoration.ITALIC, false),
                plugin.tr(player, "gui.slotprefs.info.click_fallback").color(NamedTextColor.YELLOW).decoration(
                        TextDecoration.ITALIC, false),
                plugin.tr(player, "gui.slotprefs.info.click_clear").color(NamedTextColor.RED).decoration(
                        TextDecoration.ITALIC, false)
        ));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createPrimarySlotItem(Player player, int slot, SlotPreference preference) {
        ItemStack item = preference == null ? item(GUIItems.SP_PRIMARY_SLOT,
                new ItemStack(Material.LIGHT_BLUE_STAINED_GLASS_PANE)) : getColorForItemType(
                preference.primary());
        ItemMeta meta = item.getItemMeta();

        String title = plugin.trText(player, "gui.slotprefs.slot.primary.title",
                java.util.Map.of("slot", String.valueOf(slot)));
        NamedTextColor color = preference == null ? NamedTextColor.GRAY : NamedTextColor.GREEN;
        meta.displayName(Component.text(title, color, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        if (preference != null) {
            lore.add(plugin.tr(player, "gui.slotprefs.slot.primary.preference",
                            java.util.Map.of("type", formatName(preference.primary().name())))
                    .color(NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
            if (preference.hasFallback()) {
                lore.add(plugin.tr(player, "gui.slotprefs.slot.fallback.preference",
                                java.util.Map.of("type", formatName(preference.fallback().name())))
                        .color(NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
            }
        } else {
            lore.add(plugin.tr(player, "gui.slotprefs.slot.no_preference").color(NamedTextColor.GRAY).decoration(
                    TextDecoration.ITALIC, false));
        }
        lore.add(Component.empty());
        lore.add(
                plugin.tr(player, "gui.slotprefs.slot.left_click").color(NamedTextColor.YELLOW).decoration(
                        TextDecoration.ITALIC, false));
        lore.add(plugin.tr(player, "gui.slotprefs.slot.right_click_clear").color(NamedTextColor.RED).decoration(
                TextDecoration.ITALIC, false));

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createFallbackSlotItem(Player player, int slot, SlotPreference preference) {
        boolean hasPreference = preference != null;
        ItemStack item = !hasPreference ? item(GUIItems.SP_FALLBACK_SLOT_DISABLED,
                new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE)) :
                (preference.hasFallback() ? getColorForItemType(
                        preference.fallback()) : item(GUIItems.SP_FALLBACK_SLOT,
                        new ItemStack(Material.GRAY_STAINED_GLASS_PANE)));
        ItemMeta meta = item.getItemMeta();

        String title = plugin.trText(player, "gui.slotprefs.slot.fallback.title",
                java.util.Map.of("slot", String.valueOf(slot)));
        NamedTextColor color = !hasPreference ? NamedTextColor.DARK_GRAY :
                (preference.hasFallback() ? NamedTextColor.GREEN : NamedTextColor.GRAY);
        meta.displayName(Component.text(title, color, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        if (hasPreference) {
            lore.add(plugin.tr(player, "gui.slotprefs.slot.primary.preference",
                            java.util.Map.of("type", formatName(preference.primary().name())))
                    .color(NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
            if (preference.hasFallback()) {
                lore.add(plugin.tr(player, "gui.slotprefs.slot.fallback.preference",
                                java.util.Map.of("type", formatName(preference.fallback().name())))
                        .color(NamedTextColor.AQUA).decoration(TextDecoration.ITALIC, false));
            } else {
                lore.add(plugin.tr(player, "gui.slotprefs.slot.no_fallback").color(NamedTextColor.GRAY).decoration(
                        TextDecoration.ITALIC, false));
            }
        } else {
            lore.add(plugin.tr(player, "gui.slotprefs.slot.set_primary_first").color(
                    NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false));
        }
        lore.add(Component.empty());
        lore.add(
                plugin.tr(player,
                                hasPreference ? "gui.slotprefs.slot.left_click" : "gui.slotprefs.slot.na_set_primary_first")
                        .color(hasPreference ? NamedTextColor.YELLOW : NamedTextColor.DARK_GRAY)
                        .decoration(TextDecoration.ITALIC, false));
        if (preference != null && preference.hasFallback()) {
            lore.add(plugin.tr(player, "gui.slotprefs.slot.right_click_clear_fallback").color(
                    NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
        }

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createItemTypeItem(Player player, ItemType itemType) {
        ItemStack item = getColorForItemType(itemType);
        ItemMeta meta = item.getItemMeta();

        NamedTextColor color = getColorForItemTypeText(itemType);
        meta.displayName(
                plugin.tr(player, "gui.slotprefs.item.name", java.util.Map.of("name", formatName(itemType.name())))
                        .color(color)
                        .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(plugin.tr(player, "gui.slotprefs.item.click_set").color(NamedTextColor.YELLOW).decoration(
                TextDecoration.ITALIC, false));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createUtilityItem(String key, Component title, List<Component> lore, ItemStack fallback) {
        ItemStack item = item(key, fallback);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        meta.displayName(title.decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack getColorForItemType(ItemType itemType) {
        return item(GUIItems.SP_ITEM_TYPE_BASE + itemType.name().toLowerCase(), switch (itemType) {
            case MOBILITY -> new ItemStack(Material.BLUE_CONCRETE);
            case UTILITY -> new ItemStack(Material.CYAN_CONCRETE);
            case OFFENSE -> new ItemStack(Material.RED_CONCRETE);
            case TRAP -> new ItemStack(Material.PURPLE_CONCRETE);
            case DEFENSE -> new ItemStack(Material.GREEN_CONCRETE);
            case HEALING -> new ItemStack(Material.LIME_CONCRETE);
            case INFORMATION -> new ItemStack(Material.YELLOW_CONCRETE);
            case SUPPORT -> new ItemStack(Material.ORANGE_CONCRETE);
        });
    }

    private NamedTextColor getColorForItemTypeText(ItemType itemType) {
        return switch (itemType) {
            case MOBILITY -> NamedTextColor.BLUE;
            case UTILITY -> NamedTextColor.AQUA;
            case OFFENSE -> NamedTextColor.RED;
            case TRAP -> NamedTextColor.LIGHT_PURPLE;
            case DEFENSE -> NamedTextColor.GREEN;
            case HEALING, SUPPORT -> NamedTextColor.YELLOW;
            case INFORMATION -> NamedTextColor.GOLD;
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

    private ItemStack item(String key, ItemStack fallback) {
        return plugin.getGuiItemRegistry().getOrDefault(GUINames.SLOT_PREFERENCES, key, fallback);
    }
}

