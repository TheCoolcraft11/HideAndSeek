package de.thecoolcraft11.hideAndSeek.model;

import de.thecoolcraft11.hideAndSeek.items.HiderItems;
import de.thecoolcraft11.hideAndSeek.items.SeekerItems;

public enum LoadoutItemType {

    FIRECRACKER(true, false, ItemRarity.RARE, HiderItems.EXPLOSION_ITEM_ID),
    CAT_SOUND(true, false, ItemRarity.COMMON, HiderItems.SOUND_ITEM_ID),
    RANDOM_BLOCK(true, false, ItemRarity.COMMON, HiderItems.RANDOM_BLOCK_ITEM_ID),
    SPEED_BOOST(true, false, ItemRarity.COMMON, HiderItems.SPEED_BOOST_ITEM_ID),
    TRACKER_CROSSBOW(true, false, ItemRarity.COMMON, HiderItems.TRACKER_CROSSBOW_ITEM_ID),
    KNOCKBACK_STICK(true, false, ItemRarity.COMMON, HiderItems.KNOCKBACK_STICK_ITEM_ID),
    BLOCK_SWAP(true, false, ItemRarity.RARE, HiderItems.BLOCK_SWAP_ITEM_ID),
    BIG_FIRECRACKER(true, false, ItemRarity.UNCOMMON, HiderItems.BIG_FIRECRACKER_ITEM_ID),
    FIREWORK_ROCKET(true, false, ItemRarity.UNCOMMON, HiderItems.FIREWORK_ROCKET_ITEM_ID),
    MEDKIT(true, false, ItemRarity.RARE, HiderItems.MEDKIT_ITEM_ID),
    TOTEM_OF_UNDYING(true, false, ItemRarity.LEGENDARY, HiderItems.TOTEM_ITEM_ID),
    INVISIBILITY_CLOAK(true, false, ItemRarity.EPIC, HiderItems.INVISIBILITY_CLOAK_ITEM_ID),
    SLOWNESS_BALL(true, false, ItemRarity.UNCOMMON, HiderItems.SLOWNESS_BALL_ITEM_ID),
    SMOKE_BOMB(true, false, ItemRarity.UNCOMMON, HiderItems.SMOKE_BOMB_ITEM_ID),


    GRAPPLING_HOOK(false, true, ItemRarity.COMMON, SeekerItems.GRAPPLING_HOOK_ITEM_ID),
    INK_SPLASH(false, true, ItemRarity.RARE, SeekerItems.INK_SPLASH_ITEM_ID),
    LIGHTNING_FREEZE(false, true, ItemRarity.LEGENDARY, SeekerItems.LIGHTNING_FREEZE_ITEM_ID),
    GLOWING_COMPASS(false, true, ItemRarity.EPIC, SeekerItems.GLOWING_COMPASS_ITEM_ID),
    CURSE_SPELL(false, true, ItemRarity.UNCOMMON, SeekerItems.CURSE_SPELL_ITEM_ID),
    BLOCK_RANDOMIZER(false, true, ItemRarity.EPIC, SeekerItems.BLOCK_RANDOMIZER_ITEM_ID),
    CHAIN_PULL(false, true, ItemRarity.UNCOMMON, SeekerItems.CHAIN_PULL_ITEM_ID),
    PROXIMITY_SENSOR(false, true, ItemRarity.RARE, SeekerItems.PROXIMITY_SENSOR_ITEM_ID),
    CAGE_TRAP(false, true, ItemRarity.RARE, SeekerItems.CAGE_TRAP_ITEM_ID);

    private final boolean forHiders;
    private final boolean forSeekers;
    private final ItemRarity rarity;
    private final String itemId;

    LoadoutItemType(boolean forHiders, boolean forSeekers, ItemRarity rarity, String itemId) {
        this.forHiders = forHiders;
        this.forSeekers = forSeekers;
        this.rarity = rarity;
        this.itemId = itemId;
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
}




