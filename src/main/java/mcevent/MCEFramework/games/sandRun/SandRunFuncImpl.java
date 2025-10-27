package mcevent.MCEFramework.games.sandRun;

import mcevent.MCEFramework.games.sandRun.gameObject.SandRunGameBoard;
import mcevent.MCEFramework.tools.MCEMessenger;
import mcevent.MCEFramework.tools.MCEPlayerUtils;
import mcevent.MCEFramework.tools.MCETeamUtils;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static mcevent.MCEFramework.miscellaneous.Constants.*;

public class SandRunFuncImpl {

    private static final SandRunConfigParser sandRunConfigParser = sandRun.getSandRunConfigParser();

    /**
     * 从配置文件加载数据
     */
    public static void loadConfig() {
        sandRun.setIntroTextList(sandRunConfigParser.openAndParse(sandRun.getConfigFileName()));

        // 设置沙子掉落速度
        long sandFallInterval = sandRunConfigParser.getSandFallInterval();
        sandRun.getSandFallHandler().setSandFallSpeed(sandFallInterval);
    }

    /**
     * 初始化所有玩家属性
     */
    public static void initializePlayers() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            Objects.requireNonNull(player.getAttribute(Attribute.MAX_HEALTH)).setBaseValue(10.0);
            player.setHealth(10.0);
            player.getInventory().clear();
        }
    }

    /**
     * 重置游戏计分板，初始化玩家和队伍统计
     */
    protected static void resetGameBoard() {
        SandRunGameBoard gameBoard = (SandRunGameBoard) sandRun.getGameBoard();
        gameBoard.updatePlayerRemainTitle(0);
        int teamSize = sandRun.getActiveTeams() != null ? sandRun.getActiveTeams().size() : 0;
        gameBoard.setTeamRemainCount(
                mcevent.MCEFramework.generalGameObject.MCEGameBoard.countRemainingParticipantTeams());
        for (int i = 0; i < teamSize; ++i)
            gameBoard.getTeamRemain()[i] = 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.getScoreboardTags().contains("Participant") || player.getGameMode() == GameMode.SPECTATOR)
                continue;
            Team team = MCETeamUtils.getTeam(player);
            if (team == null)
                continue;
            int idx = sandRun.getTeamId(team);
            if (idx >= 0 && idx < gameBoard.getTeamRemain().length)
                gameBoard.getTeamRemain()[idx]++;
        }
        gameBoard.updateTeamRemainTitle(null);
    }

    /**
     * 更新游戏计分板 - 当玩家死亡时调用
     */
    public static void updateGameBoardOnPlayerDeath(Player deadPlayer) {
        SandRunGameBoard gameBoard = (SandRunGameBoard) sandRun.getGameBoard();
        Team playerTeam = MCETeamUtils.getTeam(deadPlayer);

        // 更新剩余玩家数
        int alivePlayerCount = 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getGameMode() == GameMode.SURVIVAL) {
                alivePlayerCount++;
            }
        }
        gameBoard.updatePlayerRemainTitle(alivePlayerCount);

        // 更新队伍统计
        if (playerTeam != null) {
            gameBoard.updateTeamRemainTitle(playerTeam);

            // 如果该队伍已全灭，发送消息
            int teamId = sandRun.getTeamId(playerTeam);
            if (teamId >= 0 && teamId < gameBoard.getTeamRemain().length &&
                    gameBoard.getTeamRemain()[teamId] == 0) {
                MCEMessenger.sendGlobalInfo(MCEPlayerUtils.getColoredPlayerName(deadPlayer) + " 所在的" +
                        MCETeamUtils.getUncoloredTeamName(playerTeam) + "<gray>已被团灭！</gray>");
            }
        }

        // 检查游戏是否结束
        if (alivePlayerCount == 0) {
            sandRun.getTimeline().nextState(); // 直接结束游戏
        }
    }

    /**
     * 清理游戏任务
     */
    public static void clearGameTasks(SandRun sandRun) {
        for (BukkitRunnable task : sandRun.getGameTask()) {
            task.cancel();
        }
        sandRun.getGameTask().clear();
    }

    /**
     * 发送获胜消息
     */
    public static void sendWinningMessage() {
        List<Player> alivePlayers = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getGameMode() == GameMode.SURVIVAL) {
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
     * 清理世界中所有混凝土粉末方块
     */
    public static void clearConcretePowder() {
        World world = Bukkit.getWorld(sandRun.getWorldName());
        if (world == null)
            return;

        int minY = world.getMinHeight();
        int maxY = world.getMaxHeight();
        int range = 50;

        for (int x = -range; x <= range; x++) {
            for (int z = -range; z <= range; z++) {
                for (int y = minY; y < maxY; y++) {
                    Location loc = new Location(world, x, y, z);
                    Material blockType = world.getBlockAt(loc).getType();

                    if (isConcretePowder(blockType)) {
                        world.getBlockAt(loc).setType(Material.AIR);
                    }
                }
            }
        }
    }

    /**
     * 检查方块是否为混凝土粉末
     */
    private static boolean isConcretePowder(Material material) {
        return material == Material.WHITE_CONCRETE_POWDER ||
                material == Material.ORANGE_CONCRETE_POWDER ||
                material == Material.MAGENTA_CONCRETE_POWDER ||
                material == Material.LIGHT_BLUE_CONCRETE_POWDER ||
                material == Material.YELLOW_CONCRETE_POWDER ||
                material == Material.LIME_CONCRETE_POWDER ||
                material == Material.PINK_CONCRETE_POWDER ||
                material == Material.GRAY_CONCRETE_POWDER ||
                material == Material.LIGHT_GRAY_CONCRETE_POWDER ||
                material == Material.CYAN_CONCRETE_POWDER ||
                material == Material.PURPLE_CONCRETE_POWDER ||
                material == Material.BLUE_CONCRETE_POWDER ||
                material == Material.BROWN_CONCRETE_POWDER ||
                material == Material.GREEN_CONCRETE_POWDER ||
                material == Material.RED_CONCRETE_POWDER ||
                material == Material.BLACK_CONCRETE_POWDER ||
                material == Material.SAND;
    }
}