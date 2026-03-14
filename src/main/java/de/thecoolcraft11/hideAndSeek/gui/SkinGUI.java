package de.thecoolcraft11.hideAndSeek.gui;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.items.ItemSkinSelectionService;
import de.thecoolcraft11.hideAndSeek.model.ItemRarity;
import de.thecoolcraft11.minigameframework.items.variants.ItemVariant;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class SkinGUI implements Listener {
    private static final String ITEMS_TITLE = "Skin Selector";
    private static final String VARIANTS_TITLE_PREFIX = "Skin Variants: ";

    private final HideAndSeek plugin;
    private final Map<UUID, String> activeLogicalItem = new HashMap<>();

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
        Inventory inventory = Bukkit.createInventory(null, 54, Component.text(ITEMS_TITLE, NamedTextColor.GOLD));

        List<String> logicalItems = getLogicalItems();
        int slot = 0;
        for (String logicalItemId : logicalItems) {
            if (slot >= 45) {
                break;
            }
            inventory.setItem(slot++, createLogicalItemButton(player, logicalItemId));
        }

        inventory.setItem(49, createBackHint());
        inventory.setItem(50, createCoinsHint(player));
        player.openInventory(inventory);
    }

    private void openVariants(Player player, String logicalItemId) {
        String runtimeItemId = ItemSkinSelectionService.resolveRuntimeItemId(player, logicalItemId);
        List<ItemVariant> variants = plugin.getCustomItemManager().getVariantManager().getVariants(runtimeItemId);

        Inventory inventory = Bukkit.createInventory(
                null,
                54,
                Component.text(VARIANTS_TITLE_PREFIX + humanize(logicalItemId), NamedTextColor.GOLD)
        );

        int slot = 0;
        String selected = ItemSkinSelectionService.getSelectedVariant(player, logicalItemId);
        if (selected != null && !ItemSkinSelectionService.isUnlocked(player.getUniqueId(), logicalItemId, selected)) {
            selected = null;
        }
        for (ItemVariant variant : variants) {
            if (slot >= 45) {
                break;
            }
            inventory.setItem(slot++, createVariantButton(player, logicalItemId, variant, selected));
        }

        inventory.setItem(45, createUtility(Material.ARROW, "Back", NamedTextColor.YELLOW,
                List.of(Component.text("Return to skin item list", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false))));
        inventory.setItem(53, createUtility(Material.BARRIER, "Clear Selection", NamedTextColor.RED,
                List.of(Component.text("Remove saved skin for this item", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false))));
        inventory.setItem(49, createCoinsHint(player));

        activeLogicalItem.put(player.getUniqueId(), logicalItemId);
        player.openInventory(inventory);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }

        String title = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText()
                .serialize(event.getView().title());

        if (!ITEMS_TITLE.equals(title) && !title.startsWith(VARIANTS_TITLE_PREFIX)) {
            return;
        }

        event.setCancelled(true);
        if (event.getRawSlot() < 0 || event.getRawSlot() >= event.getView().getTopInventory().getSize()) {
            return;
        }

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) {
            return;
        }

        if (ITEMS_TITLE.equals(title)) {
            handleItemsClick(player, event.getRawSlot(), clicked);
            return;
        }

        handleVariantsClick(player, event.getRawSlot(), clicked, event.getClick());
    }

    private void handleItemsClick(Player player, int slot, ItemStack clicked) {
        if (slot == 49) {
            player.closeInventory();
            return;
        }

        if (!clicked.hasItemMeta() || clicked.getItemMeta() == null) {
            return;
        }

        var container = clicked.getItemMeta().getPersistentDataContainer();
        String logicalItemId = container.get(new org.bukkit.NamespacedKey(plugin, "skin_item_id"), org.bukkit.persistence.PersistentDataType.STRING);
        if (logicalItemId == null) {
            return;
        }

        player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.1f);
        openVariants(player, logicalItemId);
    }

    private void handleVariantsClick(Player player, int slot, ItemStack clicked, ClickType clickType) {
        String logicalItemId = activeLogicalItem.get(player.getUniqueId());
        if (logicalItemId == null) {
            open(player);
            return;
        }

        if (slot == 45) {
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 0.9f);
            open(player);
            return;
        }

        if (slot == 53) {
            ItemSkinSelectionService.clearSelectedVariant(player.getUniqueId(), logicalItemId);
            ItemSkinSelectionService.savePlayer(plugin, player.getUniqueId());
            player.sendMessage(Component.text("Cleared saved skin for ", NamedTextColor.YELLOW)
                    .append(Component.text(humanize(logicalItemId), NamedTextColor.GOLD)));
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.7f, 1.1f);
            openVariants(player, logicalItemId);
            return;
        }

        if (!clicked.hasItemMeta() || clicked.getItemMeta() == null) {
            return;
        }

        var container = clicked.getItemMeta().getPersistentDataContainer();
        String variantId = container.get(new org.bukkit.NamespacedKey(plugin, "skin_variant_id"), org.bukkit.persistence.PersistentDataType.STRING);
        if (variantId == null) {
            return;
        }

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


