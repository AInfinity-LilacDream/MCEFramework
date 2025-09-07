package mcevent.MCEFramework.games.captureCenter;

import mcevent.MCEFramework.games.captureCenter.gameObject.CaptureCenterGameBoard;
import mcevent.MCEFramework.tools.MCEMessenger;
import mcevent.MCEFramework.tools.MCEPlayerUtils;
import mcevent.MCEFramework.tools.MCETeamUtils;
import mcevent.MCEFramework.tools.MCETimerUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Team;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static mcevent.MCEFramework.miscellaneous.Constants.*;

public class CaptureCenterFuncImpl {
    
    private static final CaptureCenterConfigParser captureCenterConfigParser = captureCenter.getCaptureCenterConfigParser();
    private static final Map<String, Integer> teamScores = new ConcurrentHashMap<>();
    private static boolean scoringEnabled = false;
    private static BukkitRunnable scoringTask;
    private static BukkitRunnable actionBarTask;
    private static BukkitRunnable boardUpdateTask;
    private static final Set<Integer> shrunkLayers = new HashSet<>();
    private static int currentKnockbackLevel = 3; // 初始击退等级改为3
    
    // 击退等级顺序：3->2->2->1->1
    private static final int[] KNOCKBACK_LEVELS = {3, 2, 2, 1, 1};
    private static int knockbackLevelIndex = 0; // 当前击退等级索引
    
    // 常量定义
    private static final double CAPTURE_CENTER_X = 8.0;
    private static final double CAPTURE_CENTER_Z = 8.0;
    private static final int SPAWN_Y = -18;
    
    // 占点规则
    private static final int LAYER1_Y = -54; // 第一层，距离3，每tick+3分
    private static final int LAYER2_Y = -55; // 第二层，距离5，每tick+2分  
    private static final int LAYER3_Y = -56; // 第三层，距离11，每tick+1分
    private static final int KILL_REWARD = 50; // 击杀奖励分数
    
    // 平台层级定义（7层）
    private static final int BOTTOM_LAYER_Y = -61; // 最下层
    private static final int TOP_LAYER_Y = -55;    // 最上层
    
    // 地图范围常量（与地图复制区域一致）
    private static final int MAP_MIN_X = -17;
    private static final int MAP_MAX_X = 33;
    private static final int MAP_MIN_Z = -17;
    private static final int MAP_MAX_Z = 33;
    private static final int MAP_MIN_Y = -61;
    private static final int MAP_MAX_Y = -19;
    
    /**
     * 从配置文件加载数据
     */
    public static void loadConfig() {
        captureCenter.setIntroTextList(captureCenterConfigParser.openAndParse(captureCenter.getConfigFileName()));
    }
    
    /**
     * 初始化队伍分数
     */
    private static void initializeTeamScores() {
        teamScores.clear();
        if (captureCenter.getActiveTeams() != null) {
            for (Team team : captureCenter.getActiveTeams()) {
                teamScores.put(team.getName(), 0);
            }
        }
    }
    
    /**
     * 恢复地图原状
     */
    public static void resetMap() {
        World targetWorld = Bukkit.getWorld(captureCenter.getWorldName()); // capture_classic
        World sourceWorld = Bukkit.getWorld("capture_classic_original");   // 原版地图
        
        if (targetWorld == null || sourceWorld == null) {
            plugin.getLogger().warning("无法找到地图世界！target: " + captureCenter.getWorldName() + ", source: capture_classic_original");
            return;
        }
        
        // 清理平台收缩状态
        shrunkLayers.clear();
        
        // 复制区域：从 capture_classic_original 的地图范围
        // 从 (-17, -61, -17) 到 (33, -19, 33) 包含顶上的黑色平台
        copyWorldRegion(sourceWorld, targetWorld, MAP_MIN_X, MAP_MIN_Y, MAP_MIN_Z, MAP_MAX_X, MAP_MAX_Y, MAP_MAX_Z);
        
        plugin.getLogger().info("地图恢复完成！");
    }
    
    /**
     * 复制世界区域
     * @param sourceWorld 源世界
     * @param targetWorld 目标世界
     * @param minX 最小X坐标
     * @param minY 最小Y坐标
     * @param minZ 最小Z坐标
     * @param maxX 最大X坐标
     * @param maxY 最大Y坐标
     * @param maxZ 最大Z坐标
     */
    private static void copyWorldRegion(World sourceWorld, World targetWorld, 
                                       int minX, int minY, int minZ, 
                                       int maxX, int maxY, int maxZ) {
        int blocksProcessed = 0;
        int totalBlocks = (maxX - minX + 1) * (maxY - minY + 1) * (maxZ - minZ + 1);
        
        plugin.getLogger().info("开始复制地图区域，总共 " + totalBlocks + " 个方块...");
        
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    Location sourceLocation = new Location(sourceWorld, x, y, z);
                    Location targetLocation = new Location(targetWorld, x, y, z);
                    
                    // 复制方块类型和数据
                    org.bukkit.block.Block sourceBlock = sourceWorld.getBlockAt(sourceLocation);
                    org.bukkit.block.Block targetBlock = targetWorld.getBlockAt(targetLocation);
                    
                    targetBlock.setBlockData(sourceBlock.getBlockData());
                    
                    blocksProcessed++;
                    
                    // 每处理10000个方块输出一次进度
                    if (blocksProcessed % 10000 == 0) {
                        plugin.getLogger().info("地图复制进度: " + blocksProcessed + "/" + totalBlocks + " (" + 
                                               String.format("%.1f", (double)blocksProcessed/totalBlocks*100) + "%)");
                    }
                }
            }
        }
        
        plugin.getLogger().info("地图区域复制完成！处理了 " + blocksProcessed + " 个方块");
    }
    
    /**
     * 传送玩家到出生点
     */
    public static void teleportPlayersToSpawn() {
        World world = Bukkit.getWorld(captureCenter.getWorldName());
        if (world == null) return;
        
        Location spawnLocation = new Location(world, CAPTURE_CENTER_X, SPAWN_Y, CAPTURE_CENTER_Z);
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.teleport(spawnLocation);
        }
    }
    
    /**
     * 初始化所有玩家属性
     */
    public static void initializePlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            Objects.requireNonNull(player.getAttribute(Attribute.MAX_HEALTH)).setBaseValue(20.0);
            player.setHealth(20.0);
            player.getInventory().clear();
        }
    }
    
    /**
     * 移除游戏开始平台
     * 移除 (33,-19,33) 到 (-17,-19,-17) 的正方形平台
     */
    public static void removeStartPlatform() {
        World world = Bukkit.getWorld(captureCenter.getWorldName());
        if (world == null) return;
        
        int startPlatformY = -19;
        
        for (int x = MAP_MIN_X; x <= MAP_MAX_X; x++) {
            for (int z = MAP_MIN_Z; z <= MAP_MAX_Z; z++) {
                Location loc = new Location(world, x, startPlatformY, z);
                world.getBlockAt(loc).setType(Material.AIR);
            }
        }
        
        plugin.getLogger().info("游戏开始平台已移除！");
    }
    
    /**
     * 给予玩家击退棒
     */
    public static void giveKnockbackStick() {
        giveKnockbackStick(currentKnockbackLevel);
    }
    
    /**
     * 给予玩家指定等级的击退棒
     */
    public static void giveKnockbackStick(int knockbackLevel) {
        // 清除所有活跃玩家的击退棒
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getGameMode() == GameMode.ADVENTURE && player.getScoreboardTags().contains("Active")) {
                player.getInventory().clear();
            }
        }
        
        // 如果击退等级为0或以下，不给击退棒
        if (knockbackLevel <= 0) {
            return;
        }
        
        ItemStack knockbackStick = new ItemStack(Material.STICK);
        ItemMeta meta = knockbackStick.getItemMeta();
        if (meta != null) {
            meta.displayName(MiniMessage.miniMessage().deserialize("<gold><bold>击退棒（击退" + knockbackLevel + "）</bold></gold>"));
            knockbackStick.setItemMeta(meta);
            knockbackStick.addUnsafeEnchantment(Enchantment.KNOCKBACK, knockbackLevel);
        }
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getGameMode() == GameMode.ADVENTURE && player.getScoreboardTags().contains("Active")) {
                player.getInventory().addItem(knockbackStick);
            }
        }
    }
    
    /**
     * 重置游戏计分板
     */
    public static void resetGameBoard() {
        CaptureCenterGameBoard gameBoard = (CaptureCenterGameBoard) captureCenter.getGameBoard();
        gameBoard.updatePlayerCount(Bukkit.getOnlinePlayers().size());
        gameBoard.updateTeamCount(captureCenter.getActiveTeams().size());
        
        // 重置击退等级
        currentKnockbackLevel = 3;
        knockbackLevelIndex = 0;
        
        // 初始化队伍分数显示
        initializeTeamScores();
        gameBoard.updateTeamScores(teamScores);
    }
    
    /**
     * 启用占点计分
     */
    public static void enableScoring(CaptureCenter game) {
        scoringEnabled = true;
        
        // 启动占点计分任务 (每tick执行)
        scoringTask = new BukkitRunnable() {
            @Override
            public void run() {
                updateCapturePoints();
            }
        };
        scoringTask.runTaskTimer(plugin, 0L, 1L);
        
        // 启动ActionBar更新任务 (每tick执行)
        actionBarTask = new BukkitRunnable() {
            @Override
            public void run() {
                updateActionBar();
            }
        };
        actionBarTask.runTaskTimer(plugin, 0L, 1L);
        
        // 启动展示板更新任务 (每tick执行)
        boardUpdateTask = new BukkitRunnable() {
            @Override
            public void run() {
                CaptureCenterGameBoard gameBoard = (CaptureCenterGameBoard) captureCenter.getGameBoard();
                gameBoard.globalDisplay();
            }
        };
        boardUpdateTask.runTaskTimer(plugin, 0L, 1L);
    }
    
    /**
     * 更新占点计分
     */
    private static void updateCapturePoints() {
        if (!scoringEnabled) return;
        
        // 确保队伍分数已初始化
        if (teamScores.isEmpty() && captureCenter.getActiveTeams() != null) {
            initializeTeamScores();
        }
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getGameMode() != GameMode.ADVENTURE) continue;
            
            Location loc = player.getLocation();
            Team playerTeam = MCETeamUtils.getTeam(player);
            if (playerTeam == null) continue;
            
            int points = calculateCapturePoints(loc);
            if (points > 0) {
                addTeamScore(playerTeam.getName(), points);
            }
        }
    }
    
    /**
     * 计算玩家当前位置的占点分数
     */
    private static int calculateCapturePoints(Location loc) {
        double distance = Math.sqrt(Math.pow(loc.getX() - CAPTURE_CENTER_X, 2) + Math.pow(loc.getZ() - CAPTURE_CENTER_Z, 2));
        int y = (int) Math.floor(loc.getY()); // 使用玩家脚部位置
        
        if (y == LAYER1_Y && distance <= 2) {
            return 3; // 每tick+3分
        } else if (y == LAYER2_Y && distance <= 3) {
            return 2; // 每tick+2分
        } else if (y == LAYER3_Y && distance <= 6) {
            return 1; // 每tick+1分
        }
        
        return 0;
    }
    
    /**
     * 添加队伍分数
     */
    public static void addTeamScore(String teamName, int points) {
        teamScores.merge(teamName, points, Integer::sum);
        // 更新展示板分数
        CaptureCenterGameBoard gameBoard = (CaptureCenterGameBoard) captureCenter.getGameBoard();
        gameBoard.updateTeamScores(teamScores);
    }
    
    /**
     * 处理玩家击杀奖励
     */
    public static void handlePlayerKill(Player killer) {
        if (killer == null) return;
        
        Team killerTeam = MCETeamUtils.getTeam(killer);
        if (killerTeam != null) {
            addTeamScore(killerTeam.getName(), KILL_REWARD);
            MCEMessenger.sendInfoToPlayer("<gold>击杀奖励：+" + KILL_REWARD + "分！</gold>", killer);
        }
    }
    
    /**
     * 更新ActionBar显示
     */
    private static void updateActionBar() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            // 计算玩家当前加分速度并播放对应音效
            int currentScoringSpeed = calculatePlayerScoringSpeed(player);
            playSpeedBasedSound(player, currentScoringSpeed);
            
            String scorePercentage = calculateTeamScorePercentage(player);
            Component actionBarMessage = MiniMessage.miniMessage().deserialize(
                "<gold>目前队伍占点得分占比：" + scorePercentage + "</gold>"
            );
            player.sendActionBar(actionBarMessage);
        }
    }
    
    /**
     * 计算玩家当前的加分速度
     */
    private static int calculatePlayerScoringSpeed(Player player) {
        if (player.getGameMode() != GameMode.ADVENTURE) return 0;
        
        Location loc = player.getLocation();
        Team playerTeam = MCETeamUtils.getTeam(player);
        if (playerTeam == null) return 0;
        
        return calculateCapturePoints(loc);
    }
    
    /**
     * 根据加分速度播放不同音调的提示音
     */
    private static void playSpeedBasedSound(Player player, int scoringSpeed) {
        if (scoringSpeed <= 0) return; // 没有得分时不播放音效
        
        switch (scoringSpeed) {
            case 1:
                // 低音调 - 1分/tick
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.2f, 0.5f);
                break;
            case 2:
                // 中音调 - 2分/tick
                player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_CHIME, 0.2f, 0.75f);
                break;
            case 3:
                // 高音调 - 3分/tick
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.2f, 1.0f);
                break;
        }
    }
    
    /**
     * 计算队伍得分占比
     */
    private static String calculateTeamScorePercentage(Player player) {
        // 确保队伍分数已初始化
        if (teamScores.isEmpty() && captureCenter.getActiveTeams() != null) {
            initializeTeamScores();
        }
        
        Team playerTeam = MCETeamUtils.getTeam(player);
        if (playerTeam == null) return "0%";
        
        int totalScore = teamScores.values().stream().mapToInt(Integer::intValue).sum();
        if (totalScore == 0) return "0%";
        
        int teamScore = teamScores.getOrDefault(playerTeam.getName(), 0);
        double percentage = ((double) teamScore / totalScore) * 100;
        
        return String.format("%.1f%%", percentage);
    }
    
    /**
     * 开始平台收缩
     */
    public static void startPlatformShrinking(CaptureCenter game) {
        // 30秒后开始第一层收缩，之后每30秒收缩一层，直到剩余60秒时停止
        scheduleLayerShrinking(game, BOTTOM_LAYER_Y, 0); // 立即开始第一层
        scheduleLayerShrinking(game, BOTTOM_LAYER_Y + 1, 30); // 30秒后第二层
        scheduleLayerShrinking(game, BOTTOM_LAYER_Y + 2, 60); // 60秒后第三层
        scheduleLayerShrinking(game, BOTTOM_LAYER_Y + 3, 90); // 90秒后第四层
        // 游戏剩余60秒时停止收缩（总时长180秒，所以120秒后不再收缩）
    }
    
    /**
     * 安排层级收缩
     */
    private static void scheduleLayerShrinking(CaptureCenter game, int layerY, int delaySeconds) {
        game.getGameTask().add(MCETimerUtils.setDelayedTask(delaySeconds, () -> {
            // 显示预警的同时开始收缩平台
            int layerNumber = layerY - BOTTOM_LAYER_Y + 1;
            MCEMessenger.sendGlobalTitle(
                "<red><bold>第" + layerNumber + "层平台正在收缩！</bold></red>", 
                ""
            );
            
            // 立即开始收缩平台（显示标题的同时）
            shrinkLayer(layerY, game);
        }));
    }
    
    /**
     * 收缩指定层级
     */
    private static void shrinkLayer(int layerY, CaptureCenter game) {
        World world = Bukkit.getWorld(captureCenter.getWorldName());
        if (world == null) return;
        
        // 让方块交替变换为海晶灯作为预警，5秒后消失
        flashLayerWarning(world, layerY, game);
    }
    
    /**
     * 闪烁层级警告
     */
    private static void flashLayerWarning(World world, int layerY, CaptureCenter game) {
        // 存储原方块类型
        Map<Location, org.bukkit.block.data.BlockData> originalBlocks = new HashMap<>();
        
        // 收集所有非空气方块的原始状态
        for (int x = MAP_MIN_X; x <= MAP_MAX_X; x++) {
            for (int z = MAP_MIN_Z; z <= MAP_MAX_Z; z++) {
                Location loc = new Location(world, x, layerY, z);
                org.bukkit.block.Block block = world.getBlockAt(loc);
                if (block.getType() != Material.AIR) {
                    originalBlocks.put(loc.clone(), block.getBlockData().clone());
                }
            }
        }
        
        new BukkitRunnable() {
            private int flashes = 0;
            
            @Override
            public void run() {
                boolean useSealantern = flashes % 2 == 0;
                
                // 在海晶灯和原方块之间交替
                for (Map.Entry<Location, org.bukkit.block.data.BlockData> entry : originalBlocks.entrySet()) {
                    Location loc = entry.getKey();
                    org.bukkit.block.data.BlockData originalBlockData = entry.getValue();
                    org.bukkit.block.Block block = world.getBlockAt(loc);
                    
                    if (useSealantern) {
                        block.setType(Material.SEA_LANTERN);
                    } else {
                        block.setBlockData(originalBlockData);
                    }
                }
                
                flashes++;
                if (flashes >= 10) { // 闪烁5次后结束
                    this.cancel();
                    // 闪烁结束后移除该层
                    game.setDelayedTask(0.05, () -> {
                        removeLayer(world, layerY);
                        shrunkLayers.add(layerY);
                        
                        // 根据预定义的顺序更新击退等级
                        knockbackLevelIndex++;
                        if (knockbackLevelIndex < KNOCKBACK_LEVELS.length) {
                            currentKnockbackLevel = KNOCKBACK_LEVELS[knockbackLevelIndex];
                            // 给予新等级的击退棒
                            giveKnockbackStick(currentKnockbackLevel);
                        } else {
                            // 击退等级耗尽，清除所有击退棒
                            currentKnockbackLevel = 0;
                            for (Player player : Bukkit.getOnlinePlayers()) {
                                if (player.getGameMode() == GameMode.ADVENTURE) {
                                    player.getInventory().clear();
                                }
                            }
                        }
                    });
                }
            }
        }.runTaskTimer(plugin, 0L, 10L); // 每0.5秒切换一次
    }
    
    /**
     * 移除指定层级的方块
     */
    private static void removeLayer(World world, int layerY) {
        for (int x = MAP_MIN_X; x <= MAP_MAX_X; x++) {
            for (int z = MAP_MIN_Z; z <= MAP_MAX_Z; z++) {
                Location loc = new Location(world, x, layerY, z);
                world.getBlockAt(loc).setType(Material.AIR);
            }
        }
    }
    
    /**
     * 清理游戏任务
     */
    public static void clearGameTasks(CaptureCenter game) {
        for (BukkitRunnable task : game.getGameTask()) {
            task.cancel();
        }
        game.getGameTask().clear();
        
        // 停止占点任务
        if (scoringTask != null) {
            scoringTask.cancel();
            scoringTask = null;
        }
        
        if (actionBarTask != null) {
            actionBarTask.cancel();
            actionBarTask = null;
        }
        
        if (boardUpdateTask != null) {
            boardUpdateTask.cancel();
            boardUpdateTask = null;
        }
        
        scoringEnabled = false;
    }
    
    /**
     * 发送获胜消息
     */
    public static void sendWinningMessage() {
        // 首先发送最后存活玩家信息
        List<Player> survivingPlayers = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getGameMode() == GameMode.ADVENTURE) {
                survivingPlayers.add(player);
            }
        }
        
        if (!survivingPlayers.isEmpty()) {
            StringBuilder survivorMessage = new StringBuilder();
            for (int i = 0; i < survivingPlayers.size(); i++) {
                survivorMessage.append(MCEPlayerUtils.getColoredPlayerName(survivingPlayers.get(i)));
                if (i == survivingPlayers.size() - 2 && survivingPlayers.size() > 1) {
                    // 倒数第二个玩家，添加"和"
                    survivorMessage.append("和");
                } else if (i < survivingPlayers.size() - 1) {
                    // 不是最后一个玩家，添加逗号
                    survivorMessage.append(", ");
                }
            }
            survivorMessage.append(" <aqua>是最后存活的玩家！</aqua>");
            MCEMessenger.sendGlobalInfo(survivorMessage.toString());
        }
        
        if (teamScores.isEmpty()) return;
        
        // 按分数排序队伍
        List<Map.Entry<String, Integer>> sortedTeams = teamScores.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .toList();
        
        StringBuilder message = new StringBuilder();
        message.append("<gold><bold>最终排名：</bold></gold><newline>");
        
        for (int i = 0; i < sortedTeams.size(); i++) {
            Map.Entry<String, Integer> entry = sortedTeams.get(i);
            String teamName = entry.getKey();
            int score = entry.getValue();
            
            message.append("<yellow>第").append(i + 1).append("名：</yellow>")
                   .append("<aqua>").append(teamName).append("队</aqua>")
                   .append("<white> - ").append(score).append("分</white>");
            
            if (i < sortedTeams.size() - 1) {
                message.append("<newline>");
            }
        }
        
        MCEMessenger.sendGlobalInfo(message.toString());
    }
    
    /**
     * 处理玩家掉落虚空
     */
    public static void handlePlayerFallIntoVoid(Player player) {
        // 发送死亡信息
        MCEMessenger.sendGlobalInfo(
            MCEPlayerUtils.getColoredPlayerName(player) + " <gray>掉入了虚空！</gray>"
        );
        
        // 将玩家切换为旁观模式
        player.setGameMode(GameMode.SPECTATOR);
        player.setHealth(player.getMaxHealth());
        
        // 更新计分板
        CaptureCenterGameBoard gameBoard = (CaptureCenterGameBoard) captureCenter.getGameBoard();
        
        // 统计剩余玩家数
        int alivePlayerCount = 0;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getGameMode() == GameMode.ADVENTURE) {
                alivePlayerCount++;
            }
        }
        gameBoard.updatePlayerCount(alivePlayerCount);
        
        // 统计剩余队伍数
        Set<String> aliveTeams = new HashSet<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getGameMode() == GameMode.ADVENTURE) {
                Team playerTeam = MCETeamUtils.getTeam(p);
                if (playerTeam != null) {
                    aliveTeams.add(playerTeam.getName());
                }
            }
        }
        gameBoard.updateTeamCount(aliveTeams.size());
        
        // 检查游戏是否结束
        if (alivePlayerCount == 0 || aliveTeams.size() <= 1) {
            captureCenter.getTimeline().nextState();
        }
    }
}