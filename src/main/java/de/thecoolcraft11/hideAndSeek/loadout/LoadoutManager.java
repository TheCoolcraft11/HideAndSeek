package de.thecoolcraft11.hideAndSeek.loadout;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.items.ItemRarity;
import de.thecoolcraft11.hideAndSeek.items.LoadoutItemType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LoadoutManager {
    private final HideAndSeek plugin;
    private final Map<UUID, PlayerLoadout> loadouts = new HashMap<>();

    public LoadoutManager(HideAndSeek plugin) {
        this.plugin = plugin;
    }

    public PlayerLoadout getLoadout(UUID playerId) {
        return loadouts.computeIfAbsent(playerId, k -> new PlayerLoadout());
    }

    public int getMaxHiderItems() {
        return plugin.getSettingRegistry().get("loadout.hider-max-items", 3);
    }

    public int getMaxSeekerItems() {
        return plugin.getSettingRegistry().get("loadout.seeker-max-items", 4);
    }

    public int getMaxHiderTokens() {
        return plugin.getSettingRegistry().get("loadout.hider-max-tokens", 12);
    }

    public int getMaxSeekerTokens() {
        return plugin.getSettingRegistry().get("loadout.seeker-max-tokens", 12);
    }

    public int getRarityCost(ItemRarity rarity) {
        String settingKey = switch (rarity) {
            case COMMON -> "loadout.token-cost-common";
            case UNCOMMON -> "loadout.token-cost-uncommon";
            case RARE -> "loadout.token-cost-rare";
            case EPIC -> "loadout.token-cost-epic";
            case LEGENDARY -> "loadout.token-cost-legendary";
        };
        return plugin.getSettingRegistry().get(settingKey, rarity.getDefaultCost());
    }

    public int getItemCost(LoadoutItemType item) {
        return getRarityCost(item.getRarity());
    }

    public boolean canModifyLoadout() {
        String currentPhase = plugin.getStateManager().getCurrentPhaseId();
        return "lobby".equalsIgnoreCase(currentPhase);
    }

    public void clearAll() {
        loadouts.clear();
    }
}


