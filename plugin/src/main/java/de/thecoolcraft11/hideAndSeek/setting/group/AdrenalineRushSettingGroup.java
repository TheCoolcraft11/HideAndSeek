package de.thecoolcraft11.hideAndSeek.setting.group;

import de.thecoolcraft11.hideAndSeek.setting.spec.BooleanSettingSpec;
import de.thecoolcraft11.hideAndSeek.setting.spec.FloatSettingSpec;
import de.thecoolcraft11.hideAndSeek.setting.spec.IntegerSettingSpec;
import de.thecoolcraft11.hideAndSeek.setting.spec.SettingSpec;
import org.bukkit.Material;

import java.util.List;

public final class AdrenalineRushSettingGroup implements SettingGroup {

    @Override
    public List<SettingSpec> settings() {
        return List.of(


                new BooleanSettingSpec(
                        "adrenaline-rush.enabled", true,
                        "Master switch for the Adrenaline Rush comeback mechanic",
                        Material.BEACON, true),


                new BooleanSettingSpec(
                        "adrenaline-rush.border-warning-enabled", true,
                        "Show the fake world-border warning effect while Adrenaline Rush is active",
                        Material.BARRIER, true),
                new FloatSettingSpec(
                        "adrenaline-rush.border-warning-strength", 1.0f, 0.1f, 5.0f,
                        "Intensity of the fake border warning effect (higher = more vignette)",
                        Material.REDSTONE),


                new BooleanSettingSpec(
                        "adrenaline-rush.hider.enabled", true,
                        "Enable Adrenaline Rush for hiders",
                        Material.PLAYER_HEAD, true),
                new IntegerSettingSpec(
                        "adrenaline-rush.hider.trigger-hider-count", 1, 1, 10,
                        "Trigger hider Adrenaline Rush when this many (or fewer) hiders remain",
                        Material.PLAYER_HEAD),
                new IntegerSettingSpec(
                        "adrenaline-rush.hider.duration-seconds", 30, 5, 300,
                        "How long hider Adrenaline Rush lasts (seconds)",
                        Material.CLOCK),


                new BooleanSettingSpec(
                        "adrenaline-rush.hider.speed-enabled", true,
                        "Grant Speed to the last hider(s) during Adrenaline Rush",
                        Material.SUGAR, true),
                new IntegerSettingSpec(
                        "adrenaline-rush.hider.speed-amplifier", 1, 0, 4,
                        "Speed amplifier for hider Adrenaline Rush (0 = Speed I, 1 = Speed II, …)",
                        Material.SUGAR),

                new BooleanSettingSpec(
                        "adrenaline-rush.hider.resistance-enabled", true,
                        "Grant Resistance to the last hider(s) during Adrenaline Rush",
                        Material.SHIELD, true),
                new IntegerSettingSpec(
                        "adrenaline-rush.hider.resistance-amplifier", 0, 0, 4,
                        "Resistance amplifier for hider Adrenaline Rush (0 = Resistance I, …)",
                        Material.SHIELD),

                new BooleanSettingSpec(
                        "adrenaline-rush.hider.regeneration-enabled", false,
                        "Grant Regeneration to the last hider(s) during Adrenaline Rush",
                        Material.GLISTERING_MELON_SLICE, true),
                new IntegerSettingSpec(
                        "adrenaline-rush.hider.regeneration-amplifier", 0, 0, 4,
                        "Regeneration amplifier for hider Adrenaline Rush (0 = Regen I, …)",
                        Material.GLISTERING_MELON_SLICE),


                new BooleanSettingSpec(
                        "adrenaline-rush.seeker.enabled", true,
                        "Enable Adrenaline Rush for seekers",
                        Material.ENDER_EYE, true),
                new IntegerSettingSpec(
                        "adrenaline-rush.seeker.trigger-time-threshold", 60, 5, 600,
                        "Trigger seeker Adrenaline Rush when this many seconds or fewer remain on the timer",
                        Material.CLOCK),
                new IntegerSettingSpec(
                        "adrenaline-rush.seeker.trigger-hider-count", 2, 1, 20,
                        "Trigger seeker Adrenaline Rush only when at least this many hiders are still alive",
                        Material.PLAYER_HEAD),
                new IntegerSettingSpec(
                        "adrenaline-rush.seeker.duration-seconds", 30, 5, 300,
                        "How long seeker Adrenaline Rush lasts (seconds)",
                        Material.CLOCK),


                new BooleanSettingSpec(
                        "adrenaline-rush.seeker.speed-enabled", true,
                        "Grant Speed to seekers during Adrenaline Rush",
                        Material.FEATHER, true),
                new IntegerSettingSpec(
                        "adrenaline-rush.seeker.speed-amplifier", 0, 0, 4,
                        "Speed amplifier for seeker Adrenaline Rush (0 = Speed I, …)",
                        Material.FEATHER),

                new BooleanSettingSpec(
                        "adrenaline-rush.seeker.haste-enabled", true,
                        "Grant Haste to seekers during Adrenaline Rush",
                        Material.GOLDEN_PICKAXE, true),
                new IntegerSettingSpec(
                        "adrenaline-rush.seeker.haste-amplifier", 0, 0, 4,
                        "Haste amplifier for seeker Adrenaline Rush (0 = Haste I, …)",
                        Material.GOLDEN_PICKAXE),

                new BooleanSettingSpec(
                        "adrenaline-rush.seeker.regeneration-enabled", false,
                        "Grant Regeneration to seekers during Adrenaline Rush",
                        Material.GLISTERING_MELON_SLICE, true),
                new IntegerSettingSpec(
                        "adrenaline-rush.seeker.regeneration-amplifier", 0, 0, 4,
                        "Regeneration amplifier for seeker Adrenaline Rush (0 = Regen I, …)",
                        Material.GLISTERING_MELON_SLICE)
        );
    }
}