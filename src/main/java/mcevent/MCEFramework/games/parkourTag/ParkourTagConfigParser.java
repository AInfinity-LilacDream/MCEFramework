package mcevent.MCEFramework.games.parkourTag;

import lombok.Getter;
import lombok.Setter;
import mcevent.MCEFramework.tools.MCEConfigParser;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import static mcevent.MCEFramework.miscellaneous.Constants.plugin;

@Getter @Setter
public class ParkourTagConfigParser extends MCEConfigParser {
    private String mapName = "";
    private Map<String, double[]> locationData = new HashMap<>();
    
    @Override
    protected void parse() {
        mapName = "";
        locationData.clear();
        
        try {
            lines = Files.readAllLines(configPath);
        } catch (IOException e) {
            e.printStackTrace();
            return;
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
                        mapName = trimmedLine;
                        currentSection = null;
                        break;
                    default:
                        // 检查是否为位置配置（以pkt开头的配置项）
                        if (currentSection.startsWith("pkt")) {
                            parseLocationData(currentSection, trimmedLine);
                            currentSection = null;
                        }
                        break;
                }
            }
        }
        
        plugin.getLogger().info("ParkourTag配置文件读取完毕，地图：" + mapName + "，位置配置：" + locationData.size() + "个");
    }
    
    /**
     * 解析位置数据 (x, y, z)
     */
    private void parseLocationData(String locationName, String data) {
        try {
            String[] parts = data.split(",");
            if (parts.length == 3) {
                double[] coords = new double[3];
                coords[0] = Double.parseDouble(parts[0].trim()); // x
                coords[1] = Double.parseDouble(parts[1].trim()); // y  
                coords[2] = Double.parseDouble(parts[2].trim()); // z
                locationData.put(locationName, coords);
            }
        } catch (NumberFormatException e) {
            plugin.getLogger().warning("解析位置配置失败: " + locationName + " = " + data);
        }
    }
    
    /**
     * 获取位置对象
     * @param locationName 位置名称
     * @return Location对象，如果不存在则返回null
     */
    public Location getLocation(String locationName) {
        double[] coords = locationData.get(locationName);
        if (coords != null && coords.length == 3) {
            return new Location(Bukkit.getWorld(mapName), coords[0], coords[1], coords[2]);
        }
        return null;
    }
}
