package mcevent.MCEFramework.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import mcevent.MCEFramework.MCEMainController;
import mcevent.MCEFramework.tools.MCEMessenger;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static mcevent.MCEFramework.miscellaneous.Constants.*;

/*
Launch: 立即启动游戏
usage: launch <game>
<game>: 游戏名称
 */
@CommandAlias("launch")
@CommandPermission("launch.use")
public class Launch extends BaseCommand {

    @Subcommand("pkt")
    public void launchParkourTag(CommandSender sender) {
        if (checkGameRunning()) {
            if (sender instanceof Player)
                MCEMessenger.sendInfoToPlayer("当前有游戏正在运行中！", (Player) sender);
        }
        else MCEMainController.immediateLaunchGame(pkt.getId(), true);
    }

    @Subcommand("pktNoIntro")
    public void launchParkourTagNoIntro(CommandSender sender) {
        if (checkGameRunning()) {
            if (sender instanceof Player)
                MCEMessenger.sendInfoToPlayer("当前有游戏正在运行中！", (Player) sender);
        }
        else MCEMainController.immediateLaunchGame(pkt.getId(), false);
    }

    @Subcommand("discofever")
    public void launchDiscoFever(CommandSender sender) {
        if (checkGameRunning()) {
            if (sender instanceof Player)
                MCEMessenger.sendInfoToPlayer("当前有游戏正在运行中！", (Player) sender);
        }
        else MCEMainController.immediateLaunchGame(discoFever.getId(), true);
    }

    @Subcommand("discofeverNoIntro")
    public void launchDiscoFeverNoIntro(CommandSender sender) {
        if (checkGameRunning()) {
            if (sender instanceof Player)
                MCEMessenger.sendInfoToPlayer("当前有游戏正在运行中！", (Player) sender);
        }
        else MCEMainController.immediateLaunchGame(discoFever.getId(), false);
    }

    private boolean checkGameRunning() {
        return MCEMainController.checkGameRunning();
    }
}
