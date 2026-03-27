package de.thecoolcraft11.hideAndSeek.items.seeker;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.items.HiderItems;
import de.thecoolcraft11.hideAndSeek.items.ItemSkinSelectionService;
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
import org.bukkit.Sound;
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

    private static void spawnInkSplash(ItemInteractionContext context, HideAndSeek plugin) {
        Player seeker = context.getPlayer();
        int radius = plugin.getSettingRegistry().get("seeker-items.ink-splash.radius", 25);
        int duration = plugin.getSettingRegistry().get("seeker-items.ink-splash.duration", 7);
        boolean paintBalloon = ItemSkinSelectionService.isSelected(seeker, ID, "skin_paint_balloon");
        boolean mudBall = ItemSkinSelectionService.isSelected(seeker, ID, "skin_mud_ball");


        BukkitTask prevSeekerTask = inkSplashSeekerXpTasks.remove(seeker.getUniqueId());
        XpProgressHelper.SavedXp seekerSavedXp = XpProgressHelper.saveXp(seeker);
        XpProgressHelper.stopAndClear(seeker, prevSeekerTask);
        BukkitTask seekerXpTask = XpProgressHelper.start(plugin, seeker, duration * 20L, XpProgressHelper.Mode.COUNTDOWN, duration);
        inkSplashSeekerXpTasks.put(seeker.getUniqueId(), seekerXpTask);
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            BukkitTask t = inkSplashSeekerXpTasks.remove(seeker.getUniqueId());
            XpProgressHelper.stopAndRestore(seeker, t, seekerSavedXp);
        }, duration * 20L);

        if (paintBalloon) {
            seeker.getWorld().spawnParticle(Particle.ENTITY_EFFECT, seeker.getLocation().add(0, 1, 0), 20, 0.45, 0.35, 0.45, 1.0);
            seeker.playSound(seeker.getLocation(), Sound.ENTITY_SNOWBALL_THROW, 0.55f, 1.35f);
        } else if (mudBall) {
            seeker.getWorld().spawnParticle(Particle.BLOCK, seeker.getLocation().add(0, 1, 0), 20, 0.4, 0.35, 0.4,
                    Material.MUD.createBlockData());
            seeker.playSound(seeker.getLocation(), Sound.BLOCK_MUD_BREAK, 0.55f, 0.9f);
        }

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
            if (paintBalloon) {
                hider.getWorld().spawnParticle(Particle.ITEM_SLIME, hider.getEyeLocation(), 10, 0.25, 0.25, 0.25, 0.02);
                hider.playSound(hider.getLocation(), Sound.ENTITY_SLIME_SQUISH_SMALL, 0.4f, 1.3f);
            } else if (mudBall) {
                hider.getWorld().spawnParticle(Particle.BLOCK, hider.getEyeLocation(), 12, 0.22, 0.22, 0.22,
                        Material.PACKED_MUD.createBlockData());
                hider.playSound(hider.getLocation(), Sound.BLOCK_MUD_PLACE, 0.4f, 0.8f);
            }
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

    @Override
    public String getDescription(HideAndSeek plugin) {
        Number duration = plugin.getSettingRegistry().get("seeker-items.ink-splash.duration", 7);
        Number radius = plugin.getSettingRegistry().get("seeker-items.ink-splash.radius", 25);
        int points = plugin.getPointService().getInt("points.seeker.utility-success.amount", 40);
        return String.format("Splash hiders within %d blocks with ink for %ds, grants %d points per hit.", radius.intValue(), duration.intValue(), points);
    }

    @Override
    public void register(HideAndSeek plugin) {
        int inkCooldown = plugin.getSettingRegistry().get("seeker-items.ink-splash.cooldown", 20);
        plugin.getCustomItemManager().registerItem(new CustomItemBuilder(createItem(plugin), getId())
                .withDescription(getDescription(plugin))
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
}
