package de.thecoolcraft11.hideAndSeek.gui;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class ReadyGUI {
    private static final String TITLE = "Ready Overview";
    private static final int MAX_DISPLAYED_PLAYERS = 27;

    private final HideAndSeek plugin;

    public ReadyGUI(HideAndSeek plugin) {
        this.plugin = plugin;
    }

    public void open(Player viewer) {
        VoteManager voteManager = plugin.getVoteManager();
        if (!voteManager.isReadinessEnabled()) {
            viewer.sendMessage(Component.text("Readiness is disabled.", NamedTextColor.RED));
            return;
        }
        if (voteManager.isNotLobbyPhase()) {
            viewer.sendMessage(Component.text("Readiness overview is only available in the lobby.", NamedTextColor.RED));
            return;
        }

        List<Player> players = new ArrayList<>(Bukkit.getOnlinePlayers());
        players.sort(Comparator.comparing(Player::getName, String.CASE_INSENSITIVE_ORDER));

        int shownPlayers = Math.min(players.size(), MAX_DISPLAYED_PLAYERS);
        int playerPairs = Math.max(1, (shownPlayers + 8) / 9);
        int totalRows = Math.max(2, playerPairs * 2);

        FrameworkInventory inventory = new InventoryBuilder(plugin.getInventoryFramework())
                .id("ready_overview_" + viewer.getUniqueId())
                .title(TITLE)
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

            InventoryItem statusItem = new InventoryItem(createStatusPane(ready, listedPlayer.getUniqueId()));
            statusItem.setClickHandler((p, item, event, slot) -> event.setCancelled(true));
            statusItem.setAllowTakeout(false);
            statusItem.setAllowInsert(false);
            statusItem.setMetadata("status_type", "ready_status");
            statusItem.setMetadata("is_ready", ready);
            inventory.setItem(statusSlot, statusItem);
        }

        if (players.size() > MAX_DISPLAYED_PLAYERS) {
            int infoSlot = totalRows * 9 - 1;
            InventoryItem infoItem = new InventoryItem(createOverflowInfo(players.size() - MAX_DISPLAYED_PLAYERS));
            infoItem.setClickHandler((p, item, event, slot) -> event.setCancelled(true));
            infoItem.setAllowTakeout(false);
            infoItem.setAllowInsert(false);
            inventory.setItem(infoSlot, infoItem);
        }

        plugin.getInventoryFramework().openInventory(viewer, inventory);
    }


    private ItemStack createPlayerHeadItem(Player player, boolean ready) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta rawMeta = item.getItemMeta();
        if (!(rawMeta instanceof SkullMeta meta)) {
            return item;
        }

        meta.setOwningPlayer(player);
        meta.displayName(Component.text(player.getName(), NamedTextColor.AQUA, TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Status: ", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                .append(Component.text(ready ? "READY" : "NOT READY", ready ? NamedTextColor.GREEN : NamedTextColor.RED)));
        lore.add(Component.text("Vote complete: ", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                .append(Component.text(plugin.getVoteManager().hasCompletedVote(player.getUniqueId()) ? "Yes" : "No", NamedTextColor.YELLOW)));
        meta.lore(lore);

        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createStatusPane(boolean ready, UUID playerId) {
        ItemStack item = new ItemStack(ready ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        meta.displayName(Component.text(ready ? "Ready" : "Not Ready", ready ? NamedTextColor.GREEN : NamedTextColor.RED, TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.text("Player UUID: " + playerId, NamedTextColor.DARK_GRAY).decoration(TextDecoration.ITALIC, false)
        ));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createOverflowInfo(int hiddenPlayers) {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        meta.displayName(Component.text("More players not shown", NamedTextColor.YELLOW, TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.text(hiddenPlayers + " additional players are online.", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false)
        ));
        item.setItemMeta(meta);
        return item;
    }
}
