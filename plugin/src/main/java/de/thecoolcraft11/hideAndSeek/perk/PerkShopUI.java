package de.thecoolcraft11.hideAndSeek.perk;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.perk.definition.PerkDefinition;
import de.thecoolcraft11.hideAndSeek.perk.definition.PerkTarget;
import io.papermc.paper.datacomponent.DataComponentTypes;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PerkShopUI {

    public static final Component SHOP_TITLE = Component.text("Perk Shop", NamedTextColor.GOLD);

    private final HideAndSeek plugin;
    private int refreshTaskId = -1;

    public PerkShopUI(HideAndSeek plugin) {
        this.plugin = plugin;
        startRefreshTask();
    }

    private void startRefreshTask() {
        
        refreshTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.isOnline()) {
                    
                    boolean hasPerkItems = player.getInventory().getItem(getConfiguredSlots().get(0)) != null;
                    boolean hasShopOpen = SHOP_TITLE.equals(player.getOpenInventory().title());
                    
                    if (hasPerkItems || hasShopOpen) {
                        refreshForPlayer(player);
                    }
                }
            }
        }, 5L, 5L);
    }

    public void givePerkItems(Player player) {
        List<PerkDefinition> perks = getRoundPerksForPlayer(player);
        List<Integer> rowSlots = getConfiguredSlots();

        
        for (int slot : rowSlots) {
            player.getInventory().setItem(slot, buildPlaceholderItem());
        }

        List<Integer> perkSlots = centeredSlots(rowSlots);
        for (int i = 0; i < Math.min(perks.size(), perkSlots.size()); i++) {
            PerkDefinition perk = perks.get(i);
            boolean bought = isBoughtAndLocked(player.getUniqueId(), perk);
            player.getInventory().setItem(perkSlots.get(i), buildShopItem(perk, bought, player));
        }
    }

    public void removePerkItems(Player player) {
        for (int slot : getConfiguredSlots()) {
            ItemStack current = player.getInventory().getItem(slot);
            if (current != null && (isPerkShopItem(current) || current.getType() == Material.LIGHT)) {
                player.getInventory().setItem(slot, null);
            }
        }
    }

    public void refreshForPlayer(Player player) {
        givePerkItems(player);
        if (SHOP_TITLE.equals(player.getOpenInventory().title())) {
            openShopInventory(player);
        }
    }

    public void openShopInventory(Player player) {
        List<PerkDefinition> perks = getRoundPerksForPlayer(player);
        Inventory inv = Bukkit.createInventory(null, 9, SHOP_TITLE);
        int[] displaySlots = {1, 4, 7};

        for (int i = 0; i < Math.min(perks.size(), displaySlots.length); i++) {
            PerkDefinition perk = perks.get(i);
            boolean bought = isBoughtAndLocked(player.getUniqueId(), perk);
            inv.setItem(displaySlots[i], buildShopItem(perk, bought, player));
        }

        player.openInventory(inv);
    }

    public ItemStack buildShopItem(PerkDefinition perk, boolean locked, Player player) {
        int cost = plugin.getSettingRegistry().get("perks.perk." + perk.getId() + ".cost", perk.getCost());
        int balance = HideAndSeek.getDataController().getPoints(player.getUniqueId());

        if (locked) {
            ItemStack sold = new ItemStack(Material.LIGHT);
            ItemMeta meta = sold.getItemMeta();
            meta.displayName(perk.getDisplayName()
                    .decoration(TextDecoration.STRIKETHROUGH, true)
                    .color(NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false)
                    .append(Component.text(" [LOCKED]", NamedTextColor.DARK_GRAY)));
            meta.lore(List.of(Component.text("Already purchased", NamedTextColor.DARK_GREEN)
                    .decoration(TextDecoration.ITALIC, false)));
            sold.setItemMeta(meta);
            return sold;
        }

        ItemStack item = new ItemStack(perk.getIcon());
        ItemMeta meta = item.getItemMeta();
        meta.displayName(perk.getDisplayName().decoration(TextDecoration.ITALIC, false));

        boolean canAfford = balance >= cost;
        boolean isSeekerPerk = perk.getTarget() == PerkTarget.SEEKER;

        List<Component> lore = new ArrayList<>();
        lore.add(perk.getDescription().color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        if (isSeekerPerk) {
            lore.add(Component.text("Infinitely rebuyable", NamedTextColor.AQUA)
                    .decoration(TextDecoration.ITALIC, false));
        }
        lore.add(Component.text("Cost: " + cost + " pts", canAfford ? NamedTextColor.GOLD : NamedTextColor.RED)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Balance: " + balance + " pts", canAfford ? NamedTextColor.GREEN : NamedTextColor.RED)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());
        lore.add(Component.text(canAfford ? "Click to purchase" : "Not enough points",
                canAfford ? NamedTextColor.AQUA : NamedTextColor.DARK_RED)
                .decoration(TextDecoration.ITALIC, false));

        meta.lore(lore);
        meta.getPersistentDataContainer().set(
                new NamespacedKey(plugin, "perk_shop_item"),
                PersistentDataType.STRING,
                perk.getId());

        item.setItemMeta(meta);
        return item;
    }

    public boolean isPerkShopItem(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }
        return item.getItemMeta().getPersistentDataContainer()
                .has(new NamespacedKey(plugin, "perk_shop_item"), PersistentDataType.STRING);
    }

    public @Nullable String getPerkIdFromItem(ItemStack item) {
        if (!isPerkShopItem(item)) {
            return null;
        }
        return item.getItemMeta().getPersistentDataContainer()
                .get(new NamespacedKey(plugin, "perk_shop_item"), PersistentDataType.STRING);
    }

    public void clearAll() {
        if (refreshTaskId >= 0) {
            Bukkit.getScheduler().cancelTask(refreshTaskId);
            refreshTaskId = -1;
        }
        for (Player p : Bukkit.getOnlinePlayers()) {
            removePerkItems(p);
        }
    }

    private List<PerkDefinition> getRoundPerksForPlayer(Player player) {
        boolean isHider = HideAndSeek.getDataController().getHiders().contains(player.getUniqueId());
        return isHider ? plugin.getPerkRegistry().getRoundPerksForHider() : plugin.getPerkRegistry().getRoundPerksForSeeker();
    }

    private boolean isBoughtAndLocked(UUID playerId, PerkDefinition perk) {
        if (perk.getTarget() == PerkTarget.SEEKER) {
            return false;
        }
        return plugin.getPerkStateManager().hasPurchased(playerId, perk.getId());
    }

    @SuppressWarnings("unchecked")
    private List<Integer> getConfiguredSlots() {
        List<?> raw = plugin.getSettingRegistry().get("perks.inventory-slots", List.of(9, 10, 11, 12, 13, 14, 15, 16, 17));
        List<Integer> slots = new ArrayList<>();
        if (raw != null) {
            for (Object value : raw) {
                if (value instanceof Number number) {
                    slots.add(number.intValue());
                }
            }
        }
        if (slots.size() != 9) {
            return List.of(9, 10, 11, 12, 13, 14, 15, 16, 17);
        }
        return slots;
    }

    private List<Integer> centeredSlots(List<Integer> rowSlots) {
        int centerIndex = rowSlots.size() / 2;
        List<Integer> order = new ArrayList<>();
        order.add(rowSlots.get(centerIndex));

        for (int offset = 1; offset <= centerIndex; offset++) {
            int left = centerIndex - offset;
            int right = centerIndex + offset;
            if (left >= 0) {
                order.add(rowSlots.get(left));
            }
            if (right < rowSlots.size()) {
                order.add(rowSlots.get(right));
            }
        }
        return order;
    }

    private ItemStack buildPlaceholderItem() {
        ItemStack lightBlock = new ItemStack(Material.LIGHT);
        lightBlock.unsetData(DataComponentTypes.BLOCK_DATA);

        lightBlock.setData(
                DataComponentTypes.TOOLTIP_DISPLAY,
                io.papermc.paper.datacomponent.item.TooltipDisplay.tooltipDisplay().hideTooltip(true).build()
        );

        return lightBlock;
    }
}


