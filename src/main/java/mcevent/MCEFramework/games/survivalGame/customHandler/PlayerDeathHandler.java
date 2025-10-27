package mcevent.MCEFramework.games.survivalGame.customHandler;

import mcevent.MCEFramework.games.survivalGame.SurvivalGameFuncImpl;
import mcevent.MCEFramework.games.survivalGame.gameObject.SurvivalGameGameBoard;
import mcevent.MCEFramework.generalGameObject.MCEResumableEventHandler;
import mcevent.MCEFramework.tools.MCETeamUtils;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.scoreboard.Team;

import static mcevent.MCEFramework.miscellaneous.Constants.*;

/*
PlayerDeathHandler: 玩家死亡处理器（生成战利品箱并更新计分板）
 */
public class PlayerDeathHandler extends MCEResumableEventHandler implements Listener {

    public PlayerDeathHandler() {
        setSuspended(true); // 默认不激活
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (isSuspended())
            return;

        Player player = event.getPlayer();
        // 非参与者（未持有 Active 或不在本游戏世界）不处理
        if (!survivalGame.isGameParticipant(player))
            return;
        Player killer = player.getKiller();

        // 在玩家死亡位置创建战利品箱
        SurvivalGameFuncImpl.createDeathChest(player, player.getLocation());

        // 记录击杀者
        if (killer != null) {
            SurvivalGameFuncImpl.registerKill(killer);
        }

        // 清空掉落物（因为已经放入箱子）
        event.getDrops().clear();

        // 设置玩家为观察者模式
        player.setGameMode(GameMode.SPECTATOR);

        // 更新计分板
        SurvivalGameGameBoard gameBoard = (SurvivalGameGameBoard) survivalGame.getGameBoard();
        Team team = MCETeamUtils.getTeam(player);

        if (team != null) {
            gameBoard.updateTeamRemainTitle(team);
            // 若该队伍人数归零，登记淘汰顺序
            int teamId = survivalGame.getTeamId(team);
            if (teamId >= 0 && teamId < gameBoard.getTeamRemain().length) {
                if (gameBoard.getTeamRemain()[teamId] == 0) {
                    SurvivalGameFuncImpl.registerTeamElimination(team);
                }
            }
        }

        // 更新剩余玩家数
        int remainingPlayers = 0;
        for (Player p : org.bukkit.Bukkit.getOnlinePlayers()) {
            if (p.getGameMode() != GameMode.SPECTATOR) {
                remainingPlayers++;
            }
        }
        gameBoard.updatePlayerRemainTitle(remainingPlayers);

        // 检查胜利条件
        SurvivalGameFuncImpl.checkWinCondition();
    }
}
