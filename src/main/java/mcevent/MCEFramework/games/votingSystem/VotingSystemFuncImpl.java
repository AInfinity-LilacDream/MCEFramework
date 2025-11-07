package mcevent.MCEFramework.games.votingSystem;

import mcevent.MCEFramework.MCEMainController;
import mcevent.MCEFramework.games.votingSystem.gameObject.VotingGUI;
import static mcevent.MCEFramework.miscellaneous.Constants.*;
import mcevent.MCEFramework.tools.MCEMessenger;
import mcevent.MCEFramework.tools.MCEPlayerUtils;
import mcevent.MCEFramework.tools.MCETeamUtils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.*;

import static mcevent.MCEFramework.miscellaneous.Constants.*;

/**
 * VotingSystemFuncImpl: 投票系统功能实现
 */
public class VotingSystemFuncImpl {

    // 投票统计 - 游戏ID -> 票数
    private static final HashMap<Integer, Integer> votes = new HashMap<>();

    // 玩家已投票的游戏 - 玩家UUID -> 游戏ID
    private static final HashMap<String, Integer> playerVotes = new HashMap<>();

    // 投票是否已初始化
    private static boolean isVotingInitialized = false;

    // 是否跳过Intro
    private static boolean skipIntro = false;

    // 游戏名称映射（包括ID 12）
    private static final String[] gameNames = {
            "瓮中捉鳖", "色盲狂热", "跃动音律", "落沙漫步", "占山为王", "少林足球", "惊天矿工团", "暗矢狂潮", "丢锅大战", "冰雪掘战", "饥饿游戏", "", "冰雪乱斗"
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
        playerVotes.clear();

        // 初始化所有游戏的投票数为0（包括ID 12）
        for (int i = 0; i < gameNames.length; i++) {
            if (i != 11 && !gameNames[i].isEmpty()) { // 跳过索引11（空位）和空名称
                votes.put(i, 0);
            }
        }

        // 清空所有玩家的投票记录
        playerVotes.clear();

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

        // 检查游戏ID是否有效（包括ID 12）
        if (gameId < 0 || gameId >= gameNames.length || gameNames[gameId].isEmpty()) {
            MCEMessenger.sendInfoToPlayer("<red>无效的游戏选择！", player);
            return false;
        }

        // 检查是否已经为同一个游戏投票
        if (playerVotes.containsKey(playerUUID) && playerVotes.get(playerUUID) == gameId) {
            MCEMessenger.sendInfoToPlayer("<yellow>您已经为 <gold>" + gameNames[gameId] + " <yellow>投过票了！", player);
            return false;
        }

        // 如果玩家之前投过其他游戏，先减去之前的票数
        if (playerVotes.containsKey(playerUUID)) {
            int previousGameId = playerVotes.get(playerUUID);
            int previousVotes = votes.getOrDefault(previousGameId, 0);
            if (previousVotes > 0) {
                votes.put(previousGameId, previousVotes - 1);
            }
        }

        // 记录新投票
        playerVotes.put(playerUUID, gameId);
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
        // 统计总投票数
        int totalVotes = 0;
        for (int voteCount : votes.values()) {
            totalVotes += voteCount;
        }

        // 调试：输出详细投票统计
        plugin.getLogger().info("========== 投票结果统计 ==========");
        plugin.getLogger().info("总投票数: " + totalVotes);
        plugin.getLogger().info("各游戏得票情况:");

        for (int gameId = 0; gameId < gameNames.length; gameId++) {
            if (gameNames[gameId].isEmpty())
                continue; // 跳过空名称
            int voteCount = votes.getOrDefault(gameId, 0);
            double percentage = totalVotes > 0 ? (double) voteCount / totalVotes * 100 : 0;
            plugin.getLogger().info(String.format("  %d. %-12s: %2d票 (%.1f%%)",
                    gameId, gameNames[gameId], voteCount, percentage));
        }

        // 检查无效的游戏ID
        for (Map.Entry<Integer, Integer> entry : votes.entrySet()) {
            int gameId = entry.getKey();
            if (gameId < 0 || gameId >= gameNames.length || gameNames[gameId].isEmpty()) {
                plugin.getLogger().warning("发现无效的游戏ID: " + gameId + " 得票: " + entry.getValue());
            }
        }

        plugin.getLogger().info("================================");

        // 找到得票最多的游戏
        int winningGameId = -1;
        int maxVotes = -1;

        for (Map.Entry<Integer, Integer> entry : votes.entrySet()) {
            if (entry.getValue() > maxVotes) {
                maxVotes = entry.getValue();
                winningGameId = entry.getKey();
            }
        }

        plugin.getLogger()
                .info("获胜游戏: "
                        + (winningGameId >= 0 && winningGameId < gameNames.length
                                ? gameNames[winningGameId] + " (ID:" + winningGameId + ")"
                                : "无效ID:" + winningGameId)
                        + ", 得票数: " + maxVotes);

        // 处理没有投票的情况 - 停止投票，不启动游戏
        if (maxVotes == 0) {
            MCEMessenger.sendGlobalTitle("<red><bold>无人投票！</bold></red>",
                    "<yellow>投票系统停止，请手动重启游戏</yellow>");
            plugin.getLogger().info("无人投票，投票系统停止");

            // 收回所有投票卡
            removeAllVotingCards();

            // 停止音乐播放
            MCEPlayerUtils.globalStopMusic();

            // 将游戏运行状态设置为false，允许重新启动投票
            MCEMainController.setRunningGame(false);
            MCEMainController.setCurrentRunningGame(null);

            // 启动欢迎标语动画
            MCEMainController.startWelcomeMessage();

            // 无人投票：为每位在线玩家发放风弹发射器与“前往Duel”指南针
            mcevent.MCEFramework.customHandler.LobbyItemHandler lih = mcevent.MCEFramework.MCEMainController
                    .getLobbyItemHandler();
            if (lih != null) {
                for (Player p : Bukkit.getOnlinePlayers()) {
                    lih.giveLobbyItems(p);
                }
            }

            // 打开全局PVP与友伤
            mcevent.MCEFramework.MCEMainController.getGlobalPVPHandler().suspend();
            mcevent.MCEFramework.MCEMainController.getFriendlyFireHandler().suspend();

            return; // 直接返回，不启动任何游戏
        }

        // 处理平票的情况
        if (winningGameId == -1 || hasTie(maxVotes)) {
            // 仅在平票的游戏中随机选择
            java.util.List<Integer> tieCandidates = new java.util.ArrayList<>();
            for (int i = 0; i < gameNames.length; i++) {
                if (!gameNames[i].isEmpty() && votes.getOrDefault(i, 0) == maxVotes) {
                    tieCandidates.add(i);
                }
            }

            if (!tieCandidates.isEmpty()) {
                Random random = new Random();
                winningGameId = tieCandidates.get(random.nextInt(tieCandidates.size()));
                MCEMessenger.sendGlobalTitle("<gold><bold>平票！</bold></gold>",
                        "<yellow>随机选择: " + gameNames[winningGameId] + "</yellow>");
            } else {
                // 理论上不会发生：降级为全局随机
                Random random = new Random();
                java.util.List<Integer> validGames = new java.util.ArrayList<>();
                for (int i = 0; i < gameNames.length; i++) {
                    if (!gameNames[i].isEmpty()) {
                        validGames.add(i);
                    }
                }
                if (!validGames.isEmpty()) {
                    winningGameId = validGames.get(random.nextInt(validGames.size()));
                    MCEMessenger.sendGlobalTitle("<gold><bold>平票！</bold></gold>",
                            "<yellow>随机选择: " + gameNames[winningGameId] + "</yellow>");
                }
            }
        } else {
            // 再次验证winningGameId有效性
            if (winningGameId >= 0 && winningGameId < gameNames.length && !gameNames[winningGameId].isEmpty()) {
                MCEMessenger.sendGlobalTitle("<gold><bold>投票结果</bold></gold>",
                        "<yellow>获胜游戏: " + gameNames[winningGameId] + "</yellow>");
            } else {
                // 如果winningGameId无效，随机选择
                Random random = new Random();
                java.util.List<Integer> validGames = new java.util.ArrayList<>();
                for (int i = 0; i < gameNames.length; i++) {
                    if (!gameNames[i].isEmpty()) {
                        validGames.add(i);
                    }
                }
                if (!validGames.isEmpty()) {
                    winningGameId = validGames.get(random.nextInt(validGames.size()));
                    MCEMessenger.sendGlobalTitle("<gold><bold>检测到错误，随机选择</bold></gold>",
                            "<yellow>游戏: " + gameNames[winningGameId] + "</yellow>");
                }
            }
        }

        // 显示投票结果后立即收回所有投票卡
        removeAllVotingCards();

        // 等待5秒后启动游戏（确保onEnd阶段完成）
        int finalWinningGameId = winningGameId;
        boolean finalSkipIntro = skipIntro; // 保存skipIntro状态
        votingSystem.setDelayedTask(5, () -> {
            // 在启动游戏前检查并自动分队
            checkAndAutoShuffleTeams();

            // 在启动游戏前清空所有玩家的物品栏
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.getInventory().clear();
                player.updateInventory();
            }
            MCEMainController.immediateLaunchGame(finalWinningGameId, !finalSkipIntro); // skipIntro为true时，intro参数为false
        });
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
        for (int i = 0; i < gameNames.length; i++) {
            if (!gameNames[i].isEmpty()) {
                stats.append("§e").append(gameNames[i]).append(": §f")
                        .append(votes.getOrDefault(i, 0)).append("票\n");
            }
        }
        return stats.toString();
    }

    /**
     * 清空投票数据
     */
    public static void clearVotingData() {
        votes.clear();
        playerVotes.clear();
        isVotingInitialized = false;
        skipIntro = false;
    }

    /**
     * 清空指定玩家的投票记录（进入 duel 时调用）
     */
    public static void clearVotingDataForPlayer(org.bukkit.entity.Player player) {
        if (player == null)
            return;
        String uuid = player.getUniqueId().toString();
        Integer prev = playerVotes.remove(uuid);
        if (prev != null) {
            int count = votes.getOrDefault(prev, 0);
            if (count > 0)
                votes.put(prev, count - 1);
        }
    }

    /**
     * 移除所有玩家的投票卡
     */
    public static void removeAllVotingCards() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            // 遍历玩家背包，移除所有投票卡
            for (int i = 0; i < player.getInventory().getSize(); i++) {
                ItemStack item = player.getInventory().getItem(i);
                if (item != null && item.getType() == Material.PAPER) {
                    ItemMeta meta = item.getItemMeta();
                    if (meta != null && meta.hasDisplayName()) {
                        String displayName = meta.getDisplayName();
                        // 检查是否是投票卡（包含"投票卡"字样）
                        if (displayName.contains("投票卡")) {
                            player.getInventory().setItem(i, null);
                        }
                    }
                }
            }
        }
        plugin.getLogger().info("已收回所有玩家的投票卡");
    }

    /**
     * 检查是否有未分队的玩家，如果有则自动执行分队
     */
    public static void checkAndAutoShuffleTeams() {
        Collection<? extends Player> onlinePlayers = Bukkit.getOnlinePlayers();
        if (onlinePlayers.isEmpty()) {
            return; // 没有在线玩家，无需分队
        }

        // 检查是否有未分队的玩家
        List<Player> unassignedPlayers = new ArrayList<>();
        for (Player player : onlinePlayers) {
            Team playerTeam = MCETeamUtils.getTeam(player);
            if (playerTeam == null) {
                unassignedPlayers.add(player);
            }
        }

        // 如果有未分队的玩家，执行自动分队
        if (!unassignedPlayers.isEmpty()) {
            plugin.getLogger().info("发现 " + unassignedPlayers.size() + " 个未分队玩家，执行自动分队...");

            // 每个玩家一个队伍（一人一队）
            int teamCount = onlinePlayers.size();

            // 确保队伍数量不超过可用队伍数量
            teamCount = Math.min(teamCount, teams.length);

            // 执行自动分队
            autoShuffleTeams(teamCount, new ArrayList<>(onlinePlayers));

            MCEMessenger.sendGlobalInfo("<green><bold>检测到未分队玩家，已自动分队！</bold></green>");
            plugin.getLogger().info("自动分队完成：将 " + onlinePlayers.size() + " 名玩家分成 " + teamCount + " 个队伍");
        } else {
            plugin.getLogger().info("所有玩家已分队，无需自动分队");
        }
    }

    /**
     * 执行自动分队（基于ShuffleTeam命令的逻辑）
     */
    private static void autoShuffleTeams(int teamCount, List<Player> players) {
        Collections.shuffle(players);

        // 清空现有队伍
        clearExistingTeams();

        // 创建需要的队伍数量
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        for (int i = 0; i < teamCount; ++i) {
            Team team = scoreboard.registerNewTeam(teams[i].teamName());
            team.color(teams[i].teamColor());
        }

        // 将玩家分配到队伍（一人一队）
        for (int i = 0; i < players.size(); ++i) {
            Player player = players.get(i);
            if (i < teamCount) {
                Team team = scoreboard.getTeam(teams[i].teamName());
                if (team != null) {
                    team.addEntry(player.getName());
                }
            } else {
                // 如果玩家数量超过队伍数量，将多余的玩家分配到现有队伍中
                int teamIndex = i % teamCount;
                Team team = scoreboard.getTeam(teams[teamIndex].teamName());
                if (team != null) {
                    team.addEntry(player.getName());
                }
            }
        }
    }

    /**
     * 清空现有队伍（基于ShuffleTeam命令的逻辑）
     */
    private static void clearExistingTeams() {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();

        // 清空所有队伍
        for (Team team : new HashSet<>(scoreboard.getTeams())) {
            team.unregister();
        }
    }
}