package de.thecoolcraft11.hideAndSeek.items;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.items.hider.*;
import de.thecoolcraft11.hideAndSeek.model.ItemRarity;
import de.thecoolcraft11.minigameframework.items.variants.ItemVariant;
import de.thecoolcraft11.minigameframework.items.variants.ItemVariantBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class HiderItemSkins {

    private HiderItemSkins() {
    }

    public static void registerAll(HideAndSeek plugin) {
        var vm = plugin.getCustomItemManager().getVariantManager();

        register(vm, plugin, ExplosionItem.ID, "skin_confetti_popper", "Confetti Popper", "paper", 0, ItemRarity.UNCOMMON);
        register(vm, plugin, ExplosionItem.ID, "skin_bubble_popper", "Bubble Popper", "glass_bottle", -1, ItemRarity.RARE);

        register(vm, plugin, RandomBlockItem.ID, "skin_shapeshifter_dust", "Shapeshifter Dust", "redstone", 0, ItemRarity.UNCOMMON);
        register(vm, plugin, RandomBlockItem.ID, "skin_mystery_box", "Mystery Box", "chest", -1, ItemRarity.EPIC);

        for (int level = 0; level <= 5; level++) {
            String speedId = SpeedBoostItem.ID + "_" + level;
            register(vm, plugin, speedId, "skin_rocket_boots", "Rocket Boots", "iron_boots", 0, ItemRarity.UNCOMMON);
            register(vm, plugin, speedId, "skin_sugar_rush", "Sugar Rush", "cookie", -1, ItemRarity.RARE);
        }

        register(vm, plugin, TrackerCrossbowItem.ID, "skin_paintball_gun", "Paintball Gun", "stick", 0, ItemRarity.UNCOMMON);
        register(vm, plugin, TrackerCrossbowItem.ID, "skin_laser_tag", "Laser Tag", "bow", -1, ItemRarity.RARE);

        for (int level = 1; level <= 5; level++) {
            String knockbackId = KnockbackStickItem.ID + "_" + level;
            register(vm, plugin, knockbackId, "skin_squeaky_hammer", "Squeaky Hammer", "red_wool", 0, ItemRarity.COMMON);
            register(vm, plugin, knockbackId, "skin_pool_noodle", "Pool Noodle", "magenta_dye", -1, ItemRarity.UNCOMMON);
        }

        register(vm, plugin, BlockSwapItem.ID, "skin_magic_mirror", "Magic Mirror", "glass_pane", 0, ItemRarity.RARE);
        register(vm, plugin, BlockSwapItem.ID, "skin_quantum_link", "Quantum Link", "echo_shard", -1, ItemRarity.EPIC);

        register(vm, plugin, BigFirecrackerItem.ID, "skin_giant_present", "Giant Present", "barrel", 0, ItemRarity.RARE);
        register(vm, plugin, BigFirecrackerItem.ID, "skin_boombox", "Boombox", "jukebox", -1, ItemRarity.EPIC);

        register(vm, plugin, FireworkRocketItem.ID, "skin_space_shuttle", "Space Shuttle", "iron_nugget", 0, ItemRarity.UNCOMMON);
        register(vm, plugin, FireworkRocketItem.ID, "skin_signal_flare", "Signal Flare", "red_candle", -1, ItemRarity.RARE);

        register(vm, plugin, SlownessBallItem.ID, "skin_sticky_honey", "Sticky Honey", "honey_bottle", 0, ItemRarity.UNCOMMON);
        register(vm, plugin, SlownessBallItem.ID, "skin_tar_ball", "Tar Ball", "coal", -1, ItemRarity.RARE);

        register(vm, plugin, SmokeBombItem.ID, "skin_ninja_smoke", "Ninja Smoke", "gunpowder", 0, ItemRarity.UNCOMMON);
        register(vm, plugin, SmokeBombItem.ID, "skin_spore_cloud", "Spore Cloud", "brown_mushroom", -1, ItemRarity.RARE);

        register(vm, plugin, GhostEssenceItem.ID, "skin_spectral_form", "Spectral Form", "soul_soil", 0, ItemRarity.RARE);
        register(vm, plugin, GhostEssenceItem.ID, "skin_digital_phase", "Digital Phase", "echo_shard", -1, ItemRarity.EPIC);

        register(vm, plugin, InvisibilityCloakItem.ID, "skin_cardboard_box", "Cardboard Box", "chest", 0, ItemRarity.COMMON);
        register(vm, plugin, InvisibilityCloakItem.ID, "skin_camo_netting", "Camo Netting", "oak_leaves", -1, ItemRarity.UNCOMMON);

        register(vm, plugin, MedkitItem.ID, "skin_bandage_roll", "Bandage Roll", "paper", 0, ItemRarity.COMMON);
        register(vm, plugin, MedkitItem.ID, "skin_magic_potion", "Magic Potion", "glistering_melon_slice", -1, ItemRarity.UNCOMMON);

        register(vm, plugin, TotemItem.ID, "skin_phoenix_feather", "Phoenix Feather", "feather", 0, ItemRarity.RARE);
        register(vm, plugin, TotemItem.ID, "skin_extra_life_coin", "Extra Life Coin", "gold_ingot", -1, ItemRarity.LEGENDARY);

        register(vm, plugin, SoundItem.ID, "skin_megaphone", "Megaphone", "iron_ingot", 0, ItemRarity.COMMON);
        register(vm, plugin, SoundItem.ID, "skin_rubber_chicken", "Rubber Chicken", "yellow_wool", -1, ItemRarity.UNCOMMON);
    }

    private static void register(
            de.thecoolcraft11.minigameframework.items.variants.ItemVariantManager vm,
            HideAndSeek plugin,
            String itemId,
            String variantId,
            String displayName,
            String modelKey,
            int sortPriority,
            ItemRarity rarity
    ) {
        ItemStack stack = createVariantStack(plugin, itemId, displayName, modelKey);
        if (stack == null) {
            plugin.getLogger().warning("Skipping skin '" + variantId + "' for item '" + itemId + "' because base item stack is unavailable.");
            return;
        }

        ItemVariant variant = new ItemVariantBuilder(variantId, stack)
                .withDisplayName(displayName)
                .withSortPriority(sortPriority)
                .build();

        vm.registerVariant(itemId, variant);
        ItemSkinSelectionService.registerVariantMetadata(itemId, variantId, rarity);
    }

    private static ItemStack createVariantStack(HideAndSeek plugin, String itemId, String displayName, String modelKey) {
        var customItem = plugin.getCustomItemManager().getItem(itemId);
        if (customItem == null) {
            return null;
        }

        ItemStack base = customItem.getItemStack();
        if (base == null || base.getType() == Material.AIR) {
            return null;
        }

        ItemStack stack = base.clone();
        ItemMeta meta = stack.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text(displayName, NamedTextColor.GOLD, TextDecoration.BOLD)
                    .decoration(TextDecoration.ITALIC, false));
            meta.setItemModel(new NamespacedKey("minecraft", modelKey));
            stack.setItemMeta(meta);
        }

        return stack;
    }
}

