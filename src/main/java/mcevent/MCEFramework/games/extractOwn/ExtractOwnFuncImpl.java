package mcevent.MCEFramework.games.extractOwn;

import mcevent.MCEFramework.games.extractOwn.gameObject.ExtractOwnGameBoard;
import mcevent.MCEFramework.tools.MCEMessenger;
import mcevent.MCEFramework.tools.MCEPlayerUtils;
import mcevent.MCEFramework.tools.MCETeamUtils;
import mcevent.MCEFramework.tools.MCETimerUtils;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Team;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static mcevent.MCEFramework.miscellaneous.Constants.*;

/*
ExtractOwnFuncImpl: 暗矢狂潮游戏功能实现类
*/
public class ExtractOwnFuncImpl {

    private static ExtractOwn extractOwn;
    private static final Map<String, Integer> teamScores = new ConcurrentHashMap<>();

    // 游戏配置
    private static final Location MAP_CENTER = new Location(null, 409.5, 28, 127.5);
    private static final int KILL_SCORE = 50;
    private static final int SURVIVAL_SCORE = 30;

    // 缩圈配置
    private static final int INITIAL_BORDER_SIZE = 250;
    private static final int SHRINK_AMOUNT = 50;
    private static final int SHRINK_CYCLES = 4;
    private static final int FINAL_SIZE = 3;
    private static final int WAIT_TIME = 10; // 初始等待时间
    private static final int SHRINK_TIME = 20; // 缩圈时间
    private static final int CYCLE_TIME = 30; // 循环间隔

    private static BukkitRunnable borderTask;
    private static BukkitRunnable survivalTask;
    private static int currentBorderSize = INITIAL_BORDER_SIZE;

    /**
     * 加载配置
     */
    public static void loadConfig(ExtractOwn game) {
        ExtractOwnConfigParser configParser = game.getExtractOwnConfigParser();
        game.setIntroTextList(configParser.openAndParse(game.getConfigFileName()));
        plugin.getLogger().info("暗矢狂潮配置加载完成");
    }

    /**
     * 重置游戏板
     */
    public static void resetGameBoard(ExtractOwn game) {
        extractOwn = game;
        ExtractOwnGameBoard gameBoard = (ExtractOwnGameBoard) game.getGameBoard();
    }

    /**
     * 初始化队伍分数
     */
    public static void initializeTeamScores() {
        teamScores.clear();
        for (Team team : MCETeamUtils.getActiveTeams()) {
            teamScores.put(team.getName(), 0);
        }
    }

    /**
     * 给玩家弩和箭
     */
    public static void givePlayersCrossbowAndArrows() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.getInventory().clear();

            // 给弩（附魔快速装填I和无限）
            ItemStack crossbow = new ItemStack(Material.CROSSBOW);
            ItemMeta crossbowMeta = crossbow.getItemMeta();
            if (crossbowMeta != null) {
                crossbowMeta.displayName(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage()
                        .deserialize("<gold><bold>战斗弩</bold></gold>"));
                crossbowMeta.addEnchant(Enchantment.QUICK_CHARGE, 1, true);
                crossbowMeta.addEnchant(Enchantment.INFINITY, 1, true);
                crossbowMeta.setUnbreakable(true);
                crossbow.setItemMeta(crossbowMeta);
            }
            player.getInventory().setItem(0, crossbow);

            // 给箭（1支）
            ItemStack arrow = new ItemStack(Material.ARROW, 1);
            player.getInventory().setItem(1, arrow);

            player.updateInventory();
        }
    }

    /**
     * 启动缩圈机制
     */
    public static void startShrinkingBorder(ExtractOwn game) {
        World world = Bukkit.getWorld(game.getWorldName());
        if (world == null)
            return;

        // 设置初始世界边界
        world.getWorldBorder().setCenter(MAP_CENTER);
        world.getWorldBorder().setSize(INITIAL_BORDER_SIZE);
        currentBorderSize = INITIAL_BORDER_SIZE;

        borderTask = new BukkitRunnable() {
            private int cycleCount = 0;
            private boolean isWaiting = true;
            private int waitCounter = 0;

            @Override
            public void run() {
                if (isWaiting) {
                    waitCounter++;
                    if (waitCounter >= WAIT_TIME) {
                        isWaiting = false;
                        waitCounter = 0;

                        if (cycleCount < SHRINK_CYCLES) {
                            // 常规缩圈
                            int newSize = currentBorderSize - SHRINK_AMOUNT;
                            world.getWorldBorder().setSize(newSize, SHRINK_TIME);
                            currentBorderSize = newSize;

                            MCEMessenger.sendGlobalInfo("<red><bold>缩圈警告！</bold></red> <yellow>边界将在" + SHRINK_TIME
                                    + "秒内缩小到" + newSize + "格</yellow>");

                            plugin.getLogger()
                                    .info("第" + (cycleCount + 1) + "次缩圈：" + currentBorderSize + " -> " + newSize);
                        } else {
                            // 最后一次缩圈：50x50 -> 3x3
                            world.getWorldBorder().setSize(FINAL_SIZE, SHRINK_TIME);
                            currentBorderSize = FINAL_SIZE;

                            MCEMessenger.sendGlobalInfo("<dark_red><bold>最终缩圈！</bold></dark_red> <red>边界将在"
                                    + SHRINK_TIME + "秒内缩小到" + FINAL_SIZE + "x" + FINAL_SIZE + "！</red>");

                            plugin.getLogger()
                                    .info("最终缩圈：" + (currentBorderSize + SHRINK_AMOUNT) + " -> " + FINAL_SIZE);

                            // 最终缩圈后停止任务
                            this.cancel();
                            return;
                        }

                        cycleCount++;
                    }
                } else {
                    // 缩圈阶段，等待CYCLE_TIME后进入下一轮
                    waitCounter++;
                    if (waitCounter >= CYCLE_TIME) {
                        isWaiting = true;
                        waitCounter = 0;
                    }
                }
            }
        };

        // 每秒执行一次
        borderTask.runTaskTimer(plugin, 0L, 20L);
    }

    /**
     * 启动生存检测任务
     */
    public static void startSurvivalDetection(ExtractOwn game) {
        survivalTask = new BukkitRunnable() {
            @Override
            public void run() {
                checkTeamElimination(game);
                checkGameEnd(game);
            }
        };

        // 每2秒检测一次，更及时地检测游戏结束条件
        survivalTask.runTaskTimer(plugin, 0L, 40L);
    }

    /**
     * 检查队伍淘汰情况
     */
    private static void checkTeamElimination(ExtractOwn game) {
        for (Team team : MCETeamUtils.getActiveTeams()) {
            boolean hasAlivePlayers = false;

            for (Player player : MCETeamUtils.getPlayers(team)) {
                if (player.getGameMode() == GameMode.SURVIVAL && !player.getScoreboardTags().contains("dead")) {
                    hasAlivePlayers = true;
                    break;
                }
            }

            if (!hasAlivePlayers && !game.getEliminatedTeams().contains(team.getName())) {
                // 队伍被淘汰
                game.addEliminatedTeam(team.getName());

                MCEMessenger.sendGlobalInfo("<red>" + MCETeamUtils.getUncoloredTeamName(team) + " 已被淘汰！</red>");
                plugin.getLogger().info("队伍 " + team.getName() + " 被淘汰");

                // 不在这里给存活分数，只有最后获胜的队伍才能获得存活分数
            }
        }
    }

    /**
     * 检查游戏是否结束
     */
    private static void checkGameEnd(ExtractOwn game) {
        int survivingTeams = game.getSurvivingTeamCount();

        if (survivingTeams <= 1) {
            // 游戏结束
            if (survivalTask != null && !survivalTask.isCancelled()) {
                survivalTask.cancel();
            }

            // 停止游戏任务但不停止时间线任务
            if (borderTask != null && !borderTask.isCancelled()) {
                borderTask.cancel();
            }

            // 找到最后存活的队伍并记录获胜
            Team winningTeam = getLastSurvivingTeam(game);
            if (winningTeam != null) {
                String teamName = winningTeam.getName();

                // 计算存活人数并给予对应分数
                int survivingPlayers = 0;
                for (Player player : MCETeamUtils.getPlayers(winningTeam)) {
                    if (player.getGameMode() == GameMode.SURVIVAL && !player.getScoreboardTags().contains("dead")) {
                        survivingPlayers++;
                    }
                }

                int totalSurvivalScore = survivingPlayers * SURVIVAL_SCORE;
                addTeamScore(teamName, totalSurvivalScore);

                // 记录回合获胜
                game.addRoundWin(teamName);
                plugin.getLogger().info(
                        "队伍 " + teamName + " 获得回合胜利，存活 " + survivingPlayers + " 人，获得 " + totalSurvivalScore + " 分存活分数");

                // 调试信息：显示所有队伍状态
                plugin.getLogger().info("=== 获胜者判定调试信息 ===");
                plugin.getLogger().info("检测到存活队伍数: " + survivingTeams);
                plugin.getLogger().info("获胜队伍: " + teamName);

                for (Team team : MCETeamUtils.getActiveTeams()) {
                    boolean teamHasAlive = false;
                    StringBuilder teamInfo = new StringBuilder();
                    teamInfo.append("队伍 ").append(team.getName()).append(": ");

                    for (Player player : MCETeamUtils.getPlayers(team)) {
                        boolean isAlive = (player.getGameMode() == GameMode.SURVIVAL
                                && !player.getScoreboardTags().contains("dead"));
                        if (isAlive)
                            teamHasAlive = true;

                        teamInfo.append(player.getName())
                                .append("[模式:").append(player.getGameMode())
                                .append(",死亡:").append(player.getScoreboardTags().contains("dead"))
                                .append(",存活:").append(isAlive).append("] ");
                    }

                    teamInfo.append(" -> 队伍存活: ").append(teamHasAlive);
                    plugin.getLogger().info(teamInfo.toString());
                }
                plugin.getLogger().info("=== 调试信息结束 ===");
            } else {
                plugin.getLogger().warning("警告：检测到只剩1队但找不到获胜队伍！");
            }

            // 立即进入cycleEnd阶段
            game.getTimeline().nextState();
        }
    }

    /**
     * 获取最后存活的队伍
     */
    private static Team getLastSurvivingTeam(ExtractOwn game) {
        List<Team> survivingTeams = new ArrayList<>();

        for (Team team : MCETeamUtils.getActiveTeams()) {
            boolean hasAlivePlayers = false;

            // 使用与getSurvivingTeamCount相同的逻辑
            for (Player player : MCETeamUtils.getPlayers(team)) {
                if (player.getGameMode() == GameMode.SURVIVAL && !player.getScoreboardTags().contains("dead")) {
                    hasAlivePlayers = true;
                    break;
                }
            }

            if (hasAlivePlayers) {
                survivingTeams.add(team);
            }
        }

        // 应该只有一个存活队伍
        if (survivingTeams.size() == 1) {
            return survivingTeams.get(0);
        } else if (survivingTeams.size() == 0) {
            plugin.getLogger().warning("没有找到存活队伍！");
            return null;
        } else {
            plugin.getLogger().warning("发现多个存活队伍：" + survivingTeams.size() + "个");
            return survivingTeams.get(0); // 返回第一个
        }
    }

    /**
     * 处理玩家击杀
     */
    public static void handlePlayerKill(Player killer, Player victim) {
        if (killer == null || victim == null)
            return;

        Team killerTeam = MCETeamUtils.getTeam(killer);
        Team victimTeam = MCETeamUtils.getTeam(victim);

        if (killerTeam != null && victimTeam != null && !killerTeam.equals(victimTeam)) {
            addTeamScore(killerTeam.getName(), KILL_SCORE);
            MCEMessenger.sendInfoToPlayer("<gold>击杀奖励：+" + KILL_SCORE + "分！</gold>", killer);

            // 将被击杀玩家设为旁观模式
            victim.setGameMode(GameMode.SPECTATOR);
            MCEMessenger.sendInfoToPlayer("<red>你已被淘汰，现在是旁观模式</red>", victim);
        }
    }

    /**
     * 添加队伍分数
     */
    public static void addTeamScore(String teamName, int points) {
        teamScores.merge(teamName, points, Integer::sum);
    }

    /**
     * 获取队伍分数
     */
    public static int getTeamScore(String teamName) {
        return teamScores.getOrDefault(teamName, 0);
    }

    /**
     * 发送获胜消息
     */
    public static void sendWinningMessage() {
        Map<String, Integer> roundWins = extractOwn.getRoundWins();

        // 找出冠军队伍：首先按分数，分数相同时按回合获胜次数
        String winningTeam = determineChampionTeam(roundWins);

        if (!winningTeam.isEmpty()) {
            int winningScore = getTeamScore(winningTeam);
            int winningRounds = roundWins.getOrDefault(winningTeam, 0);

            MCEMessenger.sendGlobalTitle("<gold><bold>锦标赛结束！</bold></gold>",
                    "<yellow>冠军队伍：<green>" + winningTeam + "</green> <yellow>(<gold>" + winningScore + "分, "
                            + winningRounds + "回合胜利</gold>)</yellow>");
        } else {
            MCEMessenger.sendGlobalTitle("<gold><bold>锦标赛结束！</bold></gold>",
                    "<yellow>所有队伍表现相当！</yellow>");
        }

        // 显示总分数排名（分数优先）
        displayTotalScoreRankings();

        // 显示回合胜利排名
        displayRoundWinRankings();
    }

    /**
     * 确定冠军队伍：首先按分数，分数相同时按回合获胜次数
     */
    private static String determineChampionTeam(Map<String, Integer> roundWins) {
        if (teamScores.isEmpty()) {
            return "";
        }

        // 创建队伍信息列表用于排序
        List<TeamRanking> teamRankings = new ArrayList<>();

        for (String teamName : teamScores.keySet()) {
            int score = getTeamScore(teamName);
            int rounds = roundWins.getOrDefault(teamName, 0);
            teamRankings.add(new TeamRanking(teamName, score, rounds));
        }

        // 排序：首先按分数降序，分数相同时按回合获胜次数降序
        teamRankings.sort((a, b) -> {
            if (a.score != b.score) {
                return Integer.compare(b.score, a.score); // 分数高的在前
            } else {
                return Integer.compare(b.roundWins, a.roundWins); // 回合获胜多的在前
            }
        });

        return teamRankings.isEmpty() ? "" : teamRankings.get(0).teamName;
    }

    /**
     * 队伍排名辅助类
     */
    private static class TeamRanking {
        final String teamName;
        final int score;
        final int roundWins;

        TeamRanking(String teamName, int score, int roundWins) {
            this.teamName = teamName;
            this.score = score;
            this.roundWins = roundWins;
        }
    }

    /**
     * 显示回合胜利排名
     */
    private static void displayRoundWinRankings() {
        Map<String, Integer> roundWins = extractOwn.getRoundWins();
        List<Map.Entry<String, Integer>> sortedTeams = new ArrayList<>(roundWins.entrySet());
        sortedTeams.sort(Map.Entry.<String, Integer>comparingByValue().reversed());

        StringBuilder message = new StringBuilder("<gold><bold>=== 回合胜利排名 ===</bold></gold>");

        for (int i = 0; i < sortedTeams.size(); i++) {
            Map.Entry<String, Integer> entry = sortedTeams.get(i);
            String teamName = entry.getKey();
            int wins = entry.getValue();

            message.append("\n<yellow>第").append(i + 1).append("名：<green>")
                    .append(teamName).append("</green> <gray>- <gold>").append(wins)
                    .append("回合胜利</gold></gray></yellow>");
        }

        // 添加未获胜的队伍
        for (Team team : MCETeamUtils.getActiveTeams()) {
            String teamName = team.getName();
            if (!roundWins.containsKey(teamName)) {
                message.append("\n<gray>").append(teamName).append(" - 0回合胜利</gray>");
            }
        }

        // 延迟3秒显示排名
        MCETimerUtils.setDelayedTask(3, () -> {
            MCEMessenger.sendGlobalInfo(message.toString());
        });
    }

    /**
     * 显示总分数排名
     */
    private static void displayTotalScoreRankings() {
        List<Map.Entry<String, Integer>> sortedTeams = new ArrayList<>(teamScores.entrySet());
        sortedTeams.sort(Map.Entry.<String, Integer>comparingByValue().reversed());

        StringBuilder message = new StringBuilder("<aqua><bold>=== 总分数排名 ===</bold></aqua>");

        for (int i = 0; i < sortedTeams.size(); i++) {
            Map.Entry<String, Integer> entry = sortedTeams.get(i);
            String teamName = entry.getKey();
            int score = entry.getValue();

            message.append("\n<yellow>第").append(i + 1).append("名：<green>")
                    .append(teamName).append("</green> <gray>- <gold>").append(score)
                    .append("分</gold></gray></yellow>");
        }

        // 延迟6秒显示总分排名（在回合胜利排名后）
        MCETimerUtils.setDelayedTask(6, () -> {
            MCEMessenger.sendGlobalInfo(message.toString());
        });
    }

    /**
     * 停止所有任务
     */
    public static void stopAllTasks() {
        if (borderTask != null && !borderTask.isCancelled()) {
            borderTask.cancel();
            plugin.getLogger().info("缩圈任务已停止");
        }
        if (survivalTask != null && !survivalTask.isCancelled()) {
            survivalTask.cancel();
            plugin.getLogger().info("生存检测任务已停止");
        }
    }

    /**
     * 发送回合结果
     */
    public static void sendRoundResults(int currentRound) {
        MCEMessenger.sendGlobalTitle("<gold><bold>第" + currentRound + "回合结束！</bold></gold>", null);

        // 显示最后存活的玩家
        displaySurvivingPlayers();

        // 显示当前所有队伍分数
        displayCurrentTeamScores();
    }

    /**
     * 显示最后存活的玩家
     */
    private static void displaySurvivingPlayers() {
        List<Player> survivingPlayers = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getGameMode() == GameMode.SURVIVAL && !player.getScoreboardTags().contains("dead")) {
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

            // 延迟2秒显示存活玩家信息
            MCETimerUtils.setDelayedTask(2, () -> {
                MCEMessenger.sendGlobalInfo(survivorMessage.toString());
            });
        }
    }

    /**
     * 显示当前队伍分数
     */
    private static void displayCurrentTeamScores() {
        List<Map.Entry<String, Integer>> sortedTeams = new ArrayList<>(teamScores.entrySet());
        sortedTeams.sort(Map.Entry.<String, Integer>comparingByValue().reversed());

        StringBuilder message = new StringBuilder("<aqua><bold>=== 当前队伍分数 ===</bold></aqua>");

        for (int i = 0; i < sortedTeams.size(); i++) {
            Map.Entry<String, Integer> entry = sortedTeams.get(i);
            String teamName = entry.getKey();
            int score = entry.getValue();

            message.append("\n<yellow>第").append(i + 1).append("名：<green>")
                    .append(teamName).append("</green> <gray>- <gold>").append(score)
                    .append("分</gold></gray></yellow>");
        }

        // 延迟4秒显示队伍分数（在存活玩家信息后）
        MCETimerUtils.setDelayedTask(4, () -> {
            MCEMessenger.sendGlobalInfo(message.toString());
        });
    }
}