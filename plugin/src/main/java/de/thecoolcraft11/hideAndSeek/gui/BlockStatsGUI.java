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
            player.sendMessage(Component.text("Block mode is not enabled!", NamedTextColor.RED));
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
            player.sendMessage(Component.text("No blocks have been chosen yet!", NamedTextColor.YELLOW));
            return;
        }
        int rows = Math.min(6, (blockCounts.size() + 8) / 9);

        FrameworkInventory inventory = new InventoryBuilder(plugin.getInventoryFramework())
                .id("block_stats_" + player.getUniqueId())
                .title("Block Statistics")
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
            ItemStack item = createStatsItem(material, count, players);

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

    private ItemStack createStatsItem(Material material, int count, List<String> players) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String name = formatName(material.name());
            meta.displayName(Component.text(name, NamedTextColor.GOLD, TextDecoration.BOLD)
                    .decoration(TextDecoration.ITALIC, false));
            List<Component> lore = new ArrayList<>();
            lore.add(Component.empty());
            lore.add(Component.text("Players: " + count, NamedTextColor.YELLOW)
                    .decoration(TextDecoration.ITALIC, false));
            if (players != null && !players.isEmpty()) {
                lore.add(Component.empty());
                int displayCount = Math.min(players.size(), 10);
                for (int i = 0; i < displayCount; i++) {
                    lore.add(Component.text("  • " + players.get(i), NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false));
                }
                if (players.size() > 10) {
                    lore.add(Component.text("  ... +" + (players.size() - 10) + " more", NamedTextColor.DARK_GRAY)
                            .decoration(TextDecoration.ITALIC, false));
                }
            }
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        item.setAmount(Math.min(64, Math.max(1, count)));
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
