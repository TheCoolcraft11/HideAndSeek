package de.thecoolcraft11.hideAndSeek.playerdata;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import de.thecoolcraft11.hideAndSeek.loadout.PlayerLoadout;
import de.thecoolcraft11.hideAndSeek.model.LoadoutItemType;
import de.thecoolcraft11.minigameframework.MinigameFramework;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public final class YamlPlayerDataStore implements PlayerDataStore {

    private static final Gson GSON = new Gson();

    private static final String COINS_KEY = "coins";
    private static final String XP_KEY = "xp";
    private static final String WINS_KEY = "wins";
    private static final String LOSSES_KEY = "losses";
    private static final String STATS_KEY = "stats";

    private final MinigameFramework framework;
    private final File skinDataFile;
    private final File loadoutDataFile;

    private final YamlConfiguration skinConfig;
    private final YamlConfiguration loadoutConfig;

    public YamlPlayerDataStore(MinigameFramework framework) {
        this.framework = framework;

        File pluginDataFolder = framework.getDataFolder();
        File dataFolder = new File(pluginDataFolder, "data");
        ensureDirectory(dataFolder);

        this.skinDataFile = resolveDataFile(pluginDataFolder, dataFolder, "skin-data.yml");
        this.loadoutDataFile = resolveDataFile(pluginDataFolder, dataFolder, "loadout-data.yml");

        this.skinConfig = YamlConfiguration.loadConfiguration(skinDataFile);
        this.loadoutConfig = YamlConfiguration.loadConfiguration(loadoutDataFile);
    }

    private File resolveDataFile(File pluginDataFolder, File dataFolder, String fileName) {
        File preferred = new File(dataFolder, fileName);
        File legacy = new File(pluginDataFolder, fileName);

        if (!preferred.exists() && legacy.exists()) {
            try {
                Files.copy(legacy.toPath(), preferred.toPath(), StandardCopyOption.REPLACE_EXISTING);
                framework.getLogger().info("Migrated legacy YAML file to data/" + fileName);
            } catch (IOException ex) {
                framework.getLogger().warning("Failed to migrate legacy file " + fileName + ": " + ex.getMessage());
            }
        }

        ensureFile(preferred);
        return preferred;
    }

    private void ensureDirectory(File folder) {
        if (!folder.exists() && !folder.mkdirs()) {
            framework.getLogger().warning("Failed to create " + "data folder for YAML data store");
        }
    }

    @Override
    public synchronized CompletableFuture<Long> getCoins(UUID uuid) {
        return CompletableFuture.completedFuture(getGlobalLong(uuid, COINS_KEY));
    }

    @Override
    public synchronized CompletableFuture<Void> setCoins(UUID uuid, long value) {
        setGlobalLong(uuid, COINS_KEY, Math.max(0L, value));
        saveSkin();
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public synchronized CompletableFuture<Long> getXp(UUID uuid) {
        return CompletableFuture.completedFuture(getGlobalLong(uuid, XP_KEY));
    }

    @Override
    public synchronized CompletableFuture<Void> setXp(UUID uuid, long value) {
        setGlobalLong(uuid, XP_KEY, Math.max(0L, value));
        saveSkin();
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public synchronized CompletableFuture<Long> getWins(UUID uuid) {
        return CompletableFuture.completedFuture(getGlobalLong(uuid, WINS_KEY));
    }

    @Override
    public synchronized CompletableFuture<Long> getLosses(UUID uuid) {
        return CompletableFuture.completedFuture(getGlobalLong(uuid, LOSSES_KEY));
    }

    @Override
    public synchronized CompletableFuture<Void> addWin(UUID uuid) {
        setGlobalLong(uuid, WINS_KEY, getGlobalLong(uuid, WINS_KEY) + 1L);
        saveSkin();
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public synchronized CompletableFuture<Void> addLoss(UUID uuid) {
        setGlobalLong(uuid, LOSSES_KEY, getGlobalLong(uuid, LOSSES_KEY) + 1L);
        saveSkin();
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public synchronized CompletableFuture<String> getSkins(UUID uuid) {
        String basePath = "players." + uuid;

        ConfigurationSection selectedSection = skinConfig.getConfigurationSection(basePath + ".selected");
        Map<String, String> selected = new HashMap<>();
        if (selectedSection != null) {
            for (String logicalItemId : selectedSection.getKeys(false)) {
                String variantId = selectedSection.getString(logicalItemId);
                if (variantId != null && !variantId.isBlank()) {
                    selected.put(logicalItemId, variantId);
                }
            }
        }

        List<String> unlocked = skinConfig.getStringList(basePath + ".unlocked").stream()
                .filter(entry -> entry != null && !entry.isBlank())
                .sorted()
                .toList();

        Map<String, Object> payload = new HashMap<>();
        payload.put("selected", selected);
        payload.put("unlocked", unlocked);
        return CompletableFuture.completedFuture(GSON.toJson(payload));
    }

    @Override
    public synchronized CompletableFuture<Void> setSkins(UUID uuid, String json) {
        SkinPayload payload = parseSkinPayload(json);
        String basePath = "players." + uuid;

        skinConfig.set(basePath + ".selected", null);
        for (Map.Entry<String, String> entry : payload.selected.entrySet()) {
            if (entry.getValue() == null || entry.getValue().isBlank()) {
                continue;
            }
            skinConfig.set(basePath + ".selected." + entry.getKey(), entry.getValue());
        }

        skinConfig.set(basePath + ".unlocked", payload.unlocked.stream().sorted().toList());
        saveSkin();
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public synchronized CompletableFuture<String> getLoadout(UUID uuid) {
        String basePath = "players." + uuid;
        Map<String, Object> json = new HashMap<>();
        json.put("hiderItems", sanitizeItems(loadoutConfig.getStringList(basePath + ".hider-items"), true));
        json.put("seekerItems", sanitizeItems(loadoutConfig.getStringList(basePath + ".seeker-items"), false));
        json.put("hiderLocked", loadoutConfig.getBoolean(basePath + ".lock-hider", false));
        json.put("seekerLocked", loadoutConfig.getBoolean(basePath + ".lock-seeker", false));
        json.put("selectedAdminPresetHider", loadoutConfig.getInt(basePath + ".selected-admin-preset.hider", 0));
        json.put("selectedAdminPresetSeeker", loadoutConfig.getInt(basePath + ".selected-admin-preset.seeker", 0));

        Map<String, Object> presets = new HashMap<>();
        for (int presetSlot = 1; presetSlot <= PlayerLoadout.MAX_PRESETS; presetSlot++) {
            List<String> hider = sanitizeItems(loadoutConfig.getStringList(basePath + ".presets." + presetSlot + ".hider"), true);
            List<String> seeker = sanitizeItems(loadoutConfig.getStringList(basePath + ".presets." + presetSlot + ".seeker"), false);
            if (hider.isEmpty() && seeker.isEmpty()) {
                continue;
            }
            Map<String, Object> preset = new HashMap<>();
            preset.put("hider", hider);
            preset.put("seeker", seeker);
            presets.put(String.valueOf(presetSlot), preset);
        }
        json.put("presets", presets);
        return CompletableFuture.completedFuture(GSON.toJson(json));
    }

    @Override
    public synchronized CompletableFuture<Void> setLoadout(UUID uuid, String json) {
        LoadoutPayload payload = parseLoadoutPayload(json);
        String basePath = "players." + uuid;

        loadoutConfig.set(basePath + ".hider-items", sanitizeItems(payload.hiderItems, true));
        loadoutConfig.set(basePath + ".seeker-items", sanitizeItems(payload.seekerItems, false));
        loadoutConfig.set(basePath + ".lock-hider", payload.hiderLocked);
        loadoutConfig.set(basePath + ".lock-seeker", payload.seekerLocked);
        loadoutConfig.set(basePath + ".selected-admin-preset.hider", Math.max(0, payload.selectedAdminPresetHider));
        loadoutConfig.set(basePath + ".selected-admin-preset.seeker", Math.max(0, payload.selectedAdminPresetSeeker));

        String presetsPath = basePath + ".presets";
        loadoutConfig.set(presetsPath, null);
        for (Map.Entry<String, PresetPayload> entry : payload.presets.entrySet()) {
            int slot;
            try {
                slot = Integer.parseInt(entry.getKey());
            } catch (NumberFormatException ex) {
                continue;
            }
            if (slot < 1 || slot > PlayerLoadout.MAX_PRESETS) {
                continue;
            }

            List<String> hider = sanitizeItems(entry.getValue().hider, true);
            List<String> seeker = sanitizeItems(entry.getValue().seeker, false);
            if (hider.isEmpty() && seeker.isEmpty()) {
                continue;
            }

            loadoutConfig.set(presetsPath + "." + slot + ".hider", hider);
            loadoutConfig.set(presetsPath + "." + slot + ".seeker", seeker);
        }

        saveLoadout();
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public synchronized CompletableFuture<String> getStats(UUID uuid) {
        return CompletableFuture.completedFuture(skinConfig.getString(globalPath(uuid, STATS_KEY), "{}"));
    }

    @Override
    public synchronized CompletableFuture<Void> setStats(UUID uuid, String json) {
        skinConfig.set(globalPath(uuid, STATS_KEY), (json == null || json.isBlank()) ? "{}" : json);
        saveSkin();
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public synchronized CompletableFuture<Void> touchPlayer(UUID uuid, String name) {
        String basePath = "players." + uuid;

        if (!skinConfig.contains(basePath + "." + COINS_KEY)) {
            skinConfig.set(basePath + "." + COINS_KEY, 0L);
        }
        if (!skinConfig.contains(basePath + "." + XP_KEY)) {
            skinConfig.set(basePath + "." + XP_KEY, 0L);
        }
        if (!skinConfig.contains(basePath + "." + WINS_KEY)) {
            skinConfig.set(basePath + "." + WINS_KEY, 0L);
        }
        if (!skinConfig.contains(basePath + "." + LOSSES_KEY)) {
            skinConfig.set(basePath + "." + LOSSES_KEY, 0L);
        }
        if (!skinConfig.contains(basePath + ".unlocked")) {
            skinConfig.set(basePath + ".unlocked", List.of());
        }
        if (!skinConfig.contains(basePath + "." + STATS_KEY)) {
            skinConfig.set(basePath + "." + STATS_KEY, "{}");
        }

        if (!loadoutConfig.contains(basePath + ".hider-items")) {
            loadoutConfig.set(basePath + ".hider-items", List.of());
        }
        if (!loadoutConfig.contains(basePath + ".seeker-items")) {
            loadoutConfig.set(basePath + ".seeker-items", List.of());
        }

        saveSkin();
        saveLoadout();
        return CompletableFuture.completedFuture(null);
    }

    private SkinPayload parseSkinPayload(String json) {
        SkinPayload payload = new SkinPayload();
        if (json == null || json.isBlank()) {
            return payload;
        }

        try {
            SkinPayload raw = GSON.fromJson(json, SkinPayload.class);
            if (raw == null) {
                return payload;
            }
            for (Map.Entry<String, String> entry : raw.selected.entrySet()) {
                if (entry.getKey() == null || entry.getKey().isBlank()) {
                    continue;
                }
                if (entry.getValue() == null || entry.getValue().isBlank()) {
                    continue;
                }
                payload.selected.put(entry.getKey(), entry.getValue());
            }
            for (String unlocked : raw.unlocked) {
                if (unlocked != null && !unlocked.isBlank()) {
                    payload.unlocked.add(unlocked);
                }
            }
            return payload;
        } catch (JsonSyntaxException ex) {
            return payload;
        }
    }

    private LoadoutPayload parseLoadoutPayload(String json) {
        LoadoutPayload payload = new LoadoutPayload();
        if (json == null || json.isBlank()) {
            return payload;
        }

        try {
            LoadoutPayload raw = GSON.fromJson(json, LoadoutPayload.class);
            if (raw == null) {
                return payload;
            }
            payload.hiderItems = raw.hiderItems == null ? new ArrayList<>() : new ArrayList<>(raw.hiderItems);
            payload.seekerItems = raw.seekerItems == null ? new ArrayList<>() : new ArrayList<>(raw.seekerItems);
            payload.hiderLocked = raw.hiderLocked;
            payload.seekerLocked = raw.seekerLocked;
            payload.selectedAdminPresetHider = Math.max(0, raw.selectedAdminPresetHider);
            payload.selectedAdminPresetSeeker = Math.max(0, raw.selectedAdminPresetSeeker);
            payload.presets.putAll(raw.presets);
            return payload;
        } catch (JsonSyntaxException ex) {
            return payload;
        }
    }

    private List<String> sanitizeItems(Collection<String> raw, boolean hiderRole) {
        List<String> out = new ArrayList<>();
        for (String value : raw) {
            if (value == null || value.isBlank()) {
                continue;
            }
            try {
                LoadoutItemType item = LoadoutItemType.valueOf(value);
                if (hiderRole && item.isForHiders()) {
                    out.add(item.name());
                } else if (!hiderRole && item.isForSeekers()) {
                    out.add(item.name());
                }
            } catch (IllegalArgumentException ignored) {
            }
        }
        return out;
    }

    private long getGlobalLong(UUID uuid, String key) {
        return Math.max(0L, skinConfig.getLong(globalPath(uuid, key), 0L));
    }

    private void setGlobalLong(UUID uuid, String key, long value) {
        skinConfig.set(globalPath(uuid, key), Math.max(0L, value));
    }

    private String globalPath(UUID uuid, String key) {
        return "players." + uuid + "." + key;
    }

    private void ensureFile(File file) {
        if (file.exists()) {
            return;
        }
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                framework.getLogger().warning("Failed to create " + parent.getAbsolutePath());
            }
            if (!file.createNewFile()) {
                framework.getLogger().warning("Failed to create " + file.getName());
                return;
            }
            Files.writeString(file.toPath(), "players:\n");
        } catch (IOException ex) {
            framework.getLogger().warning("Failed to create " + file.getName() + ": " + ex.getMessage());
        }
    }

    private void saveSkin() {
        try {
            skinConfig.save(skinDataFile);
        } catch (IOException ex) {
            framework.getLogger().warning("Failed to save skin-data.yml: " + ex.getMessage());
        }
    }

    private void saveLoadout() {
        YamlConfiguration merged = YamlConfiguration.loadConfiguration(loadoutDataFile);
        merged.set("players", loadoutConfig.get("players"));
        try {
            merged.save(loadoutDataFile);
        } catch (IOException ex) {
            framework.getLogger().warning("Failed to save loadout-data.yml: " + ex.getMessage());
        }
    }

    private static final class SkinPayload {
        private final Map<String, String> selected = new HashMap<>();
        private final List<String> unlocked = new ArrayList<>();
    }

    private static final class LoadoutPayload {
        private final Map<String, PresetPayload> presets = new HashMap<>();
        private List<String> hiderItems = new ArrayList<>();
        private List<String> seekerItems = new ArrayList<>();
        private boolean hiderLocked;
        private boolean seekerLocked;
        private int selectedAdminPresetHider;
        private int selectedAdminPresetSeeker;
    }

    private static final class PresetPayload {
        private final List<String> hider = new ArrayList<>();
        private final List<String> seeker = new ArrayList<>();
    }
}



