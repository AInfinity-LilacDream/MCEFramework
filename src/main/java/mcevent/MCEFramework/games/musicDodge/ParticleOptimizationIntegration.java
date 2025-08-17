package mcevent.MCEFramework.games.musicDodge;

import mcevent.MCEFramework.games.musicDodge.gameObject.attacks.*;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;

/**
 * 粒子优化系统集成示例
 * 这个类展示了如何将现有的攻击系统与新的粒子优化系统集成
 */
public class ParticleOptimizationIntegration {
    
    private final AttackDataManager attackDataManager;
    private final Plugin plugin;
    private static boolean optimizationEnabled = false;
    
    public ParticleOptimizationIntegration(Plugin plugin) {
        this.plugin = plugin;
        this.attackDataManager = AttackDataManager.getInstance(plugin);
    }
    
    /**
     * 为现有SpinAttack创建优化包装器
     */
    public AttackDataManager.TrackableSpinAttack wrapSpinAttack(SpinAttack spinAttack, Location center, int rayCount, 
                             double rotationSpeed, double alertBeats, double attackBeats, int bpm) {
        
        // 创建TrackableSpinAttack包装器
        AttackDataManager.TrackableSpinAttack trackableAttack = 
            new AttackDataManager.TrackableSpinAttack(center, rayCount, 
                                                    rotationSpeed, 100, alertBeats, attackBeats, bpm);
        
        // 不在这里注册，返回包装器以便稍后注册
        return trackableAttack;
    }
    
    /**
     * 创建独立的优化SpinAttack（仅用于粒子显示）
     */
    public void createOptimizedSpinAttack(Location center, int rayCount, double rotationSpeed, 
                                        double alertBeats, double attackBeats, int bpm) {
        
        // 直接创建TrackableSpinAttack包装器（用于粒子显示）
        AttackDataManager.TrackableSpinAttack trackableAttack = 
            new AttackDataManager.TrackableSpinAttack(center, rayCount, 
                                                    rotationSpeed, 100, alertBeats, attackBeats, bpm);
        
        // 注册到攻击数据管理器
        attackDataManager.registerAttack(trackableAttack);
        
    }
    
    /**
     * 为现有LaserAttack创建优化包装器
     */
    public AttackDataManager.TrackableLaserAttack wrapLaserAttack(LaserAttack laserAttack, Location start, Location end,
                              double alertBeats, double attackBeats, int bpm) {
        
        // 创建TrackableLaserAttack包装器
        AttackDataManager.TrackableLaserAttack trackableAttack = 
            new AttackDataManager.TrackableLaserAttack(start, end, alertBeats, attackBeats, bpm);
        
        // 不在这里注册，返回包装器以便稍后注册
        return trackableAttack;
    }
    
    /**
     * 创建独立的优化LaserAttack（仅用于粒子显示）
     */
    public void createLaserAttack(Location start, Location end, 
                                double alertBeats, double attackBeats, int bpm) {
        
        // 直接创建TrackableLaserAttack包装器（用于粒子显示）
        AttackDataManager.TrackableLaserAttack trackableAttack = 
            new AttackDataManager.TrackableLaserAttack(start, end, alertBeats, attackBeats, bpm);
        
        // 注册到攻击数据管理器
        attackDataManager.registerAttack(trackableAttack);
        
    }
    
    /**
     * 为现有CircleAttack创建优化包装器
     */
    public AttackDataManager.TrackableCircleAttack wrapCircleAttack(CircleAttack circleAttack, Location center, double radius,
                               double alertBeats, double attackBeats, int bpm) {
        
        // 创建TrackableCircleAttack包装器
        AttackDataManager.TrackableCircleAttack trackableAttack = 
            new AttackDataManager.TrackableCircleAttack(center, radius, alertBeats, attackBeats, bpm);
        
        // 不在这里注册，返回包装器以便稍后注册
        return trackableAttack;
    }
    
    /**
     * 为现有WallAttack创建优化包装器
     */
    public AttackDataManager.TrackableWallAttack wrapWallAttack(WallAttack wallAttack, String direction,
                             double alertBeats, double attackBeats, int bpm) {
        
        // 创建TrackableWallAttack包装器
        AttackDataManager.TrackableWallAttack trackableAttack = 
            new AttackDataManager.TrackableWallAttack(direction, alertBeats, attackBeats, bpm);
        
        // 不在这里注册，返回包装器以便稍后注册
        return trackableAttack;
    }
    
    /**
     * 为现有SquareRingAttack创建优化包装器
     */
    public AttackDataManager.TrackableSquareRingAttack wrapSquareRingAttack(SquareRingAttack squareRingAttack, Location center, int innerRadius, 
                                   int outerRadius, double alertBeats, double attackBeats, int bpm) {
        
        // 创建TrackableSquareRingAttack包装器
        AttackDataManager.TrackableSquareRingAttack trackableAttack = 
            new AttackDataManager.TrackableSquareRingAttack(center, innerRadius, outerRadius, 
                                                          alertBeats, attackBeats, bpm);
        
        // 不在这里注册，返回包装器以便稍后注册
        return trackableAttack;
    }
    
    /**
     * 创建独立的优化CircleAttack（仅用于粒子显示）
     */
    public void createCircleAttack(Location center, double radius,
                                 double alertBeats, double attackBeats, int bpm) {
        
        // 创建TrackableCircleAttack包装器（用于粒子显示）
        AttackDataManager.TrackableCircleAttack trackableAttack = 
            new AttackDataManager.TrackableCircleAttack(center, radius, alertBeats, attackBeats, bpm);
        
        // 注册到攻击数据管理器
        attackDataManager.registerAttack(trackableAttack);
        
    }
    
    /**
     * 处理复合攻击：SideAttack
     * SideAttack由多个SquareRingAttack组成，需要拆解
     */
    public void createSideAttack(String worldName, int ringThickness, 
                               double ringAlertMeasures, double ringAttackMeasures, 
                               double totalAttackMeasures, int bpm) {
        
        // 创建AttackFactory来创建SideAttack
        AttackFactory factory = new AttackFactory(plugin, true);
        SideAttack originalAttack = factory.createSideAttack(totalAttackMeasures, worldName, 
                                                           ringThickness, ringAlertMeasures, 
                                                           ringAttackMeasures, bpm);
        
        // 计算场地中心位置
        Location center = calculateFieldCenter(worldName);
        
        // 拆解为多个SquareRingAttack
        int maxRadius = 21; // 从SideAttack类中复制的常量
        int totalRings = maxRadius / ringThickness;
        double ringGenerationInterval = totalAttackMeasures / totalRings;
        
        for (int i = 0; i < totalRings; i++) {
            int outerRadius = maxRadius - (i * ringThickness);
            int innerRadius = Math.max(0, outerRadius - ringThickness);
            
            if (outerRadius <= 0) continue;
            
            // 计算延迟启动时间
            double delayMeasures = i * ringGenerationInterval;
            
            // 创建TrackableSquareRingAttack
            AttackDataManager.TrackableSquareRingAttack trackableRing = 
                new AttackDataManager.TrackableSquareRingAttack(center, innerRadius, outerRadius,
                                                              ringAlertMeasures, ringAttackMeasures,bpm);
            
            // 延迟注册到攻击数据管理器
            scheduleDelayedRegistration(trackableRing, delayMeasures,bpm);
        }
        
        // 启动原始攻击
        originalAttack.toggle();
    }
    
    /**
     * 处理复合攻击：BarAttack
     * BarAttack由多个LaserAttack组成，需要拆解
     */
    public void createBarAttack(String direction, int spacing, String worldName, 
                              double alertBeats, double attackBeats, int bpm) {
        
        // 创建AttackFactory来创建BarAttack
        AttackFactory factory = new AttackFactory(plugin, true);
        BarAttack originalAttack = factory.createBarAttack(alertBeats, attackBeats, direction, 
                                                         spacing, org.bukkit.Bukkit.getWorld(worldName), bpm);
        
        // 拆解为多个LaserAttack
        Location fieldCenter = calculateFieldCenter(worldName);
        int fieldSize = 43;
        
        if (direction.equals("x")) {
            // 从左到右：生成多条从上到下的激光
            for (int x = 0; x < fieldSize; x += spacing) {
                Location start = fieldCenter.clone().add(x - (double) fieldSize /2, 0, (double) -fieldSize /2);
                Location end = fieldCenter.clone().add(x - (double) fieldSize /2, 0, (double) fieldSize /2);
                
                AttackDataManager.TrackableLaserAttack trackableLaser = 
                    new AttackDataManager.TrackableLaserAttack(start, end, alertBeats, attackBeats, bpm);
                
                attackDataManager.registerAttack(trackableLaser);
            }
        } else if (direction.equals("y")) {
            // 从上到下：生成多条从左到右的激光
            for (int y = 0; y < fieldSize; y += spacing) {
                Location start = fieldCenter.clone().add((double) -fieldSize /2, 0, y - (double) fieldSize /2);
                Location end = fieldCenter.clone().add((double) fieldSize /2, 0, y - (double) fieldSize /2);
                
                AttackDataManager.TrackableLaserAttack trackableLaser = 
                    new AttackDataManager.TrackableLaserAttack(start, end, alertBeats, attackBeats, bpm);
                
                attackDataManager.registerAttack(trackableLaser);
            }
        }
        
        // 启动原始攻击
        originalAttack.toggle();
    }
    
    /**
     * 计算场地中心位置
     */
    private Location calculateFieldCenter(String worldName) {
        double centerX = -7.0 + (43 - 1) / 2.0;
        double centerZ = -46.0 + (43 - 1) / 2.0;
        return new Location(org.bukkit.Bukkit.getWorld(worldName), centerX, -60.0, centerZ);
    }
    
    /**
     * 延迟注册攻击到管理器
     */
    private void scheduleDelayedRegistration(AttackDataManager.TrackableAttack attack, 
                                           double delayMeasures, int bpm) {
        // 计算延迟时间（tick）
        long delayTicks = (long)(delayMeasures * 4 * 60.0 /bpm * 20);

        Bukkit.getScheduler().runTaskLater(plugin, () -> attackDataManager.registerAttack(attack), delayTicks);
    }
    
    /**
     * 使用示例
     */
    public static void example(Plugin plugin) {
        ParticleOptimizationIntegration integration = new ParticleOptimizationIntegration(plugin);
        
        // 获取攻击数据管理器和拦截器
        AttackDataManager manager = AttackDataManager.getInstance(plugin);
        DustParticleInterceptor interceptor = DustParticleInterceptor.create(plugin);
        
        // 启用粒子优化系统
        interceptor.enable();
        manager.start();
        
        // 创建各种攻击
        Location center = new Location(org.bukkit.Bukkit.getWorld("musicdodge_classic"), 14, -60, -25);
        
        // 创建旋转攻击
        integration.createOptimizedSpinAttack(center, 5, 2.0, 0.5, 4.0, 100);
        
        // 创建直线攻击
        Location start = center.clone().add(-10, 0, -10);
        Location end = center.clone().add(10, 0, 10);
        integration.createLaserAttack(start, end, 0.5, 2.0, 100);
        
        // 创建复合攻击
        integration.createSideAttack("musicdodge_classic", 2, 0.5, 0.5, 6.0, 100);
        integration.createBarAttack("x", 6, "musicdodge_classic", 1.0, 1.0, 100);
        
        // 游戏结束后清理
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            interceptor.disable();
            manager.stop();
        }, 20 * 60); // 60秒后停止
    }
    
    /**
     * 设置优化启用状态
     */
    public static void setOptimizationEnabled(boolean enabled) {
        optimizationEnabled = enabled;
    }
    
    /**
     * 检查优化是否启用
     */
    public static boolean isOptimizationEnabled() {
        return optimizationEnabled;
    }
}