package mcevent.MCEFramework.games.hitWall.customHandler;

import lombok.Setter;
import mcevent.MCEFramework.games.hitWall.HitWall;
import mcevent.MCEFramework.games.hitWall.HitWallFuncImpl;
import mcevent.MCEFramework.generalGameObject.MCEResumableEventHandler;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import static mcevent.MCEFramework.miscellaneous.Constants.plugin;

public class PlayerFallHandler extends MCEResumableEventHandler implements Listener {
	private HitWall game;
	private boolean registered = false;
	@Setter
	private boolean preparationPhase = true;

	public void register(HitWall hitWall) {
		this.game = hitWall;
		if (!registered) {
			plugin.getServer().getPluginManager().registerEvents(this, plugin);
			registered = true;
		}
	}

	@Override
	public void start() {
		setSuspended(false);
	}

	@Override
	public void suspend() {
		setSuspended(true);
	}

	@EventHandler
	public void onPlayerMove(PlayerMoveEvent event) {
		if (isSuspended() || game == null) {
			return;
		}

		Player player = event.getPlayer();
		if (!player.getWorld().getName().equals(game.getWorldName())) {
			return;
		}
		if (!player.getScoreboardTags().contains("Participant")) {
			return;
		}
		if (player.getScoreboardTags().contains("dead")) {
			return;
		}
		if (!preparationPhase && !player.getScoreboardTags().contains("Active")) {
			return;
		}
		if (player.getGameMode() == GameMode.SPECTATOR) {
			return;
		}

		if (player.getLocation().getY() > HitWallFuncImpl.getKillY()) {
			return;
		}

//		if (preparationPhase) {
//			HitWallFuncImpl.teleportPlayerToSpawn(game, player);
//			return;
//		}

		HitWallFuncImpl.handlePlayerFallIntoVoid(game, player);
	}
}
