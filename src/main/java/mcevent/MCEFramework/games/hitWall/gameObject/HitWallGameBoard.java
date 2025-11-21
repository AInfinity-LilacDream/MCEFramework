package mcevent.MCEFramework.games.hitWall.gameObject;

import fr.mrmicky.fastboard.adventure.FastBoard;
import lombok.Getter;
import lombok.Setter;
import mcevent.MCEFramework.games.hitWall.HitWall;
import mcevent.MCEFramework.games.hitWall.HitWallFuncImpl;
import mcevent.MCEFramework.generalGameObject.MCEGameBoard;
import mcevent.MCEFramework.tools.MCETeamUtils;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;

import java.util.Map;

@Setter
@Getter
public class HitWallGameBoard extends MCEGameBoard {
    private final HitWall game;
    private int playerCount;
    private int teamCount;
    private Map<String, Integer> teamScores;
    private String playerCountTitle = "";
    private String teamCountTitle = "";
    private String scoreRankingTitle = "";
    private boolean showTimer = true;

    public HitWallGameBoard(HitWall game) {
        super(game.getTitle(), game.getWorldName());
        this.game = game;
    }

    public void updatePlayerCount(int playerCount) {
        this.playerCount = playerCount;
        int total = MCEGameBoard.countParticipantsTotal();
        this.playerCountTitle = "<green><bold> 剩余玩家: </bold></green>" +
                playerCount + "/" + Math.max(total, playerCount);
    }

    public void updateTeamCount(int teamCount) {
        this.teamCount = teamCount;
        int totalTeams = MCEGameBoard.countParticipantTeamsTotal();
        this.teamCountTitle = "<blue><bold> 剩余队伍: </bold></blue>" +
                teamCount + "/" + Math.max(totalTeams, teamCount);
    }

    public void updateTeamScores(Map<String, Integer> teamScores) {
        this.teamScores = teamScores;
        updateScoreRanking();
    }

    private void updateScoreRanking() {
        if (teamScores == null || teamScores.isEmpty()) {
            this.scoreRankingTitle = "<yellow><bold> 队伍得分: </bold></yellow>暂无数据";
            return;
        }

        String topTeam = "";
        int maxScore = 0;
        for (Map.Entry<String, Integer> entry : teamScores.entrySet()) {
            if (entry.getValue() > maxScore) {
                maxScore = entry.getValue();
                topTeam = entry.getKey();
            }
        }

        this.scoreRankingTitle = "<yellow><bold> 领先队伍: </bold></yellow>" +
                "<aqua>" + topTeam + "</aqua><white>(" + maxScore + "分)</white>";
    }

    @Override
    public void globalDisplay() {
        updatePlayerCount(MCEGameBoard.countRemainingParticipants());
        updateTeamCount(MCEGameBoard.countRemainingParticipantTeams());
        String stateDisplay;
        if (showTimer) {
            int seconds = game.getTimeline().getCounter();
            int minute = seconds / 60;
            int second = seconds % 60;
            String time = String.format("%02d:%02d", minute, second);
            stateDisplay = getStateTitle() + time;
        } else {
            int remainingWaves = HitWallFuncImpl.getRemainingWaves();
            int totalWaves = HitWallFuncImpl.getTotalWaves();
            String waveInfo = remainingWaves +
                    (totalWaves > 0 ? "/" + totalWaves : "");
            String stateLine = getStateTitle();
            if (stateLine == null) {
                stateLine = "";
            }
            stateDisplay = stateLine.isBlank() ? waveInfo : stateLine + waveInfo;
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            FastBoard board = new FastBoard(player);
            board.updateTitle(MiniMessage.miniMessage().deserialize(getMainTitle()));

            board.updateLines(
                    MiniMessage.miniMessage().deserialize(" "),
                    MiniMessage.miniMessage().deserialize(getGameTitle()),
                    MiniMessage.miniMessage().deserialize(getMapTitle()),
                    MiniMessage.miniMessage().deserialize(stateDisplay),
                    MiniMessage.miniMessage().deserialize(getPlayerCountTitle()),
                    MiniMessage.miniMessage().deserialize(getTeamCountTitle())
            );
        }
    }

    private String getPlayerScoreInfo(Player player) {
        if (teamScores == null || teamScores.isEmpty()) {
            return "<yellow><bold> 队伍得分: </bold></yellow>暂无数据";
        }

        Team playerTeam = MCETeamUtils.getTeam(player);
        if (playerTeam == null) {
            return "<yellow><bold> 队伍得分: </bold></yellow>未分队";
        }

        String teamName = MCETeamUtils.getUncoloredTeamName(playerTeam);
        int teamScore = teamScores.getOrDefault(teamName, 0);

        int totalScore = teamScores.values().stream().mapToInt(Integer::intValue).sum();
        double percentage = totalScore > 0 ? (double) teamScore / totalScore * 100 : 0;

        return "<yellow><bold> 队伍得分: </bold></yellow>" +
                "<aqua>" + teamName + "</aqua><white>(" + teamScore + "分 " +
                String.format("%.1f", percentage) + "%)</white>";
    }
}
