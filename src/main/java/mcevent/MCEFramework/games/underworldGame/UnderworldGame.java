package mcevent.MCEFramework.games.underworldGame;

import lombok.Getter;
import lombok.Setter;
import mcevent.MCEFramework.MCEMainController;
import mcevent.MCEFramework.games.underworldGame.customHandler.PlayerPositionLockHandler;
import mcevent.MCEFramework.games.underworldGame.customHandler.PlayerRespawnHandler;
import mcevent.MCEFramework.games.underworldGame.customHandler.PlayerSwapHandler;
import mcevent.MCEFramework.games.underworldGame.gameObject.UnderworldGameGameBoard;
import mcevent.MCEFramework.generalGameObject.MCEGame;
import mcevent.MCEFramework.tools.*;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

import static mcevent.MCEFramework.miscellaneous.Constants.*;
import static mcevent.MCEFramework.games.underworldGame.UnderworldGameFuncImpl.*;

/*
UnderworldGame: 阴间游戏完整实现
 */
@Getter
@Setter
public class UnderworldGame extends MCEGame {

    private PlayerPositionLockHandler positionLockHandler = new PlayerPositionLockHandler();
    private PlayerSwapHandler swapHandler = new PlayerSwapHandler();
    private PlayerRespawnHandler respawnHandler = new PlayerRespawnHandler(this);
    private UnderworldGameConfigParser configParser = new UnderworldGameConfigParser();
    
    private List<BukkitRunnable> gameTasks = new ArrayList<>();
    private String generatedWorldName;
    private int alivePlayerCount = 0;

    public UnderworldGame(String title, int id, String mapName, boolean isMultiGame, String configFileName,
            int launchDuration, int introDuration, int preparationDuration, int cyclePreparationDuration,
            int cycleStartDuration, int cycleEndDuration, int endDuration) {
        super(title, id, mapName, isMultiGame, configFileName,
                launchDuration, introDuration, preparationDuration, cyclePreparationDuration, cycleStartDuration,
                cycleEndDuration, endDuration);
    }

    /**
     * 重写getWorldName以返回动态生成的世界名称
     */
    @Override
    public String getWorldName() {
        // 如果已经生成了世界，返回生成的世界名称；否则返回基类的世界名称
        return generatedWorldName != null ? generatedWorldName : super.getWorldName();
    }

    /**
     * 重写applyGamemodeByParticipation，确保进入游戏时所有玩家都是旁观模式
     */
    @Override
    protected void applyGamemodeByParticipation() {
        // 进入游戏时，所有玩家都设置为旁观模式
        // 参与者将在onCycleStart时切换回生存模式
        for (org.bukkit.entity.Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
            p.setGameMode(org.bukkit.GameMode.SPECTATOR);
        }
    }

    @Override
    public void onLaunch() {
        loadConfig();
        MCEPlayerUtils.globalClearPotionEffects();

        // 生成新世界
        generatedWorldName = generateNewWorld();
        
        // 设置世界属性
        World world = Bukkit.getWorld(generatedWorldName);
        if (world != null) {
            world.setGameRule(GameRule.FALL_DAMAGE, false);
            world.setGameRule(GameRule.KEEP_INVENTORY, false);
            world.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true); // 开启立刻重生
            world.setDifficulty(Difficulty.HARD);
            
            // 设置世界边界 300x300
            WorldBorder border = world.getWorldBorder();
            Location spawnLoc = world.getSpawnLocation();
            border.setCenter(spawnLoc.getX(), spawnLoc.getZ());
            border.setSize(300);
            border.setWarningDistance(5);
            border.setWarningTime(10);
        }

        setActiveTeams(MCETeamUtils.getActiveTeams());
        
        // 传送所有玩家到新世界的出生点
        MCETeleporter.globalSwapWorld(generatedWorldName);
        
        MCEWorldUtils.disablePVP();
        // 进入游戏时将所有玩家设置为旁观模式
        MCEPlayerUtils.globalSetGameModeDelayed(GameMode.SPECTATOR, 5L);
        MCEPlayerUtils.globalHideNameTag();

        // 初始化玩家属性
        initializePlayers();

        // 锁定玩家在高空50格（与onCyclePreparation相同）
        positionLockHandler.start();
        lockPlayersInSky();

        this.getGameBoard().setStateTitle("<red><bold> 游戏开始：</bold></red>");
        MCEPlayerUtils.clearGlobalTags();
    }

    @Override
    public void intro() {
        // Intro阶段：锁定玩家位置在高空
        super.intro();
        positionLockHandler.start();
        lockPlayersInSky();
    }

    @Override
    public void onCyclePreparation() {
        // 准备阶段：继续锁定玩家位置
        positionLockHandler.start();
        lockPlayersInSky();
        this.getGameBoard().setStateTitle("<yellow><bold> 准备中：</bold></yellow>");
    }

    @Override
    public void onCycleStart() {
        // 游戏正式开始
        positionLockHandler.suspend(); // 解除位置锁定
        
        // 启动重生处理器
        respawnHandler.start();
        
        // 设置所有参与者的重生点到游戏世界的出生点
        World world = Bukkit.getWorld(generatedWorldName);
        if (world != null) {
            Location spawnLoc = world.getSpawnLocation();
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getWorld().getName().equals(generatedWorldName) &&
                        player.getScoreboardTags().contains("Participant")) {
                    // 设置重生点到游戏世界的出生点
                    player.setRespawnLocation(spawnLoc);
                    player.setGameMode(GameMode.SURVIVAL);
                    plugin.getLogger().info("[UnderworldGame][CycleStartDebug] 玩家 " + player.getName() + 
                        " 设置重生点到游戏世界: " + generatedWorldName + 
                        ", 位置: " + spawnLoc.getX() + "," + spawnLoc.getY() + "," + spawnLoc.getZ());
                }
            }
        } else {
            plugin.getLogger().warning("[UnderworldGame][CycleStartDebug] 无法找到游戏世界: " + generatedWorldName);
        }
        
        // 初始化存活玩家数（延迟以确保游戏模式已设置）
        gameTasks.add(MCETimerUtils.setDelayedTask(0.5, () -> {
            updateAlivePlayerCount();
        }));
        
        this.getGameBoard().setStateTitle("<green><bold> 游戏进行中</bold></green>");
        
        // 关闭玩家碰撞体积
        setTeamsCollision(false);
        
        // 开局关闭全局PVP（45秒保护期）
        MCEWorldUtils.disablePVP();
        
        // 让玩家从高处掉落
        dropPlayersFromSky();
        
        // 确保玩家可以移动（在掉落之后再次启用，确保移动能力）
        enablePlayerMovement();
        
        // 10秒后开启摔落伤害
        gameTasks.add(MCETimerUtils.setDelayedTask(10, () -> {
            World gameWorld = Bukkit.getWorld(generatedWorldName);
            if (gameWorld != null) {
                gameWorld.setGameRule(GameRule.FALL_DAMAGE, true);
                MCEMessenger.sendGlobalInfo("<red>摔落伤害已开启！</red>");
            }
        }));
        
        // 45秒后开启全局PVP并关闭友伤
        gameTasks.add(MCETimerUtils.setDelayedTask(45, () -> {
            MCEWorldUtils.enablePVP();
            MCETeamUtils.disableFriendlyFire();
            MCEMessenger.sendGlobalInfo("<red><bold>PVP保护期结束！PVP已开启，友伤已关闭！</bold></red>");
        }));
        
        // 启动玩家交换系统
        swapHandler.start();
        startPlayerSwapSystem();
    }
    
    /**
     * 设置队伍碰撞规则
     */
    private void setTeamsCollision(boolean enabled) {
        java.util.ArrayList<org.bukkit.scoreboard.Team> teams = getActiveTeams();
        if (teams == null || teams.isEmpty()) {
            teams = MCETeamUtils.getActiveTeams();
        }
        if (teams == null) {
            return;
        }
        for (org.bukkit.scoreboard.Team team : teams) {
            if (team == null) {
                continue;
            }
            try {
                team.setOption(org.bukkit.scoreboard.Team.Option.COLLISION_RULE,
                        enabled ? org.bukkit.scoreboard.Team.OptionStatus.ALWAYS : org.bukkit.scoreboard.Team.OptionStatus.NEVER);
            } catch (Throwable ignore) {
                // 兼容性考虑：若服务器不支持该API，则忽略
            }
        }
    }

    @Override
    public void handlePlayerQuitDuringGame(Player player) {
        // 更新存活玩家数
        updateAlivePlayerCount();
        
        // 检查游戏结束条件
        checkGameEndCondition();
    }

    @Override
    protected void checkGameEndCondition() {
        // 游戏结束条件由全局死亡处理器处理（只剩一个队伍时结束）
        // 这里保留方法以保持接口一致性，但实际逻辑在 GlobalEliminationHandler 中
    }

    @Override
    public void onCycleEnd() {
        swapHandler.suspend();
        respawnHandler.suspend();
        
        // 停止交换系统（停止所有交换相关的定时任务）
        stopSwapSystem();
        
        // 清理游戏任务
        clearGameTasks();
        
        // 将所有玩家设置为旁观模式
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getWorld().getName().equals(generatedWorldName)) {
                player.setGameMode(GameMode.SPECTATOR);
            }
        }
        
        this.getGameBoard().setStateTitle("<red><bold> 游戏结束：</bold></red>");
    }

    @Override
    public void onEnd() {
        swapHandler.suspend();
        positionLockHandler.suspend();
        respawnHandler.suspend();
        
        // 停止交换系统（停止所有交换相关的定时任务）
        stopSwapSystem();
        
        // 清理游戏任务（包括10秒后开启摔落伤害、45秒后开启PVP等任务）
        clearGameTasks();
        
        // 将所有玩家设置为旁观模式
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getWorld().getName().equals(generatedWorldName)) {
                player.setGameMode(GameMode.SPECTATOR);
            }
        }
        
        // 设置状态标题为"游戏结束"，并刷新展示板以显示倒计时
        this.getGameBoard().setStateTitle("<red><bold> 游戏结束：</bold></red>");
        this.getGameBoard().globalDisplay();
        
        // 发送获胜消息
        sendWinningMessage();
        
        // 启动结束倒计时（倒计时结束后会调用stop()，在那里删除世界）
        startEndCountdown();
    }

    @Override
    public void initGameBoard() {
        setRound(1); // 阴间游戏只有一个回合
        setGameBoard(new UnderworldGameGameBoard(getTitle(), generatedWorldName != null ? generatedWorldName : getWorldName()));
    }

    @Override
    public void stop() {
        super.stop();
        
        swapHandler.suspend();
        positionLockHandler.suspend();
        respawnHandler.suspend();
        
        clearGameTasks();
        
        // 恢复玩家碰撞
        setTeamsCollision(true);
        
        MCEPlayerUtils.globalShowNameTag();
        
        // 清理生成的世界
        cleanupGeneratedWorld();
    }
    
    /**
     * 清理并删除生成的世界
     */
    private void cleanupGeneratedWorld() {
        if (generatedWorldName == null || generatedWorldName.equals("world")) {
            return; // 不删除默认世界
        }
        
        World world = Bukkit.getWorld(generatedWorldName);
        if (world != null) {
            // 先传送所有玩家离开这个世界
            World lobby = Bukkit.getWorld("lobby");
            if (lobby != null) {
                for (Player player : world.getPlayers()) {
                    player.teleport(lobby.getSpawnLocation());
                }
            }
            
            // 卸载世界
            try {
                Bukkit.unloadWorld(world, false); // false = 不保存世界
                plugin.getLogger().info("已卸载世界: " + generatedWorldName);
            } catch (Exception e) {
                plugin.getLogger().warning("卸载世界失败: " + generatedWorldName + " - " + e.getMessage());
            }
        }
        
        // 删除世界文件夹（异步执行，避免阻塞）
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                java.io.File worldFolder = new java.io.File(Bukkit.getWorldContainer(), generatedWorldName);
                if (worldFolder.exists() && worldFolder.isDirectory()) {
                    deleteDirectory(worldFolder);
                    plugin.getLogger().info("已删除世界文件夹: " + generatedWorldName);
                }
            } catch (Exception e) {
                plugin.getLogger().warning("删除世界文件夹失败: " + generatedWorldName + " - " + e.getMessage());
            }
        });
    }
    
    /**
     * 递归删除目录
     */
    private void deleteDirectory(java.io.File directory) {
        if (directory.exists()) {
            java.io.File[] files = directory.listFiles();
            if (files != null) {
                for (java.io.File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
            directory.delete();
        }
    }

    /**
     * 更新存活玩家数
     */
    public void updateAlivePlayerCount() {
        alivePlayerCount = 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getWorld().getName().equals(generatedWorldName) &&
                    player.getScoreboardTags().contains("Participant") &&
                    !player.getScoreboardTags().contains("dead") &&
                    player.getGameMode() == GameMode.SURVIVAL) {
                alivePlayerCount++;
            }
        }
    }

    /**
     * 清理游戏任务
     */
    private void clearGameTasks() {
        for (BukkitRunnable task : gameTasks) {
            if (task != null && !task.isCancelled()) {
                task.cancel();
            }
        }
        gameTasks.clear();
    }

    /**
     * 启动结束倒计时
     * 注意：倒计时由时间线节点自动处理，这里只需要在倒计时结束后执行清理
     */
    private void startEndCountdown() {
        // 倒计时由 MCEGame 的时间线节点自动处理，不需要手动更新
        // 只需要在倒计时结束后执行清理
        MCETimerUtils.setDelayedTask(getEndDuration(), () -> {
            MCEPlayerUtils.globalClearFastBoard();
            UnderworldGame.this.stop();
            MCEMainController.returnToLobbyOrLaunchVoting();
        });
    }
}

