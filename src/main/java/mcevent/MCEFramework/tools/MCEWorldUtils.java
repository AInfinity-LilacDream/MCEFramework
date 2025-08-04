package mcevent.MCEFramework.tools;

import mcevent.MCEFramework.MCEMainController;
import org.bukkit.Location;

/*
MCEWorldUtils: 世界相关工具类
 */
public class MCEWorldUtils {

    // 计算相对某位置的偏移位置
    public static Location teleportLocation(Location baseLoc, Location offsetLoc, int offset) {
        return new Location(
                baseLoc.getWorld(),
                baseLoc.getX() + offsetLoc.getX() * offset,
                baseLoc.getY() + offsetLoc.getY() * offset,
                baseLoc.getZ() + offsetLoc.getZ() * offset
        );
    }

    public static void enablePVP() {
        MCEMainController.getGlobalPVPHandler().suspend();
    }

    public static void disablePVP() {
        MCEMainController.getGlobalPVPHandler().start();
    }
}
