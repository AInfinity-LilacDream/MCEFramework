package mcevent.MCEFramework.generalGameObject;

import lombok.Data;
import mcevent.MCEFramework.MCEMainController;
import mcevent.MCEFramework.tools.*;
import net.kyori.adventure.text.Component;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;

/*
MCEGame: 游戏基类，定义通用游戏接口属性与游戏流程框架
 */
@Data
public class MCEGame {
    private ArrayList<Component> introTextList = new ArrayList<>();

    private String title;
    private int id;

    private ArrayList<Team> activeTeams;

    private String worldName;

    private MCETimeline timeline = new MCETimeline();

    private MCEGameBoard gameBoard;

    // 延时任务管理
    private ArrayList<BukkitRunnable> delayedTasks = new ArrayList<>();

    // 游戏中玩家加入处理器
    private GamePlayerJoinHandler playerJoinHandler;

    private String configFileName;

    private int round = 0;
    private int currentRound = 0;
    private boolean isMultiGame = false;

    private int launchDuration;
    private int introDuration;
    private int preparationDuration;
    private int cyclePreparationDuration;
    private int cycleStartDuration;
    private int cycleEndDuration;
    private int endDuration;

    public MCEGame(String title, int id, String worldName, int round, boolean isMultiGame, String configFileName,
            int launchDuration, int introDuration, int preparationDuration, int cyclePreparationDuration,
            int cycleStartDuration, int cycleEndDuration, int endDuration) {
        setTitle(title);
        setId(id);
        setWorldName(worldName);
        setMultiGame(isMultiGame);
        setRound(round);
        setConfigFileName(configFileName);
        setLaunchDuration(launchDuration);
        setIntroDuration(introDuration);
        setPreparationDuration(preparationDuration);
        setCyclePreparationDuration(cyclePreparationDuration);
        setCycleStartDuration(cycleStartDuration);
        setCycleEndDuration(cycleEndDuration);
        setEndDuration(endDuration);

        // 初始化默认的玩家加入处理器
        this.playerJoinHandler = new DefaultGamePlayerJoinHandler(this);
    }

    public MCEGame(String title, int id, String worldName, boolean isMultiGame, String configFileName,
            int launchDuration, int introDuration, int preparationDuration, int cyclePreparationDuration,
            int cycleStartDuration, int cycleEndDuration, int endDuration) {
        setTitle(title);
        setId(id);
        setWorldName(worldName);
        setMultiGame(isMultiGame);
        setConfigFileName(configFileName);
        setLaunchDuration(launchDuration);
        setIntroDuration(introDuration);
        setPreparationDuration(preparationDuration);
        setCyclePreparationDuration(cyclePreparationDuration);
        setCycleStartDuration(cycleStartDuration);
        setCycleEndDuration(cycleEndDuration);
        setEndDuration(endDuration);

        // 初始化默认的玩家加入处理器
        this.playerJoinHandler = new DefaultGamePlayerJoinHandler(this);
    }

    public void init(boolean intro) {
        this.initGameBoard();
        this.setCurrentRound(1);

        this.setTimeline(new MCETimeline());
        this.getTimeline().addTimelineNode(
                new MCETimelineNode(launchDuration, false, () -> {
                    MCEPlayerUtils.globalClearInventoryAllSlots();
                    this.onLaunch();
                    MCEPlayerUtils.globalGrantTag("Active");
                    markParticipantsByWorld();
                    applyGamemodeByParticipation();
                }, this.getTimeline(), this.getGameBoard()));
        if (intro) {
            this.getTimeline().addTimelineNode(
                    new MCETimelineNode(introDuration, false, this::intro, this.getTimeline(), this.getGameBoard()));
        }
        this.getTimeline().addTimelineNode(
                new MCETimelineNode(preparationDuration, true, this::onPreparation, this.getTimeline(),
                        this.getGameBoard()));

        for (int i = 1; i <= round; ++i) {
            this.getTimeline().addTimelineNode(
                    new MCETimelineNode(cyclePreparationDuration, true, this::onCyclePreparation, this.getTimeline(),
                            this.getGameBoard()));
            // cycleStart: only run game-specific start; do NOT re-grant Participant here
            this.getTimeline().addTimelineNode(
                    new MCETimelineNode(cycleStartDuration, false, () -> {
                        this.onCycleStart();
                    }, this.getTimeline(), this.getGameBoard()));
            if (i < round) {
                this.getTimeline().addTimelineNode(
                        new MCETimelineNode(cycleEndDuration, true, this::onCycleEnd, this.getTimeline(),
                                this.getGameBoard()));
            }
        }

        // Wrap end to always cleanup Participant
        this.getTimeline().addTimelineNode(
                new MCETimelineNode(endDuration, false, () -> {
                    this.onEnd();
                    for (org.bukkit.entity.Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
                        p.removeScoreboardTag("Participant");
                    }
                }, this.getTimeline(), this.getGameBoard()));
    }

    public int getTeamId(Team team) {
        return activeTeams.indexOf(team);
    }

    public void start() {
        MCEMainController.setRunningGame(true);
        MCEMainController.setCurrentTimeline(this.getTimeline());

        timeline.start();
    }

    public void stop() {
        MCEMainController.setRunningGame(false);

        // 清空所有玩家的scoreboard tags
        MCEPlayerUtils.clearGlobalTags();

        // 清理所有玩家的发光效果
        MCEGlowingEffectManager.clearAllGlowingEffects();

        // 清理所有延时任务
        clearDelayedTasks();

        if (timeline != null) {
            timeline.suspend();
        }
    }

    public void onLaunch() {
        // 清理所有玩家的背包，确保每个游戏开始时背包都是空的
        MCEPlayerUtils.globalClearInventory();
        // 统一默认模式为生存
        MCEPlayerUtils.globalSetGameMode(org.bukkit.GameMode.SURVIVAL);
    }

    public void intro() {
        this.getGameBoard().setStateTitle("<red><bold> 游戏介绍：</bold></red>");
        MCEMessenger.sendIntroText(getTitle(), getIntroTextList());
    }

    public void onPreparation() {
        this.getGameBoard().setStateTitle("<red><bold> 游戏开始：</bold></red>");

        // 取消每回合阶段的 Active 发放（已在 onLaunch 统一发放）
    }

    public void onCyclePreparation() {
        // 保留其它准备逻辑，移除 Active 发放（onLaunch 已发放）
    }

    public void onCycleStart() {
    }

    public void onCycleEnd() {
    }

    public void onEnd() {
        // 回收参与者标记
        for (org.bukkit.entity.Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
            p.removeScoreboardTag("Participant");
        }
    }

    public void initGameBoard() {
    }

    /**
     * 将当前位于本游戏世界的玩家标记为参与者（Participant），其他玩家移除该标记。
     */
    protected void markParticipantsByWorld() {
        for (org.bukkit.entity.Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
            if (p.getWorld().getName().equals(getWorldName())) {
                p.addScoreboardTag("Participant");
            } else {
                p.removeScoreboardTag("Participant");
            }
        }
    }

    /**
     * 基于参与者标记设置游戏模式：参与者为生存，非参与者为旁观。
     */
    protected void applyGamemodeByParticipation() {
        for (org.bukkit.entity.Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
            if (isGameParticipant(p))
                p.setGameMode(org.bukkit.GameMode.SURVIVAL);
            else
                p.setGameMode(org.bukkit.GameMode.SPECTATOR);
        }
    }

    /**
     * 创建延时任务并自动注册到任务列表中
     */
    public BukkitRunnable setDelayedTask(double seconds, MCETimerFunction function) {
        BukkitRunnable task = MCETimerUtils.setDelayedTask(seconds, function);
        delayedTasks.add(task);
        return task;
    }

    /**
     * 清理所有延时任务
     */
    protected void clearDelayedTasks() {
        for (BukkitRunnable task : delayedTasks) {
            if (task != null && !task.isCancelled()) {
                task.cancel();
            }
        }
        delayedTasks.clear();
    }

    /**
     * 处理游戏进行中新加入的玩家
     * 使用游戏的玩家加入处理器来处理
     */
    public void handlePlayerJoinDuringGame(org.bukkit.entity.Player player) {
        if (playerJoinHandler != null) {
            playerJoinHandler.handlePlayerJoinDuringGame(player);
        }
    }

    /**
     * 检查玩家是否是游戏参与者
     */
    public boolean isGameParticipant(org.bukkit.entity.Player player) {
        if (playerJoinHandler != null) {
            return playerJoinHandler.isGameParticipant(player);
        }
        return false;
    }

    /**
     * 允许游戏自定义玩家加入处理器
     */
    public void setPlayerJoinHandler(GamePlayerJoinHandler handler) {
        this.playerJoinHandler = handler;
    }

    /**
     * 处理玩家在游戏中退出的逻辑
     * 遵循模板方法模式，提供默认实现，子类可以重写以扩展功能
     */
    public void handlePlayerQuitDuringGame(org.bukkit.entity.Player player) {
        // 默认实现：检查游戏是否应该结束
        checkGameEndCondition();
    }

    /**
     * 检查游戏结束条件
     * 子类应该重写此方法以实现特定的游戏结束逻辑
     */
    protected void checkGameEndCondition() {
        // 默认实现为空，子类可以重写
    }
}
