package de.thecoolcraft11.hideAndSeek.items.seeker;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.items.ItemSkinSelectionService;
import de.thecoolcraft11.hideAndSeek.items.api.GameItem;
import de.thecoolcraft11.hideAndSeek.util.XpProgressHelper;
import de.thecoolcraft11.hideAndSeek.util.points.PointAction;
import de.thecoolcraft11.minigameframework.items.CustomItemBuilder;
import de.thecoolcraft11.minigameframework.items.ItemActionType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;

import static de.thecoolcraft11.hideAndSeek.items.api.ItemStateManager.lightningFreezeHiderXpTasks;
import static de.thecoolcraft11.hideAndSeek.items.api.ItemStateManager.lightningFreezeXpTasks;

public class LightningFreezeItem implements GameItem {
    public static final String ID = "has_seeker_lightning_freeze";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public ItemStack createItem(HideAndSeek plugin) {
        ItemStack item = new ItemStack(Material.LIGHTNING_ROD);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Lightning Freeze", NamedTextColor.YELLOW, TextDecoration.BOLD)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.text("Right click to freeze all hiders", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false)
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    private static void castLightningFreeze(Player seeker, HideAndSeek plugin) {
        int duration = plugin.getSettingRegistry().get("seeker-items.lightning-freeze.duration", 5);
        boolean frostWand = ItemSkinSelectionService.isSelected(seeker, ID, "skin_frost_wand");
        boolean timeStopper = ItemSkinSelectionService.isSelected(seeker, ID, "skin_time_stopper");

        if (frostWand) {
            seeker.getWorld().spawnParticle(Particle.SNOWFLAKE, seeker.getLocation().add(0, 1.0, 0), 28, 0.45, 0.5, 0.45, 0.04);
            seeker.getWorld().spawnParticle(Particle.ITEM_SNOWBALL, seeker.getLocation().add(0, 1.0, 0), 12, 0.32, 0.38, 0.32, 0.02);
            seeker.playSound(seeker.getLocation(), Sound.BLOCK_AMETHYST_CLUSTER_BREAK, 0.55f, 1.4f);
        } else if (timeStopper) {
            seeker.getWorld().spawnParticle(Particle.END_ROD, seeker.getLocation().add(0, 1.0, 0), 26, 0.45, 0.5, 0.45, 0.02);
            seeker.getWorld().spawnParticle(Particle.ENCHANT, seeker.getLocation().add(0, 1.0, 0), 20, 0.4, 0.45, 0.4, 0.03);
            seeker.getWorld().spawnParticle(Particle.PORTAL, seeker.getLocation().add(0, 1.0, 0), 10, 0.35, 0.4, 0.35, 0.07);
            seeker.playSound(seeker.getLocation(), Sound.BLOCK_BELL_RESONATE, 0.45f, 0.75f);
        }


        for (Player hider : Bukkit.getOnlinePlayers()) {
            if (!HideAndSeek.getDataController().getHiders().contains(hider.getUniqueId())) continue;
            if (!hider.getWorld().equals(seeker.getWorld())) continue;

            plugin.getPointService().award(seeker.getUniqueId(), PointAction.SEEKER_UTILITY_SUCCESS);
            plugin.getPointService().markUtilitySpotted(hider.getUniqueId());

            hider.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, duration * 20, 10, false, false));
            hider.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, duration * 20, 250, false, false));

            if (frostWand) {
                hider.spawnParticle(Particle.SNOWFLAKE, hider.getLocation().add(0, 1.0, 0), 30, 0.38, 0.42, 0.38, 0.03);
                hider.spawnParticle(Particle.ITEM_SNOWBALL, hider.getLocation().add(0, 1.0, 0), 12, 0.3, 0.3, 0.3, 0.02);
                hider.spawnParticle(Particle.CLOUD, hider.getLocation().add(0, 1.0, 0), 8, 0.25, 0.3, 0.25, 0.01);
                hider.playSound(hider.getLocation(), Sound.BLOCK_GLASS_BREAK, 0.8f, 1.4f);
                hider.playSound(hider.getLocation(), Sound.BLOCK_POWDER_SNOW_PLACE, 0.35f, 0.9f);
            } else if (timeStopper) {
                hider.spawnParticle(Particle.END_ROD, hider.getLocation().add(0, 1.0, 0), 26, 0.3, 0.3, 0.3, 0.02);
                hider.spawnParticle(Particle.ENCHANT, hider.getLocation().add(0, 1.0, 0), 24, 0.3, 0.3, 0.3, 0.03);
                hider.spawnParticle(Particle.PORTAL, hider.getLocation().add(0, 1.0, 0), 10, 0.22, 0.25, 0.22, 0.04);
                hider.playSound(hider.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 0.9f, 0.6f);
                hider.playSound(hider.getLocation(), Sound.BLOCK_BELL_USE, 0.4f, 1.6f);
            } else {
                hider.spawnParticle(Particle.ELECTRIC_SPARK, hider.getLocation().add(0, 1.0, 0), 20, 0.3, 0.3, 0.3, 0.05);
                hider.spawnParticle(Particle.FLASH, hider.getLocation().add(0, 1.0, 0), 1, 0, 0, 0, Color.fromARGB(0xFFFFFF));
                hider.playSound(hider.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.0f);
                Entity entity = hider.getWorld().spawnEntity(hider.getLocation(), EntityType.LIGHTNING_BOLT);
                entity.getPersistentDataContainer().set(new NamespacedKey(plugin, "freezeLightning"), PersistentDataType.BOOLEAN, true);
                Bukkit.getOnlinePlayers().stream().filter(player -> !player.getUniqueId().equals(hider.getUniqueId())).forEach(p -> p.hideEntity(plugin, entity));
                hider.spawnParticle(Particle.WAX_ON, hider.getLocation().add(0, 1.0, 0), 8, 0.2, 0.25, 0.2, 0.01);
            }


            new org.bukkit.scheduler.BukkitRunnable() {
                final int maxTicks = duration * 20;
                int ticks = 0;

                @Override
                public void run() {
                    if (!hider.isOnline() || ticks >= maxTicks) {
                        cancel();
                        return;
                    }

                    Location aura = hider.getLocation().add(0, 1.0, 0);
                    if (frostWand) {
                        hider.spawnParticle(Particle.SNOWFLAKE, aura, 4, 0.2, 0.25, 0.2, 0.01);
                        if (ticks % 20 == 0) {
                            hider.spawnParticle(Particle.ITEM_SNOWBALL, aura, 4, 0.2, 0.22, 0.2, 0.01);
                        }
                    } else if (timeStopper) {
                        hider.spawnParticle(Particle.END_ROD, aura, 3, 0.18, 0.22, 0.18, 0.01);
                        if (ticks % 20 == 0) {
                            hider.spawnParticle(Particle.PORTAL, aura, 4, 0.2, 0.22, 0.2, 0.03);
                        }
                    }

                    ticks += 5;
                }
            }.runTaskTimer(plugin, 5L, 5L);


            BukkitTask prevHiderTask = lightningFreezeHiderXpTasks.remove(hider.getUniqueId());
            XpProgressHelper.SavedXp hiderSavedXp = XpProgressHelper.saveXp(hider);
            XpProgressHelper.stopAndClear(hider, prevHiderTask);
            BukkitTask hiderXpTask = XpProgressHelper.start(plugin, hider, duration * 20L, XpProgressHelper.Mode.COUNTDOWN, duration);
            lightningFreezeHiderXpTasks.put(hider.getUniqueId(), hiderXpTask);
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                BukkitTask t = lightningFreezeHiderXpTasks.remove(hider.getUniqueId());
                XpProgressHelper.stopAndRestore(hider, t, hiderSavedXp);
            }, duration * 20L);
        }

        seeker.sendMessage(Component.text("All hiders frozen!", NamedTextColor.AQUA));
        if (frostWand) {
            seeker.getWorld().spawnParticle(Particle.SNOWFLAKE, seeker.getLocation().add(0, 1.0, 0), 24, 0.5, 0.5, 0.5, 0.03);
            seeker.getWorld().spawnParticle(Particle.ITEM_SNOWBALL, seeker.getLocation().add(0, 1.0, 0), 8, 0.35, 0.35, 0.35, 0.01);
        } else if (timeStopper) {
            seeker.getWorld().spawnParticle(Particle.END_ROD, seeker.getLocation().add(0, 1.0, 0), 22, 0.5, 0.5, 0.5, 0.03);
            seeker.getWorld().spawnParticle(Particle.PORTAL, seeker.getLocation().add(0, 1.0, 0), 8, 0.35, 0.35, 0.35, 0.05);
        } else {
            seeker.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, seeker.getLocation().add(0, 1.0, 0), 15, 0.5, 0.5, 0.5, 0.1);
        }


        BukkitTask prevTask = lightningFreezeXpTasks.remove(seeker.getUniqueId());
        XpProgressHelper.SavedXp savedXp = XpProgressHelper.saveXp(seeker);
        XpProgressHelper.stopAndClear(seeker, prevTask);
        BukkitTask xpTask = XpProgressHelper.start(plugin, seeker, duration * 20L, XpProgressHelper.Mode.COUNTDOWN, duration);
        lightningFreezeXpTasks.put(seeker.getUniqueId(), xpTask);

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            BukkitTask t = lightningFreezeXpTasks.remove(seeker.getUniqueId());
            XpProgressHelper.stopAndRestore(seeker, t, savedXp);
        }, duration * 20L);
    }

    @Override
    public String getDescription(HideAndSeek plugin) {
        Number duration = plugin.getSettingRegistry().get("seeker-items.lightning-freeze.duration", 5);
        int points = plugin.getPointService().getInt("points.seeker.utility-success.amount", 40);
        return String.format("Call lightning that freezes all hiders for %ds, grants %d points per hider.", duration.intValue(), points);
    }

    @Override
    public void register(HideAndSeek plugin) {
        int lightningCooldown = plugin.getSettingRegistry().get("seeker-items.lightning-freeze.cooldown", 60);
        plugin.getCustomItemManager().registerItem(new CustomItemBuilder(createItem(plugin), getId())
                .withAction(ItemActionType.RIGHT_CLICK_AIR, context -> castLightningFreeze(context.getPlayer(), plugin))
                .withAction(ItemActionType.RIGHT_CLICK_BLOCK, context -> castLightningFreeze(context.getPlayer(), plugin))
                .withDescription(getDescription(plugin))
                .withDropPrevention(true)
                .withCraftPrevention(true)
                .withVanillaCooldown(lightningCooldown * 20)
                .withCustomCooldown(lightningCooldown * 1000L)
                .withVanillaCooldownDisplay(true)
                .allowOffHand(false)
                .allowArmor(false)
                .cancelDefaultAction(true)
                .build());

    }
}
