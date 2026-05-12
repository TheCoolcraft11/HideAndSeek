package de.thecoolcraft11.hideAndSeek.command;
import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.util.DataController;
import de.thecoolcraft11.hideAndSeek.util.UnstuckManager;
import de.thecoolcraft11.minigameframework.commands.MinigameSubcommand;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class UnstuckCommand implements MinigameSubcommand {
    private static final String PERMISSION = "hideandseek.command.unstuck";

    private final HideAndSeek plugin;
    private final UnstuckManager unstuckManager;
    private final DataController dataController;
    private final Map<UUID, BukkitTask> pendingTeleports = new HashMap<>();

    public UnstuckCommand(HideAndSeek plugin, UnstuckManager unstuckManager) {
        this.plugin = plugin;
        this.unstuckManager = unstuckManager;
        this.dataController = HideAndSeek.getDataController();
    }

    @Override
    public @NotNull String getName() {
        return "unstuck";
    }

    @Override
    public @NotNull List<String> getAliases() {
        return List.of("stuckfix");
    }

    @Override
    public @Nullable String getPermission() {
        return PERMISSION;
    }

    @Override
    public void handle(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.tr(sender, "common.command.only_players"));
            return;
        }

        boolean isHider = dataController.getHiders().contains(player.getUniqueId());
        boolean isSeeker = dataController.getSeekers().contains(player.getUniqueId());

        if (!isHider && !isSeeker) {
            player.sendMessage(plugin.tr(player, "command.unstuck.not_participant"));
            return;
        }

        if (!"seeking".equals(plugin.getStateManager().getCurrentPhaseId())) {
            player.sendMessage(plugin.tr(player, "command.unstuck.wrong_phase"));
            return;
        }

        if (pendingTeleports.containsKey(player.getUniqueId())) {
            player.sendMessage(plugin.tr(player, "command.unstuck.already_charging"));
            return;
        }

        long cooldownMs = unstuckManager.getRemainingCooldownMs(player.getUniqueId());
        if (cooldownMs > 0L) {
            long secondsLeft = (long) Math.ceil(cooldownMs / 1000.0);

            player.sendMessage(plugin.tr(player, "command.unstuck.cooldown",
                    Map.of("seconds", String.valueOf(secondsLeft))));
            return;
        }

        boolean worldSpawnMode = args.length > 0 && isWorldSpawnArgument(args[0]);

        if (args.length > 0 && !worldSpawnMode) {
            player.sendMessage(plugin.tr(player, "command.unstuck.usage"));
            return;
        }

        if (!isSeeker) {
            double seekerRange = plugin.getSettingRegistry().get("game.unstuck.seeker-range", 15.0);

            if (unstuckManager.hasNearbyOpponents(player, seekerRange)) {
                player.sendMessage(plugin.tr(player, "command.unstuck.seeker_too_close"));
                return;
            }
        }

        if (worldSpawnMode) {
            startWorldSpawnUnstuck(player);
            return;
        }

        UnstuckManager.UnstuckResult result =
                unstuckManager.tryFindSafePosition(player, dataController.getRoundSpawnPoint());

        if (!result.success() || result.location() == null || result.method() == null) {
            String messageKey = result.messageKey() != null ? result.messageKey() : "command.unstuck.default_fail_message";
            player.sendMessage(plugin.tr(player, messageKey, result.placeholders()));
            return;
        }

        String completionMessageKey = result.messageKey();
        startDelayedTeleport(player,
                result.location(),
                result.cooldownSeconds(),
                result.method(),
                false,
                completionMessageKey,
                result.placeholders());
    }

    private void startWorldSpawnUnstuck(Player player) {
        if (pendingTeleports.containsKey(player.getUniqueId())) {
            return;
        }

        var spawnTarget = unstuckManager.resolveWorldSpawnTarget(player);
        if (spawnTarget == null) {
            player.sendMessage(plugin.tr(player, "command.unstuck.spawn_not_safe"));
            return;
        }

        startDelayedTeleport(
                player,
                spawnTarget,
                unstuckManager.getSpawnFallbackCooldownSeconds(),
                UnstuckManager.UnstuckMethod.SPAWN,
                true,
                "command.unstuck.spawn_success",
                Map.of()
        );
    }

    private void startDelayedTeleport(Player player,
                                      org.bukkit.Location target,
                                      int cooldownSeconds,
                                      UnstuckManager.UnstuckMethod method,
                                      boolean worldSpawnMode,
                                      @Nullable String resultMessageKey,
                                      Map<String, Object> resultPlaceholders) {
        final long chargeMs = 5000L;
        final double maxMoveDistance = 1.5;
        final long cancelSneakMs = 3000L;
        final var startLocation = player.getLocation().clone();

        player.sendMessage(plugin.tr(player, "command.unstuck.charge_start"));
        player.sendMessage(plugin.tr(player, "command.unstuck.cancel_hint"));

        final long[] sneakStartedAt = new long[]{-1L};
        final long startedAt = System.currentTimeMillis();

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline()) {
                cancelPending(player.getUniqueId());
                return;
            }

            if (!"seeking".equals(plugin.getStateManager().getCurrentPhaseId())) {
                cancelPending(player.getUniqueId());
                player.sendMessage(plugin.tr(player, "command.unstuck.canceled.phase"));
                return;
            }

            if (!player.getWorld().equals(startLocation.getWorld())
                    || player.getLocation().distance(startLocation) > maxMoveDistance) {
                cancelPending(player.getUniqueId());
                player.sendMessage(plugin.tr(player, "command.unstuck.canceled.moved"));
                return;
            }

            if (player.isSneaking()) {
                if (sneakStartedAt[0] < 0L) {
                    sneakStartedAt[0] = System.currentTimeMillis();
                } else if (System.currentTimeMillis() - sneakStartedAt[0] >= cancelSneakMs) {
                    cancelPending(player.getUniqueId());
                    player.sendMessage(plugin.tr(player, "command.unstuck.canceled.sneak"));
                    return;
                }
            } else {
                sneakStartedAt[0] = -1L;
            }

            target.getWorld().spawnParticle(Particle.PORTAL, target, 18, 0.4, 0.5, 0.4, 0.05);

            if (System.currentTimeMillis() - startedAt < chargeMs) {
                return;
            }

            cancelPending(player.getUniqueId());

            boolean teleported = player.teleport(target);
            if (!teleported) {
                player.sendMessage(plugin.tr(player, "command.unstuck.failed"));
                return;
            }

            unstuckManager.recordSuccessfulUnstuck(player.getUniqueId(), target, method);
            unstuckManager.applyCooldown(player.getUniqueId(), cooldownSeconds);

            // Send the result message or use default message based on the method
            if (resultMessageKey != null && !resultMessageKey.isBlank()) {
                player.sendMessage(plugin.tr(player, resultMessageKey, resultPlaceholders));
            } else if (worldSpawnMode) {
                player.sendMessage(plugin.tr(player, "command.unstuck.success.fallback"));
            } else if (method == UnstuckManager.UnstuckMethod.SPAWN) {
                player.sendMessage(plugin.tr(player, "command.unstuck.success.spawn"));
            } else {
                player.sendMessage(plugin.tr(player, "command.unstuck.success.safe"));
            }

            player.getWorld().spawnParticle(Particle.PORTAL, target, 24, 0.3, 0.5, 0.3, 0.1);
            player.playSound(target, Sound.ENTITY_ENDERMAN_TELEPORT, 0.8f, 1.2f);
        }, 0L, 5L);

        pendingTeleports.put(player.getUniqueId(), task);
    }

    private void cancelPending(UUID playerId) {
        BukkitTask task = pendingTeleports.remove(playerId);
        if (task != null) {
            task.cancel();
        }
    }

    private boolean isWorldSpawnArgument(String argument) {
        return "spawn".equalsIgnoreCase(argument)
                || "worldspawn".equalsIgnoreCase(argument)
                || "ws".equalsIgnoreCase(argument);
    }
}




