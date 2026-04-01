package de.thecoolcraft11.hideAndSeek.perk;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.perk.definition.PerkDefinition;
import de.thecoolcraft11.hideAndSeek.perk.definition.PerkTarget;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PerkStateManager {

    private final HideAndSeek plugin;
    private final Map<UUID, Set<String>> purchased = new ConcurrentHashMap<>();
    private final Map<String, BukkitTask> activeTasks = new ConcurrentHashMap<>();

    public final Map<UUID, Integer> absorptionHearts = new ConcurrentHashMap<>();
    public final Map<UUID, Long> lastTriggerTime = new ConcurrentHashMap<>();
    public final Map<UUID, Location> scentAnchors = new ConcurrentHashMap<>();

    public final Map<UUID, Boolean> shadowStepTriggered = new ConcurrentHashMap<>();
    public final Map<UUID, BukkitTask> shadowStepChargeTask = new ConcurrentHashMap<>();
    public final Map<UUID, UUID> proximityMeterNearest = new ConcurrentHashMap<>();
    public final Map<UUID, Set<UUID>> trapSenseGlowedEntities = new ConcurrentHashMap<>();
    public final Map<UUID, Set<UUID>> trapSenseShownIndicators = new ConcurrentHashMap<>();

    public PerkStateManager(HideAndSeek plugin) {
        this.plugin = plugin;
    }

    public boolean purchase(Player player, PerkDefinition perk) {
        UUID id = player.getUniqueId();
        boolean isSeekerPerk = perk.getTarget() == PerkTarget.SEEKER;

        if (!isSeekerPerk && hasPurchased(id, perk.getId())) {
            player.sendMessage(Component.text("You already have this perk.", NamedTextColor.RED));
            return false;
        }

        int cost = plugin.getSettingRegistry().get("perks.perk." + perk.getId() + ".cost", perk.getCost());
        int balance = HideAndSeek.getDataController().getPoints(id);
        if (balance < cost) {
            player.sendMessage(Component.text("Not enough points! Need " + cost + ", have " + balance + ".", NamedTextColor.RED));
            return false;
        }

        HideAndSeek.getDataController().addPoints(id, -cost);
        purchased.computeIfAbsent(id, ignored -> new HashSet<>()).add(perk.getId());
        perk.onPurchase(player, plugin);

        player.sendMessage(Component.text()
                .append(Component.text("Perk activated: ", NamedTextColor.GOLD))
                .append(perk.getDisplayName())
                .build());

        plugin.getPerkShopUI().refreshForPlayer(player);
        return true;
    }

    public boolean hasPurchased(UUID playerId, String perkId) {
        return purchased.getOrDefault(playerId, Set.of()).contains(perkId);
    }

    public void removePurchased(UUID playerId, String perkId) {
        Set<String> perks = purchased.get(playerId);
        if (perks == null) {
            return;
        }
        perks.remove(perkId);
        if (perks.isEmpty()) {
            purchased.remove(playerId);
        }
    }

    public void scheduleExpiry(Player player, PerkDefinition perk, long durationTicks) {
        String key = player.getUniqueId() + ":" + perk.getId();
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            perk.onExpire(player, plugin);
            activeTasks.remove(key);
            removePurchased(player.getUniqueId(), perk.getId());
        }, durationTicks);
        activeTasks.put(key, task);
    }

    public void storeTask(Player player, String perkId, BukkitTask task) {
        String key = player.getUniqueId() + ":" + perkId;
        BukkitTask previous = activeTasks.put(key, task);
        if (previous != null) {
            previous.cancel();
        }
    }

    public void cancelTask(Player player, String perkId) {
        BukkitTask task = activeTasks.remove(player.getUniqueId() + ":" + perkId);
        if (task != null) {
            task.cancel();
        }
    }

    public void clearAll() {
        for (Map.Entry<UUID, Set<String>> entry : purchased.entrySet()) {
            Player p = Bukkit.getPlayer(entry.getKey());
            if (p == null) {
                continue;
            }
            for (String perkId : entry.getValue()) {
                plugin.getPerkRegistry().getAllPerks().stream()
                        .filter(pd -> pd.getId().equals(perkId))
                        .findFirst()
                        .ifPresent(pd -> pd.onExpire(p, plugin));
            }
        }

        activeTasks.values().forEach(task -> {
            try {
                task.cancel();
            } catch (Exception ignored) {
            }
        });
        activeTasks.clear();

        for (BukkitTask task : shadowStepChargeTask.values()) {
            try {
                task.cancel();
            } catch (Exception ignored) {
            }
        }

        purchased.clear();
        absorptionHearts.clear();
        lastTriggerTime.clear();
        scentAnchors.clear();
        shadowStepTriggered.clear();
        shadowStepChargeTask.clear();
        proximityMeterNearest.clear();
        trapSenseGlowedEntities.clear();
        trapSenseShownIndicators.clear();
    }
}


