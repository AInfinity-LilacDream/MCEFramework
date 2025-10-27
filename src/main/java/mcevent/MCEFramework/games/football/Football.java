package mcevent.MCEFramework.games.football;

import lombok.Getter;
import lombok.Setter;
import mcevent.MCEFramework.MCEMainController;
import mcevent.MCEFramework.games.football.customHandler.BallTrackingHandler;
import mcevent.MCEFramework.games.football.customHandler.BallBounceHandler;
import mcevent.MCEFramework.games.football.customHandler.KnockbackCooldownHandler;
import mcevent.MCEFramework.games.football.gameObject.FootballGameBoard;
import mcevent.MCEFramework.generalGameObject.MCEGame;
import mcevent.MCEFramework.tools.*;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Armadillo;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Objects;

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
    private KnockbackCooldownHandler knockbackCooldownHandler = new KnockbackCooldownHandler();
    private FootballConfigParser footballConfigParser = new FootballConfigParser();

    private Armadillo ball;
    private int redScore = 0;
    private int blueScore = 0;
    private int maxScore = 3;

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

    // 球门位置 - 人工划定的精确球门范围
    // 红方球门：X=36到39（向场外延伸），Y=-57到-54，Z=5到11
    private Location redGoalMin = new Location(null, 36, -57, 5);
    private Location redGoalMax = new Location(null, 39, -54, 11);
    // 蓝方球门：X=-20到-23（向场外延伸），Y=-57到-54，Z=5到11
    private Location blueGoalMin = new Location(null, -23, -57, 5);
    private Location blueGoalMax = new Location(null, -20, -54, 11);

    // 球的初始位置
    private Location ballSpawn = new Location(null, 8.5, -57, 8.5);

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
        isJumpingToEnd = false; // 重置结束标记

        World world = Bukkit.getWorld(this.getWorldName());
        if (world != null) {
            world.setGameRule(GameRule.FALL_DAMAGE, false);
            world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
            world.setGameRule(GameRule.KEEP_INVENTORY, true);

            // 清理世界内所有非玩家实体，重复两次以清理掉落物
            cleanupWorldEntities(world);
            cleanupWorldEntities(world);
        }

        // 更新出生点世界
        updateSpawnWorlds(world);

        // 检查队伍分配
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
    }

    @Override
    public void onCyclePreparation() {
        // 每局开始前的准备阶段（5秒倒计时）
        this.getGameBoard().setStateTitle("<yellow><bold> 第" + getCurrentRound() + "局准备：</bold></yellow>");
        this.getGameBoard().updateRoundTitle(getCurrentRound());

        // 传送玩家到出生点
        teleportPlayersToSpawns();

        // 生成犰狳（球）
        spawnBall();

        // 给所有玩家添加发光效果和禁止移动
        applyPlayerEffects();

        // 启动球跟踪、反弹和冷却处理器
        ballTrackingHandler.start(this);
        ballBounceHandler.start(this);
        knockbackCooldownHandler.start(this);
    }

    @Override
    public void onCycleStart() {
        // 每局正式开始
        this.getGameBoard().setStateTitle("<green><bold> 比赛进行中</bold></green>");
        resetGameBoard();

        // 启用友伤（允许队友间攻击）
        MCETeamUtils.enableFriendlyFire();

        // 允许玩家行动并发送开始消息
        removeMovementRestrictions();
        MCEMessenger.sendGlobalInfo("<green>第" + getCurrentRound() + "局开始！</green>");
    }

    @Override
    public void onCycleEnd() {
        // 每局结束
        this.getGameBoard().setStateTitle("<yellow><bold> 第" + getCurrentRound() + "局结束：</bold></yellow>");

        // 暂停处理器
        ballTrackingHandler.suspend();
        ballBounceHandler.suspend();
        knockbackCooldownHandler.suspend();

        // 移除球
        if (ball != null && !ball.isDead()) {
            ball.remove();
        }

        // 检查是否有队伍达到胜利条件（3球）
        if (redScore >= maxScore || blueScore >= maxScore) {
            // 提前结束，跳过所有剩余cycle直接到end
            jumpToEndPhase();
        } else {
            // 继续下一局
            this.setCurrentRound(this.getCurrentRound() + 1);
        }
    }

    @Override
    public void onEnd() {
        sendWinningMessage();
        // 不在结束阶段修改玩家游戏模式

        // 设置游戏结束状态标题，显示倒计时
        this.getGameBoard().setStateTitle("<red><bold> 游戏结束：</bold></red>");

        // onEnd结束后立即清理展示板和资源，然后启动投票系统
        setDelayedTask(getEndDuration(), () -> {
            MCEPlayerUtils.globalClearFastBoard();
            this.stop(); // 停止所有游戏资源
            MCEMainController.launchVotingSystem(); // 立即启动投票系统
        });
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
        knockbackCooldownHandler.suspend();

        // 移除球
        if (ball != null && !ball.isDead()) {
            ball.remove();
        }
    }

    // 进球处理
    public void onGoal(boolean redTeamScored) {
        if (redTeamScored) {
            redScore++;
            MCEMessenger.sendGlobalInfo("<red>红队进球！当前比分 红队 " + redScore + " : " + blueScore + " 蓝队</red>");
        } else {
            blueScore++;
            MCEMessenger.sendGlobalInfo("<blue>蓝队进球！当前比分 红队 " + redScore + " : " + blueScore + " 蓝队</blue>");
        }

        // 立即更新计分板分数
        updateScoreboard();

        // 3秒后结束当前cycle
        setDelayedTask(3, () -> {
            this.getTimeline().nextState(); // 跳转到cycleEnd
        });
    }

    private void updateSpawnWorlds(World world) {
        for (Location spawn : blueSpawns) {
            spawn.setWorld(world);
        }
        for (Location spawn : redSpawns) {
            spawn.setWorld(world);
        }
        ballSpawn.setWorld(world);
        redGoalMin.setWorld(world);
        redGoalMax.setWorld(world);
        blueGoalMin.setWorld(world);
        blueGoalMax.setWorld(world);
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
        gameBoard.setStateTitle("<red><bold> 游戏结束：</bold></red>");
        gameBoard.globalDisplay(); // 立即更新显示

        // 发送获胜消息和设置观察者模式
        sendWinningMessage();
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
                    // 倒计时结束，启动投票系统
                    MCEPlayerUtils.globalClearFastBoard();
                    Football.this.stop();
                    MCEMainController.launchVotingSystem();
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