package mcevent.MCEFramework.generalGameObject;

import lombok.Data;

import java.util.ArrayList;

/*
MCETimeline: 定义了游戏时间线，每个节点代表一个运行状态
 */
@Data
public class MCETimeline {
    private ArrayList<MCETimelineNode> timelineState = new ArrayList<>();
    private boolean isMainTimeline = false; // 如果不是主时间线，执行完之后需要切换回主时间线
    private boolean hasStarted = false;
    private boolean isSuspended = false;
    private int currentState = 0;

    public MCETimeline() {}

    public MCETimeline(boolean isMainTimeline) {
        this.isMainTimeline = isMainTimeline;
    }

    public void addTimelineNode(MCETimelineNode node) {
        if (node.getMaxDurationSeconds() == 0) return;
        timelineState.add(node);
    }

    public void reset() {
        if (hasStarted) timelineState.get(currentState).stop();
        timelineState.clear();
    }

    public void suspend() {
        if (hasStarted && currentState < timelineState.size()) {
            timelineState.get(currentState).stop();
        }
        isSuspended = true;
        hasStarted = false;
    }

    public void resume() {
        timelineState.get(currentState).resume();
        isSuspended = false;
    }

    public void start() {
        currentState = 0;
        timelineState.get(currentState).start();
        hasStarted = true;
    }

    public void nextState() {
        // 调试日志：记录时间线状态转换
        mcevent.MCEFramework.miscellaneous.Constants.plugin.getLogger().info(
            "MCETimeline: nextState() 被调用 - 从状态 " + currentState + " 转换到状态 " + 
            (currentState < timelineState.size() - 1 ? (currentState + 1) : "结束") + 
            " (总状态数: " + timelineState.size() + ")");
        
        timelineState.get(currentState).stop();
        if (currentState < timelineState.size() - 1) {
            currentState++;
            timelineState.get(currentState).start();
        }
        else {
            hasStarted = false;
            mcevent.MCEFramework.miscellaneous.Constants.plugin.getLogger().info(
                "MCETimeline: 时间线已到达最后一个状态，游戏结束");
        }
    }

    public int getCounter() {
        return timelineState.get(currentState).getTimer().getCounter();
    }

    public int getCurrentTimelineNodeDuration() {
        int counter = timelineState.get(currentState).getTimer().getCounter();
        int maxDuration = timelineState.get(currentState).getTimer().getMaxDurationSeconds();
        return maxDuration - counter;
    }
    
    /**
     * 获取当前时间线节点的剩余时间（秒）
     */
    public int getRemainingTime() {
        if (timelineState == null || timelineState.isEmpty() || currentState < 0 || currentState >= timelineState.size()) {
            return 0;
        }
        MCETimelineNode currentNode = timelineState.get(currentState);
        if (currentNode == null || currentNode.getTimer() == null) {
            return 0;
        }
        return currentNode.getTimer().getRemainingTime();
    }
}
