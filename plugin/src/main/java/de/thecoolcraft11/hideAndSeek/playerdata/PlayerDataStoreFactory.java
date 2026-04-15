package de.thecoolcraft11.hideAndSeek.playerdata;

import de.thecoolcraft11.minigameframework.MinigameFramework;
import de.thecoolcraft11.minigameframework.storage.sql.stats.GlobalStatsAPI;
import de.thecoolcraft11.minigameframework.storage.sql.stats.MinigameStatsAPI;

public final class PlayerDataStoreFactory {

    private PlayerDataStoreFactory() {
    }

    public static PlayerDataStore create(MinigameFramework framework) {
        if (framework.getGlobalStatsApi() == null) {
            framework.getLogger().warning("Database persistence disabled in config, using YAML PlayerDataStore");
            return new YamlPlayerDataStore(framework);
        }

        if (GlobalStatsAPI.isAvailable() && MinigameStatsAPI.isAvailable()) {
            framework.getLogger().info("Using database-backed PlayerDataStore");
            return new DatabasePlayerDataStore(framework);
        }

        framework.getLogger().warning("Framework database APIs unavailable, falling back to YAML PlayerDataStore");
        return new YamlPlayerDataStore(framework);
    }
}




