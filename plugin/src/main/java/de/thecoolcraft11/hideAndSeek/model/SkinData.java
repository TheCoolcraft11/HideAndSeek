package de.thecoolcraft11.hideAndSeek.model;

import org.bukkit.Material;
import org.jspecify.annotations.NonNull;

public record SkinData(String id, String name, Material icon, String value, String signature) {
    @Override
    public @NonNull String toString() {
        return "SkinData{id='" + id + "', name='" + name + "'}";
    }
}