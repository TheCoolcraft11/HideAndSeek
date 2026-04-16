package de.thecoolcraft11.hideAndSeek.setting.group;

import de.thecoolcraft11.hideAndSeek.setting.spec.BooleanSettingSpec;
import de.thecoolcraft11.hideAndSeek.setting.spec.IntegerSettingSpec;
import de.thecoolcraft11.hideAndSeek.setting.spec.LongSettingSpec;
import de.thecoolcraft11.hideAndSeek.setting.spec.SettingSpec;
import org.bukkit.Material;

import java.util.List;

public final class PerkSeekerRandomSwapSettingGroup implements SettingGroup {

    @Override
    public List<SettingSpec> settings() {
        return List.of(new BooleanSettingSpec("perks.perk.seeker_random_swap.enabled", true, "Enable Random Swap",
                        Material.WARPED_FUNGUS_ON_A_STICK, true),
                new IntegerSettingSpec("perks.perk.seeker_random_swap.cost", 250, 0, 10000, "Cost of Random Swap",
                        Material.EMERALD),
                new LongSettingSpec("perks.perk.seeker_random_swap.purchase-cooldown-ticks", 400L, 0L, 20000L,
                        "Purchase cooldown for Random Swap", Material.CLOCK),
                new IntegerSettingSpec("perks.perk.seeker_random_swap.blindness-ticks", 40, 0, 2000,
                        "Blindness duration for Random Swap", Material.INK_SAC),
                new BooleanSettingSpec("perks.perk.seeker_random_swap.exclude-hidden", true,
                        "Exclude hidden hiders from Random Swap", Material.TRIPWIRE_HOOK, true),
                new BooleanSettingSpec("perks.perk.seeker_random_swap.finite", false, "Whether Random Swap is finite",
                        Material.BARRIER, true));
    }
}
