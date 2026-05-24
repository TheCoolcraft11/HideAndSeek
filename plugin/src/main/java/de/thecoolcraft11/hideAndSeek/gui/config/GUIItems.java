package de.thecoolcraft11.hideAndSeek.gui.config;

import de.thecoolcraft11.minigameframework.gui.config.GuiItemRegistry;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.TooltipDisplay;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class GUIItems {

    public static final String KEY_BACK = "back";
    public static final String KEY_CLEAR = "clear";
    public static final String KEY_FALLBACK = "fallback";
    public static final String KEY_SEPERATOR = "seperator";
    public static final String KEY_PREVIOUS = "previous";
    public static final String KEY_NEXT = "next";
    public static final String KEY_READY = "ready";
    public static final String KEY_NOT_READY = "not_ready";

    public static final String SKIN_COIN = "coin";

    public static final String VOTE_MODE_BASE = "gamemode_";
    public static final String VOTE_MODE_NORMAL = VOTE_MODE_BASE + "normal";
    public static final String VOTE_MODE_SMALL = VOTE_MODE_BASE + "small";
    public static final String VOTE_MODE_BLOCK = VOTE_MODE_BASE + "block";
    public static final String VOTE_MODE_SKIN = VOTE_MODE_BASE + "skin";
    public static final String VOTE_MAP_LOCKED = "map_locked";
    public static final String VOTE_MAP_DEFAULT = "map_default";
    public static final String VOTE_NO_MAP = "no_map";
    public static final String VOTE_ROLE_HIDER = "role_hider";
    public static final String VOTE_ROLE_SEEKER = "role_seeker";
    public static final String VOTE_SELF = "self";

    public static final String ST_TARGET = "teleport_target";
    public static final String ST_INDICATOR = "indicator";

    public static final String R_PLAYER = "player";
    public static final String R_OVERFLOW = "overflow";

    public static final String MAP_RANDOM = "random";

    public static void registerAll(GuiItemRegistry registry) {
        registry.registerDefault(GUINames.SKIN, SKIN_COIN, new ItemStack(Material.GOLD_NUGGET));
        registry.registerDefault(GUINames.SKIN, KEY_BACK, new ItemStack(Material.ARROW));
        registry.registerDefault(GUINames.SKIN, KEY_CLEAR, new ItemStack(Material.BARRIER));

        registry.registerDefault(GUINames.SKIN_STATS, KEY_FALLBACK, new ItemStack(Material.PLAYER_HEAD));
        registry.registerDefault(GUINames.SKIN_SELECTOR, KEY_FALLBACK, new ItemStack(Material.PLAYER_HEAD));

        registry.registerDefault(GUINames.VOTE, VOTE_MODE_NORMAL, new ItemStack(Material.IRON_SWORD));
        registry.registerDefault(GUINames.VOTE, VOTE_MODE_SMALL, new ItemStack(Material.IRON_NUGGET));
        registry.registerDefault(GUINames.VOTE, VOTE_MODE_BLOCK, new ItemStack(Material.COBBLESTONE));
        registry.registerDefault(GUINames.VOTE, VOTE_MODE_SKIN, new ItemStack(Material.PLAYER_HEAD));
        registry.registerDefault(GUINames.VOTE, KEY_SEPERATOR, createSeparatorItem());
        registry.registerDefault(GUINames.VOTE, VOTE_MAP_LOCKED, new ItemStack(Material.DIRT_PATH));
        registry.registerDefault(GUINames.VOTE, VOTE_MAP_DEFAULT, new ItemStack(Material.GRASS_BLOCK));
        registry.registerDefault(GUINames.VOTE, VOTE_NO_MAP, new ItemStack(Material.BARRIER));
        registry.registerDefault(GUINames.VOTE, VOTE_ROLE_HIDER, new ItemStack(Material.LIME_WOOL));
        registry.registerDefault(GUINames.VOTE, VOTE_ROLE_SEEKER, new ItemStack(Material.RED_WOOL));
        registry.registerDefault(GUINames.VOTE, VOTE_SELF, new ItemStack(Material.PLAYER_HEAD));
        registry.registerDefault(GUINames.VOTE, KEY_READY, new ItemStack(Material.LIME_STAINED_GLASS_PANE));
        registry.registerDefault(GUINames.VOTE, KEY_NOT_READY, new ItemStack(Material.RED_STAINED_GLASS_PANE));

        registry.registerDefault(GUINames.SPECTATOR_TELEPORT, KEY_SEPERATOR, createSeparatorItem());
        registry.registerDefault(GUINames.SPECTATOR_TELEPORT, KEY_SEPERATOR, createSeparatorItem());
        registry.registerDefault(GUINames.SPECTATOR_TELEPORT, ST_TARGET, new ItemStack(Material.PLAYER_HEAD));
        registry.registerDefault(GUINames.SPECTATOR_TELEPORT, KEY_PREVIOUS, new ItemStack(Material.ARROW));
        registry.registerDefault(GUINames.SPECTATOR_TELEPORT, KEY_NEXT, new ItemStack(Material.ARROW));
        registry.registerDefault(GUINames.SPECTATOR_TELEPORT, ST_INDICATOR, new ItemStack(Material.BOOK));

        registry.registerDefault(GUINames.READY, R_PLAYER, new ItemStack(Material.PLAYER_HEAD));
        registry.registerDefault(GUINames.READY, KEY_READY, new ItemStack(Material.LIME_STAINED_GLASS_PANE));
        registry.registerDefault(GUINames.READY, KEY_NOT_READY, new ItemStack(Material.RED_STAINED_GLASS_PANE));
        registry.registerDefault(GUINames.READY, R_OVERFLOW, new ItemStack(Material.BOOK));

        registry.registerDefault(GUINames.MAP, KEY_FALLBACK, new ItemStack(Material.GRASS_BLOCK));
        registry.registerDefault(GUINames.MAP, MAP_RANDOM, new ItemStack(Material.COMPASS));
    }


    @SuppressWarnings("UnstableApiUsage")
    public static ItemStack createSeparatorItem() {
        ItemStack item = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(" "));
            item.setItemMeta(meta);
            item.setData(DataComponentTypes.TOOLTIP_DISPLAY, TooltipDisplay.tooltipDisplay().hideTooltip(true).build());
        }
        return item;
    }
}
