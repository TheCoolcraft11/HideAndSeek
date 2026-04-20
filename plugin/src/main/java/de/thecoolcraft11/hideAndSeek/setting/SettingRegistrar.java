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

        plugin.getConfigRegistry().register("nms.enabled", Boolean.class, true);

        plugin.getConfigRegistry().register("persistence.save-skin-data", Boolean.class, true);

        plugin.getConfigRegistry().register("persistence.save-loadout-data", Boolean.class, true);

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

        plugin.getSectionRegistry().register(
                SectionDefinition.builder("game.environmental-damage").icon(Material.LAVA_BUCKET).build());

        plugin.getSectionRegistry().register(
                SectionDefinition.builder("game.environmental-damage.drowning").icon(Material.WATER_BUCKET).build());

        plugin.getSectionRegistry().register(
                SectionDefinition.builder("game.environmental-damage.fire").icon(Material.FLINT_AND_STEEL).build());

        plugin.getSectionRegistry().register(
                SectionDefinition.builder("game.environmental-damage.lava").icon(Material.LAVA_BUCKET).build());

        plugin.getSectionRegistry().register(
                SectionDefinition.builder("game.environmental-damage.suffocation").icon(Material.SAND).build());

        plugin.getSectionRegistry().register(SectionDefinition.builder("game.environmental-damage.freezing").icon(
                Material.POWDER_SNOW_BUCKET).build());

        plugin.getSectionRegistry().register(
                SectionDefinition.builder("game.environmental-damage.hot-floor").icon(Material.MAGMA_BLOCK).build());

        plugin.getSectionRegistry().register(
                SectionDefinition.builder("game.environmental-damage.contact").icon(Material.SWEET_BERRIES).build());

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

        plugin.getSectionRegistry().register(SectionDefinition.builder("game.unstuck").icon(Material.ENDER_PEARL).build());

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

        plugin.getSectionRegistry().register(SectionDefinition.builder("hider-items.remote-gateway").icon(Material.ENDER_EYE).build());

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

        plugin.getSectionRegistry().register(SectionDefinition.builder("seeker-items.phantom-viewer").icon(Material.FILLED_MAP).build());

        plugin.getSectionRegistry().register(SectionDefinition.builder("seeker-items.crowbar").icon(Material.IRON_PICKAXE).build());

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
