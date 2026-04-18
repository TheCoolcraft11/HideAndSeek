package de.thecoolcraft11.hideAndSeek.gui;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
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
            player.sendMessage(
                    Component.text("Slot preferences can only be modified in the lobby!", NamedTextColor.RED));
            return;
        }

        int maxItems = isHider ? loadoutManager.getMaxHiderItems() : loadoutManager.getMaxSeekerItems();

        FrameworkInventory inv = new de.thecoolcraft11.minigameframework.inventory.InventoryBuilder(
                plugin.getInventoryFramework())
                .id("slot_prefs_" + player.getUniqueId() + "_" + (isHider ? "hider" : "seeker"))
                .title(isHider ? "Hider Slot Preferences" : "Seeker Slot Preferences")
                .rows(6)
                .allowOutsideClicks(false)
                .allowDrag(false)
                .allowPlayerInventoryInteraction(false)
                .build();

        PlayerLoadout loadout = loadoutManager.getLoadout(player.getUniqueId());


        InventoryItem infoItem = new InventoryItem(createInfoItem(maxItems));
        infoItem.setClickHandler((p, item, event, slot) -> event.setCancelled(true));
        infoItem.setAllowTakeout(false);
        infoItem.setAllowInsert(false);
        inv.setItem(4, infoItem);



        int startSlot = isHider ? 0 : 1;
        for (int i = 0; i < maxItems; i++) {
            final int slotNumber = startSlot + i;
            SlotPreference preference = isHider ? loadout.getHiderSlotPreference(
                    slotNumber) : loadout.getSeekerSlotPreference(slotNumber);

            InventoryItem slotItem = new InventoryItem(createPrimarySlotItem(i, preference));
            slotItem.setClickHandler((p, item, event, s) -> {
                if (event.isRightClick()) {

                    if (isHider) {
                        loadout.setHiderSlotPreference(slotNumber, null, null);
                    } else {
                        loadout.setSeekerSlotPreference(slotNumber, null, null);
                    }
                    loadoutManager.saveLoadout(p.getUniqueId());
                    p.playSound(p.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
                    p.sendMessage(Component.text("Cleared preference for slot " + slotNumber, NamedTextColor.YELLOW));
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

            InventoryItem slotItem = new InventoryItem(createFallbackSlotItem(i, preference));
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
                        p.sendMessage(Component.text("Cleared fallback for slot " + slotNumber, NamedTextColor.YELLOW));
                    }
                    open(p, isHider);
                } else {

                    SlotPreference current = isHider ? loadout.getHiderSlotPreference(
                            slotNumber) : loadout.getSeekerSlotPreference(slotNumber);
                    if (current != null) {
                        openPreferenceSelector(p, slotNumber, isHider, true);
                    } else {
                        p.sendMessage(Component.text("Set a primary preference first!", NamedTextColor.RED));
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
                createUtilityItem(Material.BARRIER, "Clear All Preferences", NamedTextColor.RED,
                        List.of(Component.text("Click to clear all slot preferences", NamedTextColor.GRAY).decoration(
                                TextDecoration.ITALIC, false))));
        clearAllButton.setClickHandler((p, item, event, slot) -> {
            if (isHider) {
                loadout.clearHiderSlotPreferences();
            } else {
                loadout.clearSeekerSlotPreferences();
            }
            loadoutManager.saveLoadout(p.getUniqueId());
            p.playSound(p.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 0.8f);
            p.sendMessage(Component.text("Cleared all slot preferences", NamedTextColor.YELLOW));
            open(p, isHider);
            event.setCancelled(true);
        });
        clearAllButton.setAllowTakeout(false);
        clearAllButton.setAllowInsert(false);
        inv.setItem(45, clearAllButton);


        InventoryItem backButton = new InventoryItem(
                createUtilityItem(Material.ARROW, "Back to Loadout", NamedTextColor.YELLOW,
                        List.of(Component.text("Return to loadout editor", NamedTextColor.GRAY).decoration(
                                TextDecoration.ITALIC, false))));
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
                .title("Select " + (isFallback ? "Fallback" : "Primary") + " ItemType for Slot " + slot)
                .rows(3)
                .allowOutsideClicks(false)
                .allowDrag(false)
                .allowPlayerInventoryInteraction(false)
                .build();

        int slotIndex = 9;
        for (ItemType itemType : ItemType.values()) {
            InventoryItem typeItem = new InventoryItem(createItemTypeItem(itemType));
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
                    p.sendMessage(
                            Component.text("Set slot " + slot + " primary preference to " + formatName(itemType.name()),
                                    NamedTextColor.GREEN));
                    p.sendMessage(Component.text(
                            "You can now set a fallback preference. Left-click a slot again if you don't want a fallback.",
                            NamedTextColor.YELLOW));
                    open(p, isHider);
                } else {
                    p.sendMessage(Component.text(
                            "Set slot " + slot + " fallback preference to " + formatName(itemType.name()),
                            NamedTextColor.GREEN));
                    open(p, isHider);
                }
                event.setCancelled(true);
            });
            typeItem.setAllowTakeout(false);
            typeItem.setAllowInsert(false);
            inv.setItem(slotIndex++, typeItem);
        }


        InventoryItem backButton = new InventoryItem(createUtilityItem(Material.ARROW, "Back", NamedTextColor.YELLOW,
                List.of(Component.text("Return to slot preferences", NamedTextColor.GRAY).decoration(
                        TextDecoration.ITALIC, false))));
        backButton.setClickHandler((p, item, event, s) -> {
            open(p, isHider);
            event.setCancelled(true);
        });
        backButton.setAllowTakeout(false);
        backButton.setAllowInsert(false);
        inv.setItem(22, backButton);

        plugin.getInventoryFramework().openInventory(player, inv);
    }

    private ItemStack createInfoItem(int maxItems) {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Slot Preferences", NamedTextColor.AQUA, TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(Arrays.asList(
                Component.text("Configure " + maxItems + " slot preferences", NamedTextColor.GRAY).decoration(
                        TextDecoration.ITALIC, false),
                Component.text("TOP ROW: Primary Preferences", NamedTextColor.GREEN, TextDecoration.BOLD).decoration(
                        TextDecoration.ITALIC, false),
                Component.text("BOTTOM ROW: Fallback Preferences", NamedTextColor.YELLOW,
                        TextDecoration.BOLD).decoration(
                        TextDecoration.ITALIC, false),
                Component.empty(),
                Component.text("Primary: Item placed first if available", NamedTextColor.GRAY).decoration(
                        TextDecoration.ITALIC, false),
                Component.text("Fallback: Used if primary item unavailable", NamedTextColor.GRAY).decoration(
                        TextDecoration.ITALIC, false),
                Component.empty(),
                Component.text("Left click PRIMARY to select ItemType", NamedTextColor.YELLOW).decoration(
                        TextDecoration.ITALIC, false),
                Component.text("Left click FALLBACK to add backup (if primary set)", NamedTextColor.YELLOW).decoration(
                        TextDecoration.ITALIC, false),
                Component.text("Right click any slot to clear it", NamedTextColor.RED).decoration(
                        TextDecoration.ITALIC, false)
        ));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createPrimarySlotItem(int slot, SlotPreference preference) {
        Material material = preference == null ? Material.LIGHT_BLUE_STAINED_GLASS_PANE : getColorForItemType(
                preference.primary());
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        String title = "Slot " + slot + " (Primary)";
        NamedTextColor color = preference == null ? NamedTextColor.GRAY : NamedTextColor.GREEN;
        meta.displayName(Component.text(title, color, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        if (preference != null) {
            lore.add(Component.text("Preference: " + formatName(preference.primary().name()),
                    NamedTextColor.GOLD).decoration(
                    TextDecoration.ITALIC, false));
            if (preference.hasFallback()) {
                lore.add(Component.text("Fallback: " + formatName(preference.fallback().name()),
                        NamedTextColor.AQUA).decoration(
                        TextDecoration.ITALIC, false));
            }
        } else {
            lore.add(Component.text("No preference set", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        }
        lore.add(Component.empty());
        lore.add(
                Component.text("Left click to set/modify", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC,
                        false));
        lore.add(Component.text("Right click to clear all", NamedTextColor.RED).decoration(TextDecoration.ITALIC,
                false));

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createFallbackSlotItem(int slot, SlotPreference preference) {
        boolean hasPreference = preference != null;
        Material material = !hasPreference ? Material.LIGHT_GRAY_STAINED_GLASS_PANE :
                (preference.hasFallback() ? getColorForItemType(
                        preference.fallback()) : Material.GRAY_STAINED_GLASS_PANE);
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        String title = "Slot " + slot + " (Fallback)";
        NamedTextColor color = !hasPreference ? NamedTextColor.DARK_GRAY :
                (preference.hasFallback() ? NamedTextColor.GREEN : NamedTextColor.GRAY);
        meta.displayName(Component.text(title, color, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        if (hasPreference) {
            lore.add(Component.text("Primary: " + formatName(preference.primary().name()),
                    NamedTextColor.GOLD).decoration(
                    TextDecoration.ITALIC, false));
            if (preference.hasFallback()) {
                lore.add(Component.text("Fallback: " + formatName(preference.fallback().name()),
                        NamedTextColor.AQUA).decoration(
                        TextDecoration.ITALIC, false));
            } else {
                lore.add(Component.text("No fallback set", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC,
                        false));
            }
        } else {
            lore.add(Component.text("Set primary preference first", NamedTextColor.DARK_GRAY).decoration(
                    TextDecoration.ITALIC, false));
        }
        lore.add(Component.empty());
        lore.add(
                Component.text(hasPreference ? "Left click to set/modify" : "N/A (set primary first)",
                        hasPreference ? NamedTextColor.YELLOW : NamedTextColor.DARK_GRAY).decoration(
                        TextDecoration.ITALIC,
                        false));
        if (preference != null && preference.hasFallback()) {
            lore.add(Component.text("Right click to clear fallback", NamedTextColor.RED).decoration(
                    TextDecoration.ITALIC,
                    false));
        }

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createItemTypeItem(ItemType itemType) {
        Material material = getColorForItemType(itemType);
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        NamedTextColor color = getColorForItemTypeText(itemType);
        meta.displayName(Component.text(formatName(itemType.name()), color, TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Click to set this as the preference", NamedTextColor.YELLOW).decoration(
                TextDecoration.ITALIC, false));
        meta.lore(lore);
        item.setItemMeta(meta);
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

    private Material getColorForItemType(ItemType itemType) {
        return switch (itemType) {
            case MOBILITY -> Material.BLUE_CONCRETE;
            case UTILITY -> Material.CYAN_CONCRETE;
            case OFFENSE -> Material.RED_CONCRETE;
            case TRAP -> Material.PURPLE_CONCRETE;
            case DEFENSE -> Material.GREEN_CONCRETE;
            case HEALING -> Material.LIME_CONCRETE;
            case INFORMATION -> Material.YELLOW_CONCRETE;
            case SUPPORT -> Material.ORANGE_CONCRETE;
        };
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
}










