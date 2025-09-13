package mcevent.MCEFramework.games.hitWall.customHandler;

import lombok.Setter;
import mcevent.MCEFramework.games.captureCenter.CaptureCenter;
import mcevent.MCEFramework.games.hitWall.HitWall;
import mcevent.MCEFramework.games.hitWall.HitWallFuncImpl;
import mcevent.MCEFramework.games.hitWall.gameObject.HitWallGameBoard;
import mcevent.MCEFramework.generalGameObject.MCEResumableEventHandler;
import mcevent.MCEFramework.tools.MCETeamUtils;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scoreboard.Team;

import static mcevent.MCEFramework.miscellaneous.Constants.*;

public class PlayerFallHandler extends MCEResumableEventHandler implements Listener {
	@Setter
	private boolean inGame = false, isRegistered = false;
	private HitWall game;

	public PlayerFallHandler() {
		plugin.getServer().getPluginManager().registerEvents(this, plugin);
	}

	public void register(HitWall hitWall) {
		this.game = hitWall;
		if (!isRegistered) {
			Bukkit.getPluginManager().registerEvents(this, plugin);
			isRegistered = true;
		}
	}

	public void unregister() {
		if (isRegistered) {
			HandlerList.unregisterAll(this);
			isRegistered = false;
		}
	}

	@EventHandler
	public void onPlayerMove(PlayerMoveEvent event) {
		if (this.isSuspended()) return;

		Player player = event.getPlayer();
		if (player.getY() <= HitWallFuncImpl.KILL_Y && player.getGameMode() != GameMode.SPECTATOR) {
			if (!inGame) {
				// 如果游戏未开始，则传送回去
				// *** 位置待定
				player.teleport(player.getWorld().getSpawnLocation());
			}
			else {
				HitWallGameBoard gameBoard = (HitWallGameBoard) hitWall.getGameBoard();
				Team team = MCETeamUtils.getTeam(player);

				HitWallFuncImpl.handlePlayerFallIntoVoid(player);
			}
		}
	}
}
