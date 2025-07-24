package mcevent.MCEFramework.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import mcevent.MCEFramework.tools.MCEMessenger;
import mcevent.MCEFramework.tools.MCEPlayerUtils;
import mcevent.MCEFramework.tools.MCETeamUtils;
import mcevent.MCEFramework.tools.MCEWorldUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.CommandBlock;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;

import static mcevent.MCEFramework.miscellaneous.Constants.*;

import java.util.ArrayList;

/*
PKTSelectChaser: 只能被命令方块执行
usage: pktSelectChaser
 */
@CommandAlias("pktSelectChaser")
@CommandPermission("pktSelectChaser.use")
public class PKTSelectChaser extends BaseCommand {
    @Default
    public void onPKTSelectChaser(CommandSender sender) {
        if (!(sender instanceof BlockCommandSender blockSender)) {
            MCEMessenger.sendInfoToPlayer("<red>此命令只能通过命令方块执行！</red>", (Player) sender);
            return;
        }

        CommandBlock commandBlock = (CommandBlock) blockSender.getBlock().getState();
        Location blockLocation = commandBlock.getLocation();

        Player nearestPlayer = findNearestPlayer(blockLocation);
        MCEPlayerUtils.clearTag(nearestPlayer);
        nearestPlayer.addScoreboardTag("chaser");

        Team ownTeam = MCETeamUtils.getTeam(nearestPlayer);
        ArrayList<Team> activeTeam = pkt.getActiveTeams();

        int teamPos = activeTeam.indexOf(ownTeam);
        Team opponentTeam = teamPos % 2 == 0 ? activeTeam.get(teamPos + 1) : activeTeam.get(teamPos - 1);

        int offset = teamPos / 2;
        Location locDown, locUp;
        if (teamPos % 2 == 0) {
            locDown = MCEWorldUtils.teleportLocation(pktDoorLocation1down, pktOffset, offset);
            locUp = MCEWorldUtils.teleportLocation(pktDoorLocation1up, pktOffset, offset);
        }
        else {
            locDown = MCEWorldUtils.teleportLocation(pktDoorLocation2down, pktOffset, offset);
            locUp = MCEWorldUtils.teleportLocation(pktDoorLocation2up, pktOffset, offset);
        }

        Block blockDown = locDown.getBlock();
        Block blockUp = locUp.getBlock();

        blockDown.setType(Material.RED_STAINED_GLASS_PANE);
        blockUp.setType(Material.RED_STAINED_GLASS_PANE);

        if (ownTeam != null) {
            MCEMessenger.sendInfoToTeam(opponentTeam, ownTeam.getName() + "选择了" + MCEPlayerUtils.getColoredPlayerName(nearestPlayer) + "来抓捕你们！");
        }
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
