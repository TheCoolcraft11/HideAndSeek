package de.thecoolcraft11.hideAndSeek.gui;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.*;


public final class SpectatorTeleportGUI {


    private static final int ROW1_START = 9;
    private static final int ROW1_END = 17;
    private static final int ROW2_START = 18;
    private static final int ROW2_END = 26;
    private static final int ROW2_PREV = 18;
    private static final int ROW2_INDICATOR = 22;
    private static final int ROW2_NEXT = 26;
    private static final int ROW3_START = 27;
    private static final int ROW3_END = 35;
    private static final int TARGETS_PER_ROW = 9;


    private static final String PDC_KEY = "spec_gui_action";
    private static final String ACT_PREV = "prev";
    private static final String ACT_NEXT = "next";
    private static final String ACT_SEP = "sep";
    private static final String ACT_IND = "indicator";
    private static final String ACT_TP = "tp:";


    private static final Map<UUID, Integer> pages = new HashMap<>();


    private static int refreshTaskId = -1;

    private SpectatorTeleportGUI() {
    }

    public static void give(HideAndSeek plugin, Player spectator) {
        pages.put(spectator.getUniqueId(), 0);


        plugin.getNmsAdapter().injectSpectatorInventoryHandler(
                spectator,
                containerSlot -> Bukkit.getScheduler().runTask(plugin,
                        () -> onSlotClick(plugin, spectator, containerSlot))
        );

        rebuild(plugin, spectator);
        ensureRefreshTask(plugin);
    }

    public static void remove(HideAndSeek plugin, Player player) {
        if (!pages.containsKey(player.getUniqueId())) {
            return;
        }
        pages.remove(player.getUniqueId());
        plugin.getNmsAdapter().removeSpectatorInventoryHandler(player);
        clearGuiSlots(plugin, player);
        stopRefreshTaskIfIdle();
    }

    public static void removeOnQuit(HideAndSeek plugin, Player player) {
        pages.remove(player.getUniqueId());
        plugin.getNmsAdapter().removeSpectatorInventoryHandler(player);
        stopRefreshTaskIfIdle();
    }

    public static void removeAll(HideAndSeek plugin) {
        for (UUID uid : Set.copyOf(pages.keySet())) {
            Player p = Bukkit.getPlayer(uid);
            if (p != null && p.isOnline()) {
                plugin.getNmsAdapter().removeSpectatorInventoryHandler(p);
            }
        }
        pages.clear();
        stopRefreshTaskIfIdle();
    }

    public static boolean isSpectatorWithGui(UUID playerId) {
        return pages.containsKey(playerId);
    }

    public static boolean isGuiItem(HideAndSeek plugin, ItemStack item) {
        if (item == null || item.getType() == Material.AIR || !item.hasItemMeta()) {
            return false;
        }
        return item.getItemMeta().getPersistentDataContainer()
                .has(new NamespacedKey(plugin, PDC_KEY), PersistentDataType.STRING);
    }

    private static void onSlotClick(HideAndSeek plugin, Player player, int containerSlot) {
        if (!pages.containsKey(player.getUniqueId())) {
            return;
        }
        int slot = containerSlotToInventorySlot(containerSlot);
        if (slot < 0) {
            return;
        }
        processSlot(plugin, player, slot);
    }

    private static void processSlot(HideAndSeek plugin, Player player, int inventorySlot) {

        if (inventorySlot >= ROW2_START && inventorySlot <= ROW2_END) {
            if (inventorySlot == ROW2_PREV) {
                changePage(plugin, player, -1);
            } else if (inventorySlot == ROW2_NEXT) {
                changePage(plugin, player, +1);
            }

            return;
        }


        if ((inventorySlot >= ROW1_START && inventorySlot <= ROW1_END)
                || (inventorySlot >= ROW3_START && inventorySlot <= ROW3_END)) {
            ItemStack item = player.getInventory().getItem(inventorySlot);
            if (!isGuiItem(plugin, item)) {
                return;
            }
            String action = readAction(plugin, item);
            if (action == null || !action.startsWith(ACT_TP)) {
                return;
            }
            String targetName = action.substring(ACT_TP.length());
            Player target = Bukkit.getPlayerExact(targetName);
            if (target != null && target.isOnline()) {
                player.teleport(target.getLocation());
                player.sendActionBar(
                        plugin.tr(player, "gui.spectator.teleported", Map.of("target", target.getName()))
                );
                player.closeInventory(InventoryCloseEvent.Reason.PLUGIN);
            }
        }
    }

    private static void changePage(HideAndSeek plugin, Player player, int delta) {
        int current = pages.getOrDefault(player.getUniqueId(), 0);
        int maxPage = computeMaxPage(player.getUniqueId());
        int newPage = Math.clamp(current + delta, 0, maxPage);
        if (newPage == current) {
            return;
        }
        pages.put(player.getUniqueId(), newPage);
        rebuild(plugin, player);
    }

    private static void rebuild(HideAndSeek plugin, Player spectator) {
        if (spectator.getGameMode() != GameMode.SPECTATOR) {
            return;
        }

        UUID uid = spectator.getUniqueId();
        List<Player> hiders = collectGroup(uid, true);
        List<Player> seekers = collectGroup(uid, false);

        int page = pages.getOrDefault(uid, 0);
        int maxPage = computeMaxPage(hiders, seekers);


        if (page > maxPage) {
            page = maxPage;
            pages.put(uid, page);
        }

        int offset = page * TARGETS_PER_ROW;
        boolean needsPaging = maxPage > 0;


        for (int i = 0; i < TARGETS_PER_ROW; i++) {
            int idx = offset + i;
            spectator.getInventory().setItem(
                    ROW1_START + i,
                    idx < hiders.size() ? buildHead(plugin, spectator, hiders.get(idx), true) : null
            );
        }


        for (int slot = ROW2_START; slot <= ROW2_END; slot++) {
            spectator.getInventory().setItem(slot, buildSeparator(plugin));
        }


        if (needsPaging) {
            if (page > 0) {
                spectator.getInventory().setItem(ROW2_PREV,
                        buildNav(plugin,
                                plugin.tr(spectator, "gui.spectator.previous_page"),
                                ACT_PREV,
                                plugin.tr(spectator, "gui.spectator.page_info",
                                        Map.of("current", page, "total", maxPage + 1))));
            }

            spectator.getInventory().setItem(ROW2_INDICATOR,
                    buildIndicator(plugin, spectator, page + 1, maxPage + 1));

            if (page < maxPage) {
                spectator.getInventory().setItem(ROW2_NEXT,
                        buildNav(plugin,
                                plugin.tr(spectator, "gui.spectator.next_page"),
                                ACT_NEXT,
                                plugin.tr(spectator, "gui.spectator.page_info",
                                        Map.of("current", page + 2, "total", maxPage + 1))));
            }
        }


        for (int i = 0; i < TARGETS_PER_ROW; i++) {
            int idx = offset + i;
            spectator.getInventory().setItem(
                    ROW3_START + i,
                    idx < seekers.size() ? buildHead(plugin, spectator, seekers.get(idx), false) : null
            );
        }
    }


    private static ItemStack buildHead(HideAndSeek plugin, Player spectator, Player target, boolean isHider) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();

        meta.setOwningPlayer(target);

        NamedTextColor nameColor = isHider ? NamedTextColor.GREEN : NamedTextColor.RED;
        meta.displayName(
                Component.text(target.getName(), nameColor, TextDecoration.BOLD)
                        .decoration(TextDecoration.ITALIC, false)
        );

        List<Component> lore = new ArrayList<>();
        lore.add(plugin.tr(spectator, isHider ? "gui.spectator.role_hider" : "gui.spectator.role_seeker")
                .colorIfAbsent(isHider ? NamedTextColor.GREEN : NamedTextColor.RED)
                .decoration(TextDecoration.ITALIC, false));

        if (isHider) {
            double maxHp = Objects.requireNonNull(
                    target.getAttribute(Attribute.MAX_HEALTH)).getValue();
            double hp = Math.max(0, target.getHealth());
            int pct = (int) Math.round((hp / maxHp) * 100);
            int filled = (int) Math.round((hp / maxHp) * 10);
            String bar = "█".repeat(filled) + "░".repeat(10 - filled);
            NamedTextColor hpColor = pct > 60 ? NamedTextColor.GREEN
                    : pct > 30 ? NamedTextColor.YELLOW
                      : NamedTextColor.RED;
            lore.add(plugin.tr(spectator, "gui.spectator.hp_label")
                    .colorIfAbsent(NamedTextColor.GRAY)
                    .append(Component.text(bar + " " + pct + "%", hpColor))
                    .decoration(TextDecoration.ITALIC, false));
        }

        lore.add(Component.empty());
        lore.add(plugin.tr(spectator, "gui.spectator.click_to_teleport")
                .colorIfAbsent(NamedTextColor.AQUA)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        tag(plugin, meta, ACT_TP + target.getName());
        skull.setItemMeta(meta);
        return skull;
    }

    private static ItemStack buildSeparator(HideAndSeek plugin) {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(Component.empty());
        tag(plugin, meta, ACT_SEP);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack buildNav(HideAndSeek plugin,
                                      Component name, String action, Component hint) {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(name.colorIfAbsent(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                hint.colorIfAbsent(NamedTextColor.DARK_GRAY)
                        .decoration(TextDecoration.ITALIC, false)
        ));
        tag(plugin, meta, action);
        item.setItemMeta(meta);
        return item;
    }

    private static ItemStack buildIndicator(HideAndSeek plugin, Player spectator, int currentPage, int totalPages) {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(
                plugin.tr(spectator, "gui.spectator.page_info",
                                Map.of("current", currentPage, "total", totalPages))
                        .colorIfAbsent(NamedTextColor.WHITE)
                        .decorate(TextDecoration.BOLD)
                        .decoration(TextDecoration.ITALIC, false)
        );
        meta.lore(List.of(
                plugin.tr(spectator, "gui.spectator.nav_hint")
                        .colorIfAbsent(NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)
        ));
        tag(plugin, meta, ACT_IND);
        item.setItemMeta(meta);
        return item;
    }


    private static void tag(HideAndSeek plugin, ItemMeta meta, String action) {
        meta.getPersistentDataContainer()
                .set(new NamespacedKey(plugin, PDC_KEY), PersistentDataType.STRING, action);
    }

    private static String readAction(HideAndSeek plugin, ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        return item.getItemMeta().getPersistentDataContainer()
                .get(new NamespacedKey(plugin, PDC_KEY), PersistentDataType.STRING);
    }

    private static List<Player> collectGroup(UUID spectatorId, boolean wantHiders) {
        Set<UUID> group = wantHiders
                ? new HashSet<>(HideAndSeek.getDataController().getHiders())
                : new HashSet<>(HideAndSeek.getDataController().getSeekers());

        List<Player> result = new ArrayList<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getUniqueId().equals(spectatorId)) continue;
            if (!group.contains(p.getUniqueId())) continue;
            if (wantHiders && p.getGameMode() == GameMode.SPECTATOR) continue;
            result.add(p);
        }
        result.sort(Comparator.comparing(Player::getName, String.CASE_INSENSITIVE_ORDER));
        return result;
    }

    private static int computeMaxPage(UUID uid) {
        return computeMaxPage(collectGroup(uid, true), collectGroup(uid, false));
    }

    private static int computeMaxPage(List<Player> hiders, List<Player> seekers) {
        int maxSize = Math.max(hiders.size(), seekers.size());
        return maxSize == 0 ? 0 : (maxSize - 1) / TARGETS_PER_ROW;
    }

    private static void clearGuiSlots(HideAndSeek plugin, Player player) {
        for (int slot = ROW1_START; slot <= ROW3_END; slot++) {
            if (isGuiItem(plugin, player.getInventory().getItem(slot))) {
                player.getInventory().setItem(slot, null);
            }
        }
    }

    private static int containerSlotToInventorySlot(int containerSlot) {
        if (containerSlot >= 9 && containerSlot <= 35) {
            return containerSlot;
        }
        if (containerSlot >= 36 && containerSlot <= 44) {
            return containerSlot - 36;
        }
        return -1;
    }

    private static void ensureRefreshTask(HideAndSeek plugin) {
        if (refreshTaskId >= 0) return;
        refreshTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            if (pages.isEmpty()) {
                stopRefreshTaskIfIdle();
                return;
            }
            for (UUID uid : Set.copyOf(pages.keySet())) {
                Player p = Bukkit.getPlayer(uid);
                if (p == null || !p.isOnline()) {
                    pages.remove(uid);
                    continue;
                }
                rebuild(plugin, p);
            }
        }, 20L, 20L);
    }

    private static void stopRefreshTaskIfIdle() {
        if (pages.isEmpty() && refreshTaskId >= 0) {
            Bukkit.getScheduler().cancelTask(refreshTaskId);
            refreshTaskId = -1;
        }
    }
}