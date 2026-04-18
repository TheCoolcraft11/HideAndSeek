package de.thecoolcraft11.hideAndSeek.model;

import de.thecoolcraft11.hideAndSeek.items.hider.*;
import de.thecoolcraft11.hideAndSeek.items.seeker.*;
import de.thecoolcraft11.hideAndSeek.nms.NmsAdapter;
import de.thecoolcraft11.hideAndSeek.nms.NmsCapabilities;

import java.util.Arrays;
import java.util.Set;


public enum LoadoutItemType {

    FIRECRACKER(true, false, ItemRarity.COMMON, ItemType.OFFENSE, ExplosionItem.ID),
    CAT_SOUND(true, false, ItemRarity.COMMON, ItemType.UTILITY, SoundItem.ID),
    RANDOM_BLOCK(true, false, ItemRarity.COMMON, ItemType.UTILITY, RandomBlockItem.ID),
    SPEED_BOOST(true, false, ItemRarity.COMMON, ItemType.MOBILITY, SpeedBoostItem.ID),
    TRACKER_CROSSBOW(true, false, ItemRarity.COMMON, ItemType.UTILITY, TrackerCrossbowItem.ID),
    KNOCKBACK_STICK(true, false, ItemRarity.COMMON, ItemType.SUPPORT, KnockbackStickItem.ID),
    BLOCK_SWAP(true, false, ItemRarity.RARE, ItemType.MOBILITY, BlockSwapItem.ID),
    BIG_FIRECRACKER(true, false, ItemRarity.RARE, ItemType.OFFENSE, BigFirecrackerItem.ID),
    FIREWORK_ROCKET(true, false, ItemRarity.UNCOMMON, ItemType.OFFENSE, FireworkRocketItem.ID),
    MEDKIT(true, false, ItemRarity.RARE, ItemType.HEALING, MedkitItem.ID),
    TOTEM_OF_UNDYING(true, false, ItemRarity.LEGENDARY, ItemType.DEFENSE, TotemItem.ID),
    INVISIBILITY_CLOAK(true, false, ItemRarity.EPIC, ItemType.DEFENSE, InvisibilityCloakItem.ID),
    SLOWNESS_BALL(true, false, ItemRarity.UNCOMMON, ItemType.OFFENSE, SlownessBallItem.ID),
    SMOKE_BOMB(true, false, ItemRarity.UNCOMMON, ItemType.OFFENSE, SmokeBombItem.ID),
    GHOST_ESSENCE(true, false, ItemRarity.RARE, ItemType.DEFENSE, GhostEssenceItem.ID,
            NmsCapabilities.CLIENT_GAMEMODE_SPOOFING, NmsCapabilities.MOB_PATHFINDING),
    REMOTE_GATEWAY(true, false, ItemRarity.EPIC, ItemType.MOBILITY, RemoteGatewayItem.ID),


    GRAPPLING_HOOK(false, true, ItemRarity.COMMON, ItemType.MOBILITY, GrapplingHookItem.ID),
    INK_SPLASH(false, true, ItemRarity.RARE, ItemType.OFFENSE, InkSplashItem.ID),
    LIGHTNING_FREEZE(false, true, ItemRarity.LEGENDARY, ItemType.OFFENSE, LightningFreezeItem.ID),
    GLOWING_COMPASS(false, true, ItemRarity.EPIC, ItemType.INFORMATION, GlowingCompassItem.ID),
    CURSE_SPELL(false, true, ItemRarity.UNCOMMON, ItemType.OFFENSE, CurseSpellItem.ID),
    BLOCK_RANDOMIZER(false, true, ItemRarity.EPIC, ItemType.UTILITY, BlockRandomizerItem.ID),
    CHAIN_PULL(false, true, ItemRarity.UNCOMMON, ItemType.OFFENSE, ChainPullItem.ID),
    PROXIMITY_SENSOR(false, true, ItemRarity.RARE, ItemType.UTILITY, ProximitySensorItem.ID),
    CAMERA(false, true, ItemRarity.EPIC, ItemType.INFORMATION, CameraItem.ID, NmsCapabilities.CLIENT_CAMERA_SPOOFING,
            NmsCapabilities.CLIENT_ENTITY_GLOWING, NmsCapabilities.CLIENT_ENTITY_SPAWNING),
    PHANTOM_VIEWER(false, true, ItemRarity.RARE, ItemType.INFORMATION, PhantomViewerItem.ID),
    CAGE_TRAP(false, true, ItemRarity.RARE, ItemType.TRAP, CageTrapItem.ID),
    SEEKER_ASSISTANT(false, true, ItemRarity.LEGENDARY, ItemType.SUPPORT, SeekerAssistantItem.ID,
            NmsCapabilities.MOB_PATHFINDING, NmsCapabilities.CLIENT_TEST_BLOCK_BEAM,
            NmsCapabilities.CUSTOM_ENTITY_GOALS),
    ;


    private final boolean forHiders;
    private final boolean forSeekers;
    private final ItemRarity rarity;
    private final ItemType itemType;
    private final String itemId;
    private final Set<NmsCapabilities> requiredCapabilities;

    LoadoutItemType(boolean forHiders, boolean forSeekers, ItemRarity rarity, ItemType itemType, String itemId, NmsCapabilities... requiredCapabilities) {
        this.forHiders = forHiders;
        this.forSeekers = forSeekers;
        this.rarity = rarity;
        this.itemType = itemType;
        this.itemId = itemId;
        this.requiredCapabilities = Set.of(requiredCapabilities);
    }

    public ItemType getItemType() {
        return itemType;
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

    public static LoadoutItemType fromID(String ID) {
        return Arrays.stream(values()).filter(type -> type.getItemId().equals(ID)).findFirst().orElse(null);
    }

    public boolean isSupported(NmsAdapter nms) {
        return nms.capabilities().containsAll(requiredCapabilities);
    }
}
