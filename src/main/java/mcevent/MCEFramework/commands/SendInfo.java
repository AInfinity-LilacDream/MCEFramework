package mcevent.MCEFramework.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import mcevent.MCEFramework.tools.MCEMessenger;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.CommandBlock;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/*
SendInfo: 发送一条信息
usage: sendInfo pkmedium
       sendInfo pkhard
       sendInfo <message>
<message>: 要发送的消息。支持MiniMessage语义标签
e.g. /sendInfo <gradient:red:blue>这是一条渐变色的消息</gradient>
 */
@CommandAlias("sendInfo")
@CommandPermission("sendInfo.use")
public class SendInfo extends BaseCommand {

    @Default
    public void onDefault(@Single String message) {
        MCEMessenger.sendGlobalInfo(message);
    }

    @Subcommand("pkmedium")
    public void onPkmedium(CommandSender sender) {
        if (!(sender instanceof BlockCommandSender blockSender)) {
            MCEMessenger.sendInfoToPlayer("<red>此命令只能通过命令方块执行！</red>", (Player) sender);
            return;
        }

        CommandBlock commandBlock = (CommandBlock) blockSender.getBlock().getState();
        Location blockLocation = commandBlock.getLocation();

        Player nearestPlayer = findNearestPlayer(blockLocation);

        MCEMessenger.sendGlobalInfo("<gold><bold>" + nearestPlayer.getName() + "</bold></gold><yellow>完成了</yellow><blue><bold>中等模式</bold></blue><yellow>下的跑酷！</yellow>");

    }

    @Subcommand("pkhard")
    public void onPkhard(CommandSender sender) {
        if (!(sender instanceof BlockCommandSender blockSender)) {
            MCEMessenger.sendInfoToPlayer("<red>此命令只能通过命令方块执行！</red>", (Player) sender);
            return;
        }

        CommandBlock commandBlock = (CommandBlock) blockSender.getBlock().getState();
        Location blockLocation = commandBlock.getLocation();

        Player nearestPlayer = findNearestPlayer(blockLocation);

        MCEMessenger.sendGlobalInfo("<dark_purple><bold>" + nearestPlayer.getName() + "</bold></dark_purple><yellow>完成了</yellow><red><bold>困难模式</bold></red><yellow>下的跑酷！</yellow>");
    }

    private Player findNearestPlayer(Location location) {
        Player nearestPlayer = null;
        double nearestDistance = Double.MAX_VALUE;

        for (Player player : Bukkit.getOnlinePlayers()) {
            double distance = player.getLocation().distanceSquared(location);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearestPlayer = player;
            }
        }

        return nearestPlayer;
    }
}