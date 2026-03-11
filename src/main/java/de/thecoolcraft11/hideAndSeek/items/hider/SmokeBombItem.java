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

public class SmokeBombItem implements GameItem {
    public static final String ID = "has_hider_smoke_bomb";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public ItemStack createItem(HideAndSeek plugin) {
        ItemStack item = new ItemStack(Material.GRAY_DYE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Smoke Bomb", NamedTextColor.DARK_GRAY, TextDecoration.BOLD)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.text("Right click to throw", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false),
                    Component.text("Creates a smoke cloud for cover", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false)
            ));
            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    public String getDescription(HideAndSeek plugin) {
        Number duration = plugin.getSettingRegistry().get("hider-items.smoke-bomb.duration", 8);
        return String.format("Throw a smoke bomb that creates cover for %ds.", duration.intValue());
    }

    @Override
    public void register(HideAndSeek plugin) {
        int smokeBombCooldown = plugin.getSettingRegistry().get("hider-items.smoke-bomb.cooldown", 15);
        plugin.getCustomItemManager().registerItem(new CustomItemBuilder(createItem(plugin), getId())
                .withAction(ItemActionType.RIGHT_CLICK_AIR, context -> throwSmokeBomb(context, plugin))
                .withAction(ItemActionType.RIGHT_CLICK_BLOCK, context -> throwSmokeBomb(context, plugin))
                .withDescription(getDescription(plugin))
                .withDropPrevention(true)
                .withCraftPrevention(true)
                .withVanillaCooldown(smokeBombCooldown * 20)
                .withCustomCooldown(smokeBombCooldown * 1000L)
                .withVanillaCooldownDisplay(true)
                .allowOffHand(false)
                .allowArmor(false)
                .cancelDefaultAction(true)
                .build());
    }

    private static void throwSmokeBomb(ItemInteractionContext context, HideAndSeek plugin) {
        Player player = context.getPlayer();
        if (!HideAndSeek.getDataController().getHiders().contains(player.getUniqueId())) {
            player.sendMessage(Component.text("Only hiders can use this item.", NamedTextColor.RED));
            return;
        }

        int duration = plugin.getSettingRegistry().get("hider-items.smoke-bomb.duration", 8);
        int radius = plugin.getSettingRegistry().get("hider-items.smoke-bomb.radius", 4);

        org.bukkit.entity.Snowball smokeBomb = player.launchProjectile(org.bukkit.entity.Snowball.class);
        smokeBomb.setItem(new ItemStack(Material.BLACK_CONCRETE_POWDER));
        smokeBomb.setVelocity(smokeBomb.getVelocity().multiply(1.2));
        smokeBomb.getPersistentDataContainer().set(new NamespacedKey(plugin, "smoke_bomb"), PersistentDataType.BOOLEAN, true);
        smokeBomb.getPersistentDataContainer().set(new NamespacedKey(plugin, "smoke_bomb_duration"), PersistentDataType.INTEGER, duration);
        smokeBomb.getPersistentDataContainer().set(new NamespacedKey(plugin, "smoke_bomb_radius"), PersistentDataType.INTEGER, radius);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!smokeBomb.isValid()) {
                    cancel();
                    return;
                }

                smokeBomb.getWorld().spawnParticle(Particle.SMOKE, smokeBomb.getLocation(), 2, 0.1, 0.1, 0.1, 0.02);
            }
        }.runTaskTimer(plugin, 1L, 2L);

        Bukkit.getScheduler().runTaskLater(plugin, smokeBomb::remove, 200L);
    }

    @Override
    public Set<String> getConfigKeys() {
        return Set.of("hider-items.smoke-bomb.cooldown");
    }
}
