package de.thecoolcraft11.hideAndSeek.items.hider;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.items.ItemSkinSelectionService;
import de.thecoolcraft11.hideAndSeek.items.api.GameItem;
import de.thecoolcraft11.minigameframework.items.CustomItemBuilder;
import de.thecoolcraft11.minigameframework.items.ItemActionType;
import de.thecoolcraft11.minigameframework.items.ItemInteractionContext;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class KnockbackStickItem implements GameItem {
    public static final String ID = "has_hider_knockback_stick";

    private static final Map<UUID, Integer> knockbackLevels = new HashMap<>();

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public List<String> getAllIds() {
        List<String> ids = new ArrayList<>();
        for (int i = 0; i <= 5; i++) {
            ids.add(ID + "_" + i);
        }
        return ids;
    }

    public static ItemStack createKnockbackStickItem(int level, HideAndSeek plugin) {
        ItemStack item = new ItemStack(Material.STICK);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(MiniMessage.miniMessage().deserialize(
                            plugin.trText(null, "item.knockback_stick.name", java.util.Map.of("level", level + 1)))
                    .decoration(TextDecoration.ITALIC, false));
            String loreStr = plugin.trText(null, "item.knockback_stick.lore", java.util.Map.of("level", level + 1));
            java.util.List<Component> lore = new java.util.ArrayList<>();
            for (String line : loreStr.split("\n")) {
                lore.add(MiniMessage.miniMessage().deserialize(line).decoration(TextDecoration.ITALIC, false));
            }
            meta.lore(lore);
            meta.addEnchant(Enchantment.KNOCKBACK, Math.clamp(level, 1, 5), true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    public String getDescription(HideAndSeek plugin, @Nullable Player player) {
        return plugin.trText(player, "item.knockback_stick.description");
    }

    public static void upgradeKnockbackItem(Player player, HideAndSeek plugin) {
        int level = Math.min(5, getKnockbackLevel(player.getUniqueId()) + 1);
        knockbackLevels.put(player.getUniqueId(), level);

        ItemStack upgradedItem = null;
        String selectedVariant = null;
        if (plugin != null) {
            String runtimeItemId = ID + "_" + level;
            var customItem = plugin.getCustomItemManager().getItem(runtimeItemId);
            if (customItem != null) {
                upgradedItem = customItem.getIdentifiedItemStack(plugin);

                selectedVariant = ItemSkinSelectionService.getSelectedVariant(player, ID);
                if (selectedVariant != null) {
                    var variant = plugin.getCustomItemManager().getVariantManager().getVariant(runtimeItemId,
                            selectedVariant);
                    if (variant == null) {
                        selectedVariant = null;
                    }
                }
            }
        }

        if (upgradedItem == null) {
            upgradedItem = createKnockbackStickItem(level, plugin);
        }

        if (plugin == null) return;
        removeKnockbackItems(player, plugin);
        player.getInventory().addItem(upgradedItem);
        if (selectedVariant != null) {
            plugin.getCustomItemManager().getVariantManager().switchVariant(player, ID + "_" + level, selectedVariant);
        }
        player.sendMessage(plugin.trText(player, "item.knockback_stick.messages.upgraded",
                java.util.Map.of("level", String.valueOf(level))));
    }

    @Override
    public ItemStack createItem(HideAndSeek plugin) {
        return createKnockbackStickItem(1, plugin);
    }

    private static void knockbackHit(ItemInteractionContext context) {
        Player attacker = context.getPlayer();
        if (context.getEntity() instanceof Player victim) {
            Location victimLoc = victim.getLocation().add(0, 1, 0);
            Vector fromAttacker = victimLoc.toVector().subtract(attacker.getLocation().add(0, 1, 0).toVector());
            if (fromAttacker.lengthSquared() < 0.01) {
                return;
            }

            Vector backDir = fromAttacker.normalize().multiply(-0.25);
            for (int i = 0; i < 5; i++) {
                Location point = victimLoc.clone().add(backDir.clone().multiply(i));
                victim.getWorld().spawnParticle(Particle.CLOUD, point, 2, 0.05, 0.05, 0.05, 0.02);
            }

            if (ItemSkinSelectionService.isSelected(attacker, ID, "skin_squeaky_hammer")) {
                victim.getWorld().spawnParticle(Particle.WAX_ON, victimLoc, 10, 0.3, 0.3, 0.3, 0.01);
                victim.getWorld().playSound(victimLoc, Sound.ENTITY_CHICKEN_EGG, 0.55f, 1.7f);
            } else if (ItemSkinSelectionService.isSelected(attacker, ID, "skin_pool_noodle")) {
                victim.getWorld().spawnParticle(Particle.BUBBLE, victimLoc, 10, 0.25, 0.25, 0.25, 0.02);
                victim.getWorld().playSound(victimLoc, Sound.ITEM_BUCKET_EMPTY_FISH, 0.5f, 1.4f);
            }
        }
    }

    @Override
    public void register(HideAndSeek plugin) {
        int knockbackCooldown = plugin.getSettingRegistry().get("hider-items.knockback-stick.cooldown", 5);

        for (int level = 1; level <= 5; level++) {
            String levelId = ID + "_" + level;
            plugin.getCustomItemManager().registerItem(
                    new CustomItemBuilder(createKnockbackStickItem(level, plugin), levelId)
                    .withAction(ItemActionType.LEFT_CLICK_ENTITY, KnockbackStickItem::knockbackHit)
                    .withDescription(getDescription(plugin, null))
                    .withNameKey("item.knockback_stick.name", Map.of("level", level + 1))
                    .withLoreKey("item.knockback_stick.lore", Map.of("level", level + 1))
                    .withDropPrevention(true)
                    .withCraftPrevention(true)
                    .withVanillaCooldown(knockbackCooldown * 20)
                    .withCustomCooldown(knockbackCooldown * 1000L)
                    .withVanillaCooldownDisplay(true)
                    .allowOffHand(false)
                    .allowArmor(false)
                    .cancelDefaultAction(true)
                    .cancelAttackOnCooldown(true)
                    .build());
        }
    }

    public static int getKnockbackLevel(UUID playerId) {
        return knockbackLevels.getOrDefault(playerId, 0);
    }

    public static void resetKnockbackLevel(UUID playerId) {
        knockbackLevels.put(playerId, 0);
    }

    private static void removeKnockbackItems(Player player, HideAndSeek plugin) {
        var manager = plugin.getCustomItemManager();
        var inv = player.getInventory();
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);
            if (item != null) {
                String id = manager.getCustomItemId(item);
                if (id != null && id.startsWith(ID)) {
                    inv.setItem(i, null);
                }
            }
        }
        ItemStack[] armor = inv.getArmorContents();
        for (int i = 0; i < armor.length; i++) {
            ItemStack item = armor[i];
            if (item != null) {
                String id = manager.getCustomItemId(item);
                if (id != null && id.startsWith(ID)) {
                    armor[i] = null;
                }
            }
        }
        inv.setArmorContents(armor);
        ItemStack offHand = inv.getItemInOffHand();
        String id = manager.getCustomItemId(offHand);
        if (id != null && id.startsWith(ID)) {
            inv.setItemInOffHand(null);
        }
    }

    @Override
    public Set<String> getConfigKeys() {
        return Set.of("hider-items.knockback-stick.cooldown");
    }
}
