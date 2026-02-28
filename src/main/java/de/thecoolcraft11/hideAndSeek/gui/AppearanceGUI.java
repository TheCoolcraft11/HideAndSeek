package de.thecoolcraft11.hideAndSeek.gui;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.block.BlockAppearanceConfig;
import de.thecoolcraft11.hideAndSeek.block.BlockStateFilter;
import de.thecoolcraft11.minigameframework.inventory.FrameworkInventory;
import de.thecoolcraft11.minigameframework.inventory.InventoryItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Axis;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.*;
import org.bukkit.block.data.type.Candle;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class AppearanceGUI {
    private final HideAndSeek plugin;
    private final BlockSelectorGUI blockSelectorGUI;

    public AppearanceGUI(HideAndSeek plugin, BlockSelectorGUI blockSelectorGUI) {
        this.plugin = plugin;
        this.blockSelectorGUI = blockSelectorGUI;
    }

    public void open(Player player) {
        String currentPhase = plugin.getStateManager().getCurrentPhaseId();
        if (!currentPhase.equals("hiding") && !currentPhase.equals("seeking")) {
            player.sendMessage(Component.text("Appearance editor only available during Hiding or Seeking phases!", NamedTextColor.RED));
            return;
        }
        if (!HideAndSeek.getDataController().getHiders().contains(player.getUniqueId())) {
            player.sendMessage(Component.text("Only hiders can use this!", NamedTextColor.RED));
            return;
        }

        Material chosenBlock = HideAndSeek.getDataController().getChosenBlock(player.getUniqueId());
        if (chosenBlock == null) {
            player.sendMessage(Component.text("You must choose a block first! Use /mg chooseblock", NamedTextColor.RED));
            return;
        }

        BlockAppearanceConfig config = blockSelectorGUI.getPlayerConfig(player.getUniqueId());
        if (config == null) {
            String currentMap = HideAndSeek.getDataController().getCurrentMapName();
            List<String> allowedBlocks = currentMap != null && !currentMap.isEmpty()
                    ? plugin.getMapManager().getAllowedBlocksForMap(currentMap)
                    : List.of();
            config = blockSelectorGUI.resolveConfigForMaterial(allowedBlocks, chosenBlock);
            if (config != null) {
                blockSelectorGUI.setPlayerConfig(player.getUniqueId(), config);
            }
        }
        if (config == null) {

            config = BlockAppearanceConfig.parse(chosenBlock.name());
        }


        int estimatedItems = 0;
        if (config.isAllowAllVariants() || config.hasVariantGroup()) {
            if (config.hasVariantGroup() && config.getCustomMaterials() != null) {
                estimatedItems += config.getCustomMaterials().size();
            } else {

                estimatedItems += 15;
            }
        }

        BlockData blockData = chosenBlock.createBlockData();
        List<String> allProperties = getAllBlockStateProperties(blockData);
        estimatedItems += allProperties.size();


        if (estimatedItems > 35) {
            openWithPagination(player, chosenBlock, config);
        } else {
            openSinglePage(player, chosenBlock, config);
        }
    }

    private void openSinglePage(Player player, Material chosenBlock, BlockAppearanceConfig config) {
        FrameworkInventory inventory = plugin.getInventoryFramework().create("Customize Appearance", 6);
        inventory.setSetting("allow_outside_clicks", false);
        inventory.setSetting("allow_drag", false);
        inventory.setSetting("allow_player_inventory_interaction", false);

        int slot = 0;
        int maxContentSlot = 45;


        if (config.isAllowAllVariants() || config.hasVariantGroup()) {
            slot = startSection(inventory, slot, "Variants", maxContentSlot);
            slot = addVariantsSection(inventory, slot, player, chosenBlock, config, maxContentSlot);
        }

        if (config.isAllowAllBlockStates() || !config.getAllowedStates().isEmpty() || !config.getAllowedProperties().isEmpty()) {
            slot = startSection(inventory, slot, "Block States", maxContentSlot);
            slot = addBlockStatesSection(inventory, slot, player, chosenBlock, config, maxContentSlot);
        }

        addFooter(inventory);
        plugin.getInventoryFramework().openInventory(player, inventory);
    }

    private void openWithPagination(Player player, Material chosenBlock, BlockAppearanceConfig config) {
        List<FrameworkInventory> pages = new ArrayList<>();
        FrameworkInventory currentPage = plugin.getInventoryFramework().create("Customize Appearance", 6);
        currentPage.setSetting("allow_outside_clicks", false);
        currentPage.setSetting("allow_drag", false);
        currentPage.setSetting("allow_player_inventory_interaction", false);

        int slot = 0;
        int maxContentSlot = 45;


        if (config.isAllowAllVariants() || config.hasVariantGroup()) {
            slot = startSection(currentPage, slot, "Variants", maxContentSlot);
            slot = addVariantsSection(currentPage, slot, player, chosenBlock, config, maxContentSlot);


            if (slot >= maxContentSlot) {
                pages.add(currentPage);
                currentPage = plugin.getInventoryFramework().create("Customize Appearance - Variants", 6);
                currentPage.setSetting("allow_outside_clicks", false);
                currentPage.setSetting("allow_drag", false);
                currentPage.setSetting("allow_player_inventory_interaction", false);
                slot = 0;
                slot = startSection(currentPage, slot, "Variants (continued)", maxContentSlot);
                slot = addVariantsSection(currentPage, slot, player, chosenBlock, config, maxContentSlot);
            }
        }


        if (config.isAllowAllBlockStates() || !config.getAllowedStates().isEmpty() || !config.getAllowedProperties().isEmpty()) {
            slot = alignToNextRow(slot);
            if (slot >= maxContentSlot) {
                pages.add(currentPage);
                currentPage = plugin.getInventoryFramework().create("Customize Appearance - States", 6);
                currentPage.setSetting("allow_outside_clicks", false);
                currentPage.setSetting("allow_drag", false);
                currentPage.setSetting("allow_player_inventory_interaction", false);
                slot = 0;
            }
            slot = startSection(currentPage, slot, "Block States", maxContentSlot);
            slot = addBlockStatesSection(currentPage, slot, player, chosenBlock, config, maxContentSlot);

            if (slot >= maxContentSlot) {
                pages.add(currentPage);
                currentPage = plugin.getInventoryFramework().create("Customize Appearance - States", 6);
                currentPage.setSetting("allow_outside_clicks", false);
                currentPage.setSetting("allow_drag", false);
                currentPage.setSetting("allow_player_inventory_interaction", false);
                slot = 0;
                slot = startSection(currentPage, slot, "Block States (continued)", maxContentSlot);
                slot = addBlockStatesSection(currentPage, slot, player, chosenBlock, config, maxContentSlot);
            }
        }


        if (slot > 0) {
            addFooter(currentPage);
            pages.add(currentPage);
        }


        if (!pages.isEmpty()) {
            addPaginationNavigation(pages);
            plugin.getInventoryFramework().openInventory(player, pages.getFirst());
        }
    }

    private void addPaginationNavigation(List<FrameworkInventory> pages) {
        if (pages.size() <= 1) return;

        for (int i = 0; i < pages.size(); i++) {
            FrameworkInventory page = pages.get(i);


            ItemStack separator = createSeparatorItem();
            for (int j = 45; j < 54; j++) {
                page.setItem(j, new InventoryItem(separator));
            }

            int currentPageIndex = i;


            if (pages.size() > 1) {

                if (i > 0) {
                    ItemStack prevItem = new ItemStack(Material.ARROW);
                    ItemMeta prevMeta = prevItem.getItemMeta();
                    if (prevMeta != null) {
                        prevMeta.displayName(Component.text("◀ Previous", NamedTextColor.AQUA, TextDecoration.BOLD));
                        prevItem.setItemMeta(prevMeta);
                    }
                    page.setItem(48, new InventoryItem(prevItem).onClick((p, type) -> {
                        if (type == ClickType.LEFT) {
                            plugin.getInventoryFramework().openInventory(p, pages.get(currentPageIndex - 1));
                        }
                    }));
                }


                page.setItem(49, new InventoryItem(createConfirmItem()).onClick((p, type) -> {
                    if (type == ClickType.LEFT) {
                        confirmAppearance(p);
                    }
                }));


                if (i < pages.size() - 1) {
                    ItemStack nextItem = new ItemStack(Material.ARROW);
                    ItemMeta nextMeta = nextItem.getItemMeta();
                    if (nextMeta != null) {
                        nextMeta.displayName(Component.text("Next ▶", NamedTextColor.AQUA, TextDecoration.BOLD));
                        nextItem.setItemMeta(nextMeta);
                    }
                    page.setItem(50, new InventoryItem(nextItem).onClick((p, type) -> {
                        if (type == ClickType.LEFT) {
                            plugin.getInventoryFramework().openInventory(p, pages.get(currentPageIndex + 1));
                        }
                    }));
                }
            } else {

                page.setItem(49, new InventoryItem(createConfirmItem()).onClick((p, type) -> {
                    if (type == ClickType.LEFT) {
                        confirmAppearance(p);
                    }
                }));
            }
        }
    }

    private int startSection(FrameworkInventory inventory, int slot, String title, int maxContentSlot) {
        slot = alignToNextRow(slot);
        if (slot >= maxContentSlot) {
            return slot;
        }
        ItemStack header = createSectionHeaderItem(title);
        inventory.setItem(slot, new InventoryItem(header));
        return slot + 1;
    }

    private int alignToNextRow(int slot) {
        if (slot % 9 == 0) {
            return slot;
        }
        return ((slot / 9) + 1) * 9;
    }

    private void addFooter(FrameworkInventory inventory) {
        ItemStack separator = createSeparatorItem();
        for (int i = 45; i < 54; i++) {
            inventory.setItem(i, new InventoryItem(separator));
        }
        inventory.setItem(49, new InventoryItem(createConfirmItem()).onClick((p, type) -> {
            if (type == ClickType.LEFT) {
                confirmAppearance(p);
            }
        }));
    }

    private ItemStack createSectionHeaderItem(String title) {
        ItemStack item = new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(title, NamedTextColor.GOLD, TextDecoration.BOLD)
                    .decoration(TextDecoration.ITALIC, false));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createSeparatorItem() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(" "));
            item.setItemMeta(meta);
        }
        return item;
    }

    private int addVariantsSection(FrameworkInventory inventory, int startSlot, Player player,
                                   Material currentBlock, BlockAppearanceConfig config, int maxContentSlot) {
        int slot = startSlot;

        List<Material> variants;


        if (config.hasVariantGroup() && config.getCustomMaterials() != null) {
            variants = new ArrayList<>(config.getCustomMaterials());

            variants.sort(Comparator.comparing(Enum::name));
        } else {

            String baseType = config.getBaseBlockType();
            if (baseType == null || baseType.isEmpty()) {
                baseType = currentBlock.name();
            }
            if (baseType.contains("_")) {
                baseType = baseType.substring(baseType.lastIndexOf("_") + 1);
            }
            final String baseName = baseType;

            variants = Arrays.stream(Material.values())
                    .filter(m -> m.isItem() && m.isBlock())
                    .filter(m -> m.name().endsWith("_" + baseName) || m.name().equals(baseName))
                    .sorted(Comparator.comparing(Enum::name))
                    .toList();
        }

        BlockData currentBlockData = HideAndSeek.getDataController().getChosenBlockData(player.getUniqueId());

        for (Material variant : variants) {
            if (slot >= maxContentSlot) break;

            boolean isSelected = currentBlockData != null && currentBlockData.getMaterial() == variant;
            ItemStack item = createVariantItem(variant, isSelected);
            inventory.setItem(slot, new InventoryItem(item).onClick((p, type) -> {
                if (type == ClickType.LEFT) {
                    BlockData previousData = HideAndSeek.getDataController().getChosenBlockData(p.getUniqueId());
                    BlockData newData = variant.createBlockData();
                    if (previousData != null) {
                        copySharedBlockStates(previousData, newData);
                    }
                    HideAndSeek.getDataController().setChosenBlock(p.getUniqueId(), variant);
                    HideAndSeek.getDataController().setChosenBlockData(p.getUniqueId(), newData);
                    var display = HideAndSeek.getDataController().getBlockDisplay(p.getUniqueId());
                    if (display != null && display.isValid()) {
                        display.setBlock(newData);
                        display.setRotation(p.getLocation().getYaw(), 0f);
                    }
                    de.thecoolcraft11.hideAndSeek.items.HiderItems.updateAppearanceItem(p, plugin);
                    p.sendMessage(Component.text("Variant updated to ", NamedTextColor.GREEN)
                            .append(Component.text(formatName(variant.name()), NamedTextColor.GOLD)));
                }
            }));
            slot++;
        }

        slot = alignToNextRow(slot);
        return slot;
    }

    private int addBlockStatesSection(FrameworkInventory inventory, int startSlot, Player player, Material block,
                                      BlockAppearanceConfig config, int maxContentSlot) {
        int slot = startSlot;


        BlockData blockData = block.createBlockData();
        List<String> allProperties = getAllBlockStateProperties(blockData);


        List<String> allowedRotationProperties = new ArrayList<>();
        List<String> allowedOtherProperties = new ArrayList<>();

        List<String> rotationProperties = Arrays.asList("facing", "axis", "rotation");

        for (String prop : allProperties) {

            if (BlockStateFilter.isDisallowed(prop)) {
                continue;
            }


            if (!config.getAllowedProperties().isEmpty() && !config.getAllowedProperties().contains(prop)) {
                continue;
            }


            if (rotationProperties.contains(prop)) {
                allowedRotationProperties.add(prop);
            } else {
                allowedOtherProperties.add(prop);
            }
        }


        if (!allowedRotationProperties.isEmpty()) {
            slot = addBlockStateSubsection(inventory, slot, player, block, config, allowedRotationProperties, "Rotation", maxContentSlot);
        }


        if (!allowedOtherProperties.isEmpty()) {
            slot = addBlockStateSubsection(inventory, slot, player, block, config, allowedOtherProperties, "Other States", maxContentSlot);
        }

        slot = alignToNextRow(slot);
        return slot;
    }

    private List<String> getAllBlockStateProperties(BlockData blockData) {
        List<String> properties = new ArrayList<>();


        String dataString = blockData.getAsString();
        int start = dataString.indexOf('[');
        int end = dataString.indexOf(']');

        if (start >= 0 && end > start) {
            String statesString = dataString.substring(start + 1, end);
            String[] statePairs = statesString.split(",");

            for (String pair : statePairs) {
                if (pair.contains("=")) {
                    String propertyName = pair.split("=")[0].trim();
                    if (!properties.contains(propertyName)) {
                        properties.add(propertyName);
                    }
                }
            }
        }


        List<String> rotationProperties = Arrays.asList("facing", "axis", "rotation");
        List<String> sortedProperties = new ArrayList<>();


        for (String rotProp : rotationProperties) {
            if (properties.contains(rotProp)) {
                sortedProperties.add(rotProp);
            }
        }


        for (String prop : properties) {
            if (!sortedProperties.contains(prop)) {
                sortedProperties.add(prop);
            }
        }

        return sortedProperties;
    }

    private int addBlockStateSubsection(FrameworkInventory inventory, int slot, Player player, Material block,
                                        BlockAppearanceConfig config, List<String> propertyNames, String sectionName, int maxContentSlot) {
        BlockData blockData = block.createBlockData();
        List<String> validProperties = propertyNames.stream()
                .filter(prop -> isPropertySupported(blockData, prop))
                .toList();

        if (validProperties.isEmpty()) {
            return slot;
        }


        for (String property : validProperties) {
            if (slot >= maxContentSlot) break;
            int propertySlot = slot;
            setStateItem(inventory, propertySlot, property, player, blockData);
            slot++;
        }

        return slot;
    }

    private void setStateItem(FrameworkInventory inventory, int slot, String property, Player player, BlockData baseData) {
        ItemStack stateItem = createStateItem(property, player, baseData);
        inventory.setItem(slot, new InventoryItem(stateItem).onClick((p, type) -> {
            if (type == ClickType.LEFT || type == ClickType.RIGHT) {
                cyclePropertyValue(p, property, type == ClickType.RIGHT);

                BlockData current = HideAndSeek.getDataController().getChosenBlockData(p.getUniqueId());
                if (current == null) {
                    current = baseData;
                }
                ItemStack updatedItem = createStateItem(property, p, current);
                inventory.setItem(slot, new InventoryItem(updatedItem).onClick((p2, type2) -> {
                    if (type2 == ClickType.LEFT || type2 == ClickType.RIGHT) {
                        cyclePropertyValue(p2, property, type2 == ClickType.RIGHT);
                        setStateItem(inventory, slot, property, p2, baseData);
                    }
                }));

                String value = getCurrentStateValue(current, property);
                p.sendMessage(Component.text(formatName(property) + " → " + value, NamedTextColor.GRAY));
            }
        }));
    }

    private boolean isPropertySupported(BlockData blockData, String propertyName) {


        return true;
    }

    private void cyclePropertyValue(Player player, String property, boolean reverse) {
        BlockData current = HideAndSeek.getDataController().getChosenBlockData(player.getUniqueId());
        if (current == null) {
            Material chosenBlock = HideAndSeek.getDataController().getChosenBlock(player.getUniqueId());
            if (chosenBlock == null) {
                return;
            }
            current = chosenBlock.createBlockData();
        }


        BlockData updated = getNextBlockData(current, property, reverse);
        if (updated == null) {
            return;
        }

        HideAndSeek.getDataController().setChosenBlockData(player.getUniqueId(), updated);

        var display = HideAndSeek.getDataController().getBlockDisplay(player.getUniqueId());
        if (display != null && display.isValid()) {
            display.setBlock(updated);
            display.setRotation(player.getLocation().getYaw(), 0f);
        }
        de.thecoolcraft11.hideAndSeek.items.HiderItems.updateAppearanceItem(player, plugin);
    }

    private BlockData getNextBlockData(BlockData data, String property, boolean reverse) {
        try {

            String currentValue = getPropertyValueFromString(data, property);
            List<String> possibleValues = discoverPropertyValues(data, property);

            if (possibleValues.isEmpty()) {
                plugin.getLogger().warning("No valid values found for property: " + property);
                return null;
            }


            int currentIndex = possibleValues.indexOf(currentValue);
            if (currentIndex < 0) currentIndex = 0;

            int nextIndex = reverse ?
                    (currentIndex - 1 + possibleValues.size()) % possibleValues.size() :
                    (currentIndex + 1) % possibleValues.size();

            String nextValue = possibleValues.get(nextIndex);


            String dataString = data.getAsString();
            String newDataString = buildDataStringWithProperty(dataString, property, nextValue);


            return Bukkit.createBlockData(newDataString);

        } catch (Exception e) {
            plugin.getLogger().warning("Failed to cycle property '" + property + "': " + e.getMessage());
            return null;
        }
    }

    private String getPropertyValueFromString(BlockData data, String property) {
        String dataString = data.getAsString();
        int start = dataString.indexOf('[');
        int end = dataString.indexOf(']');

        if (start < 0 || end <= start) {
            return "";
        }

        String statesString = dataString.substring(start + 1, end);
        String[] pairs = statesString.split(",");

        for (String pair : pairs) {
            String[] kv = pair.split("=");
            if (kv.length == 2 && kv[0].trim().equals(property)) {
                return kv[1].trim();
            }
        }

        return "";
    }

    private List<String> discoverPropertyValues(BlockData data, String property) {
        List<String> values = new ArrayList<>();
        String currentValue = getPropertyValueFromString(data, property);


        if (currentValue.matches("\\d+")) {

            for (int i = 0; i <= 16; i++) {
                if (trySetPropertyValue(data, property, String.valueOf(i))) {
                    values.add(String.valueOf(i));
                }
            }

            if (!values.isEmpty()) {
                values.sort(Comparator.comparingInt(Integer::parseInt));
                return values;
            }
        }


        if ("true".equals(currentValue) || "false".equals(currentValue)) {
            if (trySetPropertyValue(data, property, "false")) {
                values.add("false");
            }
            if (trySetPropertyValue(data, property, "true") && !values.contains("true")) {
                values.add("true");
            }
            return values;
        }


        String[] commonEnumValues = {
                "north", "south", "east", "west", "up", "down",
                "x", "y", "z",
                "top", "bottom", "upper", "lower",
                "left", "right",
                "single", "double",
                "straight", "inner_left", "inner_right", "outer_left", "outer_right",
                "none", "low", "tall"
        };

        for (String value : commonEnumValues) {
            if (trySetPropertyValue(data, property, value)) {
                if (!values.contains(value)) {
                    values.add(value);
                }
            }
        }

        return values;
    }

    private boolean trySetPropertyValue(BlockData data, String property, String value) {
        try {
            String dataString = data.getAsString();
            String newDataString = buildDataStringWithProperty(dataString, property, value);
            org.bukkit.Bukkit.createBlockData(newDataString);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String buildDataStringWithProperty(String dataString, String property, String value) {
        int bracketStart = dataString.indexOf('[');
        int bracketEnd = dataString.indexOf(']');

        if (bracketStart < 0 || bracketEnd <= bracketStart) {
            return dataString;
        }

        String prefix = dataString.substring(0, bracketStart + 1);
        String suffix = dataString.substring(bracketEnd);
        String statesString = dataString.substring(bracketStart + 1, bracketEnd);


        StringBuilder newStates = new StringBuilder();
        String[] pairs = statesString.split(",");
        boolean found = false;

        for (int i = 0; i < pairs.length; i++) {
            String pair = pairs[i];
            String[] kv = pair.split("=");

            if (kv.length == 2 && kv[0].trim().equals(property)) {
                newStates.append(property).append("=").append(value);
                found = true;
            } else {
                newStates.append(pair);
            }

            if (i < pairs.length - 1) {
                newStates.append(",");
            }
        }

        if (!found) {
            if (!newStates.isEmpty()) {
                newStates.append(",");
            }
            newStates.append(property).append("=").append(value);
        }

        return prefix + newStates + suffix;
    }

    private ItemStack createStateItem(String stateName, Player player, BlockData baseData) {
        BlockData current = HideAndSeek.getDataController().getChosenBlockData(player.getUniqueId());
        if (current == null) {
            current = baseData;
        }
        String value = getCurrentStateValue(current, stateName);

        ItemStack item = getStateItemMaterial(stateName, current, value);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(formatName(stateName), NamedTextColor.YELLOW, TextDecoration.BOLD)
                    .decoration(TextDecoration.ITALIC, false));

            List<Component> lore = new ArrayList<>();
            lore.add(Component.text("Current: " + value, NamedTextColor.GRAY)
                    .decoration(TextDecoration.ITALIC, false));
            lore.add(Component.text("Left/Right click to cycle", NamedTextColor.DARK_GRAY)
                    .decoration(TextDecoration.ITALIC, false));

            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack getStateItemMaterial(String property, BlockData blockData, String value) {

        if (isBooleanProperty(property)) {
            boolean isTrue = "true".equalsIgnoreCase(value);
            ItemStack item = new ItemStack(isTrue ? Material.REDSTONE : Material.GUNPOWDER);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                item.setItemMeta(meta);
            }
            return item;
        }


        if (isIntegerProperty(property)) {
            try {
                int intValue = Integer.parseInt(value);
                ItemStack item = new ItemStack(Material.REDSTONE, Math.min(intValue, 64));
                return item;
            } catch (NumberFormatException e) {
                return new ItemStack(Material.REDSTONE);
            }
        }


        return switch (property) {
            case "facing" -> {
                if (blockData instanceof Directional directional) {
                    yield getMaterialForFacing(directional.getFacing());
                }
                yield new ItemStack(Material.COMPASS);
            }
            case "axis" -> {
                if (blockData instanceof Orientable orientable) {
                    yield getMaterialForAxis(orientable.getAxis());
                }
                yield new ItemStack(Material.STICK);
            }
            case "rotation" -> new ItemStack(Material.COMPASS);
            case "half" -> "top".equalsIgnoreCase(value) ?
                    new ItemStack(Material.SMOOTH_STONE_SLAB) :
                    new ItemStack(Material.OAK_WOOD);
            case "open" -> "true".equalsIgnoreCase(value) ?
                    new ItemStack(Material.OAK_DOOR) :
                    new ItemStack(Material.IRON_DOOR);
            case "hinge" -> "left".equalsIgnoreCase(value) ?
                    new ItemStack(Material.ANVIL) :
                    new ItemStack(Material.CHIPPED_ANVIL);
            case "type" -> "top".equalsIgnoreCase(value) ?
                    new ItemStack(Material.OAK_SLAB) :
                    new ItemStack(Material.STONE_SLAB);
            case "shape" -> new ItemStack(Material.SANDSTONE_STAIRS);
            case "triggered" -> "true".equalsIgnoreCase(value) ?
                    new ItemStack(Material.DISPENSER) :
                    new ItemStack(Material.DROPPER);
            default -> new ItemStack(Material.BARRIER);
        };
    }

    private ItemStack getMaterialForFacing(BlockFace face) {
        return switch (face) {
            case NORTH -> new ItemStack(Material.ARROW);
            case SOUTH -> new ItemStack(Material.CHISELED_SANDSTONE);
            case EAST -> new ItemStack(Material.REPEATER);
            case WEST -> new ItemStack(Material.COMPARATOR);
            case UP -> new ItemStack(Material.IRON_CHAIN);
            case DOWN -> new ItemStack(Material.IRON_BLOCK);
            default -> new ItemStack(Material.COMPASS);
        };
    }

    private ItemStack getMaterialForAxis(Axis axis) {
        return switch (axis) {
            case X -> new ItemStack(Material.OAK_SLAB);
            case Y -> new ItemStack(Material.IRON_CHAIN);
            case Z -> new ItemStack(Material.SOUL_LANTERN);
        };
    }

    private boolean isBooleanProperty(String property) {
        return property.equals("lit") || property.equals("waterlogged") ||
                property.equals("powered") || property.equals("open") ||
                property.equals("triggered");
    }

    private boolean isIntegerProperty(String property) {
        return property.equals("candles") || property.equals("level") ||
                property.equals("age");
    }

    private String getCurrentStateValue(BlockData data, String stateName) {
        String value = getPropertyValueFromString(data, stateName);
        return value.isEmpty() ? "n/a" : value;
    }

    private ItemStack createStateItem(String stateName) {
        return createStateItem(stateName, null, null);
    }

    private void confirmAppearance(Player player) {
        BlockData chosenData = HideAndSeek.getDataController().getChosenBlockData(player.getUniqueId());
        if (chosenData != null) {
            player.sendMessage(Component.text("Appearance updated to ", NamedTextColor.GREEN)
                    .append(Component.text(formatName(chosenData.getMaterial().name()) + " " + chosenData.getAsString(), NamedTextColor.GOLD)));
        }
        player.closeInventory();
    }

    private ItemStack createVariantItem(Material material, boolean isSelected) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(("") + formatName(material.name()),
                            isSelected ? NamedTextColor.GREEN : NamedTextColor.YELLOW, TextDecoration.BOLD)
                    .decoration(TextDecoration.ITALIC, false));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createTitleItem(String title, NamedTextColor color) {
        ItemStack item = new ItemStack(Material.NAME_TAG);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("═══ " + title + " ═══", color, TextDecoration.BOLD)
                    .decoration(TextDecoration.ITALIC, false));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createSubsectionItem(String title) {
        ItemStack item = new ItemStack(Material.ANVIL);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("▪ " + title, NamedTextColor.GOLD, TextDecoration.BOLD)
                    .decoration(TextDecoration.ITALIC, false));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createConfirmItem() {
        ItemStack item = new ItemStack(Material.GREEN_CONCRETE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Confirm", NamedTextColor.GREEN, TextDecoration.BOLD)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.text("Click to confirm your appearance", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false)
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    private String formatName(String name) {
        StringBuilder result = new StringBuilder();
        for (String part : name.toLowerCase().split("_")) {
            if (!result.isEmpty()) result.append(" ");
            result.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return result.toString();
    }

    private void copySharedBlockStates(BlockData source, BlockData target) {
        List<String> properties = getAllBlockStateProperties(target);
        for (String property : properties) {
            applyStateFromSource(source, target, property);
        }
    }

    private void applyStateFromSource(BlockData source, BlockData target, String property) {
        switch (property) {
            case "facing" -> {
                if (source instanceof Directional src && target instanceof Directional dst) {
                    dst.setFacing(src.getFacing());
                }
            }
            case "axis" -> {
                if (source instanceof Orientable src && target instanceof Orientable dst) {
                    dst.setAxis(src.getAxis());
                }
            }
            case "rotation" -> {
                if (source instanceof Rotatable src && target instanceof Rotatable dst) {
                    dst.setRotation(src.getRotation());
                }
            }
            case "lit" -> {
                if (source instanceof Lightable src && target instanceof Lightable dst) {
                    dst.setLit(src.isLit());
                }
            }
            case "waterlogged" -> {
                if (source instanceof Waterlogged src && target instanceof Waterlogged dst) {
                    dst.setWaterlogged(src.isWaterlogged());
                }
            }
            case "candles" -> {
                if (source instanceof Candle src && target instanceof Candle dst) {
                    dst.setCandles(src.getCandles());
                }
            }
            case "powered" -> {
                if (source instanceof Powerable src && target instanceof Powerable dst) {
                    dst.setPowered(src.isPowered());
                }
            }
            case "level" -> {
                if (source instanceof Levelled src && target instanceof Levelled dst) {
                    dst.setLevel(src.getLevel());
                }
            }
            case "age" -> {
                if (source instanceof Ageable src && target instanceof Ageable dst) {
                    dst.setAge(src.getAge());
                }
            }
            case "half" -> {
                if (source instanceof org.bukkit.block.data.type.Door src && target instanceof org.bukkit.block.data.type.Door dst) {
                    dst.setHalf(src.getHalf());
                } else if (source instanceof org.bukkit.block.data.type.TrapDoor src && target instanceof org.bukkit.block.data.type.TrapDoor dst) {
                    dst.setHalf(src.getHalf());
                } else if (source instanceof org.bukkit.block.data.type.Stairs src && target instanceof org.bukkit.block.data.type.Stairs dst) {
                    dst.setHalf(src.getHalf());
                }
            }
            case "hinge" -> {
                if (source instanceof org.bukkit.block.data.type.Door src && target instanceof org.bukkit.block.data.type.Door dst) {
                    dst.setHinge(src.getHinge());
                }
            }
            case "open" -> {
                if (source instanceof org.bukkit.block.data.type.Door src && target instanceof org.bukkit.block.data.type.Door dst) {
                    dst.setOpen(src.isOpen());
                } else if (source instanceof org.bukkit.block.data.type.TrapDoor src && target instanceof org.bukkit.block.data.type.TrapDoor dst) {
                    dst.setOpen(src.isOpen());
                }
            }
            case "triggered" -> {
                if (source instanceof org.bukkit.block.data.type.Dispenser src && target instanceof org.bukkit.block.data.type.Dispenser dst) {
                    dst.setTriggered(src.isTriggered());
                }
            }
            case "type" -> {
                if (source instanceof org.bukkit.block.data.type.Slab src && target instanceof org.bukkit.block.data.type.Slab dst) {
                    dst.setType(src.getType());
                }
            }
            case "shape" -> {
                if (source instanceof org.bukkit.block.data.type.Stairs src && target instanceof org.bukkit.block.data.type.Stairs dst) {
                    dst.setShape(src.getShape());
                }
            }
            default -> {

            }
        }
    }
}
