package de.thecoolcraft11.hideAndSeek.items;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.items.seeker.*;
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

public final class SeekerItemSkins {

    private SeekerItemSkins() {
    }

    public static void registerAll(HideAndSeek plugin) {
        var vm = plugin.getCustomItemManager().getVariantManager();

        register(vm, plugin, GrapplingHookItem.ID, "skin_techno_tether", "Techno-Tether", "crossbow", 0, ItemRarity.UNCOMMON);
        register(vm, plugin, GrapplingHookItem.ID, "skin_jungle_vine", "Jungle Vine", "lead", -1, ItemRarity.RARE);
        register(vm, plugin, GrapplingHookItem.ID, "skin_ghostly_chain", "Ghostly Chain", "iron_chain", -2, ItemRarity.EPIC);

        register(vm, plugin, GlowingCompassItem.ID, "skin_tactical_tablet", "Tactical Tablet", "recovery_compass", 0, ItemRarity.UNCOMMON);
        register(vm, plugin, GlowingCompassItem.ID, "skin_eye_of_the_oracle", "Eye of the Oracle", "ender_eye", -1, ItemRarity.RARE);
        register(vm, plugin, GlowingCompassItem.ID, "skin_dowsing_rod", "Dowsing Rod", "blaze_rod", -2, ItemRarity.EPIC);

        register(vm, plugin, BlockRandomizerItem.ID, "skin_glitch_core", "Glitch Core", "echo_shard", 0, ItemRarity.UNCOMMON);
        register(vm, plugin, BlockRandomizerItem.ID, "skin_chaos_magic", "Chaos Magic", "glowstone_dust", -1, ItemRarity.RARE);

        register(vm, plugin, ChainPullItem.ID, "skin_energy_lasso", "Energy Lasso", "lead", 0, ItemRarity.UNCOMMON);
        register(vm, plugin, ChainPullItem.ID, "skin_shadow_tendril", "Shadow Tendril", "sculk_vein", -1, ItemRarity.RARE);

        register(vm, plugin, CageTrapItem.ID, "skin_laser_grid", "Laser Grid", "redstone_torch", 0, ItemRarity.RARE);
        register(vm, plugin, CageTrapItem.ID, "skin_ice_block", "Ice Block", "blue_ice", -1, ItemRarity.EPIC);

        register(vm, plugin, ProximitySensorItem.ID, "skin_cctv_camera", "CCTV Camera", "player_head", 0, ItemRarity.UNCOMMON);
        register(vm, plugin, ProximitySensorItem.ID, "skin_alarm_bell", "Alarm Bell", "bell", -1, ItemRarity.RARE);

        register(vm, plugin, CurseSpellItem.ID, "skin_voodoo_magic", "Voodoo Magic", "player_head", 0, ItemRarity.RARE);
        register(vm, plugin, CurseSpellItem.ID, "skin_toxic_tome", "Toxic Tome", "slime_ball", -1, ItemRarity.UNCOMMON);

        register(vm, plugin, InkSplashItem.ID, "skin_paint_balloon", "Paint Balloon", "cyan_dye", 0, ItemRarity.COMMON);
        register(vm, plugin, InkSplashItem.ID, "skin_mud_ball", "Mud Ball", "clay_ball", -1, ItemRarity.UNCOMMON);

        register(vm, plugin, LightningFreezeItem.ID, "skin_frost_wand", "Frost Wand", "blue_ice", 0, ItemRarity.RARE);
        register(vm, plugin, LightningFreezeItem.ID, "skin_time_stopper", "Time Stopper", "clock", -1, ItemRarity.EPIC);

        register(vm, plugin, SeekersSwordItem.ID, "skin_energy_blade", "Energy Blade", "end_rod", 0, ItemRarity.RARE);
        register(vm, plugin, SeekersSwordItem.ID, "skin_the_ban_hammer", "The Ban Hammer", "iron_axe", -1, ItemRarity.EPIC);
        register(vm, plugin, SeekersSwordItem.ID, "skin_giant_spatula", "Giant Spatula", "iron_shovel", -2, ItemRarity.UNCOMMON);
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

