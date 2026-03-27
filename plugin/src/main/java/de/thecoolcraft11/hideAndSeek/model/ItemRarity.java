package de.thecoolcraft11.hideAndSeek.model;

public enum ItemRarity {
    COMMON(1),
    UNCOMMON(2),
    RARE(4),
    EPIC(6),
    LEGENDARY(10);

    private final int defaultCost;

    ItemRarity(int defaultCost) {
        this.defaultCost = defaultCost;
    }

    public int getDefaultCost() {
        return defaultCost;
    }
}

