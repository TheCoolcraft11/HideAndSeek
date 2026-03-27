package de.thecoolcraft11.hideAndSeek.items.hider;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.items.ItemSkinSelectionService;
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
        String variantId = ItemSkinSelectionService.getSelectedVariant(hider, ID);
        String variantKey = variantId == null || variantId.isBlank() ? "default" : variantId;
        boolean giantPresent = ItemSkinSelectionService.isSelected(hider, ID, "skin_giant_present");
        boolean boombox = ItemSkinSelectionService.isSelected(hider, ID, "skin_boombox");

        Block block = location.getBlock();
        if (!block.getType().isAir()) {
            context.skipCooldown();
            return;
        }

        block.setType(giantPresent ? Material.GREEN_CANDLE : boombox ? Material.PURPLE_CANDLE : Material.RED_CANDLE);
        Candle candle = (Candle) block.getBlockData();
        candle.setLit(true);
        candle.setCandles(4);
        block.setBlockData(candle);

        if (boombox) {
            location.getWorld().spawnParticle(Particle.WAX_ON, location.clone().add(0.5, 0.9, 0.5), 14, 0.22, 0.2, 0.22, 0.01);
            location.getWorld().playSound(location, Sound.BLOCK_BEACON_POWER_SELECT, 0.35f, 1.2f);
        }

        int tauntPoints = plugin.getPointService().award(hider.getUniqueId(), PointAction.HIDER_TAUNT_LARGE);
        double baseVolume = plugin.getSettingRegistry().get("hider-items.big-firecracker.volume", 1.2);
        double basePitch = plugin.getSettingRegistry().get("hider-items.big-firecracker.pitch", 0.5);
        double miniBaseVolume = plugin.getSettingRegistry().get("hider-items.big-firecracker.mini-volume", 0.8);
        double miniBasePitch = plugin.getSettingRegistry().get("hider-items.big-firecracker.mini-pitch", 1.2);
        int baseMainParticles = plugin.getSettingRegistry().get("hider-items.big-firecracker.main-particles", 16);
        int baseMiniParticles = plugin.getSettingRegistry().get("hider-items.big-firecracker.mini-particles", 8);
        int sparkParticles = plugin.getSettingRegistry().get("hider-items.big-firecracker.spark-particles", 5);
        int fuseTime = plugin.getSettingRegistry().get("hider-items.big-firecracker.fuse-time", 60);
        int miniFuse = plugin.getSettingRegistry().get("hider-items.big-firecracker.mini-fuse-time", 30);
        int miniCount = plugin.getSettingRegistry().get("hider-items.big-firecracker.mini-count", 3);
        double volumeMultiplier = plugin.getSettingRegistry().get("hider-items.big-firecracker.variants." + variantKey + ".volume-multiplier", 1.0);
        double pitchMultiplier = plugin.getSettingRegistry().get("hider-items.big-firecracker.variants." + variantKey + ".pitch-multiplier", 1.0);
        double mainParticleMultiplier = plugin.getSettingRegistry().get("hider-items.big-firecracker.variants." + variantKey + ".main-particle-multiplier", 1.0);
        double miniParticleMultiplier = plugin.getSettingRegistry().get("hider-items.big-firecracker.variants." + variantKey + ".mini-particle-multiplier", 1.0);

        float mainVolume = (float) Math.max(0.05, baseVolume * volumeMultiplier);
        float mainPitch = (float) Math.max(0.1, basePitch * pitchMultiplier);
        float miniVolume = (float) Math.max(0.05, miniBaseVolume * volumeMultiplier);
        float miniPitch = (float) Math.max(0.1, miniBasePitch * pitchMultiplier);
        int mainParticles = Math.max(1, (int) Math.round(baseMainParticles * mainParticleMultiplier));
        int miniParticles = Math.max(1, (int) Math.round(baseMiniParticles * miniParticleMultiplier));

        hider.sendMessage(Component.text("Big Firecracker placed! +" + tauntPoints + " points", NamedTextColor.GOLD));

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            block.setType(Material.AIR);
            for (Player target : Bukkit.getOnlinePlayers()) {
                Location explosionLoc = location.clone().add(0.5, 0.5, 0.5);
                target.getWorld().spawnParticle(Particle.EXPLOSION, explosionLoc, 3, 0.3, 0.3, 0.3, 0.05);

                if (giantPresent) {
                    target.getWorld().spawnParticle(Particle.FIREWORK, explosionLoc, mainParticles, 0.5, 0.5, 0.5, 0.04);
                    target.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, explosionLoc, Math.max(1, mainParticles / 2), 0.5, 0.5, 0.5, 0.02);
                    target.playSound(location, Sound.ENTITY_FIREWORK_ROCKET_BLAST, mainVolume, mainPitch);
                } else if (boombox) {
                    target.getWorld().spawnParticle(Particle.NOTE, explosionLoc, mainParticles, 0.5, 0.5, 0.5, 1.0);
                    target.getWorld().spawnParticle(Particle.DUST, explosionLoc, mainParticles, 0.4, 0.4, 0.4,
                            new Particle.DustOptions(Color.fromRGB(180, 60, 255), 1.5f));
                    target.getWorld().spawnParticle(Particle.CAMPFIRE_SIGNAL_SMOKE, explosionLoc, 16, 0.2, 0.25, 0.2, 0.01);
                    target.getWorld().spawnParticle(Particle.END_ROD, explosionLoc, 8, 0.25, 0.25, 0.25, 0.02);
                    target.playSound(location, Sound.BLOCK_NOTE_BLOCK_BASEDRUM, mainVolume, mainPitch);
                    target.playSound(location, Sound.ENTITY_BLAZE_SHOOT, Math.max(0.1f, mainVolume * 0.35f), 1.4f);
                } else {
                    target.getWorld().spawnParticle(Particle.FLAME, explosionLoc, mainParticles, 0.4, 0.4, 0.4, 0.08);
                    target.getWorld().spawnParticle(Particle.DUST, explosionLoc, mainParticles, 0.4, 0.4, 0.4,
                            new Particle.DustOptions(Color.fromARGB(255, 255, 80, 0), 1.5f));
                    target.playSound(location, Sound.ENTITY_GENERIC_EXPLODE, mainVolume, mainPitch);
                }
            }

            for (int i = 0; i < miniCount; i++) {
                spawnMiniFirecracker(location, plugin, miniFuse, giantPresent, boombox, miniVolume, miniPitch, miniParticles, sparkParticles);
            }
        }, fuseTime);
    }

    private static void spawnMiniFirecracker(
            Location origin,
            HideAndSeek plugin,
            int miniFuse,
            boolean giantPresent,
            boolean boombox,
            float miniVolume,
            float miniPitch,
            int miniParticles,
            int sparkParticles
    ) {
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
                        Math.max(1, sparkParticles),
                        0.1, 0.1, 0.1,
                        0.02
                );

                Vector downward = new Vector(0, -0.1, 0);

                var hit = world.rayTraceBlocks(loc, downward, 0.3);

                if (hit != null && hit.getHitBlock() != null && hit.getHitBlock().getType().isSolid()) {
                    stand.remove();

                    Location land = hit.getHitBlock().getLocation().add(0, 1, 0);
                    if (land.getBlock().getType().isAir()) {
                        land.getBlock().setType(giantPresent ? Material.GREEN_CANDLE : boombox ? Material.PURPLE_CANDLE : Material.RED_CANDLE);
                        Candle candle = (Candle) land.getBlock().getBlockData();
                        candle.setLit(true);
                        candle.setCandles(1);
                        land.getBlock().setBlockData(candle);

                        Bukkit.getScheduler().runTaskLater(plugin, () -> {
                            land.getBlock().setType(Material.AIR);
                            Location miniLoc = land.clone().add(0.5, 0.5, 0.5);
                            land.getWorld().spawnParticle(Particle.EXPLOSION, miniLoc, 1, 0, 0, 0, 0);
                            if (giantPresent) {
                                land.getWorld().spawnParticle(Particle.FIREWORK, miniLoc, miniParticles, 0.2, 0.2, 0.2, 0.02);
                                land.getWorld().playSound(land, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, miniVolume, miniPitch);
                            } else if (boombox) {
                                land.getWorld().spawnParticle(Particle.NOTE, miniLoc, miniParticles, 0.2, 0.2, 0.2, 1.0);
                                land.getWorld().playSound(land, Sound.BLOCK_NOTE_BLOCK_SNARE, miniVolume, miniPitch);
                            } else {
                                land.getWorld().spawnParticle(Particle.FLAME, miniLoc, miniParticles, 0.2, 0.2, 0.2, 0.03);
                                land.getWorld().playSound(land, Sound.ENTITY_GENERIC_EXPLODE, miniVolume, miniPitch);
                            }
                        }, miniFuse);
                    }
                    cancel();
                }
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }
}
