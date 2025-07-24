package mcevent.MCEFramework.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import mcevent.MCEFramework.MCEMainController;
import mcevent.MCEFramework.tools.MCEMessenger;

/*
Suspend: 暂停当前游戏进行状态。只对指定的可暂停游戏状态生效。
usage: suspend
 */
@CommandAlias("suspend")
@CommandPermission("suspend.use")
public class Suspend extends BaseCommand {

    @Default
    public void onSuspend() {
        MCEMainController.getCurrentTimeline().suspend();
        MCEMessenger.sendGlobalTitle("<red>游戏已暂停！</red>", "请等待");
    }
}
