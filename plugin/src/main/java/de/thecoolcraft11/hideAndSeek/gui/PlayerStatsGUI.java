package de.thecoolcraft11.hideAndSeek.gui;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.playerdata.PlayerStatsService;
import de.thecoolcraft11.minigameframework.inventory.FrameworkInventory;
import de.thecoolcraft11.minigameframework.inventory.InventoryBuilder;
import de.thecoolcraft11.minigameframework.inventory.InventoryItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
            viewer.sendMessage(Component.text("Stats service is not available.", NamedTextColor.RED));
            return;
        }

        PlayerStatsService.PlayerStatsRecord stats = service.getSnapshot(target.getUniqueId());

        FrameworkInventory inv = new InventoryBuilder(plugin.getInventoryFramework())
                .id("player_stats_" + viewer.getUniqueId() + "_" + target.getUniqueId() + "_" + page.name())
                .title(page == StatsPage.OVERVIEW ? "Stats: " + target.getName()
                        : "Stats: " + target.getName() + " » " + formatPageName(page))
                .rows(6)
                .allowOutsideClicks(false)
                .allowDrag(false)
                .allowPlayerInventoryInteraction(false)
                .build();


        fillBorder(inv);


        inv.setItem(0, buildPlayerHead(target, stats));


        switch (page) {
            case OVERVIEW -> fillOverview(inv, stats);
            case COMBAT -> fillCombat(inv, stats);
            case ITEMS -> fillItems(inv, stats);
            case MAPS -> fillMaps(inv, stats);
            case PERKS -> fillPerks(inv, stats);
        }


        buildTabs(inv, viewer, target, page);


        InventoryItem closeBtn = new InventoryItem(utility(Material.BARRIER, "Close",
                NamedTextColor.RED, List.of()));
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
                              PlayerStatsService.PlayerStatsRecord stats) {

        long totalGames = stats.hiderWins + stats.seekerWins + stats.totalHiderDeaths;
        double winRate = totalGames == 0 ? 0.0 : (double) (stats.hiderWins + stats.seekerWins) / totalGames * 100.0;

        List<Component> wlLore = List.of(
                lore("Hider Wins: ", NamedTextColor.GREEN, stats.hiderWins),
                lore("Seeker Wins: ", NamedTextColor.RED, stats.seekerWins),
                lore("Hider Deaths: ", NamedTextColor.GRAY, stats.totalHiderDeaths),
                Component.empty(),
                lore("Win Rate: ", NamedTextColor.GOLD, String.format("%.1f%%", winRate))
        );
        inv.setItem(10, stat(Material.LIME_STAINED_GLASS, "Win / Loss", NamedTextColor.GREEN, wlLore));


        List<Component> roundsLore = List.of(
                lore("Hider Rounds: ", NamedTextColor.AQUA, stats.hiderRoundsPlayed),
                lore("Seeker Rounds: ", NamedTextColor.RED, stats.seekerRoundsPlayed),
                lore("Last Hider Standing: ", NamedTextColor.GOLD, stats.totalRoundsAsLastHider)
        );
        inv.setItem(12, stat(Material.BOOK, "Rounds Played", NamedTextColor.AQUA, roundsLore));


        long totalSurvivalSec = stats.hiderSurvivalMs / 1000;
        long longestSurvivalSec = stats.longestHiderSurvivalMs / 1000;
        List<Component> survivalLore = List.of(
                lore("Total Survival: ", NamedTextColor.GREEN, formatDuration(totalSurvivalSec)),
                lore("Longest Survival: ", NamedTextColor.GOLD, formatDuration(longestSurvivalSec))
        );
        inv.setItem(14, stat(Material.CLOCK, "Survival Time", NamedTextColor.YELLOW, survivalLore));


        long totalPlaySec = stats.totalPlaytimeMs / 1000;
        List<Component> playtimeLore = List.of(
                lore("Total Playtime: ", NamedTextColor.AQUA, formatDuration(totalPlaySec))
        );
        inv.setItem(16, stat(Material.COMPASS, "Playtime", NamedTextColor.AQUA, playtimeLore));


        List<Component> ecoLore = List.of(
                lore("Points Earned: ", NamedTextColor.GOLD, stats.totalPointsEarned),
                lore("Coins Earned: ", NamedTextColor.YELLOW, stats.totalCoinsEarned)
        );
        inv.setItem(28, stat(Material.GOLD_INGOT, "Economy", NamedTextColor.GOLD, ecoLore));


        List<Component> tauntLore = List.of(
                lore("Total Taunts: ", NamedTextColor.LIGHT_PURPLE, stats.totalTauntsUsed)
        );
        inv.setItem(30, stat(Material.CAT_SPAWN_EGG, "Taunts Used", NamedTextColor.LIGHT_PURPLE, tauntLore));

        List<Component> modeLore = new ArrayList<>();
        if (stats.gameModesPlayed.isEmpty()) {
            modeLore.add(Component.text("No games played yet.", NamedTextColor.DARK_GRAY)
                    .decoration(TextDecoration.ITALIC, false));
        } else {
            for (Map.Entry<String, Long> e : stats.gameModesPlayed.entrySet()) {
                modeLore.add(lore(e.getKey() + ": ", NamedTextColor.AQUA,
                        e.getValue() + " game" + (e.getValue() == 1 ? "" : "s")));
            }
        }
        inv.setItem(32, stat(Material.COMPASS, "Game Modes", NamedTextColor.AQUA, modeLore));


        long hiddenSec = stats.totalHiddenInBlockModeMs / 1000;
        long unhiddenSec = stats.totalUnhiddenInBlockModeMs / 1000;
        long totalBlockSec = hiddenSec + unhiddenSec;
        double hiddenPct = totalBlockSec == 0 ? 0.0 : (double) hiddenSec / totalBlockSec * 100.0;
        List<Component> blockLore = List.of(
                lore("Time Hidden: ", NamedTextColor.GREEN, formatDuration(hiddenSec)),
                lore("Time Exposed: ", NamedTextColor.RED, formatDuration(unhiddenSec)),
                lore("Hidden Rate: ", NamedTextColor.GOLD, String.format("%.1f%%", hiddenPct))
        );
        inv.setItem(34, stat(Material.GRASS_BLOCK, "Block Mode", NamedTextColor.GREEN, blockLore));
    }


    private void fillCombat(FrameworkInventory inv,
                            PlayerStatsService.PlayerStatsRecord stats) {

        List<Component> killsLore = List.of(
                lore("Total Kills: ", NamedTextColor.RED, stats.totalSeekerKills),
                lore("Best in One Round: ", NamedTextColor.GOLD, stats.mostKillsInASeekerRound)
        );
        inv.setItem(10, stat(Material.IRON_SWORD, "Seeker Kills", NamedTextColor.RED, killsLore));


        List<Component> damageLore = List.of(
                lore("Total Damage: ", NamedTextColor.GOLD, String.format("%.1f", stats.totalDamageDealt))
        );
        inv.setItem(12, stat(Material.GOLDEN_SWORD, "Damage Dealt", NamedTextColor.GOLD, damageLore));


        List<Component> deathsLore = List.of(
                lore("Times Found: ", NamedTextColor.GRAY, stats.totalHiderDeaths)
        );
        inv.setItem(14, stat(Material.SKELETON_SKULL, "Hider Deaths", NamedTextColor.GRAY, deathsLore));


        double kd = stats.totalHiderDeaths == 0
                ? stats.totalSeekerKills
                : (double) stats.totalSeekerKills / stats.totalHiderDeaths;
        List<Component> kdLore = List.of(
                lore("Kills: ", NamedTextColor.GREEN, stats.totalSeekerKills),
                lore("Deaths: ", NamedTextColor.RED, stats.totalHiderDeaths),
                Component.empty(),
                lore("K/D: ", NamedTextColor.GOLD, String.format("%.2f", kd))
        );
        inv.setItem(16, stat(Material.SHIELD, "K/D Ratio", NamedTextColor.GOLD, kdLore));


        List<Component> survivalLore = List.of(
                lore("Total as Hider: ", NamedTextColor.GREEN, formatDuration(stats.hiderSurvivalMs / 1000)),
                lore("Longest Round: ", NamedTextColor.GOLD, formatDuration(stats.longestHiderSurvivalMs / 1000))
        );
        inv.setItem(28, stat(Material.CLOCK, "Hider Survival", NamedTextColor.GREEN, survivalLore));


        List<Component> lastHiderLore = List.of(
                lore("Times Last Hider: ", NamedTextColor.LIGHT_PURPLE, stats.totalRoundsAsLastHider)
        );
        inv.setItem(30,
                stat(Material.TOTEM_OF_UNDYING, "Last Hider Standing", NamedTextColor.LIGHT_PURPLE, lastHiderLore));
    }


    private void fillItems(FrameworkInventory inv,
                           PlayerStatsService.PlayerStatsRecord stats) {
        if (stats.items.isEmpty()) {
            InventoryItem empty = new InventoryItem(utility(Material.GRAY_STAINED_GLASS_PANE,
                    "No Item Data", NamedTextColor.DARK_GRAY,
                    List.of(Component.text("No items have been tracked yet.", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false))));
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
                    lore("Times Equipped: ", NamedTextColor.AQUA, usage.equipped),
                    lore("Times Used: ", NamedTextColor.GREEN, usage.used)
            );

            InventoryItem itemDisplay = stat(icon, displayName, NamedTextColor.YELLOW, lore);
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
                          PlayerStatsService.PlayerStatsRecord stats) {
        if (stats.mapsPlayed.isEmpty()) {
            InventoryItem empty = new InventoryItem(utility(Material.GRAY_STAINED_GLASS_PANE,
                    "No Map Data", NamedTextColor.DARK_GRAY,
                    List.of(Component.text("No maps have been tracked yet.", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false))));
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

            Material icon = plugin.getMapManager() != null
                    ? plugin.getMapManager().getMapIconMaterial(mapName, Material.GRASS_BLOCK)
                    : Material.GRASS_BLOCK;
            String prettyName = plugin.getMapManager() != null ? plugin.getMapManager().getMapData(
                    mapName).getDisplayName() : mapName;

            List<Component> lore = List.of(
                    lore("Times Played: ", NamedTextColor.AQUA, games),
                    lore("Share: ", NamedTextColor.GOLD, String.format("%.1f%%", pct))
            );

            InventoryItem mapItem = stat(icon, prettyName, NamedTextColor.GREEN, lore);
            mapItem.setClickHandler((p, item, event, slot) -> event.setCancelled(true));
            mapItem.setAllowTakeout(false);
            mapItem.setAllowInsert(false);
            inv.setItem(displaySlots[i], mapItem);
        }
    }

    private void fillPerks(FrameworkInventory inv,
                           PlayerStatsService.PlayerStatsRecord stats) {

        int slot = 10;


        if (!stats.perksUsed.isEmpty()) {
            for (Map.Entry<String, Long> entry : stats.perksUsed.entrySet()) {
                if (slot >= 44) break;

                String perkName = humanizeItemId(entry.getKey());

                List<Component> lore = List.of(
                        lore("Times Used: ", NamedTextColor.AQUA, entry.getValue())
                );

                inv.setItem(slot++, stat(Material.ENCHANTED_BOOK,
                        "Perk: " + perkName,
                        NamedTextColor.LIGHT_PURPLE,
                        lore));
            }
        }

    }


    private void buildTabs(FrameworkInventory inv, Player viewer, Player target, StatsPage active) {

        inv.setItem(45, tab(viewer, target, active, StatsPage.OVERVIEW, Material.BOOK, "Overview"));
        inv.setItem(46, tab(viewer, target, active, StatsPage.COMBAT, Material.IRON_SWORD, "Combat"));
        inv.setItem(47, tab(viewer, target, active, StatsPage.ITEMS, Material.BLAZE_POWDER, "Items"));
        inv.setItem(48, tab(viewer, target, active, StatsPage.MAPS, Material.MAP, "Maps"));
        inv.setItem(49, tab(viewer, target, active, StatsPage.PERKS, Material.LIGHT, "Perks"));
    }


    private InventoryItem tab(Player viewer, Player target, StatsPage active,
                              StatsPage page, Material icon, String label) {
        boolean isActive = active == page;
        ItemStack stack = utility(icon,
                label + (isActive ? " (Selected)" : ""),
                isActive ? NamedTextColor.GREEN : NamedTextColor.YELLOW,
                List.of(Component.text(isActive ? "Current tab" : "Click to open", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)));

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

    private InventoryItem buildPlayerHead(Player target, PlayerStatsService.PlayerStatsRecord stats) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        if (head.getItemMeta() instanceof SkullMeta skullMeta) {
            skullMeta.setOwningPlayer(target);
            skullMeta.displayName(Component.text(target.getName(), NamedTextColor.AQUA, TextDecoration.BOLD)
                    .decoration(TextDecoration.ITALIC, false));
            List<Component> lore = List.of(
                    lore("Hider Wins: ", NamedTextColor.GREEN, stats.hiderWins),
                    lore("Seeker Wins: ", NamedTextColor.RED, stats.seekerWins),
                    lore("Rounds Played: ", NamedTextColor.GRAY, stats.hiderRoundsPlayed + stats.seekerRoundsPlayed)
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


    private void fillBorder(FrameworkInventory inv) {
        ItemStack pane = utility(Material.GRAY_STAINED_GLASS_PANE, " ", NamedTextColor.DARK_GRAY, List.of());
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


    private InventoryItem stat(Material material, String name, NamedTextColor color, List<Component> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(name, color, TextDecoration.BOLD)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(lore.stream()
                    .map(c -> c.decoration(TextDecoration.ITALIC, false))
                    .toList());
            item.setItemMeta(meta);
        }

        return new InventoryItem(item);
    }


    private ItemStack utility(Material material, String name, NamedTextColor color, List<Component> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(name, color, TextDecoration.BOLD)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(lore.stream()
                    .map(c -> c.decoration(TextDecoration.ITALIC, false))
                    .toList());
            item.setItemMeta(meta);
        }
        return item;
    }

    private Component lore(String label, NamedTextColor valueColor, long value) {
        return Component.text(label, NamedTextColor.GRAY)
                .append(Component.text(String.valueOf(value), valueColor))
                .decoration(TextDecoration.ITALIC, false);
    }

    private Component lore(String label, NamedTextColor valueColor, String value) {
        return Component.text(label, NamedTextColor.GRAY)
                .append(Component.text(value, valueColor))
                .decoration(TextDecoration.ITALIC, false);
    }

    private String formatDuration(long seconds) {
        if (seconds < 60) return seconds + "s";
        if (seconds < 3600) return (seconds / 60) + "m " + (seconds % 60) + "s";
        return (seconds / 3600) + "h " + ((seconds % 3600) / 60) + "m";
    }


    private String formatPageName(StatsPage page) {
        return switch (page) {
            case OVERVIEW -> "Overview";
            case COMBAT -> "Combat";
            case ITEMS -> "Items";
            case MAPS -> "Maps";
            case PERKS -> "Details";
        };
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

        if (itemId.contains("explosion") || itemId.contains("firecracker")) return Material.RED_CANDLE;
        if (itemId.contains("sound") || itemId.contains("cat")) return Material.CAT_SPAWN_EGG;
        if (itemId.contains("speed")) return Material.GOLDEN_HOE;
        if (itemId.contains("random_block")) return Material.BLAZE_POWDER;
        if (itemId.contains("crossbow")) return Material.CROSSBOW;
        if (itemId.contains("knockback")) return Material.STICK;
        if (itemId.contains("block_swap")) return Material.ENDER_PEARL;
        if (itemId.contains("big_firecracker")) return Material.TNT;
        if (itemId.contains("firework_rocket") || itemId.contains("firework")) return Material.FIREWORK_ROCKET;
        if (itemId.contains("medkit")) return Material.FLOWER_BANNER_PATTERN;
        if (itemId.contains("totem")) return Material.TOTEM_OF_UNDYING;
        if (itemId.contains("invisibility_cloak") || itemId.contains("invisible")) return Material.PHANTOM_MEMBRANE;
        if (itemId.contains("slowness_ball")) return Material.SNOWBALL;
        if (itemId.contains("smoke_bomb")) return Material.GRAY_DYE;
        if (itemId.contains("ghost_essence")) return Material.GHAST_TEAR;
        if (itemId.contains("remote_gateway")) return Material.ENDER_EYE;
        if (itemId.contains("grappling")) return Material.FISHING_ROD;
        if (itemId.contains("ink_splash")) return Material.INK_SAC;
        if (itemId.contains("lightning_freeze")) return Material.LIGHTNING_ROD;
        if (itemId.contains("glowing_compass")) return Material.COMPASS;
        if (itemId.contains("curse_spell")) return Material.ENCHANTED_BOOK;
        if (itemId.contains("block_randomizer")) return Material.BLAZE_POWDER;
        if (itemId.contains("chain_pull")) return Material.LEAD;
        if (itemId.contains("proximity_sensor")) return Material.REDSTONE_TORCH;
        if (itemId.contains("cage_trap")) return Material.IRON_BARS;
        if (itemId.contains("camera")) return Material.FILLED_MAP;
        if (itemId.contains("phantom_viewer")) return Material.FILLED_MAP;
        if (itemId.contains("sword")) return Material.IRON_SWORD;
        if (itemId.contains("assistant")) return Material.ZOMBIE_HEAD;
        return Material.PAPER; 
    }


    private enum StatsPage {
        OVERVIEW,
        COMBAT,
        ITEMS,
        MAPS,
        PERKS
    }
}