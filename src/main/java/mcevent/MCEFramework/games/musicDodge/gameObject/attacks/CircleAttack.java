package mcevent.MCEFramework.games.musicDodge.gameObject.attacks;

import mcevent.MCEFramework.games.musicDodge.ParticleOptimizationIntegration;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Particle.DustOptions;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.plugin.java.JavaPlugin;

public class CircleAttack extends MCEAttack {
    private final int radius;
    private final Location location;
    private final Plugin plugin;
    private mcevent.MCEFramework.games.musicDodge.AttackDataManager.TrackableCircleAttack trackableAttack;

    public CircleAttack(double alertDurationBeats, double attackDurationBeats, Location location, int radius, int bpm, Plugin plugin) {
        super(alertDurationBeats, attackDurationBeats, bpm);
        this.location = location;
        this.radius = radius;
        this.plugin = plugin;
        
    }
    
    /**
     * 设置TrackableAttack（用于粒子优化）
     */
    public void setTrackableAttack(mcevent.MCEFramework.games.musicDodge.AttackDataManager.TrackableCircleAttack trackableAttack) {
        this.trackableAttack = trackableAttack;
    }

    @Override
    public void toggle() {
        // 如果有TrackableAttack，立即注册并启动
        if (trackableAttack != null) {
            mcevent.MCEFramework.games.musicDodge.AttackDataManager.getInstance(plugin).registerAttack(trackableAttack);
            trackableAttack.start();
        }
        
        // Phase 1: Gray smoke particle circle for alert duration
        new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = getAlertDurationTicks();
            
            @Override
            public void run() {
                if (ticks >= maxTicks) {
                    this.cancel();
                    startAttackPhase();
                    return;
                }
                
                spawnCircleParticles();
                ticks += 1; // 修正：每次只增加1个tick，保持时间同步
            }
        }.runTaskTimer(plugin, 0, 1); // 改为每tick执行，确保时机准确
    }
    
    private void startAttackPhase() {
        // Play sound when switching from alert to attack phase
        playAttackSound("block.note_block.pling");
        
        // Phase 2: Red smoke particle cylinder for attack duration
        new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = getAttackDurationTicks();
            
            @Override
            public void run() {
                if (ticks >= maxTicks) {
                    this.cancel();
                    return;
                }
                
                spawnCylinderParticles();
                // 延迟1tick开始伤害检查，与视觉效果同步
                if (ticks > 0) {
                    checkPlayerDamage(location, 2.0);
                }
                ticks += 1; // 保持每tick执行，确保伤害检查的准确性
            }
        }.runTaskTimer(plugin, 0, 1); // 攻击阶段保持每tick执行，确保伤害检查准确
    }
    
    private void spawnCircleParticles() {
        // 检查是否启用了粒子优化系统，如果是则跳过原始粒子渲染
        if (mcevent.MCEFramework.games.musicDodge.ParticleOptimizationIntegration.isOptimizationEnabled()) {
            return;
        }
        
        DustOptions grayDust = new DustOptions(Color.GRAY, 1.0f);
        
        // 覆盖整个圆形区域（包括内部）- 使用更密集的粒子渲染实心圆
        for (double r = 0; r <= radius; r += 0.3) { // 每0.3格一个圆环，更密集
            int points = r == 0 ? 1 : Math.max(6, (int) (r * 12)); // 增加点数密度，每单位半径12个点
            for (int i = 0; i < points; i++) {
                double angle = 2 * Math.PI * i / points;
                double x = location.getX() + r * Math.cos(angle);
                double z = location.getZ() + r * Math.sin(angle);
                Location particleLocation = new Location(location.getWorld(), x, location.getY(), z);
                
                // Spawn gray dust particles
                location.getWorld().spawnParticle(Particle.DUST, particleLocation, 1, grayDust);
            }
        }
    }
    
    private void spawnCylinderParticles() {
        // 检查是否启用了粒子优化系统，如果是则跳过原始粒子渲染
        if (mcevent.MCEFramework.games.musicDodge.ParticleOptimizationIntegration.isOptimizationEnabled()) {
            return;
        }
        
        int height = 2; // 2 blocks high
        DustOptions redDust = new DustOptions(Color.RED, 1.0f);
        
        // 覆盖整个圆形区域（包括内部）- 使用更密集的粒子渲染实心柱体
        for (double r = 0; r <= radius; r += 0.3) { // 每0.3格一个圆环，更密集
            int points = r == 0 ? 1 : Math.max(6, (int) (r * 12)); // 增加点数密度，每单位半径12个点
            for (int i = 0; i < points; i++) {
                double angle = 2 * Math.PI * i / points;
                double x = location.getX() + r * Math.cos(angle);
                double z = location.getZ() + r * Math.sin(angle);
                
                // Create vertical line of particles (cylinder)
                for (int y = 0; y < height; y++) {
                    Location particleLocation = new Location(location.getWorld(), x, location.getY() + y, z);
                    // Spawn red dust particles
                    location.getWorld().spawnParticle(Particle.DUST, particleLocation, 1, redDust);
                }
            }
        }
    }
    
    @Override
    protected boolean isPlayerInAttackRange(Player player) {
        double distance = player.getLocation().distance(location);
        return distance <= radius;
    }
}
