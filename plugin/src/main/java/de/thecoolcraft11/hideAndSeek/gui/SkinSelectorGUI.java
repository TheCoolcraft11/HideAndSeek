package de.thecoolcraft11.hideAndSeek.gui;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.gui.config.GUIItems;
import de.thecoolcraft11.hideAndSeek.gui.config.GUINames;
import de.thecoolcraft11.hideAndSeek.model.SkinData;
import de.thecoolcraft11.hideAndSeek.util.skin.SkinManager;
import de.thecoolcraft11.minigameframework.inventory.FrameworkInventory;
import de.thecoolcraft11.minigameframework.inventory.InventoryBuilder;
import de.thecoolcraft11.minigameframework.inventory.InventoryItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

public class SkinSelectorGUI {

    private final HideAndSeek plugin;
    private final SkinManager skinManager;

    public SkinSelectorGUI(HideAndSeek plugin, SkinManager skinManager) {
        this.plugin = plugin;
        this.skinManager = skinManager;
    }

    public void open(Player player) {

        String mapName = HideAndSeek.getDataController().getCurrentMapName();

        List<String> allowedSkinIds = mapName == null
                ? List.of()
                : plugin.getMapManager().getAllowedSkinsForMap(mapName);

        List<SkinData> skins = skinManager.resolveSkins(allowedSkinIds);

        if (skins.isEmpty()) {
            player.sendMessage(plugin.tr(player, "gui.skin_selector.errors.no_configured"));
            return;
        }

        int rows = Math.min(6, (skins.size() + 8) / 9);

        FrameworkInventory inventory = new InventoryBuilder(plugin.getInventoryFramework())
                .id("skin_selector_" + player.getUniqueId())
                .title(plugin.trText(player, "gui.skin_selector.title"))
                .rows(rows)
                .allowOutsideClicks(false)
                .allowDrag(false)
                .allowPlayerInventoryInteraction(false)
                .build();

        String currentSkinId = skinManager.getAssignedSkinId(player.getUniqueId());

        int slot = 0;

        for (SkinData skin : skins) {

            boolean selected = skin.id().equals(currentSkinId);

            ItemStack item = createSkinItem(player, skin, selected);

            InventoryItem inventoryItem = getInventoryItem(skin, item, selected);

            inventory.setItem(slot, inventoryItem);

            slot++;
        }

        plugin.getInventoryFramework().openInventory(player, inventory);
    }

    private @NonNull InventoryItem getInventoryItem(SkinData skin, ItemStack item, boolean selected) {
        InventoryItem inventoryItem = new InventoryItem(item);

        inventoryItem.setMetadata("skin_id", skin.id());
        inventoryItem.setMetadata("selected", selected);

        inventoryItem.setClickHandler((p, invItem, event, s) -> {

            event.setCancelled(true);

            if (event.getClick() != ClickType.LEFT) {
                return;
            }

            String assignedSkin = skinManager.getAssignedSkinId(p.getUniqueId());

            if (skin.id().equals(assignedSkin)) {
                p.sendMessage(
                        plugin.tr(p, "gui.skin_selector.errors.already_selected")
                );
                return;
            }

            skinManager.assignSkin(p, skin);

            p.sendMessage(
                    plugin.tr(p, "gui.skin_selector.feedback.changed", java.util.Map.of("skin", skin.name()))
            );

            p.closeInventory();

        });

        inventoryItem.setAllowTakeout(false);
        inventoryItem.setAllowInsert(false);
        return inventoryItem;
    }

    private ItemStack createSkinItem(Player viewer, SkinData skin, boolean selected) {

        ItemStack icon = skin.icon() != null
                ? skin.icon()
                : plugin.getGuiItemRegistry().getOrDefault(GUINames.SKIN_SELECTOR, GUIItems.KEY_FALLBACK, new ItemStack(
                Material.PLAYER_HEAD));

        ItemStack item = new ItemStack(icon);

        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            return item;
        }

        meta.displayName(
                plugin.tr(
                                viewer,
                                selected
                                        ? "gui.skin_selector.item.name_selected"
                                        : "gui.skin_selector.item.name",
                                java.util.Map.of("name", skin.name())
                        )
                        .decoration(TextDecoration.ITALIC, false)
        );

        List<Component> lore = new ArrayList<>();

        lore.add(
                plugin.tr(viewer, "gui.skin_selector.item.click_to_apply")
                        .decoration(TextDecoration.ITALIC, false)
        );

        if (selected) {
            lore.add(
                    plugin.tr(viewer, "gui.skin_selector.item.currently_active")
                            .decoration(TextDecoration.ITALIC, false)
            );
        }

        meta.lore(lore);

        item.setItemMeta(meta);

        return item;
    }
}