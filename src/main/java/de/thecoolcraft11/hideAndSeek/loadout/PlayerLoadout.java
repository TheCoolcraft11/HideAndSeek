package de.thecoolcraft11.hideAndSeek.loadout;

import de.thecoolcraft11.hideAndSeek.items.LoadoutItemType;

import java.util.*;

public class PlayerLoadout {
    private final Set<LoadoutItemType> hiderItems = new LinkedHashSet<>();
    private final Set<LoadoutItemType> seekerItems = new LinkedHashSet<>();
    private final Map<LoadoutItemType, Integer> itemCosts = new HashMap<>();

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

    public void clear() {
        hiderItems.clear();
        seekerItems.clear();
        itemCosts.clear();
    }
}

