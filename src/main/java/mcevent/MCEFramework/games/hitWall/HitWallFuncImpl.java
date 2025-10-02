package mcevent.MCEFramework.games.hitWall;


import mcevent.MCEFramework.games.hitWall.gameObject.HitWallGameBoard;
import mcevent.MCEFramework.tools.MCEMessenger;
import mcevent.MCEFramework.tools.MCEPlayerUtils;
import mcevent.MCEFramework.tools.MCETeamUtils;
import mcevent.MCEFramework.tools.MCETimerUtils;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Team;

import java.util.*;

import static mcevent.MCEFramework.miscellaneous.Constants.hitWall;

public class HitWallFuncImpl {

	private static final double HITWALL_SPAWN_X = 0, HITWALL_SPAWN_Y = 0, HITWALL_SPAWN_Z = 0;

	public static final double KILL_Y = 3;	// 待定
	private static int currentWave = 0;
	private static List<Integer> speedList = new ArrayList();
	private static List<Direction> directionList = new ArrayList();
	private static List<PotionEffect> potionEffectList = new ArrayList();

	private static final HitWallConfigParser hitWallConfigParser = hitWall.getHitWallConfigParser();

	private static BukkitRunnable pushingWallTask;

	private enum Direction { NORTH, EAST, SOUTH, WEST };
	/**
	 * 从配置文件加载数据
	 */
	public static void loadConfig() {
		hitWall.setIntroTextList(hitWallConfigParser.openAndParse(hitWall.getConfigFileName()));
	}

	/**
	 * 传送玩家到出生点
	 */
	public static void teleportPlayersToSpawn() {
		World world = Bukkit.getWorld(hitWall.getWorldName());
		if (world == null) return;

		Location spawnLocation = new Location(world, HITWALL_SPAWN_X, HITWALL_SPAWN_Y, HITWALL_SPAWN_Z);
		for (Player player : Bukkit.getOnlinePlayers()) {
			player.teleport(spawnLocation);
		}
	}

	/**
	 * 初始化玩家属性
	 */
	public static void initializePlayers() {
		for (Player player : Bukkit.getOnlinePlayers()) {
			Objects.requireNonNull(player.getAttribute(Attribute.MAX_HEALTH)).setBaseValue(20.0);
			player.setHealth(20.0);
			player.getInventory().clear();
		}
	}

	/**
	 * 处理玩家掉落虚空
	 */
	public static void handlePlayerFallIntoVoid(Player player) {
		MCEMessenger.sendGlobalInfo(
				MCEPlayerUtils.getColoredPlayerName(player) + " <gray>掉入了虚空！</gray>"
		);

		// 将玩家切换为旁观模式
		player.setGameMode(GameMode.SPECTATOR);
		player.setHealth(player.getMaxHealth());

		// 更新计分板
		HitWallGameBoard gameBoard = (HitWallGameBoard) hitWall.getGameBoard();

		int alivePlayerCount = 0;
		for (Player p : Bukkit.getOnlinePlayers()) {
			if (p.getGameMode() == GameMode.ADVENTURE) {
				alivePlayerCount++;
			}
		}
		gameBoard.updatePlayerCount(alivePlayerCount);

		Set<String> aliveTeams = new HashSet<>();
		for (Player p : Bukkit.getOnlinePlayers()) {
			if (p.getGameMode() == GameMode.ADVENTURE) {
				Team playerTeam = MCETeamUtils.getTeam(p);
				if  (playerTeam != null) {
					aliveTeams.add(playerTeam.getName());
				}
			}
		}
		gameBoard.updateTeamCount(aliveTeams.size());

		// 检查游戏是否结束
		if (alivePlayerCount == 0 || aliveTeams.size() <= 1) {
			hitWall.getTimeline().nextState();
		}
	}

	/**
	 * 重置玩家计分板
	 */
	public static void resetGameBoard()	{
		HitWallGameBoard gameBoard = (HitWallGameBoard) hitWall.getGameBoard();
		gameBoard.updatePlayerCount(Bukkit.getOnlinePlayers().size());
		gameBoard.updateTeamCount(hitWall.getActiveTeams().size());

		// 分数统计初始化，还没做
	}

	/**
	 * 发送获胜消息
	 */
	public static void sendWinningMessage() {
		List<Player> survivingPlayers = new ArrayList<>();
		for (Player player : Bukkit.getOnlinePlayers()) {
			if (player.getGameMode() == GameMode.ADVENTURE) {
				survivingPlayers.add(player);
			}
		}

		if (!survivingPlayers.isEmpty()) {
			StringBuilder survivorMessage = new StringBuilder();
			for (int i = 0; i < survivingPlayers.size(); i++) {
				survivorMessage.append(MCEPlayerUtils.getColoredPlayerName(survivingPlayers.get(i)));
				if (i == survivingPlayers.size() - 2 && survivingPlayers.size() > 1) {
					// 倒数第二个玩家，添加"和"
					survivorMessage.append("和");
				} else if (i < survivingPlayers.size() - 1) {
					// 不是最后一个玩家，添加逗号
					survivorMessage.append(", ");
				}
			}
			survivorMessage.append(" <aqua>是最后存活的玩家！</aqua>");
			MCEMessenger.sendGlobalInfo(survivorMessage.toString());
		}
	}

	public static void startWallPushing(HitWall game) {
		scheduleWallPushing(game, Direction.NORTH, speedList.get(0), 0, new PotionEffect(PotionEffectType.SPEED, 6000, 1));
	}

	/**
	 * 每次推墙
	 * @param game this
	 * @param direction 从...方向推来，配置文件或者随机
	 * @param speed 速度，由 config 读取，待实现
	 * @param delaySeconds 延迟时间，需要根据 speed 计算，待测定
	 * @param effect 每次添加的状态效果
	 */
	private static void scheduleWallPushing(HitWall game, Direction direction, int speed, int delaySeconds, PotionEffect effect) {
		game.getGameTask().add(MCETimerUtils.setDelayedTask(delaySeconds, () -> {
			currentWave++;
			// 此处格式待修改
			MCEMessenger.sendGlobalTitle(
					"当前效果 " + effect.getType(),
					"第 " + currentWave + " 波"
			);
			// 给状态效果
			for (Player player : Bukkit.getOnlinePlayers()) {
				if (player.getGameMode() != GameMode.SPECTATOR) {
					player.addPotionEffect(effect);
				}
			}

			pushWall(game, direction, speed);
		}));
	}

	/**
	 * 准备实现：推动墙
	 * @param game 同上
	 * @param direction 同上
	 * @param speed 同上
	 */
	private static void pushWall(HitWall game, Direction direction, int speed) {

	}

	/**
	 * 清理游戏任务
	 */
	public static void clearGameTasks(HitWall game) {
		for (BukkitRunnable task : game.getGameTask()) {
			task.cancel();
		}
		game.getGameTask().clear();
	}
}
