package de.thecoolcraft11.hideAndSeek.perk;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class PerkService {

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
    }

    public void onSeekingStart() {
        if (!plugin.getSettingRegistry().get("perks.enabled", true)) {
            return;
        }

        registry.selectRoundPerks();

        PerkShopMode hiderShopMode = plugin.getSettingRegistry().get("perks.hider-shop-mode", PerkShopMode.INVENTORY);
        PerkShopMode seekerShopMode = plugin.getSettingRegistry().get("perks.seeker-shop-mode", PerkShopMode.INVENTORY);

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.isOnline()) {
                continue;
            }

            boolean isHider = HideAndSeek.getDataController().getHiders().contains(player.getUniqueId());
            boolean isSeeker = HideAndSeek.getDataController().getSeekers().contains(player.getUniqueId());
            if (!isHider && !isSeeker) {
                continue;
            }

            PerkShopMode shopMode = isHider ? hiderShopMode : seekerShopMode;
            if (shopMode == PerkShopMode.INVENTORY) {
                shopUI.givePerkItems(player);
            }
        }

        if (hiderShopMode == PerkShopMode.VENDING_MACHINE || seekerShopMode == PerkShopMode.VENDING_MACHINE) {
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

    public PerkRegistry getRegistry() {
        return registry;
    }

    public PerkStateManager getStateManager() {
        return stateManager;
    }

    public PerkShopUI getShopUI() {
        return shopUI;
    }

    public VendingMachineManager getVendingMachineManager() {
        return vendingMachineManager;
    }
}
