package mcevent.MCEFramework.games.hyperSpleef;

import lombok.Getter;
import lombok.Setter;
import mcevent.MCEFramework.MCEMainController;
import mcevent.MCEFramework.games.hyperSpleef.customHandler.*;
import mcevent.MCEFramework.games.hyperSpleef.gameObject.HyperSpleefGameBoard;
import mcevent.MCEFramework.games.hyperSpleef.gameObject.*;
import mcevent.MCEFramework.generalGameObject.MCEGame;
import mcevent.MCEFramework.generalGameObject.MCEGameQuitHandler;
import mcevent.MCEFramework.customHandler.SpecialItemInteractionHandler;
import mcevent.MCEFramework.tools.*;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.NamespacedKey;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Team;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

import static mcevent.MCEFramework.miscellaneous.Constants.*;
import static mcevent.MCEFramework.tools.MCEPlayerUtils.grantGlobalPotionEffect;

/*
HyperSpleef: 冰雪乱斗游戏完整实现
*/
@Getter
@Setter
public class HyperSpleef extends MCEGame {

    private PlayerFallHandler playerFallHandler = new PlayerFallHandler();
    private SnowBreakHandler snowBreakHandler = new SnowBreakHandler();
    private SnowballThrowHandler snowballThrowHandler = new SnowballThrowHandler();
    private SnowballHitHandler snowballHitHandler = new SnowballHitHandler();
    private TNTRightClickHandler tntRightClickHandler = new TNTRightClickHandler();
    private TNTExplodeHandler tntExplodeHandler = new TNTExplodeHandler();
    private CraftingDisableHandler craftingDisableHandler = new CraftingDisableHandler();
    private ItemSelectionGUIHandler itemSelectionGUIHandler = new ItemSelectionGUIHandler();
    private HyperSpleefConfigParser hyperSpleefConfigParser = new HyperSpleefConfigParser();
    private SpecialItemInteractionHandler specialItemHandler = new SpecialItemInteractionHandler();

    // 游戏状态追踪
    private List<String> deathOrder = new ArrayList<>();
    private List<Team> teamEliminationOrder = new ArrayList<>();
    private Map<String, Integer> playerSnowballCount = new HashMap<>();

    // 随机事件管理
    private Random random = new Random();
    private List<BukkitRunnable> eventTasks = new ArrayList<>();
    private List<EventInfo> eventSchedule = new ArrayList<>(); // 事件时间表
    private int currentEventIndex = 0; // 当前事件索引
    private Map<Integer, String> scheduledEventTypes = new HashMap<>(); // 预定的随机事件类型

    // 游戏配置
    private static final int FALL_Y_THRESHOLD = 165;
    private static final Material SHOVEL_MATERIAL = Material.GOLDEN_SHOVEL;
    private static final int SHOVEL_EFFICIENCY_LEVEL = 5;

    // 地图复制配置
    private static final String SOURCE_WORLD = "spleef_new_original";
    private static final int COPY_MIN_X = -30;
    private static final int COPY_MIN_Y = 185;
    private static final int COPY_MIN_Z = -25;
    private static final int COPY_MAX_X = 30;
    private static final int COPY_MAX_Y = 205;
    private static final int COPY_MAX_Z = 35;

    public HyperSpleef(String title, int id, String mapName, int round, boolean isMultiGame, String configFileName,
            int launchDuration, int introDuration, int preparationDuration, int cyclePreparationDuration,
            int cycleStartDuration, int cycleEndDuration, int endDuration) {
        super(title, id, mapName, round, isMultiGame, configFileName,
                launchDuration, introDuration, preparationDuration, cyclePreparationDuration,
                cycleStartDuration, cycleEndDuration, endDuration);

        // 注册特殊物品到处理器
        specialItemHandler.registerItem(new IceArrow());
        specialItemHandler.registerItem(new BlizzardStaff());
        specialItemHandler.registerItem(new FloatingFeather());
    }

    public HyperSpleef(String title, int id, String mapName, boolean isMultiGame, String configFileName,
            int launchDuration, int introDuration, int preparationDuration, int cyclePreparationDuration,
            int cycleStartDuration, int cycleEndDuration, int endDuration) {
        super(title, id, mapName, isMultiGame, configFileName,
                launchDuration, introDuration, preparationDuration, cyclePreparationDuration,
                cycleStartDuration, cycleEndDuration, endDuration);

        // 注册特殊物品到处理器
        specialItemHandler.registerItem(new IceArrow());
        specialItemHandler.registerItem(new BlizzardStaff());
        specialItemHandler.registerItem(new FloatingFeather());
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
            MCEGameQuitHandler.checkTeamElimination(playerName, playerTeam, teamEliminationOrder);
            
            // 检查游戏结束条件
            checkGameEndCondition();
        });
    }

    @Override
    protected void checkGameEndCondition() {
        // 统计还活着的"队伍"数量
        Set<Team> teams = new HashSet<>(
                getActiveTeams() != null ? getActiveTeams() : java.util.Collections.emptyList());
        int aliveTeamCount = 0;

        for (Team team : teams) {
            boolean anyAliveInTeam = false;
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (!player.getScoreboardTags().contains("Active") || player.getScoreboardTags().contains("dead"))
                    continue;
                Team playerTeam = MCETeamUtils.getTeam(player);
                if (playerTeam != null && playerTeam.equals(team)) {
                    anyAliveInTeam = true;
                    break;
                }
            }
            if (anyAliveInTeam)
                aliveTeamCount++;
        }

        plugin.getLogger().info("HyperSpleef: 存活队伍数=" + aliveTeamCount);

        if (aliveTeamCount == 1) {
            // 只剩一个队伍 -> 进入回合结束
            if (getTimeline() != null) {
                getTimeline().nextState(); // 由时间线进入 onCycleEnd
            }
        }
    }

    @Override
    public void onLaunch() {
        // 调试：打印世界名称
        plugin.getLogger().info("HyperSpleef: onLaunch - getWorldName() = " + this.getWorldName());

        loadConfig();

        // 调试：打印loadConfig后的世界名称
        plugin.getLogger().info("HyperSpleef: after loadConfig - getWorldName() = " + this.getWorldName());

        MCEPlayerUtils.globalClearPotionEffects();

        World world = Bukkit.getWorld(this.getWorldName());
        if (world != null) {
            world.setGameRule(GameRule.FALL_DAMAGE, false);
            world.setGameRule(GameRule.KEEP_INVENTORY, true);
            world.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, false);
            // 设置时间为正午，禁用昼夜循环和天气循环
            world.setTime(6000L); // 正午（6000 ticks）
            world.setGameRule(GameRule.DO_DAYLIGHT_CYCLE, false);
            world.setGameRule(GameRule.DO_WEATHER_CYCLE, false);
        }

        setActiveTeams(MCETeamUtils.getActiveTeams());

        // 首先从spleef_new_original复制地图区域到spleef_new
        copyMapRegion();

        // 随后将所有玩家传送到spleef_new世界的出生点
        MCETeleporter.globalSwapWorld(this.getWorldName());

        MCEWorldUtils.disablePVP();
        MCETeamUtils.disableFriendlyFire();
        MCEPlayerUtils.globalSetGameModeDelayed(GameMode.SURVIVAL, 5L);
        // 冰雪乱斗需要彼此可见名牌
        MCEPlayerUtils.globalShowNameTag();

        this.getGameBoard().setStateTitle("<red><bold> 游戏开始：</bold></red>");

        // 设置玩家血量为10颗心（20.0血量）
        for (Player player : Bukkit.getOnlinePlayers()) {
            Objects.requireNonNull(player.getAttribute(Attribute.MAX_HEALTH)).setBaseValue(20.0);
            player.setHealth(20.0);
        }

        grantGlobalPotionEffect(saturation);
        MCEPlayerUtils.clearGlobalTags();

        playerFallHandler.register(this);
        playerFallHandler.setPreparationPhase(true);
        playerFallHandler.start();

        // 重置游戏状态
        resetGameState();

        // 清除所有掉落物
        clearAllDroppedItems();
    }

    @Override
    public void onPreparation() {
        super.onPreparation();
        this.getGameBoard().setStateTitle("<red><bold> 游戏开始：</bold></red>");

        // 启用事件处理器
        snowBreakHandler.register(this);
        snowballThrowHandler.register(this);
        snowballHitHandler.register(this);
        tntRightClickHandler.register(this);
        tntExplodeHandler.register(this);
        craftingDisableHandler.register(this);
        itemSelectionGUIHandler.register(this);

        // 让玩家选择特殊道具
        openItemSelectionGUI();
    }

    @Override
    public void onCyclePreparation() {
        // 计算当前回合数（基于时间线状态）
        int currentRound = calculateCurrentRound();
        this.getGameBoard().updateRoundTitle(currentRound);
        this.getGameBoard().setStateTitle("<yellow><bold> 回合准备中：</bold></yellow>");

        // 每个回合开始时复制地图，恢复上一回合被破坏的地图
        copyMapRegion();

        // 取消上一回合遗留的延时任务
        clearDelayedTasks();
        clearEventTasks();

        // 清空上回合的队伍淘汰顺序
        teamEliminationOrder.clear();

        // 重置所有玩家的状态标签
        for (Player player : Bukkit.getOnlinePlayers()) {
            // 移除死亡标签，确保玩家重新激活
            player.removeScoreboardTag("dead");
            // 重新添加Active标签，确保参与者可以参与下一回合
            if (player.getScoreboardTags().contains("Participant")) {
                player.addScoreboardTag("Active");
            }
            plugin.getLogger().info("调试 - 重置玩家 " + player.getName() + " 状态标签");
        }

        // 启动GUI处理器
        itemSelectionGUIHandler.start();

        // 将所有玩家传送到世界出生点
        World world = Bukkit.getWorld(this.getWorldName());
        if (world != null) {
            Location spawnLocation = world.getSpawnLocation();
            for (Player player : Bukkit.getOnlinePlayers()) {
                // 仅传送参与者
                if (player.getScoreboardTags().contains("Participant")) {
                    player.teleport(spawnLocation);
                }
            }
            plugin.getLogger().info("HyperSpleef: 已将所有玩家传送到出生点: " + spawnLocation);
        }

        // 仅将参与者设置为生存模式，非参与者保持旁观
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getScoreboardTags().contains("Participant"))
                p.setGameMode(GameMode.SURVIVAL);
            else
                p.setGameMode(GameMode.SPECTATOR);
        }

        // 在准备阶段结束时，确保所有玩家都选择了物品并给予物品
        setDelayedTask(getCyclePreparationDuration(), () -> {
            // 确保所有玩家都选择了物品（如果未选择，默认选择第一个）
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.getScoreboardTags().contains("Participant")) {
                    ItemSelectionGUIHandler.ensureDefaultSelection(player);
                }
            }

            // 关闭所有打开的GUI
            itemSelectionGUIHandler.suspend();

            // 给玩家发放选择的特殊道具（在准备阶段结束时给予，但效果在游戏开始时生效）
            giveSelectedSpecialItems();
        });
    }

    @Override
    public void onCycleStart() {
        this.getGameBoard().setStateTitle("<green><bold> 游戏进行中：</bold></green>");

        // 播放背景音乐
        playBackgroundMusic();

        // 禁用PVP，关闭友伤
        MCEWorldUtils.disablePVP();
        MCETeamUtils.disableFriendlyFire();

        // 清除所有掉落物
        clearAllDroppedItems();

        // 给所有玩家发放效率5金铲子和初始物品
        giveGoldenShovels();
        giveInitialItems();

        // 启动特殊物品效果（物品已在准备阶段给予）
        specialItemHandler.start();

        // 启动所有事件处理器
        playerFallHandler.setPreparationPhase(false);
        snowBreakHandler.start();
        snowballThrowHandler.start();
        snowballHitHandler.start();
        tntRightClickHandler.start();
        tntExplodeHandler.start();
        craftingDisableHandler.start();

        // 启动随机事件系统（必须在此之前初始化事件表）
        startRandomEvents();

        // 启动展示板更新任务（每秒更新一次）
        startGameBoardUpdateTask();
    }

    @Override
    public void onCycleEnd() {
        // 更新状态栏为下一回合
        this.getGameBoard().setStateTitle("<yellow><bold> 下一回合：</bold></yellow>");

        // 停止背景音乐
        stopBackgroundMusic();

        // 取消本回合所有延时任务
        clearDelayedTasks();
        clearEventTasks();

        // 清除所有随机事件的药水效果
        clearRandomEventEffects();

        // 暂停所有事件处理器（为下一回合准备）
        playerFallHandler.setPreparationPhase(true);
        snowBreakHandler.suspend();
        snowballThrowHandler.suspend();
        snowballHitHandler.suspend();
        tntRightClickHandler.suspend();
        tntExplodeHandler.suspend();
        craftingDisableHandler.suspend();
        specialItemHandler.suspend();

        // 将所有玩家传送回本图世界出生点
        World world = Bukkit.getWorld(this.getWorldName());
        if (world != null) {
            Location spawnLocation = world.getSpawnLocation();
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.teleport(spawnLocation);
            }
        }

        MCEPlayerUtils.globalClearInventory();

        // 显示回合结果
        showRoundResults();
    }

    @Override
    public void onEnd() {
        this.getGameBoard().setStateTitle("<yellow><bold> 游戏结束：</bold></yellow>");

        // 停止背景音乐
        stopBackgroundMusic();

        // 暂停所有事件处理器
        playerFallHandler.suspend();
        snowBreakHandler.suspend();
        snowballThrowHandler.suspend();
        snowballHitHandler.suspend();
        tntRightClickHandler.suspend();
        tntExplodeHandler.suspend();
        craftingDisableHandler.suspend();
        specialItemHandler.suspend();

        clearEventTasks();

        // 显示结果
        showRoundResults();

        // onEnd结束后立即清理展示板和资源，然后启动投票系统
        setDelayedTask(getEndDuration(), () -> {
            MCEPlayerUtils.globalClearFastBoard();
            this.stop(); // 停止所有游戏资源
            MCEMainController.returnToLobbyOrLaunchVoting();
        });
    }

    @Override
    public void stop() {
        // 暂停所有事件处理器
        playerFallHandler.suspend();
        snowBreakHandler.suspend();
        snowballThrowHandler.suspend();
        snowballHitHandler.suspend();
        tntRightClickHandler.suspend();
        tntExplodeHandler.suspend();
        craftingDisableHandler.suspend();
        itemSelectionGUIHandler.suspend();
        specialItemHandler.suspend();

        // 取消所有延时任务
        clearDelayedTasks();
        clearEventTasks();

        super.stop();
    }

    @Override
    public void initGameBoard() {
        this.setGameBoard(new HyperSpleefGameBoard(getTitle(), getWorldName()));
    }

    /**
     * 清除世界中的所有掉落物
     */
    private void clearAllDroppedItems() {
        World world = Bukkit.getWorld(this.getWorldName());
        if (world == null) {
            return;
        }

        int clearedItems = 0;
        // 清理所有掉落物实体
        for (org.bukkit.entity.Item item : world.getEntitiesByClass(org.bukkit.entity.Item.class)) {
            item.remove();
            clearedItems++;
        }

        plugin.getLogger().info("HyperSpleef: 清理了 " + clearedItems + " 个掉落物");
    }

    /**
     * 重置游戏状态
     */
    private void resetGameState() {
        deathOrder.clear();
        playerSnowballCount.clear();

        // 初始化所有玩家的雪球计数
        for (Player player : Bukkit.getOnlinePlayers()) {
            playerSnowballCount.put(player.getName(), 0);
        }
    }

    /**
     * 给所有玩家发放初始物品
     */
    private void giveInitialItems() {
        // 7个雪块
        ItemStack snowBlocks = new ItemStack(Material.SNOW_BLOCK, 7);

        // 7个TNT
        ItemStack tnt = new ItemStack(Material.TNT, 7);

        // 效率2的铁镐
        ItemStack pickaxe = new ItemStack(Material.IRON_PICKAXE);
        ItemMeta pickaxeMeta = pickaxe.getItemMeta();
        if (pickaxeMeta != null) {
            pickaxeMeta.displayName(net.kyori.adventure.text.Component.text("铁镐",
                    net.kyori.adventure.text.format.NamedTextColor.GRAY));
            pickaxeMeta.addEnchant(Enchantment.EFFICIENCY, 2, true);
            pickaxeMeta.setUnbreakable(true);
            pickaxe.setItemMeta(pickaxeMeta);
        }

        // 8个风弹（wind_charge）
        ItemStack windCharge = new ItemStack(Material.WIND_CHARGE, 8);

        // 给所有在线玩家发放初始物品
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getScoreboardTags().contains("Active")) {
                player.getInventory().addItem(snowBlocks.clone());
                player.getInventory().addItem(tnt.clone());
                player.getInventory().addItem(pickaxe.clone());
                player.getInventory().addItem(windCharge.clone());
            }
        }
    }

    /**
     * 给所有玩家发放效率5的金铲子
     */
    private void giveGoldenShovels() {
        ItemStack shovel = new ItemStack(SHOVEL_MATERIAL);
        ItemMeta meta = shovel.getItemMeta();
        if (meta != null) {
            meta.displayName(net.kyori.adventure.text.Component.text("金铲铲",
                    net.kyori.adventure.text.format.NamedTextColor.GOLD));
            meta.addEnchant(Enchantment.EFFICIENCY, SHOVEL_EFFICIENCY_LEVEL, true);
            meta.setUnbreakable(true);

            // 设置攻击伤害为半颗心（1.0伤害值）
            meta.addAttributeModifier(Attribute.ATTACK_DAMAGE,
                    new AttributeModifier(new NamespacedKey(plugin, "hyper_spleef_shovel_damage"),
                            0.0, AttributeModifier.Operation.ADD_NUMBER)); // 设置为1点伤害（半颗心）

            shovel.setItemMeta(meta);
        }

        // 给所有在线玩家发放金铲子
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getScoreboardTags().contains("Active")) {
                player.getInventory().addItem(shovel.clone());
            }
        }
    }

    /**
     * 显示回合结果
     */
    private void showRoundResults() {
        // 找出获胜队伍
        Team winningTeam = findWinningTeam();

        if (winningTeam != null) {
            String teamName = MCETeamUtils.getTeamColoredName(winningTeam);
            MCEMessenger.sendGlobalInfo("<gold>回合结束！获胜队伍: " + teamName + "</gold>");
        } else {
            MCEMessenger.sendGlobalInfo("<gold>回合结束！没有获胜者</gold>");
        }

        // 播报本回合队伍淘汰顺序
        showTeamEliminationOrder();
    }

    /**
     * 找出获胜队伍
     */
    private Team findWinningTeam() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getScoreboardTags().contains("Active") &&
                    !player.getScoreboardTags().contains("dead")) {
                return MCETeamUtils.getTeam(player);
            }
        }
        return null;
    }

    /**
     * 玩家掉落死亡处理（由PlayerFallHandler调用）
     */
    public void onPlayerFallDeath(Player player) {
        // 添加到死亡顺序
        if (!deathOrder.contains(player.getName())) {
            deathOrder.add(player.getName());
        }

        // 预检：在切旁观/传送前，若该玩家掉落导致其队伍无人存活，则登记队伍淘汰
        Team fallingTeam = MCETeamUtils.getTeam(player);
        if (fallingTeam != null && !teamEliminationOrder.contains(fallingTeam)) {
            boolean anyAliveSameTeam = false;
            java.util.Set<Team> aliveTeamsProbe = new java.util.HashSet<>();
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.equals(player))
                    continue; // 排除当前掉落者自身
                if (p.getScoreboardTags().contains("Active") && !p.getScoreboardTags().contains("dead")) {
                    Team pt = MCETeamUtils.getTeam(p);
                    if (pt != null) {
                        aliveTeamsProbe.add(pt);
                        if (pt.equals(fallingTeam))
                            anyAliveSameTeam = true;
                    }
                }
            }
            if (!anyAliveSameTeam && aliveTeamsProbe.size() >= 1) {
                teamEliminationOrder.add(fallingTeam);
                String tname = MCETeamUtils.getTeamColoredName(fallingTeam);
                MCEMessenger.sendGlobalInfo(tname + " <gray>已被团灭！</gray>");
                MCEPlayerUtils.globalPlaySound("minecraft:team_eliminated");
            }
        }

        // 检查游戏结束条件
        checkGameEndCondition();
    }

    /**
     * 玩家掉落处理（用于玩家退出等情况）
     */
    public void handlePlayerFall(Player player) {
        if (!player.getScoreboardTags().contains("Active") ||
                player.getScoreboardTags().contains("dead")) {
            return;
        }

        // 标记淘汰并旁观
        player.addScoreboardTag("dead");
        player.removeScoreboardTag("Active");
        player.setGameMode(GameMode.SPECTATOR);

        String pname = MCEPlayerUtils.getColoredPlayerName(player);
        MCEMessenger.sendGlobalInfo(pname + " <gray>已被淘汰！</gray>");
        MCEPlayerUtils.globalPlaySound("minecraft:player_eliminated");

        // 立即传送到安全位置（世界出生点）防止继续受到伤害
        World world = Bukkit.getWorld(this.getWorldName());
        if (world != null) {
            player.teleport(world.getSpawnLocation());
        }

        // 调用游戏的死亡处理逻辑（记录死亡顺序、检查队伍淘汰等）
        onPlayerFallDeath(player);
    }

    /**
     * 播报当前回合队伍淘汰顺序（从最先淘汰到最后淘汰）
     */
    private void showTeamEliminationOrder() {
        java.util.List<Team> active = getActiveTeams() != null ? getActiveTeams() : java.util.Collections.emptyList();
        java.util.Set<Team> survivors = new java.util.HashSet<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!p.getScoreboardTags().contains("Active") || p.getScoreboardTags().contains("dead"))
                continue;
            Team pt = MCETeamUtils.getTeam(p);
            if (pt != null)
                survivors.add(pt);
        }

        MCEMessenger.sendGlobalInfo("<gold><bold>本回合队伍淘汰顺序：</bold></gold>");
        int idx = 1;
        // 先播报已淘汰队伍（按记录顺序）
        for (Team t : teamEliminationOrder) {
            if (t == null)
                continue;
            if (!active.contains(t))
                continue; // 仅显示本回合参与的队伍
            String name = MCETeamUtils.getTeamColoredName(t);
            MCEMessenger.sendGlobalInfo("<yellow>" + idx + ". </yellow>" + name);
            idx++;
        }
        // 再补齐未被淘汰的存活队伍（确保列表完整）
        for (Team t : active) {
            if (teamEliminationOrder.contains(t))
                continue;
            String name = MCETeamUtils.getTeamColoredName(t);
            MCEMessenger.sendGlobalInfo("<yellow>" + idx + ". </yellow>" + name);
            idx++;
        }
    }

    /**
     * 增加玩家雪球数量
     */
    public void addPlayerSnowballs(Player player, int amount) {
        playerSnowballCount.merge(player.getName(), amount, Integer::sum);

        // 给玩家雪球物品
        ItemStack snowballs = new ItemStack(Material.SNOWBALL, amount);
        player.getInventory().addItem(snowballs);
    }

    /**
     * 获取玩家雪球数量
     */
    public int getPlayerSnowballCount(String playerName) {
        return playerSnowballCount.getOrDefault(playerName, 0);
    }

    /**
     * 加载配置
     */
    private void loadConfig() {
        setIntroTextList(hyperSpleefConfigParser.openAndParse(getConfigFileName()));

        // 从配置文件读取地图名称并更新世界名称
        String configMapName = hyperSpleefConfigParser.getMapName();
        if (configMapName != null && !configMapName.isEmpty()) {
            plugin.getLogger().info("HyperSpleef: 从配置文件更新世界名称: " + this.getWorldName() + " -> " + configMapName);
            this.setWorldName(configMapName);
        }
    }

    /**
     * 获取掉落Y阈值
     */
    public static int getFallYThreshold() {
        return FALL_Y_THRESHOLD;
    }

    /**
     * 播放背景音乐
     */
    private void playBackgroundMusic() {
        MCEPlayerUtils.globalPlaySound("minecraft:hyper_spleef");
        plugin.getLogger().info("HyperSpleef: 播放背景音乐");
    }

    /**
     * 停止背景音乐
     */
    private void stopBackgroundMusic() {
        MCEPlayerUtils.globalStopMusic();
        plugin.getLogger().info("HyperSpleef: 停止背景音乐");
    }

    /**
     * 打开道具选择GUI
     */
    private void openItemSelectionGUI() {
        itemSelectionGUIHandler.start();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getScoreboardTags().contains("Participant")) {
                ItemSelectionGUIHandler.openItemSelectionGUI(player);
            }
        }
    }

    /**
     * 给玩家发放选择的特殊道具
     */
    private void giveSelectedSpecialItems() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.getScoreboardTags().contains("Participant"))
                continue;

            String selectedItem = ItemSelectionGUIHandler.getPlayerSelectedItem(player.getUniqueId());
            if (selectedItem == null) {
                selectedItem = "iceArrow"; // 默认选择第一个
            }

            switch (selectedItem) {
                case "iceArrow":
                    player.getInventory().addItem(new IceArrow().createItem());
                    player.getInventory().addItem(new ItemStack(Material.ARROW, 16));
                    break;
                case "blizzardStaff":
                    player.getInventory().addItem(new BlizzardStaff().createItem());
                    break;
                case "floatingFeather":
                    player.getInventory().addItem(new FloatingFeather().createItem());
                    break;
            }
        }
    }

    /**
     * 启动随机事件系统
     */
    private void startRandomEvents() {
        // 初始化事件时间表
        eventSchedule.clear();
        scheduledEventTypes.clear();
        currentEventIndex = 0;

        // 预先确定每个随机事件的具体类型
        List<String> eventTypes = Arrays.asList(
                "darkness", // 天黑请闭眼
                "levitation", // 飘浮
                "snowSupply", // 雪块补给
                "tntSupply", // TNT补给
                "slowFall", // 失重
                "jumpBoost" // 超绝弹射力
        );

        // 随机选择4个随机事件（60s, 90s, 120s, 180s）
        List<String> selectedEvents = new ArrayList<>();
        Collections.shuffle(eventTypes);
        for (int i = 0; i < 4; i++) {
            selectedEvents.add(eventTypes.get(i));
        }

        // 添加到事件表（随机事件统一显示为"随机事件"）
        eventSchedule.add(new EventInfo(60, "随机事件"));
        scheduledEventTypes.put(60, selectedEvents.get(0));

        eventSchedule.add(new EventInfo(90, "随机事件"));
        scheduledEventTypes.put(90, selectedEvents.get(1));

        eventSchedule.add(new EventInfo(120, "随机事件"));
        scheduledEventTypes.put(120, selectedEvents.get(2));

        eventSchedule.add(new EventInfo(150, "地图变动"));

        eventSchedule.add(new EventInfo(180, "随机事件"));
        scheduledEventTypes.put(180, selectedEvents.get(3));

        eventSchedule.add(new EventInfo(240, "游戏结束")); // 游戏结束时间

        HyperSpleefFuncImpl.startRandomEvents(this, eventSchedule, scheduledEventTypes);
    }

    /**
     * 获取下一个事件信息
     */
    public EventInfo getNextEvent() {
        // 如果事件表为空，返回默认事件
        if (eventSchedule.isEmpty()) {
            return new EventInfo(240, "游戏结束");
        }

        // 获取已过时间（getCounter()返回的是剩余时间）
        int elapsedTime = 0;
        if (getTimeline() != null) {
            elapsedTime = getTimeline().getCurrentTimelineNodeDuration();
        }

        // 查找下一个未触发的事件
        for (int i = currentEventIndex; i < eventSchedule.size(); i++) {
            EventInfo event = eventSchedule.get(i);
            if (event.timeSeconds > elapsedTime) {
                return event;
            }
        }
        // 所有事件都已触发，返回最后一个（游戏结束）
        return eventSchedule.get(eventSchedule.size() - 1);
    }

    /**
     * 更新当前事件索引
     */
    public void updateCurrentEventIndex() {
        // 如果事件表为空，不更新
        if (eventSchedule.isEmpty()) {
            return;
        }

        // 获取已过时间（getCounter()返回的是剩余时间）
        int elapsedTime = 0;
        if (getTimeline() != null) {
            elapsedTime = getTimeline().getCurrentTimelineNodeDuration();
        }

        for (int i = currentEventIndex; i < eventSchedule.size(); i++) {
            if (eventSchedule.get(i).timeSeconds > elapsedTime) {
                currentEventIndex = i;
                return;
            }
        }
        currentEventIndex = eventSchedule.size() - 1; // 指向最后一个事件
    }

    /**
     * 启动展示板更新任务
     */
    private void startGameBoardUpdateTask() {
        BukkitRunnable updateTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (getTimeline() == null || !getTimeline().isHasStarted()) {
                    this.cancel();
                    return;
                }

                // 更新当前事件索引
                updateCurrentEventIndex();

                // 更新展示板
                getGameBoard().globalDisplay();
            }
        };
        updateTask.runTaskTimer(plugin, 0L, 20L); // 每秒更新一次
        addEventTask(updateTask);
    }

    /**
     * 获取事件索引
     */
    public int getCurrentEventIndex() {
        return currentEventIndex;
    }

    /**
     * 获取事件时间表
     */
    public List<EventInfo> getEventSchedule() {
        return eventSchedule;
    }

    /**
     * 清理事件任务
     */
    private void clearEventTasks() {
        for (BukkitRunnable task : eventTasks) {
            if (task != null && !task.isCancelled()) {
                task.cancel();
            }
        }
        eventTasks.clear();
    }

    /**
     * 添加事件任务
     */
    public void addEventTask(BukkitRunnable task) {
        eventTasks.add(task);
    }

    /**
     * 清除所有随机事件的药水效果
     */
    private void clearRandomEventEffects() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            // 清除随机事件可能添加的药水效果
            player.removePotionEffect(PotionEffectType.BLINDNESS); // 天黑请闭眼
            player.removePotionEffect(PotionEffectType.LEVITATION); // 飘浮
            player.removePotionEffect(PotionEffectType.SLOW_FALLING); // 失重
            player.removePotionEffect(PotionEffectType.JUMP_BOOST); // 超绝弹射力
        }
    }

    /**
     * 计算当前回合数
     */
    private int calculateCurrentRound() {
        if (getTimeline() == null) {
            return 1;
        }
        int currentState = getTimeline().getCurrentState();
        // 时间线结构：
        // 0: onLaunch
        // 1: onPreparation
        // 2, 3, 4: 第1回合 (preparation, start, end)
        // 5, 6: 第2回合 (preparation, start)
        // 7: onEnd
        if (currentState < 2) {
            return 1;
        }
        int cycleState = currentState - 2;
        int roundNumber = (cycleState / 3) + 1;
        // 如果超过总回合数，返回最后一回合
        if (roundNumber > getRound()) {
            return getRound();
        }
        return roundNumber;
    }

    /**
     * 复制地图区域从原始地图到当前地图
     */
    private void copyMapRegion() {
        World sourceWorld = Bukkit.getWorld(SOURCE_WORLD);
        World targetWorld = Bukkit.getWorld(this.getWorldName());

        if (sourceWorld == null) {
            plugin.getLogger().warning("HyperSpleef: 源地图 " + SOURCE_WORLD + " 不存在！");
            return;
        }

        if (targetWorld == null) {
            plugin.getLogger().warning("HyperSpleef: 目标地图 " + this.getWorldName() + " 不存在！");
            return;
        }

        plugin.getLogger().info("HyperSpleef: 开始复制地图区域从 " + SOURCE_WORLD + " 到 " + this.getWorldName());

        int copyCount = 0;

        // 复制指定区域的方块（相同坐标）
        for (int x = COPY_MIN_X; x <= COPY_MAX_X; x++) {
            for (int y = COPY_MIN_Y; y <= COPY_MAX_Y; y++) {
                for (int z = COPY_MIN_Z; z <= COPY_MAX_Z; z++) {
                    // 获取源方块
                    Block sourceBlock = sourceWorld.getBlockAt(x, y, z);

                    // 目标位置使用相同坐标
                    Block targetBlock = targetWorld.getBlockAt(x, y, z);
                    targetBlock.setType(sourceBlock.getType());
                    targetBlock.setBlockData(sourceBlock.getBlockData());

                    copyCount++;
                }
            }
        }

        plugin.getLogger().info("HyperSpleef: 地图区域复制完成，共复制了 " + copyCount + " 个方块");
    }
}
