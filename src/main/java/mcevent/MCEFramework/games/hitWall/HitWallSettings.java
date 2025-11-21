package mcevent.MCEFramework.games.hitWall;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockFace;

import java.util.ArrayList;
import java.util.List;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Getter
@Setter
public class HitWallSettings {
	private String worldName;
	private double spawnX = 6;
	private double spawnY = 23;
	private double spawnZ = -100;
	private float spawnYaw = 180f;
	private float spawnPitch = 0f;
	private double killY = -3;
	private final List<WallWave> waves = new ArrayList<>();
	private final List<WallTemplate> eastWestTemplates = new ArrayList<>();
	private final List<WallTemplate> northSouthTemplates = new ArrayList<>();
	private final Map<BlockFace, TargetCorner> targetCorners = new EnumMap<>(BlockFace.class);

	public static HitWallSettings defaultSettings() {
		HitWallSettings settings = new HitWallSettings();
		settings.getWaves().add(new WallWave(2, 5));
		return settings;
	}

	public WallTemplate pickRandomEastWestTemplate() {
		return pickRandomTemplate(eastWestTemplates);
	}

	public WallTemplate pickRandomNorthSouthTemplate() {
		return pickRandomTemplate(northSouthTemplates);
	}

	private WallTemplate pickRandomTemplate(List<WallTemplate> templates) {
		if (templates.isEmpty()) {
			return null;
		}
		return templates.get(ThreadLocalRandom.current().nextInt(templates.size()));
	}

	public void setTargetCorner(BlockFace direction, TargetCorner corner) {
		if (direction == null || corner == null) {
			return;
		}
		targetCorners.put(direction, corner);
	}

	public TargetCorner getTargetCorner(BlockFace direction) {
		if (direction == null) {
			return null;
		}
		return targetCorners.get(direction);
	}

	@Getter
	public static class WallWave {
		private final int speed;
		private final int delaySeconds;

		public WallWave(int speed, int delaySeconds) {
			this.speed = speed;
			this.delaySeconds = delaySeconds;
		}
	}

	@Getter
	public static class TargetCorner {
		private final int x;
		private final int y;
		private final int z;

		public TargetCorner(int x, int y, int z) {
			this.x = x;
			this.y = y;
			this.z = z;
		}

		Location getLocation(World world) {
			return new Location(world, x, y, z);
		}
	}

	@Getter
	public static class WallTemplate {
		private final int minX;
		private final int maxX;
		private final int minY;
		private final int maxY;
		private final int minZ;
		private final int maxZ;

		public WallTemplate(int minX, int maxX, int minY, int maxY, int minZ, int maxZ) {
			this.minX = Math.min(minX, maxX);
			this.maxX = Math.max(minX, maxX);
			this.minY = Math.min(minY, maxY);
			this.maxY = Math.max(minY, maxY);
			this.minZ = Math.min(minZ, maxZ);
			this.maxZ = Math.max(minZ, maxZ);
		}
	}

	public static BlockFace parseDirection(String raw) {
		if (raw == null || raw.isBlank()) {
			return null;
		}
		try {
			BlockFace face = BlockFace.valueOf(raw.trim().toUpperCase(Locale.ROOT));
			return switch (face) {
				case NORTH, SOUTH, EAST, WEST -> face;
				default -> null;
			};
		} catch (IllegalArgumentException ignored) {
			return null;
		}
	}
}
