package mcevent.MCEFramework.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import mcevent.MCEFramework.MCEMainController;
import mcevent.MCEFramework.tools.MCEMessenger;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/*
Stop: 停止当前正在运行的游戏
usage: stop
 */
@CommandAlias("stopgame")
@CommandPermission("stop.use")
public class Stop extends BaseCommand {

    @Default
    public void stopGame(CommandSender sender) {
        if (!MCEMainController.checkGameRunning()) {
            if (sender instanceof Player) {
                MCEMessenger.sendInfoToPlayer("当前没有正在运行的游戏！", (Player) sender);
            } else {
                sender.sendMessage("当前没有正在运行的游戏！");
            }
            return;
        }

        boolean success = MCEMainController.stopCurrentGame();
        if (success) {
            if (sender instanceof Player) {
                MCEMessenger.sendInfoToPlayer("游戏已停止！", (Player) sender);
            } else {
                sender.sendMessage("游戏已停止！");
            }
        } else {
            if (sender instanceof Player) {
                MCEMessenger.sendInfoToPlayer("停止游戏失败！", (Player) sender);
            } else {
                sender.sendMessage("停止游戏失败！");
            }
        }
    }
}