package de.thecoolcraft11.hideAndSeek.gui;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.model.GameModeEnum;
import de.thecoolcraft11.hideAndSeek.util.map.MapData;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class MapGUI implements Listener {
    private final HideAndSeek plugin;

    public MapGUI(HideAndSeek plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        List<String> availableMaps = plugin.getMapManager().getAvailableMaps();
        int rows = Math.max(3, (availableMaps.size() + 9) / 9);

        Inventory inventory = Bukkit.createInventory(null, rows * 9,
                Component.text("Select Map", NamedTextColor.GOLD));

        String currentMapName = HideAndSeek.getDataController().getCurrentMapName();


        boolean isRandomSelected = (currentMapName == null || currentMapName.isEmpty());
        ItemStack randomMapItem = createMapMenuItem(
                isRandomSelected
        );
        inventory.setItem(0, randomMapItem);


        int slot = 1;
        for (String mapName : availableMaps) {
            boolean isCurrentMap = mapName.equals(currentMapName);
            MapData mapData = plugin.getMapManager().getMapData(mapName);

            ItemStack mapItem = createMapItemWithData(mapName, mapData, isCurrentMap);
            inventory.setItem(slot++, mapItem);
        }

        player.openInventory(inventory);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;

        Component title = event.getView().title();
        String titleStr = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(title);

        if (!titleStr.equals("Select Map")) return;

        event.setCancelled(true);

        if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;

        int slot = event.getRawSlot();

        if (event.getClick() != ClickType.LEFT) return;

        if (slot == 0) {
            selectRandomMap(player);
            return;
        }

        String mapName = getMapNameFromSlot(slot - 1);
        if (mapName != null) {
            selectSpecificMap(player, mapName);
        }
    }

    private void selectRandomMap(Player player) {
        HideAndSeek.getDataController().setCurrentMapName(null);

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

        HideAndSeek.getDataController().setCurrentMapName(mapName);

        player.sendMessage(Component.text("Map selected: ", NamedTextColor.GREEN)
                .append(Component.text(mapName, NamedTextColor.GOLD)));
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
        player.closeInventory();

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                open(player);
            }
        }, 1L);
    }

    private String getMapNameFromSlot(int index) {
        List<String> availableMaps = plugin.getMapManager().getAvailableMaps();
        if (index >= 0 && index < availableMaps.size()) {
            return availableMaps.get(index);
        }
        return null;
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
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createMapItemWithData(String mapName, MapData mapData, boolean isCurrentMap) {
        ItemStack item = new ItemStack(Material.GRASS_BLOCK);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            NamedTextColor nameColor = isCurrentMap ? NamedTextColor.GREEN : NamedTextColor.AQUA;

            meta.displayName(Component.text(mapName, nameColor, TextDecoration.BOLD)
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

            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }
}

