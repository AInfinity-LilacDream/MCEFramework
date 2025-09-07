package mcevent.MCEFramework.commands;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import mcevent.MCEFramework.miscellaneous.Constants;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/*
ShuffleTeam: 随机分队
usage: shuffleteam [队伍数量]
 */
@CommandAlias("shuffleteam")
@CommandPermission("shuffleteam.use")
public class ShuffleTeam extends BaseCommand {

    private static final Scoreboard teamBoard = Bukkit.getScoreboardManager().getMainScoreboard();

    @Default
    @CommandCompletion("2|3|4|5|6|7|8|9|10")
    public void onShuffle(@Optional Integer teamCount) {
        List<Player> onlinePlayers = new ArrayList<>(Bukkit.getOnlinePlayers());
        
        // 如果没有提供参数，使用默认行为
        if (teamCount == null) {
            int defaultTeamCount = Math.min(onlinePlayers.size(), Constants.teams.length);
            shuffleTeams(defaultTeamCount);
            return;
        }
        // 参数验证
        if (teamCount <= 0) {
            Bukkit.broadcastMessage("§c队伍数量必须大于0！");
            return;
        }
        
        if (teamCount > Constants.teams.length) {
            Bukkit.broadcastMessage("§c队伍数量不能超过 " + Constants.teams.length + "！");
            return;
        }
        
        if (teamCount > onlinePlayers.size()) {
            Bukkit.broadcastMessage("§c队伍数量不能超过在线玩家数量（" + onlinePlayers.size() + "）！");
            return;
        }
        
        // 执行分队并显示反馈
        shuffleTeams(teamCount);
        Bukkit.broadcastMessage("§a已将所有玩家平均分成 " + teamCount + " 个队伍！");
    }
    
    private void shuffleTeams(int teamCount) {
        List<Player> onlinePlayers = new ArrayList<>(Bukkit.getOnlinePlayers());
        Collections.shuffle(onlinePlayers);
        // 移除队伍顺序打乱，保持 Constants.teams 的原始顺序
        // Collections.shuffle(Arrays.asList(Constants.teams));

        clearExistingTeams();

        // 只创建需要的队伍数量
        for (int i = 0; i < teamCount; ++i) {
            Team team = teamBoard.registerNewTeam(Constants.teams[i].teamName());
            team.color(Constants.teams[i].teamColor());
        }

        List<Team> teams = new ArrayList<>(teamBoard.getTeams());
        
        // 平均分配玩家到指定数量的队伍中
        for (int i = 0; i < onlinePlayers.size(); ++i) {
            Player player = onlinePlayers.get(i);
            Team team = teams.get(i % teamCount);
            team.addEntry(player.getName());
        }
    }

    private void clearExistingTeams() {
        teamBoard.getTeams().forEach(Team::unregister);
    }
}
