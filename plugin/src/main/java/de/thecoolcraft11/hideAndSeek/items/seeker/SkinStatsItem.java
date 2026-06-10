package de.thecoolcraft11.hideAndSeek.items.seeker;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.gui.SkinStatsGUI;
import de.thecoolcraft11.hideAndSeek.items.api.GameItem;
import de.thecoolcraft11.minigameframework.items.CustomItemBuilder;
import de.thecoolcraft11.minigameframework.items.ItemActionType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Nullable;

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
            meta.displayName(MiniMessage.miniMessage().deserialize(plugin.trText(null, "item.skin_stats.name"))
                    .decoration(TextDecoration.ITALIC, false));
            String loreStr = plugin.trText(null, "item.skin_stats.lore");
            java.util.List<Component> lore = new java.util.ArrayList<>();
            for (String line : loreStr.split("\n")) {
                lore.add(MiniMessage.miniMessage().deserialize(line).decoration(TextDecoration.ITALIC, false));
            }
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    public String getDescription(HideAndSeek plugin, @Nullable Player player) {
        return plugin.trText(player, "item.skin_stats.description");
    }

    @Override
    public void register(HideAndSeek plugin) {
        SkinStatsGUI gui = new SkinStatsGUI(plugin, plugin.getSkinManager());

        plugin.getCustomItemManager().registerItem(
                new CustomItemBuilder(createItem(plugin), getId())
                        .withDescription(getDescription(plugin, null))
                        .withNameKey("item.skin_stats.name")
                        .withLoreKey("item.skin_stats.lore")
                        .withNameKey("item.skin_stats.name")
                        .withLoreKey("item.skin_stats.lore")
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