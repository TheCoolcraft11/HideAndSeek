package de.thecoolcraft11.hideAndSeek.command;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.items.ItemSkinSelectionService;
import de.thecoolcraft11.hideAndSeek.items.hider.*;
import de.thecoolcraft11.hideAndSeek.items.seeker.*;
import de.thecoolcraft11.minigameframework.commands.MinigameSubcommand;
import de.thecoolcraft11.minigameframework.items.variants.ItemVariant;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public class ItemSkinCommand implements MinigameSubcommand {
    private final HideAndSeek plugin;
    private final Map<String, String> itemAliases = new HashMap<>();

    public ItemSkinCommand(HideAndSeek plugin) {
        this.plugin = plugin;
        registerItems();
    }

    @Override
    public @NotNull String getName() {
        return "skin";
    }

    @Override
    public @NotNull List<String> getAliases() {
        return List.of("skins", "variant", "itemskin");
    }

    @Override
    public boolean handle(@NotNull CommandSender sender, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be used by players!", NamedTextColor.RED));
            return true;
        }

        if (!"lobby".equalsIgnoreCase(plugin.getStateManager().getCurrentPhaseId())) {
            player.sendMessage(Component.text("You can only switch item skins in the lobby.", NamedTextColor.RED));
            return true;
        }

        if (args.length == 0) {
            plugin.getSkinGUI().open(player);
            return true;
        }

        if ("list".equalsIgnoreCase(args[0])) {
            return handleList(player, args);
        }

        if (args.length < 2) {
            sendUsage(player);
            return true;
        }

        String logicalItemId = resolveLogicalItemId(args[0]);
        if (logicalItemId == null) {
            player.sendMessage(Component.text("Unknown item: " + args[0], NamedTextColor.RED));
            player.sendMessage(Component.text("Use /mg skin list <item> to see available variants.", NamedTextColor.GRAY));
            return true;
        }

        String runtimeItemId = ItemSkinSelectionService.resolveRuntimeItemId(player, logicalItemId);
        var variantManager = plugin.getCustomItemManager().getVariantManager();

        if (!variantManager.hasVariants(runtimeItemId)) {
            player.sendMessage(Component.text("This item has no registered skins.", NamedTextColor.RED));
            return true;
        }

        String variantInput = args[1];
        ItemVariant targetVariant = variantManager.getVariant(runtimeItemId, variantInput);
        if (targetVariant == null) {
            targetVariant = variantManager.getVariants(runtimeItemId).stream()
                    .filter(v -> v.getId().equalsIgnoreCase(variantInput))
                    .findFirst()
                    .orElse(null);
        }

        if (targetVariant == null) {
            player.sendMessage(Component.text("Unknown skin variant: " + variantInput, NamedTextColor.RED));
            player.sendMessage(Component.text("Use /mg skin list " + args[0] + " to see available variants.", NamedTextColor.GRAY));
            return true;
        }

        if (!ItemSkinSelectionService.isUnlocked(player.getUniqueId(), logicalItemId, targetVariant.getId())) {
            int cost = ItemSkinSelectionService.getCost(plugin, logicalItemId, targetVariant.getId());
            player.sendMessage(Component.text("This skin is locked (", NamedTextColor.RED)
                    .append(Component.text(cost + " coins", NamedTextColor.GOLD))
                    .append(Component.text("). Buy it in /mg skin.", NamedTextColor.RED)));
            return true;
        }

        ItemSkinSelectionService.setSelectedVariant(player.getUniqueId(), logicalItemId, targetVariant.getId());
        ItemSkinSelectionService.savePlayer(plugin, player.getUniqueId());

        String displayName = targetVariant.getDisplayName();
        String shownName = (displayName == null || displayName.isBlank()) ? targetVariant.getId() : displayName;
        player.sendMessage(Component.text("Selected skin: ", NamedTextColor.GREEN)
                .append(Component.text(shownName, NamedTextColor.GOLD))
                .append(Component.text(" for ", NamedTextColor.GREEN))
                .append(Component.text(logicalItemId, NamedTextColor.YELLOW)));
        player.sendMessage(Component.text("This skin will be applied the next time the item is given to you.", NamedTextColor.GRAY));

        return true;
    }

    @Override
    public @Nullable List<String> tabComplete(@NotNull CommandSender sender, @NotNull String[] args) {
        if (args.length == 1) {
            String prefix = args[0].toLowerCase(Locale.ROOT);
            Set<String> firstArgs = new TreeSet<>();
            firstArgs.add("list");
            firstArgs.addAll(getPrimaryItemKeys());
            return firstArgs.stream().filter(v -> v.startsWith(prefix)).toList();
        }

        if (args.length == 2 && "list".equalsIgnoreCase(args[0])) {
            String prefix = args[1].toLowerCase(Locale.ROOT);
            return getPrimaryItemKeys().stream().filter(v -> v.startsWith(prefix)).toList();
        }

        if (args.length == 2 && sender instanceof Player player) {
            String logicalItemId = resolveLogicalItemId(args[0]);
            if (logicalItemId == null) {
                return List.of();
            }
            String runtimeItemId = ItemSkinSelectionService.resolveRuntimeItemId(player, logicalItemId);
            String prefix = args[1].toLowerCase(Locale.ROOT);
            return plugin.getCustomItemManager().getVariantManager().getVariants(runtimeItemId).stream()
                    .map(ItemVariant::getId)
                    .filter(id -> id.toLowerCase(Locale.ROOT).startsWith(prefix))
                    .collect(Collectors.toList());
        }

        return List.of();
    }

    private boolean handleList(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(Component.text("Usage: /mg skin list <item>", NamedTextColor.YELLOW));
            return true;
        }

        String logicalItemId = resolveLogicalItemId(args[1]);
        if (logicalItemId == null) {
            player.sendMessage(Component.text("Unknown item: " + args[1], NamedTextColor.RED));
            return true;
        }

        String runtimeItemId = ItemSkinSelectionService.resolveRuntimeItemId(player, logicalItemId);
        List<ItemVariant> variants = plugin.getCustomItemManager().getVariantManager().getVariants(runtimeItemId);

        if (variants.isEmpty()) {
            player.sendMessage(Component.text("No skins registered for this item.", NamedTextColor.RED));
            return true;
        }

        player.sendMessage(Component.text("Available skins for " + logicalItemId + ":", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("Coins: " + ItemSkinSelectionService.getCoins(player.getUniqueId()), NamedTextColor.GOLD));
        for (ItemVariant variant : variants) {
            String display = variant.getDisplayName().isEmpty() ? variant.getId() : variant.getDisplayName();
            boolean unlocked = ItemSkinSelectionService.isUnlocked(player.getUniqueId(), logicalItemId, variant.getId());
            int cost = ItemSkinSelectionService.getCost(plugin, logicalItemId, variant.getId());
            String rarity = ItemSkinSelectionService.getRarity(logicalItemId, variant.getId()).name();
            player.sendMessage(Component.text(" - " + variant.getId() + " (" + display + ") [" + rarity + "] [" + cost + "c] " + (unlocked ? "UNLOCKED" : "LOCKED"), NamedTextColor.GRAY));
        }
        return true;
    }

    private void sendUsage(Player player) {
        player.sendMessage(Component.text("Usage: /mg skin <item> <variant_id>", NamedTextColor.YELLOW));
        player.sendMessage(Component.text("Usage: /mg skin list <item>", NamedTextColor.YELLOW));
    }

    private void registerItems() {
        addAliases(ExplosionItem.ID, "explosionitem", "explosion");
        addAliases(RandomBlockItem.ID, "randomblockitem", "randomblock");
        addAliases(SpeedBoostItem.ID, "speedboostitem", "speedboost");
        addAliases(TrackerCrossbowItem.ID, "trackercrossbowitem", "trackercrossbow", "crossbowtracker");
        addAliases(KnockbackStickItem.ID, "knockbackstickitem", "knockbackstick");
        addAliases(BlockSwapItem.ID, "blockswapitem", "blockswap");
        addAliases(BigFirecrackerItem.ID, "bigfirecrackeritem", "bigfirecracker");
        addAliases(FireworkRocketItem.ID, "fireworkrocketitem", "fireworkrocket");
        addAliases(SlownessBallItem.ID, "slownessballitem", "slownessball");
        addAliases(SmokeBombItem.ID, "smokebombitem", "smokebomb");
        addAliases(GhostEssenceItem.ID, "ghostessenceitem", "ghostessence");
        addAliases(InvisibilityCloakItem.ID, "invisibilitycloakitem", "invisibilitycloak");
        addAliases(MedkitItem.ID, "medkititem", "medkit");
        addAliases(TotemItem.ID, "totemitem", "totem");
        addAliases(SoundItem.ID, "sounditem", "sound");

        addAliases(GrapplingHookItem.ID, "grapplinghookitem", "grapplinghook");
        addAliases(GlowingCompassItem.ID, "glowingcompassitem", "glowingcompass");
        addAliases(BlockRandomizerItem.ID, "blockrandomizeritem", "blockrandomizer");
        addAliases(ChainPullItem.ID, "chainpullitem", "chainpull");
        addAliases(CageTrapItem.ID, "cagetrapitem", "cagetrap");
        addAliases(ProximitySensorItem.ID, "proximitysensoritem", "proximitysensor");
        addAliases(CameraItem.ID, "cameraitem", "camera");
        addAliases(CurseSpellItem.ID, "cursespellitem", "cursespell");
        addAliases(InkSplashItem.ID, "inksplashitem", "inksplash");
        addAliases(LightningFreezeItem.ID, "lightningfreezeitem", "lightningfreeze");
        addAliases(SeekersSwordItem.ID, "seekerssworditem", "seekerssword");
    }

    private void addAliases(String logicalItemId, String... aliases) {
        itemAliases.put(logicalItemId.toLowerCase(Locale.ROOT), logicalItemId);
        itemAliases.put(ItemSkinSelectionService.normalizeLogicalItemId(logicalItemId).toLowerCase(Locale.ROOT),
                ItemSkinSelectionService.normalizeLogicalItemId(logicalItemId));

        for (String alias : aliases) {
            itemAliases.put(alias.toLowerCase(Locale.ROOT), logicalItemId);
        }
    }

    private String resolveLogicalItemId(String input) {
        String direct = itemAliases.get(input.toLowerCase(Locale.ROOT));
        if (direct != null) {
            return ItemSkinSelectionService.normalizeLogicalItemId(direct);
        }

        String normalized = ItemSkinSelectionService.normalizeLogicalItemId(input);
        String mapped = itemAliases.get(normalized.toLowerCase(Locale.ROOT));
        if (mapped != null) {
            return ItemSkinSelectionService.normalizeLogicalItemId(mapped);
        }

        return null;
    }

    private List<String> getPrimaryItemKeys() {
        return itemAliases.entrySet().stream()
                .filter(e -> e.getKey().equals(e.getValue().toLowerCase(Locale.ROOT)))
                .map(Map.Entry::getKey)
                .sorted()
                .toList();
    }
}
