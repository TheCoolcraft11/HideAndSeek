package de.thecoolcraft11.hideAndSeek.perk;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.perk.definition.PerkDefinition;
import de.thecoolcraft11.hideAndSeek.perk.definition.PerkTarget;
import de.thecoolcraft11.hideAndSeek.perk.impl.hider.*;
import de.thecoolcraft11.hideAndSeek.perk.impl.seeker.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class PerkRegistry {

    private final HideAndSeek plugin;
    private final List<PerkDefinition> allPerks = new ArrayList<>();
    private List<PerkDefinition> roundHiderPerks = List.of();
    private List<PerkDefinition> roundSeekerPerks = List.of();

    public PerkRegistry(HideAndSeek plugin) {
        this.plugin = plugin;
        registerAll();
    }

    private void registerAll() {
        allPerks.add(new AdaptiveSpeedPerk());
        allPerks.add(new SeekerWarningPerk());
        allPerks.add(new ExtraLifePerk());
        allPerks.add(new ShadowStepPerk());
        allPerks.add(new CamouflagePerk());
        allPerks.add(new DoubleJumpPerk());
        allPerks.add(new TrapSensePerk());

        allPerks.add(new DeathZonePerk());
        allPerks.add(new RandomSwapPerk());
        allPerks.add(new MapTeleportPerk());
        allPerks.add(new RelocatePerk());
        allPerks.add(new ElytraRushPerk());
        allPerks.add(new ProximityMeterPerk());
        allPerks.add(new ScentTrailPerk());
    }

    public void selectRoundPerks() {
        int n = plugin.getSettingRegistry().get("perks.perks-per-round", 3);
        roundHiderPerks = selectRandom(PerkTarget.HIDER, n);
        roundSeekerPerks = selectRandom(PerkTarget.SEEKER, n);
    }

    private List<PerkDefinition> selectRandom(PerkTarget target, int count) {
        List<PerkDefinition> pool = allPerks.stream()
                .filter(p -> p.getTarget() == target)
                .filter(p -> plugin.getSettingRegistry().get("perks.perk." + p.getId() + ".enabled", true))
                .collect(Collectors.toCollection(ArrayList::new));
        Collections.shuffle(pool);
        return pool.subList(0, Math.min(count, pool.size()));
    }

    public List<PerkDefinition> getRoundPerksForHider() {
        return roundHiderPerks;
    }

    public List<PerkDefinition> getRoundPerksForSeeker() {
        return roundSeekerPerks;
    }

    public List<PerkDefinition> getAllPerks() {
        return Collections.unmodifiableList(allPerks);
    }
}

