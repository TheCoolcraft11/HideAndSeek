package de.thecoolcraft11.hideAndSeek.command.debug;

import com.google.gson.Gson;
import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.loadout.PlayerLoadout;
import de.thecoolcraft11.hideAndSeek.model.LoadoutItemType;
import de.thecoolcraft11.hideAndSeek.playerdata.DatabasePlayerDataStore;
import de.thecoolcraft11.minigameframework.storage.sql.stats.GlobalStatsAPI;
import de.thecoolcraft11.minigameframework.storage.sql.stats.MinigameStatsAPI;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class DebugMigrateYamlCommand implements DebugSubcommand {

    private static final Gson GSON = new Gson();
    private final HideAndSeek plugin;

    public DebugMigrateYamlCommand(HideAndSeek plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean handle(@NotNull CommandSender sender, @NotNull String[] args) {

        boolean force = Arrays.stream(args)
                .anyMatch(arg -> arg.equalsIgnoreCase("--force"));

        boolean playersOnline = !Bukkit.getOnlinePlayers().isEmpty();

        if (playersOnline && !force) {
            sender.sendMessage(plugin.tr(sender, "command.debug.migrateyaml.blocked_online"));
            sender.sendMessage(plugin.tr(sender, "command.debug.migrateyaml.blocked_offline"));
            sender.sendMessage(plugin.tr(sender, "command.debug.migrateyaml.blocked_force"));

            return false;
        }

        if (!GlobalStatsAPI.isAvailable() || !MinigameStatsAPI.isAvailable()) {
            sender.sendMessage(plugin.tr(sender, "command.debug.migrateyaml.unavailable"));
            return false;
        }

        sender.sendMessage(plugin.tr(sender, "command.debug.migrateyaml.start"));

        CompletableFuture.supplyAsync(this::readMigrationSource)
                .thenCompose(this::migrateToDatabase)
                .whenComplete((report, error) -> {
                    if (error != null) {
                        runOnMain(() -> sender.sendMessage(plugin.tr(sender, "command.debug.migrateyaml.failed",
                                java.util.Map.of("error", error.getMessage()))));
                        return;
                    }

                    runOnMain(() -> {
                        sender.sendMessage(plugin.tr(sender, "command.debug.migrateyaml.complete"));
                        sender.sendMessage(plugin.tr(sender, "command.debug.migrateyaml.players_migrated",
                                java.util.Map.of("count", String.valueOf(report.playersMigrated.get()))));
                        sender.sendMessage(plugin.tr(sender, "command.debug.migrateyaml.fields_migrated",
                                java.util.Map.of("count", String.valueOf(report.fieldsMigrated.get()))));
                        sender.sendMessage(plugin.tr(sender, "command.debug.migrateyaml.failures",
                                java.util.Map.of("count", String.valueOf(report.failures.size()))));
                        int limit = Math.min(10, report.failures.size());
                        for (int i = 0; i < limit; i++) {
                            sender.sendMessage(plugin.tr(sender, "command.debug.migrateyaml.failure_entry",
                                    java.util.Map.of("failure", report.failures.get(i))));
                        }
                    });
                });
        return true;
    }

    private CompletableFuture<MigrationReport> migrateToDatabase(MigrationSource source) {
        MigrationReport report = new MigrationReport();
        List<CompletableFuture<Void>> tasks = new ArrayList<>();

        for (UUID playerId : source.allPlayerIds()) {
            String playerName = safePlayerName(playerId);
            tasks.add(track(playerId, GlobalStatsAPI.touchPlayer(playerId, playerName), "touchPlayer", report));

            GlobalData global = source.globalData.get(playerId);
            if (global != null) {
                tasks.add(track(playerId, GlobalStatsAPI.setStat(playerId, "coins", Math.max(0L, global.coins)), "coins", report));
                tasks.add(track(playerId, GlobalStatsAPI.setStat(playerId, "xp", Math.max(0L, global.xp)), "xp", report));
                tasks.add(track(playerId, GlobalStatsAPI.setStat(playerId, "wins", Math.max(0L, global.wins)), "wins", report));
                tasks.add(track(playerId, GlobalStatsAPI.setStat(playerId, "losses", Math.max(0L, global.losses)), "losses", report));
            }

            String skinsJson = source.skinsJson.get(playerId);
            if (skinsJson != null) {
                tasks.add(track(playerId,
                        MinigameStatsAPI.setTypedStatString(playerId, DatabasePlayerDataStore.MINIGAME_ID, "skins", sanitizeJson(skinsJson)),
                        "skins",
                        report));
            }

            String loadoutJson = source.loadoutJson.get(playerId);
            if (loadoutJson != null) {
                tasks.add(track(playerId,
                        MinigameStatsAPI.setTypedStatString(playerId, DatabasePlayerDataStore.MINIGAME_ID, "loadout", sanitizeJson(loadoutJson)),
                        "loadout",
                        report));
            }

            String statsJson = source.statsJson.get(playerId);
            if (statsJson != null) {
                tasks.add(track(playerId,
                        MinigameStatsAPI.setTypedStatString(playerId, DatabasePlayerDataStore.MINIGAME_ID, "stats",
                                sanitizeJson(statsJson)),
                        "stats",
                        report));
            }
        }

        return CompletableFuture.allOf(tasks.toArray(CompletableFuture[]::new)).thenApply(ignored -> report);
    }

    private CompletableFuture<Void> track(UUID playerId, CompletableFuture<Void> future, String field, MigrationReport report) {
        return future.thenRun(() -> {
            report.fieldsMigrated.incrementAndGet();
            if (report.playerSuccess.add(playerId)) {
                report.playersMigrated.incrementAndGet();
            }
        }).exceptionally(ex -> {
            report.failures.add(playerId + " -> " + field + ": " + ex.getMessage());
            return null;
        });
    }

    private MigrationSource readMigrationSource() {
        MigrationSource source = new MigrationSource();

        for (File globalFile : resolveCandidates("global.yml")) {
            if (globalFile.exists()) {
                readGlobalYaml(YamlConfiguration.loadConfiguration(globalFile), source.globalData);
            }
        }
        for (File minigameFile : resolveCandidates("minigame.yml")) {
            if (minigameFile.exists()) {
                readMinigameYaml(YamlConfiguration.loadConfiguration(minigameFile), source.skinsJson,
                        source.loadoutJson, source.statsJson);
            }
        }

        List<File> legacySkinFiles = resolveCandidates("skin-data.yml");
        for (int i = 0; i < legacySkinFiles.size(); i++) {
            File legacySkinFile = legacySkinFiles.get(i);
            if (legacySkinFile.exists()) {
                readLegacySkinsYaml(YamlConfiguration.loadConfiguration(legacySkinFile), source.globalData,
                        source.skinsJson, source.statsJson, i > 0);
            }
        }

        List<File> legacyLoadoutFiles = resolveCandidates("loadout-data.yml");
        for (int i = 0; i < legacyLoadoutFiles.size(); i++) {
            File legacyLoadoutFile = legacyLoadoutFiles.get(i);
            if (legacyLoadoutFile.exists()) {
                readLegacyLoadoutYaml(YamlConfiguration.loadConfiguration(legacyLoadoutFile), source.loadoutJson, i > 0);
            }
        }

        return source;
    }

    private List<File> resolveCandidates(String fileName) {
        List<File> files = new ArrayList<>();
        files.add(new File(plugin.getDataFolder(), fileName));
        files.add(new File(new File(plugin.getDataFolder(), "data"), fileName));
        return files;
    }

    private void readGlobalYaml(YamlConfiguration yaml, Map<UUID, GlobalData> out) {
        ConfigurationSection players = yaml.getConfigurationSection("players");
        if (players == null) {
            return;
        }
        for (String key : players.getKeys(false)) {
            UUID uuid = parseUuid(key);
            if (uuid == null) {
                continue;
            }
            String base = "players." + key;
            GlobalData data = new GlobalData(
                    yaml.getLong(base + ".coins", 0L),
                    yaml.getLong(base + ".xp", 0L),
                    yaml.getLong(base + ".wins", 0L),
                    yaml.getLong(base + ".losses", 0L)
            );
            out.put(uuid, data);
        }
    }

    private void readMinigameYaml(YamlConfiguration yaml, Map<UUID, String> skins, Map<UUID, String> loadout, Map<UUID, String> stats) {
        ConfigurationSection players = yaml.getConfigurationSection("players");
        if (players == null) {
            return;
        }
        for (String key : players.getKeys(false)) {
            UUID uuid = parseUuid(key);
            if (uuid == null) {
                continue;
            }
            String base = "players." + key;
            String skinsJson = yaml.getString(base + ".skins");
            if (skinsJson != null && !skinsJson.isBlank()) {
                skins.put(uuid, skinsJson);
            }
            String loadoutJson = yaml.getString(base + ".loadout");
            if (loadoutJson != null && !loadoutJson.isBlank()) {
                loadout.put(uuid, loadoutJson);
            }

            String statsJson = yaml.getString(base + ".stats");
            if (statsJson != null && !statsJson.isBlank()) {
                stats.put(uuid, statsJson);
            }
        }
    }

    private void readLegacySkinsYaml(YamlConfiguration yaml, Map<UUID, GlobalData> global, Map<UUID, String> skins, Map<UUID, String> stats, boolean overrideExisting) {
        ConfigurationSection players = yaml.getConfigurationSection("players");
        if (players == null) {
            return;
        }
        for (String key : players.getKeys(false)) {
            UUID uuid = parseUuid(key);
            if (uuid == null) {
                continue;
            }
            String base = "players." + key;
            long coins = Math.max(0L, yaml.getLong(base + ".coins", 0L));
            GlobalData current = global.get(uuid);
            long xp = current == null ? 0L : current.xp;
            long wins = current == null ? 0L : current.wins;
            long losses = current == null ? 0L : current.losses;
            GlobalData updated = new GlobalData(coins, xp, wins, losses);
            if (overrideExisting) {
                global.put(uuid, updated);
            } else {
                global.putIfAbsent(uuid, updated);
            }

            Map<String, Object> selected = new HashMap<>();
            ConfigurationSection selectedSection = yaml.getConfigurationSection(base + ".selected");
            if (selectedSection != null) {
                for (String logical : selectedSection.getKeys(false)) {
                    String variant = selectedSection.getString(logical);
                    if (variant != null && !variant.isBlank()) {
                        selected.put(logical, variant);
                    }
                }
            }

            List<String> unlocked = yaml.getStringList(base + ".unlocked").stream().filter(v -> v != null && !v.isBlank()).toList();
            Map<String, Object> payload = new HashMap<>();
            payload.put("selected", selected);
            payload.put("unlocked", unlocked);
            if (overrideExisting) {
                skins.put(uuid, GSON.toJson(payload));
            } else {
                skins.putIfAbsent(uuid, GSON.toJson(payload));
            }

            String statsJson = yaml.getString(base + ".stats");
            if (statsJson != null && !statsJson.isBlank()) {
                if (overrideExisting) {
                    stats.put(uuid, statsJson);
                } else {
                    stats.putIfAbsent(uuid, statsJson);
                }
            }
        }
    }

    private void readLegacyLoadoutYaml(YamlConfiguration yaml, Map<UUID, String> loadouts, boolean overrideExisting) {
        ConfigurationSection players = yaml.getConfigurationSection("players");
        if (players == null) {
            return;
        }
        for (String key : players.getKeys(false)) {
            UUID uuid = parseUuid(key);
            if (uuid == null) {
                continue;
            }
            String base = "players." + key;

            Map<String, Object> json = new HashMap<>();
            json.put("hiderItems", sanitizeItems(yaml.getStringList(base + ".hider-items"), true));
            json.put("seekerItems", sanitizeItems(yaml.getStringList(base + ".seeker-items"), false));
            json.put("hiderLocked", yaml.getBoolean(base + ".lock-hider", false));
            json.put("seekerLocked", yaml.getBoolean(base + ".lock-seeker", false));
            json.put("selectedAdminPresetHider", yaml.getInt(base + ".selected-admin-preset.hider", 0));
            json.put("selectedAdminPresetSeeker", yaml.getInt(base + ".selected-admin-preset.seeker", 0));

            Map<String, Object> presets = new HashMap<>();
            for (int slot = 1; slot <= PlayerLoadout.MAX_PRESETS; slot++) {
                List<String> hider = sanitizeItems(yaml.getStringList(base + ".presets." + slot + ".hider"), true);
                List<String> seeker = sanitizeItems(yaml.getStringList(base + ".presets." + slot + ".seeker"), false);
                if (hider.isEmpty() && seeker.isEmpty()) {
                    continue;
                }
                Map<String, Object> preset = new HashMap<>();
                preset.put("hider", hider);
                preset.put("seeker", seeker);
                presets.put(String.valueOf(slot), preset);
            }
            json.put("presets", presets);
            if (overrideExisting) {
                loadouts.put(uuid, GSON.toJson(json));
            } else {
                loadouts.putIfAbsent(uuid, GSON.toJson(json));
            }
        }
    }

    private String sanitizeJson(String json) {
        return (json == null || json.isBlank()) ? "{}" : json;
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

    private UUID parseUuid(String raw) {
        try {
            return UUID.fromString(raw);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private String safePlayerName(UUID uuid) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
        String name = player.getName();
        return (name == null || name.isBlank()) ? uuid.toString() : name;
    }

    private void runOnMain(Runnable runnable) {
        Bukkit.getScheduler().runTask(plugin, runnable);
    }


    private record GlobalData(long coins, long xp, long wins, long losses) {
    }

    private static final class MigrationSource {
        private final Map<UUID, GlobalData> globalData = new HashMap<>();
        private final Map<UUID, String> skinsJson = new HashMap<>();
        private final Map<UUID, String> loadoutJson = new HashMap<>();
        private final Map<UUID, String> statsJson = new HashMap<>();

        private Set<UUID> allPlayerIds() {
            Set<UUID> all = new LinkedHashSet<>();
            all.addAll(globalData.keySet());
            all.addAll(skinsJson.keySet());
            all.addAll(loadoutJson.keySet());
            all.addAll(statsJson.keySet());
            return all;
        }
    }

    private static final class MigrationReport {
        private final AtomicInteger playersMigrated = new AtomicInteger();
        private final AtomicInteger fieldsMigrated = new AtomicInteger();
        private final Set<UUID> playerSuccess = ConcurrentHashMap.newKeySet();
        private final List<String> failures = java.util.Collections.synchronizedList(new ArrayList<>());
    }
}

