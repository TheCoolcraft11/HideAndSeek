package de.thecoolcraft11.hideAndSeek.gui.config;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.model.GameModeEnum;
import de.thecoolcraft11.hideAndSeek.model.ItemType;
import de.thecoolcraft11.minigameframework.gui.config.GuiItemRegistry;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.TooltipDisplay;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

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
    public static final String KEY_CONFIRM = "confirm";

    public static final String SKIN_COIN = "coin";

    public static final String MAIN_MENU_LOADOUT = "loadout_menu";
    public static final String MAIN_MENU_MAP_SELECTION = "map_selection_menu";
    public static final String MAIN_MENU_SKIN_SELECTION = "skin_selection_menu";

    public static final String PERKS_KEY_LIGHT = "light";
    public static final String PERKS_KEY_SOLD = "sold";
    public static final String PERKS_KEY_PLACEHOLDER = "placeholder";

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

    public static final String A_STATE_BASE = "state_";
    public static final String A_STATE_FACING = A_STATE_BASE + "facing";
    public static final String A_STATE_ROTATION = A_STATE_BASE + "rotation";
    public static final String A_STATE_AXIS = A_STATE_BASE + "axis";
    public static final String A_STATE_WATERLOGGED = A_STATE_BASE + "waterlogged";
    public static final String A_STATE_LIT = A_STATE_BASE + "lit";
    public static final String A_STATE_POWERED = A_STATE_BASE + "powered";
    public static final String A_STATE_OPEN = A_STATE_BASE + "open";
    public static final String A_STATE_HALF = A_STATE_BASE + "half";
    public static final String A_STATE_TYPE = A_STATE_BASE + "type";
    public static final String A_STATE_SHAPE = A_STATE_BASE + "shape";
    public static final String A_STATE_AGE = A_STATE_BASE + "age";
    public static final String A_STATE_LEVEL = A_STATE_BASE + "level";
    public static final String A_STATE_BITES = A_STATE_BASE + "bites";
    public static final String A_STATE_HONEY_LEVEL = A_STATE_BASE + "honey_level";
    public static final String A_STATE_CANDLES = A_STATE_BASE + "candles";
    public static final String A_STATE_SNOWY = A_STATE_BASE + "snowy";
    public static final String A_STATE_HINGE = A_STATE_BASE + "hinge";
    public static final String A_STATE_DEFAULT = A_STATE_BASE + "default";
    public static final String A_SECTION_HEADER = "section_header";

    public static final String ADMIN_MODE_TOGGLE = "admin_mode_toggle";
    public static final String ADMIN_CLEAR_ENTRIES = "admin_clear_entries";
    public static final String ADMIN_RESET_ALL = "admin_reset_all";
    public static final String ADMIN_GLOBAL_LOCK_ON = "admin_global_lock_on";
    public static final String ADMIN_GLOBAL_LOCK_OFF = "admin_global_lock_off";
    public static final String ADMIN_APPLY_CHANGES = "admin_apply_changes";
    public static final String ADMIN_CLOSE = "admin_close";
    public static final String ADMIN_TAB_HIDER = "admin_tab_hider";
    public static final String ADMIN_TAB_SEEKER = "admin_tab_seeker";
    public static final String ADMIN_TAB_PERKS = "admin_tab_perks";
    public static final String ADMIN_TAB_PLAYERS = "admin_tab_players";
    public static final String ADMIN_TAB_PRESETS = "admin_tab_presets";
    public static final String ADMIN_PERKS_TITLE = "admin_perks_title";
    public static final String ADMIN_PERKS_REFRESH = "admin_perks_refresh";
    public static final String ADMIN_PLAYERS_TITLE = "admin_players_title";
    public static final String ADMIN_PLAYERS_TOGGLE_HIDER = "admin_players_toggle_hider";
    public static final String ADMIN_PLAYERS_TOGGLE_SEEKER = "admin_players_toggle_seeker";
    public static final String ADMIN_PLAYERS_RESET_HIDER = "admin_players_reset_hider";
    public static final String ADMIN_PLAYERS_RESET_SEEKER = "admin_players_reset_seeker";
    public static final String ADMIN_PLAYERS_RESET_ALL = "admin_players_reset_all";
    public static final String ADMIN_PLAYERS_EDIT_HIDER = "admin_players_edit_hider";
    public static final String ADMIN_PLAYERS_EDIT_SEEKER = "admin_players_edit_seeker";
    public static final String ADMIN_PLAYERS_RESET_EVERYONE = "admin_players_reset_everyone";
    public static final String ADMIN_EDITOR_BACK = "admin_editor_back";
    public static final String ADMIN_EDITOR_SWITCH_ROLE = "admin_editor_switch_role";
    public static final String ADMIN_EDITOR_SUMMARY = "admin_editor_summary";
    public static final String ADMIN_PRESETS_TITLE = "admin_presets_title";
    public static final String ADMIN_PRESETS_ROLE = "admin_presets_role";
    public static final String ADMIN_PRESETS_RESTRICT_ON = "admin_presets_restrict_on";
    public static final String ADMIN_PRESETS_RESTRICT_OFF = "admin_presets_restrict_off";
    public static final String ADMIN_PRESETS_FORCED_ON = "admin_presets_forced_on";
    public static final String ADMIN_PRESETS_FORCED_OFF = "admin_presets_forced_off";
    public static final String ADMIN_PRESETS_EMPTY_PREVIEW = "admin_presets_empty_preview";
    public static final String ADMIN_INFO = "admin_info";
    public static final String ADMIN_PRESET_ITEM_FALLBACK = "admin_preset_item_fallback";

    public static final String STATS_CLOSE = "stats_close";
    public static final String STATS_BORDER = "stats_border";
    public static final String STATS_TAB_OVERVIEW = "stats_tab_overview";
    public static final String STATS_TAB_COMBAT = "stats_tab_combat";
    public static final String STATS_TAB_ITEMS = "stats_tab_items";
    public static final String STATS_TAB_MAPS = "stats_tab_maps";
    public static final String STATS_TAB_PERKS = "stats_tab_perks";

    public static void registerAll(GuiItemRegistry registry, HideAndSeek plugin) {
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

        registry.registerDefault(GUINames.APPEARANCE, A_STATE_FACING, new ItemStack(Material.COMPASS));
        registry.registerDefault(GUINames.APPEARANCE, A_STATE_ROTATION, new ItemStack(Material.COMPASS));
        registry.registerDefault(GUINames.APPEARANCE, A_STATE_AXIS, new ItemStack(Material.STICK));
        registry.registerDefault(GUINames.APPEARANCE, A_STATE_WATERLOGGED, new ItemStack(Material.WATER_BUCKET));
        registry.registerDefault(GUINames.APPEARANCE, A_STATE_LIT, new ItemStack(Material.REDSTONE_TORCH));
        registry.registerDefault(GUINames.APPEARANCE, A_STATE_POWERED, new ItemStack(Material.REDSTONE_TORCH));
        registry.registerDefault(GUINames.APPEARANCE, A_STATE_OPEN, new ItemStack(Material.OAK_DOOR));
        registry.registerDefault(GUINames.APPEARANCE, A_STATE_HALF, new ItemStack(Material.SMOOTH_STONE_SLAB));
        registry.registerDefault(GUINames.APPEARANCE, A_STATE_TYPE, new ItemStack(Material.SMOOTH_STONE_SLAB));
        registry.registerDefault(GUINames.APPEARANCE, A_STATE_SHAPE, new ItemStack(Material.OAK_STAIRS));
        registry.registerDefault(GUINames.APPEARANCE, A_STATE_AGE, new ItemStack(Material.WHEAT_SEEDS));
        registry.registerDefault(GUINames.APPEARANCE, A_STATE_LEVEL, new ItemStack(Material.EXPERIENCE_BOTTLE));
        registry.registerDefault(GUINames.APPEARANCE, A_STATE_BITES, new ItemStack(Material.CAKE));
        registry.registerDefault(GUINames.APPEARANCE, A_STATE_HONEY_LEVEL, new ItemStack(Material.HONEY_BOTTLE));
        registry.registerDefault(GUINames.APPEARANCE, A_STATE_CANDLES, new ItemStack(Material.CANDLE));
        registry.registerDefault(GUINames.APPEARANCE, A_STATE_SNOWY, new ItemStack(Material.SNOWBALL));
        registry.registerDefault(GUINames.APPEARANCE, A_STATE_HINGE, new ItemStack(Material.TRIPWIRE_HOOK));
        registry.registerDefault(GUINames.APPEARANCE, A_STATE_DEFAULT, new ItemStack(Material.REPEATER));
        registry.registerDefault(GUINames.APPEARANCE, A_SECTION_HEADER,
                new ItemStack(Material.LIME_STAINED_GLASS_PANE));
        registry.registerDefault(GUINames.APPEARANCE, KEY_SEPERATOR, createSeparatorItem());
        registry.registerDefault(GUINames.APPEARANCE, KEY_SEPERATOR, createSeparatorItem());
        registry.registerDefault(GUINames.APPEARANCE, KEY_CONFIRM, new ItemStack(Material.GREEN_STAINED_GLASS_PANE));
        registry.registerDefault(GUINames.APPEARANCE, KEY_PREVIOUS, new ItemStack(Material.ARROW));
        registry.registerDefault(GUINames.APPEARANCE, KEY_NEXT, new ItemStack(Material.ARROW));

        registry.registerDefault(GUINames.ADMIN_LOADOUT, ADMIN_MODE_TOGGLE, new ItemStack(Material.LECTERN));
        registry.registerDefault(GUINames.ADMIN_LOADOUT, ADMIN_CLEAR_ENTRIES, new ItemStack(Material.BARRIER));
        registry.registerDefault(GUINames.ADMIN_LOADOUT, ADMIN_RESET_ALL, new ItemStack(Material.TNT));
        registry.registerDefault(GUINames.ADMIN_LOADOUT, ADMIN_GLOBAL_LOCK_ON, new ItemStack(Material.BARRIER));
        registry.registerDefault(GUINames.ADMIN_LOADOUT, ADMIN_GLOBAL_LOCK_OFF, new ItemStack(Material.LIME_CONCRETE));
        registry.registerDefault(GUINames.ADMIN_LOADOUT, ADMIN_APPLY_CHANGES, new ItemStack(Material.CLOCK));
        registry.registerDefault(GUINames.ADMIN_LOADOUT, ADMIN_CLOSE, new ItemStack(Material.BARRIER));
        registry.registerDefault(GUINames.ADMIN_LOADOUT, ADMIN_TAB_HIDER, new ItemStack(Material.BLUE_CONCRETE));
        registry.registerDefault(GUINames.ADMIN_LOADOUT, ADMIN_TAB_SEEKER, new ItemStack(Material.RED_CONCRETE));
        registry.registerDefault(GUINames.ADMIN_LOADOUT, ADMIN_TAB_PERKS, createPerkLightItem(plugin, false, true));
        registry.registerDefault(GUINames.ADMIN_LOADOUT, ADMIN_TAB_PLAYERS, new ItemStack(Material.PLAYER_HEAD));
        registry.registerDefault(GUINames.ADMIN_LOADOUT, ADMIN_TAB_PRESETS, new ItemStack(Material.BOOKSHELF));

        registry.registerDefault(GUINames.ADMIN_LOADOUT, ADMIN_PERKS_TITLE, new ItemStack(Material.BEACON));
        registry.registerDefault(GUINames.ADMIN_LOADOUT, ADMIN_PERKS_REFRESH, new ItemStack(Material.CLOCK));
        registry.registerDefault(GUINames.ADMIN_LOADOUT, ADMIN_PLAYERS_TITLE, new ItemStack(Material.PLAYER_HEAD));
        registry.registerDefault(GUINames.ADMIN_LOADOUT, ADMIN_PLAYERS_TOGGLE_HIDER, new ItemStack(Material.IRON_DOOR));
        registry.registerDefault(GUINames.ADMIN_LOADOUT, ADMIN_PLAYERS_TOGGLE_SEEKER,
                new ItemStack(Material.IRON_DOOR));
        registry.registerDefault(GUINames.ADMIN_LOADOUT, ADMIN_PLAYERS_RESET_HIDER, new ItemStack(Material.REDSTONE));
        registry.registerDefault(GUINames.ADMIN_LOADOUT, ADMIN_PLAYERS_RESET_SEEKER, new ItemStack(Material.REDSTONE));
        registry.registerDefault(GUINames.ADMIN_LOADOUT, ADMIN_PLAYERS_RESET_ALL, new ItemStack(Material.TNT));
        registry.registerDefault(GUINames.ADMIN_LOADOUT, ADMIN_PLAYERS_EDIT_HIDER, new ItemStack(Material.CHEST));
        registry.registerDefault(GUINames.ADMIN_LOADOUT, ADMIN_PLAYERS_EDIT_SEEKER, new ItemStack(Material.CHEST));
        registry.registerDefault(GUINames.ADMIN_LOADOUT, ADMIN_PLAYERS_RESET_EVERYONE,
                new ItemStack(Material.LAVA_BUCKET));
        registry.registerDefault(GUINames.ADMIN_LOADOUT, ADMIN_EDITOR_BACK, new ItemStack(Material.ARROW));
        registry.registerDefault(GUINames.ADMIN_LOADOUT, ADMIN_EDITOR_SWITCH_ROLE, new ItemStack(Material.COMPASS));
        registry.registerDefault(GUINames.ADMIN_LOADOUT, ADMIN_EDITOR_SUMMARY, new ItemStack(Material.BOOK));
        registry.registerDefault(GUINames.ADMIN_LOADOUT, ADMIN_PRESETS_TITLE, new ItemStack(Material.BOOKSHELF));
        registry.registerDefault(GUINames.ADMIN_LOADOUT, ADMIN_PRESETS_ROLE, new ItemStack(Material.COMPASS));
        registry.registerDefault(GUINames.ADMIN_LOADOUT, ADMIN_PRESETS_RESTRICT_ON,
                new ItemStack(Material.REDSTONE_BLOCK));
        registry.registerDefault(GUINames.ADMIN_LOADOUT, ADMIN_PRESETS_RESTRICT_OFF,
                new ItemStack(Material.LIME_CONCRETE));
        registry.registerDefault(GUINames.ADMIN_LOADOUT, ADMIN_PRESETS_FORCED_ON, new ItemStack(Material.RED_BED));
        registry.registerDefault(GUINames.ADMIN_LOADOUT, ADMIN_PRESETS_FORCED_OFF, new ItemStack(Material.GRAY_BED));
        registry.registerDefault(GUINames.ADMIN_LOADOUT, ADMIN_PRESETS_EMPTY_PREVIEW,
                new ItemStack(Material.GRAY_STAINED_GLASS_PANE));
        registry.registerDefault(GUINames.ADMIN_LOADOUT, ADMIN_INFO, new ItemStack(Material.BOOK));
        registry.registerDefault(GUINames.ADMIN_LOADOUT, ADMIN_PRESET_ITEM_FALLBACK, new ItemStack(Material.CHEST));

        registry.registerDefault(GUINames.PLAYER_STATS, STATS_CLOSE, new ItemStack(Material.BARRIER));
        registry.registerDefault(GUINames.PLAYER_STATS, STATS_BORDER, createSeparatorItem());
        registry.registerDefault(GUINames.PLAYER_STATS, STATS_TAB_OVERVIEW, new ItemStack(Material.BOOK));
        registry.registerDefault(GUINames.PLAYER_STATS, STATS_TAB_COMBAT, new ItemStack(Material.IRON_SWORD));
        registry.registerDefault(GUINames.PLAYER_STATS, STATS_TAB_ITEMS, new ItemStack(Material.BLAZE_POWDER));
        registry.registerDefault(GUINames.PLAYER_STATS, STATS_TAB_MAPS, new ItemStack(Material.MAP));
        registry.registerDefault(GUINames.PLAYER_STATS, STATS_TAB_PERKS, createPerkLightItem(plugin, false, true));

        registry.registerDefault(GUINames.MAIN_MENU, MAIN_MENU_LOADOUT, new ItemStack(Material.CHEST));
        registry.registerDefault(GUINames.MAIN_MENU, MAIN_MENU_MAP_SELECTION, new ItemStack(Material.MAP));
        registry.registerDefault(GUINames.MAIN_MENU, MAIN_MENU_SKIN_SELECTION, new ItemStack(Material.ARMOR_STAND));

        registry.registerDefault(GUINames.PERKS, PERKS_KEY_LIGHT, createPerkLightItem(plugin, false, false));
        registry.registerDefault(GUINames.PERKS, PERKS_KEY_SOLD, createPerkLightItem(plugin, false, true));
        registry.registerDefault(GUINames.PERKS, PERKS_KEY_PLACEHOLDER, createPerkLightItem(plugin, true, false));
    }

    @SuppressWarnings("UnstableApiUsage")
    private static ItemStack createPerkLightItem(Plugin plugin, boolean removeBlockData, boolean hideBlockDataInTooltip) {
        NamespacedKey lightKey = new NamespacedKey(plugin, "perk_shop_light");
        ItemStack item = new ItemStack(Material.LIGHT);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(" "));
            meta.getPersistentDataContainer().set(lightKey, PersistentDataType.BOOLEAN, true);
            item.setItemMeta(meta);
        }
        if (removeBlockData) {
            item.unsetData(DataComponentTypes.BLOCK_DATA);
        }
        if (hideBlockDataInTooltip) {
            item.setData(DataComponentTypes.TOOLTIP_DISPLAY,
                    TooltipDisplay.tooltipDisplay().addHiddenComponents(DataComponentTypes.BLOCK_DATA).build());
        } else {
            item.setData(DataComponentTypes.TOOLTIP_DISPLAY, TooltipDisplay.tooltipDisplay().hideTooltip(true).build());
        }
        return item;
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
