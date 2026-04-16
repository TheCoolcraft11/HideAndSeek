package de.thecoolcraft11.hideAndSeek.setting.group;

import de.thecoolcraft11.hideAndSeek.model.SeekerKillModeEnum;
import de.thecoolcraft11.hideAndSeek.setting.spec.*;
import de.thecoolcraft11.minigameframework.config.SettingDefinition;
import de.thecoolcraft11.minigameframework.config.SettingType;
import org.bukkit.Material;

import java.util.List;
import java.util.Map;

public final class GameAdvancedSettingGroup implements SettingGroup {

    @Override
    public List<SettingSpec> settings() {
        return List.of(new BooleanSettingSpec("game.block-form.scale-to-block", false,
                        "Scale hiders to the hidden block's height while hidden in BLOCK mode", Material.SCAFFOLDING, true),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(
                        SettingDefinition.builder("game.seekers.kill-mode", SettingType.ENUM,
                                SeekerKillModeEnum.class).defaultValue(
                                resolver.getEnum(plugin, "game.seekers.kill-mode", SeekerKillModeEnum.class,
                                        SeekerKillModeEnum.NORMAL)).description("How seekers kill hiders").valueIcons(
                                Map.of(SeekerKillModeEnum.NORMAL, Material.IRON_SWORD, SeekerKillModeEnum.ONE_HIT,
                                        Material.DIAMOND_SWORD, SeekerKillModeEnum.GAZE_KILL,
                                        Material.ENDER_EYE)).build()),
                new DoubleSettingSpec("game.seekers.gaze-kill.max-distance", 10.0, 5.0, 50.0,
                        "Maximum distance for gaze kill in blocks", Material.SPYGLASS),
                new DoubleSettingSpec("game.seekers.gaze-kill.fov", 30.0, 10.0, 180.0,
                        "Field of view angle for gaze kill in degrees", Material.BOW),
                new ListSettingSpec("disallowed-blockstates", List.of("waterlogged", "conditional"),
                        "Blockstate properties hidden in the block appearance GUI", Material.BARRIER),
                new ListSettingSpec("seeker-break-blocks",
                        List.of("SHORT_GRASS", "TALL_GRASS", "SEAGRASS", "TALL_SEAGRASS"),
                        "Global material rules seekers can break when no map override is set", Material.IRON_PICKAXE),
                new ListSettingSpec("block-interaction-exceptions",
                        List.of("*_DOOR", "*_FENCE_GATE", "*_TRAPDOOR", "*_BUTTON", "*_LEVER"),
                        "Global material rules exempt from interaction cancellation", Material.LEVER),
                new ListSettingSpec("block-physics-exceptions",
                        List.of("*_DOOR", "*_FENCE_GATE", "*_TRAPDOOR", "*_BUTTON", "*_LEVER"),
                        "Global material rules exempt from block-physics handling", Material.OBSERVER),
                new BooleanSettingSpec("game.apply-player-direction", true,
                        "Apply player look direction to placed blockstate in BLOCK mode", Material.COMPASS, true),
                new IntegerSettingSpec("game.max-air-above-liquid", 2, 0, 16,
                        "Maximum air blocks above water/lava that still allow hiding placement", Material.WATER_BUCKET),
                new BooleanSettingSpec("game.seekers.gaze-kill.show-particles", true,
                        "Show particles when looking at hiders during gaze kill mode", Material.REDSTONE, true),
                new BooleanSettingSpec("game.round.auto-cleanup", true,
                        "Automatically teleport players to lobby and delete map after round", Material.LAVA_BUCKET,
                        true), new DoubleSettingSpec("game.small-mode.seeker-size", 1.0, 0.1, 2.0,
                        "Size scale for seekers in SMALL mode (1.0 = normal size)", Material.MAGMA_CREAM),
                new BooleanSettingSpec("game.blockstats.show-names", false, "Show player names in Block Statistics GUI",
                        Material.NAME_TAG, true), new BooleanSettingSpec("game.blockstats.enabled", true,
                        "If the Blockstats GUI should be enabled (Only relevant when playing in BLOCK Mode)",
                        Material.BOOK, true),
                new BooleanSettingSpec("game.seeking-bossbar.enabled", true, "Enable the seeking phase bossbar",
                        Material.MAGMA_CREAM, true),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(
                        SettingDefinition.builder("game.seeking-bossbar.name-layout", SettingType.ENUM,
                                de.thecoolcraft11.hideAndSeek.model.SeekingBossBarNameLayout.class).defaultValue(
                                resolver.getEnum(plugin, "game.seeking-bossbar.name-layout",
                                        de.thecoolcraft11.hideAndSeek.model.SeekingBossBarNameLayout.class,
                                        de.thecoolcraft11.hideAndSeek.model.SeekingBossBarNameLayout.HIDERS_AND_SEEKERS)).description(
                                "What to display in the bossbar title").customIcon(Material.NAME_TAG).valueIcons(
                                Map.of(de.thecoolcraft11.hideAndSeek.model.SeekingBossBarNameLayout.HIDERS_ONLY,
                                        Material.PLAYER_HEAD,
                                        de.thecoolcraft11.hideAndSeek.model.SeekingBossBarNameLayout.SEEKERS_ONLY,
                                        Material.ENDER_EYE,
                                        de.thecoolcraft11.hideAndSeek.model.SeekingBossBarNameLayout.HIDERS_AND_SEEKERS,
                                        Material.PAPER)).build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(
                        SettingDefinition.builder("game.seeking-bossbar.progress-mode", SettingType.ENUM,
                                de.thecoolcraft11.hideAndSeek.model.SeekingBossBarProgressMode.class).defaultValue(
                                resolver.getEnum(plugin, "game.seeking-bossbar.progress-mode",
                                        de.thecoolcraft11.hideAndSeek.model.SeekingBossBarProgressMode.class,
                                        de.thecoolcraft11.hideAndSeek.model.SeekingBossBarProgressMode.PROGRESS)).description(
                                "How the bossbar progress is displayed").customIcon(
                                Material.EXPERIENCE_BOTTLE).valueIcons(
                                Map.of(de.thecoolcraft11.hideAndSeek.model.SeekingBossBarProgressMode.PROGRESS,
                                        Material.EMERALD,
                                        de.thecoolcraft11.hideAndSeek.model.SeekingBossBarProgressMode.FULL,
                                        Material.GOLD_BLOCK)).build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(
                        SettingDefinition.builder("game.seeking-bossbar.color.mode", SettingType.ENUM,
                                de.thecoolcraft11.hideAndSeek.model.SeekingBossBarColorMode.class).defaultValue(
                                resolver.getEnum(plugin, "game.seeking-bossbar.color.mode",
                                        de.thecoolcraft11.hideAndSeek.model.SeekingBossBarColorMode.class,
                                        de.thecoolcraft11.hideAndSeek.model.SeekingBossBarColorMode.DYNAMIC)).description(
                                "Whether to use dynamic or static color for the bossbar").customIcon(
                                Material.REDSTONE).valueIcons(
                                Map.of(de.thecoolcraft11.hideAndSeek.model.SeekingBossBarColorMode.DYNAMIC,
                                        Material.LAVA_BUCKET,
                                        de.thecoolcraft11.hideAndSeek.model.SeekingBossBarColorMode.STATIC,
                                        Material.REDSTONE_BLOCK)).build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(
                        SettingDefinition.builder("game.seeking-bossbar.color.static-color", SettingType.ENUM,
                                de.thecoolcraft11.hideAndSeek.model.SeekingBossBarStaticColor.class).defaultValue(
                                resolver.getEnum(plugin, "game.seeking-bossbar.color.static-color",
                                        de.thecoolcraft11.hideAndSeek.model.SeekingBossBarStaticColor.class,
                                        de.thecoolcraft11.hideAndSeek.model.SeekingBossBarStaticColor.GREEN)).description(
                                "Static color when color mode is set to STATIC").customIcon(
                                Material.LEATHER_BOOTS).valueIcons(
                                Map.of(de.thecoolcraft11.hideAndSeek.model.SeekingBossBarStaticColor.RED,
                                        Material.RED_WOOL,
                                        de.thecoolcraft11.hideAndSeek.model.SeekingBossBarStaticColor.GREEN,
                                        Material.GREEN_WOOL,
                                        de.thecoolcraft11.hideAndSeek.model.SeekingBossBarStaticColor.YELLOW,
                                        Material.YELLOW_WOOL,
                                        de.thecoolcraft11.hideAndSeek.model.SeekingBossBarStaticColor.BLUE,
                                        Material.BLUE_WOOL,
                                        de.thecoolcraft11.hideAndSeek.model.SeekingBossBarStaticColor.PURPLE,
                                        Material.PURPLE_WOOL,
                                        de.thecoolcraft11.hideAndSeek.model.SeekingBossBarStaticColor.PINK,
                                        Material.PINK_WOOL,
                                        de.thecoolcraft11.hideAndSeek.model.SeekingBossBarStaticColor.WHITE,
                                        Material.WHITE_WOOL)).build()),
                new BooleanSettingSpec("game.seeking-bossbar.animation.enabled", true,
                        "Enable animation when a hider is eliminated", Material.CLOCK, true),
                new IntegerSettingSpec("game.seeking-bossbar.animation.speed-ticks", 3, 1, 10,
                        "Speed of death animation in ticks (lower = faster)", Material.REDSTONE));
    }
}
