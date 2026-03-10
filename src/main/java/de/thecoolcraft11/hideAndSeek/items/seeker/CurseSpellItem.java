package de.thecoolcraft11.hideAndSeek.items.seeker;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.items.api.GameItem;
import de.thecoolcraft11.hideAndSeek.util.XpProgressHelper;
import de.thecoolcraft11.minigameframework.items.CustomItemBuilder;
import de.thecoolcraft11.minigameframework.items.ItemActionType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.UUID;

import static de.thecoolcraft11.hideAndSeek.items.api.ItemStateManager.*;

public class CurseSpellItem implements GameItem {
    public static final String ID = "has_seeker_curse_spell";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public ItemStack createItem(HideAndSeek plugin) {
        ItemStack item = new ItemStack(Material.ENCHANTED_BOOK);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Curse Spell", NamedTextColor.DARK_PURPLE, TextDecoration.BOLD)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.text("Right click to curse hiders", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false)
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    public String getDescription() {
        return "Curse hiders when hitting them";
    }

    @Override
    public void register(HideAndSeek plugin) {
        int curseCooldown = plugin.getSettingRegistry().get("seeker-items.curse-spell.cooldown", 30);
        plugin.getCustomItemManager().registerItem(new CustomItemBuilder(createItem(plugin), getId())
                .withAction(ItemActionType.RIGHT_CLICK_AIR, context -> activateCurseSpell(context.getPlayer(), plugin))
                .withAction(ItemActionType.RIGHT_CLICK_BLOCK, context -> activateCurseSpell(context.getPlayer(), plugin))
                .withDescription(getDescription())
                .withDropPrevention(true)
                .withCraftPrevention(true)
                .withVanillaCooldown(curseCooldown * 20)
                .withCustomCooldown(curseCooldown * 1000L)
                .withVanillaCooldownDisplay(true)
                .allowOffHand(false)
                .allowArmor(false)
                .cancelDefaultAction(true)
                .build());
    }

    private static void activateCurseSpell(Player seeker, HideAndSeek plugin) {
        int duration = plugin.getSettingRegistry().get("seeker-items.curse-spell.active-duration", 10);
        long until = System.currentTimeMillis() + (duration * 1000L);
        seekerCurseActiveUntil.put(seeker.getUniqueId(), until);

        ItemStack sword = seeker.getInventory().getItemInMainHand();
        ItemMeta meta = sword.getItemMeta();
        if (meta != null) {
            meta.addEnchant(Enchantment.SHARPNESS, 1, true);
            sword.setItemMeta(meta);
        }

        seeker.sendMessage(Component.text("Curse spell activated! (" + duration + "s)", NamedTextColor.DARK_PURPLE));

        
        BukkitTask prevTask = curseSpellSeekerXpTasks.remove(seeker.getUniqueId());
        XpProgressHelper.SavedXp savedXp = XpProgressHelper.saveXp(seeker);
        XpProgressHelper.stopAndClear(seeker, prevTask);
        BukkitTask xpTask = XpProgressHelper.start(plugin, seeker, duration * 20L, XpProgressHelper.Mode.COUNTDOWN, duration);
        curseSpellSeekerXpTasks.put(seeker.getUniqueId(), xpTask);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            seekerCurseActiveUntil.remove(seeker.getUniqueId());
            BukkitTask t = curseSpellSeekerXpTasks.remove(seeker.getUniqueId());
            XpProgressHelper.stopAndRestore(seeker, t, savedXp);
            ItemStack s = seeker.getInventory().getItemInMainHand();
            ItemMeta m = s.getItemMeta();
            if (m != null) {
                m.removeEnchant(Enchantment.SHARPNESS);
                s.setItemMeta(m);
            }
        }, duration * 20L);
    }

    public static boolean isCurseActive(UUID seekerId) {
        Long until = seekerCurseActiveUntil.get(seekerId);
        if (until == null) {
            return false;
        }
        if (System.currentTimeMillis() > until) {
            seekerCurseActiveUntil.remove(seekerId);
            return false;
        }
        return true;
    }

    public static boolean isHiderCursed(UUID hiderId) {
        Long until = hiderCursedUntil.get(hiderId);
        if (until == null) {
            return false;
        }
        if (System.currentTimeMillis() > until) {
            hiderCursedUntil.remove(hiderId);
            return false;
        }
        return true;
    }


    public static void applyCurseToHider(Player hider, HideAndSeek plugin) {
        int duration = plugin.getSettingRegistry().get("seeker-items.curse-spell.curse-duration", 8);
        long until = System.currentTimeMillis() + (duration * 1000L);
        hiderCursedUntil.put(hider.getUniqueId(), until);

        var gameModeResult = plugin.getSettingService().getSetting("game.gametype");
        Object gameModeObj = gameModeResult.isSuccess() ? gameModeResult.getValue() : null;
        String mode = gameModeObj != null ? gameModeObj.toString() : "";

        if ("BLOCK".equals(mode)) {
            if (HideAndSeek.getDataController().isHidden(hider.getUniqueId())) {
                plugin.getBlockModeListener().forceUnhide(hider);
            }
        } else {
            hider.removePotionEffect(PotionEffectType.INVISIBILITY);
        }

        if ("SMALL".equals(mode)) {
            var smallSizeResult = plugin.getSettingService().getSetting("game.small_mode_size");
            Object sizeObj = smallSizeResult.isSuccess() ? smallSizeResult.getValue() : 0.5;
            double smallSize = (sizeObj instanceof Number) ? ((Number) sizeObj).doubleValue() : 0.5;

            int shrinkDelay = plugin.getSettingRegistry().get("seeker-items.curse-spell.small-shrink-delay", 8);
            var scale = hider.getAttribute(Attribute.SCALE);
            if (scale != null) {
                scale.setBaseValue(1.0);
                Bukkit.getScheduler().runTaskLater(plugin, () -> scale.setBaseValue(smallSize), shrinkDelay * 20L);
            }
        }

        hider.sendMessage(Component.text("You have been cursed!", NamedTextColor.DARK_PURPLE));

        
        BukkitTask prevXpTask = hiderCursedXpTasks.remove(hider.getUniqueId());
        XpProgressHelper.SavedXp savedXp = XpProgressHelper.saveXp(hider);
        XpProgressHelper.stopAndClear(hider, prevXpTask);
        BukkitTask xpTask = XpProgressHelper.start(plugin, hider, duration * 20L, XpProgressHelper.Mode.COUNTDOWN, duration);
        hiderCursedXpTasks.put(hider.getUniqueId(), xpTask);

        new BukkitRunnable() {
            int ticks = 0;

            @Override
            public void run() {
                if (!hider.isOnline() || !isHiderCursed(hider.getUniqueId())) {
                    BukkitTask t = hiderCursedXpTasks.remove(hider.getUniqueId());
                    XpProgressHelper.stopAndRestore(hider, t, savedXp);
                    cancel();
                    return;
                }

                Location loc = hider.getLocation().add(0, 1, 0);
                hider.getWorld().spawnParticle(Particle.SOUL, loc, 8, 0.3, 0.5, 0.3, 0.1);

                hider.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, loc, 5, 0.2, 0.3, 0.2, 0.05);

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 5L);
    }
}
