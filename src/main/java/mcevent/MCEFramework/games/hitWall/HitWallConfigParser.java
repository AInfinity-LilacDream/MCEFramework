package mcevent.MCEFramework.games.hitWall;

import lombok.Getter;
import lombok.Setter;
import mcevent.MCEFramework.tools.MCEConfigParser;
import org.bukkit.block.BlockFace;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static mcevent.MCEFramework.miscellaneous.Constants.plugin;

@Getter
@Setter
public class HitWallConfigParser extends MCEConfigParser {
	private HitWallSettings settings = HitWallSettings.defaultSettings();

	@Override
	public void parse() {
		HitWallSettings parsed = HitWallSettings.defaultSettings();
		List<HitWallSettings.WallWave> parsedWaves = new ArrayList<>();
		boolean wavesConfigured = false;

		try {
			lines = Files.readAllLines(configPath);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		String currentSection = null;
		List<Integer> currentAxisList = new ArrayList<>();

		for (String rawLine : lines) {
			String line = rawLine.trim();
			if (line.startsWith("\uFEFF")) {
				line = line.substring(1);
			}
			if (line.isEmpty() || line.startsWith("#")) {
				continue;
			}

			if (line.startsWith("[") && line.endsWith("]")) {
				currentSection = line.substring(1, line.length() - 1).toLowerCase(Locale.ROOT);
				currentAxisList = new ArrayList<>();
				continue;
			}

			if ("intro".equals(currentSection)) {
				continue;
			}

			if ("map_name".equals(currentSection)) {
				parsed.setWorldName(line);
				continue;
			}

			if ("spawn".equals(currentSection)) {
				parseSpawnValue(parsed, line);
				continue;
			}

			if ("kill_y".equals(currentSection)) {
				parsed.setKillY(parseDouble(line, parsed.getKillY()));
				continue;
			}

			if ("waves".equals(currentSection)) {
				if (!wavesConfigured) {
					parsedWaves.clear();
					wavesConfigured = true;
				}
				HitWallSettings.WallWave wave = parseWave(line);
				if (wave != null) {
					parsedWaves.add(wave);
				}
				continue;
			}

			if ("east_west_templates".equals(currentSection)) {
				parseEastWestTemplateLine(parsed, currentAxisList, line);
				continue;
			}

			if ("north_south_templates".equals(currentSection)) {
				parseNorthSouthTemplateLine(parsed, currentAxisList, line);
				continue;
			}

			if ("target_corners".equals(currentSection)) {
				parseTargetCornerLine(parsed, line);
			}
		}

		if (!parsedWaves.isEmpty()) {
			parsed.getWaves().clear();
			parsed.getWaves().addAll(parsedWaves);
		}

		settings = parsed;
		plugin.getLogger().info("HitWall配置文件读取完毕");
	}

	private void parseEastWestTemplateLine(HitWallSettings parsed, List<Integer> currentAxisList, String line) {
		if (line.startsWith("x_values") || line.startsWith("x=")) {
			currentAxisList.clear();
			currentAxisList.addAll(parseNumberList(line.substring(line.indexOf('=') + 1)));
			return;
		}

		if (!line.startsWith("rect")) {
			return;
		}

		if (currentAxisList.isEmpty()) {
			plugin.getLogger().warning("HitWall配置：east_west_templates 中 rect 缺少对应的 x_values");
			return;
		}

		int[] values = parseRectValues(line);
		if (values.length != 4) {
			plugin.getLogger().warning("HitWall配置：east_west_templates rect 需要 4 个数字");
			return;
		}
		int y1 = values[0];
		int z1 = values[1];
		int y2 = values[2];
		int z2 = values[3];

		for (int x : currentAxisList) {
			parsed.getEastWestTemplates().add(
					new HitWallSettings.WallTemplate(x, x, y1, y2, z1, z2)
			);
		}
	}

	private void parseNorthSouthTemplateLine(HitWallSettings parsed, List<Integer> currentAxisList, String line) {
		if (line.startsWith("z_values") || line.startsWith("z=")) {
			currentAxisList.clear();
			currentAxisList.addAll(parseNumberList(line.substring(line.indexOf('=') + 1)));
			return;
		}

		if (!line.startsWith("rect")) {
			return;
		}

		if (currentAxisList.isEmpty()) {
			plugin.getLogger().warning("HitWall配置：north_south_templates 中 rect 缺少对应的 z_values");
			return;
		}

		int[] values = parseRectValues(line);
		if (values.length != 4) {
			plugin.getLogger().warning("HitWall配置：north_south_templates rect 需要 4 个数字");
			return;
		}
		int y1 = values[0];
		int x1 = values[1];
		int y2 = values[2];
		int x2 = values[3];

		for (int z : currentAxisList) {
			parsed.getNorthSouthTemplates().add(
					new HitWallSettings.WallTemplate(x1, x2, y1, y2, z, z)
			);
		}
	}

	private void parseTargetCornerLine(HitWallSettings parsed, String line) {
		if (!line.contains("=")) {
			return;
		}
		String[] kv = line.split("=", 2);
		if (kv.length != 2) {
			return;
		}
		BlockFace direction = HitWallSettings.parseDirection(kv[0]);
		if (direction == null) {
			return;
		}
		int[] coords = parseCornerCoordinates(kv[1]);
		if (coords == null) {
			plugin.getLogger().warning("HitWall配置：target_corners " + kv[0] + " 需要提供三个坐标");
			return;
		}
		parsed.setTargetCorner(direction, new HitWallSettings.TargetCorner(coords[0], coords[1], coords[2]));
	}

	private List<Integer> parseNumberList(String raw) {
		String[] parts = raw.split(",");
		List<Integer> list = new ArrayList<>();
		for (String part : parts) {
			try {
				double parsed = Double.parseDouble(part.trim());
				list.add((int) Math.round(parsed));
			} catch (NumberFormatException ignored) {
			}
		}
		return list;
	}

	private int[] parseRectValues(String line) {
		int idx = line.indexOf('=');
		String payload = idx >= 0 ? line.substring(idx + 1) : line;
		String[] parts = payload.split(",");
		int[] numbers = new int[parts.length];
		for (int i = 0; i < parts.length; i++) {
			try {
				double parsed = Double.parseDouble(parts[i].trim());
				numbers[i] = (int) Math.round(parsed);
			} catch (NumberFormatException e) {
				numbers[i] = 0;
			}
		}
		return numbers;
	}

	private int[] parseCornerCoordinates(String raw) {
		String[] parts = raw.split(",");
		if (parts.length < 3) {
			return null;
		}
		int[] coords = new int[3];
		for (int i = 0; i < 3; i++) {
			try {
				double parsed = Double.parseDouble(parts[i].trim());
				coords[i] = (int) Math.round(parsed);
			} catch (NumberFormatException e) {
				coords[i] = 0;
			}
		}
		return coords;
	}

	private void parseSpawnValue(HitWallSettings parsed, String line) {
		String[] entries = line.split(",");
		for (String entry : entries) {
			String normalized = entry.replace(":", "=");
			String[] kv = normalized.split("=");
			if (kv.length != 2) {
				continue;
			}
			String key = kv[0].trim().toLowerCase(Locale.ROOT);
			String value = kv[1].trim();
			switch (key) {
				case "x":
					parsed.setSpawnX(parseDouble(value, parsed.getSpawnX()));
					break;
				case "y":
					parsed.setSpawnY(parseDouble(value, parsed.getSpawnY()));
					break;
				case "z":
					parsed.setSpawnZ(parseDouble(value, parsed.getSpawnZ()));
					break;
				case "yaw":
					parsed.setSpawnYaw((float) parseDouble(value, parsed.getSpawnYaw()));
					break;
				case "pitch":
					parsed.setSpawnPitch((float) parseDouble(value, parsed.getSpawnPitch()));
					break;
				default:
					break;
			}
		}
	}

	private HitWallSettings.WallWave parseWave(String line) {
		String[] tokens = line.split(",");
		if (tokens.length < 1) {
			return null;
		}

		int index = 0;
		BlockFace direction = HitWallSettings.parseDirection(tokens[index]);
		if (direction != null) {
			index++;
		}

		if (index >= tokens.length) {
			return null;
		}

		int speed = parseInt(tokens[index], 2);
		int delay = tokens.length > index + 1 ? parseInt(tokens[index + 1], 5) : 5;

		return new HitWallSettings.WallWave(speed, delay);
	}

	private double parseDouble(String raw, double fallback) {
		try {
			return Double.parseDouble(raw.trim());
		} catch (NumberFormatException e) {
			return fallback;
		}
	}

	private int parseInt(String raw, int fallback) {
		try {
			return Integer.parseInt(raw.trim());
		} catch (NumberFormatException e) {
			return fallback;
		}
	}
}
