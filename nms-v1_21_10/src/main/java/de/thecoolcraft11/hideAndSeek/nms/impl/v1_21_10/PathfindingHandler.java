package de.thecoolcraft11.hideAndSeek.nms.impl.v1_21_10;

import org.bukkit.Location;
import org.bukkit.craftbukkit.entity.CraftMob;
import org.bukkit.entity.Mob;


public final class PathfindingHandler {


    public boolean canPathfind(Mob mob, Location start, Location end) {
        if (!start.getWorld().equals(end.getWorld())) {
            return false;
        }

        CraftMob craftMob = (CraftMob) mob;

        try {

            craftMob.getHandle().getNavigation().recomputePath();

            var pathfinder = craftMob.getPathfinder();
            var path = pathfinder.findPath(end);

            if (path == null || path.getFinalPoint() == null) {
                return false;
            }


            return path.getFinalPoint().distanceSquared(end) < 6.0;

        } catch (Exception ignored) {
            return false;
        }
    }
}
