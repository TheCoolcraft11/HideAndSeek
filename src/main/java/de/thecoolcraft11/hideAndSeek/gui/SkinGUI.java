package de.thecoolcraft11.hideAndSeek.gui;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.items.ItemSkinSelectionService;
import de.thecoolcraft11.hideAndSeek.model.ItemRarity;
import de.thecoolcraft11.minigameframework.inventory.FrameworkInventory;
import de.thecoolcraft11.minigameframework.inventory.InventoryBuilder;
import de.thecoolcraft11.minigameframework.inventory.InventoryItem;
import de.thecoolcraft11.minigameframework.items.variants.ItemVariant;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class SkinGUI {
    private static final String ITEMS_TITLE = "Skin Selector";
    private static final String VARIANTS_TITLE_PREFIX = "Skin Variants: ";

    private final HideAndSeek plugin;

    public SkinGUI(HideAndSeek plugin) {
        this.plugin = plugin;
    }

    private static String humanize(String id) {
        String shortId = id
                .replace("has_hider_", "")
                .replace("has_seeker_", "")
                .replace('_', ' ');
        String[] parts = shortId.split(" ");
        StringBuilder out = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (!out.isEmpty()) {
                out.append(' ');
            }
            out.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return out.toString();
    }

    public void open(Player player) {
        FrameworkInventory inventory = new InventoryBuilder(plugin.getInventoryFramework())
                .id("skin_selector_" + player.getUniqueId())
                .title(ITEMS_TITLE)
                .rows(6)
                .allowOutsideClicks(false)
                .allowDrag(false)
                .allowPlayerInventoryInteraction(false)
                .build();

        List<String> logicalItems = getLogicalItems();
        int slot = 0;
        for (String logicalItemId : logicalItems) {
            if (slot >= 45) {
                break;
            }
            InventoryItem skinItem = new InventoryItem(createLogicalItemButton(player, logicalItemId));
            skinItem.setClickHandler((p, item, event, s) -> {
                p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.1f);
                openVariants(p, logicalItemId);
                event.setCancelled(true);
            });
            skinItem.setAllowTakeout(false);
            skinItem.setAllowInsert(false);
            skinItem.setMetadata("logical_item_id", logicalItemId);
            inventory.setItem(slot++, skinItem);
        }

        InventoryItem backItem = new InventoryItem(createBackHint());
        backItem.setClickHandler((p, item, event, s) -> {
            p.closeInventory();
            event.setCancelled(true);
        });
        backItem.setAllowTakeout(false);
        backItem.setAllowInsert(false);
        inventory.setItem(49, backItem);

        InventoryItem coinsItem = new InventoryItem(createCoinsHint(player));
        coinsItem.setClickHandler((p, item, event, s) -> event.setCancelled(true));
        coinsItem.setAllowTakeout(false);
        coinsItem.setAllowInsert(false);
        inventory.setItem(50, coinsItem);

        plugin.getInventoryFramework().openInventory(player, inventory);
    }

    private void openVariants(Player player, String logicalItemId) {
        String runtimeItemId = ItemSkinSelectionService.resolveRuntimeItemId(player, logicalItemId);
        List<ItemVariant> variants = plugin.getCustomItemManager().getVariantManager().getVariants(runtimeItemId);

        FrameworkInventory inventory = new InventoryBuilder(plugin.getInventoryFramework())
                .id("skin_variants_" + player.getUniqueId() + "_" + logicalItemId)
                .title(VARIANTS_TITLE_PREFIX + humanize(logicalItemId))
                .rows(6)
                .allowOutsideClicks(false)
                .allowDrag(false)
                .allowPlayerInventoryInteraction(false)
                .build();

        int slot = 0;
        String selected = ItemSkinSelectionService.getSelectedVariant(player, logicalItemId);
        if (selected != null && !ItemSkinSelectionService.isUnlocked(player.getUniqueId(), logicalItemId, selected)) {
            selected = null;
        }
        for (ItemVariant variant : variants) {
            if (slot >= 45) {
                break;
            }
            String variantId = variant.getId();
            InventoryItem variantItem = new InventoryItem(createVariantButton(player, logicalItemId, variant, selected));
            variantItem.setClickHandler((p, item, event, s) -> {
                handleVariantClick(p, logicalItemId, variantId, event.getClick());
                event.setCancelled(true);
            });
            variantItem.setAllowTakeout(false);
            variantItem.setAllowInsert(false);
            variantItem.setMetadata("variant_id", variantId);
            variantItem.setMetadata("logical_item_id", logicalItemId);
            inventory.setItem(slot++, variantItem);
        }

        InventoryItem backBtn = new InventoryItem(createUtility(Material.ARROW, "Back", NamedTextColor.YELLOW,
                List.of(Component.text("Return to skin item list", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false))));
        backBtn.setClickHandler((p, item, event, s) -> {
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 0.9f);
            open(p);
            event.setCancelled(true);
        });
        backBtn.setAllowTakeout(false);
        backBtn.setAllowInsert(false);
        inventory.setItem(45, backBtn);

        InventoryItem clearBtn = new InventoryItem(createUtility(Material.BARRIER, "Clear Selection", NamedTextColor.RED,
                List.of(Component.text("Remove saved skin for this item", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false))));
        clearBtn.setClickHandler((p, item, event, s) -> {
            ItemSkinSelectionService.clearSelectedVariant(p.getUniqueId(), logicalItemId);
            ItemSkinSelectionService.savePlayer(plugin, p.getUniqueId());
            p.sendMessage(Component.text("Cleared saved skin for ", NamedTextColor.YELLOW)
                    .append(Component.text(humanize(logicalItemId), NamedTextColor.GOLD)));
            p.playSound(p.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.7f, 1.1f);
            openVariants(p, logicalItemId);
            event.setCancelled(true);
        });
        clearBtn.setAllowTakeout(false);
        clearBtn.setAllowInsert(false);
        inventory.setItem(53, clearBtn);

        InventoryItem coinsItem = new InventoryItem(createCoinsHint(player));
        coinsItem.setClickHandler((p, item, event, s) -> event.setCancelled(true));
        coinsItem.setAllowTakeout(false);
        coinsItem.setAllowInsert(false);
        inventory.setItem(49, coinsItem);

        plugin.getInventoryFramework().openInventory(player, inventory);
    }


    private void handleVariantClick(Player player, String logicalItemId, String variantId, ClickType clickType) {
        boolean unlocked = ItemSkinSelectionService.isUnlocked(player.getUniqueId(), logicalItemId, variantId);
        if (!unlocked) {
            int cost = ItemSkinSelectionService.getCost(plugin, logicalItemId, variantId);
            if (!ItemSkinSelectionService.unlock(plugin, player.getUniqueId(), logicalItemId, variantId)) {
                player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 0.8f, 0.9f);
                player.sendMessage(Component.text("Not enough coins. Need ", NamedTextColor.RED)
                        .append(Component.text(cost, NamedTextColor.GOLD))
                        .append(Component.text(" to unlock this skin.", NamedTextColor.RED)));
                openVariants(player, logicalItemId);
                return;
            }

            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.9f, 1.2f);
            player.sendMessage(Component.text("Unlocked skin ", NamedTextColor.GREEN)
                    .append(Component.text(variantId, NamedTextColor.GOLD))
                    .append(Component.text(" for ", NamedTextColor.GREEN))
                    .append(Component.text(cost + " coins", NamedTextColor.YELLOW)));
        }

        ItemSkinSelectionService.setSelectedVariant(player.getUniqueId(), logicalItemId, variantId);
        ItemSkinSelectionService.savePlayer(plugin, player.getUniqueId());
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.8f, 1.3f);
        player.sendMessage(Component.text("Selected skin ", NamedTextColor.GREEN)
                .append(Component.text(variantId, NamedTextColor.GOLD))
                .append(Component.text(" for ", NamedTextColor.GREEN))
                .append(Component.text(humanize(logicalItemId), NamedTextColor.YELLOW)));

        if (clickType == ClickType.RIGHT) {
            player.closeInventory();
            return;
        }

        openVariants(player, logicalItemId);
    }

    private ItemStack createLogicalItemButton(Player player, String logicalItemId) {
        String runtimeItemId = ItemSkinSelectionService.resolveRuntimeItemId(player, logicalItemId);
        var customItem = plugin.getCustomItemManager().getItem(runtimeItemId);

        ItemStack stack = customItem != null ? customItem.getItemStack().clone() : new ItemStack(Material.PAPER);
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return stack;
        }

        String selected = ItemSkinSelectionService.getSelectedVariant(player, logicalItemId);
        int unlockedCount = (int) plugin.getCustomItemManager().getVariantManager().getVariants(runtimeItemId).stream()
                .filter(variant -> ItemSkinSelectionService.isUnlocked(player.getUniqueId(), logicalItemId, variant.getId()))
                .count();
        int variantCount = plugin.getCustomItemManager().getVariantManager().getVariants(runtimeItemId).size();

        meta.displayName(Component.text(humanize(logicalItemId), NamedTextColor.AQUA, TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Unlocked: " + unlockedCount + "/" + variantCount, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Selected: " + ((selected == null || selected.isBlank()) ? "Default" : selected),
                NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Left click to open", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        meta.getPersistentDataContainer().set(
                new org.bukkit.NamespacedKey(plugin, "skin_item_id"),
                org.bukkit.persistence.PersistentDataType.STRING,
                logicalItemId
        );
        stack.setItemMeta(meta);
        return stack;
    }

    private ItemStack createVariantButton(Player player, String logicalItemId, ItemVariant variant, String selectedVariant) {
        ItemStack stack = variant.getItemStack().clone();
        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return stack;
        }

        String label = variant.getDisplayName() == null || variant.getDisplayName().isBlank()
                ? variant.getId()
                : variant.getDisplayName();
        boolean selected = variant.getId().equals(selectedVariant);
        boolean unlocked = ItemSkinSelectionService.isUnlocked(player.getUniqueId(), logicalItemId, variant.getId());
        int cost = ItemSkinSelectionService.getCost(plugin, logicalItemId, variant.getId());
        ItemRarity rarity = ItemSkinSelectionService.getRarity(logicalItemId, variant.getId());

        meta.displayName(Component.text(label,
                        selected ? NamedTextColor.GREEN : (unlocked ? NamedTextColor.AQUA : NamedTextColor.RED),
                        TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("ID: " + variant.getId(), NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Rarity: " + rarity.name(), getRarityColor(rarity)).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Cost: " + cost + " coins", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text(selected ? "Currently selected" : (unlocked ? "Click to select" : "Click to unlock + select"),
                        selected ? NamedTextColor.GREEN : NamedTextColor.YELLOW)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Right click: select/unlock and close", NamedTextColor.DARK_GRAY)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);

        meta.getPersistentDataContainer().set(
                new org.bukkit.NamespacedKey(plugin, "skin_variant_id"),
                org.bukkit.persistence.PersistentDataType.STRING,
                variant.getId()
        );
        stack.setItemMeta(meta);
        return stack;
    }

    private ItemStack createBackHint() {
        return createUtility(Material.BOOK, "Skin Selection",
                NamedTextColor.YELLOW,
                List.of(
                        Component.text("Pick an item first", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                        Component.text("Then buy and equip skin variants", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                ));
    }

    private ItemStack createCoinsHint(Player player) {
        int coins = ItemSkinSelectionService.getCoins(player.getUniqueId());
        return createUtility(Material.GOLD_NUGGET, "Coins: " + coins,
                NamedTextColor.GOLD,
                List.of(Component.text("Earn coins from round points", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
    }

    private NamedTextColor getRarityColor(ItemRarity rarity) {
        return switch (rarity) {
            case COMMON -> NamedTextColor.WHITE;
            case UNCOMMON -> NamedTextColor.GREEN;
            case RARE -> NamedTextColor.BLUE;
            case EPIC -> NamedTextColor.DARK_PURPLE;
            case LEGENDARY -> NamedTextColor.GOLD;
        };
    }

    private ItemStack createUtility(Material material, String name, NamedTextColor color, List<Component> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(name, color, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private List<String> getLogicalItems() {
        Set<String> ids = new TreeSet<>();
        for (String itemId : plugin.getCustomItemManager().getVariantManager().getAllVariants().keySet()) {
            ids.add(ItemSkinSelectionService.normalizeLogicalItemId(itemId));
        }
        return new ArrayList<>(ids);
    }
}


