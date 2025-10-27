package mcevent.MCEFramework.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import mcevent.MCEFramework.miscellaneous.Constants;
import mcevent.MCEFramework.miscellaneous.TeamWithDetails;
import mcevent.MCEFramework.tools.MCEMessenger;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;

/*
Party: 控制玩家加入队伍逻辑
usage: party join <team>
       party join <player> <team>
<player>: 玩家名称
<team>: 队伍名称
 */
@CommandAlias("party")
@CommandPermission("party.use")
public class Party extends BaseCommand {
    private static final Scoreboard teamBoard = Bukkit.getScoreboardManager().getMainScoreboard();

    @Subcommand("join")
    @Syntax("<team>")
    public void onPartyJoin(CommandSender sender, @Single String team) {
        if (!(sender instanceof Player player))
            return;
        for (TeamWithDetails teamWithDetails : Constants.teams) {
            if (teamWithDetails != null && teamWithDetails.alias() != null
                    && teamWithDetails.alias().equalsIgnoreCase(team)) {
                String teamName = teamWithDetails.teamName();
                if (teamName == null || teamName.isEmpty()) {
                    MCEMessenger.sendInfoToPlayer("队伍未配置有效名称！", player);
                    return;
                }
                org.bukkit.scoreboard.Team t = teamBoard.getTeam(teamName);
                if (t == null) {
                    try {
                        t = teamBoard.registerNewTeam(teamName);
                    } catch (IllegalArgumentException e) {
                        MCEMessenger.sendInfoToPlayer("无法创建队伍：" + teamName, player);
                        return;
                    }
                }
                t.addEntry(player.getName());
                return;
            }
        }
        MCEMessenger.sendInfoToPlayer("该队伍不存在！", player);
    }

    @Subcommand("join")
    @Syntax("<player> <team>")
    public void onPlayerPartyJoin(CommandSender sender, @Single String playerName, @Single String team) {
        if (!(sender instanceof Player))
            return;

        Player player = Bukkit.getPlayer(playerName);
        if (player == null) {
            MCEMessenger.sendInfoToPlayer("<red>找不到玩家！</red>", (Player) sender);
            return;
        }

        for (TeamWithDetails teamWithDetails : Constants.teams) {
            if (teamWithDetails != null && teamWithDetails.alias() != null
                    && teamWithDetails.alias().equalsIgnoreCase(team)) {
                String teamName = teamWithDetails.teamName();
                if (teamName == null || teamName.isEmpty()) {
                    MCEMessenger.sendInfoToPlayer("队伍未配置有效名称！", (Player) sender);
                    return;
                }
                org.bukkit.scoreboard.Team t = teamBoard.getTeam(teamName);
                if (t == null) {
                    try {
                        t = teamBoard.registerNewTeam(teamName);
                    } catch (IllegalArgumentException e) {
                        MCEMessenger.sendInfoToPlayer("无法创建队伍：" + teamName, (Player) sender);
                        return;
                    }
                }
                t.addEntry(player.getName());
                return;
            }
        }
        MCEMessenger.sendInfoToPlayer("该队伍不存在！", player);
    }
}
