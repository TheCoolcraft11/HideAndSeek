package de.thecoolcraft11.hideAndSeek.setting.group;

import de.thecoolcraft11.hideAndSeek.setting.spec.DoubleSettingSpec;
import de.thecoolcraft11.hideAndSeek.setting.spec.IntegerSettingSpec;
import de.thecoolcraft11.hideAndSeek.setting.spec.SettingSpec;
import org.bukkit.Material;

import java.util.List;

public final class PointsSettingGroup implements SettingGroup {

    @Override
    public List<SettingSpec> settings() {
        return List.of(new IntegerSettingSpec("points.tracking.interval-seconds", 1, 1, 5,
                        "Global update interval for dynamic point tracking", Material.CLOCK),
                new IntegerSettingSpec("points.hider.survival.amount", 5, 0, 1000,
                        "Points awarded per hider survival tick", Material.EMERALD),
                new IntegerSettingSpec("points.hider.survival.interval-seconds", 20, 1, 300,
                        "Seconds between hider survival tick awards", Material.CLOCK),
                new IntegerSettingSpec("points.hider.survival.start-delay-seconds", 20, 0, 300,
                        "Delay before first hider survival tick award", Material.CLOCK),
                new IntegerSettingSpec("points.hider.proximity.amount-per-second", 2, 0, 1000,
                        "Points per second for hiders near seekers", Material.SCULK_SENSOR),
                new DoubleSettingSpec("points.hider.proximity.range", 8.0, 1.0, 64.0, "Range for hider proximity bonus",
                        Material.BLAZE_POWDER),
                new IntegerSettingSpec("points.hider.near-miss.amount", 50, 0, 1000, "Points for escaping a near miss",
                        Material.TOTEM_OF_UNDYING),
                new DoubleSettingSpec("points.hider.near-miss.range", 3.0, 0.5, 16.0,
                        "Distance that counts as near-miss danger", Material.BLAZE_POWDER),
                new IntegerSettingSpec("points.hider.near-miss.escape-seconds", 4, 1, 60,
                        "Seconds to survive after leaving near-miss danger", Material.CLOCK),
                new IntegerSettingSpec("points.hider.taunt.small", 25, 0, 1000, "Points for small hider taunts",
                        Material.CAT_SPAWN_EGG),
                new IntegerSettingSpec("points.hider.taunt.large", 75, 0, 2000, "Points for large hider taunts",
                        Material.FIREWORK_ROCKET),
                new IntegerSettingSpec("points.hider.sharpshooter.amount", 20, 0, 1000,
                        "Points for each hider crossbow hit", Material.CROSSBOW),
                new IntegerSettingSpec("points.hider.survivor.amount", 100, 0, 2000,
                        "Round-end points for surviving hiders", Material.SHIELD),
                new IntegerSettingSpec("points.hider.special.ghost", 200, 0, 5000,
                        "Special bonus for never being utility-spotted", Material.GHAST_TEAR),
                new IntegerSettingSpec("points.hider.special.distractor", 200, 0, 5000,
                        "Special bonus for most hider proximity time", Material.BELL),
                new IntegerSettingSpec("points.seeker.active-hunter.amount-per-second", 2, 0, 1000,
                        "Points per second for seekers near hiders", Material.IRON_SWORD),
                new DoubleSettingSpec("points.seeker.active-hunter.range", 16.0, 1.0, 64.0,
                        "Range for active hunter points", Material.BLAZE_POWDER),
                new IntegerSettingSpec("points.seeker.utility-success.amount", 40, 0, 2000,
                        "Points for successful seeker utility usage", Material.ENDER_EYE),
                new IntegerSettingSpec("points.seeker.interception.amount", 15, 0, 1000,
                        "Points for damaging a hider without killing", Material.IRON_SWORD),
                new IntegerSettingSpec("points.seeker.kill.amount", 300, 0, 5000, "Points for eliminating a hider",
                        Material.NETHERITE_SWORD), new IntegerSettingSpec("points.seeker.assist.amount", 100, 0, 5000,
                        "Points for assist on hider elimination", Material.CHAINMAIL_CHESTPLATE),
                new DoubleSettingSpec("points.seeker.assist.range", 16.0, 1.0, 64.0,
                        "Assist range when another seeker gets the kill", Material.BLAZE_POWDER),
                new IntegerSettingSpec("points.seeker.special.bloodhound", 200, 0, 5000,
                        "Special bonus for most captures", Material.WOLF_SPAWN_EGG),
                new IntegerSettingSpec("points.seeker.first-blood.amount", 100, 0, 5000,
                        "Bonus for first kill of the round", Material.REDSTONE),
                new IntegerSettingSpec("points.seeker.environmental-elimination.amount", 50, 0, 5000,
                        "Points awarded to all seekers when hider dies to camping or world border",
                        Material.LIGHTNING_ROD));
    }
}
