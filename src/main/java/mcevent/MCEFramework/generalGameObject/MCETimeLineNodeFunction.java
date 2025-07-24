package mcevent.MCEFramework.generalGameObject;

/*
MCETimelineNodeFunction: 函数式接口，定义了当前时间节点的游戏状态
 */
@FunctionalInterface
public interface MCETimeLineNodeFunction {
    void onRunning();
}
