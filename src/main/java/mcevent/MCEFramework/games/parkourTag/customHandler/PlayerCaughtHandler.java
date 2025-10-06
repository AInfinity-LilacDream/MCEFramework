package mcevent.MCEFramework.games.parkourTag.customHandler;

import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;
import mcevent.MCEFramework.generalGameObject.MCEResumableEventHandler;
import mcevent.MCEFramework.tools.MCEMessenger;
import mcevent.MCEFramework.tools.MCEPlayerUtils;
import mcevent.MCEFramework.tools.MCETeamUtils;
import static mcevent.MCEFramework.miscellaneous.Constants.*;
import org.bukkit.entity.Player;
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
        // 统一淘汰处理
        mcevent.MCEFramework.customHandler.GlobalEliminationHandler.eliminateNow(runner);

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
        if (survivingMembers <= 1) { // 只剩1人（抓人者）时，队伍被抓完
            pkt.completeMatchesTot++;
            pkt.setTeamCompleteTime(catcherTeam, pkt.getTimeline().getCurrentTimelineNodeDuration() - 10);
            // 交给全局处理器发送团灭提示；这里无需改抓捕者模式
            MCEMessenger.sendTitleToPlayer(catcher, "<green>专业猎手！</green>", "<green>干得漂亮！</green>");
            if (pkt.getTeamCompleteTime(runnerTeam) == 0) {
                MCEMessenger.sendInfoToTeam(catcherTeam, "<green>[✅] 你们比对手更快抓住了所有猎物( " +
                        pkt.getTeamCompleteTime(catcherTeam) +
                        " 秒)，获得了本轮的胜利！</green>");
                MCEMessenger.sendInfoToTeam(runnerTeam, "<gold>[⚠] 对手比你们更快抓住了所有猎物，你们输了TAT</gold>");
                MCEMessenger.sendInfoToTeam(runnerTeam, "<red>[⚠] 你们被对方抓完了</red>");
                MCEMessenger.sendInfoToPlayer("<green>[✅] 你抓完了所有猎物，你做的好啊！</green>", catcher);
            }
        }

        if (pkt.completeMatchesTot == pkt.getActiveTeams().size()) {
            pkt.completeMatchesTot = 0; // 防止重复触发nextState
            pkt.getTimeline().nextState();
        }
    }
}
