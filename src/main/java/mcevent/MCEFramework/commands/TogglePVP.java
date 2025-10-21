package mcevent.MCEFramework.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import mcevent.MCEFramework.MCEMainController;
import mcevent.MCEFramework.customHandler.GlobalPVPHandler;
import mcevent.MCEFramework.tools.MCEMessenger;
import mcevent.MCEFramework.tools.MCEWorldUtils;

/*
togglepvp: 全局开/关PVP
usage: togglepvp
 */
@CommandAlias("togglepvp")
@CommandPermission("togglepvp.use")
public class TogglePVP extends BaseCommand {

    @Default
    public void onTogglePVP() {
        GlobalPVPHandler globalPVPHandler = MCEMainController.getGlobalPVPHandler();
        if (globalPVPHandler.isSuspended()) {
            MCEWorldUtils.disablePVP();
            MCEMessenger.sendGlobalInfo("全局PVP已关闭！");
        } else {
            MCEWorldUtils.enablePVP();
            MCEMessenger.sendGlobalInfo("全局PVP已开启！");
        }
    }
}
