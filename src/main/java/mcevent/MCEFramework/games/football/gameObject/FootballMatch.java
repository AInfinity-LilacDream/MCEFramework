package mcevent.MCEFramework.games.football.gameObject;

import mcevent.MCEFramework.games.football.Football;
import mcevent.MCEFramework.games.football.customHandler.BallBounceHandler;
import mcevent.MCEFramework.games.football.customHandler.BallTrackingHandler;
import mcevent.MCEFramework.tools.MCEMessenger;
import mcevent.MCEFramework.tools.MCETeamUtils;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Armadillo;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;

/**
 * FootballMatch: 单场对局（两支队伍）
 * 该类封装了对局的独立状态、球体与处理器，确保四队模式下两场完全解耦。
 */
public class FootballMatch {

    private final Football parent; // 所属的 Football 游戏
    private final boolean secondField; // 是否为第二球场（青/黄）

    // 计分与回合
    private int scoreA = 0; // A队得分（红/青）
    private int scoreB = 0; // B队得分（蓝/黄）
    private int round = 1;
    private int requiredScore = 3;
    private boolean finished = false;

    // 场地要素
    private Armadillo ball;
    private final BallTrackingHandler trackingHandler = new BallTrackingHandler();
    private final BallBounceHandler bounceHandler = new BallBounceHandler();

    // 目标区域（球门）
    private Location goalMinA; // A队对应对方球门（例如红队要把球推进蓝门）
    private Location goalMaxA;
    private Location goalMinB;
    private Location goalMaxB;

    // 边界框（用于反弹）
    private double minX, maxX, minZ, maxZ, minY, maxY;

    public FootballMatch(Football parent,
            boolean secondField,
            int requiredScore,
            Location goalMinA, Location goalMaxA,
            Location goalMinB, Location goalMaxB,
            double minX, double maxX, double minZ, double maxZ, double minY, double maxY) {
        this.parent = parent;
        this.secondField = secondField;
        this.requiredScore = Math.max(3, requiredScore);
        this.goalMinA = goalMinA;
        this.goalMaxA = goalMaxA;
        this.goalMinB = goalMinB;
        this.goalMaxB = goalMaxB;
        this.minX = minX;
        this.maxX = maxX;
        this.minZ = minZ;
        this.maxZ = maxZ;
        this.minY = minY;
        this.maxY = maxY;
    }

    public void prepareRound() {
        if (finished)
            return;
        // 确保重启前停止上一回合追踪与反弹任务
        trackingHandler.suspend();
        bounceHandler.suspend();
        if (secondField) {
            mcevent.MCEFramework.games.football.FootballFuncImpl.teleportCYPlayersToSpawns();
            mcevent.MCEFramework.games.football.FootballFuncImpl.spawnBall2();
            this.ball = parent.getBall2();
            trackingHandler.startSecond(this);
            bounceHandler.startSecond(parent, minX, maxX, minZ, maxZ, minY, maxY);
            mcevent.MCEFramework.games.football.FootballFuncImpl.applyPlayerEffectsCY();
        } else {
            mcevent.MCEFramework.games.football.FootballFuncImpl.teleportRBPlayersToSpawns();
            mcevent.MCEFramework.games.football.FootballFuncImpl.spawnBall();
            this.ball = parent.getBall();
            trackingHandler.start(this);
            bounceHandler.start(parent);
            mcevent.MCEFramework.games.football.FootballFuncImpl.applyPlayerEffectsRB();
        }
    }

    public void startRound() {
        if (finished)
            return;
        if (secondField) {
            mcevent.MCEFramework.games.football.FootballFuncImpl.removeMovementRestrictionsCY();
            FootballGameBoard gb = (FootballGameBoard) parent.getGameBoard();
            gb.setStateTitleCY("<green><bold> 比赛进行中</bold></green>");
            Team cyanT = Bukkit.getScoreboardManager().getMainScoreboard()
                    .getTeam(mcevent.MCEFramework.miscellaneous.Constants.teams[5].teamName());
            Team yellowT = Bukkit.getScoreboardManager().getMainScoreboard()
                    .getTeam(mcevent.MCEFramework.miscellaneous.Constants.teams[2].teamName());
            if (cyanT != null)
                MCEMessenger.sendInfoToTeam(cyanT, "<green>第" + round + "局开始！</green>");
            if (yellowT != null)
                MCEMessenger.sendInfoToTeam(yellowT, "<green>第" + round + "局开始！</green>");
        } else {
            mcevent.MCEFramework.games.football.FootballFuncImpl.removeMovementRestrictionsRB();
            FootballGameBoard gb = (FootballGameBoard) parent.getGameBoard();
            gb.setStateTitleRB("<green><bold> 比赛进行中</bold></green>");
            Team redT = Bukkit.getScoreboardManager().getMainScoreboard()
                    .getTeam(mcevent.MCEFramework.miscellaneous.Constants.teams[0].teamName());
            Team blueT = Bukkit.getScoreboardManager().getMainScoreboard()
                    .getTeam(mcevent.MCEFramework.miscellaneous.Constants.teams[7].teamName());
            if (redT != null)
                MCEMessenger.sendInfoToTeam(redT, "<green>第" + round + "局开始！</green>");
            if (blueT != null)
                MCEMessenger.sendInfoToTeam(blueT, "<green>第" + round + "局开始！</green>");
        }
    }

    // 供处理器回调：A方将球推进 B方球门
    public void onGoalByA() {
        if (finished)
            return;
        scoreA++;
        // 同步父对象比分与展示板
        if (secondField)
            parent.setCyanScore(scoreA);
        else
            parent.setRedScore(scoreA);
        if (secondField) {
            MCEMessenger.sendGlobalInfo("<dark_aqua>青队进球！当前比分 青队 " + scoreA + " : " + scoreB + " 黄队</dark_aqua>");
        } else {
            MCEMessenger.sendGlobalInfo("<red>红队进球！当前比分 红队 " + scoreA + " : " + scoreB + " 蓝队</red>");
        }
        mcevent.MCEFramework.games.football.FootballFuncImpl.updateScoreboard();
        removeBallAndPause();
        if (scoreA >= requiredScore) {
            finishMatch(true);
        } else {
            nextRoundDelayed();
        }
    }

    // 供处理器回调：B方将球推进 A方球门
    public void onGoalByB() {
        if (finished)
            return;
        scoreB++;
        if (secondField)
            parent.setYellowScore(scoreB);
        else
            parent.setBlueScore(scoreB);
        if (secondField) {
            MCEMessenger.sendGlobalInfo("<yellow>黄队进球！当前比分 青队 " + scoreA + " : " + scoreB + " 黄队</yellow>");
        } else {
            MCEMessenger.sendGlobalInfo("<blue>蓝队进球！当前比分 红队 " + scoreA + " : " + scoreB + " 蓝队</blue>");
        }
        mcevent.MCEFramework.games.football.FootballFuncImpl.updateScoreboard();
        removeBallAndPause();
        if (scoreB >= requiredScore) {
            finishMatch(false);
        } else {
            nextRoundDelayed();
        }
    }

    private void removeBallAndPause() {
        if (ball != null && !ball.isDead())
            ball.remove();
        trackingHandler.suspend();
        bounceHandler.suspend();
        ball = null;
    }

    private void nextRoundDelayed() {
        round++;
        if (secondField)
            parent.setRoundCY(round);
        else
            parent.setRoundRB(round);
        // 更新展示板为“第X局准备”
        FootballGameBoard gb = (FootballGameBoard) parent.getGameBoard();
        if (secondField) {
            gb.updateRoundTitleCY(round);
            gb.setStateTitleCY("<yellow><bold> 第" + round + "局准备</bold></yellow>");
            mcevent.MCEFramework.games.football.FootballFuncImpl.teleportCYPlayersToSpawns();
        } else {
            gb.updateRoundTitleRB(round);
            gb.setStateTitleRB("<yellow><bold> 第" + round + "局准备</bold></yellow>");
            mcevent.MCEFramework.games.football.FootballFuncImpl.teleportRBPlayersToSpawns();
        }
        prepareRound();
        mcevent.MCEFramework.miscellaneous.Constants.plugin.getLogger()
                .info("[Schedule] next round in 3s, CY=" + secondField + " round=" + round);
        parent.setDelayedTask(3, () -> {
            startRound();
        });
    }

    private void finishMatch(boolean aWin) {
        finished = true;
        if (secondField)
            parent.setCyFinished(true);
        else
            parent.setRbFinished(true);
        Team teamA;
        Team teamB;
        if (secondField) {
            teamA = Bukkit.getScoreboardManager().getMainScoreboard()
                    .getTeam(mcevent.MCEFramework.miscellaneous.Constants.teams[5].teamName());
            teamB = Bukkit.getScoreboardManager().getMainScoreboard()
                    .getTeam(mcevent.MCEFramework.miscellaneous.Constants.teams[2].teamName());
        } else {
            teamA = Bukkit.getScoreboardManager().getMainScoreboard()
                    .getTeam(mcevent.MCEFramework.miscellaneous.Constants.teams[0].teamName());
            teamB = Bukkit.getScoreboardManager().getMainScoreboard()
                    .getTeam(mcevent.MCEFramework.miscellaneous.Constants.teams[7].teamName());
        }

        String msg = aWin
                ? (secondField ? "<dark_aqua><bold>青队获胜！</bold></dark_aqua> 最终比分：青队 " + scoreA + " : " + scoreB + " 黄队"
                        : "<red><bold>红队获胜！</bold></red> 最终比分：红队 " + scoreA + " : " + scoreB + " 蓝队")
                : (secondField ? "<yellow><bold>黄队获胜！</bold></yellow> 最终比分：青队 " + scoreA + " : " + scoreB + " 黄队"
                        : "<blue><bold>蓝队获胜！</bold></blue> 最终比分：红队 " + scoreA + " : " + scoreB + " 蓝队");
        if (teamA != null)
            MCEMessenger.sendInfoToTeam(teamA, msg);
        if (teamB != null)
            MCEMessenger.sendInfoToTeam(teamB, msg);

        // 本场玩家切旁观
        for (Player p : Bukkit.getOnlinePlayers()) {
            Team t = MCETeamUtils.getTeam(p);
            if (t == null)
                continue;
            boolean inThisMatch = (teamA != null && t.equals(teamA)) || (teamB != null && t.equals(teamB));
            if (inThisMatch)
                p.setGameMode(GameMode.SPECTATOR);
        }

        // 标记本场胜负播报已完成（避免重复）
        if (secondField)
            parent.setCyAnnounced(true);
        else
            parent.setRbAnnounced(true);

        // 通知父类：若另一场也结束则进入总结束流程
        parent.onSubMatchFinished();
    }

    // === 供处理器访问的只读数据 ===
    public Armadillo getBallEntity() {
        return ball;
    }

    public Location getGoalMinA() {
        return goalMinA;
    }

    public Location getGoalMaxA() {
        return goalMaxA;
    }

    public Location getGoalMinB() {
        return goalMinB;
    }

    public Location getGoalMaxB() {
        return goalMaxB;
    }

    public boolean isSecondField() {
        return secondField;
    }

    public int getRound() {
        return round;
    }

    public int getScoreA() {
        return scoreA;
    }

    public int getScoreB() {
        return scoreB;
    }

    public boolean isFinished() {
        return finished;
    }
}
