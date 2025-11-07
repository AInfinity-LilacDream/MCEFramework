package mcevent.MCEFramework.games.hyperSpleef;

/*
事件信息类
*/
public class EventInfo {
    public int timeSeconds; // 事件触发时间（秒）
    public String eventName; // 事件名称

    public EventInfo(int timeSeconds, String eventName) {
        this.timeSeconds = timeSeconds;
        this.eventName = eventName;
    }
}
