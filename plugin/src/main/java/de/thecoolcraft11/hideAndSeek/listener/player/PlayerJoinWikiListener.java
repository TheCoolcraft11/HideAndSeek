package de.thecoolcraft11.hideAndSeek.listener.player;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.minigameframework.commands.MinigameSubcommandRegistry;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;

import java.util.UUID;

public class PlayerJoinWikiListener implements Listener {

    private final HideAndSeek plugin;

    public PlayerJoinWikiListener(HideAndSeek plugin) {
        this.plugin = plugin;
    }

    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.MONITOR)
    public void onLogin(PlayerLoginEvent event) {
        Bukkit.getScheduler().runTask(plugin,
                () -> plugin.getNmsAdapter().injectDialogFilter(event.getPlayer().getUniqueId(), plugin,
                        (command, p) -> {
                            UUID uuid = p.getUniqueId();

                            LuckPerms luckPerms = Bukkit.getServicesManager()
                                    .load(LuckPerms.class);
                            if (luckPerms == null) return false;
                            User user = luckPerms.getUserManager().getUser(uuid);
                            if (user == null) return false;
                            String cmd = command.startsWith("/") ? command.substring(1) : command;


                            String[] parts = cmd.split(" ", 2);
                            if (parts.length > 1) {
                                var subcommand = MinigameSubcommandRegistry.get(parts[1].split(" ")[0]);
                                if (subcommand != null && subcommand.getPermission() != null) {
                                    return user.getCachedData().getPermissionData().checkPermission(
                                            subcommand.getPermission()).asBoolean();
                                }
                            }

                            return user.getCachedData().getPermissionData().checkPermission(
                                    getFrameworkPerm(parts[1].split(" ")[0])).asBoolean();
                        }));
    }

    private String getFrameworkPerm(String subcommand) {
        return "minigameframework." + subcommand;
    }
}
