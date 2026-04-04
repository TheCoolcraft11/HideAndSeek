package de.thecoolcraft11.hideAndSeek.setting.group;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.model.GhostEssenceParticleMode;
import de.thecoolcraft11.hideAndSeek.model.SpeedBoostType;
import de.thecoolcraft11.hideAndSeek.setting.spec.SettingSpec;
import de.thecoolcraft11.minigameframework.config.SettingDefinition;
import de.thecoolcraft11.minigameframework.config.SettingType;
import org.bukkit.Material;

import java.util.List;
import java.util.Map;

public final class HiderItemsSettingGroup implements SettingGroup {

    private static SettingSpec variantMultiplierSetting(String key, double defaultValue, String description) {
        return (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder(key, SettingType.DOUBLE, Double.class)
                .defaultValue(getConfigValue(plugin, key, defaultValue))
                .rangeDouble(0.1, 2.0)
                .description(description)
                .customIcon(Material.COMPARATOR)
                .build());
    }

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
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private static <E extends Enum<E>> E getEnumConfigValue(HideAndSeek plugin, String path, Class<E> enumClass, E fallback) {
        Object value = plugin.getConfig().get("settings." + path);
        if (value == null) {
            return fallback;
        }
        if (enumClass.isInstance(value)) {
            return enumClass.cast(value);
        }
        if (value instanceof String s) {
            try {
                return Enum.valueOf(enumClass, s.toUpperCase(java.util.Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    @Override
    public List<SettingSpec> settings() {
        return List.<SettingSpec>of(
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.random-block.uses", SettingType.INTEGER, Integer.class)
                        .defaultValue(getConfigValue(plugin, "hider-items.random-block.uses", 5))
                        .range(1, 20)
                        .description("Max uses for random block transform item")
                        .customIcon(Material.COBBLESTONE)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.crossbow.hits-per-upgrade", SettingType.INTEGER, Integer.class)
                        .defaultValue(getConfigValue(plugin, "hider-items.crossbow.hits-per-upgrade", 3))
                        .range(1, 10)
                        .description("Hits needed to upgrade speed boost")
                        .customIcon(Material.ARROW)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.sound.cooldown", SettingType.INTEGER, Integer.class)

                        .defaultValue(getConfigValue(plugin, "hider-items.sound.cooldown", 4))
                        .range(1, 30)
                        .description("Cooldown for cat sound item in seconds")
                        .customIcon(Material.CLOCK)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.sound.volume", SettingType.DOUBLE, Double.class)
                        .defaultValue(getConfigValue(plugin, "hider-items.sound.volume", 0.75))
                        .rangeDouble(0.1, 2.0)
                        .description("Volume of cat sound (0.1 = quiet, 2.0 = loud)")
                        .customIcon(Material.JUKEBOX)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.sound.pitch", SettingType.DOUBLE, Double.class)
                        .defaultValue(getConfigValue(plugin, "hider-items.sound.pitch", 0.8))
                        .rangeDouble(0.5, 2.0)
                        .description("Pitch of cat sound (0.5 = low, 2.0 = high)")
                        .customIcon(Material.NOTE_BLOCK)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.sound.note-particles", SettingType.INTEGER, Integer.class)
                        .defaultValue(getConfigValue(plugin, "hider-items.sound.note-particles", 8))
                        .range(1, 40)
                        .description("Base note particle amount for taunt sounds")
                        .customIcon(Material.NOTE_BLOCK)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.sound.accent-particles", SettingType.INTEGER, Integer.class)
                        .defaultValue(getConfigValue(plugin, "hider-items.sound.accent-particles", 6))
                        .range(1, 40)
                        .description("Base accent particle amount for taunt sounds")
                        .customIcon(Material.GLOW_INK_SAC)
                        .build()),
                variantMultiplierSetting("hider-items.sound.variants.default.volume-multiplier", 1.0, "Default sound skin volume multiplier"),
                variantMultiplierSetting("hider-items.sound.variants.default.pitch-multiplier", 1.0, "Default sound skin pitch multiplier"),
                variantMultiplierSetting("hider-items.sound.variants.default.particle-multiplier", 1.0, "Default sound skin particle multiplier"),
                variantMultiplierSetting("hider-items.sound.variants.skin_megaphone.volume-multiplier", 0.9, "Megaphone skin volume multiplier"),
                variantMultiplierSetting("hider-items.sound.variants.skin_megaphone.pitch-multiplier", 1.0, "Megaphone skin pitch multiplier"),
                variantMultiplierSetting("hider-items.sound.variants.skin_megaphone.particle-multiplier", 1.0, "Megaphone skin particle multiplier"),
                variantMultiplierSetting("hider-items.sound.variants.skin_rubber_chicken.volume-multiplier", 0.95, "Rubber chicken skin volume multiplier"),
                variantMultiplierSetting("hider-items.sound.variants.skin_rubber_chicken.pitch-multiplier", 1.1, "Rubber chicken skin pitch multiplier"),
                variantMultiplierSetting("hider-items.sound.variants.skin_rubber_chicken.particle-multiplier", 1.0, "Rubber chicken skin particle multiplier"),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.explosion.cooldown", SettingType.INTEGER, Integer.class)
                        .defaultValue(getConfigValue(plugin, "hider-items.explosion.cooldown", 8))
                        .range(0, 30)
                        .description("Cooldown for firecracker item in seconds")
                        .customIcon(Material.CLOCK)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.explosion.volume", SettingType.DOUBLE, Double.class)
                        .defaultValue(getConfigValue(plugin, "hider-items.explosion.volume", 0.65))
                        .rangeDouble(0.1, 2.0)
                        .description("Volume of explosion sound")
                        .customIcon(Material.JUKEBOX)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.explosion.pitch", SettingType.DOUBLE, Double.class)
                        .defaultValue(getConfigValue(plugin, "hider-items.explosion.pitch", 1.5))
                        .rangeDouble(0.5, 2.0)
                        .description("Pitch of explosion sound")
                        .customIcon(Material.NOTE_BLOCK)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.explosion.smoke-particles", SettingType.INTEGER, Integer.class)
                        .defaultValue(getConfigValue(plugin, "hider-items.explosion.smoke-particles", 3))
                        .range(1, 20)
                        .description("Number of smoke particles per tick")
                        .customIcon(Material.GUNPOWDER)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.explosion.accent-particles", SettingType.INTEGER, Integer.class)
                        .defaultValue(getConfigValue(plugin, "hider-items.explosion.accent-particles", 2))
                        .range(1, 20)
                        .description("Accent particle amount while fuse burns")
                        .customIcon(Material.BLAZE_POWDER)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.explosion.burst-particles", SettingType.INTEGER, Integer.class)
                        .defaultValue(getConfigValue(plugin, "hider-items.explosion.burst-particles", 14))
                        .range(1, 50)
                        .description("Base burst particles when explosion taunt detonates")
                        .customIcon(Material.FIREWORK_STAR)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.explosion.fuse-time", SettingType.INTEGER, Integer.class)
                        .defaultValue(getConfigValue(plugin, "hider-items.explosion.fuse-time", 40))
                        .range(10, 100)
                        .description("Fuse time in ticks before explosion (20 ticks = 1 second)")
                        .customIcon(Material.CLOCK)
                        .build()),
                variantMultiplierSetting("hider-items.explosion.variants.default.volume-multiplier", 1.0, "Default explosion skin volume multiplier"),
                variantMultiplierSetting("hider-items.explosion.variants.default.pitch-multiplier", 1.0, "Default explosion skin pitch multiplier"),
                variantMultiplierSetting("hider-items.explosion.variants.default.smoke-multiplier", 1.0, "Default explosion skin smoke multiplier"),
                variantMultiplierSetting("hider-items.explosion.variants.default.burst-multiplier", 1.0, "Default explosion skin burst multiplier"),
                variantMultiplierSetting("hider-items.explosion.variants.skin_confetti_popper.volume-multiplier", 0.95, "Confetti skin volume multiplier"),
                variantMultiplierSetting("hider-items.explosion.variants.skin_confetti_popper.pitch-multiplier", 1.05, "Confetti skin pitch multiplier"),
                variantMultiplierSetting("hider-items.explosion.variants.skin_confetti_popper.smoke-multiplier", 1.0, "Confetti skin smoke multiplier"),
                variantMultiplierSetting("hider-items.explosion.variants.skin_confetti_popper.burst-multiplier", 1.05, "Confetti skin burst multiplier"),
                variantMultiplierSetting("hider-items.explosion.variants.skin_bubble_popper.volume-multiplier", 0.9, "Bubble skin volume multiplier"),
                variantMultiplierSetting("hider-items.explosion.variants.skin_bubble_popper.pitch-multiplier", 1.1, "Bubble skin pitch multiplier"),
                variantMultiplierSetting("hider-items.explosion.variants.skin_bubble_popper.smoke-multiplier", 1.0, "Bubble skin smoke multiplier"),
                variantMultiplierSetting("hider-items.explosion.variants.skin_bubble_popper.burst-multiplier", 1.05, "Bubble skin burst multiplier"),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.speed-boost.type", SettingType.ENUM, SpeedBoostType.class)
                        .defaultValue(getEnumConfigValue(plugin, "hider-items.speed-boost.type", SpeedBoostType.class, SpeedBoostType.SPEED_EFFECT))
                        .description("Speed boost type: SPEED_EFFECT or VELOCITY_BOOST")
                        .customIcon(Material.FEATHER)
                        .valueIcons(Map.of(
                                SpeedBoostType.SPEED_EFFECT, Material.POTION,
                                SpeedBoostType.VELOCITY_BOOST, Material.FEATHER
                        ))
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.speed-boost.cooldown", SettingType.INTEGER, Integer.class)
                        .defaultValue(getConfigValue(plugin, "hider-items.speed-boost.cooldown", 10))
                        .range(0, 60)
                        .description("Cooldown for speed boost in seconds")
                        .customIcon(Material.CLOCK)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.speed-boost.duration", SettingType.INTEGER, Integer.class)
                        .defaultValue(getConfigValue(plugin, "hider-items.speed-boost.duration", 5))
                        .range(1, 30)
                        .description("Duration of speed effect in seconds (SPEED_EFFECT only)")
                        .customIcon(Material.CLOCK)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.speed-boost.boost-power", SettingType.DOUBLE, Double.class)
                        .defaultValue(getConfigValue(plugin, "hider-items.speed-boost.boost-power", 0.5))
                        .rangeDouble(0.1, 2.0)
                        .description("Power of velocity boost (VELOCITY_BOOST only)")
                        .customIcon(Material.FEATHER)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.knockback-stick.level", SettingType.INTEGER, Integer.class)
                        .defaultValue(getConfigValue(plugin, "hider-items.knockback-stick.level", 5))
                        .range(0, 60)
                        .description("Knockback level for knockback stick")
                        .customIcon(Material.ANVIL)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.block-swap.cooldown", SettingType.INTEGER, Integer.class)
                        .defaultValue(getConfigValue(plugin, "hider-items.block-swap.cooldown", 15))
                        .range(0, 60)
                        .description("Cooldown for block swap in seconds")
                        .customIcon(Material.CLOCK)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.block-swap.range", SettingType.DOUBLE, Double.class)
                        .defaultValue(getConfigValue(plugin, "hider-items.block-swap.range", 50.0))
                        .rangeDouble(5.0, 200.0)
                        .description("Maximum swap range for block swap")
                        .customIcon(Material.BLAZE_POWDER)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.big-firecracker.cooldown", SettingType.INTEGER, Integer.class)
                        .defaultValue(getConfigValue(plugin, "hider-items.big-firecracker.cooldown", 12))
                        .range(0, 60)
                        .description("Cooldown for big firecracker in seconds")
                        .customIcon(Material.CLOCK)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.big-firecracker.fuse-time", SettingType.INTEGER, Integer.class)
                        .defaultValue(getConfigValue(plugin, "hider-items.big-firecracker.fuse-time", 60))
                        .range(10, 200)
                        .description("Fuse time in ticks before big explosion")
                        .customIcon(Material.CLOCK)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.big-firecracker.mini-fuse-time", SettingType.INTEGER, Integer.class)
                        .defaultValue(getConfigValue(plugin, "hider-items.big-firecracker.mini-fuse-time", 30))
                        .range(5, 100)
                        .description("Fuse time in ticks for mini firecrackers")
                        .customIcon(Material.CLOCK)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.big-firecracker.mini-count", SettingType.INTEGER, Integer.class)
                        .defaultValue(getConfigValue(plugin, "hider-items.big-firecracker.mini-count", 3))
                        .range(1, 10)
                        .description("Number of mini firecrackers")
                        .customIcon(Material.FIREWORK_STAR)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.big-firecracker.volume", SettingType.DOUBLE, Double.class)
                        .defaultValue(getConfigValue(plugin, "hider-items.big-firecracker.volume", 1.2))
                        .rangeDouble(0.1, 2.0)
                        .description("Explosion volume for big firecracker")
                        .customIcon(Material.JUKEBOX)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.big-firecracker.pitch", SettingType.DOUBLE, Double.class)
                        .defaultValue(getConfigValue(plugin, "hider-items.big-firecracker.pitch", 0.5))
                        .rangeDouble(0.1, 2.0)
                        .description("Explosion pitch for big firecracker")
                        .customIcon(Material.NOTE_BLOCK)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.big-firecracker.mini-volume", SettingType.DOUBLE, Double.class)
                        .defaultValue(getConfigValue(plugin, "hider-items.big-firecracker.mini-volume", 0.8))
                        .rangeDouble(0.1, 2.0)
                        .description("Volume for mini firecracker explosions")
                        .customIcon(Material.JUKEBOX)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.big-firecracker.mini-pitch", SettingType.DOUBLE, Double.class)
                        .defaultValue(getConfigValue(plugin, "hider-items.big-firecracker.mini-pitch", 1.2))
                        .rangeDouble(0.1, 2.0)
                        .description("Pitch for mini firecracker explosions")
                        .customIcon(Material.NOTE_BLOCK)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.big-firecracker.main-particles", SettingType.INTEGER, Integer.class)
                        .defaultValue(getConfigValue(plugin, "hider-items.big-firecracker.main-particles", 16))
                        .range(1, 60)
                        .description("Base particle count for main big firecracker detonation")
                        .customIcon(Material.FIREWORK_STAR)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.big-firecracker.mini-particles", SettingType.INTEGER, Integer.class)
                        .defaultValue(getConfigValue(plugin, "hider-items.big-firecracker.mini-particles", 8))
                        .range(1, 40)
                        .description("Base particle count for mini firecracker detonations")
                        .customIcon(Material.BLAZE_POWDER)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.big-firecracker.spark-particles", SettingType.INTEGER, Integer.class)
                        .defaultValue(getConfigValue(plugin, "hider-items.big-firecracker.spark-particles", 5))
                        .range(1, 30)
                        .description("Spark particles shown while mini firecrackers are flying")
                        .customIcon(Material.GLOWSTONE_DUST)
                        .build()),
                variantMultiplierSetting("hider-items.big-firecracker.variants.default.volume-multiplier", 1.0, "Default big firecracker volume multiplier"),
                variantMultiplierSetting("hider-items.big-firecracker.variants.default.pitch-multiplier", 1.0, "Default big firecracker pitch multiplier"),
                variantMultiplierSetting("hider-items.big-firecracker.variants.default.main-particle-multiplier", 1.0, "Default big firecracker main particle multiplier"),
                variantMultiplierSetting("hider-items.big-firecracker.variants.default.mini-particle-multiplier", 1.0, "Default big firecracker mini particle multiplier"),
                variantMultiplierSetting("hider-items.big-firecracker.variants.skin_giant_present.volume-multiplier", 0.95, "Giant Present skin volume multiplier"),
                variantMultiplierSetting("hider-items.big-firecracker.variants.skin_giant_present.pitch-multiplier", 1.05, "Giant Present skin pitch multiplier"),
                variantMultiplierSetting("hider-items.big-firecracker.variants.skin_giant_present.main-particle-multiplier", 1.0, "Giant Present skin main particle multiplier"),
                variantMultiplierSetting("hider-items.big-firecracker.variants.skin_giant_present.mini-particle-multiplier", 1.0, "Giant Present skin mini particle multiplier"),
                variantMultiplierSetting("hider-items.big-firecracker.variants.skin_boombox.volume-multiplier", 0.95, "Boombox skin volume multiplier"),
                variantMultiplierSetting("hider-items.big-firecracker.variants.skin_boombox.pitch-multiplier", 0.95, "Boombox skin pitch multiplier"),
                variantMultiplierSetting("hider-items.big-firecracker.variants.skin_boombox.main-particle-multiplier", 1.0, "Boombox skin main particle multiplier"),
                variantMultiplierSetting("hider-items.big-firecracker.variants.skin_boombox.mini-particle-multiplier", 1.0, "Boombox skin mini particle multiplier"),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.firework-rocket.cooldown", SettingType.INTEGER, Integer.class)
                        .defaultValue(getConfigValue(plugin, "hider-items.firework-rocket.cooldown", 10))
                        .range(0, 60)
                        .customIcon(Material.CLOCK)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.firework-rocket.target-y", SettingType.INTEGER, Integer.class)
                        .defaultValue(getConfigValue(plugin, "hider-items.firework-rocket.target-y", 128))
                        .range(-64, 320)
                        .description("Target Y for firework explosion")
                        .customIcon(Material.LADDER)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.firework-rocket.volume", SettingType.DOUBLE, Double.class)
                        .defaultValue(getConfigValue(plugin, "hider-items.firework-rocket.volume", 10.0))
                        .rangeDouble(0.1, 15.0)
                        .description("Explosion volume for firework rocket")
                        .customIcon(Material.JUKEBOX)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.medkit.cooldown", SettingType.INTEGER, Integer.class)
                        .defaultValue(getConfigValue(plugin, "hider-items.medkit.cooldown", 30))
                        .range(0, 120)
                        .description("Cooldown for medkit in seconds")
                        .customIcon(Material.CLOCK)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.medkit.channel-time", SettingType.INTEGER, Integer.class)
                        .defaultValue(getConfigValue(plugin, "hider-items.medkit.channel-time", 5))
                        .range(1, 30)
                        .description("Time to stand still before healing")
                        .customIcon(Material.CLOCK)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.medkit.heal-amount", SettingType.DOUBLE, Double.class)
                        .defaultValue(getConfigValue(plugin, "hider-items.medkit.heal-amount", 20.0))
                        .rangeDouble(1.0, 40.0)
                        .description("Heal amount in half-hearts")
                        .customIcon(Material.GLISTERING_MELON_SLICE)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.totem.effect-duration", SettingType.INTEGER, Integer.class)
                        .defaultValue(getConfigValue(plugin, "hider-items.totem.effect-duration", 30))
                        .range(5, 120)
                        .description("Duration of revive effect in seconds")
                        .customIcon(Material.CLOCK)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.totem.max-uses", SettingType.INTEGER, Integer.class)
                        .defaultValue(getConfigValue(plugin, "hider-items.totem.max-uses", 1))
                        .range(1, 5)
                        .description("Max uses per totem")
                        .customIcon(Material.TOTEM_OF_UNDYING)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.invisibility-cloak.cooldown", SettingType.INTEGER, Integer.class)
                        .defaultValue(getConfigValue(plugin, "hider-items.invisibility-cloak.cooldown", 20))
                        .range(0, 120)
                        .description("Cooldown for invisibility cloak in seconds")
                        .customIcon(Material.CLOCK)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.invisibility-cloak.duration", SettingType.INTEGER, Integer.class)
                        .defaultValue(getConfigValue(plugin, "hider-items.invisibility-cloak.duration", 8))
                        .range(1, 30)
                        .description("Duration of invisibility in seconds")
                        .customIcon(Material.CLOCK)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.slowness-ball.cooldown", SettingType.INTEGER, Integer.class)
                        .defaultValue(getConfigValue(plugin, "hider-items.slowness-ball.cooldown", 10))
                        .range(0, 60)
                        .description("Cooldown for slowness ball in seconds")
                        .customIcon(Material.CLOCK)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.slowness-ball.duration", SettingType.INTEGER, Integer.class)
                        .defaultValue(getConfigValue(plugin, "hider-items.slowness-ball.duration", 6))
                        .range(1, 30)
                        .description("Duration of slowness effect in seconds")
                        .customIcon(Material.CLOCK)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.slowness-ball.amplifier", SettingType.INTEGER, Integer.class)
                        .defaultValue(getConfigValue(plugin, "hider-items.slowness-ball.amplifier", 1))
                        .range(0, 10)
                        .description("Slowness effect amplifier (0 = slowness I, 1 = slowness II, etc)")
                        .customIcon(Material.FERMENTED_SPIDER_EYE)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.smoke-bomb.cooldown", SettingType.INTEGER, Integer.class)
                        .defaultValue(getConfigValue(plugin, "hider-items.smoke-bomb.cooldown", 15))
                        .range(0, 60)
                        .description("Cooldown for smoke bomb in seconds")
                        .customIcon(Material.CLOCK)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.smoke-bomb.duration", SettingType.INTEGER, Integer.class)
                        .defaultValue(getConfigValue(plugin, "hider-items.smoke-bomb.duration", 8))
                        .range(1, 30)
                        .description("Duration of smoke cloud in seconds")
                        .customIcon(Material.CLOCK)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.smoke-bomb.radius", SettingType.INTEGER, Integer.class)
                        .defaultValue(getConfigValue(plugin, "hider-items.smoke-bomb.radius", 4))
                        .range(1, 15)
                        .description("Radius of smoke cloud in blocks")
                        .customIcon(Material.BLAZE_POWDER)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.ghost-essence.cooldown", SettingType.INTEGER, Integer.class)
                        .defaultValue(getConfigValue(plugin, "hider-items.ghost-essence.cooldown", 25))
                        .range(0, 300)
                        .description("Cooldown for ghost essence in seconds")
                        .customIcon(Material.CLOCK)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.ghost-essence.max-radius", SettingType.INTEGER, Integer.class)
                        .defaultValue(getConfigValue(plugin, "hider-items.ghost-essence.max-radius", 15))
                        .range(1, 100)
                        .description("Maximum radius (in blocks) a ghost can move from their body")
                        .customIcon(Material.COMPASS)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.ghost-essence.min-light-block", SettingType.INTEGER, Integer.class)
                        .defaultValue(getConfigValue(plugin, "hider-items.ghost-essence.min-light-block", 1))
                        .range(0, 15)
                        .description("Minimum block light level required to materialize")
                        .customIcon(Material.TORCH)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.ghost-essence.min-light-sky", SettingType.INTEGER, Integer.class)
                        .defaultValue(getConfigValue(plugin, "hider-items.ghost-essence.min-light-sky", 1))
                        .range(0, 15)
                        .description("Minimum sky light level required to materialize")
                        .customIcon(Material.SUNFLOWER)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.ghost-essence.flying-speed", SettingType.FLOAT, Float.class)
                        .defaultValue(getConfigValue(plugin, "hider-items.ghost-essence.flying-speed", 0.01f))
                        .rangeFloat(0.001f, 1.0f)
                        .description("Client-side flying speed while ghostly")
                        .customIcon(Material.FEATHER)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.ghost-essence.max-duration", SettingType.FLOAT, Float.class)
                        .defaultValue(getConfigValue(plugin, "hider-items.ghost-essence.max-duration", 1.5f))
                        .rangeFloat(1.0f, 60.0f)
                        .description("Max ghost duration in seconds")
                        .customIcon(Material.CLOCK)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.ghost-essence.boost-power", SettingType.FLOAT, Float.class)
                        .defaultValue(getConfigValue(plugin, "hider-items.ghost-essence.boost-power", 1.5f))
                        .rangeFloat(0.0f, 5.0f)
                        .description("Initial boost power when activating ghost essence")
                        .customIcon(Material.GHAST_TEAR)
                        .build()),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.ghost-essence.particle-mode", SettingType.ENUM, GhostEssenceParticleMode.class)
                        .defaultValue(getEnumConfigValue(plugin, "hider-items.ghost-essence.particle-mode", GhostEssenceParticleMode.class, GhostEssenceParticleMode.FLYING))
                        .description("Particle effect mode for ghost essence")
                        .customIcon(Material.SOUL_TORCH)
                        .valueIcons(Map.of(
                                GhostEssenceParticleMode.FLYING, Material.FEATHER,
                                GhostEssenceParticleMode.SNAP, Material.AMETHYST_SHARD,
                                GhostEssenceParticleMode.NONE, Material.BARRIER
                        ))
                        .build())
        );
    }
}
