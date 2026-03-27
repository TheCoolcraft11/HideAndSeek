package de.thecoolcraft11.hideAndSeek.block;

import org.bukkit.Material;
import org.bukkit.block.data.BlockData;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BlockAppearanceConfig {

    private static final Pattern COMPLEX_PATTERN = Pattern.compile("([({])([^})]+)([})])\\{([^}]+)}(?:\\[([^]]+)])?");

    private static final Pattern VARIANT_PATTERN = Pattern.compile("\\*(\\w+)\\{(\\w+)}(?:\\[([^]]+)])?");

    private static final Pattern TAG_WITH_DEFAULT_PATTERN = Pattern.compile("#(\\w+)\\{(\\w+)}(?:\\[([^]]+)])?");

    private static final Pattern BLOCKSTATE_ALL_PATTERN = Pattern.compile("(\\w+)\\[\\*]");
    private static final Pattern BLOCKSTATE_PATTERN = Pattern.compile("(\\w+)\\[([^]]+)]");

    private static final Pattern CUSTOM_LIST_PATTERN = Pattern.compile("\\{([^}]+)}");

    private static final Pattern complexNoDefaultPattern = Pattern.compile("([({])([^})]+)([})])(?:\\[([^]]+)])?");
    private static final Pattern tagOnlyPattern = Pattern.compile("#(\\w+)(?:\\[([^]]+)])?");

    private String baseBlockType;
    private String defaultVariant;
    private boolean allowAllVariants;
    private boolean allowAllBlockStates;
    private final Set<String> allowedProperties;
    private final Map<String, Set<String>> allowedStates;
    private Set<Material> customMaterials;
    private boolean showAllVariantsInSelector;
    private boolean hasVariantGroup;

    private BlockAppearanceConfig() {
        this.allowedStates = new HashMap<>();
        this.allowedProperties = new HashSet<>();
        this.showAllVariantsInSelector = false;
        this.hasVariantGroup = false;
    }

    public static BlockAppearanceConfig parse(String pattern) {
        if (pattern == null || pattern.isEmpty()) {
            return null;
        }

        BlockAppearanceConfig config = new BlockAppearanceConfig();


        Matcher complexMatcher = COMPLEX_PATTERN.matcher(pattern);
        if (complexMatcher.matches()) {
            String materialsList = complexMatcher.group(2);
            String defaultMaterial = complexMatcher.group(4);
            String blockstates = complexMatcher.group(5);

            config.customMaterials = BlockListParser.parseBlockList(materialsList);
            if (!config.customMaterials.isEmpty()) {
                config.baseBlockType = "CUSTOM_LIST";
                config.defaultVariant = defaultMaterial.toUpperCase();
                config.allowAllVariants = true;
                config.hasVariantGroup = true;
                config.showAllVariantsInSelector = false;


                if (blockstates != null && !blockstates.isEmpty()) {
                    if (blockstates.equals("*")) {
                        config.allowAllBlockStates = true;
                    } else {
                        config.allowAllBlockStates = false;
                        parseBlockStates(config, blockstates);
                    }
                } else {
                    config.allowAllBlockStates = true;
                }

                return validateAndReturn(config);
            }
        }

        Matcher complexNoDefaultMatcher = complexNoDefaultPattern.matcher(pattern);
        if (complexNoDefaultMatcher.matches() && !pattern.contains("{") || (pattern.indexOf("{") != pattern.lastIndexOf("{"))) {

            String materialsList = complexNoDefaultMatcher.group(2);
            String blockstates = complexNoDefaultMatcher.group(4);

            config.customMaterials = BlockListParser.parseBlockList(materialsList);
            if (!config.customMaterials.isEmpty()) {
                config.baseBlockType = "CUSTOM_LIST";
                config.allowAllVariants = false;
                config.hasVariantGroup = false;
                config.showAllVariantsInSelector = true;


                if (blockstates != null && !blockstates.isEmpty()) {
                    if (blockstates.equals("*")) {
                        config.allowAllBlockStates = true;
                    } else {
                        config.allowAllBlockStates = false;
                        parseBlockStates(config, blockstates);
                    }
                } else {
                    config.allowAllBlockStates = true;
                }

                return validateAndReturn(config);
            }
        }


        Matcher tagDefaultMatcher = TAG_WITH_DEFAULT_PATTERN.matcher(pattern);
        if (tagDefaultMatcher.matches()) {
            String tagName = tagDefaultMatcher.group(1);
            String defaultMaterial = tagDefaultMatcher.group(2);
            String blockstates = tagDefaultMatcher.group(3);

            config.customMaterials = BlockListParser.parseBlockList("#" + tagName);
            if (!config.customMaterials.isEmpty()) {
                config.baseBlockType = "TAG_" + tagName.toUpperCase();
                config.defaultVariant = defaultMaterial.toUpperCase();
                config.allowAllVariants = true;
                config.hasVariantGroup = true;
                config.showAllVariantsInSelector = false;


                if (blockstates != null && !blockstates.isEmpty()) {
                    if (blockstates.equals("*")) {
                        config.allowAllBlockStates = true;
                    } else {
                        config.allowAllBlockStates = false;
                        parseBlockStates(config, blockstates);
                    }
                } else {
                    config.allowAllBlockStates = true;
                }

                return validateAndReturn(config);
            }
        }

        Matcher tagOnlyMatcher = tagOnlyPattern.matcher(pattern);
        if (tagOnlyMatcher.matches() && !pattern.contains("{")) {
            String tagName = tagOnlyMatcher.group(1);
            String blockstates = tagOnlyMatcher.group(2);

            config.customMaterials = BlockListParser.parseBlockList("#" + tagName);
            if (!config.customMaterials.isEmpty()) {
                config.baseBlockType = "TAG_" + tagName.toUpperCase();
                config.allowAllVariants = false;
                config.hasVariantGroup = false;
                config.showAllVariantsInSelector = true;


                if (blockstates != null && !blockstates.isEmpty()) {
                    if (blockstates.equals("*")) {
                        config.allowAllBlockStates = true;
                    } else {
                        config.allowAllBlockStates = false;
                        parseBlockStates(config, blockstates);
                    }
                } else {
                    config.allowAllBlockStates = true;
                }

                return validateAndReturn(config);
            }
        }


        Matcher variantMatcher = VARIANT_PATTERN.matcher(pattern);
        if (variantMatcher.matches()) {
            config.baseBlockType = variantMatcher.group(1);
            config.defaultVariant = variantMatcher.group(2);
            config.allowAllVariants = true;
            String blockstates = variantMatcher.group(3);


            if (blockstates != null && !blockstates.isEmpty()) {
                if (blockstates.equals("*")) {
                    config.allowAllBlockStates = true;
                } else {
                    config.allowAllBlockStates = false;
                    parseBlockStates(config, blockstates);
                }
            } else {
                config.allowAllBlockStates = true;
            }

            return validateAndReturn(config);
        }


        Matcher customListMatcher = CUSTOM_LIST_PATTERN.matcher(pattern);
        if (customListMatcher.matches()) {
            config.customMaterials = BlockListParser.parseBlockList(customListMatcher.group(1));
            if (!config.customMaterials.isEmpty()) {
                config.baseBlockType = "CUSTOM_LIST";
                config.allowAllVariants = false;
                config.allowAllBlockStates = true;
                config.hasVariantGroup = false;
                config.showAllVariantsInSelector = true;
                return validateAndReturn(config);
            }
        }


        Matcher blockstateAllMatcher = BLOCKSTATE_ALL_PATTERN.matcher(pattern);
        if (blockstateAllMatcher.matches()) {
            config.baseBlockType = blockstateAllMatcher.group(1);
            config.allowAllBlockStates = true;
            config.allowAllVariants = false;
            return validateAndReturn(config);
        }


        Matcher blockstateMatcher = BLOCKSTATE_PATTERN.matcher(pattern);
        if (blockstateMatcher.matches()) {
            config.baseBlockType = blockstateMatcher.group(1);
            config.allowAllBlockStates = false;
            config.allowAllVariants = false;
            String states = blockstateMatcher.group(2);
            parseBlockStates(config, states);
            return validateAndReturn(config);
        }


        try {
            Material.valueOf(pattern);
            config.baseBlockType = pattern;
            config.allowAllVariants = false;
            config.allowAllBlockStates = false;
            return validateAndReturn(config);
        } catch (IllegalArgumentException ignored) {

        }

        return null;
    }

    private static void parseBlockStates(BlockAppearanceConfig config, String statesString) {


        String[] statePairs = statesString.split(",");
        for (String pair : statePairs) {
            pair = pair.trim();
            if (pair.contains("=")) {

                String[] parts = pair.split("=");
                if (parts.length == 2) {
                    String key = parts[0].trim();
                    String value = parts[1].trim();
                    config.allowedStates.computeIfAbsent(key, k -> new HashSet<>()).add(value);
                }
            } else {


                config.allowedProperties.add(pair);
            }
        }
    }

    private static BlockAppearanceConfig validateAndReturn(BlockAppearanceConfig config) {
        if (config.baseBlockType == null || config.baseBlockType.isEmpty()) {
            return null;
        }
        return config;
    }

    public String getBaseBlockType() {
        return baseBlockType;
    }

    public String getDefaultVariant() {
        return defaultVariant;
    }

    public boolean isAllowAllVariants() {
        return allowAllVariants;
    }

    public boolean isAllowAllBlockStates() {
        return allowAllBlockStates;
    }

    public Set<String> getAllowedProperties() {
        return allowedProperties;
    }

    public Map<String, Set<String>> getAllowedStates() {
        return allowedStates;
    }

    public Set<Material> getCustomMaterials() {
        return customMaterials;
    }

    public boolean isCustomList() {
        return customMaterials != null && !customMaterials.isEmpty();
    }

    public boolean shouldShowAllVariantsInSelector() {
        return showAllVariantsInSelector;
    }

    public boolean hasVariantGroup() {
        return hasVariantGroup;
    }


    public boolean isBlockStateAllowed(BlockData blockData) {
        if (blockData == null) {
            return false;
        }


        if (allowAllBlockStates) {
            return true;
        }


        if (allowedStates.isEmpty() && allowedProperties.isEmpty()) {
            return true;
        }


        if (!allowedStates.isEmpty()) {

            for (Map.Entry<String, Set<String>> constraint : allowedStates.entrySet()) {
                String propertyName = constraint.getKey();
                Set<String> allowedValues = constraint.getValue();


                String currentValue = getBlockDataPropertyValue(blockData, propertyName);
                if (currentValue == null || !allowedValues.contains(currentValue)) {
                    return false;
                }
            }
        }

        return true;
    }

    private String getBlockDataPropertyValue(BlockData blockData, String propertyName) {
        try {
            String dataString = blockData.getAsString();
            int start = dataString.indexOf('[');
            int end = dataString.indexOf(']');
            if (start < 0 || end < 0 || end <= start) {
                return null;
            }
            String states = dataString.substring(start + 1, end);
            for (String part : states.split(",")) {
                String[] kv = part.split("=");
                if (kv.length != 2) {
                    continue;
                }
                String key = kv[0].trim();
                String value = kv[1].trim();
                if (key.equalsIgnoreCase(propertyName)) {
                    return value;
                }
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String toString() {
        return "BlockAppearanceConfig{" +
                "baseBlockType='" + baseBlockType + '\'' +
                ", defaultVariant='" + defaultVariant + '\'' +
                ", allowAllVariants=" + allowAllVariants +
                ", allowAllBlockStates=" + allowAllBlockStates +
                ", allowedProperties=" + allowedProperties +
                ", allowedStates=" + allowedStates +
                '}';
    }
}
