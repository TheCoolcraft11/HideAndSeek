package de.thecoolcraft11.hideAndSeek.command;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.util.DataController;
import de.thecoolcraft11.hideAndSeek.util.UnstuckManager;
import de.thecoolcraft11.minigameframework.commands.MinigameSubcommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
            sender.sendMessage(Component.text("This command can only be used by players!", NamedTextColor.RED));
            return;
        }

        boolean isHider = dataController.getHiders().contains(player.getUniqueId());
        boolean isSeeker = dataController.getSeekers().contains(player.getUniqueId());
        if (!isHider && !isSeeker) {
            player.sendMessage(Component.text("Only hiders and seekers can use /mg unstuck.", NamedTextColor.RED));
            return;
        }

        if (!"seeking".equals(plugin.getStateManager().getCurrentPhaseId())) {
            player.sendMessage(Component.text("You can only use this during the seeking phase.", NamedTextColor.RED));
            return;
        }

        if (pendingTeleports.containsKey(player.getUniqueId())) {
            player.sendMessage(Component.text("Unstuck is already charging. Wait, move, or sneak to cancel.", NamedTextColor.YELLOW));
            return;
        }

        long cooldownMs = unstuckManager.getRemainingCooldownMs(player.getUniqueId());
        if (cooldownMs > 0L) {
            long secondsLeft = (long) Math.ceil(cooldownMs / 1000.0);
            player.sendMessage(Component.text("Unstuck is on cooldown for " + secondsLeft + "s.", NamedTextColor.RED));
            return;
        }

        boolean worldSpawnMode = args.length > 0 && isWorldSpawnArgument(args[0]);
        if (args.length > 0 && !worldSpawnMode) {
            player.sendMessage(Component.text("Usage: /mg unstuck [spawn]", NamedTextColor.RED));
            return;
        }

        if (!isSeeker) {
            double seekerRange = plugin.getSettingRegistry().get("game.unstuck.seeker-range", 15.0);
            if (unstuckManager.hasNearbyOpponents(player, seekerRange)) {
                player.sendMessage(Component.text("A seeker is too close. Try again when it is safe.", NamedTextColor.RED));
                return;
            }
        }

        if (worldSpawnMode) {
            startWorldSpawnUnstuck(player);
            return;
        }

        UnstuckManager.UnstuckResult result = unstuckManager.tryFindSafePosition(player, dataController.getRoundSpawnPoint());
        if (!result.success() || result.location() == null || result.method() == null) {
            String message = result.message() == null ? "No safe unstuck position found." : result.message();
            player.sendMessage(Component.text(message, NamedTextColor.RED));
            return;
        }

        startDelayedTeleport(player, result.location(), result.cooldownSeconds(), result.method(), false, result.message());
    }

    private void startWorldSpawnUnstuck(Player player) {
        if (pendingTeleports.containsKey(player.getUniqueId())) {
            return;
        }

        var spawnTarget = unstuckManager.resolveWorldSpawnTarget(player);
        if (spawnTarget == null) {
            player.sendMessage(Component.text("World spawn is not safe right now.", NamedTextColor.RED));
            return;
        }

        startDelayedTeleport(
                player,
                spawnTarget,
                unstuckManager.getSpawnFallbackCooldownSeconds(),
                UnstuckManager.UnstuckMethod.SPAWN,
                true,
                "Teleported to world spawn."
        );
    }

    private void startDelayedTeleport(Player player,
                                      org.bukkit.Location target,
                                      int cooldownSeconds,
                                      UnstuckManager.UnstuckMethod method,
                                      boolean worldSpawnMode,
                                      @Nullable String completionMessage) {
        final long chargeMs = 5000L;
        final double maxMoveDistance = 1.5;
        final long cancelSneakMs = 3000L;
        final var startLocation = player.getLocation().clone();

        player.sendMessage(Component.text("Unstuck charging for 5s. Do not move more than 1.5 blocks.", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("Sneak for 3s to cancel.", NamedTextColor.GRAY));

        final long[] sneakStartedAt = new long[]{-1L};
        final long startedAt = System.currentTimeMillis();

        BukkitTask task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            if (!player.isOnline()) {
                cancelPending(player.getUniqueId());
                return;
            }

            if (!"seeking".equals(plugin.getStateManager().getCurrentPhaseId())) {
                cancelPending(player.getUniqueId());
                player.sendMessage(Component.text("Unstuck canceled because the game phase changed.", NamedTextColor.RED));
                return;
            }

            if (!player.getWorld().equals(startLocation.getWorld())
                    || player.getLocation().distance(startLocation) > maxMoveDistance) {
                cancelPending(player.getUniqueId());
                player.sendMessage(Component.text("Unstuck canceled because you moved too far.", NamedTextColor.RED));
                return;
            }

            if (player.isSneaking()) {
                if (sneakStartedAt[0] < 0L) {
                    sneakStartedAt[0] = System.currentTimeMillis();
                } else if (System.currentTimeMillis() - sneakStartedAt[0] >= cancelSneakMs) {
                    cancelPending(player.getUniqueId());
                    player.sendMessage(Component.text("Unstuck canceled by sneaking.", NamedTextColor.RED));
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
                player.sendMessage(Component.text("Teleport failed. Please try again.", NamedTextColor.RED));
                return;
            }

            unstuckManager.recordSuccessfulUnstuck(player.getUniqueId(), target, method);
            unstuckManager.applyCooldown(player.getUniqueId(), cooldownSeconds);

            if (completionMessage != null && !completionMessage.isBlank()) {
                NamedTextColor color = method == UnstuckManager.UnstuckMethod.SPAWN ? NamedTextColor.YELLOW : NamedTextColor.GREEN;
                player.sendMessage(Component.text(completionMessage, color));
            } else if (worldSpawnMode) {
                player.sendMessage(Component.text("Teleported to world spawn.", NamedTextColor.YELLOW));
            } else if (method == UnstuckManager.UnstuckMethod.SPAWN) {
                player.sendMessage(Component.text("Unstuck fallback used: sent near spawn.", NamedTextColor.YELLOW));
            } else {
                player.sendMessage(Component.text("Teleported to a safe unstuck location.", NamedTextColor.GREEN));
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




