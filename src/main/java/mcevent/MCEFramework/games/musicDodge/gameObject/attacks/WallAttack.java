package mcevent.MCEFramework.games.musicDodge.gameObject.attacks;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Particle.DustOptions;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class WallAttack extends MCEAttack {
    private final String direction;
    private final World world;
    private final Plugin plugin;
    private final double speed; // calculated speed (blocks per tick)
    private mcevent.MCEFramework.games.musicDodge.AttackDataManager.TrackableWallAttack trackableAttack;
    
    // Game field coordinates: left-bottom corner (-7, -60, -46) is origin (0,0)
    // Field: right-bottom (-7, -60, -4) to left-top (35, -60, -46)
    // Size: 43x43, X-axis points right (world X+), Y-axis points up (world Z+)
    private static final double WORLD_LEFT_BOTTOM_X = -7.0;
    private static final double WORLD_LEFT_BOTTOM_Z = -46.0;
    private static final double WORLD_Y = -60.0;
    private static final int FIELD_SIZE = 43;
    private static final int WALL_HEIGHT = 8;
    
    // Current wall position (in game coordinates)
    private double currentPosition;
    private boolean isMoving = false;
    
    public WallAttack(double alertDurationBeats, double attackDurationBeats, String direction, World world, int bpm, Plugin plugin) {
        super(alertDurationBeats, attackDurationBeats, bpm);
        this.direction = direction;
        this.world = world;
        this.plugin = plugin;
        
        // Calculate speed: wall needs to travel FIELD_SIZE blocks in attackDurationTicks
        int attackTicks = getAttackDurationTicks();
        this.speed = (double) FIELD_SIZE / attackTicks;
        
        // Set initial position based on direction
        initializePosition();
    }
    
    private void initializePosition() {
        switch (direction.toLowerCase()) {
            case "x":
                currentPosition = -1; // Start from left edge (before 0)
                break;
            case "-x":
                currentPosition = FIELD_SIZE; // Start from right edge (after 41)
                break;
            case "y":
                currentPosition = -1; // Start from bottom edge (before 0)
                break;
            case "-y":
                currentPosition = FIELD_SIZE; // Start from top edge (after 41)
                break;
            default:
                throw new IllegalArgumentException("Invalid direction: " + direction + ". Use 'x', '-x', 'y', or '-y'.");
        }
    }
    
    /**
     * 设置TrackableAttack（用于粒子优化）
     */
    public void setTrackableAttack(mcevent.MCEFramework.games.musicDodge.AttackDataManager.TrackableWallAttack trackableAttack) {
        this.trackableAttack = trackableAttack;
    }
    
    @Override
    public void toggle() {
        // 如果有TrackableAttack，立即注册并启动
        if (trackableAttack != null) {
            mcevent.MCEFramework.games.musicDodge.AttackDataManager.getInstance(plugin).registerAttack(trackableAttack);
            trackableAttack.start();
        }
        
        // Phase 1: Static gray wall for alert duration
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
                
                spawnWallParticles(new DustOptions(Color.GRAY, 1.0f));
                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }
    
    private void startAttackPhase() {
        playAttackSound("block.note_block.pling");
        isMoving = true;
        
        // Phase 2: Moving red wall for attack duration
        new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = getAttackDurationTicks();
            
            @Override
            public void run() {
                if (ticks >= maxTicks || isWallOutOfBounds()) {
                    this.cancel();
                    return;
                }
                
                updateWallPosition();
                spawnWallParticles(new DustOptions(Color.RED, 1.0f));
                // 延迟1tick开始伤害检查，与视觉效果同步
                if (ticks > 0) {
                    checkPlayerDamage(getCurrentWallLocation(), 2.0);
                }
                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }
    
    private void updateWallPosition() {
        if (!isMoving) return;
        
        switch (direction.toLowerCase()) {
            case "x":
                currentPosition += speed;
                break;
            case "-x":
                currentPosition -= speed;
                break;
            case "y":
                currentPosition += speed;
                break;
            case "-y":
                currentPosition -= speed;
                break;
        }
    }
    
    private boolean isWallOutOfBounds() {
        switch (direction.toLowerCase()) {
            case "x":
                return currentPosition > FIELD_SIZE;
            case "-x":
                return currentPosition < -1;
            case "y":
                return currentPosition > FIELD_SIZE;
            case "-y":
                return currentPosition < -1;
            default:
                return true;
        }
    }
    
    private Location getCurrentWallLocation() {
        // Return center location of the wall for damage checking
        switch (direction.toLowerCase()) {
            case "x":
            case "-x":
                return new Location(world, 
                    WORLD_LEFT_BOTTOM_X + currentPosition, 
                    WORLD_Y + WALL_HEIGHT / 2.0, 
                    WORLD_LEFT_BOTTOM_Z + FIELD_SIZE / 2.0);
            case "y":
            case "-y":
                return new Location(world, 
                    WORLD_LEFT_BOTTOM_X + FIELD_SIZE / 2.0, 
                    WORLD_Y + WALL_HEIGHT / 2.0, 
                    WORLD_LEFT_BOTTOM_Z + currentPosition);
            default:
                return new Location(world, 0, 0, 0);
        }
    }
    
    private void spawnWallParticles(DustOptions dustOptions) {
        switch (direction.toLowerCase()) {
            case "x":
            case "-x":
                spawnVerticalWall(currentPosition, dustOptions);
                break;
            case "y":
            case "-y":
                spawnHorizontalWall(currentPosition, dustOptions);
                break;
        }
    }
    
    private void spawnVerticalWall(double xPos, DustOptions dustOptions) {
        // 检查是否启用了粒子优化系统，如果是则跳过原始粒子渲染
        if (mcevent.MCEFramework.games.musicDodge.ParticleOptimizationIntegration.isOptimizationEnabled()) {
            return;
        }
        
        // Wall moving in X direction (vertical wall spanning Z axis)
        double worldX = WORLD_LEFT_BOTTOM_X + xPos;
        
        for (int z = 0; z <= FIELD_SIZE; z++) {
            double worldZ = WORLD_LEFT_BOTTOM_Z + z;
            
            for (int y = 0; y < WALL_HEIGHT; y++) {
                Location particleLocation = new Location(world, worldX, WORLD_Y + y, worldZ);
                world.spawnParticle(Particle.DUST, particleLocation, 1, dustOptions);
            }
        }
    }
    
    private void spawnHorizontalWall(double zPos, DustOptions dustOptions) {
        // 检查是否启用了粒子优化系统，如果是则跳过原始粒子渲染
        if (mcevent.MCEFramework.games.musicDodge.ParticleOptimizationIntegration.isOptimizationEnabled()) {
            return;
        }
        
        // Wall moving in Z direction (horizontal wall spanning X axis)
        double worldZ = WORLD_LEFT_BOTTOM_Z + zPos;
        
        for (int x = 0; x <= FIELD_SIZE; x++) {
            double worldX = WORLD_LEFT_BOTTOM_X + x;
            
            for (int y = 0; y < WALL_HEIGHT; y++) {
                Location particleLocation = new Location(world, worldX, WORLD_Y + y, worldZ);
                world.spawnParticle(Particle.DUST, particleLocation, 1, dustOptions);
            }
        }
    }
    
    @Override
    protected boolean isPlayerInAttackRange(Player player) {
        Location playerLoc = player.getLocation();
        
        // Convert world coordinates to game coordinates
        double playerGameX = playerLoc.getX() - WORLD_LEFT_BOTTOM_X;
        double playerGameZ = playerLoc.getZ() - WORLD_LEFT_BOTTOM_Z;
        double playerGameY = playerLoc.getY() - WORLD_Y;
        
        // Check if player is within wall height
        if (playerGameY < 0 || playerGameY >= WALL_HEIGHT) {
            return false;
        }
        
        // Check if player is within wall bounds
        switch (direction.toLowerCase()) {
            case "x":
            case "-x":
                // Vertical wall: check if player X position is close to wall position
                return Math.abs(playerGameX - currentPosition) <= 1.0;
            case "y":
            case "-y":
                // Horizontal wall: check if player Z position is close to wall position
                return Math.abs(playerGameZ - currentPosition) <= 1.0;
            default:
                return false;
        }
    }
}