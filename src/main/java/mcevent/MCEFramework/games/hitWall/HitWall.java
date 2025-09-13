package mcevent.MCEFramework.games.hitWall;

import lombok.Getter;
import lombok.Setter;
import mcevent.MCEFramework.MCEMainController;
import mcevent.MCEFramework.games.captureCenter.gameObject.CaptureCenterGameBoard;
import mcevent.MCEFramework.games.hitWall.customHandler.PlayerFallHandler;
import mcevent.MCEFramework.generalGameObject.MCEGame;
import mcevent.MCEFramework.tools.*;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.GameRule;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

import static mcevent.MCEFramework.miscellaneous.Constants.saturation;
import static mcevent.MCEFramework.tools.MCEPlayerUtils.grantGlobalPotionEffect;

/*
 * HitWall: 墙洞洞墙完整实现
 */
@Getter
@Setter
public class HitWall extends MCEGame {

	private PlayerFallHandler playerFallHandler = new PlayerFallHandler();
	private List<BukkitRunnable> gameTask = new ArrayList<>();
	private HitWallConfigParser hitWallConfigParser = new HitWallConfigParser();

	public HitWall(String title, int id, String mapName, int round, boolean isMultiGame, String configFileName,
						 int launchDuration, int introDuration, int preparationDuration, int cyclePreparationDuration,
						 int cycleStartDuration, int cycleEndDuration, int endDuration) {
		super(title, id, mapName, round, isMultiGame, configFileName,
				launchDuration, introDuration, preparationDuration, cyclePreparationDuration, cycleStartDuration, cycleEndDuration, endDuration);
	}

	@Override
	public void onLaunch() {
		HitWallFuncImpl.loadConfig();

		World world = Bukkit.getWorld(this.getWorldName());
		if (world != null) {
			world.setGameRule(GameRule.FALL_DAMAGE, false);
			world.setGameRule(GameRule.KEEP_INVENTORY, false);
		}

		setActiveTeams(MCETeamUtils.getActiveTeams());
		MCETeleporter.globalSwapWorld(this.getWorldName());

		HitWallFuncImpl.teleportPlayersToSpawn();

		MCEWorldUtils.disablePVP();
		MCEPlayerUtils.globalSetGameModeDelayed(GameMode.ADVENTURE, 5L);

		HitWallFuncImpl.initializePlayers();

		this.getGameBoard().setStateTitle("<red><bold> 游戏开始：</bold></red>");
		grantGlobalPotionEffect(saturation);
		MCEPlayerUtils.clearGlobalTags();

		// 关闭玩家碰撞；穿鞋子

		playerFallHandler.register(this);
	}

	@Override
	public void onCycleStart() {
		HitWallFuncImpl.resetGameBoard();
		this.getGameBoard().setStateTitle("<red><bold> 剩余时间：</bold></red>");

		MCEWorldUtils.disablePVP();

		// 声音，待修改
		MCEPlayerUtils.globalPlaySound("minecraft:disco_fever");
		MCEPlayerUtils.globalGrantTag("Active");

		// 设置隐身

		// 启动游戏进程 (循环)
		gameTask.add(MCETimerUtils.setDelayedTask(5, () -> {
			HitWallFuncImpl.startWallPushing(this);
		}));
	}

	@Override
	public void onEnd() {
		HitWallFuncImpl.sendWinningMessage();
		MCEPlayerUtils.globalSetGameMode(GameMode.SPECTATOR);

		// onEnd结束后立即清理展示板和资源，然后启动投票系统
		setDelayedTask(getEndDuration(), () -> {
			MCEPlayerUtils.globalClearFastBoard();
			this.stop(); // 停止所有游戏资源
			MCEMainController.launchVotingSystem(); // 立即启动投票系统
		});
	}

	@Override
	public void initGameBoard() {
		setGameBoard(new CaptureCenterGameBoard(getTitle(), getWorldName()));
	}

	@Override
	public void stop() {
		super.stop();

		MCEPlayerUtils.globalStopMusic();

		HitWallFuncImpl.clearGameTasks(this);
	}
}
