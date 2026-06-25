package de.thecoolcraft11.hideAndSeek.setting.group;

import de.thecoolcraft11.hideAndSeek.setting.spec.BooleanSettingSpec;
import de.thecoolcraft11.hideAndSeek.setting.spec.DoubleSettingSpec;
import de.thecoolcraft11.hideAndSeek.setting.spec.SettingSpec;
import org.bukkit.Material;

import java.util.List;

public final class BlindnessAntiCheatSettingGroup implements SettingGroup {

    @Override
    public List<SettingSpec> settings() {
        return List.of(
                new BooleanSettingSpec("anticheat.blindness.enabled", true,
                        "When enabled, blind hidden hiders only see players within range",
                        Material.GLASS, true),
                new DoubleSettingSpec("anticheat.blindness.range", 2.0, 0.5, 16.0,
                        "Max distance in blocks that blind hidden hiders can see other players",
                        Material.COMPASS)
        );
    }
}
