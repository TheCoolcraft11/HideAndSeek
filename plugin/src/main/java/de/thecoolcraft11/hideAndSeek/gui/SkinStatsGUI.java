package de.thecoolcraft11.hideAndSeek.gui;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.gui.config.GUIItems;
import de.thecoolcraft11.hideAndSeek.gui.config.GUINames;
import de.thecoolcraft11.hideAndSeek.model.SkinData;
import de.thecoolcraft11.hideAndSeek.util.skin.SkinManager;
import de.thecoolcraft11.minigameframework.inventory.FrameworkInventory;
import de.thecoolcraft11.minigameframework.inventory.InventoryBuilder;
import de.thecoolcraft11.minigameframework.inventory.InventoryItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class SkinStatsGUI {

    private final HideAndSeek plugin;
    private final SkinManager skinManager;

    public SkinStatsGUI(
            HideAndSeek plugin,
            SkinManager skinManager
    ) {
        this.plugin = plugin;
        this.skinManager = skinManager;
    }

    public void open(Player player) {

        var gameModeResult = plugin.getSettingService()
                .getSetting("game.mode");

        Object gameModeObj = gameModeResult.isSuccess()
                ? gameModeResult.getValue()
                : null;

        if (
                gameModeObj == null
                        || !gameModeObj.toString().equals("SKIN")
        ) {

            player.sendMessage(plugin.tr(player, "gui.skin_stats.errors.disabled"));

            return;
        }

        boolean showNames = plugin.getSettingRegistry()
                .get("game.skinstats.show-names", false);

        Map<String, Integer> skinCounts = new HashMap<>();

        Map<String, List<String>> skinPlayers = new HashMap<>();

        Map<String, SkinData> skinDataMap = new HashMap<>();

        for (UUID hiderId : HideAndSeek.getDataController().getHiders()) {

            SkinData skin = skinManager.getAssignedSkin(hiderId);

            Player hider = Bukkit.getPlayer(hiderId);

            if (
                    skin != null
                            && hider != null
                            && hider.isOnline()
            ) {

                skinCounts.put(
                        skin.id(),
                        skinCounts.getOrDefault(skin.id(), 0) + 1
                );

                skinDataMap.put(skin.id(), skin);

                if (showNames) {

                    skinPlayers.computeIfAbsent(
                            skin.id(),
                            k -> new ArrayList<>()
                    ).add(hider.getName());
                }
            }
        }

        if (skinCounts.isEmpty()) {

            player.sendMessage(plugin.tr(player, "gui.skin_stats.errors.empty"));

            return;
        }

        int rows = Math.min(
                6,
                (skinCounts.size() + 8) / 9
        );

        FrameworkInventory inventory = new InventoryBuilder(
                plugin.getInventoryFramework()
        )
                .id("skin_stats_" + player.getUniqueId())
                .title(plugin.trText(player, "gui.skin_stats.title"))
                .rows(rows)
                .allowOutsideClicks(false)
                .allowDrag(false)
                .allowPlayerInventoryInteraction(false)
                .build();

        List<Map.Entry<String, Integer>> sortedEntries =
                new ArrayList<>(skinCounts.entrySet());

        sortedEntries.sort(
                (a, b) -> b.getValue().compareTo(a.getValue())
        );

        int slot = 0;

        for (Map.Entry<String, Integer> entry : sortedEntries) {

            if (slot >= rows * 9) {
                break;
            }

            String skinId = entry.getKey();

            int count = entry.getValue();

            SkinData skin = skinDataMap.get(skinId);

            List<String> players = showNames
                    ? skinPlayers.get(skinId)
                    : null;

            ItemStack item = createStatsItem(
                    player,
                    skin,
                    count,
                    players
            );

            InventoryItem statsItem = new InventoryItem(item);

            statsItem.setClickHandler(
                    (p, invItem, event, s) ->
                            event.setCancelled(true)
            );

            statsItem.setAllowTakeout(false);
            statsItem.setAllowInsert(false);

            statsItem.setMetadata("skin_id", skinId);
            statsItem.setMetadata("count", count);

            inventory.setItem(slot, statsItem);

            slot++;
        }

        plugin.getInventoryFramework()
                .openInventory(player, inventory);
    }

    private ItemStack createStatsItem(
            Player viewer,
            SkinData skin,
            int count,
            List<String> players
    ) {

        ItemStack icon = skin.icon() != null
                ? skin.icon()
                : plugin.getGuiItemRegistry().getOrDefault(GUINames.SKIN_STATS, GUIItems.KEY_FALLBACK, new ItemStack(
                Material.PLAYER_HEAD));

        ItemStack item = new ItemStack(icon);

        ItemMeta meta = item.getItemMeta();

        if (meta != null) {

            meta.displayName(
                    plugin.tr(viewer, "gui.skin_stats.item.name", Map.of("skin", skin.getDisplayName(plugin, viewer)))
                            .decoration(
                                    TextDecoration.ITALIC,
                                    false
                            )
            );

            List<Component> lore = new ArrayList<>();

            lore.add(Component.empty());
            lore.add(
                    plugin.tr(viewer, "gui.skin_stats.item.players_count", Map.of("count", count))
                            .decoration(TextDecoration.ITALIC, false)
            );

            if (
                    players != null
                            && !players.isEmpty()
            ) {

                lore.add(Component.empty());

                int displayCount = Math.min(
                        players.size(),
                        10
                );

                for (int i = 0; i < displayCount; i++) {

                    lore.add(
                            plugin.tr(viewer, "gui.skin_stats.item.player_entry", Map.of("name", players.get(i)))
                                    .decoration(TextDecoration.ITALIC, false)
                    );
                }

                if (players.size() > 10) {

                    lore.add(
                            plugin.tr(viewer, "gui.skin_stats.item.more_players", Map.of("count", players.size() - 10))
                                    .decoration(TextDecoration.ITALIC, false)
                    );
                }
            }

            meta.lore(lore);

            item.setItemMeta(meta);
        }

        item.setAmount(Math.clamp(count, 1, 64));

        return item;
    }
}