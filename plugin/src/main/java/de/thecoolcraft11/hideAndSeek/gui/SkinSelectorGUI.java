package de.thecoolcraft11.hideAndSeek.gui;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.model.SkinData;
import de.thecoolcraft11.hideAndSeek.util.skin.SkinManager;
import de.thecoolcraft11.minigameframework.inventory.FrameworkInventory;
import de.thecoolcraft11.minigameframework.inventory.InventoryBuilder;
import de.thecoolcraft11.minigameframework.inventory.InventoryItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
            player.sendMessage(
                    Component.text(
                            "No skins are configured for this map.",
                            NamedTextColor.RED
                    )
            );
            return;
        }

        int rows = Math.min(6, (skins.size() + 8) / 9);

        FrameworkInventory inventory = new InventoryBuilder(plugin.getInventoryFramework())
                .id("skin_selector_" + player.getUniqueId())
                .title("Choose Your Skin")
                .rows(rows)
                .allowOutsideClicks(false)
                .allowDrag(false)
                .allowPlayerInventoryInteraction(false)
                .build();

        String currentSkinId = skinManager.getAssignedSkinId(player.getUniqueId());

        int slot = 0;

        for (SkinData skin : skins) {

            boolean selected = skin.id().equals(currentSkinId);

            ItemStack item = createSkinItem(skin, selected);

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
                        Component.text(
                                "You are already using that skin.",
                                NamedTextColor.YELLOW
                        )
                );
                return;
            }

            skinManager.assignSkin(p, skin);

            p.sendMessage(
                    Component.text(
                            "Skin changed to ",
                            NamedTextColor.GREEN
                    ).append(
                            Component.text(
                                    skin.name(),
                                    NamedTextColor.GOLD
                            )
                    )
            );

            p.closeInventory();

        });

        inventoryItem.setAllowTakeout(false);
        inventoryItem.setAllowInsert(false);
        return inventoryItem;
    }

    private ItemStack createSkinItem(SkinData skin, boolean selected) {

        Material icon = skin.icon() != null
                ? skin.icon()
                : Material.PLAYER_HEAD;

        ItemStack item = new ItemStack(icon);

        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            return item;
        }

        meta.displayName(
                Component.text(
                                skin.name(),
                                selected
                                        ? NamedTextColor.GREEN
                                        : NamedTextColor.WHITE,
                                TextDecoration.BOLD
                        )
                        .decoration(TextDecoration.ITALIC, false)
        );

        List<Component> lore = new ArrayList<>();

        lore.add(
                Component.text(
                                "Click to apply this skin",
                                NamedTextColor.GRAY
                        )
                        .decoration(TextDecoration.ITALIC, false)
        );

        if (selected) {
            lore.add(
                    Component.text(
                                    "✔ Currently active",
                                    NamedTextColor.GREEN
                            )
                            .decoration(TextDecoration.ITALIC, false)
            );
        }

        meta.lore(lore);

        item.setItemMeta(meta);

        return item;
    }
}