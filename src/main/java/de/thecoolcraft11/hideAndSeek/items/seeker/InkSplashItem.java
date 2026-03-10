package de.thecoolcraft11.hideAndSeek.items.seeker;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.items.HiderItems;
import de.thecoolcraft11.hideAndSeek.items.api.GameItem;
import de.thecoolcraft11.hideAndSeek.listener.player.HiderEquipmentChangeListener;
import de.thecoolcraft11.hideAndSeek.util.XpProgressHelper;
import de.thecoolcraft11.hideAndSeek.util.points.PointAction;
import de.thecoolcraft11.minigameframework.items.CustomItemBuilder;
import de.thecoolcraft11.minigameframework.items.ItemActionType;
import de.thecoolcraft11.minigameframework.items.ItemInteractionContext;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;

import static de.thecoolcraft11.hideAndSeek.items.api.ItemStateManager.*;

public class InkSplashItem implements GameItem {
    public static final String ID = "has_seeker_ink_splash";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public ItemStack createItem(HideAndSeek plugin) {
        ItemStack item = new ItemStack(Material.INK_SAC);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Ink Splash", NamedTextColor.DARK_AQUA, TextDecoration.BOLD)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.text("Right click to blind hiders", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false)
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    public String getDescription() {
        return "Throw ink to blind hiders";
    }

    @Override
    public void register(HideAndSeek plugin) {
        int inkCooldown = plugin.getSettingRegistry().get("seeker-items.ink-splash.cooldown", 20);
        plugin.getCustomItemManager().registerItem(new CustomItemBuilder(createItem(plugin), getId())
                .withDescription(getDescription())
                .withAction(ItemActionType.RIGHT_CLICK_AIR, context -> spawnInkSplash(context, plugin))
                .withAction(ItemActionType.RIGHT_CLICK_BLOCK, context -> spawnInkSplash(context, plugin))
                .withDropPrevention(true)
                .withCraftPrevention(true)
                .withVanillaCooldown(inkCooldown * 20)
                .withCustomCooldown(inkCooldown * 1000L)
                .withVanillaCooldownDisplay(true)
                .allowOffHand(false)
                .allowArmor(false)
                .cancelDefaultAction(true)
                .build());
    }

    private static void spawnInkSplash(ItemInteractionContext context, HideAndSeek plugin) {
        Player seeker = context.getPlayer();
        int radius = plugin.getSettingRegistry().get("seeker-items.ink-splash.radius", 25);
        int duration = plugin.getSettingRegistry().get("seeker-items.ink-splash.duration", 7);

        
        BukkitTask prevSeekerTask = inkSplashSeekerXpTasks.remove(seeker.getUniqueId());
        XpProgressHelper.SavedXp seekerSavedXp = XpProgressHelper.saveXp(seeker);
        XpProgressHelper.stopAndClear(seeker, prevSeekerTask);
        BukkitTask seekerXpTask = XpProgressHelper.start(plugin, seeker, duration * 20L, XpProgressHelper.Mode.COUNTDOWN, duration);
        inkSplashSeekerXpTasks.put(seeker.getUniqueId(), seekerXpTask);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            BukkitTask t = inkSplashSeekerXpTasks.remove(seeker.getUniqueId());
            XpProgressHelper.stopAndRestore(seeker, t, seekerSavedXp);
        }, duration * 20L);

        for (Player hider : Bukkit.getOnlinePlayers()) {
            if (!HideAndSeek.getDataController().getHiders().contains(hider.getUniqueId())) continue;
            if (hider.getLocation().distance(seeker.getLocation()) > radius) continue;

            plugin.getPointService().award(seeker.getUniqueId(), PointAction.SEEKER_UTILITY_SUCCESS);
            plugin.getPointService().markUtilitySpotted(hider.getUniqueId());

            ItemStack previous = hider.getInventory().getHelmet();
            inkHelmetBackup.put(hider.getUniqueId(), previous);
            HiderItems.applyMask(hider, plugin);
            HiderEquipmentChangeListener.hideHelmet(hider);
            BukkitTask helmetTask = Bukkit.getScheduler().runTaskTimer(
                    plugin,
                    () -> HiderEquipmentChangeListener.hideHelmet(hider),
                    0L, 1L
            );

            hider.getWorld().spawnParticle(Particle.SQUID_INK, hider.getEyeLocation(), 10, 0.3, 0.3, 0.3, 0.1);
            hider.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, duration * 20, 255, false, false));
            hider.sendMessage(Component.text("You've been hit with ink!", NamedTextColor.DARK_AQUA));

            
            BukkitTask prevXpTask = inkSplashXpTasks.remove(hider.getUniqueId());
            XpProgressHelper.SavedXp savedXp = XpProgressHelper.saveXp(hider);
            XpProgressHelper.stopAndClear(hider, prevXpTask);
            BukkitTask xpTask = XpProgressHelper.start(plugin, hider, duration * 20L, XpProgressHelper.Mode.COUNTDOWN, duration);
            inkSplashXpTasks.put(hider.getUniqueId(), xpTask);

            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                helmetTask.cancel();
                ItemStack restore = inkHelmetBackup.remove(hider.getUniqueId());
                hider.getInventory().setHelmet(restore);
                HiderEquipmentChangeListener.hideHelmet(hider);

                BukkitTask t = inkSplashXpTasks.remove(hider.getUniqueId());
                XpProgressHelper.stopAndRestore(hider, t, savedXp);
            }, duration * 20L);
        }
    }
}
