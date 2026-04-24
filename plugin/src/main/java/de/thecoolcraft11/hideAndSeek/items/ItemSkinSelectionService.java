package de.thecoolcraft11.hideAndSeek.items;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.items.hider.KnockbackStickItem;
import de.thecoolcraft11.hideAndSeek.items.hider.SpeedBoostItem;
import de.thecoolcraft11.hideAndSeek.model.ItemRarity;
import de.thecoolcraft11.hideAndSeek.playerdata.PlayerDataStore;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public final class ItemSkinSelectionService {

    private static final Gson GSON = new Gson();

    private static final Map<UUID, Map<String, String>> PLAYER_VARIANTS = new ConcurrentHashMap<>();
    private static final Map<UUID, Set<String>> PLAYER_UNLOCKS = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> PLAYER_COINS = new ConcurrentHashMap<>();
    private static final Map<String, SkinMeta> SKIN_META = new ConcurrentHashMap<>();

    private static PlayerDataStore dataStore;

    public static void initialize(HideAndSeek plugin) {
        dataStore = plugin.getPlayerDataStore();
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
                .put(normalizeLogicalItemId(logicalItemId), variantId);
    }

    public static void clearSelectedVariant(UUID playerId, String logicalItemId) {
        Map<String, String> variants = PLAYER_VARIANTS.get(playerId);
        if (variants == null) {
            return;
        }
        variants.remove(normalizeLogicalItemId(logicalItemId));
        if (variants.isEmpty()) {
            PLAYER_VARIANTS.remove(playerId);
        }
    }

    public static String getSelectedVariant(UUID playerId, String logicalItemId) {
        Map<String, String> variants = PLAYER_VARIANTS.get(playerId);
        return variants == null ? null : variants.get(normalizeLogicalItemId(logicalItemId));
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
        return Math.max(0, PLAYER_COINS.getOrDefault(playerId, 0));
    }

    public static void addCoins(HideAndSeek plugin, UUID playerId, int amount) {
        if (amount <= 0) return;

        int updated = getCoins(playerId) + amount;
        PLAYER_COINS.put(playerId, updated);
        var statsService = de.thecoolcraft11.hideAndSeek.playerdata.PlayerStatsService.getActive();
        if (statsService != null) {
            statsService.recordCoinsEarned(playerId, amount);
        }
        savePlayer(plugin, playerId);
    }

    public static void addCoins(HideAndSeek plugin, UUID playerId, int amount, boolean force) {
        if (amount <= 0 && !force) return;

        int updated = Math.max(0, getCoins(playerId) + amount);
        PLAYER_COINS.put(playerId, updated);
        if (amount > 0) {
            var statsService = de.thecoolcraft11.hideAndSeek.playerdata.PlayerStatsService.getActive();
            if (statsService != null) {
                statsService.recordCoinsEarned(playerId, amount);
            }
        }
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
        ensureStore(plugin);
        Player player = Bukkit.getPlayer(playerId);
        String playerName = player != null ? player.getName() : playerId.toString();

        dataStore.touchPlayer(playerId, playerName)
                .exceptionally(ex -> {
                    plugin.getLogger().warning("Failed to touch player in data store for " + playerId + ": " + ex.getMessage());
                    return null;
                });

        CompletableFuture<Long> coinsFuture = dataStore.getCoins(playerId);
        CompletableFuture<String> skinsFuture = dataStore.getSkins(playerId);

        coinsFuture.thenCombine(skinsFuture, LoadedSkinData::from)
                .thenAccept(loaded -> {
                    PLAYER_COINS.put(playerId, (int) Math.max(0L, loaded.coins));

                    if (loaded.unlocked.isEmpty()) {
                        PLAYER_UNLOCKS.remove(playerId);
                    } else {
                        Set<String> unlocks = ConcurrentHashMap.newKeySet();
                        unlocks.addAll(loaded.unlocked);
                        PLAYER_UNLOCKS.put(playerId, unlocks);
                    }

                    if (loaded.selected.isEmpty()) {
                        PLAYER_VARIANTS.remove(playerId);
                    } else {
                        PLAYER_VARIANTS.put(playerId, new ConcurrentHashMap<>(loaded.selected));
                    }
                })
                .exceptionally(ex -> {
                    plugin.getLogger().warning("Failed to load skins data for " + playerId + ": " + ex.getMessage());
                    return null;
                });
    }

    public static void savePlayer(HideAndSeek plugin, UUID playerId) {
        if (!plugin.getConfig().getBoolean("persistence.save-skin-data", true)) {
            return;
        }
        ensureStore(plugin);
        String skinsJson = toSkinsJson(playerId);

        CompletableFuture<Void> coinsSave = dataStore.setCoins(playerId, Math.max(0, getCoins(playerId)));
        CompletableFuture<Void> skinsSave = dataStore.setSkins(playerId, skinsJson);

        CompletableFuture.allOf(coinsSave, skinsSave)
                .exceptionally(ex -> {
                    plugin.getLogger().warning("Failed to save skins data for " + playerId + ": " + ex.getMessage());
                    return null;
                });
    }

    public static void saveAll(HideAndSeek plugin) {
        for (UUID playerId : Bukkit.getOnlinePlayers().stream().map(Player::getUniqueId).toList()) {
            savePlayer(plugin, playerId);
        }
    }

    private static void ensureStore(HideAndSeek plugin) {
        if (dataStore == null) {
            dataStore = plugin.getPlayerDataStore();
        }
    }

    private static String toSkinsJson(UUID playerId) {
        Map<String, String> selected = PLAYER_VARIANTS.getOrDefault(playerId, Map.of());
        Set<String> unlocked = PLAYER_UNLOCKS.getOrDefault(playerId, Set.of());

        SkinDataJson skinData = new SkinDataJson();
        skinData.selected = new HashMap<>();
        for (Map.Entry<String, String> entry : selected.entrySet()) {
            if (entry.getValue() != null && !entry.getValue().isBlank()) {
                skinData.selected.put(normalizeLogicalItemId(entry.getKey()), entry.getValue());
            }
        }
        skinData.unlocked = unlocked.stream().sorted().toList();
        return GSON.toJson(skinData);
    }

    private static LoadedSkinData parseSkinsJson(String json) {
        if (json == null || json.isBlank()) {
            return LoadedSkinData.empty();
        }
        try {
            SkinDataJson data = GSON.fromJson(json, SkinDataJson.class);
            if (data == null) {
                return LoadedSkinData.empty();
            }

            Set<String> unlocked = ConcurrentHashMap.newKeySet();
            if (data.unlocked != null) {
                unlocked.addAll(data.unlocked.stream().filter(v -> v != null && !v.isBlank()).toList());
            }

            Map<String, String> selected = new ConcurrentHashMap<>();
            if (data.selected != null) {
                for (Map.Entry<String, String> entry : data.selected.entrySet()) {
                    String logicalItemId = normalizeLogicalItemId(entry.getKey());
                    String variantId = entry.getValue();
                    if (variantId == null || variantId.isBlank()) {
                        continue;
                    }
                    if (unlocked.contains(metaKey(logicalItemId, variantId))) {
                        selected.put(logicalItemId, variantId);
                    }
                }
            }

            return new LoadedSkinData(selected, unlocked, 0L);
        } catch (JsonSyntaxException ex) {
            return LoadedSkinData.empty();
        }
    }

    private static String metaKey(String logicalItemId, String variantId) {
        return normalizeLogicalItemId(logicalItemId) + "|" + variantId;
    }

    private record SkinMeta(ItemRarity rarity) {
    }

    private static final class SkinDataJson {
        Map<String, String> selected = new HashMap<>();
        List<String> unlocked = new ArrayList<>();
    }

    private record LoadedSkinData(Map<String, String> selected, Set<String> unlocked, long coins) {
        static LoadedSkinData empty() {
            return new LoadedSkinData(new ConcurrentHashMap<>(), ConcurrentHashMap.newKeySet(), 0L);
        }

        static LoadedSkinData from(Long coins, String json) {
            LoadedSkinData parsed = parseSkinsJson(json);
            return new LoadedSkinData(parsed.selected, parsed.unlocked, coins == null ? 0L : coins);
        }
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
