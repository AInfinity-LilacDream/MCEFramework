package mcevent.MCEFramework.games.survivalGame;

import lombok.Getter;
import lombok.Setter;
import mcevent.MCEFramework.MCEMainController;
import mcevent.MCEFramework.games.survivalGame.customHandler.MovementRestrictionHandler;
import mcevent.MCEFramework.games.survivalGame.customHandler.BuildRulesHandler;
import mcevent.MCEFramework.games.survivalGame.customHandler.PlayerDeathHandler;
import mcevent.MCEFramework.games.survivalGame.customHandler.AnvilDurabilityHandler;
import mcevent.MCEFramework.games.survivalGame.customHandler.FireCleanupHandler;
import mcevent.MCEFramework.games.survivalGame.customHandler.SnowballDamageHandler;
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
    private BuildRulesHandler buildRulesHandler = new BuildRulesHandler();
    private PlayerDeathHandler playerDeathHandler = new PlayerDeathHandler();
    private AnvilDurabilityHandler anvilDurabilityHandler = new AnvilDurabilityHandler();
    private FireCleanupHandler fireCleanupHandler = new FireCleanupHandler();
    private SnowballDamageHandler snowballDamageHandler = new SnowballDamageHandler();

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
        MCEPlayerUtils.globalClearPotionEffects();

        World world = Bukkit.getWorld(this.getWorldName());
        if (world != null) {
            world.setGameRule(GameRule.NATURAL_REGENERATION, true);
            world.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true);
            world.setGameRule(GameRule.KEEP_INVENTORY, false);
        }

        setActiveTeams(MCETeamUtils.getActiveTeams());
        MCETeleporter.globalSwapWorld(this.getWorldName());
        MCEWorldUtils.disablePVP();
        MCEPlayerUtils.globalSetGameModeDelayed(GameMode.SURVIVAL, 5L);

        this.getGameBoard().setStateTitle("<red><bold> 游戏开始：</bold></red>");

        // 设置玩家血量
        for (Player player : Bukkit.getOnlinePlayers()) {
            Objects.requireNonNull(player.getAttribute(Attribute.MAX_HEALTH)).setBaseValue(20.0);
            player.setHealth(20.0);
        }

        MCEPlayerUtils.clearGlobalTags();

        // 启动处理器（由全局淘汰监听器处理死亡；PVP 由全局处理器控制）
        playerDeathHandler.suspend();

        // 注册火焰清理处理器（默认挂起，回合阶段控制启动/暂停）
        fireCleanupHandler.register(this);
        // 注册雪球伤害处理器（默认挂起，回合阶段控制启动/暂停）
        snowballDamageHandler.register(this);

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

        // 回合准备：确保关闭全局 PVP（避免上一回合遗留导致开局可打人）
        pvpEnabled = false;
        MCEWorldUtils.disablePVP();

        // 额外清理上一回合遗留的死亡箱子
        clearDeathChests();

        // 清理上一回合遗留的掉落物与经验球
        clearDropsAndOrbs();

        // 分配出生点
        assignSpawnPoints();

        // 每回合准备：将所有玩家经验设置为100级并清零经验条
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (isGameParticipant(p)) {
                p.setLevel(100);
                p.setExp(0);
            }
        }

        // 每回合开始前，将所有玩家设置为生存模式
        // 仅对参与者设为生存，其余保持旁观
        org.bukkit.Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (isGameParticipant(p))
                    p.setGameMode(GameMode.SURVIVAL);
                else
                    p.setGameMode(GameMode.SPECTATOR);
            }
        }, 5L);

        // 在回合准备阶段生成并填充战利品箱
        spawnLootChests();

        // 倒计时准备（禁用移动 + 倒计时），在准备阶段进行
        movementRestrictionHandler.start();
        // 准备阶段允许建造规则启用（可提前放置/记录）
        buildRulesHandler.start();
        // 启用铁砧损耗
        anvilDurabilityHandler.start();
        // 暂停火焰记录，防止准备阶段的杂火计入
        fireCleanupHandler.suspend();
        // 暂停雪球伤害处理（准备阶段不生效）
        snowballDamageHandler.suspend();
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
        // 建造规则保持启用
        buildRulesHandler.start();
        // 回合开始：解除箱子上锁
        unlockLootChests();
        MCEMessenger.sendGlobalInfo("<green><bold>游戏开始！</bold></green>");

        // 开始循环背景音乐（4分08秒=248秒一轮）
        startBackgroundMusic();

        // 清空本回合击杀统计与淘汰顺序
        clearKillStats();
        clearEliminationOrder();

        // 开始记录本回合火焰，以便回合结束清理
        fireCleanupHandler.start();
        // 启用雪球伤害处理（仅当 PVP 开启后实际生效）
        snowballDamageHandler.start();

        // 45秒后开启PvP（调用全局PVP控制方法）
        setDelayedTask(45, () -> {
            pvpEnabled = true;
            MCEWorldUtils.enablePVP();
            MCEMessenger.sendGlobalInfo("<red><bold>PvP已开启！</bold></red>");
        });

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
        // 处理可能延迟1tick创建的死亡箱：延迟再清理一次
        setDelayedTask(0.1, SurvivalGameFuncImpl::clearDeathChests);
        // 清理回合内产生的火焰
        try {
            fireCleanupHandler.clearFires();
        } catch (Throwable ignored) {
        }
        // 回溯玩家放置导致的地形变化
        mcevent.MCEFramework.tools.MCEBlockRestoreUtils.restoreAllForWorld(getWorldName());
        // 清空玩家放置记录
        mcevent.MCEFramework.games.survivalGame.SurvivalGameFuncImpl.clearPlacedBlocks();
        // 暂停铁砧损耗
        anvilDurabilityHandler.suspend();
        // 暂停雪球伤害处理，为下一回合准备
        snowballDamageHandler.suspend();

        // 递增当前回合数以供下一回合显示正确
        this.setCurrentRound(this.getCurrentRound() + 1);

        // 回合结束：发送队伍排名
        sendRoundRanking();
        // 回合结束：发送击杀榜
        sendKillRanking();

        // 不再在回合结束修改 PvP 状态

        // 清理地面掉落物与经验球
        clearDropsAndOrbs();

        // 清理玩家背包（包含护甲与副手）
        MCEPlayerUtils.globalClearInventoryAllSlots();

        // 重置玩家血量
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getGameMode() != GameMode.SPECTATOR) {
                player.setHealth(20.0);
            }
        }
    }

    @Override
    public void onEnd() {
        // 显示“游戏结束”状态并刷新面板
        this.getGameBoard().setStateTitle("<red><bold> 游戏结束：</bold></red>");
        this.getGameBoard().globalDisplay();
        sendWinningMessage();
        // 不在结束阶段修改玩家游戏模式

        // 清理死亡箱子，防止遗留到后续流程
        clearDeathChests();
        // 处理可能延迟1tick创建的死亡箱：延迟再清理一次
        setDelayedTask(0.1, SurvivalGameFuncImpl::clearDeathChests);
        // 清理回合内产生的火焰（与 onCycleEnd 保持一致）
        try {
            fireCleanupHandler.clearFires();
        } catch (Throwable ignored) {
        }
        // 先回溯被替换的原始方块，再移除“纯新增”方块
        int restored = mcevent.MCEFramework.tools.MCEBlockRestoreUtils.restoreAllForWorld(getWorldName());
        mcevent.MCEFramework.games.survivalGame.SurvivalGameFuncImpl.restoreAndClearPlayerPlacedBlocks();
        plugin.getLogger().info("SurvivalGame: 结束阶段方块回溯数量=" + restored);
        // 再延迟1tick二次回溯，兜底潜在的异步更新
        setDelayedTask(0.1, () -> {
            int restored2 = mcevent.MCEFramework.tools.MCEBlockRestoreUtils.restoreAllForWorld(getWorldName());
            mcevent.MCEFramework.games.survivalGame.SurvivalGameFuncImpl.restoreAndClearPlayerPlacedBlocks();
            plugin.getLogger().info("SurvivalGame: 二次方块回溯数量=" + restored2);
        });
        // 同步清理战利品箱并暂停铁砧损耗（与 onCycleEnd 保持一致）
        clearLootChests();
        anvilDurabilityHandler.suspend();
        // 清理地面掉落物与经验球（末回合也清一次，带二次兜底）
        clearDropsAndOrbs();

        // 清除参与者标记
        for (org.bukkit.entity.Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
            p.removeScoreboardTag("Participant");
        }

        setDelayedTask(getEndDuration(), () -> {
            MCEPlayerUtils.globalClearFastBoard();
            this.stop();
            MCEMainController.launchVotingSystem();
        });
    }

    // 统一清理掉落物与经验球，带一次延迟兜底
    private void clearDropsAndOrbs() {
        World world = Bukkit.getWorld(this.getWorldName());
        if (world == null)
            return;
        int clearedItems = 0;
        int clearedOrbs = 0;
        for (org.bukkit.entity.Item it : world.getEntitiesByClass(org.bukkit.entity.Item.class)) {
            it.remove();
            clearedItems++;
        }
        for (org.bukkit.entity.ExperienceOrb orb : world.getEntitiesByClass(org.bukkit.entity.ExperienceOrb.class)) {
            orb.remove();
            clearedOrbs++;
        }
        plugin.getLogger().info("SurvivalGame: 清理 掉落物=" + clearedItems + ", 经验球=" + clearedOrbs);
        // 二次清理
        setDelayedTask(0.1, () -> {
            World w2 = Bukkit.getWorld(this.getWorldName());
            if (w2 == null)
                return;
            int items2 = 0;
            int orbs2 = 0;
            for (org.bukkit.entity.Item it2 : w2.getEntitiesByClass(org.bukkit.entity.Item.class)) {
                it2.remove();
                items2++;
            }
            for (org.bukkit.entity.ExperienceOrb orb2 : w2.getEntitiesByClass(org.bukkit.entity.ExperienceOrb.class)) {
                orb2.remove();
                orbs2++;
            }
            plugin.getLogger().info("SurvivalGame: 二次清理 掉落物=" + items2 + ", 经验球=" + orbs2);
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
        buildRulesHandler.suspend();
        playerDeathHandler.suspend();
        anvilDurabilityHandler.suspend();

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
