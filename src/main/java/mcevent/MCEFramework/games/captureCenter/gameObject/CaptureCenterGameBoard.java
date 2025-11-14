package mcevent.MCEFramework.games.captureCenter.gameObject;

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

import static mcevent.MCEFramework.miscellaneous.Constants.*;

@Setter
@Getter
public class CaptureCenterGameBoard extends MCEGameBoard {
    private int playerCount;
    private int teamCount;
    private Map<String, Integer> teamScores;
    private String playerCountTitle = "";
    private String teamCountTitle = "";
    private String scoreRankingTitle = "";

    public CaptureCenterGameBoard(String gameName, String mapName) {
        super(gameName, mapName);
    }

    public void updatePlayerCount(int playerCount) {
        int alive = mcevent.MCEFramework.generalGameObject.MCEGameBoard.countRemainingParticipants();
        int total = 0;
        for (org.bukkit.entity.Player p : Bukkit.getOnlinePlayers()) {
            if (p.getWorld().getName().equals(captureCenter.getWorldName())
                    && p.getScoreboardTags().contains("Participant"))
                total++;
        }
        this.playerCount = alive;
        this.playerCountTitle = "<green><bold> 剩余玩家：</bold></green>" + alive + "/" + total;
    }

    public void updateTeamCount(int teamCount) {
        int aliveTeams = mcevent.MCEFramework.generalGameObject.MCEGameBoard.countRemainingParticipantTeams();
        int totalTeams = mcevent.MCEFramework.generalGameObject.MCEGameBoard.countParticipantTeamsTotal();
        this.teamCount = aliveTeams;
        this.teamCountTitle = "<blue><bold> 剩余队伍：</bold></blue>" + aliveTeams + "/" + totalTeams;
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
        String stateTitle = getStateTitle() != null ? getStateTitle() : "";

        // 如果状态标题已经包含倒计时（比如"游戏结束： MM:SS"），就不再添加时间线的时间
        String stateDisplay;
        if (stateTitle.contains("游戏结束") && stateTitle.matches(".*\\d{2}:\\d{2}.*")) {
            // 状态标题已经包含倒计时，直接使用
            stateDisplay = stateTitle;
        } else {
            // 否则添加时间线的时间
            int seconds = captureCenter.getTimeline().getCounter();
            int minute = seconds / 60;
            int second = seconds % 60;
            String time = String.format("%02d:%02d", minute, second);
            stateDisplay = stateTitle + time;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            FastBoard board = new FastBoard(player);
            board.updateTitle(MiniMessage.miniMessage().deserialize(getMainTitle()));

            // 刷新玩家/队伍数
            updatePlayerCount(0);
            updateTeamCount(0);

            board.updateLines(
                    MiniMessage.miniMessage().deserialize(" "),
                    MiniMessage.miniMessage().deserialize(getGameTitle()),
                    MiniMessage.miniMessage().deserialize(getMapTitle()),
                    MiniMessage.miniMessage().deserialize(stateDisplay),
                    MiniMessage.miniMessage().deserialize(getPlayerCountTitle()),
                    MiniMessage.miniMessage().deserialize(getTeamCountTitle()));
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