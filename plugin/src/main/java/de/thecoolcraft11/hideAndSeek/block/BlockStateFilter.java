package de.thecoolcraft11.hideAndSeek.block;

import java.util.HashSet;
import java.util.Set;

public class BlockStateFilter {
    private static final Set<String> DISALLOWED_PROPERTIES = new HashSet<>();

    public static void addDisallowedProperty(String property) {
        if (property != null) {
            DISALLOWED_PROPERTIES.add(property);
        }
    }

    public static boolean isDisallowed(String property) {
        if (property == null) return false;
        return DISALLOWED_PROPERTIES.contains(property);
    }


    public static void clear() {
        DISALLOWED_PROPERTIES.clear();
    }
}
