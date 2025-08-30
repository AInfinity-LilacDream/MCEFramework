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

    @Override
    public void globalDisplay() {
        int seconds = football.getTimeline().getCounter();
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            FastBoard board = new FastBoard(player);
            board.updateTitle(MiniMessage.miniMessage().deserialize(getMainTitle()));
            
            // 检查当前是否需要隐藏时间
            // 比赛进行中和准备下一回合都不显示时间
            String timeDisplay = "";
            String currentStateTitle = getStateTitle();
            if (!currentStateTitle.contains("比赛进行中") && !currentStateTitle.contains("准备下一回合")) {
                int minute = seconds / 60;
                int second = seconds % 60;
                timeDisplay = String.format("%02d:%02d", minute, second);
            }
            
            board.updateLines(
                    MiniMessage.miniMessage().deserialize(" "),
                    MiniMessage.miniMessage().deserialize(getGameTitle()),
                    MiniMessage.miniMessage().deserialize(getMapTitle()),
                    MiniMessage.miniMessage().deserialize(getStateTitle() + timeDisplay),
                    MiniMessage.miniMessage().deserialize(" "),
                    MiniMessage.miniMessage().deserialize(getScoreTitle()),
                    MiniMessage.miniMessage().deserialize(getTeamCountTitle()),
                    MiniMessage.miniMessage().deserialize(" ")
            );
        }
    }
}