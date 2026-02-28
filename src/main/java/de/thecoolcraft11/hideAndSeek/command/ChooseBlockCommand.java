package de.thecoolcraft11.hideAndSeek.command;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.minigameframework.commands.MinigameSubcommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ChooseBlockCommand implements MinigameSubcommand {
    private final HideAndSeek plugin;

    public ChooseBlockCommand(HideAndSeek plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getName() {
        return "chooseblock";
    }

    @Override
    public @NotNull List<String> getAliases() {
        return List.of("cb", "block");
    }

    @Override
    public boolean handle(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be used by players!", NamedTextColor.RED));
            return true;
        }


        if (!HideAndSeek.getDataController().getHiders().contains(player.getUniqueId())) {
            player.sendMessage(Component.text("Only hiders can choose a block!", NamedTextColor.RED));
            return true;
        }


        var gameModeResult = plugin.getSettingService().getSetting("game.gametype");
        Object gameModeObj = gameModeResult.isSuccess() ? gameModeResult.getValue() : null;

        if (gameModeObj == null || !gameModeObj.toString().equals("BLOCK")) {
            player.sendMessage(Component.text("Block mode is not enabled!", NamedTextColor.RED));
            return true;
        }

        if (args.length < 1) {
            player.sendMessage(Component.text("Usage: /mg chooseblock <block_type> [blockstates]", NamedTextColor.YELLOW));
            player.sendMessage(Component.text("Example: /mg chooseblock STONE", NamedTextColor.GRAY));
            player.sendMessage(Component.text("Example: /mg chooseblock OAK_LOG[axis=y]", NamedTextColor.GRAY));
            return true;
        }


        String fullArg = args[0].toUpperCase();
        String blockName = fullArg;
        String blockStateString = null;


        if (fullArg.contains("[") && fullArg.contains("]")) {
            int bracketStart = fullArg.indexOf('[');
            int bracketEnd = fullArg.indexOf(']');
            blockName = fullArg.substring(0, bracketStart);
            blockStateString = fullArg.substring(bracketStart + 1, bracketEnd);
        }

        Material material;

        try {
            material = Material.valueOf(blockName);
        } catch (IllegalArgumentException e) {
            player.sendMessage(Component.text("Invalid block type: " + blockName, NamedTextColor.RED));
            player.sendMessage(Component.text("Try: STONE, GRASS_BLOCK, OAK_LOG, etc.", NamedTextColor.GRAY));
            return true;
        }

        if (!material.isBlock() || material.isAir()) {
            player.sendMessage(Component.text("You must choose a valid block!", NamedTextColor.RED));
            return true;
        }


        String currentMap = HideAndSeek.getDataController().getCurrentMapName();
        if (currentMap != null && !currentMap.isEmpty()) {
            List<String> allowedBlocks = plugin.getMapManager().getAllowedBlocksForMap(currentMap);
            if (!allowedBlocks.isEmpty()) {
                var selector = plugin.getBlockSelectorGUI();
                var resolvedConfig = selector.resolveConfigForMaterial(allowedBlocks, material);
                if (resolvedConfig == null) {
                    player.sendMessage(Component.text("That block is not allowed on this map!", NamedTextColor.RED));
                    player.sendMessage(Component.text("Allowed blocks: " + String.join(", ", allowedBlocks), NamedTextColor.GRAY));
                    return true;
                }
                selector.setPlayerConfig(player.getUniqueId(), resolvedConfig);


                org.bukkit.block.data.BlockData blockData;
                if (resolvedConfig.getDefaultVariant() != null) {
                    try {
                        Material variantMaterial = Material.valueOf(resolvedConfig.getDefaultVariant());
                        blockData = variantMaterial.createBlockData();
                    } catch (IllegalArgumentException e) {
                        blockData = material.createBlockData();
                    }
                } else {
                    blockData = material.createBlockData();
                }


                if (blockStateString != null && !blockStateString.isEmpty()) {
                    try {
                        blockData = org.bukkit.Bukkit.createBlockData(blockData.getMaterial(), "[" + blockStateString + "]");
                    } catch (IllegalArgumentException e) {
                        player.sendMessage(Component.text("Invalid blockstate: " + blockStateString, NamedTextColor.RED));
                        player.sendMessage(Component.text("Using default blockstate instead", NamedTextColor.GRAY));
                    }
                }

                HideAndSeek.getDataController().setChosenBlockData(player.getUniqueId(), blockData);
            }
        }


        HideAndSeek.getDataController().setChosenBlock(player.getUniqueId(), material);
        de.thecoolcraft11.hideAndSeek.items.HiderItems.updateAppearanceItem(player, plugin);

        player.sendMessage(Component.text()
                .append(Component.text("", NamedTextColor.GREEN))
                .append(Component.text("You will transform into a ", NamedTextColor.YELLOW))
                .append(Component.text(material.name(), NamedTextColor.GOLD))
                .append(Component.text("!", NamedTextColor.YELLOW))
                .build());

        plugin.getLogger().info(player.getName() + " chose block: " + material.name());

        return true;
    }

    @Override
    public @Nullable List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length == 1) {

            String currentMap = HideAndSeek.getDataController().getCurrentMapName();
            if (currentMap != null && !currentMap.isEmpty()) {
                List<String> allowedBlocks = plugin.getMapManager().getAllowedBlocksForMap(currentMap);
                if (!allowedBlocks.isEmpty()) {
                    String prefix = args[0].toUpperCase();
                    List<String> completions = new ArrayList<>();

                    for (String pattern : allowedBlocks) {

                        de.thecoolcraft11.hideAndSeek.block.BlockAppearanceConfig config =
                                de.thecoolcraft11.hideAndSeek.block.BlockAppearanceConfig.parse(pattern);
                        if (config != null) {
                            String materialName;
                            if (config.isAllowAllVariants() && config.getDefaultVariant() != null) {
                                materialName = config.getDefaultVariant();
                            } else {
                                materialName = config.getBaseBlockType();
                            }

                            if (materialName.startsWith(prefix)) {
                                completions.add(materialName);
                            }
                        }
                    }

                    return completions.stream()
                            .distinct()
                            .sorted()
                            .collect(Collectors.toList());
                }
            }


            List<String> commonBlocks = Arrays.asList(
                    "STONE", "GRASS_BLOCK", "DIRT", "COBBLESTONE", "OAK_LOG", "BIRCH_LOG",
                    "SPRUCE_LOG", "JUNGLE_LOG", "ACACIA_LOG", "DARK_OAK_LOG", "OAK_PLANKS",
                    "STONE_BRICKS", "BRICKS", "SAND", "GRAVEL", "HAY_BLOCK", "MELON",
                    "PUMPKIN", "BARREL", "CHEST", "CRAFTING_TABLE", "FURNACE"
            );

            String prefix = args[0].toUpperCase();
            return commonBlocks.stream()
                    .filter(block -> block.startsWith(prefix))
                    .collect(Collectors.toList());
        }
        return List.of();
    }
}