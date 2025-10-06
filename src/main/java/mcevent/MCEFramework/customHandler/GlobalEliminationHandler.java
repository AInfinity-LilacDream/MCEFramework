package mcevent.MCEFramework.customHandler;

import mcevent.MCEFramework.MCEMainController;
import mcevent.MCEFramework.games.survivalGame.SurvivalGame;
import mcevent.MCEFramework.games.survivalGame.SurvivalGameFuncImpl;
import mcevent.MCEFramework.games.survivalGame.gameObject.SurvivalGameGameBoard;
import mcevent.MCEFramework.generalGameObject.MCEResumableEventHandler;
import mcevent.MCEFramework.generalGameObject.MCEGame;
import mcevent.MCEFramework.tools.MCEMessenger;
import mcevent.MCEFramework.tools.MCEPlayerUtils;
import mcevent.MCEFramework.tools.MCETeamUtils;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.scoreboard.Team;

import static mcevent.MCEFramework.miscellaneous.Constants.*;

/*
GlobalEliminationHandler: 统一处理玩家与队伍淘汰的全局监听器
*/
public class GlobalEliminationHandler extends MCEResumableEventHandler implements Listener {

    public GlobalEliminationHandler() {
        setSuspended(false); // 全局常驻
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!MCEMainController.isRunningGame())
            return;

        Player victim = event.getEntity();
        // SG 特化：生成死亡箱并清空掉落
        MCEGame current = MCEMainController.getCurrentRunningGame();
        if (current instanceof SurvivalGame) {
            SurvivalGameFuncImpl.createDeathChest(victim, victim.getLocation());
            event.getDrops().clear();
        }

        // 统一处理
        eliminateNow(victim);
    }

    public static void eliminateNow(Player victim) {
        if (!MCEMainController.isRunningGame())
            return;
        MCEGame current = MCEMainController.getCurrentRunningGame();

        // 设置旁观
        victim.setGameMode(GameMode.SPECTATOR);

        // 玩家淘汰提示与音效
        String pname = MCEPlayerUtils.getColoredPlayerName(victim);
        MCEMessenger.sendGlobalInfo(pname + " <gray>已被淘汰！</gray>");
        MCEPlayerUtils.globalPlaySound("minecraft:player_eliminated");

        // 若是饥饿游戏，记录击杀并更新展示板计数
        if (current instanceof SurvivalGame sg) {
            Player killer = victim.getKiller();
            if (killer != null) {
                SurvivalGameFuncImpl.registerKill(killer);
            }
            if (sg.getGameBoard() instanceof SurvivalGameGameBoard board) {
                Team team = MCETeamUtils.getTeam(victim);
                if (team != null) {
                    board.updateTeamRemainTitle(team);
                }
                int remaining = 0;
                for (Player p : Bukkit.getOnlinePlayers()) {
                    if (p.getGameMode() != GameMode.SPECTATOR)
                        remaining++;
                }
                board.updatePlayerRemainTitle(remaining);
            }
        }

        // 队伍团灭检测
        Team vteam = MCETeamUtils.getTeam(victim);
        if (vteam != null) {
            boolean anyAlive = false;
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (MCETeamUtils.getTeam(p) == vteam && p.getGameMode() != GameMode.SPECTATOR) {
                    anyAlive = true;
                    break;
                }
            }
            if (!anyAlive) {
                String tname = MCETeamUtils.getTeamColoredName(vteam);
                MCEMessenger.sendGlobalInfo(tname + " <gray>已被团灭！</gray>");
                MCEPlayerUtils.globalPlaySound("minecraft:team_eliminated");
                if (current instanceof SurvivalGame) {
                    SurvivalGameFuncImpl.registerTeamElimination(vteam);
                }
            }
        }
    }
}
