package de.thecoolcraft11.hideAndSeek.items.hider;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.gui.BlockSelectorGUI;
import de.thecoolcraft11.hideAndSeek.items.api.GameItem;
import de.thecoolcraft11.minigameframework.items.CustomItemBuilder;
import de.thecoolcraft11.minigameframework.items.ItemActionType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class BlockSelectorItem implements GameItem {
    public static final String ID = "has_hider_block_selector";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public ItemStack createItem(HideAndSeek plugin) {
        ItemStack item = new ItemStack(Material.COMMAND_BLOCK);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.displayName(Component.text("Block Selector", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.text("Right click to choose your block", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false)
            ));
            item.setItemMeta(meta);
        }

        return item;
    }

    @Override
    public String getDescription(HideAndSeek plugin) {
        return "Open the block picker to choose your disguise block.";
    }

    @Override
    public void register(HideAndSeek plugin) {
        BlockSelectorGUI blockSelectorGUI = plugin.getBlockSelectorGUI();
        plugin.getCustomItemManager().registerItem(new CustomItemBuilder(createItem(plugin), getId())
                .withAction(ItemActionType.RIGHT_CLICK_AIR, context -> openBlockSelectorUnhidden(context.getPlayer(), blockSelectorGUI))
                .withAction(ItemActionType.RIGHT_CLICK_BLOCK, context -> openBlockSelectorUnhidden(context.getPlayer(), blockSelectorGUI))
                .withAction(ItemActionType.SHIFT_RIGHT_CLICK_AIR, context -> openBlockSelectorUnhidden(context.getPlayer(), blockSelectorGUI))
                .withAction(ItemActionType.SHIFT_RIGHT_CLICK_BLOCK, context -> openBlockSelectorUnhidden(context.getPlayer(), blockSelectorGUI))
                .withDescription(getDescription(plugin))
                .withDropPrevention(true)
                .withCraftPrevention(true)
                .allowOffHand(false)
                .allowArmor(false)
                .cancelDefaultAction(true)
                .build());
    }

    private static void openBlockSelectorUnhidden(Player player, BlockSelectorGUI blockSelectorGUI) {
        if (HideAndSeek.getDataController().isHidden(player.getUniqueId())) return;
        blockSelectorGUI.open(player);
    }
}

