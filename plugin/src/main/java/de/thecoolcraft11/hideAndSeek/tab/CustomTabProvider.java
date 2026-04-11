package de.thecoolcraft11.hideAndSeek.tab;

import de.thecoolcraft11.hideAndSeek.HideAndSeek;
import de.thecoolcraft11.hideAndSeek.items.ItemSkinSelectionService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class CustomTabProvider {
    private final String serverIp;
    private final List<TextColor> titleColors = new ArrayList<>();
    private final List<TextColor> ipColors = new ArrayList<>();
    private final double speedTitle;
    private final double speedIp;
    private final boolean enableHeader;
    private final boolean enableFooter;
    private int animationTick = 0;

    public CustomTabProvider(FileConfiguration config) {

        this.serverIp = config.getString("tab.server-ip", "yoursever.com");
        this.speedTitle = config.getDouble("tab.title-speed", 0.5);
        this.speedIp = config.getDouble("tab.ip-speed", 0.3);
        this.enableHeader = config.getBoolean("tab.enable-header", true);
        this.enableFooter = config.getBoolean("tab.enable-footer", true);


        loadColors(config.getStringList("tab.title-colors"), titleColors, 0xFF0000, 0xFFFF00);
        loadColors(config.getStringList("tab.ip-colors"), ipColors, 0x00FF00, 0x00FFFF);
    }

    private void loadColors(List<String> hexStrings, List<TextColor> targetList, int def1, int def2) {
        if (hexStrings.isEmpty()) {
            targetList.add(TextColor.color(def1));
            targetList.add(TextColor.color(def2));
        } else {
            for (String hex : hexStrings) {
                targetList.add(TextColor.fromHexString(hex));
            }
        }
    }

    public void updateTab(Player player) {

        String role = HideAndSeek.getDataController().getHiders().contains(player.getUniqueId()) ? "Hider" : "Seeker";
        int total = Bukkit.getOnlinePlayers().size();
        int h = HideAndSeek.getDataController().getHiders().size();
        int s = HideAndSeek.getDataController().getSeekers().size();
        int p = HideAndSeek.getDataController().getPoints(player.getUniqueId());
        int c = ItemSkinSelectionService.getCoins(player.getUniqueId());

        Component header = buildHeader(player.getName());
        Component footer = buildFooter(role, total, h, s, p, c);

        if (enableHeader) player.sendPlayerListHeader(header);
        if (enableFooter) player.sendPlayerListFooter(footer);
        animationTick++;
    }

    private Component buildHeader(String playerName) {

        Component animatedTitle = buildWaveText("HideAndSeek", speedTitle, titleColors);
        Component animatedIp = buildWaveText(serverIp, speedIp, ipColors);

        return Component.text()
                .append(Component.text("Hello ", NamedTextColor.GRAY))
                .append(Component.text(playerName + "!", NamedTextColor.YELLOW))
                .append(Component.text("\nYou are playing ", NamedTextColor.GRAY))
                .append(animatedTitle)
                .append(Component.text(" on ", NamedTextColor.GRAY))
                .append(animatedIp)
                .append(Component.text("\n\n"))
                .build();
    }

    private Component buildWaveText(String text, double spread, List<TextColor> colorList) {
        TextComponent.Builder builder = Component.text();

        for (int i = 0; i < text.length(); i++) {
            double t = (i * spread) + (animationTick * 0.2);
            float ratio = (float) ((Math.sin(t) + 1) / 2);

            builder.append(Component.text(text.charAt(i), interpolateColor(ratio, colorList)));
        }
        return builder.build();
    }

    private TextColor interpolateColor(float ratio, List<TextColor> list) {
        if (list.size() < 2) return list.getFirst();

        float sector = ratio * (list.size() - 1);
        int index = (int) sector;
        float localRatio = sector - index;

        if (index >= list.size() - 1) return list.getLast();

        return TextColor.lerp(localRatio, list.get(index), list.get(index + 1));
    }

    private Component buildFooter(String role, int total, int h, int s, int p, int c) {
        TextColor roleColor = role.equals("Hider") ? NamedTextColor.BLUE : NamedTextColor.RED;

        return Component.text()
                .append(Component.text("\nRole: ", NamedTextColor.DARK_GRAY))
                .append(Component.text(role, roleColor))
                .append(Component.text("\nPlayers: ", NamedTextColor.DARK_GRAY))
                .append(Component.text(total, NamedTextColor.WHITE))
                .append(Component.text(" (", NamedTextColor.DARK_GRAY))
                .append(Component.text(h, NamedTextColor.GREEN))
                .append(Component.text("|", NamedTextColor.DARK_GRAY))
                .append(Component.text(s, NamedTextColor.RED))
                .append(Component.text(")", NamedTextColor.DARK_GRAY))
                .append(Component.text("\nPoints: ", NamedTextColor.GOLD))
                .append(Component.text(p, NamedTextColor.GOLD))
                .append(Component.text("\nCoins: ", NamedTextColor.YELLOW))
                .append(Component.text(c, NamedTextColor.YELLOW))
                .append(Component.text("\n"))
                .build();
    }
}