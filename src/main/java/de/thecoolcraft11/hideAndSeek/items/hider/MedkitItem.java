package de.thecoolcraft11.hideAndSeek.items.hider;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.items.ItemSkinSelectionService;
import de.thecoolcraft11.hideAndSeek.items.api.GameItem;
import de.thecoolcraft11.hideAndSeek.util.XpProgressHelper;
import de.thecoolcraft11.minigameframework.items.CustomItemBuilder;
import de.thecoolcraft11.minigameframework.items.ItemActionType;
import de.thecoolcraft11.minigameframework.items.ItemInteractionContext;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.BlocksAttacks;
import io.papermc.paper.datacomponent.item.blocksattacks.DamageReduction;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import static de.thecoolcraft11.hideAndSeek.items.api.ItemStateManager.*;

@SuppressWarnings("UnstableApiUsage")
public class MedkitItem implements GameItem {
    public static final String ID = "has_hider_medkit";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public ItemStack createItem(HideAndSeek plugin) {
        ItemStack item = new ItemStack(Material.FLOWER_BANNER_PATTERN);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Medkit", NamedTextColor.GREEN, TextDecoration.BOLD)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.text("Hold right click to channel heal", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false),
                    Component.text("Release early to cancel", NamedTextColor.DARK_GRAY)
                            .decoration(TextDecoration.ITALIC, false)
            ));
            item.setItemMeta(meta);


            item.setData(
                    DataComponentTypes.BLOCKS_ATTACKS,
                    BlocksAttacks.blocksAttacks()
                            .addDamageReduction(DamageReduction.damageReduction().base(0).factor(0).horizontalBlockingAngle(0.1f).build())
                            .build()
            );
        }
        return item;
    }

    private static void startMedkitCharge(ItemInteractionContext context, HideAndSeek plugin) {
        Player player = context.getPlayer();
        context.skipCooldown();
        if (!HideAndSeek.getDataController().getHiders().contains(player.getUniqueId())) {
            player.sendMessage(Component.text("Only hiders can use this item.", NamedTextColor.RED));
            return;
        }

        clearMedkitCharge(player);

        int channelTime = plugin.getSettingRegistry().get("hider-items.medkit.channel-time", 5);
        long totalTicks = channelTime * 20L;
        boolean bandageRoll = ItemSkinSelectionService.isSelected(player, ID, "skin_bandage_roll");
        boolean magicPotion = ItemSkinSelectionService.isSelected(player, ID, "skin_magic_potion");

        XpProgressHelper.SavedXp savedXp = XpProgressHelper.saveXp(player);
        medkitChannelXp.put(player.getUniqueId(), savedXp);

        BukkitTask xpTask = XpProgressHelper.start(plugin, player, totalTicks, XpProgressHelper.Mode.COUNTDOWN, channelTime);
        medkitChannelTasks.put(player.getUniqueId(), xpTask);


        BukkitTask particleTask = new BukkitRunnable() {
            long ticks = 0;

            @Override
            public void run() {
                if (!player.isOnline() || ticks > totalTicks) {
                    cancel();
                    return;
                }

                Location loc = player.getLocation().add(0, 1, 0);
                player.getWorld().spawnParticle(Particle.HEART, loc, 1, 0.2, 0.3, 0.2, 0.05);
                player.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, loc, 1, 0.15, 0.25, 0.15, 0.02);
                if (bandageRoll && ticks % 3 == 0) {
                    player.getWorld().spawnParticle(Particle.BLOCK, loc, 2, 0.12, 0.12, 0.12,
                            Material.WHITE_WOOL.createBlockData());
                    player.getWorld().spawnParticle(Particle.CLOUD, loc, 1, 0.1, 0.12, 0.1, 0.01);
                    if (ticks % 10 == 0) {
                        player.getWorld().spawnParticle(Particle.WAX_ON, loc, 2, 0.16, 0.18, 0.16, 0.01);
                        player.playSound(player.getLocation(), Sound.ITEM_ARMOR_EQUIP_LEATHER, 0.15f, 1.55f);
                    }
                } else if (magicPotion && ticks % 3 == 0) {
                    player.getWorld().spawnParticle(Particle.WITCH, loc, 3, 0.18, 0.22, 0.18, 0.03);
                    player.getWorld().spawnParticle(Particle.ENCHANT, loc, 2, 0.18, 0.22, 0.18, 0.02);
                    if (ticks % 10 == 0) {
                        player.getWorld().spawnParticle(Particle.ENTITY_EFFECT, loc, 3, 0.2, 0.25, 0.2, 0.01);
                        player.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 0.2f, 1.25f);
                    }
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 1L);
        medkitChannelParticleTasks.put(player.getUniqueId(), particleTask);
    }

    @Override
    public String getDescription(HideAndSeek plugin) {
        Number channelTime = plugin.getSettingRegistry().get("hider-items.medkit.channel-time", 5);
        return String.format("Hold block for %ds, then release to heal yourself fully.", channelTime.intValue());
    }

    @Override
    public Set<String> getConfigKeys() {
        return Set.of("hider-items.medkit.cooldown");
    }

    @Override
    public void register(HideAndSeek plugin) {
        int medkitCooldown = plugin.getSettingRegistry().get("hider-items.medkit.cooldown", 30);
        plugin.getCustomItemManager().registerItem(new CustomItemBuilder(createItem(plugin), getId())
                .withAction(ItemActionType.BLOCK_START, context -> startMedkitCharge(context, plugin))
                .withAction(ItemActionType.BLOCK_RELEASE, context -> finishMedkitCharge(context, plugin))
                .withCustomCooldown(medkitCooldown * 1000L)
                .withVanillaCooldown(medkitCooldown * 20)
                .withVanillaCooldownDisplay(true)
                .withDescription(getDescription(plugin))
                .withDropPrevention(true)
                .withCraftPrevention(true)
                .allowOffHand(false)
                .allowArmor(false)
                .cancelDefaultAction(false)
                .build());
    }

    private static void finishMedkitCharge(ItemInteractionContext context, HideAndSeek plugin) {
        Player player = context.getPlayer();
        long holdDurationMs = Math.max(0L, context.getHoldDurationMs());
        int channelTimeSeconds = plugin.getSettingRegistry().get("hider-items.medkit.channel-time", 5);
        long requiredMs = channelTimeSeconds * 1000L;

        clearMedkitCharge(player);

        if (!HideAndSeek.getDataController().getHiders().contains(player.getUniqueId())) {
            player.sendMessage(Component.text("Only hiders can use this item.", NamedTextColor.RED));
            context.skipCooldown();
            return;
        }

        if (holdDurationMs < requiredMs) {
            long remainingMs = requiredMs - holdDurationMs;
            double remainingSeconds = Math.ceil(remainingMs / 100.0) / 10.0;
            player.sendMessage(Component.text("Medkit canceled. Hold for " + remainingSeconds + "s longer.", NamedTextColor.YELLOW));
            context.skipCooldown();
            return;
        }

        double healAmount = plugin.getSettingRegistry().get("hider-items.medkit.heal-amount", 20.0);
        boolean bandageRoll = ItemSkinSelectionService.isSelected(player, ID, "skin_bandage_roll");
        boolean magicPotion = ItemSkinSelectionService.isSelected(player, ID, "skin_magic_potion");
        double maxHealth = Objects.requireNonNull(player.getAttribute(org.bukkit.attribute.Attribute.MAX_HEALTH)).getValue();
        double newHealth = Math.min(maxHealth, player.getHealth() + healAmount);
        player.setHealth(newHealth);
        player.sendMessage(Component.text("Healed!", NamedTextColor.GREEN));
        player.getWorld().spawnParticle(Particle.TOTEM_OF_UNDYING, player.getLocation().add(0, 1, 0), 10, 0.3, 0.4, 0.3, 0.1);
        player.playSound(player.getLocation(), Sound.ITEM_TOTEM_USE, 0.3f, 1.5f);
        if (bandageRoll) {
            player.getWorld().spawnParticle(Particle.CLOUD, player.getLocation().add(0, 1, 0), 12, 0.25, 0.3, 0.25, 0.02);
            player.getWorld().spawnParticle(Particle.BLOCK, player.getLocation().add(0, 1, 0), 6, 0.2, 0.25, 0.2,
                    Material.WHITE_WOOL.createBlockData());
            player.playSound(player.getLocation(), Sound.ITEM_HONEY_BOTTLE_DRINK, 0.45f, 1.3f);
        } else if (magicPotion) {
            player.getWorld().spawnParticle(Particle.WITCH, player.getLocation().add(0, 1, 0), 14, 0.3, 0.35, 0.3, 0.03);
            player.getWorld().spawnParticle(Particle.ENCHANT, player.getLocation().add(0, 1, 0), 10, 0.28, 0.32, 0.28, 0.02);
            player.playSound(player.getLocation(), Sound.ENTITY_WITCH_DRINK, 0.45f, 1.2f);
        }
    }

    public static void cleanupMedkitCharge(UUID playerId) {
        Player player = Bukkit.getPlayer(playerId);
        if (player != null) {
            clearMedkitCharge(player);
            return;
        }

        BukkitTask existingTask = medkitChannelTasks.remove(playerId);
        if (existingTask != null) existingTask.cancel();

        BukkitTask particleTask = medkitChannelParticleTasks.remove(playerId);
        if (particleTask != null) particleTask.cancel();

        medkitChannelXp.remove(playerId);
    }

    private static void clearMedkitCharge(Player player) {
        BukkitTask existingTask = medkitChannelTasks.remove(player.getUniqueId());
        BukkitTask particleTask = medkitChannelParticleTasks.remove(player.getUniqueId());
        if (particleTask != null) particleTask.cancel();

        XpProgressHelper.SavedXp savedXp = medkitChannelXp.remove(player.getUniqueId());
        XpProgressHelper.stopAndRestore(player, existingTask, savedXp);
    }
}
