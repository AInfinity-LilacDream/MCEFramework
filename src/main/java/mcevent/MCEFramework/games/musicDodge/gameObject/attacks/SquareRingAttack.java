package mcevent.MCEFramework.games.musicDodge.gameObject.attacks;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Particle.DustOptions;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class SquareRingAttack extends MCEAttack {
    private final int innerRadius;
    private final int outerRadius;
    private final Location location;
    private final Plugin plugin;
    private mcevent.MCEFramework.games.musicDodge.AttackDataManager.TrackableSquareRingAttack trackableAttack;

    public SquareRingAttack(double alertDurationBeats, double attackDurationBeats, Location location, int innerRadius, int outerRadius, int bpm, Plugin plugin) {
        super(alertDurationBeats, attackDurationBeats, bpm);
        this.location = location;
        this.innerRadius = innerRadius;
        this.outerRadius = outerRadius;
        this.plugin = plugin;
    }
    
    /**
     * 设置TrackableAttack（用于粒子优化）
     */
    public void setTrackableAttack(mcevent.MCEFramework.games.musicDodge.AttackDataManager.TrackableSquareRingAttack trackableAttack) {
        this.trackableAttack = trackableAttack;
    }

    @Override
    public void toggle() {
        // 如果有TrackableAttack，立即注册并启动
        if (trackableAttack != null) {
            mcevent.MCEFramework.games.musicDodge.AttackDataManager.getInstance(plugin).registerAttack(trackableAttack);
            trackableAttack.start();
        }
        
        // Phase 1: Gray square ring for alert duration
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
                
                spawnSquareRingParticles();
                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }
    
    private void startAttackPhase() {
        // Play sound when switching from alert to attack phase
        playAttackSound("entity.wither.shoot");
        
        // Phase 2: Red square ring cylinder for attack duration
        new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = getAttackDurationTicks();
            
            @Override
            public void run() {
                if (ticks >= maxTicks) {
                    this.cancel();
                    return;
                }
                
                spawnSquareRingCylinderParticles();
                // 延迟1tick开始伤害检查，与视觉效果同步
                if (ticks > 0) {
                    checkPlayerDamage(location, 2.0);
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }
    
    private void spawnSquareRingParticles() {
        DustOptions grayDust = new DustOptions(Color.GRAY, 1.0f);
        spawnSquareRingAtHeight(location.getY(), grayDust);
    }
    
    private void spawnSquareRingCylinderParticles() {
        int height = 2; // 2 blocks high
        DustOptions redDust = new DustOptions(Color.RED, 1.0f);
        
        // Create vertical square ring (cylinder)
        for (int y = 0; y < height; y++) {
            spawnSquareRingAtHeight(location.getY() + y, redDust);
        }
    }
    
    private void spawnSquareRingAtHeight(double y, DustOptions dustOptions) {
        // 检查是否启用了粒子优化系统，如果是则跳过原始粒子渲染
        if (mcevent.MCEFramework.games.musicDodge.ParticleOptimizationIntegration.isOptimizationEnabled()) {
            return;
        }
        
        // Draw outer square
        drawSquare(outerRadius, y, dustOptions);
        
        // Draw inner square (if inner radius > 0)
        if (innerRadius > 0) {
            drawSquare(innerRadius, y, dustOptions);
        }
    }
    
    private void drawSquare(int radius, double y, DustOptions dustOptions) {
        // Draw four sides of the square
        for (int i = -radius; i <= radius; i++) {
            // Top side (z = radius)
            Location topSide = new Location(location.getWorld(), 
                location.getX() + i, y, location.getZ() + radius);
            location.getWorld().spawnParticle(Particle.DUST, topSide, 1, dustOptions);
            
            // Bottom side (z = -radius)
            Location bottomSide = new Location(location.getWorld(), 
                location.getX() + i, y, location.getZ() - radius);
            location.getWorld().spawnParticle(Particle.DUST, bottomSide, 1, dustOptions);
            
            // Left side (x = -radius)
            Location leftSide = new Location(location.getWorld(), 
                location.getX() - radius, y, location.getZ() + i);
            location.getWorld().spawnParticle(Particle.DUST, leftSide, 1, dustOptions);
            
            // Right side (x = radius)
            Location rightSide = new Location(location.getWorld(), 
                location.getX() + radius, y, location.getZ() + i);
            location.getWorld().spawnParticle(Particle.DUST, rightSide, 1, dustOptions);
        }
    }
    
    @Override
    protected boolean isPlayerInAttackRange(Player player) {
        double dx = Math.abs(player.getLocation().getX() - location.getX());
        double dz = Math.abs(player.getLocation().getZ() - location.getZ());
        
        // Check if player is within outer square but outside inner square
        boolean inOuterSquare = dx <= outerRadius && dz <= outerRadius;
        boolean outsideInnerSquare = dx > innerRadius || dz > innerRadius;
        
        return inOuterSquare && outsideInnerSquare;
    }
}