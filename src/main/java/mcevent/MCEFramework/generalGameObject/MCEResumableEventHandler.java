package mcevent.MCEFramework.generalGameObject;

import lombok.Data;

/*
MCEResumableHandler: 可手动控制开启/关闭的事件监听器
 */
@Data
public class MCEResumableEventHandler {
    private boolean isSuspended = true;
    public void start() { isSuspended = false; }
    public void suspend() { isSuspended = true; }
}
