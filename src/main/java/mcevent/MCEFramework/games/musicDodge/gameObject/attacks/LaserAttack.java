package mcevent.MCEFramework.games.musicDodge.gameObject.attacks;

import mcevent.MCEFramework.games.musicDodge.ParticleOptimizationIntegration;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Particle.DustOptions;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class LaserAttack extends MCEAttack {
    private final int startX, startY;
    private final int endX, endY;
    private final Location baseLocation;
    private final Plugin plugin;
    private mcevent.MCEFramework.games.musicDodge.AttackDataManager.TrackableLaserAttack trackableAttack;
    
    // Game field coordinates: left-bottom corner (-7, -60, -46) is origin (0,0)
    // Field: right-bottom (-7, -60, -4) to left-top (35, -60, -46)
    // Size: 42x42, X-axis points right (world X+), Y-axis points up (world Z+)
    private static final double WORLD_LEFT_BOTTOM_X = -7.0;
    private static final double WORLD_LEFT_BOTTOM_Z = -46.0;
    private static final double WORLD_Y = -60.0;
    
    public LaserAttack(double alertDurationBeats, double attackDurationBeats, int startX, int startY, int endX, int endY, World world, int bpm, Plugin plugin) {
        super(alertDurationBeats, attackDurationBeats, bpm);
        this.startX = startX;
        this.startY = startY;
        this.endX = endX;
        this.endY = endY;
        this.plugin = plugin;
        
        // Use center of the laser line as base location
        double centerX = WORLD_LEFT_BOTTOM_X + (startX + endX) / 2.0;
        double centerZ = WORLD_LEFT_BOTTOM_Z + (startY + endY) / 2.0;
        this.baseLocation = new Location(world, centerX, WORLD_Y, centerZ);
        
    }
    
    /**
     * 设置TrackableAttack（用于粒子优化）
     */
    public void setTrackableAttack(mcevent.MCEFramework.games.musicDodge.AttackDataManager.TrackableLaserAttack trackableAttack) {
        this.trackableAttack = trackableAttack;
    }
    
    @Override
    public void toggle() {
        // 如果有TrackableAttack，立即注册并启动
        if (trackableAttack != null) {
            mcevent.MCEFramework.games.musicDodge.AttackDataManager.getInstance(plugin).registerAttack(trackableAttack);
            trackableAttack.start();
        }
        
        // 如果优化系统启用，通知优化系统立即启动相应的攻击
        if (ParticleOptimizationIntegration.isOptimizationEnabled()) {
            // 攻击已经通过AttackFactory注册到优化系统，这里不需要额外处理
            // 只需要启动原始攻击的时间逻辑用于伤害判定
        }
        
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
                
                spawnLaserParticles(new DustOptions(Color.GRAY, 1.0f));
                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }
    
    private void startAttackPhase() {
        playAttackSound("block.note_block.pling");
        
        new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = getAttackDurationTicks();
            
            @Override
            public void run() {
                if (ticks >= maxTicks) {
                    this.cancel();
                    return;
                }
                
                spawnLaserParticles(new DustOptions(Color.RED, 1.0f));
                // 延迟1tick开始伤害检查，与视觉效果同步
                if (ticks > 0) {
                    checkPlayerDamage(baseLocation, 2.0);
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }
    
    private void spawnLaserParticles(DustOptions dustOptions) {
        // 检查是否启用了粒子优化系统，如果是则跳过原始粒子渲染
        if (ParticleOptimizationIntegration.isOptimizationEnabled()) {
            return;
        }
        
        // Convert game coordinates to world coordinates
        // Game X+ = World X+, Game Y+ = World Z+
        double worldStartX = WORLD_LEFT_BOTTOM_X + startX;
        double worldStartZ = WORLD_LEFT_BOTTOM_Z + startY;
        double worldEndX = WORLD_LEFT_BOTTOM_X + endX;
        double worldEndZ = WORLD_LEFT_BOTTOM_Z + endY;
        
        // Calculate the distance and direction
        double dx = worldEndX - worldStartX;
        double dz = worldEndZ - worldStartZ;
        double distance = Math.sqrt(dx * dx + dz * dz);
        
        // Normalize direction vector
        Vector direction = new Vector(dx / distance, 0, dz / distance);
        
        // Spawn particles along the laser line
        int particleCount = (int) Math.ceil(distance * 2); // 2 particles per block for smooth line
        for (int i = 0; i <= particleCount; i++) {
            double progress = i / (double) particleCount;
            double currentX = worldStartX + dx * progress;
            double currentZ = worldStartZ + dz * progress;
            
            // Create laser line with height
            for (int y = 0; y < 2; y++) {
                Location particleLocation = new Location(baseLocation.getWorld(), currentX, WORLD_Y + y, currentZ);
                baseLocation.getWorld().spawnParticle(Particle.DUST, particleLocation, 1, dustOptions);
            }
        }
    }
    
    @Override
    protected boolean isPlayerInAttackRange(Player player) {
        Location playerLoc = player.getLocation();
        
        // Convert world coordinates back to game coordinates for calculation
        double playerGameX = playerLoc.getX() - WORLD_LEFT_BOTTOM_X;
        double playerGameY = playerLoc.getZ() - WORLD_LEFT_BOTTOM_Z;
        
        // Calculate distance from player to laser line
        double dx = endX - startX;
        double dy = endY - startY;
        double lineLength = Math.sqrt(dx * dx + dy * dy);
        
        if (lineLength == 0) {
            // Single point laser
            double distToPoint = Math.sqrt(Math.pow(playerGameX - startX, 2) + Math.pow(playerGameY - startY, 2));
            return distToPoint <= 1.0;
        }
        
        // Calculate perpendicular distance from player to laser line
        double t = ((playerGameX - startX) * dx + (playerGameY - startY) * dy) / (lineLength * lineLength);
        t = Math.max(0, Math.min(1, t)); // Clamp t to [0,1]
        
        double closestX = startX + t * dx;
        double closestY = startY + t * dy;
        
        double distanceToLine = Math.sqrt(Math.pow(playerGameX - closestX, 2) + Math.pow(playerGameY - closestY, 2));
        
        return distanceToLine <= 1.0; // Within 1 block of the laser line
    }
}