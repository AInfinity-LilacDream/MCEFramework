package mcevent.MCEFramework.games.football.gameObject;

import fr.mrmicky.fastboard.adventure.FastBoard;
import lombok.Getter;
import lombok.Setter;
import mcevent.MCEFramework.generalGameObject.MCEGameBoard;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import static mcevent.MCEFramework.miscellaneous.Constants.*;

/*
FootballGameBoard: 足球游戏展示板
显示比分和队伍信息，不显示时间
*/
@Setter @Getter
public class FootballGameBoard extends MCEGameBoard {
    private int redScore = 0;
    private int blueScore = 0;
    private int redTeamCount = 0;
    private int blueTeamCount = 0;
    private String scoreTitle = "";
    private String teamCountTitle = "";
    private String roundTitle = "";

    public FootballGameBoard(String gameName, String mapName) {
        super(gameName, mapName);
    }

    public void updateScores(int redScore, int blueScore) {
        this.redScore = redScore;
        this.blueScore = blueScore;
        this.scoreTitle = "<red><bold>红队：</bold></red>" + redScore + 
                         " <gray>-</gray> " +
                         "<blue><bold>蓝队：</bold></blue>" + blueScore;
    }
    
    public void updateTeamCounts(int redCount, int blueCount) {
        this.redTeamCount = redCount;
        this.blueTeamCount = blueCount;
        this.teamCountTitle = "<red>红队人数：</red>" + redCount + 
                             " <gray>|</gray> " +
                             "<blue>蓝队人数：</blue>" + blueCount;
    }
    
    public void updateRoundTitle(int currentRound) {
        this.roundTitle = "<gold><bold>第" + currentRound + "局</bold></gold>";
    }

    @Override
    public void globalDisplay() {
        int seconds = football.getTimeline().getCounter();
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            FastBoard board = new FastBoard(player);
            board.updateTitle(MiniMessage.miniMessage().deserialize(getMainTitle()));
            
            // 检查当前是否需要隐藏时间
            // 比赛进行中不显示时间，preparation和end阶段显示时间
            // 如果状态标题已经包含时间（如游戏结束倒计时），则不再添加timeline时间
            String timeDisplay = "";
            String currentStateTitle = getStateTitle();
            if (currentStateTitle != null && !currentStateTitle.contains("比赛进行中") 
                && !currentStateTitle.contains("游戏结束：")) {
                int minute = seconds / 60;
                int second = seconds % 60;
                timeDisplay = String.format(" %02d:%02d", minute, second);
            }
            
            board.updateLines(
                    MiniMessage.miniMessage().deserialize(" "),
                    MiniMessage.miniMessage().deserialize(getGameTitle()),
                    MiniMessage.miniMessage().deserialize(getMapTitle()),
                    MiniMessage.miniMessage().deserialize(getRoundTitle()),
                    MiniMessage.miniMessage().deserialize(getStateTitle() + timeDisplay),
                    MiniMessage.miniMessage().deserialize(" "),
                    MiniMessage.miniMessage().deserialize(getScoreTitle()),
                    MiniMessage.miniMessage().deserialize(getTeamCountTitle()),
                    MiniMessage.miniMessage().deserialize(" ")
            );
        }
    }
}