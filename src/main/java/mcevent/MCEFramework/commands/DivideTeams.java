package mcevent.MCEFramework.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Default;
import mcevent.MCEFramework.miscellaneous.Constants;
import mcevent.MCEFramework.tools.MCEMessenger;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/*
DivideTeams: 将所有玩家平均随机分为红蓝两队
usage: divideteams
*/
@CommandAlias("divideteams")
@CommandPermission("divideteams.use")
public class DivideTeams extends BaseCommand {

    private static final Scoreboard teamBoard = Bukkit.getScoreboardManager().getMainScoreboard();

    @Default
    public void onDivide(CommandSender sender) {
        List<Player> onlinePlayers = new ArrayList<>(Bukkit.getOnlinePlayers());
        
        if (onlinePlayers.isEmpty()) {
            if (sender instanceof Player) {
                MCEMessenger.sendInfoToPlayer("<red>没有在线玩家可以分队！</red>", (Player) sender);
            } else {
                Bukkit.getLogger().info("没有在线玩家可以分队！");
            }
            return;
        }

        // 随机打乱玩家列表
        Collections.shuffle(onlinePlayers);

        // 清除现有队伍
        clearExistingTeams();

        // 创建红队和蓝队
        Team redTeam = teamBoard.registerNewTeam(Constants.teams[0].teamName()); // 红色山楂
        redTeam.color(Constants.teams[0].teamColor());
        
        Team blueTeam = teamBoard.registerNewTeam(Constants.teams[7].teamName()); // 蓝色葡萄
        blueTeam.color(Constants.teams[7].teamColor());

        // 将玩家平均分配到两队
        for (int i = 0; i < onlinePlayers.size(); i++) {
            Player player = onlinePlayers.get(i);
            if (i % 2 == 0) {
                redTeam.addEntry(player.getName());
            } else {
                blueTeam.addEntry(player.getName());
            }
        }

        // 发送分队结果消息
        int redCount = redTeam.getEntries().size();
        int blueCount = blueTeam.getEntries().size();
        
        String message = String.format("<green>分队完成！红队：%d人，蓝队：%d人</green>", redCount, blueCount);
        
        if (sender instanceof Player) {
            MCEMessenger.sendInfoToPlayer(message, (Player) sender);
        } else {
            Bukkit.getLogger().info("分队完成！红队：" + redCount + "人，蓝队：" + blueCount + "人");
        }
    }

    private void clearExistingTeams() {
        teamBoard.getTeams().forEach(Team::unregister);
    }
}