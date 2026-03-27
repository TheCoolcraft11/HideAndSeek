package de.thecoolcraft11.hideAndSeek.items.hider;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.items.api.GameItem;
import de.thecoolcraft11.minigameframework.items.CustomItemBuilder;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class InkFaceItem implements GameItem {
    public static final String ID = "has_hider_ink_face";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public ItemStack createItem(HideAndSeek plugin) {
        ItemStack item = new ItemStack(Material.CARVED_PUMPKIN);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.displayName(Component.text("Ink", NamedTextColor.DARK_BLUE, TextDecoration.BOLD)
                    .decoration(TextDecoration.ITALIC, false));
            meta.setUnbreakable(true);
            meta.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            meta.setEnchantmentGlintOverride(false);
            meta.addEnchant(Enchantment.BINDING_CURSE, 1, true);
            meta.setItemModel(new NamespacedKey("minecraft", "air"));
            item.setItemMeta(meta);
        }

        return item;
    }

    @Override
    public String getDescription(HideAndSeek plugin) {
        return "Hidden helmet used for ink-blindness effects.";
    }

    @Override
    public void register(HideAndSeek plugin) {
        plugin.getCustomItemManager().registerItem(new CustomItemBuilder(createItem(plugin), getId())
                .withDropPrevention(true)
                .withCraftPrevention(true)
                .allowOffHand(false)
                .allowArmor(true)
                .withInventoryMovePrevention(true)
                .cancelDefaultAction(false)
                .build());
    }

    public static void applyMask(Player player, HideAndSeek plugin) {
        player.getInventory().setHelmet(plugin.getCustomItemManager().getIdentifiedItemStack(ID, player));
    }
}

