package de.thecoolcraft11.hideAndSeek.items.seeker;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.items.api.GameItem;
import de.thecoolcraft11.hideAndSeek.items.api.ItemStateManager;
import de.thecoolcraft11.minigameframework.items.CustomItemBuilder;
import de.thecoolcraft11.minigameframework.items.ItemActionType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

public class SeekerAssistantItem implements GameItem {

    public static final String ID = "has_seeker_assistant";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public ItemStack createItem(HideAndSeek plugin) {
        ItemStack item = new ItemStack(Material.ZOMBIE_HEAD);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(Component.text("Seeker's Assistant")
                .color(NamedTextColor.RED)
                .decorate(TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, false));

        List<Component> lore = new ArrayList<>(List.of(
                Component.text("Summon a hunting assistant to track down hiders.", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false),
                Component.text("Max 2 active at the same time.", NamedTextColor.GRAY)
                        .decoration(TextDecoration.ITALIC, false),
                Component.empty(),
                Component.text("LEGENDARY", NamedTextColor.GOLD)
                        .decorate(TextDecoration.BOLD)
                        .decoration(TextDecoration.ITALIC, false)
        ));

        if (!plugin.getNmsAdapter().hasNmsCapabilities()) {
            lore.add(Component.text("Not available on this server version", NamedTextColor.DARK_RED)
                    .decoration(TextDecoration.ITALIC, false));
        }

        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    @Override
    public String getDescription(HideAndSeek plugin) {
        return "Summons a hunter assistant that tracks and shoots at visible hiders.";
    }

    @Override
    public void register(HideAndSeek plugin) {
        if (!plugin.getNmsAdapter().hasNmsCapabilities()) {
            return;
        }

        int cooldownSeconds = plugin.getSettingRegistry().get("seeker-items.assistant.cooldown", 120);

        plugin.getCustomItemManager().registerItem(
                new CustomItemBuilder(createItem(plugin), getId())
                        .withAction(ItemActionType.RIGHT_CLICK_AIR, context -> handleUse(context.getPlayer(), plugin))
                        .withAction(ItemActionType.RIGHT_CLICK_BLOCK, context -> handleUse(context.getPlayer(), plugin))
                        .withDescription(getDescription(plugin))
                        .withDropPrevention(true)
                        .withCraftPrevention(true)
                        .withVanillaCooldown(cooldownSeconds * 20)
                        .withCustomCooldown((long) cooldownSeconds * 1000L)
                        .withVanillaCooldownDisplay(true)
                        .allowOffHand(false)
                        .allowArmor(false)
                        .cancelDefaultAction(true)
                        .build()
        );
    }

    private void handleUse(Player player, HideAndSeek plugin) {
        if (!plugin.getNmsAdapter().hasNmsCapabilities()) {
            player.sendMessage(Component.text("The Seeker's Assistant is not available on this server version.", NamedTextColor.RED));
            return;
        }

        if (!"seeking".equalsIgnoreCase(plugin.getStateManager().getCurrentPhaseId())) {
            player.sendMessage(Component.text("The Seeker's Assistant can only be summoned during seeking.", NamedTextColor.RED));
            return;
        }

        UUID seekerId = player.getUniqueId();
        ItemStateManager.pruneInvalidAssistants(seekerId);
        List<UUID> active = ItemStateManager.activeAssistants.computeIfAbsent(seekerId, ignored -> new CopyOnWriteArrayList<>());
        int max = plugin.getSettingRegistry().get("seeker-items.assistant.max-per-seeker", 2);
        if (active.size() >= max) {
            player.sendMessage(Component.text("You already have " + max + " assistants active!", NamedTextColor.RED));
            return;
        }

        Location spawn = player.getLocation();
        Entity assistant = plugin.getNmsAdapter().spawnSeekerAssistant(plugin, player, spawn);
        if (assistant == null) {
            player.sendMessage(Component.text("Failed to summon assistant.", NamedTextColor.RED));
            return;
        }

        UUID assistantId = assistant.getUniqueId();
        active.add(assistantId);
        ItemStateManager.assistantOrigins.put(assistantId, spawn.clone());
        ItemStateManager.assistantSpawnTimes.put(assistantId, System.currentTimeMillis());
        ItemStateManager.assistantHitCounts.put(assistantId, 0);

        int lifetimeSeconds = plugin.getSettingRegistry().get("seeker-items.assistant.lifetime", 90);
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            Entity e = plugin.getServer().getEntity(assistantId);
            if (e != null && e.isValid()) {
                e.remove();
            }
            ItemStateManager.removeAssistant(assistantId);
        }, lifetimeSeconds * 20L);

        player.playSound(player.getLocation(), Sound.ENTITY_EVOKER_CAST_SPELL, 1.0f, 0.8f);
        player.playSound(player.getLocation(), Sound.ENTITY_WITHER_AMBIENT, 0.6f, 1.5f);

        player.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, player.getLocation(), 20, 0.4, 0.6, 0.4, 0.03);
        player.getWorld().spawnParticle(Particle.ELECTRIC_SPARK, player.getLocation(), 16, 0.3, 0.4, 0.3, 0.02);
        player.getWorld().spawnParticle(Particle.END_ROD, player.getLocation(), 10, 0.3, 0.5, 0.3, 0.01);

        player.sendActionBar(Component.text()
                .append(Component.text("Assistant summoned! ", NamedTextColor.RED))
                .append(Component.text("(" + lifetimeSeconds + "s lifetime)", NamedTextColor.GRAY))
                .build());
    }

    @Override
    public Set<String> getConfigKeys() {
        return Set.of(
                "seeker-items.assistant.cooldown",
                "seeker-items.assistant.lifetime",
                "seeker-items.assistant.health",
                "seeker-items.assistant.speed",
                "seeker-items.assistant.max-per-seeker"
        );
    }
}



