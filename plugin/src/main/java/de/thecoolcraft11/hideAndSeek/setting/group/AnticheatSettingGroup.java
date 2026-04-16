package de.thecoolcraft11.hideAndSeek.setting.group;

import de.thecoolcraft11.hideAndSeek.setting.spec.BooleanSettingSpec;
import de.thecoolcraft11.hideAndSeek.setting.spec.DoubleSettingSpec;
import de.thecoolcraft11.hideAndSeek.setting.spec.IntegerSettingSpec;
import de.thecoolcraft11.hideAndSeek.setting.spec.SettingSpec;
import org.bukkit.Material;

import java.util.List;

public final class AnticheatSettingGroup implements SettingGroup {

    @Override
    public List<SettingSpec> settings() {
        return List.of(new BooleanSettingSpec("anticheat.enabled", true,
                        "Master switch for anti-cheat seeker visibility filtering", Material.SHIELD, true),
                new BooleanSettingSpec("anticheat.hiding.filter.enabled", true,
                        "During HIDING: hide all hider entities from seekers while keeping tab entries",
                        Material.ENDER_EYE, true), new BooleanSettingSpec("anticheat.seeking.filter.enabled", true,
                        "During SEEKING: seekers only see nearby hiders and never hidden BLOCK-mode hiders",
                        Material.SPYGLASS, true),
                new DoubleSettingSpec("anticheat.seeking.visibility-range", 12.0, 1.0, 128.0,
                        "Distance in blocks at which seekers can see hiders during SEEKING", Material.COMPASS),
                new BooleanSettingSpec("anticheat.seeking.line-of-sight.enabled", true,
                        "Allow seeker line-of-sight reveal outside base anti-cheat visibility range", Material.SPYGLASS,
                        true), new DoubleSettingSpec("anticheat.seeking.line-of-sight.range", 64.0, 8.0, 256.0,
                        "Maximum range for line-of-sight reveal checks", Material.COMPASS),
                new DoubleSettingSpec("anticheat.seeking.line-of-sight.fov", 60.0, 5.0, 90.0,
                        "Seeker view angle in degrees for line-of-sight reveal checks", Material.ENDER_EYE),
                new BooleanSettingSpec("anticheat.hider-camping.enabled", true,
                        "Whether to prevent hiders from camping in the same spot", Material.CAMPFIRE, true),
                new IntegerSettingSpec("anticheat.hider-camping.max-duration", 90, 5, 600,
                        "How long a hider can stay in the same spot before being punished for camping", Material.CLOCK),
                new IntegerSettingSpec("anticheat.hider-camping.warn-time", 15, 0, 300,
                        "Time in seconds before camping punishment when a warning is issued to the hider",
                        Material.CLOCK), new DoubleSettingSpec("anticheat.hider-camping.spot-radius", 2.5, 0.25, 8.0,
                        "Horizontal radius (blocks) treated as the same camping spot", Material.TARGET),
                new DoubleSettingSpec("anticheat.hider-camping.damage-amount", 1.0, 0.5, 10.0,
                        "Damage dealt each punishment tick while the hider keeps camping", Material.IRON_SWORD),
                new IntegerSettingSpec("anticheat.hider-camping.damage-cooldown-ticks", 20, 1, 200,
                        "Ticks between repeated camping punishment damage", Material.REPEATER));
    }
}
