package de.thecoolcraft11.hideAndSeek.gui;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.gui.config.GUIItems;
import de.thecoolcraft11.hideAndSeek.gui.config.GUINames;
import de.thecoolcraft11.hideAndSeek.perk.PerkShopMode;
import de.thecoolcraft11.hideAndSeek.perk.definition.PerkDefinition;
import de.thecoolcraft11.hideAndSeek.perk.definition.PerkTarget;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.TooltipDisplay;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
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
import java.util.Map;
import java.util.UUID;

public class PerkShopGUI {

    public static final String SHOP_TITLE_KEY = "gui.perks.title";
    public static final String SHOP_LIGHT_KEY = "perk_shop_light";

    private final HideAndSeek plugin;

    public PerkShopGUI(HideAndSeek plugin) {
        this.plugin = plugin;
        startRefreshTask();
    }

    private void startRefreshTask() {
        Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.isOnline()) {
                    boolean hasPerkItems = hasAnyShopRowItem(player);
                    boolean hasShopOpen = isShopInventoryOpen(player);

                    if (hasPerkItems || hasShopOpen) {
                        refreshForPlayer(player);
                    }
                }
            }
        }, 5L, 5L);
    }

    private boolean isShopInventoryOpen(Player player) {
        Component currentTitle = player.getOpenInventory().title();

        return currentTitle.equals(plugin.tr(player, SHOP_TITLE_KEY));
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
        if (isShopInventoryOpen(player)) {
            openShopInventory(player);
        }
    }

    public void openShopInventory(Player player) {
        List<PerkDefinition> perks = getRoundPerksForPlayer(player);

        Inventory inv = Bukkit.createInventory(null, 9, plugin.tr(player, SHOP_TITLE_KEY));
        int[] displaySlots = {1, 4, 7};

        for (int i = 0; i < Math.min(perks.size(), displaySlots.length); i++) {
            PerkDefinition perk = perks.get(i);
            boolean bought = isBoughtAndLocked(player.getUniqueId(), perk);
            inv.setItem(displaySlots[i], buildShopItem(perk, bought, player));
        }

        player.openInventory(inv);
    }

    @SuppressWarnings("UnstableApiUsage")
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

            ItemStack sold = plugin.getGuiItemRegistry().getOrDefault(GUINames.PERKS, GUIItems.PERKS_KEY_SOLD,
                    new ItemStack(Material.LIGHT));

            if (sold.getItemMeta() instanceof BlockDataMeta blockDataMeta) {
                int level = soldOutGlobally ? 0 : 15;
                org.bukkit.block.data.type.Light bd = (org.bukkit.block.data.type.Light) Material.LIGHT.createBlockData();
                bd.setLevel(level);
                blockDataMeta.setBlockData(bd);
                sold.setItemMeta(blockDataMeta);
            }

            ItemMeta meta = sold.getItemMeta();

            meta.displayName(perk.getDisplayName(player)
                    .decoration(TextDecoration.STRIKETHROUGH, true)
                    .color(NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false)
                    .append(plugin.tr(player, "gui.perks.locked_suffix")));

            List<Component> lockedLore = new ArrayList<>();
            lockedLore.add(perk.getDescription(player).color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            lockedLore.add(Component.empty());

            if (perk.getTarget() == PerkTarget.HIDER) {
                Player owner = Bukkit.getPlayer(plugin.getPerkStateManager().getFiniteOwnerUUID(perk.getId()));
                if (owner != null) {

                    lockedLore.add(plugin.tr(player, "gui.perks.owned_by", Map.of("owner", owner.getName()))
                            .decoration(TextDecoration.ITALIC, false));
                }
                lockedLore.add(Component.empty());
            }


            Component reason = soldOutGlobally
                    ? plugin.tr(player, "gui.perks.sold_out")
                    : plugin.tr(player, "gui.perks.already_purchased");
            lockedLore.add(reason.decoration(TextDecoration.ITALIC, false));

            meta.lore(lockedLore);
            sold.setItemMeta(meta);
            sold.setData(DataComponentTypes.TOOLTIP_DISPLAY, TooltipDisplay.tooltipDisplay().addHiddenComponents(DataComponentTypes.BLOCK_DATA).build());
            markProtectedShopLight(sold);

            return sold;
        }

        ItemStack item = new ItemStack(perk.getIcon());
        ItemMeta meta = item.getItemMeta();
        meta.displayName(perk.getDisplayName(player).decoration(TextDecoration.ITALIC, false));

        boolean canAfford = balance >= cost;
        boolean isSeekerPerk = perk.getTarget() == PerkTarget.SEEKER;

        List<Component> lore = new ArrayList<>();
        lore.add(perk.getDescription(player).color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());

        if (isSeekerPerk) {
            if (finitePerk) {
                int buyers = plugin.getPerkStateManager().getFiniteBuyerCount(perk.getId());

                lore.add(plugin.tr(player, "gui.perks.finite_seeker").decoration(TextDecoration.ITALIC, false));
                if (finiteLimit > 0) {
                    lore.add(plugin.tr(player, "gui.perks.owners_limit", Map.of("buyers", buyers, "limit", finiteLimit))
                            .decoration(TextDecoration.ITALIC, false));
                } else {
                    lore.add(plugin.tr(player, "gui.perks.owners_unlimited", Map.of("buyers", buyers))
                            .decoration(TextDecoration.ITALIC, false));
                }
            } else {
                lore.add(plugin.tr(player, "gui.perks.rebuyable").decoration(TextDecoration.ITALIC, false));
                long maxCooldownTicks = plugin.getPerkRegistry().getRebuyCooldownTicks(perk);
                long maxSeconds = Math.max(0L, (maxCooldownTicks + 19L) / 20L);
                NamedTextColor cooldownColor = cooldownTicks > 0L ? NamedTextColor.YELLOW : NamedTextColor.GREEN;
                lore.add(plugin.tr(player, "gui.perks.cooldown", Map.of("seconds", maxSeconds))
                        .color(cooldownColor).decoration(TextDecoration.ITALIC, false));
            }
        }


        lore.add(plugin.tr(player, "gui.perks.cost", Map.of("cost", cost))
                .color(canAfford ? NamedTextColor.GOLD : NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
        lore.add(plugin.tr(player, "gui.perks.balance", Map.of("balance", balance))
                .color(canAfford ? NamedTextColor.GREEN : NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
        lore.add(Component.empty());


        Component statusLine;
        if (cooldownTicks > 0L) {
            long seconds = Math.max(1L, (cooldownTicks + 19L) / 20L);
            statusLine = plugin.tr(player, "gui.perks.cooldown", Map.of("seconds", seconds)).color(
                    NamedTextColor.YELLOW);
        } else if (canAfford) {
            statusLine = plugin.tr(player, "gui.perks.click_to_purchase");
        } else {
            statusLine = plugin.tr(player, "gui.perks.not_enough_points");
        }
        lore.add(statusLine.decoration(TextDecoration.ITALIC, false));

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

    public void refreshAllPlayersWithShopItems() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.isOnline()) {
                continue;
            }
            refreshForPlayer(player);
        }
    }


    public void clearAll() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            removePerkItems(p);
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
        if (plugin.getStateManager().isPhase("lobby")) return null;
        if (player.getGameMode() == GameMode.SPECTATOR) return null;
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
        ItemStack lightBlock = plugin.getGuiItemRegistry().getOrDefault(GUINames.PERKS, GUIItems.PERKS_KEY_PLACEHOLDER,
                new ItemStack(Material.LIGHT));
        markProtectedShopLight(lightBlock);

        return lightBlock;
    }

    public boolean isProtectedShopLight(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
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