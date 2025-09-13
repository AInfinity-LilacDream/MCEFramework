package mcevent.MCEFramework.games.hitWall.gameObject;

import fr.mrmicky.fastboard.adventure.FastBoard;
import lombok.Getter;
import lombok.Setter;
import mcevent.MCEFramework.generalGameObject.MCEGameBoard;
import mcevent.MCEFramework.tools.MCETeamUtils;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;

import java.util.Map;

import static mcevent.MCEFramework.miscellaneous.Constants.hitWall;

@Setter @Getter
public class HitWallGameBoard extends MCEGameBoard {
    private int playerCount;
    private int teamCount;
    private Map<String, Integer> teamScores;
    private String playerCountTitle = "";
    private String teamCountTitle = "";
    private String scoreRankingTitle = "";

    public HitWallGameBoard(String gameName, String mapName) {
        super(gameName, mapName);
    }

    public void updatePlayerCount(int playerCount) {
        this.playerCount = playerCount;
        this.playerCountTitle = "<green><bold> 剩余玩家：</bold></green>" +
                playerCount + "/" + Bukkit.getOnlinePlayers().size();
    }

    public void updateTeamCount(int teamCount) {
        this.teamCount = teamCount;
        this.teamCountTitle = "<blue><bold> 剩余队伍：</bold></blue>" +
                teamCount + "/" + hitWall.getActiveTeams().size();
    }

    public void updateTeamScores(Map<String, Integer> teamScores) {
        this.teamScores = teamScores;
        updateScoreRanking();
    }

    private void updateScoreRanking() {
        if (teamScores == null || teamScores.isEmpty()) {
            this.scoreRankingTitle = "<yellow><bold> 队伍得分：</bold></yellow>暂无数据";
            return;
        }

        // 找出得分最高的队伍
        String topTeam = "";
        int maxScore = 0;
        for (Map.Entry<String, Integer> entry : teamScores.entrySet()) {
            if (entry.getValue() > maxScore) {
                maxScore = entry.getValue();
                topTeam = entry.getKey();
            }
        }

        this.scoreRankingTitle = "<yellow><bold> 领先队伍：</bold></yellow>" +
                "<aqua>" + topTeam + "</aqua><white>(" + maxScore + "分)</white>";
    }

    @Override
    public void globalDisplay() {
        int seconds = hitWall.getTimeline().getCounter();

        int minute = seconds / 60;
        int second = seconds % 60;
        String time = String.format("%02d:%02d", minute, second);
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            FastBoard board = new FastBoard(player);
            board.updateTitle(MiniMessage.miniMessage().deserialize(getMainTitle()));
            
            board.updateLines(
                    MiniMessage.miniMessage().deserialize(" "),
                    MiniMessage.miniMessage().deserialize(getGameTitle()),
                    MiniMessage.miniMessage().deserialize(getMapTitle()),
                    MiniMessage.miniMessage().deserialize(getStateTitle() + time),
                    MiniMessage.miniMessage().deserialize(getPlayerCountTitle()),
                    MiniMessage.miniMessage().deserialize(getTeamCountTitle())
            );
        }
    }
    
    /**
     * 获取玩家的队伍分数信息
     */
    private String getPlayerScoreInfo(Player player) {
        if (teamScores == null || teamScores.isEmpty()) {
            return "<yellow><bold> 队伍得分：</bold></yellow>暂无数据";
        }
        
        Team playerTeam = MCETeamUtils.getTeam(player);
        if (playerTeam == null) {
            return "<yellow><bold> 队伍得分：</bold></yellow>未分队";
        }
        
        String teamName = MCETeamUtils.getUncoloredTeamName(playerTeam);
        int teamScore = teamScores.getOrDefault(teamName, 0);
        
        // 计算总分
        int totalScore = teamScores.values().stream().mapToInt(Integer::intValue).sum();
        
        // 计算占比
        double percentage = totalScore > 0 ? (double) teamScore / totalScore * 100 : 0;
        
        return "<yellow><bold> 队伍得分：</bold></yellow>" +
               "<aqua>" + teamName + "</aqua><white>(" + teamScore + "分, " + 
               String.format("%.1f", percentage) + "%)</white>";
    }
}