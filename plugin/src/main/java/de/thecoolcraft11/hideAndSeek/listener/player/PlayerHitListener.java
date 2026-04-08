package de.thecoolcraft11.hideAndSeek.listener.player;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.items.HiderItems;
import de.thecoolcraft11.hideAndSeek.items.SeekerItems;
import de.thecoolcraft11.hideAndSeek.items.api.ItemStateManager;
import de.thecoolcraft11.hideAndSeek.items.effects.KillEffectService;
import de.thecoolcraft11.hideAndSeek.items.effects.death.DeathMessageService;
import de.thecoolcraft11.hideAndSeek.model.GameStyleEnum;
import de.thecoolcraft11.hideAndSeek.perk.definition.PerkTarget;
import de.thecoolcraft11.hideAndSeek.perk.impl.seeker.DeathZonePerk;
import de.thecoolcraft11.hideAndSeek.perk.impl.seeker.RelocatePerk;
import de.thecoolcraft11.hideAndSeek.util.PlayerStateResetUtil;
import de.thecoolcraft11.hideAndSeek.util.points.PointAction;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.damage.DamageSource;
import org.bukkit.damage.DamageType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scoreboard.Team;

import java.time.Duration;
import java.util.*;

import static de.thecoolcraft11.hideAndSeek.items.hider.TotemItem.isTotemActive;
import static de.thecoolcraft11.hideAndSeek.items.hider.TotemItem.reviveWithTotem;
import static de.thecoolcraft11.hideAndSeek.items.seeker.CurseSpellItem.applyCurseToHider;
import static de.thecoolcraft11.hideAndSeek.items.seeker.CurseSpellItem.isCurseActive;

public class PlayerHitListener implements Listener {
    private final Map<UUID, EnvironmentalDeathCause> environmentalDeaths = new HashMap<>();
    private final Map<UUID, UUID> environmentalDeathAttributors = new HashMap<>();

    private final HideAndSeek plugin;
    private final KillEffectService killEffectService;
    private final DeathMessageService deathMessageService;
    private final Map<UUID, Long> hidersBorderExitTime = new HashMap<>();
    private final Map<UUID, Long> lastDamageTime = new HashMap<>();


    public PlayerHitListener(HideAndSeek plugin) {
        this.plugin = plugin;
        this.killEffectService = new KillEffectService(plugin);
        this.deathMessageService = new DeathMessageService(plugin);
    }

    public void markEnvironmentalDeath(UUID playerId, EnvironmentalDeathCause cause) {
        markEnvironmentalDeath(playerId, cause, null);
    }

    public void markEnvironmentalDeath(UUID playerId, EnvironmentalDeathCause cause, UUID attributedSeekerId) {
        if (playerId == null || cause == null) {
            return;
        }
        environmentalDeaths.put(playerId, cause);
        if (attributedSeekerId != null) {
            environmentalDeathAttributors.put(playerId, attributedSeekerId);
        } else {
            environmentalDeathAttributors.remove(playerId);
        }
    }

    public EnvironmentalDeathCause peekEnvironmentalDeathCause(UUID playerId) {
        return environmentalDeaths.get(playerId);
    }

    public EnvironmentalDeathCause consumeEnvironmentalDeathCause(UUID playerId) {
        return environmentalDeaths.remove(playerId);
    }

    public UUID peekEnvironmentalDeathAttributor(UUID playerId) {
        return environmentalDeathAttributors.get(playerId);
    }

    public void consumeEnvironmentalDeathAttributor(UUID playerId) {
        environmentalDeathAttributors.remove(playerId);
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player deceased = event.getEntity();


        if (!plugin.getStateManager().getCurrentPhaseId().equals("seeking")) {
            return;
        }


        if (HideAndSeek.getDataController().getHiders().contains(deceased.getUniqueId())) {
            event.getDrops().clear();
            event.setKeepInventory(false);
        }

        EnvironmentalDeathCause environmentalCause = consumeEnvironmentalDeathCause(deceased.getUniqueId());
        consumeEnvironmentalDeathAttributor(deceased.getUniqueId());
        if (environmentalCause != null && HideAndSeek.getDataController().getHiders().contains(deceased.getUniqueId())) {

            for (java.util.UUID seekerId : HideAndSeek.getDataController().getSeekers()) {
                plugin.getPointService().award(seekerId, PointAction.SEEKER_ENVIRONMENTAL_ELIMINATION);
            }

            var gameStyleResult = plugin.getSettingService().getSetting("game.style");
            Object gameStyleObj = gameStyleResult.isSuccess() ? gameStyleResult.getValue() : GameStyleEnum.SPECTATOR;
            GameStyleEnum gameStyle = (gameStyleObj instanceof GameStyleEnum) ?
                    (GameStyleEnum) gameStyleObj : GameStyleEnum.SPECTATOR;

            killEffectService.triggerEnvironmentalKillEffect(deceased, deceased.getLocation(), environmentalCause);
            handleHiderElimination(deceased, null, gameStyle);
            return;
        }

        if (event.getDamageSource().getCausingEntity() instanceof Player killer) {


            if (HideAndSeek.getDataController().getSeekers().contains(killer.getUniqueId()) &&
                    HideAndSeek.getDataController().getHiders().contains(deceased.getUniqueId())) {


                var gameStyleResult = plugin.getSettingService().getSetting("game.style");
                Object gameStyleObj = gameStyleResult.isSuccess() ? gameStyleResult.getValue() : GameStyleEnum.SPECTATOR;
                GameStyleEnum gameStyle = (gameStyleObj instanceof GameStyleEnum) ?
                        (GameStyleEnum) gameStyleObj : GameStyleEnum.SPECTATOR;

                killEffectService.triggerKillEffect(killer, deceased, deceased.getLocation());


                Component customKillMessage = deathMessageService.getKillMessage(killer, deceased);
                if (customKillMessage != null) {
                    event.deathMessage(customKillMessage);
                }

                handleHiderElimination(deceased, killer, gameStyle);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamage(EntityDamageEvent event) {
        if (isAssistant(event.getEntity())) {
            EntityDamageEvent.DamageCause cause = event.getCause();
            boolean combatDamage = cause == EntityDamageEvent.DamageCause.ENTITY_ATTACK
                    || cause == EntityDamageEvent.DamageCause.ENTITY_SWEEP_ATTACK
                    || cause == EntityDamageEvent.DamageCause.PROJECTILE;
            if (!combatDamage) {
                event.setCancelled(true);
            }
            return;
        }

        if (!(event.getEntity() instanceof Player)) {
            return;
        }

        if (event.getCause() == EntityDamageEvent.DamageCause.CUSTOM || event.getCause() == EntityDamageEvent.DamageCause.WORLD_BORDER) {
            return;
        }

        if (plugin.getStateManager().getCurrentPhaseId().equals("seeking") || plugin.getStateManager().getCurrentPhaseId().equals("hiding")) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!plugin.getStateManager().getCurrentPhaseId().equals("seeking")) {
            return;
        }

        if (isAssistant(event.getEntity())) {
            Player attacker = resolveAttacker(event);
            if (attacker == null) {
                event.setCancelled(true);
                return;
            }

            boolean attackerIsSeeker = HideAndSeek.getDataController().getSeekers().contains(attacker.getUniqueId());
            boolean attackerIsHider = HideAndSeek.getDataController().getHiders().contains(attacker.getUniqueId());

            if (attackerIsSeeker) {
                event.setCancelled(true);
                return;
            }

            if (!attackerIsHider) {
                event.setCancelled(true);
                return;
            }

            return;
        }

        if (event.getDamager() instanceof Player damager && !(event.getEntity() instanceof Player)) {
            if (HideAndSeek.getDataController().getHiders().contains(damager.getUniqueId())) {
                event.setCancelled(true);
            }
            return;
        }

        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }

        Player attacker = resolveAttacker(event);

        if (attacker == null) {
            event.setCancelled(true);
            return;
        }

        boolean attackerIsHider = HideAndSeek.getDataController().getHiders().contains(attacker.getUniqueId());
        boolean attackerIsSeeker = HideAndSeek.getDataController().getSeekers().contains(attacker.getUniqueId());
        boolean victimIsHider = HideAndSeek.getDataController().getHiders().contains(victim.getUniqueId());
        boolean victimIsSeeker = HideAndSeek.getDataController().getSeekers().contains(victim.getUniqueId());

        if (victimIsHider && HideAndSeek.getDataController().isHidden(victim.getUniqueId())) {
            if (!HideAndSeek.getDataController().isBlockDamageOverrideActive(victim.getUniqueId())) {
                boolean gazeKill = plugin.getSettingRegistry().get("game.seekers.kill-mode").equals("GAZE_KILL");
                plugin.getBlockModeListener().damageHiddenPlayer(attacker, victim.getUniqueId(), gazeKill);
                event.setCancelled(true);
                return;
            }
            event.setCancelled(false);
        }

        if (attackerIsSeeker && victimIsHider) {
            if (isCurseActive(attacker.getUniqueId())) {
                applyCurseToHider(victim, plugin);
            }

            if (isTotemActive(victim.getUniqueId()) && event.getFinalDamage() >= victim.getHealth()) {
                event.setCancelled(true);
                reviveWithTotem(victim);
                return;
            }

            plugin.getPointService().onHiderDamagedBySeeker(attacker, victim, event.getFinalDamage());
            event.setCancelled(false);
            return;
        }

        if (attackerIsHider && victimIsSeeker) {
            event.setCancelled(false);
            return;
        }


        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onAssistantDeath(EntityDeathEvent event) {
        if (!isAssistant(event.getEntity())) {
            return;
        }

        event.getDrops().clear();
        event.setDroppedExp(0);

        UUID assistantId = event.getEntity().getUniqueId();
        Player killer = event.getEntity().getKiller();
        Location loc = event.getEntity().getLocation();

        playAssistantDeathEffects(loc);

        if (killer != null && HideAndSeek.getDataController().getHiders().contains(killer.getUniqueId())) {
            killer.sendMessage(Component.text("You destroyed a Seeker Assistant!", NamedTextColor.GREEN));
        }

        ItemStateManager.removeAssistant(assistantId);
    }

    private void playAssistantDeathEffects(Location loc) {
        if (loc == null || loc.getWorld() == null) {
            return;
        }

        loc.getWorld().spawnParticle(org.bukkit.Particle.SOUL_FIRE_FLAME, loc, 24, 0.4, 0.6, 0.4, 0.05);
        loc.getWorld().spawnParticle(org.bukkit.Particle.SMOKE, loc, 18, 0.3, 0.3, 0.3, 0.02);
        loc.getWorld().spawnParticle(org.bukkit.Particle.ELECTRIC_SPARK, loc, 12, 0.3, 0.3, 0.3, 0.03);
        loc.getWorld().playSound(loc, org.bukkit.Sound.ENTITY_WITHER_DEATH, 0.6f, 1.4f);
        loc.getWorld().playSound(loc, org.bukkit.Sound.ENTITY_BLAZE_DEATH, 0.5f, 0.8f);
    }

    private Player resolveAttacker(EntityDamageByEntityEvent event) {
        if (event.getDamager() instanceof Player player) {
            return player;
        }

        if (event.getDamager() instanceof org.bukkit.entity.Projectile projectile
                && projectile.getShooter() instanceof Player player) {
            return player;
        }

        return null;
    }

    private boolean isAssistant(Entity entity) {
        if (entity == null) {
            return false;
        }

        return entity.getPersistentDataContainer().has(
                new NamespacedKey(plugin, "assistant_entity"),
                PersistentDataType.BOOLEAN
        );
    }

    public void checkWorldBorderDamage() {

        boolean damageEnabled = plugin.getSettingRegistry().get("game.world-border.damage-hiders-outside", true);
        if (!damageEnabled) {
            return;
        }


        if (!plugin.getStateManager().getCurrentPhaseId().equals("seeking")) {

            hidersBorderExitTime.clear();
            lastDamageTime.clear();
            environmentalDeaths.clear();
            environmentalDeathAttributors.clear();
            return;
        }


        int damageTimeoutSeconds = plugin.getSettingRegistry().get("game.world-border.damage-delay-seconds", 10);
        double damageAmount = plugin.getSettingRegistry().get("game.world-border.damage-amount", 2.0);
        int damageCooldownTicks = plugin.getSettingRegistry().get("game.world-border.damage-cooldown-ticks", 20);
        long currentTime = System.currentTimeMillis();


        List<UUID> hidersToCheck = new ArrayList<>(HideAndSeek.getDataController().getHiders());
        for (UUID hiderId : hidersToCheck) {
            Player hider = Bukkit.getPlayer(hiderId);
            if (hider == null || !hider.isOnline()) {
                hidersBorderExitTime.remove(hiderId);
                lastDamageTime.remove(hiderId);
                continue;
            }

            boolean isOutsideBorder = !hider.getWorld().getWorldBorder().isInside(hider.getLocation());

            if (isOutsideBorder) {

                if (!hidersBorderExitTime.containsKey(hiderId)) {
                    hidersBorderExitTime.put(hiderId, currentTime);
                    if (plugin.getDebugSettings().isVerboseLoggingEnabled()) {
                        plugin.getLogger().info(hider.getName() + " went outside world border");
                    }
                }


                long timeOutsideMs = currentTime - hidersBorderExitTime.get(hiderId);
                long timeOutsideSeconds = timeOutsideMs / 1000;

                if (timeOutsideSeconds >= damageTimeoutSeconds) {

                    long lastDamageTick = lastDamageTime.getOrDefault(hiderId, 0L);
                    long ticksSinceLastDamage = (currentTime - lastDamageTick) / 50;

                    if (ticksSinceLastDamage >= damageCooldownTicks) {
                        double currentHealth = hider.getHealth();
                        if (currentHealth - damageAmount <= 0) {
                            markEnvironmentalDeath(hiderId, EnvironmentalDeathCause.WORLD_BORDER);
                        }

                        hider.damage(damageAmount, DamageSource.builder(DamageType.OUTSIDE_BORDER).build());
                        lastDamageTime.put(hiderId, currentTime);
                        if (plugin.getDebugSettings().isVerboseLoggingEnabled()) {
                            plugin.getLogger().info(hider.getName() + " took " + damageAmount + " damage for being outside border (" + timeOutsideSeconds + "s outside)");
                        }
                    }
                }
            } else {

                if (hidersBorderExitTime.containsKey(hiderId)) {
                    if (plugin.getDebugSettings().isVerboseLoggingEnabled()) {
                        plugin.getLogger().info(hider.getName() + " returned inside world border");
                    }
                }
                hidersBorderExitTime.remove(hiderId);
                lastDamageTime.remove(hiderId);
            }
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();


        if (plugin.getStateManager().getCurrentPhaseId().equals("seeking")) {
            var gameStyleResult = plugin.getSettingService().getSetting("game.style");
            Object gameStyleObj = gameStyleResult.isSuccess() ? gameStyleResult.getValue() : GameStyleEnum.SPECTATOR;
            GameStyleEnum gameStyle = (gameStyleObj instanceof GameStyleEnum) ?
                    (GameStyleEnum) gameStyleObj : GameStyleEnum.SPECTATOR;


            if (!HideAndSeek.getDataController().getHiders().contains(player.getUniqueId()) &&
                    HideAndSeek.getDataController().getSeekers().contains(player.getUniqueId())) {

                if (gameStyle == GameStyleEnum.SPECTATOR) {

                    Location deathLocation = player.getLocation();
                    event.setRespawnLocation(deathLocation);

                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        PlayerStateResetUtil.resetPlayerForSpectator(player, false);
                        plugin.getAntiCheatVisibilityListener().refreshSoon();
                    }, 1L);
                } else if (gameStyle == GameStyleEnum.INVASION) {

                    Location deathLocation = player.getLocation();
                    event.setRespawnLocation(deathLocation);

                    Bukkit.getScheduler().runTaskLater(plugin, () -> {
                        PlayerStateResetUtil.resetPlayerCompletely(player, false);
                        SeekerItems.giveItemsWithLoadout(player, plugin);
                        plugin.getPerkShopUI().refreshForPlayer(player);
                        player.sendMessage(Component.text("You were transformed! You're now a seeker!", NamedTextColor.GREEN));
                        plugin.getAntiCheatVisibilityListener().refreshSoon();
                    }, 1L);
                }
            } else if (gameStyle == GameStyleEnum.INFINITE &&
                    HideAndSeek.getDataController().getHiders().contains(player.getUniqueId())) {


                Location roundSpawn = HideAndSeek.getDataController().getRoundSpawnPoint();
                if (roundSpawn != null) {
                    event.setRespawnLocation(roundSpawn);
                }

                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    PlayerStateResetUtil.resetPlayerCompletely(player, false);


                    HiderItems.giveItems(player, plugin, false);
                    plugin.getPerkShopUI().refreshForPlayer(player);

                    player.sendMessage(Component.text("You respawned! Keep hiding!", NamedTextColor.GREEN));
                    plugin.getAntiCheatVisibilityListener().refreshSoon();
                }, 1L);
            }
        }
    }

    private void handleHiderElimination(Player hider, Player seeker, GameStyleEnum gameStyle) {

        int seekerPoints = plugin.getPointService().onHiderEliminated(hider, seeker);

        plugin.getSeekingBossBarService().onHiderEliminated();

        if (seeker != null) {
            seeker.sendMessage(Component.text("+" + seekerPoints + " points for finding " + hider.getName() + "!", NamedTextColor.GOLD));
        }

        if (gameStyle == GameStyleEnum.INVASION || gameStyle == GameStyleEnum.INFINITE) {
            clearHiderPerksAfterElimination(hider);
        }

        switch (gameStyle) {
            case SPECTATOR:

                handleSpectatorMode(hider);
                break;

            case INVASION:

                handleInvasionMode(hider);
                break;

            case INFINITE:


                hider.sendMessage(Component.text("You'll respawn and can continue hiding!", NamedTextColor.GOLD));
                break;
        }
    }

    private void handleSpectatorMode(Player hider) {
        HideAndSeek.getDataController().removeHider(hider.getUniqueId());
        plugin.getUnstuckManager().clearPlayerData(hider.getUniqueId());
        HideAndSeek.getDataController().addSeeker(hider.getUniqueId());


        Team seekerTeam = null;
        for (Team team : plugin.getTeamManager().getAllTeams()) {
            if (!plugin.getTeamManager().isSpectatorTeam(team.getName())) {

                for (UUID seekerId : HideAndSeek.getDataController().getSeekers()) {
                    if (seekerId.equals(hider.getUniqueId())) continue;

                    Player seeker = Bukkit.getPlayer(seekerId);
                    if (seeker != null) {
                        String seekerTeamName = plugin.getTeamManager().getPlayerTeam(seeker);
                        if (seekerTeamName != null && seekerTeamName.equals(team.getName())) {
                            seekerTeam = team;
                            break;
                        }
                    }
                }
                if (seekerTeam != null) break;
            }
        }

        if (seekerTeam != null) {
            plugin.getTeamManager().addPlayerToTeam(hider, seekerTeam.getName());
            if (plugin.getDebugSettings().isVerboseLoggingEnabled()) {
                plugin.getLogger().info("Moved " + hider.getName() + " to seeker team: " + seekerTeam.getName());
            }
        } else {
            plugin.getLogger().warning("Could not find seeker team for eliminated hider!");
        }

        plugin.getTeamManager().removeRole(hider, "hider");
        plugin.getTeamManager().addRole(hider, "seeker");

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            PlayerStateResetUtil.resetPlayerForSpectator(hider, false);


            cleanupBlockModeHider(hider);

            Title title = Title.title(
                    Component.text("YOU WERE FOUND!", NamedTextColor.RED),
                    Component.text("You are now spectating", NamedTextColor.GRAY),
                    Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(3), Duration.ofMillis(500))
            );
            hider.showTitle(title);
            plugin.getAntiCheatVisibilityListener().refreshSoon();
        }, 1L);

        if (plugin.getDebugSettings().isVerboseLoggingEnabled()) {
            plugin.getLogger().info(hider.getName() + " was eliminated and is now spectating (moved to seeker team)");
        }
    }

    private void handleInvasionMode(Player hider) {
        HideAndSeek.getDataController().removeHider(hider.getUniqueId());
        HideAndSeek.getDataController().addSeeker(hider.getUniqueId());

        clearConvertedPlayerAreaWarnings(hider);


        Team seekerTeam = null;
        for (Team team : plugin.getTeamManager().getAllTeams()) {
            if (!plugin.getTeamManager().isSpectatorTeam(team.getName())) {

                for (UUID seekerId : HideAndSeek.getDataController().getSeekers()) {
                    if (seekerId.equals(hider.getUniqueId())) continue;

                    Player seeker = Bukkit.getPlayer(seekerId);
                    if (seeker != null) {
                        String seekerTeamName = plugin.getTeamManager().getPlayerTeam(seeker);
                        if (seekerTeamName != null && seekerTeamName.equals(team.getName())) {
                            seekerTeam = team;
                            break;
                        }
                    }
                }
                if (seekerTeam != null) break;
            }
        }

        if (seekerTeam != null) {
            plugin.getTeamManager().addPlayerToTeam(hider, seekerTeam.getName());
            if (plugin.getDebugSettings().isVerboseLoggingEnabled()) {
                plugin.getLogger().info("Added " + hider.getName() + " to seeker team: " + seekerTeam.getName());
            }
        } else {
            plugin.getLogger().warning("Could not find seeker team for INVASION mode conversion!");
        }

        plugin.getTeamManager().removeRole(hider, "hider");
        plugin.getTeamManager().addRole(hider, "seeker");

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            PlayerStateResetUtil.resetPlayerCompletely(hider, true);
            hider.getInventory().setHelmet(null);


            cleanupBlockModeHider(hider);


            SeekerItems.giveItemsWithLoadout(hider, plugin);
            plugin.getPerkShopUI().refreshForPlayer(hider);


            hider.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.SATURATION,
                    Integer.MAX_VALUE,
                    0,
                    false,
                    false,
                    false
            ));


            Title title = Title.title(
                    Component.text("YOU'RE NOW A SEEKER!", NamedTextColor.RED),
                    Component.text("Help find the remaining hiders!", NamedTextColor.YELLOW),
                    Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(3), Duration.ofMillis(500))
            );
            hider.showTitle(title);
            plugin.getAntiCheatVisibilityListener().refreshSoon();
        }, 1L);

        if (plugin.getDebugSettings().isVerboseLoggingEnabled()) {
            plugin.getLogger().info(hider.getName() + " was eliminated and joined the seekers (INVASION mode)");
        }
    }

    private void cleanupBlockModeHider(Player hider) {
        java.util.UUID hiderId = hider.getUniqueId();


        org.bukkit.entity.BlockDisplay display = HideAndSeek.getDataController().getBlockDisplay(hiderId);
        if (display != null && display.isValid()) {
            display.remove();
        }


        org.bukkit.Location lastLoc = HideAndSeek.getDataController().getLastLocation(hiderId);
        if (lastLoc != null) {
            org.bukkit.block.Block block = lastLoc.getBlock();
            org.bukkit.Material chosenBlock = HideAndSeek.getDataController().getChosenBlock(hiderId);
            if (chosenBlock != null && block.getType() == chosenBlock) {
                block.setType(org.bukkit.Material.AIR);
                HideAndSeek.getDataController().removePlacedBlockKey(block.getLocation());
            }
        }


        hider.removePotionEffect(org.bukkit.potion.PotionEffectType.INVISIBILITY);
    }

    private void clearHiderPerksAfterElimination(Player hider) {
        boolean refund = plugin.getSettingRegistry().get("perks.refund-hider-perks-on-convert", false);
        int refunded = plugin.getPerkStateManager().clearPurchasedPerks(hider.getUniqueId(), PerkTarget.HIDER, refund);
        if (refund && refunded > 0) {
            hider.sendMessage(Component.text("Your hider perks were removed and " + refunded + " points were refunded.", NamedTextColor.YELLOW));
        }
    }

    private void clearConvertedPlayerAreaWarnings(Player player) {
        UUID playerId = player.getUniqueId();
        DeathZonePerk.clearWarningsFor(playerId);
        RelocatePerk.clearWarningsFor(playerId);
    }

    public DeathMessageService getDeathMessageService() {
        return deathMessageService;
    }

    public enum EnvironmentalDeathCause {
        WORLD_BORDER,
        CAMPING,
        PERK_DEATH_ZONE,
        PERK_RELOCATE,
        PERK_GENERIC
    }
}
