package de.thecoolcraft11.hideAndSeek.gui;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.vote.VoteManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class ReadyGUI implements Listener {
    private static final Component TITLE = Component.text("Ready Overview", NamedTextColor.GOLD);
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

        Inventory inventory = Bukkit.createInventory(new ReadyMenuHolder(), totalRows * 9, TITLE);

        for (int i = 0; i < shownPlayers; i++) {
            Player listedPlayer = players.get(i);
            boolean ready = voteManager.isReady(listedPlayer.getUniqueId());
            int pairRow = i / 9;
            int column = i % 9;
            int headSlot = pairRow * 18 + column;
            int statusSlot = headSlot + 9;

            inventory.setItem(headSlot, createPlayerHeadItem(listedPlayer, ready));
            inventory.setItem(statusSlot, createStatusPane(ready, listedPlayer.getUniqueId()));
        }

        if (players.size() > MAX_DISPLAYED_PLAYERS) {
            int infoSlot = totalRows * 9 - 1;
            inventory.setItem(infoSlot, createOverflowInfo(players.size() - MAX_DISPLAYED_PLAYERS));
        }

        viewer.openInventory(inventory);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof ReadyMenuHolder)) {
            return;
        }
        event.setCancelled(true);
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

    private static final class ReadyMenuHolder implements InventoryHolder {
        @Override
        public @NotNull Inventory getInventory() {
            throw new UnsupportedOperationException("Not used by this inventory holder.");
        }
    }
}

