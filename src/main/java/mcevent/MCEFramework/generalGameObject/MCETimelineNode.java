package mcevent.MCEFramework.generalGameObject;

import lombok.Data;
import mcevent.MCEFramework.MCEMainController;
import mcevent.MCEFramework.tools.MCEPausableTimer;

/*
MCETimelineNode: 游戏时间线的单个节点
包含当前节点需要执行的函数，当前节点的最长持续时间等
*/
@Data
public class MCETimelineNode {
    private int maxDurationSeconds;
    private boolean canSuspend;
    private boolean isSwitchNode; // 是否在这个node进入子游戏timeline
    private MCETimeLineNodeFunction onRunning;
    private MCEPausableTimer timer;
    private MCETimeline parentTimeline;
    private MCETimeline switchedTimeline; // 如果进入子游戏，切换到子游戏的timeline

    public MCETimelineNode(
            int maxDurationSeconds,
            boolean canSuspend,
            MCETimeLineNodeFunction onRunning,
            MCETimeline parentTimeline,
            MCEGameBoard gameBoard) {
        this.maxDurationSeconds = maxDurationSeconds;
        this.canSuspend = canSuspend;
        this.onRunning = onRunning;
        this.parentTimeline = parentTimeline;
        this.timer = new MCEPausableTimer(maxDurationSeconds, parentTimeline, gameBoard);
        this.isSwitchNode = false;
    }

    public void suspend() {
        if (canSuspend) timer.pause();
    }

    public void resume() {
        timer.resume();
    }

    public void start() {
        if (!isSwitchNode) {
            timer.start();
            onRunning.onRunning();
        }
        else MCEMainController.switchToTimeline(switchedTimeline, parentTimeline); // 切换到游戏时间线
    }

    public void stop() {
        timer.stop();
    }
}
