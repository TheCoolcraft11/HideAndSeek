package de.thecoolcraft11.hideAndSeek.gui;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.model.GameModeEnum;
import de.thecoolcraft11.hideAndSeek.util.map.MapData;
import de.thecoolcraft11.hideAndSeek.vote.VoteManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class VoteGUI implements Listener {
    private static final Component TITLE = Component.text("Vote", NamedTextColor.GOLD);
    private final HideAndSeek plugin;

    public VoteGUI(HideAndSeek plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        VoteManager voteManager = plugin.getVoteManager();
        if (!voteManager.isVotingEnabled()) {
            player.sendMessage(Component.text("Voting is disabled.", NamedTextColor.RED));
            return;
        }
        if (voteManager.isNotLobbyPhase()) {
            player.sendMessage(Component.text("Voting is only available in the lobby.", NamedTextColor.RED));
            return;
        }
        boolean gamemodeEnabled = voteManager.isGamemodeVotingEnabled();
        boolean mapEnabled = voteManager.isMapVotingEnabled();
        GameModeEnum selectedGamemode = voteManager.getGamemodeVote(player.getUniqueId()).orElse(null);
        String selectedMap = voteManager.getMapVote(player.getUniqueId()).orElse(null);
        List<String> allMaps = plugin.getMapManager().getMapsForVoting();
        List<String> displayMaps = getDisplayMaps(gamemodeEnabled, selectedGamemode, allMaps);
        int gamemodeRows = gamemodeEnabled ? getRows(GameModeEnum.values().length) : 0;
        int separatorRows = gamemodeEnabled && mapEnabled ? 1 : 0;
        int readinessRows = voteManager.isReadinessEnabled() ? 1 : 0;
        int mapRows = 0;
        if (mapEnabled) {
            int requestedRows = getRows(Math.max(displayMaps.size(), 1));
            int maxMapRows = Math.max(1, 6 - gamemodeRows - separatorRows - readinessRows);
            mapRows = Math.min(requestedRows, maxMapRows);
        }
        int totalRows = Math.max(1, Math.min(6, gamemodeRows + separatorRows + mapRows + readinessRows));
        VoteMenuHolder holder = new VoteMenuHolder();
        Inventory inventory = Bukkit.createInventory(holder, totalRows * 9, TITLE);
        holder.inventory = inventory;
        Set<UUID> eligibleVoters = voteManager.getOnlineVoterIds();
        int rowOffset = 0;
        if (gamemodeEnabled) {
            rowOffset = addGamemodeRows(inventory, holder, selectedGamemode, eligibleVoters);
        }
        if (separatorRows == 1 && rowOffset < totalRows) {
            fillSeparatorRow(inventory, rowOffset);
            rowOffset++;
        }
        int mapEndRowExclusive = totalRows - readinessRows;
        if (mapEnabled && rowOffset < mapEndRowExclusive) {
            boolean lockMapVotes = gamemodeEnabled && selectedGamemode == null;
            addMapRows(inventory, holder, rowOffset, mapEndRowExclusive, displayMaps, selectedMap, selectedGamemode, lockMapVotes, eligibleVoters);
        }
        if (readinessRows == 1) {
            fillReadinessRow(inventory, holder, player, totalRows - 1, voteManager);
        }
        player.openInventory(inventory);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (!(event.getInventory().getHolder() instanceof VoteMenuHolder holder)) {
            return;
        }
        event.setCancelled(true);
        if (event.getRawSlot() < 0 || event.getRawSlot() >= event.getInventory().getSize()) {
            return;
        }
        if (event.getClick() != ClickType.LEFT) {
            return;
        }
        VoteManager voteManager = plugin.getVoteManager();
        if (voteManager.isNotLobbyPhase()) {
            player.sendMessage(Component.text("Voting is only available in the lobby.", NamedTextColor.RED));
            player.closeInventory();
            return;
        }
        int slot = event.getRawSlot();
        if (slot == holder.readyToggleSlot && voteManager.isReadinessEnabled()) {
            boolean ready = voteManager.toggleReady(player.getUniqueId());
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, ready ? 1.2f : 0.9f);
            player.sendMessage(Component.text("Ready status: ", NamedTextColor.GRAY)
                    .append(Component.text(ready ? "READY" : "NOT READY", ready ? NamedTextColor.GREEN : NamedTextColor.RED)));
            if (voteManager.tryAutoStartIfEveryoneReady()) {
                Bukkit.broadcast(Component.text("All players are ready. Starting the round!", NamedTextColor.GREEN));
            }
            open(player);
            return;
        }
        GameModeEnum clickedMode = holder.gamemodeSlots.get(slot);
        if (clickedMode != null && voteManager.isGamemodeVotingEnabled()) {
            voteManager.castGamemodeVote(player.getUniqueId(), clickedMode);
            boolean autoReady = voteManager.markReadyIfVoteComplete(player.getUniqueId());
            player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);
            player.sendMessage(Component.text("Voted gamemode: ", NamedTextColor.GREEN)
                    .append(Component.text(clickedMode.name(), NamedTextColor.GOLD)));
            if (autoReady) {
                player.sendMessage(Component.text("Vote complete. You are now ready.", NamedTextColor.GREEN));
            }
            if (voteManager.tryAutoStartIfEveryoneReady()) {
                Bukkit.broadcast(Component.text("All players are ready. Starting the round!", NamedTextColor.GREEN));
            }
            open(player);
            return;
        }
        String clickedMap = holder.mapSlots.get(slot);
        if (clickedMap == null || !voteManager.isMapVotingEnabled()) {
            return;
        }
        if (voteManager.isGamemodeVotingEnabled() && voteManager.getGamemodeVote(player.getUniqueId()).isEmpty()) {
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            player.sendMessage(Component.text("Select a gamemode first to vote for maps.", NamedTextColor.RED));
            return;
        }
        voteManager.castMapVote(player.getUniqueId(), clickedMap);
        boolean autoReady = voteManager.markReadyIfVoteComplete(player.getUniqueId());
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.1f);
        player.sendMessage(Component.text("Voted map: ", NamedTextColor.GREEN)
                .append(Component.text(clickedMap, NamedTextColor.GOLD)));
        if (autoReady) {
            player.sendMessage(Component.text("Vote complete. You are now ready.", NamedTextColor.GREEN));
        }
        if (voteManager.tryAutoStartIfEveryoneReady()) {
            Bukkit.broadcast(Component.text("All players are ready. Starting the round!", NamedTextColor.GREEN));
        }
        open(player);
    }

    private int addGamemodeRows(Inventory inventory, VoteMenuHolder holder, GameModeEnum selectedGamemode, Set<UUID> eligibleVoters) {
        Map<GameModeEnum, Long> modeVotes = plugin.getVoteManager().countGamemodeVotes(eligibleVoters);
        int slot = 0;
        for (GameModeEnum mode : GameModeEnum.values()) {
            if (slot >= inventory.getSize()) {
                break;
            }
            boolean selected = mode == selectedGamemode;
            long votes = modeVotes.getOrDefault(mode, 0L);
            inventory.setItem(slot, createGamemodeItem(mode, selected, votes));
            holder.gamemodeSlots.put(slot, mode);
            slot++;
        }
        return getRows(GameModeEnum.values().length);
    }

    private void fillSeparatorRow(Inventory inventory, int row) {
        int rowStart = row * 9;
        for (int i = 0; i < 9; i++) {
            inventory.setItem(rowStart + i, createSeparatorItem());
        }
    }

    private void addMapRows(
            Inventory inventory,
            VoteMenuHolder holder,
            int rowOffset,
            int endRowExclusive,
            List<String> displayMaps,
            String selectedMap,
            GameModeEnum selectedGamemode,
            boolean lockMapVotes,
            Set<UUID> eligibleVoters
    ) {
        Collection<String> eligibleMaps = selectedGamemode == null
                ? plugin.getMapManager().getMapsForVoting()
                : plugin.getMapManager().getAvailableMapsForMode(selectedGamemode);
        Map<String, Long> mapVotes = plugin.getVoteManager().countMapVotes(eligibleVoters, eligibleMaps);
        int startSlot = rowOffset * 9;
        int maxSlots = (endRowExclusive - rowOffset) * 9;
        if (displayMaps.isEmpty()) {
            inventory.setItem(startSlot, createNoMapsItem());
            return;
        }
        for (int i = 0; i < displayMaps.size() && i < maxSlots; i++) {
            int slot = startSlot + i;
            String mapName = displayMaps.get(i);
            boolean selected = mapName.equals(selectedMap);
            long votes = mapVotes.getOrDefault(mapName, 0L);
            MapData mapData = plugin.getMapManager().getMapData(mapName);
            inventory.setItem(slot, createMapItem(mapName, mapData, selected, votes, lockMapVotes));
            holder.mapSlots.put(slot, mapName);
        }
    }

    private void fillReadinessRow(Inventory inventory, VoteMenuHolder holder, Player player, int row, VoteManager voteManager) {
        int rowStart = row * 9;
        for (int i = 0; i < 9; i++) {
            inventory.setItem(rowStart + i, createSeparatorItem());
        }

        int headSlot = rowStart + 7;
        int toggleSlot = rowStart + 8;
        boolean ready = voteManager.isReady(player.getUniqueId());
        boolean voteComplete = voteManager.hasCompletedVote(player.getUniqueId());

        inventory.setItem(headSlot, createSelfHeadItem(player, ready));
        inventory.setItem(toggleSlot, createReadyToggleItem(ready, voteComplete));
        holder.readyToggleSlot = toggleSlot;
    }

    private List<String> getDisplayMaps(boolean gamemodeEnabled, GameModeEnum selectedGamemode, List<String> allMaps) {
        if (!gamemodeEnabled) {
            return allMaps;
        }
        if (selectedGamemode == null) {
            return allMaps;
        }
        return plugin.getMapManager().getAvailableMapsForMode(selectedGamemode);
    }

    private int getRows(int amount) {
        return Math.max(1, (amount + 8) / 9);
    }

    private ItemStack createGamemodeItem(GameModeEnum mode, boolean selected, long votes) {
        Material icon = switch (mode) {
            case NORMAL -> Material.IRON_SWORD;
            case SMALL -> Material.IRON_NUGGET;
            case BLOCK -> Material.COBBLESTONE;
        };
        ItemStack item = new ItemStack(icon);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        NamedTextColor nameColor = selected ? NamedTextColor.GREEN : NamedTextColor.AQUA;
        meta.displayName(Component.text(mode.name(), nameColor, TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        if (plugin.getVoteManager().showVoteCounts()) {
            lore.add(Component.text("Votes: " + votes, NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
        }
        if (selected) {
            lore.add(Component.text("Selected", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
        }
        lore.add(Component.text("Click to vote", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        item.setItemMeta(meta);
        applySelectionGlow(item, selected);
        return item;
    }

    private ItemStack createSeparatorItem() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(" ", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createMapItem(String mapName, MapData mapData, boolean selected, long votes, boolean lockedNoGamemode) {
        ItemStack item = new ItemStack(lockedNoGamemode ? Material.DIRT_PATH : Material.GRASS_BLOCK);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        NamedTextColor nameColor = selected ? NamedTextColor.GREEN : NamedTextColor.AQUA;
        meta.displayName(Component.text(mapName, nameColor, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        if (lockedNoGamemode) {
            lore.add(Component.text("Select a gamemode first to vote for maps.", NamedTextColor.RED)
                    .decoration(TextDecoration.ITALIC, false));
        } else {
            if (plugin.getVoteManager().showVoteCounts()) {
                lore.add(Component.text("Votes: " + votes, NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
            }
            if (mapData != null) {
                if (!mapData.getDescription().isEmpty()) {
                    lore.add(Component.text(mapData.getDescription(), NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
                }
                Integer minPlayers = mapData.getMinPlayers();
                Integer recommendedPlayers = mapData.getRecommendedPlayers();
                Integer maxPlayers = mapData.getMaxPlayers();
                if (minPlayers != null || recommendedPlayers != null || maxPlayers != null) {
                    StringBuilder playerInfo = new StringBuilder("Players: ");
                    if (minPlayers != null) {
                        playerInfo.append(minPlayers);
                    }
                    if (maxPlayers != null) {
                        if (minPlayers != null) {
                            playerInfo.append("-");
                        }
                        playerInfo.append(maxPlayers);
                    }
                    if (recommendedPlayers != null) {
                        playerInfo.append(" (recommended: ").append(recommendedPlayers).append(")");
                    }
                    lore.add(Component.text(playerInfo.toString(), NamedTextColor.LIGHT_PURPLE)
                            .decoration(TextDecoration.ITALIC, false));
                }
            }
            lore.add(Component.text("Click to vote", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        }
        if (selected) {
            lore.add(Component.text("Selected", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
        }
        meta.lore(lore);
        item.setItemMeta(meta);
        applySelectionGlow(item, selected);
        return item;
    }

    private void applySelectionGlow(ItemStack item, boolean selected) {
        if (!selected) {
            return;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return;
        }
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        item.setItemMeta(meta);
    }

    private ItemStack createNoMapsItem() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("No maps available :(", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(Component.text("There are no maps configured for this mode.", NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false)));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createSelfHeadItem(Player player, boolean ready) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta rawMeta = item.getItemMeta();
        if (!(rawMeta instanceof SkullMeta meta)) {
            return item;
        }

        meta.setOwningPlayer(player);
        meta.displayName(Component.text("Your Readiness", NamedTextColor.AQUA, TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                Component.text("Status: ", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
                        .append(Component.text(ready ? "READY" : "NOT READY", ready ? NamedTextColor.GREEN : NamedTextColor.RED))
        ));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createReadyToggleItem(boolean ready, boolean voteComplete) {
        Material material = ready ? Material.LIME_STAINED_GLASS_PANE : Material.RED_STAINED_GLASS_PANE;
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        meta.displayName(Component.text(ready ? "Ready" : "Not Ready", ready ? NamedTextColor.GREEN : NamedTextColor.RED, TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("Click to toggle", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        if (!voteComplete && plugin.getVoteManager().isVotingEnabled()) {
            lore.add(Component.text("Voting is not complete yet.", NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("You can still ready manually.", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        }
        meta.lore(lore);
        item.setItemMeta(meta);
        applySelectionGlow(item, ready);
        return item;
    }

    private static final class VoteMenuHolder implements InventoryHolder {
        private final Map<Integer, GameModeEnum> gamemodeSlots = new HashMap<>();
        private final Map<Integer, String> mapSlots = new HashMap<>();
        private int readyToggleSlot = -1;
        private Inventory inventory;

        @Override
        public @NotNull Inventory getInventory() {
            return inventory;
        }
    }
}
