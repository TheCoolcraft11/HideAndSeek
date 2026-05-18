package de.thecoolcraft11.hideAndSeek.setting.group;

import de.thecoolcraft11.hideAndSeek.setting.spec.BooleanSettingSpec;
import de.thecoolcraft11.hideAndSeek.setting.spec.DoubleSettingSpec;
import de.thecoolcraft11.hideAndSeek.setting.spec.IntegerSettingSpec;
import de.thecoolcraft11.hideAndSeek.setting.spec.SettingSpec;
import org.bukkit.Material;

import java.util.List;

@SuppressWarnings("unused")
public final class PerkSeekerSwordBounceSettingGroup implements SettingGroup {

    @Override
    public List<SettingSpec> settings() {
        return List.of(
                new BooleanSettingSpec("perks.perk.seeker_sword_bounce.enabled", true, "Enable Sword Bounce",
                        Material.RABBIT_FOOT, true),
                new IntegerSettingSpec("perks.perk.seeker_sword_bounce.cost", 460, 0, 10000,
                        "Cost of Sword Bounce", Material.EMERALD),
                new IntegerSettingSpec("perks.perk.seeker_sword_bounce.max-bounces", 2, 0, 10,
                        "How many extra hiders the sword can bounce to after the first hit", Material.ARROW),
                new DoubleSettingSpec("perks.perk.seeker_sword_bounce.search-range", 24.0d, 1.0d, 128.0d,
                        "Maximum range used when looking for the next hider to bounce to", Material.SPYGLASS),
                new DoubleSettingSpec("perks.perk.seeker_sword_bounce.speed-retention", 0.85d, 0.1d, 1.0d,
                        "How much speed the sword keeps after each bounce", Material.RABBIT_FOOT),
                new DoubleSettingSpec("perks.perk.seeker_sword_bounce.damage-multiplier", 0.80d, 0.1d, 1.0d,
                        "Damage multiplier applied to each hit after the first", Material.IRON_SWORD),
                new BooleanSettingSpec("perks.perk.seeker_sword_bounce.finite", true,
                        "Whether Sword Bounce is finite", Material.BARRIER, true));
    }
}

