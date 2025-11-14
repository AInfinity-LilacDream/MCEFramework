package mcevent.MCEFramework.games.parkourTag.customHandler;

import static mcevent.MCEFramework.miscellaneous.Constants.*;

import mcevent.MCEFramework.generalGameObject.MCEResumableEventHandler;
import mcevent.MCEFramework.tools.MCETeamUtils;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;

/*
敌对队伍发光处理器 - 使用 NMS 实现发光效果
 */
public class OpponentTeamGlowingHandler extends MCEResumableEventHandler {

    /**
     * 在每个回合中，让抓捕者看到对方队伍的逃脱者发光
     * 使用 MCETeamUtils 中的 NMS 发光方法
     */
    public void toggleGlowing() {
        if (isSuspended()) {
            return;
        }

        // 遍历所有在线玩家
        for (Player player : Bukkit.getOnlinePlayers()) {
            // 只处理抓捕者
            if (!player.getScoreboardTags().contains("chaser")) {
                continue;
            }

            // 获取抓捕者所在的队伍
            Team chaserTeam = MCETeamUtils.getTeam(player);
            if (chaserTeam == null) {
                continue;
            }

            // 获取对手队伍
            Team opponentTeam = pkt.getOpponentTeam(chaserTeam);
            if (opponentTeam == null) {
                continue;
            }

            // 获取对手队伍中的所有逃脱者
            for (Player runner : MCETeamUtils.getPlayers(opponentTeam)) {
                // 跳过无效玩家和旁观者
                if (runner == null || runner.getGameMode() == GameMode.SPECTATOR) {
                    continue;
                }

                // 只让逃脱者发光（排除对手队伍的抓捕者）
                if (runner.getScoreboardTags().contains("runner")) {
                    // 使用 MCETeamUtils 的 NMS 发光方法
                    MCETeamUtils.setPlayerGlowingNMS(runner, player, true);
                }
            }
        }
    }

    /**
     * 清除所有发光效果（在回合结束时调用）
     */
    public void clearGlowing() {
        // 遍历所有在线玩家
        for (Player player : Bukkit.getOnlinePlayers()) {
            // 只处理抓捕者
            if (!player.getScoreboardTags().contains("chaser")) {
                continue;
            }

            // 获取抓捕者所在的队伍
            Team chaserTeam = MCETeamUtils.getTeam(player);
            if (chaserTeam == null) {
                continue;
            }

            // 获取对手队伍
            Team opponentTeam = pkt.getOpponentTeam(chaserTeam);
            if (opponentTeam == null) {
                continue;
            }

            // 清除对手队伍中所有逃脱者的发光效果
            for (Player runner : MCETeamUtils.getPlayers(opponentTeam)) {
                if (runner == null || runner.getGameMode() == GameMode.SPECTATOR) {
                    continue;
                }

                if (runner.getScoreboardTags().contains("runner")) {
                    // 清除发光效果
                    MCETeamUtils.setPlayerGlowingNMS(runner, player, false);
                }
            }
        }
    }
}
