package de.thecoolcraft11.hideAndSeek.listener.perk;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.perk.PerkService;
import de.thecoolcraft11.hideAndSeek.perk.PerkShopMode;
import de.thecoolcraft11.hideAndSeek.perk.PerkShopUI;
import de.thecoolcraft11.hideAndSeek.perk.definition.DelayedActivationPerk;
import de.thecoolcraft11.hideAndSeek.perk.impl.seeker.ElytraRushPerk;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.util.Vector;

public class PerkListener implements Listener {

    private final HideAndSeek plugin;
    private final PerkService perkService;

    public PerkListener(HideAndSeek plugin, PerkService perkService) {
        this.plugin = plugin;
        this.perkService = perkService;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (event.getCurrentItem() == null) {
            return;
        }

        if (PerkShopUI.SHOP_TITLE.equals(event.getView().title())) {
            event.setCancelled(true);
            String perkId = perkService.getShopUI().getPerkIdFromItem(event.getCurrentItem());
            if (perkId == null) {
                return;
            }

            perkService.getRegistry().getAllPerks().stream()
                    .filter(p -> p.getId().equals(perkId))
                    .findFirst()
                    .ifPresent(perk -> {
                        perkService.getStateManager().purchase(player, perk);
                        if (!(perk instanceof DelayedActivationPerk)) {
                            perkService.getShopUI().openShopInventory(player);
                        }
                    });
            return;
        }

        if (event.getClickedInventory() != null && event.getClickedInventory().equals(player.getInventory())) {
            String perkId = perkService.getShopUI().getPerkIdFromItem(event.getCurrentItem());
            if (perkId == null) {
                return;
            }

            event.setCancelled(true);
            perkService.getRegistry().getAllPerks().stream()
                    .filter(p -> p.getId().equals(perkId))
                    .findFirst()
                    .ifPresent(perk -> perkService.getStateManager().purchase(player, perk));
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        perkService.getShopUI().removePerkItems(event.getPlayer());
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) {
            return;
        }
        if (event.getClickedBlock().getType() != Material.DROPPER) {
            return;
        }
        if (!perkService.getVendingMachineManager().isVendingMachine(event.getClickedBlock().getLocation())) {
            return;
        }

        event.setCancelled(true);

        if (!"seeking".equals(plugin.getStateManager().getCurrentPhaseId())) {
            event.getPlayer().sendMessage(Component.text("The perk shop is only open during the seeking phase.", NamedTextColor.RED));
            return;
        }

        boolean isHider = HideAndSeek.getDataController().getHiders().contains(event.getPlayer().getUniqueId());
        boolean isSeeker = HideAndSeek.getDataController().getSeekers().contains(event.getPlayer().getUniqueId());
        PerkShopMode shopMode = isHider
                ? plugin.getSettingRegistry().get("perks.hider-shop-mode", PerkShopMode.INVENTORY)
                : plugin.getSettingRegistry().get("perks.seeker-shop-mode", PerkShopMode.INVENTORY);
        if ((!isHider && !isSeeker) || shopMode != PerkShopMode.VENDING_MACHINE) {
            event.getPlayer().sendMessage(Component.text("Your perk shop is configured to use the inventory instead.", NamedTextColor.GRAY));
            return;
        }

        perkService.getShopUI().openShopInventory(event.getPlayer());
    }

    @EventHandler
    public void onToggleFlight(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() != GameMode.SURVIVAL) {
            return;
        }

        if (!perkService.getStateManager().hasPurchased(player.getUniqueId(), "hider_double_jump")) {
            return;
        }
        event.setCancelled(true);

        if (!HideAndSeek.getDataController().getHiders().contains(player.getUniqueId())
                || HideAndSeek.getDataController().isHidden(player.getUniqueId())) {
            player.setFlying(false);
            player.setAllowFlight(false);
            return;
        }

        double jumpPower = plugin.getSettingRegistry().get("perks.perk.hider_double_jump.jump-power", 0.7d);
        double horizontalBoost = plugin.getSettingRegistry().get("perks.perk.hider_double_jump.horizontal-boost", 0.1d);

        Vector vel = player.getVelocity();
        vel.setY(jumpPower);
        vel.setX(vel.getX() + player.getLocation().getDirection().getX() * horizontalBoost);
        vel.setZ(vel.getZ() + player.getLocation().getDirection().getZ() * horizontalBoost);
        player.setVelocity(vel);
        player.setAllowFlight(false);

        player.playSound(player.getLocation(), Sound.ENTITY_BAT_TAKEOFF, 0.7f, 1.2f);
        player.spawnParticle(Particle.CLOUD, player.getLocation(), 6, 0.2, 0.0, 0.2, 0.05);
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL
                && ElytraRushPerk.hasNoFall(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityToggleGlide(EntityToggleGlideEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (!ElytraRushPerk.hasNoFall(player.getUniqueId())) {
            return;
        }

        event.setCancelled(true);
        if (!event.isGliding()) {
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (player.isOnline()) {
                    player.setGliding(true);
                }
            });
        }
    }
}


