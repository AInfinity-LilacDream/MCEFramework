package mcevent.MCEFramework.customHandler;

import mcevent.MCEFramework.MCEMainController;
import mcevent.MCEFramework.games.captureCenter.CaptureCenter;
import mcevent.MCEFramework.games.crazyMiner.CrazyMiner;
import mcevent.MCEFramework.games.discoFever.DiscoFever;
import mcevent.MCEFramework.games.extractOwn.ExtractOwn;
import mcevent.MCEFramework.games.sandRun.SandRun;
import mcevent.MCEFramework.games.spleef.Spleef;
import mcevent.MCEFramework.games.survivalGame.SurvivalGame;
import mcevent.MCEFramework.games.survivalGame.SurvivalGameFuncImpl;
import mcevent.MCEFramework.games.survivalGame.gameObject.SurvivalGameGameBoard;
import mcevent.MCEFramework.games.tntTag.TNTTag;
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

import java.util.HashSet;
import java.util.Set;

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

        // 每次淘汰后评估是否应当结束当前回合
        evaluateRoundEnd(current);
    }

    private static void evaluateRoundEnd(MCEGame current) {
        if (current == null)
            return;

        // 模式一：只剩一队结束
        if (current instanceof CaptureCenter
                || current instanceof CrazyMiner
                || current instanceof ExtractOwn
                || current instanceof Spleef
                || current instanceof SurvivalGame) {
            if (countAliveTeams() <= 1) {
                current.getTimeline().nextState();
            }
            return;
        }

        // 模式二：所有人都死了才结束（无存活玩家）
        if (current instanceof DiscoFever || current instanceof SandRun) {
            if (countAlivePlayers() == 0) {
                current.getTimeline().nextState();
            }
            return;
        }

        // 模式三：只剩一个人结束
        if (current instanceof TNTTag) {
            if (countAlivePlayers() <= 1) {
                current.getTimeline().nextState();
            }
        }
    }

    private static int countAlivePlayers() {
        int alive = 0;
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getGameMode() != GameMode.SPECTATOR)
                alive++;
        }
        return alive;
    }

    private static int countAliveTeams() {
        Set<Team> aliveTeams = new HashSet<>();
        for (Player p : Bukkit.getOnlinePlayers()) {
            if (p.getGameMode() == GameMode.SURVIVAL || p.getGameMode() == GameMode.ADVENTURE) {
                Team t = MCETeamUtils.getTeam(p);
                if (t != null)
                    aliveTeams.add(t);
            }
        }
        return aliveTeams.size();
    }
}
