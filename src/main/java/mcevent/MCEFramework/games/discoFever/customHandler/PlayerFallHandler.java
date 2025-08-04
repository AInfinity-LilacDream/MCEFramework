package mcevent.MCEFramework.games.discoFever.customHandler;

import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;
import lombok.Setter;
import mcevent.MCEFramework.games.discoFever.gameObject.DiscoFeverGameBoard;
import mcevent.MCEFramework.generalGameObject.MCEResumableEventHandler;
import mcevent.MCEFramework.tools.MCEMessenger;
import mcevent.MCEFramework.tools.MCEPlayerUtils;
import mcevent.MCEFramework.tools.MCETeamUtils;
import static mcevent.MCEFramework.miscellaneous.Constants.*;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scoreboard.Team;

import java.util.Objects;

/*
PlayerCaughtHandler: 玩家坠落事件监听器
 */
public class PlayerFallHandler extends MCEResumableEventHandler implements Listener {

    @Setter
    private boolean inGame = false;

    public PlayerFallHandler() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (this.isSuspended()) return;

        Player player = event.getPlayer();
        if (player.getY() <= 3 && player.getGameMode() != GameMode.SPECTATOR) {
            if (!inGame) {
                player.teleport(player.getWorld().getSpawnLocation());
            }
            else {
                DiscoFeverGameBoard gameBoard = (DiscoFeverGameBoard) discoFever.getGameBoard();
                Team team = MCETeamUtils.getTeam(player);

                MCEMessenger.sendGlobalInfo(MCEPlayerUtils.getColoredPlayerName(player) + " <gray>掉入了虚空！</gray>");
                player.setGameMode(GameMode.SPECTATOR);
                gameBoard.updatePlayerRemainTitle(gameBoard.getPlayerRemain() - 1);
                gameBoard.updateTeamRemainTitle(team);
                gameBoard.globalDisplay();

                if (gameBoard.getTeamRemain()[discoFever.getTeamId(team)] == 0)
                    if (team != null)
                        MCEMessenger.sendGlobalInfo(team.getName() + "<gray>已被团灭！</gray>");

                if (gameBoard.getPlayerRemain() == 0) {
                    this.suspend(); // 防止重复触发nextState
                    discoFever.getTimeline().nextState();
                }
            }
        }
    }
}
