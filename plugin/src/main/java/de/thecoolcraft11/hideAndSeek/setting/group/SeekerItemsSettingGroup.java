package de.thecoolcraft11.hideAndSeek.setting.group;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.setting.spec.SettingSpec;
import de.thecoolcraft11.minigameframework.config.SettingDefinition;
import de.thecoolcraft11.minigameframework.config.SettingType;
import org.bukkit.Material;

import java.util.List;

public final class SeekerItemsSettingGroup implements SettingGroup {

    @SuppressWarnings("unchecked")
    private static <T> T getConfigValue(HideAndSeek plugin, String path, T fallback) {
        Object value = plugin.getConfig().get("settings." + path);
        if (value == null) {
            return fallback;
        }
        return (T) value;
    }

    @Override
    public List<SettingSpec> settings() {
        return List.<SettingSpec>of(
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.grappling-hook.cooldown", SettingType.INTEGER, Integer.class)

                        .defaultValue(getConfigValue(plugin, "seeker-items.grappling-hook.cooldown", 2))
                        .range(0, 30)
                        .description("Cooldown for grappling hook in seconds")
                        .customIcon(Material.CLOCK)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.grappling-hook.speed", SettingType.DOUBLE, Double.class)
                        .defaultValue(getConfigValue(plugin, "seeker-items.grappling-hook.speed", 1.5))
                        .rangeDouble(0.3, 3.0)
                        .description("Base speed for grappling hook pull")
                        .customIcon(Material.BLAZE_POWDER)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.ink-splash.cooldown", SettingType.INTEGER, Integer.class)
                        .defaultValue(getConfigValue(plugin, "seeker-items.ink-splash.cooldown", 20))
                        .range(0, 60)
                        .description("Cooldown for ink splash in seconds")
                        .customIcon(Material.CLOCK)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.ink-splash.radius", SettingType.INTEGER, Integer.class)
                        .defaultValue(getConfigValue(plugin, "seeker-items.ink-splash.radius", 25))
                        .range(1, 50)
                        .description("Radius of ink splash")
                        .customIcon(Material.COMPASS)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.ink-splash.duration", SettingType.INTEGER, Integer.class)
                        .defaultValue(getConfigValue(plugin, "seeker-items.ink-splash.duration", 7))
                        .range(1, 30)
                        .description("Duration of ink blindness in seconds")
                        .customIcon(Material.CLOCK)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.lightning-freeze.cooldown", SettingType.INTEGER, Integer.class)
                        .defaultValue(getConfigValue(plugin, "seeker-items.lightning-freeze.cooldown", 60))
                        .range(10, 300)
                        .description("Cooldown for lightning freeze in seconds")
                        .customIcon(Material.CLOCK)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.lightning-freeze.duration", SettingType.INTEGER, Integer.class)
                        .defaultValue(getConfigValue(plugin, "seeker-items.lightning-freeze.duration", 5))
                        .range(1, 30)
                        .description("Duration of freeze in seconds")
                        .customIcon(Material.CLOCK)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.glowing-compass.cooldown", SettingType.INTEGER, Integer.class)
                        .defaultValue(getConfigValue(plugin, "seeker-items.glowing-compass.cooldown", 25))
                        .range(0, 60)
                        .description("Cooldown for glowing compass in seconds")
                        .customIcon(Material.CLOCK)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.glowing-compass.duration", SettingType.INTEGER, Integer.class)
                        .defaultValue(getConfigValue(plugin, "seeker-items.glowing-compass.duration", 10))
                        .range(1, 60)
                        .description("Duration of glow in seconds")
                        .customIcon(Material.CLOCK)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.glowing-compass.range", SettingType.DOUBLE, Double.class)
                        .defaultValue(getConfigValue(plugin, "seeker-items.glowing-compass.range", 50.0))
                        .rangeDouble(10.0, 200.0)
                        .description("Range to detect nearest hider")
                        .customIcon(Material.COMPASS)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.curse-spell.cooldown", SettingType.INTEGER, Integer.class)
                        .defaultValue(getConfigValue(plugin, "seeker-items.curse-spell.cooldown", 30))
                        .range(0, 60)
                        .description("Cooldown for curse spell in seconds")
                        .customIcon(Material.CLOCK)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.curse-spell.active-duration", SettingType.INTEGER, Integer.class)
                        .defaultValue(getConfigValue(plugin, "seeker-items.curse-spell.active-duration", 10))
                        .range(1, 60)
                        .description("Duration curse spell is active on sword")
                        .customIcon(Material.CLOCK)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.curse-spell.curse-duration", SettingType.INTEGER, Integer.class)
                        .defaultValue(getConfigValue(plugin, "seeker-items.curse-spell.curse-duration", 8))
                        .range(1, 60)
                        .description("Duration of curse on hider")
                        .customIcon(Material.CLOCK)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.curse-spell.small-shrink-delay", SettingType.INTEGER, Integer.class)
                        .defaultValue(getConfigValue(plugin, "seeker-items.curse-spell.small-shrink-delay", 8))
                        .range(1, 60)
                        .description("Delay before shrinking back in SMALL mode")
                        .customIcon(Material.CLOCK)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.block-randomizer.cooldown", SettingType.INTEGER, Integer.class)
                        .defaultValue(getConfigValue(plugin, "seeker-items.block-randomizer.cooldown", 45))
                        .range(10, 120)
                        .description("Cooldown for block randomizer in seconds")
                        .customIcon(Material.CLOCK)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.chain-pull.cooldown", SettingType.INTEGER, Integer.class)
                        .defaultValue(getConfigValue(plugin, "seeker-items.chain-pull.cooldown", 12))
                        .range(0, 60)
                        .description("Cooldown for chain pull in seconds")
                        .customIcon(Material.CLOCK)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.chain-pull.range", SettingType.DOUBLE, Double.class)
                        .defaultValue(getConfigValue(plugin, "seeker-items.chain-pull.range", 30.0))
                        .rangeDouble(5.0, 100.0)
                        .description("Maximum range for chain pull")
                        .customIcon(Material.LEAD)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.chain-pull.pull-power", SettingType.DOUBLE, Double.class)
                        .defaultValue(getConfigValue(plugin, "seeker-items.chain-pull.pull-power", 2.0))
                        .rangeDouble(0.5, 5.0)
                        .description("Pull power multiplier")
                        .customIcon(Material.IRON_BLOCK)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.chain-pull.slowness-duration", SettingType.INTEGER, Integer.class)
                        .defaultValue(getConfigValue(plugin, "seeker-items.chain-pull.slowness-duration", 3))
                        .range(1, 20)
                        .description("Slowness duration in seconds after pull")
                        .customIcon(Material.CLOCK)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.proximity-sensor.cooldown", SettingType.INTEGER, Integer.class)
                        .defaultValue(getConfigValue(plugin, "seeker-items.proximity-sensor.cooldown", 20))
                        .range(0, 60)
                        .description("Cooldown for proximity sensor in seconds")
                        .customIcon(Material.CLOCK)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.proximity-sensor.range", SettingType.DOUBLE, Double.class)
                        .defaultValue(getConfigValue(plugin, "seeker-items.proximity-sensor.range", 8.0))
                        .rangeDouble(1.0, 50.0)
                        .description("Detection range for proximity sensor")
                        .customIcon(Material.SCULK_SENSOR)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.proximity-sensor.duration", SettingType.INTEGER, Integer.class)
                        .defaultValue(getConfigValue(plugin, "seeker-items.proximity-sensor.duration", 60))
                        .range(-1, 600)
                        .description("Duration of proximity sensor in seconds (-1 = until round ends)")
                        .customIcon(Material.CLOCK)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.proximity-sensor.fov-angle", SettingType.DOUBLE, Double.class)
                        .defaultValue(getConfigValue(plugin, "seeker-items.proximity-sensor.fov-angle", 90.0))
                        .rangeDouble(30.0, 360.0)
                        .description("Field of view angle for wall-mounted sensors in degrees (360 = full circle)")
                        .customIcon(Material.BOW)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.cage-trap.cooldown", SettingType.INTEGER, Integer.class)
                        .defaultValue(getConfigValue(plugin, "seeker-items.cage-trap.cooldown", 20))
                        .range(0, 60)
                        .description("Cooldown for cage trap in seconds")
                        .customIcon(Material.CLOCK)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.cage-trap.range", SettingType.DOUBLE, Double.class)
                        .defaultValue(getConfigValue(plugin, "seeker-items.cage-trap.range", 3.0))
                        .rangeDouble(1.0, 50.0)
                        .description("Trigger range for cage trap")
                        .customIcon(Material.IRON_BARS)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.cage-trap.duration", SettingType.INTEGER, Integer.class)
                        .defaultValue(getConfigValue(plugin, "seeker-items.cage-trap.duration", -1))
                        .range(-1, 600)
                        .description("Duration of cage trap in seconds (-1 = until round ends)")
                        .customIcon(Material.CLOCK)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.cage-trap.paralyze-duration", SettingType.INTEGER, Integer.class)
                        .defaultValue(getConfigValue(plugin, "seeker-items.cage-trap.paralyze-duration", 5))
                        .range(1, 60)
                        .description("Duration of paralyze effect when trapped in seconds")
                        .customIcon(Material.CLOCK)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.cage-trap.setup-time", SettingType.INTEGER, Integer.class)
                        .defaultValue(getConfigValue(plugin, "seeker-items.cage-trap.setup-time", 5))
                        .range(0, 60)
                        .description("Time in seconds the cage trap takes to set up before it can trap a player")
                        .customIcon(Material.CLOCK)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.phantom-viewer.cooldown", SettingType.INTEGER, Integer.class)
                        .defaultValue(getConfigValue(plugin, "seeker-items.phantom-viewer.cooldown", 45))
                        .range(0, 180)
                        .description("Cooldown for phantom viewer in seconds")
                        .customIcon(Material.CLOCK)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.phantom-viewer.ray-distance", SettingType.INTEGER, Integer.class)
                        .defaultValue(getConfigValue(plugin, "seeker-items.phantom-viewer.ray-distance", 24))
                        .range(8, 64)
                        .description("Maximum render distance for phantom snapshot rays")
                        .customIcon(Material.COMPASS)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.phantom-viewer.show-player-name", SettingType.BOOLEAN, Boolean.class)
                        .defaultValue(getConfigValue(plugin, "seeker-items.phantom-viewer.show-player-name", false))
                        .description("Show the target hider name when phantom snapshot is captured")
                        .customIcon(Material.NAME_TAG)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.phantom-viewer.map-duration-seconds", SettingType.INTEGER, Integer.class)
                        .defaultValue(getConfigValue(plugin, "seeker-items.phantom-viewer.map-duration-seconds", 60))
                        .range(5, 300)
                        .description("How long phantom snapshot maps remain in player inventory")
                        .customIcon(Material.FILLED_MAP)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.phantom-viewer.target-random", SettingType.BOOLEAN, Boolean.class)
                        .defaultValue(getConfigValue(plugin, "seeker-items.phantom-viewer.target-random", true))
                        .description("Use random hider target instead of nearest hider")
                        .customIcon(Material.ENDER_EYE)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.phantom-viewer.apply-to-item-map-data", SettingType.BOOLEAN, Boolean.class)
                        .defaultValue(getConfigValue(plugin, "seeker-items.phantom-viewer.apply-to-item-map-data", false))
                        .description("Apply generated map data to the Phantom Viewer item instead of giving extra filled maps")
                        .customIcon(Material.FILLED_MAP)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.phantom-viewer.target-name-in-lore", SettingType.BOOLEAN, Boolean.class)
                        .defaultValue(getConfigValue(plugin, "seeker-items.phantom-viewer.target-name-in-lore", false))
                        .description("Add captured target name as lore on the Phantom Viewer item")
                        .customIcon(Material.NAME_TAG)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.assistant.cooldown", SettingType.INTEGER, Integer.class)
                        .defaultValue(getConfigValue(plugin, "seeker-items.assistant.cooldown", 120))
                        .range(0, 600)
                        .description("Cooldown for summoning an assistant in seconds")
                        .customIcon(Material.CLOCK)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.assistant.lifetime", SettingType.INTEGER, Integer.class)
                        .defaultValue(getConfigValue(plugin, "seeker-items.assistant.lifetime", 90))
                        .range(5, 600)
                        .description("How long each assistant stays active in seconds")
                        .customIcon(Material.CLOCK)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.assistant.max-per-seeker", SettingType.INTEGER, Integer.class)
                        .defaultValue(getConfigValue(plugin, "seeker-items.assistant.max-per-seeker", 2))
                        .range(1, 5)
                        .description("Maximum active assistants per seeker")
                        .customIcon(Material.ZOMBIE_HEAD)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.assistant.shoot-range", SettingType.DOUBLE, Double.class)
                        .defaultValue(getConfigValue(plugin, "seeker-items.assistant.shoot-range", 18.0))
                        .rangeDouble(2.0, 64.0)
                        .description("Range where an assistant can fire projectiles")
                        .customIcon(Material.BOW)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.assistant.shoot-cooldown", SettingType.INTEGER, Integer.class)
                        .defaultValue(getConfigValue(plugin, "seeker-items.assistant.shoot-cooldown", 70))
                        .range(1, 400)
                        .description("Ticks between assistant shots")
                        .customIcon(Material.CLOCK)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.assistant.projectile-speed", SettingType.DOUBLE, Double.class)
                        .defaultValue(getConfigValue(plugin, "seeker-items.assistant.projectile-speed", 0.3))
                        .rangeDouble(0.1, 3.0)
                        .description("Assistant projectile speed")
                        .customIcon(Material.SNOWBALL)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.assistant.max-hits", SettingType.INTEGER, Integer.class)
                        .defaultValue(getConfigValue(plugin, "seeker-items.assistant.max-hits", 2))
                        .range(1, 10)
                        .description("Projectile hits required to destroy an assistant")
                        .customIcon(Material.SHIELD)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.assistant.hit-direct-points", SettingType.INTEGER, Integer.class)
                        .defaultValue(getConfigValue(plugin, "seeker-items.assistant.hit-direct-points", 60))
                        .range(0, 500)
                        .description("Points awarded for a direct assistant projectile hit")
                        .customIcon(Material.EMERALD)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.assistant.hit-near-points", SettingType.INTEGER, Integer.class)
                        .defaultValue(getConfigValue(plugin, "seeker-items.assistant.hit-near-points", 20))
                        .range(0, 500)
                        .description("Points awarded for a near assistant projectile hit")
                        .customIcon(Material.EMERALD)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.assistant.projectile-aim-spread-stationary", SettingType.DOUBLE, Double.class)
                        .defaultValue(getConfigValue(plugin, "seeker-items.assistant.projectile-aim-spread-stationary", 0.06))
                        .rangeDouble(0.0, 0.5)
                        .description("Projectile spread for stationary targets (lower = more accurate)")
                        .customIcon(Material.SNOWBALL)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.assistant.projectile-aim-spread-moving", SettingType.DOUBLE, Double.class)
                        .defaultValue(getConfigValue(plugin, "seeker-items.assistant.projectile-aim-spread-moving", 0.22))
                        .rangeDouble(0.0, 0.5)
                        .description("Projectile spread for moving targets (higher = less accurate)")
                        .customIcon(Material.SNOWBALL)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.assistant.hit-direct-threshold-stationary", SettingType.DOUBLE, Double.class)
                        .defaultValue(getConfigValue(plugin, "seeker-items.assistant.hit-direct-threshold-stationary", 1.0))
                        .rangeDouble(0.5, 3.0)
                        .description("Distance threshold for direct hits on stationary targets")
                        .customIcon(Material.TARGET)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.assistant.hit-direct-threshold-moving", SettingType.DOUBLE, Double.class)
                        .defaultValue(getConfigValue(plugin, "seeker-items.assistant.hit-direct-threshold-moving", 0.35))
                        .rangeDouble(0.1, 1.5)
                        .description("Distance threshold for direct hits on moving targets")
                        .customIcon(Material.TARGET)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.assistant.hit-moving-speed-threshold", SettingType.DOUBLE, Double.class)
                        .defaultValue(getConfigValue(plugin, "seeker-items.assistant.hit-moving-speed-threshold", 0.05))
                        .rangeDouble(0.01, 0.5)
                        .description("Horizontal speed threshold to consider a target as moving")
                        .customIcon(Material.FEATHER)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.assistant.hit-moving-lucky-direct-chance", SettingType.DOUBLE, Double.class)
                        .defaultValue(getConfigValue(plugin, "seeker-items.assistant.hit-moving-lucky-direct-chance", 0.1))
                        .rangeDouble(0.0, 1.0)
                        .description("Chance (0.0-1.0) for moving targets to be lucky-hit as direct despite distance")
                        .customIcon(Material.EMERALD)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.assistant.health", SettingType.DOUBLE, Double.class)
                        .defaultValue(getConfigValue(plugin, "seeker-items.assistant.health", 6.0))
                        .rangeDouble(1.0, 50.0)
                        .description("Assistant mob max health")
                        .customIcon(Material.REDSTONE)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.assistant.speed", SettingType.DOUBLE, Double.class)
                        .defaultValue(getConfigValue(plugin, "seeker-items.assistant.speed", 0.38))
                        .rangeDouble(0.1, 1.0)
                        .description("Assistant mob movement speed multiplier")
                        .customIcon(Material.FEATHER)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.assistant.pathfind-speed-multiplier", SettingType.DOUBLE, Double.class)
                        .defaultValue(getConfigValue(plugin, "seeker-items.assistant.pathfind-speed-multiplier", 1.0))
                        .rangeDouble(0.5, 2.0)
                        .description("Pathfinding speed multiplier (relative to base speed)")
                        .customIcon(Material.IRON_BOOTS)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.assistant.standoff-range", SettingType.DOUBLE, Double.class)
                        .defaultValue(getConfigValue(plugin, "seeker-items.assistant.standoff-range", 6.0))
                        .rangeDouble(2.0, 20.0)
                        .description("Distance assistant maintains from target when shooting")
                        .customIcon(Material.BOW)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.assistant.standoff-tolerance", SettingType.DOUBLE, Double.class)
                        .defaultValue(getConfigValue(plugin, "seeker-items.assistant.standoff-tolerance", 1.5))
                        .rangeDouble(0.5, 5.0)
                        .description("Tolerance range for standoff distance")
                        .customIcon(Material.TARGET)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.assistant.sniff-interval", SettingType.INTEGER, Integer.class)
                        .defaultValue(getConfigValue(plugin, "seeker-items.assistant.sniff-interval", 15))
                        .range(1, 100)
                        .description("Ticks between target scans")
                        .customIcon(Material.CLOCK)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.assistant.sniff-range-front", SettingType.DOUBLE, Double.class)
                        .defaultValue(getConfigValue(plugin, "seeker-items.assistant.sniff-range-front", 35.0))
                        .rangeDouble(5.0, 100.0)
                        .description("Detection range in front of assistant")
                        .customIcon(Material.COMPASS)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.assistant.sniff-range-rear", SettingType.DOUBLE, Double.class)
                        .defaultValue(getConfigValue(plugin, "seeker-items.assistant.sniff-range-rear", 15.0))
                        .rangeDouble(2.0, 50.0)
                        .description("Detection range behind assistant")
                        .customIcon(Material.COMPASS)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.assistant.sniff-hidden-multiplier", SettingType.DOUBLE, Double.class)
                        .defaultValue(getConfigValue(plugin, "seeker-items.assistant.sniff-hidden-multiplier", 0.5))
                        .rangeDouble(0.1, 1.0)
                        .description("Range multiplier for hidden (block-mode) targets")
                        .customIcon(Material.DEEPSLATE_BRICKS)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.assistant.alert-range", SettingType.DOUBLE, Double.class)
                        .defaultValue(getConfigValue(plugin, "seeker-items.assistant.alert-range", 12.0))
                        .rangeDouble(2.0, 50.0)
                        .description("Range where assistant triggers an alert message")
                        .customIcon(Material.BELL)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.assistant.alert-cooldown", SettingType.INTEGER, Integer.class)
                        .defaultValue(getConfigValue(plugin, "seeker-items.assistant.alert-cooldown", 80))
                        .range(1, 400)
                        .description("Ticks between alert triggers")
                        .customIcon(Material.CLOCK)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.assistant.projectile-homing", SettingType.DOUBLE, Double.class)
                        .defaultValue(getConfigValue(plugin, "seeker-items.assistant.projectile-homing", 6.5))
                        .rangeDouble(0.0, 45.0)
                        .description("Projectile homing angle adjustment per tick in degrees")
                        .customIcon(Material.ARROW)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.assistant.projectile-homing-range", SettingType.DOUBLE, Double.class)
                        .defaultValue(getConfigValue(plugin, "seeker-items.assistant.projectile-homing-range", 18.0))
                        .rangeDouble(5.0, 100.0)
                        .description("Max distance for projectile homing to be active")
                        .customIcon(Material.ARROW)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.assistant.projectile-gravity", SettingType.DOUBLE, Double.class)
                        .defaultValue(getConfigValue(plugin, "seeker-items.assistant.projectile-gravity", 0.03))
                        .rangeDouble(0.0, 0.1)
                        .description("Per-tick gravity on projectile")
                        .customIcon(Material.FEATHER)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.assistant.projectile-lifetime", SettingType.INTEGER, Integer.class)
                        .defaultValue(getConfigValue(plugin, "seeker-items.assistant.projectile-lifetime", 70))
                        .range(10, 400)
                        .description("Projectile lifetime in ticks before despawn")
                        .customIcon(Material.CLOCK)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.assistant.projectile-aim-spread", SettingType.DOUBLE, Double.class)
                        .defaultValue(getConfigValue(plugin, "seeker-items.assistant.projectile-aim-spread", 0.12))
                        .rangeDouble(0.0, 0.5)
                        .description("Base projectile aim spread")
                        .customIcon(Material.SNOWBALL)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.assistant.hit-direct-threshold", SettingType.DOUBLE, Double.class)
                        .defaultValue(getConfigValue(plugin, "seeker-items.assistant.hit-direct-threshold", 0.6))
                        .rangeDouble(0.1, 3.0)
                        .description("Distance threshold for any direct hit")
                        .customIcon(Material.TARGET)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.assistant.hit-near-threshold", SettingType.DOUBLE, Double.class)
                        .defaultValue(getConfigValue(plugin, "seeker-items.assistant.hit-near-threshold", 2.5))
                        .rangeDouble(0.5, 10.0)
                        .description("Distance threshold for near hit")
                        .customIcon(Material.TARGET)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.assistant.hit-direct-slowness-duration", SettingType.INTEGER, Integer.class)
                        .defaultValue(getConfigValue(plugin, "seeker-items.assistant.hit-direct-slowness-duration", 120))
                        .range(1, 600)
                        .description("Slowness duration on direct hit (ticks)")
                        .customIcon(Material.REDSTONE)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.assistant.hit-direct-nausea-duration", SettingType.INTEGER, Integer.class)
                        .defaultValue(getConfigValue(plugin, "seeker-items.assistant.hit-direct-nausea-duration", 100))
                        .range(1, 600)
                        .description("Nausea duration on direct hit (ticks)")
                        .customIcon(Material.REDSTONE)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.assistant.hit-near-slowness-base", SettingType.INTEGER, Integer.class)
                        .defaultValue(getConfigValue(plugin, "seeker-items.assistant.hit-near-slowness-base", 80))
                        .range(1, 600)
                        .description("Base slowness duration on near hit (scales with distance)")
                        .customIcon(Material.REDSTONE)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.assistant.hit-near-nausea-base", SettingType.INTEGER, Integer.class)
                        .defaultValue(getConfigValue(plugin, "seeker-items.assistant.hit-near-nausea-base", 60))
                        .range(1, 600)
                        .description("Base nausea duration on near hit (scales with distance)")
                        .customIcon(Material.REDSTONE)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.assistant.beam-duration", SettingType.INTEGER, Integer.class)
                        .defaultValue(getConfigValue(plugin, "seeker-items.assistant.beam-duration", 120))
                        .range(10, 400)
                        .description("Duration of test block beam display (ticks)")
                        .customIcon(Material.CLOCK)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.assistant.wander-radius-phase1", SettingType.DOUBLE, Double.class)
                        .defaultValue(getConfigValue(plugin, "seeker-items.assistant.wander-radius-phase1", 15.0))
                        .rangeDouble(5.0, 100.0)
                        .description("Wander radius phase 1 (first 20 seconds)")
                        .customIcon(Material.COMPASS)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.assistant.wander-radius-phase2", SettingType.DOUBLE, Double.class)
                        .defaultValue(getConfigValue(plugin, "seeker-items.assistant.wander-radius-phase2", 25.0))
                        .rangeDouble(5.0, 100.0)
                        .description("Wander radius phase 2 (20-50 seconds)")
                        .customIcon(Material.COMPASS)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.assistant.wander-radius-phase3", SettingType.DOUBLE, Double.class)
                        .defaultValue(getConfigValue(plugin, "seeker-items.assistant.wander-radius-phase3", 40.0))
                        .rangeDouble(5.0, 100.0)
                        .description("Wander radius phase 3 (after 50 seconds)")
                        .customIcon(Material.COMPASS)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.sword-of-seeking.cooldown", SettingType.INTEGER, Integer.class)
                        .defaultValue(getConfigValue(plugin, "seeker-items.cage-trap.setup-time", 5))
                        .range(0, 60)
                        .description("Cooldown for thrown seeker sword in seconds  ")
                        .customIcon(Material.CLOCK)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.seeker-sword-throw.max-charge-seconds", SettingType.INTEGER, Integer.class)
                        .defaultValue(getConfigValue(plugin, "seeker-items.seeker-sword-throw.max-charge-seconds", 5))
                        .range(1, 15)
                        .description("Maximum sword charge time in seconds")
                        .customIcon(Material.CLOCK)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.seeker-sword-throw.min-speed", SettingType.DOUBLE, Double.class)
                        .defaultValue(getConfigValue(plugin, "seeker-items.seeker-sword-throw.min-speed", 0.8))
                        .rangeDouble(0.2, 5.0)
                        .description("Thrown sword speed with minimum charge")
                        .customIcon(Material.TRIDENT)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.seeker-sword-throw.max-speed", SettingType.DOUBLE, Double.class)
                        .defaultValue(getConfigValue(plugin, "seeker-items.seeker-sword-throw.max-speed", 2.4))
                        .rangeDouble(0.5, 8.0)
                        .description("Thrown sword speed with full charge")
                        .customIcon(Material.TRIDENT)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.seeker-sword-throw.gravity", SettingType.DOUBLE, Double.class)
                        .defaultValue(getConfigValue(plugin, "seeker-items.seeker-sword-throw.gravity", 0.035))
                        .rangeDouble(0.0, 0.2)
                        .description("Per-tick gravity applied to the thrown sword")
                        .customIcon(Material.FEATHER)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.seeker-sword-throw.hitbox", SettingType.DOUBLE, Double.class)
                        .defaultValue(getConfigValue(plugin, "seeker-items.seeker-sword-throw.hitbox", 0.4))
                        .rangeDouble(0.1, 1.5)
                        .description("Collision radius used for hider hit detection")
                        .customIcon(Material.TARGET)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.seeker-sword-throw.max-flight-seconds", SettingType.INTEGER, Integer.class)
                        .defaultValue(getConfigValue(plugin, "seeker-items.seeker-sword-throw.max-flight-seconds", 6))
                        .range(1, 30)
                        .description("Maximum travel time before the thrown sword despawns")
                        .customIcon(Material.CLOCK)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.seeker-sword-throw.stuck-seconds", SettingType.INTEGER, Integer.class)
                        .defaultValue(getConfigValue(plugin, "seeker-items.seeker-sword-throw.stuck-seconds", 12))
                        .range(1, 60)
                        .description("How long the sword remains stuck in a block")
                        .customIcon(Material.CLOCK)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.crowbar.enabled", SettingType.BOOLEAN, Boolean.class)
                        .defaultValue(getConfigValue(plugin, "seeker-items.crowbar.enabled", true))
                        .description("Enable or disable the crowbar item")
                        .customIcon(Material.IRON_PICKAXE)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.crowbar.cooldown", SettingType.INTEGER, Integer.class)
                        .defaultValue(getConfigValue(plugin, "seeker-items.crowbar.cooldown", 30))
                        .range(0, 60)
                        .description("Cooldown for crowbar block breaking in seconds")
                        .customIcon(Material.CLOCK)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.crowbar.breaking-ticks", SettingType.INTEGER, Integer.class)
                        .defaultValue(getConfigValue(plugin, "seeker-items.crowbar.breaking-ticks", 100))
                        .range(20, 200)
                        .description("Ticks it takes for the crowbar to break a block")
                        .customIcon(Material.CLOCK)
                        .build())
        );
    }
}
