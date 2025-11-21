package mcevent.MCEFramework.games.hitWall;

import com.comphenix.protocol.wrappers.EnumWrappers;
import mcevent.MCEFramework.games.hitWall.HitWallSettings.WallWave;
import mcevent.MCEFramework.games.hitWall.gameObject.HitWallGameBoard;
import mcevent.MCEFramework.generalGameObject.MCEGame;
import mcevent.MCEFramework.generalGameObject.MCEGameBoard;
import mcevent.MCEFramework.tools.MCEMessenger;
import mcevent.MCEFramework.tools.MCEPlayerUtils;
import mcevent.MCEFramework.tools.MCETimerUtils;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.Directional;
import org.bukkit.block.data.FaceAttachable;
import org.bukkit.block.data.type.Switch;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import static mcevent.MCEFramework.miscellaneous.Constants.hitWall;
import static mcevent.MCEFramework.miscellaneous.Constants.plugin;

public final class HitWallFuncImpl {

	private static HitWallSettings settings = HitWallSettings.defaultSettings();
	private static int currentWave = 0;
	private static int totalWaves = 0;
	private static final int DEFAULT_PUSH_CYCLES = 12;
	private static PotionEffect activeWaveEffect = null;
	private static boolean wavesCompleted = false;
	private static final int SHRINK_TRIGGER_WAVES = 5;
	private static final int SHRINK_BASE_POINTS = 2; // 每波选择的基础点，镜像后最多 8
	private static final int SHRINK_FLICKER_COUNT = 3;
	private static final int SHRINK_FLICKER_INTERVAL_TICKS = 20; // 1s
	private static final int PLATFORM_MIN_X = 1;
	private static final int PLATFORM_MAX_X = 11;
	private static final int PLATFORM_MIN_Z = -128;
	private static final int PLATFORM_MAX_Z = -118;
	private static final int PLATFORM_Y = 8;
	private static final int PLATFORM_AXIS_X = 6;
	private static final int PLATFORM_AXIS_Z = -123;
	private static final java.util.Set<String> removedPlatformKeys = new java.util.HashSet<>();
	private static final java.util.Map<String, BlockState> platformSnapshots = new java.util.HashMap<>();

	private HitWallFuncImpl() {
	}

	/**
	 * 读取配置并同步游戏字段与静态设置
	 */
	public static void loadConfig(HitWall game) {
		if (game == null) {
			return;
		}
		HitWallConfigParser parser = game.getHitWallConfigParser();
		game.setIntroTextList(parser.openAndParse(game.getConfigFileName()));
		HitWallSettings parsed = parser.getSettings();
		if (parsed != null) {
			settings = parsed;
			if (parsed.getWorldName() != null && !parsed.getWorldName().isBlank()) {
				game.setWorldName(parsed.getWorldName());
			}
		}
	}

	/**
	 * 确保地图世界已加载或尝试创建
	 */
	public static boolean ensureMapWorldLoaded(HitWall game) {
		if (game == null) {
			return false;
		}
		String worldName = settings.getWorldName();
		if (worldName == null || worldName.isBlank()) {
			worldName = game.getWorldName();
		}
		if (worldName == null || worldName.isBlank()) {
			plugin.getLogger().warning("HitWall: 未在配置中找到有效的世界名称，无法加载地图。");
			return false;
		}
		World existing = Bukkit.getWorld(worldName);
		if (existing != null) {
			game.setWorldName(existing.getName());
			settings.setWorldName(existing.getName());
			return true;
		}
		try {
			plugin.getLogger().info("HitWall: 未加载世界 " + worldName + "，正在尝试创建/加载该世界。");
			WorldCreator creator = new WorldCreator(worldName);
			World created = creator.createWorld();
			if (created == null) {
				plugin.getLogger().warning("HitWall: 无法创建世界 " + worldName + "，请检查地图文件是否存在。");
				return false;
			}
			game.setWorldName(created.getName());
			settings.setWorldName(created.getName());
			return true;
		} catch (Throwable t) {
			plugin.getLogger().severe("HitWall: 加载世界 " + worldName + " 时出错: " + t.getMessage());
			return false;
		}
	}

	/**
	 * 将当前世界玩家传送至出生点
	 */
	public static void teleportPlayersToSpawn(HitWall game) {
		Location spawn = resolveSpawnLocation(game);
		if (spawn == null) {
			return;
		}
		World world = spawn.getWorld();
		if (world == null) {
			return;
		}
		for (Player player : world.getPlayers()) {
			player.teleport(spawn);
		}
	}

	/**
	 * 将指定玩家传送至出生点
	 */
	public static void teleportPlayerToSpawn(HitWall game, Player player) {
		if (player == null) {
			return;
		}
		Location spawn = resolveSpawnLocation(game);
		if (spawn != null) {
			player.teleport(spawn);
		}
	}

	/**
	 * 获取虚空淘汰的 Y 阈值
	 */
	public static double getKillY() {
		return settings.getKillY();
	}

	/**
	 * 初始化玩家生命与背包
	 */
	public static void initializePlayers(HitWall game) {
		if (game == null) {
			return;
		}
		World world = Bukkit.getWorld(game.getWorldName());
		if (world == null) {
			return;
		}
		for (Player player : world.getPlayers()) {
			AttributeInstance health = player.getAttribute(Attribute.MAX_HEALTH);
			if (health != null) {
				health.setBaseValue(20.0);
			}
			AttributeInstance playerMaxHealth = player.getAttribute(Attribute.MAX_HEALTH);
			double targetHealth = playerMaxHealth != null ? playerMaxHealth.getValue() : 20.0;
			player.setHealth(targetHealth);
			player.getInventory().clear();
		}
	}

	/**
	 * 处理玩家坠入虚空的判负逻辑
	 */
	public static void handlePlayerFallIntoVoid(HitWall game, Player player) {
		if (game == null || player == null) {
			return;
		}
		MCEMessenger.sendGlobalInfo(MCEPlayerUtils.getColoredPlayerName(player) + " <gray>掉入了虚空！</gray>");
		player.addScoreboardTag("dead");
		player.removeScoreboardTag("Active");
		player.setGameMode(GameMode.SPECTATOR);
		AttributeInstance playerMaxHealth = player.getAttribute(Attribute.MAX_HEALTH);
		double targetHealth = playerMaxHealth != null ? playerMaxHealth.getValue() : 20.0;
		player.setHealth(targetHealth);
		player.getActivePotionEffects().forEach(effect ->
				player.removePotionEffect(effect.getType())
		);

		if (game.getGameBoard() instanceof HitWallGameBoard board) {
			int alivePlayers = MCEGameBoard.countRemainingParticipants();
			int aliveTeams = MCEGameBoard.countRemainingParticipantTeams();
			board.updatePlayerCount(alivePlayers);
			board.updateTeamCount(aliveTeams);

			if (alivePlayers == 0 || aliveTeams <= 1) {
				clearGameTasks(game);
				game.getTimeline().nextState();
			}
		}
	}

	/**
	 * 重置计分牌显示
	 */
	public static void resetGameBoard(HitWall game) {
		if (game == null) {
			return;
		}
		if (game.getGameBoard() instanceof HitWallGameBoard board) {
			board.updatePlayerCount(MCEGameBoard.countRemainingParticipants());
			board.updateTeamCount(MCEGameBoard.countRemainingParticipantTeams());
		}
	}

	/**
	 * 公布存活玩家
	 */
	public static void sendWinningMessage(HitWall game) {
		if (game == null) {
			return;
		}
		World world = Bukkit.getWorld(game.getWorldName());
		if (world == null) {
			return;
		}

		List<Player> survivors = new ArrayList<>();
		for (Player player : world.getPlayers()) {
			if (!player.getScoreboardTags().contains("Participant")) {
				continue;
			}
			if (player.getGameMode() == GameMode.SPECTATOR) {
				continue;
			}
			survivors.add(player);
		}

		if (survivors.isEmpty()) {
			return;
		}

		StringBuilder survivorMessage = new StringBuilder();
		for (int i = 0; i < survivors.size(); i++) {
			survivorMessage.append(MCEPlayerUtils.getColoredPlayerName(survivors.get(i)));
			if (i == survivors.size() - 2) {
				survivorMessage.append(" 和 ");
			} else if (i < survivors.size() - 2) {
				survivorMessage.append(", ");
			}
		}
		survivorMessage.append(" <aqua>是最后存活的玩家！</aqua>");
		MCEMessenger.sendGlobalInfo(survivorMessage.toString());
	}

	/**
	 * 启动推墙流程
	 */
	public static void startWallPushing(HitWall game) {
		if (game == null) {
			return;
		}
		ensurePlatformSnapshot(game);
		List<WallWave> waves = resolveWaves();
		if (waves.isEmpty()) {
			plugin.getLogger().warning("HitWall: 未配置任何墙体波次，无法启动推墙流程。");
			return;
		}
		currentWave = 0;
		totalWaves = waves.size();
		wavesCompleted = false;
		scheduleWave(game, waves, 0);
	}

	/**
	 * 按顺序调度每一波
	 */
	private static void scheduleWave(HitWall game, List<WallWave> waves, int index) {
		if (game == null || waves == null) {
			return;
		}
		if (index >= waves.size()) {
			finishAllWaves(game);
			return;
		}
		WallWave wave = waves.get(index);
		if (wave == null) {
			scheduleWave(game, waves, index + 1);
			return;
		}
		int delay = Math.max(0, wave.getDelaySeconds());
		Runnable startWave = () -> runWave(game, wave, () -> scheduleWave(game, waves, index + 1));
		if (delay > 0) {
			game.getGameTask().add(MCETimerUtils.setDelayedTask(delay, startWave::run));
		} else {
			startWave.run();
		}
	}

	/**
	 * 执行单波推墙及效果
	 */
	private static void runWave(HitWall game, WallWave wave, Runnable onComplete) {
		if (game == null || wave == null) {
			safeInvoke(onComplete);
			return;
		}
		currentWave++;
		schedulePlatformShrinkIfNeeded(game);
		MCEMessenger.sendGlobalTitle("", "<gray><obfuscated>abcd</obfuscated></gray>");
		java.util.EnumMap<BlockFace, PreparedWave> prepared = new java.util.EnumMap<>(BlockFace.class);
		for (BlockFace face : new BlockFace[]{BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST}) {
			PreparedWave pw = prepareDirection(game, wave, face, null, null);
			if (pw != null) {
				prepared.put(face, pw);
			}
		}

		if (prepared.isEmpty()) {
			clearActiveWaveEffect(game);
			safeInvoke(onComplete);
			return;
		}

		Runnable finishWave = () -> {
			clearActiveWaveEffect(game);
			safeInvoke(onComplete);
		};

		BukkitRunnable delay = new BukkitRunnable() {
			@Override
			public void run() {
				executeWaveMechanism(game, wave, prepared, finishWave);
			}
		};
		delay.runTaskLater(plugin, 40L);
		game.getGameTask().add(delay);
	}

	/**
	 * 获取剩余波次数（不含当前已开始的波）
	 */
	public static int getRemainingWaves() {
		if (wavesCompleted) {
			return 0;
		}
		return Math.max(0, totalWaves - currentWave);
	}

	/**
	 * 获取总波次数
	 */
	public static int getTotalWaves() {
		return totalWaves;
	}

	private static void finishAllWaves(HitWall game) {
		if (wavesCompleted) {
			return;
		}
		wavesCompleted = true;
		clearGameTasks(game);
		restorePlatformBlocks(game);
		clearActiveWaveEffect(game);
		try {
			if (game != null && game.getTimeline() != null) {
				game.getTimeline().nextState();
			}
		} catch (Throwable t) {
			plugin.getLogger().warning("HitWall: 完成所有波次后推进时间线失败: " + t.getMessage());
		}
	}

	private static void ensurePlatformSnapshot(HitWall game) {
		if (game == null || !platformSnapshots.isEmpty()) {
			return;
		}
		World world = Bukkit.getWorld(game.getWorldName());
		if (world == null) {
			return;
		}
		for (int x = PLATFORM_MIN_X; x <= PLATFORM_MAX_X; x++) {
			for (int z = PLATFORM_MIN_Z; z <= PLATFORM_MAX_Z; z++) {
				Block block = world.getBlockAt(x, PLATFORM_Y, z);
				platformSnapshots.put(key(x, PLATFORM_Y, z), block.getState());
			}
		}
	}

	private static void schedulePlatformShrinkIfNeeded(HitWall game) {
		if (game == null) {
			return;
		}
		int remaining = getRemainingWaves();
		if (remaining <= 0 || remaining > SHRINK_TRIGGER_WAVES) {
			return;
		}
		List<Location> targets = pickShrinkTargets(game);
		if (!targets.isEmpty()) {
			scheduleFlickerAndRemove(game, targets);
		}
	}

	private static List<Location> pickShrinkTargets(HitWall game) {
		List<Location> chosen = new ArrayList<>();
		World world = Bukkit.getWorld(game.getWorldName());
		if (world == null) {
			return chosen;
		}
		ThreadLocalRandom random = ThreadLocalRandom.current();
		for (int i = 0; i < SHRINK_BASE_POINTS; i++) {
			int tries = 0;
			while (tries < 200) {
				int x = random.nextInt(PLATFORM_MIN_X, PLATFORM_MAX_X + 1);
				int z = random.nextInt(PLATFORM_MIN_Z, PLATFORM_MAX_Z + 1);
				int y = PLATFORM_Y;
				String baseKey = key(x, y, z);
				if (removedPlatformKeys.contains(baseKey)) {
					tries++;
					continue;
				}
				Block baseBlock = world.getBlockAt(x, y, z);
				if (baseBlock.getType() == Material.AIR) {
					tries++;
					continue;
				}
				int mirrorX = 2 * PLATFORM_AXIS_X - x;
				int mirrorZ = 2 * PLATFORM_AXIS_Z - z;
				int[][] coords = new int[][]{
						{x, z}, {mirrorX, z}, {x, mirrorZ}, {mirrorX, mirrorZ}
				};
				boolean added = false;
				for (int[] coord : coords) {
					int cx = coord[0];
					int cz = coord[1];
					if (cx < PLATFORM_MIN_X || cx > PLATFORM_MAX_X || cz < PLATFORM_MIN_Z || cz > PLATFORM_MAX_Z) {
						continue;
					}
					String ck = key(cx, y, cz);
					if (removedPlatformKeys.contains(ck)) {
						continue;
					}
					Block target = world.getBlockAt(cx, y, cz);
					if (target.getType() == Material.AIR) {
						continue;
					}
					chosen.add(target.getLocation());
					removedPlatformKeys.add(ck);
					added = true;
				}
				if (added) {
					break;
				}
				tries++;
			}
		}
		return chosen;
	}

	private static void scheduleFlickerAndRemove(HitWall game, List<Location> targets) {
		if (game == null || targets == null || targets.isEmpty()) {
			return;
		}
		World world = Bukkit.getWorld(game.getWorldName());
		if (world == null) {
			return;
		}
		for (Location loc : targets) {
			Block block = world.getBlockAt(loc);
			platformSnapshots.putIfAbsent(key(block.getX(), block.getY(), block.getZ()), block.getState());
		}
		for (Location loc : targets) {
			BukkitRunnable flicker = new BukkitRunnable() {
				int flashes = 0;

				@Override
				public void run() {
					if (flashes >= SHRINK_FLICKER_COUNT) {
						world.getBlockAt(loc).setType(Material.AIR, false);
						cancel();
						return;
					}
					Block b = world.getBlockAt(loc);
					BlockState original = platformSnapshots.get(key(b.getX(), b.getY(), b.getZ()));
					b.setType(Material.SEA_LANTERN, false);
					Bukkit.getScheduler().runTaskLater(plugin, () -> {
						if (original != null) {
							original.update(true, false);
						} else {
							b.setType(Material.AIR, false);
						}
					}, SHRINK_FLICKER_INTERVAL_TICKS / 2);
					flashes++;
				}
			};
			flicker.runTaskTimer(plugin, 0L, SHRINK_FLICKER_INTERVAL_TICKS);
			game.getGameTask().add(flicker);
		}
	}

	private static void restorePlatformBlocks(HitWall game) {
		if (game == null) {
			return;
		}
		World world = Bukkit.getWorld(game.getWorldName());
		if (world == null) {
			return;
		}
		for (String k : removedPlatformKeys) {
			BlockState state = platformSnapshots.get(k);
			if (state != null) {
				state.update(true, false);
			}
		}
		removedPlatformKeys.clear();
		platformSnapshots.clear();
	}

	private static String key(int x, int y, int z) {
		return x + "|" + y + "|" + z;
	}

	private static String toRoman(int number) {
		switch (number) {
			case 1:
				return "I";
			case 2:
				return "II";
			case 3:
				return "III";
			case 4:
				return "IV";
			case 5:
				return "V";
			case 6:
				return "VI";
			case 7:
				return "VII";
			case 8:
				return "VIII";
			case 9:
				return "IX";
			case 10:
				return "X";
			default:
				return String.valueOf(number); // 超过 X 用阿拉伯数字
		}
	}

	/**
	 * 准备某方向的墙体并复制到入口处
	 */
	private static PreparedWave prepareDirection(HitWall game, WallWave wave, BlockFace direction,
												 HitWallSettings.WallTemplate forcedTemplate, Integer overridePushIterations) {
		if (game == null || wave == null || direction == null) {
			return null;
		}
		World world = Bukkit.getWorld(game.getWorldName());
		if (world == null) {
			return null;
		}

		HitWallSettings.TargetCorner corner = settings.getTargetCorner(direction);
		if (corner == null) {
			return null;
		}

		HitWallSettings.WallTemplate template = forcedTemplate;
		if (template == null) {
			if (direction == BlockFace.NORTH || direction == BlockFace.SOUTH) {
				template = settings.pickRandomNorthSouthTemplate();
			} else {
				template = settings.pickRandomEastWestTemplate();
			}
		}
		if (template == null) {
			return null;
		}

		Location targetCorner = corner.getLocation(world);
		int minX = targetCorner.getBlockX();
		int minY = targetCorner.getBlockY();
		int minZ = targetCorner.getBlockZ();
		int maxX = minX + (template.getMaxX() - template.getMinX());
		int maxY = minY + (template.getMaxY() - template.getMinY());
		int maxZ = minZ + (template.getMaxZ() - template.getMinZ());
		int pistonLayerY = Math.min(minY + 5, maxY);

		int offsetX = -direction.getModX() * 2;
		int offsetZ = -direction.getModZ() * 2;
		Location entryCorner = targetCorner.clone().add(offsetX, 0, offsetZ);
		copyWallTemplateToLocation(game, template, entryCorner);
		int entryMinX = entryCorner.getBlockX();
		int entryMinY = entryCorner.getBlockY();
		int entryMinZ = entryCorner.getBlockZ();
		int entryMaxX = entryMinX + (template.getMaxX() - template.getMinX());
		int entryMaxY = entryMinY + (template.getMaxY() - template.getMinY());
		int entryMaxZ = entryMinZ + (template.getMaxZ() - template.getMinZ());
		removeGlassFromWall(world, entryMinX, entryMaxX, entryMinY, entryMaxY, entryMinZ, entryMaxZ);
		int entrySteps = 3;
		BukkitTask entryTask = playEntryAnimation(game, world, direction, entryMinX, entryMaxX, entryMinY, entryMaxY, entryMinZ, entryMaxZ,
				pistonLayerY, entrySteps, wave.getSpeed());


		HitWallSettings.TargetCorner destinationCorner = settings.getTargetCorner(direction.getOppositeFace());
		int destMinX = 0;
		int destMinY = 0;
		int destMinZ = 0;
		int destMaxX = 0;
		int destMaxY = 0;
		int destMaxZ = 0;
		boolean hasDestination = destinationCorner != null;
		if (destinationCorner != null) {
			destMinX = destinationCorner.getX();
			destMinY = destinationCorner.getY();
			destMinZ = destinationCorner.getZ();
			destMaxX = destMinX + (template.getMaxX() - template.getMinX());
			destMaxY = destMinY + (template.getMaxY() - template.getMinY());
			destMaxZ = destMinZ + (template.getMaxZ() - template.getMinZ());
		}

		int pushIterations = overridePushIterations != null ? overridePushIterations : resolvePushIterations(direction);
		if (pushIterations <= 0) {
			return null;
		}
		int interval = Math.max(1, wave.getSpeed());

		int pathMinX;
		int pathMaxX;
		int pathMinY;
		int pathMaxY;
		int pathMinZ;
		int pathMaxZ;

		if (direction == BlockFace.EAST || direction == BlockFace.WEST) {
			int endX = destinationCorner != null ? destinationCorner.getX() : corner.getX();
			pathMinX = Math.min(corner.getX(), endX);
			pathMaxX = Math.max(corner.getX(), endX);
			pathMinY = corner.getY();
			pathMaxY = corner.getY() + 5;
			pathMinZ = corner.getZ();
			pathMaxZ = corner.getZ() + 10;
		} else {
			int endZ = destinationCorner != null ? destinationCorner.getZ() : corner.getZ();
			pathMinZ = Math.min(corner.getZ(), endZ);
			pathMaxZ = Math.max(corner.getZ(), endZ);
			pathMinY = corner.getY();
			pathMaxY = corner.getY() + 5;
			pathMinX = corner.getX();
			pathMaxX = corner.getX() + 10;
		}

		return new PreparedWave(template, world, direction, hasDestination,
				minX, maxX, minY, maxY, minZ, maxZ,
				destMinX, destMaxX, destMinY, destMaxY, destMinZ, destMaxZ,
				pathMinX, pathMaxX, pathMinY, pathMaxY, pathMinZ, pathMaxZ,
				pistonLayerY, pushIterations, interval,
				entryMinX, entryMaxX, entryMinZ, entryMaxZ, entrySteps, entryTask);
	}

	/**
	 * 为往返阶段复制墙体到反向起点并准备直接推回
	 */
	private static PreparedWave prepareReverseWave(HitWall game, WallWave wave, PreparedWave forward, BlockFace reverseDirection) {
		if (game == null || wave == null || forward == null || reverseDirection == null) {
			return null;
		}
		World world = Bukkit.getWorld(game.getWorldName());
		if (world == null) {
			return null;
		}
		HitWallSettings.WallTemplate template = forward.template();
		HitWallSettings.TargetCorner startCorner = settings.getTargetCorner(reverseDirection);
		if (template == null || startCorner == null) {
			return null;
		}

		Location startLocation = startCorner.getLocation(world);
		copyWallTemplateToLocation(game, template, startLocation);

		int minX = startCorner.getX();
		int minY = startCorner.getY();
		int minZ = startCorner.getZ();
		int maxX = minX + (template.getMaxX() - template.getMinX());
		int maxY = minY + (template.getMaxY() - template.getMinY());
		int maxZ = minZ + (template.getMaxZ() - template.getMinZ());
		removeGlassFromWall(world, minX, maxX, minY, maxY, minZ, maxZ);

		HitWallSettings.TargetCorner destinationCorner = settings.getTargetCorner(reverseDirection.getOppositeFace());
		boolean hasDestination = destinationCorner != null;
		int destMinX = 0, destMaxX = 0, destMinY = 0, destMaxY = 0, destMinZ = 0, destMaxZ = 0;
		if (destinationCorner != null) {
			destMinX = destinationCorner.getX();
			destMinY = destinationCorner.getY();
			destMinZ = destinationCorner.getZ();
			destMaxX = destMinX + (template.getMaxX() - template.getMinX());
			destMaxY = destMinY + (template.getMaxY() - template.getMinY());
			destMaxZ = destMinZ + (template.getMaxZ() - template.getMinZ());
		}

		int pathMinX;
		int pathMaxX;
		int pathMinY = minY;
		int pathMaxY = minY + 5;
		int pathMinZ;
		int pathMaxZ;
		if (reverseDirection == BlockFace.EAST || reverseDirection == BlockFace.WEST) {
			int endX = destinationCorner != null ? destinationCorner.getX() : startCorner.getX();
			pathMinX = Math.min(startCorner.getX(), endX);
			pathMaxX = Math.max(startCorner.getX(), endX);
			pathMinZ = startCorner.getZ();
			pathMaxZ = startCorner.getZ() + 10;
		} else {
			int endZ = destinationCorner != null ? destinationCorner.getZ() : startCorner.getZ();
			pathMinZ = Math.min(startCorner.getZ(), endZ);
			pathMaxZ = Math.max(startCorner.getZ(), endZ);
			pathMinX = startCorner.getX();
			pathMaxX = startCorner.getX() + 10;
		}

		int pistonLayerY = Math.min(minY + 5, maxY);
		int pushIterations = forward.pushIterations();
		int interval = Math.max(1, wave.getSpeed());

		return new PreparedWave(template, world, reverseDirection, hasDestination,
				minX, maxX, minY, maxY, minZ, maxZ,
				destMinX, destMaxX, destMinY, destMaxY, destMinZ, destMaxZ,
				pathMinX, pathMaxX, pathMinY, pathMaxY, pathMinZ, pathMaxZ,
				pistonLayerY, pushIterations, interval,
				minX, maxX, minZ, maxZ, 0, null);
	}

	/**
	 * 播放入场阶段的活塞推进动画
	 */
	private static BukkitTask playEntryAnimation(HitWall game, World world, BlockFace direction,
												 int minX, int maxX, int minY, int maxY, int minZ, int maxZ,
												 int pistonY, int steps, int speed) {
		if (world == null || direction == null || steps <= 0) {
			return null;
		}
		int interval = Math.max(1, speed);
		BukkitRunnable entryPushTask = new BukkitRunnable() {
			int remaining = steps;
			int currentMinX = minX;
			int currentMaxX = maxX;
			int currentMinZ = minZ;
			int currentMaxZ = maxZ;

			@Override
			public void run() {
				if (remaining <= 0) {
					cancel();
					return;
				}
				spawnAndFirePistonsAtTop(world, direction, pistonY, currentMinX, currentMaxX, currentMinZ, currentMaxZ);
				currentMinX += direction.getModX();
				currentMaxX += direction.getModX();
				currentMinZ += direction.getModZ();
				currentMaxZ += direction.getModZ();
				remaining--;
			}
		};
		BukkitTask task = entryPushTask.runTaskTimer(plugin, 0L, interval);
		if (game != null) {
			game.getGameTask().add(entryPushTask);
		}
		return task;
	}

	/**
	 * 选择并执行波次机制
	 */
	private static void executeWaveMechanism(HitWall game, WallWave wave,
											 java.util.EnumMap<BlockFace, PreparedWave> prepared, Runnable finishWave) {
		if (game != null) {
			PotionEffect effect = pickRandomWaveEffect();
			applyWaveEffect(game, effect);
			String effectName = effect != null ? effect.getType().getKey().getKey() : "无";
			String roman = effect != null ? toRoman(effect.getAmplifier() + 1) : "";
			MCEMessenger.sendGlobalTitle("", "<gold><bold><translate:effect.minecraft." + effectName + "> " + roman + "</bold></gold>");
			Bukkit.getScheduler().runTaskLater(plugin, () -> MCEMessenger.sendGlobalTitle("", ""), 10L);
		}
		List<Integer> mechanisms = new ArrayList<>();
		mechanisms.add(1);
		mechanisms.add(2);
		mechanisms.add(3);
		java.util.Collections.shuffle(mechanisms);

		for (int mech : mechanisms) {
			switch (mech) {
				case 1 -> {
					if (runSingleDirectionMechanism(game, prepared, finishWave)) {
						return;
					}
				}
				case 2 -> {
					if (runAdjacentPairMechanism(game, prepared, finishWave)) {
						return;
					}
				}
				case 3 -> {
					if (runForwardBackwardMechanism(game, wave, prepared, finishWave)) {
						return;
					}
				}
				default -> {
				}
			}
		}

		// fallback
		runSingleDirectionMechanism(game, prepared, finishWave);
	}

	/**
	 * 仅推送一个方向的墙
	 */
	private static boolean runSingleDirectionMechanism(HitWall game,
													   java.util.EnumMap<BlockFace, PreparedWave> prepared,
													   Runnable finishWave) {
		if (prepared.isEmpty()) {
			return false;
		}
		plugin.getLogger().info("Running Single Direction Mechanism...");
		List<BlockFace> directions = new ArrayList<>(prepared.keySet());
		java.util.Collections.shuffle(directions);
		BlockFace target = directions.get(0);
		PreparedWave selected = prepared.get(target);
		prepared.forEach((dir, pw) -> {
			if (dir != target) {
				clearPreparedWave(pw);
			}
		});
		pushPreparedWave(game, selected, finishWave);
		return true;
	}

	/**
	 * 推送相邻双方向的墙
	 */
	private static boolean runAdjacentPairMechanism(HitWall game,
													java.util.EnumMap<BlockFace, PreparedWave> prepared,
													Runnable finishWave) {
		plugin.getLogger().info("Running Adjacent Pair Mechanism...");
		BlockFace[][] pairs = new BlockFace[][]{
				{BlockFace.NORTH, BlockFace.EAST},
				{BlockFace.EAST, BlockFace.SOUTH},
				{BlockFace.SOUTH, BlockFace.WEST},
				{BlockFace.WEST, BlockFace.NORTH}
		};
		List<BlockFace[]> valid = new ArrayList<>();
		for (BlockFace[] pair : pairs) {
			if (prepared.containsKey(pair[0]) && prepared.containsKey(pair[1])) {
				valid.add(pair);
			}
		}
		if (valid.isEmpty()) {
			return false;
		}
		BlockFace[] chosen = valid.get(ThreadLocalRandom.current().nextInt(valid.size()));
		PreparedWave first = prepared.get(chosen[0]);
		PreparedWave second = prepared.get(chosen[1]);
		prepared.forEach((dir, pw) -> {
			if (dir != chosen[0] && dir != chosen[1]) {
				clearPreparedWave(pw);
			}
		});
		pushPreparedWave(game, first, () -> pushPreparedWave(game, second, finishWave));
		return true;
	}

	/**
	 * 先正向再反向推同一堵墙
	 */
	private static boolean runForwardBackwardMechanism(HitWall game, WallWave wave,
													   java.util.EnumMap<BlockFace, PreparedWave> prepared, Runnable finishWave) {
		List<BlockFace> available = new ArrayList<>(prepared.keySet());
		if (available.isEmpty()) {
			return false;
		}
		plugin.getLogger().info("Running Forward Backward Mechanism...");
		java.util.Collections.shuffle(available);
		for (BlockFace direction : available) {
			PreparedWave first = prepared.get(direction);
			if (first == null) {
				continue;
			}
			prepared.forEach((dir, pw) -> {
				if (dir != direction) {
					clearPreparedWave(pw);
				}
			});
			BlockFace reverse = direction.getOppositeFace();
			plugin.getLogger().info("Start pushing backward...");
			pushPreparedWave(game, first, () -> {
				PreparedWave reverseWave = prepareReverseWave(game, wave, first, reverse);
				if (reverseWave == null) {
					finishWave.run();
					return;
				}
				pushPreparedWave(game, reverseWave, finishWave);
			});
			return true;
		}
		return false;
	}

	/**
	 * 使用准备好的墙体参数按节奏推进
	 */
	private static void pushPreparedWave(HitWall game, PreparedWave prepared, Runnable onComplete) {
		if (prepared == null) {
			safeInvoke(onComplete);
			return;
		}
		cancelEntryTask(prepared);
		Runnable completion = () -> safeInvoke(onComplete);
		plugin.getLogger().info("当前墙信息: 方向 " + prepared.direction() + "; 推动次数 " + prepared.pushIterations()
				+ "; Interval: " + prepared.interval());

		BukkitRunnable pushTask = new BukkitRunnable() {
			int executed = 0;
			int currentMinX = prepared.minX();
			int currentMaxX = prepared.maxX();
			int currentMinZ = prepared.minZ();
			int currentMaxZ = prepared.maxZ();
			boolean clearedDestination = false;
			boolean completionInvoked = false;

			private void completeOnce() {
				if (completionInvoked) {
					return;
				}
				completionInvoked = true;
				completion.run();
			}

			@Override
			public void run() {
				if (executed >= prepared.pushIterations()) {
					if (prepared.hasDestination() && !clearedDestination) {
						clearWallRegion(prepared.world(),
								prepared.destMinX(), prepared.destMaxX(),
								prepared.destMinY(), prepared.destMaxY(),
								prepared.destMinZ(), prepared.destMaxZ());
						clearWallPath(prepared.world(),
								prepared.pathMinX(), prepared.pathMaxX(),
								prepared.pathMinY(), prepared.pathMaxY(),
								prepared.pathMinZ(), prepared.pathMaxZ());
						clearedDestination = true;
					}
					cancel();
					completeOnce();
					return;
				}
				spawnAndFirePistonsAtTop(prepared.world(), prepared.direction(), prepared.pistonLayerY(),
						currentMinX, currentMaxX, currentMinZ, currentMaxZ);
				currentMinX += prepared.direction().getModX();
				currentMaxX += prepared.direction().getModX();
				currentMinZ += prepared.direction().getModZ();
				currentMaxZ += prepared.direction().getModZ();
				executed++;
			}
		};
		pushTask.runTaskTimer(plugin, 0L, prepared.interval());
		game.getGameTask().add(pushTask);
	}

	/**
	 * 获取推动次数，待补充或保持不变
	 *
	 * @param direction 方向
	 * @return 在 direction 方向上的推动次数
	 */
	private static int resolvePushIterations(BlockFace direction) {
		if (direction == null) {
			return 0;
		}
		HitWallSettings.TargetCorner start = settings.getTargetCorner(direction);
		HitWallSettings.TargetCorner destination = settings.getTargetCorner(direction.getOppositeFace());
		if (start == null || destination == null) {
			return DEFAULT_PUSH_CYCLES;
		}
		int distance;
		if (direction == BlockFace.NORTH || direction == BlockFace.SOUTH) {
			distance = Math.abs(start.getZ() - destination.getZ());
		} else {
			distance = Math.abs(start.getX() - destination.getX());
		}
		return distance > 0 ? distance : DEFAULT_PUSH_CYCLES;
	}

	/**
	 * 删去玻璃
	 */
	private static void removeGlassFromWall(World world, int minX, int maxX,
											int minY, int maxY, int minZ, int maxZ) {
		for (int x = minX; x <= maxX; x++) {
			for (int y = minY; y <= maxY; y++) {
				for (int z = minZ; z <= maxZ; z++) {
					Block block = world.getBlockAt(x, y, z);
					if (block.getType() == Material.GLASS) {
						block.setType(Material.AIR, false);
					}
				}
			}
		}
	}

	/**
	 * 清除指定范围内的障碍方块
	 */
	private static void clearWallRegion(World world, int minX, int maxX,
										int minY, int maxY, int minZ, int maxZ) {
		for (int x = minX; x <= maxX; x++) {
			for (int y = minY; y <= maxY; y++) {
				for (int z = minZ; z <= maxZ; z++) {
					Block block = world.getBlockAt(x, y, z);
					if (block.getType() == Material.SLIME_BLOCK || block.getType() == Material.NETHER_BRICKS || block.getType() == Material.NETHER_BRICK_STAIRS
							|| block.getType() == Material.NETHER_BRICK_WALL || block.getType() == Material.NETHER_BRICK_FENCE || block.getType() == Material.WARPED_PLANKS
							|| block.getType() == Material.NETHER_BRICK_SLAB || block.getType() == Material.WARPED_TRAPDOOR || block.getType() == Material.PISTON) {
						block.setType(Material.AIR, false);
//						block.breakNaturally();

//						BlockBreakEvent event = new BlockBreakEvent(block, world.getPlayers().get(0));
//						Bukkit.getPluginManager().callEvent(event);
//
//						if (event.isCancelled()) {
//							continue;
//						}
//						event.setDropItems(false);
//						block.setType(Material.AIR, true);
					}
				}
			}
		}
	}

	/**
	 * 清除单条路径
	 */
	private static void clearWallPath(World world,
									  int minX, int maxX,
									  int minY, int maxY,
									  int minZ, int maxZ) {
		clearWallRegion(world, minX, maxX, minY, maxY, minZ, maxZ);
	}

	/**
	 * 清除四个方向所有路径与墙体残留
	 */
	public static void clearAllWallPaths(HitWall game) {
		if (game == null) {
			return;
		}
		World world = Bukkit.getWorld(game.getWorldName());
		if (world == null) {
			return;
		}
		HitWallSettings.TargetCorner west = settings.getTargetCorner(BlockFace.WEST);
		HitWallSettings.TargetCorner east = settings.getTargetCorner(BlockFace.EAST);
		if (west != null && east != null) {
			int minX = Math.min(west.getX(), east.getX()) - 4;
			int maxX = Math.max(west.getX(), east.getX()) + 4;
			int minY = west.getY();
			int maxY = west.getY() + 5;
			int minZ = west.getZ();
			int maxZ = west.getZ() + 10;
			clearWallRegion(world, minX, maxX, minY, maxY, minZ, maxZ);
		}

		HitWallSettings.TargetCorner north = settings.getTargetCorner(BlockFace.NORTH);
		HitWallSettings.TargetCorner south = settings.getTargetCorner(BlockFace.SOUTH);
		if (north != null && south != null) {
			int minZ = Math.min(north.getZ(), south.getZ()) - 4;
			int maxZ = Math.max(north.getZ(), south.getZ()) + 4;
			int minY = north.getY();
			int maxY = north.getY() + 5;
			int minX = north.getX();
			int maxX = north.getX() + 10;
			clearWallRegion(world, minX, maxX, minY, maxY, minZ, maxZ);
		}
	}

	/**
	 * 安全执行回调并捕获异常
	 */
	private static void safeInvoke(Runnable runnable) {
		if (runnable == null) {
			return;
		}
		try {
			runnable.run();
		} catch (Throwable t) {
			plugin.getLogger().warning("HitWall: 波次完成回调执行失败: " + t.getMessage());
		}
	}

	/**
	 * 将参与者传送到预设黑曜石平台
	 */
	public static void teleportPlayersToObsidianPlatform(HitWall game) {
		if (game == null) {
			plugin.getLogger().warning("HitWall: game is null!");
			return;
		}
		World world = Bukkit.getWorld(game.getWorldName());
		if (world == null) {
			plugin.getLogger().warning("HitWall: 世界 " + game.getWorldName() + " 不存在，无法传送玩家到平台。");
			return;
		}
		int minX = Math.min(1, 11);
		int maxX = Math.max(1, 11);
		int minZ = Math.min(-128, -118);
		int maxZ = Math.max(-128, -118);
		int y = 8;

		List<Location> targets = new ArrayList<>();
		for (int x = minX; x <= maxX; x++) {
			for (int z = minZ; z <= maxZ; z++) {
				Block block = world.getBlockAt(x, y, z);
				if (block.getType() == Material.CRYING_OBSIDIAN) {
					targets.add(block.getLocation().add(0.5, 1, 0.5));
				}
			}
		}
		if (targets.isEmpty()) {
			plugin.getLogger().warning("HitWall: 指定平台范围内没有找到哭泣的黑曜石，无法传送玩家。");
			return;
		}

		List<Player> participants = new ArrayList<>();
		for (Player player : world.getPlayers()) {
			if (player.getGameMode() == GameMode.SPECTATOR) {
				continue;
			}
			if (!game.isGameParticipant(player)) {
				continue;
			}
			participants.add(player);
		}
		if (participants.isEmpty()) {
			return;
		}

		for (Player player : participants) {
			Location target = targets.get(ThreadLocalRandom.current().nextInt(targets.size()));
			player.teleport(target);
		}
	}

	/**
	 * 生成和推出活塞
	 */
	private static void spawnAndFirePistonsAtTop(World world, BlockFace direction, int pistonY,
												 int minX, int maxX, int minZ, int maxZ) {
		if (direction == null) {
			return;
		}
		// 放置活塞
		List<TempBlock> pistons = placePistons(world, direction, pistonY, minX, maxX, minZ, maxZ);
		if (pistons.isEmpty()) {
			return;
		}
		// 推出活塞，然后清除
		powerAndCleanupPistons(direction, pistons);
	}

	/**
	 * 在对应墙位置放置活塞，返回放置的活塞表
	 */
	private static List<TempBlock> placePistons(World world, BlockFace direction, int pistonY,
												int minX, int maxX, int minZ, int maxZ) {
		List<TempBlock> pistons = new ArrayList<>();
		BlockFace behind = direction.getOppositeFace();
		int offsetX = behind.getModX();
		int offsetZ = behind.getModZ();
		for (int x = minX; x <= maxX; x++) {
			for (int z = minZ; z <= maxZ; z++) {
				if (!isBackFace(direction, x, z, minX, maxX, minZ, maxZ)) {
					continue;
				}
				Block block = world.getBlockAt(x, pistonY, z);
				if (block.getType() != Material.SLIME_BLOCK && block.getType() != Material.NETHER_BRICKS && block.getType() != Material.WARPED_PLANKS) {
					continue;
				}
				Block pistonBlock = world.getBlockAt(x + offsetX, pistonY, z + offsetZ);
				BlockState previousState = pistonBlock.getState();
				pistonBlock.setType(Material.PISTON, true);
				if (pistonBlock.getBlockData() instanceof Directional directional) {
					directional.setFacing(direction);
					pistonBlock.setBlockData(directional, true);
				}
				pistons.add(new TempBlock(pistonBlock, previousState));
			}
		}
		return pistons;
	}

	/**
	 * 根据活塞表推出活塞，然后清除活塞
	 */
	private static void powerAndCleanupPistons(BlockFace direction, List<TempBlock> pistons) {
		if (direction == null) {
			return;
		}
		BlockFace behind = direction.getOppositeFace();
		List<TempBlock> buttons = new ArrayList<>();
		for (TempBlock piston : pistons) {
			Block buttonBlock = piston.block().getRelative(behind);
			BlockState previousState = buttonBlock.getState();
			buttonBlock.setType(Material.STONE_BUTTON, true);
			if (buttonBlock.getBlockData() instanceof Switch buttonData) {
				if (buttonData instanceof FaceAttachable attachable) {
					attachable.setAttachedFace(FaceAttachable.AttachedFace.WALL);
				}
				buttonData.setFacing(behind);
				buttonData.setPowered(true);
				buttonBlock.setBlockData(buttonData, true);
			}
			buttons.add(new TempBlock(buttonBlock, previousState));
		}
		Bukkit.getScheduler().runTaskLater(plugin, () -> {
			for (TempBlock button : buttons) {
				button.restore();
			}
		}, 2L);
		Bukkit.getScheduler().runTaskLater(plugin, () -> {
			for (TempBlock piston : pistons) {
				piston.restore();
			}
		}, 4L);
	}

	private static boolean isBackFace(BlockFace direction, int x, int z,
									  int minX, int maxX, int minZ, int maxZ) {
		return switch (direction) {
			case NORTH -> z == maxZ;
			case SOUTH -> z == minZ;
			case EAST -> x == minX;
			case WEST -> x == maxX;
			default -> false;
		};
	}

	private record TempBlock(Block block, BlockState previousState) {
		void restore() {
			if (block == null) {
				return;
			}
			if (previousState == null) {
				block.setType(Material.AIR, false);
				return;
			}
			if (previousState.getBlock().getType() == Material.PISTON) {
				block.setType(Material.AIR, false);
				return;
			}
//			block.setType(previousState.getType(), false);
//			block.setBlockData(previousState.getBlockData(), false);
			block.setType(Material.AIR, false);
		}

		public Block block() {
			return block;
		}
	}


	/**
	 * 清除当前波次药水效果并恢复基础状态
	 */
	private static void clearActiveWaveEffect(HitWall game) {
		if (game == null || activeWaveEffect == null) {
			activeWaveEffect = null;
			if (game != null) {
				applyBaselineJumpBoost(game);
			}
			return;
		}
		PotionEffectType type = activeWaveEffect.getType();
		for (Player player : Bukkit.getOnlinePlayers()) {
			if (!player.getWorld().getName().equals(game.getWorldName())) {
				continue;
			}
			if (!game.isGameParticipant(player)) {
				continue;
			}
			player.removePotionEffect(type);
		}
		activeWaveEffect = null;
		applyBaselineJumpBoost(game);
	}

	/**
	 * 应用并记录本波次的药水效果
	 */
	private static void applyWaveEffect(HitWall game, PotionEffect effect) {
		if (game == null || effect == null) {
			applyBaselineJumpBoost(game);
			return;
		}
		clearActiveWaveEffect(game);
		PotionEffect persistent = new PotionEffect(
				effect.getType(),
				Integer.MAX_VALUE,
				effect.getAmplifier(),
				effect.isAmbient(),
				effect.hasParticles(),
				effect.hasIcon());
		activeWaveEffect = persistent;
		for (Player player : Bukkit.getOnlinePlayers()) {
			if (!player.getWorld().getName().equals(game.getWorldName())) {
				continue;
			}
			if (player.getGameMode() == GameMode.SPECTATOR) {
				continue;
			}
			if (!game.isGameParticipant(player)) {
				continue;
			}
			player.addPotionEffect(persistent);
		}
	}

	/**
	 * 为参与者维持基础跳跃效果
	 */
	private static void applyBaselineJumpBoost(HitWall game) {
		if (game == null) {
			return;
		}
		for (Player player : Bukkit.getOnlinePlayers()) {
			if (!player.getWorld().getName().equals(game.getWorldName())) {
				continue;
			}
			if (player.getGameMode() == GameMode.SPECTATOR) {
				continue;
			}
			if (!game.isGameParticipant(player)) {
				continue;
			}
			player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, Integer.MAX_VALUE, 1, true, false, false));
		}
	}

	/**
	 * 将墙复制到 corner 指示的坐标处
	 *
	 * @param game         游戏
	 * @param wall         墙对象
	 * @param targetCorner 目标的 minX, minY, minZ 位置
	 */
	public static void copyWallTemplateToLocation(HitWall game, HitWallSettings.WallTemplate wall, Location targetCorner) {
		if (game == null || wall == null || targetCorner == null) {
			return;
		}
		World sourceWorld = Bukkit.getWorld(game.getWorldName());
		if (sourceWorld == null) {
			plugin.getLogger().warning("HitWall: 世界 " + game.getWorldName() + " 不存在，无法复制墙体。");
			return;
		}
		World targetWorld = targetCorner.getWorld();
		if (targetWorld == null) {
			targetWorld = sourceWorld;
		}
		int offsetX = targetCorner.getBlockX() - wall.getMinX();
		int offsetY = targetCorner.getBlockY() - wall.getMinY();
		int offsetZ = targetCorner.getBlockZ() - wall.getMinZ();

		for (int x = wall.getMinX(); x <= wall.getMaxX(); x++) {
			for (int y = wall.getMinY(); y <= wall.getMaxY(); y++) {
				for (int z = wall.getMinZ(); z <= wall.getMaxZ(); z++) {
					Block sourceBlock = sourceWorld.getBlockAt(x, y, z);
					Block targetBlock = targetWorld.getBlockAt(x + offsetX, y + offsetY, z + offsetZ);
					targetBlock.setType(sourceBlock.getType(), false);    // 不产生方块更新
					targetBlock.setBlockData(sourceBlock.getBlockData(), false);
				}
			}
		}
	}


	/**
	 * 取消并清空游戏任务
	 */
	public static void clearGameTasks(HitWall game) {
		if (game == null || game.getGameTask() == null) {
			return;
		}
		List<BukkitRunnable> tasks = new ArrayList<>(game.getGameTask());
		for (BukkitRunnable task : tasks) {
			if (task == null) {
				continue;
			}
			try {
				task.cancel();
			} catch (Throwable ignored) {
			}
		}
		game.getGameTask().clear();
		currentWave = 0;
		totalWaves = 0;
		wavesCompleted = false;
		restorePlatformBlocks(game);
	}

	/**
	 * 解析出生点位置
	 */
	private static Location resolveSpawnLocation(HitWall game) {
		if (game == null) {
			return null;
		}
		String worldName = settings.getWorldName();
		if (worldName == null || worldName.isBlank()) {
			worldName = game.getWorldName();
		}
		World world = Bukkit.getWorld(worldName);
		if (world == null) {
			plugin.getLogger().warning("HitWall: 世界 " + worldName + " 不存在，无法定位出生点。");
			return null;
		}
		return new Location(world,
				settings.getSpawnX(),
				settings.getSpawnY(),
				settings.getSpawnZ(),
				settings.getSpawnYaw(),
				settings.getSpawnPitch());
	}

	/**
	 * 获取配置的波次列表，缺省则补默认波
	 */
	private static List<WallWave> resolveWaves() {
		List<WallWave> waves = new ArrayList<>(settings.getWaves());
		if (waves.isEmpty()) {
			waves.add(new WallWave(2, 5));
		}
		return waves;
	}

	private record PreparedWave(
			HitWallSettings.WallTemplate template,
			World world,
			BlockFace direction,
			boolean hasDestination,
			int minX,
			int maxX,
			int minY,
			int maxY,
			int minZ,
			int maxZ,
			int destMinX,
			int destMaxX,
			int destMinY,
			int destMaxY,
			int destMinZ,
			int destMaxZ,
			int pathMinX,
			int pathMaxX,
			int pathMinY,
			int pathMaxY,
			int pathMinZ,
			int pathMaxZ,
			int pistonLayerY,
			int pushIterations,
			int interval,
			int entryMinX,
			int entryMaxX,
			int entryMinZ,
			int entryMaxZ,
			int entrySteps,
			BukkitTask entryTask
	) {
	}

	private static void cancelEntryTask(PreparedWave prepared) {
		if (prepared == null) {
			return;
		}
		BukkitTask task = prepared.entryTask();
		if (task == null) {
			return;
		}
		try {
			task.cancel();
		} catch (Throwable ignored) {
		}
	}

	/**
	 * 清除尚未使用的准备墙体
	 */
	private static void clearPreparedWave(PreparedWave prepared) {
		if (prepared == null || prepared.world() == null) {
			return;
		}
		// 清理未选中墙的入场与当前位置，避免残留影响其他方向
		World world = prepared.world();
		cancelEntryTask(prepared);
		// 入场起点区域
		clearWallRegion(world,
				prepared.entryMinX(), prepared.entryMaxX(),
				prepared.minY(), prepared.maxY(),
				prepared.entryMinZ(), prepared.entryMaxZ());
		// 入场推移后的区域（entrySteps 步后的位置）
		int shiftX = prepared.direction().getModX() * prepared.entrySteps();
		int shiftZ = prepared.direction().getModZ() * prepared.entrySteps();
		clearWallRegion(world,
				prepared.entryMinX() + shiftX, prepared.entryMaxX() + shiftX,
				prepared.minY(), prepared.maxY(),
				prepared.entryMinZ() + shiftZ, prepared.entryMaxZ() + shiftZ);
		// 目标区域
		clearWallRegion(world,
				prepared.minX(), prepared.maxX(),
				prepared.minY(), prepared.maxY(),
				prepared.minZ(), prepared.maxZ());
	}

	/**
	 * 随机选择一项波次药水效果
	 */
	private static PotionEffect pickRandomWaveEffect() {
		int choice = ThreadLocalRandom.current().nextInt(5);
		switch (choice) {
			case 0:
				return new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, 4, true, false, false);
			case 1:
				return new PotionEffect(PotionEffectType.SLOWNESS, Integer.MAX_VALUE, 2, true, false, false);
			case 2:
				return new PotionEffect(PotionEffectType.JUMP_BOOST, Integer.MAX_VALUE, 3, true, false, false);
			case 3:
				return new PotionEffect(PotionEffectType.SLOW_FALLING, Integer.MAX_VALUE, 1, true, false, false);
			default:
				return new PotionEffect(PotionEffectType.DARKNESS, Integer.MAX_VALUE, 3, true, false, false);
		}
	}
}
