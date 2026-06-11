package de.thecoolcraft11.hideAndSeek.loadout;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.items.HiderItems;
import de.thecoolcraft11.hideAndSeek.items.SeekerItems;
import de.thecoolcraft11.hideAndSeek.model.ItemRarity;
import de.thecoolcraft11.hideAndSeek.model.LoadoutItemType;
import de.thecoolcraft11.hideAndSeek.perk.definition.PerkTarget;
import de.thecoolcraft11.hideAndSeek.util.DataController;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;

public class LoadoutManager {
    public boolean isRoleLocked(UUID playerId, LoadoutRole role) {
        PlayerLoadout loadout = getLoadout(playerId);
        return role == LoadoutRole.HIDER ? loadout.isHiderLocked() : loadout.isSeekerLocked();
    }

    private final HideAndSeek plugin;
    private final Map<UUID, PlayerLoadout> loadouts = new HashMap<>();

    public LoadoutManager(HideAndSeek plugin) {
        this.plugin = plugin;
    }

    public PlayerLoadout getLoadout(UUID playerId) {
        return loadouts.computeIfAbsent(playerId, k -> {
            PlayerLoadout loadout = LoadoutDataService.getLoadout(playerId);
            return loadout != null ? loadout : new PlayerLoadout();
        });
    }

    public void saveLoadout(UUID playerId) {
        if (!plugin.getConfig().getBoolean("persistence.save-loadout-data", true)) {
            return;
        }
        LoadoutDataService.savePlayer(plugin, playerId);
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

    public void setRoleLocked(UUID playerId, LoadoutRole role, boolean locked) {
        PlayerLoadout loadout = getLoadout(playerId);
        if (role == LoadoutRole.HIDER) {
            loadout.setHiderLocked(locked);
        } else {
            loadout.setSeekerLocked(locked);
        }
        saveLoadout(playerId);
    }

    public LoadoutFilterMode getFilterMode(LoadoutRole role) {
        return LoadoutDataService.getFilterMode(role);
    }

    public void setFilterMode(LoadoutRole role, LoadoutFilterMode mode) {
        LoadoutDataService.setFilterMode(role, mode);
        LoadoutDataService.saveAdminPolicy(plugin);
    }

    public Set<LoadoutItemType> getFilterItems(LoadoutRole role) {
        return LoadoutDataService.getFilterItems(role);
    }

    public void toggleRoleFilterItem(LoadoutRole role, LoadoutItemType item) {
        Set<LoadoutItemType> items = EnumSet.copyOf(getFilterItems(role));
        if (items.contains(item)) {
            items.remove(item);
        } else {
            items.add(item);
        }
        LoadoutDataService.setFilterItems(role, items);
        LoadoutDataService.saveAdminPolicy(plugin);
    }

    public void clearRoleFilter(LoadoutRole role) {
        LoadoutDataService.setFilterItems(role, EnumSet.noneOf(LoadoutItemType.class));
        LoadoutDataService.saveAdminPolicy(plugin);
    }

    public boolean isItemAvailableForRole(LoadoutRole role, LoadoutItemType item) {
        if (role == LoadoutRole.HIDER && !item.isForHiders()) {
            return false;
        }
        if (role == LoadoutRole.SEEKER && !item.isForSeekers()) {
            return false;
        }
        if (!item.isSupported(plugin.getNmsAdapter())) {
            return false;
        }

        Set<LoadoutItemType> filtered = getFilterItems(role);
        LoadoutFilterMode mode = getFilterMode(role);
        return (mode == LoadoutFilterMode.BLACKLIST) != filtered.contains(item);
    }

    public Set<String> getDisabledPerks(LoadoutRole role) {
        return LoadoutDataService.getDisabledPerks(role);
    }

    public void toggleDisabledPerk(LoadoutRole role, String perkId) {
        Set<String> perkIds = new java.util.HashSet<>(getDisabledPerks(role));
        if (perkIds.contains(perkId)) {
            perkIds.remove(perkId);
        } else {
            perkIds.add(perkId);
        }
        LoadoutDataService.setDisabledPerks(role, perkIds);
        LoadoutDataService.saveAdminPolicy(plugin);
    }

    public boolean isPerkAllowed(PerkTarget target, String perkId) {
        LoadoutRole role = target == PerkTarget.HIDER ? LoadoutRole.HIDER : target == PerkTarget.SEEKER ? LoadoutRole.SEEKER : null;
        if (role == null) {
            return true;
        }
        if (getDisabledPerks(role).contains(perkId)) {
            return false;
        }

        int forcedSlot = getForcedRolePresetSlot(role);
        if (forcedSlot > 0) {
            AdminRolePreset forced = LoadoutDataService.getAdminPresetOrNull(role, forcedSlot);
            return forced == null || !forced.getDisabledPerks().contains(perkId);
        }
        return true;
    }

    public Set<UUID> getKnownPlayerIds() {
        return LoadoutDataService.getAllKnownPlayerIds();
    }

    public void resetPlayerLoadout(UUID playerId, LoadoutRole role) {
        PlayerLoadout loadout = getLoadout(playerId);
        if (role == LoadoutRole.HIDER) {
            loadout.clearHiderItems();
        } else {
            loadout.clearSeekerItems();
        }
        saveLoadout(playerId);
    }

    public void resetPlayerLoadout(UUID playerId) {
        PlayerLoadout loadout = getLoadout(playerId);
        loadout.clearHiderItems();
        loadout.clearSeekerItems();
        saveLoadout(playerId);
    }

    public int resetAllLoadouts(LoadoutRole role) {
        int changed = 0;
        for (UUID playerId : getKnownPlayerIds()) {
            PlayerLoadout loadout = getLoadout(playerId);
            int before = role == LoadoutRole.HIDER ? loadout.getHiderItems().size() : loadout.getSeekerItems().size();
            resetPlayerLoadout(playerId, role);
            if (before > 0) {
                changed++;
            }
        }
        return changed;
    }

    public int enforcePoliciesAndNotify() {
        for (UUID playerId : getKnownPlayerIds()) {
            applyAdminPresetPolicy(playerId);
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            int removed = sanitizePlayerLoadout(player.getUniqueId());
            boolean presetAdjusted = applyAdminPresetPolicy(player.getUniqueId());
            if (removed > 0 || presetAdjusted) {
                player.sendMessage(plugin.trText(player, "loadout.updated_by_admin", java.util.Map.of("count", String.valueOf(removed))));
                refreshRoleInventory(player);
            }
        }

        plugin.getPerkRegistry().selectRoundPerks();
        plugin.getPerkShopGUI().refreshAllPlayersWithShopItems();
        return Bukkit.getOnlinePlayers().size();
    }

    public int sanitizePlayerLoadout(UUID playerId) {
        PlayerLoadout loadout = getLoadout(playerId);
        int removed = 0;
        for (LoadoutItemType item : Set.copyOf(loadout.getHiderItems())) {
            if (!isItemAvailableForRole(LoadoutRole.HIDER, item)) {
                loadout.removeHiderItem(item);
                removed++;
            }
        }
        for (LoadoutItemType item : Set.copyOf(loadout.getSeekerItems())) {
            if (!isItemAvailableForRole(LoadoutRole.SEEKER, item)) {
                loadout.removeSeekerItem(item);
                removed++;
            }
        }
        if (removed > 0) {
            saveLoadout(playerId);
        }
        return removed;
    }

    public AdminRolePreset getAdminPreset(LoadoutRole role, int slot) {
        return LoadoutDataService.getAdminPreset(role, slot);
    }


    public boolean isAdminPresetEnabled(LoadoutRole role, int slot) {
        AdminRolePreset preset = LoadoutDataService.getAdminPresetOrNull(role, slot);
        return preset != null && preset.isEnabled();
    }

    public Set<Integer> getAdminPresetSlots(LoadoutRole role) {
        return LoadoutDataService.getAdminPresetSlots(role);
    }

    public List<Integer> getEnabledAdminPresetSlots(LoadoutRole role) {
        List<Integer> slots = new ArrayList<>();
        for (int slot : getAdminPresetSlots(role)) {
            if (isAdminPresetEnabled(role, slot)) {
                slots.add(slot);
            }
        }
        slots.sort(Comparator.naturalOrder());
        return slots;
    }

    public void setAdminPresetEnabled(LoadoutRole role, int slot, boolean enabled) {
        AdminRolePreset preset = getAdminPreset(role, slot);
        preset.setEnabled(enabled);
        LoadoutDataService.saveAdminPolicy(plugin);
    }

    public void toggleAdminPresetItem(LoadoutRole role, int slot, LoadoutItemType item) {
        if ((role == LoadoutRole.HIDER && !item.isForHiders()) || (role == LoadoutRole.SEEKER && !item.isForSeekers())) {
            return;
        }
        AdminRolePreset preset = getAdminPreset(role, slot);
        if (preset.getItems().contains(item)) {
            preset.getItems().remove(item);
        } else {
            preset.getItems().add(item);
        }
        LoadoutDataService.saveAdminPolicy(plugin);
    }


    public void deleteAdminPreset(LoadoutRole role, int slot) {
        LoadoutDataService.clearAdminPreset(role, slot);
        for (UUID playerId : getKnownPlayerIds()) {
            PlayerLoadout loadout = getLoadout(playerId);
            if (loadout.getSelectedAdminPresetSlot(role) == slot) {
                loadout.setSelectedAdminPresetSlot(role, 0);
                saveLoadout(playerId);
            }
        }
        LoadoutDataService.saveAdminPolicy(plugin);
    }

    public boolean isRoleRestrictedToAdminPresets(LoadoutRole role) {
        return LoadoutDataService.isRoleRestrictedToAdminPresets(role);
    }

    public void setRoleRestrictedToAdminPresets(LoadoutRole role, boolean restricted) {
        LoadoutDataService.setRoleRestrictedToAdminPresets(role, restricted);
        LoadoutDataService.saveAdminPolicy(plugin);
    }

    public int getForcedRolePresetSlot(LoadoutRole role) {
        return LoadoutDataService.getForcedRolePresetSlot(role);
    }

    public void setForcedRolePresetSlot(LoadoutRole role, int slot) {
        LoadoutDataService.setForcedRolePresetSlot(role, slot);
        LoadoutDataService.saveAdminPolicy(plugin);
    }

    public boolean applyAdminPresetToPlayer(UUID playerId, LoadoutRole role, int slot) {
        if (!isAdminPresetEnabled(role, slot)) {
            return false;
        }
        AdminRolePreset preset = LoadoutDataService.getAdminPresetOrNull(role, slot);
        if (preset == null) {
            return false;
        }

        PlayerLoadout loadout = getLoadout(playerId);
        boolean changed = applyRoleItems(loadout, role, preset.getItems());
        loadout.setSelectedAdminPresetSlot(role, slot);
        saveLoadout(playerId);
        return changed;
    }

    public void clearPlayerSelectedAdminPreset(UUID playerId, LoadoutRole role) {
        PlayerLoadout loadout = getLoadout(playerId);
        if (loadout.getSelectedAdminPresetSlot(role) == 0) {
            return;
        }
        loadout.setSelectedAdminPresetSlot(role, 0);
        saveLoadout(playerId);
    }

    public void refreshRoleInventory(Player player) {
        UUID playerId = player.getUniqueId();
        if (DataController.getInstance().getHiders().contains(playerId)) {
            HiderItems.giveLoadoutItems(player, plugin);
            return;
        }
        if (DataController.getInstance().getSeekers().contains(playerId)) {
            SeekerItems.giveLoadoutItems(player, plugin);
        }
    }

    public void savePreset(UUID playerId, int presetSlot) {
        if (presetSlot < 1 || presetSlot > PlayerLoadout.MAX_PRESETS) {
            return;
        }

        PlayerLoadout loadout = getLoadout(playerId);
        loadout.savePreset(presetSlot);
        saveLoadout(playerId);
    }

    public boolean deletePreset(UUID playerId, int presetSlot) {
        PlayerLoadout loadout = getLoadout(playerId);
        boolean existed = loadout.hasPreset(presetSlot);
        loadout.clearPreset(presetSlot);
        saveLoadout(playerId);
        return existed;
    }

    public PlayerLoadout.Preset getPreset(UUID playerId, int presetSlot) {
        return getLoadout(playerId).getPreset(presetSlot);
    }

    public boolean hasPreset(UUID playerId, int presetSlot) {
        return getLoadout(playerId).hasPreset(presetSlot);
    }

    public PresetLoadResult analyzePresetLoad(UUID playerId, int presetSlot) {
        return evaluatePresetLoad(playerId, presetSlot, false);
    }

    public PresetLoadResult loadPreset(UUID playerId, int presetSlot) {
        return evaluatePresetLoad(playerId, presetSlot, true);
    }

    private PresetLoadResult evaluatePresetLoad(UUID playerId, int presetSlot, boolean apply) {
        PlayerLoadout loadout = getLoadout(playerId);
        if (!loadout.hasPreset(presetSlot)) {
            return new PresetLoadResult(false, 0, 0, 0, 0);
        }

        PlayerLoadout.Preset preset = loadout.getPreset(presetSlot);
        int totalItems = preset.hiderItems.size() + preset.seekerItems.size();

        if (apply) {
            LinkedHashSet<LoadoutItemType> acceptedHider = new LinkedHashSet<>();
            LinkedHashSet<LoadoutItemType> acceptedSeeker = new LinkedHashSet<>();

            int hiderUsedTokens = 0;
            int seekerUsedTokens = 0;
            int blockedByPolicy = 0;
            int blockedByLimits = 0;

            for (LoadoutItemType item : preset.hiderItems) {
                if (!isItemAvailableForRole(LoadoutRole.HIDER, item)) {
                    blockedByPolicy++;
                    continue;
                }
                int cost = getItemCost(item);
                if (acceptedHider.size() >= getMaxHiderItems() || hiderUsedTokens + cost > getMaxHiderTokens()) {
                    blockedByLimits++;
                    continue;
                }
                acceptedHider.add(item);
                hiderUsedTokens += cost;
            }

            for (LoadoutItemType item : preset.seekerItems) {
                if (!isItemAvailableForRole(LoadoutRole.SEEKER, item)) {
                    blockedByPolicy++;
                    continue;
                }
                int cost = getItemCost(item);
                if (acceptedSeeker.size() >= getMaxSeekerItems() || seekerUsedTokens + cost > getMaxSeekerTokens()) {
                    blockedByLimits++;
                    continue;
                }
                acceptedSeeker.add(item);
                seekerUsedTokens += cost;
            }

            loadout.clearHiderItems();
            loadout.clearSeekerItems();
            for (LoadoutItemType item : acceptedHider) {
                loadout.addHiderItemForced(item, getItemCost(item));
            }
            for (LoadoutItemType item : acceptedSeeker) {
                loadout.addSeekerItemForced(item, getItemCost(item));
            }
            saveLoadout(playerId);

            int appliedItems = acceptedHider.size() + acceptedSeeker.size();
            return new PresetLoadResult(true, totalItems, appliedItems, blockedByPolicy, blockedByLimits);
        }


        int canLoadHider = 0;
        int canLoadSeeker = 0;
        int hiderBlocked = 0;
        int seekerBlocked = 0;

        int hiderUsedTokens = 0;
        int seekerUsedTokens = 0;
        for (LoadoutItemType item : preset.hiderItems) {
            if (!isItemAvailableForRole(LoadoutRole.HIDER, item)) {
                hiderBlocked++;
                continue;
            }
            int cost = getItemCost(item);
            if (canLoadHider >= getMaxHiderItems() || hiderUsedTokens + cost > getMaxHiderTokens()) {
                hiderBlocked++;
                continue;
            }
            canLoadHider++;
            hiderUsedTokens += cost;
        }

        for (LoadoutItemType item : preset.seekerItems) {
            if (!isItemAvailableForRole(LoadoutRole.SEEKER, item)) {
                seekerBlocked++;
                continue;
            }
            int cost = getItemCost(item);
            if (canLoadSeeker >= getMaxSeekerItems() || seekerUsedTokens + cost > getMaxSeekerTokens()) {
                seekerBlocked++;
                continue;
            }
            canLoadSeeker++;
            seekerUsedTokens += cost;
        }

        return new PresetLoadResult(true, totalItems, canLoadHider + canLoadSeeker, hiderBlocked + seekerBlocked, 0);
    }

    private boolean applyAdminPresetPolicy(UUID playerId) {
        PlayerLoadout loadout = getLoadout(playerId);
        boolean changed = false;
        for (LoadoutRole role : LoadoutRole.values()) {
            int forcedSlot = getForcedRolePresetSlot(role);
            if (forcedSlot > 0 && isAdminPresetEnabled(role, forcedSlot)) {
                AdminRolePreset forced = LoadoutDataService.getAdminPresetOrNull(role, forcedSlot);
                if (forced != null) {
                    changed |= applyRoleItems(loadout, role, forced.getItems());
                    if (loadout.getSelectedAdminPresetSlot(role) != forcedSlot) {
                        loadout.setSelectedAdminPresetSlot(role, forcedSlot);
                        changed = true;
                    }
                }
                continue;
            }

            int selectedSlot = loadout.getSelectedAdminPresetSlot(role);
            if (selectedSlot > 0 && !isAdminPresetEnabled(role, selectedSlot)) {
                int fallback = getEnabledAdminPresetSlots(role).stream().findFirst().orElse(0);
                if (fallback > 0) {
                    AdminRolePreset fallbackPreset = LoadoutDataService.getAdminPresetOrNull(role, fallback);
                    if (fallbackPreset != null) {
                        applyRoleItems(loadout, role, fallbackPreset.getItems());
                    }
                    loadout.setSelectedAdminPresetSlot(role, fallback);
                } else {
                    clearRoleItems(loadout, role);
                    loadout.setSelectedAdminPresetSlot(role, 0);
                }
                changed = true;
            }

            if (isRoleRestrictedToAdminPresets(role)) {
                int active = loadout.getSelectedAdminPresetSlot(role);
                if (active <= 0 || !isAdminPresetEnabled(role, active)) {
                    int fallback = getEnabledAdminPresetSlots(role).stream().findFirst().orElse(0);
                    if (fallback > 0) {
                        AdminRolePreset fallbackPreset = LoadoutDataService.getAdminPresetOrNull(role, fallback);
                        if (fallbackPreset != null) {
                            applyRoleItems(loadout, role, fallbackPreset.getItems());
                        }
                        loadout.setSelectedAdminPresetSlot(role, fallback);
                    } else {
                        clearRoleItems(loadout, role);
                        loadout.setSelectedAdminPresetSlot(role, 0);
                    }
                    changed = true;
                }
            }
        }

        if (changed) {
            saveLoadout(playerId);
        }
        return changed;
    }

    private boolean applyRoleItems(PlayerLoadout loadout, LoadoutRole role, Set<LoadoutItemType> items) {
        boolean changed = clearRoleItems(loadout, role);
        for (LoadoutItemType item : items) {
            if (!isItemAvailableForRole(role, item)) {
                continue;
            }
            changed |= role == LoadoutRole.HIDER
                    ? loadout.addHiderItemForced(item, getItemCost(item))
                    : loadout.addSeekerItemForced(item, getItemCost(item));
        }
        return changed;
    }

    private boolean clearRoleItems(PlayerLoadout loadout, LoadoutRole role) {
        int before = role == LoadoutRole.HIDER ? loadout.getHiderItems().size() : loadout.getSeekerItems().size();
        if (role == LoadoutRole.HIDER) {
            loadout.clearHiderItems();
        } else {
            loadout.clearSeekerItems();
        }
        return before > 0;
    }

    public boolean isGlobalLoadoutLocked() {
        return LoadoutDataService.isGlobalLoadoutLock();
    }

    public void setGlobalLoadoutLocked(boolean locked) {
        LoadoutDataService.setGlobalLoadoutLock(locked);
        LoadoutDataService.saveAdminPolicy(plugin);
    }

    public record PresetLoadResult(boolean presetExists, int totalItems, int appliedItems, int blockedByPolicy,
                                   int blockedByLimits) {
        public boolean isFullyApplied() {
            return presetExists && totalItems > 0 && appliedItems == totalItems;
        }


        public boolean isEmptyPreset() {
            return presetExists && totalItems == 0;
        }
    }

}
