package mcevent.MCEFramework.games.football;

import lombok.Getter;
import lombok.Setter;
import mcevent.MCEFramework.games.football.customHandler.BallTrackingHandler;
import mcevent.MCEFramework.games.football.customHandler.BallBounceHandler;
import mcevent.MCEFramework.games.football.customHandler.KnockbackCooldownHandler;
import mcevent.MCEFramework.games.football.gameObject.FootballGameBoard;
import mcevent.MCEFramework.generalGameObject.MCEGame;
import mcevent.MCEFramework.tools.*;
import org.bukkit.*;
import org.bukkit.entity.Armadillo;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static mcevent.MCEFramework.games.football.FootballFuncImpl.*;
import static mcevent.MCEFramework.miscellaneous.Constants.*;
import static mcevent.MCEFramework.tools.MCEPlayerUtils.grantGlobalPotionEffect;

/*
Football: 足球游戏的完整实现
红蓝两队对抗，将犰狳推进对方球门
*/
@Getter @Setter
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
        new Location(null, 1, -57, 0),
        new Location(null, -3, -57, 8), 
        new Location(null, 1, -57, 16)
    };
    private Location[] redSpawns = {
        new Location(null, 15, -57, 0),
        new Location(null, 19, -57, 8),
        new Location(null, 15, -57, 16)
    };
    
    // 球门位置 - 设置为更宽更深的检测区域
    private Location redGoalMin = new Location(null, 34, -59, 3);
    private Location redGoalMax = new Location(null, 38, -52, 13);
    private Location blueGoalMin = new Location(null, -23, -59, 3);
    private Location blueGoalMax = new Location(null, -17, -52, 13);
    
    // 球的初始位置
    private Location ballSpawn = new Location(null, 8, -57, 8);

    public Football(String title, int id, String mapName, int round, boolean isMultiGame, String configFileName,
                    int launchDuration, int introDuration, int preparationDuration, int cyclePreparationDuration, 
                    int cycleStartDuration, int cycleEndDuration, int endDuration) {
        super(title, id, mapName, round, isMultiGame, configFileName,
                launchDuration, introDuration, preparationDuration, cyclePreparationDuration, 
                Integer.MAX_VALUE, cycleEndDuration, endDuration); // 设置无限游戏时间
    }

    @Override
    public void onLaunch() {
        loadConfig();
        World world = Bukkit.getWorld(this.getWorldName());
        if (world != null) {
            world.setGameRule(GameRule.FALL_DAMAGE, false);
            world.setGameRule(GameRule.DO_MOB_SPAWNING, false);
            world.setGameRule(GameRule.KEEP_INVENTORY, true);
        }
        
        // 更新出生点世界
        updateSpawnWorlds(world);
        
        // 检查队伍分配
        ensureTwoTeamsSplit();
        
        MCETeleporter.globalSwapWorld(this.getWorldName());
        MCEWorldUtils.disablePVP(); // 禁用全局PVP
        MCEPlayerUtils.globalSetGameMode(GameMode.ADVENTURE);
        
        this.getGameBoard().setStateTitle("<red><bold> 游戏开始：</bold></red>");
        
        grantGlobalPotionEffect(saturation);
        MCEPlayerUtils.clearGlobalTags();
    }

    @Override
    public void onPreparation() {
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
        
        this.getGameBoard().setStateTitle("<yellow><bold> 准备阶段：</bold></yellow>");
    }

    @Override
    public void onCyclePreparation() {
        // 在cycle_preparation阶段倒计时，这个阶段设置为5秒
        this.getGameBoard().setStateTitle("<yellow><bold> 比赛即将开始：</bold></yellow>");
    }
    
    @Override
    public void onCycleStart() {
        resetGameBoard();
        this.getGameBoard().setStateTitle("<green><bold> 比赛进行中</bold></green>");

        // 播放背景音乐
        MCEPlayerUtils.globalPlaySound("minecraft:football_theme");
        
        // cycle_preparation阶段结束后立即允许行动并发送开始消息
        removeMovementRestrictions();
        MCEMessenger.sendGlobalInfo("<green>比赛开始！</green>");
    }

    @Override
    public void onEnd() {
        // 停止背景音乐
        MCEPlayerUtils.globalStopMusic();
        
        ballTrackingHandler.suspend();
        ballBounceHandler.suspend();
        knockbackCooldownHandler.suspend();
        
        // 移除球
        if (ball != null && !ball.isDead()) {
            ball.remove();
        }
        
        sendWinningMessage();
        MCEPlayerUtils.globalSetGameMode(GameMode.SPECTATOR);
    }

    @Override
    public void initGameBoard() {
        setGameBoard(new FootballGameBoard(getTitle(), getWorldName()));
    }

    @Override
    public void stop() {
        super.stop();
        
        // 停止背景音乐
        MCEPlayerUtils.globalStopMusic();
        
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
        
        // 检查是否获胜
        if (redScore >= maxScore || blueScore >= maxScore) {
            this.getTimeline().nextState(); // 跳转到结束阶段
        } else {
            // 3秒后重新开始
            new BukkitRunnable() {
                @Override
                public void run() {
                    resetRound();
                }
            }.runTaskLater(plugin, 60L); // 3秒 = 60 ticks
        }
    }
    
    // 重置回合
    private void resetRound() {
        // 设置准备状态
        this.getGameBoard().setStateTitle("<yellow><bold> 准备下一回合：</bold></yellow>");
        
        teleportPlayersToSpawns();
        spawnBall();
        applyPlayerEffects();
        
        // 5秒后允许行动并切换到比赛状态
        new BukkitRunnable() {
            @Override
            public void run() {
                removeMovementRestrictions();
                // 重新设置为比赛进行中状态，这样时间就会隐藏
                getGameBoard().setStateTitle("<green><bold> 比赛进行中</bold></green>");
                MCEMessenger.sendGlobalInfo("<green>新回合开始！</green>");
            }
        }.runTaskLater(plugin, 100L);
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
}