package de.thecoolcraft11.hideAndSeek.setting.group;

import de.thecoolcraft11.hideAndSeek.setting.spec.SettingSpec;
import de.thecoolcraft11.minigameframework.config.SettingDefinition;
import de.thecoolcraft11.minigameframework.config.SettingType;
import org.bukkit.Material;

import java.util.List;

public final class PointsSettingGroup implements SettingGroup {

    @Override
    public List<SettingSpec> settings() {
        return List.of(
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("points.tracking.interval-seconds", SettingType.INTEGER, Integer.class)
                        .defaultValue(resolver.get(plugin, "points.tracking.interval-seconds", 1))
                        .range(1, 5)
                        .description("Global update interval for dynamic point tracking")
                        .customIcon(Material.CLOCK)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("points.hider.survival.amount", SettingType.INTEGER, Integer.class)
                        .defaultValue(resolver.get(plugin, "points.hider.survival.amount", 5))
                        .range(0, 1000)
                        .description("Points awarded per hider survival tick")
                        .customIcon(Material.EMERALD)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("points.hider.survival.interval-seconds", SettingType.INTEGER, Integer.class)
                        .defaultValue(resolver.get(plugin, "points.hider.survival.interval-seconds", 20))
                        .range(1, 300)
                        .description("Seconds between hider survival tick awards")
                        .customIcon(Material.CLOCK)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("points.hider.survival.start-delay-seconds", SettingType.INTEGER, Integer.class)
                        .defaultValue(resolver.get(plugin, "points.hider.survival.start-delay-seconds", 20))
                        .range(0, 300)
                        .description("Delay before first hider survival tick award")
                        .customIcon(Material.CLOCK)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("points.hider.proximity.amount-per-second", SettingType.INTEGER, Integer.class)
                        .defaultValue(resolver.get(plugin, "points.hider.proximity.amount-per-second", 2))
                        .range(0, 1000)
                        .description("Points per second for hiders near seekers")
                        .customIcon(Material.SCULK_SENSOR)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("points.hider.proximity.range", SettingType.DOUBLE, Double.class)
                        .defaultValue(resolver.get(plugin, "points.hider.proximity.range", 8.0))
                        .rangeDouble(1.0, 64.0)
                        .description("Range for hider proximity bonus")
                        .customIcon(Material.BLAZE_POWDER)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("points.hider.near-miss.amount", SettingType.INTEGER, Integer.class)
                        .defaultValue(resolver.get(plugin, "points.hider.near-miss.amount", 50))
                        .range(0, 1000)
                        .description("Points for escaping a near miss")
                        .customIcon(Material.TOTEM_OF_UNDYING)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("points.hider.near-miss.range", SettingType.DOUBLE, Double.class)
                        .defaultValue(resolver.get(plugin, "points.hider.near-miss.range", 3.0))
                        .rangeDouble(0.5, 16.0)
                        .description("Distance that counts as near-miss danger")
                        .customIcon(Material.BLAZE_POWDER)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("points.hider.near-miss.escape-seconds", SettingType.INTEGER, Integer.class)
                        .defaultValue(resolver.get(plugin, "points.hider.near-miss.escape-seconds", 4))
                        .range(1, 60)
                        .description("Seconds to survive after leaving near-miss danger")
                        .customIcon(Material.CLOCK)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("points.hider.taunt.small", SettingType.INTEGER, Integer.class)
                        .defaultValue(resolver.get(plugin, "points.hider.taunt.small", 25))
                        .range(0, 1000)
                        .description("Points for small hider taunts")
                        .customIcon(Material.CAT_SPAWN_EGG)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("points.hider.taunt.large", SettingType.INTEGER, Integer.class)
                        .defaultValue(resolver.get(plugin, "points.hider.taunt.large", 75))
                        .range(0, 2000)
                        .description("Points for large hider taunts")
                        .customIcon(Material.FIREWORK_ROCKET)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("points.hider.sharpshooter.amount", SettingType.INTEGER, Integer.class)
                        .defaultValue(resolver.get(plugin, "points.hider.sharpshooter.amount", 20))
                        .range(0, 1000)
                        .description("Points for each hider crossbow hit")
                        .customIcon(Material.CROSSBOW)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("points.hider.survivor.amount", SettingType.INTEGER, Integer.class)
                        .defaultValue(resolver.get(plugin, "points.hider.survivor.amount", 100))
                        .range(0, 2000)
                        .description("Round-end points for surviving hiders")
                        .customIcon(Material.SHIELD)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("points.hider.special.ghost", SettingType.INTEGER, Integer.class)
                        .defaultValue(resolver.get(plugin, "points.hider.special.ghost", 200))
                        .range(0, 5000)
                        .description("Special bonus for never being utility-spotted")
                        .customIcon(Material.GHAST_TEAR)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("points.hider.special.distractor", SettingType.INTEGER, Integer.class)
                        .defaultValue(resolver.get(plugin, "points.hider.special.distractor", 200))
                        .range(0, 5000)
                        .description("Special bonus for most hider proximity time")
                        .customIcon(Material.BELL)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("points.seeker.active-hunter.amount-per-second", SettingType.INTEGER, Integer.class)
                        .defaultValue(resolver.get(plugin, "points.seeker.active-hunter.amount-per-second", 2))
                        .range(0, 1000)
                        .description("Points per second for seekers near hiders")
                        .customIcon(Material.IRON_SWORD)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("points.seeker.active-hunter.range", SettingType.DOUBLE, Double.class)
                        .defaultValue(resolver.get(plugin, "points.seeker.active-hunter.range", 16.0))
                        .rangeDouble(1.0, 64.0)
                        .description("Range for active hunter points")
                        .customIcon(Material.BLAZE_POWDER)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("points.seeker.utility-success.amount", SettingType.INTEGER, Integer.class)
                        .defaultValue(resolver.get(plugin, "points.seeker.utility-success.amount", 40))
                        .range(0, 2000)
                        .description("Points for successful seeker utility usage")
                        .customIcon(Material.ENDER_EYE)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("points.seeker.interception.amount", SettingType.INTEGER, Integer.class)
                        .defaultValue(resolver.get(plugin, "points.seeker.interception.amount", 15))
                        .range(0, 1000)
                        .description("Points for damaging a hider without killing")
                        .customIcon(Material.IRON_SWORD)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("points.seeker.kill.amount", SettingType.INTEGER, Integer.class)
                        .defaultValue(resolver.get(plugin, "points.seeker.kill.amount", 300))
                        .range(0, 5000)
                        .description("Points for eliminating a hider")
                        .customIcon(Material.NETHERITE_SWORD)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("points.seeker.assist.amount", SettingType.INTEGER, Integer.class)
                        .defaultValue(resolver.get(plugin, "points.seeker.assist.amount", 100))
                        .range(0, 5000)
                        .description("Points for assist on hider elimination")
                        .customIcon(Material.CHAINMAIL_CHESTPLATE)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("points.seeker.assist.range", SettingType.DOUBLE, Double.class)
                        .defaultValue(resolver.get(plugin, "points.seeker.assist.range", 16.0))
                        .rangeDouble(1.0, 64.0)
                        .description("Assist range when another seeker gets the kill")
                        .customIcon(Material.BLAZE_POWDER)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("points.seeker.special.bloodhound", SettingType.INTEGER, Integer.class)
                        .defaultValue(resolver.get(plugin, "points.seeker.special.bloodhound", 200))
                        .range(0, 5000)
                        .description("Special bonus for most captures")
                        .customIcon(Material.WOLF_SPAWN_EGG)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("points.seeker.first-blood.amount", SettingType.INTEGER, Integer.class)
                        .defaultValue(resolver.get(plugin, "points.seeker.first-blood.amount", 100))
                        .range(0, 5000)
                        .description("Bonus for first kill of the round")
                        .customIcon(Material.REDSTONE)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("points.seeker.environmental-elimination.amount", SettingType.INTEGER, Integer.class)
                        .defaultValue(resolver.get(plugin, "points.seeker.environmental-elimination.amount", 50))
                        .range(0, 5000)
                        .description("Points awarded to all seekers when hider dies to camping or world border")
                        .customIcon(Material.LIGHTNING_ROD)
                        .build())
        );
    }
}
