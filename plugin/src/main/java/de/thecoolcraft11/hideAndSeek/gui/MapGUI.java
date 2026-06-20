package de.thecoolcraft11.hideAndSeek.gui;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.gui.config.GUIItems;
import de.thecoolcraft11.hideAndSeek.gui.config.GUINames;
import de.thecoolcraft11.hideAndSeek.model.GameModeEnum;
import de.thecoolcraft11.hideAndSeek.util.map.MapData;
import de.thecoolcraft11.minigameframework.inventory.FrameworkInventory;
import de.thecoolcraft11.minigameframework.inventory.InventoryBuilder;
import de.thecoolcraft11.minigameframework.inventory.InventoryItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jspecify.annotations.NonNull;

import java.util.*;

public class MapGUI {
    private static final String ADMIN_MAP_PERMISSION = "hideandseek.command.map";
    private final HideAndSeek plugin;

    public MapGUI(HideAndSeek plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        List<String> availableMaps = plugin.getMapManager().getAvailableMaps();
        int rows = Math.max(3, (availableMaps.size() + 9) / 9);
        boolean canManageMapVoting = player.hasPermission(ADMIN_MAP_PERMISSION);

        FrameworkInventory inventory = new InventoryBuilder(plugin.getInventoryFramework())
                .id("map_selector_" + player.getUniqueId())
                .title(plugin.trText(player, "gui.map_selector.title"))
                .rows(rows)
                .allowOutsideClicks(false)
                .allowDrag(false)
                .allowPlayerInventoryInteraction(false)
                .build();

        String currentMapName = HideAndSeek.getDataController().getCurrentMapName();

        boolean isRandomSelected = (currentMapName == null || currentMapName.isEmpty());
        ItemStack randomMapItem = createMapMenuItem(isRandomSelected, player);
        InventoryItem randomItem = new InventoryItem(randomMapItem);
        randomItem.setClickHandler((p, item, event, slot) -> {
            selectRandomMap(p);
            event.setCancelled(true);
        });
        randomItem.setAllowTakeout(false);
        randomItem.setAllowInsert(false);
        inventory.setItem(0, randomItem);

        int slot = 1;
        for (String mapName : availableMaps) {
            boolean isCurrentMap = mapName.equals(currentMapName);
            boolean voteDisabled = plugin.getMapManager().isMapVoteDisabled(mapName);
            MapData mapData = plugin.getMapManager().getMapData(mapName);

            ItemStack mapItem = createMapItemWithData(mapName, mapData, isCurrentMap, voteDisabled, canManageMapVoting,
                    player);
            InventoryItem mapGuiItem = getInventoryItem(mapName, mapItem,
                    canManageMapVoting);
            inventory.setItem(slot++, mapGuiItem);
        }

        plugin.getInventoryFramework().openInventory(player, inventory);
    }

    private @NonNull InventoryItem getInventoryItem(String mapName, ItemStack mapItem, boolean canManageMapVoting) {
        final String selectedMapName = mapName;
        InventoryItem mapGuiItem = new InventoryItem(mapItem);
        mapGuiItem.setClickHandler((p, item, event, s) -> {
            if (canManageMapVoting && event.getClick() == ClickType.RIGHT) {
                toggleMapVoteDisabled(p, selectedMapName);
                event.setCancelled(true);
                return;
            }
            selectSpecificMap(p, selectedMapName);
            event.setCancelled(true);
        });
        mapGuiItem.setAllowTakeout(false);
        mapGuiItem.setAllowInsert(false);
        return mapGuiItem;
    }

    private void toggleMapVoteDisabled(Player admin, String mapName) {
        if (!admin.hasPermission(ADMIN_MAP_PERMISSION)) {
            admin.sendMessage(plugin.tr(admin, "gui.map_selector.admin.no_permission"));
            admin.playSound(admin.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        boolean nowDisabled = plugin.getMapManager().toggleMapVoteDisabled(mapName);
        MapData mapData = plugin.getMapManager().getMapData(mapName);
        String displayName = mapData != null ? mapData.getDisplayName(plugin, admin) : mapName;

        admin.sendMessage(plugin.tr(admin, "gui.map_selector.admin.toggled",
                Map.of(
                        "name", displayName,
                        "status", nowDisabled ? "DISABLED" : "ENABLED",
                        "color", nowDisabled ? "red" : "green"
                )));
        admin.playSound(admin.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, nowDisabled ? 0.8f : 1.2f);

        if (nowDisabled) {
            Set<UUID> affectedVoterIds = plugin.getVoteManager().clearMapVotesForMap(mapName);
            for (UUID playerId : affectedVoterIds) {
                Player affectedPlayer = Bukkit.getPlayer(playerId);
                if (affectedPlayer == null) {
                    continue;
                }
                affectedPlayer.sendMessage(plugin.tr(affectedPlayer,
                        "gui.map_selector.admin.player_reset_notify",
                        Map.of("name", displayName)));

                affectedPlayer.sendMessage(plugin.tr(affectedPlayer,
                        "gui.map_selector.admin.player_readiness_reset"));
            }
            if (!affectedVoterIds.isEmpty()) {
                admin.sendMessage(plugin.tr(admin,
                        "gui.map_selector.admin.reset_summary",
                        Map.of("count", affectedVoterIds.size())));
            }
        }

        open(admin);
    }


    private void selectRandomMap(Player player) {
        HideAndSeek.getDataController().setCurrentMapName(null, false);

        player.sendMessage(plugin.tr(player, "gui.map_selector.feedback.selection_random"));
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
        player.closeInventory();

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                open(player);
            }
        }, 1L);
    }

    private void selectSpecificMap(Player player, String mapName) {
        org.bukkit.World sourceWorld = Bukkit.getWorld(mapName);
        if (sourceWorld == null) {
            player.sendMessage(plugin.tr(player,
                    "gui.map_selector.feedback.error_not_found",
                    Map.of("map", mapName)));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        HideAndSeek.getDataController().setCurrentMapName(mapName, true);

        MapData mapData = plugin.getMapManager().getMapData(mapName);
        String displayName = (mapData != null) ? mapData.getDisplayName(plugin, player) : mapName;

        player.sendMessage(plugin.tr(player,
                "gui.map_selector.feedback.selection_specific",
                Map.of("name", displayName)));
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
        player.closeInventory();

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                open(player);
            }
        }, 1L);
    }


    private ItemStack createMapMenuItem(boolean highlight, Player player) {
        ItemStack item = item(GUIItems.MAP_RANDOM, new ItemStack(Material.COMPASS));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {

            meta.displayName(plugin.tr(player, "gui.map_selector.random_map.name"));

            List<Component> lore = new ArrayList<>();
            lore.add(plugin.tr(player, "gui.map_selector.random_map.description"));

            if (highlight) {
                lore.add(plugin.tr(player, "gui.map_selector.status.selected"));
            }

            meta.lore(lore);
            meta.setEnchantmentGlintOverride(highlight);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createMapItemWithData(String mapName, MapData mapData, boolean isCurrentMap,
                                            boolean voteDisabled, boolean canManageMapVoting, Player player) {

        ItemStack item = new ItemStack(plugin.getMapManager().getMapIcon(mapName,
                item(GUIItems.KEY_FALLBACK, new ItemStack(Material.GRASS_BLOCK))));
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            NamedTextColor nameColor = isCurrentMap ? NamedTextColor.GREEN : NamedTextColor.AQUA;
            String displayName = mapData != null ? mapData.getDisplayName(plugin, player) : mapName;

            meta.displayName(Component.text(displayName, nameColor, TextDecoration.BOLD)
                    .decoration(TextDecoration.ITALIC, false));

            List<Component> lore = new ArrayList<>();

            if (mapData != null) {


                if (!mapData.getDescription().isEmpty()) {
                    lore.add(plugin.tr(player, "gui.map_selector.data.description",
                            Map.of("description", mapData.getDescription())));
                    lore.add(Component.empty());
                }


                if (mapData.getAuthor() != null && !mapData.getAuthor().isEmpty()) {
                    lore.add(plugin.tr(player, "gui.map_selector.data.by",
                            Map.of("author", mapData.getAuthor())));
                }


                if (mapData.getSize() != null && !mapData.getSize().isEmpty()) {
                    lore.add(plugin.tr(player, "gui.map_selector.data.size",
                            Map.of("size", mapData.getSize())));
                }


                int spawnCount = mapData.getSpawnPoints().size();
                lore.add(plugin.tr(player, "gui.map_selector.data.spawns",
                        Map.of("count", spawnCount)));


                List<GameModeEnum> preferredModes = mapData.getPreferredModes();
                if (!preferredModes.isEmpty()) {
                    String modes = preferredModes.stream()
                            .map(Enum::name)
                            .reduce((a, b) -> a + ", " + b)
                            .orElse("");

                    lore.add(plugin.tr(player, "gui.map_selector.data.modes",
                            Map.of("modes", modes)));
                }

                lore.add(Component.empty());


                if (mapData.getMinPlayers() != null || mapData.getMaxPlayers() != null || mapData.getRecommendedPlayers() != null) {

                    String range = "";
                    if (mapData.getMinPlayers() != null) {
                        range += mapData.getMinPlayers();
                    }
                    if (mapData.getMaxPlayers() != null) {
                        range += "-" + mapData.getMaxPlayers();
                    }

                    String recommended = "";
                    if (mapData.getRecommendedPlayers() != null) {
                        recommended = plugin.trText(player, "gui.map_selector.data.players_recommended",
                                Map.of("count", mapData.getRecommendedPlayers()));
                    }

                    lore.add(plugin.tr(player, "gui.map_selector.data.players",
                            Map.of(
                                    "range", range,
                                    "recommended", recommended
                            )));
                }


                if (mapData.getMinSeekers() != null || mapData.getMaxSeekers() != null) {

                    String range = "";
                    if (mapData.getMinSeekers() != null) {
                        range += mapData.getMinSeekers();
                    }
                    if (mapData.getMaxSeekers() != null) {
                        range += "-" + mapData.getMaxSeekers();
                    }

                    String ratio = "";
                    if (mapData.getSeekersPerPlayers() != null && mapData.getSeekersPerPlayers() > 0) {
                        ratio = plugin.trText(player, "gui.map_selector.data.seekers_ratio",
                                Map.of("value", mapData.getSeekersPerPlayers()));
                    }

                    lore.add(plugin.tr(player, "gui.map_selector.data.seekers",
                            Map.of(
                                    "range", range,
                                    "ratio", ratio
                            )));
                }


                if (mapData.getHidingTime() != null || mapData.getSeekingTime() != null) {

                    String hiding = "";
                    if (mapData.getHidingTime() != null) {
                        hiding = plugin.trText(player, "gui.map_selector.data.time_hiding",
                                Map.of("seconds", mapData.getHidingTime()));
                    }

                    String seeking = "";
                    if (mapData.getSeekingTime() != null) {
                        seeking = plugin.trText(player, "gui.map_selector.data.time_seeking",
                                Map.of("seconds", mapData.getSeekingTime()));
                    }

                    lore.add(plugin.tr(player, "gui.map_selector.data.time",
                            Map.of(
                                    "hiding", hiding,
                                    "seeking", seeking
                            )));
                }
            }


            if (isCurrentMap) {
                lore.add(plugin.tr(player, "gui.map_selector.status.selected"));
            }


            lore.add(plugin.tr(player, "gui.map_selector.status.vote_visibility",
                    Map.of(
                            "status", plugin.trText(player,
                                    voteDisabled ? "common.state.disabled" : "common.state.enabled"),
                            "color", voteDisabled ? "red" : "green"
                    )));


            if (canManageMapVoting) {
                lore.add(plugin.tr(player, "gui.map_selector.status.toggle_hint"));
            }

            lore.add(Component.empty());


            lore.add(plugin.tr(player, "gui.map_selector.data.id",
                    Map.of("id", mapName)));

            meta.lore(lore);
            meta.setEnchantmentGlintOverride(isCurrentMap);
            item.setItemMeta(meta);
        }

        return item;
    }

    private ItemStack item(String key, ItemStack fallback) {
        return plugin.getGuiItemRegistry().getOrDefault(GUINames.READY, key, fallback);
    }
}
