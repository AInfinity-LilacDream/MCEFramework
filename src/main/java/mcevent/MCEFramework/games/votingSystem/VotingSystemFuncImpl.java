package mcevent.MCEFramework.games.votingSystem;

import mcevent.MCEFramework.MCEMainController;
import mcevent.MCEFramework.games.votingSystem.gameObject.VotingGUI;
import mcevent.MCEFramework.tools.MCEMessenger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static mcevent.MCEFramework.miscellaneous.Constants.*;

/**
 * VotingSystemFuncImpl: 投票系统功能实现
 */
public class VotingSystemFuncImpl {
    
    // 投票统计 - 游戏ID -> 票数
    private static final HashMap<Integer, Integer> votes = new HashMap<>();
    
    // 玩家是否已投票 - 玩家UUID -> 是否已投票
    private static final HashMap<String, Boolean> hasVoted = new HashMap<>();
    
    // 投票是否已初始化
    private static boolean isVotingInitialized = false;
    
    // 是否跳过Intro
    private static boolean skipIntro = false;
    
    // 游戏名称映射
    private static final String[] gameNames = {
        "瓮中捉鳖", "色盲狂热", "跃动音律", "落沙漫步", "占山为王", "少林足球"
    };

    /**
     * 加载配置
     */
    public static void loadConfig() {
        // 投票系统的配置加载逻辑（如果需要的话）
        plugin.getLogger().info("投票系统配置加载完成");
    }

    /**
     * 重置游戏板
     */
    public static void resetGameBoard() {
        // 重置投票相关的游戏板信息
    }

    /**
     * 初始化投票
     */
    public static void initializeVoting() {
        // 清空投票记录
        votes.clear();
        hasVoted.clear();
        
        // 初始化所有游戏的投票数为0
        for (int i = 0; i < 6; i++) {
            votes.put(i, 0);
        }
        
        // 清空所有玩家的投票状态
        for (Player player : Bukkit.getOnlinePlayers()) {
            hasVoted.put(player.getUniqueId().toString(), false);
        }
        
        isVotingInitialized = true;
        skipIntro = false; // 重置跳过Intro状态
    }
    
    /**
     * 检查投票是否已初始化，如果没有则初始化
     */
    public static void ensureVotingInitialized() {
        if (!isVotingInitialized) {
            initializeVoting();
        }
    }
    
    /**
     * 切换跳过Intro状态
     */
    public static void toggleSkipIntro() {
        skipIntro = !skipIntro;
    }
    
    /**
     * 获取跳过Intro状态
     */
    public static boolean isSkipIntro() {
        return skipIntro;
    }

    /**
     * 玩家投票
     */
    public static boolean vote(Player player, int gameId) {
        String playerUUID = player.getUniqueId().toString();
        
        // 检查玩家是否已经投票
        if (hasVoted.getOrDefault(playerUUID, false)) {
            MCEMessenger.sendInfoToPlayer("<red>您已经投过票了！", player);
            return false; // 已经投票
        }
        
        // 记录投票
        hasVoted.put(playerUUID, true);
        votes.put(gameId, votes.getOrDefault(gameId, 0) + 1);
        
        // 发送投票成功消息
        String gameName = gameNames[gameId];
        MCEMessenger.sendInfoToPlayer("<green>您已为 <yellow>" + gameName + " <green>投票！", player);
        
        // 刷新所有打开的投票GUI
        VotingGUI.refreshAllVotingGUIs();
        
        return true;
    }

    /**
     * 获取某个游戏的票数
     */
    public static int getVotes(int gameId) {
        return votes.getOrDefault(gameId, 0);
    }

    /**
     * 处理投票结果并启动获胜游戏
     */
    public static void processVotingResults() {
        // 调试：输出投票统计
        plugin.getLogger().info("投票统计:");
        for (Map.Entry<Integer, Integer> entry : votes.entrySet()) {
            plugin.getLogger().info("游戏 " + entry.getKey() + " (" + gameNames[entry.getKey()] + "): " + entry.getValue() + " 票");
        }
        
        // 找到得票最多的游戏
        int winningGameId = -1;
        int maxVotes = -1;
        
        for (Map.Entry<Integer, Integer> entry : votes.entrySet()) {
            if (entry.getValue() > maxVotes) {
                maxVotes = entry.getValue();
                winningGameId = entry.getKey();
            }
        }
        
        plugin.getLogger().info("最大票数: " + maxVotes + ", 获胜游戏ID: " + winningGameId);
        
        // 处理没有投票或平票的情况
        if (winningGameId == -1 || maxVotes == 0 || hasTie(maxVotes)) {
            // 随机选择一个游戏
            Random random = new Random();
            winningGameId = random.nextInt(6);
            
            String reason = (maxVotes == 0) ? "无人投票" : "平票";
            MCEMessenger.sendGlobalTitle("<gold><bold>" + reason + "！</bold></gold>", 
                                       "<yellow>随机选择: " + gameNames[winningGameId] + "</yellow>");
        } else {
            MCEMessenger.sendGlobalTitle("<gold><bold>投票结果</bold></gold>", 
                                       "<yellow>获胜游戏: " + gameNames[winningGameId] + "</yellow>");
        }
        
        // 等待5秒后启动游戏（确保onEnd阶段完成）
        int finalWinningGameId = winningGameId;
        boolean finalSkipIntro = skipIntro; // 保存skipIntro状态
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            MCEMainController.immediateLaunchGame(finalWinningGameId, !finalSkipIntro); // skipIntro为true时，intro参数为false
        }, 100L); // 5秒 = 100 ticks
    }

    /**
     * 检查是否存在平票
     */
    private static boolean hasTie(int maxVotes) {
        int gamesWithMaxVotes = 0;
        for (int voteCount : votes.values()) {
            if (voteCount == maxVotes) {
                gamesWithMaxVotes++;
            }
        }
        plugin.getLogger().info("平票检查: 最大票数=" + maxVotes + ", 有" + gamesWithMaxVotes + "个游戏得到最大票数");
        return gamesWithMaxVotes > 1;
    }

    /**
     * 获取投票统计信息
     */
    public static String getVotingStats() {
        StringBuilder stats = new StringBuilder("§6投票统计:\n");
        for (int i = 0; i < 6; i++) {
            stats.append("§e").append(gameNames[i]).append(": §f")
                 .append(votes.getOrDefault(i, 0)).append("票\n");
        }
        return stats.toString();
    }

    /**
     * 清空投票数据
     */
    public static void clearVotingData() {
        votes.clear();
        hasVoted.clear();
        isVotingInitialized = false;
        skipIntro = false;
    }
}