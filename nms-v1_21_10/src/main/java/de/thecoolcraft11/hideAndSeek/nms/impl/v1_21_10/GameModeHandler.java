package de.thecoolcraft11.hideAndSeek.nms.impl.v1_21_10;

import net.minecraft.network.protocol.game.ClientboundGameEventPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import org.bukkit.GameMode;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;


public final class GameModeHandler {

    public void setServerSpectator(Player player) {
        ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();
        serverPlayer.setGameMode(GameType.SPECTATOR);
    }


    public void spoofClientGameMode(Player player, GameMode mode) {
        ServerPlayer serverPlayer = ((CraftPlayer) player).getHandle();

        float value = switch (mode) {
            case CREATIVE -> 1f;
            case ADVENTURE -> 2f;
            case SPECTATOR -> 3f;
            default -> 0f;
        };

        serverPlayer.connection.send(
                new ClientboundGameEventPacket(
                        ClientboundGameEventPacket.CHANGE_GAME_MODE,
                        value
                )
        );
    }
}
