package mcevent.MCEFramework.games.crazyMiner;

import lombok.Getter;
import lombok.Setter;
import mcevent.MCEFramework.tools.MCEConfigParser;
import org.bukkit.Material;

import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

import static mcevent.MCEFramework.miscellaneous.Constants.plugin;
import static mcevent.MCEFramework.miscellaneous.Constants.crazyMiner;

/*
CrazyMinerConfigParser: CrazyMiner 配置文件解析器
*/
@Getter @Setter
public class CrazyMinerConfigParser extends MCEConfigParser {
    
    // Game area configuration
    private String mapName = "";
    private int gameDuration = 1080; // 18 minutes in seconds
    private double gameAreaCenterX = 5;
    private double gameAreaCenterZ = 14;
    private int gameAreaSizeX = 199;
    private int gameAreaSizeZ = 199;
    private int gameAreaHeight = 22;
    private int gameAreaY = -63;
    private int initialWorldBorderSize = 210;
    private int centerClearAreaSize = 21; // 中心空缺区域大小
    
    // World border shrinking configuration
    private int worldBorderShrink1Time = 180;
    private int worldBorderShrink1Amount = 123;
    private int worldBorderShrink1Duration = 10200;
    private int worldBorderShrink2Time = 690;
    private int worldBorderShrink2Amount = 65;
    private int worldBorderShrink2Duration = 3600;
    
    // Block generation configuration
    private Map<Material, Double> outerRingBlocks = new HashMap<>();
    private Map<Material, Double> innerRingBlocks = new HashMap<>();
    
    public void parse() {
        mapName = "";
        gameDuration = 1080;
        gameAreaCenterX = 5;
        gameAreaCenterZ = 14;
        gameAreaSizeX = 199;
        gameAreaSizeZ = 199;
        gameAreaHeight = 22;
        gameAreaY = -63;
        initialWorldBorderSize = 210;
        centerClearAreaSize = 21;
        worldBorderShrink1Time = 180;
        worldBorderShrink1Amount = 123;
        worldBorderShrink1Duration = 10200;
        worldBorderShrink2Time = 690;
        worldBorderShrink2Amount = 65;
        worldBorderShrink2Duration = 3600;
        outerRingBlocks.clear();
        innerRingBlocks.clear();

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
                plugin.getLogger().info("进入配置段: " + currentSection);
                continue;
            }

            if (currentSection != null) {
                switch (currentSection) {
                    case "map_name":
                        mapName = trimmedLine;
                        crazyMiner.setWorldName(mapName);
                        currentSection = null;
                        break;
                    case "game_duration":
                        gameDuration = Integer.parseInt(trimmedLine);
                        currentSection = null;
                        break;
                    case "game_area_center_x":
                        gameAreaCenterX = Double.parseDouble(trimmedLine);
                        currentSection = null;
                        break;
                    case "game_area_center_z":
                        gameAreaCenterZ = Double.parseDouble(trimmedLine);
                        currentSection = null;
                        break;
                    case "game_area_size_x":
                        gameAreaSizeX = Integer.parseInt(trimmedLine);
                        currentSection = null;
                        break;
                    case "game_area_size_z":
                        gameAreaSizeZ = Integer.parseInt(trimmedLine);
                        currentSection = null;
                        break;
                    case "game_area_height":
                        gameAreaHeight = Integer.parseInt(trimmedLine);
                        currentSection = null;
                        break;
                    case "game_area_y":
                        gameAreaY = Integer.parseInt(trimmedLine);
                        currentSection = null;
                        break;
                    case "initial_world_border_size":
                        initialWorldBorderSize = Integer.parseInt(trimmedLine);
                        currentSection = null;
                        break;
                    case "center_clear_area_size":
                        centerClearAreaSize = Integer.parseInt(trimmedLine);
                        currentSection = null;
                        break;
                    case "world_border_shrink_1_time":
                        worldBorderShrink1Time = Integer.parseInt(trimmedLine);
                        currentSection = null;
                        break;
                    case "world_border_shrink_1_amount":
                        worldBorderShrink1Amount = Integer.parseInt(trimmedLine);
                        currentSection = null;
                        break;
                    case "world_border_shrink_1_duration":
                        worldBorderShrink1Duration = Integer.parseInt(trimmedLine);
                        currentSection = null;
                        break;
                    case "world_border_shrink_2_time":
                        worldBorderShrink2Time = Integer.parseInt(trimmedLine);
                        currentSection = null;
                        break;
                    case "world_border_shrink_2_amount":
                        worldBorderShrink2Amount = Integer.parseInt(trimmedLine);
                        currentSection = null;
                        break;
                    case "world_border_shrink_2_duration":
                        worldBorderShrink2Duration = Integer.parseInt(trimmedLine);
                        currentSection = null;
                        break;
                    case "outer_ring":
                        // Parse outer ring blocks: "BLOCK_NAME probability"
                        plugin.getLogger().info("解析外圈行: " + trimmedLine);
                        String[] outerParts = trimmedLine.split(" ");
                        plugin.getLogger().info("分割结果: " + java.util.Arrays.toString(outerParts));
                        if (outerParts.length == 2) {
                            Material outerMaterial = Material.getMaterial(outerParts[0]);
                            if (outerMaterial != null) {
                                try {
                                    double outerProbability = Double.parseDouble(outerParts[1]);
                                    outerRingBlocks.put(outerMaterial, outerProbability);
                                    plugin.getLogger().info("解析外圈方块: " + outerMaterial + " = " + outerProbability);
                                } catch (NumberFormatException e) {
                                    plugin.getLogger().warning("无法解析外圈方块概率: " + outerParts[1]);
                                }
                            } else {
                                plugin.getLogger().warning("无法找到材质: " + outerParts[0]);
                            }
                        }
                        // 不清空currentSection，继续处理outer_ring部分的其他行
                        break;
                    case "inner_ring":
                        // Parse inner ring blocks: "BLOCK_NAME probability"
                        plugin.getLogger().info("解析内圈行: " + trimmedLine);
                        String[] innerParts = trimmedLine.split(" ");
                        plugin.getLogger().info("分割结果: " + java.util.Arrays.toString(innerParts));
                        if (innerParts.length == 2) {
                            Material innerMaterial = Material.getMaterial(innerParts[0]);
                            if (innerMaterial != null) {
                                try {
                                    double innerProbability = Double.parseDouble(innerParts[1]);
                                    innerRingBlocks.put(innerMaterial, innerProbability);
                                    plugin.getLogger().info("解析内圈方块: " + innerMaterial + " = " + innerProbability);
                                } catch (NumberFormatException e) {
                                    plugin.getLogger().warning("无法解析内圈方块概率: " + innerParts[1]);
                                }
                            } else {
                                plugin.getLogger().warning("无法找到材质: " + innerParts[0]);
                            }
                        }
                        // 不清空currentSection，继续处理inner_ring部分的其他行
                        break;
                }
            }
        }
        
        plugin.getLogger().info("CrazyMiner 配置文件读取完毕 - 外圈方块: " + outerRingBlocks.size() + ", 内圈方块: " + innerRingBlocks.size());
        
        // 调试信息：输出解析到的方块配置
        plugin.getLogger().info("外圈方块配置: " + outerRingBlocks.toString());
        plugin.getLogger().info("内圈方块配置: " + innerRingBlocks.toString());
    }
    
    // Getter methods for new block configurations
    public Map<Material, Double> getOuterRingBlocks() {
        return outerRingBlocks;
    }
    
    public Map<Material, Double> getInnerRingBlocks() {
        return innerRingBlocks;
    }
}