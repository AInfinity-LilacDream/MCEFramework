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
        task.runTaskTimer(Constants.plugin, 0L, 20L);
    }

    public void pause() {
        isPaused = true;
    }

    public void resume() {
        isPaused = false;
    }

    public void stop() {
        if (task != null && !task.isCancelled()) task.cancel();
    }
}
