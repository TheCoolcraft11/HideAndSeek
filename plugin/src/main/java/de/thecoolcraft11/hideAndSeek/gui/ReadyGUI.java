package de.thecoolcraft11.hideAndSeek.gui;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.gui.config.GUIItems;
import de.thecoolcraft11.hideAndSeek.gui.config.GUINames;
import de.thecoolcraft11.hideAndSeek.vote.VoteManager;
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
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;

public class ReadyGUI {
    private static final int MAX_DISPLAYED_PLAYERS = 27;

    private final HideAndSeek plugin;

    public ReadyGUI(HideAndSeek plugin) {
        this.plugin = plugin;
    }

    public void open(Player viewer) {
        VoteManager voteManager = plugin.getVoteManager();

        if (!voteManager.isReadinessEnabled()) {
            viewer.sendMessage(plugin.tr(viewer, "gui.ready.errors.disabled"));
            return;
        }

        if (voteManager.isNotLobbyPhase()) {
            viewer.sendMessage(plugin.tr(viewer, "gui.ready.errors.lobby_only"));
            return;
        }

        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
        players.sort(Comparator.comparing(Player::getName, String.CASE_INSENSITIVE_ORDER));

        int shownPlayers = Math.min(players.size(), MAX_DISPLAYED_PLAYERS);
        int playerPairs = Math.max(1, (shownPlayers + 8) / 9);
        int totalRows = Math.max(2, playerPairs * 2);

        FrameworkInventory inventory = new InventoryBuilder(plugin.getInventoryFramework())
                .id("ready_overview_" + viewer.getUniqueId())
                .title(plugin.trText(viewer, "gui.ready.title"))
                .rows(totalRows)
                .allowOutsideClicks(false)
                .allowDrag(false)
                .allowPlayerInventoryInteraction(false)
                .build();

        for (int i = 0; i < shownPlayers; i++) {
            Player listedPlayer = players.get(i);
            boolean ready = voteManager.isReady(listedPlayer.getUniqueId());

            int pairRow = i / 9;
            int column = i % 9;
            int headSlot = pairRow * 18 + column;
            int statusSlot = headSlot + 9;

            InventoryItem headItem = new InventoryItem(createPlayerHeadItem(listedPlayer, ready));
            headItem.setClickHandler((p, item, event, slot) -> event.setCancelled(true));
            headItem.setAllowTakeout(false);
            headItem.setAllowInsert(false);
            headItem.setMetadata("player_uuid", listedPlayer.getUniqueId().toString());
            headItem.setMetadata("player_ready", ready);
            inventory.setItem(headSlot, headItem);

            InventoryItem statusItem = new InventoryItem(createStatusPane(ready, listedPlayer.getUniqueId(), viewer));
            statusItem.setClickHandler((p, item, event, slot) -> event.setCancelled(true));
            statusItem.setAllowTakeout(false);
            statusItem.setAllowInsert(false);
            statusItem.setMetadata("status_type", "ready_status");
            statusItem.setMetadata("is_ready", ready);
            inventory.setItem(statusSlot, statusItem);
        }

        if (players.size() > MAX_DISPLAYED_PLAYERS) {
            int infoSlot = totalRows * 9 - 1;

            InventoryItem infoItem = new InventoryItem(createOverflowInfo(players.size() - MAX_DISPLAYED_PLAYERS,
                    viewer));
            infoItem.setClickHandler((p, item, event, slot) -> event.setCancelled(true));
            infoItem.setAllowTakeout(false);
            infoItem.setAllowInsert(false);

            inventory.setItem(infoSlot, infoItem);
        }

        plugin.getInventoryFramework().openInventory(viewer, inventory);
    }


    private ItemStack createPlayerHeadItem(Player player, boolean ready) {
        ItemStack item = item(GUIItems.R_PLAYER, new ItemStack(Material.PLAYER_HEAD));
        ItemMeta rawMeta = item.getItemMeta();

        if (!(rawMeta instanceof SkullMeta meta)) {
            return item;
        }

        meta.setOwningPlayer(player);

        meta.displayName(Component.text(player.getName(), NamedTextColor.AQUA, TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();

        lore.add(plugin.tr(player, "gui.ready.status.title", Map.of("status", plugin.trText(player, ready
                ? "gui.ready.status.ready"
                : "gui.ready.status.not_ready"))));

        lore.add(plugin.tr(player, "gui.ready.vote.title",
                Map.of("status", plugin.trText(player, plugin.getVoteManager().hasCompletedVote(player.getUniqueId())
                        ? "gui.ready.vote.complete_yes"
                        : "gui.ready.vote.complete_no"))));

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createStatusPane(boolean ready, UUID playerId, Player player) {
        ItemStack item = new ItemStack(
                ready ? item(GUIItems.KEY_READY, new ItemStack(Material.LIME_STAINED_GLASS_PANE))
                        : item(GUIItems.KEY_NOT_READY, new ItemStack(Material.RED_STAINED_GLASS_PANE)));

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return item;

        meta.displayName(plugin.tr(player,
                ready ? "gui.ready.status.ready" : "gui.ready.status.not_ready"
        ));

        meta.lore(List.of(
                plugin.tr(player,
                        "gui.ready.status.player_uuid",
                        Map.of("uuid", playerId.toString())
                )
        ));

        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createOverflowInfo(int hiddenPlayers, Player player) {
        ItemStack item = item(GUIItems.R_OVERFLOW, new ItemStack(Material.BOOK));
        ItemMeta meta = item.getItemMeta();

        if (meta == null) return item;

        meta.displayName(plugin.tr(player, "gui.ready.overflow.title"));

        meta.lore(List.of(
                plugin.tr(player, "gui.ready.overflow.info",
                        Map.of("count", hiddenPlayers))
        ));

        item.setItemMeta(meta);
        return item;
    }

    private ItemStack item(String key, ItemStack fallback) {
        return plugin.getGuiItemRegistry().getOrDefault(GUINames.READY, key, fallback);
    }
}
