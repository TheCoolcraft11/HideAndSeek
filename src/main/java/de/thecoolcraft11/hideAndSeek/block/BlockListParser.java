package de.thecoolcraft11.hideAndSeek.block;

import org.bukkit.Material;
import org.bukkit.Tag;

import java.util.HashSet;
import java.util.Set;

public class BlockListParser {

    public static Set<Material> parseBlockList(String blockListString) {
        if (blockListString == null || blockListString.isEmpty()) {
            return new HashSet<>();
        }

        Set<Material> materials = new HashSet<>();
        String[] parts = blockListString.split(",");

        for (String part : parts) {
            part = part.trim();
            if (part.isEmpty()) {
                continue;
            }


            if (part.startsWith("*") && part.contains("{")) {
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
        Set<Material> materials = new HashSet<>();


        int braceStart = variantSpec.indexOf('{');
        int braceEnd = variantSpec.indexOf('}');

        if (braceStart < 0 || braceEnd < 0) {
            return materials;
        }

        String baseType = variantSpec.substring(1, braceStart).toUpperCase();


        for (Material mat : Material.values()) {
            String matName = mat.name();

            if (matName.endsWith(baseType) && mat.isBlock()) {
                materials.add(mat);
            }
        }

        return materials;
    }

    private static Set<Material> parseTag(String tagSpec) {
        Set<Material> materials = new HashSet<>();
        String tagName = tagSpec.substring(1).toUpperCase();


        switch (tagName) {
            case "FLOWERS":
                materials.addAll(Tag.FLOWERS.getValues());
                break;
            case "LEAVES":
                materials.addAll(Tag.LEAVES.getValues());
                break;
            case "LOGS":
                materials.addAll(Tag.LOGS.getValues());
                break;
            case "SAPLINGS":
                materials.addAll(Tag.SAPLINGS.getValues());
                break;
            case "CORALS":
                materials.addAll(Tag.CORALS.getValues());
                break;
            case "CORAL_BLOCKS":
                materials.addAll(Tag.CORAL_BLOCKS.getValues());
                break;
            case "WART_BLOCKS":
                materials.addAll(Tag.WART_BLOCKS.getValues());
                break;
            case "DOORS":
                materials.addAll(Tag.DOORS.getValues());
                break;
            case "TRAPDOORS":
                materials.addAll(Tag.TRAPDOORS.getValues());
                break;
            case "PRESSURE_PLATES":
                materials.addAll(Tag.PRESSURE_PLATES.getValues());
                break;
            case "RAILS":
                materials.addAll(Tag.RAILS.getValues());
                break;
            case "BUTTONS":
                materials.addAll(Tag.BUTTONS.getValues());
                break;
            case "SIGNS":
                materials.addAll(Tag.SIGNS.getValues());
                break;
            case "BEDS":
                materials.addAll(Tag.BEDS.getValues());
                break;
            case "SHULKER_BOXES":
                materials.addAll(Tag.SHULKER_BOXES.getValues());
                break;
            case "WOOL":
                materials.addAll(Tag.WOOL.getValues());
                break;
            case "PLANKS":
                materials.addAll(Tag.PLANKS.getValues());
                break;
            case "WOODEN_SLABS":
                materials.addAll(Tag.WOODEN_SLABS.getValues());
                break;
            case "WOODEN_STAIRS":
                materials.addAll(Tag.WOODEN_STAIRS.getValues());
                break;
            case "TERRACOTTA":
                materials.addAll(Tag.TERRACOTTA.getValues());
                break;
            case "CANDLES":
                materials.addAll(Tag.CANDLES.getValues());
                break;
            default:

                break;
        }

        return materials;
    }

    public static boolean isBreakableBlock(Material material, String blockListString) {
        Set<Material> breakableBlocks = parseBlockList(blockListString);
        return breakableBlocks.contains(material);
    }
}



