package mcevent.MCEFramework.games.spleef;

import lombok.Getter;
import lombok.Setter;
import mcevent.MCEFramework.MCEMainController;
import mcevent.MCEFramework.games.spleef.customHandler.CraftingDisableHandler;
import mcevent.MCEFramework.games.spleef.customHandler.PlayerFallHandler;
import mcevent.MCEFramework.games.spleef.customHandler.SnowBreakHandler;
import mcevent.MCEFramework.games.spleef.customHandler.SnowballThrowHandler;
import mcevent.MCEFramework.games.spleef.gameObject.SpleefGameBoard;
import mcevent.MCEFramework.generalGameObject.MCEGame;
import mcevent.MCEFramework.tools.*;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.EquipmentSlotGroup;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.NamespacedKey;
import org.bukkit.scoreboard.Team;

import java.util.*;

import static mcevent.MCEFramework.games.spleef.SpleefFuncImpl.*;
import static mcevent.MCEFramework.miscellaneous.Constants.*;
import static mcevent.MCEFramework.tools.MCEPlayerUtils.grantGlobalPotionEffect;

/*
Spleef: 冰雪掘战游戏完整实现
*/
@Getter
@Setter
public class Spleef extends MCEGame {

    private PlayerFallHandler playerFallHandler = new PlayerFallHandler();
    private SnowBreakHandler snowBreakHandler = new SnowBreakHandler();
    private SnowballThrowHandler snowballThrowHandler = new SnowballThrowHandler();
    private CraftingDisableHandler craftingDisableHandler = new CraftingDisableHandler();
    private SpleefConfigParser spleefConfigParser = new SpleefConfigParser();

    // 游戏状态追踪
    private List<String> deathOrder = new ArrayList<>();
    private Map<String, Integer> playerSnowballCount = new HashMap<>();

    // 游戏配置
    private static final int FALL_Y_THRESHOLD = 26;
    private static final Material SHOVEL_MATERIAL = Material.GOLDEN_SHOVEL;
    private static final int SHOVEL_EFFICIENCY_LEVEL = 5;

    // 地图复制配置
    private static final String SOURCE_WORLD = "spleef_christmas_original";
    private static final Location COPY_FROM_MIN = new Location(null, -14, 28, -14);
    private static final Location COPY_FROM_MAX = new Location(null, 30, 35, 30);
    private static final Location COPY_TO_MIN = new Location(null, -14, 28, -14);

    public Spleef(String title, int id, String mapName, int round, boolean isMultiGame, String configFileName,
            int launchDuration, int introDuration, int preparationDuration, int cyclePreparationDuration,
            int cycleStartDuration, int cycleEndDuration, int endDuration) {
        super(title, id, mapName, round, isMultiGame, configFileName,
                launchDuration, introDuration, preparationDuration, cyclePreparationDuration,
                cycleStartDuration, cycleEndDuration, endDuration);
    }

    public Spleef(String title, int id, String mapName, boolean isMultiGame, String configFileName,
            int launchDuration, int introDuration, int preparationDuration, int cyclePreparationDuration,
            int cycleStartDuration, int cycleEndDuration, int endDuration) {
        super(title, id, mapName, isMultiGame, configFileName,
                launchDuration, introDuration, preparationDuration, cyclePreparationDuration,
                cycleStartDuration, cycleEndDuration, endDuration);
    }

    @Override
    public void handlePlayerQuitDuringGame(org.bukkit.entity.Player player) {
        // 将退出玩家添加到死亡顺序
        if (!deathOrder.contains(player.getName())) {
            deathOrder.add(player.getName());
        }

        // 检查游戏结束条件
        checkGameEndCondition();
    }

    @Override
    protected void checkGameEndCondition() {
        // 统计还活着的队伍数量
        Set<Team> aliveTeams = new HashSet<>();
        int totalPlayers = 0;
        int alivePlayers = 0;

        for (Player player : Bukkit.getOnlinePlayers()) {
            totalPlayers++;
            plugin.getLogger().info("调试 - 玩家 " + player.getName() +
                    ": 游戏模式=" + player.getGameMode() +
                    ", Active=" + player.getScoreboardTags().contains("Active") +
                    ", dead=" + player.getScoreboardTags().contains("dead"));

            if (player.getScoreboardTags().contains("Active") &&
                    !player.getScoreboardTags().contains("dead")) {
                alivePlayers++;
                Team playerTeam = MCETeamUtils.getTeam(player);
                if (playerTeam != null) {
                    aliveTeams.add(playerTeam);
                    plugin.getLogger().info("调试 - 存活玩家 " + player.getName() + " 在队伍: " + playerTeam.getName());
                }
            }
        }

        plugin.getLogger()
                .info("调试 - 总玩家: " + totalPlayers + ", 存活玩家: " + alivePlayers + ", 存活队伍: " + aliveTeams.size());

        if (aliveTeams.size() <= 1) {
            // 只剩一个队伍或没有队伍，立即结束当前回合
            plugin.getLogger().info("Spleef: 只剩" + aliveTeams.size() + "个队伍，结束当前回合");

            if (!aliveTeams.isEmpty()) {
                Team winningTeam = aliveTeams.iterator().next();
                plugin.getLogger().info("获胜队伍: " + winningTeam.getName());
            }

            // 强制结束当前cycle
            if (getTimeline() != null) {
                plugin.getLogger().info("调试 - 调用timeline.nextState()");
                getTimeline().nextState();
            } else {
                plugin.getLogger().warning("调试 - timeline为null！");
            }
        }
    }

    @Override
    public void onLaunch() {
        loadConfig();

        World world = Bukkit.getWorld(this.getWorldName());
        if (world != null) {
            world.setGameRule(GameRule.FALL_DAMAGE, false);
            world.setGameRule(GameRule.KEEP_INVENTORY, true);
            world.setGameRule(GameRule.DO_IMMEDIATE_RESPAWN, false);
        }

        setActiveTeams(MCETeamUtils.getActiveTeams());
        MCETeleporter.globalSwapWorld(this.getWorldName());
        MCEWorldUtils.enablePVP();
        MCETeamUtils.disableFriendlyFire();
        MCEPlayerUtils.globalSetGameModeDelayed(GameMode.ADVENTURE, 5L);
        MCEPlayerUtils.globalHideNameTag();

        this.getGameBoard().setStateTitle("<red><bold> 游戏开始：</bold></red>");

        // 设置玩家血量为10颗心（20.0血量）
        for (Player player : Bukkit.getOnlinePlayers()) {
            Objects.requireNonNull(player.getAttribute(Attribute.MAX_HEALTH)).setBaseValue(20.0);
            player.setHealth(20.0);
        }

        grantGlobalPotionEffect(saturation);
        MCEPlayerUtils.clearGlobalTags();

        // 重置游戏状态
        resetGameState();

        // 复制地图区域
        copyMapRegion();
    }

    @Override
    public void onPreparation() {
        super.onPreparation();
        this.getGameBoard().setStateTitle("<red><bold> 游戏开始：</bold></red>");

        // 启用事件处理器
        playerFallHandler.register(this);
        snowBreakHandler.register(this);
        snowballThrowHandler.register(this);
        craftingDisableHandler.register(this);
    }

    @Override
    public void onCyclePreparation() {
        this.getGameBoard().setStateTitle("<yellow><bold> 回合准备中：</bold></yellow>");

        // 重置所有玩家的状态标签
        for (Player player : Bukkit.getOnlinePlayers()) {
            // 移除死亡标签，确保玩家重新激活
            player.removeScoreboardTag("dead");
            if (!player.getScoreboardTags().contains("Active")) {
                player.addScoreboardTag("Active");
            }
            plugin.getLogger().info("调试 - 重置玩家 " + player.getName() + " 状态标签");
        }

        // 将所有玩家传送到世界出生点
        World world = Bukkit.getWorld(this.getWorldName());
        if (world != null) {
            Location spawnLocation = world.getSpawnLocation();
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.teleport(spawnLocation);
            }
            plugin.getLogger().info("Spleef: 已将所有玩家传送到出生点: " + spawnLocation);
        }

        // 将玩家游戏模式设置为冒险模式（准备阶段）
        MCEPlayerUtils.globalSetGameMode(GameMode.ADVENTURE);
    }

    @Override
    public void onCycleStart() {
        this.getGameBoard().setStateTitle("<green><bold> 游戏进行中：</bold></green>");

        // 播放背景音乐
        playBackgroundMusic();

        // 给所有玩家发放效率5金铲子
        giveGoldenShovels();

        // 将玩家游戏模式改为生存模式
        MCEPlayerUtils.globalSetGameMode(GameMode.SURVIVAL);

        // 启动所有事件处理器
        playerFallHandler.start();
        snowBreakHandler.start();
        snowballThrowHandler.start();
        craftingDisableHandler.start();
    }

    @Override
    public void onCycleEnd() {
        // 更新状态栏为下一回合
        this.getGameBoard().setStateTitle("<yellow><bold> 下一回合：</bold></yellow>");

        // 停止背景音乐
        stopBackgroundMusic();

        // 暂停所有事件处理器（为下一回合准备）
        playerFallHandler.suspend();
        snowBreakHandler.suspend();
        snowballThrowHandler.suspend();
        craftingDisableHandler.suspend();

        MCEPlayerUtils.globalClearInventory();
        MCEPlayerUtils.globalSetGameMode(GameMode.SPECTATOR);

        // 重置地图（为下一回合准备）
        copyMapRegion();

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
        craftingDisableHandler.suspend();

        // 显示最终游戏结果
        showFinalGameResults();

        // onEnd结束后立即清理展示板和资源，然后启动投票系统
        setDelayedTask(getEndDuration(), () -> {
            MCEPlayerUtils.globalClearFastBoard();
            this.stop(); // 停止所有游戏资源
            MCEMainController.launchVotingSystem(); // 立即启动投票系统
        });
    }

    @Override
    public void stop() {
        // 暂停所有事件处理器
        playerFallHandler.suspend();
        snowBreakHandler.suspend();
        snowballThrowHandler.suspend();
        craftingDisableHandler.suspend();

        super.stop();
    }

    @Override
    public void initGameBoard() {
        this.setGameBoard(new SpleefGameBoard(getTitle(), getWorldName()));
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
     * 给所有玩家发放效率5的金铲子
     */
    private void giveGoldenShovels() {
        ItemStack shovel = new ItemStack(SHOVEL_MATERIAL);
        ItemMeta meta = shovel.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§6§l金铲铲");
            meta.addEnchant(Enchantment.EFFICIENCY, SHOVEL_EFFICIENCY_LEVEL, true);
            meta.setUnbreakable(true);

            // 设置攻击伤害为半颗心（1.0伤害值）
            meta.addAttributeModifier(Attribute.ATTACK_DAMAGE,
                    new AttributeModifier(new NamespacedKey(plugin, "spleef_shovel_damage"),
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
            String teamName = winningTeam.getDisplayName();
            MCEMessenger.sendGlobalInfo("<gold>回合结束！获胜队伍: " + teamName + "</gold>");
        } else {
            MCEMessenger.sendGlobalInfo("<gold>回合结束！没有获胜者</gold>");
        }
    }

    /**
     * 显示最终游戏结果
     */
    private void showFinalGameResults() {
        // 找出获胜队伍
        Team winningTeam = findWinningTeam();

        if (winningTeam != null) {
            String teamName = winningTeam.getDisplayName();
            MCEMessenger.sendGlobalTitle("<gold><bold>游戏结束！</bold></gold>", "<yellow>获胜队伍: " + teamName + "</yellow>");
            MCEMessenger.sendGlobalInfo("恭喜 " + teamName + " 队获得胜利！");
        } else {
            MCEMessenger.sendGlobalTitle("<gold><bold>游戏结束！</bold></gold>", "<red>没有获胜者</red>");
            MCEMessenger.sendGlobalInfo("所有队伍都被淘汰了！");
        }

        // 显示死亡顺序
        showDeathOrder();
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
     * 显示死亡顺序
     */
    private void showDeathOrder() {
        if (deathOrder.isEmpty()) {
            MCEMessenger.sendGlobalInfo("<gray>没有玩家被淘汰</gray>");
            return;
        }

        MCEMessenger.sendGlobalInfo("<gold><bold>淘汰顺序：</bold></gold>");
        for (int i = 0; i < deathOrder.size(); i++) {
            String playerName = deathOrder.get(i);
            MCEMessenger.sendGlobalInfo("<gray>第" + (i + 1) + "名淘汰: <white>" + playerName + "</white></gray>");
        }
    }

    /**
     * 玩家掉落处理
     */
    public void handlePlayerFall(Player player) {
        if (!player.getScoreboardTags().contains("Active") ||
                player.getScoreboardTags().contains("dead")) {
            return;
        }

        // 添加到死亡顺序
        if (!deathOrder.contains(player.getName())) {
            deathOrder.add(player.getName());
        }

        // 交给全局淘汰处理器统一处理
        mcevent.MCEFramework.customHandler.GlobalEliminationHandler.eliminateNow(player);

        // 立即传送到安全位置（世界出生点）防止继续受到伤害
        World world = Bukkit.getWorld(this.getWorldName());
        if (world != null) {
            player.teleport(world.getSpawnLocation());
        }

        // 消息由全局监听器统一发送（淘汰文案）

        // 检查游戏结束条件
        checkGameEndCondition();
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
        setIntroTextList(spleefConfigParser.openAndParse(getConfigFileName()));
    }

    /**
     * 获取掉落Y阈值
     */
    public static int getFallYThreshold() {
        return FALL_Y_THRESHOLD;
    }

    /**
     * 复制地图区域从原始地图到当前地图
     */
    private void copyMapRegion() {
        World sourceWorld = Bukkit.getWorld(SOURCE_WORLD);
        World targetWorld = Bukkit.getWorld(this.getWorldName());

        if (sourceWorld == null) {
            plugin.getLogger().warning("Spleef: 源地图 " + SOURCE_WORLD + " 不存在！");
            return;
        }

        if (targetWorld == null) {
            plugin.getLogger().warning("Spleef: 目标地图 " + this.getWorldName() + " 不存在！");
            return;
        }

        plugin.getLogger().info("Spleef: 开始复制地图区域从 " + SOURCE_WORLD + " 到 " + this.getWorldName());

        int copyCount = 0;

        // 复制指定区域的方块
        for (int x = COPY_FROM_MIN.getBlockX(); x <= COPY_FROM_MAX.getBlockX(); x++) {
            for (int y = COPY_FROM_MIN.getBlockY(); y <= COPY_FROM_MAX.getBlockY(); y++) {
                for (int z = COPY_FROM_MIN.getBlockZ(); z <= COPY_FROM_MAX.getBlockZ(); z++) {
                    // 获取源方块
                    Block sourceBlock = sourceWorld.getBlockAt(x, y, z);

                    // 计算目标位置（相对于COPY_TO_MIN）
                    int offsetX = x - COPY_FROM_MIN.getBlockX();
                    int offsetY = y - COPY_FROM_MIN.getBlockY();
                    int offsetZ = z - COPY_FROM_MIN.getBlockZ();

                    int targetX = COPY_TO_MIN.getBlockX() + offsetX;
                    int targetY = COPY_TO_MIN.getBlockY() + offsetY;
                    int targetZ = COPY_TO_MIN.getBlockZ() + offsetZ;

                    // 获取目标方块并复制
                    Block targetBlock = targetWorld.getBlockAt(targetX, targetY, targetZ);
                    targetBlock.setType(sourceBlock.getType());
                    targetBlock.setBlockData(sourceBlock.getBlockData());

                    copyCount++;
                }
            }
        }

        plugin.getLogger().info("Spleef: 地图区域复制完成，共复制了 " + copyCount + " 个方块");
    }

    /**
     * 播放背景音乐
     */
    private void playBackgroundMusic() {
        MCEPlayerUtils.globalPlaySound("minecraft:spleef");
        plugin.getLogger().info("Spleef: 播放背景音乐");
    }

    /**
     * 停止背景音乐
     */
    private void stopBackgroundMusic() {
        MCEPlayerUtils.globalStopMusic();
        plugin.getLogger().info("Spleef: 停止背景音乐");
    }
}