package de.thecoolcraft11.hideAndSeek.gui;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.model.GameModeEnum;
import de.thecoolcraft11.hideAndSeek.util.map.MapData;
import de.thecoolcraft11.hideAndSeek.vote.PreferredRole;
import de.thecoolcraft11.hideAndSeek.vote.VoteManager;
import de.thecoolcraft11.minigameframework.inventory.FrameworkInventory;
import de.thecoolcraft11.minigameframework.inventory.InventoryBuilder;
import de.thecoolcraft11.minigameframework.inventory.InventoryItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.*;


public class VoteGUI {
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
        boolean rolePreferenceEnabled = voteManager.isRolePreferenceVotingEnabled();
        GameModeEnum selectedGamemode = voteManager.getGamemodeVote(player.getUniqueId()).orElse(null);
        String selectedMap = voteManager.getMapVote(player.getUniqueId()).orElse(null);
        PreferredRole selectedRole = voteManager.getPreferredRoleVote(player.getUniqueId()).orElse(null);
        List<String> allMaps = plugin.getMapManager().getMapsForVoting();
        List<String> displayMaps = getDisplayMaps(gamemodeEnabled, selectedGamemode, allMaps);
        int gamemodeRows = gamemodeEnabled ? getRows(GameModeEnum.values().length) : 0;
        int separatorRows = gamemodeEnabled && mapEnabled ? 1 : 0;
        int roleRows = rolePreferenceEnabled ? 1 : 0;
        int readinessRows = voteManager.isReadinessEnabled() ? 1 : 0;
        int mapRows = 0;
        if (mapEnabled) {
            int requestedRows = getRows(Math.max(displayMaps.size(), 1));
            int maxMapRows = Math.max(1, 6 - gamemodeRows - separatorRows - roleRows - readinessRows);
            mapRows = Math.min(requestedRows, maxMapRows);
        }
        int totalRows = Math.clamp(gamemodeRows + separatorRows + mapRows + roleRows + readinessRows, 1, 6);

        FrameworkInventory inventory = new InventoryBuilder(plugin.getInventoryFramework())
                .id("vote_menu_" + player.getUniqueId() + "_" + System.currentTimeMillis())
                .title("Vote")
                .rows(totalRows)
                .allowOutsideClicks(false)
                .allowDrag(false)
                .allowPlayerInventoryInteraction(false)
                .setting("auto_update_animations", true)
                .build();

        Set<UUID> eligibleVoters = voteManager.getOnlineVoterIds();
        int rowOffset = 0;
        if (gamemodeEnabled) {
            rowOffset = addGamemodeRows(inventory, selectedGamemode, eligibleVoters, voteManager);
        }
        if (separatorRows == 1 && rowOffset < totalRows) {
            fillSeparatorRow(inventory, rowOffset);
            rowOffset++;
        }
        int mapEndRowExclusive = totalRows - readinessRows - roleRows;
        if (mapEnabled && rowOffset < mapEndRowExclusive) {
            boolean lockMapVotes = gamemodeEnabled && selectedGamemode == null;
            addMapRows(inventory, rowOffset, mapEndRowExclusive, displayMaps, selectedMap, selectedGamemode, lockMapVotes, eligibleVoters, voteManager);
        }
        if (rolePreferenceEnabled) {
            int roleRow = totalRows - readinessRows - 1;
            if (roleRow >= 0 && roleRow < totalRows) {
                fillRolePreferenceRow(inventory, roleRow, selectedRole, eligibleVoters, voteManager);
            }
        }
        if (readinessRows == 1) {
            fillReadinessRow(inventory, player, totalRows - 1, voteManager);
        }
        plugin.getInventoryFramework().openInventory(player, inventory);
    }

    private int addGamemodeRows(FrameworkInventory inventory, GameModeEnum selectedGamemode, Set<UUID> eligibleVoters, VoteManager voteManager) {
        Map<GameModeEnum, Long> modeVotes = voteManager.countGamemodeVotes(eligibleVoters);
        int slot = 0;
        for (GameModeEnum mode : GameModeEnum.values()) {
            if (slot >= inventory.getTotalSlots()) {
                break;
            }
            boolean selected = mode == selectedGamemode;
            long votes = modeVotes.getOrDefault(mode, 0L);
            InventoryItem modeItem = new InventoryItem(createGamemodeItem(mode, selected, votes));
            final GameModeEnum clickedMode = mode;
            modeItem.setClickHandler((p, item, event, s) -> {
                voteManager.castGamemodeVote(p.getUniqueId(), clickedMode);
                boolean autoReady = voteManager.markReadyIfVoteComplete(p.getUniqueId());
                p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);
                p.sendMessage(Component.text("Voted gamemode: ", NamedTextColor.GREEN)
                        .append(Component.text(clickedMode.name(), NamedTextColor.GOLD)));
                if (autoReady) {
                    p.sendMessage(Component.text("Vote complete. You are now ready.", NamedTextColor.GREEN));
                }
                if (voteManager.tryAutoStartIfEveryoneReady()) {
                    Bukkit.broadcast(Component.text("All players are ready. Starting the round!", NamedTextColor.GREEN));
                }
                open(p);
                event.setCancelled(true);
            });
            modeItem.setAllowTakeout(false);
            modeItem.setAllowInsert(false);
            modeItem.setMetadata("vote_type", "gamemode");
            modeItem.setMetadata("mode", mode.name());
            inventory.setItem(slot, modeItem);
            slot++;
        }


        int gamemodeRows = getRows(GameModeEnum.values().length);
        int gamemodeEndSlot = gamemodeRows * 9;
        while (slot < gamemodeEndSlot) {
            InventoryItem fillerItem = new InventoryItem(createSeparatorItem());
            fillerItem.setClickHandler((p, item, event, s) -> event.setCancelled(true));
            fillerItem.setAllowTakeout(false);
            fillerItem.setAllowInsert(false);
            inventory.setItem(slot, fillerItem);
            slot++;
        }

        return gamemodeRows;
    }

    private void fillSeparatorRow(FrameworkInventory inventory, int row) {
        int rowStart = row * 9;
        for (int i = 0; i < 9; i++) {
            InventoryItem sepItem = new InventoryItem(createSeparatorItem());
            sepItem.setClickHandler((p, item, event, slot) -> event.setCancelled(true));
            sepItem.setAllowTakeout(false);
            sepItem.setAllowInsert(false);
            inventory.setItem(rowStart + i, sepItem);
        }
    }

    private void addMapRows(
            FrameworkInventory inventory,
            int rowOffset,
            int endRowExclusive,
            List<String> displayMaps,
            String selectedMap,
            GameModeEnum selectedGamemode,
            boolean lockMapVotes,
            Set<UUID> eligibleVoters,
            VoteManager voteManager
    ) {
        Collection<String> eligibleMaps = selectedGamemode == null
                ? plugin.getMapManager().getMapsForVoting()
                : plugin.getMapManager().getAvailableMapsForMode(selectedGamemode);
        Map<String, Long> mapVotes = voteManager.countMapVotes(eligibleVoters, eligibleMaps);
        int startSlot = rowOffset * 9;
        int maxSlots = (endRowExclusive - rowOffset) * 9;
        if (displayMaps.isEmpty()) {
            InventoryItem noMapItem = new InventoryItem(createNoMapsItem());
            noMapItem.setClickHandler((p, item, event, slot) -> event.setCancelled(true));
            noMapItem.setAllowTakeout(false);
            noMapItem.setAllowInsert(false);
            inventory.setItem(startSlot, noMapItem);
            return;
        }
        for (int i = 0; i < displayMaps.size() && i < maxSlots; i++) {
            int slot = startSlot + i;
            String mapName = displayMaps.get(i);
            boolean selected = mapName.equals(selectedMap);
            long votes = mapVotes.getOrDefault(mapName, 0L);
            MapData mapData = plugin.getMapManager().getMapData(mapName);

            InventoryItem mapItem = new InventoryItem(createMapItem(mapName, mapData, selected, votes, lockMapVotes));
            final String clickedMap = mapName;
            mapItem.setClickHandler((p, item, event, s) -> {
                if (voteManager.isGamemodeVotingEnabled() && voteManager.getGamemodeVote(p.getUniqueId()).isEmpty()) {
                    p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    p.sendMessage(Component.text("Select a gamemode first to vote for maps.", NamedTextColor.RED));
                    event.setCancelled(true);
                    return;
                }

                GameModeEnum currentModeVote = voteManager.getGamemodeVote(p.getUniqueId()).orElse(null);
                boolean mapStillVisible = plugin.getMapManager().getMapsForVoting().contains(clickedMap);
                boolean mapAllowedForMode = currentModeVote == null || plugin.getMapManager().getAvailableMapsForMode(
                        currentModeVote).contains(clickedMap);
                if (!mapStillVisible || !mapAllowedForMode) {
                    p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    p.sendMessage(Component.text("This map is no longer available for voting.", NamedTextColor.RED));
                    open(p);
                    event.setCancelled(true);
                    return;
                }

                voteManager.castMapVote(p.getUniqueId(), clickedMap);
                boolean autoReady = voteManager.markReadyIfVoteComplete(p.getUniqueId());
                p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.1f);
                String clickedMapDisplay = mapData != null ? mapData.getDisplayName() : clickedMap;
                p.sendMessage(Component.text("Voted map: ", NamedTextColor.GREEN)
                        .append(Component.text(clickedMapDisplay, NamedTextColor.GOLD)));
                if (autoReady) {
                    p.sendMessage(Component.text("Vote complete. You are now ready.", NamedTextColor.GREEN));
                }
                if (voteManager.tryAutoStartIfEveryoneReady()) {
                    Bukkit.broadcast(Component.text("All players are ready. Starting the round!", NamedTextColor.GREEN));
                }
                open(p);
                event.setCancelled(true);
            });
            mapItem.setAllowTakeout(false);
            mapItem.setAllowInsert(false);
            mapItem.setMetadata("vote_type", "map");
            mapItem.setMetadata("map_name", mapName);
            inventory.setItem(slot, mapItem);
        }
    }

    private void fillReadinessRow(FrameworkInventory inventory, Player player, int row, VoteManager voteManager) {
        int rowStart = row * 9;
        for (int i = 0; i < 9; i++) {
            InventoryItem sepItem = new InventoryItem(createSeparatorItem());
            sepItem.setClickHandler((p, item, event, slot) -> event.setCancelled(true));
            sepItem.setAllowTakeout(false);
            sepItem.setAllowInsert(false);
            inventory.setItem(rowStart + i, sepItem);
        }

        int headSlot = rowStart + 7;
        int toggleSlot = rowStart + 8;
        boolean ready = voteManager.isReady(player.getUniqueId());
        boolean voteComplete = voteManager.hasCompletedVote(player.getUniqueId());

        InventoryItem headItem = new InventoryItem(createSelfHeadItem(player, ready));
        headItem.setClickHandler((p, item, event, slot) -> event.setCancelled(true));
        headItem.setAllowTakeout(false);
        headItem.setAllowInsert(false);
        inventory.setItem(headSlot, headItem);

        InventoryItem toggleItem = new InventoryItem(createReadyToggleItem(ready, voteComplete));
        toggleItem.setClickHandler((p, item, event, slot) -> {
            boolean newReady = voteManager.toggleReady(p.getUniqueId());
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, newReady ? 1.2f : 0.9f);
            p.sendMessage(Component.text("Ready status: ", NamedTextColor.GRAY)
                    .append(Component.text(newReady ? "READY" : "NOT READY", newReady ? NamedTextColor.GREEN : NamedTextColor.RED)));
            if (voteManager.tryAutoStartIfEveryoneReady()) {
                Bukkit.broadcast(Component.text("All players are ready. Starting the round!", NamedTextColor.GREEN));
            }
            open(p);
            event.setCancelled(true);
        });
        toggleItem.setAllowTakeout(false);
        toggleItem.setAllowInsert(false);
        inventory.setItem(toggleSlot, toggleItem);
    }

    private void fillRolePreferenceRow(
            FrameworkInventory inventory,
            int row,
            PreferredRole selectedRole,
            Set<UUID> eligibleVoters,
            VoteManager voteManager
    ) {
        int rowStart = row * 9;
        for (int i = 0; i < 9; i++) {
            InventoryItem sepItem = new InventoryItem(createSeparatorItem());
            sepItem.setClickHandler((p, item, event, slot) -> event.setCancelled(true));
            sepItem.setAllowTakeout(false);
            sepItem.setAllowInsert(false);
            inventory.setItem(rowStart + i, sepItem);
        }

        Map<PreferredRole, Long> roleVotes = voteManager.countRolePreferenceVotes(eligibleVoters);
        int hiderSlot = rowStart + 3;
        int seekerSlot = rowStart + 5;

        InventoryItem hiderItem = new InventoryItem(createRolePreferenceItem(PreferredRole.HIDER, selectedRole == PreferredRole.HIDER, roleVotes.getOrDefault(PreferredRole.HIDER, 0L)));
        hiderItem.setClickHandler((p, item, event, slot) -> {
            voteManager.castRolePreferenceVote(p.getUniqueId(), PreferredRole.HIDER);
            boolean autoReady = voteManager.markReadyIfVoteComplete(p.getUniqueId());
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            p.sendMessage(Component.text("Preferred role: ", NamedTextColor.GREEN)
                    .append(Component.text("HIDER", NamedTextColor.GOLD)));
            if (autoReady) {
                p.sendMessage(Component.text("Vote complete. You are now ready.", NamedTextColor.GREEN));
            }
            if (voteManager.tryAutoStartIfEveryoneReady()) {
                Bukkit.broadcast(Component.text("All players are ready. Starting the round!", NamedTextColor.GREEN));
            }
            open(p);
            event.setCancelled(true);
        });
        hiderItem.setAllowTakeout(false);
        hiderItem.setAllowInsert(false);
        inventory.setItem(hiderSlot, hiderItem);

        InventoryItem seekerItem = new InventoryItem(createRolePreferenceItem(PreferredRole.SEEKER, selectedRole == PreferredRole.SEEKER, roleVotes.getOrDefault(PreferredRole.SEEKER, 0L)));
        seekerItem.setClickHandler((p, item, event, slot) -> {
            voteManager.castRolePreferenceVote(p.getUniqueId(), PreferredRole.SEEKER);
            boolean autoReady = voteManager.markReadyIfVoteComplete(p.getUniqueId());
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            p.sendMessage(Component.text("Preferred role: ", NamedTextColor.GREEN)
                    .append(Component.text("SEEKER", NamedTextColor.GOLD)));
            if (autoReady) {
                p.sendMessage(Component.text("Vote complete. You are now ready.", NamedTextColor.GREEN));
            }
            if (voteManager.tryAutoStartIfEveryoneReady()) {
                Bukkit.broadcast(Component.text("All players are ready. Starting the round!", NamedTextColor.GREEN));
            }
            open(p);
            event.setCancelled(true);
        });
        seekerItem.setAllowTakeout(false);
        seekerItem.setAllowInsert(false);
        inventory.setItem(seekerSlot, seekerItem);
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
        Material icon = lockedNoGamemode
                ? Material.DIRT_PATH
                : plugin.getMapManager().getMapIconMaterial(mapName, Material.GRASS_BLOCK);
        ItemStack item = new ItemStack(icon);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        NamedTextColor nameColor = selected ? NamedTextColor.GREEN : NamedTextColor.AQUA;
        String displayName = mapData != null ? mapData.getDisplayName() : mapName;
        meta.displayName(Component.text(displayName, nameColor, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
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
                if (mapData.getSize() != null && !mapData.getSize().isEmpty()) {
                    lore.add(Component.text("Size: " + mapData.getSize(), NamedTextColor.YELLOW)
                            .decoration(TextDecoration.ITALIC, false));
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

    private ItemStack createRolePreferenceItem(PreferredRole role, boolean selected, long votes) {
        Material icon = role == PreferredRole.HIDER ? Material.LIME_WOOL : Material.RED_WOOL;
        ItemStack item = new ItemStack(icon);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        NamedTextColor nameColor = role == PreferredRole.HIDER ? NamedTextColor.GREEN : NamedTextColor.RED;
        meta.displayName(Component.text(role.displayName(), nameColor, TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        if (plugin.getVoteManager().showVoteCounts()) {
            lore.add(Component.text("Votes: " + votes, NamedTextColor.YELLOW).decoration(TextDecoration.ITALIC, false));
        }
        lore.add(Component.text("Influences the role assignment", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));
        if (selected) {
            lore.add(Component.text("Selected", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
        }
        lore.add(Component.text("Click to vote", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false));

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
}
