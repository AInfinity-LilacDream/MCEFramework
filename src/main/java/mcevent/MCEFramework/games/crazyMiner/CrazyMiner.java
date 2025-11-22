package mcevent.MCEFramework.games.crazyMiner;

import lombok.Getter;
import lombok.Setter;
import mcevent.MCEFramework.MCEMainController;
import mcevent.MCEFramework.games.crazyMiner.customHandler.BlockBreakHandler;
import mcevent.MCEFramework.games.crazyMiner.customHandler.BorderDistanceHandler;
import mcevent.MCEFramework.games.crazyMiner.gameObject.CrazyMinerGameBoard;
import mcevent.MCEFramework.games.crazyMiner.customHandler.ExplosionDropHandler;
import mcevent.MCEFramework.generalGameObject.MCEGame;
import mcevent.MCEFramework.generalGameObject.MCEGameQuitHandler;
import mcevent.MCEFramework.tools.*;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

import org.bukkit.scoreboard.Team;

import static mcevent.MCEFramework.games.crazyMiner.CrazyMinerFuncImpl.*;
import static mcevent.MCEFramework.miscellaneous.Constants.*;
// import static mcevent.MCEFramework.tools.MCEPlayerUtils.grantGlobalPotionEffect;

/*
CrazyMiner: 惊天矿工团游戏完整实现
*/
@Getter
@Setter
public class CrazyMiner extends MCEGame {

    private BlockBreakHandler blockBreakHandler = new BlockBreakHandler();
    private BorderDistanceHandler borderDistanceHandler = new BorderDistanceHandler();
    private List<BukkitRunnable> gameTasks = new ArrayList<>();
    private CrazyMinerConfigParser crazyMinerConfigParser = new CrazyMinerConfigParser();
    private ExplosionDropHandler explosionDropHandler = new ExplosionDropHandler();

    // 玩家漂浮保护次数记录与提示节流
    private java.util.Map<java.util.UUID, Integer> levitationUsed = new java.util.HashMap<>();
    private java.util.Map<java.util.UUID, Long> levitationLastWarnAt = new java.util.HashMap<>();

    // Game area configuration (loaded from config)
    private Location gameAreaCenter;
    private int gameAreaSizeX;
    private int gameAreaSizeZ;
    private int gameAreaHeight;
    private int gameAreaY;

    // Block generation configuration
    private Map<Material, Double> outerRingBlocks;
    private Map<Material, Double> innerRingBlocks;

    // Spawn points configuration
    private List<Location> spawnPoints = new ArrayList<>();
    private Map<Team, Location> teamSpawnPoints = new HashMap<>();

    // Team elimination tracking
    private List<Team> teamDeathOrder = new ArrayList<>(); // 记录队伍死亡顺序，最先死的在前面

    // Music looping task
    private BukkitRunnable musicLoopTask;

    public CrazyMiner(String title, int id, String mapName, int round, boolean isMultiGame, String configFileName,
                      int launchDuration, int introDuration, int preparationDuration, int cyclePreparationDuration,
                      int cycleStartDuration, int cycleEndDuration, int endDuration) {
        super(title, id, mapName, round, isMultiGame, configFileName,
                launchDuration, introDuration, preparationDuration, cyclePreparationDuration,
                cycleStartDuration, cycleEndDuration, endDuration);
    }

    @Override
    public void init(boolean intro) {
        // 在timeline初始化之前预加载配置，确保使用正确的游戏持续时间
        crazyMinerConfigParser.openAndParse(getConfigFileName());

        // 如果配置文件中的持续时间与构造函数中的不同，使用配置文件的值
        if (getCycleStartDuration() != crazyMinerConfigParser.getGameDuration()) {
            plugin.getLogger().info("暗矢狂潮：使用配置文件中的游戏持续时间: " + crazyMinerConfigParser.getGameDuration() + "秒 (而不是"
                    + getCycleStartDuration() + "秒)");
            setCycleStartDuration(crazyMinerConfigParser.getGameDuration());
        }

        // 调用父类init方法来初始化timeline
        super.init(intro);
    }

    @Override
    public void onLaunch() {
        loadConfig(this);
        MCEPlayerUtils.globalClearPotionEffects();
        MCEPlayerUtils.grantGlobalPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, PotionEffect.INFINITE_DURATION, 1, false, false, true));

        World world = Bukkit.getWorld(this.getWorldName());
        if (world != null) {
            world.setGameRule(GameRule.FALL_DAMAGE, false);
            world.setGameRule(GameRule.KEEP_INVENTORY, true);
            world.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, true);
        }

        setActiveTeams(MCETeamUtils.getActiveTeams());
        MCETeleporter.globalSwapWorld(this.getWorldName());
        MCEWorldUtils.disablePVP();
        MCEPlayerUtils.globalSetGameModeDelayed(GameMode.SURVIVAL, 5L);

        // Initialize world border and game area (creates bedrock structure)
        initializeGameArea(this);

        // Initialize players
        initializePlayers();

        // 开局给予30秒抗火
        try {
            MCEPlayerUtils.grantGlobalPotionEffect(new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.FIRE_RESISTANCE,
                    20 * 30, // 30秒
                    0,
                    false,
                    false,
                    false));
        } catch (Throwable ignored) {
        }

        this.getGameBoard().setStateTitle("<red><bold> 游戏开始：</bold></red>");

        MCEPlayerUtils.clearGlobalTags();

        // 重置漂浮次数与提示节流
        levitationUsed.clear();
        levitationLastWarnAt.clear();

        // Start block break handler
        blockBreakHandler.start();
        // Start explosion drop handler
        explosionDropHandler.start();

        // 10tick后清理掉落物
        setDelayedTask(0.5, () -> {
            World gameWorld = Bukkit.getWorld(this.getWorldName());
            if (gameWorld != null) {
                clearAllDroppedItems(gameWorld);
            }
        });
    }

    @Override
    public void onCycleStart() {
        resetGameBoard(this);
        this.getGameBoard().setStateTitle("<red><bold> 剩余时间：</bold></red>");

        // 开启全局PVP，但关闭友伤（允许不同队伍互相攻击，禁止同队误伤）
        MCEWorldUtils.enablePVP();
        MCETeamUtils.disableFriendlyFire();

        // 播放背景音乐并开始循环
        startBackgroundMusic();

        // 回合准备阶段已设置为生存模式

        // 给玩家初始物品（木镐和牛排）
        giveInitialItems();

        // 给所有玩家上 急迫 II 效果（持续全局，显示粒子，隐藏图标）
        try {
            org.bukkit.potion.PotionEffect haste2 = new org.bukkit.potion.PotionEffect(
                    org.bukkit.potion.PotionEffectType.HASTE,
                    Integer.MAX_VALUE, // 持续整局
                    1, // 等级II -> amplifier=1
                    false,
                    false,
                    false);
            for (org.bukkit.entity.Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
                if (p.getWorld() != null && p.getWorld().getName().equals(getWorldName())) {
                    p.addPotionEffect(haste2);
                }
            }
        } catch (Throwable ignored) {
        }

        // 给所有玩家指向中心的指南针
        giveCompassToAllPlayers();

        // 随机分配队伍到出生点并传送玩家
        assignTeamsToSpawnPoints(this);
        teleportTeamsToSpawnPoints(this);

        // Schedule world border shrinking
        scheduleWorldBorderShrinking(this);

        // Start border distance monitoring
        borderDistanceHandler.start();

        // 游戏结束时间由MCETimeline的cycleStartDuration控制，不需要额外的定时器
        plugin.getLogger().info("暗矢狂潮：游戏开始，将在 " + getCycleStartDuration() + " 秒后自动结束");
    }

    @Override
    public void onPreparation() {
        System.out.println("=== onPreparation() 被调用 ===");
        this.getGameBoard().setStateTitle("<green><bold> 正在生成地图...</bold></green>");
        // Generate random blocks during preparation phase start
        generateRandomBlocks(this);
    }

    @Override
    public void handlePlayerQuitDuringGame(org.bukkit.entity.Player player) {
        // 使用统一的退出处理逻辑
        String playerName = player.getName();
        Team playerTeam = MCETeamUtils.getTeam(player);

        MCEGameQuitHandler.handlePlayerQuit(this, player, () -> {
            // CrazyMiner 使用 teamDeathOrder 而不是 teamEliminationOrder
            // 检查队伍淘汰
            if (MCEGameQuitHandler.checkTeamElimination(playerName, playerTeam, teamDeathOrder)) {
                // 队伍淘汰已在 checkTeamElimination 中处理
            }

            // 更新游戏板并检查游戏是否应该结束（与正常死亡处理保持一致）
            // MCEGameQuitHandler.handlePlayerQuit 已经使用了延迟任务，所以这里直接调用即可
            CrazyMinerFuncImpl.updateGameBoardOnPlayerDeath(this, player);
        });
    }

    @Override
    public void onCyclePreparation() {
        this.getGameBoard().setStateTitle("<yellow><bold> 准备开始游戏...</bold></yellow>");
    }

    @Override
    public void onEnd() {
        sendWinningMessage(this);
        // 不在结束阶段修改玩家游戏模式
        MCEPlayerUtils.globalClearPotionEffects();

        // 设置结束阶段标题（让时间线计时继续推进，用于显示结束倒计时）
        this.getGameBoard().setStateTitle("<red><bold> 游戏结束：</bold></red>");
        // 仅停止音乐；玩法任务与监听器延后到切换回主城时一并停止，避免影响时间线刷新
        MCEPlayerUtils.globalStopMusic();
        stopBackgroundMusic();
        this.getGameBoard().globalDisplay();

        // onEnd结束后等待一段时间，再清理展示板与资源并返回投票系统
        setDelayedTask(getEndDuration(), () -> {
            MCEPlayerUtils.globalClearFastBoard();
            // 结束时统一停止玩法任务与监听器
            clearGameTasks(this);
            blockBreakHandler.stop();
            borderDistanceHandler.stop();
            explosionDropHandler.stop();
            this.stop();
            MCEMainController.returnToLobbyOrLaunchVoting();
        });
    }

    @Override
    public void initGameBoard() {
        setGameBoard(new CrazyMinerGameBoard(getTitle(), getWorldName()));
    }

    @Override
    public void stop() {
        super.stop();

        // 停止背景音乐循环
        stopBackgroundMusic();

        clearGameTasks(this);
        blockBreakHandler.stop();
        borderDistanceHandler.stop();
        explosionDropHandler.stop();
    }

    private void giveInitialItems() {
        // Give all players unbreakable wooden pickaxe with efficiency 1 and 16 steaks
        ItemStack woodenPickaxe = new ItemStack(Material.WOODEN_PICKAXE);
        ItemMeta pickaxeMeta = woodenPickaxe.getItemMeta();
        if (pickaxeMeta != null) {
            pickaxeMeta.setUnbreakable(true);
            pickaxeMeta.addEnchant(Enchantment.EFFICIENCY, 1, false);
            woodenPickaxe.setItemMeta(pickaxeMeta);
        }

        ItemStack steaks = new ItemStack(Material.COOKED_BEEF, 16);

        for (Player player : Bukkit.getOnlinePlayers()) {
            player.getInventory().clear();
            player.getInventory().addItem(woodenPickaxe);
            player.getInventory().addItem(steaks);
        }
    }

    private void initializePlayers() {
        // Set player attributes
        for (Player player : Bukkit.getOnlinePlayers()) {
            Objects.requireNonNull(player.getAttribute(Attribute.MAX_HEALTH)).setBaseValue(20.0);
            player.setHealth(20.0);
            player.setFoodLevel(20);
        }
    }

    private void giveCompassToAllPlayers() {
        // 创建磁石指针，绑定到游戏区域中心
        ItemStack lodestoneCompass = new ItemStack(Material.COMPASS);

        // 使用从配置文件加载的游戏区域中心位置
        Location targetLocation = this.gameAreaCenter.clone();
        // 调整Y坐标到游戏区域的中心高度
        targetLocation.setY(this.gameAreaY + (this.gameAreaHeight / 2));

        // 设置磁石指针的目标位置和名称
        org.bukkit.inventory.meta.CompassMeta compassMeta = (org.bukkit.inventory.meta.CompassMeta) lodestoneCompass
                .getItemMeta();
        if (compassMeta != null) {
            compassMeta.setDisplayName("§6中心指针");
            compassMeta.setLodestone(targetLocation);
            compassMeta.setLodestoneTracked(false); // 不需要实际的磁石方块
            lodestoneCompass.setItemMeta(compassMeta);
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            // 给玩家磁石指针
            player.getInventory().addItem(lodestoneCompass);
        }
    }

    private void clearAllDroppedItems(World world) {
        int clearedItems = 0;

        // 清理所有掉落物实体
        for (org.bukkit.entity.Entity entity : world.getEntities()) {
            if (entity instanceof org.bukkit.entity.Item) {
                entity.remove();
                clearedItems++;
            }
        }

        plugin.getLogger().info("清理了 " + clearedItems + " 个掉落物");
    }

    /**
     * 开始播放循环背景音乐
     */
    private void startBackgroundMusic() {
        // 立即播放音乐
        MCEPlayerUtils.globalPlaySound("minecraft:crazy_miner");

        // 音乐长度为310秒，设置循环播放
        // 每310秒重新播放一次音乐，直到游戏结束
        musicLoopTask = new BukkitRunnable() {
            @Override
            public void run() {
                // 重新播放音乐
                MCEPlayerUtils.globalPlaySound("minecraft:crazy_miner");
            }
        };

        // 310秒后开始循环，每310秒重复一次
        musicLoopTask.runTaskTimer(plugin, 310 * 20L, 310 * 20L);
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

}