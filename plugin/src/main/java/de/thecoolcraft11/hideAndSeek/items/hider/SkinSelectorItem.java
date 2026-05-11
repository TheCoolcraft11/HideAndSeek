package de.thecoolcraft11.hideAndSeek.items.hider;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.gui.SkinSelectorGUI;
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

public class SkinSelectorItem implements GameItem {

    public static final String ID = "has_hider_skin_selector";

    private static void open(Player player, SkinSelectorGUI gui) {
        gui.open(player);
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public ItemStack createItem(HideAndSeek plugin) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(
                    Component.text("Skin Selector", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD)
                            .decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.text("Right-click to choose your disguise skin", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false)
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    public String getDescription(HideAndSeek plugin) {
        return "Open the skin picker to choose your disguise skin.";
    }

    @Override
    public void register(HideAndSeek plugin) {
        SkinSelectorGUI gui = plugin.getSkinSelectorGUI();

        plugin.getCustomItemManager().registerItem(
                new CustomItemBuilder(createItem(plugin), getId())
                        .withAction(ItemActionType.RIGHT_CLICK_AIR,
                                ctx -> open(ctx.getPlayer(), gui))
                        .withAction(ItemActionType.RIGHT_CLICK_BLOCK,
                                ctx -> open(ctx.getPlayer(), gui))
                        .withAction(ItemActionType.SHIFT_RIGHT_CLICK_AIR,
                                ctx -> open(ctx.getPlayer(), gui))
                        .withAction(ItemActionType.SHIFT_RIGHT_CLICK_BLOCK,
                                ctx -> open(ctx.getPlayer(), gui))
                        .withDescription(getDescription(plugin))
                        .withDropPrevention(true)
                        .withCraftPrevention(true)
                        .allowOffHand(false)
                        .allowArmor(false)
                        .cancelDefaultAction(true)
                        .build());
    }
}