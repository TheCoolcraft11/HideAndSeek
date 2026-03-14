package de.thecoolcraft11.hideAndSeek.items.hider;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.items.ItemSkinSelectionService;
import de.thecoolcraft11.hideAndSeek.items.api.GameItem;
import de.thecoolcraft11.hideAndSeek.util.points.PointAction;
import de.thecoolcraft11.minigameframework.items.CustomItemBuilder;
import de.thecoolcraft11.minigameframework.items.ItemActionType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;
import java.util.Set;

public class SoundItem implements GameItem {
    public static final String ID = "has_hider_sound";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public ItemStack createItem(HideAndSeek plugin) {
        ItemStack item = new ItemStack(Material.CAT_SPAWN_EGG);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.displayName(Component.text("Cat Sound", NamedTextColor.AQUA, TextDecoration.BOLD)
                    .decoration(TextDecoration.ITALIC, false));
            meta.lore(List.of(
                    Component.text("Right click to play a sound", NamedTextColor.GRAY)
                            .decoration(TextDecoration.ITALIC, false)
            ));
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            item.setItemMeta(meta);
        }

        return item;
    }

    @Override
    public String getDescription(HideAndSeek plugin) {
        int points = plugin.getPointService().getInt("points.hider.taunt.small", 25);
        return String.format("Play a loud cat taunt for all players, grants %d points.", points);
    }

    @Override
    public void register(HideAndSeek plugin) {
        int soundCooldown = plugin.getSettingRegistry().get("hider-items.sound.cooldown", 4);
        plugin.getCustomItemManager().registerItem(new CustomItemBuilder(createItem(plugin), getId())
                .withAction(ItemActionType.RIGHT_CLICK_BLOCK, context -> playSoundForAll(context.getLocation(), plugin, context.getPlayer()))
                .withAction(ItemActionType.SHIFT_RIGHT_CLICK_BLOCK, context -> playSoundForAll(context.getLocation(), plugin, context.getPlayer()))
                .withVanillaCooldown(soundCooldown * 20)
                .withCustomCooldown(soundCooldown * 1000L)
                .withVanillaCooldownDisplay(true)
                .withDescription(getDescription(plugin))
                .withDropPrevention(true)
                .withCraftPrevention(true)
                .allowOffHand(false)
                .allowArmor(false)
                .cancelDefaultAction(true)
                .build());
    }

    private static void playSoundForAll(Location location, HideAndSeek plugin, Player hider) {
        int tauntPoints = plugin.getPointService().award(hider.getUniqueId(), PointAction.HIDER_TAUNT_SMALL);
        String variantId = ItemSkinSelectionService.getSelectedVariant(hider, ID);
        String variantKey = variantId == null || variantId.isBlank() ? "default" : variantId;
        Sound sound = Sound.ENTITY_CAT_AMBIENT;
        Particle accentParticle = Particle.HEART;
        double baseVolume = plugin.getSettingRegistry().get("hider-items.sound.volume", 0.75);
        double basePitch = plugin.getSettingRegistry().get("hider-items.sound.pitch", 0.8);
        int baseNoteParticles = plugin.getSettingRegistry().get("hider-items.sound.note-particles", 8);
        int baseAccentParticles = plugin.getSettingRegistry().get("hider-items.sound.accent-particles", 6);

        if (ItemSkinSelectionService.isSelected(hider, ID, "skin_megaphone")) {
            sound = Sound.BLOCK_NOTE_BLOCK_COW_BELL;
            accentParticle = Particle.GLOW;
        } else if (ItemSkinSelectionService.isSelected(hider, ID, "skin_rubber_chicken")) {
            sound = Sound.ENTITY_CHICKEN_AMBIENT;
            accentParticle = Particle.HAPPY_VILLAGER;
        }

        double volumeMultiplier = plugin.getSettingRegistry().get("hider-items.sound.variants." + variantKey + ".volume-multiplier", 1.0);
        double pitchMultiplier = plugin.getSettingRegistry().get("hider-items.sound.variants." + variantKey + ".pitch-multiplier", 1.0);
        double particleMultiplier = plugin.getSettingRegistry().get("hider-items.sound.variants." + variantKey + ".particle-multiplier", 1.0);

        double volume = Math.max(0.05, baseVolume * volumeMultiplier);
        double pitch = Math.max(0.1, basePitch * pitchMultiplier);
        int noteParticles = Math.max(1, (int) Math.round(baseNoteParticles * particleMultiplier));
        int accentParticles = Math.max(1, (int) Math.round(baseAccentParticles * particleMultiplier));

        hider.sendMessage(Component.text("You used a taunt!", NamedTextColor.GREEN));
        hider.sendMessage(Component.text("+" + tauntPoints + " points", NamedTextColor.GOLD));

        Location particleLoc = location.clone().add(0.5, 1.0, 0.5);
        hider.getWorld().spawnParticle(Particle.NOTE, particleLoc, noteParticles, 0.3, 0.3, 0.3, 1.0);
        hider.getWorld().spawnParticle(accentParticle, particleLoc, accentParticles, 0.2, 0.2, 0.2, 0);

        for (Player target : Bukkit.getOnlinePlayers()) {
            target.playSound(location, sound, (float) volume, (float) pitch);
        }
    }

    @Override
    public Set<String> getConfigKeys() {
        return Set.of("hider-items.sound.cooldown");
    }
}
