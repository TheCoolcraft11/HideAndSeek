package de.thecoolcraft11.hideAndSeek.listener;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.items.HiderItems;
import de.thecoolcraft11.hideAndSeek.items.SeekerItems;
import de.thecoolcraft11.hideAndSeek.util.GameStyleEnum;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

import java.time.Duration;
import java.util.Objects;

public class PlayerHitListener implements Listener {
    private final HideAndSeek plugin;

    public PlayerHitListener(HideAndSeek plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player victim)) {
            return;
        }


        if (plugin.getStateManager().getCurrentPhaseId().equals("seeking") || plugin.getStateManager().getCurrentPhaseId().equals("hiding")) {
            if (HideAndSeek.getDataController().isHidden(victim.getUniqueId()) &&
                    HideAndSeek.getDataController().isBlockDamageOverrideActive(victim.getUniqueId())) {
                return;
            }

            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
        if (!plugin.getStateManager().getCurrentPhaseId().equals("seeking")) {
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

        Player attacker = null;


        if (event.getDamager() instanceof Player p) {
            attacker = p;
        }


        if (event.getDamager() instanceof org.bukkit.entity.Projectile projectile) {
            if (projectile.getShooter() instanceof Player p) {
                attacker = p;
            }
        }

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
                org.bukkit.Location hiddenLocation = HideAndSeek.getDataController().getLastLocation(victim.getUniqueId());
                if (hiddenLocation != null) {
                    org.bukkit.block.Block hiddenBlock = hiddenLocation.getBlock();
                    plugin.getBlockModeListener().damageHiddenPlayer(attacker, hiddenBlock, plugin.getSettingRegistry().get("game.seeker-kill-mode").equals("GAZE_KILL"));
                }
                event.setCancelled(true);
                return;
            }
            event.setCancelled(false);
            return;
        }

        if (attackerIsSeeker && victimIsHider) {
            if (SeekerItems.isCurseActive(attacker.getUniqueId())) {
                SeekerItems.applyCurseToHider(victim, plugin);
            }

            if (HiderItems.isTotemActive(victim.getUniqueId()) && event.getFinalDamage() >= victim.getHealth()) {
                event.setCancelled(true);
                HiderItems.reviveWithTotem(victim, plugin);
                return;
            }

            event.setCancelled(false);
            return;
        }

        if (attackerIsHider && victimIsSeeker) {
            event.setCancelled(false);
            return;
        }


        event.setCancelled(true);
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

        if (event.getDamageSource().getCausingEntity() instanceof Player killer) {


            if (HideAndSeek.getDataController().getSeekers().contains(killer.getUniqueId()) &&
                    HideAndSeek.getDataController().getHiders().contains(deceased.getUniqueId())) {


                var gameStyleResult = plugin.getSettingService().getSetting("game.gamestyle");
                Object gameStyleObj = gameStyleResult.isSuccess() ? gameStyleResult.getValue() : GameStyleEnum.SPECTATOR;
                GameStyleEnum gameStyle = (gameStyleObj instanceof GameStyleEnum) ?
                        (GameStyleEnum) gameStyleObj : GameStyleEnum.SPECTATOR;

                handleHiderElimination(deceased, killer, gameStyle);
            }
        }
    }

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();


        if (plugin.getStateManager().getCurrentPhaseId().equals("seeking")) {
            var gameStyleResult = plugin.getSettingService().getSetting("game.gamestyle");
            Object gameStyleObj = gameStyleResult.isSuccess() ? gameStyleResult.getValue() : GameStyleEnum.SPECTATOR;
            GameStyleEnum gameStyle = (gameStyleObj instanceof GameStyleEnum) ?
                    (GameStyleEnum) gameStyleObj : GameStyleEnum.SPECTATOR;


            if (gameStyle == GameStyleEnum.INFINITE &&
                    HideAndSeek.getDataController().getHiders().contains(player.getUniqueId())) {

                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    player.setGameMode(GameMode.SURVIVAL);
                    player.setHealth(Objects.requireNonNull(player.getAttribute(Attribute.MAX_HEALTH)).getBaseValue());
                    player.setFoodLevel(20);
                    player.sendMessage(Component.text("You respawned! Keep hiding!", NamedTextColor.GREEN));
                }, 1L);
            }
        }
    }

    private void handleHiderElimination(Player hider, Player seeker, GameStyleEnum gameStyle) {


        var seekerPoints = plugin.getSettingRegistry().get("had.points.seeker-find", 10);
        HideAndSeek.getDataController().addPoints(seeker.getUniqueId(), seekerPoints);
        seeker.sendMessage(Component.text("+" + seekerPoints + " points for finding " + hider.getName() + "!", NamedTextColor.GOLD));

        Component announcement = Component.text(seeker.getName(), NamedTextColor.RED)
                .append(Component.text(" found ", NamedTextColor.YELLOW))
                .append(Component.text(hider.getName(), NamedTextColor.GREEN))
                .append(Component.text("!", NamedTextColor.YELLOW));

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(announcement);
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
        HideAndSeek.getDataController().addSeeker(hider.getUniqueId());


        var seekerTeams = plugin.getTeamManager().getAllTeams().stream()
                .filter(t -> !plugin.getTeamManager().isSpectatorTeam(t.getName()))
                .findFirst();

        if (seekerTeams.isPresent()) {
            plugin.getTeamManager().addPlayerToTeam(hider, seekerTeams.get().getName());
            plugin.getLogger().info("Moved " + hider.getName() + " to seeker team: " + seekerTeams.get().getName());
        } else {
            plugin.getLogger().warning("Could not find seeker team for eliminated hider!");
        }

        plugin.getTeamManager().removeRole(hider, "hider");
        plugin.getTeamManager().addRole(hider, "seeker");

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            hider.setGameMode(GameMode.SPECTATOR);
            hider.setHealth(20.0);


            cleanupBlockModeHider(hider);

            Title title = Title.title(
                    Component.text("YOU WERE FOUND!", NamedTextColor.RED),
                    Component.text("You are now spectating", NamedTextColor.GRAY),
                    Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(3), Duration.ofMillis(500))
            );
            hider.showTitle(title);
        }, 1L);

        plugin.getLogger().info(hider.getName() + " was eliminated and is now spectating (moved to seeker team)");
    }

    private void handleInvasionMode(Player hider) {
        HideAndSeek.getDataController().removeHider(hider.getUniqueId());
        HideAndSeek.getDataController().addSeeker(hider.getUniqueId());


        var seekerTeams = plugin.getTeamManager().getAllTeams().stream()
                .filter(t -> !plugin.getTeamManager().isSpectatorTeam(t.getName()))
                .findFirst();

        if (seekerTeams.isPresent()) {
            plugin.getTeamManager().addPlayerToTeam(hider, seekerTeams.get().getName());
            plugin.getLogger().info("Added " + hider.getName() + " to seeker team: " + seekerTeams.get().getName());
        } else {
            plugin.getLogger().warning("Could not find seeker team for INVASION mode conversion!");
        }

        plugin.getTeamManager().removeRole(hider, "hider");
        plugin.getTeamManager().addRole(hider, "seeker");

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            hider.setGameMode(GameMode.SURVIVAL);
            hider.setHealth(20.0);
            hider.setFoodLevel(20);
            hider.getInventory().clear();


            cleanupBlockModeHider(hider);


            SeekerItems.giveItems(hider, plugin);
            SeekerItems.giveGrapplingHook(hider, plugin);
            SeekerItems.applyMask(hider, plugin);


            var gameModeResult = plugin.getSettingService().getSetting("game.gametype");
            Object gameModeObj = gameModeResult.isSuccess() ? gameModeResult.getValue() : null;
            boolean isBlockMode = gameModeObj != null && gameModeObj.toString().equals("BLOCK");

            if (isBlockMode) {
                SeekerItems.giveBlockStats(hider, plugin);
            }


            hider.addPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.SATURATION,
                    Integer.MAX_VALUE,
                    0,
                    false,
                    false,
                    false
            ));

            try {
                Objects.requireNonNull(hider.getAttribute(Attribute.SCALE)).setBaseValue(1.0);
                plugin.getLogger().info("Reset size for converted seeker: " + hider.getName());
            } catch (Exception e) {
                plugin.getLogger().warning("Failed to reset size for " + hider.getName());
            }

            Title title = Title.title(
                    Component.text("YOU'RE NOW A SEEKER!", NamedTextColor.RED),
                    Component.text("Help find the remaining hiders!", NamedTextColor.YELLOW),
                    Title.Times.times(Duration.ofMillis(500), Duration.ofSeconds(3), Duration.ofMillis(500))
            );
            hider.showTitle(title);
        }, 1L);

        plugin.getLogger().info(hider.getName() + " was eliminated and joined the seekers (INVASION mode)");
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

}
