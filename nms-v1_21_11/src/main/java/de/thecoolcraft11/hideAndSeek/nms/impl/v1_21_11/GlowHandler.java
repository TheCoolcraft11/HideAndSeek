package de.thecoolcraft11.hideAndSeek.nms.impl.v1_21_11;

import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.craftbukkit.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.util.List;

public final class GlowHandler {


    private static final byte GLOW_FLAG = 0x40;


    private static volatile EntityDataAccessor<Byte> cachedSharedFlagsAccessor = null;

    @SuppressWarnings("unchecked")
    private static EntityDataAccessor<Byte> getSharedFlagsAccessor() {
        if (cachedSharedFlagsAccessor != null) {
            return cachedSharedFlagsAccessor;
        }
        try {
            Field field = net.minecraft.world.entity.Entity.class.getDeclaredField("DATA_SHARED_FLAGS_ID");
            field.setAccessible(true);
            cachedSharedFlagsAccessor = (EntityDataAccessor<Byte>) field.get(null);
            return cachedSharedFlagsAccessor;
        } catch (Throwable ignored) {
            return null;
        }
    }

    public void setGlowing(Player viewer, Entity target, boolean glowing) {
        if (viewer == null || target == null || !viewer.isOnline() || !target.isValid()) {
            return;
        }

        try {
            ServerPlayer viewerHandle = ((CraftPlayer) viewer).getHandle();
            net.minecraft.world.entity.Entity targetHandle = ((CraftEntity) target).getHandle();

            EntityDataAccessor<Byte> accessor = getSharedFlagsAccessor();
            if (accessor == null) {
                return;
            }

            byte currentFlags = targetHandle.getEntityData().get(accessor);
            byte updatedFlags = glowing
                    ? (byte) (currentFlags | GLOW_FLAG)
                    : (byte) (currentFlags & ~GLOW_FLAG);

            SynchedEntityData.DataValue<Byte> value =
                    SynchedEntityData.DataValue.create(accessor, updatedFlags);

            viewerHandle.connection.send(
                    new ClientboundSetEntityDataPacket(targetHandle.getId(), List.of(value)));
        } catch (Throwable ignored) {
        }
    }

    public void setGlowing(Player viewer, Player target, boolean glowing) {
        setGlowing(viewer, (Entity) target, glowing);
    }
}
