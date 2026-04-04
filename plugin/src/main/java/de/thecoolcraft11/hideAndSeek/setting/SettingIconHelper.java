package de.thecoolcraft11.hideAndSeek.setting;

import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Color;
import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class SettingIconHelper {

    public ItemStack enchanted(Material material, boolean enchanted) {
        ItemStack itemStack = new ItemStack(material);
        ItemMeta meta = itemStack.getItemMeta();
        meta.setEnchantmentGlintOverride(enchanted);
        itemStack.setItemMeta(meta);
        return itemStack;
    }

    public DyeColor mapToNearestDye(TextColor color) {
        int r = color.red();
        int g = color.green();
        int b = color.blue();

        DyeColor closest = DyeColor.WHITE;
        double closestDistance = Double.MAX_VALUE;

        for (DyeColor dye : DyeColor.values()) {
            Color dyeColor = dye.getColor();
            int dr = dyeColor.getRed() - r;
            int dg = dyeColor.getGreen() - g;
            int db = dyeColor.getBlue() - b;
            double distance = dr * dr + dg * dg + db * db;

            if (distance < closestDistance) {
                closestDistance = distance;
                closest = dye;
            }
        }

        return closest;
    }
}

