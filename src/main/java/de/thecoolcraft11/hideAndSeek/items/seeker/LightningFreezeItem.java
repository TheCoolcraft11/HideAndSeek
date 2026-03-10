package de.thecoolcraft11.hideAndSeek.items.seeker;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
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

    @Override
    public String getDescription() {
        return "Freeze all hiders";
    }

    @Override
    public void register(HideAndSeek plugin) {
        int lightningCooldown = plugin.getSettingRegistry().get("seeker-items.lightning-freeze.cooldown", 60);
        plugin.getCustomItemManager().registerItem(new CustomItemBuilder(createItem(plugin), getId())
                .withAction(ItemActionType.RIGHT_CLICK_AIR, context -> castLightningFreeze(context.getPlayer(), plugin))
                .withAction(ItemActionType.RIGHT_CLICK_BLOCK, context -> castLightningFreeze(context.getPlayer(), plugin))
                .withDescription(getDescription())
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

    private static void castLightningFreeze(Player seeker, HideAndSeek plugin) {
        int duration = plugin.getSettingRegistry().get("seeker-items.lightning-freeze.duration", 5);

        for (Player hider : Bukkit.getOnlinePlayers()) {
            if (!HideAndSeek.getDataController().getHiders().contains(hider.getUniqueId())) continue;
            if (!hider.getWorld().equals(seeker.getWorld())) continue;

            plugin.getPointService().award(seeker.getUniqueId(), PointAction.SEEKER_UTILITY_SUCCESS);
            plugin.getPointService().markUtilitySpotted(hider.getUniqueId());

            hider.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, duration * 20, 10, false, false));
            hider.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, duration * 20, 250, false, false));

            hider.spawnParticle(Particle.ELECTRIC_SPARK, hider.getLocation().add(0, 1.0, 0), 20, 0.3, 0.3, 0.3, 0.05);
            hider.spawnParticle(Particle.FLASH, hider.getLocation().add(0, 1.0, 0), 1, 0, 0, 0, Color.fromARGB(0xFFFFFF));
            hider.playSound(hider.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 1.0f, 1.0f);
            Entity entity = hider.getWorld().spawnEntity(hider.getLocation(), EntityType.LIGHTNING_BOLT);
            entity.getPersistentDataContainer().set(new NamespacedKey(plugin, "freezeLightning"), PersistentDataType.BOOLEAN, true);
            Bukkit.getOnlinePlayers().stream().filter(player -> !player.getUniqueId().equals(hider.getUniqueId())).forEach(p -> p.hideEntity(plugin, entity));

            
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
        seeker.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, seeker.getLocation().add(0, 1.0, 0), 15, 0.5, 0.5, 0.5, 0.1);

        
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
}
