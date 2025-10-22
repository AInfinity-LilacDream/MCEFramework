package mcevent.MCEFramework.games.parkourTag.customHandler;

import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;
import mcevent.MCEFramework.generalGameObject.MCEResumableEventHandler;
import mcevent.MCEFramework.tools.MCEMessenger;
import mcevent.MCEFramework.tools.MCEPlayerUtils;
import mcevent.MCEFramework.tools.MCETeamUtils;
import static mcevent.MCEFramework.miscellaneous.Constants.*;
import org.bukkit.entity.Player;
import org.bukkit.GameMode;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.scoreboard.Team;

import java.util.Objects;

/*
PlayerCaughtHandler: 玩家抓捕事件监听器
 */
public class PlayerCaughtHandler extends MCEResumableEventHandler implements Listener {

    public PlayerCaughtHandler() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onCaught(PrePlayerAttackEntityEvent event) {
        if (this.isSuspended())
            return;

        event.setCancelled(true);
        if (!(event.getAttacked() instanceof Player runner))
            return;
        Player catcher = event.getPlayer();

        if (Objects.requireNonNull(MCETeamUtils.getTeam(catcher)).getName()
                .equals(Objects.requireNonNull(MCETeamUtils.getTeam(runner)).getName()))
            return;
        if (catcher.getScoreboardTags().contains("runner"))
            return;
        if (runner.getScoreboardTags().contains("caught"))
            return;

        runner.addScoreboardTag("caught");
        // 本地淘汰处理：不走全局监听器
        runner.addScoreboardTag("dead");
        runner.removeScoreboardTag("Active");
        runner.setGameMode(GameMode.SPECTATOR);

        Team runnerTeam = MCETeamUtils.getTeam(runner);
        Team catcherTeam = MCETeamUtils.getTeam(catcher);
        String runnerName = MCEPlayerUtils.getColoredPlayerName(runner);
        String catcherName = MCEPlayerUtils.getColoredPlayerName(catcher);

        // 发送一些提示信息
        MCEMessenger.sendInfoToTeam(runnerTeam, runnerName + " 被 " + catcherName + " 抓住了");
        MCEMessenger.sendInfoToTeam(catcherTeam, runnerName + " 被 " + catcherName + " 抓住了");
        MCEMessenger.sendTitleToPlayer(catcher, "<red>⚔</red> " + runnerName, null);

        int runnerTeamPos = pkt.getTeamId(runnerTeam);
        pkt.getSurvivePlayerTot().set(runnerTeamPos, pkt.getSurvivePlayerTot().get(runnerTeamPos) - 1);
        pkt.getGameBoard().globalDisplay();

        // 检查队伍是否被抓完：被抓人数 = 队伍总人数 - 1（因为每队有一个抓人者）
        // 由全局淘汰监听器负责队伍团灭提示；此处仅更新内部计数
        int survivingMembers = pkt.getSurvivePlayerTot().get(runnerTeamPos);
        // survivePlayerTot 统计的是“逃跑者”人数，初始为队伍总人数-1（不含抓人者）
        // 因此当 survivingMembers == 0 才表示该队所有逃跑者被抓完
        if (survivingMembers <= 0) {
            // 记录本方完成时间
            pkt.setTeamCompleteTime(catcherTeam, pkt.getTimeline().getCurrentTimelineNodeDuration() - 10);
            MCEMessenger.sendTitleToPlayer(catcher, "<green>专业猎手！</green>", "<green>干得漂亮！</green>");
            // 若对手尚未完成，发送“率先抓完”相关提示
            if (pkt.getTeamCompleteTime(runnerTeam) == 0) {
                MCEMessenger.sendInfoToTeam(catcherTeam, "<green>[✅] 你们比对手更快抓住了所有猎物( " +
                        pkt.getTeamCompleteTime(catcherTeam) +
                        " 秒)，获得了本轮的胜利！</green>");
                MCEMessenger.sendInfoToTeam(runnerTeam, "<gold>[⚠] 对手比你们更快抓住了所有猎物，你们输了TAT</gold>");
                MCEMessenger.sendInfoToTeam(runnerTeam, "<red>[⚠] 你们被对方抓完了</red>");
                MCEMessenger.sendInfoToPlayer("<green>[✅] 你抓完了所有猎物，你做的好啊！</green>", catcher);
            }

            // 若对手也已记录完成时间（双方均完成），则该对局计为完成+1
            Team opponentTeam = pkt.getOpponentTeam(catcherTeam);
            if (pkt.getTeamCompleteTime(opponentTeam) > 0) {
                pkt.completeMatchesTot++;
            }
        }

        // 每个小局由两支队伍对抗，因此全部小局完成的数量应为 活跃队伍数/2
        if (pkt.completeMatchesTot >= Math.max(1, pkt.getActiveTeams().size() / 2)) {
            pkt.completeMatchesTot = 0; // 防止重复触发nextState
            pkt.getTimeline().nextState();
        }
    }
}
