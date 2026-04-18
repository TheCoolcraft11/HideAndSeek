package de.thecoolcraft11.hideAndSeek.model;

public record SlotPreference(ItemType primary, ItemType fallback) {

    public static SlotPreference fromString(String str) {
        if (str == null || str.isEmpty()) {
            return null;
        }
        String[] parts = str.split(",");
        if (parts.length == 0) {
            return null;
        }
        try {
            ItemType primary = ItemType.valueOf(parts[0].trim());
            ItemType fallback = parts.length > 1 ? ItemType.valueOf(parts[1].trim()) : null;
            return new SlotPreference(primary, fallback);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public boolean hasFallback() {
        return fallback != null;
    }

    @Override
    public String toString() {
        return primary + (fallback != null ? "," + fallback : "");
    }
}

