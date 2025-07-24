package mcevent.MCEFramework.tools;

import mcevent.MCEFramework.miscellaneous.Constants;
import org.bukkit.scheduler.BukkitRunnable;

/*
MCETimerUtils: 计时器相关函数
 */
public class MCETimerUtils {
    public static BukkitRunnable setDelayedTask(int seconds, MCETimerFunction function) {
        BukkitRunnable runnable = new BukkitRunnable() {
            @Override
            public void run() {
                function.run();
            }
        };
        runnable.runTaskLater(Constants.plugin, 20L * seconds);

        return runnable;
    }

    // 帧定时任务
    public static BukkitRunnable setFramedTask(MCETimerFunction function) {
        BukkitRunnable runnable = new BukkitRunnable() {
            @Override
            public void run() {
                function.run();
            }
        };
        runnable.runTaskTimer(Constants.plugin, 0L, 1L);

        return runnable;
    }
}
