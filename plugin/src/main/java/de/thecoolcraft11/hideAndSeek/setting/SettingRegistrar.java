package de.thecoolcraft11.hideAndSeek.setting;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.setting.group.*;
import de.thecoolcraft11.minigameframework.config.SectionDefinition;
import org.bukkit.Material;

import java.util.List;

public class SettingRegistrar {

    public static void registerAll(HideAndSeek plugin) {
        registerSections(plugin);
        registerConfig(plugin);
        registerSettings(plugin);
    }

    public static void registerConfig(HideAndSeek plugin) {

        plugin.getConfigRegistry().register("maps", List.class, List.of("map1"));

        plugin.getConfigRegistry().register("disallowed-blockstates", List.class, List.of("waterlogged", "conditional"));

        plugin.getConfigRegistry().register("seeker-break-blocks", List.class, List.of("SHORT_GRASS", "TALL_GRASS", "SEAGRASS", "TALL_SEAGRASS"));

        plugin.getConfigRegistry().register("block-interaction-exceptions", List.class, List.of("*_DOOR", "*_FENCE_GATE", "*_TRAPDOOR", "*_BUTTON", "*_LEVER"));

        plugin.getConfigRegistry().register("block-physics-exceptions", List.class, List.of("*_DOOR", "*_FENCE_GATE", "*_TRAPDOOR", "*_BUTTON", "*_LEVER"));

        plugin.getConfigRegistry().register("game.apply-player-direction", Boolean.class, true);

        plugin.getConfigRegistry().register("game.max-air-above-liquid", Integer.class, 2);

        plugin.getConfigRegistry().register("nms.enabled", Boolean.class, true);

        plugin.getConfigRegistry().register("persistence.save-skin-data", Boolean.class, true);

        plugin.getConfigRegistry().register("persistence.save-loadout-data", Boolean.class, true);


        plugin.getConfigRegistry().register("settings.game.mode", String.class, "NORMAL");

        plugin.getConfigRegistry().register("settings.game.style", String.class, "SPECTATOR");

        plugin.getConfigRegistry().register("settings.game.hiding-time", Integer.class, 60);

        plugin.getConfigRegistry().register("settings.game.seeking-time", Integer.class, 300);

        plugin.getConfigRegistry().register("settings.game.hider-invisibility", Boolean.class, false);

        plugin.getConfigRegistry().register("settings.game.world-border.damage-hiders-outside", Boolean.class, true);

        plugin.getConfigRegistry().register("settings.game.world-border.damage-delay-seconds", Integer.class, 10);

        plugin.getConfigRegistry().register("settings.game.world-border.damage-amount", Double.class, 2.0);

        plugin.getConfigRegistry().register("settings.game.world-border.damage-cooldown-ticks", Integer.class, 20);

        plugin.getConfigRegistry().register("settings.game.small-mode.hider-size", Double.class, 0.5);

        plugin.getConfigRegistry().register("settings.game.team-distribution.random", Boolean.class, true);

        plugin.getConfigRegistry().register("settings.game.voting.game-mode-enabled", Boolean.class, true);

        plugin.getConfigRegistry().register("settings.game.voting.map-enabled", Boolean.class, true);

        plugin.getConfigRegistry().register("settings.game.voting.show-counts", Boolean.class, true);

        plugin.getConfigRegistry().register("settings.game.lobby.readiness-enabled", Boolean.class, true);

        plugin.getConfigRegistry().register("settings.game.maps.use-preferred-modes", Boolean.class, true);

        plugin.getConfigRegistry().register("settings.game.maps.use-map-timings", Boolean.class, true);

        plugin.getConfigRegistry().register("settings.game.maps.use-map-seeker-count", Boolean.class, true);

        plugin.getConfigRegistry().register("settings.game.maps.use-map-player-limits", Boolean.class, true);

        plugin.getConfigRegistry().register("settings.game.maps.use-map-setting-overrides", Boolean.class, true);

        plugin.getConfigRegistry().register("settings.game.maps.show-round-start-map-info-title", Boolean.class, true);

        plugin.getConfigRegistry().register("settings.game.maps.round-start-map-info-display-mode", String.class, "NAME_AUTHOR_DESCRIPTION");

        plugin.getConfigRegistry().register("settings.game.teams.fixed-seeker-team", String.class, "");

        plugin.getConfigRegistry().register("settings.game.teams.seeker-count", Integer.class, 1);

        plugin.getConfigRegistry().register("settings.game.hiders.health", Integer.class, 20);

        plugin.getConfigRegistry().register("settings.game.block-form.view-height", Float.class, 0.1f);

        plugin.getConfigRegistry().register("settings.game.block-form.scale-to-block", Boolean.class, false);

        plugin.getConfigRegistry().register("settings.game.seekers.kill-mode", String.class, "NORMAL");

        plugin.getConfigRegistry().register("settings.game.seekers.gaze-kill.max-distance", Double.class, 10.0);

        plugin.getConfigRegistry().register("settings.game.seekers.gaze-kill.fov", Double.class, 30.0);

        plugin.getConfigRegistry().register("settings.game.seekers.gaze-kill.show-particles", Boolean.class, true);

        plugin.getConfigRegistry().register("settings.game.round.auto-cleanup", Boolean.class, true);

        plugin.getConfigRegistry().register("settings.game.small-mode.seeker-size", Double.class, 1.0);

        plugin.getConfigRegistry().register("settings.perks.enabled", Boolean.class, true);

        plugin.getConfigRegistry().register("settings.perks.shop-mode", String.class, "INVENTORY");

        plugin.getConfigRegistry().register("settings.perks.perks-per-round", Integer.class, 3);

        plugin.getConfigRegistry().register("settings.perks.inventory-slots", List.class, List.of(9, 10, 11, 12, 13, 14, 15, 16, 17));

        plugin.getConfigRegistry().register("settings.perks.open-during-hiding", Boolean.class, false);

        plugin.getConfigRegistry().register("settings.perks.hider-finite-player-limit", Integer.class, 1);

        plugin.getConfigRegistry().register("settings.perks.seeker-finite-player-limit", Integer.class, 0);

        plugin.getConfigRegistry().register("settings.perks.finite-player-limit", Integer.class, 1);

        plugin.getConfigRegistry().register("settings.perks.refund-hider-perks-on-convert", Boolean.class, false);

        plugin.getConfigRegistry().register("settings.perks.perk.global.map-picker.view-height", Integer.class, 350);


        plugin.getConfigRegistry().register("settings.perks.perk.hider_adaptive_speed.enabled", Boolean.class, true);

        plugin.getConfigRegistry().register("settings.perks.perk.hider_adaptive_speed.cost", Integer.class, 80);

        plugin.getConfigRegistry().register("settings.perks.perk.hider_adaptive_speed.cooldown-ticks", Long.class, 0L);

        plugin.getConfigRegistry().register("settings.perks.perk.hider_adaptive_speed.hp-threshold", Double.class, 0.5d);

        plugin.getConfigRegistry().register("settings.perks.perk.hider_adaptive_speed.speed-duration-ticks", Integer.class, 100);

        plugin.getConfigRegistry().register("settings.perks.perk.hider_adaptive_speed.trigger-cooldown-ticks", Long.class, 300L);

        plugin.getConfigRegistry().register("settings.perks.perk.hider_seeker_warning.enabled", Boolean.class, true);

        plugin.getConfigRegistry().register("settings.perks.perk.hider_seeker_warning.cost", Integer.class, 140);

        plugin.getConfigRegistry().register("settings.perks.perk.hider_seeker_warning.cooldown-ticks", Long.class, 0L);

        plugin.getConfigRegistry().register("settings.perks.perk.hider_seeker_warning.range", Double.class, 12.0d);

        plugin.getConfigRegistry().register("settings.perks.perk.hider_seeker_warning.fov", Double.class, 45.0d);

        plugin.getConfigRegistry().register("settings.perks.perk.hider_seeker_warning.movement-range", Double.class, 8.0d);

        plugin.getConfigRegistry().register("settings.perks.perk.hider_seeker_warning.trigger-cooldown-ticks", Long.class, 60L);

        plugin.getConfigRegistry().register("settings.perks.perk.hider_extra_life.enabled", Boolean.class, true);

        plugin.getConfigRegistry().register("settings.perks.perk.hider_extra_life.cost", Integer.class, 200);

        plugin.getConfigRegistry().register("settings.perks.perk.hider_extra_life.cooldown-ticks", Long.class, 0L);

        plugin.getConfigRegistry().register("settings.perks.perk.hider_extra_life.points-per-heart", Integer.class, 100);

        plugin.getConfigRegistry().register("settings.perks.perk.hider_extra_life.max-hearts", Integer.class, 5);

        plugin.getConfigRegistry().register("settings.perks.perk.hider_shadow_step.enabled", Boolean.class, true);

        plugin.getConfigRegistry().register("settings.perks.perk.hider_shadow_step.cost", Integer.class, 100);

        plugin.getConfigRegistry().register("settings.perks.perk.hider_shadow_step.cooldown-ticks", Long.class, 0L);

        plugin.getConfigRegistry().register("settings.perks.perk.hider_shadow_step.hp-trigger", Double.class, 5.0d);

        plugin.getConfigRegistry().register("settings.perks.perk.hider_shadow_step.charge-ticks", Long.class, 30L);

        plugin.getConfigRegistry().register("settings.perks.perk.hider_shadow_step.teleport-range", Double.class, 15.0d);

        plugin.getConfigRegistry().register("settings.perks.perk.hider_shadow_step.min-seeker-distance", Double.class, 5.0d);

        plugin.getConfigRegistry().register("settings.perks.perk.hider_camouflage.enabled", Boolean.class, true);

        plugin.getConfigRegistry().register("settings.perks.perk.hider_camouflage.cost", Integer.class, 80);

        plugin.getConfigRegistry().register("settings.perks.perk.hider_camouflage.cooldown-ticks", Long.class, 0L);

        plugin.getConfigRegistry().register("settings.perks.perk.hider_camouflage.re-cleanse-interval-ticks", Long.class, 100L);

        plugin.getConfigRegistry().register("settings.perks.perk.hider_double_jump.enabled", Boolean.class, true);

        plugin.getConfigRegistry().register("settings.perks.perk.hider_double_jump.cost", Integer.class, 120);

        plugin.getConfigRegistry().register("settings.perks.perk.hider_double_jump.cooldown-ticks", Long.class, 0L);

        plugin.getConfigRegistry().register("settings.perks.perk.hider_double_jump.jump-power", Double.class, 0.7d);

        plugin.getConfigRegistry().register("settings.perks.perk.hider_double_jump.horizontal-boost", Double.class, 0.1d);

        plugin.getConfigRegistry().register("settings.perks.perk.hider_trap_sense.enabled", Boolean.class, true);

        plugin.getConfigRegistry().register("settings.perks.perk.hider_trap_sense.cost", Integer.class, 60);

        plugin.getConfigRegistry().register("settings.perks.perk.hider_trap_sense.cooldown-ticks", Long.class, 0L);

        plugin.getConfigRegistry().register("settings.perks.perk.hider_trap_sense.indicator-range", Double.class, 30.0d);

        plugin.getConfigRegistry().register("settings.perks.perk.hider_trap_sense.glow-range", Double.class, 20.0d);

        plugin.getConfigRegistry().register("settings.perks.perk.hider_trap_sense.warn-range", Double.class, 6.0d);

        plugin.getConfigRegistry().register("settings.perks.perk.hider_trap_sense.warn-cooldown-ticks", Long.class, 40L);


        plugin.getConfigRegistry().register("settings.perks.perk.seeker_death_zone.enabled", Boolean.class, true);

        plugin.getConfigRegistry().register("settings.perks.perk.seeker_death_zone.cost", Integer.class, 350);

        plugin.getConfigRegistry().register("settings.perks.perk.seeker_death_zone.purchase-cooldown-ticks", Long.class, 3600L);

        plugin.getConfigRegistry().register("settings.perks.perk.seeker_death_zone.escape-seconds", Integer.class, 60);

        plugin.getConfigRegistry().register("settings.perks.perk.seeker_death_zone.finite", Boolean.class, false);

        plugin.getConfigRegistry().register("settings.perks.perk.seeker_random_swap.enabled", Boolean.class, true);

        plugin.getConfigRegistry().register("settings.perks.perk.seeker_random_swap.cost", Integer.class, 250);

        plugin.getConfigRegistry().register("settings.perks.perk.seeker_random_swap.purchase-cooldown-ticks", Long.class, 400L);

        plugin.getConfigRegistry().register("settings.perks.perk.seeker_random_swap.blindness-ticks", Integer.class, 40);

        plugin.getConfigRegistry().register("settings.perks.perk.seeker_random_swap.exclude-hidden", Boolean.class, true);

        plugin.getConfigRegistry().register("settings.perks.perk.seeker_random_swap.finite", Boolean.class, false);

        plugin.getConfigRegistry().register("settings.perks.perk.seeker_map_teleport.enabled", Boolean.class, true);

        plugin.getConfigRegistry().register("settings.perks.perk.seeker_map_teleport.cost", Integer.class, 150);

        plugin.getConfigRegistry().register("settings.perks.perk.seeker_map_teleport.purchase-cooldown-ticks", Long.class, 600L);

        plugin.getConfigRegistry().register("settings.perks.perk.seeker_map_teleport.min-distance-from-hider", Double.class, 5.0d);

        plugin.getConfigRegistry().register("settings.perks.perk.seeker_map_teleport.blindness-ticks", Integer.class, 40);

        plugin.getConfigRegistry().register("settings.perks.perk.seeker_map_teleport.finite", Boolean.class, false);

        plugin.getConfigRegistry().register("settings.perks.perk.seeker_relocate.enabled", Boolean.class, true);

        plugin.getConfigRegistry().register("settings.perks.perk.seeker_relocate.cost", Integer.class, 280);

        plugin.getConfigRegistry().register("settings.perks.perk.seeker_relocate.purchase-cooldown-ticks", Long.class, 1200L);

        plugin.getConfigRegistry().register("settings.perks.perk.seeker_relocate.escape-seconds", Integer.class, 60);

        plugin.getConfigRegistry().register("settings.perks.perk.seeker_relocate.radius", Double.class, 6.0d);

        plugin.getConfigRegistry().register("settings.perks.perk.seeker_relocate.finite", Boolean.class, false);

        plugin.getConfigRegistry().register("settings.perks.perk.seeker_elytra_rush.enabled", Boolean.class, true);

        plugin.getConfigRegistry().register("settings.perks.perk.seeker_elytra_rush.cost", Integer.class, 180);

        plugin.getConfigRegistry().register("settings.perks.perk.seeker_elytra_rush.purchase-cooldown-ticks", Long.class, 600L);

        plugin.getConfigRegistry().register("settings.perks.perk.seeker_elytra_rush.duration-ticks", Long.class, 600L);

        plugin.getConfigRegistry().register("settings.perks.perk.seeker_elytra_rush.launch-power", Double.class, 0.8d);

        plugin.getConfigRegistry().register("settings.perks.perk.seeker_elytra_rush.fallback-to-levitation", Boolean.class, true);

        plugin.getConfigRegistry().register("settings.perks.perk.seeker_elytra_rush.finite", Boolean.class, false);

        plugin.getConfigRegistry().register("settings.perks.perk.seeker_proximity_meter.enabled", Boolean.class, true);

        plugin.getConfigRegistry().register("settings.perks.perk.seeker_proximity_meter.cost", Integer.class, 100);

        plugin.getConfigRegistry().register("settings.perks.perk.seeker_proximity_meter.purchase-cooldown-ticks", Long.class, 0L);

        plugin.getConfigRegistry().register("settings.perks.perk.seeker_proximity_meter.threshold-burning", Double.class, 5.0d);

        plugin.getConfigRegistry().register("settings.perks.perk.seeker_proximity_meter.threshold-very-warm", Double.class, 10.0d);

        plugin.getConfigRegistry().register("settings.perks.perk.seeker_proximity_meter.threshold-warm", Double.class, 18.0d);

        plugin.getConfigRegistry().register("settings.perks.perk.seeker_proximity_meter.threshold-lukewarm", Double.class, 30.0d);

        plugin.getConfigRegistry().register("settings.perks.perk.seeker_proximity_meter.threshold-cool", Double.class, 50.0d);

        plugin.getConfigRegistry().register("settings.perks.perk.seeker_proximity_meter.finite", Boolean.class, true);

        plugin.getConfigRegistry().register("settings.perks.perk.seeker_scent_trail.enabled", Boolean.class, true);

        plugin.getConfigRegistry().register("settings.perks.perk.seeker_scent_trail.cost", Integer.class, 90);

        plugin.getConfigRegistry().register("settings.perks.perk.seeker_scent_trail.purchase-cooldown-ticks", Long.class, 0L);

        plugin.getConfigRegistry().register("settings.perks.perk.seeker_scent_trail.trail-interval-ticks", Long.class, 5L);

        plugin.getConfigRegistry().register("settings.perks.perk.seeker_scent_trail.move-threshold", Double.class, 0.3d);

        plugin.getConfigRegistry().register("settings.perks.perk.seeker_scent_trail.particle-lifetime-seconds", Integer.class, 8);

        plugin.getConfigRegistry().register("settings.perks.perk.seeker_scent_trail.finite", Boolean.class, true);

        plugin.getConfigRegistry().register("settings.anticheat.enabled", Boolean.class, true);

        plugin.getConfigRegistry().register("settings.anticheat.hiding.filter.enabled", Boolean.class, true);

        plugin.getConfigRegistry().register("settings.anticheat.seeking.filter.enabled", Boolean.class, true);

        plugin.getConfigRegistry().register("settings.anticheat.seeking.visibility-range", Double.class, 12.0);

        plugin.getConfigRegistry().register("settings.anticheat.seeking.line-of-sight.enabled", Boolean.class, true);

        plugin.getConfigRegistry().register("settings.anticheat.seeking.line-of-sight.range", Double.class, 64.0);

        plugin.getConfigRegistry().register("settings.anticheat.seeking.line-of-sight.fov", Double.class, 60.0);

        plugin.getConfigRegistry().register("settings.anticheat.hider-camping.enabled", Boolean.class, true);

        plugin.getConfigRegistry().register("settings.anticheat.hider-camping.max-duration", Integer.class, 90);

        plugin.getConfigRegistry().register("settings.anticheat.hider-camping.warn-time", Integer.class, 15);

        plugin.getConfigRegistry().register("settings.anticheat.hider-camping.spot-radius", Double.class, 2.5);

        plugin.getConfigRegistry().register("settings.anticheat.hider-camping.damage-amount", Double.class, 1.0);

        plugin.getConfigRegistry().register("settings.anticheat.hider-camping.damage-cooldown-ticks", Integer.class, 20);

        plugin.getConfigRegistry().register("settings.anticheat.hider-camping.seeker-reward-points", Integer.class, 50);


        plugin.getConfigRegistry().register("settings.game.blockstats.show-names", Boolean.class, false);


        plugin.getConfigRegistry().register("settings.hider-items.random-block.uses", Integer.class, 5);

        plugin.getConfigRegistry().register("settings.hider-items.crossbow.hits-per-upgrade", Integer.class, 3);

        plugin.getConfigRegistry().register("settings.hider-items.sound.cooldown", Integer.class, 8);

        plugin.getConfigRegistry().register("settings.hider-items.sound.volume", Double.class, 0.75);

        plugin.getConfigRegistry().register("settings.hider-items.sound.pitch", Double.class, 0.8);

        plugin.getConfigRegistry().register("settings.hider-items.sound.note-particles", Integer.class, 8);

        plugin.getConfigRegistry().register("settings.hider-items.sound.accent-particles", Integer.class, 6);

        plugin.getConfigRegistry().register("settings.hider-items.sound.variants.default.volume-multiplier", Double.class, 1.0);

        plugin.getConfigRegistry().register("settings.hider-items.sound.variants.default.pitch-multiplier", Double.class, 1.0);

        plugin.getConfigRegistry().register("settings.hider-items.sound.variants.default.particle-multiplier", Double.class, 1.0);

        plugin.getConfigRegistry().register("settings.hider-items.sound.variants.skin_megaphone.volume-multiplier", Double.class, 0.9);

        plugin.getConfigRegistry().register("settings.hider-items.sound.variants.skin_megaphone.pitch-multiplier", Double.class, 1.0);

        plugin.getConfigRegistry().register("settings.hider-items.sound.variants.skin_megaphone.particle-multiplier", Double.class, 1.0);

        plugin.getConfigRegistry().register("settings.hider-items.sound.variants.skin_rubber_chicken.volume-multiplier", Double.class, 0.95);

        plugin.getConfigRegistry().register("settings.hider-items.sound.variants.skin_rubber_chicken.pitch-multiplier", Double.class, 1.1);

        plugin.getConfigRegistry().register("settings.hider-items.sound.variants.skin_rubber_chicken.particle-multiplier", Double.class, 1.0);

        plugin.getConfigRegistry().register("settings.hider-items.explosion.cooldown", Integer.class, 12);

        plugin.getConfigRegistry().register("settings.hider-items.explosion.volume", Double.class, 0.65);

        plugin.getConfigRegistry().register("settings.hider-items.explosion.pitch", Double.class, 1.5);

        plugin.getConfigRegistry().register("settings.hider-items.explosion.smoke-particles", Integer.class, 3);

        plugin.getConfigRegistry().register("settings.hider-items.explosion.accent-particles", Integer.class, 2);

        plugin.getConfigRegistry().register("settings.hider-items.explosion.burst-particles", Integer.class, 14);

        plugin.getConfigRegistry().register("settings.hider-items.explosion.fuse-time", Integer.class, 40);

        plugin.getConfigRegistry().register("settings.hider-items.explosion.variants.default.volume-multiplier", Double.class, 1.0);

        plugin.getConfigRegistry().register("settings.hider-items.explosion.variants.default.pitch-multiplier", Double.class, 1.0);

        plugin.getConfigRegistry().register("settings.hider-items.explosion.variants.default.smoke-multiplier", Double.class, 1.0);

        plugin.getConfigRegistry().register("settings.hider-items.explosion.variants.default.burst-multiplier", Double.class, 1.0);

        plugin.getConfigRegistry().register("settings.hider-items.explosion.variants.skin_confetti_popper.volume-multiplier", Double.class, 0.95);

        plugin.getConfigRegistry().register("settings.hider-items.explosion.variants.skin_confetti_popper.pitch-multiplier", Double.class, 1.05);

        plugin.getConfigRegistry().register("settings.hider-items.explosion.variants.skin_confetti_popper.smoke-multiplier", Double.class, 1.0);

        plugin.getConfigRegistry().register("settings.hider-items.explosion.variants.skin_confetti_popper.burst-multiplier", Double.class, 1.05);

        plugin.getConfigRegistry().register("settings.hider-items.explosion.variants.skin_bubble_popper.volume-multiplier", Double.class, 0.9);

        plugin.getConfigRegistry().register("settings.hider-items.explosion.variants.skin_bubble_popper.pitch-multiplier", Double.class, 1.1);

        plugin.getConfigRegistry().register("settings.hider-items.explosion.variants.skin_bubble_popper.smoke-multiplier", Double.class, 1.0);

        plugin.getConfigRegistry().register("settings.hider-items.explosion.variants.skin_bubble_popper.burst-multiplier", Double.class, 1.05);

        plugin.getConfigRegistry().register("settings.hider-items.speed-boost.type", String.class, "SPEED_EFFECT");

        plugin.getConfigRegistry().register("settings.hider-items.speed-boost.cooldown", Integer.class, 25);

        plugin.getConfigRegistry().register("settings.hider-items.speed-boost.duration", Integer.class, 4);

        plugin.getConfigRegistry().register("settings.hider-items.speed-boost.boost-power", Double.class, 0.65);

        plugin.getConfigRegistry().register("settings.hider-items.knockback-stick.cooldown", Integer.class, 9);

        plugin.getConfigRegistry().register("settings.hider-items.block-swap.cooldown", Integer.class, 30);

        plugin.getConfigRegistry().register("settings.hider-items.block-swap.range", Double.class, 35.0);

        plugin.getConfigRegistry().register("settings.hider-items.big-firecracker.cooldown", Integer.class, 18);

        plugin.getConfigRegistry().register("settings.hider-items.big-firecracker.fuse-time", Integer.class, 60);

        plugin.getConfigRegistry().register("settings.hider-items.big-firecracker.mini-fuse-time", Integer.class, 30);

        plugin.getConfigRegistry().register("settings.hider-items.big-firecracker.mini-count", Integer.class, 3);

        plugin.getConfigRegistry().register("settings.hider-items.big-firecracker.points", Integer.class, 20);

        plugin.getConfigRegistry().register("settings.hider-items.big-firecracker.volume", Double.class, 1.2);

        plugin.getConfigRegistry().register("settings.hider-items.big-firecracker.pitch", Double.class, 0.5);

        plugin.getConfigRegistry().register("settings.hider-items.big-firecracker.mini-volume", Double.class, 0.8);

        plugin.getConfigRegistry().register("settings.hider-items.big-firecracker.mini-pitch", Double.class, 1.2);

        plugin.getConfigRegistry().register("settings.hider-items.big-firecracker.main-particles", Integer.class, 16);

        plugin.getConfigRegistry().register("settings.hider-items.big-firecracker.mini-particles", Integer.class, 8);

        plugin.getConfigRegistry().register("settings.hider-items.big-firecracker.spark-particles", Integer.class, 5);

        plugin.getConfigRegistry().register("settings.hider-items.big-firecracker.variants.default.volume-multiplier", Double.class, 1.0);

        plugin.getConfigRegistry().register("settings.hider-items.big-firecracker.variants.default.pitch-multiplier", Double.class, 1.0);

        plugin.getConfigRegistry().register("settings.hider-items.big-firecracker.variants.default.main-particle-multiplier", Double.class, 1.0);

        plugin.getConfigRegistry().register("settings.hider-items.big-firecracker.variants.default.mini-particle-multiplier", Double.class, 1.0);

        plugin.getConfigRegistry().register("settings.hider-items.big-firecracker.variants.skin_giant_present.volume-multiplier", Double.class, 0.95);

        plugin.getConfigRegistry().register("settings.hider-items.big-firecracker.variants.skin_giant_present.pitch-multiplier", Double.class, 1.05);

        plugin.getConfigRegistry().register("settings.hider-items.big-firecracker.variants.skin_giant_present.main-particle-multiplier", Double.class, 1.0);

        plugin.getConfigRegistry().register("settings.hider-items.big-firecracker.variants.skin_giant_present.mini-particle-multiplier", Double.class, 1.0);

        plugin.getConfigRegistry().register("settings.hider-items.big-firecracker.variants.skin_boombox.volume-multiplier", Double.class, 0.95);

        plugin.getConfigRegistry().register("settings.hider-items.big-firecracker.variants.skin_boombox.pitch-multiplier", Double.class, 0.95);

        plugin.getConfigRegistry().register("settings.hider-items.big-firecracker.variants.skin_boombox.main-particle-multiplier", Double.class, 1.0);

        plugin.getConfigRegistry().register("settings.hider-items.big-firecracker.variants.skin_boombox.mini-particle-multiplier", Double.class, 1.0);

        plugin.getConfigRegistry().register("settings.hider-items.firework-rocket.cooldown", Integer.class, 10);

        plugin.getConfigRegistry().register("settings.hider-items.firework-rocket.target-y", Integer.class, 128);

        plugin.getConfigRegistry().register("settings.hider-items.firework-rocket.volume", Double.class, 10.0);

        plugin.getConfigRegistry().register("settings.hider-items.medkit.cooldown", Integer.class, 65);

        plugin.getConfigRegistry().register("settings.hider-items.medkit.channel-time", Integer.class, 5);

        plugin.getConfigRegistry().register("settings.hider-items.medkit.heal-amount", Double.class, 20.0);

        plugin.getConfigRegistry().register("settings.hider-items.totem.effect-duration", Integer.class, 5);

        plugin.getConfigRegistry().register("settings.hider-items.totem.max-uses", Integer.class, 1);

        plugin.getConfigRegistry().register("settings.hider-items.invisibility-cloak.cooldown", Integer.class, 50);

        plugin.getConfigRegistry().register("settings.hider-items.invisibility-cloak.duration", Integer.class, 6);

        plugin.getConfigRegistry().register("settings.hider-items.slowness-ball.cooldown", Integer.class, 14);

        plugin.getConfigRegistry().register("settings.hider-items.slowness-ball.duration", Integer.class, 4);

        plugin.getConfigRegistry().register("settings.hider-items.slowness-ball.amplifier", Integer.class, 1);

        plugin.getConfigRegistry().register("settings.hider-items.smoke-bomb.cooldown", Integer.class, 20);

        plugin.getConfigRegistry().register("settings.hider-items.smoke-bomb.duration", Integer.class, 6);

        plugin.getConfigRegistry().register("settings.hider-items.smoke-bomb.radius", Integer.class, 4);

        plugin.getConfigRegistry().register("settings.hider-items.ghost-essence.cooldown", Integer.class, 45);

        plugin.getConfigRegistry().register("settings.hider-items.ghost-essence.max-radius", Integer.class, 15);

        plugin.getConfigRegistry().register("settings.hider-items.ghost-essence.min-light-block", Integer.class, 1);

        plugin.getConfigRegistry().register("settings.hider-items.ghost-essence.min-light-sky", Integer.class, 1);

        plugin.getConfigRegistry().register("settings.hider-items.ghost-essence.flying-speed", Double.class, 0.01);

        plugin.getConfigRegistry().register("settings.hider-items.ghost-essence.max-duration", Float.class, 1.5f);

        plugin.getConfigRegistry().register("settings.hider-items.ghost-essence.boost-power", Float.class, 1.5f);

        plugin.getConfigRegistry().register("settings.hider-items.ghost-essence.particle-mode", String.class, "SNAP");

        plugin.getConfigRegistry().register("settings.seeker-items.grappling-hook.cooldown", Integer.class, 7);

        plugin.getConfigRegistry().register("settings.seeker-items.grappling-hook.speed", Double.class, 1.5);

        plugin.getConfigRegistry().register("settings.seeker-items.ink-splash.cooldown", Integer.class, 35);

        plugin.getConfigRegistry().register("settings.seeker-items.ink-splash.radius", Integer.class, 25);

        plugin.getConfigRegistry().register("settings.seeker-items.ink-splash.duration", Integer.class, 8);

        plugin.getConfigRegistry().register("settings.seeker-items.lightning-freeze.cooldown", Integer.class, 130);

        plugin.getConfigRegistry().register("settings.seeker-items.lightning-freeze.duration", Integer.class, 5);

        plugin.getConfigRegistry().register("settings.seeker-items.glowing-compass.cooldown", Integer.class, 50);

        plugin.getConfigRegistry().register("settings.seeker-items.glowing-compass.duration", Integer.class, 6);

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

        plugin.getConfigRegistry().register("settings.seeker-items.proximity-sensor.cooldown", Integer.class, 35);

        plugin.getConfigRegistry().register("settings.seeker-items.proximity-sensor.range", Double.class, 8.0);

        plugin.getConfigRegistry().register("settings.seeker-items.proximity-sensor.duration", Integer.class, 25);

        plugin.getConfigRegistry().register("settings.seeker-items.proximity-sensor.fov-angle", Double.class, 160.0);

        plugin.getConfigRegistry().register("settings.seeker-items.camera.cooldown", Integer.class, 2);

        plugin.getConfigRegistry().register("settings.seeker-items.camera.max-placed", Integer.class, 5);

        plugin.getConfigRegistry().register("settings.seeker-items.cage-trap.cooldown", Integer.class, 25);

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

        plugin.getConfigRegistry().register("settings.seeker-items.assistant.cooldown", Integer.class, 120);

        plugin.getConfigRegistry().register("settings.seeker-items.assistant.lifetime", Integer.class, 90);

        plugin.getConfigRegistry().register("settings.seeker-items.assistant.health", Double.class, 6.0);

        plugin.getConfigRegistry().register("settings.seeker-items.assistant.speed", Double.class, 0.38);

        plugin.getConfigRegistry().register("settings.seeker-items.assistant.pathfind-speed-multiplier", Double.class, 1.0);

        plugin.getConfigRegistry().register("settings.seeker-items.assistant.standoff-range", Double.class, 6.0);

        plugin.getConfigRegistry().register("settings.seeker-items.assistant.standoff-tolerance", Double.class, 1.5);

        plugin.getConfigRegistry().register("settings.seeker-items.assistant.max-per-seeker", Integer.class, 2);

        plugin.getConfigRegistry().register("settings.seeker-items.assistant.max-hits", Integer.class, 4);

        plugin.getConfigRegistry().register("settings.seeker-items.assistant.sniff-interval", Integer.class, 15);

        plugin.getConfigRegistry().register("settings.seeker-items.assistant.sniff-range-front", Double.class, 35.0);

        plugin.getConfigRegistry().register("settings.seeker-items.assistant.sniff-range-rear", Double.class, 15.0);

        plugin.getConfigRegistry().register("settings.seeker-items.assistant.sniff-hidden-multiplier", Double.class, 0.5);

        plugin.getConfigRegistry().register("settings.seeker-items.assistant.alert-range", Double.class, 12.0);

        plugin.getConfigRegistry().register("settings.seeker-items.assistant.alert-cooldown", Integer.class, 80);

        plugin.getConfigRegistry().register("settings.seeker-items.assistant.shoot-range", Double.class, 18.0);

        plugin.getConfigRegistry().register("settings.seeker-items.assistant.shoot-cooldown", Integer.class, 70);

        plugin.getConfigRegistry().register("settings.seeker-items.assistant.projectile-speed", Double.class, 0.3);

        plugin.getConfigRegistry().register("settings.seeker-items.assistant.projectile-gravity", Double.class, 0.03);

        plugin.getConfigRegistry().register("settings.seeker-items.assistant.projectile-lifetime", Integer.class, 70);

        plugin.getConfigRegistry().register("settings.seeker-items.assistant.projectile-homing", Double.class, 6.5);

        plugin.getConfigRegistry().register("settings.seeker-items.assistant.projectile-homing-range", Double.class, 18.0);

        plugin.getConfigRegistry().register("settings.seeker-items.assistant.projectile-aim-spread", Double.class, 0.12);

        plugin.getConfigRegistry().register("settings.seeker-items.assistant.projectile-aim-spread-stationary", Double.class, 0.06);

        plugin.getConfigRegistry().register("settings.seeker-items.assistant.projectile-aim-spread-moving", Double.class, 0.22);

        plugin.getConfigRegistry().register("settings.seeker-items.assistant.hit-direct-threshold", Double.class, 0.6);

        plugin.getConfigRegistry().register("settings.seeker-items.assistant.hit-near-threshold", Double.class, 2.5);

        plugin.getConfigRegistry().register("settings.seeker-items.assistant.hit-direct-threshold-stationary", Double.class, 1.0);

        plugin.getConfigRegistry().register("settings.seeker-items.assistant.hit-direct-threshold-moving", Double.class, 0.35);

        plugin.getConfigRegistry().register("settings.seeker-items.assistant.hit-moving-speed-threshold", Double.class, 0.05);

        plugin.getConfigRegistry().register("settings.seeker-items.assistant.hit-moving-lucky-direct-chance", Double.class, 0.);

        plugin.getConfigRegistry().register("settings.seeker-items.assistant.hit-direct-slowness-duration", Integer.class, 120);

        plugin.getConfigRegistry().register("settings.seeker-items.assistant.hit-direct-nausea-duration", Integer.class, 100);

        plugin.getConfigRegistry().register("settings.seeker-items.assistant.hit-direct-blindness-duration", Integer.class, 100);

        plugin.getConfigRegistry().register("settings.seeker-items.assistant.hit-near-slowness-base", Integer.class, 80);

        plugin.getConfigRegistry().register("settings.seeker-items.assistant.hit-near-nausea-base", Integer.class, 60);

        plugin.getConfigRegistry().register("settings.seeker-items.assistant.hit-near-blindness-base", Integer.class, 60);

        plugin.getConfigRegistry().register("settings.seeker-items.assistant.hit-direct-points", Integer.class, 60);

        plugin.getConfigRegistry().register("settings.seeker-items.assistant.hit-near-points", Integer.class, 20);

        plugin.getConfigRegistry().register("settings.seeker-items.assistant.beam-duration", Integer.class, 120);

        plugin.getConfigRegistry().register("settings.seeker-items.assistant.wander-radius-phase1", Double.class, 15.0);

        plugin.getConfigRegistry().register("settings.seeker-items.assistant.wander-radius-phase2", Double.class, 25.0);

        plugin.getConfigRegistry().register("settings.seeker-items.assistant.wander-radius-phase3", Double.class, 40.0);


        plugin.getConfigRegistry().register("settings.loadout.hider-max-items", Integer.class, 3);

        plugin.getConfigRegistry().register("settings.loadout.seeker-max-items", Integer.class, 4);

        plugin.getConfigRegistry().register("settings.loadout.hider-max-tokens", Integer.class, 12);

        plugin.getConfigRegistry().register("settings.loadout.seeker-max-tokens", Integer.class, 12);

        plugin.getConfigRegistry().register("settings.loadout.token-cost-common", Integer.class, 1);

        plugin.getConfigRegistry().register("settings.loadout.token-cost-uncommon", Integer.class, 2);

        plugin.getConfigRegistry().register("settings.loadout.token-cost-rare", Integer.class, 4);

        plugin.getConfigRegistry().register("settings.loadout.token-cost-epic", Integer.class, 6);

        plugin.getConfigRegistry().register("settings.loadout.token-cost-legendary", Integer.class, 10);

        plugin.getConfigRegistry().register("settings.skin-shop.points-per-coin", Integer.class, 25);

        plugin.getConfigRegistry().register("settings.skin-shop.cost-common", Integer.class, 100);

        plugin.getConfigRegistry().register("settings.skin-shop.cost-uncommon", Integer.class, 250);

        plugin.getConfigRegistry().register("settings.skin-shop.cost-rare", Integer.class, 500);

        plugin.getConfigRegistry().register("settings.skin-shop.cost-epic", Integer.class, 900);

        plugin.getConfigRegistry().register("settings.skin-shop.cost-legendary", Integer.class, 1500);

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

        plugin.getConfigRegistry().register("settings.points.seeker.environmental-elimination.amount", Integer.class, 50);

        plugin.getConfigRegistry().register("settings.timer.hiding.primary-color", String.class, "#FF0000");

        plugin.getConfigRegistry().register("settings.timer.hiding.secondary-color", String.class, "#0000FF");

        plugin.getConfigRegistry().register("settings.timer.seeking.primary-color", String.class, "#FFFF00");

        plugin.getConfigRegistry().register("settings.timer.seeking.secondary-color", String.class, "#00FFFF");

        plugin.getConfigRegistry().register("settings.timer.animation.type", String.class, "WAVE");

        plugin.getConfigRegistry().register("settings.timer.animation.speed", Double.class, 0.5);

        plugin.getConfigRegistry().register("settings.game.seeking-bossbar.enabled", Boolean.class, true);

        plugin.getConfigRegistry().register("settings.game.seeking-bossbar.name-layout", String.class, "HIDERS_AND_SEEKERS");

        plugin.getConfigRegistry().register("settings.game.seeking-bossbar.progress-mode", String.class, "PROGRESS");

        plugin.getConfigRegistry().register("settings.game.seeking-bossbar.color.mode", String.class, "DYNAMIC");

        plugin.getConfigRegistry().register("settings.game.seeking-bossbar.color.static-color", String.class, "GREEN");

        plugin.getConfigRegistry().register("settings.game.seeking-bossbar.animation.enabled", Boolean.class, true);

        plugin.getConfigRegistry().register("settings.game.seeking-bossbar.animation.speed-ticks", Integer.class, 3);
    }


    private static void registerSections(HideAndSeek plugin) {
        plugin.getSectionRegistry().register(SectionDefinition.builder("game").icon(Material.COMPARATOR).build());

        plugin.getSectionRegistry().register(SectionDefinition.builder("game.block-form").icon(Material.BRICKS).build());

        plugin.getSectionRegistry().register(SectionDefinition.builder("game.blockstats").icon(Material.BOOKSHELF).build());

        plugin.getSectionRegistry().register(SectionDefinition.builder("game.hiders").icon(Material.PLAYER_HEAD).build());

        plugin.getSectionRegistry().register(SectionDefinition.builder("game.lobby").icon(Material.LIME_STAINED_GLASS_PANE).build());

        plugin.getSectionRegistry().register(SectionDefinition.builder("game.maps").icon(Material.MAP).build());

        plugin.getSectionRegistry().register(SectionDefinition.builder("game.round").icon(Material.CLOCK).build());

        plugin.getSectionRegistry().register(SectionDefinition.builder("game.seekers").icon(Material.IRON_SWORD).build());

        plugin.getSectionRegistry().register(SectionDefinition.builder("game.seekers.gaze-kill").icon(Material.ENDER_EYE).build());

        plugin.getSectionRegistry().register(SectionDefinition.builder("game.small-mode").icon(Material.SLIME_BALL).build());

        plugin.getSectionRegistry().register(SectionDefinition.builder("game.team-distribution").icon(Material.PLAYER_HEAD).build());

        plugin.getSectionRegistry().register(SectionDefinition.builder("game.teams").icon(Material.WHITE_BANNER).build());

        plugin.getSectionRegistry().register(SectionDefinition.builder("game.voting").icon(Material.PAPER).build());

        plugin.getSectionRegistry().register(SectionDefinition.builder("game.world-border").icon(Material.BARRIER).build());

        plugin.getSectionRegistry().register(SectionDefinition.builder("perks").icon(Material.NETHER_STAR).build());

        plugin.getSectionRegistry().register(SectionDefinition.builder("perks.perk").icon(Material.BREWING_STAND).build());

        plugin.getSectionRegistry().register(SectionDefinition.builder("perks.perk.global").icon(Material.FILLED_MAP).build());

        plugin.getSectionRegistry().register(SectionDefinition.builder("perks.perk.hider_adaptive_speed").icon(Material.SUGAR).build());

        plugin.getSectionRegistry().register(SectionDefinition.builder("perks.perk.hider_seeker_warning").icon(Material.CLOCK).build());

        plugin.getSectionRegistry().register(SectionDefinition.builder("perks.perk.hider_extra_life").icon(Material.TOTEM_OF_UNDYING).build());

        plugin.getSectionRegistry().register(SectionDefinition.builder("perks.perk.hider_shadow_step").icon(Material.ENDER_EYE).build());

        plugin.getSectionRegistry().register(SectionDefinition.builder("perks.perk.hider_camouflage").icon(Material.FERN).build());

        plugin.getSectionRegistry().register(SectionDefinition.builder("perks.perk.hider_double_jump").icon(Material.FEATHER).build());

        plugin.getSectionRegistry().register(SectionDefinition.builder("perks.perk.hider_trap_sense").icon(Material.TRIPWIRE_HOOK).build());

        plugin.getSectionRegistry().register(SectionDefinition.builder("perks.perk.seeker_death_zone").icon(Material.WITHER_ROSE).build());

        plugin.getSectionRegistry().register(SectionDefinition.builder("perks.perk.seeker_random_swap").icon(Material.WARPED_FUNGUS_ON_A_STICK).build());

        plugin.getSectionRegistry().register(SectionDefinition.builder("perks.perk.seeker_map_teleport").icon(Material.COMPASS).build());

        plugin.getSectionRegistry().register(SectionDefinition.builder("perks.perk.seeker_relocate").icon(Material.LEAD).build());

        plugin.getSectionRegistry().register(SectionDefinition.builder("perks.perk.seeker_elytra_rush").icon(Material.FEATHER).build());

        plugin.getSectionRegistry().register(SectionDefinition.builder("perks.perk.seeker_proximity_meter").icon(Material.CLOCK).build());

        plugin.getSectionRegistry().register(SectionDefinition.builder("perks.perk.seeker_scent_trail").icon(Material.DIRT_PATH).build());

        plugin.getSectionRegistry().register(SectionDefinition.builder("hider-items").icon(Material.PLAYER_HEAD).build());

        plugin.getSectionRegistry().register(SectionDefinition.builder("hider-items.crossbow").icon(Material.CROSSBOW).build());

        plugin.getSectionRegistry().register(SectionDefinition.builder("hider-items.random-block").icon(Material.COBBLESTONE).build());

        plugin.getSectionRegistry().register(SectionDefinition.builder("hider-items.sound").icon(Material.CAT_SPAWN_EGG).build());

        plugin.getSectionRegistry().register(SectionDefinition.builder("hider-items.sound.variants").icon(Material.JUKEBOX).build());

        plugin.getSectionRegistry().register(SectionDefinition.builder("hider-items.sound.variants.default").icon(Material.NOTE_BLOCK).build());

        plugin.getSectionRegistry().register(SectionDefinition.builder("hider-items.sound.variants.skin_megaphone").icon(Material.BELL).build());

        plugin.getSectionRegistry().register(SectionDefinition.builder("hider-items.sound.variants.skin_rubber_chicken").icon(Material.EGG).build());

        plugin.getSectionRegistry().register(SectionDefinition.builder("hider-items.explosion").icon(Material.RED_CANDLE).build());

        plugin.getSectionRegistry().register(SectionDefinition.builder("hider-items.explosion.variants").icon(Material.FIREWORK_STAR).build());

        plugin.getSectionRegistry().register(SectionDefinition.builder("hider-items.explosion.variants.default").icon(Material.TNT).build());

        plugin.getSectionRegistry().register(SectionDefinition.builder("hider-items.explosion.variants.skin_bubble_popper").icon(Material.WATER_BUCKET).build());

        plugin.getSectionRegistry().register(SectionDefinition.builder("hider-items.explosion.variants.skin_confetti_popper").icon(Material.FIREWORK_ROCKET).build());

        plugin.getSectionRegistry().register(SectionDefinition.builder("hider-items.speed-boost").icon(Material.WOODEN_HOE).build());

        plugin.getSectionRegistry().register(SectionDefinition.builder("hider-items.knockback-stick").icon(Material.STICK).build());

        plugin.getSectionRegistry().register(SectionDefinition.builder("hider-items.block-swap").icon(Material.ENDER_PEARL).build());

        plugin.getSectionRegistry().register(SectionDefinition.builder("hider-items.big-firecracker").icon(Material.TNT).build());

        plugin.getSectionRegistry().register(SectionDefinition.builder("hider-items.big-firecracker.variants").icon(Material.TNT).build());

        plugin.getSectionRegistry().register(SectionDefinition.builder("hider-items.big-firecracker.variants.default").icon(Material.FIREWORK_STAR).build());

        plugin.getSectionRegistry().register(SectionDefinition.builder("hider-items.big-firecracker.variants.skin_boombox").icon(Material.JUKEBOX).build());

        plugin.getSectionRegistry().register(SectionDefinition.builder("hider-items.big-firecracker.variants.skin_giant_present").icon(Material.CHEST).build());

        plugin.getSectionRegistry().register(SectionDefinition.builder("hider-items.firework-rocket").icon(Material.FIREWORK_ROCKET).build());

        plugin.getSectionRegistry().register(SectionDefinition.builder("hider-items.medkit").icon(Material.GOLDEN_APPLE).build());

        plugin.getSectionRegistry().register(SectionDefinition.builder("hider-items.totem").icon(Material.TOTEM_OF_UNDYING).build());

        plugin.getSectionRegistry().register(SectionDefinition.builder("hider-items.invisibility-cloak").icon(Material.PHANTOM_MEMBRANE).build());

        plugin.getSectionRegistry().register(SectionDefinition.builder("hider-items.slowness-ball").icon(Material.SNOWBALL).build());

        plugin.getSectionRegistry().register(SectionDefinition.builder("hider-items.smoke-bomb").icon(Material.GRAY_DYE).build());

        plugin.getSectionRegistry().register(SectionDefinition.builder("hider-items.ghost-essence").icon(Material.GHAST_TEAR).build());

        plugin.getSectionRegistry().register(SectionDefinition.builder("timer").icon(Material.CLOCK).build());

        plugin.getSectionRegistry().register(SectionDefinition.builder("timer.animation").icon(Material.AMETHYST_SHARD).build());

        plugin.getSectionRegistry().register(SectionDefinition.builder("timer.hiding").icon(Material.RED_CONCRETE).build());

        plugin.getSectionRegistry().register(SectionDefinition.builder("timer.seeking").icon(Material.YELLOW_CONCRETE).build());

        plugin.getSectionRegistry().register(SectionDefinition.builder("seeker-items").icon(Material.ENDER_EYE).build());

        plugin.getSectionRegistry().register(SectionDefinition.builder("seeker-items.grappling-hook").icon(Material.FISHING_ROD).build());

        plugin.getSectionRegistry().register(SectionDefinition.builder("seeker-items.ink-splash").icon(Material.INK_SAC).build());

        plugin.getSectionRegistry().register(SectionDefinition.builder("seeker-items.lightning-freeze").icon(Material.LIGHTNING_ROD).build());

        plugin.getSectionRegistry().register(SectionDefinition.builder("seeker-items.glowing-compass").icon(Material.COMPASS).build());

        plugin.getSectionRegistry().register(SectionDefinition.builder("seeker-items.curse-spell").icon(Material.ENCHANTED_BOOK).build());

        plugin.getSectionRegistry().register(SectionDefinition.builder("seeker-items.block-randomizer").icon(Material.BLAZE_POWDER).build());

        plugin.getSectionRegistry().register(SectionDefinition.builder("seeker-items.chain-pull").icon(Material.LEAD).build());

        plugin.getSectionRegistry().register(SectionDefinition.builder("seeker-items.proximity-sensor").icon(Material.REDSTONE_TORCH).build());

        plugin.getSectionRegistry().register(SectionDefinition.builder("seeker-items.assistant").icon(Material.ZOMBIE_HEAD).build());

        plugin.getSectionRegistry().register(SectionDefinition.builder("seeker-items.cage-trap").icon(Material.IRON_BARS).build());

        plugin.getSectionRegistry().register(SectionDefinition.builder("seeker-items.sword-of-seeking").icon(Material.IRON_SWORD).build());

        plugin.getSectionRegistry().register(SectionDefinition.builder("seeker-items.seeker-sword-throw").icon(Material.DIAMOND_SWORD).build());

        plugin.getSectionRegistry().register(SectionDefinition.builder("loadout").icon(Material.ARMOR_STAND).build());

        plugin.getSectionRegistry().register(SectionDefinition.builder("loadout.token-cost").icon(Material.GOLD_BLOCK).build());

        plugin.getSectionRegistry().register(SectionDefinition.builder("skin-shop").icon(Material.DIAMOND).build());

        plugin.getSectionRegistry().register(SectionDefinition.builder("persistence").icon(Material.CHEST_MINECART).build());

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

        plugin.getSectionRegistry().register(SectionDefinition.builder("points.seeker.environmental-elimination").icon(Material.LAVA_BUCKET).build());

        plugin.getSectionRegistry().register(SectionDefinition.builder("points.seeker.first-blood").icon(Material.REDSTONE).build());

        plugin.getSectionRegistry().register(SectionDefinition.builder("points.seeker.special").icon(Material.WOLF_SPAWN_EGG).build());

        plugin.getSectionRegistry().register(SectionDefinition.builder("anticheat").icon(Material.SHIELD).build());

        plugin.getSectionRegistry().register(SectionDefinition.builder("anticheat.hider-camping").icon(Material.CAMPFIRE).build());

        plugin.getSectionRegistry().register(SectionDefinition.builder("anticheat.hiding").icon(Material.SHIELD).build());

        plugin.getSectionRegistry().register(SectionDefinition.builder("anticheat.hiding.filter").icon(Material.HOPPER).build());

        plugin.getSectionRegistry().register(SectionDefinition.builder("anticheat.seeking").icon(Material.ENDER_EYE).build());

        plugin.getSectionRegistry().register(SectionDefinition.builder("anticheat.seeking.filter").icon(Material.HOPPER).build());

        plugin.getSectionRegistry().register(SectionDefinition.builder("anticheat.seeking.line-of-sight").icon(Material.SPYGLASS).build());

        plugin.getSectionRegistry().register(SectionDefinition.builder("game.seeking-bossbar").icon(Material.MAGMA_CREAM).build());

        plugin.getSectionRegistry().register(SectionDefinition.builder("game.seeking-bossbar.color").icon(Material.REDSTONE).build());

        plugin.getSectionRegistry().register(SectionDefinition.builder("game.seeking-bossbar.animation").icon(Material.CLOCK).build());
    }

    public static void registerSettings(HideAndSeek plugin) {
        SettingGroupRegistrar.register(plugin, List.of(
                new GameCoreSettingGroup(),
                new PerkCoreSettingGroup(),
                new PerkHiderAdaptiveSpeedSettingGroup(),
                new PerkHiderExtraLifeSettingGroup(),
                new PerkHiderCamouflageSettingGroup(),
                new PerkHiderDoubleJumpSettingGroup(),
                new PerkHiderSeekerWarningSettingGroup(),
                new PerkHiderTrapSenseSettingGroup(),
                new PerkHiderShadowStepSettingGroup(),
                new PerkSeekerDeathZoneSettingGroup(),
                new PerkSeekerRelocateSettingGroup(),
                new PerkSeekerElytraRushSettingGroup(),
                new PerkSeekerProximityMeterSettingGroup(),
                new PerkSeekerScentTrailSettingGroup(),
                new PerkSeekerMapTeleportSettingGroup(),
                new PerkSeekerRandomSwapSettingGroup(),
                new TimerSettingGroup(),
                new AnticheatSettingGroup(),
                new LoadoutSettingGroup(),
                new SkinShopSettingGroup(),
                new PointsSettingGroup(),
                new GameAdvancedSettingGroup(),
                new HiderItemsSettingGroup(),
                new SeekerItemsSettingGroup()
        ));
    }
}
