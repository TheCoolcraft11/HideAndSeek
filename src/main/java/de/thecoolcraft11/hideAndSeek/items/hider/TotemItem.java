package de.thecoolcraft11.hideAndSeek.items.hider;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.items.api.GameItem;
import de.thecoolcraft11.hideAndSeek.util.XpProgressHelper;
import de.thecoolcraft11.minigameframework.items.CustomItemBuilder;
import de.thecoolcraft11.minigameframework.items.ItemActionType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;

import static de.thecoolcraft11.hideAndSeek.items.api.ItemStateManager.totemXpTasks;

public class TotemItem implements GameItem {
    public static final String ID = "has_hider_totem";

    private static final Map<UUID, Long> totemActiveUntil = new HashMap<>();

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public ItemStack createItem(HideAndSeek plugin) {
        ItemStack item = new ItemStack(Material.TOTEM_OF_UNDYING);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Totem of Undying", NamedTextColor.LIGHT_PURPLE, TextDecoration.BOLD)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.text("Right click to activate revive mode", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false)
            ));
            meta.setUnbreakable(true);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static void activateTotem(Player player, HideAndSeek plugin) {
        if (!HideAndSeek.getDataController().getHiders().contains(player.getUniqueId())) {
            player.sendMessage(Component.text("Only hiders can use this item.", NamedTextColor.RED));
            return;
        }

        int duration = plugin.getSettingRegistry().get("hider-items.totem.effect-duration", 30);
        long expiresAt = System.currentTimeMillis() + (duration * 1000L);
        totemActiveUntil.put(player.getUniqueId(), expiresAt);

        player.getInventory().removeItem(new ItemStack(Material.TOTEM_OF_UNDYING, 1));
        player.sendMessage(Component.text("Revive mode activated for " + duration + " seconds!", NamedTextColor.GOLD));


        XpProgressHelper.SavedXp savedXp = XpProgressHelper.saveXp(player);
        BukkitTask xpTask = XpProgressHelper.start(plugin, player, duration * 20L, XpProgressHelper.Mode.COUNTDOWN, duration);
        totemXpTasks.put(player.getUniqueId(), xpTask);

        new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = duration * 20;

            @Override
            public void run() {
                if (!player.isOnline() || ticks >= maxTicks || !isTotemActive(player.getUniqueId())) {
                    cancel();

                    BukkitTask t = totemXpTasks.remove(player.getUniqueId());
                    XpProgressHelper.stopAndRestore(player, t, savedXp);
                    return;
                }

                Location loc = player.getLocation().add(0, 1, 0);

                player.getWorld().spawnParticle(Particle.GLOW, loc, 8, 0.4, 0.4, 0.4, 0.05);

                if (ticks % 5 == 0) {
                    player.getWorld().spawnParticle(Particle.DUST, loc, 5, 0.3, 0.3, 0.3,
                            new Particle.DustOptions(Color.fromARGB(255, 255, 200, 0), 1.0f));
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 1L, 2L);
    }

    @Override
    public String getDescription(HideAndSeek plugin) {
        Number duration = plugin.getSettingRegistry().get("hider-items.totem.effect-duration", 30);
        return String.format("Activate a one-time revive window for %ds.", duration.intValue());
    }

    @Override
    public void register(HideAndSeek plugin) {
        int totemUses = plugin.getSettingRegistry().get("hider-items.totem.max-uses", 1);
        plugin.getCustomItemManager().registerItem(new CustomItemBuilder(createItem(plugin), getId())
                .withAction(ItemActionType.RIGHT_CLICK_AIR, context -> activateTotem(context.getPlayer(), plugin))
                .withAction(ItemActionType.RIGHT_CLICK_BLOCK, context -> activateTotem(context.getPlayer(), plugin))
                .withDescription(getDescription(plugin))
                .withDropPrevention(true)
                .withCraftPrevention(true)
                .withMaxPlayerUses(totemUses)
                .allowOffHand(false)
                .allowArmor(false)
                .cancelDefaultAction(true)
                .withUsesExhaustedHandler((context, isTeamLimit) -> context.getPlayer().sendMessage(Component.text("You've already used your totem!", NamedTextColor.RED)))
                .build());
    }

    public static boolean isTotemActive(UUID playerId) {
        Long expiresAt = totemActiveUntil.get(playerId);
        if (expiresAt == null) {
            return false;
        }
        if (System.currentTimeMillis() > expiresAt) {
            totemActiveUntil.remove(playerId);
            return false;
        }
        return true;
    }

    public static void clearTotem(UUID playerId) {
        totemActiveUntil.remove(playerId);
        BukkitTask xpTask = totemXpTasks.remove(playerId);
        if (xpTask != null) xpTask.cancel();
    }

    public static void reviveWithTotem(Player player) {
        if (player == null) {
            return;
        }
        clearTotem(player.getUniqueId());

        player.playEffect(org.bukkit.EntityEffect.PROTECTED_FROM_DEATH);
        player.setHealth(Math.max(1.0, Objects.requireNonNull(player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH)).getValue()));
        player.setFoodLevel(20);

        org.bukkit.Location roundSpawn = HideAndSeek.getDataController().getRoundSpawnPoint();
        if (roundSpawn != null) {
            player.teleport(roundSpawn);
        }

        player.sendMessage(Component.text("You were revived!", NamedTextColor.GOLD));
    }
}
