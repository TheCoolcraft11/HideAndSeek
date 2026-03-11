package de.thecoolcraft11.hideAndSeek.items.hider;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.items.api.GameItem;
import de.thecoolcraft11.minigameframework.items.CustomItemBuilder;
import de.thecoolcraft11.minigameframework.items.ItemActionType;
import de.thecoolcraft11.minigameframework.items.ItemInteractionContext;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;

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

    @Override
    public ItemStack createItem(HideAndSeek plugin) {
        return createKnockbackStickItem(1);
    }

    @Override
    public String getDescription(HideAndSeek plugin) {
        return "Smack seekers to launch them away.";
    }

    @Override
    public void register(HideAndSeek plugin) {
        int knockbackCooldown = plugin.getSettingRegistry().get("hider-items.knockback-stick.cooldown", 5);

        for (int level = 1; level <= 5; level++) {
            String levelId = ID + "_" + level;
            plugin.getCustomItemManager().registerItem(new CustomItemBuilder(createKnockbackStickItem(level), levelId)
                    .withAction(ItemActionType.LEFT_CLICK_ENTITY, KnockbackStickItem::knockbackHit)
                    .withDescription(getDescription(plugin))
                    .withDropPrevention(true)
                    .withCraftPrevention(true)
                    .withVanillaCooldown(knockbackCooldown * 20)
                    .withCustomCooldown(knockbackCooldown * 1000L)
                    .withVanillaCooldownDisplay(true)
                    .allowOffHand(false)
                    .allowArmor(false)
                    .cancelDefaultAction(true)
                    .build());
        }
    }


    public static ItemStack createKnockbackStickItem(int level) {
        ItemStack item = new ItemStack(Material.STICK);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Knockback Stick", NamedTextColor.DARK_RED, TextDecoration.BOLD)
                    .append(Component.space())
                    .append(Component.text("(Level " + level + ")", NamedTextColor.GOLD)
                            .decoration(TextDecoration.ITALIC, false))
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.text("Left click to knock seekers away", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false)
            ));
            meta.addEnchant(Enchantment.KNOCKBACK, Math.max(1, Math.min(5, level)), true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            meta.setUnbreakable(true);
            item.setItemMeta(meta);
        }
        return item;
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
                victim.getWorld().spawnParticle(org.bukkit.Particle.CLOUD, point, 2, 0.05, 0.05, 0.05, 0.02);
            }
        }
    }

    public static void upgradeKnockbackItem(Player player) {
        int level = Math.min(5, getKnockbackLevel(player.getUniqueId()) + 1);
        knockbackLevels.put(player.getUniqueId(), level);
        removeKnockbackItems(player);
        player.getInventory().addItem(createKnockbackStickItem(level));
        player.sendMessage(Component.text("Knockback stick upgraded!", NamedTextColor.GOLD));
    }

    public static int getKnockbackLevel(UUID playerId) {
        return knockbackLevels.getOrDefault(playerId, 1);
    }

    private static void removeKnockbackItems(Player player) {
        player.getInventory().remove(Material.STICK);
    }

    @Override
    public Set<String> getConfigKeys() {
        return Set.of("hider-items.knockback-stick.cooldown");
    }
}
