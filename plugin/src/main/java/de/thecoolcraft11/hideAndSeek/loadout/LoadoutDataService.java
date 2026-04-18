package de.thecoolcraft11.hideAndSeek.loadout;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.model.ItemType;
import de.thecoolcraft11.hideAndSeek.model.LoadoutItemType;
import de.thecoolcraft11.hideAndSeek.playerdata.PlayerDataStore;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class LoadoutDataService {

    private static final Gson GSON = new Gson();

    private static final Map<UUID, PlayerLoadout> PLAYER_LOADOUTS = new ConcurrentHashMap<>();
    private static final Map<LoadoutRole, LoadoutFilterMode> FILTER_MODES = new EnumMap<>(LoadoutRole.class);
    private static final Map<LoadoutRole, Set<LoadoutItemType>> FILTER_ITEMS = new EnumMap<>(LoadoutRole.class);
    private static final Map<LoadoutRole, Set<String>> DISABLED_PERKS = new EnumMap<>(LoadoutRole.class);
    private static final Map<LoadoutRole, Map<Integer, AdminRolePreset>> ADMIN_ROLE_PRESETS = new EnumMap<>(LoadoutRole.class);
    private static final Map<LoadoutRole, Boolean> RESTRICT_TO_ADMIN_PRESETS = new EnumMap<>(LoadoutRole.class);
    private static final Map<LoadoutRole, Integer> FORCED_ROLE_PRESET_SLOT = new EnumMap<>(LoadoutRole.class);
    private static boolean GLOBAL_LOADOUT_LOCK;
    private static File dataFile;
    private static YamlConfiguration dataConfig;
    private static PlayerDataStore dataStore;

    private LoadoutDataService() {
    }

    public static void initialize(HideAndSeek plugin) {
        dataStore = plugin.getPlayerDataStore();
        resetAdminDefaults();
        dataFile = resolveDataFile(plugin);
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
        loadAdminPolicy();
    }

    private static File resolveDataFile(HideAndSeek plugin) {
        File dataFolder = new File(plugin.getDataFolder(), "data");
        if (!dataFolder.exists() && !dataFolder.mkdirs()) {
            plugin.getLogger().warning("Failed to create data folder for " + "loadout-data.yml");
        }

        File preferred = new File(dataFolder, "loadout-data.yml");
        File legacy = new File(plugin.getDataFolder(), "loadout-data.yml");
        if (!preferred.exists() && legacy.exists()) {
            try {
                Files.copy(legacy.toPath(), preferred.toPath(), StandardCopyOption.REPLACE_EXISTING);
                plugin.getLogger().info("Migrated legacy YAML file to data/" + "loadout-data.yml");
            } catch (IOException ex) {
                plugin.getLogger().warning("Failed to migrate legacy YAML file " + "loadout-data.yml" + ": " + ex.getMessage());
            }
        }

        return preferred;
    }

    public static PlayerLoadout getLoadout(UUID playerId) {
        return PLAYER_LOADOUTS.computeIfAbsent(playerId, k -> new PlayerLoadout());
    }

    public static void loadPlayer(HideAndSeek plugin, UUID playerId) {
        ensureStore(plugin);
        dataStore.getLoadout(playerId)
                .thenApply(LoadoutDataService::parseLoadoutJson)
                .thenAccept(loadout -> PLAYER_LOADOUTS.put(playerId, loadout))
                .exceptionally(ex -> {
                    plugin.getLogger().warning("Failed to load loadout for " + playerId + ": " + ex.getMessage());
                    PLAYER_LOADOUTS.putIfAbsent(playerId, new PlayerLoadout());
                    return null;
                });
    }

    public static void savePlayer(HideAndSeek plugin, UUID playerId) {
        ensureStore(plugin);
        PlayerLoadout loadout = PLAYER_LOADOUTS.get(playerId);
        if (loadout == null) {
            return;
        }
        dataStore.setLoadout(playerId, toLoadoutJson(loadout))
                .exceptionally(ex -> {
                    plugin.getLogger().warning("Failed to save loadout for " + playerId + ": " + ex.getMessage());
                    return null;
                });
    }

    public static void saveAll(HideAndSeek plugin) {
        for (UUID playerId : new HashSet<>(PLAYER_LOADOUTS.keySet())) {
            savePlayer(plugin, playerId);
        }
        saveAdminPolicy(plugin, false);
        saveData(plugin);
    }

    public static Set<UUID> getAllKnownPlayerIds() {
        return new HashSet<>(PLAYER_LOADOUTS.keySet());
    }

    public static LoadoutFilterMode getFilterMode(LoadoutRole role) {
        return FILTER_MODES.getOrDefault(role, LoadoutFilterMode.BLACKLIST);
    }

    public static void setFilterMode(LoadoutRole role, LoadoutFilterMode mode) {
        FILTER_MODES.put(role, mode == null ? LoadoutFilterMode.BLACKLIST : mode);
    }

    public static Set<LoadoutItemType> getFilterItems(LoadoutRole role) {
        return EnumSet.copyOf(FILTER_ITEMS.getOrDefault(role, EnumSet.noneOf(LoadoutItemType.class)));
    }

    public static void setFilterItems(LoadoutRole role, Set<LoadoutItemType> items) {
        FILTER_ITEMS.put(role, items == null || items.isEmpty() ? EnumSet.noneOf(LoadoutItemType.class) : EnumSet.copyOf(items));
    }

    public static Set<String> getDisabledPerks(LoadoutRole role) {
        return Set.copyOf(DISABLED_PERKS.getOrDefault(role, Set.of()));
    }

    public static void setDisabledPerks(LoadoutRole role, Set<String> perkIds) {
        DISABLED_PERKS.put(role, perkIds == null ? new HashSet<>() : new HashSet<>(perkIds));
    }

    public static AdminRolePreset getAdminPreset(LoadoutRole role, int slot) {
        Map<Integer, AdminRolePreset> bySlot = ADMIN_ROLE_PRESETS.computeIfAbsent(role, ignored -> new HashMap<>());
        return bySlot.computeIfAbsent(slot, ignored -> new AdminRolePreset());
    }

    public static AdminRolePreset getAdminPresetOrNull(LoadoutRole role, int slot) {
        return ADMIN_ROLE_PRESETS.getOrDefault(role, Map.of()).get(slot);
    }

    public static void clearAdminPreset(LoadoutRole role, int slot) {
        ADMIN_ROLE_PRESETS.computeIfAbsent(role, ignored -> new HashMap<>()).remove(slot);
        if (getForcedRolePresetSlot(role) == slot) {
            setForcedRolePresetSlot(role, 0);
        }
    }

    public static Set<Integer> getAdminPresetSlots(LoadoutRole role) {
        return Set.copyOf(ADMIN_ROLE_PRESETS.getOrDefault(role, Map.of()).keySet());
    }

    public static boolean isRoleRestrictedToAdminPresets(LoadoutRole role) {
        return RESTRICT_TO_ADMIN_PRESETS.getOrDefault(role, false);
    }

    public static void setRoleRestrictedToAdminPresets(LoadoutRole role, boolean restricted) {
        RESTRICT_TO_ADMIN_PRESETS.put(role, restricted);
    }

    public static int getForcedRolePresetSlot(LoadoutRole role) {
        return Math.max(0, FORCED_ROLE_PRESET_SLOT.getOrDefault(role, 0));
    }

    public static void setForcedRolePresetSlot(LoadoutRole role, int slot) {
        FORCED_ROLE_PRESET_SLOT.put(role, Math.max(0, slot));
    }

    public static boolean isGlobalLoadoutLock() {
        return GLOBAL_LOADOUT_LOCK;
    }

    public static void setGlobalLoadoutLock(boolean locked) {
        GLOBAL_LOADOUT_LOCK = locked;
    }

    public static void saveAdminPolicy(HideAndSeek plugin) {
        saveAdminPolicy(plugin, true);
    }

    public static void saveAdminPolicy(HideAndSeek plugin, boolean flush) {
        if (dataConfig == null) {
            initialize(plugin);
        }

        for (LoadoutRole role : LoadoutRole.values()) {
            String roleKey = role.name().toLowerCase();
            dataConfig.set("admin.item-filter." + roleKey + ".mode", getFilterMode(role).name());
            dataConfig.set("admin.item-filter." + roleKey + ".entries", getFilterItems(role).stream().map(Enum::name).sorted().toList());
            dataConfig.set("admin.perks." + roleKey + ".disabled", getDisabledPerks(role).stream().sorted().toList());
            dataConfig.set("admin.restrict-to-presets." + roleKey, isRoleRestrictedToAdminPresets(role));
            dataConfig.set("admin.forced-preset." + roleKey, getForcedRolePresetSlot(role));

            dataConfig.set("admin.role-presets." + roleKey, null);
            for (int slot = 1; slot <= PlayerLoadout.MAX_PRESETS; slot++) {
                AdminRolePreset preset = getAdminPresetOrNull(role, slot);
                if (preset == null) {
                    continue;
                }
                String base = "admin.role-presets." + roleKey + "." + slot;
                dataConfig.set(base + ".enabled", preset.isEnabled());
                dataConfig.set(base + ".items", preset.getItems().stream().map(Enum::name).toList());
                dataConfig.set(base + ".disabled-perks", preset.getDisabledPerks().stream().sorted().toList());
            }
        }
        dataConfig.set("admin.global-loadout-lock", GLOBAL_LOADOUT_LOCK);

        if (flush) {
            saveData(plugin);
        }
    }

    public static void shutdown(HideAndSeek plugin) {
        saveAll(plugin);
    }


    private static void ensureStore(HideAndSeek plugin) {
        if (dataStore == null) {
            dataStore = plugin.getPlayerDataStore();
        }
    }

    private static String toLoadoutJson(PlayerLoadout loadout) {
        LoadoutJson json = new LoadoutJson();
        json.hiderItems = loadout.getHiderItems().stream().map(Enum::name).sorted().toList();
        json.seekerItems = loadout.getSeekerItems().stream().map(Enum::name).sorted().toList();
        json.hiderLocked = loadout.isHiderLocked();
        json.seekerLocked = loadout.isSeekerLocked();
        json.selectedAdminPresetHider = loadout.getSelectedAdminPresetSlot(LoadoutRole.HIDER);
        json.selectedAdminPresetSeeker = loadout.getSelectedAdminPresetSlot(LoadoutRole.SEEKER);

        for (Map.Entry<Integer, ItemType> entry : loadout.getHiderSlotPreferences().entrySet()) {
            json.hiderSlotPreferences.put(String.valueOf(entry.getKey()), entry.getValue().name());
        }

        for (Map.Entry<Integer, ItemType> entry : loadout.getSeekerSlotPreferences().entrySet()) {
            json.seekerSlotPreferences.put(String.valueOf(entry.getKey()), entry.getValue().name());
        }

        for (int presetSlot = 1; presetSlot <= PlayerLoadout.MAX_PRESETS; presetSlot++) {
            if (!loadout.hasPreset(presetSlot)) {
                continue;
            }
            PlayerLoadout.Preset preset = loadout.getPreset(presetSlot);
            PresetJson presetJson = new PresetJson();
            presetJson.hider = preset.hiderItems.stream().map(Enum::name).toList();
            presetJson.seeker = preset.seekerItems.stream().map(Enum::name).toList();
            json.presets.put(String.valueOf(presetSlot), presetJson);
        }
        return GSON.toJson(json);
    }

    private static PlayerLoadout parseLoadoutJson(String rawJson) {
        if (rawJson == null || rawJson.isBlank()) {
            return new PlayerLoadout();
        }
        try {
            LoadoutJson json = GSON.fromJson(rawJson, LoadoutJson.class);
            if (json == null) {
                return new PlayerLoadout();
            }

            PlayerLoadout loadout = new PlayerLoadout();
            for (String itemStr : json.hiderItems) {
                try {
                    LoadoutItemType item = LoadoutItemType.valueOf(itemStr);
                    if (item.isForHiders()) {
                        loadout.addHiderItemForced(item, item.getRarity().getDefaultCost());
                    }
                } catch (IllegalArgumentException ignored) {
                }
            }

            for (String itemStr : json.seekerItems) {
                try {
                    LoadoutItemType item = LoadoutItemType.valueOf(itemStr);
                    if (item.isForSeekers()) {
                        loadout.addSeekerItemForced(item, item.getRarity().getDefaultCost());
                    }
                } catch (IllegalArgumentException ignored) {
                }
            }

            loadout.setHiderLocked(json.hiderLocked);
            loadout.setSeekerLocked(json.seekerLocked);
            loadout.setSelectedAdminPresetSlot(LoadoutRole.HIDER, Math.max(0, json.selectedAdminPresetHider));
            loadout.setSelectedAdminPresetSlot(LoadoutRole.SEEKER, Math.max(0, json.selectedAdminPresetSeeker));

            for (Map.Entry<String, String> entry : json.hiderSlotPreferences.entrySet()) {
                try {
                    int slot = Integer.parseInt(entry.getKey());
                    ItemType itemType = ItemType.valueOf(entry.getValue());
                    loadout.setHiderSlotPreference(slot, itemType);
                } catch (Exception ignored) {
                }
            }

            for (Map.Entry<String, String> entry : json.seekerSlotPreferences.entrySet()) {
                try {
                    int slot = Integer.parseInt(entry.getKey());
                    ItemType itemType = ItemType.valueOf(entry.getValue());
                    loadout.setSeekerSlotPreference(slot, itemType);
                } catch (Exception ignored) {
                }
            }

            for (Map.Entry<String, PresetJson> entry : json.presets.entrySet()) {
                int slot;
                try {
                    slot = Integer.parseInt(entry.getKey());
                } catch (NumberFormatException ex) {
                    continue;
                }
                if (slot < 1 || slot > PlayerLoadout.MAX_PRESETS) {
                    continue;
                }

                PresetJson presetJson = entry.getValue();
                if (presetJson == null) {
                    continue;
                }

                LinkedHashSet<LoadoutItemType> hiderItems = new LinkedHashSet<>();
                for (String itemStr : presetJson.hider) {
                    try {
                        LoadoutItemType item = LoadoutItemType.valueOf(itemStr);
                        if (item.isForHiders()) {
                            hiderItems.add(item);
                        }
                    } catch (IllegalArgumentException ignored) {
                    }
                }

                LinkedHashSet<LoadoutItemType> seekerItems = new LinkedHashSet<>();
                for (String itemStr : presetJson.seeker) {
                    try {
                        LoadoutItemType item = LoadoutItemType.valueOf(itemStr);
                        if (item.isForSeekers()) {
                            seekerItems.add(item);
                        }
                    } catch (IllegalArgumentException ignored) {
                    }
                }

                if (!hiderItems.isEmpty() || !seekerItems.isEmpty()) {
                    loadout.setPreset(slot, hiderItems, seekerItems);
                }
            }

            return loadout;
        } catch (JsonSyntaxException ex) {
            return new PlayerLoadout();
        }
    }

    private static void saveData(HideAndSeek plugin) {
        if (dataConfig == null || dataFile == null) {
            return;
        }


        YamlConfiguration merged = YamlConfiguration.loadConfiguration(dataFile);
        for (String key : new HashSet<>(merged.getKeys(true))) {
            if (key.startsWith("admin.")) {
                merged.set(key, null);
            }
        }
        for (String key : dataConfig.getKeys(true)) {
            if (key.startsWith("admin.")) {
                merged.set(key, dataConfig.get(key));
            }
        }

        try {
            merged.save(dataFile);
            dataConfig = merged;
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save loadout data: " + e.getMessage());
        }
    }

    private static final class LoadoutJson {
        private final Map<String, PresetJson> presets = new HashMap<>();
        private List<String> hiderItems = new ArrayList<>();
        private List<String> seekerItems = new ArrayList<>();
        private final Map<String, String> hiderSlotPreferences = new HashMap<>();
        private final Map<String, String> seekerSlotPreferences = new HashMap<>();
        private boolean hiderLocked;
        private boolean seekerLocked;
        private int selectedAdminPresetHider;
        private int selectedAdminPresetSeeker;
    }

    private static void loadAdminPolicy() {
        resetAdminDefaults();
        if (dataConfig == null) {
            return;
        }

        for (LoadoutRole role : LoadoutRole.values()) {
            String roleKey = role.name().toLowerCase();
            String rawMode = dataConfig.getString("admin.item-filter." + roleKey + ".mode", LoadoutFilterMode.BLACKLIST.name());
            try {
                FILTER_MODES.put(role, LoadoutFilterMode.valueOf(rawMode.toUpperCase()));
            } catch (IllegalArgumentException ignored) {
                FILTER_MODES.put(role, LoadoutFilterMode.BLACKLIST);
            }

            Set<LoadoutItemType> items = EnumSet.noneOf(LoadoutItemType.class);
            for (String entry : dataConfig.getStringList("admin.item-filter." + roleKey + ".entries")) {
                try {
                    items.add(LoadoutItemType.valueOf(entry));
                } catch (IllegalArgumentException ignored) {
                }
            }
            FILTER_ITEMS.put(role, items);

            Set<String> perks = new HashSet<>();
            for (String perkId : dataConfig.getStringList("admin.perks." + roleKey + ".disabled")) {
                if (perkId != null && !perkId.isBlank()) {
                    perks.add(perkId);
                }
            }
            DISABLED_PERKS.put(role, perks);

            RESTRICT_TO_ADMIN_PRESETS.put(role, dataConfig.getBoolean("admin.restrict-to-presets." + roleKey, false));
            FORCED_ROLE_PRESET_SLOT.put(role, Math.max(0, dataConfig.getInt("admin.forced-preset." + roleKey, 0)));

            Map<Integer, AdminRolePreset> bySlot = ADMIN_ROLE_PRESETS.computeIfAbsent(role, ignored -> new HashMap<>());
            bySlot.clear();
            for (int slot = 1; slot <= PlayerLoadout.MAX_PRESETS; slot++) {
                String base = "admin.role-presets." + roleKey + "." + slot;
                List<String> itemIds = dataConfig.getStringList(base + ".items");
                List<String> disabledPerkIds = dataConfig.getStringList(base + ".disabled-perks");
                boolean enabled = dataConfig.getBoolean(base + ".enabled", false);
                if (itemIds.isEmpty() && disabledPerkIds.isEmpty() && !enabled) {
                    continue;
                }
                AdminRolePreset preset = new AdminRolePreset();
                for (String raw : itemIds) {
                    try {
                        LoadoutItemType item = LoadoutItemType.valueOf(raw);
                        if ((role == LoadoutRole.HIDER && item.isForHiders()) || (role == LoadoutRole.SEEKER && item.isForSeekers())) {
                            preset.getItems().add(item);
                        }
                    } catch (IllegalArgumentException ignored) {
                    }
                }
                preset.replaceDisabledPerks(disabledPerkIds);
                preset.setEnabled(enabled);
                bySlot.put(slot, preset);
            }
        }
        GLOBAL_LOADOUT_LOCK = dataConfig.getBoolean("admin.global-loadout-lock", false);
    }

    private static void resetAdminDefaults() {
        for (LoadoutRole role : LoadoutRole.values()) {
            FILTER_MODES.put(role, LoadoutFilterMode.BLACKLIST);
            FILTER_ITEMS.put(role, EnumSet.noneOf(LoadoutItemType.class));
            DISABLED_PERKS.put(role, new HashSet<>());
            ADMIN_ROLE_PRESETS.put(role, new HashMap<>());
            RESTRICT_TO_ADMIN_PRESETS.put(role, false);
            FORCED_ROLE_PRESET_SLOT.put(role, 0);
        }
        GLOBAL_LOADOUT_LOCK = false;
    }

    private static final class PresetJson {
        private List<String> hider = new ArrayList<>();
        private List<String> seeker = new ArrayList<>();
    }
}



