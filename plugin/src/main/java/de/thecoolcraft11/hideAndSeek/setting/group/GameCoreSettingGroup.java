package de.thecoolcraft11.hideAndSeek.setting.group;

import de.thecoolcraft11.hideAndSeek.model.GameModeEnum;
import de.thecoolcraft11.hideAndSeek.model.GameStyleEnum;
import de.thecoolcraft11.hideAndSeek.model.MapInfoDisplayMode;
import de.thecoolcraft11.hideAndSeek.setting.SettingIconHelper;
import de.thecoolcraft11.hideAndSeek.setting.spec.*;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;

public final class GameCoreSettingGroup implements SettingGroup {

    @Override
    public List<SettingSpec> settings() {
        return List.of(
                new EnumSettingSpec<>("game.mode", GameModeEnum.class, GameModeEnum.NORMAL, "Gamemode of the game",
                        Material.COMMAND_BLOCK,
                        Map.of(GameModeEnum.NORMAL, Material.PLAYER_HEAD, GameModeEnum.BLOCK, Material.COBBLESTONE,
                                GameModeEnum.SMALL, Material.IRON_NUGGET)),
                new EnumSettingSpec<>("game.style", GameStyleEnum.class, GameStyleEnum.SPECTATOR,
                        "Gamemode of the game", Material.NETHER_STAR,
                        Map.of(GameStyleEnum.SPECTATOR, Material.ENDER_EYE, GameStyleEnum.INVASION,
                                Material.SUSPICIOUS_STEW, GameStyleEnum.INFINITE, Material.BLAZE_POWDER)),
                new IntegerSettingSpec("game.hiding-time", 60, 10, 600, "Hiding phase duration in seconds",
                        Material.CLOCK),
                new IntegerSettingSpec("game.seeking-time", 300, 60, 1800, "Seeking phase duration in seconds",
                        Material.CLOCK), new BooleanSettingSpec("game.hider-invisibility", false,
                        "Grant hiders invisibility during hiding phase", Material.POTION, true),
                new BooleanSettingSpec("game.world-border.damage-hiders-outside", true,
                        "Damage hiders when they go outside the world border", Material.BARRIER, true),
                new IntegerSettingSpec("game.world-border.damage-delay-seconds", 10, 1, 60,
                        "Seconds a hider must be outside the border before taking damage", Material.CLOCK),
                new DoubleSettingSpec("game.world-border.damage-amount", 2.0, 0.5, 20.0,
                        "Damage per tick dealt to hiders outside the border", Material.REDSTONE),
                new IntegerSettingSpec("game.world-border.damage-cooldown-ticks", 20, 1, 100,
                        "Ticks between damage hits (20 = 1 second)", Material.GOLDEN_APPLE),
                new BooleanSettingSpec("game.environmental-damage.drowning.enabled", true,
                        "Apply plugin damage if a hider takes drowning damage for too long", Material.WATER_BUCKET,
                        true),
                new IntegerSettingSpec("game.environmental-damage.drowning.safe-duration-seconds", 8, 0, 120,
                        "Seconds drowning damage is allowed before plugin punishment starts", Material.CLOCK),
                new DoubleSettingSpec("game.environmental-damage.drowning.damage-amount", 1.0, 0.1, 20.0,
                        "Damage per punishment hit for drowning", Material.PRISMARINE_CRYSTALS),
                new IntegerSettingSpec("game.environmental-damage.drowning.damage-cooldown-ticks", 20, 1, 100,
                        "Ticks between drowning punishment hits", Material.GOLD_NUGGET),
                new BooleanSettingSpec("game.environmental-damage.fire.enabled", true,
                        "Apply plugin damage if a hider burns for too long", Material.FLINT_AND_STEEL, true),
                new IntegerSettingSpec("game.environmental-damage.fire.safe-duration-seconds", 10, 0, 120,
                        "Seconds fire damage is allowed before plugin punishment starts", Material.CLOCK),
                new DoubleSettingSpec("game.environmental-damage.fire.damage-amount", 1.0, 0.1, 20.0,
                        "Damage per punishment hit for fire", Material.BLAZE_POWDER),
                new IntegerSettingSpec("game.environmental-damage.fire.damage-cooldown-ticks", 20, 1, 100,
                        "Ticks between fire punishment hits", Material.GOLD_NUGGET),
                new BooleanSettingSpec("game.environmental-damage.lava.enabled", true,
                        "Apply plugin damage if a hider stays in lava for too long", Material.LAVA_BUCKET, true),
                new IntegerSettingSpec("game.environmental-damage.lava.safe-duration-seconds", 6, 0, 120,
                        "Seconds lava damage is allowed before plugin punishment starts", Material.CLOCK),
                new DoubleSettingSpec("game.environmental-damage.lava.damage-amount", 1.5, 0.1, 20.0,
                        "Damage per punishment hit for lava", Material.MAGMA_BLOCK),
                new IntegerSettingSpec("game.environmental-damage.lava.damage-cooldown-ticks", 15, 1, 100,
                        "Ticks between lava punishment hits", Material.GOLD_NUGGET),
                new BooleanSettingSpec("game.environmental-damage.suffocation.enabled", true,
                        "Apply plugin damage if a hider suffocates for too long", Material.SANDSTONE, true),
                new IntegerSettingSpec("game.environmental-damage.suffocation.safe-duration-seconds", 6, 0, 120,
                        "Seconds suffocation damage is allowed before plugin punishment starts", Material.CLOCK),
                new DoubleSettingSpec("game.environmental-damage.suffocation.damage-amount", 1.0, 0.1, 20.0,
                        "Damage per punishment hit for suffocation", Material.SAND),
                new IntegerSettingSpec("game.environmental-damage.suffocation.damage-cooldown-ticks", 20, 1, 100,
                        "Ticks between suffocation punishment hits", Material.GOLD_NUGGET),
                new BooleanSettingSpec("game.environmental-damage.freezing.enabled", true,
                        "Apply plugin damage if a hider keeps freezing for too long", Material.POWDER_SNOW_BUCKET,
                        true),
                new IntegerSettingSpec("game.environmental-damage.freezing.safe-duration-seconds", 10, 0, 120,
                        "Seconds freeze damage is allowed before plugin punishment starts", Material.CLOCK),
                new DoubleSettingSpec("game.environmental-damage.freezing.damage-amount", 1.0, 0.1, 20.0,
                        "Damage per punishment hit for freezing", Material.SNOWBALL),
                new IntegerSettingSpec("game.environmental-damage.freezing.damage-cooldown-ticks", 20, 1, 100,
                        "Ticks between freeze punishment hits", Material.GOLD_NUGGET),
                new BooleanSettingSpec("game.environmental-damage.hot-floor.enabled", true,
                        "Apply plugin damage if a hider stays on hot floor blocks for too long", Material.MAGMA_BLOCK,
                        true),
                new IntegerSettingSpec("game.environmental-damage.hot-floor.safe-duration-seconds", 8, 0, 120,
                        "Seconds hot-floor damage is allowed before plugin punishment starts", Material.CLOCK),
                new DoubleSettingSpec("game.environmental-damage.hot-floor.damage-amount", 1.0, 0.1, 20.0,
                        "Damage per punishment hit for hot floor", Material.CAMPFIRE),
                new IntegerSettingSpec("game.environmental-damage.hot-floor.damage-cooldown-ticks", 20, 1, 100,
                        "Ticks between hot-floor punishment hits", Material.GOLD_NUGGET),
                new BooleanSettingSpec("game.environmental-damage.contact.enabled", true,
                        "Apply plugin damage if a hider takes cactus/berry contact damage for too long",
                        Material.SWEET_BERRIES, true),
                new IntegerSettingSpec("game.environmental-damage.contact.safe-duration-seconds", 8, 0, 120,
                        "Seconds contact damage is allowed before plugin punishment starts", Material.CLOCK),
                new DoubleSettingSpec("game.environmental-damage.contact.damage-amount", 1.0, 0.1, 20.0,
                        "Damage per punishment hit for contact hazards", Material.CACTUS),
                new IntegerSettingSpec("game.environmental-damage.contact.damage-cooldown-ticks", 20, 1, 100,
                        "Ticks between contact punishment hits", Material.GOLD_NUGGET),
                new DoubleSettingSpec("game.small-mode.hider-size", 0.5, 0.0, 2.0,
                        "Size scale for SMALL mode hiders (0.1 = tiny, 1.0 = normal)", Material.SLIME_BALL),
                new BooleanSettingSpec("game.team-distribution.random", true,
                        "Enable random distribution of players into hider/seeker teams", Material.PLAYER_HEAD, true),
                new BooleanSettingSpec("game.voting.game-mode-enabled", true,
                        "Allow players to vote for gamemodes in lobby", Material.COMMAND_BLOCK, true),
                new BooleanSettingSpec("game.voting.map-enabled", true, "Allow players to vote for maps in lobby",
                        Material.MAP, true), new BooleanSettingSpec("game.voting.role-preference-enabled", false,
                        "Allow players to vote for a preferred hider/seeker role in lobby", Material.PLAYER_HEAD, true),
                new BooleanSettingSpec("game.voting.show-counts", true, "Show current vote counts in the voting GUI",
                        Material.PAPER, true), new BooleanSettingSpec("game.lobby.readiness-enabled", true,
                        "Require players to ready up before the round can start", Material.LIME_STAINED_GLASS_PANE,
                        true), new BooleanSettingSpec("game.maps.use-preferred-modes", true,
                        "Only select maps that have the current game mode in their preferred modes list", Material.MAP,
                        true), new BooleanSettingSpec("game.maps.use-map-timings", true,
                        "Use hiding/seeking times from map config if available, otherwise use global settings",
                        Material.CLOCK, true), new BooleanSettingSpec("game.maps.use-map-seeker-count", true,
                        "Use seeker configuration from map config if available, otherwise use global settings",
                        Material.IRON_SWORD, true), new BooleanSettingSpec("game.maps.use-map-player-limits", true,
                        "Use player count recommendations from map config if available", Material.PLAYER_HEAD, true),
                new BooleanSettingSpec("game.maps.use-map-setting-overrides", true,
                        "Apply map setting-overrides from maps.yml during a round", Material.COMMAND_BLOCK, true),
                new BooleanSettingSpec("game.maps.show-round-start-map-info-title", true,
                        "Show a map info title when the HIDING phase starts", Material.NAME_TAG, true),
                new EnumSettingSpec<>("game.maps.round-start-map-info-display-mode", MapInfoDisplayMode.class,
                        MapInfoDisplayMode.NAME_AUTHOR_DESCRIPTION,
                        "Which map info fields should be shown in the HIDING start title", Material.WRITABLE_BOOK,
                        Map.of(MapInfoDisplayMode.NAME_ONLY, Material.MAP, MapInfoDisplayMode.NAME_AND_AUTHOR,
                                Material.NAME_TAG, MapInfoDisplayMode.NAME_AUTHOR_DESCRIPTION, Material.BOOK)),
                new StringSettingSpec("game.teams.fixed-seeker-team", "",
                        "Fixed seeker team (leave empty for random). Set to a team name to always use that team as seekers",
                        Material.WHITE_BANNER, (plugin, value) -> {
                    String teamName = value instanceof String stringValue ? stringValue.trim() : "";
                    if (teamName.isEmpty()) {
                        return new ItemStack(Material.WHITE_BANNER);
                    }
                    var team = plugin.getTeamManager().getTeam(teamName);
                    if (team == null) {
                        return new ItemStack(Material.WHITE_BANNER);
                    }
                    TextColor textColor = team.color();
                    DyeColor dyeColor = new SettingIconHelper().mapToNearestDye(textColor);
                    Material banner = Material.valueOf(dyeColor.name() + "_BANNER");
                    return new ItemStack(banner);
                }), new StringSettingSpec("game.teams.fixed-hider-team", "Hiders",
                        "Fixed hider team. Set to a team name to assign new player to this team automatically",
                        Material.WHITE_BANNER, (plugin, value) -> {
                    String teamName = value instanceof String stringValue ? stringValue.trim() : "";
                    if (teamName.isEmpty()) {
                        return new ItemStack(Material.WHITE_BANNER);
                    }
                    var team = plugin.getTeamManager().getTeam(teamName);
                    if (team == null) {
                        return new ItemStack(Material.WHITE_BANNER);
                    }
                    TextColor textColor = team.color();
                    DyeColor dyeColor = new SettingIconHelper().mapToNearestDye(textColor);
                    Material banner = Material.valueOf(dyeColor.name() + "_BANNER");
                    return new ItemStack(banner);
                }), new IntegerSettingSpec("game.teams.seeker-count", 1, 1, 10,
                        "Number of seekers (if random distribution is enabled)", Material.IRON_SWORD,
                        (plugin, value) -> {
                            int count = value instanceof Number number ? number.intValue() : 1;
                            if (count <= 1) {
                                return new ItemStack(Material.IRON_SWORD);
                            }
                            if (count <= 3) {
                                return new ItemStack(Material.DIAMOND_SWORD);
                            }
                            return new ItemStack(Material.NETHERITE_SWORD);
                        }), new IntegerSettingSpec("game.hiders.health", 20, 1, 20, "Health of hiders (in half-hearts)",
                        Material.GOLDEN_APPLE, (plugin, value) -> {
                    int health = value instanceof Number number ? number.intValue() : 20;
                    if (health <= 6) {
                        return new ItemStack(Material.POISONOUS_POTATO);
                    }
                    if (health <= 14) {
                        return new ItemStack(Material.APPLE);
                    }
                    return new ItemStack(Material.GOLDEN_APPLE);
                }), new FloatSettingSpec("game.block-form.view-height", 0.1f, 0f, 1.5f,
                        "View Height of player when they hide in a block", Material.LADDER));
    }
}
