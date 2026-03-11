package de.thecoolcraft11.hideAndSeek.util.setting;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.model.*;
import de.thecoolcraft11.minigameframework.config.SectionDefinition;
import de.thecoolcraft11.minigameframework.config.SettingDefinition;
import de.thecoolcraft11.minigameframework.config.SettingType;
import de.thecoolcraft11.timer.AnimationType;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public class SettingRegisterer {

    @SuppressWarnings("unchecked")
    private static <T> T getConfigValue(HideAndSeek plugin, String path, T fallback) {
        Object value = plugin.getConfig().get("settings." + path);
        if (value == null) {
            return fallback;
        }
        try {

            if (fallback instanceof Float) {
                if (value instanceof Number) {
                    return (T) Float.valueOf(((Number) value).floatValue());
                }
                if (value instanceof String) {
                    return (T) Float.valueOf(Float.parseFloat((String) value));
                }
            }
            if (fallback instanceof Double) {
                if (value instanceof Number) {
                    return (T) Double.valueOf(((Number) value).doubleValue());
                }
                if (value instanceof String) {
                    return (T) Double.valueOf(Double.parseDouble((String) value));
                }
            }
            if (fallback instanceof Integer) {
                if (value instanceof Number) {
                    return (T) Integer.valueOf(((Number) value).intValue());
                }
                if (value instanceof String) {
                    return (T) Integer.valueOf(Integer.parseInt((String) value));
                }
            }
            if (fallback instanceof Long) {
                if (value instanceof Number) {
                    return (T) Long.valueOf(((Number) value).longValue());
                }
                if (value instanceof String) {
                    return (T) Long.valueOf(Long.parseLong((String) value));
                }
            }
            if (fallback instanceof Boolean) {
                if (value instanceof Boolean) {
                    return (T) value;
                }
                if (value instanceof String) {
                    return (T) Boolean.valueOf((String) value);
                }
            }
            if (fallback instanceof String) {
                return (T) value.toString();
            }

            return (T) value;
        } catch (Exception e) {
            plugin.getLogger().warning("Invalid config value for " + path + ", using fallback: " + fallback + " (error: " + e.getMessage() + ")");
            return fallback;
        }
    }

    private static <E extends Enum<E>> E getEnumConfigValue(HideAndSeek plugin, String path, Class<E> enumClass, E fallback) {
        String value = plugin.getConfig().getString("settings." + path);
        if (value == null) {
            return fallback;
        }
        try {
            return Enum.valueOf(enumClass, value);
        } catch (IllegalArgumentException e) {
            plugin.getLogger().warning("Invalid enum config value for " + path + ", using fallback: " + fallback);
            return fallback;
        }
    }


    public static void registerAll(HideAndSeek plugin) {
        registerSections(plugin);
        registerConfig(plugin);
        registerSettings(plugin);
    }

    public static void registerConfig(HideAndSeek plugin) {

        plugin.getConfigRegistry().register("maps", List.class, List.of("map1"));
        plugin.getConfigRegistry().register("disallowed-blockstates", List.class, List.of("waterlogged", "conditional"));
        plugin.getConfigRegistry().register("seeker-break-blocks", List.class, List.of("SHORT_GRASS", "TALL_GRASS", "SEAGRASS", "TALL_SEAGRASS"));
        plugin.getConfigRegistry().register("game.apply-player-direction", Boolean.class, true);
        plugin.getConfigRegistry().register("game.max-air-above-liquid", Integer.class, 2);
        plugin.getConfigRegistry().register("nms.enabled", Boolean.class, true);


        plugin.getConfigRegistry().register("settings.game.gametype", String.class, "NORMAL");
        plugin.getConfigRegistry().register("settings.game.gamestyle", String.class, "SPECTATOR");
        plugin.getConfigRegistry().register("settings.game.hiding_time", Integer.class, 60);
        plugin.getConfigRegistry().register("settings.game.seeking_time", Integer.class, 300);
        plugin.getConfigRegistry().register("settings.game.hider_invisibility", Boolean.class, false);
        plugin.getConfigRegistry().register("settings.game.small_mode_size", Double.class, 0.5);
        plugin.getConfigRegistry().register("settings.game.random_team_distribution", Boolean.class, true);
        plugin.getConfigRegistry().register("settings.game.use_preferred_modes", Boolean.class, true);
        plugin.getConfigRegistry().register("settings.game.use_map_specific_timings", Boolean.class, true);
        plugin.getConfigRegistry().register("settings.game.use_map_specific_seeker_count", Boolean.class, true);
        plugin.getConfigRegistry().register("settings.game.use_map_specific_player_limits", Boolean.class, true);
        plugin.getConfigRegistry().register("settings.game.fixed_seeker_team", String.class, "");
        plugin.getConfigRegistry().register("settings.game.seeker_count", Integer.class, 1);
        plugin.getConfigRegistry().register("settings.game.hider_health", Integer.class, 20);
        plugin.getConfigRegistry().register("settings.game.block_view_height", Float.class, 0.1f);
        plugin.getConfigRegistry().register("settings.game.block_size_to_block", Boolean.class, false);
        plugin.getConfigRegistry().register("settings.game.seeker_kill_mode", String.class, "NORMAL");
        plugin.getConfigRegistry().register("settings.game.gaze_kill_max_distance", Double.class, 10.0);
        plugin.getConfigRegistry().register("settings.game.gaze_kill_fov", Double.class, 30.0);
        plugin.getConfigRegistry().register("settings.game.gaze_kill_show_particles", Boolean.class, true);
        plugin.getConfigRegistry().register("settings.game.auto_cleanup_after_round", Boolean.class, true);
        plugin.getConfigRegistry().register("settings.game.small_mode_seeker_size", Double.class, 1.0);


        plugin.getConfigRegistry().register("settings.blockstats.show-names", Boolean.class, false);


        plugin.getConfigRegistry().register("settings.hider-items.random-block.uses", Integer.class, 5);
        plugin.getConfigRegistry().register("settings.hider-items.crossbow.hits-per-upgrade", Integer.class, 3);
        plugin.getConfigRegistry().register("settings.hider-items.sound.cooldown", Integer.class, 4);
        plugin.getConfigRegistry().register("settings.hider-items.sound.points", Integer.class, 5);
        plugin.getConfigRegistry().register("settings.hider-items.sound.volume", Double.class, 0.75);
        plugin.getConfigRegistry().register("settings.hider-items.sound.pitch", Double.class, 0.8);
        plugin.getConfigRegistry().register("settings.hider-items.explosion.cooldown", Integer.class, 8);
        plugin.getConfigRegistry().register("settings.hider-items.explosion.points", Integer.class, 10);
        plugin.getConfigRegistry().register("settings.hider-items.explosion.volume", Double.class, 0.65);
        plugin.getConfigRegistry().register("settings.hider-items.explosion.pitch", Double.class, 1.5);
        plugin.getConfigRegistry().register("settings.hider-items.explosion.smoke-particles", Integer.class, 3);
        plugin.getConfigRegistry().register("settings.hider-items.explosion.fuse-time", Integer.class, 40);
        plugin.getConfigRegistry().register("settings.hider-items.speed-boost.type", String.class, "SPEED_EFFECT");
        plugin.getConfigRegistry().register("settings.hider-items.speed-boost.cooldown", Integer.class, 10);
        plugin.getConfigRegistry().register("settings.hider-items.speed-boost.duration", Integer.class, 5);
        plugin.getConfigRegistry().register("settings.hider-items.speed-boost.boost-power", Double.class, 0.5);
        plugin.getConfigRegistry().register("settings.hider-items.knockback-stick.level", Integer.class, 2);
        plugin.getConfigRegistry().register("settings.hider-items.block-swap.cooldown", Integer.class, 15);
        plugin.getConfigRegistry().register("settings.hider-items.block-swap.range", Double.class, 50.0);
        plugin.getConfigRegistry().register("settings.hider-items.big-firecracker.cooldown", Integer.class, 12);
        plugin.getConfigRegistry().register("settings.hider-items.big-firecracker.fuse-time", Integer.class, 60);
        plugin.getConfigRegistry().register("settings.hider-items.big-firecracker.mini-fuse-time", Integer.class, 30);
        plugin.getConfigRegistry().register("settings.hider-items.big-firecracker.mini-count", Integer.class, 3);
        plugin.getConfigRegistry().register("settings.hider-items.big-firecracker.points", Integer.class, 20);
        plugin.getConfigRegistry().register("settings.hider-items.big-firecracker.volume", Double.class, 1.2);
        plugin.getConfigRegistry().register("settings.hider-items.big-firecracker.pitch", Double.class, 0.5);
        plugin.getConfigRegistry().register("settings.hider-items.firework-rocket.cooldown", Integer.class, 10);
        plugin.getConfigRegistry().register("settings.hider-items.firework-rocket.target-y", Integer.class, 128);
        plugin.getConfigRegistry().register("settings.hider-items.firework-rocket.points", Integer.class, 15);
        plugin.getConfigRegistry().register("settings.hider-items.firework-rocket.volume", Double.class, 10.0);
        plugin.getConfigRegistry().register("settings.hider-items.medkit.cooldown", Integer.class, 30);
        plugin.getConfigRegistry().register("settings.hider-items.medkit.channel-time", Integer.class, 5);
        plugin.getConfigRegistry().register("settings.hider-items.medkit.heal-amount", Double.class, 20.0);
        plugin.getConfigRegistry().register("settings.hider-items.totem.effect-duration", Integer.class, 30);
        plugin.getConfigRegistry().register("settings.hider-items.totem.max-uses", Integer.class, 1);
        plugin.getConfigRegistry().register("settings.hider-items.invisibility-cloak.cooldown", Integer.class, 20);
        plugin.getConfigRegistry().register("settings.hider-items.invisibility-cloak.duration", Integer.class, 8);
        plugin.getConfigRegistry().register("settings.hider-items.slowness-ball.cooldown", Integer.class, 10);
        plugin.getConfigRegistry().register("settings.hider-items.slowness-ball.duration", Integer.class, 6);
        plugin.getConfigRegistry().register("settings.hider-items.slowness-ball.amplifier", Integer.class, 1);
        plugin.getConfigRegistry().register("settings.hider-items.smoke-bomb.cooldown", Integer.class, 15);
        plugin.getConfigRegistry().register("settings.hider-items.smoke-bomb.duration", Integer.class, 8);
        plugin.getConfigRegistry().register("settings.hider-items.smoke-bomb.radius", Integer.class, 4);

        plugin.getConfigRegistry().register("settings.hider-items.ghost-essence.cooldown", Integer.class, 25);
        plugin.getConfigRegistry().register("settings.hider-items.ghost-essence.max-radius", Integer.class, 15);
        plugin.getConfigRegistry().register("settings.hider-items.ghost-essence.min-light-block", Integer.class, 1);
        plugin.getConfigRegistry().register("settings.hider-items.ghost-essence.min-light-sky", Integer.class, 1);
        plugin.getConfigRegistry().register("settings.hider-items.ghost-essence.flying-speed", Double.class, 0.01);
        plugin.getConfigRegistry().register("settings.hider-items.ghost-essence.max-duration", Float.class, 1.5f);
        plugin.getConfigRegistry().register("settings.hider-items.ghost-essence.boost-power", Float.class, 1.5f);
        plugin.getConfigRegistry().register("settings.hider-items.ghost-essence.particle-mode", String.class, "FLYING");


        plugin.getConfigRegistry().register("settings.timer.hiding_color1", String.class, "#FF0000");
        plugin.getConfigRegistry().register("settings.timer.hiding_color2", String.class, "#0000FF");
        plugin.getConfigRegistry().register("settings.timer.seeking_color1", String.class, "#FFFF00");
        plugin.getConfigRegistry().register("settings.timer.seeking_color2", String.class, "#00FFFF");
        plugin.getConfigRegistry().register("settings.timer.animation_type", String.class, "WAVE");
        plugin.getConfigRegistry().register("settings.timer.animation_speed", Double.class, 0.5);


        plugin.getConfigRegistry().register("settings.seeker-items.grappling-hook.cooldown", Integer.class, 2);
        plugin.getConfigRegistry().register("settings.seeker-items.grappling-hook.speed", Double.class, 1.5);
        plugin.getConfigRegistry().register("settings.seeker-items.ink-splash.cooldown", Integer.class, 20);
        plugin.getConfigRegistry().register("settings.seeker-items.ink-splash.radius", Integer.class, 25);
        plugin.getConfigRegistry().register("settings.seeker-items.ink-splash.duration", Integer.class, 7);
        plugin.getConfigRegistry().register("settings.seeker-items.lightning-freeze.cooldown", Integer.class, 60);
        plugin.getConfigRegistry().register("settings.seeker-items.lightning-freeze.duration", Integer.class, 5);
        plugin.getConfigRegistry().register("settings.seeker-items.glowing-compass.cooldown", Integer.class, 25);
        plugin.getConfigRegistry().register("settings.seeker-items.glowing-compass.duration", Integer.class, 10);
        plugin.getConfigRegistry().register("settings.seeker-items.glowing-compass.range", Double.class, 50.0);
        plugin.getConfigRegistry().register("settings.seeker-items.curse-spell.cooldown", Integer.class, 30);
        plugin.getConfigRegistry().register("settings.seeker-items.curse-spell.active-duration", Integer.class, 10);
        plugin.getConfigRegistry().register("settings.seeker-items.curse-spell.curse-duration", Integer.class, 8);
        plugin.getConfigRegistry().register("settings.seeker-items.curse-spell.small-shrink-delay", Integer.class, 8);
        plugin.getConfigRegistry().register("settings.seeker-items.block-randomizer.cooldown", Integer.class, 45);
        plugin.getConfigRegistry().register("settings.seeker-items.chain-pull.cooldown", Integer.class, 12);
        plugin.getConfigRegistry().register("settings.seeker-items.chain-pull.range", Double.class, 30.0);
        plugin.getConfigRegistry().register("settings.seeker-items.chain-pull.pull-power", Double.class, 2.0);
        plugin.getConfigRegistry().register("settings.seeker-items.chain-pull.slowness-duration", Integer.class, 3);
        plugin.getConfigRegistry().register("settings.seeker-items.proximity-sensor.cooldown", Integer.class, 20);
        plugin.getConfigRegistry().register("settings.seeker-items.proximity-sensor.range", Double.class, 8.0);
        plugin.getConfigRegistry().register("settings.seeker-items.proximity-sensor.duration", Integer.class, 60);
        plugin.getConfigRegistry().register("settings.seeker-items.proximity-sensor.fov-angle", Double.class, 90.0);
        plugin.getConfigRegistry().register("settings.seeker-items.cage-trap.cooldown", Integer.class, 20);
        plugin.getConfigRegistry().register("settings.seeker-items.cage-trap.range", Double.class, 3.0);
        plugin.getConfigRegistry().register("settings.seeker-items.cage-trap.duration", Integer.class, -1);
        plugin.getConfigRegistry().register("settings.seeker-items.cage-trap.paralyze-duration", Integer.class, 5);
        plugin.getConfigRegistry().register("settings.seeker-items.cage-trap.setup-time", Integer.class, 5);
        plugin.getConfigRegistry().register("settings.seeker-items.sword-of-seeking.cooldown", Integer.class, 5);
        plugin.getConfigRegistry().register("settings.seeker-items.seeker-sword-throw.max-charge-seconds", Integer.class, 5);
        plugin.getConfigRegistry().register("settings.seeker-items.seeker-sword-throw.min-speed", Double.class, 0.8);
        plugin.getConfigRegistry().register("settings.seeker-items.seeker-sword-throw.max-speed", Double.class, 2.4);
        plugin.getConfigRegistry().register("settings.seeker-items.seeker-sword-throw.gravity", Double.class, 0.035);
        plugin.getConfigRegistry().register("settings.seeker-items.seeker-sword-throw.hitbox", Double.class, 0.4);
        plugin.getConfigRegistry().register("settings.seeker-items.seeker-sword-throw.max-flight-seconds", Integer.class, 6);
        plugin.getConfigRegistry().register("settings.seeker-items.seeker-sword-throw.stuck-seconds", Integer.class, 12);


        plugin.getConfigRegistry().register("settings.loadout.hider-max-items", Integer.class, 3);
        plugin.getConfigRegistry().register("settings.loadout.seeker-max-items", Integer.class, 4);
        plugin.getConfigRegistry().register("settings.loadout.hider-max-tokens", Integer.class, 12);
        plugin.getConfigRegistry().register("settings.loadout.seeker-max-tokens", Integer.class, 12);
        plugin.getConfigRegistry().register("settings.loadout.token-cost-common", Integer.class, 1);
        plugin.getConfigRegistry().register("settings.loadout.token-cost-uncommon", Integer.class, 2);
        plugin.getConfigRegistry().register("settings.loadout.token-cost-rare", Integer.class, 4);
        plugin.getConfigRegistry().register("settings.loadout.token-cost-epic", Integer.class, 6);
        plugin.getConfigRegistry().register("settings.loadout.token-cost-legendary", Integer.class, 10);

        plugin.getConfigRegistry().register("settings.points.tracking.interval-seconds", Integer.class, 1);
        plugin.getConfigRegistry().register("settings.points.hider.survival.amount", Integer.class, 5);
        plugin.getConfigRegistry().register("settings.points.hider.survival.interval-seconds", Integer.class, 20);
        plugin.getConfigRegistry().register("settings.points.hider.survival.start-delay-seconds", Integer.class, 20);
        plugin.getConfigRegistry().register("settings.points.hider.proximity.amount-per-second", Integer.class, 2);
        plugin.getConfigRegistry().register("settings.points.hider.proximity.range", Double.class, 8.0);
        plugin.getConfigRegistry().register("settings.points.hider.near-miss.amount", Integer.class, 50);
        plugin.getConfigRegistry().register("settings.points.hider.near-miss.range", Double.class, 3.0);
        plugin.getConfigRegistry().register("settings.points.hider.near-miss.escape-seconds", Integer.class, 4);
        plugin.getConfigRegistry().register("settings.points.hider.taunt.small", Integer.class, 25);
        plugin.getConfigRegistry().register("settings.points.hider.taunt.large", Integer.class, 75);
        plugin.getConfigRegistry().register("settings.points.hider.sharpshooter.amount", Integer.class, 20);
        plugin.getConfigRegistry().register("settings.points.hider.survivor.amount", Integer.class, 100);
        plugin.getConfigRegistry().register("settings.points.hider.special.ghost", Integer.class, 200);
        plugin.getConfigRegistry().register("settings.points.hider.special.distractor", Integer.class, 200);

        plugin.getConfigRegistry().register("settings.points.seeker.active-hunter.amount-per-second", Integer.class, 2);
        plugin.getConfigRegistry().register("settings.points.seeker.active-hunter.range", Double.class, 16.0);
        plugin.getConfigRegistry().register("settings.points.seeker.utility-success.amount", Integer.class, 40);
        plugin.getConfigRegistry().register("settings.points.seeker.interception.amount", Integer.class, 15);
        plugin.getConfigRegistry().register("settings.points.seeker.kill.amount", Integer.class, 300);
        plugin.getConfigRegistry().register("settings.points.seeker.assist.amount", Integer.class, 100);
        plugin.getConfigRegistry().register("settings.points.seeker.assist.range", Double.class, 16.0);
        plugin.getConfigRegistry().register("settings.points.seeker.special.bloodhound", Integer.class, 200);
        plugin.getConfigRegistry().register("settings.points.seeker.first-blood.amount", Integer.class, 100);
    }


    private static void registerSections(HideAndSeek plugin) {
        plugin.getSectionRegistry().register(SectionDefinition.builder("game").icon(Material.COMPARATOR).build());
        plugin.getSectionRegistry().register(SectionDefinition.builder("blockstats").icon(Material.BOOKSHELF).build());
        plugin.getSectionRegistry().register(SectionDefinition.builder("hider-items").icon(Material.PLAYER_HEAD).build());
        plugin.getSectionRegistry().register(SectionDefinition.builder("hider-items.sound").icon(Material.CAT_SPAWN_EGG).build());
        plugin.getSectionRegistry().register(SectionDefinition.builder("hider-items.explosion").icon(Material.RED_CANDLE).build());
        plugin.getSectionRegistry().register(SectionDefinition.builder("hider-items.speed-boost").icon(Material.WOODEN_HOE).build());
        plugin.getSectionRegistry().register(SectionDefinition.builder("hider-items.knockback-stick").icon(Material.STICK).build());
        plugin.getSectionRegistry().register(SectionDefinition.builder("hider-items.block-swap").icon(Material.ENDER_PEARL).build());
        plugin.getSectionRegistry().register(SectionDefinition.builder("hider-items.big-firecracker").icon(Material.TNT).build());
        plugin.getSectionRegistry().register(SectionDefinition.builder("hider-items.firework-rocket").icon(Material.FIREWORK_ROCKET).build());
        plugin.getSectionRegistry().register(SectionDefinition.builder("hider-items.medkit").icon(Material.GOLDEN_APPLE).build());
        plugin.getSectionRegistry().register(SectionDefinition.builder("hider-items.totem").icon(Material.TOTEM_OF_UNDYING).build());
        plugin.getSectionRegistry().register(SectionDefinition.builder("hider-items.invisibility-cloak").icon(Material.PHANTOM_MEMBRANE).build());
        plugin.getSectionRegistry().register(SectionDefinition.builder("hider-items.slowness-ball").icon(Material.SNOWBALL).build());
        plugin.getSectionRegistry().register(SectionDefinition.builder("hider-items.smoke-bomb").icon(Material.GRAY_DYE).build());
        plugin.getSectionRegistry().register(SectionDefinition.builder("hider-items.ghost-essence").icon(Material.GHAST_TEAR).build());
        plugin.getSectionRegistry().register(SectionDefinition.builder("timer").icon(Material.CLOCK).build());
        plugin.getSectionRegistry().register(SectionDefinition.builder("seeker-items").icon(Material.ENDER_EYE).build());
        plugin.getSectionRegistry().register(SectionDefinition.builder("seeker-items.grappling-hook").icon(Material.FISHING_ROD).build());
        plugin.getSectionRegistry().register(SectionDefinition.builder("seeker-items.ink-splash").icon(Material.INK_SAC).build());
        plugin.getSectionRegistry().register(SectionDefinition.builder("seeker-items.lightning-freeze").icon(Material.LIGHTNING_ROD).build());
        plugin.getSectionRegistry().register(SectionDefinition.builder("seeker-items.glowing-compass").icon(Material.COMPASS).build());
        plugin.getSectionRegistry().register(SectionDefinition.builder("seeker-items.curse-spell").icon(Material.ENCHANTED_BOOK).build());
        plugin.getSectionRegistry().register(SectionDefinition.builder("seeker-items.block-randomizer").icon(Material.BLAZE_POWDER).build());
        plugin.getSectionRegistry().register(SectionDefinition.builder("seeker-items.chain-pull").icon(Material.LEAD).build());
        plugin.getSectionRegistry().register(SectionDefinition.builder("seeker-items.proximity-sensor").icon(Material.REDSTONE_TORCH).build());
        plugin.getSectionRegistry().register(SectionDefinition.builder("seeker-items.cage-trap").icon(Material.IRON_BARS).build());
        plugin.getSectionRegistry().register(SectionDefinition.builder("seeker-items.sword-of-seeking").icon(Material.IRON_SWORD).build());
        plugin.getSectionRegistry().register(SectionDefinition.builder("seeker-items.seeker-sword-throw").icon(Material.DIAMOND_SWORD).build());
        plugin.getSectionRegistry().register(SectionDefinition.builder("loadout").icon(Material.ARMOR_STAND).build());
        plugin.getSectionRegistry().register(SectionDefinition.builder("loadout.token-cost").icon(Material.GOLD_BLOCK).build());
        plugin.getSectionRegistry().register(SectionDefinition.builder("points").icon(Material.EMERALD).build());
        plugin.getSectionRegistry().register(SectionDefinition.builder("points.tracking").icon(Material.CLOCK).build());
        plugin.getSectionRegistry().register(SectionDefinition.builder("points.hider").icon(Material.PLAYER_HEAD).build());
        plugin.getSectionRegistry().register(SectionDefinition.builder("points.hider.survival").icon(Material.EMERALD).build());
        plugin.getSectionRegistry().register(SectionDefinition.builder("points.hider.proximity").icon(Material.SCULK_SENSOR).build());
        plugin.getSectionRegistry().register(SectionDefinition.builder("points.hider.near-miss").icon(Material.TOTEM_OF_UNDYING).build());
        plugin.getSectionRegistry().register(SectionDefinition.builder("points.hider.taunt").icon(Material.FIREWORK_ROCKET).build());
        plugin.getSectionRegistry().register(SectionDefinition.builder("points.hider.sharpshooter").icon(Material.CROSSBOW).build());
        plugin.getSectionRegistry().register(SectionDefinition.builder("points.hider.survivor").icon(Material.SHIELD).build());
        plugin.getSectionRegistry().register(SectionDefinition.builder("points.hider.special").icon(Material.BELL).build());
        plugin.getSectionRegistry().register(SectionDefinition.builder("points.seeker").icon(Material.ENDER_EYE).build());
        plugin.getSectionRegistry().register(SectionDefinition.builder("points.seeker.active-hunter").icon(Material.IRON_SWORD).build());
        plugin.getSectionRegistry().register(SectionDefinition.builder("points.seeker.utility-success").icon(Material.ENDER_EYE).build());
        plugin.getSectionRegistry().register(SectionDefinition.builder("points.seeker.interception").icon(Material.IRON_SWORD).build());
        plugin.getSectionRegistry().register(SectionDefinition.builder("points.seeker.kill").icon(Material.NETHERITE_SWORD).build());
        plugin.getSectionRegistry().register(SectionDefinition.builder("points.seeker.assist").icon(Material.CHAINMAIL_CHESTPLATE).build());
        plugin.getSectionRegistry().register(SectionDefinition.builder("points.seeker.special").icon(Material.WOLF_SPAWN_EGG).build());
    }

    public static void registerSettings(HideAndSeek plugin) {
        plugin.getSettingRegistry().register(SettingDefinition.builder("game.gametype", SettingType.ENUM, GameModeEnum.class)
                .defaultValue(getEnumConfigValue(plugin, "game.gametype", GameModeEnum.class, GameModeEnum.NORMAL))
                .customIcon(Material.COMMAND_BLOCK)
                .description("Gamemode of the game")
                .valueIcons(Map.of(
                        GameModeEnum.NORMAL, Material.PLAYER_HEAD,
                        GameModeEnum.BLOCK, Material.COBBLESTONE,
                        GameModeEnum.SMALL, Material.IRON_NUGGET
                ))
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("game.gamestyle", SettingType.ENUM, GameStyleEnum.class)
                .defaultValue(getEnumConfigValue(plugin, "game.gamestyle", GameStyleEnum.class, GameStyleEnum.SPECTATOR))
                .customIcon(Material.NETHER_STAR)
                .description("Gamemode of the game")
                .valueIcons(Map.of(
                        GameStyleEnum.SPECTATOR, Material.ENDER_EYE,
                        GameStyleEnum.INVASION, Material.SUSPICIOUS_STEW,
                        GameStyleEnum.INFINITE, Material.BLAZE_POWDER
                ))
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("game.hiding_time", SettingType.INTEGER, Integer.class)
                .defaultValue(getConfigValue(plugin, "game.hiding_time", 60))
                .range(10, 600)
                .description("Hiding phase duration in seconds")
                .customIcon(Material.CLOCK)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("game.seeking_time", SettingType.INTEGER, Integer.class)
                .defaultValue(getConfigValue(plugin, "game.seeking_time", 300))
                .range(60, 1800)
                .description("Seeking phase duration in seconds")
                .customIcon(Material.CLOCK)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("game.hider_invisibility", SettingType.BOOLEAN, Boolean.class)
                .defaultValue(getConfigValue(plugin, "game.hider_invisibility", false))
                .description("Grant hiders invisibility during hiding phase")
                .customIcon(Material.POTION)
                .valueIconStacks(Map.of(
                        true, setEnchanted(Material.POTION, true),
                        false, setEnchanted(Material.POTION, false)
                ))
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("game.small_mode_size", SettingType.DOUBLE, Double.class)
                .defaultValue(getConfigValue(plugin, "game.small_mode_size", 0.5))
                .description("Size scale for SMALL mode hiders (0.1 = tiny, 1.0 = normal)")
                .customIcon(Material.SLIME_BALL)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("game.random_team_distribution", SettingType.BOOLEAN, Boolean.class)
                .defaultValue(getConfigValue(plugin, "game.random_team_distribution", true))
                .description("Enable random distribution of players into hider/seeker teams")
                .customIcon(Material.PLAYER_HEAD)
                .valueIconStacks(Map.of(
                        true, setEnchanted(Material.PLAYER_HEAD, true),
                        false, setEnchanted(Material.PLAYER_HEAD, false)
                ))
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("game.use_preferred_modes", SettingType.BOOLEAN, Boolean.class)
                .defaultValue(getConfigValue(plugin, "game.use_preferred_modes", true))
                .description("Only select maps that have the current game mode in their preferred modes list")
                .customIcon(Material.MAP)
                .valueIconStacks(Map.of(
                        true, setEnchanted(Material.MAP, true),
                        false, setEnchanted(Material.MAP, false)
                ))
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("game.use_map_specific_timings", SettingType.BOOLEAN, Boolean.class)
                .defaultValue(getConfigValue(plugin, "game.use_map_specific_timings", true))
                .description("Use hiding/seeking times from map config if available, otherwise use global settings")
                .customIcon(Material.CLOCK)
                .valueIconStacks(Map.of(
                        true, setEnchanted(Material.CLOCK, true),
                        false, setEnchanted(Material.CLOCK, false)
                ))
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("game.use_map_specific_seeker_count", SettingType.BOOLEAN, Boolean.class)
                .defaultValue(getConfigValue(plugin, "game.use_map_specific_seeker_count", true))
                .description("Use seeker configuration from map config if available, otherwise use global settings")
                .customIcon(Material.IRON_SWORD)
                .valueIconStacks(Map.of(
                        true, setEnchanted(Material.IRON_SWORD, true),
                        false, setEnchanted(Material.IRON_SWORD, false)
                ))
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("game.use_map_specific_player_limits", SettingType.BOOLEAN, Boolean.class)
                .defaultValue(getConfigValue(plugin, "game.use_map_specific_player_limits", true))
                .description("Use player count recommendations from map config if available")
                .customIcon(Material.PLAYER_HEAD)
                .valueIconStacks(Map.of(
                        true, setEnchanted(Material.PLAYER_HEAD, true),
                        false, setEnchanted(Material.PLAYER_HEAD, false)
                ))
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("game.fixed_seeker_team", SettingType.STRING, String.class)
                .defaultValue(getConfigValue(plugin, "game.fixed_seeker_team", ""))
                .description("Fixed seeker team (leave empty for random). Set to a team name to always use that team as seekers")
                .customIcon(Material.WHITE_BANNER)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("game.seeker_count", SettingType.INTEGER, Integer.class)
                .defaultValue(getConfigValue(plugin, "game.seeker_count", 1))
                .range(1, 10)
                .description("Number of seekers (if random distribution is enabled)")
                .customIcon(Material.IRON_SWORD)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("game.hider_health", SettingType.INTEGER, Integer.class)
                .defaultValue(getConfigValue(plugin, "game.hider_health", 20))
                .range(1, 20)
                .description("Health of hiders (in half-hearts)")
                .customIcon(Material.GOLDEN_APPLE)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("game.block_view_height", SettingType.FLOAT, Float.class)
                .defaultValue(getConfigValue(plugin, "game.block_view_height", 0.1f))
                .rangeFloat(0f, 1.5f)
                .description("View Height of player when they hide in a block")
                .customIcon(Material.LADDER)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("game.block_size_to_block", SettingType.BOOLEAN, Boolean.class)
                .defaultValue(getConfigValue(plugin, "game.block_size_to_block", false))
                .description("Scale hiders to the hidden block's height while hidden in BLOCK mode")
                .customIcon(Material.SCAFFOLDING)
                .valueIconStacks(Map.of(
                        true, setEnchanted(Material.SCAFFOLDING, true),
                        false, setEnchanted(Material.SCAFFOLDING, false)
                ))
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("game.seeker_kill_mode", SettingType.ENUM, SeekerKillModeEnum.class)
                .defaultValue(getEnumConfigValue(plugin, "game.seeker_kill_mode", SeekerKillModeEnum.class, SeekerKillModeEnum.NORMAL))
                .description("How seekers kill hiders")
                .valueIcons(Map.of(
                        SeekerKillModeEnum.NORMAL, Material.IRON_SWORD,
                        SeekerKillModeEnum.ONE_HIT, Material.DIAMOND_SWORD,
                        SeekerKillModeEnum.GAZE_KILL, Material.ENDER_EYE
                ))
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("game.gaze_kill_max_distance", SettingType.DOUBLE, Double.class)
                .defaultValue(getConfigValue(plugin, "game.gaze_kill_max_distance", 10.0))
                .rangeDouble(5.0, 50.0)
                .description("Maximum distance for gaze kill in blocks")
                .customIcon(Material.SPYGLASS)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("game.gaze_kill_fov", SettingType.DOUBLE, Double.class)
                .defaultValue(getConfigValue(plugin, "game.gaze_kill_fov", 30.0))
                .rangeDouble(10.0, 180.0)
                .description("Field of view angle for gaze kill in degrees")
                .customIcon(Material.BOW)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("game.gaze_kill_show_particles", SettingType.BOOLEAN, Boolean.class)
                .defaultValue(getConfigValue(plugin, "game.gaze_kill_show_particles", true))
                .description("Show particles when looking at hiders during gaze kill mode")
                .customIcon(Material.REDSTONE)
                .valueIconStacks(Map.of(
                        true, setEnchanted(Material.REDSTONE, true),
                        false, setEnchanted(Material.REDSTONE, false)
                ))
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("game.auto_cleanup_after_round", SettingType.BOOLEAN, Boolean.class)
                .defaultValue(getConfigValue(plugin, "game.auto_cleanup_after_round", true))
                .description("Automatically teleport players to lobby and delete map after round")
                .customIcon(ItemStack.of(Material.LAVA_BUCKET))
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("game.small_mode_seeker_size", SettingType.DOUBLE, Double.class)
                .defaultValue(getConfigValue(plugin, "game.small_mode_seeker_size", 1.0))
                .rangeDouble(0.1, 2.0)
                .description("Size scale for seekers in SMALL mode (1.0 = normal size)")
                .customIcon(Material.MAGMA_CREAM)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("blockstats.show-names", SettingType.BOOLEAN, Boolean.class)
                .defaultValue(getConfigValue(plugin, "blockstats.show-names", false))
                .description("Show player names in Block Statistics GUI")
                .customIcon(Material.NAME_TAG)
                .valueIconStacks(Map.of(
                        true, setEnchanted(Material.NAME_TAG, true),
                        false, setEnchanted(Material.NAME_TAG, false)
                ))
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.random-block.uses", SettingType.INTEGER, Integer.class)
                .defaultValue(getConfigValue(plugin, "hider-items.random-block.uses", 5))
                .range(1, 20)
                .description("Max uses for random block transform item")
                .customIcon(Material.COBBLESTONE)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.crossbow.hits-per-upgrade", SettingType.INTEGER, Integer.class)
                .defaultValue(getConfigValue(plugin, "hider-items.crossbow.hits-per-upgrade", 3))
                .range(1, 10)
                .description("Hits needed to upgrade speed boost")
                .customIcon(Material.ARROW)
                .build());


        plugin.getSettingRegistry().register(SettingDefinition.builder("timer.hiding_color1", SettingType.STRING, String.class)
                .defaultValue(getConfigValue(plugin, "timer.hiding_color1", "#FF0000"))
                .description("Primary color for hiding timer (hex code)")
                .itemProvider(value -> colorIconFromHex(value, "CONCRETE_POWDER", Material.RED_CONCRETE_POWDER))
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("timer.hiding_color2", SettingType.STRING, String.class)
                .defaultValue(getConfigValue(plugin, "timer.hiding_color2", "#0000FF"))
                .description("Secondary color for hiding timer (hex code)")
                .itemProvider(value -> colorIconFromHex(value, "CONCRETE", Material.BLUE_CONCRETE))
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("timer.seeking_color1", SettingType.STRING, String.class)
                .defaultValue(getConfigValue(plugin, "timer.seeking_color1", "#FFFF00"))
                .description("Primary color for seeking timer (hex code)")
                .itemProvider(value -> colorIconFromHex(value, "WOOL", Material.YELLOW_WOOL))
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("timer.seeking_color2", SettingType.STRING, String.class)
                .defaultValue(getConfigValue(plugin, "timer.seeking_color2", "#00FFFF"))
                .description("Secondary color for seeking timer (hex code)")
                .itemProvider(value -> colorIconFromHex(value, "STAINED_GLASS", Material.CYAN_STAINED_GLASS))
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("timer.animation_type", SettingType.ENUM, AnimationType.class)
                .defaultValue(getEnumConfigValue(plugin, "timer.animation_type", AnimationType.class, AnimationType.WAVE))
                .description("Timer animation type")
                .customIcon(Material.AMETHYST_SHARD)
                .valueIcons(Map.of(
                        AnimationType.WAVE, Material.WATER_BUCKET,
                        AnimationType.GRADIENT, Material.AMETHYST_BLOCK,
                        AnimationType.PULSE, Material.REDSTONE_LAMP
                ))
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("timer.animation_speed", SettingType.DOUBLE, Double.class)
                .defaultValue(getConfigValue(plugin, "timer.animation_speed", 0.5))
                .range(0, 2)
                .description("Timer animation speed (0.1 = slow, 2.0 = fast)")
                .customIcon(Material.REDSTONE)
                .build());


        plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.sound.cooldown", SettingType.INTEGER, Integer.class)
                .defaultValue(getConfigValue(plugin, "hider-items.sound.cooldown", 4))
                .range(1, 30)
                .description("Cooldown for cat sound item in seconds")
                .customIcon(Material.CLOCK)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.sound.points", SettingType.INTEGER, Integer.class)
                .defaultValue(getConfigValue(plugin, "hider-items.sound.points", 5))
                .range(0, 100)
                .description("Points awarded for using cat sound")
                .customIcon(Material.EMERALD)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.sound.volume", SettingType.DOUBLE, Double.class)
                .defaultValue(getConfigValue(plugin, "hider-items.sound.volume", 0.75))
                .rangeDouble(0.1, 2.0)
                .description("Volume of cat sound (0.1 = quiet, 2.0 = loud)")
                .customIcon(Material.JUKEBOX)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.sound.pitch", SettingType.DOUBLE, Double.class)
                .defaultValue(getConfigValue(plugin, "hider-items.sound.pitch", 0.8))
                .rangeDouble(0.5, 2.0)
                .description("Pitch of cat sound (0.5 = low, 2.0 = high)")
                .customIcon(Material.NOTE_BLOCK)
                .build());


        plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.explosion.cooldown", SettingType.INTEGER, Integer.class)
                .defaultValue(getConfigValue(plugin, "hider-items.explosion.cooldown", 8))
                .range(0, 30)
                .description("Cooldown for firecracker item in seconds")
                .customIcon(Material.CLOCK)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.explosion.points", SettingType.INTEGER, Integer.class)
                .defaultValue(getConfigValue(plugin, "hider-items.explosion.points", 10))
                .range(0, 100)
                .description("Points awarded for using firecracker")
                .customIcon(Material.EMERALD)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.explosion.volume", SettingType.DOUBLE, Double.class)
                .defaultValue(getConfigValue(plugin, "hider-items.explosion.volume", 0.65))
                .rangeDouble(0.1, 2.0)
                .description("Volume of explosion sound")
                .customIcon(Material.JUKEBOX)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.explosion.pitch", SettingType.DOUBLE, Double.class)
                .defaultValue(getConfigValue(plugin, "hider-items.explosion.pitch", 1.5))
                .rangeDouble(0.5, 2.0)
                .description("Pitch of explosion sound")
                .customIcon(Material.NOTE_BLOCK)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.explosion.smoke-particles", SettingType.INTEGER, Integer.class)
                .defaultValue(getConfigValue(plugin, "hider-items.explosion.smoke-particles", 3))
                .range(1, 20)
                .description("Number of smoke particles per tick")
                .customIcon(Material.GUNPOWDER)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.explosion.fuse-time", SettingType.INTEGER, Integer.class)
                .defaultValue(getConfigValue(plugin, "hider-items.explosion.fuse-time", 40))
                .range(10, 100)
                .description("Fuse time in ticks before explosion (20 ticks = 1 second)")
                .customIcon(Material.CLOCK)
                .build());


        plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.speed-boost.type", SettingType.ENUM, SpeedBoostType.class)
                .defaultValue(getEnumConfigValue(plugin, "hider-items.speed-boost.type", SpeedBoostType.class, SpeedBoostType.SPEED_EFFECT))
                .description("Speed boost type: SPEED_EFFECT or VELOCITY_BOOST")
                .customIcon(Material.FEATHER)
                .valueIcons(Map.of(
                        SpeedBoostType.SPEED_EFFECT, Material.POTION,
                        SpeedBoostType.VELOCITY_BOOST, Material.FEATHER
                ))
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.speed-boost.cooldown", SettingType.INTEGER, Integer.class)
                .defaultValue(getConfigValue(plugin, "hider-items.speed-boost.cooldown", 10))
                .range(0, 60)
                .description("Cooldown for speed boost in seconds")
                .customIcon(Material.CLOCK)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.speed-boost.duration", SettingType.INTEGER, Integer.class)
                .defaultValue(getConfigValue(plugin, "hider-items.speed-boost.duration", 5))
                .range(1, 30)
                .description("Duration of speed effect in seconds (SPEED_EFFECT only)")
                .customIcon(Material.CLOCK)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.speed-boost.boost-power", SettingType.DOUBLE, Double.class)
                .defaultValue(getConfigValue(plugin, "hider-items.speed-boost.boost-power", 0.5))
                .rangeDouble(0.1, 2.0)
                .description("Power of velocity boost (VELOCITY_BOOST only)")
                .customIcon(Material.FEATHER)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.knockback-stick.level", SettingType.INTEGER, Integer.class)
                .defaultValue(getConfigValue(plugin, "hider-items.knockback-stick.level", 2))
                .range(1, 5)
                .description("Knockback level for knockback stick")
                .customIcon(Material.ANVIL)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.block-swap.cooldown", SettingType.INTEGER, Integer.class)
                .defaultValue(getConfigValue(plugin, "hider-items.block-swap.cooldown", 15))
                .range(0, 60)
                .description("Cooldown for block swap in seconds")
                .customIcon(Material.CLOCK)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.block-swap.range", SettingType.DOUBLE, Double.class)
                .defaultValue(getConfigValue(plugin, "hider-items.block-swap.range", 50.0))
                .rangeDouble(5.0, 200.0)
                .description("Maximum swap range for block swap")
                .customIcon(Material.BLAZE_POWDER)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.big-firecracker.cooldown", SettingType.INTEGER, Integer.class)
                .defaultValue(getConfigValue(plugin, "hider-items.big-firecracker.cooldown", 12))
                .range(0, 60)
                .description("Cooldown for big firecracker in seconds")
                .customIcon(Material.CLOCK)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.big-firecracker.fuse-time", SettingType.INTEGER, Integer.class)
                .defaultValue(getConfigValue(plugin, "hider-items.big-firecracker.fuse-time", 60))
                .range(10, 200)
                .description("Fuse time in ticks before big explosion")
                .customIcon(Material.CLOCK)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.big-firecracker.mini-fuse-time", SettingType.INTEGER, Integer.class)
                .defaultValue(getConfigValue(plugin, "hider-items.big-firecracker.mini-fuse-time", 30))
                .range(5, 100)
                .description("Fuse time in ticks for mini firecrackers")
                .customIcon(Material.CLOCK)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.big-firecracker.mini-count", SettingType.INTEGER, Integer.class)
                .defaultValue(getConfigValue(plugin, "hider-items.big-firecracker.mini-count", 3))
                .range(1, 10)
                .description("Number of mini firecrackers")
                .customIcon(Material.FIREWORK_STAR)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.big-firecracker.points", SettingType.INTEGER, Integer.class)
                .defaultValue(getConfigValue(plugin, "hider-items.big-firecracker.points", 20))
                .range(0, 200)
                .description("Points for big firecracker")
                .customIcon(Material.EMERALD)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.big-firecracker.volume", SettingType.DOUBLE, Double.class)
                .defaultValue(getConfigValue(plugin, "hider-items.big-firecracker.volume", 1.2))
                .rangeDouble(0.1, 2.0)
                .description("Explosion volume for big firecracker")
                .customIcon(Material.JUKEBOX)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.big-firecracker.pitch", SettingType.DOUBLE, Double.class)
                .defaultValue(getConfigValue(plugin, "hider-items.big-firecracker.pitch", 0.5))
                .rangeDouble(0.1, 2.0)
                .description("Explosion pitch for big firecracker")
                .customIcon(Material.NOTE_BLOCK)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.firework-rocket.cooldown", SettingType.INTEGER, Integer.class)
                .defaultValue(getConfigValue(plugin, "hider-items.firework-rocket.cooldown", 10))
                .range(0, 60)
                .description("Cooldown for firework rocket in seconds")
                .customIcon(Material.CLOCK)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.firework-rocket.target-y", SettingType.INTEGER, Integer.class)
                .defaultValue(getConfigValue(plugin, "hider-items.firework-rocket.target-y", 128))
                .range(-64, 320)
                .description("Target Y for firework explosion")
                .customIcon(Material.LADDER)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.firework-rocket.points", SettingType.INTEGER, Integer.class)
                .defaultValue(getConfigValue(plugin, "hider-items.firework-rocket.points", 15))
                .range(0, 200)
                .description("Points for firework rocket")
                .customIcon(Material.EMERALD)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.firework-rocket.volume", SettingType.DOUBLE, Double.class)
                .defaultValue(getConfigValue(plugin, "hider-items.firework-rocket.volume", 10.0))
                .rangeDouble(0.1, 15.0)
                .description("Explosion volume for firework rocket")
                .customIcon(Material.JUKEBOX)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.medkit.cooldown", SettingType.INTEGER, Integer.class)
                .defaultValue(getConfigValue(plugin, "hider-items.medkit.cooldown", 30))
                .range(0, 120)
                .description("Cooldown for medkit in seconds")
                .customIcon(Material.CLOCK)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.medkit.channel-time", SettingType.INTEGER, Integer.class)
                .defaultValue(getConfigValue(plugin, "hider-items.medkit.channel-time", 5))
                .range(1, 30)
                .description("Time to stand still before healing")
                .customIcon(Material.CLOCK)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.medkit.heal-amount", SettingType.DOUBLE, Double.class)
                .defaultValue(getConfigValue(plugin, "hider-items.medkit.heal-amount", 20.0))
                .rangeDouble(1.0, 40.0)
                .description("Heal amount in half-hearts")
                .customIcon(Material.GLISTERING_MELON_SLICE)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.totem.effect-duration", SettingType.INTEGER, Integer.class)
                .defaultValue(getConfigValue(plugin, "hider-items.totem.effect-duration", 30))
                .range(5, 120)
                .description("Duration of revive effect in seconds")
                .customIcon(Material.CLOCK)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.totem.max-uses", SettingType.INTEGER, Integer.class)
                .defaultValue(getConfigValue(plugin, "hider-items.totem.max-uses", 1))
                .range(1, 5)
                .description("Max uses per totem")
                .customIcon(Material.TOTEM_OF_UNDYING)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.invisibility-cloak.cooldown", SettingType.INTEGER, Integer.class)
                .defaultValue(getConfigValue(plugin, "hider-items.invisibility-cloak.cooldown", 20))
                .range(0, 120)
                .description("Cooldown for invisibility cloak in seconds")
                .customIcon(Material.CLOCK)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.invisibility-cloak.duration", SettingType.INTEGER, Integer.class)
                .defaultValue(getConfigValue(plugin, "hider-items.invisibility-cloak.duration", 8))
                .range(1, 30)
                .description("Duration of invisibility in seconds")
                .customIcon(Material.CLOCK)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.slowness-ball.cooldown", SettingType.INTEGER, Integer.class)
                .defaultValue(getConfigValue(plugin, "hider-items.slowness-ball.cooldown", 10))
                .range(0, 60)
                .description("Cooldown for slowness ball in seconds")
                .customIcon(Material.CLOCK)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.slowness-ball.duration", SettingType.INTEGER, Integer.class)
                .defaultValue(getConfigValue(plugin, "hider-items.slowness-ball.duration", 6))
                .range(1, 30)
                .description("Duration of slowness effect in seconds")
                .customIcon(Material.CLOCK)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.slowness-ball.amplifier", SettingType.INTEGER, Integer.class)
                .defaultValue(getConfigValue(plugin, "hider-items.slowness-ball.amplifier", 1))
                .range(0, 10)
                .description("Slowness effect amplifier (0 = slowness I, 1 = slowness II, etc)")
                .customIcon(Material.FERMENTED_SPIDER_EYE)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.smoke-bomb.cooldown", SettingType.INTEGER, Integer.class)
                .defaultValue(getConfigValue(plugin, "hider-items.smoke-bomb.cooldown", 15))
                .range(0, 60)
                .description("Cooldown for smoke bomb in seconds")
                .customIcon(Material.CLOCK)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.smoke-bomb.duration", SettingType.INTEGER, Integer.class)
                .defaultValue(getConfigValue(plugin, "hider-items.smoke-bomb.duration", 8))
                .range(1, 30)
                .description("Duration of smoke cloud in seconds")
                .customIcon(Material.CLOCK)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.smoke-bomb.radius", SettingType.INTEGER, Integer.class)
                .defaultValue(getConfigValue(plugin, "hider-items.smoke-bomb.radius", 4))
                .range(1, 15)
                .description("Radius of smoke cloud in blocks")
                .customIcon(Material.BLAZE_POWDER)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.ghost-essence.cooldown", SettingType.INTEGER, Integer.class)
                .defaultValue(getConfigValue(plugin, "hider-items.ghost-essence.cooldown", 25))
                .range(0, 300)
                .description("Cooldown for ghost essence in seconds")
                .customIcon(Material.CLOCK)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.ghost-essence.max-radius", SettingType.INTEGER, Integer.class)
                .defaultValue(getConfigValue(plugin, "hider-items.ghost-essence.max-radius", 15))
                .range(1, 100)
                .description("Maximum radius (in blocks) a ghost can move from their body")
                .customIcon(Material.COMPASS)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.ghost-essence.min-light-block", SettingType.INTEGER, Integer.class)
                .defaultValue(getConfigValue(plugin, "hider-items.ghost-essence.min-light-block", 1))
                .range(0, 15)
                .description("Minimum block light level required to materialize")
                .customIcon(Material.TORCH)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.ghost-essence.min-light-sky", SettingType.INTEGER, Integer.class)
                .defaultValue(getConfigValue(plugin, "hider-items.ghost-essence.min-light-sky", 1))
                .range(0, 15)
                .description("Minimum sky light level required to materialize")
                .customIcon(Material.SUNFLOWER)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.ghost-essence.flying-speed", SettingType.FLOAT, Float.class)
                .defaultValue(getConfigValue(plugin, "hider-items.ghost-essence.flying-speed", 0.01f))
                .rangeFloat(0.001f, 1.0f)
                .description("Client-side flying speed while ghostly")
                .customIcon(Material.FEATHER)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.ghost-essence.max-duration", SettingType.FLOAT, Float.class)
                .defaultValue(getConfigValue(plugin, "hider-items.ghost-essence.max-duration", 1.5f))
                .rangeFloat(1.0f, 60.0f)
                .description("Max ghost duration in seconds")
                .customIcon(Material.CLOCK)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.ghost-essence.boost-power", SettingType.FLOAT, Float.class)
                .defaultValue(getConfigValue(plugin, "hider-items.ghost-essence.boost-power", 1.5f))
                .rangeFloat(0.0f, 5.0f)
                .description("Initial boost power when activating ghost essence")
                .customIcon(Material.GHAST_TEAR)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.ghost-essence.particle-mode", SettingType.ENUM, GhostEssenceParticleMode.class)
                .defaultValue(getEnumConfigValue(plugin, "hider-items.ghost-essence.particle-mode", GhostEssenceParticleMode.class, GhostEssenceParticleMode.FLYING))
                .description("Particle effect mode for ghost essence")
                .customIcon(Material.SOUL_TORCH)
                .valueIcons(Map.of(
                        GhostEssenceParticleMode.FLYING, Material.FEATHER,
                        GhostEssenceParticleMode.SNAP, Material.AMETHYST_SHARD,
                        GhostEssenceParticleMode.NONE, Material.BARRIER
                ))
                .build());


        plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.grappling-hook.cooldown", SettingType.INTEGER, Integer.class)

                .defaultValue(getConfigValue(plugin, "seeker-items.grappling-hook.cooldown", 2))
                .range(0, 30)
                .description("Cooldown for grappling hook in seconds")
                .customIcon(Material.CLOCK)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.grappling-hook.speed", SettingType.DOUBLE, Double.class)
                .defaultValue(getConfigValue(plugin, "seeker-items.grappling-hook.speed", 1.5))
                .rangeDouble(0.3, 3.0)
                .description("Base speed for grappling hook pull")
                .customIcon(Material.BLAZE_POWDER)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.ink-splash.cooldown", SettingType.INTEGER, Integer.class)
                .defaultValue(getConfigValue(plugin, "seeker-items.ink-splash.cooldown", 20))
                .range(0, 60)
                .description("Cooldown for ink splash in seconds")
                .customIcon(Material.CLOCK)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.ink-splash.radius", SettingType.INTEGER, Integer.class)
                .defaultValue(getConfigValue(plugin, "seeker-items.ink-splash.radius", 25))
                .range(1, 50)
                .description("Radius of ink splash")
                .customIcon(Material.COMPASS)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.ink-splash.duration", SettingType.INTEGER, Integer.class)
                .defaultValue(getConfigValue(plugin, "seeker-items.ink-splash.duration", 7))
                .range(1, 30)
                .description("Duration of ink blindness in seconds")
                .customIcon(Material.CLOCK)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.lightning-freeze.cooldown", SettingType.INTEGER, Integer.class)
                .defaultValue(getConfigValue(plugin, "seeker-items.lightning-freeze.cooldown", 60))
                .range(10, 300)
                .description("Cooldown for lightning freeze in seconds")
                .customIcon(Material.CLOCK)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.lightning-freeze.duration", SettingType.INTEGER, Integer.class)
                .defaultValue(getConfigValue(plugin, "seeker-items.lightning-freeze.duration", 5))
                .range(1, 30)
                .description("Duration of freeze in seconds")
                .customIcon(Material.CLOCK)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.glowing-compass.cooldown", SettingType.INTEGER, Integer.class)
                .defaultValue(getConfigValue(plugin, "seeker-items.glowing-compass.cooldown", 25))
                .range(0, 60)
                .description("Cooldown for glowing compass in seconds")
                .customIcon(Material.CLOCK)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.glowing-compass.duration", SettingType.INTEGER, Integer.class)
                .defaultValue(getConfigValue(plugin, "seeker-items.glowing-compass.duration", 10))
                .range(1, 60)
                .description("Duration of glow in seconds")
                .customIcon(Material.CLOCK)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.glowing-compass.range", SettingType.DOUBLE, Double.class)
                .defaultValue(getConfigValue(plugin, "seeker-items.glowing-compass.range", 50.0))
                .rangeDouble(10.0, 200.0)
                .description("Range to detect nearest hider")
                .customIcon(Material.COMPASS)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.curse-spell.cooldown", SettingType.INTEGER, Integer.class)
                .defaultValue(getConfigValue(plugin, "seeker-items.curse-spell.cooldown", 30))
                .range(0, 60)
                .description("Cooldown for curse spell in seconds")
                .customIcon(Material.CLOCK)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.curse-spell.active-duration", SettingType.INTEGER, Integer.class)
                .defaultValue(getConfigValue(plugin, "seeker-items.curse-spell.active-duration", 10))
                .range(1, 60)
                .description("Duration curse spell is active on sword")
                .customIcon(Material.CLOCK)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.curse-spell.curse-duration", SettingType.INTEGER, Integer.class)
                .defaultValue(getConfigValue(plugin, "seeker-items.curse-spell.curse-duration", 8))
                .range(1, 60)
                .description("Duration of curse on hider")
                .customIcon(Material.CLOCK)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.curse-spell.small-shrink-delay", SettingType.INTEGER, Integer.class)
                .defaultValue(getConfigValue(plugin, "seeker-items.curse-spell.small-shrink-delay", 8))
                .range(1, 60)
                .description("Delay before shrinking back in SMALL mode")
                .customIcon(Material.CLOCK)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.block-randomizer.cooldown", SettingType.INTEGER, Integer.class)
                .defaultValue(getConfigValue(plugin, "seeker-items.block-randomizer.cooldown", 45))
                .range(10, 120)
                .description("Cooldown for block randomizer in seconds")
                .customIcon(Material.CLOCK)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.chain-pull.cooldown", SettingType.INTEGER, Integer.class)
                .defaultValue(getConfigValue(plugin, "seeker-items.chain-pull.cooldown", 12))
                .range(0, 60)
                .description("Cooldown for chain pull in seconds")
                .customIcon(Material.CLOCK)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.chain-pull.range", SettingType.DOUBLE, Double.class)
                .defaultValue(getConfigValue(plugin, "seeker-items.chain-pull.range", 30.0))
                .rangeDouble(5.0, 100.0)
                .description("Maximum range for chain pull")
                .customIcon(Material.LEAD)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.chain-pull.pull-power", SettingType.DOUBLE, Double.class)
                .defaultValue(getConfigValue(plugin, "seeker-items.chain-pull.pull-power", 2.0))
                .rangeDouble(0.5, 5.0)
                .description("Pull power multiplier")
                .customIcon(Material.IRON_BLOCK)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.chain-pull.slowness-duration", SettingType.INTEGER, Integer.class)
                .defaultValue(getConfigValue(plugin, "seeker-items.chain-pull.slowness-duration", 3))
                .range(1, 20)
                .description("Slowness duration in seconds after pull")
                .customIcon(Material.CLOCK)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.proximity-sensor.cooldown", SettingType.INTEGER, Integer.class)
                .defaultValue(getConfigValue(plugin, "seeker-items.proximity-sensor.cooldown", 20))
                .range(0, 60)
                .description("Cooldown for proximity sensor in seconds")
                .customIcon(Material.CLOCK)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.proximity-sensor.range", SettingType.DOUBLE, Double.class)
                .defaultValue(getConfigValue(plugin, "seeker-items.proximity-sensor.range", 8.0))
                .rangeDouble(1.0, 50.0)
                .description("Detection range for proximity sensor")
                .customIcon(Material.SCULK_SENSOR)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.proximity-sensor.duration", SettingType.INTEGER, Integer.class)
                .defaultValue(getConfigValue(plugin, "seeker-items.proximity-sensor.duration", 60))
                .range(-1, 600)
                .description("Duration of proximity sensor in seconds (-1 = until round ends)")
                .customIcon(Material.CLOCK)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.proximity-sensor.fov-angle", SettingType.DOUBLE, Double.class)
                .defaultValue(getConfigValue(plugin, "seeker-items.proximity-sensor.fov-angle", 90.0))
                .rangeDouble(30.0, 360.0)
                .description("Field of view angle for wall-mounted sensors in degrees (360 = full circle)")
                .customIcon(Material.BOW)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.cage-trap.cooldown", SettingType.INTEGER, Integer.class)
                .defaultValue(getConfigValue(plugin, "seeker-items.cage-trap.cooldown", 20))
                .range(0, 60)
                .description("Cooldown for cage trap in seconds")
                .customIcon(Material.CLOCK)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.cage-trap.range", SettingType.DOUBLE, Double.class)
                .defaultValue(getConfigValue(plugin, "seeker-items.cage-trap.range", 3.0))
                .rangeDouble(1.0, 50.0)
                .description("Trigger range for cage trap")
                .customIcon(Material.IRON_BARS)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.cage-trap.duration", SettingType.INTEGER, Integer.class)
                .defaultValue(getConfigValue(plugin, "seeker-items.cage-trap.duration", -1))
                .range(-1, 600)
                .description("Duration of cage trap in seconds (-1 = until round ends)")
                .customIcon(Material.CLOCK)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.cage-trap.paralyze-duration", SettingType.INTEGER, Integer.class)
                .defaultValue(getConfigValue(plugin, "seeker-items.cage-trap.paralyze-duration", 5))
                .range(1, 60)
                .description("Duration of paralyze effect when trapped in seconds")
                .customIcon(Material.CLOCK)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.cage-trap.setup-time", SettingType.INTEGER, Integer.class)
                .defaultValue(getConfigValue(plugin, "seeker-items.cage-trap.setup-time", 5))
                .range(0, 60)
                .description("Time in seconds the cage trap takes to set up before it can trap a player")
                .customIcon(Material.CLOCK)
                .build());
        plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.sword-of-seeking.cooldown", SettingType.INTEGER, Integer.class)
                .defaultValue(getConfigValue(plugin, "seeker-items.cage-trap.setup-time", 5))
                .range(0, 60)
                .description("Cooldown for thrown seeker sword in seconds  ")
                .customIcon(Material.CLOCK)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.seeker-sword-throw.max-charge-seconds", SettingType.INTEGER, Integer.class)
                .defaultValue(getConfigValue(plugin, "seeker-items.seeker-sword-throw.max-charge-seconds", 5))
                .range(1, 15)
                .description("Maximum sword charge time in seconds")
                .customIcon(Material.CLOCK)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.seeker-sword-throw.min-speed", SettingType.DOUBLE, Double.class)
                .defaultValue(getConfigValue(plugin, "seeker-items.seeker-sword-throw.min-speed", 0.8))
                .rangeDouble(0.2, 5.0)
                .description("Thrown sword speed with minimum charge")
                .customIcon(Material.TRIDENT)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.seeker-sword-throw.max-speed", SettingType.DOUBLE, Double.class)
                .defaultValue(getConfigValue(plugin, "seeker-items.seeker-sword-throw.max-speed", 2.4))
                .rangeDouble(0.5, 8.0)
                .description("Thrown sword speed with full charge")
                .customIcon(Material.TRIDENT)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.seeker-sword-throw.gravity", SettingType.DOUBLE, Double.class)
                .defaultValue(getConfigValue(plugin, "seeker-items.seeker-sword-throw.gravity", 0.035))
                .rangeDouble(0.0, 0.2)
                .description("Per-tick gravity applied to the thrown sword")
                .customIcon(Material.FEATHER)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.seeker-sword-throw.hitbox", SettingType.DOUBLE, Double.class)
                .defaultValue(getConfigValue(plugin, "seeker-items.seeker-sword-throw.hitbox", 0.4))
                .rangeDouble(0.1, 1.5)
                .description("Collision radius used for hider hit detection")
                .customIcon(Material.TARGET)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.seeker-sword-throw.max-flight-seconds", SettingType.INTEGER, Integer.class)
                .defaultValue(getConfigValue(plugin, "seeker-items.seeker-sword-throw.max-flight-seconds", 6))
                .range(1, 30)
                .description("Maximum travel time before the thrown sword despawns")
                .customIcon(Material.CLOCK)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.seeker-sword-throw.stuck-seconds", SettingType.INTEGER, Integer.class)
                .defaultValue(getConfigValue(plugin, "seeker-items.seeker-sword-throw.stuck-seconds", 12))
                .range(1, 60)
                .description("How long the sword remains stuck in a block")
                .customIcon(Material.CLOCK)
                .build());


        plugin.getSettingRegistry().register(SettingDefinition.builder("loadout.hider-max-items", SettingType.INTEGER, Integer.class)
                .defaultValue(getConfigValue(plugin, "loadout.hider-max-items", 3))
                .range(0, 7)
                .description("Maximum number of items hiders can select in their loadout")
                .customIcon(Material.CHEST)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("loadout.seeker-max-items", SettingType.INTEGER, Integer.class)
                .defaultValue(getConfigValue(plugin, "loadout.seeker-max-items", 4))
                .range(0, 7)
                .description("Maximum number of items seekers can select in their loadout")
                .customIcon(Material.CHEST)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("loadout.hider-max-tokens", SettingType.INTEGER, Integer.class)
                .defaultValue(getConfigValue(plugin, "loadout.hider-max-tokens", 12))
                .range(1, 50)
                .description("Maximum tokens hiders can spend on items")
                .customIcon(Material.GOLD_BLOCK)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("loadout.seeker-max-tokens", SettingType.INTEGER, Integer.class)
                .defaultValue(getConfigValue(plugin, "loadout.seeker-max-tokens", 12))
                .range(1, 50)
                .description("Maximum tokens seekers can spend on items")
                .customIcon(Material.GOLD_BLOCK)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("loadout.token-cost-common", SettingType.INTEGER, Integer.class)
                .defaultValue(getConfigValue(plugin, "loadout.token-cost-common", 1))
                .range(1, 20)
                .description("Token cost for Common rarity items")
                .customIcon(Material.STONE)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("loadout.token-cost-uncommon", SettingType.INTEGER, Integer.class)
                .defaultValue(getConfigValue(plugin, "loadout.token-cost-uncommon", 2))
                .range(1, 20)
                .description("Token cost for Uncommon rarity items")
                .customIcon(Material.IRON_INGOT)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("loadout.token-cost-rare", SettingType.INTEGER, Integer.class)
                .defaultValue(getConfigValue(plugin, "loadout.token-cost-rare", 4))
                .range(1, 20)
                .description("Token cost for Rare rarity items")
                .customIcon(Material.DIAMOND)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("loadout.token-cost-epic", SettingType.INTEGER, Integer.class)
                .defaultValue(getConfigValue(plugin, "loadout.token-cost-epic", 6))
                .range(1, 20)
                .description("Token cost for Epic rarity items")
                .customIcon(Material.AMETHYST_CLUSTER)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("loadout.token-cost-legendary", SettingType.INTEGER, Integer.class)
                .defaultValue(getConfigValue(plugin, "loadout.token-cost-legendary", 10))
                .range(1, 30)
                .description("Token cost for Legendary rarity items")
                .customIcon(Material.NETHERITE_INGOT)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("points.tracking.interval-seconds", SettingType.INTEGER, Integer.class)
                .defaultValue(getConfigValue(plugin, "points.tracking.interval-seconds", 1))
                .range(1, 5)
                .description("Global update interval for dynamic point tracking")
                .customIcon(Material.CLOCK)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("points.hider.survival.amount", SettingType.INTEGER, Integer.class)
                .defaultValue(getConfigValue(plugin, "points.hider.survival.amount", 5))
                .range(0, 1000)
                .description("Points awarded per hider survival tick")
                .customIcon(Material.EMERALD)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("points.hider.survival.interval-seconds", SettingType.INTEGER, Integer.class)
                .defaultValue(getConfigValue(plugin, "points.hider.survival.interval-seconds", 20))
                .range(1, 300)
                .description("Seconds between hider survival tick awards")
                .customIcon(Material.CLOCK)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("points.hider.survival.start-delay-seconds", SettingType.INTEGER, Integer.class)
                .defaultValue(getConfigValue(plugin, "points.hider.survival.start-delay-seconds", 20))
                .range(0, 300)
                .description("Delay before first hider survival tick award")
                .customIcon(Material.CLOCK)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("points.hider.proximity.amount-per-second", SettingType.INTEGER, Integer.class)
                .defaultValue(getConfigValue(plugin, "points.hider.proximity.amount-per-second", 2))
                .range(0, 1000)
                .description("Points per second for hiders near seekers")
                .customIcon(Material.SCULK_SENSOR)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("points.hider.proximity.range", SettingType.DOUBLE, Double.class)
                .defaultValue(getConfigValue(plugin, "points.hider.proximity.range", 8.0))
                .rangeDouble(1.0, 64.0)
                .description("Range for hider proximity bonus")
                .customIcon(Material.BLAZE_POWDER)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("points.hider.near-miss.amount", SettingType.INTEGER, Integer.class)
                .defaultValue(getConfigValue(plugin, "points.hider.near-miss.amount", 50))
                .range(0, 1000)
                .description("Points for escaping a near miss")
                .customIcon(Material.TOTEM_OF_UNDYING)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("points.hider.near-miss.range", SettingType.DOUBLE, Double.class)
                .defaultValue(getConfigValue(plugin, "points.hider.near-miss.range", 3.0))
                .rangeDouble(0.5, 16.0)
                .description("Distance that counts as near-miss danger")
                .customIcon(Material.BLAZE_POWDER)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("points.hider.near-miss.escape-seconds", SettingType.INTEGER, Integer.class)
                .defaultValue(getConfigValue(plugin, "points.hider.near-miss.escape-seconds", 4))
                .range(1, 60)
                .description("Seconds to survive after leaving near-miss danger")
                .customIcon(Material.CLOCK)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("points.hider.taunt.small", SettingType.INTEGER, Integer.class)
                .defaultValue(getConfigValue(plugin, "points.hider.taunt.small", 25))
                .range(0, 1000)
                .description("Points for small hider taunts")
                .customIcon(Material.CAT_SPAWN_EGG)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("points.hider.taunt.large", SettingType.INTEGER, Integer.class)
                .defaultValue(getConfigValue(plugin, "points.hider.taunt.large", 75))
                .range(0, 2000)
                .description("Points for large hider taunts")
                .customIcon(Material.FIREWORK_ROCKET)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("points.hider.sharpshooter.amount", SettingType.INTEGER, Integer.class)
                .defaultValue(getConfigValue(plugin, "points.hider.sharpshooter.amount", 20))
                .range(0, 1000)
                .description("Points for each hider crossbow hit")
                .customIcon(Material.CROSSBOW)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("points.hider.survivor.amount", SettingType.INTEGER, Integer.class)
                .defaultValue(getConfigValue(plugin, "points.hider.survivor.amount", 100))
                .range(0, 2000)
                .description("Round-end points for surviving hiders")
                .customIcon(Material.SHIELD)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("points.hider.special.ghost", SettingType.INTEGER, Integer.class)
                .defaultValue(getConfigValue(plugin, "points.hider.special.ghost", 200))
                .range(0, 5000)
                .description("Special bonus for never being utility-spotted")
                .customIcon(Material.GHAST_TEAR)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("points.hider.special.distractor", SettingType.INTEGER, Integer.class)
                .defaultValue(getConfigValue(plugin, "points.hider.special.distractor", 200))
                .range(0, 5000)
                .description("Special bonus for most hider proximity time")
                .customIcon(Material.BELL)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("points.seeker.active-hunter.amount-per-second", SettingType.INTEGER, Integer.class)
                .defaultValue(getConfigValue(plugin, "points.seeker.active-hunter.amount-per-second", 2))
                .range(0, 1000)
                .description("Points per second for seekers near hiders")
                .customIcon(Material.IRON_SWORD)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("points.seeker.active-hunter.range", SettingType.DOUBLE, Double.class)
                .defaultValue(getConfigValue(plugin, "points.seeker.active-hunter.range", 16.0))
                .rangeDouble(1.0, 64.0)
                .description("Range for active hunter points")
                .customIcon(Material.BLAZE_POWDER)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("points.seeker.utility-success.amount", SettingType.INTEGER, Integer.class)
                .defaultValue(getConfigValue(plugin, "points.seeker.utility-success.amount", 40))
                .range(0, 2000)
                .description("Points for successful seeker utility usage")
                .customIcon(Material.ENDER_EYE)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("points.seeker.interception.amount", SettingType.INTEGER, Integer.class)
                .defaultValue(getConfigValue(plugin, "points.seeker.interception.amount", 15))
                .range(0, 1000)
                .description("Points for damaging a hider without killing")
                .customIcon(Material.IRON_SWORD)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("points.seeker.kill.amount", SettingType.INTEGER, Integer.class)
                .defaultValue(getConfigValue(plugin, "points.seeker.kill.amount", 300))
                .range(0, 5000)
                .description("Points for eliminating a hider")
                .customIcon(Material.NETHERITE_SWORD)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("points.seeker.assist.amount", SettingType.INTEGER, Integer.class)
                .defaultValue(getConfigValue(plugin, "points.seeker.assist.amount", 100))
                .range(0, 5000)
                .description("Points for assist on hider elimination")
                .customIcon(Material.CHAINMAIL_CHESTPLATE)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("points.seeker.assist.range", SettingType.DOUBLE, Double.class)
                .defaultValue(getConfigValue(plugin, "points.seeker.assist.range", 16.0))
                .rangeDouble(1.0, 64.0)
                .description("Assist range when another seeker gets the kill")
                .customIcon(Material.BLAZE_POWDER)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("points.seeker.special.bloodhound", SettingType.INTEGER, Integer.class)
                .defaultValue(getConfigValue(plugin, "points.seeker.special.bloodhound", 200))
                .range(0, 5000)
                .description("Special bonus for most captures")
                .customIcon(Material.WOLF_SPAWN_EGG)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("points.seeker.first-blood.amount", SettingType.INTEGER, Integer.class)
                .defaultValue(getConfigValue(plugin, "points.seeker.first-blood.amount", 100))
                .range(0, 5000)
                .description("Bonus for first kill of the round")
                .customIcon(Material.REDSTONE)
                .build());
    }

    private static final String[] DYE_NAMES = {
            "WHITE", "ORANGE", "MAGENTA", "LIGHT_BLUE", "YELLOW", "LIME", "PINK", "GRAY",
            "LIGHT_GRAY", "CYAN", "PURPLE", "BLUE", "BROWN", "GREEN", "RED", "BLACK"
    };

    private static final int[] DYE_RGB = {
            0xF9FFFE, 0xF9801D, 0xC74EBD, 0x3AB3DA, 0xFED83D, 0x80C71F, 0xF38BAA, 0x474F52,
            0x9D9D97, 0x169C9C, 0x8932B8, 0x3C44AA, 0x835432, 0x5E7C16, 0xB02E26, 0x1D1D21
    };

    private static ItemStack colorIconFromHex(Object value, String suffix, Material fallback) {
        return new ItemStack(nearestColorMaterial(value, suffix, fallback));
    }

    private static Material nearestColorMaterial(Object value, String suffix, Material fallback) {
        int rgb = parseHexColor(value);
        if (rgb < 0) {
            return fallback;
        }

        int bestIndex = 0;
        long bestDistance = Long.MAX_VALUE;

        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;

        for (int i = 0; i < DYE_RGB.length; i++) {
            int pr = (DYE_RGB[i] >> 16) & 0xFF;
            int pg = (DYE_RGB[i] >> 8) & 0xFF;
            int pb = DYE_RGB[i] & 0xFF;

            long dr = r - pr;
            long dg = g - pg;
            long db = b - pb;
            long distance = dr * dr + dg * dg + db * db;

            if (distance < bestDistance) {
                bestDistance = distance;
                bestIndex = i;
            }
        }

        String materialName = DYE_NAMES[bestIndex] + "_" + suffix;
        Material material = Material.matchMaterial(materialName);
        return material == null ? fallback : material;
    }

    private static int parseHexColor(Object value) {
        if (value == null) {
            return -1;
        }

        String input = String.valueOf(value).trim();
        if (input.isEmpty()) {
            return -1;
        }

        if (input.startsWith("#")) {
            input = input.substring(1);
        }

        input = input.toUpperCase(Locale.ROOT);
        if (!input.matches("[0-9A-F]{6}")) {
            return -1;
        }

        try {
            return Integer.parseInt(input, 16);
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private static ItemStack setEnchanted(Material material, boolean enchanted) {
        ItemStack itemStack = new ItemStack(material);
        ItemMeta meta = itemStack.getItemMeta();
        meta.setEnchantmentGlintOverride(enchanted);
        itemStack.setItemMeta(meta);
        return itemStack;
    }
}

