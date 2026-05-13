package de.thecoolcraft11.hideAndSeek.util.skin;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.model.GameModeEnum;
import de.thecoolcraft11.hideAndSeek.model.SkinData;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class SkinManager {
    private final HideAndSeek plugin;
    private final Map<String, SkinData> skinRegistry = new LinkedHashMap<>();
    private final Map<UUID, String> assignedSkins = new ConcurrentHashMap<>();
    private final Map<UUID, PlayerProfile> originalProfiles = new HashMap<>();

    public SkinManager(HideAndSeek plugin) {
        this.plugin = plugin;
    }

    public void loadSkins() {
        skinRegistry.clear();

        File file = new File(plugin.getDataFolder(), "skins.yml");

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);

        for (String key : config.getKeys(false)) {
            ConfigurationSection section = config.getConfigurationSection(key);
            if (section == null) {
                continue;
            }

            String name = section.getString("name", key);
            String iconName = section.getString("icon", "PLAYER_HEAD");
            String value = section.getString("value", "");
            String signature = section.getString("signature", "");

            if (value.isBlank()) {
                plugin.getLogger().warning("Skin '" + key + "' has no value, skipping.");
                continue;
            }
            if (signature.isBlank()) {
                plugin.getLogger().warning("Skin '" + key + "' has no signature, skipping.");
                continue;
            }

            Material icon = Material.matchMaterial(iconName.toUpperCase(Locale.ROOT));
            if (icon == null) {
                plugin.getLogger().warning(
                        "Skin '" + key + "' has invalid icon '" + iconName + "', defaulting to PLAYER_HEAD.");
                icon = Material.PLAYER_HEAD;
            }

            skinRegistry.put(key, new SkinData(key, name, icon, value, signature));
        }

        plugin.getLogger().info("Loaded " + skinRegistry.size() + " skins from skins.yml");
    }

    public List<SkinData> resolveSkins(List<String> skinIds) {
        List<SkinData> skinData = new ArrayList<>();
        for (String skinId : skinIds) {
            if (!skinRegistry.containsKey(skinId)) {
                plugin.getLogger().warning("Skin ID '" + skinId + "' is not registered in skins.yml");
                continue;
            }
            skinData.add(skinRegistry.get(skinId));
        }
        return skinData;
    }

    public String getAssignedSkinId(UUID playerId) {
        if (isNotSkinMode()) return null;
        return assignedSkins.get(playerId);
    }

    public SkinData getAssignedSkin(UUID playerId) {
        if (isNotSkinMode()) return null;
        return skinRegistry.get(assignedSkins.get(playerId));
    }

    public SkinData getSkinById(String id) {
        return skinRegistry.get(id);
    }

    public void assignSkin(Player player, SkinData skin) {
        if (isNotSkinMode()) return;
        if (player == null || skin == null) return;

        PlayerProfile profile = player.getPlayerProfile();

        String value = skin.value();
        String signature = skin.signature();

        assignedSkins.put(player.getUniqueId(), skin.id());

        profile.removeProperty("textures");

        profile.setProperty(new ProfileProperty("textures", value, signature));
        player.setPlayerProfile(profile);
        refreshPlayer(player);
    }

    public void resetSkin(Player player) {
        if (isNotSkinMode()) return;
        if (player == null) return;

        PlayerProfile profile = originalProfiles.get(player.getUniqueId());
        if (profile == null) {
            plugin.getLogger().warning("No original profile found for player " + player.getName());
            return;
        }
        assignedSkins.remove(player.getUniqueId());
        player.setPlayerProfile(profile);
        refreshPlayer(player);
    }

    public void setOriginalProfile(UUID uuid, PlayerProfile profile) {
        if (profile == null) {
            originalProfiles.remove(uuid);
            return;
        }
        originalProfiles.put(uuid, profile);
    }

    private void refreshPlayer(Player player) {

        for (Player viewer : Bukkit.getOnlinePlayers()) {
            if (viewer.equals(player)) continue;

            viewer.hidePlayer(plugin, player);

            Bukkit.getScheduler().runTaskLater(plugin, () -> viewer.showPlayer(plugin, player), 2L);
        }
    }

    private boolean isNotSkinMode() {
        var gameModeResult = plugin.getSettingService().getSetting("game.mode");
        Object gameModeObj = gameModeResult.isSuccess() ? gameModeResult.getValue() : GameModeEnum.NORMAL;
        GameModeEnum gameMode = (gameModeObj instanceof GameModeEnum) ?
                (GameModeEnum) gameModeObj : GameModeEnum.NORMAL;

        return gameMode != GameModeEnum.SKIN;
    }
}
