package mcevent.MCEFramework.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import mcevent.MCEFramework.tools.MCEMessenger;
import mcevent.MCEFramework.tools.MCETeamUtils;

/*
togglefriendlyfire: 开/关友伤系统
usage: togglefriendlyfire
 */
@CommandAlias("togglefriendlyfire")
@CommandPermission("togglefriendlyfire.use")
public class ToggleFriendlyFire extends BaseCommand {

    @Default
    public void onToggleFriendlyFire() {
        if (MCETeamUtils.isFriendlyFireEnabled()) {
            MCETeamUtils.disableFriendlyFire();
            MCEMessenger.sendGlobalInfo("友伤系统已关闭！");
        } else {
            MCETeamUtils.enableFriendlyFire();
            MCEMessenger.sendGlobalInfo("友伤系统已开启！");
        }
    }
}