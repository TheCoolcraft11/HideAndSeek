package de.thecoolcraft11.hideAndSeek.gui.config;

import de.thecoolcraft11.hideAndSeek.model.GameModeEnum;
import de.thecoolcraft11.hideAndSeek.model.ItemType;
import de.thecoolcraft11.minigameframework.gui.config.GuiItemRegistry;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.TooltipDisplay;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Locale;

public class GUIItems {

    public static final String KEY_BACK = "back";
    public static final String KEY_CLEAR = "clear";
    public static final String KEY_FALLBACK = "fallback";
    public static final String KEY_SEPERATOR = "seperator";
    public static final String KEY_PREVIOUS = "previous";
    public static final String KEY_NEXT = "next";
    public static final String KEY_READY = "ready";
    public static final String KEY_NOT_READY = "not_ready";
    public static final String KEY_INFO = "info";

    public static final String SKIN_COIN = "coin";

    public static final String VOTE_MODE_BASE = "gamemode_";
    public static final String VOTE_MODE_NORMAL = VOTE_MODE_BASE + GameModeEnum.NORMAL.name().toLowerCase(Locale.ROOT);
    public static final String VOTE_MODE_SMALL = VOTE_MODE_BASE + GameModeEnum.SMALL.name().toLowerCase(Locale.ROOT);
    public static final String VOTE_MODE_BLOCK = VOTE_MODE_BASE + GameModeEnum.BLOCK.name().toLowerCase(Locale.ROOT);
    public static final String VOTE_MODE_SKIN = VOTE_MODE_BASE + GameModeEnum.SKIN.name().toLowerCase(Locale.ROOT);
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

    public static final String L_SLOT_PREFS = "slot_prefs";
    public static final String L_PRESET_TITLE = "preset_title";
    public static final String L_PRESET_INFO = "preset_info";
    public static final String L_PRESET_EMPTY = "preset_empty";
    public static final String L_PRESET_FALLBACK = "presets_fallback";
    public static final String L_TAB_BASE = "tab_";
    public static final String L_TAB_HIDER = L_TAB_BASE + "hider";
    public static final String L_TAB_SEEKER = L_TAB_BASE + "seeker";
    public static final String L_TAB_PRESETS = L_TAB_BASE + "presets";
    public static final String L_RESTRICTED_ITEM_INFO = "restricted_item_info";
    public static final String L_ADMIN_PRESET_EMPTY = "admin_preset_empty";
    public static final String L_ADMIN_PRESET_DISABLED = "admin_preset_disabled";
    public static final String L_ADMIN_PRESET_FALLBACK = "admin_preset_fallback";
    public static final String L_NO_PREVIEW = "no_preview";

    public static final String SP_BACK_LOADOUT = "back_loadout";
    public static final String SP_BACK_PREFS = "back_prefs";
    public static final String SP_PRIMARY_SLOT = "primary_slot";
    public static final String SP_FALLBACK_SLOT = "fallback_slot";
    public static final String SP_FALLBACK_SLOT_DISABLED = "fallback_slot_disabled";
    public static final String SP_ITEM_TYPE_BASE = "item_type_";
    public static final String SP_ITEM_MOBILITY = SP_ITEM_TYPE_BASE + ItemType.MOBILITY.name().toLowerCase();
    public static final String SP_ITEM_UTILITY = SP_ITEM_TYPE_BASE + ItemType.UTILITY.name().toLowerCase();
    public static final String SP_ITEM_OFFENSE = SP_ITEM_TYPE_BASE + ItemType.OFFENSE.name().toLowerCase();
    public static final String SP_ITEM_TRAP = SP_ITEM_TYPE_BASE + ItemType.TRAP.name().toLowerCase();
    public static final String SP_ITEM_DEFENSE = SP_ITEM_TYPE_BASE + ItemType.DEFENSE.name().toLowerCase();
    public static final String SP_ITEM_HEALING = SP_ITEM_TYPE_BASE + ItemType.HEALING.name().toLowerCase();
    public static final String SP_ITEM_SUPPORT = SP_ITEM_TYPE_BASE + ItemType.SUPPORT.name().toLowerCase();
    public static final String SP_ITEM_INFORMATION = SP_ITEM_TYPE_BASE + ItemType.INFORMATION.name().toLowerCase();

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

        registry.registerDefault(GUINames.LOADOUT, L_SLOT_PREFS, new ItemStack(Material.HOPPER));
        registry.registerDefault(GUINames.LOADOUT, L_PRESET_TITLE, new ItemStack(Material.WRITABLE_BOOK));
        registry.registerDefault(GUINames.LOADOUT, L_PRESET_EMPTY, new ItemStack(Material.GRAY_STAINED_GLASS_PANE));
        registry.registerDefault(GUINames.LOADOUT, L_PRESET_INFO, new ItemStack(Material.COMPASS));
        registry.registerDefault(GUINames.LOADOUT, L_PRESET_FALLBACK, new ItemStack(Material.CHEST));
        registry.registerDefault(GUINames.LOADOUT, L_TAB_HIDER, new ItemStack(Material.BLUE_CONCRETE));
        registry.registerDefault(GUINames.LOADOUT, L_TAB_SEEKER, new ItemStack(Material.RED_CONCRETE));
        registry.registerDefault(GUINames.LOADOUT, L_TAB_PRESETS, new ItemStack(Material.COMPASS));
        registry.registerDefault(GUINames.LOADOUT, L_RESTRICTED_ITEM_INFO, new ItemStack(Material.BOOK));
        registry.registerDefault(GUINames.LOADOUT, L_ADMIN_PRESET_EMPTY,
                new ItemStack(Material.GRAY_STAINED_GLASS_PANE));
        registry.registerDefault(GUINames.LOADOUT, L_ADMIN_PRESET_DISABLED, new ItemStack(Material.BARRIER));
        registry.registerDefault(GUINames.LOADOUT, L_ADMIN_PRESET_FALLBACK, new ItemStack(Material.CHEST));
        registry.registerDefault(GUINames.LOADOUT, KEY_INFO, new ItemStack(Material.BOOK));
        registry.registerDefault(GUINames.LOADOUT, L_NO_PREVIEW, new ItemStack(Material.BARRIER));

        registry.registerDefault(GUINames.SLOT_PREFERENCES, KEY_CLEAR, new ItemStack(Material.BARRIER));
        registry.registerDefault(GUINames.SLOT_PREFERENCES, SP_BACK_LOADOUT, new ItemStack(Material.ARROW));
        registry.registerDefault(GUINames.SLOT_PREFERENCES, SP_BACK_PREFS, new ItemStack(Material.ARROW));
        registry.registerDefault(GUINames.SLOT_PREFERENCES, KEY_INFO, new ItemStack(Material.BOOK));
        registry.registerDefault(GUINames.SLOT_PREFERENCES, SP_PRIMARY_SLOT,
                new ItemStack(Material.LIGHT_BLUE_STAINED_GLASS_PANE));
        registry.registerDefault(GUINames.SLOT_PREFERENCES, SP_FALLBACK_SLOT,
                new ItemStack(Material.GRAY_STAINED_GLASS_PANE));
        registry.registerDefault(GUINames.SLOT_PREFERENCES, SP_FALLBACK_SLOT_DISABLED,
                new ItemStack(Material.LIGHT_GRAY_STAINED_GLASS_PANE));
        registry.registerDefault(GUINames.SLOT_PREFERENCES, SP_ITEM_MOBILITY, new ItemStack(Material.BLUE_CONCRETE));
        registry.registerDefault(GUINames.SLOT_PREFERENCES, SP_ITEM_UTILITY, new ItemStack(Material.CYAN_CONCRETE));
        registry.registerDefault(GUINames.SLOT_PREFERENCES, SP_ITEM_OFFENSE, new ItemStack(Material.RED_CONCRETE));
        registry.registerDefault(GUINames.SLOT_PREFERENCES, SP_ITEM_TRAP, new ItemStack(Material.PURPLE_CONCRETE));
        registry.registerDefault(GUINames.SLOT_PREFERENCES, SP_ITEM_DEFENSE, new ItemStack(Material.GREEN_CONCRETE));
        registry.registerDefault(GUINames.SLOT_PREFERENCES, SP_ITEM_HEALING, new ItemStack(Material.LIME_CONCRETE));
        registry.registerDefault(GUINames.SLOT_PREFERENCES, SP_ITEM_INFORMATION,
                new ItemStack(Material.YELLOW_CONCRETE));
        registry.registerDefault(GUINames.SLOT_PREFERENCES, SP_ITEM_SUPPORT, new ItemStack(Material.ORANGE_CONCRETE));
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
