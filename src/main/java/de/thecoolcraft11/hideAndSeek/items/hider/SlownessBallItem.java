package de.thecoolcraft11.hideAndSeek.items.hider;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.items.api.GameItem;
import de.thecoolcraft11.minigameframework.items.CustomItemBuilder;
import de.thecoolcraft11.minigameframework.items.ItemActionType;
import de.thecoolcraft11.minigameframework.items.ItemInteractionContext;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;
import java.util.Set;

public class SlownessBallItem implements GameItem {
    public static final String ID = "has_hider_slowness_ball";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public ItemStack createItem(HideAndSeek plugin) {
        ItemStack item = new ItemStack(Material.SNOWBALL);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Slowness Ball", NamedTextColor.AQUA, TextDecoration.BOLD)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.text("Right click to throw at seekers", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false)
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    public String getDescription(HideAndSeek plugin) {
        Number duration = plugin.getSettingRegistry().get("hider-items.slowness-ball.duration", 6);
        return String.format("Throw an ice ball that slows seekers for %ds.", duration.intValue());
    }

    @Override
    public void register(HideAndSeek plugin) {
        int slownessBallCooldown = plugin.getSettingRegistry().get("hider-items.slowness-ball.cooldown", 10);
        plugin.getCustomItemManager().registerItem(new CustomItemBuilder(createItem(plugin), getId())
                .withAction(ItemActionType.RIGHT_CLICK_AIR, context -> throwSlownessBall(context, plugin))
                .withAction(ItemActionType.RIGHT_CLICK_BLOCK, context -> throwSlownessBall(context, plugin))
                .withDescription(getDescription(plugin))
                .withDropPrevention(true)
                .withCraftPrevention(true)
                .withVanillaCooldown(slownessBallCooldown * 20)
                .withCustomCooldown(slownessBallCooldown * 1000L)
                .withVanillaCooldownDisplay(true)
                .allowOffHand(false)
                .allowArmor(false)
                .cancelDefaultAction(true)
                .build());
    }

    @Override
    public Set<String> getConfigKeys() {
        return Set.of("hider-items.slowness-ball.cooldown");
    }

    private static void throwSlownessBall(ItemInteractionContext context, HideAndSeek plugin) {
        Player player = context.getPlayer();
        if (!HideAndSeek.getDataController().getHiders().contains(player.getUniqueId())) {
            player.sendMessage(Component.text("Only hiders can use this item.", NamedTextColor.RED));
            return;
        }

        int duration = plugin.getSettingRegistry().get("hider-items.slowness-ball.duration", 6);
        int amplifier = plugin.getSettingRegistry().get("hider-items.slowness-ball.amplifier", 1);

        org.bukkit.entity.Snowball snowball = player.launchProjectile(org.bukkit.entity.Snowball.class);
        snowball.setItem(new ItemStack(Material.ICE));
        snowball.setVelocity(snowball.getVelocity().multiply(1.5));
        snowball.getPersistentDataContainer().set(new NamespacedKey(plugin, "slowness_ball"), PersistentDataType.BOOLEAN, true);
        snowball.getPersistentDataContainer().set(new NamespacedKey(plugin, "slowness_ball_duration"), PersistentDataType.INTEGER, duration);
        snowball.getPersistentDataContainer().set(new NamespacedKey(plugin, "slowness_ball_amplifier"), PersistentDataType.INTEGER, amplifier);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!snowball.isValid()) {
                    cancel();
                    return;
                }

                snowball.getWorld().spawnParticle(Particle.SNOWFLAKE, snowball.getLocation(), 3, 0.1, 0.1, 0.1, 0.05);
            }
        }.runTaskTimer(plugin, 1L, 2L);

        Bukkit.getScheduler().runTaskLater(plugin, snowball::remove, 200L);
    }
}
