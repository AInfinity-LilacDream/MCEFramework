package mcevent.MCEFramework.games.musicDodge;

import mcevent.MCEFramework.games.musicDodge.gameObject.attacks.*;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;

/**
 * 攻击工厂类 - 负责创建攻击并自动注册到优化系统
 * 解决无限递归问题，通过工厂模式统一管理攻击创建和优化注册
 */
public class AttackFactory {
    
    private final Plugin plugin;
    private final ParticleOptimizationIntegration integration;
    private final boolean optimizationEnabled;
    
    public AttackFactory(Plugin plugin, boolean optimizationEnabled) {
        this.plugin = plugin;
        this.optimizationEnabled = optimizationEnabled;
        this.integration = optimizationEnabled ? new ParticleOptimizationIntegration(plugin) : null;
    }
    
    /**
     * 创建SpinAttack，如果启用优化则自动注册
     */
    public SpinAttack createSpinAttack(double alertDurationBeats, double attackDurationBeats, 
                                     Location location, int rayCount, double rotationSpeed, int bpm) {
        
        
        // 创建原始攻击（不会触发递归，因为构造函数不再调用优化系统）
        SpinAttack attack = new SpinAttack(alertDurationBeats, attackDurationBeats, location, 
                                         rayCount, rotationSpeed, bpm, plugin);
        
        // 如果优化启用，创建优化包装器但不立即注册
        if (optimizationEnabled && integration != null) {
            AttackDataManager.TrackableSpinAttack trackableAttack = 
                integration.wrapSpinAttack(attack, location, rayCount, rotationSpeed, 
                                         alertDurationBeats, attackDurationBeats, bpm);
            // 将TrackableAttack存储在原始攻击中，稍后启动时注册
            attack.setTrackableAttack(trackableAttack);
        }
        // 不在这里调用toggle，由调度器在正确时间调用
        
        return attack;
    }
    
    /**
     * 创建LaserAttack，如果启用优化则自动注册
     */
    public LaserAttack createLaserAttack(double alertDurationBeats, double attackDurationBeats, 
                                       int startX, int startY, int endX, int endY, 
                                       org.bukkit.World world, int bpm) {
        
        
        // 创建原始攻击
        LaserAttack attack = new LaserAttack(alertDurationBeats, attackDurationBeats, 
                                           startX, startY, endX, endY, world, bpm, plugin);
        
        // 如果优化启用，创建优化包装器但不立即注册
        if (optimizationEnabled && integration != null) {
            // 从LaserAttack类中复制坐标转换逻辑
            double WORLD_LEFT_BOTTOM_X = -7.0;
            double WORLD_LEFT_BOTTOM_Z = -46.0;
            double WORLD_Y = -60.0;
            
            Location startLoc = new Location(world, WORLD_LEFT_BOTTOM_X + startX, WORLD_Y, WORLD_LEFT_BOTTOM_Z + startY);
            Location endLoc = new Location(world, WORLD_LEFT_BOTTOM_X + endX, WORLD_Y, WORLD_LEFT_BOTTOM_Z + endY);
            
            AttackDataManager.TrackableLaserAttack trackableAttack = 
                integration.wrapLaserAttack(attack, startLoc, endLoc, alertDurationBeats, attackDurationBeats, bpm);
            // 将TrackableAttack存储在原始攻击中，稍后启动时注册
            attack.setTrackableAttack(trackableAttack);
        }
        // 不在这里调用toggle，由调度器在正确时间调用
        
        return attack;
    }
    
    /**
     * 创建CircleAttack，如果启用优化则自动注册
     */
    public CircleAttack createCircleAttack(double alertDurationBeats, double attackDurationBeats, 
                                         Location location, int radius, int bpm) {
        
        
        // 创建原始攻击
        CircleAttack attack = new CircleAttack(alertDurationBeats, attackDurationBeats, location, radius, bpm, plugin);
        
        // 如果优化启用，创建优化包装器但不立即注册
        if (optimizationEnabled && integration != null) {
            AttackDataManager.TrackableCircleAttack trackableAttack = 
                integration.wrapCircleAttack(attack, location, (double) radius, alertDurationBeats, attackDurationBeats, bpm);
            // 将TrackableAttack存储在原始攻击中，稍后启动时注册
            attack.setTrackableAttack(trackableAttack);
        }
        // 不在这里调用toggle，由调度器在正确时间调用
        
        return attack;
    }
    
    /**
     * 创建SquareRingAttack，如果启用优化则自动注册
     */
    public SquareRingAttack createSquareRingAttack(double alertDurationBeats, double attackDurationBeats,
                                                 Location center, int innerRadius, int outerRadius, int bpm) {
        
        
        // 创建原始攻击
        SquareRingAttack attack = new SquareRingAttack(alertDurationBeats, attackDurationBeats, center, 
                                                     innerRadius, outerRadius, bpm, plugin);
        
        // 如果优化启用，创建优化包装器但不立即注册
        if (optimizationEnabled && integration != null) {
            AttackDataManager.TrackableSquareRingAttack trackableAttack = 
                integration.wrapSquareRingAttack(attack, center, innerRadius, outerRadius, 
                                               alertDurationBeats, attackDurationBeats, bpm);
            // 将TrackableAttack存储在原始攻击中，稍后启动时注册
            attack.setTrackableAttack(trackableAttack);
        }
        // 不在这里调用toggle，由调度器在正确时间调用
        
        return attack;
    }
    
    /**
     * 创建WallAttack，如果启用优化则自动注册
     */
    public WallAttack createWallAttack(double alertDurationBeats, double attackDurationBeats,
                                     String direction, org.bukkit.World world, int bpm) {
        
        
        // 创建原始攻击
        WallAttack attack = new WallAttack(alertDurationBeats, attackDurationBeats, direction, world, bpm, plugin);
        
        // 如果优化启用，创建优化包装器但不立即注册
        if (optimizationEnabled && integration != null) {
            AttackDataManager.TrackableWallAttack trackableAttack = 
                integration.wrapWallAttack(attack, direction, alertDurationBeats, attackDurationBeats, bpm);
            // 将TrackableAttack存储在原始攻击中，稍后启动时注册
            attack.setTrackableAttack(trackableAttack);
        }
        // 不在这里调用toggle，由调度器在正确时间调用
        
        return attack;
    }
    
    /**
     * 创建SideAttack，如果启用优化则自动注册
     */
    public SideAttack createSideAttack(double attackDurationMeasures, String worldName, int ringThickness,
                                     double ringAlertMeasures, double ringAttackMeasures, int bpm) {
        
        
        // 创建原始攻击，传入AttackFactory以便创建子攻击
        SideAttack attack = new SideAttack(attackDurationMeasures, worldName, ringThickness,
                                         ringAlertMeasures, ringAttackMeasures, bpm, plugin, this);
        
        // SideAttack不需要单独的优化包装器，因为它的子攻击（SquareRingAttack）会被自动优化
        // 不在这里调用toggle，由调度器在正确时间调用
        
        return attack;
    }
    
    /**
     * 创建BarAttack，如果启用优化则自动注册
     */
    public BarAttack createBarAttack(double alertDurationMeasures, double attackDurationMeasures,
                                   String direction, int spacing, org.bukkit.World world, int bpm) {
        
        
        // 创建原始攻击，传入AttackFactory以便创建子攻击
        BarAttack attack = new BarAttack(alertDurationMeasures, attackDurationMeasures, direction, 
                                       spacing, world, bpm, plugin, this);
        
        // BarAttack不需要单独的优化包装器，因为它的子攻击（LaserAttack）会被自动优化
        // 不在这里调用toggle，由调度器在正确时间调用
        
        return attack;
    }
    
    /**
     * 创建RandomAttack，如果启用优化则自动注册
     */
    public RandomAttack createRandomAttack(double attackDurationMeasures, org.bukkit.World world, 
                                         int circleCount, double generationSpeedMeasures, 
                                         double circleRadius, double circleAlertMeasures, int bpm) {
        
        
        // 创建原始攻击
        RandomAttack attack = new RandomAttack(attackDurationMeasures, world, circleCount, 
                                             generationSpeedMeasures, circleRadius, circleAlertMeasures, bpm, plugin);
        
        // RandomAttack不需要单独的优化包装器，因为它的子攻击（CircleAttack）会被自动优化
        // 不在这里调用toggle，由调度器在正确时间调用
        
        return attack;
    }
    
    /**
     * 创建RandomLaserAttack，如果启用优化则自动注册
     */
    public RandomLaserAttack createRandomLaserAttack(double attackDurationMeasures, org.bukkit.World world,
                                                   int laserCount, double generationSpeedMeasures,
                                                   double laserAlertMeasures, int bpm) {
        
        
        // 创建原始攻击
        RandomLaserAttack attack = new RandomLaserAttack(attackDurationMeasures, world, laserCount,
                                                       generationSpeedMeasures, laserAlertMeasures, bpm, plugin);
        
        // RandomLaserAttack不需要单独的优化包装器，因为它的子攻击（LaserAttack）会被自动优化
        // 不在这里调用toggle，由调度器在正确时间调用
        
        return attack;
    }
    
    /**
     * 检查优化是否启用
     */
    public boolean isOptimizationEnabled() {
        return optimizationEnabled;
    }
}