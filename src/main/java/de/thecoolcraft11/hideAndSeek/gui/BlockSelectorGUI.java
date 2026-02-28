package de.thecoolcraft11.hideAndSeek.gui;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.block.BlockAppearanceConfig;
import de.thecoolcraft11.minigameframework.inventory.FrameworkInventory;
import de.thecoolcraft11.minigameframework.inventory.InventoryItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BlockSelectorGUI {
    private final HideAndSeek plugin;
    private final Map<UUID, BlockAppearanceConfig> playerConfigs;

    public BlockSelectorGUI(HideAndSeek plugin) {
        this.plugin = plugin;
        this.playerConfigs = new java.util.HashMap<>();
    }

    public void open(Player player) {
        if (!plugin.getStateManager().getCurrentPhaseId().equals("hiding")) {
            player.sendMessage(Component.text("Block selector only available during Hiding phase!", NamedTextColor.RED));
            return;
        }
        if (!HideAndSeek.getDataController().getHiders().contains(player.getUniqueId())) {
            player.sendMessage(Component.text("Only hiders can use this!", NamedTextColor.RED));
            return;
        }
        var gameModeResult = plugin.getSettingService().getSetting("game.gametype");
        Object gameModeObj = gameModeResult.isSuccess() ? gameModeResult.getValue() : null;
        if (gameModeObj == null || !gameModeObj.toString().equals("BLOCK")) {
            player.sendMessage(Component.text("Block mode is not enabled!", NamedTextColor.RED));
            return;
        }
        String currentMap = HideAndSeek.getDataController().getCurrentMapName();
        List<String> allowedBlocks = new ArrayList<>();
        if (currentMap != null && !currentMap.isEmpty()) {
            allowedBlocks = plugin.getMapManager().getAllowedBlocksForMap(currentMap);
        }
        if (allowedBlocks.isEmpty()) {
            player.sendMessage(Component.text("No allowed blocks configured for this map!", NamedTextColor.RED));
            return;
        }


        List<Material> displayMaterials = new ArrayList<>();
        Map<Material, BlockAppearanceConfig> configMap = new java.util.HashMap<>();

        for (String blockPattern : allowedBlocks) {
            BlockAppearanceConfig config = BlockAppearanceConfig.parse(blockPattern);
            if (config == null) {
                plugin.getLogger().warning("Invalid block pattern: " + blockPattern);
                continue;
            }


            if (config.isCustomList() && config.hasVariantGroup() && !config.shouldShowAllVariantsInSelector()) {

                if (config.getDefaultVariant() != null) {
                    try {
                        Material defaultMat = Material.valueOf(config.getDefaultVariant());
                        if (!displayMaterials.contains(defaultMat)) {
                            displayMaterials.add(defaultMat);
                            configMap.put(defaultMat, config);
                        }
                    } catch (IllegalArgumentException e) {
                        plugin.getLogger().warning("Invalid default variant: " + config.getDefaultVariant());
                    }
                }
                continue;
            }


            if (config.isCustomList() && config.shouldShowAllVariantsInSelector()) {
                for (Material mat : config.getCustomMaterials()) {
                    if (!displayMaterials.contains(mat)) {
                        displayMaterials.add(mat);
                        configMap.put(mat, config);
                    }
                }
                continue;
            }


            Material displayMaterial = null;

            if (config.isAllowAllVariants() && config.getDefaultVariant() != null) {
                try {
                    displayMaterial = Material.valueOf(config.getDefaultVariant());
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid default variant: " + config.getDefaultVariant());
                }
            } else {
                try {
                    displayMaterial = Material.valueOf(config.getBaseBlockType());
                } catch (IllegalArgumentException e) {
                    plugin.getLogger().warning("Invalid base block type: " + config.getBaseBlockType());
                }
            }

            if (displayMaterial != null && !displayMaterials.contains(displayMaterial)) {
                displayMaterials.add(displayMaterial);
                configMap.put(displayMaterial, config);
            }
        }

        int rows = Math.min(6, (displayMaterials.size() + 8) / 9);
        FrameworkInventory inventory = plugin.getInventoryFramework().create("Choose Your Block", rows);
        inventory.setSetting("allow_outside_clicks", false);
        inventory.setSetting("allow_drag", false);
        inventory.setSetting("allow_player_inventory_interaction", false);
        Material currentlyChosen = HideAndSeek.getDataController().getChosenBlock(player.getUniqueId());
        int slot = 0;
        for (Material material : displayMaterials) {
            if (slot >= rows * 9) break;
            BlockAppearanceConfig config = configMap.get(material);
            ItemStack item = createBlockItem(material, currentlyChosen == material);
            inventory.setItem(slot, new InventoryItem(item).onClick((p, type) -> {
                if (type == ClickType.LEFT) {
                    HideAndSeek.getDataController().setChosenBlock(p.getUniqueId(), material);
                    playerConfigs.put(p.getUniqueId(), config);

                    if (config.getDefaultVariant() != null) {
                        try {
                            Material variantMaterial = Material.valueOf(config.getDefaultVariant());
                            HideAndSeek.getDataController().setChosenBlockData(p.getUniqueId(), variantMaterial.createBlockData());
                        } catch (IllegalArgumentException e) {
                            HideAndSeek.getDataController().setChosenBlockData(p.getUniqueId(), material.createBlockData());
                        }
                    } else {
                        HideAndSeek.getDataController().setChosenBlockData(p.getUniqueId(), material.createBlockData());
                    }


                    de.thecoolcraft11.hideAndSeek.items.HiderItems.updateAppearanceItem(p, plugin);

                    p.sendMessage(Component.text("Selected ", NamedTextColor.GREEN)
                            .append(Component.text(formatName(material.name()), NamedTextColor.GOLD)));


                    if (config.isAllowAllVariants() || config.hasVariantGroup() || config.isAllowAllBlockStates() || !config.getAllowedStates().isEmpty()) {
                        p.closeInventory();

                        Bukkit.getScheduler().runTaskLater(plugin, () -> new AppearanceGUI(plugin, BlockSelectorGUI.this).open(p), 1L);
                    } else {
                        p.closeInventory();
                    }
                }
            }));
            slot++;
        }
        plugin.getInventoryFramework().openInventory(player, inventory);
    }

    public BlockAppearanceConfig getPlayerConfig(UUID uuid) {
        return playerConfigs.get(uuid);
    }

    public void setPlayerConfig(UUID uuid, BlockAppearanceConfig config) {
        playerConfigs.put(uuid, config);
    }

    public BlockAppearanceConfig resolveConfigForMaterial(List<String> allowedBlocks, Material material) {
        if (allowedBlocks == null || material == null) {
            return null;
        }
        for (String pattern : allowedBlocks) {
            BlockAppearanceConfig config = BlockAppearanceConfig.parse(pattern);
            if (config == null) {
                continue;
            }
            if (matchesConfig(config, material)) {
                return config;
            }
        }
        return BlockAppearanceConfig.parse(material.name());
    }

    private boolean matchesConfig(BlockAppearanceConfig config, Material material) {
        String materialName = material.name();
        String base = config.getBaseBlockType();


        if (config.isCustomList() && config.getCustomMaterials() != null) {
            if (config.getCustomMaterials().contains(material)) {
                return true;
            }
        }


        if (config.isAllowAllVariants()) {
            return materialName.endsWith(base);
        }


        return materialName.equals(base);
    }

    private ItemStack createBlockItem(Material material, boolean isSelected) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            String name = formatName(material.name());
            meta.displayName(Component.text(("") + name,
                            isSelected ? NamedTextColor.GREEN : NamedTextColor.YELLOW, TextDecoration.BOLD)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(Component.text("Click to select", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
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
}

