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
        timelineState.add(node);
    }

    public void reset() {
        if (hasStarted) timelineState.get(currentState).stop();
        timelineState.clear();
    }

    public void suspend() {
        timelineState.get(currentState).suspend();
        isSuspended = true;
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
        timelineState.get(currentState).stop();
        if (currentState < timelineState.size() - 1) {
            currentState++;
            timelineState.get(currentState).start();
        }
        else hasStarted = false;
    }

    public int getCounter() {
        return timelineState.get(currentState).getTimer().getCounter();
    }

    public int getCurrentTimelineNodeDuration() {
        int counter = timelineState.get(currentState).getTimer().getCounter();
        int maxDuration = timelineState.get(currentState).getTimer().getMaxDurationSeconds();
        return maxDuration - counter;
    }
}
