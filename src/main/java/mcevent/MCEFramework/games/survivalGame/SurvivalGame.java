package mcevent.MCEFramework.games.survivalGame;

import lombok.Getter;
import lombok.Setter;
import mcevent.MCEFramework.MCEMainController;
import mcevent.MCEFramework.games.survivalGame.customHandler.MovementRestrictionHandler;
import mcevent.MCEFramework.games.survivalGame.customHandler.PlayerDeathHandler;
import mcevent.MCEFramework.games.survivalGame.customHandler.PvPControlHandler;
import mcevent.MCEFramework.games.survivalGame.gameObject.SurvivalGameGameBoard;
import mcevent.MCEFramework.generalGameObject.MCEGame;
import mcevent.MCEFramework.tools.*;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static mcevent.MCEFramework.games.survivalGame.SurvivalGameFuncImpl.*;
import static mcevent.MCEFramework.miscellaneous.Constants.*;

/*
SurvivalGame: 饥饿游戏的完整实现
 */
@Getter
@Setter
public class SurvivalGame extends MCEGame {

    private SurvivalGameConfigParser survivalGameConfigParser = new SurvivalGameConfigParser();
    private MovementRestrictionHandler movementRestrictionHandler = new MovementRestrictionHandler();
    private PvPControlHandler pvPControlHandler = new PvPControlHandler();
    private PlayerDeathHandler playerDeathHandler = new PlayerDeathHandler();

    private List<BukkitRunnable> gameTasks = new ArrayList<>();
    private BukkitRunnable musicLoopTask;

    // 游戏配置数据
    private List<Location> spawnPoints = new ArrayList<>();
    private List<Location> chestLocations = new ArrayList<>();
    private Location centerLocation;

    // 游戏状态
    private boolean pvpEnabled = false;
    private int initialBorderSize = 280;
    private int firstShrinkSize = 50;
    private int finalShrinkSize = 1;

    public SurvivalGame(String title, int id, String mapName, int round, boolean isMultiGame, String configFileName,
            int launchDuration, int introDuration, int preparationDuration, int cyclePreparationDuration,
            int cycleStartDuration, int cycleEndDuration, int endDuration) {
        super(title, id, mapName, round, isMultiGame, configFileName,
                launchDuration, introDuration, preparationDuration, cyclePreparationDuration,
                cycleStartDuration, cycleEndDuration, endDuration);
    }

    @Override
    public void onLaunch() {
        super.onLaunch();

        // 加载配置
        loadConfig();

        World world = Bukkit.getWorld(this.getWorldName());
        if (world != null) {
            world.setGameRule(GameRule.NATURAL_REGENERATION, false);
            world.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true);
            world.setGameRule(GameRule.KEEP_INVENTORY, false);
        }

        setActiveTeams(MCETeamUtils.getActiveTeams());
        MCETeleporter.globalSwapWorld(this.getWorldName());
        MCEWorldUtils.disablePVP();
        MCEPlayerUtils.globalSetGameModeDelayed(GameMode.ADVENTURE, 5L);

        this.getGameBoard().setStateTitle("<red><bold> 游戏开始：</bold></red>");

        // 设置玩家血量
        for (Player player : Bukkit.getOnlinePlayers()) {
            Objects.requireNonNull(player.getAttribute(Attribute.MAX_HEALTH)).setBaseValue(20.0);
            player.setHealth(20.0);
        }

        MCEPlayerUtils.clearGlobalTags();

        // 启动处理器
        pvPControlHandler.start();
        // 由全局淘汰监听器处理死亡；本地 handler 可保持挂起或用于仅限SG的扩展
        playerDeathHandler.suspend();

        // 启动时重置世界边界（并应用外扩设置）
        resetWorldBorder();

        // 启动时先给所有箱子上一遍锁
        lockLootChests();
    }

    @Override
    public void onPreparation() {
        super.onPreparation();
        plugin.getLogger().info("SurvivalGame: onPreparation triggered. duration=" + getPreparationDuration() + "s");
        this.getGameBoard().setStateTitle("<red><bold> 游戏开始：</bold></red>");
        this.getGameBoard().globalDisplay();
    }

    @Override
    public void onCyclePreparation() {
        super.onCyclePreparation();

        // 调试日志移除
        this.getGameBoard().setStateTitle("<red><bold> 回合准备中：</bold></red>");
        this.getGameBoard().globalDisplay();

        // 重置游戏板
        resetGameBoard();

        // 额外清理上一回合遗留的死亡箱子
        clearDeathChests();

        // 清理上一回合遗留的掉落物
        World worldPrep = Bukkit.getWorld(this.getWorldName());
        if (worldPrep != null) {
            int cleared = 0;
            for (org.bukkit.entity.Entity e : worldPrep.getEntities()) {
                if (e instanceof org.bukkit.entity.Item) {
                    e.remove();
                    cleared++;
                }
            }
            plugin.getLogger().info("SurvivalGame: 准备阶段清理掉落物数量=" + cleared);
        }

        // 分配出生点（将队伍玩家传送到出生点）
        assignSpawnPoints();

        // 每回合准备：将所有玩家经验设置为100级并清零经验条
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.setLevel(100);
            p.setExp(0);
        }

        // 每回合开始前，将所有玩家设置为冒险模式
        MCEPlayerUtils.globalSetGameModeDelayed(GameMode.ADVENTURE, 5L);

        // 在回合准备阶段生成并填充战利品箱
        spawnLootChests();

        // 倒计时准备（禁用移动 + 倒计时），在准备阶段进行
        movementRestrictionHandler.start();
        // 回合准备阶段：使用原版锁机制上锁所有箱子
        lockLootChests();
        // 在准备阶段末尾开始10秒倒计时，使倒计时正好跨到回合开始
        startCountdown(Math.max(0, getCyclePreparationDuration() - 10));
    }

    @Override
    public void onCycleStart() {
        // 调试日志移除
        this.getGameBoard().setStateTitle("<red><bold> 第" + getCurrentRound() + "回合：</bold></red>");
        this.getGameBoard().globalDisplay();

        // 设置世界边界
        setupWorldBorder();

        // 在回合开始阶段允许移动并提示开始（准备阶段倒计时结束）
        movementRestrictionHandler.suspend();
        // 回合开始：解除箱子上锁
        unlockLootChests();
        MCEMessenger.sendGlobalInfo("<green><bold>游戏开始！</bold></green>");

        // 开始循环背景音乐（4分08秒=248秒一轮）
        startBackgroundMusic();

        // 清空本回合击杀统计与淘汰顺序
        clearKillStats();
        clearEliminationOrder();

        // 不再本地控制 PvP，完全交给全局 /togglepvp 与 /togglefriendlyfire

        // 3分钟后开始第一次缩圈
        setDelayedTask(180, SurvivalGameFuncImpl::startFirstBorderShrink);

        // 6分钟后开始第二次缩圈
        setDelayedTask(360, SurvivalGameFuncImpl::startFinalBattle);
    }

    @Override
    public void onCycleEnd() {
        this.getGameBoard().setStateTitle("<red><bold> 第 " + getCurrentRound() + " 回合结束：</bold></red>");

        // 停止一切计时任务，避免跨回合影响
        clearDelayedTasks();
        clearGameTasks();

        // 停止循环背景音乐
        stopBackgroundMusic();

        // 重置世界边界
        resetWorldBorder();

        // 清理箱子
        clearLootChests();
        // 清理死亡箱子
        clearDeathChests();

        // 回合结束：发送队伍排名
        sendRoundRanking();
        // 回合结束：发送击杀榜
        sendKillRanking();

        // 不再在回合结束修改 PvP 状态

        // 清理地面掉落物
        World worldEnd = Bukkit.getWorld(this.getWorldName());
        if (worldEnd != null) {
            int clearedEnd = 0;
            for (org.bukkit.entity.Entity e : worldEnd.getEntities()) {
                if (e instanceof org.bukkit.entity.Item) {
                    e.remove();
                    clearedEnd++;
                }
            }
            plugin.getLogger().info("SurvivalGame: 回合结束清理掉落物数量=" + clearedEnd);
        }

        // 清理玩家背包
        MCEPlayerUtils.globalClearInventory();

        // 重置玩家血量
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getGameMode() != GameMode.SPECTATOR) {
                player.setHealth(20.0);
            }
        }
    }

    @Override
    public void onEnd() {
        sendWinningMessage();
        MCEPlayerUtils.globalSetGameMode(GameMode.SPECTATOR);

        // 清理死亡箱子，防止遗留到后续流程
        clearDeathChests();

        setDelayedTask(getEndDuration(), () -> {
            MCEPlayerUtils.globalClearFastBoard();
            this.stop();
            MCEMainController.launchVotingSystem();
        });
    }

    @Override
    public void initGameBoard() {
        setGameBoard(new SurvivalGameGameBoard(getTitle(), getWorldName()));
    }

    @Override
    public void stop() {
        super.stop();

        clearGameTasks();
        movementRestrictionHandler.suspend();
        pvPControlHandler.suspend();
        playerDeathHandler.suspend();

        // 停止循环背景音乐
        stopBackgroundMusic();

        // 重置世界边界
        resetWorldBorder();

        // 清理箱子
        clearLootChests();
        // 确保清理所有死亡箱子
        clearDeathChests();
    }

    @Override
    protected void checkGameEndCondition() {
        // 检查是否只剩一个队伍
        checkWinCondition();
    }

    public void clearGameTasks() {
        for (BukkitRunnable task : gameTasks) {
            if (task != null && !task.isCancelled()) {
                task.cancel();
            }
        }
        gameTasks.clear();
    }

    private void startBackgroundMusic() {
        // 立即播放一次
        MCEPlayerUtils.globalPlaySound("minecraft:survival_game");

        // 248秒循环播放
        if (musicLoopTask != null && !musicLoopTask.isCancelled()) {
            musicLoopTask.cancel();
        }
        musicLoopTask = new BukkitRunnable() {
            @Override
            public void run() {
                MCEPlayerUtils.globalPlaySound("minecraft:survival_game");
            }
        };
        musicLoopTask.runTaskTimer(plugin, 248 * 20L, 248 * 20L);
    }

    private void stopBackgroundMusic() {
        MCEPlayerUtils.globalStopMusic();
        if (musicLoopTask != null && !musicLoopTask.isCancelled()) {
            musicLoopTask.cancel();
        }
        musicLoopTask = null;
    }
}
