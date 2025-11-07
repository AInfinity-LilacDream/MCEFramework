package mcevent.MCEFramework.games.sandRun;

import lombok.Getter;
import lombok.Setter;
import mcevent.MCEFramework.MCEMainController;
import mcevent.MCEFramework.games.sandRun.customHandler.SandFallHandler;
import mcevent.MCEFramework.games.sandRun.gameObject.SandRunGameBoard;
import mcevent.MCEFramework.generalGameObject.MCEGame;
import mcevent.MCEFramework.generalGameObject.MCEGameQuitHandler;
import mcevent.MCEFramework.tools.*;
import org.bukkit.scoreboard.Team;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

import static mcevent.MCEFramework.miscellaneous.Constants.*;
import static mcevent.MCEFramework.tools.MCEPlayerUtils.grantGlobalPotionEffect;

/*
SandRun: Sand run 完整实现
 */
@Getter
@Setter
public class SandRun extends MCEGame {

    private SandFallHandler sandFallHandler = new SandFallHandler();
    private List<BukkitRunnable> gameTask = new ArrayList<>();
    private SandRunConfigParser sandRunConfigParser = new SandRunConfigParser();
    
    // 游戏状态追踪
    private List<String> deathOrder = new ArrayList<>();
    private List<Team> teamEliminationOrder = new ArrayList<>();

    public SandRun(String title, int id, String mapName, int round, boolean isMultiGame, String configFileName,
            int launchDuration, int introDuration, int preparationDuration, int cyclePreparationDuration,
            int cycleStartDuration, int cycleEndDuration, int endDuration) {
        super(title, id, mapName, round, isMultiGame, configFileName,
                launchDuration, introDuration, preparationDuration, cyclePreparationDuration, cycleStartDuration,
                cycleEndDuration, endDuration);
    }

    @Override
    public void onLaunch() {
        SandRunFuncImpl.loadConfig();
        MCEPlayerUtils.globalClearPotionEffects();

        World world = Bukkit.getWorld(this.getWorldName());
        if (world != null) {
            world.setGameRule(GameRule.FALL_DAMAGE, false);
            world.setGameRule(GameRule.KEEP_INVENTORY, false);
        }

        // 清理世界中的混凝土粉末
        SandRunFuncImpl.clearConcretePowder();

        setActiveTeams(MCETeamUtils.getActiveTeams());
        MCETeleporter.globalSwapWorld(this.getWorldName());
        MCEWorldUtils.disablePVP();
        MCEPlayerUtils.globalSetGameModeDelayed(GameMode.SURVIVAL, 5L);

        // 使用 SandRunFuncImpl 代替直接循环操作玩家
        SandRunFuncImpl.initializePlayers();

        this.getGameBoard().setStateTitle("<red><bold> 游戏开始：</bold></red>");
        grantGlobalPotionEffect(saturation);
        MCEPlayerUtils.clearGlobalTags();
    }

    @Override
    public void handlePlayerQuitDuringGame(org.bukkit.entity.Player player) {
        // 使用统一的退出处理逻辑
        String playerName = player.getName();
        Team playerTeam = MCETeamUtils.getTeam(player);
        
        MCEGameQuitHandler.handlePlayerQuit(this, player, () -> {
            // 添加到死亡顺序
            if (!deathOrder.contains(playerName)) {
                deathOrder.add(playerName);
            }
            
            // 检查队伍淘汰
            boolean teamEliminated = MCEGameQuitHandler.checkTeamElimination(playerName, playerTeam, teamEliminationOrder);
            
            // 更新游戏板（类似 updateGameBoardOnPlayerDeath 的逻辑）
            SandRunGameBoard gameBoard = (SandRunGameBoard) getGameBoard();
            if (playerTeam != null) {
                gameBoard.updateTeamRemainTitle(playerTeam);
                
                // 如果队伍被团灭，发送消息（checkTeamElimination 已经发送了消息，这里只是确保游戏板更新）
                int teamId = getTeamId(playerTeam);
                if (teamId >= 0 && teamId < gameBoard.getTeamRemain().length &&
                        gameBoard.getTeamRemain()[teamId] == 0 && teamEliminated) {
                    // 消息已经在 checkTeamElimination 中发送，这里只需要确保游戏板已更新
                }
            }
            
            // 更新剩余玩家数
            int alivePlayerCount = 0;
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getWorld().getName().equals(this.getWorldName()) &&
                        p.getScoreboardTags().contains("Active") &&
                        !p.getScoreboardTags().contains("dead") &&
                        p.getGameMode() == GameMode.SURVIVAL) {
                    alivePlayerCount++;
                }
            }
            gameBoard.updatePlayerRemainTitle(alivePlayerCount);
            
            // 检查游戏结束条件：当所有玩家都退出或死亡时，提前结束游戏
            if (alivePlayerCount == 0) {
                // 停止落沙任务
                sandFallHandler.stopSandFall();
                // 清理游戏任务
                SandRunFuncImpl.clearGameTasks(this);
                // 跳转到时间线下一阶段（onEnd）
                if (this.getTimeline() != null) {
                    this.getTimeline().nextState();
                }
            }
        });
    }

    @Override
    public void onCycleStart() {
        SandRunFuncImpl.resetGameBoard();
        this.getGameBoard().setStateTitle("<yellow><bold> 游戏进行中：</bold></yellow>");

        // 播放背景音乐
        MCEPlayerUtils.globalPlaySound("minecraft:sand_run");

        MCEWorldUtils.enablePVP();

        sandFallHandler.startSandFall();

        gameTask.add(MCETimerUtils.setDelayedTask(180, () -> this.getTimeline().nextState()));
    }

    @Override
    public void onEnd() {
        SandRunFuncImpl.sendWinningMessage();
        // 不在结束阶段修改玩家游戏模式

        // 进入结束阶段：设置标题并停止音乐与所有定时任务（保留返回主城的延时）
        MCEPlayerUtils.globalStopMusic();
        // 清理仅与玩法相关的自建任务，避免在结束阶段被意外触发 nextState
        SandRunFuncImpl.clearGameTasks(this);
        // 停止落沙任务
        sandFallHandler.stopSandFall();

        // 启动结束倒计时
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
                    SandRun.this.stop();
                    MCEMainController.returnToLobbyOrLaunchVoting();
                    cancel();
                    return;
                }

                // 更新状态标题显示倒计时
                int minutes = remainingSeconds / 60;
                int seconds = remainingSeconds % 60;
                String timeDisplay = String.format(" %02d:%02d", minutes, seconds);
                SandRunGameBoard gameBoard = (SandRunGameBoard) getGameBoard();
                gameBoard.setStateTitle("<red><bold> 游戏结束：</bold></red>" + timeDisplay);
                gameBoard.globalDisplay(); // 更新显示

                remainingSeconds--;
            }
        }.runTaskTimer(plugin, 0L, 20L); // 每秒更新一次
    }

    @Override
    public void initGameBoard() {
        setGameBoard(new SandRunGameBoard(getTitle(), getWorldName()));
    }

    @Override
    public void stop() {
        super.stop();

        // 停止背景音乐
        MCEPlayerUtils.globalStopMusic();

        SandRunFuncImpl.clearGameTasks(this);
        sandFallHandler.stopSandFall();
    }
}