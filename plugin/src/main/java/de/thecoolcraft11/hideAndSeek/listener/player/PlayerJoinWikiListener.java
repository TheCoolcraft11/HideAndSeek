package de.thecoolcraft11.hideAndSeek.listener.player;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.minigameframework.commands.MinigameSubcommandRegistry;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;

public class PlayerJoinWikiListener implements Listener {

    private final HideAndSeek plugin;

    public PlayerJoinWikiListener(HideAndSeek plugin) {
        this.plugin = plugin;
    }

    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.MONITOR)
    public void onLogin(PlayerLoginEvent event) {
        final org.bukkit.entity.Player player = event.getPlayer();

        Bukkit.getScheduler().runTask(plugin,
                () -> plugin.getNmsAdapter().injectDialogFilter(player.getUniqueId(), plugin,
                        (command, p) -> {
                            String cmd = command.startsWith("/") ? command.substring(1) : command;
                            String[] parts = cmd.split(" ", 2);

                            if (parts.length > 1) {
                                String subName = parts[1].split(" ")[0];
                                var subcommand = MinigameSubcommandRegistry.get(subName);

                                if (subcommand != null && subcommand.getPermission() != null) {
                                    return player.hasPermission(subcommand.getPermission());
                                }
                            }

                            String frameworkPerm = getFrameworkPerm(parts[1].split(" ")[0]);
                            return player.hasPermission(frameworkPerm);
                        }));
    }

    private String getFrameworkPerm(String subcommand) {
        return "minigameframework." + subcommand;
    }
}
