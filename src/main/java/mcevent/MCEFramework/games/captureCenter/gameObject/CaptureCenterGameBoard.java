package mcevent.MCEFramework.games.captureCenter.gameObject;

import fr.mrmicky.fastboard.adventure.FastBoard;
import lombok.Getter;
import lombok.Setter;
import mcevent.MCEFramework.generalGameObject.MCEGameBoard;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Map;

import static mcevent.MCEFramework.miscellaneous.Constants.*;

@Setter @Getter
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
        this.playerCount = playerCount;
        this.playerCountTitle = "<green><bold> 剩余玩家：</bold></green>" +
                playerCount + "/" + Bukkit.getOnlinePlayers().size();
    }

    public void updateTeamCount(int teamCount) {
        this.teamCount = teamCount;
        this.teamCountTitle = "<blue><bold> 剩余队伍：</bold></blue>" +
                teamCount + "/" + captureCenter.getActiveTeams().size();
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
        int seconds = captureCenter.getTimeline().getCounter();

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
                    MiniMessage.miniMessage().deserialize(getTeamCountTitle()),
                    MiniMessage.miniMessage().deserialize(getScoreRankingTitle())
            );
        }
    }
}