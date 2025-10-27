package mcevent.MCEFramework.games.extractOwn;

import lombok.Getter;
import lombok.Setter;
import mcevent.MCEFramework.MCEMainController;
import mcevent.MCEFramework.games.extractOwn.customHandler.AutoReloadHandler;
import mcevent.MCEFramework.games.extractOwn.customHandler.CrossbowAttackHandler;
import mcevent.MCEFramework.games.extractOwn.customHandler.PlayerDeathHandler;
import mcevent.MCEFramework.games.extractOwn.gameObject.ExtractOwnGameBoard;
import mcevent.MCEFramework.generalGameObject.MCEGame;
import mcevent.MCEFramework.tools.*;
import mcevent.MCEFramework.tools.MCEMessenger;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.*;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

import org.bukkit.scoreboard.Team;

import static mcevent.MCEFramework.games.extractOwn.ExtractOwnFuncImpl.*;
import static mcevent.MCEFramework.miscellaneous.Constants.*;
import static mcevent.MCEFramework.tools.MCEPlayerUtils.grantGlobalPotionEffect;

/*
ExtractOwn: 暗矢狂潮游戏完整实现
*/
@Getter
@Setter
public class ExtractOwn extends MCEGame {

    private AutoReloadHandler autoReloadHandler = new AutoReloadHandler();
    private CrossbowAttackHandler crossbowAttackHandler = new CrossbowAttackHandler();
    private PlayerDeathHandler playerDeathHandler = new PlayerDeathHandler();
    private List<BukkitRunnable> gameTasks = new ArrayList<>();
    private ExtractOwnConfigParser extractOwnConfigParser = new ExtractOwnConfigParser();

    // Auto healing task
    private BukkitRunnable autoHealingTask;

    // 游戏区域配置
    private static final Location MAP_CENTER = new Location(null, 409.5, 28, 127.5);
    private static final int INITIAL_BORDER_SIZE = 250;
    private static final int FINAL_BORDER_SIZE = 3;

    // 八个出生点坐标
    private static final Location[] SPAWN_POINTS = {
            new Location(null, 453, 28, 220),
            new Location(null, 501, 28, 163),
            new Location(null, 502, 28, 83),
            new Location(null, 445, 28, 35),
            new Location(null, 365, 28, 34),
            new Location(null, 317, 28, 91),
            new Location(null, 316, 28, 171),
            new Location(null, 373, 28, 219)
    };

    // 队伍出生点分配
    private Map<Team, Location> teamSpawnPoints = new HashMap<>();

    // 淘汰队伍追踪
    private List<String> eliminatedTeams = new ArrayList<>();

    // 回合获胜分数追踪 <队伍名, 回合获胜次数>
    private Map<String, Integer> roundWins = new HashMap<>();

    public ExtractOwn(String title, int id, String mapName, int round, boolean isMultiGame, String configFileName,
            int launchDuration, int introDuration, int preparationDuration, int cyclePreparationDuration,
            int cycleStartDuration, int cycleEndDuration, int endDuration) {
        super(title, id, mapName, round, isMultiGame, configFileName,
                launchDuration, introDuration, preparationDuration, cyclePreparationDuration, cycleStartDuration,
                cycleEndDuration, endDuration);
    }

    @Override
    public void handlePlayerQuitDuringGame(org.bukkit.entity.Player player) {
        // 检查游戏结束条件
        checkGameEndCondition();
    }

    @Override
    protected void checkGameEndCondition() {
        int survivingTeams = getSurvivingTeamCount();
        if (survivingTeams <= 1) {
            // 游戏应该结束，但让时间线自然过渡到cycleEnd阶段
            // 不主动调用nextState，保持游戏流程的一致性
        }
    }

    @Override
    public void onLaunch() {
        // 读取配置文件（包括intro信息）
        loadConfig();
        MCEPlayerUtils.globalClearPotionEffects();
        // 给予全体饱和效果
        grantGlobalPotionEffect(mcevent.MCEFramework.miscellaneous.Constants.saturation);

        World world = Bukkit.getWorld(this.getWorldName());
        if (world != null) {
            world.setGameRule(GameRule.FALL_DAMAGE, false);
            world.setGameRule(GameRule.KEEP_INVENTORY, false);
            world.setGameRule(GameRule.NATURAL_REGENERATION, false);

            // 重置世界边界到初始状态
            world.getWorldBorder().setSize(INITIAL_BORDER_SIZE);
            world.getWorldBorder().setCenter(MAP_CENTER);

            // 设置地图中心位置的世界
            MAP_CENTER.setWorld(world);
            for (Location spawn : SPAWN_POINTS) {
                spawn.setWorld(world);
            }
            MCEWorldUtils.disablePVP();
        }

        // 重置游戏状态
        resetGame();

        setActiveTeams(MCETeamUtils.getActiveTeams());
        MCETeleporter.globalSwapWorld(this.getWorldName());

        // 仅队友可见名牌
        MCEPlayerUtils.globalChangeTeamNameTag();

        // 关闭玩家间碰撞
        setTeamsCollision(false);

        // 初始化玩家属性
        initializePlayers();

        // 第一回合时分配队伍出生点
        assignTeamSpawnPoints();

        // 设置传送阶段状态标题
        this.getGameBoard().setStateTitle("<red><bold> 游戏开始：</bold></red>");

        // 仅传送参与者到世界出生点
        World w = Bukkit.getWorld(this.getWorldName());
        if (w != null) {
            Location worldSpawn = w.getSpawnLocation();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getScoreboardTags().contains("Participant"))
                    p.teleport(worldSpawn);
            }
        }

        // 仅将参与者设置为生存模式（延迟以确保传送后应用）
        this.setDelayedTask(0.25, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getScoreboardTags().contains("Participant") && this.getWorldName().equals(p.getWorld().getName()))
                    p.setGameMode(GameMode.SURVIVAL);
                else
                    p.setGameMode(GameMode.SPECTATOR);
            }
        });

        // 注册事件处理器
        autoReloadHandler.register(this);
        crossbowAttackHandler.register(this);
        playerDeathHandler.register(this);
    }

    @Override
    public void intro() {
        this.getGameBoard().setStateTitle("<red><bold> 游戏介绍：</bold></red>");
        MCEMessenger.sendIntroText(getTitle(), getIntroTextList());
    }

    @Override
    public void onCyclePreparation() {
        // 每局开始前的准备阶段
        this.getGameBoard().setStateTitle("<yellow><bold> 准备阶段：</bold></yellow>");
        if (this.getGameBoard() != null)
            this.getGameBoard().updateRoundTitle(this.getCurrentRound());
        MCEWorldUtils.disablePVP();

        // 仅传送参与者到游戏世界的出生点
        World w2 = Bukkit.getWorld(this.getWorldName());
        if (w2 != null) {
            Location worldSpawn = w2.getSpawnLocation();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getScoreboardTags().contains("Participant"))
                    p.teleport(worldSpawn);
            }
        }

        // 重置回合状态
        eliminatedTeams.clear(); // 清空淘汰队伍列表

        // 重置世界边界到初始状态
        resetWorldBorder();

        // 重置所有玩家状态（仅对参与者：移除死亡标签，设为生存模式；非参与者保持旁观）
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getScoreboardTags().contains("Participant")
                    && this.getWorldName().equals(player.getWorld().getName())) {
                player.removeScoreboardTag("dead");
                player.setGameMode(GameMode.SURVIVAL);
                player.setHealth(player.getMaxHealth());
                player.getInventory().clear();
            } else {
                player.setGameMode(GameMode.SPECTATOR);
            }
        }

        // 重新随机分配队伍出生点
        reassignTeamSpawnPoints();

        // 仅将参与者传送到新的出生点
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.getScoreboardTags().contains("Participant"))
                continue;
            Team playerTeam = MCETeamUtils.getTeam(player);
            if (playerTeam != null && teamSpawnPoints.containsKey(playerTeam)) {
                Location spawnPoint = teamSpawnPoints.get(playerTeam);
                player.teleport(spawnPoint);
            }
        }

        // 给玩家弩和箭
        giveCrossbowAndArrows();

        // 给玩家游戏区域地图
        giveGameAreaMap();

        // 停止之前的音乐并播放新的背景音乐
        MCEPlayerUtils.globalStopMusic();
        MCEPlayerUtils.globalPlaySound("minecraft:extractown");

        // 禁用玩家移动和跳跃
        disablePlayerMovement();

        // 仅队友可见名牌（再次确保）
        MCEPlayerUtils.globalChangeTeamNameTag();

        // 准备阶段禁用PVP
        MCEWorldUtils.disablePVP();

        // 准备阶段关闭玩家碰撞
        setTeamsCollision(false);
    }

    @Override
    public void onPreparation() {
        resetGameBoard();
        if (this.getGameBoard() != null)
            this.getGameBoard().updateRoundTitle(this.getCurrentRound());
        this.getGameBoard().setStateTitle("<yellow><bold> 准备阶段：</bold></yellow>");
    }

    @Override
    public void onCycleStart() {
        MCEPlayerUtils.globalChangeTeamNameTag();
        resetGameBoard();
        if (this.getGameBoard() != null)
            this.getGameBoard().updateRoundTitle(this.getCurrentRound());
        this.getGameBoard().setStateTitle("<red><bold> 剩余时间：</bold></red>");

        // 恢复玩家移动和跳跃
        enablePlayerMovement();

        // 开启全局PVP，但关闭友伤（允许不同队伍互相攻击，禁止同队误伤）
        MCEWorldUtils.enablePVP();
        MCETeamUtils.disableFriendlyFire();

        // 开始自动回血功能
        startAutoHealing();

        // 仅将参与者设置为生存模式，非参与者保持旁观
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getScoreboardTags().contains("Participant") && this.getWorldName().equals(p.getWorld().getName()))
                p.setGameMode(GameMode.SURVIVAL);
            else
                p.setGameMode(GameMode.SPECTATOR);
        }

        // 启动缩圈机制
        startShrinkingBorder();

        // 启动事件处理器
        autoReloadHandler.start();
        crossbowAttackHandler.start();
        playerDeathHandler.start();

        // 启动生存检测任务
        startSurvivalDetection();

        // 游戏时长由MCETimeline的cycleStartDuration控制，不需要额外的定时器
        plugin.getLogger().info("暗矢狂潮：回合开始，将在 " + getCycleStartDuration() + " 秒后自动结束");
    }

    @Override
    public void onCycleEnd() {
        this.getGameBoard().setStateTitle("<green><bold> 回合结束：</bold></green>");
        MCEPlayerUtils.globalShowNameTag();

        // 恢复玩家碰撞
        setTeamsCollision(true);

        // 停止当前回合的任务
        stopRoundTasks();

        // 停止自动回血功能
        stopAutoHealing();

        // 停止背景音乐
        MCEPlayerUtils.globalStopMusic();

        // 显示回合结果
        sendRoundResults();

        // 将所有玩家设为旁观模式
        MCEPlayerUtils.globalSetGameMode(GameMode.SPECTATOR);

        // 增加当前回合数
        this.setCurrentRound(this.getCurrentRound() + 1);
    }

    @Override
    public void onEnd() {
        this.getGameBoard().setStateTitle("<red><bold> 游戏结束：</bold></red>");
        sendWinningMessage();
        MCEPlayerUtils.globalSetGameMode(GameMode.SPECTATOR);

        // 恢复玩家碰撞
        setTeamsCollision(true);

        // onEnd结束后立即清理展示板和资源，然后启动投票系统
        setDelayedTask(getEndDuration(), () -> {
            MCEPlayerUtils.globalClearFastBoard();
            this.stop(); // 停止所有游戏资源
            MCEMainController.launchVotingSystem(); // 立即启动投票系统
        });
    }

    @Override
    public void initGameBoard() {
        setGameBoard(new ExtractOwnGameBoard(getTitle(), getWorldName(), getRound()));
    }

    @Override
    public void stop() {
        super.stop();

        // 停止所有游戏任务
        for (BukkitRunnable task : gameTasks) {
            if (task != null && !task.isCancelled()) {
                task.cancel();
            }
        }
        gameTasks.clear();

        // 停止所有FuncImpl中的任务（包括缩圈任务）
        ExtractOwnFuncImpl.stopAllTasks();

        // 停止背景音乐
        MCEPlayerUtils.globalStopMusic();

        autoReloadHandler.suspend();
        crossbowAttackHandler.suspend();
        playerDeathHandler.suspend();

        // 确保恢复玩家碰撞
        setTeamsCollision(true);

        // 重置世界边界
        World world = Bukkit.getWorld(this.getWorldName());
        if (world != null) {
            world.getWorldBorder().setSize(1000);
            world.getWorldBorder().setCenter(MAP_CENTER);
        }
    }

    /**
     * 重置游戏状态
     */
    private void resetGame() {
        eliminatedTeams.clear();
        teamSpawnPoints.clear();
        initializeTeamScores();
    }

    /**
     * 初始化所有玩家属性
     */
    private void initializePlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            Objects.requireNonNull(player.getAttribute(Attribute.MAX_HEALTH)).setBaseValue(20.0);
            player.setHealth(20.0);
            player.getInventory().clear();
        }
    }

    /**
     * 重置所有玩家状态为新回合做准备
     */
    private void resetAllPlayersForNewRound() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            // 移除死亡标签
            player.removeScoreboardTag("dead");

            // 设置为生存模式
            player.setGameMode(GameMode.SURVIVAL);

            // 回满血
            player.setHealth(player.getMaxHealth());

            // 清空背包
            player.getInventory().clear();
        }
    }

    /**
     * 分配队伍出生点
     */
    private void assignTeamSpawnPoints() {
        List<Team> activeTeams = MCETeamUtils.getActiveTeams();

        if (activeTeams.size() <= 8) {
            // 如果队伍数量不超过8个，按照最优距离分配
            assignOptimalSpawnPoints(activeTeams);
        } else {
            // 如果超过8个队伍，只取前8个
            for (int i = 0; i < Math.min(8, activeTeams.size()); i++) {
                teamSpawnPoints.put(activeTeams.get(i), SPAWN_POINTS[i]);
            }
        }
    }

    /**
     * 按照最优平均距离分配出生点
     */
    private void assignOptimalSpawnPoints(List<Team> teams) {
        if (teams.size() <= 4) {
            // 4个队伍或更少：选择对角线的4个点保证最大距离
            int[] optimalSlots = { 0, 2, 4, 6 }; // 对角线位置
            for (int i = 0; i < teams.size(); i++) {
                teamSpawnPoints.put(teams.get(i), SPAWN_POINTS[optimalSlots[i]]);
            }
        } else {
            // 5个或更多队伍：按顺序分配，尽可能平均分布
            for (int i = 0; i < teams.size(); i++) {
                teamSpawnPoints.put(teams.get(i), SPAWN_POINTS[i]);
            }
        }
    }

    /**
     * 重新随机分配队伍出生点
     */
    private void reassignTeamSpawnPoints() {
        teamSpawnPoints.clear();
        List<Team> activeTeams = MCETeamUtils.getActiveTeams();

        if (activeTeams.size() <= 8) {
            // 创建出生点索引列表并打乱
            List<Integer> spawnIndexes = new ArrayList<>();
            for (int i = 0; i < Math.min(8, SPAWN_POINTS.length); i++) {
                spawnIndexes.add(i);
            }
            java.util.Collections.shuffle(spawnIndexes);

            // 随机分配出生点
            for (int i = 0; i < activeTeams.size(); i++) {
                teamSpawnPoints.put(activeTeams.get(i), SPAWN_POINTS[spawnIndexes.get(i)]);
            }
        }
    }

    /**
     * 传送玩家到各自队伍的出生点
     */
    private void teleportPlayersToSpawnPoints() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            Team playerTeam = MCETeamUtils.getTeam(player);
            if (playerTeam != null && teamSpawnPoints.containsKey(playerTeam)) {
                Location spawnPoint = teamSpawnPoints.get(playerTeam);
                player.teleport(spawnPoint);
            }
        }
    }

    /**
     * 传送所有玩家到世界出生点
     */
    private void teleportPlayersToWorldSpawn() {
        World world = Bukkit.getWorld(this.getWorldName());
        if (world != null) {
            Location worldSpawn = world.getSpawnLocation();
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.teleport(worldSpawn);
            }
        }
    }

    /**
     * 禁用玩家移动和跳跃
     */
    private void disablePlayerMovement() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            Objects.requireNonNull(player.getAttribute(Attribute.MOVEMENT_SPEED)).setBaseValue(0);
            Objects.requireNonNull(player.getAttribute(Attribute.JUMP_STRENGTH)).setBaseValue(0);
        }
    }

    /**
     * 恢复玩家移动和跳跃
     */
    private void enablePlayerMovement() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            Objects.requireNonNull(player.getAttribute(Attribute.MOVEMENT_SPEED)).setBaseValue(0.1);
            Objects.requireNonNull(player.getAttribute(Attribute.JUMP_STRENGTH)).setBaseValue(0.42);
        }
    }

    /**
     * 给玩家弩和箭
     */
    private void giveCrossbowAndArrows() {
        ExtractOwnFuncImpl.givePlayersCrossbowAndArrows();
    }

    /**
     * 启动缩圈机制
     */
    private void startShrinkingBorder() {
        ExtractOwnFuncImpl.startShrinkingBorder(this);
    }

    /**
     * 启动生存检测任务
     */
    private void startSurvivalDetection() {
        ExtractOwnFuncImpl.startSurvivalDetection(this);
    }

    /**
     * 发送获胜消息
     */
    private void sendWinningMessage() {
        ExtractOwnFuncImpl.sendWinningMessage();
    }

    /**
     * 重置游戏板
     */
    private void resetGameBoard() {
        ExtractOwnFuncImpl.resetGameBoard(this);
    }

    /**
     * 初始化队伍分数
     */
    private void initializeTeamScores() {
        ExtractOwnFuncImpl.initializeTeamScores();
    }

    /**
     * 加载配置文件
     */
    private void loadConfig() {
        ExtractOwnFuncImpl.loadConfig(this);
    }

    /**
     * 重置世界边界
     */
    private void resetWorldBorder() {
        World world = Bukkit.getWorld(this.getWorldName());
        if (world != null) {
            world.getWorldBorder().setSize(INITIAL_BORDER_SIZE);
            world.getWorldBorder().setCenter(MAP_CENTER);
        }
    }

    /**
     * 停止当前回合的任务
     */
    private void stopRoundTasks() {
        ExtractOwnFuncImpl.stopAllTasks();
    }

    // ===== 玩家碰撞控制 =====
    private void setTeamsCollision(boolean enabled) {
        org.bukkit.scoreboard.Scoreboard sb = Bukkit.getScoreboardManager().getMainScoreboard();
        for (Team t : sb.getTeams()) {
            if (t == null)
                continue;
            try {
                t.setOption(Team.Option.COLLISION_RULE,
                        enabled ? Team.OptionStatus.ALWAYS : Team.OptionStatus.NEVER);
            } catch (Throwable ignored) {
            }
        }
    }

    /**
     * 发送回合结果
     */
    private void sendRoundResults() {
        ExtractOwnFuncImpl.sendRoundResults(getCurrentRound());
    }

    /**
     * 添加淘汰队伍
     */
    public void addEliminatedTeam(String teamName) {
        if (!eliminatedTeams.contains(teamName)) {
            eliminatedTeams.add(teamName);
        }
    }

    /**
     * 获取存活队伍数量
     */
    public int getSurvivingTeamCount() {
        int survivingTeams = 0;

        for (Team team : MCETeamUtils.getActiveTeams()) {
            boolean hasAlivePlayers = false;

            // 检查队伍中是否有存活的玩家
            for (Player player : MCETeamUtils.getPlayers(team)) {
                if (player.getGameMode() == GameMode.SURVIVAL && !player.getScoreboardTags().contains("dead")) {
                    hasAlivePlayers = true;
                    break;
                }
            }

            if (hasAlivePlayers) {
                survivingTeams++;
            }
        }

        return survivingTeams;
    }

    /**
     * 获取回合获胜记录
     */
    public Map<String, Integer> getRoundWins() {
        return roundWins;
    }

    /**
     * 添加回合获胜
     */
    public void addRoundWin(String teamName) {
        roundWins.merge(teamName, 1, Integer::sum);
    }

    /**
     * 给玩家游戏区域地图
     */
    private void giveGameAreaMap() {
        World world = Bukkit.getWorld(this.getWorldName());
        if (world == null)
            return;

        // 创建地图
        ItemStack mapItem = new ItemStack(Material.FILLED_MAP);
        MapMeta mapMeta = (MapMeta) mapItem.getItemMeta();

        if (mapMeta != null) {
            mapMeta.displayName(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage()
                    .deserialize("<gold><bold>游戏区域地图</bold></gold>"));

            // 创建地图视图，使用原版地图生成逻辑
            MapView mapView = Bukkit.createMap(world);
            mapView.setCenterX(409); // 游戏区域中心 X 坐标
            mapView.setCenterZ(127); // 游戏区域中心 Z 坐标
            mapView.setScale(MapView.Scale.CLOSE); // 使用CLOSE缩放级别
            mapView.setUnlimitedTracking(true); // 允许无限制追踪，预加载整个区域
            mapView.setTrackingPosition(false); // 禁用玩家位置跟踪，固定显示设定区域
            mapView.setLocked(false); // 确保地图未锁定，允许探索渲染

            // 保留原版渲染器，让Minecraft自动生成地图

            mapMeta.setMapView(mapView);
            mapItem.setItemMeta(mapMeta);
        }

        // 预加载地图区域数据
        if (mapMeta != null) {
            MapView mapView = mapMeta.getMapView();
            if (mapView != null) {
                preloadMapArea(world);
            }
        }

        // 给所有玩家地图
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.getInventory().setItem(8, mapItem); // 放在第9格（索引8）
        }
    }

    /**
     * 预加载地图区域数据
     */
    private void preloadMapArea(World world) {
        // 异步预加载128x128区域的所有区块数据
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            int centerX = 409;
            int centerZ = 127;

            for (int x = (centerX - 64) >> 4; x <= (centerX + 64) >> 4; x++) {
                for (int z = (centerZ - 64) >> 4; z <= (centerZ + 64) >> 4; z++) {
                    world.getChunkAt(x, z);
                }
            }
        });
    }

    /**
     * 开始自动回血功能，每4秒回复0.5颗心
     */
    private void startAutoHealing() {
        plugin.getLogger().info("=== 暗矢狂潮：启动自动回血功能 ===");

        autoHealingTask = new BukkitRunnable() {
            @Override
            public void run() {
                int healedPlayerCount = 0;

                // 为所有生存模式且是参与者且未死亡的玩家回复0.5颗心
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.getGameMode() == GameMode.SURVIVAL &&
                            player.getScoreboardTags().contains("Participant") &&
                            !player.getScoreboardTags().contains("dead") &&
                            player.getWorld().getName().equals(getWorldName())) {

                        double currentHealth = player.getHealth();
                        double maxHealth = Objects.requireNonNull(player.getAttribute(Attribute.MAX_HEALTH))
                                .getBaseValue();

                        // 只有当前生命值小于满血时才回复
                        if (currentHealth < maxHealth) {
                            // 回复0.5颗心（1.0血量点），但不超过最大血量
                            double newHealth = Math.min(currentHealth + 1.0, maxHealth);
                            player.setHealth(newHealth);
                            healedPlayerCount++;
                        }
                    }
                }

                // 调试信息：记录回血玩家数量
                if (healedPlayerCount > 0) {
                    plugin.getLogger().info("暗矢狂潮自动回血：为 " + healedPlayerCount + " 名玩家回复生命值");
                }
            }
        };

        // 每4秒执行一次（4 * 20 = 80 ticks）
        autoHealingTask.runTaskTimer(plugin, 80L, 80L);
        plugin.getLogger().info("暗矢狂潮自动回血任务已启动，每4秒执行一次");
    }

    /**
     * 停止自动回血功能
     */
    private void stopAutoHealing() {
        if (autoHealingTask != null && !autoHealingTask.isCancelled()) {
            autoHealingTask.cancel();
            autoHealingTask = null;
            plugin.getLogger().info("=== 暗矢狂潮：自动回血功能已停止 ===");
        }
    }
}