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

import java.util.Objects;

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
        if (!(sender instanceof Player player)) return;
        for (TeamWithDetails teamWithDetails : Constants.teams) {
            if (Objects.equals(teamWithDetails.alias(), team)) {
                Objects.requireNonNull(teamBoard.getTeam(teamWithDetails.teamName())).addEntry(player.getName());
                return;
            }
        }
        MCEMessenger.sendInfoToPlayer("该队伍不存在！", player);
    }

    @Subcommand("join")
    @Syntax("<player> <team>")
    public void onPlayerPartyJoin(CommandSender sender, @Single String playerName, @Single String team) {
        if (!(sender instanceof Player)) return;

        Player player = Bukkit.getPlayer(playerName);
        if (player == null) {
            MCEMessenger.sendInfoToPlayer("<red>找不到玩家！</red>", (Player) sender);
            return;
        }

        for (TeamWithDetails teamWithDetails : Constants.teams) {
            if (Objects.equals(teamWithDetails.alias(), team)) {
                Objects.requireNonNull(teamBoard.getTeam(teamWithDetails.teamName())).addEntry(player.getName());
                return;
            }
        }
        MCEMessenger.sendInfoToPlayer("该队伍不存在！", player);
    }
}
