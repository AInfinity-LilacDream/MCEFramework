package mcevent.MCEFramework.games.football;

import lombok.Getter;
import lombok.Setter;
import mcevent.MCEFramework.MCEMainController;
import mcevent.MCEFramework.games.football.customHandler.BallTrackingHandler;
import mcevent.MCEFramework.games.football.customHandler.BallBounceHandler;
import mcevent.MCEFramework.games.football.customHandler.KnockbackCooldownHandler;
import mcevent.MCEFramework.games.football.gameObject.FootballGameBoard;
import mcevent.MCEFramework.games.football.gameObject.FootballMatch;
import mcevent.MCEFramework.generalGameObject.MCEGame;
import mcevent.MCEFramework.tools.*;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Armadillo;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

import static mcevent.MCEFramework.miscellaneous.Constants.*;
import static mcevent.MCEFramework.tools.MCEPlayerUtils.grantGlobalPotionEffect;

/*
Football: 足球游戏的完整实现
红蓝两队对抗，将犰狳推进对方球门
*/
@Getter
@Setter
public class Football extends MCEGame {

    private BallTrackingHandler ballTrackingHandler = new BallTrackingHandler();
    private BallBounceHandler ballBounceHandler = new BallBounceHandler();
    // 第二球场处理器与球
    private BallTrackingHandler ballTrackingHandler2 = new BallTrackingHandler();
    private BallBounceHandler ballBounceHandler2 = new BallBounceHandler();
    private KnockbackCooldownHandler knockbackCooldownHandler = new KnockbackCooldownHandler();
    private FootballConfigParser footballConfigParser = new FootballConfigParser();

    private Armadillo ball;
    private Armadillo ball2;
    private int redScore = 0;
    private int blueScore = 0;
    private int cyanScore = 0;
    private int yellowScore = 0;
    private int maxScore = 3;
    private boolean rbFinished = false; // 红蓝对局是否结束
    private boolean cyFinished = false; // 青黄对局是否结束

    // 记录击球者，以便判定进球的球员、助攻的球员
    private Deque<UUID> hitQueue = new ArrayDeque<>();
    private Deque<UUID> hitQueue2 = new ArrayDeque<>();

    // 分离的对局局数与推进标记（四队模式使用）
    private int roundRB = 1;
    private int roundCY = 1;
    private boolean pendingNextRoundRB = false;
    private boolean pendingNextRoundCY = false;
    private boolean skipCycleEnd = false; // 进球后跳过“第x局结束”阶段
    private boolean notifyStartRB = false; // 本轮开始时是否通知红蓝
    private boolean notifyStartCY = false; // 本轮开始时是否通知青黄
    // 场次结算提示是否已播报
    private boolean rbAnnounced = false;
    private boolean cyAnnounced = false;
    // 全局最终结算是否已播报（防止重复）
    private boolean finalAnnounced = false;
    // 首次准备阶段是否已执行（仅第一次两场同时启动）
    private boolean initialPreparationDone = false;

    // 子对局（四队模式用）：RB为红/蓝，CY为青/黄
    private FootballMatch matchRB;
    private FootballMatch matchCY;

    // 出生点位置
    private Location[] blueSpawns = {
            new Location(null, 1.5, -57, 16.5),
            new Location(null, -2.5, -57, 8.5),
            new Location(null, 1.5, -57, 0.5)
    };
    private Location[] redSpawns = {
            new Location(null, 15.5, -57, 0.5),
            new Location(null, 19.5, -57, 8.5),
            new Location(null, 15.5, -57, 16.5)
    };

    // 第二球场（青/黄）出生点与球门、球初始点
    private Location[] cyanSpawns = {
            new Location(null, 1.5 + 7, -57 + 16, 16.5 + 59),
            new Location(null, -2.5 + 7, -57 + 16, 8.5 + 59),
            new Location(null, 1.5 + 7, -57 + 16, 0.5 + 59)
    };
    private Location[] yellowSpawns = {
            new Location(null, 15.5 + 7, -57 + 16, 0.5 + 59),
            new Location(null, 19.5 + 7, -57 + 16, 8.5 + 59),
            new Location(null, 15.5 + 7, -57 + 16, 16.5 + 59)
    };

    // 球门位置 - 人工划定的精确球门范围
    // 红方球门：X=36到39（向场外延伸），Y=-57到-54，Z=5到11
    private Location redGoalMin = new Location(null, 36, -57, 5);
    private Location redGoalMax = new Location(null, 39, -54, 11);
    // 蓝方球门：X=-20到-23（向场外延伸），Y=-57到-54，Z=5到11
    private Location blueGoalMin = new Location(null, -23, -57, 5);
    private Location blueGoalMax = new Location(null, -20, -54, 11);

    // 球的初始位置
    private Location ballSpawn = new Location(null, 8.5, -57, 8.5);

    // 第二球场（青/黄）球门与球（修正：青队在西侧，目标为东侧黄门；黄队在东侧，目标为西侧青门）
    private Location cyanGoalMin = new Location(null, -23 + 7, -57 + 16, 5 + 59);
    private Location cyanGoalMax = new Location(null, -20 + 7, -54 + 16, 11 + 59);
    private Location yellowGoalMin = new Location(null, 36 + 7, -57 + 16, 5 + 59);
    private Location yellowGoalMax = new Location(null, 39 + 7, -54 + 16, 11 + 59);
    private Location ballSpawn2 = new Location(null, 8.5 + 7, -57 + 16, 8.5 + 59);

    public Location[] getCyanSpawns() {
        return cyanSpawns;
    }

    public Location[] getYellowSpawns() {
        return yellowSpawns;
    }

    public Location getCyanGoalMin() {
        return cyanGoalMin;
    }

    public Location getCyanGoalMax() {
        return cyanGoalMax;
    }

    public Location getYellowGoalMin() {
        return yellowGoalMin;
    }

    public Location getYellowGoalMax() {
        return yellowGoalMax;
    }

    public Location getBallSpawn2() {
        return ballSpawn2;
    }

    // Music looping task
    private BukkitRunnable musicLoopTask;

    public Football(String title, int id, String mapName, int round, boolean isMultiGame, String configFileName,
                    int launchDuration, int introDuration, int preparationDuration, int cyclePreparationDuration,
                    int cycleStartDuration, int cycleEndDuration, int endDuration) {
        super(title, id, mapName, round, isMultiGame, configFileName,
                launchDuration, introDuration, preparationDuration, cyclePreparationDuration,
                cycleStartDuration, cycleEndDuration, endDuration);
    }

    @Override
    public void onLaunch() {
        loadConfig();

        MCEPlayerUtils.globalClearPotionEffects();

        // 重置比分和游戏状态
        redScore = 0;
        blueScore = 0;
        cyanScore = 0;
        yellowScore = 0;
        isJumpingToEnd = false; // 重置结束标记
        rbFinished = false;
        cyFinished = false;
        roundRB = 1;
        roundCY = 1;
        pendingNextRoundRB = false;
        pendingNextRoundCY = false;
        skipCycleEnd = false;
        notifyStartRB = false;
        notifyStartCY = false;
        rbAnnounced = false;
        cyAnnounced = false;
        finalAnnounced = false;
        initialPreparationDone = false;

        World world = Bukkit.getWorld(this.getWorldName());
        if (world != null) {
            world.setGameRule(GameRule.FALL_DAMAGE, false);
            world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
            world.setGameRule(GameRule.KEEP_INVENTORY, true);

            // 清理世界内所有非玩家实体，重复两次以清理掉落物
            cleanupWorldEntities(world);
            cleanupWorldEntities(world);
        }

        // 额外保险：清理内存引用的上一局足球（包含第二球场）
        if (ball != null && !ball.isDead()) {
            ball.remove();
            ball = null;
        }
        if (ball2 != null && !ball2.isDead()) {
            ball2.remove();
            ball2 = null;
        }

        // 更新出生点世界
        updateSpawnWorlds(world);

        // 检查队伍分配（根据设置为2队或4队）
        int teamSetting = mcevent.MCEFramework.games.settings.GameSettingsState.getFootballTeams();
        ensureTwoTeamsSplit();

        MCETeleporter.globalSwapWorld(this.getWorldName());
        MCEWorldUtils.enablePVP(); // 启用全局PVP
        MCEPlayerUtils.globalSetGameModeDelayed(GameMode.SURVIVAL, 5L);

        this.getGameBoard().setStateTitle("<red><bold> 游戏开始：</bold></red>");

        // 设置玩家血量为10颗心（20.0血量）
        for (Player player : Bukkit.getOnlinePlayers()) {
            Objects.requireNonNull(player.getAttribute(Attribute.MAX_HEALTH)).setBaseValue(20.0);
            player.setHealth(20.0);
        }

        // 在游戏开始时启动背景音乐，整场游戏只启动一次
        startBackgroundMusic();

        grantGlobalPotionEffect(saturation);
        MCEPlayerUtils.clearGlobalTags();

        // 初始化四队模式的双子对局对象（仅在四队时创建）
        if (mcevent.MCEFramework.games.settings.GameSettingsState.getFootballTeams() == 4) {
            // 第一球场（红/蓝）：A=红，B=蓝；A进B门
            matchRB = new FootballMatch(
                    this,
                    false,
                    this.maxScore,
                    redGoalMin, redGoalMax,
                    blueGoalMin, blueGoalMax,
                    -19.0, 36.0, -6.0, 23.0, -60.0, -50.0);
            // 第二球场（青/黄）：A=青，B=黄；A进B门；边界整体偏移
            matchCY = new FootballMatch(
                    this,
                    true,
                    this.maxScore,
                    cyanGoalMin, cyanGoalMax,
                    yellowGoalMin, yellowGoalMax,
                    -19.0 + 7.0, 36.0 + 7.0, -6.0 + 59.0, 23.0 + 59.0, -60.0 + 16.0, -50.0 + 16.0);
        } else {
            matchRB = null;
            matchCY = null;
        }
    }

    @Override
    public void onCyclePreparation() {
        // 每局开始前的准备阶段（5秒倒计时）
        // 状态标题不再包含回合号，回合显示由各自对局的回合标题负责
        this.getGameBoard().setStateTitle("<yellow><bold> 准备阶段</bold></yellow>");

        // 再次校验队伍分配，确保在进入比赛前符合设置
        int teamSetting = mcevent.MCEFramework.games.settings.GameSettingsState.getFootballTeams();
        ensureTwoTeamsSplit();

        // 四队模式使用子对局对象驱动；两队模式保持原逻辑
        if (teamSetting == 4) {
            if (!initialPreparationDone) {
                mcevent.MCEFramework.games.football.FootballFuncImpl.removeExistingBalls();
                FootballGameBoard gb = (FootballGameBoard) getGameBoard();
                roundRB = 1;
                roundCY = 1;
                gb.updateRoundTitleRB(roundRB);
                gb.updateRoundTitleCY(roundCY);
                gb.setStateTitleRB("<yellow><bold> 第" + roundRB + "局准备</bold></yellow>");
                gb.setStateTitleCY("<yellow><bold> 第" + roundCY + "局准备</bold></yellow>");
                if (matchRB != null)
                    matchRB.prepareRound();
                if (matchCY != null)
                    matchCY.prepareRound();
                notifyStartRB = true;
                notifyStartCY = true;
                initialPreparationDone = true;
            }
        } else {
            // 两队模式：保持原逻辑
            this.getGameBoard().updateRoundTitle(getCurrentRound());
            teleportPlayersToSpawns();
            FootballFuncImpl.removeExistingBalls();
            spawnBall();
            applyPlayerEffects();
            ballTrackingHandler.start(this);
            ballBounceHandler.start(this);
        }
        knockbackCooldownHandler.start(this);
    }

    @Override
    public void onCycleStart() {
        // 每局正式开始
        this.getGameBoard().setStateTitle("<green><bold> 比赛进行中</bold></green>");
        resetGameBoard();
        // 初始化展示板分数（两场）
        mcevent.MCEFramework.games.football.gameObject.FootballGameBoard gb = (mcevent.MCEFramework.games.football.gameObject.FootballGameBoard) getGameBoard();
        gb.updateScoresSecond(cyanScore, yellowScore);
        if (mcevent.MCEFramework.games.settings.GameSettingsState.getFootballTeams() == 4) {
            // 仅首局开始时调用，由子对局内部推进后续回合
            if (notifyStartRB && matchRB != null) {
                matchRB.startRound();
                gb.setStateTitleRB("<green><bold> 比赛进行中</bold></green>");
            }
            if (notifyStartCY && matchCY != null) {
                matchCY.startRound();
                gb.setStateTitleCY("<green><bold> 比赛进行中</bold></green>");
            }
            gb.updateRoundTitleRB(roundRB);
            gb.updateRoundTitleCY(roundCY);
        } else {
            // 两队模式：解除移动限制
            removeMovementRestrictions();
        }

        // 启用友伤（允许队友间攻击）
        MCETeamUtils.enableFriendlyFire();
    }

    @Override
    public void onCycleEnd() {
        // 四队模式：不做任何全局暂停或清球动作，避免另一场被影响，仅推进时间线
        if (mcevent.MCEFramework.games.settings.GameSettingsState.getFootballTeams() == 4) {
            this.getTimeline().nextState();
            return;
        }

        // 两队模式：跳过“第x局结束”展示，直接推进到下一局准备，并进行必要清理
        ballTrackingHandler.suspend();
        ballBounceHandler.suspend();
        knockbackCooldownHandler.suspend();
        if (ball != null && !ball.isDead()) {
            ball.remove();
        }
        if (redScore >= maxScore || blueScore >= maxScore) {
            jumpToEndPhase();
            return;
        }
        this.setCurrentRound(this.getCurrentRound() + 1);
        this.getTimeline().nextState();
    }

    @Override
    public void onEnd() {
        // 四队模式：若仅一场结束，延长时间线，继续另一场，直到两场都结束
        if (mcevent.MCEFramework.games.settings.GameSettingsState.getFootballTeams() == 4
                && !(rbFinished && cyFinished)) {
            // 动态追加一个新的回合（三个节点）：准备 -> 开始 -> 结束
            mcevent.MCEFramework.generalGameObject.MCETimeline tl = this.getTimeline();
            mcevent.MCEFramework.generalGameObject.MCETimelineNode prep = new mcevent.MCEFramework.generalGameObject.MCETimelineNode(
                    getCyclePreparationDuration(), true, this::onCyclePreparation, tl, this.getGameBoard());
            mcevent.MCEFramework.generalGameObject.MCETimelineNode start = new mcevent.MCEFramework.generalGameObject.MCETimelineNode(
                    getCycleStartDuration(), false, this::onCycleStart, tl, this.getGameBoard());
            mcevent.MCEFramework.generalGameObject.MCETimelineNode end = new mcevent.MCEFramework.generalGameObject.MCETimelineNode(
                    getCycleEndDuration(), true, this::onCycleEnd, tl, this.getGameBoard());
            tl.addTimelineNode(prep);
            tl.addTimelineNode(start);
            tl.addTimelineNode(end);
            // 立即跳转到新增的准备节点，避免进入真正的结束阶段
            tl.nextState();
            return;
        }

        sendWinningMessage();
        // 不在结束阶段修改玩家游戏模式
        // 清空分场状态标题，避免覆盖全局结束标题
        FootballGameBoard gbEnd = (FootballGameBoard) this.getGameBoard();
        gbEnd.setStateTitleRB("");
        gbEnd.setStateTitleCY("");
        // 启动状态栏倒计时刷新
        startEndCountdown();
    }

    @Override
    public void initGameBoard() {
        setGameBoard(new FootballGameBoard(getTitle(), getWorldName()));
    }

    @Override
    public void stop() {
        super.stop();

        // 停止背景音乐循环
        stopBackgroundMusic();

        ballTrackingHandler.suspend();
        ballBounceHandler.suspend();
        ballTrackingHandler2.suspend();
        ballBounceHandler2.suspend();
        knockbackCooldownHandler.suspend();

        // 移除球
        if (ball != null && !ball.isDead()) {
            ball.remove();
        }
        if (ball2 != null && !ball2.isDead()) {
            ball2.remove();
        }
    }

    // 进球处理
    public void onGoal(boolean redTeamScored) {
        // 四队模式：对齐两队逻辑——进球后结束当前局，进入下一局准备
        if (mcevent.MCEFramework.games.settings.GameSettingsState.getFootballTeams() == 4) {
            int requiredScore = Math.max(3, this.maxScore); // 四队模式下至少打到3分
            if (redTeamScored) {
                redScore++;
                String redName = teams[0].teamName();
                String blueName = teams[7].teamName();
                Player lastHit = Bukkit.getPlayer(hitQueue.pop());
                Player assist = Bukkit.getPlayer(hitQueue.pop());
                hitQueue.clear();
                boolean own = blueName.equals(MCETeamUtils.getTeam(lastHit).getName());
                if (own) {
                    MCEMessenger.sendGlobalInfo("<red>乌龙球！进球队员：</red><blue>" + lastHit.getName() + "</blue>");
                } else {
                    if (redName.equals(MCETeamUtils.getTeam(assist).getName()) && lastHit != assist) {
                        MCEMessenger.sendGlobalInfo("<red>红队进球！进球队员：" + lastHit.getName() + "，助攻：" + assist.getName() + "</red>");
                    } else {
                        MCEMessenger.sendGlobalInfo("<red>红队进球！进球队员：" + lastHit.getName() + "</red>");
                    }
                }
                MCEMessenger.sendGlobalInfo("<red>当前比分 红队 " + redScore + " : " + blueScore + " 蓝队</red>");
            } else {
                blueScore++;
                String redName = teams[0].teamName();
                String blueName = teams[7].teamName();
                Player lastHit = Bukkit.getPlayer(hitQueue.pop());
                Player assist = Bukkit.getPlayer(hitQueue.pop());
                hitQueue.clear();
                boolean own = redName.equals(MCETeamUtils.getTeam(lastHit).getName());
                if (own) {
                    MCEMessenger.sendGlobalInfo("<blue>乌龙球！进球队员：</blue><red>" + lastHit.getName() + "</red>");
                } else {
                    if (blueName.equals(MCETeamUtils.getTeam(assist).getName()) && lastHit != assist) {
                        MCEMessenger.sendGlobalInfo("<blue>蓝队进球！进球队员：" + lastHit.getName() + "，助攻：" + assist.getName() + "</blue>");
                    } else {
                        MCEMessenger.sendGlobalInfo("<blue>蓝队进球！进球队员：" + lastHit.getName() + "</blue>");
                    }
                }
                MCEMessenger.sendGlobalInfo("<blue>当前比分 红队 " + redScore + " : " + blueScore + " 蓝队</blue>");
            }
            updateScoreboard();
            // 进球后立刻移除红蓝球并暂停该场处理器，避免3秒等待期间重复判定
            if (ball != null && !ball.isDead()) {
                ball.remove();
            }
            ballTrackingHandler.suspend();
            ballBounceHandler.suspend();
            // 达到胜利分数：结束红蓝对局（不再重开球），等待另一场结束
            if (!rbFinished && (redScore >= requiredScore || blueScore >= requiredScore)) {
                rbFinished = true;
                // 先向红蓝两队发送胜负提示与最终比分，并标记已播报，再根据另一场状态决定是否进入最终阶段
                org.bukkit.scoreboard.Scoreboard sb = org.bukkit.Bukkit.getScoreboardManager().getMainScoreboard();
                org.bukkit.scoreboard.Team redT = sb
                        .getTeam(mcevent.MCEFramework.miscellaneous.Constants.teams[0].teamName());
                org.bukkit.scoreboard.Team blueT = sb
                        .getTeam(mcevent.MCEFramework.miscellaneous.Constants.teams[7].teamName());
                boolean redWin = redScore > blueScore;
                String rbMsg = redWin
                        ? ("<red><bold>红队获胜！</bold></red> 最终比分：红队 " + redScore + " : " + blueScore + " 蓝队")
                        : ("<blue><bold>蓝队获胜！</bold></blue> 最终比分：红队 " + redScore + " : " + blueScore + " 蓝队");
                if (redT != null)
                    MCEMessenger.sendInfoToTeam(redT, rbMsg);
                if (blueT != null)
                    MCEMessenger.sendInfoToTeam(blueT, rbMsg);
                rbAnnounced = true;
                if (cyFinished)
                    jumpToEndPhase();
                // 红蓝对局玩家切换为旁观者，观看另一场
                for (Player p : Bukkit.getOnlinePlayers()) {
                    org.bukkit.scoreboard.Team t = MCETeamUtils.getTeam(p);
                    if (t == null)
                        continue;
                    String n = t.getName();
                    if (n.equals(mcevent.MCEFramework.miscellaneous.Constants.teams[0].teamName())
                            || n.equals(mcevent.MCEFramework.miscellaneous.Constants.teams[7].teamName())) {
                        p.setGameMode(GameMode.SPECTATOR);
                    }
                }
            }
            // 3秒后推进时间线（onCycleEnd 将被快速跳过，直接到下一局准备）
            setDelayedTask(3, () -> this.getTimeline().nextState());
            // 标记仅红蓝对局推进一局
            pendingNextRoundRB = true;
            return;
        }

        // 两队模式：沿用原逻辑
        if (redTeamScored) {
            redScore++;
            String redName = teams[0].teamName();
            String blueName = teams[7].teamName();
            Player lastHit = Bukkit.getPlayer(hitQueue.pop());
            Player assist = Bukkit.getPlayer(hitQueue.pop());
            hitQueue.clear();
            boolean own = blueName.equals(MCETeamUtils.getTeam(lastHit).getName());
            if (own) {
                MCEMessenger.sendGlobalInfo("<red>乌龙球！进球队员：</red><blue>" + lastHit.getName() + "</blue>");
            } else {
                if (redName.equals(MCETeamUtils.getTeam(assist).getName()) && lastHit != assist) {
                    MCEMessenger.sendGlobalInfo("<red>红队进球！进球队员：" + lastHit.getName() + "，助攻：" + assist.getName() + "</red>");
                } else {
                    MCEMessenger.sendGlobalInfo("<red>红队进球！进球队员：" + lastHit.getName() + "</red>");
                }
            }
            MCEMessenger.sendGlobalInfo("<red>当前比分 红队 " + redScore + " : " + blueScore + " 蓝队</red>");
        } else {
            blueScore++;
            String redName = teams[0].teamName();
            String blueName = teams[7].teamName();
            Player lastHit = Bukkit.getPlayer(hitQueue.pop());
            Player assist = Bukkit.getPlayer(hitQueue.pop());
            hitQueue.clear();
            boolean own = redName.equals(MCETeamUtils.getTeam(lastHit).getName());
            if (own) {
                MCEMessenger.sendGlobalInfo("<blue>乌龙球！进球队员：</blue><red>" + lastHit.getName() + "</red>");
            } else {
                if (blueName.equals(MCETeamUtils.getTeam(assist).getName()) && lastHit != assist) {
                    MCEMessenger.sendGlobalInfo("<blue>蓝队进球！进球队员：" + lastHit.getName() + "，助攻：" + assist.getName() + "</blue>");
                } else {
                    MCEMessenger.sendGlobalInfo("<blue>蓝队进球！进球队员：" + lastHit.getName() + "</blue>");
                }
            }
            MCEMessenger.sendGlobalInfo("<blue>当前比分 红队 " + redScore + " : " + blueScore + " 蓝队</blue>");
        }
        updateScoreboard();
        // 两队模式同样：进球后立刻移除红蓝球并暂停处理器，避免重复判定
        if (ball != null && !ball.isDead()) {
            ball.remove();
        }
        ballTrackingHandler.suspend();
        ballBounceHandler.suspend();
        // 两队模式：3秒后推进，onCycleEnd立即跳过并加1回合
        setDelayedTask(3, () -> this.getTimeline().nextState());
    }

    // 第二球场进球（true=青队进球；false=黄队进球）
    public void onGoalSecond(boolean cyanTeamScored) {
        if (cyanTeamScored) {
            cyanScore++;
            String cyanName = teams[5].teamName();
            String yellowName = teams[2].teamName();
            Player lastHit = Bukkit.getPlayer(hitQueue2.pop());
            Player assist = Bukkit.getPlayer(hitQueue2.pop());
            hitQueue2.clear();
            boolean own = yellowName.equals(MCETeamUtils.getTeam(lastHit).getName());
            if (own) {
                MCEMessenger.sendGlobalInfo("<dark_aqua>乌龙球！进球队员：</dark_aqua><yellow>" + lastHit.getName() + "</yellow>");
            } else {
                if (cyanName.equals(MCETeamUtils.getTeam(assist).getName()) && lastHit != assist) {
                    MCEMessenger.sendGlobalInfo("<dark_aqua>青队进球！进球队员：" + lastHit.getName() + "，助攻：" + assist.getName() + "</dark_aqua>");
                } else {
                    MCEMessenger.sendGlobalInfo("<dark_aqua>青队进球！进球队员：" + lastHit.getName() + "</dark_aqua>");
                }
            }
            MCEMessenger.sendGlobalInfo("<dark_aqua>当前比分 青队 " + cyanScore + " : " + yellowScore + " 黄队</dark_aqua>");
        } else {
            yellowScore++;
            String cyanName = teams[5].teamName();
            String yellowName = teams[2].teamName();
            Player lastHit = Bukkit.getPlayer(hitQueue2.pop());
            Player assist = Bukkit.getPlayer(hitQueue2.pop());
            hitQueue2.clear();
            boolean own = cyanName.equals(MCETeamUtils.getTeam(lastHit).getName());
            if (own) {
                MCEMessenger.sendGlobalInfo("<yellow>乌龙球！进球队员：</yellow><dark_aqua>" + lastHit.getName() + "</dark_aqua>");
            } else {
                if (yellowName.equals(MCETeamUtils.getTeam(assist).getName()) && lastHit != assist) {
                    MCEMessenger.sendGlobalInfo("<yellow>黄队进球！进球队员：" + lastHit.getName() + "，助攻：" + assist.getName() + "</yellow>");
                } else {
                    MCEMessenger.sendGlobalInfo("<yellow>黄队进球！进球队员：" + lastHit.getName() + "</yellow>");
                }
            }
            MCEMessenger.sendGlobalInfo("<yellow>当前比分 青队 " + cyanScore + " : " + yellowScore + " 黄队</yellow>");
        }
        mcevent.MCEFramework.games.football.gameObject.FootballGameBoard gb = (mcevent.MCEFramework.games.football.gameObject.FootballGameBoard) getGameBoard();
        gb.updateScoresSecond(cyanScore, yellowScore);
        updateScoreboard();
        // 进球后立刻移除青黄球并暂停该场处理器，避免3秒等待期间重复判定
        if (ball2 != null && !ball2.isDead()) {
            ball2.remove();
        }
        ballTrackingHandler2.suspend();
        ballBounceHandler2.suspend();
        int requiredScoreCY = Math.max(3, this.maxScore);
        if (!cyFinished && (cyanScore >= requiredScoreCY || yellowScore >= requiredScoreCY)) {
            cyFinished = true;
            // 先向青黄两队发送胜负提示与最终比分，并标记已播报，再根据另一场状态决定是否进入最终阶段
            org.bukkit.scoreboard.Scoreboard sb = org.bukkit.Bukkit.getScoreboardManager().getMainScoreboard();
            org.bukkit.scoreboard.Team cyanT = sb
                    .getTeam(mcevent.MCEFramework.miscellaneous.Constants.teams[5].teamName());
            org.bukkit.scoreboard.Team yellowT = sb
                    .getTeam(mcevent.MCEFramework.miscellaneous.Constants.teams[2].teamName());
            boolean cyanWin = cyanScore > yellowScore;
            String cyMsg = cyanWin
                    ? ("<dark_aqua><bold>青队获胜！</bold></dark_aqua> 最终比分：青 " + cyanScore + " : " + yellowScore + " 黄")
                    : ("<yellow><bold>黄队获胜！</bold></yellow> 最终比分：青 " + cyanScore + " : " + yellowScore + " 黄");
            if (cyanT != null)
                MCEMessenger.sendInfoToTeam(cyanT, cyMsg);
            if (yellowT != null)
                MCEMessenger.sendInfoToTeam(yellowT, cyMsg);
            cyAnnounced = true;
            if (rbFinished)
                jumpToEndPhase();
            // 青黄对局玩家切换为旁观者，观看另一场
            for (Player p : Bukkit.getOnlinePlayers()) {
                org.bukkit.scoreboard.Team t = MCETeamUtils.getTeam(p);
                if (t == null)
                    continue;
                String n = t.getName();
                if (n.equals(mcevent.MCEFramework.miscellaneous.Constants.teams[5].teamName())
                        || n.equals(mcevent.MCEFramework.miscellaneous.Constants.teams[2].teamName())) {
                    p.setGameMode(GameMode.SPECTATOR);
                }
            }
            return;
        }
        // 非达成胜分：独立推进青黄下一局（不动用全局时间线）
        pendingNextRoundCY = true;
        roundCY++;
        gb.updateRoundTitleCY(roundCY);
        gb.setStateTitleCY("<yellow><bold> 第" + roundCY + "局准备：</bold></yellow>");
        // 3秒后仅青黄开局
        setDelayedTask(3, () -> {
            mcevent.MCEFramework.games.football.FootballFuncImpl.teleportCYPlayersToSpawns();
            mcevent.MCEFramework.games.football.FootballFuncImpl.spawnBall2();
            ballTrackingHandler2.startSecond(this);
            ballBounceHandler2.startSecond(this, -19.0 + 7.0, 36.0 + 7.0, -6.0 + 59.0, 23.0 + 59.0, -60.0 + 16.0,
                    -50.0 + 16.0);
            mcevent.MCEFramework.games.football.FootballFuncImpl.removeMovementRestrictionsCY();
            org.bukkit.scoreboard.Scoreboard sb2 = org.bukkit.Bukkit.getScoreboardManager().getMainScoreboard();
            org.bukkit.scoreboard.Team cyanT2 = sb2
                    .getTeam(mcevent.MCEFramework.miscellaneous.Constants.teams[5].teamName());
            org.bukkit.scoreboard.Team yellowT2 = sb2
                    .getTeam(mcevent.MCEFramework.miscellaneous.Constants.teams[2].teamName());
            if (cyanT2 != null)
                MCEMessenger.sendInfoToTeam(cyanT2, "<green>第" + roundCY + "局开始！</green>");
            if (yellowT2 != null)
                MCEMessenger.sendInfoToTeam(yellowT2, "<green>第" + roundCY + "局开始！</green>");
            // 恢复青黄状态标题为进行中
            gb.setStateTitleCY("<green><bold> 比赛进行中</bold></green>");
        });
    }

    private void updateSpawnWorlds(World world) {
        for (Location spawn : blueSpawns) {
            spawn.setWorld(world);
        }
        for (Location spawn : redSpawns) {
            spawn.setWorld(world);
        }
        for (Location spawn : cyanSpawns) {
            spawn.setWorld(world);
        }
        for (Location spawn : yellowSpawns) {
            spawn.setWorld(world);
        }
        ballSpawn.setWorld(world);
        ballSpawn2.setWorld(world);
        redGoalMin.setWorld(world);
        redGoalMax.setWorld(world);
        blueGoalMin.setWorld(world);
        blueGoalMax.setWorld(world);
        cyanGoalMin.setWorld(world);
        cyanGoalMax.setWorld(world);
        yellowGoalMin.setWorld(world);
        yellowGoalMax.setWorld(world);
    }

    private void teleportPlayersToSpawns() {
        FootballFuncImpl.teleportPlayersToSpawns();
    }

    private void spawnBall() {
        FootballFuncImpl.spawnBall();
    }

    private void applyPlayerEffects() {
        FootballFuncImpl.applyPlayerEffects();
    }

    private void removeMovementRestrictions() {
        FootballFuncImpl.removeMovementRestrictions();
    }

    private void resetGameBoard() {
        FootballFuncImpl.resetGameBoard();
    }

    private void sendWinningMessage() {
        FootballFuncImpl.sendWinningMessage();
    }

    private void ensureTwoTeamsSplit() {
        FootballFuncImpl.ensureTwoTeamsSplit();
    }

    private void loadConfig() {
        FootballFuncImpl.loadConfig();
        // 从配置解析器中读取最大分数
        this.maxScore = footballConfigParser.getMaxScore();
    }

    private void updateScoreboard() {
        FootballFuncImpl.updateScoreboard();
    }

    // 子对局回调：当任一场结束后检查是否两场都结束
    public synchronized void onSubMatchFinished() {
        if (mcevent.MCEFramework.games.settings.GameSettingsState.getFootballTeams() != 4)
            return;
        if (matchRB != null && matchCY != null && matchRB.isFinished() && matchCY.isFinished()) {
            // 两场都结束，启动终局流程
            if (!finalAnnounced) {
                jumpToEndPhase();
            }
        }
    }

    /**
     * 开始播放循环背景音乐
     */
    private void startBackgroundMusic() {
        // 立即播放音乐
        MCEPlayerUtils.globalPlaySound("minecraft:football");

        // 音乐长度为211秒（3分31秒），设置循环播放
        // 每211秒重新播放一次音乐，直到游戏结束
        musicLoopTask = new BukkitRunnable() {
            @Override
            public void run() {
                // 重新播放音乐
                MCEPlayerUtils.globalPlaySound("minecraft:football");
            }
        };

        // 211秒后开始循环，每211秒重复一次
        musicLoopTask.runTaskTimer(plugin, 211 * 20L, 211 * 20L);
    }

    /**
     * 停止循环背景音乐
     */
    private void stopBackgroundMusic() {
        // 停止当前播放的音乐
        MCEPlayerUtils.globalStopMusic();

        // 取消循环任务
        if (musicLoopTask != null && !musicLoopTask.isCancelled()) {
            musicLoopTask.cancel();
            musicLoopTask = null;
        }
    }

    /**
     * 清理世界内所有非玩家实体
     */
    private void cleanupWorldEntities(World world) {
        int entityCount = 0;
        for (Entity entity : world.getEntities()) {
            if (!(entity instanceof Player)) {
                entity.remove();
                entityCount++;
            }
        }
        if (entityCount > 0) {
            plugin.getLogger().info("清理了 " + entityCount + " 个非玩家实体");
        }
    }

    // 添加标记防止重复跳转
    private boolean isJumpingToEnd = false;

    /**
     * 跳转到游戏结束阶段（提前结束游戏时使用）
     */
    private void jumpToEndPhase() {
        // 防止重复调用
        if (isJumpingToEnd)
            return;
        isJumpingToEnd = true;

        // 当有队伍达到3分时，直接结束游戏
        plugin.getLogger().info("提前结束游戏，当前回合: " + getCurrentRound() + ", 最大回合: " + getRound());

        // 停止timeline执行，防止继续触发后续节点
        this.getTimeline().suspend();

        // 停止当前的处理器
        ballTrackingHandler.suspend();
        ballBounceHandler.suspend();
        knockbackCooldownHandler.suspend();
        stopBackgroundMusic();

        // 移除球
        if (ball != null && !ball.isDead()) {
            ball.remove();
        }

        // 清理GameBoard的回合显示并立即设置游戏结束状态
        FootballGameBoard gameBoard = (FootballGameBoard) this.getGameBoard();
        gameBoard.setRoundTitle(""); // 清空回合显示
        // 清空分场状态，避免“比赛进行中”覆盖全局
        gameBoard.setStateTitleRB("");
        gameBoard.setStateTitleCY("");
        gameBoard.setStateTitle("<red><bold> 游戏结束：</bold></red>");
        gameBoard.globalDisplay(); // 立即更新显示

        // 发送获胜消息（仅一次）
        if (!finalAnnounced) {
            sendWinningMessage();
            finalAnnounced = true;
        }
        // 不在结束阶段修改玩家游戏模式

        // 启动独立的结束倒计时
        startEndCountdown();
    }

    /**
     * 启动游戏结束倒计时
     */
    private void startEndCountdown() {
        new BukkitRunnable() {
            int remainingSeconds = getEndDuration();

            @Override
            public void run() {
                if (remainingSeconds <= 0) {
                    // 倒计时结束：根据全局设置返回主城或启动投票
                    MCEPlayerUtils.globalClearFastBoard();
                    Football.this.stop();
                    MCEMainController.returnToLobbyOrLaunchVoting();
                    cancel();
                    return;
                }

                // 更新状态标题显示倒计时
                int minutes = remainingSeconds / 60;
                int seconds = remainingSeconds % 60;
                String timeDisplay = String.format(" %02d:%02d", minutes, seconds);
                FootballGameBoard gameBoard = (FootballGameBoard) getGameBoard();
                gameBoard.setStateTitle("<red><bold> 游戏结束：</bold></red>" + timeDisplay);
                gameBoard.setRoundTitle(""); // 确保回合显示为空
                gameBoard.globalDisplay(); // 更新显示

                remainingSeconds--;
            }
        }.runTaskTimer(plugin, 0L, 20L); // 每秒更新一次
    }
}