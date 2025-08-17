package mcevent.MCEFramework.games.musicDodge.gameObject.items;

import mcevent.MCEFramework.generalGameObject.MCESpecialItem;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

/**
 * 传送宝珠 - 允许玩家向前方瞬移的特殊物品
 */
public class TeleportOrb extends MCESpecialItem {
    
    private static final double TELEPORT_DISTANCE = 5.0; // 瞬移距离
    
    // 场地边界 - 和其他攻击类保持一致
    private static final double WORLD_LEFT_BOTTOM_X = -7.0;
    private static final double WORLD_LEFT_BOTTOM_Z = -46.0;
    private static final double WORLD_RIGHT_TOP_X = 35.0;
    private static final double WORLD_RIGHT_TOP_Z = -4.0;
    private static final double WORLD_Y = -60.0;

    public TeleportOrb(Plugin plugin) {
        super("<gold><bold>传送宝珠</bold></gold>", Material.ENDER_PEARL, 40, plugin); // 2s = 40 ticks
    }

    @Override
    protected boolean executeAction(Player player, PlayerInteractEvent event) {
        // 注意：事件取消已在基类中处理
        
        Location currentLocation = player.getLocation();
        Location targetLocation = calculateTeleportLocation(player, currentLocation);
        
        // 如果目标位置和当前位置相同，说明无法瞬移
        if (targetLocation.equals(currentLocation)) {
            sendActionBar(player, "<red>面朝墙面过近，无法瞬移！</red>");
            return false;
        }
        
        // 在瞬移前位置生成粒子效果
        spawnTeleportParticles(currentLocation);
        
        // 执行瞬移
        player.teleport(targetLocation);
        
        // 在瞬移后位置生成粒子效果
        spawnTeleportParticles(targetLocation);
        
        // 播放末影人传送声音
        playSound(currentLocation, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
        playSound(targetLocation, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0f, 1.0f);
        
        return true;
    }
    
    /**
     * 计算瞬移目标位置
     * @param player 玩家
     * @param currentLocation 当前位置
     * @return 目标位置
     */
    private Location calculateTeleportLocation(Player player, Location currentLocation) {
        // 获取玩家面朝的水平方向
        Vector direction = player.getLocation().getDirection();
        direction.setY(0); // 忽略Y轴分量，只考虑水平方向
        direction.normalize();
        
        // 计算目标位置
        Vector teleportVector = direction.multiply(TELEPORT_DISTANCE);
        Location targetLocation = currentLocation.clone().add(teleportVector);
        
        // 检查是否会碰撞到墙壁，如果会则调整到墙壁前
        targetLocation = adjustForWallCollision(currentLocation, targetLocation, direction);
        
        return targetLocation;
    }
    
    /**
     * 调整位置以避免穿墙
     * @param start 起始位置
     * @param target 目标位置
     * @param direction 方向向量
     * @return 调整后的位置
     */
    private Location adjustForWallCollision(Location start, Location target, Vector direction) {
        // 检查目标位置是否超出场地边界
        double targetX = target.getX();
        double targetZ = target.getZ();
        
        // 计算到各个边界的距离
        double distanceToLeftWall = (targetX - WORLD_LEFT_BOTTOM_X);
        double distanceToRightWall = (WORLD_RIGHT_TOP_X - targetX);
        double distanceToTopWall = (WORLD_RIGHT_TOP_Z - targetZ);
        double distanceToBottomWall = (targetZ - WORLD_LEFT_BOTTOM_Z);
        
        // 如果目标位置在场地内，直接返回
        if (targetX >= WORLD_LEFT_BOTTOM_X && targetX <= WORLD_RIGHT_TOP_X &&
            targetZ >= WORLD_LEFT_BOTTOM_Z && targetZ <= WORLD_RIGHT_TOP_Z) {
            return target;
        }
        
        // 计算到达边界前的最大安全距离
        double maxDistance = TELEPORT_DISTANCE;
        
        // 检查X方向
        if (direction.getX() > 0) { // 向右移动
            double distanceToWall = distanceToRightWall;
            if (distanceToWall < maxDistance) {
                maxDistance = Math.max(0, distanceToWall - 0.5); // 留0.5格安全距离
            }
        } else if (direction.getX() < 0) { // 向左移动
            double distanceToWall = distanceToLeftWall;
            if (distanceToWall < maxDistance) {
                maxDistance = Math.max(0, distanceToWall - 0.5);
            }
        }
        
        // 检查Z方向
        if (direction.getZ() > 0) { // 向上移动（游戏坐标系）
            double distanceToWall = distanceToTopWall;
            if (distanceToWall < maxDistance) {
                maxDistance = Math.max(0, distanceToWall - 0.5);
            }
        } else if (direction.getZ() < 0) { // 向下移动
            double distanceToWall = distanceToBottomWall;
            if (distanceToWall < maxDistance) {
                maxDistance = Math.max(0, distanceToWall - 0.5);
            }
        }
        
        // 如果最大距离太小，不进行瞬移
        if (maxDistance < 1.0) {
            return start; // 返回原位置
        }
        
        // 计算调整后的目标位置
        Vector adjustedVector = direction.multiply(maxDistance);
        return start.clone().add(adjustedVector);
    }
    
    /**
     * 生成传送粒子效果
     * @param location 位置
     */
    private void spawnTeleportParticles(Location location) {
        // 生成末影人传送粒子
        spawnParticles(location, Particle.PORTAL, 30, 0.5, 1.0, 0.5, 0.1);
        spawnParticles(location, Particle.REVERSE_PORTAL, 30, 0.3, 0.8, 0.3, 0.05);
    }
}