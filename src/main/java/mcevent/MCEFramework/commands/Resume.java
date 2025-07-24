package mcevent.MCEFramework.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import mcevent.MCEFramework.MCEMainController;
import mcevent.MCEFramework.tools.MCEMessenger;

/*
Resume: 恢复当前游戏进行状态。只对指定的可暂停游戏状态生效。
usage: resume
 */
@CommandAlias("resume")
@CommandPermission("resume.use")
public class Resume extends BaseCommand {

    @Default
    public void onResume() {
        MCEMessenger.sendGlobalTitle("<green>游戏已恢复！</green>", null);
        MCEMainController.getCurrentTimeline().resume();
    }
}
