package mcevent.MCEFramework.games.hitWall;

import lombok.Getter;
import lombok.Setter;
import mcevent.MCEFramework.MCEMainController;
import mcevent.MCEFramework.games.hitWall.customHandler.PlayerFallHandler;
import mcevent.MCEFramework.games.hitWall.gameObject.HitWallGameBoard;
import mcevent.MCEFramework.generalGameObject.MCEGame;
import mcevent.MCEFramework.tools.*;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Team;

import java.util.ArrayList;
import java.util.List;

import static mcevent.MCEFramework.miscellaneous.Constants.plugin;
import static mcevent.MCEFramework.miscellaneous.Constants.saturation;
import static mcevent.MCEFramework.tools.MCEPlayerUtils.grantGlobalPotionEffect;

/**
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
		// 推墙阶段不依赖固定时长，由波次结束主动驱动
		setCycleStartDuration(Integer.MAX_VALUE);
	}

	@Override
	public void onLaunch() {
		HitWallFuncImpl.loadConfig(this);
		if (!HitWallFuncImpl.ensureMapWorldLoaded(this)) {
			plugin.getLogger().warning("HitWall: 地图世界加载失败，已终止游戏启动流程。");
			return;
		}

		World world = Bukkit.getWorld(this.getWorldName());
		if (world != null) {
			world.setGameRule(GameRule.FALL_DAMAGE, false);
			world.setGameRule(GameRule.KEEP_INVENTORY, false);
		}
		HitWallFuncImpl.clearAllWallPaths(this);

		setActiveTeams(MCETeamUtils.getActiveTeams());
		MCETeleporter.globalSwapWorld(this.getWorldName());

		HitWallFuncImpl.teleportPlayersToSpawn(this);

		MCEWorldUtils.disablePVP();
		MCEPlayerUtils.globalSetGameModeDelayed(GameMode.ADVENTURE, 5L);

		HitWallFuncImpl.initializePlayers(this);

		this.getGameBoard().setStateTitle("<red><bold> 游戏开始: </bold></red>");
		grantGlobalPotionEffect(saturation);
		MCEPlayerUtils.clearGlobalTags();

		playerFallHandler.register(this);
		playerFallHandler.setPreparationPhase(true);
		playerFallHandler.start();

		// 关闭玩家碰撞；穿鞋子
		setTeamsCollision(false);
		equipTeamBootsForAllPlayers();

//		HitWallFuncImpl.teleportPlayersToObsidianPlatform(this);
	}

	@Override
	public void onCyclePreparation() {
		HitWallFuncImpl.teleportPlayersToObsidianPlatform(this);
		if (getGameBoard() instanceof HitWallGameBoard board) {
			board.setShowTimer(true);
		}
		this.getGameBoard().setStateTitle("<red><bold> 剩余轮次: </bold></red>");
		// 设置隐身
		grantGlobalPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, Integer.MAX_VALUE, 1, false, false, false));
		grantGlobalPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 20 * 600, 0, false, false, false));
	}

	@Override
	public void onCycleStart() {
		HitWallFuncImpl.resetGameBoard(this);
//		HitWallFuncImpl.teleportPlayersToObsidianPlatform(this);
//		this.getGameBoard().setStateTitle("<red><bold> 剩余波次: </bold></red>");
		if (getGameBoard() instanceof HitWallGameBoard board) {
			board.setShowTimer(false);
		}

		MCEWorldUtils.disablePVP();

		// 声音，待修改
		MCEPlayerUtils.globalPlaySound("minecraft:disco_fever");
		MCEPlayerUtils.globalGrantTag("Active");

		equipTeamBootsForAllPlayers();
		// 启动游戏进程 (循环)
		gameTask.add(MCETimerUtils.setDelayedTask(0, () -> {
			HitWallFuncImpl.startWallPushing(this);
		}));
	}

	@Override
	public void onCycleEnd() {
		playerFallHandler.setPreparationPhase(true);
		playerFallHandler.start();
		if (getGameBoard() instanceof HitWallGameBoard board) {
			board.setShowTimer(true); // 回合结束阶段显示倒计时
		}
	}

	@Override
	public void onEnd() {
		playerFallHandler.suspend();
		if (getGameBoard() instanceof HitWallGameBoard board) {
			board.setShowTimer(true);
		}
		this.getGameBoard().setStateTitle("<gold><bold> 结束倒计时: </bold></gold>");
		HitWallFuncImpl.sendWinningMessage(this);
		MCEPlayerUtils.globalSetGameMode(GameMode.SPECTATOR);

		// onEnd结束后立即清理展示板和资源，然后启动投票系统
		setDelayedTask(getEndDuration(), () -> {
			MCEPlayerUtils.globalClearFastBoard();
			this.stop(); // 停止所有游戏资�?
			MCEMainController.launchVotingSystem(); // 立即启动投票系统
		});
	}

	@Override
	public void initGameBoard() {
		setGameBoard(new HitWallGameBoard(this));
	}

	@Override
	public void stop() {
		super.stop();

		playerFallHandler.suspend();
		MCEPlayerUtils.globalStopMusic();

		HitWallFuncImpl.clearGameTasks(this);
		setTeamsCollision(true);
	}

	private void setTeamsCollision(boolean enabled) {
		ArrayList<org.bukkit.scoreboard.Team> teams = getActiveTeams();
		if (teams == null || teams.isEmpty()) {
			teams = MCETeamUtils.getActiveTeams();
		}
		if (teams == null) {
			return;
		}
		for (org.bukkit.scoreboard.Team team : teams) {
			if (team == null) {
				continue;
			}
			try {
				team.setOption(org.bukkit.scoreboard.Team.Option.COLLISION_RULE,
						enabled ? org.bukkit.scoreboard.Team.OptionStatus.ALWAYS : org.bukkit.scoreboard.Team.OptionStatus.NEVER);
			} catch (Throwable ignored) {
			}
		}
	}

	private void equipTeamBootsForAllPlayers() {
		for (Player player : Bukkit.getOnlinePlayers()) {
			Team team = MCETeamUtils.getTeam(player);
			if (team == null)
				continue;
			Color color = mapTeamToLeatherColor(team);
			if (color == null)
				continue;
			ItemStack boots = new ItemStack(Material.LEATHER_BOOTS);
			LeatherArmorMeta meta = (LeatherArmorMeta) boots.getItemMeta();
			if (meta != null) {
				meta.setColor(color);
				meta.setUnbreakable(true);
				boots.setItemMeta(meta);
			}
			player.getInventory().setBoots(boots);
		}
	}

	private Color mapTeamToLeatherColor(Team team) {
		String name = team.getName();
		if (name.contains("红"))
			return Color.RED;
		if (name.contains("橙"))
			return Color.ORANGE;
		if (name.contains("黄"))
			return Color.YELLOW;
		if (name.contains("翠") || name.contains("绿"))
			return Color.LIME;
		if (name.contains("青") || name.contains("缥"))
			return Color.AQUA;
		if (name.contains("蓝"))
			return Color.BLUE;
		if (name.contains("紫") || name.contains("粉"))
			return Color.FUCHSIA;
		return Color.WHITE;
	}
}



