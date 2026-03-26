package de.thecoolcraft11.hideAndSeek.loadout;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.model.LoadoutItemType;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class LoadoutDataService {

    private static final Map<UUID, PlayerLoadout> PLAYER_LOADOUTS = new ConcurrentHashMap<>();
    private static File dataFile;
    private static YamlConfiguration dataConfig;

    private LoadoutDataService() {
    }

    public static void initialize(HideAndSeek plugin) {
        dataFile = new File(plugin.getDataFolder(), "loadout-data.yml");
        if (!dataFile.exists()) {
            try {
                File parent = dataFile.getParentFile();
                if (parent != null && !parent.exists()) {
                    if (!parent.mkdirs()) {
                        plugin.getLogger().warning("Failed to create parent directories for loadout data file");
                    }
                }
                if (!dataFile.createNewFile()) {
                    plugin.getLogger().warning("Failed to create loadout data file");
                }
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to create loadout data file: " + e.getMessage());
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        loadAll(plugin);
    }

    public static PlayerLoadout getLoadout(UUID playerId) {
        return PLAYER_LOADOUTS.computeIfAbsent(playerId, k -> new PlayerLoadout());
    }

    public static void loadPlayer(HideAndSeek plugin, UUID playerId) {
        if (dataConfig == null) {
            initialize(plugin);
        }

        String basePath = "players." + playerId;
        PlayerLoadout loadout = new PlayerLoadout();


        List<String> hiderItemsStr = dataConfig.getStringList(basePath + ".hider-items");
        for (String itemStr : hiderItemsStr) {
            try {
                LoadoutItemType item = LoadoutItemType.valueOf(itemStr);
                if (item.isForHiders()) {
                    loadout.addHiderItem(item, 999, 999, item.getRarity().getDefaultCost());
                }
            } catch (IllegalArgumentException ignored) {
            }
        }


        List<String> seekerItemsStr = dataConfig.getStringList(basePath + ".seeker-items");
        for (String itemStr : seekerItemsStr) {
            try {
                LoadoutItemType item = LoadoutItemType.valueOf(itemStr);
                if (item.isForSeekers()) {
                    loadout.addSeekerItem(item, 999, 999, item.getRarity().getDefaultCost());
                }
            } catch (IllegalArgumentException ignored) {
            }
        }

        PLAYER_LOADOUTS.put(playerId, loadout);
    }

    public static void savePlayer(HideAndSeek plugin, UUID playerId) {
        savePlayer(plugin, playerId, true);
    }

    public static void savePlayer(HideAndSeek plugin, UUID playerId, boolean flush) {
        if (dataConfig == null) {
            initialize(plugin);
        }

        PlayerLoadout loadout = PLAYER_LOADOUTS.get(playerId);
        if (loadout == null) {
            return;
        }

        String basePath = "players." + playerId;


        List<String> hiderItems = loadout.getHiderItems().stream()
                .map(LoadoutItemType::name)
                .sorted()
                .toList();
        dataConfig.set(basePath + ".hider-items", hiderItems);


        List<String> seekerItems = loadout.getSeekerItems().stream()
                .map(LoadoutItemType::name)
                .sorted()
                .toList();
        dataConfig.set(basePath + ".seeker-items", seekerItems);

        if (flush) {
            saveData(plugin);
        }
    }

    public static void saveAll(HideAndSeek plugin) {
        if (dataConfig == null) {
            initialize(plugin);
        }
        for (UUID playerId : Bukkit.getOnlinePlayers().stream().map(Player::getUniqueId).toList()) {
            savePlayer(plugin, playerId, false);
        }
        saveData(plugin);
    }

    public static void shutdown(HideAndSeek plugin) {
        saveAll(plugin);
    }

    private static void loadAll(HideAndSeek plugin) {
        if (dataConfig == null) {
            return;
        }
        org.bukkit.configuration.ConfigurationSection players = dataConfig.getConfigurationSection("players");
        if (players == null) {
            return;
        }
        for (String key : players.getKeys(false)) {
            UUID playerId = UUID.fromString(key);
            loadPlayer(plugin, playerId);
        }
    }

    private static void saveData(HideAndSeek plugin) {
        if (dataConfig == null || dataFile == null) {
            return;
        }
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save loadout data: " + e.getMessage());
        }
    }
}



