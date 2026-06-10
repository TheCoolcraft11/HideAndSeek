package de.thecoolcraft11.hideAndSeek.items;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.items.hider.*;
import de.thecoolcraft11.hideAndSeek.model.ItemRarity;
import de.thecoolcraft11.hideAndSeek.util.CustomModelDataUtil;
import de.thecoolcraft11.minigameframework.items.variants.ItemVariant;
import de.thecoolcraft11.minigameframework.items.variants.ItemVariantBuilder;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Map;

public final class HiderItemSkins {

    private HiderItemSkins() {
    }

    public static void registerAll(HideAndSeek plugin) {
        var vm = plugin.getCustomItemManager().getVariantManager();

        register(vm, plugin, ExplosionItem.ID, "skin_confetti_popper", "paper", 0, ItemRarity.UNCOMMON);
        register(vm, plugin, ExplosionItem.ID, "skin_bubble_popper", "glass_bottle", -1, ItemRarity.RARE);

        register(vm, plugin, RandomBlockItem.ID, "skin_shapeshifter_dust", "redstone", 0, ItemRarity.UNCOMMON);
        register(vm, plugin, RandomBlockItem.ID, "skin_mystery_box", "chest", -1, ItemRarity.EPIC);

        for (int level = 0; level <= 5; level++) {
            String speedId = SpeedBoostItem.ID + "_" + level;
            register(vm, plugin, speedId, "skin_rocket_boots", "iron_boots", 0, ItemRarity.UNCOMMON);
            register(vm, plugin, speedId, "skin_sugar_rush", "cookie", -1, ItemRarity.RARE);
        }

        register(vm, plugin, TrackerCrossbowItem.ID, "skin_paintball_gun", "stick", 0, ItemRarity.UNCOMMON);
        register(vm, plugin, TrackerCrossbowItem.ID, "skin_laser_tag", "bow", -1, ItemRarity.RARE);

        for (int level = 1; level <= 5; level++) {
            String knockbackId = KnockbackStickItem.ID + "_" + level;
            register(vm, plugin, knockbackId, "skin_squeaky_hammer", "red_wool", 0, ItemRarity.COMMON);
            register(vm, plugin, knockbackId, "skin_pool_noodle", "magenta_dye", -1, ItemRarity.UNCOMMON);
        }

        register(vm, plugin, BlockSwapItem.ID, "skin_magic_mirror", "glass_pane", 0, ItemRarity.RARE);
        register(vm, plugin, BlockSwapItem.ID, "skin_quantum_link", "echo_shard", -1, ItemRarity.EPIC);

        register(vm, plugin, BigFirecrackerItem.ID, "skin_giant_present", "barrel", 0, ItemRarity.RARE);
        register(vm, plugin, BigFirecrackerItem.ID, "skin_boombox", "jukebox", -1, ItemRarity.EPIC);

        register(vm, plugin, FireworkRocketItem.ID, "skin_space_shuttle", "iron_nugget", 0, ItemRarity.UNCOMMON);
        register(vm, plugin, FireworkRocketItem.ID, "skin_signal_flare", "red_candle", -1, ItemRarity.RARE);

        register(vm, plugin, SlownessBallItem.ID, "skin_sticky_honey", "honey_bottle", 0, ItemRarity.UNCOMMON);
        register(vm, plugin, SlownessBallItem.ID, "skin_tar_ball", "coal", -1, ItemRarity.RARE);

        register(vm, plugin, SmokeBombItem.ID, "skin_ninja_smoke", "gunpowder", 0, ItemRarity.UNCOMMON);
        register(vm, plugin, SmokeBombItem.ID, "skin_spore_cloud", "brown_mushroom", -1, ItemRarity.RARE);

        register(vm, plugin, GhostEssenceItem.ID, "skin_spectral_form", "soul_soil", 0, ItemRarity.RARE);
        register(vm, plugin, GhostEssenceItem.ID, "skin_digital_phase", "echo_shard", -1, ItemRarity.EPIC);

        register(vm, plugin, InvisibilityCloakItem.ID, "skin_cardboard_box", "chest", 0, ItemRarity.COMMON);
        register(vm, plugin, InvisibilityCloakItem.ID, "skin_camo_netting", "oak_leaves", -1, ItemRarity.UNCOMMON);

        register(vm, plugin, MedkitItem.ID, "skin_bandage_roll", "paper", 0, ItemRarity.COMMON);
        register(vm, plugin, MedkitItem.ID, "skin_magic_potion", "glistering_melon_slice", -1, ItemRarity.UNCOMMON);

        register(vm, plugin, TotemItem.ID, "skin_phoenix_feather", "feather", 0, ItemRarity.RARE);
        register(vm, plugin, TotemItem.ID, "skin_extra_life_coin", "gold_ingot", -1, ItemRarity.LEGENDARY);

        register(vm, plugin, SoundItem.ID, "skin_megaphone", "iron_ingot", 0, ItemRarity.COMMON);
        register(vm, plugin, SoundItem.ID, "skin_rubber_chicken", "yellow_wool", -1, ItemRarity.UNCOMMON);

        register(vm, plugin, RemoteGatewayItem.ID, "skin_end_rift", "ender_eye", 0, ItemRarity.RARE);
        register(vm, plugin, RemoteGatewayItem.ID, "skin_phase_door", "warped_door", -1, ItemRarity.UNCOMMON);
        register(vm, plugin, RemoteGatewayItem.ID, "skin_dematerializer", "light_blue_stained_glass", -2,
                ItemRarity.EPIC);
        register(vm, plugin, RemoteGatewayItem.ID, "skin_void_lattice", "sculk", -3, ItemRarity.LEGENDARY);
    }

    private static String skinKey(String itemId, String variantId) {
        String key = ItemSkinSelectionService.normalizeLogicalItemId(itemId)
                .replace("has_hider_", "").replace("has_seeker_", "");
        String skin = variantId.startsWith("skin_") ? variantId.substring(5) : variantId;
        return "item." + key + ".skin." + skin;
    }

    private static void register(
            de.thecoolcraft11.minigameframework.items.variants.ItemVariantManager vm,
            HideAndSeek plugin,
            String itemId,
            String variantId,
            String modelKey,
            int sortPriority,
            ItemRarity rarity
    ) {
        String nameKey = skinKey(itemId, variantId);
        Map<String, Object> nameArgs = nameArgs(plugin, itemId);
        ItemStack stack = createVariantStack(plugin, itemId, variantId, modelKey);
        if (stack == null) {
            plugin.getLogger().warning("Skipping skin '" + variantId + "' for item '" + itemId + "' because base item stack is unavailable.");
            return;
        }

        ItemVariant variant = new ItemVariantBuilder(variantId, stack)
                .withNameKey(nameKey, nameArgs)
                .withSortPriority(sortPriority)
                .build();

        vm.registerVariant(itemId, variant);
        ItemSkinSelectionService.registerVariantMetadata(itemId, variantId, rarity);
    }

    private static Map<String, Object> nameArgs(HideAndSeek plugin, String itemId) {
        if (itemId.startsWith(SpeedBoostItem.ID + "_")) {
            int level = Integer.parseInt(itemId.substring(itemId.lastIndexOf('_') + 1)) + 1;
            return Map.of("level", level);
        }
        if (itemId.startsWith(KnockbackStickItem.ID + "_")) {
            int level = Integer.parseInt(itemId.substring(itemId.lastIndexOf('_') + 1)) + 1;
            return Map.of("level", level);
        }
        if (itemId.equals(RandomBlockItem.ID)) {
            int maxUses = plugin.getSettingRegistry().get("hider-items.random-block.uses", 5);
            return Map.of("uses", maxUses, "maxUses", maxUses);
        }
        return Map.of();
    }

    private static ItemStack createVariantStack(HideAndSeek plugin, String itemId, String variantId, String modelKey) {
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
            meta.setItemModel(new NamespacedKey("minecraft", modelKey));
            stack.setItemMeta(meta);
        }

        CustomModelDataUtil.setCustomModelData(stack, itemId, variantId);

        return stack;
    }
}

