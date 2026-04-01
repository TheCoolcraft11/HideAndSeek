package de.thecoolcraft11.hideAndSeek.perk;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.perk.definition.PerkDefinition;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.util.Vector;

public class PerkService implements Listener {

    private final HideAndSeek plugin;
    private final PerkRegistry registry;
    private final PerkStateManager stateManager;
    private final PerkShopUI shopUI;
    private final VendingMachineManager vendingMachineManager;

    public PerkService(HideAndSeek plugin) {
        this.plugin = plugin;
        this.registry = new PerkRegistry(plugin);
        this.stateManager = new PerkStateManager(plugin);
        this.shopUI = new PerkShopUI(plugin);
        this.vendingMachineManager = new VendingMachineManager(plugin);
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void onSeekingStart() {
        if (!plugin.getSettingRegistry().get("perks.enabled", true)) {
            return;
        }

        registry.selectRoundPerks();

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.isOnline()) {
                continue;
            }
            if (!HideAndSeek.getDataController().getHiders().contains(player.getUniqueId())
                    && !HideAndSeek.getDataController().getSeekers().contains(player.getUniqueId())) {
                continue;
            }
            shopUI.givePerkItems(player);
        }

        String shopMode = String.valueOf(plugin.getSettingRegistry().get("perks.shop-mode", "INVENTORY"));
        if ("VENDING_MACHINE".equalsIgnoreCase(shopMode)) {
            vendingMachineManager.placeVendingMachines();
        }
    }

    public void onRoundEnd() {
        shopUI.clearAll();
        stateManager.clearAll();
        vendingMachineManager.removeVendingMachines();
    }

    public void shutdown() {
        onRoundEnd();
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
            String perkId = shopUI.getPerkIdFromItem(event.getCurrentItem());
            if (perkId == null) {
                return;
            }

            PerkDefinition perk = registry.getAllPerks().stream().filter(p -> p.getId().equals(perkId)).findFirst().orElse(null);
            if (perk != null) {
                stateManager.purchase(player, perk);
                shopUI.openShopInventory(player);
            }
            return;
        }

        if (event.getClickedInventory() != null && event.getClickedInventory().equals(player.getInventory())) {
            String perkId = shopUI.getPerkIdFromItem(event.getCurrentItem());
            if (perkId == null) {
                return;
            }

            event.setCancelled(true);
            PerkDefinition perk = registry.getAllPerks().stream().filter(p -> p.getId().equals(perkId)).findFirst().orElse(null);
            if (perk != null) {
                stateManager.purchase(player, perk);
            }
        }
    }

    @EventHandler
    public void onToggleFlight(PlayerToggleFlightEvent event) {
        Player player = event.getPlayer();
        if (player.getGameMode() != GameMode.SURVIVAL) {
            return;
        }

        if (!stateManager.hasPurchased(player.getUniqueId(), "hider_double_jump")) {
            return;
        }
        if (!HideAndSeek.getDataController().getHiders().contains(player.getUniqueId())) {
            return;
        }
        if (HideAndSeek.getDataController().isHidden(player.getUniqueId())) {
            return;
        }

        event.setCancelled(true);

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
    public void onPlayerQuit(PlayerQuitEvent event) {
        shopUI.removePerkItems(event.getPlayer());
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (event.getCause() == EntityDamageEvent.DamageCause.FALL
                && de.thecoolcraft11.hideAndSeek.perk.impl.seeker.ElytraRushPerk.hasNoFall(player.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onEntityToggleGlide(EntityToggleGlideEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        if (!de.thecoolcraft11.hideAndSeek.perk.impl.seeker.ElytraRushPerk.hasNoFall(player.getUniqueId())) {
            return;
        }

        
        if (!event.isGliding()) {
            event.setCancelled(true);
            Bukkit.getScheduler().runTask(plugin, () -> {
                if (player.isOnline()) {
                    player.setGliding(true);
                }
            });
        }
    }

    public PerkRegistry getRegistry() {
        return registry;
    }

    public PerkStateManager getStateManager() {
        return stateManager;
    }

    public PerkShopUI getShopUI() {
        return shopUI;
    }
}




