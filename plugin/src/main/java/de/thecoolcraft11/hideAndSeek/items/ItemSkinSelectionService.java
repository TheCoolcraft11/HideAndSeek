package de.thecoolcraft11.hideAndSeek.items;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.items.hider.KnockbackStickItem;
import de.thecoolcraft11.hideAndSeek.items.hider.SpeedBoostItem;
import de.thecoolcraft11.hideAndSeek.model.ItemRarity;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class ItemSkinSelectionService {

    private static final Map<UUID, Map<String, String>> PLAYER_VARIANTS = new ConcurrentHashMap<>();
    private static final Map<UUID, Set<String>> PLAYER_UNLOCKS = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> PLAYER_COINS = new ConcurrentHashMap<>();
    private static final Map<String, SkinMeta> SKIN_META = new ConcurrentHashMap<>();

    private static File dataFile;
    private static YamlConfiguration dataConfig;

    public static void initialize(HideAndSeek plugin) {
        dataFile = new File(plugin.getDataFolder(), "skin-data.yml");
        if (!dataFile.exists()) {
            try {
                File parent = dataFile.getParentFile();
                if (parent != null && !parent.exists()) {
                    parent.mkdirs();
                }
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("Failed to create skin data file: " + e.getMessage());
            }
        }
        dataConfig = YamlConfiguration.loadConfiguration(dataFile);
        loadAll();
    }

    private ItemSkinSelectionService() {
    }

    public static void setSelectedVariant(UUID playerId, String logicalItemId, String variantId) {
        if (variantId == null || variantId.isBlank()) {
            clearSelectedVariant(playerId, logicalItemId);
            return;
        }
        PLAYER_VARIANTS
                .computeIfAbsent(playerId, ignored -> new ConcurrentHashMap<>())
                .put(logicalItemId, variantId);
    }

    public static void clearSelectedVariant(UUID playerId, String logicalItemId) {
        Map<String, String> variants = PLAYER_VARIANTS.get(playerId);
        if (variants == null) {
            return;
        }
        variants.remove(logicalItemId);
        if (variants.isEmpty()) {
            PLAYER_VARIANTS.remove(playerId);
        }
    }

    public static String getSelectedVariant(UUID playerId, String logicalItemId) {
        Map<String, String> variants = PLAYER_VARIANTS.get(playerId);
        return variants == null ? null : variants.get(logicalItemId);
    }

    public static String getSelectedVariant(Player player, String logicalItemId) {
        return player == null ? null : getSelectedVariant(player.getUniqueId(), logicalItemId);
    }

    public static boolean isSelected(Player player, String logicalItemId, String variantId) {
        String selected = getSelectedVariant(player, logicalItemId);
        return selected != null && selected.equals(variantId);
    }

    public static void shutdown(HideAndSeek plugin) {
        saveAll(plugin);
    }

    public static void registerVariantMetadata(String itemId, String variantId, ItemRarity rarity) {
        SKIN_META.put(metaKey(normalizeLogicalItemId(itemId), variantId), new SkinMeta(rarity));
    }

    public static ItemRarity getRarity(String logicalItemId, String variantId) {
        SkinMeta meta = SKIN_META.get(metaKey(normalizeLogicalItemId(logicalItemId), variantId));
        return meta == null ? ItemRarity.COMMON : meta.rarity();
    }

    public static int getCost(HideAndSeek plugin, String logicalItemId, String variantId) {
        return switch (getRarity(logicalItemId, variantId)) {
            case COMMON -> plugin.getSettingRegistry().get("skin-shop.cost-common", 100);
            case UNCOMMON -> plugin.getSettingRegistry().get("skin-shop.cost-uncommon", 250);
            case RARE -> plugin.getSettingRegistry().get("skin-shop.cost-rare", 500);
            case EPIC -> plugin.getSettingRegistry().get("skin-shop.cost-epic", 900);
            case LEGENDARY -> plugin.getSettingRegistry().get("skin-shop.cost-legendary", 1500);
        };
    }

    public static int getCoins(UUID playerId) {
        return PLAYER_COINS.getOrDefault(playerId, 0);
    }

    public static void addCoins(HideAndSeek plugin, UUID playerId, int amount) {
        if (amount <= 0) return;

        int updated = getCoins(playerId) + amount;
        PLAYER_COINS.put(playerId, updated);
        savePlayer(plugin, playerId);
    }

    public static void addCoins(HideAndSeek plugin, UUID playerId, int amount, boolean force) {
        if (amount <= 0 && !force) return;

        int updated = getCoins(playerId) + amount;
        PLAYER_COINS.put(playerId, updated);
        savePlayer(plugin, playerId);
    }

    public static boolean isUnlocked(UUID playerId, String logicalItemId, String variantId) {
        Set<String> unlocks = PLAYER_UNLOCKS.get(playerId);
        if (unlocks == null) {
            return false;
        }
        return unlocks.contains(metaKey(normalizeLogicalItemId(logicalItemId), variantId));
    }

    public static boolean unlock(HideAndSeek plugin, UUID playerId, String logicalItemId, String variantId) {
        if (isUnlocked(playerId, logicalItemId, variantId)) {
            return true;
        }
        int cost = getCost(plugin, logicalItemId, variantId);
        int coins = getCoins(playerId);
        if (coins < cost) {
            return false;
        }

        PLAYER_COINS.put(playerId, coins - cost);
        PLAYER_UNLOCKS
                .computeIfAbsent(playerId, ignored -> ConcurrentHashMap.newKeySet())
                .add(metaKey(normalizeLogicalItemId(logicalItemId), variantId));
        savePlayer(plugin, playerId);
        return true;
    }

    public static void applySelectedVariants(Player player, HideAndSeek plugin) {
        Map<String, String> variants = PLAYER_VARIANTS.get(player.getUniqueId());
        if (variants == null || variants.isEmpty()) {
            return;
        }

        var variantManager = plugin.getCustomItemManager().getVariantManager();
        for (Map.Entry<String, String> entry : variants.entrySet()) {
            if (entry.getValue() == null || entry.getValue().isBlank()) {
                continue;
            }
            if (!isUnlocked(player.getUniqueId(), entry.getKey(), entry.getValue())) {
                continue;
            }
            String runtimeItemId = resolveRuntimeItemId(player, entry.getKey());
            if (!variantManager.hasVariants(runtimeItemId)) {
                continue;
            }
            variantManager.switchVariant(player, runtimeItemId, entry.getValue());
        }
    }

    public static void loadPlayer(HideAndSeek plugin, UUID playerId) {
        if (dataConfig == null) {
            initialize(plugin);
        }

        String basePath = "players." + playerId;
        PLAYER_COINS.put(playerId, Math.max(0, dataConfig.getInt(basePath + ".coins", 0)));

        Set<String> unlocks = ConcurrentHashMap.newKeySet();
        unlocks.addAll(dataConfig.getStringList(basePath + ".unlocked"));
        if (!unlocks.isEmpty()) {
            PLAYER_UNLOCKS.put(playerId, unlocks);
        } else {
            PLAYER_UNLOCKS.remove(playerId);
        }

        ConfigurationSection selectedSection = dataConfig.getConfigurationSection(basePath + ".selected");
        if (selectedSection == null) {
            PLAYER_VARIANTS.remove(playerId);
            return;
        }

        Map<String, String> selected = new ConcurrentHashMap<>();
        for (String key : selectedSection.getKeys(false)) {
            String variantId = selectedSection.getString(key);
            if (variantId == null || variantId.isBlank()) {
                continue;
            }
            if (isUnlocked(playerId, key, variantId)) {
                selected.put(normalizeLogicalItemId(key), variantId);
            }
        }

        if (selected.isEmpty()) {
            PLAYER_VARIANTS.remove(playerId);
        } else {
            PLAYER_VARIANTS.put(playerId, selected);
        }
    }


    public static void savePlayer(HideAndSeek plugin, UUID playerId) {
        savePlayer(plugin, playerId, true);
    }

    public static void savePlayer(HideAndSeek plugin, UUID playerId, boolean flush) {
        if (!plugin.getConfig().getBoolean("persistence.save-skin-data", true)) {
            return;
        }

        if (dataConfig == null) {
            initialize(plugin);
        }

        String basePath = "players." + playerId;
        dataConfig.set(basePath + ".coins", Math.max(0, getCoins(playerId)));

        Set<String> unlocks = PLAYER_UNLOCKS.get(playerId);
        dataConfig.set(basePath + ".unlocked", unlocks == null ? java.util.List.of() : unlocks.stream().sorted().toList());

        dataConfig.set(basePath + ".selected", null);
        Map<String, String> selected = PLAYER_VARIANTS.get(playerId);
        if (selected != null && !selected.isEmpty()) {
            for (Map.Entry<String, String> entry : selected.entrySet()) {
                if (entry.getValue() == null || entry.getValue().isBlank()) {
                    continue;
                }
                dataConfig.set(basePath + ".selected." + normalizeLogicalItemId(entry.getKey()), entry.getValue());
            }
        }

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

    private static void loadAll() {
        if (dataConfig == null) {
            return;
        }
        ConfigurationSection players = dataConfig.getConfigurationSection("players");
        if (players == null) {
            return;
        }
        for (String key : players.getKeys(false)) {
            try {
                UUID playerId = UUID.fromString(key);
                PLAYER_COINS.put(playerId, Math.max(0, dataConfig.getInt("players." + key + ".coins", 0)));

                Set<String> unlocks = ConcurrentHashMap.newKeySet();
                unlocks.addAll(dataConfig.getStringList("players." + key + ".unlocked"));
                if (!unlocks.isEmpty()) {
                    PLAYER_UNLOCKS.put(playerId, unlocks);
                }

                ConfigurationSection selectedSection = dataConfig.getConfigurationSection("players." + key + ".selected");
                if (selectedSection != null) {
                    Map<String, String> selected = new ConcurrentHashMap<>();
                    for (String logicalItemId : selectedSection.getKeys(false)) {
                        String variantId = selectedSection.getString(logicalItemId);
                        if (variantId == null || variantId.isBlank()) {
                            continue;
                        }
                        if (unlocks.contains(metaKey(normalizeLogicalItemId(logicalItemId), variantId))) {
                            selected.put(normalizeLogicalItemId(logicalItemId), variantId);
                        }
                    }
                    if (!selected.isEmpty()) {
                        PLAYER_VARIANTS.put(playerId, selected);
                    }
                }
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    private static void saveData(HideAndSeek plugin) {
        if (dataConfig == null || dataFile == null) {
            return;
        }
        try {
            dataConfig.save(dataFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save skin data: " + e.getMessage());
        }
    }

    private static String metaKey(String logicalItemId, String variantId) {
        return normalizeLogicalItemId(logicalItemId) + "|" + variantId;
    }

    private record SkinMeta(ItemRarity rarity) {
    }

    public static String resolveRuntimeItemId(Player player, String logicalItemId) {
        if (logicalItemId.equals(SpeedBoostItem.ID)) {
            return SpeedBoostItem.ID + "_" + SpeedBoostItem.getSpeedLevel(player.getUniqueId());
        }
        if (logicalItemId.equals(KnockbackStickItem.ID)) {
            return KnockbackStickItem.ID + "_" + KnockbackStickItem.getKnockbackLevel(player.getUniqueId());
        }
        return logicalItemId;
    }

    public static String normalizeLogicalItemId(String itemId) {
        if (itemId.startsWith(SpeedBoostItem.ID + "_")) {
            return SpeedBoostItem.ID;
        }
        if (itemId.startsWith(KnockbackStickItem.ID + "_")) {
            return KnockbackStickItem.ID;
        }
        return itemId;
    }
}
