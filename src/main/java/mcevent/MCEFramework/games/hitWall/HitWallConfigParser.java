package mcevent.MCEFramework.games.hitWall;

import lombok.Getter;
import lombok.Setter;
import mcevent.MCEFramework.tools.MCEConfigParser;

import java.io.IOException;
import java.nio.file.Files;

import static mcevent.MCEFramework.miscellaneous.Constants.captureCenter;
import static mcevent.MCEFramework.miscellaneous.Constants.plugin;

@Getter @Setter
public class HitWallConfigParser extends MCEConfigParser {
	public void parse() {
		try {
			lines = Files.readAllLines(configPath);
		} catch (IOException e) {
			e.printStackTrace();
		}

		String currentSection = null;

		for (String line : lines) {
			String trimmedLine = line.trim();

			if (trimmedLine.isEmpty()) continue;

			if (trimmedLine.startsWith("[") && trimmedLine.endsWith("]")) {
				currentSection = trimmedLine.substring(1, trimmedLine.length() - 1);
				continue;
			}

			if (currentSection != null) {
				switch (currentSection) {
					case "map_name":
						for (String value : trimmedLine.split(",")) {
							String v = value.trim();
							captureCenter.setWorldName(v);
						}
						break;
				}
			}
		}
		plugin.getLogger().info("CaptureCenter配置文件读取完毕");

	}
}
