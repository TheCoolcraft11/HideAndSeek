package de.thecoolcraft11.hideAndSeek.items.hider;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.block.BlockAppearanceConfig;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class HiderItemUtil {

    public static String formatName(String name) {
        StringBuilder result = new StringBuilder();
        for (String part : name.toLowerCase().split("_")) {
            if (!result.isEmpty()) result.append(" ");
            result.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return result.toString();
    }

    public static void updateAppearanceItem(Player player, HideAndSeek plugin) {
        String currentPhase = plugin.getStateManager().getCurrentPhaseId();
        boolean isHiding = "hiding".equals(currentPhase);
        int appearanceSlot = isHiding ? 7 : 8;

        if (hasCustomizableBlock(player, plugin)) {
            player.getInventory().setItem(appearanceSlot, plugin.getCustomItemManager().getIdentifiedItemStack(AppearanceItem.ID, player));
        } else {
            player.getInventory().setItem(appearanceSlot, new ItemStack(Material.AIR));
        }
    }

    public static boolean hasCustomizableBlock(Player player, HideAndSeek plugin) {
        Material chosenBlock = HideAndSeek.getDataController().getChosenBlock(player.getUniqueId());

        String currentMap = HideAndSeek.getDataController().getCurrentMapName();
        if (currentMap == null || currentMap.isEmpty()) {
            return false;
        }

        List<String> allowedBlocks = plugin.getMapManager().getAllowedBlocksForMap(currentMap);
        if (allowedBlocks == null || allowedBlocks.isEmpty()) {
            return false;
        }

        if (chosenBlock == null) {
            return mapHasCustomizableBlocks(allowedBlocks);
        }

        BlockAppearanceConfig config = plugin.getBlockSelectorGUI().resolveConfigForMaterial(allowedBlocks, chosenBlock);
        if (config == null) {
            return false;
        }

        return isConfigCustomizable(config);
    }

    private static boolean mapHasCustomizableBlocks(List<String> allowedBlocks) {
        for (String pattern : allowedBlocks) {
            BlockAppearanceConfig config = BlockAppearanceConfig.parse(pattern);
            if (config != null && isConfigCustomizable(config)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isConfigCustomizable(BlockAppearanceConfig config) {
        return config.isAllowAllVariants() ||
                config.hasVariantGroup() ||
                config.isAllowAllBlockStates() ||
                !config.getAllowedProperties().isEmpty() ||
                !config.getAllowedStates().isEmpty();
    }
}

