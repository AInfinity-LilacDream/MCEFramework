package mcevent.MCEFramework.games.crazyMiner;

import mcevent.MCEFramework.games.crazyMiner.gameObject.CrazyMinerGameBoard;
import mcevent.MCEFramework.tools.MCEMessenger;
import mcevent.MCEFramework.tools.MCEPlayerUtils;
import mcevent.MCEFramework.tools.MCETeamUtils;
import mcevent.MCEFramework.tools.MCETimerUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Team;

import java.util.*;

import static mcevent.MCEFramework.miscellaneous.Constants.*;

public class CrazyMinerFuncImpl {
    
    private static final CrazyMinerConfigParser crazyMinerConfigParser = crazyMiner.getCrazyMinerConfigParser();
    
    /**
     * 从配置文件加载数据
     */
    public static void loadConfig(CrazyMiner game) {
        game.setIntroTextList(crazyMinerConfigParser.openAndParse(game.getConfigFileName()));
        
        // 加载游戏区域配置
        game.setGameAreaSizeX(crazyMinerConfigParser.getGameAreaSizeX());
        game.setGameAreaSizeZ(crazyMinerConfigParser.getGameAreaSizeZ());
        game.setGameAreaHeight(crazyMinerConfigParser.getGameAreaHeight());
        game.setGameAreaY(crazyMinerConfigParser.getGameAreaY());
        
        game.setGameAreaCenter(new Location(Bukkit.getWorld(game.getWorldName()), 
            crazyMinerConfigParser.getGameAreaCenterX(), game.getGameAreaY(), 
            crazyMinerConfigParser.getGameAreaCenterZ()));
        
        // 加载方块配置
        game.setOuterRingBlocks(crazyMinerConfigParser.getOuterRingBlocks());
        game.setInnerRingBlocks(crazyMinerConfigParser.getInnerRingBlocks());
        
        // 调试信息：输出所有配置参数
        plugin.getLogger().info("=== CrazyMiner 配置加载完成 ===");
        plugin.getLogger().info("地图名称: " + crazyMinerConfigParser.getMapName());
        plugin.getLogger().info("游戏时长: " + crazyMinerConfigParser.getGameDuration() + " 秒");
        plugin.getLogger().info("游戏区域中心: (" + crazyMinerConfigParser.getGameAreaCenterX() + ", " + crazyMinerConfigParser.getGameAreaCenterZ() + ")");
        plugin.getLogger().info("游戏区域大小: " + crazyMinerConfigParser.getGameAreaSizeX() + "x" + crazyMinerConfigParser.getGameAreaSizeZ());
        plugin.getLogger().info("游戏区域高度: " + crazyMinerConfigParser.getGameAreaHeight());
        plugin.getLogger().info("游戏区域Y坐标: " + crazyMinerConfigParser.getGameAreaY());
        plugin.getLogger().info("初始世界边界大小: " + crazyMinerConfigParser.getInitialWorldBorderSize());
        plugin.getLogger().info("中心空缺区域大小: " + crazyMinerConfigParser.getCenterClearAreaSize() + "x" + crazyMinerConfigParser.getCenterClearAreaSize());
        plugin.getLogger().info("第一次边界收缩时间: " + crazyMinerConfigParser.getWorldBorderShrink1Time() + " 秒");
        plugin.getLogger().info("第一次边界收缩量: " + crazyMinerConfigParser.getWorldBorderShrink1Amount());
        plugin.getLogger().info("第一次边界收缩持续时间: " + crazyMinerConfigParser.getWorldBorderShrink1Duration() + " ticks");
        plugin.getLogger().info("第二次边界收缩时间: " + crazyMinerConfigParser.getWorldBorderShrink2Time() + " 秒");
        plugin.getLogger().info("第二次边界收缩量: " + crazyMinerConfigParser.getWorldBorderShrink2Amount());
        plugin.getLogger().info("第二次边界收缩持续时间: " + crazyMinerConfigParser.getWorldBorderShrink2Duration() + " ticks");
        plugin.getLogger().info("外圈方块类型数: " + (game.getOuterRingBlocks() != null ? game.getOuterRingBlocks().size() : 0));
        plugin.getLogger().info("内圈方块类型数: " + (game.getInnerRingBlocks() != null ? game.getInnerRingBlocks().size() : 0));
        plugin.getLogger().info("================================");
    }
    
    /**
     * 初始化游戏区域，设置基岩边界和世界边界
     */
    public static void initializeGameArea(CrazyMiner game) {
        World world = Bukkit.getWorld(game.getWorldName());
        if (world == null) return;
        
        Location center = game.getGameAreaCenter();
        double centerX = center.getX();
        double centerZ = center.getZ();
        int y = game.getGameAreaY();
        CrazyMinerConfigParser config = game.getCrazyMinerConfigParser();
        
        // 设置世界边界 (边界中心比游戏中心各偏移1格)
        WorldBorder border = world.getWorldBorder();
        double borderCenterX = centerX + 0.5;
        double borderCenterZ = centerZ + 0.5;
        border.setCenter(borderCenterX, borderCenterZ);
        border.setSize(config.getInitialWorldBorderSize());
        border.setWarningDistance(5);
        border.setWarningTime(10);
        
        plugin.getLogger().info("游戏中心: (" + centerX + ", " + centerZ + ")");
        plugin.getLogger().info("边界中心: (" + borderCenterX + ", " + borderCenterZ + ")");
        
        // 清理中心区域为空气
        clearCenterArea(world, (int)centerX, (int)centerZ, y, config);
        
        // 填充游戏区域边界为基岩
        fillGameAreaBoundaries(world, (int)centerX, (int)centerZ, y, game);
    }
    
    /**
     * 清理中心区域为空气
     */
    private static void clearCenterArea(World world, int centerX, int centerZ, int y, CrazyMinerConfigParser config) {
        int halfClearSize = config.getCenterClearAreaSize() / 2;
        for (int x = centerX - halfClearSize; x <= centerX + halfClearSize; x++) {
            for (int z = centerZ - halfClearSize; z <= centerZ + halfClearSize; z++) {
                for (int currentY = y; currentY < y + config.getGameAreaHeight(); currentY++) {
                    world.getBlockAt(x, currentY, z).setType(Material.AIR);
                }
            }
        }
    }
    
    /**
     * 初始化游戏地图：创建199x22x199的基岩结构，中间留出21x22x21空缺，最外层保持基岩
     */
    private static void fillGameAreaBoundaries(World world, int centerX, int centerZ, int y, CrazyMiner game) {
        int halfSizeX = game.getGameAreaSizeX() / 2; // 199/2 = 99
        int halfSizeZ = game.getGameAreaSizeZ() / 2; // 199/2 = 99  
        int height = game.getGameAreaHeight(); // 22
        
        // 填充整个199x22x199区域为基岩 
        // Y从-63到-42 (总共22格: -63, -62, -61, ..., -44, -43, -42)
        for (int x = centerX - halfSizeX; x <= centerX + halfSizeX; x++) {
            for (int z = centerZ - halfSizeZ; z <= centerZ + halfSizeZ; z++) {
                for (int currentY = y; currentY < y + height; currentY++) {
                    world.getBlockAt(x, currentY, z).setType(Material.BEDROCK);
                }
            }
        }
        
        // 挖出中心空缺区域
        CrazyMinerConfigParser config = game.getCrazyMinerConfigParser();
        int halfClearSize = config.getCenterClearAreaSize() / 2;
        for (int x = centerX - halfClearSize; x <= centerX + halfClearSize; x++) {
            for (int z = centerZ - halfClearSize; z <= centerZ + halfClearSize; z++) {
                for (int currentY = y; currentY < y + height; currentY++) {
                    if (currentY == y) {
                        // 底层设为空气，玩家可以掉入虚空
                        world.getBlockAt(x, currentY, z).setType(Material.AIR);
                    } else if (currentY == y + height - 1) {
                        // 顶层设为屏障，防止玩家逃脱
                        world.getBlockAt(x, currentY, z).setType(Material.BARRIER);
                    } else {
                        // 中间层设为空气
                        world.getBlockAt(x, currentY, z).setType(Material.AIR);
                    }
                }
            }
        }
        
        // 创建8个出生点洞穴(7x5x5)
        createSpawnHoles(world, centerX, centerZ, y, height, halfSizeX, halfSizeZ, game);
        
        // 强制重新生成最外层基岩墙，确保完整性
        reinforceOuterBedrockWall(world, centerX, centerZ, y, height, halfSizeX, halfSizeZ);
    }
    
    /**
     * 强制重新生成最外层基岩墙，确保完整性
     */
    private static void reinforceOuterBedrockWall(World world, int centerX, int centerZ, int y, int height, int halfSizeX, int halfSizeZ) {
        plugin.getLogger().info("强化最外层基岩墙...");
        
        for (int currentY = y; currentY < y + height; currentY++) {
            // 北墙 (Z最小)
            int northZ = centerZ - halfSizeZ;
            for (int x = centerX - halfSizeX; x <= centerX + halfSizeX; x++) {
                world.getBlockAt(x, currentY, northZ).setType(Material.BEDROCK);
            }
            
            // 南墙 (Z最大)
            int southZ = centerZ + halfSizeZ;
            for (int x = centerX - halfSizeX; x <= centerX + halfSizeX; x++) {
                world.getBlockAt(x, currentY, southZ).setType(Material.BEDROCK);
            }
            
            // 西墙 (X最小)
            int westX = centerX - halfSizeX;
            for (int z = centerZ - halfSizeZ; z <= centerZ + halfSizeZ; z++) {
                world.getBlockAt(westX, currentY, z).setType(Material.BEDROCK);
            }
            
            // 东墙 (X最大)
            int eastX = centerX + halfSizeX;
            for (int z = centerZ - halfSizeZ; z <= centerZ + halfSizeZ; z++) {
                world.getBlockAt(eastX, currentY, z).setType(Material.BEDROCK);
            }
        }
        
        // 强化顶层和底层基岩，但不覆盖中心区域
        for (int x = centerX - halfSizeX; x <= centerX + halfSizeX; x++) {
            for (int z = centerZ - halfSizeZ; z <= centerZ + halfSizeZ; z++) {
                // 跳过中心清理区域，保持其特殊设置
                int halfClearSize = crazyMinerConfigParser.getCenterClearAreaSize() / 2;
                if (Math.abs(x - centerX) <= halfClearSize && Math.abs(z - centerZ) <= halfClearSize) {
                    continue;
                }
                world.getBlockAt(x, y, z).setType(Material.BEDROCK); // 底层
                world.getBlockAt(x, y + height - 1, z).setType(Material.BEDROCK); // 顶层
            }
        }
        
        plugin.getLogger().info("最外层基岩墙强化完成");
    }
    
    /**
     * 创建8个出生点洞穴 (7x5x5)
     * 在每个方向的边缘基岩处对称开两个洞，Y轴位置在中间
     */
    private static void createSpawnHoles(World world, int centerX, int centerZ, int y, int height, int halfSizeX, int halfSizeZ, CrazyMiner game) {
        plugin.getLogger().info("开始创建8个出生点洞穴...");
        
        // Y轴中心位置 (中间5层的中心)
        int midY = y + height / 2 - 2; // 从中心往下2层开始，总共5层
        
        List<Location> spawnPoints = new ArrayList<>();
        
        // 北墙 (Z最小) - 2个洞，往内移一格保护最外层基岩墙，分布到五分点位置
        int northZ = centerZ - halfSizeZ + 1;
        createSpawnHole(world, centerX - 60, midY, northZ, spawnPoints); // 左洞 (五分之一位置)
        createSpawnHole(world, centerX + 60, midY, northZ, spawnPoints); // 右洞 (五分之四位置)
        
        // 南墙 (Z最大) - 2个洞，往内移一格保护最外层基岩墙，分布到五分点位置  
        int southZ = centerZ + halfSizeZ - 1;
        createSpawnHole(world, centerX - 60, midY, southZ, spawnPoints); // 左洞 (五分之一位置)
        createSpawnHole(world, centerX + 60, midY, southZ, spawnPoints); // 右洞 (五分之四位置)
        
        // 西墙 (X最小) - 2个洞，往内移一格保护最外层基岩墙，分布到五分点位置
        int westX = centerX - halfSizeX + 1;
        createSpawnHole(world, westX, midY, centerZ - 60, spawnPoints); // 上洞 (五分之一位置)
        createSpawnHole(world, westX, midY, centerZ + 60, spawnPoints); // 下洞 (五分之四位置)
        
        // 东墙 (X最大) - 2个洞，往内移一格保护最outer基岩墙，分布到五分点位置
        int eastX = centerX + halfSizeX - 1;
        createSpawnHole(world, eastX, midY, centerZ - 60, spawnPoints); // 上洞 (五分之一位置)
        createSpawnHole(world, eastX, midY, centerZ + 60, spawnPoints); // 下洞 (五分之四位置)
        
        game.setSpawnPoints(spawnPoints);
        plugin.getLogger().info("创建了 " + spawnPoints.size() + " 个出生点");
    }
    
    /**
     * 创建单个7x5x5出生点洞穴
     */
    private static void createSpawnHole(World world, int centerX, int centerY, int centerZ, List<Location> spawnPoints) {
        // 7x5x5洞穴 (X方向7格，Y方向5格，Z方向5格)
        for (int x = centerX - 3; x <= centerX + 3; x++) {
            for (int y = centerY; y < centerY + 5; y++) {
                for (int z = centerZ - 2; z <= centerZ + 2; z++) {
                    world.getBlockAt(x, y, z).setType(Material.AIR);
                }
            }
        }
        
        // 在洞穴底层铺圆石/苔石层
        Random random = new Random();
        for (int x = centerX - 3; x <= centerX + 3; x++) {
            for (int z = centerZ - 2; z <= centerZ + 2; z++) {
                // 底层随机选择圆石或苔石圆石
                Material floorMaterial = random.nextBoolean() ? Material.COBBLESTONE : Material.MOSSY_COBBLESTONE;
                world.getBlockAt(x, centerY - 1, z).setType(floorMaterial);
                
                // 顶层也随机选择圆石或苔石圆石
                Material ceilingMaterial = random.nextBoolean() ? Material.COBBLESTONE : Material.MOSSY_COBBLESTONE;
                world.getBlockAt(x, centerY + 5, z).setType(ceilingMaterial);
            }
        }
        
        // 添加出生点位置 (洞穴中心点向内1格)
        Location spawnLocation = new Location(world, centerX, centerY + 1, centerZ);
        spawnPoints.add(spawnLocation);
        
        plugin.getLogger().info("创建出生点洞穴: " + centerX + "," + (centerY + 1) + "," + centerZ);
    }
    
    /**
     * 使用最优分配算法将队伍分配到出生点，使队伍间距离方差最小
     */
    public static void assignTeamsToSpawnPoints(CrazyMiner game) {
        List<Location> spawnPoints = game.getSpawnPoints();
        List<Team> activeTeams = game.getActiveTeams();
        
        if (spawnPoints.size() < 8) {
            plugin.getLogger().warning("出生点数量不足！出生点: " + spawnPoints.size());
            return;
        }
        
        if (activeTeams.isEmpty()) {
            plugin.getLogger().warning("没有活跃队伍！");
            return;
        }
        
        plugin.getLogger().info("开始为 " + activeTeams.size() + " 个队伍分配出生点...");
        
        // 根据队伍数量选择最优分配策略
        List<Integer> selectedSpawnIndices = selectOptimalSpawnPoints(spawnPoints, activeTeams.size());
        
        Map<Team, Location> teamSpawnPoints = new HashMap<>();
        for (int i = 0; i < activeTeams.size(); i++) {
            Team team = activeTeams.get(i);
            int spawnIndex = selectedSpawnIndices.get(i);
            Location spawnPoint = spawnPoints.get(spawnIndex);
            teamSpawnPoints.put(team, spawnPoint);
            
            plugin.getLogger().info("队伍 " + team.getName() + " 分配到出生点 " + spawnIndex + ": " + 
                (int)spawnPoint.getX() + "," + (int)spawnPoint.getY() + "," + (int)spawnPoint.getZ());
        }
        
        game.setTeamSpawnPoints(teamSpawnPoints);
    }
    
    /**
     * 根据队伍数量选择最优的出生点组合，使队伍间距离方差最小
     */
    private static List<Integer> selectOptimalSpawnPoints(List<Location> spawnPoints, int teamCount) {
        // 出生点按方向分组：北(0,1), 南(2,3), 西(4,5), 东(6,7)
        List<Integer> selectedIndices = new ArrayList<>();
        
        switch (teamCount) {
            case 1:
                // 1个队伍：任选一个出生点
                selectedIndices.add(0);
                break;
            case 2:
                // 2个队伍：选择对角线上最远的两个出生点
                selectedIndices.addAll(Arrays.asList(0, 2)); // 北墙左，南墙左
                break;
            case 3:
                // 3个队伍：选择三个方向，尽量均匀分布
                selectedIndices.addAll(Arrays.asList(0, 2, 4)); // 北，南，西
                break;
            case 4:
                // 4个队伍：每个方向选择一个，最均匀分布
                selectedIndices.addAll(Arrays.asList(0, 2, 4, 6)); // 北左，南左，西上，东上
                break;
            case 5:
                // 5个队伍：4个方向各一个，加一个对角
                selectedIndices.addAll(Arrays.asList(0, 2, 4, 6, 1)); // 4个方向 + 北右
                break;
            case 6:
                // 6个队伍：每个方向各一个，再加两个对角
                selectedIndices.addAll(Arrays.asList(0, 2, 4, 6, 1, 3)); // 4个方向 + 北右，南右
                break;
            case 7:
                // 7个队伍：除了一个出生点，其他都用
                selectedIndices.addAll(Arrays.asList(0, 1, 2, 3, 4, 6, 7)); // 除了西下(5)
                break;
            case 8:
            default:
                // 8个或更多队伍：使用所有出生点
                for (int i = 0; i < Math.min(8, teamCount); i++) {
                    selectedIndices.add(i);
                }
                break;
        }
        
        plugin.getLogger().info("为 " + teamCount + " 个队伍选择出生点: " + selectedIndices);
        return selectedIndices;
    }
    
    /**
     * 将所有队伍传送到对应的出生点
     */
    public static void teleportTeamsToSpawnPoints(CrazyMiner game) {
        Map<Team, Location> teamSpawnPoints = game.getTeamSpawnPoints();
        
        if (teamSpawnPoints.isEmpty()) {
            plugin.getLogger().warning("队伍出生点映射为空！");
            return;
        }
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            Team playerTeam = MCETeamUtils.getTeam(player);
            if (playerTeam != null && teamSpawnPoints.containsKey(playerTeam)) {
                Location spawnPoint = teamSpawnPoints.get(playerTeam);
                player.teleport(spawnPoint);
                plugin.getLogger().info("传送玩家 " + player.getName() + " 到队伍 " + playerTeam.getName() + " 的出生点");
            } else {
                plugin.getLogger().warning("玩家 " + player.getName() + " 没有队伍或队伍没有分配出生点");
            }
        }
    }
    
    /**
     * 在基岩结构内生成随机方块
     * 在199x22x199基岩结构中填充随机方块，跳过中心21x22x21空缺区域
     */
    public static void generateRandomBlocks(CrazyMiner game) {
        World world = Bukkit.getWorld(game.getWorldName());
        if (world == null) {
            plugin.getLogger().warning("世界为空，无法生成随机方块！");
            return;
        }
        
        Random random = new Random();
        Map<Material, Double> outerRingBlocks = game.getOuterRingBlocks();
        Map<Material, Double> innerRingBlocks = game.getInnerRingBlocks();
        
        plugin.getLogger().info("=== 开始生成随机方块 ===");
        plugin.getLogger().info("世界名称: " + game.getWorldName());
        plugin.getLogger().info("外圈方块数量: " + (outerRingBlocks != null ? outerRingBlocks.size() : "null"));
        plugin.getLogger().info("内圈方块数量: " + (innerRingBlocks != null ? innerRingBlocks.size() : "null"));
        
        if (outerRingBlocks == null || outerRingBlocks.isEmpty()) {
            plugin.getLogger().warning("外圈方块配置为空！");
        }
        if (innerRingBlocks == null || innerRingBlocks.isEmpty()) {
            plugin.getLogger().warning("内圈方块配置为空！");
        }
        
        Location center = game.getGameAreaCenter();
        int centerX = (int) center.getX();
        int centerZ = (int) center.getZ();
        int y = game.getGameAreaY();
        int halfSizeX = game.getGameAreaSizeX() / 2; // 99
        int halfSizeZ = game.getGameAreaSizeZ() / 2; // 99
        int height = game.getGameAreaHeight(); // 22
        
        // 内圈87x87范围的边界 (中心点为centerX, centerZ)
        int innerHalfSize = 43; // 87/2 = 43.5, 取43
        int innerMinX = centerX - innerHalfSize;
        int innerMaxX = centerX + innerHalfSize;
        int innerMinZ = centerZ - innerHalfSize;
        int innerMaxZ = centerZ + innerHalfSize;
        
        // 在基岩结构中填充随机方块
        int totalBlocks = 0;
        int replacedBlocks = 0;
        
        plugin.getLogger().info("开始替换基岩方块，范围: x(" + (centerX - halfSizeX) + "~" + (centerX + halfSizeX) + "), y(" + y + "~" + (y + height - 1) + "), z(" + (centerZ - halfSizeZ) + "~" + (centerZ + halfSizeZ) + ")");
        
        // 在整个游戏区域内替换基岩（但保持最外层边界不变）
        for (int x = centerX - halfSizeX; x <= centerX + halfSizeX; x++) {
            for (int z = centerZ - halfSizeZ; z <= centerZ + halfSizeZ; z++) {
                // 跳过中心空缺区域
                CrazyMinerConfigParser config = game.getCrazyMinerConfigParser();
                int halfClearSize = config.getCenterClearAreaSize() / 2;
                if (Math.abs(x - centerX) <= halfClearSize && Math.abs(z - centerZ) <= halfClearSize) {
                    continue;
                }
                
                // 检查是否为最外层边界（这些位置保持基岩不变）
                boolean isOuterBoundary = (x == centerX - halfSizeX || x == centerX + halfSizeX || 
                                          z == centerZ - halfSizeZ || z == centerZ + halfSizeZ);
                
                // 检查是否在内圈范围内
                boolean isInnerRing = (x >= innerMinX && x <= innerMaxX && z >= innerMinZ && z <= innerMaxZ);
                
                // 跳过底层基岩，替换上面21层
                for (int currentY = y + 1; currentY < y + height; currentY++) {
                    // 如果是最外层边界或顶层，保持基岩不变
                    if (isOuterBoundary || currentY == y + height - 1) {
                        continue;
                    }
                    totalBlocks++;
                    // 只替换基岩方块，不影响空气
                    Material currentBlock = world.getBlockAt(x, currentY, z).getType();
                    if (currentBlock == Material.BEDROCK) {
                        // 选择使用内圈或外圈的方块配置
                        Map<Material, Double> blockConfig = isInnerRing ? innerRingBlocks : outerRingBlocks;
                        
                        if (blockConfig != null && !blockConfig.isEmpty()) {
                            // 随机选择方块类型
                            Material selectedBlock = selectRandomBlock(blockConfig, random);
                            if (selectedBlock != null) {
                                world.getBlockAt(x, currentY, z).setType(selectedBlock);
                                replacedBlocks++;
                            }
                        }
                    }
                }
            }
        }
        
        plugin.getLogger().info("方块替换完成！检查了 " + totalBlocks + " 个位置，替换了 " + replacedBlocks + " 个基岩方块");
        
        // 清理所有掉落物
        clearAllDroppedItems(world);
    }
    
    /**
     * 清理世界中的所有掉落物
     */
    private static void clearAllDroppedItems(World world) {
        int clearedItems = 0;
        
        // 清理所有掉落物实体
        for (org.bukkit.entity.Entity entity : world.getEntities()) {
            if (entity instanceof org.bukkit.entity.Item) {
                entity.remove();
                clearedItems++;
            }
        }
        
        plugin.getLogger().info("清理了 " + clearedItems + " 个掉落物");
    }
    
    /**
     * 基于概率选择随机方块
     */
    private static Material selectRandomBlock(Map<Material, Double> blockConfig, Random random) {
        if (blockConfig.isEmpty()) return null;
        
        double totalWeight = blockConfig.values().stream().mapToDouble(Double::doubleValue).sum();
        double randomValue = random.nextDouble() * totalWeight;
        double currentWeight = 0.0;
        
        for (Map.Entry<Material, Double> entry : blockConfig.entrySet()) {
            currentWeight += entry.getValue();
            if (randomValue <= currentWeight) {
                return entry.getKey();
            }
        }
        
        // 如果没有选中任何方块，返回第一个
        return blockConfig.keySet().iterator().next();
    }
    
    /**
     * 安排世界边界收缩
     */
    public static void scheduleWorldBorderShrinking(CrazyMiner game) {
        WorldBorder border = Bukkit.getWorld(game.getWorldName()).getWorldBorder();
        CrazyMinerConfigParser config = game.getCrazyMinerConfigParser();
        
        // 第一次缩圈：使用配置参数
        game.setDelayedTask(config.getWorldBorderShrink1Time(), () -> {
            double newSize = config.getInitialWorldBorderSize() - config.getWorldBorderShrink1Amount();
            
            // 调试信息：输出边界收缩信息
            plugin.getLogger().info("=== 第一次边界收缩 ===");
            plugin.getLogger().info("收缩中心: (" + border.getCenter().getX() + ", " + border.getCenter().getZ() + ")");
            plugin.getLogger().info("收缩前大小: " + border.getSize());
            plugin.getLogger().info("收缩后大小: " + newSize);
            plugin.getLogger().info("收缩持续时间: " + (config.getWorldBorderShrink1Duration() / 20L) + " 秒");
            plugin.getLogger().info("====================");
            
            border.setSize(newSize, config.getWorldBorderShrink1Duration() / 20L); // ticks转秒
            MCEMessenger.sendGlobalInfo("<red><bold>世界边界开始收缩！</bold></red>");
            
            // 使用新版Component API发送title
            MiniMessage mm = MiniMessage.miniMessage();
            Component titleComponent = mm.deserialize("<red><bold>世界边界收缩！</bold></red>");
            Component subtitleComponent = mm.deserialize("<yellow>快速向中心移动！</yellow>");
            
            Title title = Title.title(
                titleComponent,
                subtitleComponent,
                Title.Times.times(
                    java.time.Duration.ofMillis(500),  // fadeIn: 10 ticks = 500ms
                    java.time.Duration.ofSeconds(3),   // stay: 60 ticks = 3s
                    java.time.Duration.ofSeconds(1)    // fadeOut: 20 ticks = 1s
                )
            );
            
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.showTitle(title);
            }
        });
        
        // 第二次缩圈：使用配置参数
        game.setDelayedTask(config.getWorldBorderShrink2Time(), () -> {
            double firstShrinkSize = config.getInitialWorldBorderSize() - config.getWorldBorderShrink1Amount();
            double newSize = firstShrinkSize - config.getWorldBorderShrink2Amount();
            
            // 调试信息：输出边界收缩信息
            plugin.getLogger().info("=== 第二次边界收缩 ===");
            plugin.getLogger().info("收缩中心: (" + border.getCenter().getX() + ", " + border.getCenter().getZ() + ")");
            plugin.getLogger().info("收缩前大小: " + border.getSize());
            plugin.getLogger().info("收缩后大小: " + newSize);
            plugin.getLogger().info("收缩持续时间: " + (config.getWorldBorderShrink2Duration() / 20L) + " 秒");
            plugin.getLogger().info("====================");
            
            border.setSize(newSize, config.getWorldBorderShrink2Duration() / 20L); // ticks转秒
            MCEMessenger.sendGlobalInfo("<red><bold>最终收缩！准备决战！</bold></red>");
            
            // 使用新版Component API发送title
            MiniMessage mm = MiniMessage.miniMessage();
            Component titleComponent = mm.deserialize("<dark_red><bold>最终收缩！</bold></dark_red>");
            Component subtitleComponent = mm.deserialize("<gold>准备决战！</gold>");
            
            Title title = Title.title(
                titleComponent,
                subtitleComponent,
                Title.Times.times(
                    java.time.Duration.ofMillis(500),  // fadeIn: 10 ticks = 500ms
                    java.time.Duration.ofSeconds(3),   // stay: 60 ticks = 3s
                    java.time.Duration.ofSeconds(1)    // fadeOut: 20 ticks = 1s
                )
            );
            
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.showTitle(title);
            }
        });
    }
    
    /**
     * 处理方块挖掘：只有矿石给熔炼物，其他方块给正常掉落
     */
    public static ItemStack getSmeltedDrop(Material originalBlock) {
        return switch (originalBlock) {
            // 只有矿石类方块给熔炼物
            case IRON_ORE -> new ItemStack(Material.IRON_INGOT);
            case GOLD_ORE -> new ItemStack(Material.GOLD_INGOT);
            case DIAMOND_ORE -> new ItemStack(Material.DIAMOND);
            case REDSTONE_ORE -> new ItemStack(Material.REDSTONE, 4 + (int)(Math.random() * 2));
            case LAPIS_ORE -> new ItemStack(Material.LAPIS_LAZULI, 4 + (int)(Math.random() * 5));
            case COAL_ORE -> new ItemStack(Material.COAL);
            case DEEPSLATE_IRON_ORE -> new ItemStack(Material.IRON_INGOT);
            case DEEPSLATE_GOLD_ORE -> new ItemStack(Material.GOLD_INGOT);
            case DEEPSLATE_DIAMOND_ORE -> new ItemStack(Material.DIAMOND);
            case ANCIENT_DEBRIS -> new ItemStack(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE);
            
            // 特殊方块的正常掉落物
            case STONE -> new ItemStack(Material.COBBLESTONE); // 石头挖出来是圆石
            case DEEPSLATE -> new ItemStack(Material.COBBLED_DEEPSLATE); // 深板岩挖出来是圆石深板岩
            case COBWEB -> new ItemStack(Material.STRING); // 蜘蛛网挖出来是线
            
            // 其他所有方块都给原始方块
            default -> new ItemStack(originalBlock);
        };
    }
    
    /**
     * 处理远古残骸挖掘：额外掉落下界合金升级模板
     */
    public static void handleAncientDebrisBreak(Player player, Location location) {
        ItemStack template = new ItemStack(Material.NETHERITE_UPGRADE_SMITHING_TEMPLATE);
        location.getWorld().dropItemNaturally(location, template);
    }
    
    /**
     * 重置游戏计分板
     */
    public static void resetGameBoard(CrazyMiner game) {
        CrazyMinerGameBoard gameBoard = (CrazyMinerGameBoard) game.getGameBoard();
        gameBoard.updatePlayerRemainTitle(Bukkit.getOnlinePlayers().size());
        gameBoard.setTeamRemainCount(game.getActiveTeams().size());
        
        for (int i = 0; i < game.getActiveTeams().size(); ++i) {
            gameBoard.getTeamRemain()[i] = 0;
        }
        
        for (Player player : Bukkit.getOnlinePlayers()) {
            Team team = MCETeamUtils.getTeam(player);
            if (team != null) {
                gameBoard.getTeamRemain()[game.getTeamId(team)]++;
            }
        }
        gameBoard.updateTeamRemainTitle(null);
    }
    
    /**
     * 更新游戏计分板 - 当玩家死亡时调用
     */
    public static void updateGameBoardOnPlayerDeath(CrazyMiner game, Player deadPlayer) {
        CrazyMinerGameBoard gameBoard = (CrazyMinerGameBoard) game.getGameBoard();
        Team playerTeam = MCETeamUtils.getTeam(deadPlayer);
        
        // 更新剩余玩家数（死亡的玩家还没有被设置为观察者模式，所以要手动减1）
        int alivePlayerCount = 0;
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getGameMode() == GameMode.ADVENTURE && player.getScoreboardTags().contains("Active")) {
                alivePlayerCount++;
            }
        }
        // 减去当前死亡的玩家
        if (deadPlayer.getGameMode() == GameMode.ADVENTURE) {
            alivePlayerCount--;
        }
        gameBoard.updatePlayerRemainTitle(alivePlayerCount);
        
        // 更新队伍统计
        if (playerTeam != null) {
            int teamId = game.getTeamId(playerTeam);
            boolean wasTeamAliveBeforeUpdate = false;
            
            // 检查队伍死亡前是否还有存活成员
            if (teamId >= 0 && teamId < gameBoard.getTeamRemain().length) {
                wasTeamAliveBeforeUpdate = gameBoard.getTeamRemain()[teamId] > 0;
            }
            
            // 更新队伍统计
            gameBoard.updateTeamRemainTitle(playerTeam);
            
            // 检查队伍是否刚刚团灭（从有人存活变为无人存活）
            boolean isTeamEliminatedNow = (teamId >= 0 && teamId < gameBoard.getTeamRemain().length && 
                                         gameBoard.getTeamRemain()[teamId] == 0);
            
            if (wasTeamAliveBeforeUpdate && isTeamEliminatedNow) {
                // 记录队伍死亡顺序（确保不重复记录）
                if (!game.getTeamDeathOrder().contains(playerTeam)) {
                    game.getTeamDeathOrder().add(playerTeam);
                    
                    MCEMessenger.sendGlobalInfo(MCEPlayerUtils.getColoredPlayerName(deadPlayer) + 
                        " 所在的" + playerTeam.getName() + "<gray>已被团灭！</gray>");
                }
            }
        }
        
        // 刷新展示板显示
        gameBoard.globalDisplay();
        
        // 检查游戏是否结束 - 使用统计后的队伍数量
        int aliveTeamCount = gameBoard.getTeamRemainCount();
        if (aliveTeamCount <= 1) {
            game.getTimeline().nextState();
        }
    }
    
    
    /**
     * 清理游戏任务
     */
    public static void clearGameTasks(CrazyMiner game) {
        for (BukkitRunnable task : game.getGameTasks()) {
            task.cancel();
        }
        game.getGameTasks().clear();
    }
    
    /**
     * 发送获胜消息和队伍排名
     */
    public static void sendWinningMessage(CrazyMiner game) {
        // 显示获胜队伍
        displayWinningTeam(game);
        
        // 显示完整的队伍排名
        displayTeamRankings(game);
    }
    
    /**
     * 显示获胜队伍
     */
    private static void displayWinningTeam(CrazyMiner game) {
        List<Team> winningTeams = getAliveTeams(game);
        
        if (!winningTeams.isEmpty()) {
            Team winningTeam = winningTeams.getFirst();
            List<Player> winningPlayers = getPlayersInTeam(winningTeam);
            
            StringBuilder message = new StringBuilder();
            message.append(winningTeam.getName());
            message.append("<gold><bold> 获得胜利！</bold></gold> 获胜成员：");
            
            for (int i = 0; i < winningPlayers.size(); i++) {
                if (i > 0) message.append("<aqua>, </aqua>");
                message.append(MCEPlayerUtils.getColoredPlayerName(winningPlayers.get(i)));
            }
            
            MCEMessenger.sendGlobalInfo(message.toString());
        } else {
            MCEMessenger.sendGlobalInfo("<red><bold>没有队伍存活，游戏结束！</bold></red>");
        }
    }
    
    /**
     * 显示队伍排名（死亡顺序）
     */
    private static void displayTeamRankings(CrazyMiner game) {
        List<Team> deathOrder = game.getTeamDeathOrder();
        List<Team> aliveTeams = getAliveTeams(game);
        
        // 构建完整排名：存活队伍 + 死亡队伍（逆序）
        List<Team> rankings = new ArrayList<>();
        rankings.addAll(aliveTeams); // 存活的队伍排在前面
        
        // 死亡队伍按死亡顺序逆序添加（最后死的排在前面）
        for (int i = deathOrder.size() - 1; i >= 0; i--) {
            rankings.add(deathOrder.get(i));
        }
        
        MCEMessenger.sendGlobalInfo("<yellow><bold>=== 队伍排名 ===</bold></yellow>");
        
        for (int i = 0; i < rankings.size(); i++) {
            Team team = rankings.get(i);
            String rank = getRankString(i + 1);
            String teamStatus = aliveTeams.contains(team) ? "<green>存活</green>" : "<red>淘汰</red>";
            
            MCEMessenger.sendGlobalInfo(rank + " " + team.getName() +
                " <gray>(" + teamStatus + ")</gray>");
        }
    }
    
    /**
     * 获取存活的队伍列表
     */
    private static List<Team> getAliveTeams(CrazyMiner game) {
        List<Team> aliveTeams = new ArrayList<>();
        CrazyMinerGameBoard gameBoard = (CrazyMinerGameBoard) game.getGameBoard();
        int[] teamRemain = gameBoard.getTeamRemain();
        List<Team> activeTeams = game.getActiveTeams();
        
        // 确保不会越界
        int maxIndex = Math.min(teamRemain.length, activeTeams.size());
        
        for (int i = 0; i < maxIndex; i++) {
            if (teamRemain[i] > 0) {
                aliveTeams.add(activeTeams.get(i));
            }
        }
        
        return aliveTeams;
    }
    
    /**
     * 获取指定队伍的所有玩家
     */
    private static List<Player> getPlayersInTeam(Team team) {
        List<Player> teamPlayers = new ArrayList<>();
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (MCETeamUtils.getTeam(player) == team) {
                teamPlayers.add(player);
            }
        }
        return teamPlayers;
    }
    
    /**
     * 获取排名字符串
     */
    private static String getRankString(int rank) {
        return switch (rank) {
            case 1 -> "<gold><bold>🥇 第1名</bold></gold>";
            case 2 -> "<white><bold>🥈 第2名</bold></white>";
            case 3 -> "<#8B4513><bold>🥉 第3名</bold></#8B4513>";
            default -> "<gray><bold>" + rank + "名</bold></gray>";
        };
    }
}