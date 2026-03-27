package de.thecoolcraft11.hideAndSeek.model;

import de.thecoolcraft11.hideAndSeek.items.hider.*;
import de.thecoolcraft11.hideAndSeek.items.seeker.*;
import de.thecoolcraft11.hideAndSeek.nms.NmsAdapter;
import de.thecoolcraft11.hideAndSeek.nms.NmsCapabilities;

import java.util.Set;

public enum LoadoutItemType {

    FIRECRACKER(true, false, ItemRarity.COMMON, ExplosionItem.ID),
    CAT_SOUND(true, false, ItemRarity.COMMON, SoundItem.ID),
    RANDOM_BLOCK(true, false, ItemRarity.COMMON, RandomBlockItem.ID),
    SPEED_BOOST(true, false, ItemRarity.COMMON, SpeedBoostItem.ID),
    TRACKER_CROSSBOW(true, false, ItemRarity.COMMON, TrackerCrossbowItem.ID),
    KNOCKBACK_STICK(true, false, ItemRarity.COMMON, KnockbackStickItem.ID),
    BLOCK_SWAP(true, false, ItemRarity.RARE, BlockSwapItem.ID),
    BIG_FIRECRACKER(true, false, ItemRarity.RARE, BigFirecrackerItem.ID),
    FIREWORK_ROCKET(true, false, ItemRarity.UNCOMMON, FireworkRocketItem.ID),
    MEDKIT(true, false, ItemRarity.RARE, MedkitItem.ID),
    TOTEM_OF_UNDYING(true, false, ItemRarity.LEGENDARY, TotemItem.ID),
    INVISIBILITY_CLOAK(true, false, ItemRarity.EPIC, InvisibilityCloakItem.ID),
    SLOWNESS_BALL(true, false, ItemRarity.UNCOMMON, SlownessBallItem.ID),
    SMOKE_BOMB(true, false, ItemRarity.UNCOMMON, SmokeBombItem.ID),
    GHOST_ESSENCE(true, false, ItemRarity.RARE, GhostEssenceItem.ID, NmsCapabilities.CLIENT_GAMEMODE_SPOOFING, NmsCapabilities.MOB_PATHFINDING),


    GRAPPLING_HOOK(false, true, ItemRarity.COMMON, GrapplingHookItem.ID),
    INK_SPLASH(false, true, ItemRarity.RARE, InkSplashItem.ID),
    LIGHTNING_FREEZE(false, true, ItemRarity.LEGENDARY, LightningFreezeItem.ID),
    GLOWING_COMPASS(false, true, ItemRarity.EPIC, GlowingCompassItem.ID),
    CURSE_SPELL(false, true, ItemRarity.UNCOMMON, CurseSpellItem.ID),
    BLOCK_RANDOMIZER(false, true, ItemRarity.EPIC, BlockRandomizerItem.ID),
    CHAIN_PULL(false, true, ItemRarity.UNCOMMON, ChainPullItem.ID),
    PROXIMITY_SENSOR(false, true, ItemRarity.RARE, ProximitySensorItem.ID),
    CAMERA(false, true, ItemRarity.EPIC, CameraItem.ID, NmsCapabilities.CLIENT_GAMEMODE_SPOOFING, NmsCapabilities.CLIENT_ENTITY_GLOWING, NmsCapabilities.CLIENT_ENTITY_SPAWNING),
    CAGE_TRAP(false, true, ItemRarity.RARE, CageTrapItem.ID),
    ;

    private final boolean forHiders;
    private final boolean forSeekers;
    private final ItemRarity rarity;
    private final String itemId;
    private final Set<NmsCapabilities> requiredCapabilities;

    LoadoutItemType(boolean forHiders, boolean forSeekers, ItemRarity rarity, String itemId, NmsCapabilities... requiredCapabilities) {
        this.forHiders = forHiders;
        this.forSeekers = forSeekers;
        this.rarity = rarity;
        this.itemId = itemId;
        this.requiredCapabilities = Set.of(requiredCapabilities);
    }

    public boolean isForHiders() {
        return forHiders;
    }

    public boolean isForSeekers() {
        return forSeekers;
    }

    public ItemRarity getRarity() {
        return rarity;
    }

    public String getItemId() {
        return itemId;
    }

    public boolean isSupported(NmsAdapter nms) {
        return nms.capabilities().containsAll(requiredCapabilities);
    }
}
