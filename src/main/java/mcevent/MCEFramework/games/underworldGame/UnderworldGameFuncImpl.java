package mcevent.MCEFramework.games.underworldGame;

import mcevent.MCEFramework.tools.MCEMessenger;
import mcevent.MCEFramework.tools.MCEPlayerUtils;
import mcevent.MCEFramework.tools.MCETimerUtils;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static mcevent.MCEFramework.miscellaneous.Constants.*;

/*
UnderworldGameFuncImpl: 封装阴间游戏逻辑函数
 */
public class UnderworldGameFuncImpl {

    private static final UnderworldGameConfigParser configParser = underworldGame.getConfigParser();
    private static BukkitRunnable swapTask = null;
    private static int nextSwapTime = 0;

    /**
     * 从配置文件加载数据
     */
    public static void loadConfig() {
        underworldGame.setIntroTextList(configParser.openAndParse(underworldGame.getConfigFileName()));
    }

    /**
     * 生成新世界
     */
    public static String generateNewWorld() {
        // 生成唯一的世界名称
        String worldName = "underworld_" + System.currentTimeMillis();
        
        // 使用Bukkit API创建新世界
        WorldCreator creator = new WorldCreator(worldName);
        creator.type(WorldType.NORMAL);
        creator.environment(World.Environment.NORMAL);
        creator.generateStructures(true);
        
        World world = creator.createWorld();
        if (world != null) {
            // 设置世界出生点（保持在地面，不设置偏移）
            Location spawnLoc = world.getSpawnLocation();
            world.setSpawnLocation((int)spawnLoc.getX(), (int)spawnLoc.getY(), (int)spawnLoc.getZ());
            
            plugin.getLogger().info("已生成新世界: " + worldName);
            return worldName;
        }
        
        plugin.getLogger().warning("无法生成新世界，使用默认世界");
        return "world";
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
     * 锁定玩家在高空位置
     */
    public static void lockPlayersInSky() {
        World world = Bukkit.getWorld(underworldGame.getGeneratedWorldName());
        if (world == null) return;
        
        Location spawnLoc = world.getSpawnLocation();
        double skyY = spawnLoc.getY() + 50; // 高空50格
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getWorld().getName().equals(underworldGame.getGeneratedWorldName())) {
                Location skyLoc = new Location(world, spawnLoc.getX(), skyY, spawnLoc.getZ());
                player.teleport(skyLoc);
                
                // 禁用移动
                Objects.requireNonNull(player.getAttribute(Attribute.MOVEMENT_SPEED)).setBaseValue(0);
                Objects.requireNonNull(player.getAttribute(Attribute.JUMP_STRENGTH)).setBaseValue(0);
            }
        }
    }

    /**
     * 让玩家从高处掉落
     */
    public static void dropPlayersFromSky() {
        World world = Bukkit.getWorld(underworldGame.getGeneratedWorldName());
        if (world == null) return;
        
        Location spawnLoc = world.getSpawnLocation();
        double skyY = spawnLoc.getY() + 50; // 高空50格
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getWorld().getName().equals(underworldGame.getGeneratedWorldName()) &&
                    player.getScoreboardTags().contains("Participant")) {
                // 随机分散位置
                Random random = ThreadLocalRandom.current();
                double offsetX = (random.nextDouble() - 0.5) * 50; // -25 到 25
                double offsetZ = (random.nextDouble() - 0.5) * 50;
                
                Location dropLoc = new Location(world, 
                        spawnLoc.getX() + offsetX, 
                        skyY, 
                        spawnLoc.getZ() + offsetZ);
                player.teleport(dropLoc);
            }
        }
    }

    /**
     * 启用玩家移动
     */
    public static void enablePlayerMovement() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getWorld().getName().equals(underworldGame.getGeneratedWorldName())) {
                Objects.requireNonNull(player.getAttribute(Attribute.MOVEMENT_SPEED)).setBaseValue(0.1);
                Objects.requireNonNull(player.getAttribute(Attribute.JUMP_STRENGTH)).setBaseValue(0.42);
            }
        }
    }

    /**
     * 启动玩家交换系统
     */
    public static void startPlayerSwapSystem() {
        // 取消之前的任务
        if (swapTask != null && !swapTask.isCancelled()) {
            swapTask.cancel();
        }
        
        // 生成第一次交换时间（15-120秒）
        scheduleNextSwap();
    }

    /**
     * 安排下一次交换
     */
    private static void scheduleNextSwap() {
        Random random = ThreadLocalRandom.current();
        nextSwapTime = 15 + random.nextInt(106); // 15-120秒
        
        plugin.getLogger().info("下一次交换将在 " + nextSwapTime + " 秒后发生");
        
        // 如果交换时间大于10秒，在剩余10秒时提示
        if (nextSwapTime > 10) {
            int warningTime = nextSwapTime - 10;
            MCETimerUtils.setDelayedTask(warningTime, () -> {
                // 检查游戏是否还在运行
                if (!mcevent.MCEFramework.MCEMainController.isRunningGame() ||
                    mcevent.MCEFramework.MCEMainController.getCurrentRunningGame() != underworldGame) {
                    return;
                }
                MCEMessenger.sendGlobalInfo("<yellow><bold>⚠ 10秒后将交换位置！</bold></yellow>");
                // 播放音效
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.getWorld().getName().equals(underworldGame.getGeneratedWorldName())) {
                        player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.0f);
                    }
                }
            });
        }
        
        // 在 nextSwapTime 秒后执行交换
        swapTask = MCETimerUtils.setDelayedTask(nextSwapTime, () -> {
            // 检查游戏是否还在运行
            if (!mcevent.MCEFramework.MCEMainController.isRunningGame() ||
                mcevent.MCEFramework.MCEMainController.getCurrentRunningGame() != underworldGame) {
                return;
            }
            performPlayerSwap();
            // 安排下一次交换
            scheduleNextSwap();
        });
    }

    /**
     * 执行玩家位置交换
     * 确保每个玩家都与其他玩家交换位置，避免玩家和自己交换
     */
    private static void performPlayerSwap() {
        // 获取所有存活的玩家
        List<Player> alivePlayers = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getWorld().getName().equals(underworldGame.getGeneratedWorldName()) &&
                    player.getScoreboardTags().contains("Participant") &&
                    !player.getScoreboardTags().contains("dead") &&
                    player.getGameMode() == GameMode.SURVIVAL) {
                alivePlayers.add(player);
            }
        }
        
        if (alivePlayers.size() < 2) {
            // 玩家数量不足，不交换
            plugin.getLogger().info("[UnderworldGame][SwapDebug] 玩家数量不足，不交换。存活玩家数: " + alivePlayers.size());
            return;
        }
        
        // 保存所有玩家的位置
        Map<Player, Location> playerLocations = new HashMap<>();
        for (Player player : alivePlayers) {
            playerLocations.put(player, player.getLocation().clone());
        }
        
        // 使用 Fisher-Yates 洗牌算法创建循环交换映射
        // 确保每个玩家都被分配到其他玩家的位置
        List<Player> shuffledPlayers = new ArrayList<>(alivePlayers);
        Collections.shuffle(shuffledPlayers, ThreadLocalRandom.current());
        
        // 检查是否有玩家被分配到自己原来的位置（这种情况在洗牌后可能发生）
        // 如果有，进行修正以确保每个玩家都与其他玩家交换
        boolean hasSelfSwap = false;
        for (int i = 0; i < alivePlayers.size(); i++) {
            if (alivePlayers.get(i).equals(shuffledPlayers.get(i))) {
                hasSelfSwap = true;
                break;
            }
        }
        
        // 如果有玩家被分配到自己原来的位置，进行修正
        if (hasSelfSwap) {
            plugin.getLogger().info("[UnderworldGame][SwapDebug] 检测到玩家可能与自己交换，进行修正...");
            // 使用循环交换：每个玩家都得到下一个玩家的位置
            // 例如：A->B, B->C, C->A
            Player first = shuffledPlayers.get(0);
            for (int i = 0; i < shuffledPlayers.size() - 1; i++) {
                shuffledPlayers.set(i, shuffledPlayers.get(i + 1));
            }
            shuffledPlayers.set(shuffledPlayers.size() - 1, first);
        }
        
        // 执行位置交换
        Map<Player, Location> swapMap = new HashMap<>();
        for (int i = 0; i < alivePlayers.size(); i++) {
            Player originalPlayer = alivePlayers.get(i);
            Player targetPlayer = shuffledPlayers.get(i);
            swapMap.put(originalPlayer, playerLocations.get(targetPlayer));
            
            plugin.getLogger().info("[UnderworldGame][SwapDebug] 玩家 " + originalPlayer.getName() + 
                " 将被传送到玩家 " + targetPlayer.getName() + " 的位置");
        }
        
        // 执行传送
        for (Map.Entry<Player, Location> entry : swapMap.entrySet()) {
            entry.getKey().teleport(entry.getValue());
        }
        
        MCEMessenger.sendGlobalInfo("<red><bold>位置已交换！</bold></red>");
        
        // 播放交换音效
        for (Player player : alivePlayers) {
            player.playSound(player.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
        }
    }

    /**
     * 发送获胜消息
     */
    public static void sendWinningMessage() {
        List<Player> alivePlayers = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getWorld().getName().equals(underworldGame.getGeneratedWorldName()) &&
                    player.getScoreboardTags().contains("Participant") &&
                    !player.getScoreboardTags().contains("dead") &&
                    player.getGameMode() == GameMode.SURVIVAL) {
                alivePlayers.add(player);
            }
        }

        if (!alivePlayers.isEmpty()) {
            StringBuilder message = new StringBuilder();
            for (int i = 0; i < alivePlayers.size(); i++) {
                if (i > 0)
                    message.append("<aqua>, </aqua>");
                message.append(MCEPlayerUtils.getColoredPlayerName(alivePlayers.get(i)));
            }
            message.append("<aqua><bold>是最后存活的玩家！</bold></aqua>");
            MCEMessenger.sendGlobalInfo(message.toString());
        }
    }

    /**
     * 停止交换系统
     */
    public static void stopSwapSystem() {
        if (swapTask != null && !swapTask.isCancelled()) {
            swapTask.cancel();
            swapTask = null;
        }
    }
}

