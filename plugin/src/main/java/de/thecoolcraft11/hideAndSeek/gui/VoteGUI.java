package de.thecoolcraft11.hideAndSeek.gui;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.gui.config.GUIItems;
import de.thecoolcraft11.hideAndSeek.gui.config.GUINames;
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
            player.sendMessage(plugin.tr(player, "gui.vote.errors.disabled"));
            return;
        }
        if (voteManager.isNotLobbyPhase()) {
            player.sendMessage(plugin.tr(player, "gui.vote.errors.lobby_only"));
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
                .title(plugin.trText(player, "gui.vote.title"))
                .rows(totalRows)
                .allowOutsideClicks(false)
                .allowDrag(false)
                .allowPlayerInventoryInteraction(false)
                .setting("auto_update_animations", true)
                .build();

        Set<UUID> eligibleVoters = voteManager.getOnlineVoterIds();
        int rowOffset = 0;
        if (gamemodeEnabled) {
            rowOffset = addGamemodeRows(inventory, player, selectedGamemode, eligibleVoters, voteManager);
        }
        if (separatorRows == 1 && rowOffset < totalRows) {
            fillSeparatorRow(inventory, rowOffset);
            rowOffset++;
        }
        int mapEndRowExclusive = totalRows - readinessRows - roleRows;
        if (mapEnabled && rowOffset < mapEndRowExclusive) {
            boolean lockMapVotes = gamemodeEnabled && selectedGamemode == null;
            addMapRows(inventory, player, rowOffset, mapEndRowExclusive, displayMaps, selectedMap, selectedGamemode,
                    lockMapVotes, eligibleVoters, voteManager);
        }
        if (rolePreferenceEnabled) {
            int roleRow = totalRows - readinessRows - 1;
            if (roleRow >= 0 && roleRow < totalRows) {
                fillRolePreferenceRow(inventory, player, roleRow, selectedRole, eligibleVoters, voteManager);
            }
        }
        if (readinessRows == 1) {
            fillReadinessRow(inventory, player, totalRows - 1, voteManager);
        }
        plugin.getInventoryFramework().openInventory(player, inventory);
    }

    private int addGamemodeRows(FrameworkInventory inventory, Player player, GameModeEnum selectedGamemode, Set<UUID> eligibleVoters, VoteManager voteManager) {
        Map<GameModeEnum, Long> modeVotes = voteManager.countGamemodeVotes(eligibleVoters);
        int slot = 0;
        for (GameModeEnum mode : GameModeEnum.values()) {
            if (slot >= inventory.getTotalSlots()) {
                break;
            }
            boolean selected = mode == selectedGamemode;
            long votes = modeVotes.getOrDefault(mode, 0L);
            InventoryItem modeItem = new InventoryItem(createGamemodeItem(player, mode, selected, votes));
            final GameModeEnum clickedMode = mode;
            modeItem.setClickHandler((p, item, event, s) -> {
                voteManager.castGamemodeVote(p.getUniqueId(), clickedMode);
                boolean autoReady = voteManager.markReadyIfVoteComplete(p.getUniqueId());
                p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.2f);
                p.sendMessage(plugin.tr(p, "gui.vote.feedback.gamemode_voted",
                        Map.of("mode",
                                plugin.trText(p, "gui.vote.gamemode." + clickedMode.name().toLowerCase(Locale.ROOT)))));
                if (autoReady) {
                    p.sendMessage(plugin.tr(p, "gui.vote.feedback.vote_complete"));
                }
                if (voteManager.tryAutoStartIfEveryoneReady()) {
                    plugin.broadcastTr("gui.vote.feedback.all_ready");
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
            InventoryItem fillerItem = new InventoryItem(item(GUIItems.KEY_SEPERATOR, GUIItems.createSeparatorItem()));
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
            InventoryItem sepItem = new InventoryItem(item(GUIItems.KEY_SEPERATOR, GUIItems.createSeparatorItem()));
            sepItem.setClickHandler((p, item, event, slot) -> event.setCancelled(true));
            sepItem.setAllowTakeout(false);
            sepItem.setAllowInsert(false);
            inventory.setItem(rowStart + i, sepItem);
        }
    }

    private void addMapRows(
            FrameworkInventory inventory,
            Player player,
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
            InventoryItem noMapItem = new InventoryItem(createNoMapsItem(player));
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

            InventoryItem mapItem = new InventoryItem(
                    createMapItem(player, mapName, mapData, selected, votes, lockMapVotes));
            final String clickedMap = mapName;
            mapItem.setClickHandler((p, item, event, s) -> {
                if (voteManager.isGamemodeVotingEnabled() && voteManager.getGamemodeVote(p.getUniqueId()).isEmpty()) {
                    p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    p.sendMessage(plugin.tr(p, "gui.vote.feedback.select_gamemode_first"));
                    event.setCancelled(true);
                    return;
                }

                GameModeEnum currentModeVote = voteManager.getGamemodeVote(p.getUniqueId()).orElse(null);
                boolean mapStillVisible = plugin.getMapManager().getMapsForVoting().contains(clickedMap);
                boolean mapAllowedForMode = currentModeVote == null || plugin.getMapManager().getAvailableMapsForMode(
                        currentModeVote).contains(clickedMap);
                if (!mapStillVisible || !mapAllowedForMode) {
                    p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
                    p.sendMessage(plugin.tr(p, "gui.vote.feedback.map_unavailable"));
                    open(p);
                    event.setCancelled(true);
                    return;
                }

                voteManager.castMapVote(p.getUniqueId(), clickedMap);
                boolean autoReady = voteManager.markReadyIfVoteComplete(p.getUniqueId());
                p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.1f);
                String clickedMapDisplay = mapData != null ? mapData.getDisplayName() : clickedMap;
                p.sendMessage(plugin.tr(p, "gui.vote.feedback.map_voted",
                        Map.of("map", clickedMapDisplay)));
                if (autoReady) {
                    p.sendMessage(plugin.tr(p, "gui.vote.feedback.vote_complete"));
                }
                if (voteManager.tryAutoStartIfEveryoneReady()) {
                    plugin.broadcastTr("gui.vote.feedback.all_ready");
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
            InventoryItem sepItem = new InventoryItem(item(GUIItems.KEY_SEPERATOR, GUIItems.createSeparatorItem()));
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

        InventoryItem toggleItem = new InventoryItem(createReadyToggleItem(player, ready, voteComplete));
        toggleItem.setClickHandler((p, item, event, slot) -> {
            boolean newReady = voteManager.toggleReady(p.getUniqueId());
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, newReady ? 1.2f : 0.9f);
            p.sendMessage(plugin.tr(p, "gui.vote.feedback.ready_status",
                    Map.of("state", plugin.trText(p, newReady ? "common.state.ready" : "common.state.not_ready"))));
            if (voteManager.tryAutoStartIfEveryoneReady()) {
                plugin.broadcastTr("gui.vote.feedback.all_ready");
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
            Player player,
            int row,
            PreferredRole selectedRole,
            Set<UUID> eligibleVoters,
            VoteManager voteManager
    ) {
        int rowStart = row * 9;
        for (int i = 0; i < 9; i++) {
            InventoryItem sepItem = new InventoryItem(item(GUIItems.KEY_SEPERATOR, GUIItems.createSeparatorItem()));
            sepItem.setClickHandler((p, item, event, slot) -> event.setCancelled(true));
            sepItem.setAllowTakeout(false);
            sepItem.setAllowInsert(false);
            inventory.setItem(rowStart + i, sepItem);
        }

        Map<PreferredRole, Long> roleVotes = voteManager.countRolePreferenceVotes(eligibleVoters);
        int hiderSlot = rowStart + 3;
        int seekerSlot = rowStart + 5;

        InventoryItem hiderItem = new InventoryItem(
                createRolePreferenceItem(player, PreferredRole.HIDER, selectedRole == PreferredRole.HIDER,
                        roleVotes.getOrDefault(PreferredRole.HIDER, 0L)));
        hiderItem.setClickHandler((p, item, event, slot) -> {
            voteManager.castRolePreferenceVote(p.getUniqueId(), PreferredRole.HIDER);
            boolean autoReady = voteManager.markReadyIfVoteComplete(p.getUniqueId());
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            p.sendMessage(plugin.tr(p, "gui.vote.feedback.preferred_role",
                    Map.of("role", plugin.trText(p, "gui.vote.role.hider"))));
            if (autoReady) {
                p.sendMessage(plugin.tr(p, "gui.vote.feedback.vote_complete"));
            }
            if (voteManager.tryAutoStartIfEveryoneReady()) {
                plugin.broadcastTr("gui.vote.feedback.all_ready");
            }
            open(p);
            event.setCancelled(true);
        });
        hiderItem.setAllowTakeout(false);
        hiderItem.setAllowInsert(false);
        inventory.setItem(hiderSlot, hiderItem);

        InventoryItem seekerItem = new InventoryItem(
                createRolePreferenceItem(player, PreferredRole.SEEKER, selectedRole == PreferredRole.SEEKER,
                        roleVotes.getOrDefault(PreferredRole.SEEKER, 0L)));
        seekerItem.setClickHandler((p, item, event, slot) -> {
            voteManager.castRolePreferenceVote(p.getUniqueId(), PreferredRole.SEEKER);
            boolean autoReady = voteManager.markReadyIfVoteComplete(p.getUniqueId());
            p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, 1.0f);
            p.sendMessage(plugin.tr(p, "gui.vote.feedback.preferred_role",
                    Map.of("role", plugin.trText(p, "gui.vote.role.seeker"))));
            if (autoReady) {
                p.sendMessage(plugin.tr(p, "gui.vote.feedback.vote_complete"));
            }
            if (voteManager.tryAutoStartIfEveryoneReady()) {
                plugin.broadcastTr("gui.vote.feedback.all_ready");
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

    private ItemStack createGamemodeItem(Player player, GameModeEnum mode, boolean selected, long votes) {
        ItemStack item = item(GUIItems.VOTE_MODE_BASE + mode.name().toLowerCase(Locale.ROOT), switch (mode) {
            case NORMAL -> new ItemStack(Material.IRON_SWORD);
            case SMALL -> new ItemStack(Material.IRON_NUGGET);
            case BLOCK -> new ItemStack(Material.COBBLESTONE);
            case SKIN -> new ItemStack(Material.PLAYER_HEAD);
        });
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        meta.displayName(plugin.tr(player, "gui.vote.gamemode." + mode.name().toLowerCase(Locale.ROOT))
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        if (plugin.getVoteManager().showVoteCounts()) {
            lore.add(plugin.tr(player, "gui.vote.item.votes", Map.of("votes", String.valueOf(votes)))
                    .decoration(TextDecoration.ITALIC, false));
        }
        if (selected) {
            lore.add(plugin.tr(player, "gui.vote.item.selected").decoration(TextDecoration.ITALIC, false));
        }
        lore.add(plugin.tr(player, "gui.vote.item.click_to_vote").decoration(TextDecoration.ITALIC, false));
        meta.lore(lore);
        item.setItemMeta(meta);
        applySelectionGlow(item, selected);
        return item;
    }


    private ItemStack item(String key, ItemStack itemStack) {
        return plugin.getGuiItemRegistry().getOrDefault(GUINames.VOTE, key, itemStack);
    }

    private ItemStack createMapItem(Player player, String mapName, MapData mapData, boolean selected, long votes, boolean lockedNoGamemode) {
        ItemStack item = lockedNoGamemode
                ? item(GUIItems.VOTE_MAP_LOCKED, new ItemStack(Material.DIRT_PATH))
                : plugin.getMapManager().getMapIcon(mapName,
                item(GUIItems.VOTE_MAP_DEFAULT, new ItemStack(Material.GRASS_BLOCK)));
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }
        NamedTextColor nameColor = selected ? NamedTextColor.GREEN : NamedTextColor.AQUA;
        String displayName = mapData != null ? mapData.getDisplayName() : mapName;
        meta.displayName(Component.text(displayName, nameColor, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        if (lockedNoGamemode) {
            lore.add(plugin.tr(player, "gui.vote.item.locked_for_gamemode").decoration(TextDecoration.ITALIC, false));
        } else {
            if (plugin.getVoteManager().showVoteCounts()) {
                lore.add(plugin.tr(player, "gui.vote.item.votes", Map.of("votes", String.valueOf(votes)))
                        .decoration(TextDecoration.ITALIC, false));
            }
            if (mapData != null) {
                if (!mapData.getDescription().isEmpty()) {
                    lore.add(plugin.tr(player, "gui.vote.item.map_description",
                                    Map.of("description", mapData.getDescription()))
                            .decoration(TextDecoration.ITALIC, false));
                }
                if (mapData.getSize() != null && !mapData.getSize().isEmpty()) {
                    lore.add(plugin.tr(player, "gui.vote.item.size",
                                    Map.of("size", mapData.getSize()))
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
                    lore.add(plugin.tr(player, "gui.vote.item.players",
                                    Map.of("players", playerInfo.toString()))
                            .decoration(TextDecoration.ITALIC, false));
                }
            }
            lore.add(plugin.tr(player, "gui.vote.item.click_to_vote").decoration(TextDecoration.ITALIC, false));
        }
        if (selected) {
            lore.add(plugin.tr(player, "gui.vote.item.selected").decoration(TextDecoration.ITALIC, false));
        }
        meta.lore(lore);
        item.setItemMeta(meta);
        applySelectionGlow(item, selected);
        return item;
    }

    private ItemStack createRolePreferenceItem(Player player, PreferredRole role, boolean selected, long votes) {
        ItemStack item = role == PreferredRole.HIDER ? item(GUIItems.VOTE_ROLE_HIDER,
                new ItemStack(Material.LIME_WOOL)) : item(GUIItems.VOTE_ROLE_SEEKER, new ItemStack(Material.RED_WOOL));
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        meta.displayName(plugin.tr(player, "gui.vote.role." + role.name().toLowerCase(Locale.ROOT))
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>();
        if (plugin.getVoteManager().showVoteCounts()) {
            lore.add(plugin.tr(player, "gui.vote.item.votes", Map.of("votes", String.valueOf(votes)))
                    .decoration(TextDecoration.ITALIC, false));
        }
        lore.add(plugin.tr(player, "gui.vote.item.influences_role").decoration(TextDecoration.ITALIC, false));
        if (selected) {
            lore.add(plugin.tr(player, "gui.vote.item.selected").decoration(TextDecoration.ITALIC, false));
        }
        lore.add(plugin.tr(player, "gui.vote.item.click_to_vote").decoration(TextDecoration.ITALIC, false));

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

    private ItemStack createNoMapsItem(Player player) {
        ItemStack item = item(GUIItems.VOTE_NO_MAP, new ItemStack(Material.BARRIER));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(plugin.tr(player, "gui.vote.item.no_maps_available")
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(plugin.tr(player, "gui.vote.item.no_maps_description")
                    .decoration(TextDecoration.ITALIC, false)));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createSelfHeadItem(Player player, boolean ready) {
        ItemStack item = item(GUIItems.VOTE_SELF, new ItemStack(Material.PLAYER_HEAD));
        ItemMeta rawMeta = item.getItemMeta();
        if (!(rawMeta instanceof SkullMeta meta)) {
            return item;
        }

        meta.setOwningPlayer(player);
        meta.displayName(plugin.tr(player, "gui.vote.item.your_readiness")
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));
        meta.lore(List.of(
                plugin.tr(player, "gui.vote.item.status",
                        Map.of("state", plugin.trText(player,
                                ready ? "common.state.ready" : "common.state.not_ready"
                        ))
                )
        ));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createReadyToggleItem(Player player, boolean ready, boolean voteComplete) {
        Material material = ready ? item(GUIItems.KEY_READY,
                new ItemStack(Material.LIME_STAINED_GLASS_PANE)).getType() : item(GUIItems.KEY_NOT_READY,
                new ItemStack(Material.RED_STAINED_GLASS_PANE)).getType();
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return item;
        }

        meta.displayName(plugin.tr(player, ready ? "common.state.ready" : "common.state.not_ready")
                .decoration(TextDecoration.BOLD, true)
                .decoration(TextDecoration.ITALIC, false));
        List<Component> lore = new ArrayList<>();
        lore.add(plugin.tr(player, "gui.vote.item.click_to_toggle").decoration(TextDecoration.ITALIC, false));
        if (!voteComplete && plugin.getVoteManager().isVotingEnabled()) {
            lore.add(plugin.tr(player, "gui.vote.item.voting_not_complete").decoration(TextDecoration.ITALIC, false));
            lore.add(plugin.tr(player, "gui.vote.item.can_still_ready").decoration(TextDecoration.ITALIC, false));
        }
        meta.lore(lore);
        item.setItemMeta(meta);
        applySelectionGlow(item, ready);
        return item;
    }
}
