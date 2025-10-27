package mcevent.MCEFramework.generalGameObject;

import mcevent.MCEFramework.tools.MCETeamUtils;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Team;

import static mcevent.MCEFramework.miscellaneous.Constants.plugin;

/**
 * 默认的游戏中玩家加入处理器
 * 提供通用的处理逻辑：让新加入玩家离开队伍，设置为观察者模式，不参与游戏
 */
public class DefaultGamePlayerJoinHandler implements GamePlayerJoinHandler {

    private final MCEGame game;

    public DefaultGamePlayerJoinHandler(MCEGame game) {
        this.game = game;
    }

    @Override
    public void handlePlayerJoinDuringGame(Player player) {
        plugin.getLogger().info("处理游戏中新加入的玩家: " + player.getName());

        // 刷新游戏的活跃队伍列表（有人加入时可能发生变化）
        try {
            game.setActiveTeams(MCETeamUtils.getActiveTeams());
        } catch (Throwable ignored) {
        }

        // 检查玩家是否已经有队伍（可能是断线重连的游戏参与者），且未被淘汰
        Team currentTeam = MCETeamUtils.getTeam(player);
        boolean inActiveTeam = (currentTeam != null && game.getActiveTeams() != null
                && game.getActiveTeams().contains(currentTeam));
        boolean notEliminated = !player.getScoreboardTags().contains("dead");
        boolean shouldBeParticipant = inActiveTeam && notEliminated;

        if (shouldBeParticipant) {
            // 玩家在活跃队伍且未被淘汰：作为参与者加入
            plugin.getLogger().info("玩家 " + player.getName() + " 加入进行中的游戏，队伍="
                    + (currentTeam != null ? currentTeam.getName() : "null"));
            // 标记 Active 并设置为生存模式（延迟以确保传送完成）
            player.addScoreboardTag("Active");
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (player.isOnline()) {
                        player.setGameMode(GameMode.SURVIVAL);
                    }
                }
            }.runTaskLater(plugin, 10L);
        } else {
            // 非参与者：成为旁观
            // 若玩家无队伍，则不在任何队伍；若在队伍但已淘汰，保留队伍但不加入游戏
            if (currentTeam == null || !inActiveTeam) {
                removePlayerFromTeam(player);
            }
            player.removeScoreboardTag("Active");
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (player.isOnline() && !isGameParticipant(player)) {
                        player.setGameMode(getDefaultJoinGameMode());
                        player.removeScoreboardTag("Active");
                        player.sendMessage("§c游戏正在进行中，你已设置为观察者模式。");
                        plugin.getLogger().info("已将玩家 " + player.getName() + " 设置为观察者模式");
                    }
                }
            }.runTaskLater(plugin, 10L);
        }
    }

    @Override
    public boolean isGameParticipant(Player player) {
        // 参与者：持有开局发放的特殊标签且位于该游戏世界
        return player.getScoreboardTags().contains("Participant") &&
                player.getWorld().getName().equals(game.getWorldName());
    }

    /**
     * 让玩家离开当前队伍
     */
    private void removePlayerFromTeam(Player player) {
        Team currentTeam = MCETeamUtils.getTeam(player);
        if (currentTeam != null) {
            currentTeam.removeEntry(player.getName());
            plugin.getLogger().info("已将玩家 " + player.getName() + " 从队伍 " + currentTeam.getName() + " 中移除");

            // 通知玩家
            player.sendMessage("§e你已自动离开队伍: §f" + currentTeam.getName());
        }
    }
}