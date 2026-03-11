package de.thecoolcraft11.hideAndSeek.items.hider;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.items.api.GameItem;
import de.thecoolcraft11.hideAndSeek.util.points.PointAction;
import de.thecoolcraft11.minigameframework.items.CustomItemBuilder;
import de.thecoolcraft11.minigameframework.items.ItemActionType;
import de.thecoolcraft11.minigameframework.items.ItemInteractionContext;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.type.Candle;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Set;

public class BigFirecrackerItem implements GameItem {
    public static final String ID = "has_hider_big_firecracker";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public ItemStack createItem(HideAndSeek plugin) {
        ItemStack item = new ItemStack(Material.TNT);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Big Firecracker", NamedTextColor.RED, TextDecoration.BOLD)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.text("Right click to place a big firecracker", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false)
            ));
            meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);
        }
        return item;
    }

    @Override
    public String getDescription(HideAndSeek plugin) {
        int points = plugin.getPointService().getInt("points.hider.taunt.large", 75);
        return String.format("Place a large firecracker that explodes, granting %d points.", points);
    }

    @Override
    public void register(HideAndSeek plugin) {
        int bigFirecrackerCooldown = plugin.getSettingRegistry().get("hider-items.big-firecracker.cooldown", 12);
        plugin.getCustomItemManager().registerItem(new CustomItemBuilder(createItem(plugin), getId())
                .withAction(ItemActionType.RIGHT_CLICK_BLOCK, context -> spawnBigFirecracker(context, plugin))
                .withAction(ItemActionType.SHIFT_RIGHT_CLICK_BLOCK, context -> spawnBigFirecracker(context, plugin))
                .withDescription(getDescription(plugin))
                .withDropPrevention(true)
                .withCraftPrevention(true)
                .withVanillaCooldown(bigFirecrackerCooldown * 20)
                .withCustomCooldown(bigFirecrackerCooldown * 1000L)
                .withVanillaCooldownDisplay(true)
                .allowOffHand(false)
                .allowArmor(false)
                .cancelDefaultAction(true)
                .build());
    }

    @Override
    public Set<String> getConfigKeys() {
        return Set.of("hider-items.big-firecracker.cooldown");
    }

    private static void spawnBigFirecracker(ItemInteractionContext context, HideAndSeek plugin) {
        Location location = context.getLocation().clone().add(0, 1, 0);
        Player hider = context.getPlayer();

        Block block = location.getBlock();
        if (!block.getType().isAir()) {
            context.skipCooldown();
            return;
        }

        block.setType(Material.RED_CANDLE);
        Candle candle = (Candle) block.getBlockData();
        candle.setLit(true);
        candle.setCandles(4);
        block.setBlockData(candle);

        int tauntPoints = plugin.getPointService().award(hider.getUniqueId(), PointAction.HIDER_TAUNT_LARGE);
        double volume = plugin.getSettingRegistry().get("hider-items.big-firecracker.volume", 1.2);
        double pitch = plugin.getSettingRegistry().get("hider-items.big-firecracker.pitch", 0.5);
        int fuseTime = plugin.getSettingRegistry().get("hider-items.big-firecracker.fuse-time", 60);
        int miniFuse = plugin.getSettingRegistry().get("hider-items.big-firecracker.mini-fuse-time", 30);
        int miniCount = plugin.getSettingRegistry().get("hider-items.big-firecracker.mini-count", 3);

        hider.sendMessage(Component.text("Big Firecracker placed! +" + tauntPoints + " points", NamedTextColor.GOLD));

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            block.setType(Material.AIR);
            for (Player target : Bukkit.getOnlinePlayers()) {
                Location explosionLoc = location.clone().add(0.5, 0.5, 0.5);
                target.getWorld().spawnParticle(Particle.EXPLOSION, explosionLoc, 3, 0.3, 0.3, 0.3, 0.05);

                target.getWorld().spawnParticle(Particle.FLAME, explosionLoc, 15, 0.4, 0.4, 0.4, 0.08);

                target.getWorld().spawnParticle(Particle.DUST, explosionLoc, 20, 0.4, 0.4, 0.4,
                        new Particle.DustOptions(Color.fromARGB(255, 255, 80, 0), 1.5f));
                target.playSound(location, Sound.ENTITY_GENERIC_EXPLODE, (float) volume, (float) pitch);
            }

            for (int i = 0; i < miniCount; i++) {
                spawnMiniFirecracker(location, plugin, miniFuse);
            }
        }, fuseTime);
    }

    private static void spawnMiniFirecracker(Location origin, HideAndSeek plugin, int miniFuse) {
        Location spawnLoc = origin.clone().add(0.5, 1.0, 0.5);
        ArmorStand stand = origin.getWorld().spawn(spawnLoc, ArmorStand.class, s -> {
            s.setInvisible(true);
            s.setSmall(true);
            s.setGravity(true);
            s.setCollidable(false);
            s.getPersistentDataContainer().set(new NamespacedKey(plugin, "firecracker"), PersistentDataType.BOOLEAN, true);
        });

        Vector velocity = new Vector(
                (Math.random() - 0.5) * 0.8,
                0.3 + Math.random() * 0.5,
                (Math.random() - 0.5) * 0.8
        );
        stand.setVelocity(velocity);

        new BukkitRunnable() {
            @Override
            public void run() {
                if (!stand.isValid()) {
                    cancel();
                    return;
                }

                Location loc = stand.getLocation();
                World world = loc.getWorld();

                world.spawnParticle(
                        Particle.ELECTRIC_SPARK,
                        loc.getX(), loc.getY(), loc.getZ(),
                        5,
                        0.1, 0.1, 0.1,
                        0.02
                );

                Vector downward = new Vector(0, -0.1, 0);

                var hit = world.rayTraceBlocks(loc, downward, 0.3);

                if (hit != null && hit.getHitBlock() != null && hit.getHitBlock().getType().isSolid()) {
                    stand.remove();

                    Location land = hit.getHitBlock().getLocation().add(0, 1, 0);
                    if (land.getBlock().getType().isAir()) {
                        land.getBlock().setType(Material.RED_CANDLE);
                        Candle candle = (Candle) land.getBlock().getBlockData();
                        candle.setLit(true);
                        candle.setCandles(1);
                        land.getBlock().setBlockData(candle);

                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            land.getBlock().setType(Material.AIR);
                            land.getWorld().spawnParticle(
                                    Particle.EXPLOSION,
                                    land.clone().add(0.5, 0.5, 0.5),
                                    1, 0, 0, 0, 0
                            );
                            land.getWorld().playSound(land, Sound.ENTITY_GENERIC_EXPLODE, 0.8f, 1.2f);
                        }, miniFuse);
                    }
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }
}
