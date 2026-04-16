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
