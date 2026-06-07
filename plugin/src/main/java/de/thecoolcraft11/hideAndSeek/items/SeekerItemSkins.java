package de.thecoolcraft11.hideAndSeek.items;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.items.seeker.*;
import de.thecoolcraft11.hideAndSeek.model.ItemRarity;
import de.thecoolcraft11.hideAndSeek.util.CustomModelDataUtil;
import de.thecoolcraft11.minigameframework.items.variants.ItemVariant;
import de.thecoolcraft11.minigameframework.items.variants.ItemVariantBuilder;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class SeekerItemSkins {

    private SeekerItemSkins() {
    }

    public static void registerAll(HideAndSeek plugin) {
        var vm = plugin.getCustomItemManager().getVariantManager();

        register(vm, plugin, GrapplingHookItem.ID, "skin_techno_tether", "crossbow", 0, ItemRarity.UNCOMMON);
        register(vm, plugin, GrapplingHookItem.ID, "skin_jungle_vine", "lead", -1, ItemRarity.RARE);
        register(vm, plugin, GrapplingHookItem.ID, "skin_ghostly_chain", "iron_chain", -2, ItemRarity.EPIC);

        register(vm, plugin, GlowingCompassItem.ID, "skin_tactical_tablet", "recovery_compass", 0, ItemRarity.UNCOMMON);
        register(vm, plugin, GlowingCompassItem.ID, "skin_eye_of_the_oracle", "ender_eye", -1, ItemRarity.RARE);
        register(vm, plugin, GlowingCompassItem.ID, "skin_dowsing_rod", "blaze_rod", -2, ItemRarity.EPIC);

        register(vm, plugin, BlockRandomizerItem.ID, "skin_glitch_core", "echo_shard", 0, ItemRarity.UNCOMMON);
        register(vm, plugin, BlockRandomizerItem.ID, "skin_chaos_magic", "glowstone_dust", -1, ItemRarity.RARE);

        register(vm, plugin, ChainPullItem.ID, "skin_energy_lasso", "lead", 0, ItemRarity.UNCOMMON);
        register(vm, plugin, ChainPullItem.ID, "skin_shadow_tendril", "sculk_vein", -1, ItemRarity.RARE);

        register(vm, plugin, CageTrapItem.ID, "skin_laser_grid", "redstone_torch", 0, ItemRarity.RARE);
        register(vm, plugin, CageTrapItem.ID, "skin_ice_block", "blue_ice", -1, ItemRarity.EPIC);

        register(vm, plugin, ProximitySensorItem.ID, "skin_cctv_camera", "observer", 0, ItemRarity.UNCOMMON);
        register(vm, plugin, ProximitySensorItem.ID, "skin_alarm_bell", "bell", -1, ItemRarity.RARE);

        register(vm, plugin, CurseSpellItem.ID, "skin_voodoo_magic", "player_head", 0, ItemRarity.RARE);
        register(vm, plugin, CurseSpellItem.ID, "skin_toxic_tome", "slime_ball", -1, ItemRarity.UNCOMMON);

        register(vm, plugin, InkSplashItem.ID, "skin_paint_balloon", "cyan_dye", 0, ItemRarity.COMMON);
        register(vm, plugin, InkSplashItem.ID, "skin_mud_ball", "clay_ball", -1, ItemRarity.UNCOMMON);

        register(vm, plugin, LightningFreezeItem.ID, "skin_frost_wand", "blue_ice", 0, ItemRarity.RARE);
        register(vm, plugin, LightningFreezeItem.ID, "skin_time_stopper", "clock", -1, ItemRarity.EPIC);

        register(vm, plugin, SeekersSwordItem.ID, "skin_energy_blade", "end_rod", 0, ItemRarity.RARE);
        register(vm, plugin, SeekersSwordItem.ID, "skin_the_ban_hammer", "iron_axe", -1, ItemRarity.EPIC);
        register(vm, plugin, SeekersSwordItem.ID, "skin_giant_spatula", "iron_shovel", -2, ItemRarity.UNCOMMON);

        register(vm, plugin, CameraItem.ID, "skin_spy_lens", "spyglass", 0, ItemRarity.UNCOMMON);
        register(vm, plugin, CameraItem.ID, "skin_owl_eye", "compass", -1, ItemRarity.RARE);
        register(vm, plugin, CameraItem.ID, "skin_orbital_spy", "nether_star", -2, ItemRarity.EPIC);

        register(vm, plugin, PhantomViewerItem.ID, "skin_echo_slate", "echo_shard", 0, ItemRarity.UNCOMMON);
        register(vm, plugin, PhantomViewerItem.ID, "skin_spectral_cartography", "map", -1, ItemRarity.RARE);
        register(vm, plugin, PhantomViewerItem.ID, "skin_quantum_viewfinder", "recovery_compass", -2, ItemRarity.EPIC);
        register(vm, plugin, PhantomViewerItem.ID, "skin_night_recon", "spyglass", -3, ItemRarity.LEGENDARY);

        register(vm, plugin, SeekerAssistantItem.ID, "skin_steel_golem", "iron_ingot", 0, ItemRarity.UNCOMMON);
        register(vm, plugin, SeekerAssistantItem.ID, "skin_ghost_drone", "phantom_membrane", -1, ItemRarity.RARE);
        register(vm, plugin, SeekerAssistantItem.ID, "skin_battle_mech", "netherite_ingot", -2, ItemRarity.EPIC);
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
        ItemStack stack = createVariantStack(plugin, itemId, variantId, nameKey, modelKey);
        if (stack == null) {
            plugin.getLogger().warning("Skipping skin '" + variantId + "' for item '" + itemId + "' because base item stack is unavailable.");
            return;
        }

        ItemVariant variant = new ItemVariantBuilder(variantId, stack)
                .withNameKey(nameKey)
                .withSortPriority(sortPriority)
                .build();

        vm.registerVariant(itemId, variant);
        ItemSkinSelectionService.registerVariantMetadata(itemId, variantId, rarity);
    }

    private static ItemStack createVariantStack(HideAndSeek plugin, String itemId, String variantId, String nameKey, String modelKey) {
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
            meta.displayName(MiniMessage.miniMessage().deserialize(plugin.trText(null, nameKey))
                    .decoration(TextDecoration.ITALIC, false));
            meta.setItemModel(new NamespacedKey("minecraft", modelKey));
            stack.setItemMeta(meta);
        }

        CustomModelDataUtil.setCustomModelData(stack, itemId, variantId);

        return stack;
    }
}

