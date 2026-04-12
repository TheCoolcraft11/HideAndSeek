package de.thecoolcraft11.hideAndSeek.setting.group;

import de.thecoolcraft11.hideAndSeek.model.SeekerKillModeEnum;
import de.thecoolcraft11.hideAndSeek.setting.spec.SettingSpec;
import de.thecoolcraft11.minigameframework.config.SettingDefinition;
import de.thecoolcraft11.minigameframework.config.SettingType;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;

public final class GameAdvancedSettingGroup implements SettingGroup {

    @Override
    public List<SettingSpec> settings() {
        return List.of(
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("game.block-form.scale-to-block", SettingType.BOOLEAN, Boolean.class)
                        .defaultValue(resolver.get(plugin, "game.block-form.scale-to-block", false))
                        .description("Scale hiders to the hidden block's height while hidden in BLOCK mode")
                        .customIcon(Material.SCAFFOLDING)
                        .valueIconStacks(Map.of(
                                true, iconHelper.enchanted(Material.SCAFFOLDING, true),
                                false, iconHelper.enchanted(Material.SCAFFOLDING, false)
                        ))
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("game.seekers.kill-mode", SettingType.ENUM, SeekerKillModeEnum.class)
                        .defaultValue(resolver.getEnum(plugin, "game.seekers.kill-mode", SeekerKillModeEnum.class, SeekerKillModeEnum.NORMAL))
                        .description("How seekers kill hiders")
                        .valueIcons(Map.of(
                                SeekerKillModeEnum.NORMAL, Material.IRON_SWORD,
                                SeekerKillModeEnum.ONE_HIT, Material.DIAMOND_SWORD,
                                SeekerKillModeEnum.GAZE_KILL, Material.ENDER_EYE
                        ))
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("game.seekers.gaze-kill.max-distance", SettingType.DOUBLE, Double.class)
                        .defaultValue(resolver.get(plugin, "game.seekers.gaze-kill.max-distance", 10.0))
                        .rangeDouble(5.0, 50.0)
                        .description("Maximum distance for gaze kill in blocks")
                        .customIcon(Material.SPYGLASS)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("game.seekers.gaze-kill.fov", SettingType.DOUBLE, Double.class)
                        .defaultValue(resolver.get(plugin, "game.seekers.gaze-kill.fov", 30.0))
                        .rangeDouble(10.0, 180.0)
                        .description("Field of view angle for gaze kill in degrees")
                        .customIcon(Material.BOW)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("game.seekers.gaze-kill.show-particles", SettingType.BOOLEAN, Boolean.class)
                        .defaultValue(resolver.get(plugin, "game.seekers.gaze-kill.show-particles", true))
                        .description("Show particles when looking at hiders during gaze kill mode")
                        .customIcon(Material.REDSTONE)
                        .valueIconStacks(Map.of(
                                true, iconHelper.enchanted(Material.REDSTONE, true),
                                false, iconHelper.enchanted(Material.REDSTONE, false)
                        ))
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("game.round.auto-cleanup", SettingType.BOOLEAN, Boolean.class)
                        .defaultValue(resolver.get(plugin, "game.round.auto-cleanup", true))
                        .description("Automatically teleport players to lobby and delete map after round")
                        .customIcon(ItemStack.of(Material.LAVA_BUCKET))
                        .valueIconStacks(Map.of(
                                true, iconHelper.enchanted(Material.LAVA_BUCKET, true),
                                false, iconHelper.enchanted(Material.BUCKET, false)
                        ))
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("game.small-mode.seeker-size", SettingType.DOUBLE, Double.class)
                        .defaultValue(resolver.get(plugin, "game.small-mode.seeker-size", 1.0))
                        .rangeDouble(0.1, 2.0)
                        .description("Size scale for seekers in SMALL mode (1.0 = normal size)")
                        .customIcon(Material.MAGMA_CREAM)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("game.blockstats.show-names", SettingType.BOOLEAN, Boolean.class)
                        .defaultValue(resolver.get(plugin, "game.blockstats.show-names", false))
                        .description("Show player names in Block Statistics GUI")
                        .customIcon(Material.NAME_TAG)
                        .valueIconStacks(Map.of(
                                true, iconHelper.enchanted(Material.NAME_TAG, true),
                                false, iconHelper.enchanted(Material.NAME_TAG, false)
                        ))
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("game.blockstats.enabled", SettingType.BOOLEAN, Boolean.class)
                        .defaultValue(resolver.get(plugin, "game.blockstats.enabled", true))
                        .description("If the Blockstats GUI should be enabled (Only relevant when playing in BLOCK Mode)")
                        .customIcon(Material.BOOK)
                        .valueIconStacks(Map.of(
                                true, iconHelper.enchanted(Material.BOOK, true),
                                false, iconHelper.enchanted(Material.BOOK, false)
                        ))
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("game.seeking-bossbar.enabled", SettingType.BOOLEAN, Boolean.class)
                        .defaultValue(resolver.get(plugin, "game.seeking-bossbar.enabled", true))
                        .description("Enable the seeking phase bossbar")
                        .customIcon(Material.MAGMA_CREAM)
                        .valueIconStacks(Map.of(
                                true, iconHelper.enchanted(Material.MAGMA_CREAM, true),
                                false, iconHelper.enchanted(Material.MAGMA_CREAM, false)
                        ))
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("game.seeking-bossbar.name-layout", SettingType.ENUM, de.thecoolcraft11.hideAndSeek.model.SeekingBossBarNameLayout.class)
                        .defaultValue(resolver.getEnum(plugin, "game.seeking-bossbar.name-layout", de.thecoolcraft11.hideAndSeek.model.SeekingBossBarNameLayout.class, de.thecoolcraft11.hideAndSeek.model.SeekingBossBarNameLayout.HIDERS_AND_SEEKERS))
                        .description("What to display in the bossbar title")
                        .customIcon(Material.NAME_TAG)
                        .valueIcons(Map.of(
                                de.thecoolcraft11.hideAndSeek.model.SeekingBossBarNameLayout.HIDERS_ONLY, Material.PLAYER_HEAD,
                                de.thecoolcraft11.hideAndSeek.model.SeekingBossBarNameLayout.SEEKERS_ONLY, Material.ENDER_EYE,
                                de.thecoolcraft11.hideAndSeek.model.SeekingBossBarNameLayout.HIDERS_AND_SEEKERS, Material.PAPER
                        ))
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("game.seeking-bossbar.progress-mode", SettingType.ENUM, de.thecoolcraft11.hideAndSeek.model.SeekingBossBarProgressMode.class)
                        .defaultValue(resolver.getEnum(plugin, "game.seeking-bossbar.progress-mode", de.thecoolcraft11.hideAndSeek.model.SeekingBossBarProgressMode.class, de.thecoolcraft11.hideAndSeek.model.SeekingBossBarProgressMode.PROGRESS))
                        .description("How the bossbar progress is displayed")
                        .customIcon(Material.EXPERIENCE_BOTTLE)
                        .valueIcons(Map.of(
                                de.thecoolcraft11.hideAndSeek.model.SeekingBossBarProgressMode.PROGRESS, Material.EMERALD,
                                de.thecoolcraft11.hideAndSeek.model.SeekingBossBarProgressMode.FULL, Material.GOLD_BLOCK
                        ))
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("game.seeking-bossbar.color.mode", SettingType.ENUM, de.thecoolcraft11.hideAndSeek.model.SeekingBossBarColorMode.class)
                        .defaultValue(resolver.getEnum(plugin, "game.seeking-bossbar.color.mode", de.thecoolcraft11.hideAndSeek.model.SeekingBossBarColorMode.class, de.thecoolcraft11.hideAndSeek.model.SeekingBossBarColorMode.DYNAMIC))
                        .description("Whether to use dynamic or static color for the bossbar")
                        .customIcon(Material.REDSTONE)
                        .valueIcons(Map.of(
                                de.thecoolcraft11.hideAndSeek.model.SeekingBossBarColorMode.DYNAMIC, Material.LAVA_BUCKET,
                                de.thecoolcraft11.hideAndSeek.model.SeekingBossBarColorMode.STATIC, Material.REDSTONE_BLOCK
                        ))
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("game.seeking-bossbar.color.static-color", SettingType.ENUM, de.thecoolcraft11.hideAndSeek.model.SeekingBossBarStaticColor.class)
                        .defaultValue(resolver.getEnum(plugin, "game.seeking-bossbar.color.static-color", de.thecoolcraft11.hideAndSeek.model.SeekingBossBarStaticColor.class, de.thecoolcraft11.hideAndSeek.model.SeekingBossBarStaticColor.GREEN))
                        .description("Static color when color mode is set to STATIC")
                        .customIcon(Material.LEATHER_BOOTS)
                        .valueIcons(Map.of(
                                de.thecoolcraft11.hideAndSeek.model.SeekingBossBarStaticColor.RED, Material.RED_WOOL,
                                de.thecoolcraft11.hideAndSeek.model.SeekingBossBarStaticColor.GREEN, Material.GREEN_WOOL,
                                de.thecoolcraft11.hideAndSeek.model.SeekingBossBarStaticColor.YELLOW, Material.YELLOW_WOOL,
                                de.thecoolcraft11.hideAndSeek.model.SeekingBossBarStaticColor.BLUE, Material.BLUE_WOOL,
                                de.thecoolcraft11.hideAndSeek.model.SeekingBossBarStaticColor.PURPLE, Material.PURPLE_WOOL,
                                de.thecoolcraft11.hideAndSeek.model.SeekingBossBarStaticColor.PINK, Material.PINK_WOOL,
                                de.thecoolcraft11.hideAndSeek.model.SeekingBossBarStaticColor.WHITE, Material.WHITE_WOOL
                        ))
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("game.seeking-bossbar.animation.enabled", SettingType.BOOLEAN, Boolean.class)
                        .defaultValue(resolver.get(plugin, "game.seeking-bossbar.animation.enabled", true))
                        .description("Enable animation when a hider is eliminated")
                        .customIcon(Material.CLOCK)
                        .valueIconStacks(Map.of(
                                true, iconHelper.enchanted(Material.CLOCK, true),
                                false, iconHelper.enchanted(Material.CLOCK, false)
                        ))
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("game.seeking-bossbar.animation.speed-ticks", SettingType.INTEGER, Integer.class)
                        .defaultValue(resolver.get(plugin, "game.seeking-bossbar.animation.speed-ticks", 3))
                        .range(1, 10)
                        .description("Speed of death animation in ticks (lower = faster)")
                        .customIcon(Material.REDSTONE)
                        .build())
        );
    }
}
