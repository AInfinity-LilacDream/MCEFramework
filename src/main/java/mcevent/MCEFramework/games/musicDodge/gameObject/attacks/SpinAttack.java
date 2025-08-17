package mcevent.MCEFramework.games.musicDodge.gameObject.attacks;

import mcevent.MCEFramework.games.musicDodge.ParticleOptimizationIntegration;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Particle.DustOptions;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class SpinAttack extends MCEAttack {
    private final int rayCount;
    private final double rotationSpeed; // degrees per tick
    private final Location location;
    private final Plugin plugin;
    private final int maxDistance = 100;
    private double currentRotation = 0;
    private mcevent.MCEFramework.games.musicDodge.AttackDataManager.TrackableSpinAttack trackableAttack;

    public SpinAttack(double alertDurationBeats, double attackDurationBeats, Location location, int rayCount, double rotationSpeed, int bpm, Plugin plugin) {
        super(alertDurationBeats, attackDurationBeats, bpm);
        this.location = location;
        this.rayCount = rayCount;
        this.rotationSpeed = rotationSpeed;
        this.plugin = plugin;
        
    }
    
    /**
     * 设置TrackableAttack（用于粒子优化）
     */
    public void setTrackableAttack(mcevent.MCEFramework.games.musicDodge.AttackDataManager.TrackableSpinAttack trackableAttack) {
        this.trackableAttack = trackableAttack;
    }

    @Override
    public void toggle() {
        // 如果有TrackableAttack，立即注册并启动
        if (trackableAttack != null) {
            mcevent.MCEFramework.games.musicDodge.AttackDataManager.getInstance(plugin).registerAttack(trackableAttack);
            trackableAttack.start();
        }
        
        // Phase 1: Gray spinning rays for alert duration
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
                
                spawnSpinningRays(new DustOptions(Color.GRAY, 1.0f));
                updateRotation();
                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }
    
    private void startAttackPhase() {
        // Play sound when switching from alert to attack phase
        playAttackSound("block.note_block.pling");
        
        // Phase 2: Red spinning rays for attack duration
        new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = getAttackDurationTicks();
            
            @Override
            public void run() {
                if (ticks >= maxTicks) {
                    this.cancel();
                    return;
                }
                
                spawnSpinningRays(new DustOptions(Color.RED, 1.0f));
                // 延迟1tick开始伤害检查，与视觉效果同步
                if (ticks > 0) {
                    checkPlayerDamage(location, 2.0);
                }
                updateRotation();
                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }
    
    private void updateRotation() {
        currentRotation += rotationSpeed;
        if (currentRotation >= 360) {
            currentRotation -= 360;
        }
    }
    
    private void spawnSpinningRays(DustOptions dustOptions) {
        // 检查是否启用了粒子优化系统，如果是则跳过原始粒子渲染
        if (mcevent.MCEFramework.games.musicDodge.ParticleOptimizationIntegration.isOptimizationEnabled()) {
            return;
        }
        
        double angleStep = 360.0 / rayCount;
        
        for (int i = 0; i < rayCount; i++) {
            double angle = Math.toRadians(currentRotation + (i * angleStep));
            drawRay(angle, dustOptions);
        }
    }
    
    private void drawRay(double angle, DustOptions dustOptions) {
        Vector direction = new Vector(Math.cos(angle), 0, Math.sin(angle));
        Location currentPos = location.clone();
        
        for (int distance = 0; distance < maxDistance; distance++) {
            currentPos.add(direction);
            
            // Check if we hit a solid block
            if (currentPos.getBlock().getType().isSolid()) {
                break;
            }
            
            // Spawn particle every 0.5 blocks for smoother appearance
            if (distance % 1 == 0) {
                // Create 2-block high laser ray
                for (int y = 0; y < 2; y++) {
                    Location particleLocation = currentPos.clone().add(0, y, 0);
                    location.getWorld().spawnParticle(Particle.DUST, particleLocation, 1, dustOptions);
                }
            }
        }
    }
    
    private List<Location> getRayPositions() {
        List<Location> rayPositions = new ArrayList<>();
        double angleStep = 360.0 / rayCount;
        
        for (int i = 0; i < rayCount; i++) {
            double angle = Math.toRadians(currentRotation + (i * angleStep));
            Vector direction = new Vector(Math.cos(angle), 0, Math.sin(angle));
            Location currentPos = location.clone();
            
            for (int distance = 0; distance < maxDistance; distance++) {
                currentPos.add(direction);
                
                // Check if we hit a solid block
                if (currentPos.getBlock().getType().isSolid()) {
                    break;
                }
                
                rayPositions.add(currentPos.clone());
            }
        }
        
        return rayPositions;
    }
    
    @Override
    protected boolean isPlayerInAttackRange(Player player) {
        Location playerLoc = player.getLocation();
        List<Location> rayPositions = getRayPositions();
        
        // Check if player is close to any ray position
        for (Location rayPos : rayPositions) {
            double distance = playerLoc.distance(rayPos);
            if (distance <= 1.0) { // Within 1 block of the ray
                return true;
            }
        }
        
        return false;
    }
}