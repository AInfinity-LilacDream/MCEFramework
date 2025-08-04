package mcevent.MCEFramework.tools;

import mcevent.MCEFramework.miscellaneous.Constants;
import org.bukkit.Bukkit;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

/*
MCETimerUtils: 计时器相关函数
 */
public class MCETimerUtils {
    public static BukkitRunnable setDelayedTask(double seconds, MCETimerFunction function) {
        BukkitRunnable runnable = new BukkitRunnable() {
            @Override
            public void run() {
                function.run();
            }
        };
        runnable.runTaskLater(Constants.plugin, (long)(20L * seconds));

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

    // 有时长的帧定时任务
    public static void setFramedTaskWithDuration(MCETimerFunction function, double duration) {
        BukkitRunnable runnable = new BukkitRunnable() {
            int time = 0;
            final int frame = (int)(duration * 20);
            @Override
            public void run() {
                function.run();
                time++;
                if (time == frame) this.cancel();
            }
        };
        runnable.runTaskTimer(Constants.plugin, 0L, 1L);
    }

    // 展示一个时间流逝的Boss栏动画
    public static void showGlobalDurationOnBossBar(BossBar bossBar, double duration, boolean isReversed) {
        bossBar.removeAll();
        bossBar.setColor(isReversed ? BarColor.GREEN : BarColor.RED);
        for (Player player : Bukkit.getOnlinePlayers()) bossBar.addPlayer(player);

        final double[] count = {0};

        setFramedTaskWithDuration(() -> {
            double frame = duration * 20;
            final double progress = isReversed ? (count[0] / frame) : ((frame - count[0]) / frame);
            bossBar.setProgress(progress);
            count[0]++;
        }, duration);
    }
}
