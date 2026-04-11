package de.thecoolcraft11.hideAndSeek.perk;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.perk.definition.PerkDefinition;
import de.thecoolcraft11.hideAndSeek.perk.definition.PerkTarget;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.TooltipDisplay;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockDataMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@SuppressWarnings("ALL")
public class PerkShopUI {

    public static final Component SHOP_TITLE = Component.text("Perk Shop", NamedTextColor.GOLD);
    private static final String SHOP_LIGHT_KEY = "perk_shop_light";

    private final HideAndSeek plugin;

    public PerkShopUI(HideAndSeek plugin) {
        this.plugin = plugin;
        startRefreshTask();
    }

    private void startRefreshTask() {
        Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.isOnline()) {
                    boolean hasPerkItems = hasAnyShopRowItem(player);
                    boolean hasShopOpen = SHOP_TITLE.equals(player.getOpenInventory().title());

                    if (hasPerkItems || hasShopOpen) {
                        refreshForPlayer(player);
                    }
                }
            }
        }, 5L, 5L);
    }

    public void givePerkItems(Player player) {
        if (getShopModeForPlayer(player) != PerkShopMode.INVENTORY) {
            removePerkItems(player);
            return;
        }

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
            if (current != null && (isPerkShopItem(current) || isProtectedShopLight(current))) {
                player.getInventory().setItem(slot, null);
            }
        }
    }

    public void refreshForPlayer(Player player) {
        if (getShopModeForPlayer(player) == PerkShopMode.INVENTORY) {
            givePerkItems(player);
        } else {
            removePerkItems(player);
        }
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
        boolean finitePerk = perk.getTarget() == PerkTarget.HIDER
                || (perk.getTarget() == PerkTarget.SEEKER && plugin.getPerkRegistry().isFiniteSeekerPerk(perk.getId()));
        long cooldownTicks = plugin.getPerkStateManager().getPurchaseCooldownRemainingTicks(player.getUniqueId(), perk.getId());
        int finiteLimit = plugin.getPerkRegistry().getFinitePlayerLimit(perk.getTarget());

        if (finitePerk) {
            locked = plugin.getPerkStateManager().isFinitePurchaseLocked(player.getUniqueId(), perk);
        }

        if (locked) {
            boolean purchasedByPlayer = plugin.getPerkStateManager().hasPurchased(player.getUniqueId(), perk.getId());
            int buyers = plugin.getPerkStateManager().getFiniteBuyerCount(perk.getId());
            boolean soldOutGlobally = finiteLimit > 0 && buyers >= finiteLimit && !purchasedByPlayer;

            ItemStack sold = new ItemStack(Material.LIGHT);

            if (sold.getItemMeta() instanceof BlockDataMeta blockDataMeta) {
                int level = soldOutGlobally ? 0 : 15;

                org.bukkit.block.data.BlockData bd = Material.LIGHT.createBlockData();
                ((org.bukkit.block.data.type.Light) bd).setLevel(level);

                blockDataMeta.setBlockData(bd);
                sold.setItemMeta(blockDataMeta);
            }

            ItemMeta meta = sold.getItemMeta();
            meta.displayName(perk.getDisplayName()
                    .decoration(TextDecoration.STRIKETHROUGH, true)
                    .color(NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false)
                    .append(Component.text(" [LOCKED]", NamedTextColor.DARK_GRAY)));

            List<Component> lockedLore = new ArrayList<>();
            lockedLore.add(perk.getDescription().color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            lockedLore.add(Component.empty());

            if (perk.getTarget() == PerkTarget.HIDER) {
                Player owner = Bukkit.getPlayer(plugin.getPerkStateManager().getFiniteOwnerUUID(perk.getId()));
                if (owner != null) {
                    lockedLore.add(Component.text("Owned by: " + owner.getName(), NamedTextColor.YELLOW)
                            .decoration(TextDecoration.ITALIC, false));
                }
                lockedLore.add(Component.empty());
            }

            Component reason = soldOutGlobally
                    ? Component.text("Sold out (global limit reached)", NamedTextColor.DARK_RED)
                    : Component.text("Already purchased", NamedTextColor.DARK_GREEN);
            lockedLore.add(reason.decoration(TextDecoration.ITALIC, false));

            meta.lore(lockedLore);
            sold.setItemMeta(meta);

            sold.setData(DataComponentTypes.TOOLTIP_DISPLAY, TooltipDisplay.tooltipDisplay().addHiddenComponents(DataComponentTypes.BLOCK_DATA).build());
            markProtectedShopLight(sold);

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
            if (finitePerk) {
                int buyers = plugin.getPerkStateManager().getFiniteBuyerCount(perk.getId());
                lore.add(Component.text("Finite seeker perk", NamedTextColor.AQUA)
                        .decoration(TextDecoration.ITALIC, false));
                if (finiteLimit > 0) {
                    lore.add(Component.text("Owners: " + buyers + "/" + finiteLimit, NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false));
                } else {
                    lore.add(Component.text("Owners: " + buyers + "/unlimited", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false));
                }
            } else {
                lore.add(Component.text("Rebuyable with cooldown", NamedTextColor.AQUA)
                        .decoration(TextDecoration.ITALIC, false));
                long maxCooldownTicks = plugin.getPerkRegistry().getRebuyCooldownTicks(perk);
                long maxSeconds = Math.max(0L, (maxCooldownTicks + 19L) / 20L);
                NamedTextColor cooldownColor = cooldownTicks > 0L ? NamedTextColor.YELLOW : NamedTextColor.GREEN;
                lore.add(Component.text("Cooldown: " + maxSeconds + "s", cooldownColor)
                        .decoration(TextDecoration.ITALIC, false));
            }
        }

        lore.add(Component.text("Cost: " + cost + " pts", canAfford ? NamedTextColor.GOLD : NamedTextColor.RED)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.text("Balance: " + balance + " pts", canAfford ? NamedTextColor.GREEN : NamedTextColor.RED)
                .decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());

        Component statusLine;
        if (cooldownTicks > 0L) {
            long seconds = Math.max(1L, (cooldownTicks + 19L) / 20L);
            statusLine = Component.text("Cooldown: " + seconds + "s", NamedTextColor.YELLOW);
        } else if (canAfford) {
            statusLine = Component.text("Click to purchase", NamedTextColor.AQUA);
        } else {
            statusLine = Component.text("Not enough points", NamedTextColor.DARK_RED);
        }
        lore.add(statusLine
                .decoration(TextDecoration.ITALIC, false));

        meta.lore(lore);
        meta.getPersistentDataContainer().set(
                new NamespacedKey(plugin, "perk_shop_item"),
                PersistentDataType.STRING,
                perk.getId());

        item.setItemMeta(meta);

        item.setData(DataComponentTypes.TOOLTIP_DISPLAY, TooltipDisplay.tooltipDisplay().addHiddenComponents(DataComponentTypes.BLOCK_DATA).build());

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
        for (Player p : Bukkit.getOnlinePlayers()) {
            removePerkItems(p);
        }
    }

    public void refreshAllPlayersWithShopItems() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.isOnline()) {
                continue;
            }
            boolean hasPerkItems = hasAnyShopRowItem(player);
            boolean hasShopOpen = SHOP_TITLE.equals(player.getOpenInventory().title());
            if (hasPerkItems || hasShopOpen) {
                refreshForPlayer(player);
            }
        }
    }

    private List<PerkDefinition> getRoundPerksForPlayer(Player player) {
        boolean isHider = HideAndSeek.getDataController().getHiders().contains(player.getUniqueId());
        return isHider ? plugin.getPerkRegistry().getRoundPerksForHider() : plugin.getPerkRegistry().getRoundPerksForSeeker();
    }

    private boolean isBoughtAndLocked(UUID playerId, PerkDefinition perk) {
        boolean finitePerk = perk.getTarget() == PerkTarget.HIDER
                || (perk.getTarget() == PerkTarget.SEEKER && plugin.getPerkRegistry().isFiniteSeekerPerk(perk.getId()));
        if (!finitePerk) {
            return false;
        }
        return plugin.getPerkStateManager().isFinitePurchaseLocked(playerId, perk);
    }

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

    private PerkShopMode getShopModeForPlayer(Player player) {
        if (HideAndSeek.getDataController().getHiders().contains(player.getUniqueId())) {
            return plugin.getSettingRegistry().get("perks.hider-shop-mode", PerkShopMode.INVENTORY);
        }
        return plugin.getSettingRegistry().get("perks.seeker-shop-mode", PerkShopMode.INVENTORY);
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

    private boolean hasAnyShopRowItem(Player player) {
        for (int slot : getConfiguredSlots()) {
            if (player.getInventory().getItem(slot) != null) {
                return true;
            }
        }
        return false;
    }

    private ItemStack buildPlaceholderItem() {
        ItemStack lightBlock = new ItemStack(Material.LIGHT);
        lightBlock.unsetData(DataComponentTypes.BLOCK_DATA);
        markProtectedShopLight(lightBlock);

        lightBlock.setData(
                DataComponentTypes.TOOLTIP_DISPLAY,
                io.papermc.paper.datacomponent.item.TooltipDisplay.tooltipDisplay().hideTooltip(true).build()
        );

        return lightBlock;
    }

    public boolean isProtectedShopLight(ItemStack item) {
        if (item == null || !item.hasItemMeta() || item.getType() != Material.LIGHT) {
            return false;
        }
        return item.getItemMeta().getPersistentDataContainer()
                .has(new NamespacedKey(plugin, SHOP_LIGHT_KEY), PersistentDataType.BOOLEAN);
    }

    private void markProtectedShopLight(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        meta.getPersistentDataContainer().set(new NamespacedKey(plugin, SHOP_LIGHT_KEY), PersistentDataType.BOOLEAN, true);
        item.setItemMeta(meta);
    }
}


