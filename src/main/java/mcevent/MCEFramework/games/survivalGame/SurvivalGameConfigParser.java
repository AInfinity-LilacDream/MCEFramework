package mcevent.MCEFramework.games.survivalGame;

import lombok.Getter;
import lombok.Setter;
import mcevent.MCEFramework.tools.MCEConfigParser;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static mcevent.MCEFramework.miscellaneous.Constants.*;

/*
SurvivalGameConfigParser: 读取饥饿游戏配置文件
 */
@Getter
@Setter
public class SurvivalGameConfigParser extends MCEConfigParser {
    private List<Location> spawnPoints = new ArrayList<>();
    private List<Location> chestLocations = new ArrayList<>();
    private Location centerLocation;
    private String mapName = "";

    public void parse() {
        spawnPoints.clear();
        chestLocations.clear();
        centerLocation = null; // 不再从配置解析中心
        mapName = "";

        try {
            lines = Files.readAllLines(configPath);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 第一遍：先找到 map_name
        for (String line : lines) {
            String trimmedLine = line.trim();
            if (trimmedLine.isEmpty())
                continue;

            if (trimmedLine.equals("[map_name]")) {
                int index = lines.indexOf(line);
                if (index + 1 < lines.size()) {
                    mapName = lines.get(index + 1).trim();
                    break;
                }
            }
        }

        // 第二遍：解析其他内容
        String currentSection = null;

        for (String line : lines) {
            String trimmedLine = line.trim();

            if (trimmedLine.isEmpty())
                continue;

            if (trimmedLine.startsWith("[") && trimmedLine.endsWith("]")) {
                currentSection = trimmedLine.substring(1, trimmedLine.length() - 1);
                continue;
            }

            if (currentSection != null) {
                switch (currentSection) {
                    case "spawn_points":
                        parseLocation(trimmedLine, spawnPoints);
                        break;
                    case "chest_loc":
                        parseLocation(trimmedLine, chestLocations);
                        break;
                    // 不再从配置解析中心坐标
                    case "map_name":
                        // 已经在第一遍解析过了，跳过
                        currentSection = null;
                        break;
                }
            }
        }

        // 如果从数据目录解析后仍未获得任何 chest_loc，尝试回退读取 jar 内置资源（避免数据目录旧文件未更新）
        if (chestLocations.isEmpty()) {
            try {
                String relPath = dataFolderPath != null && configPath != null
                        ? dataFolderPath.relativize(configPath).toString().replace('\\', '/')
                        : "MCEConfig/SurvivalGame.cfg";
                try (var in = plugin.getResource(relPath)) {
                    if (in != null) {
                        java.io.BufferedReader br = new java.io.BufferedReader(
                                new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8));
                        java.util.List<String> resourceLines = new java.util.ArrayList<>();
                        String s;
                        while ((s = br.readLine()) != null)
                            resourceLines.add(s);

                        // 重新获取 map_name
                        for (String line : resourceLines) {
                            String trimmed = line.trim();
                            if (trimmed.equals("[map_name]")) {
                                int idx = resourceLines.indexOf(line);
                                if (idx + 1 < resourceLines.size()) {
                                    mapName = resourceLines.get(idx + 1).trim();
                                    break;
                                }
                            }
                        }

                        String section2 = null;
                        for (String line : resourceLines) {
                            String trimmed = line.trim();
                            if (trimmed.isEmpty())
                                continue;
                            if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                                section2 = trimmed.substring(1, trimmed.length() - 1);
                                continue;
                            }
                            if (section2 != null) {
                                switch (section2) {
                                    case "spawn_points" -> parseLocation(trimmed, spawnPoints);
                                    case "chest_loc" -> parseLocation(trimmed, chestLocations);
                                    // 不再从资源解析中心坐标
                                    case "map_name" -> section2 = null;
                                }
                            }
                        }
                        plugin.getLogger().warning(
                                "[SurvivalGameConfigParser] chest_loc loaded from JAR resource fallback, count="
                                        + chestLocations.size());
                    }
                }
            } catch (Exception e) {
                plugin.getLogger().warning("[SurvivalGameConfigParser] resource fallback failed: " + e.getMessage());
            }
        }
        plugin.getLogger().info("饥饿游戏配置文件读取完毕");
        // 调试：打印配置文件路径与解析结果统计
        try {
            String pathStr = configPath != null ? configPath.toString() : "<null>";
            plugin.getLogger().info("[SurvivalGameConfigParser] using config=" + pathStr
                    + ", mapName=" + mapName
                    + ", chest_loc count=" + chestLocations.size()
                    + ", spawn_points count=" + spawnPoints.size());
        } catch (Exception ignored) {
        }
    }

    private void parseLocation(String line, List<Location> locations) {
        String[] parts = line.split(",");
        if (parts.length >= 3) {
            try {
                double x = Double.parseDouble(parts[0].trim());
                double y = Double.parseDouble(parts[1].trim());
                double z = Double.parseDouble(parts[2].trim());
                float yaw = parts.length >= 4 ? Float.parseFloat(parts[3].trim()) : 0;
                float pitch = parts.length >= 5 ? Float.parseFloat(parts[4].trim()) : 0;
                // 优先使用配置文件中的 mapName，如果该世界未加载，则使用游戏实例的 worldName 作为回退
                String worldName = mapName;
                if (Bukkit.getWorld(worldName) == null && survivalGame != null && survivalGame.getWorldName() != null) {
                    worldName = survivalGame.getWorldName();
                }
                Location loc = new Location(Bukkit.getWorld(worldName), x, y, z, yaw, pitch);
                locations.add(loc);
            } catch (NumberFormatException e) {
                plugin.getLogger().warning("无法解析坐标: " + line);
            }
        }
    }

    private Location parseLocationSingle(String line) {
        String[] parts = line.split(",");
        if (parts.length >= 3) {
            try {
                double x = Double.parseDouble(parts[0].trim());
                double y = Double.parseDouble(parts[1].trim());
                double z = Double.parseDouble(parts[2].trim());

                return new Location(Bukkit.getWorld(mapName), x, y, z);
            } catch (NumberFormatException e) {
                plugin.getLogger().warning("无法解析中心坐标: " + line);
            }
        }
        return null;
    }
}
