package de.thecoolcraft11.hideAndSeek.gui;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.minigameframework.inventory.FrameworkInventory;
import de.thecoolcraft11.minigameframework.inventory.InventoryBuilder;
import de.thecoolcraft11.minigameframework.inventory.InventoryItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class BlockStatsGUI {
    private final HideAndSeek plugin;

    public BlockStatsGUI(HideAndSeek plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        var gameModeResult = plugin.getSettingService().getSetting("game.mode");
        Object gameModeObj = gameModeResult.isSuccess() ? gameModeResult.getValue() : null;
        if (gameModeObj == null || !gameModeObj.toString().equals("BLOCK")) {
            player.sendMessage(plugin.tr(player, "gui.block_stats.errors.mode_disabled"));
            return;
        }
        boolean showNames = plugin.getSettingRegistry().get("game.blockstats.show-names", false);
        Map<Material, Integer> blockCounts = new HashMap<>();
        Map<Material, List<String>> blockPlayers = new HashMap<>();
        for (UUID hiderId : HideAndSeek.getDataController().getHiders()) {
            Material chosenBlock = HideAndSeek.getDataController().getChosenBlock(hiderId);
            Player hider = Bukkit.getPlayer(hiderId);
            if (chosenBlock != null && hider != null && hider.isOnline()) {
                blockCounts.put(chosenBlock, blockCounts.getOrDefault(chosenBlock, 0) + 1);
                if (showNames) {
                    blockPlayers.computeIfAbsent(chosenBlock, k -> new ArrayList<>()).add(hider.getName());
                }
            }
        }
        if (blockCounts.isEmpty()) {
            player.sendMessage(plugin.tr(player, "gui.block_stats.errors.no_blocks"));
            return;
        }
        int rows = Math.min(6, (blockCounts.size() + 8) / 9);

        FrameworkInventory inventory = new InventoryBuilder(plugin.getInventoryFramework())
                .id("block_stats_" + player.getUniqueId())
                .title(plugin.trText(player, "gui.block_stats.title"))
                .rows(rows)
                .allowOutsideClicks(false)
                .allowDrag(false)
                .allowPlayerInventoryInteraction(false)
                .build();

        List<Map.Entry<Material, Integer>> sortedEntries = new ArrayList<>(blockCounts.entrySet());
        sortedEntries.sort((a, b) -> b.getValue().compareTo(a.getValue()));
        int slot = 0;
        for (Map.Entry<Material, Integer> entry : sortedEntries) {
            if (slot >= rows * 9) break;
            Material material = entry.getKey();
            int count = entry.getValue();
            List<String> players = showNames ? blockPlayers.get(material) : null;
            ItemStack item = createStatsItem(material, count, players, player);

            InventoryItem statsItem = new InventoryItem(item);
            statsItem.setClickHandler((p, invItem, event, s) -> event.setCancelled(true));
            statsItem.setAllowTakeout(false);
            statsItem.setAllowInsert(false);
            statsItem.setMetadata("material", material.name());
            statsItem.setMetadata("count", count);

            inventory.setItem(slot, statsItem);
            slot++;
        }
        plugin.getInventoryFramework().openInventory(player, inventory);
    }

    private ItemStack createStatsItem(Material material, int count, List<String> players, Player viewer) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String name = formatName(material.name());
            meta.displayName(Component.text(name, NamedTextColor.GOLD, TextDecoration.BOLD)
                    .decoration(TextDecoration.ITALIC, false));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());
            lore.add(plugin.tr(viewer, "gui.block_stats.item.players_count", Map.of("count", count))
                    .decoration(TextDecoration.ITALIC, false));
            if (players != null && !players.isEmpty()) {
                lore.add(Component.empty());
                int displayCount = Math.min(players.size(), 10);
                for (int i = 0; i < displayCount; i++) {
                    lore.add(plugin.tr(viewer, "gui.block_stats.item.player_entry", Map.of("name", players.get(i)))
                            .decoration(TextDecoration.ITALIC, false));
                }
                if (players.size() > 10) {
                    lore.add(
                            plugin.tr(viewer, "gui.block_stats.item.more_players", Map.of("count", players.size() - 10))
                            .decoration(TextDecoration.ITALIC, false));
                }
            }
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        item.setAmount(Math.clamp(count, 1, 64));
        return item;
    }

    private String formatName(String name) {
        StringBuilder result = new StringBuilder();
        for (String part : name.toLowerCase().split("_")) {
            if (!result.isEmpty()) result.append(" ");
            result.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return result.toString();
    }
}
