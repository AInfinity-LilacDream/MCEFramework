package mcevent.MCEFramework.games.musicDodge;

import lombok.Getter;
import lombok.Setter;
import mcevent.MCEFramework.games.musicDodge.gameObject.attacks.*;
import mcevent.MCEFramework.tools.MCEConfigParser;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static mcevent.MCEFramework.miscellaneous.Constants.plugin;


/**
 * MusicDodgeConfigParser: 解析MusicDodge游戏配置文件
 * 支持[Attacks]栏位，定义攻击序列
 */
@Getter @Setter
public class MusicDodgeConfigParser extends MCEConfigParser {
    
    private List<String> attackLines = new ArrayList<>();
    private String mapName = "";
    private AttackFactory attackFactory;
    
    @Override
    protected void parse() {
        attackLines.clear();
        mapName = "";
        
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
                    case "Attacks":
                        // 跳过注释行（以#开头的行）
                        if (!trimmedLine.startsWith("#")) {
                            attackLines.add(trimmedLine);
                        }
                        break;
                    case "map_name":
                        mapName = trimmedLine;
                        currentSection = null;
                        break;
                }
            }
        }
        
    }
    
    /**
     * 解析攻击配置并创建攻击对象列表
     * @param worldName 世界名称
     * @param bpm BPM值
     * @return 攻击对象列表
     */
    public List<MCEAttack> parseAttacks(String worldName, int bpm) {
        List<MCEAttack> attacks = new ArrayList<>();
        Location spawnLocation = Objects.requireNonNull(Bukkit.getWorld(worldName)).getSpawnLocation();
        
        for (String line : attackLines) {
            try {
                MCEAttack attack = parseAttackLine(line, worldName, spawnLocation, bpm);
                if (attack != null) {
                    attacks.add(attack);
                }
            } catch (Exception e) {
                plugin.getLogger().severe("解析攻击配置失败: " + line + " - " + e.getMessage());
            }
        }
        
        return attacks;
    }
    
    /**
     * 解析攻击配置并创建攻击配置对象列表（包含同步信息）
     * @param worldName 世界名称
     * @param bpm BPM值
     * @return 攻击配置对象列表
     */
    public List<AttackConfig> parseAttackConfigs(String worldName, int bpm) {
        // 初始化攻击工厂（默认启用优化）
        if (attackFactory == null) {
            attackFactory = new AttackFactory(plugin, true);
        }
        
        List<AttackConfig> attackConfigs = new ArrayList<>();
        Location spawnLocation = Objects.requireNonNull(Bukkit.getWorld(worldName)).getSpawnLocation();
        
        for (String line : attackLines) {
            try {
                AttackConfig config = parseAttackConfigLine(line, worldName, spawnLocation, bpm);
                if (config != null) {
                    attackConfigs.add(config);
                }
            } catch (Exception e) {
                plugin.getLogger().severe("解析攻击配置失败: " + line + " - " + e.getMessage());
            }
        }
        
        return attackConfigs;
    }
    
    /**
     * 解析单行攻击配置（包含同步信息）
     * @param line 配置行
     * @param worldName 世界名称
     * @param spawnLocation 生成位置
     * @param bpm BPM值
     * @return 攻击配置对象，解析失败返回null
     */
    private AttackConfig parseAttackConfigLine(String line, String worldName, Location spawnLocation, int bpm) {
        String[] parts = line.split(",");
        if (parts.length == 0) return null;
        
        // 检查最后一个参数是否为"sync"
        boolean sync = false;
        String[] attackParts = parts;
        if (parts.length > 1 && parts[parts.length - 1].trim().equalsIgnoreCase("sync")) {
            sync = true;
            // 移除sync参数，只保留攻击参数
            attackParts = new String[parts.length - 1];
            System.arraycopy(parts, 0, attackParts, 0, parts.length - 1);
        }
        
        MCEAttack attack = parseAttackFromParts(attackParts, worldName, spawnLocation, bpm);
        return attack != null ? new AttackConfig(attack, sync) : null;
    }
    
    /**
     * 解析单行攻击配置
     * @param line 配置行
     * @param worldName 世界名称
     * @param spawnLocation 生成位置
     * @param bpm BPM值
     * @return 攻击对象，解析失败返回null
     */
    private MCEAttack parseAttackLine(String line, String worldName, Location spawnLocation, int bpm) {
        String[] parts = line.split(",");
        return parseAttackFromParts(parts, worldName, spawnLocation, bpm);
    }
    
    /**
     * 从参数数组解析攻击对象
     * @param parts 参数数组
     * @param worldName 世界名称  
     * @param spawnLocation 生成位置
     * @param bpm BPM值
     * @return 攻击对象，解析失败返回null
     */
    private MCEAttack parseAttackFromParts(String[] parts, String worldName, Location spawnLocation, int bpm) {
        if (parts.length == 0) return null;
        
        String attackType = parts[0].trim();
        
        switch (attackType) {
            case "Void":
                if (parts.length >= 2) {
                    double duration = Double.parseDouble(parts[1].trim());
                    return new VoidAttack(duration, bpm);
                }
                break;
                
            case "SquareRing":
                if (parts.length >= 5) {
                    double alertDuration = Double.parseDouble(parts[1].trim());
                    double attackDuration = Double.parseDouble(parts[2].trim());
                    int innerRadius = Integer.parseInt(parts[3].trim());
                    int outerRadius = Integer.parseInt(parts[4].trim());
                    // 使用场地中心位置
                    Location center = calculateFieldCenter(spawnLocation.getWorld());
                    return attackFactory.createSquareRingAttack(alertDuration, attackDuration, center, innerRadius, outerRadius, bpm);
                }
                break;
                
            case "Spin":
                if (parts.length >= 5) {
                    double alertDuration = Double.parseDouble(parts[1].trim());
                    double attackDuration = Double.parseDouble(parts[2].trim());
                    int rayCount = Integer.parseInt(parts[3].trim());
                    double rotationSpeed = Double.parseDouble(parts[4].trim());
                    // 使用场地中心位置
                    Location center = calculateFieldCenter(spawnLocation.getWorld());
                    return attackFactory.createSpinAttack(alertDuration, attackDuration, center, rayCount, rotationSpeed, bpm);
                }
                break;
                
            case "Wall":
                if (parts.length >= 4) {
                    double alertDuration = Double.parseDouble(parts[1].trim());
                    double attackDuration = Double.parseDouble(parts[2].trim());
                    String direction = parts[3].trim();
                    return attackFactory.createWallAttack(alertDuration, attackDuration, direction, Objects.requireNonNull(Bukkit.getWorld(worldName)), bpm);
                }
                break;
                
            case "Random":
                if (parts.length >= 6) {
                    double attackDuration = Double.parseDouble(parts[1].trim());
                    int circleCount = Integer.parseInt(parts[2].trim());
                    double generationSpeed = Double.parseDouble(parts[3].trim());
                    double circleRadius = Double.parseDouble(parts[4].trim());
                    double alertDuration = Double.parseDouble(parts[5].trim());
                    return attackFactory.createRandomAttack(attackDuration, Objects.requireNonNull(Bukkit.getWorld(worldName)), circleCount, generationSpeed, circleRadius, alertDuration, bpm);
                }
                break;
                
            case "Bar":
                if (parts.length >= 5) {
                    double alertDuration = Double.parseDouble(parts[1].trim());
                    double attackDuration = Double.parseDouble(parts[2].trim());
                    String direction = parts[3].trim();
                    int spacing = Integer.parseInt(parts[4].trim());
                    return attackFactory.createBarAttack(alertDuration, attackDuration, direction, spacing, Objects.requireNonNull(Bukkit.getWorld(worldName)), bpm);
                }
                break;
                
            case "RandomLaser":
                if (parts.length >= 5) {
                    double attackDuration = Double.parseDouble(parts[1].trim());
                    int laserCount = Integer.parseInt(parts[2].trim());
                    double generationSpeed = Double.parseDouble(parts[3].trim());
                    double alertDuration = Double.parseDouble(parts[4].trim());
                    return attackFactory.createRandomLaserAttack(attackDuration, Objects.requireNonNull(Bukkit.getWorld(worldName)), laserCount, generationSpeed, alertDuration, bpm);
                }
                break;
                
            case "Side":
                if (parts.length >= 5) {
                    double attackDuration = Double.parseDouble(parts[1].trim());
                    int ringThickness = Integer.parseInt(parts[2].trim());
                    double ringAlert = Double.parseDouble(parts[3].trim());
                    double ringAttack = Double.parseDouble(parts[4].trim());
                    return attackFactory.createSideAttack(attackDuration, worldName, ringThickness, ringAlert, ringAttack, bpm);
                }
                break;
                
            case "Circle":
                if (parts.length >= 4) {
                    double alertDuration = Double.parseDouble(parts[1].trim());
                    double attackDuration = Double.parseDouble(parts[2].trim());
                    int radius = Integer.parseInt(parts[3].trim());
                    // 使用场地中心位置
                    Location center = calculateFieldCenter(spawnLocation.getWorld());
                    return attackFactory.createCircleAttack(alertDuration, attackDuration, center, radius, bpm);
                }
                break;
                
            case "Laser":
                if (parts.length >= 7) {
                    double alertDuration = Double.parseDouble(parts[1].trim());
                    double attackDuration = Double.parseDouble(parts[2].trim());
                    int startX = Integer.parseInt(parts[3].trim());
                    int startY = Integer.parseInt(parts[4].trim());
                    int endX = Integer.parseInt(parts[5].trim());
                    int endY = Integer.parseInt(parts[6].trim());
                    return attackFactory.createLaserAttack(alertDuration, attackDuration, startX, startY, endX, endY, spawnLocation.getWorld(), bpm);
                }
                break;
                
            default:
                plugin.getLogger().warning("未知的攻击类型: " + attackType);
                break;
        }
        
        return null;
    }
    
    /**
     * 计算场地中心位置
     */
    private Location calculateFieldCenter(org.bukkit.World world) {
        // MusicDodge场地范围：从(-7, -60, -46)到(35, -60, -4)
        double centerX = -7.0 + (43 - 1) / 2.0;  // 场地宽度43格
        double centerZ = -46.0 + (43 - 1) / 2.0; // 场地长度43格
        double centerY = -60.0;
        return new Location(world, centerX, centerY, centerZ);
    }
}
