package de.thecoolcraft11.hideAndSeek.items.hider;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.items.api.GameItem;
import de.thecoolcraft11.hideAndSeek.util.XpProgressHelper;
import de.thecoolcraft11.minigameframework.items.CustomItemBuilder;
import de.thecoolcraft11.minigameframework.items.ItemActionType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.Set;

import static de.thecoolcraft11.hideAndSeek.items.api.ItemStateManager.invisibilityCloakXpTasks;

public class InvisibilityCloakItem implements GameItem {
    public static final String ID = "has_hider_invisibility_cloak";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public ItemStack createItem(HideAndSeek plugin) {
        ItemStack item = new ItemStack(Material.PHANTOM_MEMBRANE);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.displayName(Component.text("Invisibility Cloak", NamedTextColor.DARK_PURPLE, TextDecoration.BOLD)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.text("Right click to become invisible", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false)
            ));
            meta.addEnchant(org.bukkit.enchantments.Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(org.bukkit.inventory.ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);
        }
        return item;
    }

    private static void useInvisibilityCloak(Player player, HideAndSeek plugin) {
        if (!HideAndSeek.getDataController().getHiders().contains(player.getUniqueId())) {
            player.sendMessage(Component.text("Only hiders can use this item.", NamedTextColor.RED));
            return;
        }

        int duration = plugin.getSettingRegistry().get("hider-items.invisibility-cloak.duration", 8);

        var gameModeResult = plugin.getSettingService().getSetting("game.gametype");
        Object gameModeObj = gameModeResult.isSuccess() ? gameModeResult.getValue() : null;
        String gameMode = gameModeObj != null ? gameModeObj.toString() : "";

        if ("BLOCK".equals(gameMode)) {
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.INVISIBILITY,
                    duration * 20,
                    0,
                    false,
                    true,
                    true
            ));

            BlockDisplay display = HideAndSeek.getDataController().getBlockDisplay(player.getUniqueId());
            if (display != null && display.isValid()) {
                display.setVisibleByDefault(false);
            }
        } else {
            player.addPotionEffect(new PotionEffect(
                    PotionEffectType.INVISIBILITY,
                    duration * 20,
                    0,
                    false,
                    true,
                    true
            ));
        }

        player.sendMessage(Component.text("You are now invisible!", NamedTextColor.AQUA));

        Location loc = player.getLocation().add(0, 1, 0);
        player.getWorld().spawnParticle(Particle.POOF, loc, 30, 0.5, 0.5, 0.5, 0.15);
        player.getWorld().spawnParticle(Particle.GLOW, loc, 15, 0.4, 0.4, 0.4, 0.1);
        player.playSound(player.getLocation(), Sound.ENTITY_PHANTOM_FLAP, 1.0f, 1.5f);


        BukkitTask prevXpTask = invisibilityCloakXpTasks.remove(player.getUniqueId());
        XpProgressHelper.stopAndClear(player, prevXpTask);

        XpProgressHelper.SavedXp savedXp = XpProgressHelper.saveXp(player);
        BukkitTask xpTask = XpProgressHelper.start(plugin, player, duration * 20L, XpProgressHelper.Mode.COUNTDOWN, duration);
        invisibilityCloakXpTasks.put(player.getUniqueId(), xpTask);

        new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = duration * 20;

            @Override
            public void run() {
                if (!player.isOnline() || ticks >= maxTicks) {
                    cancel();


                    BukkitTask t = invisibilityCloakXpTasks.remove(player.getUniqueId());
                    XpProgressHelper.stopAndRestore(player, t, savedXp);

                    if ("BLOCK".equals(gameMode)) {
                        BlockDisplay d = HideAndSeek.getDataController().getBlockDisplay(player.getUniqueId());
                        if (d != null && d.isValid()) {
                            d.setVisibleByDefault(true);
                        }
                    }
                    return;
                }

                if (ticks % 10 == 0) {
                    Location particleLoc = player.getLocation().add(0, 1, 0);
                    player.getWorld().spawnParticle(Particle.SOUL, particleLoc, 2, 0.2, 0.2, 0.2, 0.02);
                }

                ticks++;
            }
        }.runTaskTimer(plugin, 1L, 1L);
    }

    @Override
    public String getDescription(HideAndSeek plugin) {
        Number duration = plugin.getSettingRegistry().get("hider-items.invisibility-cloak.duration", 8);
        return String.format("Turn invisible for %ds.", duration.intValue());
    }

    @Override
    public Set<String> getConfigKeys() {
        return Set.of("hider-items.invisibility-cloak.cooldown");
    }

    @Override
    public void register(HideAndSeek plugin) {
        int invisibilityCloakCooldown = plugin.getSettingRegistry().get("hider-items.invisibility-cloak.cooldown", 20);
        plugin.getCustomItemManager().registerItem(new CustomItemBuilder(createItem(plugin), getId())
                .withAction(ItemActionType.RIGHT_CLICK_AIR, context -> useInvisibilityCloak(context.getPlayer(), plugin))
                .withAction(ItemActionType.RIGHT_CLICK_BLOCK, context -> useInvisibilityCloak(context.getPlayer(), plugin))
                .withDescription(getDescription(plugin))
                .withDropPrevention(true)
                .withCraftPrevention(true)
                .withVanillaCooldown(invisibilityCloakCooldown * 20)
                .withCustomCooldown(invisibilityCloakCooldown * 1000L)
                .withVanillaCooldownDisplay(true)
                .allowOffHand(false)
                .allowArmor(false)
                .cancelDefaultAction(true)
                .build());
    }
}
