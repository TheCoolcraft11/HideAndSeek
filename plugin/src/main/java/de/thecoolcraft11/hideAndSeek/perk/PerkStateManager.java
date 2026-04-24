package de.thecoolcraft11.hideAndSeek.perk;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.perk.definition.DelayedActivationPerk;
import de.thecoolcraft11.hideAndSeek.perk.definition.PerkDefinition;
import de.thecoolcraft11.hideAndSeek.perk.definition.PerkTarget;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PerkStateManager {

    private final HideAndSeek plugin;
    private final Map<UUID, Set<String>> purchased = new ConcurrentHashMap<>();
    private final Map<String, BukkitTask> activeTasks = new ConcurrentHashMap<>();
    private final Map<String, Set<UUID>> finiteOwners = new ConcurrentHashMap<>();
    private final Map<UUID, Map<String, Long>> purchaseCooldownEnds = new ConcurrentHashMap<>();

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

    public void purchase(Player player, PerkDefinition perk) {
        UUID id = player.getUniqueId();
        boolean isSeekerPerk = perk.getTarget() == PerkTarget.SEEKER;
        boolean isFinitePerk = perk.getTarget() == PerkTarget.HIDER
                || (isSeekerPerk && plugin.getPerkRegistry().isFiniteSeekerPerk(perk.getId()));

        if (isFinitePerk && hasPurchased(id, perk.getId())) {
            player.sendMessage(Component.text("You already have this perk.", NamedTextColor.RED));
            return;
        }

        if (isFinitePerk) {
            int limit = plugin.getPerkRegistry().getFinitePlayerLimit(perk.getTarget());
            if (limit > 0) {
                Set<UUID> owners = finiteOwners.computeIfAbsent(perk.getId(), ignored -> ConcurrentHashMap.newKeySet());
                if (owners.size() >= limit && !owners.contains(id)) {
                    player.sendMessage(Component.text("That perk is already at the global player limit.", NamedTextColor.RED));
                    return;
                }
            }
        } else if (isSeekerPerk) {
            long remainingTicks = getPurchaseCooldownRemainingTicks(id, perk.getId());
            if (remainingTicks > 0) {
                long seconds = Math.max(1L, (remainingTicks + 19L) / 20L);
                player.sendMessage(Component.text("You can buy that perk again in " + seconds + "s.", NamedTextColor.RED));
                return;
            }
        }

        int cost = plugin.getSettingRegistry().get("perks.perk." + perk.getId() + ".cost", perk.getCost());
        int balance = HideAndSeek.getDataController().getPoints(id);
        if (balance < cost) {
            player.sendMessage(Component.text("Not enough points! Need " + cost + ", have " + balance + ".", NamedTextColor.RED));
            return;
        }

        HideAndSeek.getDataController().addPoints(id, -cost);
        purchased.computeIfAbsent(id, ignored -> new HashSet<>()).add(perk.getId());

        if (isFinitePerk) {
            finiteOwners.computeIfAbsent(perk.getId(), ignored -> ConcurrentHashMap.newKeySet()).add(id);
        } else if (isSeekerPerk) {
            setPurchaseCooldown(id, perk);
        }

        try {
            perk.onPurchase(player, plugin);
        } catch (RuntimeException ex) {
            player.sendMessage(Component.text("That perk could not be activated.", NamedTextColor.RED));
            player.sendMessage(Component.text("Your points were refunded.", NamedTextColor.RED));
            refundPurchase(id, perk.getId(), cost);
            if (!(perk instanceof DelayedActivationPerk)) {
                plugin.getLogger().warning("Perk activation failed for " + perk.getId() + ": " + ex.getMessage());
            }
            return;
        }

        if (!(perk instanceof DelayedActivationPerk)) {
            player.sendMessage(Component.text()
                    .append(Component.text("Perk activated: ", NamedTextColor.GOLD))
                    .append(perk.getDisplayName())
                    .build());
        }

        if (isFinitePerk) {
            plugin.getPerkShopUI().refreshAllPlayersWithShopItems();
        } else {
            plugin.getPerkShopUI().refreshForPlayer(player);
        }
        var statsService = de.thecoolcraft11.hideAndSeek.playerdata.PlayerStatsService.getActive();
        if (statsService != null) {
            statsService.recordPerkUsed(id, perk.getId());
        }
    }

    public boolean grantDebug(Player player, PerkDefinition perk, boolean bypassCooldown) {
        UUID id = player.getUniqueId();
        boolean isSeekerPerk = perk.getTarget() == PerkTarget.SEEKER;
        boolean isFinitePerk = perk.getTarget() == PerkTarget.HIDER
                || (isSeekerPerk && plugin.getPerkRegistry().isFiniteSeekerPerk(perk.getId()));

        if (isFinitePerk && hasPurchased(id, perk.getId())) {
            player.sendMessage(Component.text("You already have this perk.", NamedTextColor.RED));
            return false;
        }

        if (isFinitePerk) {
            int limit = plugin.getPerkRegistry().getFinitePlayerLimit(perk.getTarget());
            if (limit > 0) {
                Set<UUID> owners = finiteOwners.computeIfAbsent(perk.getId(), ignored -> ConcurrentHashMap.newKeySet());
                if (owners.size() >= limit && !owners.contains(id)) {
                    player.sendMessage(Component.text("That perk is already at the global player limit.", NamedTextColor.RED));
                    return false;
                }
            }
        } else if (isSeekerPerk && !bypassCooldown) {
            long remainingTicks = getPurchaseCooldownRemainingTicks(id, perk.getId());
            if (remainingTicks > 0) {
                long seconds = Math.max(1L, (remainingTicks + 19L) / 20L);
                player.sendMessage(Component.text("You can buy that perk again in " + seconds + "s.", NamedTextColor.RED));
                return false;
            }
        }

        purchased.computeIfAbsent(id, ignored -> new HashSet<>()).add(perk.getId());
        if (isFinitePerk) {
            finiteOwners.computeIfAbsent(perk.getId(), ignored -> ConcurrentHashMap.newKeySet()).add(id);
        } else if (isSeekerPerk) {
            setPurchaseCooldown(id, perk);
        }

        try {
            perk.onPurchase(player, plugin);
        } catch (RuntimeException ex) {
            player.sendMessage(Component.text("That perk could not be activated.", NamedTextColor.RED));
            removePurchased(id, perk.getId());
            if (!(perk instanceof DelayedActivationPerk)) {
                plugin.getLogger().warning("Perk activation failed for " + perk.getId() + ": " + ex.getMessage());
            }
            return false;
        }

        if (!(perk instanceof DelayedActivationPerk)) {
            player.sendMessage(Component.text()
                    .append(Component.text("Perk activated: ", NamedTextColor.GOLD))
                    .append(perk.getDisplayName())
                    .build());
        }

        if (isFinitePerk) {
            plugin.getPerkShopUI().refreshAllPlayersWithShopItems();
        } else {
            plugin.getPerkShopUI().refreshForPlayer(player);
        }
        var statsService = de.thecoolcraft11.hideAndSeek.playerdata.PlayerStatsService.getActive();
        if (statsService != null) {
            statsService.recordPerkUsed(id, perk.getId());
        }
        return true;
    }

    public void refundPurchase(UUID playerId, String perkId, int amount) {
        if (amount > 0) {
            HideAndSeek.getDataController().addPoints(playerId, amount);
        }
        removePurchased(playerId, perkId);
    }

    public boolean hasPurchased(UUID playerId, String perkId) {
        return purchased.getOrDefault(playerId, Set.of()).contains(perkId);
    }

    public void removePurchased(UUID playerId, String perkId) {
        BukkitTask task = activeTasks.remove(playerId + ":" + perkId);
        if (task != null) {
            task.cancel();
        }

        Set<String> perks = purchased.get(playerId);
        if (perks == null) {
            return;
        }
        perks.remove(perkId);
        if (perks.isEmpty()) {
            purchased.remove(playerId);
        }

        Set<UUID> owners = finiteOwners.get(perkId);
        if (owners != null) {
            owners.remove(playerId);
            if (owners.isEmpty()) {
                finiteOwners.remove(perkId);
            }
        }

        Map<String, Long> cooldowns = purchaseCooldownEnds.get(playerId);
        if (cooldowns != null) {
            cooldowns.remove(perkId);
            if (cooldowns.isEmpty()) {
                purchaseCooldownEnds.remove(playerId);
            }
        }
    }

    public int clearPurchasedPerks(UUID playerId, PerkTarget targetFilter, boolean refundPoints) {
        Set<String> ownedPerks = purchased.get(playerId);
        if (ownedPerks == null || ownedPerks.isEmpty()) {
            return 0;
        }

        Player player = Bukkit.getPlayer(playerId);
        int refundedPoints = 0;

        for (String perkId : Set.copyOf(ownedPerks)) {
            PerkDefinition perk = plugin.getPerkRegistry().getAllPerks().stream()
                    .filter(definition -> definition.getId().equals(perkId))
                    .findFirst()
                    .orElse(null);

            if (perk == null) {
                removePurchased(playerId, perkId);
                continue;
            }

            if (targetFilter != null && perk.getTarget() != targetFilter) {
                continue;
            }

            if (player != null && player.isOnline()) {
                try {
                    perk.onExpire(player, plugin);
                } catch (RuntimeException ex) {
                    plugin.getLogger().warning("Perk expiry failed for " + perk.getId() + ": " + ex.getMessage());
                }
            }

            if (refundPoints) {
                refundedPoints += plugin.getSettingRegistry().get("perks.perk." + perk.getId() + ".cost", perk.getCost());
            }

            removePurchased(playerId, perkId);
        }

        if (refundPoints && refundedPoints > 0) {
            HideAndSeek.getDataController().addPoints(playerId, refundedPoints);
        }

        if (player != null && player.isOnline()) {
            plugin.getPerkShopUI().refreshForPlayer(player);
        }

        return refundedPoints;
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

    public boolean isFinitePurchaseLocked(UUID playerId, PerkDefinition perk) {
        if (perk == null) {
            return false;
        }

        boolean finite = perk.getTarget() == PerkTarget.HIDER
                || (perk.getTarget() == PerkTarget.SEEKER && plugin.getPerkRegistry().isFiniteSeekerPerk(perk.getId()));
        if (!finite) {
            return false;
        }

        if (hasPurchased(playerId, perk.getId())) {
            return true;
        }

        int limit = plugin.getPerkRegistry().getFinitePlayerLimit(perk.getTarget());
        if (limit <= 0) {
            return false;
        }

        Set<UUID> owners = finiteOwners.get(perk.getId());
        return owners != null && owners.size() >= limit;
    }

    public int getFiniteBuyerCount(String perkId) {
        Set<UUID> owners = finiteOwners.get(perkId);
        return owners == null ? 0 : owners.size();
    }

    public UUID getFiniteOwnerUUID(String perkId) {
        Set<UUID> owners = finiteOwners.get(perkId);
        if (owners != null && !owners.isEmpty()) {
            return owners.iterator().next();
        }
        return null;
    }

    public Map<UUID, Set<String>> getPurchased() {
        return purchased;
    }

    public long getPurchaseCooldownRemainingTicks(UUID playerId, String perkId) {
        Map<String, Long> cooldowns = purchaseCooldownEnds.get(playerId);
        if (cooldowns == null) {
            return 0L;
        }

        Long endsAt = cooldowns.get(perkId);
        if (endsAt == null) {
            return 0L;
        }

        long remainingMs = endsAt - System.currentTimeMillis();
        if (remainingMs <= 0L) {
            cooldowns.remove(perkId);
            if (cooldowns.isEmpty()) {
                purchaseCooldownEnds.remove(playerId);
            }
            return 0L;
        }

        return (remainingMs + 49L) / 50L;
    }

    private void setPurchaseCooldown(UUID playerId, PerkDefinition perk) {
        long cooldownTicks = Math.max(0L, plugin.getPerkRegistry().getRebuyCooldownTicks(perk));
        if (cooldownTicks == 0L) {
            return;
        }

        purchaseCooldownEnds
                .computeIfAbsent(playerId, ignored -> new ConcurrentHashMap<>())
                .put(perk.getId(), System.currentTimeMillis() + (cooldownTicks * 50L));
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
        finiteOwners.clear();
        purchaseCooldownEnds.clear();
    }
}

