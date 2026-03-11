package de.thecoolcraft11.hideAndSeek.items.hider;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.block.BlockAppearanceConfig;
import de.thecoolcraft11.hideAndSeek.items.api.GameItem;
import de.thecoolcraft11.minigameframework.items.CustomItemBuilder;
import de.thecoolcraft11.minigameframework.items.ItemActionType;
import de.thecoolcraft11.minigameframework.items.ItemInteractionContext;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Random;
import java.util.Set;

import static de.thecoolcraft11.hideAndSeek.items.seeker.CurseSpellItem.isHiderCursed;

public class RandomBlockItem implements GameItem {
    public static final String ID = "has_hider_random_block";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public ItemStack createItem(HideAndSeek plugin) {
        int maxUses = plugin.getSettingRegistry().get("hider-items.random-block.uses", 5);
        ItemStack item = new ItemStack(Material.BLAZE_POWDER);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.displayName(Component.text("Random Block", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD).append(Component.space()).append(Component.text("(" + maxUses + "/" + maxUses + ")", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)));
            meta.lore(List.of(
                    Component.text("Right click to randomize", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false)
            ));
            item.setItemMeta(meta);
        }

        return item;
    }

    @Override
    public String getDescription(HideAndSeek plugin) {
        return "Reroll your disguise block from the map's allowed blocks.";
    }

    @Override
    public void register(HideAndSeek plugin) {
        int randomBlockCooldown = plugin.getSettingRegistry().get("hider-items.random-block.cooldown", 3);
        int randomUses = plugin.getSettingRegistry().get("hider-items.random-block.uses", 5);

        plugin.getCustomItemManager().registerItem(new CustomItemBuilder(createItem(plugin), getId())
                .withAction(ItemActionType.RIGHT_CLICK_AIR, context -> randomizeBlockWithContext(context, plugin))
                .withAction(ItemActionType.RIGHT_CLICK_BLOCK, context -> randomizeBlockWithContext(context, plugin))
                .withAction(ItemActionType.SHIFT_RIGHT_CLICK_AIR, context -> randomizeBlockWithContext(context, plugin))
                .withAction(ItemActionType.SHIFT_RIGHT_CLICK_BLOCK, context -> randomizeBlockWithContext(context, plugin))
                .withMaxPlayerUses(randomUses)
                .withVanillaCooldown(randomBlockCooldown * 20)
                .withCustomCooldown(randomBlockCooldown * 1000L)
                .withVanillaCooldownDisplay(true)
                .withDescription(getDescription(plugin))
                .withDropPrevention(true)
                .withCraftPrevention(true)
                .allowOffHand(false)
                .allowArmor(false)
                .cancelDefaultAction(true)
                .withUsesExhaustedHandler((context, isTeamLimit) -> context.getPlayer().sendMessage(Component.text("You ran out of random block uses!", NamedTextColor.RED)))
                .withAppearanceProvider((player, item, context) -> {
                    ItemStack itemStack = item.getItemStack();
                    ItemMeta meta = itemStack.getItemMeta();
                    meta.displayName(Component.text("Random Block", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD)
                            .append(Component.space())
                            .append(Component.text("(" + context.getPlayerRemainingUses() + "/" + context.getMaxPlayerUses() + ")", NamedTextColor.GRAY)
                                    .decoration(TextDecoration.ITALIC, false)));
                    itemStack.setItemMeta(meta);
                    return itemStack;
                })
                .build());
    }

    private static void randomizeBlockWithContext(ItemInteractionContext context, HideAndSeek plugin) {
        Player player = context.getPlayer();
        if (player == null) {
            context.skipCooldown();
            return;
        }
        if (!HideAndSeek.getDataController().getHiders().contains(player.getUniqueId())) {
            player.sendMessage(Component.text("Only hiders can use this item.", NamedTextColor.RED));
            context.skipCooldown();
            return;
        }
        if (isHiderCursed(player.getUniqueId())) {
            player.sendMessage(Component.text("You are cursed and cannot change blocks right now!", NamedTextColor.RED));
            context.skipCooldown();
            return;
        }

        randomizeBlock(player, plugin);
    }

    public static void randomizeBlock(Player player, HideAndSeek plugin) {
        if (player == null) {
            return;
        }
        if (!HideAndSeek.getDataController().getHiders().contains(player.getUniqueId())) {
            player.sendMessage(Component.text("Only hiders can use this item.", NamedTextColor.RED));
            return;
        }
        if (isHiderCursed(player.getUniqueId())) {
            player.sendMessage(Component.text("You are cursed and cannot change blocks right now!", NamedTextColor.RED));
            return;
        }

        if (HideAndSeek.getDataController().isHidden(player.getUniqueId())) {
            player.sendMessage(Component.text("You cant transform into a new block while being hidden!", NamedTextColor.RED));
            return;
        }

        player.getInventory().remove(Material.COMPARATOR);

        String currentMap = HideAndSeek.getDataController().getCurrentMapName();
        if (currentMap == null || currentMap.isEmpty()) {
            player.sendMessage(Component.text("No active map found.", NamedTextColor.RED));
            return;
        }

        List<String> allowedBlocks = plugin.getMapManager().getAllowedBlocksForMap(currentMap);
        if (allowedBlocks.isEmpty()) {
            player.sendMessage(Component.text("No allowed blocks configured for this map.", NamedTextColor.RED));
            return;
        }

        List<String> possibleBlocks = allowedBlocks.stream()
                .filter(block -> !block.equalsIgnoreCase(HideAndSeek.getDataController().getChosenBlock(player.getUniqueId()).name()))
                .toList();

        String chosenPattern = possibleBlocks.get(new Random().nextInt(allowedBlocks.size()));

        BlockAppearanceConfig config = BlockAppearanceConfig.parse(chosenPattern);

        if (config == null) {
            player.sendMessage(Component.text("Invalid block pattern in config: " + chosenPattern, NamedTextColor.RED));
            return;
        }

        Material chosenMaterial;

        if (config.getDefaultVariant() != null) {
            try {
                chosenMaterial = Material.valueOf(config.getDefaultVariant());
            } catch (IllegalArgumentException e) {
                player.sendMessage(Component.text("Invalid default variant in config: " + config.getDefaultVariant(), NamedTextColor.RED));
                return;
            }
        } else {
            try {
                chosenMaterial = Material.valueOf(config.getBaseBlockType());
            } catch (IllegalArgumentException e) {
                player.sendMessage(Component.text("Invalid block type in config: " + config.getBaseBlockType(), NamedTextColor.RED));
                return;
            }
        }

        HideAndSeek.getDataController().setChosenBlock(player.getUniqueId(), chosenMaterial);

        BlockData blockData = chosenMaterial.createBlockData();
        HideAndSeek.getDataController().setChosenBlockData(player.getUniqueId(), blockData);

        plugin.getBlockSelectorGUI().setPlayerConfig(player.getUniqueId(), config);

        var display = HideAndSeek.getDataController().getBlockDisplay(player.getUniqueId());
        if (display != null && display.isValid()) {
            display.setBlock(blockData);
            display.setRotation(player.getLocation().getYaw(), 0f);
        }

        HiderItemUtil.updateAppearanceItem(player, plugin);

        player.sendMessage(Component.text("Transformed into ", NamedTextColor.GREEN)
                .append(Component.text(HiderItemUtil.formatName(chosenMaterial.name()), NamedTextColor.GOLD)));
    }

    public static void randomizeBlockFor(Player player, HideAndSeek plugin, boolean forceUnhide) {
        if (player == null) {
            return;
        }
        if (forceUnhide && HideAndSeek.getDataController().isHidden(player.getUniqueId())) {
            plugin.getBlockModeListener().forceUnhide(player);
            org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> randomizeBlock(player, plugin), 1L);
        } else {
            randomizeBlock(player, plugin);
        }
    }

    @Override
    public Set<String> getConfigKeys() {
        return Set.of("hider-items.random-block.cooldown");
    }
}
