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
@Setter
@Getter
public class FootballGameBoard extends MCEGameBoard {
    private int redScore = 0;
    private int blueScore = 0;
    private int cyanScore = 0;
    private int yellowScore = 0;
    private int redTeamCount = 0;
    private int blueTeamCount = 0;
    private String scoreTitle = "";
    private String scoreTitleSecond = "";
    private String teamCountTitle = "";
    private String teamCountTitleSecond = "";
    private String roundTitle = "";
    private String roundTitleRB = "";
    private String roundTitleCY = "";
    private String stateTitleRB = "";
    private String stateTitleCY = "";

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

    public void updateScoresSecond(int cyan, int yellow) {
        this.cyanScore = cyan;
        this.yellowScore = yellow;
        this.scoreTitleSecond = "<dark_aqua><bold>青：</bold></dark_aqua>" + cyan +
                " <gray>-</gray> " +
                "<yellow><bold>黄：</bold></yellow>" + yellow;
    }

    public void updateTeamCounts(int redCount, int blueCount) {
        this.redTeamCount = redCount;
        this.blueTeamCount = blueCount;
        this.teamCountTitle = "<red>红队人数：</red>" + redCount +
                " <gray>|</gray> " +
                "<blue>蓝队人数：</blue>" + blueCount;
    }

    public void updateTeamCountsSecond(int cyanCount, int yellowCount) {
        this.teamCountTitleSecond = "<dark_aqua>青队人数：</dark_aqua>" + cyanCount +
                " <gray>|</gray> " +
                "<yellow>黄队人数：</yellow>" + yellowCount;
    }

    public void updateRoundTitle(int currentRound) {
        this.roundTitle = "<gold><bold>第" + currentRound + "局</bold></gold>";
    }

    public void updateRoundTitleRB(int currentRound) {
        this.roundTitleRB = "<gold><bold>第" + currentRound + "局</bold></gold>";
    }

    public void updateRoundTitleCY(int currentRound) {
        this.roundTitleCY = "<gold><bold>第" + currentRound + "局</bold></gold>";
    }

    @Override
    public void globalDisplay() {
        int seconds = football.getTimeline().getCounter();

        for (Player player : Bukkit.getOnlinePlayers()) {
            FastBoard board = new FastBoard(player);
            board.updateTitle(MiniMessage.miniMessage().deserialize(getMainTitle()));

            // 判定玩家属于哪一场（红蓝或青黄），并选择对应的状态标题
            boolean isRB = false;
            org.bukkit.scoreboard.Team t = org.bukkit.Bukkit.getScoreboardManager().getMainScoreboard()
                    .getEntryTeam(player.getName());
            if (t != null) {
                String n = t.getName();
                isRB = n.equals(teams[0].teamName()) || n.equals(teams[7].teamName());
            }
            String currentStateTitle = isRB
                    ? (!getStateTitleRB().isEmpty() ? getStateTitleRB() : getStateTitle())
                    : (!getStateTitleCY().isEmpty() ? getStateTitleCY() : getStateTitle());

            // 检查当前是否需要隐藏时间（比赛进行中不显示）
            String timeDisplay = "";
            if (currentStateTitle != null && !currentStateTitle.contains("比赛进行中")
                    && !currentStateTitle.contains("游戏结束") && !currentStateTitle.contains("局准备")) {
                int minute = seconds / 60;
                int second = seconds % 60;
                timeDisplay = String.format(" %02d:%02d", minute, second);
            }
            if (isRB) {
                board.updateLines(
                        MiniMessage.miniMessage().deserialize(" "),
                        MiniMessage.miniMessage().deserialize(getGameTitle()),
                        MiniMessage.miniMessage().deserialize(getMapTitle()),
                        // 按玩家队伍显示对应对局的回合数（红蓝）
                        MiniMessage.miniMessage()
                                .deserialize(getRoundTitleRB().isEmpty() ? getRoundTitle() : getRoundTitleRB()),
                        MiniMessage.miniMessage().deserialize(currentStateTitle + timeDisplay),
                        MiniMessage.miniMessage().deserialize(" "),
                        MiniMessage.miniMessage().deserialize(getScoreTitle()),
                        MiniMessage.miniMessage().deserialize(getTeamCountTitle()),
                        MiniMessage.miniMessage().deserialize(" "));
            } else {
                board.updateLines(
                        MiniMessage.miniMessage().deserialize(" "),
                        MiniMessage.miniMessage().deserialize(getGameTitle()),
                        MiniMessage.miniMessage().deserialize(getMapTitle()),
                        // 青黄对局显示自身回合数
                        MiniMessage.miniMessage()
                                .deserialize(getRoundTitleCY().isEmpty() ? getRoundTitle() : getRoundTitleCY()),
                        MiniMessage.miniMessage().deserialize(currentStateTitle + timeDisplay),
                        MiniMessage.miniMessage().deserialize(" "),
                        MiniMessage.miniMessage().deserialize(getScoreTitleSecond()),
                        MiniMessage.miniMessage().deserialize(getTeamCountTitleSecond()),
                        MiniMessage.miniMessage().deserialize(" "));
            }
        }
    }
}