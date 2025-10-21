package mcevent.MCEFramework.games.crazyMiner.customHandler;

import mcevent.MCEFramework.generalGameObject.MCEResumableEventHandler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.WorldBorder;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import static mcevent.MCEFramework.miscellaneous.Constants.*;

/**
 * BorderDistanceHandler: 边界距离提示处理器
 * 当玩家距离世界边界10格以内时，在ActionBar显示距离提示
 */
public class BorderDistanceHandler extends MCEResumableEventHandler {

    private BukkitTask distanceCheckTask;
    private static final int WARNING_DISTANCE = 10; // 警告距离阈值

    public BorderDistanceHandler() {
        // 不需要注册事件监听器，使用定时任务检查
    }

    public void start() {
        super.start();

        // 启动定时检查任务，每1tick（逐帧）检查一次
        distanceCheckTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (isSuspended())
                    return;

                checkAllPlayersDistanceToBorder();
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    public void stop() {
        super.suspend();

        if (distanceCheckTask != null && !distanceCheckTask.isCancelled()) {
            distanceCheckTask.cancel();
            distanceCheckTask = null;
        }
    }

    /**
     * 检查所有玩家与边界的距离
     */
    private void checkAllPlayersDistanceToBorder() {
        if (crazyMiner == null || crazyMiner.getWorldName() == null) {
            return;
        }

        org.bukkit.World world = Bukkit.getWorld(crazyMiner.getWorldName());
        if (world == null)
            return;

        WorldBorder border = world.getWorldBorder();
        if (border == null)
            return;

        // 获取边界中心和大小
        Location borderCenter = border.getCenter();
        double borderSize = border.getSize();
        double borderRadius = borderSize / 2.0;

        for (Player player : Bukkit.getOnlinePlayers()) {
            // 只检查在游戏世界中且为冒险模式并有Active标签的玩家
            if (!player.getWorld().equals(world) ||
                    player.getGameMode() != GameMode.SURVIVAL ||
                    !player.getScoreboardTags().contains("Active")) {
                continue;
            }

            Location playerLocation = player.getLocation();
            double distanceToBorder = calculateDistanceToBorder(playerLocation, borderCenter, borderRadius);

            // 如果距离边界小于等于警告距离，显示警告
            if (distanceToBorder <= WARNING_DISTANCE) {
                showBorderWarning(player, distanceToBorder);
            }
        }
    }

    /**
     * 计算玩家到边界的最短距离
     */
    private double calculateDistanceToBorder(Location playerLocation, Location borderCenter, double borderRadius) {
        double playerX = playerLocation.getX();
        double playerZ = playerLocation.getZ();
        double centerX = borderCenter.getX();
        double centerZ = borderCenter.getZ();

        // 计算玩家到边界中心的距离
        double distanceToCenter = Math.sqrt(Math.pow(playerX - centerX, 2) + Math.pow(playerZ - centerZ, 2));

        // 计算到边界的距离（边界半径减去到中心的距离）
        double distanceToBorder = borderRadius - distanceToCenter;

        // 边界是方形的，不是圆形的，需要重新计算
        // 计算玩家到方形边界的最短距离
        double halfSize = borderRadius; // borderRadius实际上是边界大小的一半

        // 计算玩家相对于中心的坐标
        double relativeX = Math.abs(playerX - centerX);
        double relativeZ = Math.abs(playerZ - centerZ);

        // 计算到方形边界的最短距离
        double distanceToEdgeX = halfSize - relativeX;
        double distanceToEdgeZ = halfSize - relativeZ;

        // 返回到最近边界的距离（X或Z方向中较小的那个）
        return Math.min(distanceToEdgeX, distanceToEdgeZ);
    }

    /**
     * 向玩家显示边界警告
     */
    private void showBorderWarning(Player player, double distance) {
        Component message;
        int distanceInt = (int) Math.ceil(distance);

        if (distance <= 0) {
            // 玩家已经在边界外或边界上
            message = Component.text("⚠ 你已经触碰到边界！立即回到安全区域！", NamedTextColor.RED);
        } else if (distance <= 3) {
            // 非常接近边界，红色警告
            message = Component.text("⚠ 边界距离你还有 " + distanceInt + " 格！", NamedTextColor.RED);
        } else if (distance <= 6) {
            // 比较接近边界，橙色警告
            message = Component.text("⚠ 边界距离你还有 " + distanceInt + " 格！", NamedTextColor.GOLD);
        } else {
            // 在警告范围内但不太危险，黄色提示
            message = Component.text("⚠ 边界距离你还有 " + distanceInt + " 格！", NamedTextColor.YELLOW);
        }

        player.sendActionBar(message);
    }
}