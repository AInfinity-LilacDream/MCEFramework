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

    private static final CaptureCenterConfigParser captureCenterConfigParser = captureCenter
            .getCaptureCenterConfigParser();
    private static final Map<String, Integer> teamScores = new ConcurrentHashMap<>();
    private static boolean scoringEnabled = false;
    private static BukkitRunnable scoringTask;
    private static BukkitRunnable actionBarTask;
    private static BukkitRunnable boardUpdateTask;
    private static final Set<Integer> shrunkLayers = new HashSet<>();
    private static final List<BukkitRunnable> flashTasks = new ArrayList<>(); // 跟踪所有闪烁任务
    private static int currentKnockbackLevel = 3; // 保留字段以兼容旧调用

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
    private static final int TOP_LAYER_Y = -55; // 最上层

    // 地图范围常量（与地图复制区域一致）
    private static final int MAP_MIN_X = -17;
    private static final int MAP_MAX_X = 33;
    private static final int MAP_MIN_Z = -17;
    private static final int MAP_MAX_Z = 33;
    private static final int MAP_MIN_Y = -61;
    private static final int MAP_MAX_Y = -19;

    // 击退半径映射（中心与阈值）
    private static final double KB_CENTER_X = 8.5;
    private static final double KB_CENTER_Z = 8.5;
    private static final double KB_RADIUS_L12 = 2.5; // 第1-2层→击退3
    private static final double KB_RADIUS_L3 = 5.5; // 第3层→击退2
    private static final double KB_RADIUS_L4567 = 15.5; // 第4-7层→击退1

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
                if (team == null)
                    continue;
                String name = team.getName();
                if (name == null)
                    continue;
                teamScores.put(name, 0);
            }
        }
    }

    /**
     * 恢复地图原状
     */
    public static void resetMap() {
        World targetWorld = Bukkit.getWorld(captureCenter.getWorldName()); // capture_classic
        World sourceWorld = Bukkit.getWorld("capture_classic_original"); // 原版地图

        if (targetWorld == null || sourceWorld == null) {
            plugin.getLogger()
                    .warning("无法找到地图世界！target: " + captureCenter.getWorldName() + ", source: capture_classic_original");
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
                                String.format("%.1f", (double) blocksProcessed / totalBlocks * 100) + "%)");
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
        if (world == null)
            return;

        Location spawnLocation = new Location(world, CAPTURE_CENTER_X, SPAWN_Y, CAPTURE_CENTER_Z);
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getScoreboardTags().contains("Participant")) {
                player.teleport(spawnLocation);
            }
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
     */
    public static void removeStartPlatform() {
        World world = Bukkit.getWorld(captureCenter.getWorldName());
        if (world == null)
            return;

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
     * 给予玩家击退棒（按半径映射）
     */
    public static void giveKnockbackStick() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getGameMode() == GameMode.SURVIVAL && player.getScoreboardTags().contains("Participant")) {
                player.getInventory().clear();
                int level = getKnockbackLevelForLocation(player.getLocation());
                giveKnockbackStickToPlayer(player, level);
            }
        }
    }

    /**
     * 兼容接口：为所有玩家发放指定等级的击退棒
     */
    public static void giveKnockbackStick(int knockbackLevel) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getGameMode() == GameMode.SURVIVAL && player.getScoreboardTags().contains("Participant")) {
                player.getInventory().clear();
                giveKnockbackStickToPlayer(player, Math.max(0, knockbackLevel));
            }
        }
    }

    private static void giveKnockbackStickToPlayer(Player player, int knockbackLevel) {
        if (knockbackLevel <= 0)
            return;
        ItemStack knockbackStick = new ItemStack(Material.STICK);
        ItemMeta meta = knockbackStick.getItemMeta();
        if (meta != null) {
            meta.displayName(MiniMessage.miniMessage()
                    .deserialize("<gold><bold>击退棒（击退" + knockbackLevel + "）</bold></gold>"));
            knockbackStick.setItemMeta(meta);
            knockbackStick.addUnsafeEnchantment(Enchantment.KNOCKBACK, knockbackLevel);
        }
        ItemStack cur = player.getInventory().getItem(0);
        if (cur == null || cur.getType() == Material.AIR) {
            player.getInventory().setItem(0, knockbackStick);
        } else {
            player.getInventory().addItem(knockbackStick);
        }
    }

    private static int getKnockbackLevelForLocation(Location loc) {
        double dx = loc.getX() - KB_CENTER_X;
        double dz = loc.getZ() - KB_CENTER_Z;
        double r = Math.sqrt(dx * dx + dz * dz);
        if (r <= KB_RADIUS_L12)
            return 3; // 第1、2层
        if (r <= KB_RADIUS_L3)
            return 2; // 第3层
        if (r <= KB_RADIUS_L4567)
            return 1; // 第4-7层
        return 1; // 半径外默认最低等级
    }

    /**
     * 重置游戏计分板
     */
    public static void resetGameBoard() {
        CaptureCenterGameBoard gameBoard = (CaptureCenterGameBoard) captureCenter.getGameBoard();
        gameBoard.updatePlayerCount(0);
        gameBoard.updateTeamCount(0);

        // 击退等级由玩家所在半径决定
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
                // 实时校准击退棒等级（玩家移动半径时）
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getGameMode() != GameMode.SURVIVAL || !p.getScoreboardTags().contains("Participant"))
                        continue;
                    int expected = getKnockbackLevelForLocation(p.getLocation());
                    ItemStack held = p.getInventory().getItem(0);
                    int cur = 0;
                    if (held != null && held.getType() == Material.STICK && held.hasItemMeta()
                            && held.getEnchantments().containsKey(Enchantment.KNOCKBACK)) {
                        cur = held.getEnchantmentLevel(Enchantment.KNOCKBACK);
                    }
                    if (cur != expected) {
                        p.getInventory().setItem(0, null);
                        giveKnockbackStickToPlayer(p, expected);
                    }
                }
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
        if (!scoringEnabled)
            return;

        // 确保队伍分数已初始化
        if (teamScores.isEmpty() && captureCenter.getActiveTeams() != null) {
            initializeTeamScores();
        }

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getGameMode() != GameMode.SURVIVAL)
                continue;

            Location loc = player.getLocation();
            Team playerTeam = MCETeamUtils.getTeam(player);
            if (playerTeam == null)
                continue;

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
        double distance = Math
                .sqrt(Math.pow(loc.getX() - CAPTURE_CENTER_X, 2) + Math.pow(loc.getZ() - CAPTURE_CENTER_Z, 2));
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
        if (killer == null)
            return;

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
                    "<gold>目前队伍占点得分占比：" + scorePercentage + "</gold>");
            player.sendActionBar(actionBarMessage);
        }
    }

    /**
     * 计算玩家当前的加分速度
     */
    private static int calculatePlayerScoringSpeed(Player player) {
        if (player.getGameMode() != GameMode.SURVIVAL)
            return 0;

        Location loc = player.getLocation();
        Team playerTeam = MCETeamUtils.getTeam(player);
        if (playerTeam == null)
            return 0;

        return calculateCapturePoints(loc);
    }

    /**
     * 根据加分速度播放不同音调的提示音
     */
    private static void playSpeedBasedSound(Player player, int scoringSpeed) {
        if (scoringSpeed <= 0)
            return; // 没有得分时不播放音效

        switch (scoringSpeed) {
            case 1:
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 0.2f, 0.5f);
                break;
            case 2:
                player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_NOTE_BLOCK_CHIME, 0.2f, 0.75f);
                break;
            case 3:
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
        if (playerTeam == null)
            return "0%";

        int totalScore = teamScores.values().stream().mapToInt(Integer::intValue).sum();
        if (totalScore == 0)
            return "0%";

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
            int layerNumber = layerY - BOTTOM_LAYER_Y + 1;
            MCEMessenger.sendGlobalTitle(
                    "<red><bold>第" + layerNumber + "层平台正在收缩！</bold></red>",
                    "");
            shrinkLayer(layerY, game);
        }));
    }

    /**
     * 收缩指定层级
     */
    private static void shrinkLayer(int layerY, CaptureCenter game) {
        World world = Bukkit.getWorld(captureCenter.getWorldName());
        if (world == null)
            return;
        flashLayerWarning(world, layerY, game);
    }

    /**
     * 闪烁层级警告
     */
    private static void flashLayerWarning(World world, int layerY, CaptureCenter game) {
        Map<Location, org.bukkit.block.data.BlockData> originalBlocks = new HashMap<>();
        for (int x = MAP_MIN_X; x <= MAP_MAX_X; x++) {
            for (int z = MAP_MIN_Z; z <= MAP_MAX_Z; z++) {
                Location loc = new Location(world, x, layerY, z);
                org.bukkit.block.Block block = world.getBlockAt(loc);
                if (block.getType() != Material.AIR) {
                    originalBlocks.put(loc.clone(), block.getBlockData().clone());
                }
            }
        }

        BukkitRunnable flashTask = new BukkitRunnable() {
            private int flashes = 0;

            @Override
            public void run() {
                boolean useSealantern = flashes % 2 == 0;
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
                if (flashes >= 10) {
                    this.cancel();
                    flashTasks.remove(this);
                    game.setDelayedTask(0.05, () -> {
                        removeLayer(world, layerY);
                        shrunkLayers.add(layerY);
                        // 不再根据缩圈改变击退等级
                    });
                }
            }
        };
        flashTasks.add(flashTask);
        flashTask.runTaskTimer(plugin, 0L, 10L);
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

        // 停止所有闪烁任务
        for (BukkitRunnable flashTask : flashTasks) {
            if (flashTask != null && !flashTask.isCancelled()) {
                flashTask.cancel();
            }
        }
        flashTasks.clear();

        scoringEnabled = false;
    }

    /**
     * 发送获胜消息
     */
    public static void sendWinningMessage() {
        if (teamScores.isEmpty()) return;
        MCEMessenger.sendGlobalText("<newline><yellow><bold>=== 占山为王 结果统计 ===</bold></yellow>");
        List<Map.Entry<String, Integer>> sortedTeams = teamScores.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed()).toList();
        MCEMessenger.sendGlobalText("<newline><red><bold>📊 排行榜：</bold></red><newline>");
        for (int i = 0; i < 5 && i < sortedTeams.size(); i++) {
            Map.Entry<String, Integer> entry = sortedTeams.get(i);
            String teamName = entry.getKey();
            int score = entry.getValue();
            MCEMessenger.sendGlobalText("<red>" + number2OrdinalString(i + 1) + "</red> " + teamName + " <gray>-</gray><red> " + score + " 分</red>");
        }
        MCETeamUtils.getActiveTeams().forEach(team -> {
            var name = team.getName();
            for (int i = 0; i < sortedTeams.size(); i++) {
                if (sortedTeams.get(i).getKey().equals(name)) {
                    var rank = i + 1;
                    MCETeamUtils.getPlayers(team).forEach(player -> {
                        player.sendMessage(MiniMessage.miniMessage().deserialize("<newline><bold><red>🥇 您的名次是：</red><gold>第 " + rank + " 名</gold></bold><newline>"));
                    });
                }
            }
        });
    }

    private static String number2OrdinalString(int n) {
        return switch (n) {
            case 1 -> "①";
            case 2 -> "②";
            case 3 -> "③";
            case 4 -> "④";
            case 5 -> "⑤";
            default -> "";
        };
    }

    /**
     * 处理玩家掉落虚空
     */
    public static void handlePlayerFallIntoVoid(Player player) {
        mcevent.MCEFramework.customHandler.GlobalEliminationHandler.eliminateNow(player);

        int alivePlayers = 0;
        java.util.Set<String> aliveTeamNames = new java.util.HashSet<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getGameMode() == GameMode.SURVIVAL) {
                alivePlayers++;
                Team pt = MCETeamUtils.getTeam(p);
                if (pt != null)
                    aliveTeamNames.add(pt.getName());
            }
        }
        if (alivePlayers == 0 || aliveTeamNames.size() <= 1) {
            captureCenter.getTimeline().nextState();
        }
    }
}