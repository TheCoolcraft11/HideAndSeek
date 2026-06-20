package de.thecoolcraft11.hideAndSeek.gui;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.gui.config.GUIItems;
import de.thecoolcraft11.hideAndSeek.gui.config.GUINames;
import de.thecoolcraft11.hideAndSeek.items.HiderItems;
import de.thecoolcraft11.hideAndSeek.items.SeekerItems;
import de.thecoolcraft11.hideAndSeek.items.api.GameItem;
import de.thecoolcraft11.hideAndSeek.playerdata.PlayerStatsService;
import de.thecoolcraft11.minigameframework.inventory.FrameworkInventory;
import de.thecoolcraft11.minigameframework.inventory.InventoryBuilder;
import de.thecoolcraft11.minigameframework.inventory.InventoryItem;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.TooltipDisplay;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class PlayerStatsGUI {

    private final HideAndSeek plugin;

    public PlayerStatsGUI(HideAndSeek plugin) {
        this.plugin = plugin;
    }

    public void open(Player viewer) {
        open(viewer, viewer, StatsPage.OVERVIEW);
    }

    public void open(Player viewer, Player target) {
        open(viewer, target, StatsPage.OVERVIEW);
    }

    private void open(Player viewer, Player target, StatsPage page) {
        PlayerStatsService service = PlayerStatsService.getActive();
        if (service == null) {
            viewer.sendMessage(plugin.tr(viewer, "gui.stats.service_unavailable"));
            return;
        }

        PlayerStatsService.PlayerStatsRecord stats = service.getSnapshot(target.getUniqueId());

        FrameworkInventory inv = new InventoryBuilder(plugin.getInventoryFramework())
                .id("player_stats_" + viewer.getUniqueId() + "_" + target.getUniqueId() + "_" + page.name())
                .title(page == StatsPage.OVERVIEW
                        ? plugin.trText(viewer, "gui.stats.title", Map.of("player", target.getName()))
                        : plugin.trText(viewer, "gui.stats.title_with_page",
                        Map.of("player", target.getName(), "page", formatPageName(viewer, page))))
                .rows(6)
                .allowOutsideClicks(false)
                .allowDrag(false)
                .allowPlayerInventoryInteraction(false)
                .build();

        fillBorder(inv);

        inv.setItem(0, buildPlayerHead(viewer, target, stats));

        switch (page) {
            case OVERVIEW -> fillOverview(inv, viewer, stats);
            case COMBAT -> fillCombat(inv, viewer, stats);
            case ITEMS -> fillItems(inv, viewer, stats);
            case MAPS -> fillMaps(inv, viewer, stats);
            case PERKS -> fillPerks(inv, viewer, stats);
        }

        buildTabs(inv, viewer, target, page);

        InventoryItem closeBtn = new InventoryItem(
                utility(GUIItems.STATS_CLOSE, plugin.tr(viewer, "gui.stats.close"), List.of()));
        closeBtn.setClickHandler((p, item, event, slot) -> {
            p.closeInventory();
            event.setCancelled(true);
        });
        closeBtn.setAllowTakeout(false);
        closeBtn.setAllowInsert(false);
        inv.setItem(53, closeBtn);

        plugin.getInventoryFramework().openInventory(viewer, inv);
    }

    private void fillOverview(FrameworkInventory inv,
                              Player viewer,
                              PlayerStatsService.PlayerStatsRecord stats) {
        long totalGames = stats.hiderWins + stats.seekerWins + stats.totalHiderDeaths;
        double winRate = totalGames == 0 ? 0.0 : (double) (stats.hiderWins + stats.seekerWins) / totalGames * 100.0;

        List<Component> wlLore = List.of(
                plugin.tr(viewer, "gui.stats.overview.hider_wins", Map.of("value", stats.hiderWins)),
                plugin.tr(viewer, "gui.stats.overview.seeker_wins", Map.of("value", stats.seekerWins)),
                plugin.tr(viewer, "gui.stats.overview.hider_deaths", Map.of("value", stats.totalHiderDeaths)),
                Component.empty(),
                plugin.tr(viewer, "gui.stats.overview.win_rate", Map.of("value", String.format("%.1f%%", winRate)))
        );
        inv.setItem(10, stat(Material.LIME_STAINED_GLASS, plugin.tr(viewer, "gui.stats.overview.win_loss"), wlLore));


        List<Component> roundsLore = List.of(
                plugin.tr(viewer, "gui.stats.overview.hider_rounds", Map.of("value", stats.hiderRoundsPlayed)),
                plugin.tr(viewer, "gui.stats.overview.seeker_rounds", Map.of("value", stats.seekerRoundsPlayed)),
                plugin.tr(viewer, "gui.stats.overview.last_hider_standing",
                        Map.of("value", stats.totalRoundsAsLastHider))
        );
        inv.setItem(12, stat(Material.BOOK, plugin.tr(viewer, "gui.stats.overview.rounds_played"), roundsLore));


        long totalSurvivalSec = stats.hiderSurvivalMs / 1000;
        long longestSurvivalSec = stats.longestHiderSurvivalMs / 1000;
        List<Component> survivalLore = List.of(
                plugin.tr(viewer, "gui.stats.overview.total_survival",
                        Map.of("value", formatDuration(viewer, totalSurvivalSec))),
                plugin.tr(viewer, "gui.stats.overview.longest_survival",
                        Map.of("value", formatDuration(viewer, longestSurvivalSec)))
        );
        inv.setItem(14, stat(Material.CLOCK, plugin.tr(viewer, "gui.stats.overview.survival_time"), survivalLore));


        long totalPlaySec = stats.totalPlaytimeMs / 1000;
        List<Component> playtimeLore = List.of(
                plugin.tr(viewer, "gui.stats.overview.total_playtime",
                        Map.of("value", formatDuration(viewer, totalPlaySec)))
        );
        inv.setItem(16, stat(Material.COMPASS, plugin.tr(viewer, "gui.stats.overview.playtime"), playtimeLore));


        List<Component> ecoLore = List.of(
                plugin.tr(viewer, "gui.stats.overview.points_earned", Map.of("value", stats.totalPointsEarned)),
                plugin.tr(viewer, "gui.stats.overview.coins_earned", Map.of("value", stats.totalCoinsEarned))
        );
        inv.setItem(28, stat(Material.GOLD_INGOT, plugin.tr(viewer, "gui.stats.overview.economy"), ecoLore));


        List<Component> tauntLore = List.of(
                plugin.tr(viewer, "gui.stats.overview.total_taunts", Map.of("value", stats.totalTauntsUsed))
        );
        inv.setItem(30, stat(Material.CAT_SPAWN_EGG, plugin.tr(viewer, "gui.stats.overview.taunts_used"), tauntLore));

        List<Component> modeLore = new ArrayList<>();
        if (stats.gameModesPlayed.isEmpty()) {
            modeLore.add(plugin.tr(viewer, "gui.stats.overview.no_games_played"));
        } else {
            for (Map.Entry<String, Long> e : stats.gameModesPlayed.entrySet()) {
                modeLore.add(plugin.tr(viewer, "gui.stats.overview.game_mode_entry", Map.of(
                        "mode", e.getKey(),
                        "count", e.getValue(),
                        "suffix", e.getValue() == 1
                                ? plugin.trText(viewer, "gui.stats.overview.game_singular")
                                : plugin.trText(viewer, "gui.stats.overview.games_plural")
                )));
            }
        }
        inv.setItem(32, stat(Material.COMPASS, plugin.tr(viewer, "gui.stats.overview.game_modes"), modeLore));


        long hiddenSec = stats.totalHiddenInBlockModeMs / 1000;
        long unhiddenSec = stats.totalUnhiddenInBlockModeMs / 1000;
        long totalBlockSec = hiddenSec + unhiddenSec;
        double hiddenPct = totalBlockSec == 0 ? 0.0 : (double) hiddenSec / totalBlockSec * 100.0;
        List<Component> blockLore = List.of(
                plugin.tr(viewer, "gui.stats.overview.time_hidden", Map.of("value", formatDuration(viewer, hiddenSec))),
                plugin.tr(viewer, "gui.stats.overview.time_exposed",
                        Map.of("value", formatDuration(viewer, unhiddenSec))),
                plugin.tr(viewer, "gui.stats.overview.hidden_rate", Map.of("value", String.format("%.1f%%", hiddenPct)))
        );
        inv.setItem(34, stat(Material.GRASS_BLOCK, plugin.tr(viewer, "gui.stats.overview.block_mode"), blockLore));
    }


    private void fillCombat(FrameworkInventory inv,
                            Player viewer,
                            PlayerStatsService.PlayerStatsRecord stats) {

        List<Component> killsLore = List.of(
                plugin.tr(viewer, "gui.stats.combat.total_kills", Map.of("value", stats.totalSeekerKills)),
                plugin.tr(viewer, "gui.stats.combat.best_round", Map.of("value", stats.mostKillsInASeekerRound))
        );
        inv.setItem(10, stat(Material.IRON_SWORD, plugin.tr(viewer, "gui.stats.combat.seeker_kills"), killsLore));


        List<Component> damageLore = List.of(
                plugin.tr(viewer, "gui.stats.combat.total_damage",
                        Map.of("value", String.format("%.1f", stats.totalDamageDealt)))
        );
        inv.setItem(12, stat(Material.GOLDEN_SWORD, plugin.tr(viewer, "gui.stats.combat.damage_dealt"), damageLore));


        List<Component> deathsLore = List.of(
                plugin.tr(viewer, "gui.stats.combat.times_found", Map.of("value", stats.totalHiderDeaths))
        );
        inv.setItem(14, stat(Material.SKELETON_SKULL, plugin.tr(viewer, "gui.stats.combat.hider_deaths"), deathsLore));


        double kd = stats.totalHiderDeaths == 0
                ? stats.totalSeekerKills
                : (double) stats.totalSeekerKills / stats.totalHiderDeaths;
        List<Component> kdLore = List.of(
                plugin.tr(viewer, "gui.stats.combat.kills", Map.of("value", stats.totalSeekerKills)),
                plugin.tr(viewer, "gui.stats.combat.deaths", Map.of("value", stats.totalHiderDeaths)),
                Component.empty(),
                plugin.tr(viewer, "gui.stats.combat.kd", Map.of("value", String.format("%.2f", kd)))
        );
        inv.setItem(16, stat(Material.SHIELD, plugin.tr(viewer, "gui.stats.combat.kd_ratio"), kdLore));


        List<Component> survivalLore = List.of(
                plugin.tr(viewer, "gui.stats.combat.total_as_hider",
                        Map.of("value", formatDuration(viewer, stats.hiderSurvivalMs / 1000))),
                plugin.tr(viewer, "gui.stats.combat.longest_round",
                        Map.of("value", formatDuration(viewer, stats.longestHiderSurvivalMs / 1000)))
        );
        inv.setItem(28, stat(Material.CLOCK, plugin.tr(viewer, "gui.stats.combat.hider_survival"), survivalLore));


        List<Component> lastHiderLore = List.of(
                plugin.tr(viewer, "gui.stats.combat.times_last_hider", Map.of("value", stats.totalRoundsAsLastHider))
        );
        inv.setItem(30,
                stat(Material.TOTEM_OF_UNDYING, plugin.tr(viewer, "gui.stats.combat.last_hider_standing"),
                        lastHiderLore));
    }


    private void fillItems(FrameworkInventory inv,
                           Player viewer,
                           PlayerStatsService.PlayerStatsRecord stats) {
        if (stats.items.isEmpty()) {
            InventoryItem empty = new InventoryItem(utility(GUIItems.STATS_BORDER,
                    plugin.tr(viewer, "gui.stats.items.no_data"),
                    List.of(plugin.tr(viewer, "gui.stats.items.no_tracked"))));
            empty.setClickHandler((p, item, event, slot) -> event.setCancelled(true));
            empty.setAllowTakeout(false);
            empty.setAllowInsert(false);
            inv.setItem(22, empty);
            return;
        }


        List<Map.Entry<String, PlayerStatsService.ItemUsage>> sorted = new ArrayList<>(stats.items.entrySet());
        sorted.sort(Comparator.<Map.Entry<String, PlayerStatsService.ItemUsage>>comparingLong(
                e -> e.getValue().equipped + e.getValue().used).reversed());


        int[] displaySlots = buildItemDisplaySlots();
        int shown = Math.min(sorted.size(), displaySlots.length);

        for (int i = 0; i < shown; i++) {
            Map.Entry<String, PlayerStatsService.ItemUsage> entry = sorted.get(i);
            String itemId = entry.getKey();
            PlayerStatsService.ItemUsage usage = entry.getValue();

            String displayName = humanizeItemId(itemId);
            Material icon = resolveItemIcon(itemId);

            List<Component> lore = List.of(
                    plugin.tr(viewer, "gui.stats.items.times_equipped", Map.of("value", usage.equipped)),
                    plugin.tr(viewer, "gui.stats.items.times_used", Map.of("value", usage.used))
            );

            InventoryItem itemDisplay = stat(icon,
                    plugin.tr(viewer, "gui.stats.item.display_name", Map.of("name", displayName)), lore);
            itemDisplay.setClickHandler((p, item, event, slot) -> event.setCancelled(true));
            itemDisplay.setAllowTakeout(false);
            itemDisplay.setAllowInsert(false);
            inv.setItem(displaySlots[i], itemDisplay);
        }
    }


    private int[] buildItemDisplaySlots() {

        List<Integer> slots = new ArrayList<>();
        for (int row = 1; row <= 3; row++) {
            for (int col = 1; col <= 7; col++) {
                slots.add(row * 9 + col);
            }
        }
        return slots.stream().mapToInt(Integer::intValue).toArray();
    }

    private void fillMaps(FrameworkInventory inv,
                          Player viewer,
                          PlayerStatsService.PlayerStatsRecord stats) {
        if (stats.mapsPlayed.isEmpty()) {
            InventoryItem empty = new InventoryItem(utility(GUIItems.STATS_BORDER,
                    plugin.tr(viewer, "gui.stats.maps.no_data"),
                    List.of(plugin.tr(viewer, "gui.stats.maps.no_tracked"))));
            empty.setClickHandler((p, item, event, slot) -> event.setCancelled(true));
            empty.setAllowTakeout(false);
            empty.setAllowInsert(false);
            inv.setItem(22, empty);
            return;
        }

        List<Map.Entry<String, Long>> sorted = new ArrayList<>(stats.mapsPlayed.entrySet());
        sorted.sort(Map.Entry.<String, Long>comparingByValue().reversed());

        long totalGames = sorted.stream().mapToLong(Map.Entry::getValue).sum();

        int[] displaySlots = buildItemDisplaySlots();
        int shown = Math.min(sorted.size(), displaySlots.length);

        for (int i = 0; i < shown; i++) {
            Map.Entry<String, Long> entry = sorted.get(i);
            String mapName = entry.getKey();
            long games = entry.getValue();
            double pct = totalGames == 0 ? 0.0 : (double) games / totalGames * 100.0;

            ItemStack icon = plugin.getMapManager() != null
                    ? plugin.getMapManager().getMapIcon(mapName, new ItemStack(Material.GRASS_BLOCK))
                    : new ItemStack(Material.GRASS_BLOCK);
            String prettyName = plugin.getMapManager() != null ? plugin.getMapManager().getMapData(
                    mapName).getDisplayName(plugin, viewer) : mapName;

            List<Component> lore = List.of(
                    plugin.tr(viewer, "gui.stats.maps.times_played", Map.of("value", games)),
                    plugin.tr(viewer, "gui.stats.maps.share", Map.of("value", String.format("%.1f%%", pct)))
            );

            InventoryItem mapItem = stat(icon,
                    plugin.tr(viewer, "gui.stats.item.display_name", Map.of("name", prettyName)), lore);
            mapItem.setClickHandler((p, item, event, slot) -> event.setCancelled(true));
            mapItem.setAllowTakeout(false);
            mapItem.setAllowInsert(false);
            inv.setItem(displaySlots[i], mapItem);
        }
    }

    private void fillPerks(FrameworkInventory inv,
                           Player viewer,
                           PlayerStatsService.PlayerStatsRecord stats) {

        int slot = 10;


        if (!stats.perksUsed.isEmpty()) {
            for (Map.Entry<String, Long> entry : stats.perksUsed.entrySet()) {
                if (slot >= 44) break;

                String perkName = humanizeItemId(entry.getKey());

                List<Component> lore = List.of(
                        plugin.tr(viewer, "gui.stats.perks.times_used", Map.of("value", entry.getValue()))
                );

                inv.setItem(slot++, stat(Material.ENCHANTED_BOOK,
                        plugin.tr(viewer, "gui.stats.perks.name", Map.of("perk", perkName)),
                        lore));
            }
        }

    }


    private void buildTabs(FrameworkInventory inv, Player viewer, Player target, StatsPage active) {

        inv.setItem(45, tab(viewer, target, active, StatsPage.OVERVIEW, GUIItems.STATS_TAB_OVERVIEW));
        inv.setItem(46, tab(viewer, target, active, StatsPage.COMBAT, GUIItems.STATS_TAB_COMBAT));
        inv.setItem(47, tab(viewer, target, active, StatsPage.ITEMS, GUIItems.STATS_TAB_ITEMS));
        inv.setItem(48, tab(viewer, target, active, StatsPage.MAPS, GUIItems.STATS_TAB_MAPS));
        inv.setItem(49, tab(viewer, target, active, StatsPage.PERKS, GUIItems.STATS_TAB_PERKS));
    }


    private InventoryItem tab(Player viewer, Player target, StatsPage active,
                              StatsPage page, String key) {
        boolean isActive = active == page;
        Component label = plugin.tr(viewer, page.tabKey());
        if (isActive) {
            label = label.append(plugin.tr(viewer, "gui.stats.tabs.selected_suffix"));
        }

        ItemStack stack = utility(key,
                label,
                List.of(plugin.tr(viewer, isActive ? "gui.stats.tabs.current" : "gui.stats.tabs.click_to_open")));

        if (isActive) {
            ItemMeta meta = stack.getItemMeta();
            if (meta != null) {
                meta.setEnchantmentGlintOverride(true);
                stack.setItemMeta(meta);
            }
        }

        InventoryItem btn = new InventoryItem(stack);
        btn.setClickHandler((p, item, event, slot) -> {
            if (!isActive) {
                open(viewer, target, page);
            }
            event.setCancelled(true);
        });
        btn.setAllowTakeout(false);
        btn.setAllowInsert(false);
        return btn;
    }

    private InventoryItem buildPlayerHead(Player viewer, Player target, PlayerStatsService.PlayerStatsRecord stats) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        if (head.getItemMeta() instanceof SkullMeta skullMeta) {
            skullMeta.setOwningPlayer(target);
            skullMeta.displayName(plugin.tr(viewer, "gui.stats.player_head.name", Map.of("player", target.getName()))
                    .decoration(TextDecoration.ITALIC, false));
            List<Component> lore = List.of(
                    plugin.tr(viewer, "gui.stats.player_head.hider_wins", Map.of("value", stats.hiderWins)),
                    plugin.tr(viewer, "gui.stats.player_head.seeker_wins", Map.of("value", stats.seekerWins)),
                    plugin.tr(viewer, "gui.stats.player_head.rounds_played",
                            Map.of("value", stats.hiderRoundsPlayed + stats.seekerRoundsPlayed))
            );
            skullMeta.lore(lore);
            head.setItemMeta(skullMeta);
        }

        InventoryItem item = new InventoryItem(head);
        item.setClickHandler((p, i, event, slot) -> event.setCancelled(true));
        item.setAllowTakeout(false);
        item.setAllowInsert(false);
        return item;
    }


    @SuppressWarnings("UnstableApiUsage")
    private void fillBorder(FrameworkInventory inv) {
        ItemStack pane = item(GUIItems.STATS_BORDER, new ItemStack(Material.GRAY_STAINED_GLASS_PANE));
        pane.setData(DataComponentTypes.TOOLTIP_DISPLAY, TooltipDisplay.tooltipDisplay().hideTooltip(true).build());
        InventoryItem border = new InventoryItem(pane);
        border.setClickHandler((p, item, event, slot) -> event.setCancelled(true));
        border.setAllowTakeout(false);
        border.setAllowInsert(false);


        for (int i = 0; i < 9; i++) inv.setItem(i, border);

        for (int i = 45; i < 54; i++) inv.setItem(i, border);

        for (int row = 1; row <= 4; row++) {
            inv.setItem(row * 9, border);
            inv.setItem(row * 9 + 8, border);
        }
    }


    private InventoryItem stat(Material material, Component name, List<Component> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(name.decoration(TextDecoration.ITALIC, false));
            meta.lore(lore.stream()
                    .map(c -> c.decoration(TextDecoration.ITALIC, false))
                    .toList());
            item.setItemMeta(meta);
        }

        return new InventoryItem(item);
    }

    private InventoryItem stat(ItemStack item, Component name, List<Component> lore) {
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(name.decoration(TextDecoration.ITALIC, false));
            meta.lore(lore.stream()
                    .map(c -> c.decoration(TextDecoration.ITALIC, false))
                    .toList());
            item.setItemMeta(meta);
        }

        return new InventoryItem(item);
    }


    private ItemStack utility(String key, Component name, List<Component> lore) {
        ItemStack item = item(key, new ItemStack(Material.DIRT));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(name.decoration(TextDecoration.ITALIC, false));
            meta.lore(lore.stream()
                    .map(c -> c.decoration(TextDecoration.ITALIC, false))
                    .toList());
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack item(String key, ItemStack fallback) {
        return plugin.getGuiItemRegistry().getOrDefault(GUINames.PLAYER_STATS, key, fallback);
    }

    private String formatDuration(Player viewer, long seconds) {
        if (seconds < 60) {
            return plugin.trText(viewer, "gui.stats.duration.seconds", Map.of("value", seconds));
        }
        if (seconds < 3600) {
            return plugin.trText(viewer, "gui.stats.duration.minutes_seconds", Map.of(
                    "minutes", seconds / 60,
                    "seconds", seconds % 60
            ));
        }
        return plugin.trText(viewer, "gui.stats.duration.hours_minutes", Map.of(
                "hours", seconds / 3600,
                "minutes", (seconds % 3600) / 60
        ));
    }


    private String formatPageName(Player viewer, StatsPage page) {
        return plugin.trText(viewer, page.pageKey());
    }


    private enum StatsPage {
        OVERVIEW("gui.stats.page.overview", "gui.stats.tabs.overview"),
        COMBAT("gui.stats.page.combat", "gui.stats.tabs.combat"),
        ITEMS("gui.stats.page.items", "gui.stats.tabs.items"),
        MAPS("gui.stats.page.maps", "gui.stats.tabs.maps"),
        PERKS("gui.stats.page.perks", "gui.stats.tabs.perks");

        private final String pageKey;
        private final String tabKey;

        StatsPage(String pageKey, String tabKey) {
            this.pageKey = pageKey;
            this.tabKey = tabKey;
        }

        private String pageKey() {
            return pageKey;
        }

        private String tabKey() {
            return tabKey;
        }
    }

    private String humanizeItemId(String itemId) {

        String stripped = itemId
                .replace("has_hider_", "")
                .replace("has_seeker_", "")
                .replace("has_", "");
        String[] parts = stripped.split("_");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!sb.isEmpty()) sb.append(' ');
            if (!part.isEmpty()) {
                sb.append(Character.toUpperCase(part.charAt(0)));
                sb.append(part.substring(1));
            }
        }
        return sb.toString();
    }

    private Material resolveItemIcon(String itemId) {

        GameItem item = HiderItems.getItem(itemId);
        if (item == null) item = SeekerItems.getItem(itemId);
        if (item == null) return Material.PAPER;
        return item.createItem(plugin).getType();
    }
}