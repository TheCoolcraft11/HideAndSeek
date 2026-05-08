package de.thecoolcraft11.hideAndSeek.nms.impl.v1_21_10;

import net.minecraft.network.protocol.game.ClientboundInitializeBorderPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.border.WorldBorder;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Player;


@SuppressWarnings("resource")
public final class WorldBorderHandler {

    public void showWarning(Player viewer, float strength) {
        if (viewer == null || !viewer.isOnline()) {
            return;
        }
        try {
            ServerPlayer handle = ((CraftPlayer) viewer).getHandle();
            WorldBorder serverBorder = handle.level().getWorldBorder();


            WorldBorder fakeBorder = new WorldBorder();
            fakeBorder.setCenter(serverBorder.getCenterX(), serverBorder.getCenterZ());
            fakeBorder.setSize(serverBorder.getSize());
            fakeBorder.setWarningBlocks((int) (serverBorder.getSize() * strength));
            fakeBorder.setWarningTime(0);

            handle.connection.send(new ClientboundInitializeBorderPacket(fakeBorder));
        } catch (Throwable ignored) {
        }
    }


    public void resetWarning(Player viewer) {
        if (viewer == null || !viewer.isOnline()) {
            return;
        }
        try {
            ServerPlayer handle = ((CraftPlayer) viewer).getHandle();
            handle.connection.send(
                    new ClientboundInitializeBorderPacket(handle.level().getWorldBorder()));
        } catch (Throwable ignored) {
        }
    }
}
