package de.thecoolcraft11.hideAndSeek.gui;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

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
                .title("Select Map")
                .rows(rows)
                .allowOutsideClicks(false)
                .allowDrag(false)
                .allowPlayerInventoryInteraction(false)
                .build();

        String currentMapName = HideAndSeek.getDataController().getCurrentMapName();

        boolean isRandomSelected = (currentMapName == null || currentMapName.isEmpty());
        ItemStack randomMapItem = createMapMenuItem(isRandomSelected);
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

            ItemStack mapItem = createMapItemWithData(mapName, mapData, isCurrentMap, voteDisabled, canManageMapVoting);
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
            admin.sendMessage(Component.text("You do not have permission to manage map voting.", NamedTextColor.RED));
            admin.playSound(admin.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        boolean nowDisabled = plugin.getMapManager().toggleMapVoteDisabled(mapName);
        MapData mapData = plugin.getMapManager().getMapData(mapName);
        String displayName = mapData != null ? mapData.getDisplayName() : mapName;

        admin.sendMessage(Component.text("Map vote visibility for ", NamedTextColor.YELLOW)
                .append(Component.text(displayName, NamedTextColor.GOLD))
                .append(Component.text(": " + (nowDisabled ? "DISABLED" : "ENABLED"),
                        nowDisabled ? NamedTextColor.RED : NamedTextColor.GREEN)));
        admin.playSound(admin.getLocation(), Sound.UI_BUTTON_CLICK, 1.0f, nowDisabled ? 0.8f : 1.2f);

        if (nowDisabled) {
            Set<UUID> affectedVoterIds = plugin.getVoteManager().clearMapVotesForMap(mapName);
            for (UUID playerId : affectedVoterIds) {
                Player affectedPlayer = Bukkit.getPlayer(playerId);
                if (affectedPlayer == null) {
                    continue;
                }
                affectedPlayer.sendMessage(Component.text("Your vote for ", NamedTextColor.YELLOW)
                        .append(Component.text(displayName, NamedTextColor.GOLD))
                        .append(Component.text(" was reset because this map is now vote-disabled.",
                                NamedTextColor.YELLOW)));
                affectedPlayer.sendMessage(Component.text("Your readiness status was reset.", NamedTextColor.RED));
            }
            if (!affectedVoterIds.isEmpty()) {
                admin.sendMessage(Component.text("Reset votes/readiness for " + affectedVoterIds.size() + " player(s).",
                        NamedTextColor.YELLOW));
            }
        }

        open(admin);
    }


    private void selectRandomMap(Player player) {
        HideAndSeek.getDataController().setCurrentMapName(null, false);

        player.sendMessage(Component.text("Map selection: ", NamedTextColor.GREEN)
                .append(Component.text("Random", NamedTextColor.GOLD)));
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
            player.sendMessage(Component.text("Map '" + mapName + "' not found or not loaded!", NamedTextColor.RED));
            player.playSound(player.getLocation(), Sound.ENTITY_VILLAGER_NO, 1.0f, 1.0f);
            return;
        }

        HideAndSeek.getDataController().setCurrentMapName(mapName, true);

        MapData mapData = plugin.getMapManager().getMapData(mapName);
        String displayName = (mapData != null) ? mapData.getDisplayName() : mapName;

        player.sendMessage(Component.text("Map selected: ", NamedTextColor.GREEN)
                .append(Component.text(displayName, NamedTextColor.GOLD)));
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
        player.closeInventory();

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                open(player);
            }
        }, 1L);
    }


    private ItemStack createMapMenuItem(boolean highlight) {
        ItemStack item = new ItemStack(Material.COMPASS);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            NamedTextColor nameColor = highlight ? NamedTextColor.GREEN : NamedTextColor.AQUA;

            meta.displayName(Component.text("Random Map", nameColor, TextDecoration.BOLD)
                    .decoration(TextDecoration.ITALIC, false));

            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Randomly select a map", NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));

            if (highlight) {
                lore.add(Component.text("Selected", NamedTextColor.GREEN)
                        .decoration(TextDecoration.ITALIC, false));
            }

            meta.lore(lore);
            meta.setEnchantmentGlintOverride(highlight);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createMapItemWithData(String mapName, MapData mapData, boolean isCurrentMap, boolean voteDisabled, boolean canManageMapVoting) {
        ItemStack item = new ItemStack(plugin.getMapManager().getMapIconMaterial(mapName, Material.GRASS_BLOCK));
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            NamedTextColor nameColor = isCurrentMap ? NamedTextColor.GREEN : NamedTextColor.AQUA;
            String displayName = mapData != null ? mapData.getDisplayName() : mapName;

            meta.displayName(Component.text(displayName, nameColor, TextDecoration.BOLD)
                    .decoration(TextDecoration.ITALIC, false));

            List<Component> lore = new ArrayList<>();

            if (mapData != null) {

                if (!mapData.getDescription().isEmpty()) {
                    lore.add(Component.text(mapData.getDescription(), NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false));
                    lore.add(Component.empty());
                }


                if (mapData.getAuthor() != null && !mapData.getAuthor().isEmpty()) {
                    lore.add(Component.text("By: ", NamedTextColor.WHITE)
                            .append(Component.text(mapData.getAuthor(), NamedTextColor.YELLOW))
                            .decoration(TextDecoration.ITALIC, false));
                }


                if (mapData.getSize() != null && !mapData.getSize().isEmpty()) {
                    lore.add(Component.text("Size: ", NamedTextColor.WHITE)
                            .append(Component.text(mapData.getSize(), NamedTextColor.YELLOW))
                            .decoration(TextDecoration.ITALIC, false));
                }

                int spawnCount = mapData.getSpawnPoints().size();
                lore.add(Component.text("Spawns: " + spawnCount, NamedTextColor.YELLOW)
                        .decoration(TextDecoration.ITALIC, false));


                List<GameModeEnum> preferredModes = mapData.getPreferredModes();
                if (!preferredModes.isEmpty()) {
                    StringBuilder modesStr = new StringBuilder();
                    for (int i = 0; i < preferredModes.size(); i++) {
                        if (i > 0) modesStr.append(", ");
                        modesStr.append(preferredModes.get(i).name());
                    }
                    lore.add(Component.text("Modes: " + modesStr, NamedTextColor.YELLOW)
                            .decoration(TextDecoration.ITALIC, false));
                }

                lore.add(Component.empty());


                if (mapData.getMinPlayers() != null || mapData.getRecommendedPlayers() != null || mapData.getMaxPlayers() != null) {
                    StringBuilder playerStr = new StringBuilder("Players: ");
                    if (mapData.getMinPlayers() != null) {
                        playerStr.append(mapData.getMinPlayers()).append("-");
                    }
                    if (mapData.getMaxPlayers() != null) {
                        playerStr.append(mapData.getMaxPlayers());
                    }
                    if (mapData.getRecommendedPlayers() != null) {
                        playerStr.append(" (").append(mapData.getRecommendedPlayers()).append(" recommended)");
                    }
                    lore.add(Component.text(playerStr.toString(), NamedTextColor.LIGHT_PURPLE)
                            .decoration(TextDecoration.ITALIC, false));
                }


                if (mapData.getMinSeekers() != null || mapData.getMaxSeekers() != null) {
                    StringBuilder seekerStr = new StringBuilder("Seekers: ");
                    if (mapData.getMinSeekers() != null) {
                        seekerStr.append(mapData.getMinSeekers()).append("-");
                    }
                    if (mapData.getMaxSeekers() != null) {
                        seekerStr.append(mapData.getMaxSeekers());
                    }
                    if (mapData.getSeekersPerPlayers() != null && mapData.getSeekersPerPlayers() > 0) {
                        seekerStr.append(" (1 per ").append(mapData.getSeekersPerPlayers()).append(" players)");
                    }
                    lore.add(Component.text(seekerStr.toString(), NamedTextColor.LIGHT_PURPLE)
                            .decoration(TextDecoration.ITALIC, false));
                }


                if (mapData.getHidingTime() != null || mapData.getSeekingTime() != null) {
                    StringBuilder timingStr = new StringBuilder("Time: ");
                    if (mapData.getHidingTime() != null) {
                        timingStr.append(mapData.getHidingTime()).append("s hiding");
                    }
                    if (mapData.getSeekingTime() != null) {
                        if (mapData.getHidingTime() != null) timingStr.append(", ");
                        timingStr.append(mapData.getSeekingTime()).append("s seeking");
                    }
                    lore.add(Component.text(timingStr.toString(), NamedTextColor.LIGHT_PURPLE)
                            .decoration(TextDecoration.ITALIC, false));
                }
            }

            if (isCurrentMap) {
                lore.add(Component.text("Selected", NamedTextColor.GREEN)
                        .decoration(TextDecoration.ITALIC, false));
            }

            lore.add(Component.text("Vote visibility: " + (voteDisabled ? "DISABLED" : "ENABLED"),
                            voteDisabled ? NamedTextColor.RED : NamedTextColor.GREEN)
                    .decoration(TextDecoration.ITALIC, false));
            if (canManageMapVoting) {
                lore.add(Component.text("Right click: toggle vote visibility", NamedTextColor.YELLOW)
                        .decoration(TextDecoration.ITALIC, false));
            }

            lore.add(Component.empty());
            lore.add(Component.text("ID: " + mapName, NamedTextColor.DARK_GRAY)
                    .decoration(TextDecoration.ITALIC, false));

            meta.lore(lore);
            meta.setEnchantmentGlintOverride(isCurrentMap);
            item.setItemMeta(meta);
        }
        return item;
    }
}
