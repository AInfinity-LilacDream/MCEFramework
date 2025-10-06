package mcevent.MCEFramework.tools;

import lombok.Data;
import mcevent.MCEFramework.generalGameObject.MCEGameBoard;
import mcevent.MCEFramework.generalGameObject.MCETimeline;
import mcevent.MCEFramework.miscellaneous.Constants;
import org.bukkit.scheduler.BukkitRunnable;

/*
MCEPausableTimer: 支持暂停的计时器
 */
@Data
public class MCEPausableTimer {
    private boolean isPaused = false;
    private BukkitRunnable task;
    private int maxDurationSeconds;
    private int counter;
    private MCETimeline parentTimeline;
    private MCEGameBoard gameBoard;

    public MCEPausableTimer(int seconds, MCETimeline parentTimeline, MCEGameBoard gameBoard) {
        maxDurationSeconds = seconds;
        this.parentTimeline = parentTimeline;
        this.gameBoard = gameBoard;
        this.counter = maxDurationSeconds;
    }

    public void start() {
        // 如果持续时间为0，延迟1tick后跳到下一个状态（确保当前阶段的回调先执行）
        if (maxDurationSeconds == 0) {
            task = new BukkitRunnable() {
                @Override
                public void run() {
                    parentTimeline.nextState();
                }
            };
            task.runTaskLater(Constants.plugin, 1L); // 1 tick 延迟
            return;
        }

        task = new BukkitRunnable() {

            @Override
            public void run() {
                if (!isPaused) {
                    counter--;
                    gameBoard.globalDisplay();
                }
                if (counter == 0) {
                    counter = maxDurationSeconds;
                    parentTimeline.nextState();
                }
            }
        };
        // 修复初始立刻触发导致少1秒的问题：首轮延迟1秒再开始每秒递减
        task.runTaskTimer(Constants.plugin, 20L, 20L);
    }

    public void pause() {
        isPaused = true;
    }

    public void resume() {
        isPaused = false;
    }

    public void stop() {
        if (task != null && !task.isCancelled())
            task.cancel();
    }

    /**
     * 获取剩余时间（秒）
     */
    public int getRemainingTime() {
        return counter;
    }
}
