package de.thecoolcraft11.hideAndSeek.gui;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.loadout.LoadoutManager;
import de.thecoolcraft11.hideAndSeek.loadout.PlayerLoadout;
import de.thecoolcraft11.hideAndSeek.model.ItemType;
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


        InventoryItem infoItem = new InventoryItem(createInfoItem());
        infoItem.setClickHandler((p, item, event, slot) -> event.setCancelled(true));
        infoItem.setAllowTakeout(false);
        infoItem.setAllowInsert(false);
        inv.setItem(4, infoItem);


        int startSlot = isHider ? 0 : 1;
        int endSlot = 9;
        int invSlot = 9;

        for (int slot = startSlot; slot < endSlot; slot++) {
            final int currentSlot = slot;
            ItemType preference = isHider ? loadout.getHiderSlotPreference(slot) : loadout.getSeekerSlotPreference(
                    slot);

            InventoryItem slotItem = new InventoryItem(createSlotItem(slot, preference));
            slotItem.setClickHandler((p, item, event, s) -> {
                if (event.isRightClick()) {

                    if (isHider) {
                        loadout.setHiderSlotPreference(currentSlot, null);
                    } else {
                        loadout.setSeekerSlotPreference(currentSlot, null);
                    }
                    loadoutManager.saveLoadout(p.getUniqueId());
                    p.playSound(p.getLocation(), Sound.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
                    p.sendMessage(Component.text("Cleared preference for slot " + currentSlot, NamedTextColor.YELLOW));
                    open(p, isHider);
                } else {

                    openItemTypeSelector(p, currentSlot, isHider);
                }
                event.setCancelled(true);
            });
            slotItem.setAllowTakeout(false);
            slotItem.setAllowInsert(false);
            inv.setItem(invSlot++, slotItem);
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

    private void openItemTypeSelector(Player player, int slot, boolean isHider) {
        FrameworkInventory inv = new de.thecoolcraft11.minigameframework.inventory.InventoryBuilder(
                plugin.getInventoryFramework())
                .id("item_type_selector_" + player.getUniqueId() + "_" + slot)
                .title("Select ItemType for Slot " + slot)
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
                if (isHider) {
                    loadout.setHiderSlotPreference(slot, itemType);
                } else {
                    loadout.setSeekerSlotPreference(slot, itemType);
                }
                loadoutManager.saveLoadout(p.getUniqueId());
                p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
                p.sendMessage(Component.text("Set slot " + slot + " preference to " + formatName(itemType.name()),
                        NamedTextColor.GREEN));
                open(p, isHider);
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

    private ItemStack createInfoItem() {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.text("Slot Preferences", NamedTextColor.AQUA, TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(Arrays.asList(
                Component.text("Configure preferred ItemTypes for each slot", NamedTextColor.GRAY).decoration(
                        TextDecoration.ITALIC, false),
                Component.text("Items matching the slot preference will be placed first",
                        NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                Component.text("Remaining items fill other slots in order", NamedTextColor.GRAY).decoration(
                        TextDecoration.ITALIC, false),
                Component.empty(),
                Component.text("Left click a slot to select ItemType", NamedTextColor.YELLOW).decoration(
                        TextDecoration.ITALIC, false),
                Component.text("Right click a slot to clear it", NamedTextColor.YELLOW).decoration(
                        TextDecoration.ITALIC, false)
        ));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createSlotItem(int slot, ItemType preference) {
        Material material = preference == null ? Material.GRAY_STAINED_GLASS_PANE : getColorForItemType(preference);
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();

        String title = "Slot " + slot;
        NamedTextColor color = preference == null ? NamedTextColor.GRAY : NamedTextColor.GREEN;
        meta.displayName(Component.text(title, color, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        if (preference != null) {
            lore.add(Component.text("Preference: " + formatName(preference.name()), NamedTextColor.GOLD).decoration(
                    TextDecoration.ITALIC, false));
        } else {
            lore.add(Component.text("No preference set", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        }
        lore.add(Component.empty());
        lore.add(
                Component.text("Left click to select ItemType", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC,
                        false));
        lore.add(Component.text("Right click to clear preference", NamedTextColor.RED).decoration(TextDecoration.ITALIC,
                false));

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
            case HEALING -> NamedTextColor.YELLOW;
            case INFORMATION -> NamedTextColor.GOLD;
            case SUPPORT -> NamedTextColor.YELLOW;
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




