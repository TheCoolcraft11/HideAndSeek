package de.thecoolcraft11.hideAndSeek.util.setting;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.util.GameModeEnum;
import de.thecoolcraft11.hideAndSeek.util.GameStyleEnum;
import de.thecoolcraft11.hideAndSeek.util.SeekerKillModeEnum;
import de.thecoolcraft11.hideAndSeek.util.SpeedBoostType;
import de.thecoolcraft11.minigameframework.config.SettingDefinition;
import de.thecoolcraft11.minigameframework.config.SettingType;
import de.thecoolcraft11.timer.AnimationType;
import org.bukkit.Material;

import java.util.Map;

public class SettingRegisterer {


    public static void registerSettings(HideAndSeek plugin) {
        plugin.getSettingRegistry().register(SettingDefinition.builder("game.gametype", SettingType.ENUM, GameModeEnum.class)
                .defaultValue(GameModeEnum.NORMAL)
                .customIcon(Material.POTION)
                .description("Gamemode of the game")
                .valueIcons(Map.of(
                        GameModeEnum.NORMAL, Material.PLAYER_HEAD,
                        GameModeEnum.BLOCK, Material.COBBLESTONE,
                        GameModeEnum.SMALL, Material.IRON_NUGGET
                ))
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("game.gamestyle", SettingType.ENUM, GameStyleEnum.class)
                .defaultValue(GameStyleEnum.SPECTATOR)
                .customIcon(Material.IRON_SWORD)
                .description("Gamemode of the game")
                .valueIcons(Map.of(
                        GameStyleEnum.SPECTATOR, Material.ENDER_EYE,
                        GameStyleEnum.INVASION, Material.SUSPICIOUS_STEW,
                        GameStyleEnum.INFINITE, Material.BLAZE_POWDER
                ))
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("game.hiding_time", SettingType.INTEGER, Integer.class)
                .defaultValue(60)
                .range(10, 600)
                .description("Hiding phase duration in seconds")
                .customIcon(Material.CLOCK)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("game.seeking_time", SettingType.INTEGER, Integer.class)
                .defaultValue(300)
                .range(60, 1800)
                .description("Seeking phase duration in seconds")
                .customIcon(Material.COMPASS)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("game.hider_invisibility", SettingType.BOOLEAN, Boolean.class)
                .defaultValue(false)
                .description("Grant hiders invisibility during hiding phase")
                .customIcon(Material.DARK_OAK_SAPLING)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("game.small_mode_size", SettingType.DOUBLE, Double.class)
                .defaultValue(0.5)
                .description("Size scale for SMALL mode hiders (0.1 = tiny, 1.0 = normal)")
                .customIcon(Material.IRON_NUGGET)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("game.random_team_distribution", SettingType.BOOLEAN, Boolean.class)
                .defaultValue(true)
                .description("Enable random distribution of players into hider/seeker teams")
                .customIcon(Material.REDSTONE)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("game.use_preferred_modes", SettingType.BOOLEAN, Boolean.class)
                .defaultValue(true)
                .description("Only select maps that have the current game mode in their preferred modes list")
                .customIcon(Material.MAP)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("game.fixed_seeker_team", SettingType.STRING, String.class)
                .defaultValue("")
                .description("Fixed seeker team (leave empty for random). Set to a team name to always use that team as seekers")
                .customIcon(Material.ENDER_EYE)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("game.seeker_count", SettingType.INTEGER, Integer.class)
                .defaultValue(1)
                .range(1, 10)
                .description("Number of seekers (if random distribution is enabled)")
                .customIcon(Material.ENDER_EYE)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("game.hider_health", SettingType.INTEGER, Integer.class)
                .defaultValue(20)
                .range(1, 20)
                .description("Health of hiders (in half-hearts)")
                .customIcon(Material.REDSTONE_BLOCK)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("game.block_view_height", SettingType.FLOAT, Float.class)
                .defaultValue(0.1f)
                .rangeFloat(0f, 1.5f)
                .description("View Height of player when they hide in a block")
                .customIcon(Material.LADDER)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("game.block_size_to_block", SettingType.BOOLEAN, Boolean.class)
                .defaultValue(false)
                .description("Scale hiders to the hidden block's height while hidden in BLOCK mode")
                .customIcon(Material.SCAFFOLDING)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("game.seeker_kill_mode", SettingType.ENUM, SeekerKillModeEnum.class)
                .defaultValue(SeekerKillModeEnum.NORMAL)
                .description("How seekers kill hiders")
                .valueIcons(Map.of(
                        SeekerKillModeEnum.NORMAL, Material.IRON_SWORD,
                        SeekerKillModeEnum.ONE_HIT, Material.DIAMOND_SWORD,
                        SeekerKillModeEnum.GAZE_KILL, Material.ENDER_EYE
                ))
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("game.auto_cleanup_after_round", SettingType.BOOLEAN, Boolean.class)
                .defaultValue(true)
                .description("Automatically teleport players to lobby and delete map after round")
                .customIcon(Material.REDSTONE_BLOCK)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("game.small_mode_seeker_size", SettingType.DOUBLE, Double.class)
                .defaultValue(1.0)
                .rangeDouble(0.1, 2.0)
                .description("Size scale for seekers in SMALL mode (1.0 = normal size)")
                .customIcon(Material.IRON_NUGGET)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("blockstats.show-names", SettingType.BOOLEAN, Boolean.class)
                .defaultValue(false)
                .description("Show player names in Block Statistics GUI")
                .customIcon(Material.NAME_TAG)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.random-block.uses", SettingType.INTEGER, Integer.class)
                .defaultValue(5)
                .range(1, 20)
                .description("Max uses for random block transform item")
                .customIcon(Material.BLAZE_POWDER)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.crossbow.hits-per-upgrade", SettingType.INTEGER, Integer.class)
                .defaultValue(3)
                .range(1, 10)
                .description("Hits needed to upgrade speed boost")
                .customIcon(Material.CROSSBOW)
                .build());


        plugin.getSettingRegistry().register(SettingDefinition.builder("timer.hiding_color1", SettingType.STRING, String.class)
                .defaultValue("#FF0000")
                .description("Primary color for hiding timer (hex code)")
                .customIcon(Material.RED_DYE)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("timer.hiding_color2", SettingType.STRING, String.class)
                .defaultValue("#0000FF")
                .description("Secondary color for hiding timer (hex code)")
                .customIcon(Material.BLUE_DYE)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("timer.seeking_color1", SettingType.STRING, String.class)
                .defaultValue("#FFFF00")
                .description("Primary color for seeking timer (hex code)")
                .customIcon(Material.YELLOW_DYE)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("timer.seeking_color2", SettingType.STRING, String.class)
                .defaultValue("#00FFFF")
                .description("Secondary color for seeking timer (hex code)")
                .customIcon(Material.CYAN_DYE)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("timer.animation_type", SettingType.ENUM, AnimationType.class)
                .defaultValue(AnimationType.WAVE)
                .description("Timer animation type")
                .customIcon(Material.AMETHYST_SHARD)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("timer.animation_speed", SettingType.DOUBLE, Double.class)
                .defaultValue(0.5)
                .range(0, 2)
                .description("Timer animation speed (0.1 = slow, 2.0 = fast)")
                .customIcon(Material.REDSTONE)
                .build());


        plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.sound.cooldown", SettingType.INTEGER, Integer.class)
                .defaultValue(4)
                .range(1, 30)
                .description("Cooldown for cat sound item in seconds")
                .customIcon(Material.CAT_SPAWN_EGG)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.sound.points", SettingType.INTEGER, Integer.class)
                .defaultValue(5)
                .range(0, 100)
                .description("Points awarded for using cat sound")
                .customIcon(Material.CAT_SPAWN_EGG)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.sound.volume", SettingType.DOUBLE, Double.class)
                .defaultValue(0.75)
                .rangeDouble(0.1, 2.0)
                .description("Volume of cat sound (0.1 = quiet, 2.0 = loud)")
                .customIcon(Material.CAT_SPAWN_EGG)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.sound.pitch", SettingType.DOUBLE, Double.class)
                .defaultValue(0.8)
                .rangeDouble(0.5, 2.0)
                .description("Pitch of cat sound (0.5 = low, 2.0 = high)")
                .customIcon(Material.CAT_SPAWN_EGG)
                .build());


        plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.explosion.cooldown", SettingType.INTEGER, Integer.class)
                .defaultValue(8)
                .range(0, 30)
                .description("Cooldown for firecracker item in seconds")
                .customIcon(Material.RED_CANDLE)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.explosion.points", SettingType.INTEGER, Integer.class)
                .defaultValue(10)
                .range(0, 100)
                .description("Points awarded for using firecracker")
                .customIcon(Material.RED_CANDLE)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.explosion.volume", SettingType.DOUBLE, Double.class)
                .defaultValue(0.65)
                .rangeDouble(0.1, 2.0)
                .description("Volume of explosion sound")
                .customIcon(Material.RED_CANDLE)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.explosion.pitch", SettingType.DOUBLE, Double.class)
                .defaultValue(1.5)
                .rangeDouble(0.5, 2.0)
                .description("Pitch of explosion sound")
                .customIcon(Material.RED_CANDLE)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.explosion.smoke-particles", SettingType.INTEGER, Integer.class)
                .defaultValue(3)
                .range(1, 20)
                .description("Number of smoke particles per tick")
                .customIcon(Material.RED_CANDLE)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.explosion.fuse-time", SettingType.INTEGER, Integer.class)
                .defaultValue(40)
                .range(10, 100)
                .description("Fuse time in ticks before explosion (20 ticks = 1 second)")
                .customIcon(Material.RED_CANDLE)
                .build());


        plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.speed-boost.type", SettingType.ENUM, SpeedBoostType.class)
                .defaultValue(SpeedBoostType.SPEED_EFFECT)
                .description("Speed boost type: SPEED_EFFECT or VELOCITY_BOOST")
                .customIcon(Material.WOODEN_HOE)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.speed-boost.cooldown", SettingType.INTEGER, Integer.class)
                .defaultValue(10)
                .range(0, 60)
                .description("Cooldown for speed boost in seconds")
                .customIcon(Material.WOODEN_HOE)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.speed-boost.duration", SettingType.INTEGER, Integer.class)
                .defaultValue(5)
                .range(1, 30)
                .description("Duration of speed effect in seconds (SPEED_EFFECT only)")
                .customIcon(Material.WOODEN_HOE)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.speed-boost.boost-power", SettingType.DOUBLE, Double.class)
                .defaultValue(0.5)
                .rangeDouble(0.1, 2.0)
                .description("Power of velocity boost (VELOCITY_BOOST only)")
                .customIcon(Material.WOODEN_HOE)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.knockback-stick.level", SettingType.INTEGER, Integer.class)
                .defaultValue(2)
                .range(1, 5)
                .description("Knockback level for knockback stick")
                .customIcon(Material.STICK)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.block-swap.cooldown", SettingType.INTEGER, Integer.class)
                .defaultValue(15)
                .range(0, 60)
                .description("Cooldown for block swap in seconds")
                .customIcon(Material.ENDER_PEARL)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.block-swap.range", SettingType.DOUBLE, Double.class)
                .defaultValue(50.0)
                .rangeDouble(5.0, 200.0)
                .description("Maximum swap range for block swap")
                .customIcon(Material.ENDER_PEARL)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.big-firecracker.cooldown", SettingType.INTEGER, Integer.class)
                .defaultValue(12)
                .range(0, 60)
                .description("Cooldown for big firecracker in seconds")
                .customIcon(Material.TNT)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.big-firecracker.fuse-time", SettingType.INTEGER, Integer.class)
                .defaultValue(60)
                .range(10, 200)
                .description("Fuse time in ticks before big explosion")
                .customIcon(Material.TNT)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.big-firecracker.mini-fuse-time", SettingType.INTEGER, Integer.class)
                .defaultValue(30)
                .range(5, 100)
                .description("Fuse time in ticks for mini firecrackers")
                .customIcon(Material.TNT)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.big-firecracker.mini-count", SettingType.INTEGER, Integer.class)
                .defaultValue(3)
                .range(1, 10)
                .description("Number of mini firecrackers")
                .customIcon(Material.TNT)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.big-firecracker.points", SettingType.INTEGER, Integer.class)
                .defaultValue(20)
                .range(0, 200)
                .description("Points for big firecracker")
                .customIcon(Material.TNT)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.big-firecracker.volume", SettingType.DOUBLE, Double.class)
                .defaultValue(1.2)
                .rangeDouble(0.1, 2.0)
                .description("Explosion volume for big firecracker")
                .customIcon(Material.TNT)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.big-firecracker.pitch", SettingType.DOUBLE, Double.class)
                .defaultValue(0.5)
                .rangeDouble(0.1, 2.0)
                .description("Explosion pitch for big firecracker")
                .customIcon(Material.TNT)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.firework-rocket.cooldown", SettingType.INTEGER, Integer.class)
                .defaultValue(10)
                .range(0, 60)
                .description("Cooldown for firework rocket in seconds")
                .customIcon(Material.FIREWORK_ROCKET)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.firework-rocket.target-y", SettingType.INTEGER, Integer.class)
                .defaultValue(128)
                .range(-64, 320)
                .description("Target Y for firework explosion")
                .customIcon(Material.FIREWORK_ROCKET)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.firework-rocket.points", SettingType.INTEGER, Integer.class)
                .defaultValue(15)
                .range(0, 200)
                .description("Points for firework rocket")
                .customIcon(Material.FIREWORK_ROCKET)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.firework-rocket.volume", SettingType.DOUBLE, Double.class)
                .defaultValue(10.0)
                .rangeDouble(0.1, 15.0)
                .description("Explosion volume for firework rocket")
                .customIcon(Material.FIREWORK_ROCKET)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.medkit.cooldown", SettingType.INTEGER, Integer.class)
                .defaultValue(30)
                .range(0, 120)
                .description("Cooldown for medkit in seconds")
                .customIcon(Material.GOLDEN_APPLE)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.medkit.channel-time", SettingType.INTEGER, Integer.class)
                .defaultValue(5)
                .range(1, 30)
                .description("Time to stand still before healing")
                .customIcon(Material.GOLDEN_APPLE)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.medkit.heal-amount", SettingType.DOUBLE, Double.class)
                .defaultValue(20.0)
                .rangeDouble(1.0, 40.0)
                .description("Heal amount in half-hearts")
                .customIcon(Material.GOLDEN_APPLE)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.totem.effect-duration", SettingType.INTEGER, Integer.class)
                .defaultValue(30)
                .range(5, 120)
                .description("Duration of revive effect in seconds")
                .customIcon(Material.TOTEM_OF_UNDYING)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("hider-items.totem.max-uses", SettingType.INTEGER, Integer.class)
                .defaultValue(1)
                .range(1, 5)
                .description("Max uses per totem")
                .customIcon(Material.TOTEM_OF_UNDYING)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.grappling-hook.cooldown", SettingType.INTEGER, Integer.class)
                .defaultValue(2)
                .range(0, 30)
                .description("Cooldown for grappling hook in seconds")
                .customIcon(Material.FISHING_ROD)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.grappling-hook.speed", SettingType.DOUBLE, Double.class)
                .defaultValue(1.5)
                .rangeDouble(0.3, 3.0)
                .description("Base speed for grappling hook pull")
                .customIcon(Material.FISHING_ROD)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.ink-splash.cooldown", SettingType.INTEGER, Integer.class)
                .defaultValue(20)
                .range(0, 60)
                .description("Cooldown for ink splash in seconds")
                .customIcon(Material.INK_SAC)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.ink-splash.radius", SettingType.INTEGER, Integer.class)
                .defaultValue(5)
                .range(1, 50)
                .description("Radius of ink splash")
                .customIcon(Material.INK_SAC)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.ink-splash.duration", SettingType.INTEGER, Integer.class)
                .defaultValue(5)
                .range(1, 30)
                .description("Duration of ink blindness in seconds")
                .customIcon(Material.INK_SAC)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.lightning-freeze.cooldown", SettingType.INTEGER, Integer.class)
                .defaultValue(60)
                .range(10, 300)
                .description("Cooldown for lightning freeze in seconds")
                .customIcon(Material.LIGHTNING_ROD)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.lightning-freeze.duration", SettingType.INTEGER, Integer.class)
                .defaultValue(5)
                .range(1, 30)
                .description("Duration of freeze in seconds")
                .customIcon(Material.LIGHTNING_ROD)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.glowing-compass.cooldown", SettingType.INTEGER, Integer.class)
                .defaultValue(25)
                .range(0, 60)
                .description("Cooldown for glowing compass in seconds")
                .customIcon(Material.COMPASS)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.glowing-compass.duration", SettingType.INTEGER, Integer.class)
                .defaultValue(10)
                .range(1, 60)
                .description("Duration of glow in seconds")
                .customIcon(Material.COMPASS)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.glowing-compass.range", SettingType.DOUBLE, Double.class)
                .defaultValue(50.0)
                .rangeDouble(10.0, 200.0)
                .description("Range to detect nearest hider")
                .customIcon(Material.COMPASS)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.curse-spell.cooldown", SettingType.INTEGER, Integer.class)
                .defaultValue(30)
                .range(0, 60)
                .description("Cooldown for curse spell in seconds")
                .customIcon(Material.ENCHANTED_BOOK)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.curse-spell.active-duration", SettingType.INTEGER, Integer.class)
                .defaultValue(10)
                .range(1, 60)
                .description("Duration curse spell is active on sword")
                .customIcon(Material.ENCHANTED_BOOK)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.curse-spell.curse-duration", SettingType.INTEGER, Integer.class)
                .defaultValue(8)
                .range(1, 60)
                .description("Duration of curse on hider")
                .customIcon(Material.ENCHANTED_BOOK)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.curse-spell.small-shrink-delay", SettingType.INTEGER, Integer.class)
                .defaultValue(8)
                .range(1, 60)
                .description("Delay before shrinking back in SMALL mode")
                .customIcon(Material.ENCHANTED_BOOK)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.block-randomizer.cooldown", SettingType.INTEGER, Integer.class)
                .defaultValue(45)
                .range(10, 120)
                .description("Cooldown for block randomizer in seconds")
                .customIcon(Material.BLAZE_POWDER)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.chain-pull.cooldown", SettingType.INTEGER, Integer.class)
                .defaultValue(12)
                .range(0, 60)
                .description("Cooldown for chain pull in seconds")
                .customIcon(Material.LEAD)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.chain-pull.range", SettingType.DOUBLE, Double.class)
                .defaultValue(30.0)
                .rangeDouble(5.0, 100.0)
                .description("Maximum range for chain pull")
                .customIcon(Material.LEAD)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.chain-pull.pull-power", SettingType.DOUBLE, Double.class)
                .defaultValue(2.0)
                .rangeDouble(0.5, 5.0)
                .description("Pull power multiplier")
                .customIcon(Material.LEAD)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("seeker-items.chain-pull.slowness-duration", SettingType.INTEGER, Integer.class)
                .defaultValue(3)
                .range(1, 20)
                .description("Slowness duration in seconds after pull")
                .customIcon(Material.LEAD)
                .build());


        
        plugin.getSettingRegistry().register(SettingDefinition.builder("loadout.hider-max-items", SettingType.INTEGER, Integer.class)
                .defaultValue(3)
                .range(1, 10)
                .description("Maximum number of items hiders can select in their loadout")
                .customIcon(Material.CHEST)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("loadout.seeker-max-items", SettingType.INTEGER, Integer.class)
                .defaultValue(4)
                .range(1, 10)
                .description("Maximum number of items seekers can select in their loadout")
                .customIcon(Material.CHEST)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("loadout.hider-max-tokens", SettingType.INTEGER, Integer.class)
                .defaultValue(12)
                .range(1, 50)
                .description("Maximum tokens hiders can spend on items")
                .customIcon(Material.GOLD_BLOCK)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("loadout.seeker-max-tokens", SettingType.INTEGER, Integer.class)
                .defaultValue(12)
                .range(1, 50)
                .description("Maximum tokens seekers can spend on items")
                .customIcon(Material.GOLD_BLOCK)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("loadout.token-cost-common", SettingType.INTEGER, Integer.class)
                .defaultValue(1)
                .range(1, 20)
                .description("Token cost for Common rarity items")
                .customIcon(Material.IRON_NUGGET)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("loadout.token-cost-uncommon", SettingType.INTEGER, Integer.class)
                .defaultValue(2)
                .range(1, 20)
                .description("Token cost for Uncommon rarity items")
                .customIcon(Material.LAPIS_LAZULI)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("loadout.token-cost-rare", SettingType.INTEGER, Integer.class)
                .defaultValue(4)
                .range(1, 20)
                .description("Token cost for Rare rarity items")
                .customIcon(Material.DIAMOND)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("loadout.token-cost-epic", SettingType.INTEGER, Integer.class)
                .defaultValue(6)
                .range(1, 20)
                .description("Token cost for Epic rarity items")
                .customIcon(Material.AMETHYST_BLOCK)
                .build());

        plugin.getSettingRegistry().register(SettingDefinition.builder("loadout.token-cost-legendary", SettingType.INTEGER, Integer.class)
                .defaultValue(10)
                .range(1, 30)
                .description("Token cost for Legendary rarity items")
                .customIcon(Material.NETHERITE_BLOCK)
                .build());

    }
}

