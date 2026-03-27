package de.thecoolcraft11.hideAndSeek.gui;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.block.BlockAppearanceConfig;
import de.thecoolcraft11.hideAndSeek.block.BlockListParser;
import de.thecoolcraft11.hideAndSeek.block.BlockStateFilter;
import de.thecoolcraft11.minigameframework.inventory.FrameworkInventory;
import de.thecoolcraft11.minigameframework.inventory.InventoryBuilder;
import de.thecoolcraft11.minigameframework.inventory.InventoryItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

public class AppearanceGUI {
    private final HideAndSeek plugin;
    private final BlockSelectorGUI blockSelectorGUI;


    private static final Set<String> UNIVERSAL_VALUE_POOL = new HashSet<>();
    private static boolean poolInitialized = false;

    public AppearanceGUI(HideAndSeek plugin, BlockSelectorGUI blockSelectorGUI) {
        this.plugin = plugin;
        this.blockSelectorGUI = blockSelectorGUI;
        initializeValuePool();
    }

    private void initializeValuePool() {
        if (poolInitialized) return;
        UNIVERSAL_VALUE_POOL.addAll(Arrays.asList("north", "south", "east", "west", "up", "down", "x", "y", "z"));
        for (Material mat : Material.values()) {
            if (!mat.isBlock()) continue;
            try {
                BlockData data = mat.createBlockData();
                for (Class<?> iface : data.getClass().getInterfaces()) {
                    if (!iface.getPackageName().startsWith("org.bukkit.block.data")) continue;
                    for (Method method : iface.getMethods()) {
                        if (method.getReturnType().isEnum()) {
                            for (Object constant : method.getReturnType().getEnumConstants()) {
                                UNIVERSAL_VALUE_POOL.add(constant.toString().toLowerCase());
                            }
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }
        poolInitialized = true;
    }

    public void open(Player player) {
        open(player, 0);
    }

    public void open(Player player, int targetPage) {
        String currentPhase = plugin.getStateManager().getCurrentPhaseId();
        if (!currentPhase.equals("hiding") && !currentPhase.equals("seeking")) {
            player.sendMessage(Component.text("Appearance editor only available during Hiding or Seeking phases!", NamedTextColor.RED));
            return;
        }

        Material chosenBlock = HideAndSeek.getDataController().getChosenBlock(player.getUniqueId());
        if (chosenBlock == null) {
            player.sendMessage(Component.text("You must choose a block first!", NamedTextColor.RED));
            return;
        }

        BlockAppearanceConfig config = blockSelectorGUI.getPlayerConfig(player.getUniqueId());
        if (config == null) config = BlockAppearanceConfig.parse(chosenBlock.name());

        List<InventoryItem> allItems = new ArrayList<>();

        boolean hasVariants = config.isAllowAllVariants() || config.hasVariantGroup();
        boolean hasStates = config.isAllowAllBlockStates() || !config.getAllowedProperties().isEmpty();


        if (hasVariants) {
            allItems.add(new InventoryItem(createSectionHeaderItem("Variants")));
            addVariantItems(allItems, player, chosenBlock, config);
            padToRow(allItems);
        }


        if (hasVariants && hasStates) {
            ItemStack spacer = createSeparatorItem();
            for (int i = 0; i < 9; i++) {
                allItems.add(new InventoryItem(spacer));
            }
        }


        if (hasStates) {
            allItems.add(new InventoryItem(createSectionHeaderItem("Block States")));
            addBlockStateItems(allItems, player, chosenBlock, config);
        }

        paginateAndOpen(player, allItems, targetPage);
    }

    private void paginateAndOpen(Player player, List<InventoryItem> allItems, int targetPage) {
        int itemsPerPage = 45;
        int totalPages = (int) Math.ceil((double) allItems.size() / itemsPerPage);
        if (totalPages == 0) totalPages = 1;

        if (targetPage >= totalPages) targetPage = totalPages - 1;
        if (targetPage < 0) targetPage = 0;

        List<FrameworkInventory> inventories = new ArrayList<>();

        for (int i = 0; i < totalPages; i++) {
            FrameworkInventory inv = new InventoryBuilder(plugin.getInventoryFramework())
                    .id("appearance_" + player.getUniqueId() + "_page_" + i)
                    .title("Customize Appearance (" + (i + 1) + "/" + totalPages + ")")
                    .rows(6)
                    .allowOutsideClicks(false)
                    .allowDrag(false)
                    .allowPlayerInventoryInteraction(false)
                    .build();

            int start = i * itemsPerPage;
            int end = Math.min(start + itemsPerPage, allItems.size());

            for (int slot = 0; slot < (end - start); slot++) {
                InventoryItem item = allItems.get(start + slot);
                item.setAllowTakeout(false);
                item.setAllowInsert(false);
                inv.setItem(slot, item);
            }

            addFooter(inv);
            inventories.add(inv);
        }

        for (int i = 0; i < inventories.size(); i++) {
            FrameworkInventory current = inventories.get(i);
            if (i > 0) {
                int finalI = i;
                InventoryItem prevBtn = new InventoryItem(createNavArrow("◀ Previous"));
                prevBtn.setClickHandler((p, item, event, slot) -> {
                    p.playSound(p.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.8f, 1.0f);
                    plugin.getInventoryFramework().openInventory(p, inventories.get(finalI - 1));
                    event.setCancelled(true);
                });
                prevBtn.setAllowTakeout(false);
                prevBtn.setAllowInsert(false);
                current.setItem(48, prevBtn);
            }
            if (i < inventories.size() - 1) {
                int finalI1 = i;
                InventoryItem nextBtn = new InventoryItem(createNavArrow("Next ▶"));
                nextBtn.setClickHandler((p, item, event, slot) -> {
                    p.playSound(p.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.8f, 1.0f);
                    plugin.getInventoryFramework().openInventory(p, inventories.get(finalI1 + 1));
                    event.setCancelled(true);
                });
                nextBtn.setAllowTakeout(false);
                nextBtn.setAllowInsert(false);
                current.setItem(50, nextBtn);
            }
        }

        plugin.getInventoryFramework().openInventory(player, inventories.get(targetPage));
    }

    private void addVariantItems(List<InventoryItem> allItems, Player player, Material currentBlock, BlockAppearanceConfig config) {
        List<Material> variants;

        if (config.hasVariantGroup() && config.getCustomMaterials() != null) {
            variants = new ArrayList<>(config.getCustomMaterials());
        } else {
            String baseType = config.getBaseBlockType();
            if (baseType == null || baseType.isEmpty()) baseType = currentBlock.name();
            if (baseType.contains("_")) baseType = baseType.substring(baseType.lastIndexOf("_") + 1);
            variants = new ArrayList<>(BlockListParser.parseBlockList("*" + baseType));
        }

        variants.sort(Comparator.comparing(Enum::name));
        BlockData currentBlockData = HideAndSeek.getDataController().getChosenBlockData(player.getUniqueId());

        for (Material variant : variants) {
            boolean isSelected = currentBlockData != null && currentBlockData.getMaterial() == variant;

            int myPage = allItems.size() / 45;

            InventoryItem variantItem = new InventoryItem(createVariantItem(variant, isSelected));
            variantItem.setClickHandler((p, item, event, slot) -> {
                BlockData previousData = HideAndSeek.getDataController().getChosenBlockData(p.getUniqueId());
                BlockData newData = variant.createBlockData();
                if (previousData != null) copySharedBlockStates(previousData, newData);

                updatePlayerAppearance(p, variant, newData);

                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.5f, 1.5f);
                open(p, myPage);
                event.setCancelled(true);
            });
            variantItem.setAllowTakeout(false);
            variantItem.setAllowInsert(false);
            variantItem.setMetadata("variant_material", variant.name());
            variantItem.setMetadata("is_selected", isSelected);

            allItems.add(variantItem);
        }
    }

    private void addBlockStateItems(List<InventoryItem> allItems, Player player, Material block, BlockAppearanceConfig config) {
        BlockData data = HideAndSeek.getDataController().getChosenBlockData(player.getUniqueId());
        if (data == null) data = block.createBlockData();

        for (String prop : getAllBlockStateProperties(data)) {
            if (BlockStateFilter.isDisallowed(prop)) continue;
            if (!config.getAllowedProperties().isEmpty() && !config.getAllowedProperties().contains(prop)) continue;

            final String propertyName = prop;

            int myPage = allItems.size() / 45;

            InventoryItem stateItem = new InventoryItem(createStateItem(propertyName, player, data));
            stateItem.setClickHandler((p, item, event, slot) -> {
                cyclePropertyValue(p, propertyName, event.getClick() == ClickType.RIGHT);
                p.playSound(p.getLocation(), Sound.UI_BUTTON_CLICK, 0.5f, 1.2f);
                open(p, myPage);
                event.setCancelled(true);
            });
            stateItem.setAllowTakeout(false);
            stateItem.setAllowInsert(false);
            stateItem.setMetadata("property_name", propertyName);

            allItems.add(stateItem);
        }
    }

    private void cyclePropertyValue(Player player, String property, boolean reverse) {
        BlockData data = HideAndSeek.getDataController().getChosenBlockData(player.getUniqueId());
        if (data == null) data = HideAndSeek.getDataController().getChosenBlock(player.getUniqueId()).createBlockData();

        List<String> possible = discoverPropertyValues(data, property);
        if (possible.isEmpty()) return;

        String currentVal = getPropertyValueFromString(data, property);
        int index = possible.indexOf(currentVal);
        int nextIndex = reverse ? (index - 1 + possible.size()) % possible.size() : (index + 1) % possible.size();

        try {
            String updated = buildDataStringWithProperty(data.getAsString(), property, possible.get(nextIndex));
            updatePlayerAppearance(player, data.getMaterial(), Bukkit.createBlockData(updated));
        } catch (Exception ignored) {
        }
    }

    private List<String> discoverPropertyValues(BlockData data, String property) {
        List<String> values = new ArrayList<>();
        String dataString = data.getAsString();

        if (isValidValue(dataString, property, "true")) return Arrays.asList("false", "true");

        if (property.equals("age") || property.equals("level") || property.equals("candles") || property.equals("layers")) {
            for (int i = 0; i <= 15; i++) {
                if (isValidValue(dataString, property, String.valueOf(i))) values.add(String.valueOf(i));
            }
            return values;
        }

        for (String val : UNIVERSAL_VALUE_POOL) {
            if (isValidValue(dataString, property, val)) values.add(val);
        }
        Collections.sort(values);
        return values;
    }

    private void copySharedBlockStates(BlockData source, BlockData target) {
        String sourceStr = source.getAsString();
        if (!sourceStr.contains("[")) return;
        String props = sourceStr.substring(sourceStr.indexOf("[") + 1, sourceStr.lastIndexOf("]"));
        for (String pair : props.split(",")) {
            String[] kv = pair.split("=");
            try {
                String targetStr = buildDataStringWithProperty(target.getAsString(), kv[0], kv[1]);
                target.copyTo(Bukkit.createBlockData(targetStr));
            } catch (Exception ignored) {
            }
        }
    }

    private boolean isValidValue(String dataString, String property, String value) {
        try {
            Bukkit.createBlockData(buildDataStringWithProperty(dataString, property, value));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private String buildDataStringWithProperty(String dataString, String property, String value) {
        int start = dataString.indexOf('[');
        int end = dataString.indexOf(']');
        String base = (start == -1) ? dataString : dataString.substring(0, start);
        String props = (start != -1) ? dataString.substring(start + 1, end) : "";

        Map<String, String> map = new HashMap<>();
        if (!props.isEmpty()) {
            for (String p : props.split(",")) {
                String[] kv = p.split("=");
                map.put(kv[0], kv[1]);
            }
        }
        map.put(property, value);
        StringJoiner sj = new StringJoiner(",");
        map.forEach((k, v) -> sj.add(k + "=" + v));
        return base + "[" + sj + "]";
    }

    private List<String> getAllBlockStateProperties(BlockData data) {
        String s = data.getAsString();
        if (!s.contains("[")) return new ArrayList<>();
        String props = s.substring(s.indexOf("[") + 1, s.lastIndexOf("]"));
        return Arrays.stream(props.split(",")).map(p -> p.split("=")[0]).collect(Collectors.toList());
    }

    private String getPropertyValueFromString(BlockData data, String property) {
        String s = data.getAsString();
        if (!s.contains(property + "=")) return "";
        String after = s.substring(s.indexOf(property + "=") + property.length() + 1);
        int end = after.indexOf(',');
        if (end == -1) end = after.indexOf(']');
        return end == -1 ? after : after.substring(0, end);
    }

    private void updatePlayerAppearance(Player p, Material mat, BlockData data) {
        HideAndSeek.getDataController().setChosenBlock(p.getUniqueId(), mat);
        HideAndSeek.getDataController().setChosenBlockData(p.getUniqueId(), data);
        var display = HideAndSeek.getDataController().getBlockDisplay(p.getUniqueId());
        if (display != null) display.setBlock(data);
        de.thecoolcraft11.hideAndSeek.items.HiderItems.updateAppearanceItem(p, plugin);
    }

    private void padToRow(List<InventoryItem> items) {
        while (items.size() % 9 != 0) items.add(new InventoryItem(new ItemStack(Material.AIR)));
    }

    private void addFooter(FrameworkInventory inv) {
        ItemStack sep = createSeparatorItem();
        for (int i = 45; i < 54; i++) inv.setItem(i, new InventoryItem(sep));
        inv.setItem(49, new InventoryItem(createConfirmItem()).onClick((p, t) -> p.closeInventory()));
    }


    private ItemStack createVariantItem(Material material, boolean selected) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(formatName(material.name()), selected ? NamedTextColor.GREEN : NamedTextColor.YELLOW, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));

            if (selected) {
                meta.setEnchantmentGlintOverride(true);
                meta.lore(List.of(Component.text("Currently Selected", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createStateItem(String state, Player player, BlockData base) {
        BlockData current = HideAndSeek.getDataController().getChosenBlockData(player.getUniqueId());
        if (current == null) current = base;
        String val = getPropertyValueFromString(current, state);


        ItemStack item = getDynamicStateIcon(state, val);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.displayName(Component.text(formatName(state), NamedTextColor.AQUA, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));


            List<String> possible = discoverPropertyValues(current, state);
            int index = possible.indexOf(val);
            String next = possible.isEmpty() ? "N/A" : possible.get((index + 1) % possible.size());
            String prev = possible.isEmpty() ? "N/A" : possible.get((index - 1 + possible.size()) % possible.size());

            meta.lore(Arrays.asList(
                    Component.text("Current: " + val, NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false),
                    Component.text(""),
                    Component.text("Left-Click: Cycle to " + next, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false),
                    Component.text("Right-Click: Cycle to " + prev, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
            ));

            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack getDynamicStateIcon(String state, String val) {
        return switch (state) {
            case "facing", "rotation" -> new ItemStack(Material.COMPASS);
            case "axis" -> new ItemStack(Material.STICK);
            case "waterlogged" -> new ItemStack(Material.WATER_BUCKET);
            case "lit", "powered" -> new ItemStack("true".equals(val) ? Material.REDSTONE_TORCH : Material.LEVER);
            case "open" -> new ItemStack(Material.OAK_DOOR);
            case "half", "type" -> new ItemStack(Material.SMOOTH_STONE_SLAB);
            case "shape" -> new ItemStack(Material.OAK_STAIRS);
            case "age", "level", "bites", "honey_level" -> new ItemStack(Material.EXPERIENCE_BOTTLE);
            case "candles" -> new ItemStack(Material.CANDLE);
            case "snowy" -> new ItemStack(Material.SNOWBALL);
            case "hinge" -> new ItemStack(Material.TRIPWIRE_HOOK);
            default -> new ItemStack(Material.REPEATER);
        };
    }

    private ItemStack createSectionHeaderItem(String text) {
        ItemStack item = new ItemStack(Material.LIME_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(text, NamedTextColor.GOLD, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
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

    private ItemStack createConfirmItem() {
        ItemStack item = new ItemStack(Material.GREEN_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Confirm", NamedTextColor.GREEN, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
            item.setItemMeta(meta);
        }
        return item;
    }

    private ItemStack createNavArrow(String name) {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(name, NamedTextColor.WHITE).decoration(TextDecoration.ITALIC, false));
            item.setItemMeta(meta);
        }
        return item;
    }

    private String formatName(String name) {
        return Arrays.stream(name.toLowerCase().split("_")).map(s -> s.substring(0, 1).toUpperCase() + s.substring(1)).collect(Collectors.joining(" "));
    }
}
