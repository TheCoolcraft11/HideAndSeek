package de.thecoolcraft11.hideAndSeek.block;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Tag;

import java.util.*;

public class BlockListParser {


    private static final Map<String, Set<Material>> suffixIndex = new HashMap<>();
    private static boolean indexed = false;


    private static void ensureIndexed() {
        if (indexed) return;

        for (Material mat : Material.values()) {
            if (!mat.isBlock()) continue;

            String name = mat.name();

            String[] parts = name.split("_");
            String lastPart = parts[parts.length - 1];

            suffixIndex.computeIfAbsent(lastPart, k -> new HashSet<>()).add(mat);
        }
        indexed = true;
    }

    public static Set<Material> parseBlockList(String blockListString) {
        if (blockListString == null || blockListString.isEmpty()) {
            return new HashSet<>();
        }

        Set<Material> materials = new HashSet<>();
        String[] parts = blockListString.split(",");

        for (String part : parts) {
            part = part.trim();
            if (part.isEmpty()) continue;

            if (part.startsWith("*")) {
                materials.addAll(parseVariantList(part));
            } else if (part.startsWith("#")) {
                materials.addAll(parseTag(part));
            } else {
                try {
                    Material mat = Material.valueOf(part.toUpperCase());
                    materials.add(mat);
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        return materials;
    }

    private static Set<Material> parseVariantList(String variantSpec) {
        ensureIndexed();

        int braceStart = variantSpec.indexOf('{');

        String suffix = (braceStart > 0)
                ? variantSpec.substring(1, braceStart).toUpperCase()
                : variantSpec.substring(1).toUpperCase();

        return new HashSet<>(suffixIndex.getOrDefault(suffix, Collections.emptySet()));
    }

    private static Set<Material> parseTag(String tagSpec) {
        String tagName = tagSpec.substring(1).toLowerCase();
        try {
            Tag<Material> tag = Bukkit.getTag(Tag.REGISTRY_BLOCKS, NamespacedKey.minecraft(tagName), Material.class);
            if (tag != null) return tag.getValues();
        } catch (Exception ignored) {
        }
        return new HashSet<>();
    }
}
