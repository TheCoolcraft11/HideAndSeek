package de.thecoolcraft11.hideAndSeek.loadout;

import de.thecoolcraft11.hideAndSeek.model.ItemType;
import de.thecoolcraft11.hideAndSeek.model.LoadoutItemType;

import java.util.*;

public class PlayerLoadout {
    public static final int MAX_PRESETS = 5;

    private final Set<LoadoutItemType> hiderItems = new LinkedHashSet<>();
    private final Set<LoadoutItemType> seekerItems = new LinkedHashSet<>();
    private final Map<LoadoutItemType, Integer> itemCosts = new HashMap<>();
    private final Map<Integer, Preset> presets = new HashMap<>();
    private final Map<Integer, ItemType> hiderSlotPreferences = new HashMap<>();
    private final Map<Integer, ItemType> seekerSlotPreferences = new HashMap<>();
    private int selectedHiderAdminPresetSlot;
    private int selectedSeekerAdminPresetSlot;
    private boolean hiderLocked;
    private boolean seekerLocked;

    public PlayerLoadout() {
    }

    public boolean addHiderItemForced(LoadoutItemType item, int cost) {
        if (!item.isForHiders()) {
            return false;
        }
        itemCosts.put(item, cost);
        return hiderItems.add(item);
    }

    public boolean addHiderItem(LoadoutItemType item, int maxItems, int maxTokens, int cost) {
        if (!item.isForHiders()) return false;
        if (hiderItems.size() >= maxItems) return false;
        if (getHiderTokensUsed() + cost > maxTokens) return false;
        if (hiderItems.contains(item)) return false;

        itemCosts.put(item, cost);
        return hiderItems.add(item);
    }

    public boolean addSeekerItem(LoadoutItemType item, int maxItems, int maxTokens, int cost) {
        if (!item.isForSeekers()) return false;
        if (seekerItems.size() >= maxItems) return false;
        if (getSeekerTokensUsed() + cost > maxTokens) return false;
        if (seekerItems.contains(item)) return false;

        itemCosts.put(item, cost);
        return seekerItems.add(item);
    }

    public void removeHiderItem(LoadoutItemType item) {
        hiderItems.remove(item);
        itemCosts.remove(item);
    }

    public void removeSeekerItem(LoadoutItemType item) {
        seekerItems.remove(item);
        itemCosts.remove(item);
    }

    public boolean addSeekerItemForced(LoadoutItemType item, int cost) {
        if (!item.isForSeekers()) {
            return false;
        }
        itemCosts.put(item, cost);
        return seekerItems.add(item);
    }

    public void clearHiderItems() {
        for (LoadoutItemType item : Set.copyOf(hiderItems)) {
            removeHiderItem(item);
        }
    }

    public void clearSeekerItems() {
        for (LoadoutItemType item : Set.copyOf(seekerItems)) {
            removeSeekerItem(item);
        }
    }

    public boolean isHiderLocked() {
        return hiderLocked;
    }

    public int getHiderTokensUsed() {
        return hiderItems.stream().mapToInt(item -> itemCosts.getOrDefault(item, item.getRarity().getDefaultCost())).sum();
    }

    public int getSeekerTokensUsed() {
        return seekerItems.stream().mapToInt(item -> itemCosts.getOrDefault(item, item.getRarity().getDefaultCost())).sum();
    }

    public Set<LoadoutItemType> getHiderItems() {
        return Collections.unmodifiableSet(hiderItems);
    }

    public Set<LoadoutItemType> getSeekerItems() {
        return Collections.unmodifiableSet(seekerItems);
    }

    public void setHiderLocked(boolean hiderLocked) {
        this.hiderLocked = hiderLocked;
    }

    public boolean isSeekerLocked() {
        return seekerLocked;
    }

    public void setSeekerLocked(boolean seekerLocked) {
        this.seekerLocked = seekerLocked;
    }

    public void savePreset(int presetSlot) {
        if (presetSlot < 1 || presetSlot > MAX_PRESETS) {
            return;
        }
        presets.put(presetSlot, new Preset(hiderItems, seekerItems));
    }

    public void setPreset(int presetSlot, Collection<LoadoutItemType> hider, Collection<LoadoutItemType> seeker) {
        if (presetSlot < 1 || presetSlot > MAX_PRESETS) {
            return;
        }
        presets.put(presetSlot, new Preset(hider, seeker));
    }

    public void clearPreset(int presetSlot) {
        presets.remove(presetSlot);
    }

    public boolean hasPreset(int presetSlot) {
        return presets.containsKey(presetSlot);
    }

    public Preset getPreset(int presetSlot) {
        Preset preset = presets.get(presetSlot);
        return preset != null ? preset : new Preset();
    }

    public int getSelectedAdminPresetSlot(LoadoutRole role) {
        return role == LoadoutRole.HIDER ? selectedHiderAdminPresetSlot : selectedSeekerAdminPresetSlot;
    }

    public void setSelectedAdminPresetSlot(LoadoutRole role, int slot) {
        int normalized = Math.max(0, slot);
        if (role == LoadoutRole.HIDER) {
            selectedHiderAdminPresetSlot = normalized;
            return;
        }
        selectedSeekerAdminPresetSlot = normalized;
    }

    public void setHiderSlotPreference(int slot, ItemType itemType) {
        if (slot < 0 || slot > 8) return;
        if (itemType == null) {
            hiderSlotPreferences.remove(slot);
        } else {
            hiderSlotPreferences.put(slot, itemType);
        }
    }

    public void setSeekerSlotPreference(int slot, ItemType itemType) {
        if (slot < 0 || slot > 8) return;
        if (itemType == null) {
            seekerSlotPreferences.remove(slot);
        } else {
            seekerSlotPreferences.put(slot, itemType);
        }
    }

    public ItemType getHiderSlotPreference(int slot) {
        return hiderSlotPreferences.getOrDefault(slot, null);
    }

    public ItemType getSeekerSlotPreference(int slot) {
        return seekerSlotPreferences.getOrDefault(slot, null);
    }

    public Map<Integer, ItemType> getHiderSlotPreferences() {
        return Collections.unmodifiableMap(hiderSlotPreferences);
    }

    public Map<Integer, ItemType> getSeekerSlotPreferences() {
        return Collections.unmodifiableMap(seekerSlotPreferences);
    }

    public void clearHiderSlotPreferences() {
        hiderSlotPreferences.clear();
    }

    public void clearSeekerSlotPreferences() {
        seekerSlotPreferences.clear();
    }

    public static class Preset {
        public final Set<LoadoutItemType> hiderItems;
        public final Set<LoadoutItemType> seekerItems;

        public Preset(Collection<LoadoutItemType> hider, Collection<LoadoutItemType> seeker) {
            this.hiderItems = new LinkedHashSet<>(hider == null ? Set.of() : hider);
            this.seekerItems = new LinkedHashSet<>(seeker == null ? Set.of() : seeker);
        }

        public Preset() {
            this.hiderItems = new LinkedHashSet<>();
            this.seekerItems = new LinkedHashSet<>();
        }
    }
}

