package de.thecoolcraft11.hideAndSeek.items.seeker;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.gui.SkinStatsGUI;
import de.thecoolcraft11.hideAndSeek.items.api.GameItem;
import de.thecoolcraft11.minigameframework.items.CustomItemBuilder;
import de.thecoolcraft11.minigameframework.items.ItemActionType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class SkinStatsItem implements GameItem {

    public static final String ID = "has_seeker_skin_stats";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public ItemStack createItem(HideAndSeek plugin) {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(
                    Component.text("Skin Statistics", NamedTextColor.AQUA, TextDecoration.BOLD)
                            .decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.text("Right-click to view hider skins", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false)
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    public String getDescription(HideAndSeek plugin) {
        return "View which skin each hider is currently disguised as.";
    }

    @Override
    public void register(HideAndSeek plugin) {
        SkinStatsGUI gui = new SkinStatsGUI(plugin, plugin.getSkinManager());

        plugin.getCustomItemManager().registerItem(
                new CustomItemBuilder(createItem(plugin), getId())
                        .withDescription(getDescription(plugin))
                        .withAction(ItemActionType.RIGHT_CLICK_AIR,
                                ctx -> gui.open(ctx.getPlayer()))
                        .withAction(ItemActionType.RIGHT_CLICK_BLOCK,
                                ctx -> gui.open(ctx.getPlayer()))
                        .withDropPrevention(true)
                        .withCraftPrevention(true)
                        .allowOffHand(false)
                        .allowArmor(false)
                        .build());
    }
}