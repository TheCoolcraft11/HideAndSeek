package de.thecoolcraft11.hideAndSeek.setting.group;

import de.thecoolcraft11.hideAndSeek.setting.spec.SettingSpec;
import de.thecoolcraft11.minigameframework.config.SettingDefinition;
import de.thecoolcraft11.minigameframework.config.SettingType;
import org.bukkit.Material;

import java.util.List;
import java.util.Map;

public final class AnticheatSettingGroup implements SettingGroup {

    @Override
    public List<SettingSpec> settings() {
        return List.of(
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("anticheat.enabled", SettingType.BOOLEAN, Boolean.class)
                        .defaultValue(resolver.get(plugin, "anticheat.enabled", true))
                        .description("Master switch for anti-cheat seeker visibility filtering")
                        .customIcon(Material.SHIELD)
                        .valueIconStacks(Map.of(
                                true, iconHelper.enchanted(Material.SHIELD, true),
                                false, iconHelper.enchanted(Material.SHIELD, false)
                        ))
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("anticheat.hiding.filter.enabled", SettingType.BOOLEAN, Boolean.class)
                        .defaultValue(resolver.get(plugin, "anticheat.hiding.filter.enabled", true))
                        .description("During HIDING: hide all hider entities from seekers while keeping tab entries")
                        .customIcon(Material.ENDER_EYE)
                        .valueIconStacks(Map.of(
                                true, iconHelper.enchanted(Material.ENDER_EYE, true),
                                false, iconHelper.enchanted(Material.ENDER_EYE, false)
                        ))
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("anticheat.seeking.filter.enabled", SettingType.BOOLEAN, Boolean.class)
                        .defaultValue(resolver.get(plugin, "anticheat.seeking.filter.enabled", true))
                        .description("During SEEKING: seekers only see nearby hiders and never hidden BLOCK-mode hiders")
                        .customIcon(Material.SPYGLASS)
                        .valueIconStacks(Map.of(
                                true, iconHelper.enchanted(Material.SPYGLASS, true),
                                false, iconHelper.enchanted(Material.SPYGLASS, false)
                        ))
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("anticheat.seeking.visibility-range", SettingType.DOUBLE, Double.class)
                        .defaultValue(resolver.get(plugin, "anticheat.seeking.visibility-range", 12.0))
                        .rangeDouble(1.0, 128.0)
                        .description("Distance in blocks at which seekers can see hiders during SEEKING")
                        .customIcon(Material.COMPASS)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("anticheat.seeking.line-of-sight.enabled", SettingType.BOOLEAN, Boolean.class)
                        .defaultValue(resolver.get(plugin, "anticheat.seeking.line-of-sight.enabled", true))
                        .description("Allow seeker line-of-sight reveal outside base anti-cheat visibility range")
                        .customIcon(Material.SPYGLASS)
                        .valueIconStacks(Map.of(
                                true, iconHelper.enchanted(Material.SPYGLASS, true),
                                false, iconHelper.enchanted(Material.SPYGLASS, false)
                        ))
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("anticheat.seeking.line-of-sight.range", SettingType.DOUBLE, Double.class)
                        .defaultValue(resolver.get(plugin, "anticheat.seeking.line-of-sight.range", 64.0))
                        .rangeDouble(8.0, 256.0)
                        .description("Maximum range for line-of-sight reveal checks")
                        .customIcon(Material.COMPASS)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("anticheat.seeking.line-of-sight.fov", SettingType.DOUBLE, Double.class)
                        .defaultValue(resolver.get(plugin, "anticheat.seeking.line-of-sight.fov", 60.0))
                        .rangeDouble(5.0, 90.0)
                        .description("Seeker view angle in degrees for line-of-sight reveal checks")
                        .customIcon(Material.ENDER_EYE)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("anticheat.hider-camping.enabled", SettingType.BOOLEAN, Boolean.class)
                        .defaultValue(resolver.get(plugin, "anticheat.hider-camping.enabled", true))
                        .description("Whether to prevent hiders from camping in the same spot")
                        .customIcon(Material.CAMPFIRE)
                        .valueIconStacks(Map.of(
                                true, iconHelper.enchanted(Material.CAMPFIRE, true),
                                false, iconHelper.enchanted(Material.CAMPFIRE, false)
                        ))
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("anticheat.hider-camping.max-duration", SettingType.INTEGER, Integer.class)
                        .defaultValue(resolver.get(plugin, "anticheat.hider-camping.max-duration", 90))
                        .range(5, 600)
                        .description("How long a hider can stay in the same spot before being punished for camping")
                        .customIcon(Material.CLOCK)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("anticheat.hider-camping.warn-time", SettingType.INTEGER, Integer.class)
                        .defaultValue(resolver.get(plugin, "anticheat.hider-camping.warn-time", 15))
                        .range(0, 300)
                        .description("Time in seconds before camping punishment when a warning is issued to the hider")
                        .customIcon(Material.CLOCK)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("anticheat.hider-camping.spot-radius", SettingType.DOUBLE, Double.class)
                        .defaultValue(resolver.get(plugin, "anticheat.hider-camping.spot-radius", 2.5))
                        .rangeDouble(0.25, 8.0)
                        .description("Horizontal radius (blocks) treated as the same camping spot")
                        .customIcon(Material.TARGET)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("anticheat.hider-camping.damage-amount", SettingType.DOUBLE, Double.class)
                        .defaultValue(resolver.get(plugin, "anticheat.hider-camping.damage-amount", 1.0))
                        .rangeDouble(0.5, 10.0)
                        .description("Damage dealt each punishment tick while the hider keeps camping")
                        .customIcon(Material.IRON_SWORD)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("anticheat.hider-camping.damage-cooldown-ticks", SettingType.INTEGER, Integer.class)
                        .defaultValue(resolver.get(plugin, "anticheat.hider-camping.damage-cooldown-ticks", 20))
                        .range(1, 200)
                        .description("Ticks between repeated camping punishment damage")
                        .customIcon(Material.REPEATER)
                        .build())
        );
    }
}
