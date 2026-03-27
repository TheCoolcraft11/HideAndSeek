package de.thecoolcraft11.hideAndSeek.util;

import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.components.CustomModelDataComponent;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("UnstableApiUsage")
public class CustomModelDataUtil {

    private CustomModelDataUtil() {
    }

    public static void setCustomModelData(ItemStack itemStack, String itemId, String skinId) {
        if (itemStack == null) {
            return;
        }

        ItemMeta meta = itemStack.getItemMeta();
        if (meta == null) {
            return;
        }

        List<String> data = new ArrayList<>();
        if (itemId != null && !itemId.isEmpty()) {
            data.add(itemId);
        }
        if (skinId != null && !skinId.isEmpty()) {
            data.add(skinId);
        }

        if (!data.isEmpty()) {
            CustomModelDataComponent customModelData = meta.getCustomModelDataComponent();
            customModelData.setStrings(data);
            meta.setCustomModelDataComponent(customModelData);
            itemStack.setItemMeta(meta);
        }
    }

    public static void setCustomModelData(ItemStack itemStack, String itemId) {
        setCustomModelData(itemStack, itemId, null);
    }

    public static void setForInventorySlot(PlayerInventory inventory, int slot, String itemId, String skinId) {
        if (inventory == null) {
            return;
        }
        ItemStack stack = inventory.getItem(slot);
        if (stack == null || stack.getType().isAir()) {
            return;
        }
        setCustomModelData(stack, itemId, skinId);
        inventory.setItem(slot, stack);
    }
}
