package de.thecoolcraft11.hideAndSeek.setting.group;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.model.GhostEssenceParticleMode;
import de.thecoolcraft11.hideAndSeek.model.SpeedBoostType;
import de.thecoolcraft11.hideAndSeek.setting.spec.*;
import de.thecoolcraft11.minigameframework.config.SettingDefinition;
import de.thecoolcraft11.minigameframework.config.SettingType;
import org.bukkit.Material;

import java.util.List;
import java.util.Map;

public final class HiderItemsSettingGroup implements SettingGroup {

    private static SettingSpec variantMultiplierSetting(String key, double defaultValue, String description) {
        return new DoubleSettingSpec(key, defaultValue, 0.1, 2.0, description, Material.COMPARATOR);
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
        return List.of(new IntegerSettingSpec("hider-items.random-block.uses", 5, 1, 20,
                        "Max uses for random block transform item", Material.COBBLESTONE),
                new IntegerSettingSpec("hider-items.random-block.cooldown", 10, 0, 60,
                        "Cooldown for random block item in seconds", Material.CLOCK),
                new IntegerSettingSpec("game.unstuck.cooldown", 30, 0, 180,
                        "Cooldown in seconds for /mg unstuck after normal unstuck teleports", Material.CLOCK),
                new IntegerSettingSpec("game.unstuck.spawn-cooldown", 90, 0, 300,
                        "Cooldown in seconds after spawn fallback is used", Material.RESPAWN_ANCHOR),
                new DoubleSettingSpec("game.unstuck.seeker-range", 15.0, 3.0, 40.0,
                        "Blocks around a hider/spawn where seekers block unstuck", Material.ENDER_EYE),
                new IntegerSettingSpec("game.unstuck.history-seconds", 12, 6, 30,
                        "How many seconds of movement history unstuck keeps", Material.WRITABLE_BOOK),
                new IntegerSettingSpec("game.unstuck.scan-radius", 3, 1, 8,
                        "Horizontal radius for nearby safe-ground scan", Material.COMPASS),
                new IntegerSettingSpec("game.unstuck.spawn-search-radius", 4, 1, 10,
                        "Horizontal radius for finding a safe fallback near spawn", Material.LODESTONE),
                new IntegerSettingSpec("game.unstuck.stationary-seconds", 4, 2, 12,
                        "How long a player must remain mostly stationary before spawn fallback", Material.BARRIER),
                new DoubleSettingSpec("game.unstuck.stationary-radius", 0.75, 0.1, 2.0,
                        "Movement tolerance used to consider a player stationary", Material.SLIME_BALL),
                new DoubleSettingSpec("game.unstuck.max-upward-gain", 1.0, 0.0, 3.0,
                        "Max upward gain before rollback also requires short horizontal distance", Material.LADDER),
                new DoubleSettingSpec("game.unstuck.max-horizontal-rollback", 4.0, 1.0, 12.0,
                        "Max rollback horizontal distance allowed for steep upward teleports", Material.STRING),
                new IntegerSettingSpec("game.unstuck.force-spawn-after-attempts", 3, 1, 8,
                        "After this many consecutive unstucks, force a spawn fallback to break stuck loops",
                        Material.RESPAWN_ANCHOR),
                new IntegerSettingSpec("hider-items.crossbow.hits-per-upgrade", 3, 1, 10,
                        "Hits needed to upgrade speed boost", Material.ARROW),
                new IntegerSettingSpec("hider-items.crossbow.cooldown", 6, 0, 60,
                        "Cooldown for hider crossbow in seconds", Material.CLOCK),
                new IntegerSettingSpec("hider-items.sound.cooldown", 4, 1, 30, "Cooldown for cat sound item in seconds",
                        Material.CLOCK), new DoubleSettingSpec("hider-items.sound.volume", 0.75, 0.1, 2.0,
                        "Volume of cat sound (0.1 = quiet, 2.0 = loud)", Material.JUKEBOX),
                new DoubleSettingSpec("hider-items.sound.pitch", 0.8, 0.5, 2.0,
                        "Pitch of cat sound (0.5 = low, 2.0 = high)", Material.NOTE_BLOCK),
                new IntegerSettingSpec("hider-items.sound.note-particles", 8, 1, 40,
                        "Base note particle amount for taunt sounds", Material.NOTE_BLOCK),
                new IntegerSettingSpec("hider-items.sound.accent-particles", 6, 1, 40,
                        "Base accent particle amount for taunt sounds", Material.GLOW_INK_SAC),
                variantMultiplierSetting("hider-items.sound.variants.default.volume-multiplier", 1.0,
                        "Default sound skin volume multiplier"),
                variantMultiplierSetting("hider-items.sound.variants.default.pitch-multiplier", 1.0,
                        "Default sound skin pitch multiplier"),
                variantMultiplierSetting("hider-items.sound.variants.default.particle-multiplier", 1.0,
                        "Default sound skin particle multiplier"),
                variantMultiplierSetting("hider-items.sound.variants.skin_megaphone.volume-multiplier", 0.9,
                        "Megaphone skin volume multiplier"),
                variantMultiplierSetting("hider-items.sound.variants.skin_megaphone.pitch-multiplier", 1.0,
                        "Megaphone skin pitch multiplier"),
                variantMultiplierSetting("hider-items.sound.variants.skin_megaphone.particle-multiplier", 1.0,
                        "Megaphone skin particle multiplier"),
                variantMultiplierSetting("hider-items.sound.variants.skin_rubber_chicken.volume-multiplier", 0.95,
                        "Rubber chicken skin volume multiplier"),
                variantMultiplierSetting("hider-items.sound.variants.skin_rubber_chicken.pitch-multiplier", 1.1,
                        "Rubber chicken skin pitch multiplier"),
                variantMultiplierSetting("hider-items.sound.variants.skin_rubber_chicken.particle-multiplier", 1.0,
                        "Rubber chicken skin particle multiplier"),
                new IntegerSettingSpec("hider-items.explosion.cooldown", 8, 0, 30,
                        "Cooldown for firecracker item in seconds", Material.CLOCK),
                new DoubleSettingSpec("hider-items.explosion.volume", 0.65, 0.1, 2.0, "Volume of explosion sound",
                        Material.JUKEBOX),
                new DoubleSettingSpec("hider-items.explosion.pitch", 1.5, 0.5, 2.0, "Pitch of explosion sound",
                        Material.NOTE_BLOCK), new IntegerSettingSpec("hider-items.explosion.smoke-particles", 3, 1, 20,
                        "Number of smoke particles per tick", Material.GUNPOWDER),
                new IntegerSettingSpec("hider-items.explosion.accent-particles", 2, 1, 20,
                        "Accent particle amount while fuse burns", Material.BLAZE_POWDER),
                new IntegerSettingSpec("hider-items.explosion.burst-particles", 14, 1, 50,
                        "Base burst particles when explosion taunt detonates", Material.FIREWORK_STAR),
                new IntegerSettingSpec("hider-items.explosion.fuse-time", 40, 10, 100,
                        "Fuse time in ticks before explosion (20 ticks = 1 second)", Material.CLOCK),
                variantMultiplierSetting("hider-items.explosion.variants.default.volume-multiplier", 1.0,
                        "Default explosion skin volume multiplier"),
                variantMultiplierSetting("hider-items.explosion.variants.default.pitch-multiplier", 1.0,
                        "Default explosion skin pitch multiplier"),
                variantMultiplierSetting("hider-items.explosion.variants.default.smoke-multiplier", 1.0,
                        "Default explosion skin smoke multiplier"),
                variantMultiplierSetting("hider-items.explosion.variants.default.burst-multiplier", 1.0,
                        "Default explosion skin burst multiplier"),
                variantMultiplierSetting("hider-items.explosion.variants.skin_confetti_popper.volume-multiplier", 0.95,
                        "Confetti skin volume multiplier"),
                variantMultiplierSetting("hider-items.explosion.variants.skin_confetti_popper.pitch-multiplier", 1.05,
                        "Confetti skin pitch multiplier"),
                variantMultiplierSetting("hider-items.explosion.variants.skin_confetti_popper.smoke-multiplier", 1.0,
                        "Confetti skin smoke multiplier"),
                variantMultiplierSetting("hider-items.explosion.variants.skin_confetti_popper.burst-multiplier", 1.05,
                        "Confetti skin burst multiplier"),
                variantMultiplierSetting("hider-items.explosion.variants.skin_bubble_popper.volume-multiplier", 0.9,
                        "Bubble skin volume multiplier"),
                variantMultiplierSetting("hider-items.explosion.variants.skin_bubble_popper.pitch-multiplier", 1.1,
                        "Bubble skin pitch multiplier"),
                variantMultiplierSetting("hider-items.explosion.variants.skin_bubble_popper.smoke-multiplier", 1.0,
                        "Bubble skin smoke multiplier"),
                variantMultiplierSetting("hider-items.explosion.variants.skin_bubble_popper.burst-multiplier", 1.05,
                        "Bubble skin burst multiplier"),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(
                        SettingDefinition.builder("hider-items.speed-boost.type", SettingType.ENUM,
                                SpeedBoostType.class).defaultValue(
                                getEnumConfigValue(plugin, "hider-items.speed-boost.type", SpeedBoostType.class,
                                        SpeedBoostType.SPEED_EFFECT)).description(
                                "Speed boost type: SPEED_EFFECT or VELOCITY_BOOST").customIcon(
                                Material.FEATHER).valueIcons(
                                Map.of(SpeedBoostType.SPEED_EFFECT, Material.POTION, SpeedBoostType.VELOCITY_BOOST,
                                        Material.FEATHER)).build()),
                new IntegerSettingSpec("hider-items.speed-boost.cooldown", 10, 0, 60,
                        "Cooldown for speed boost in seconds", Material.CLOCK),
                new IntegerSettingSpec("hider-items.speed-boost.duration", 5, 1, 30,
                        "Duration of speed effect in seconds (SPEED_EFFECT only)", Material.CLOCK),
                new DoubleSettingSpec("hider-items.speed-boost.boost-power", 0.5, 0.1, 2.0,
                        "Power of velocity boost (VELOCITY_BOOST only)", Material.FEATHER),
                new IntegerSettingSpec("hider-items.knockback-stick.level", 5, 0, 60,
                        "Knockback level for knockback stick", Material.ANVIL),
                new IntegerSettingSpec("hider-items.knockback-stick.cooldown", 5, 0, 60,
                        "Knockback level for knockback stick", Material.ANVIL),
                new IntegerSettingSpec("hider-items.block-swap.cooldown", 15, 0, 60,
                        "Cooldown for block swap in seconds", Material.CLOCK),
                new DoubleSettingSpec("hider-items.block-swap.range", 50.0, 5.0, 200.0,
                        "Maximum swap range for block swap", Material.BLAZE_POWDER),
                new IntegerSettingSpec("hider-items.big-firecracker.cooldown", 18, 0, 60,
                        "Cooldown for big firecracker in seconds", Material.CLOCK),
                new IntegerSettingSpec("hider-items.big-firecracker.fuse-time", 60, 10, 200,
                        "Fuse time in ticks before big explosion", Material.CLOCK),
                new IntegerSettingSpec("hider-items.big-firecracker.mini-fuse-time", 30, 5, 100,
                        "Fuse time in ticks for mini firecrackers", Material.CLOCK),
                new IntegerSettingSpec("hider-items.big-firecracker.mini-count", 3, 1, 10,
                        "Number of mini firecrackers", Material.FIREWORK_STAR),
                new DoubleSettingSpec("hider-items.big-firecracker.volume", 1.2, 0.1, 2.0,
                        "Explosion volume for big firecracker", Material.JUKEBOX),
                new DoubleSettingSpec("hider-items.big-firecracker.pitch", 0.5, 0.1, 2.0,
                        "Explosion pitch for big firecracker", Material.NOTE_BLOCK),
                new DoubleSettingSpec("hider-items.big-firecracker.mini-volume", 0.8, 0.1, 2.0,
                        "Volume for mini firecracker explosions", Material.JUKEBOX),
                new DoubleSettingSpec("hider-items.big-firecracker.mini-pitch", 1.2, 0.1, 2.0,
                        "Pitch for mini firecracker explosions", Material.NOTE_BLOCK),
                new IntegerSettingSpec("hider-items.big-firecracker.main-particles", 16, 1, 60,
                        "Base particle count for main big firecracker detonation", Material.FIREWORK_STAR),
                new IntegerSettingSpec("hider-items.big-firecracker.mini-particles", 8, 1, 40,
                        "Base particle count for mini firecracker detonations", Material.BLAZE_POWDER),
                new IntegerSettingSpec("hider-items.big-firecracker.spark-particles", 5, 1, 30,
                        "Spark particles shown while mini firecrackers are flying", Material.GLOWSTONE_DUST),
                variantMultiplierSetting("hider-items.big-firecracker.variants.default.volume-multiplier", 1.0,
                        "Default big firecracker volume multiplier"),
                variantMultiplierSetting("hider-items.big-firecracker.variants.default.pitch-multiplier", 1.0,
                        "Default big firecracker pitch multiplier"),
                variantMultiplierSetting("hider-items.big-firecracker.variants.default.main-particle-multiplier", 1.0,
                        "Default big firecracker main particle multiplier"),
                variantMultiplierSetting("hider-items.big-firecracker.variants.default.mini-particle-multiplier", 1.0,
                        "Default big firecracker mini particle multiplier"),
                variantMultiplierSetting("hider-items.big-firecracker.variants.skin_giant_present.volume-multiplier",
                        0.95, "Giant Present skin volume multiplier"),
                variantMultiplierSetting("hider-items.big-firecracker.variants.skin_giant_present.pitch-multiplier",
                        1.05, "Giant Present skin pitch multiplier"), variantMultiplierSetting(
                        "hider-items.big-firecracker.variants.skin_giant_present.main-particle-multiplier", 1.0,
                        "Giant Present skin main particle multiplier"), variantMultiplierSetting(
                        "hider-items.big-firecracker.variants.skin_giant_present.mini-particle-multiplier", 1.0,
                        "Giant Present skin mini particle multiplier"),
                variantMultiplierSetting("hider-items.big-firecracker.variants.skin_boombox.volume-multiplier", 0.95,
                        "Boombox skin volume multiplier"),
                variantMultiplierSetting("hider-items.big-firecracker.variants.skin_boombox.pitch-multiplier", 0.95,
                        "Boombox skin pitch multiplier"),
                variantMultiplierSetting("hider-items.big-firecracker.variants.skin_boombox.main-particle-multiplier",
                        1.0, "Boombox skin main particle multiplier"),
                variantMultiplierSetting("hider-items.big-firecracker.variants.skin_boombox.mini-particle-multiplier",
                        1.0, "Boombox skin mini particle multiplier"),
                new IntegerSettingSpec("hider-items.firework-rocket.cooldown", 30, 0, 60, "", Material.CLOCK),
                new IntegerSettingSpec("hider-items.firework-rocket.target-y", 128, -64, 320,
                        "Target Y for firework explosion", Material.LADDER),
                new DoubleSettingSpec("hider-items.firework-rocket.volume", 10.0, 0.1, 15.0,
                        "Explosion volume for firework rocket", Material.JUKEBOX),
                new IntegerSettingSpec("hider-items.medkit.cooldown", 30, 0, 120, "Cooldown for medkit in seconds",
                        Material.CLOCK), new IntegerSettingSpec("hider-items.medkit.channel-time", 5, 1, 30,
                        "Time to stand still before healing", Material.CLOCK),
                new DoubleSettingSpec("hider-items.medkit.heal-amount", 20.0, 1.0, 40.0, "Heal amount in half-hearts",
                        Material.GLISTERING_MELON_SLICE),
                new IntegerSettingSpec("hider-items.totem.effect-duration", 30, 5, 120,
                        "Duration of revive effect in seconds", Material.CLOCK),
                new IntegerSettingSpec("hider-items.totem.max-uses", 1, 1, 5, "Max uses per totem",
                        Material.TOTEM_OF_UNDYING),
                new IntegerSettingSpec("hider-items.invisibility-cloak.cooldown", 20, 0, 120,
                        "Cooldown for invisibility cloak in seconds", Material.CLOCK),
                new IntegerSettingSpec("hider-items.invisibility-cloak.duration", 8, 1, 30,
                        "Duration of invisibility in seconds", Material.CLOCK),
                new IntegerSettingSpec("hider-items.slowness-ball.cooldown", 10, 0, 60,
                        "Cooldown for slowness ball in seconds", Material.CLOCK),
                new IntegerSettingSpec("hider-items.slowness-ball.duration", 6, 1, 30,
                        "Duration of slowness effect in seconds", Material.CLOCK),
                new IntegerSettingSpec("hider-items.slowness-ball.amplifier", 1, 0, 10,
                        "Slowness effect amplifier (0 = slowness I, 1 = slowness II, etc)",
                        Material.FERMENTED_SPIDER_EYE),
                new IntegerSettingSpec("hider-items.smoke-bomb.cooldown", 15, 0, 60,
                        "Cooldown for smoke bomb in seconds", Material.CLOCK),
                new IntegerSettingSpec("hider-items.smoke-bomb.duration", 8, 1, 30,
                        "Duration of smoke cloud in seconds", Material.CLOCK),
                new IntegerSettingSpec("hider-items.smoke-bomb.radius", 4, 1, 15, "Radius of smoke cloud in blocks",
                        Material.BLAZE_POWDER), new IntegerSettingSpec("hider-items.remote-gateway.cooldown", 8, 0, 120,
                        "Cooldown for remote gateway placement in seconds", Material.CLOCK),
                new IntegerSettingSpec("hider-items.remote-gateway.max-pairs", 1, 1, 3,
                        "Maximum active remote gateway pairs per hider", Material.ENDER_EYE),
                new IntegerSettingSpec("hider-items.remote-gateway.duration-seconds", 120, -1, 600,
                        "How long remote gateway pairs remain active in seconds (-1 = until round ends)",
                        Material.CLOCK),
                new DoubleSettingSpec("hider-items.remote-gateway.travel-cooldown-seconds", 1.5, 0.0, 30.0,
                        "Cooldown in seconds before a player can use any gateway again", Material.CLOCK),
                new DoubleSettingSpec("hider-items.remote-gateway.portal-stand-seconds", 0.0, 0.0, 10.0,
                        "Seconds a player must stand inside a gateway before teleporting",
                        Material.HEAVY_WEIGHTED_PRESSURE_PLATE),
                new BooleanSettingSpec("hider-items.remote-gateway.seeker-can-use", false,
                        "Allow seekers to use remote gateways", Material.IRON_SWORD, false),
                new IntegerSettingSpec("hider-items.ghost-essence.cooldown", 25, 0, 300,
                        "Cooldown for ghost essence in seconds", Material.CLOCK),
                new IntegerSettingSpec("hider-items.ghost-essence.max-radius", 15, 1, 100,
                        "Maximum radius (in blocks) a ghost can move from their body", Material.COMPASS),
                new IntegerSettingSpec("hider-items.ghost-essence.min-light-block", 1, 0, 15,
                        "Minimum block light level required to materialize", Material.TORCH),
                new IntegerSettingSpec("hider-items.ghost-essence.min-light-sky", 1, 0, 15,
                        "Minimum sky light level required to materialize", Material.SUNFLOWER),
                new FloatSettingSpec("hider-items.ghost-essence.flying-speed", 0.01f, 0.001f, 1.0f,
                        "Client-side flying speed while ghostly", Material.FEATHER),
                new FloatSettingSpec("hider-items.ghost-essence.max-duration", 1.5f, 1.0f, 60.0f,
                        "Max ghost duration in seconds", Material.CLOCK),
                new FloatSettingSpec("hider-items.ghost-essence.boost-power", 1.5f, 0.0f, 5.0f,
                        "Initial boost power when activating ghost essence", Material.GHAST_TEAR),
                (plugin, resolver, iconHelper) -> plugin.getSettingRegistry().register(
                        SettingDefinition.builder("hider-items.ghost-essence.particle-mode", SettingType.ENUM,
                                GhostEssenceParticleMode.class).defaultValue(
                                getEnumConfigValue(plugin, "hider-items.ghost-essence.particle-mode",
                                        GhostEssenceParticleMode.class, GhostEssenceParticleMode.FLYING)).description(
                                "Particle effect mode for ghost essence").customIcon(Material.SOUL_TORCH).valueIcons(
                                Map.of(GhostEssenceParticleMode.FLYING, Material.FEATHER, GhostEssenceParticleMode.SNAP,
                                        Material.AMETHYST_SHARD, GhostEssenceParticleMode.NONE,
                                        Material.BARRIER)).build()));
    }
}
