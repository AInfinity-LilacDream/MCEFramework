package mcevent.MCEFramework.games.football.customHandler;

import mcevent.MCEFramework.games.football.Football;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Armadillo;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import static mcevent.MCEFramework.miscellaneous.Constants.*;

/*
BallBounceHandler: 处理足球的墙壁反弹效果
*/
public class BallBounceHandler {
    
    private BukkitRunnable bounceTask;
    private Football game;
    private boolean isActive = false;
    
    // 球场边界（根据足球场地图调整）- 这些是实际墙壁位置
    private static final double MIN_X = -19.0;
    private static final double MAX_X = 36.0;
    private static final double MIN_Z = -6.0;
    private static final double MAX_Z = 23.0;
    private static final double MIN_Y = -60.0;
    private static final double MAX_Y = -50.0; // 添加最大高度限制
    
    // 反弹衰减系数
    private static final double WALL_DAMPING = 0.8;  // 墙壁反弹衰减
    private static final double GROUND_DAMPING = 0.5; // 地面反弹衰减
    private static final double MIN_BOUNCE_VELOCITY = 0.05; // 最小反弹速度阈值，低于此值不反弹
    private static final double STOP_VELOCITY_THRESHOLD = 0.03; // 完全停止的阈值
    private static final double GROUND_TOLERANCE = 0.2; // 地面检测容差
    
    // 维护球的速度状态
    private Vector ballVelocity = new Vector(0, 0, 0);

    public void start(Football football) {
        this.game = football;
        this.isActive = true;
        
        bounceTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!isActive || game.getBall() == null || game.getBall().isDead()) {
                    return;
                }
                
                checkBounce();
            }
        };
        
        // 每tick检查一次反弹（20次/秒）
        bounceTask.runTaskTimer(plugin, 0L, 1L);
    }
    
    public void suspend() {
        this.isActive = false;
        if (bounceTask != null) {
            bounceTask.cancel();
        }
    }
    
    private void checkBounce() {
        Armadillo ball = game.getBall();
        Location ballLocation = ball.getLocation();
        Vector currentVelocity = ball.getVelocity();
        
        // 更新球的速度状态
        ballVelocity = currentVelocity.clone();
        
        double vx = ballVelocity.getX();
        double vy = ballVelocity.getY();
        double vz = ballVelocity.getZ();
        
        boolean bounced = false;
        
        // 获取犰狳的碰撞箱
        double entityWidth = 0.7; // 犰狳的宽度
        double entityHeight = 0.65; // 犰狳的高度
        
        
        // 添加调试信息 - 只在有明显移动时输出
        if (Math.abs(vx) > 0.05 || Math.abs(vz) > 0.05) {
            plugin.getLogger().info(String.format("[反弹调试] 球心(%.2f,%.2f,%.2f) 速度(%.3f,%.3f,%.3f)", 
                ballLocation.getX(), ballLocation.getY(), ballLocation.getZ(), vx, vy, vz));
            plugin.getLogger().info(String.format("[反弹调试] 球边界: 西%.2f 东%.2f 北%.2f 南%.2f", 
                ballLocation.getX() - entityWidth/2, ballLocation.getX() + entityWidth/2,
                ballLocation.getZ() - entityWidth/2, ballLocation.getZ() + entityWidth/2));
            plugin.getLogger().info(String.format("[反弹调试] 场地边界: X[%.1f到%.1f] Z[%.1f到%.1f]", MIN_X, MAX_X, MIN_Z, MAX_Z));
        }
        
        // X轴边界碰撞 - 只有当球实际碰到墙壁且速度朝向墙壁时才反弹
        double ballWestEdge = ballLocation.getX() - entityWidth/2;
        double ballEastEdge = ballLocation.getX() + entityWidth/2;
        
        if (ballWestEdge <= MIN_X && vx < -MIN_BOUNCE_VELOCITY) {
            plugin.getLogger().info(String.format("[碰撞检测] 西墙反弹触发: 球西边缘%.3f <= 西墙%.1f, 朝向墙速度%.3f", 
                ballWestEdge, MIN_X, vx));
            vx = -vx * WALL_DAMPING;
            bounced = true;
            plugin.getLogger().info(String.format("[反弹结果] 西墙反弹完成: 速度%.3f->%.3f", ballVelocity.getX(), vx));
        } else if (ballEastEdge >= MAX_X && vx > MIN_BOUNCE_VELOCITY) {
            plugin.getLogger().info(String.format("[碰撞检测] 东墙反弹触发: 球东边缘%.3f >= 东墙%.1f, 朝向墙速度%.3f", 
                ballEastEdge, MAX_X, vx));
            vx = -vx * WALL_DAMPING;
            bounced = true;
            plugin.getLogger().info(String.format("[反弹结果] 东墙反弹完成: 速度%.3f->%.3f", ballVelocity.getX(), vx));
        } else {
            // 添加未触发反弹的调试信息
            if ((ballWestEdge <= MIN_X && vx >= -MIN_BOUNCE_VELOCITY) || 
                (ballEastEdge >= MAX_X && vx <= MIN_BOUNCE_VELOCITY)) {
                plugin.getLogger().info(String.format("[反弹未触发] X轴: 西边缘%.3f(边界%.1f) 东边缘%.3f(边界%.1f) 速度%.3f(阈值±%.3f)", 
                    ballWestEdge, MIN_X, ballEastEdge, MAX_X, vx, MIN_BOUNCE_VELOCITY));
            }
        }
        
        // Z轴边界碰撞
        double ballNorthEdge = ballLocation.getZ() - entityWidth/2;
        double ballSouthEdge = ballLocation.getZ() + entityWidth/2;
        
        if (ballNorthEdge <= MIN_Z && vz < -MIN_BOUNCE_VELOCITY) {
            plugin.getLogger().info(String.format("[碰撞检测] 北墙反弹触发: 球北边缘%.3f <= 北墙%.1f, 朝向墙速度%.3f", 
                ballNorthEdge, MIN_Z, vz));
            vz = -vz * WALL_DAMPING;
            bounced = true;
            plugin.getLogger().info(String.format("[反弹结果] 北墙反弹完成: 速度%.3f->%.3f", ballVelocity.getZ(), vz));
        } else if (ballSouthEdge >= MAX_Z && vz > MIN_BOUNCE_VELOCITY) {
            plugin.getLogger().info(String.format("[碰撞检测] 南墙反弹触发: 球南边缘%.3f >= 南墙%.1f, 朝向墙速度%.3f", 
                ballSouthEdge, MAX_Z, vz));
            vz = -vz * WALL_DAMPING;
            bounced = true;
            plugin.getLogger().info(String.format("[反弹结果] 南墙反弹完成: 速度%.3f->%.3f", ballVelocity.getZ(), vz));
        } else {
            // 添加未触发反弹的调试信息
            if ((ballNorthEdge <= MIN_Z && vz >= -MIN_BOUNCE_VELOCITY) || 
                (ballSouthEdge >= MAX_Z && vz <= MIN_BOUNCE_VELOCITY)) {
                plugin.getLogger().info(String.format("[反弹未触发] Z轴: 北边缘%.3f(边界%.1f) 南边缘%.3f(边界%.1f) 速度%.3f(阈值±%.3f)", 
                    ballNorthEdge, MIN_Z, ballSouthEdge, MAX_Z, vz, MIN_BOUNCE_VELOCITY));
            }
        }
        
        // Y轴边界碰撞
        double ballTopEdge = ballLocation.getY() + entityHeight/2;
        double ballBottomEdge = ballLocation.getY() - entityHeight/2;
        
        if (ballTopEdge >= MAX_Y && vy > MIN_BOUNCE_VELOCITY) {
            vy = -vy * WALL_DAMPING; // 天花板反弹
            bounced = true;
            plugin.getLogger().info(String.format("天花板反弹: 球顶边缘%.2f >= 天花板%.1f, 速度%.3f->%.3f", 
                ballTopEdge, MAX_Y, ballVelocity.getY(), vy));
        }
        
        // 地面碰撞 - 只有向下运动且速度足够大才反弹
        if (ballBottomEdge <= MIN_Y && vy < -MIN_BOUNCE_VELOCITY) {
            vy = -vy * GROUND_DAMPING; // 地面反弹衰减更多
            bounced = true;
            plugin.getLogger().info(String.format("地面反弹: 球底边缘%.2f <= 地面%.1f, 速度%.3f->%.3f", 
                ballBottomEdge, MIN_Y, ballVelocity.getY(), vy));
        }
        
        // 暂时禁用方块碰撞检测，只使用边界碰撞
        // TODO: 修复方块碰撞逻辑
        /*
        // 检查方块碰撞
        if (!bounced) {
            Vector newVelocity = checkBlockCollision(ball, ballLocation, new Vector(vx, vy, vz), entityWidth, entityHeight);
            if (newVelocity != null) {
                vx = newVelocity.getX();
                vy = newVelocity.getY();
                vz = newVelocity.getZ();
                bounced = true;
            }
        }
        */
        
        // 应用空气阻力/摩擦力
        boolean isNearGround = (ballLocation.getY() - entityHeight/2) <= MIN_Y + GROUND_TOLERANCE;
        if (isNearGround) {
            // 地面摩擦
            vx *= 0.98;
            vz *= 0.98;
        } else {
            // 空气阻力
            vx *= 0.995;
            vz *= 0.995;
        }
        
        // 检查是否应该停止运动
        double totalSpeed = Math.sqrt(vx * vx + vy * vy + vz * vz);
        if (isNearGround && totalSpeed < STOP_VELOCITY_THRESHOLD && Math.abs(vy) < 0.01) {
            vx = 0;
            vy = 0;
            vz = 0;
            if (ballVelocity.lengthSquared() > 0) {
                plugin.getLogger().info("球已完全停止");
            }
        }
        
        // 更新球的速度
        Vector newVelocity = new Vector(vx, vy, vz);
        ball.setVelocity(newVelocity);
        ballVelocity = newVelocity;
    }
    
    
    private Vector checkBlockCollision(Armadillo ball, Location ballLocation, Vector velocity, double entityWidth, double entityHeight) {
        // 检查实体边界是否与方块碰撞
        double halfWidth = entityWidth / 2;
        double halfHeight = entityHeight / 2;
        
        // 检查实体边界的6个面
        Location[] checkPoints = {
            // 底面中心
            ballLocation.clone().add(0, -halfHeight, 0),
            // 顶面中心
            ballLocation.clone().add(0, halfHeight, 0),
            // 东面中心
            ballLocation.clone().add(halfWidth, 0, 0),
            // 西面中心
            ballLocation.clone().add(-halfWidth, 0, 0),
            // 南面中心
            ballLocation.clone().add(0, 0, halfWidth),
            // 北面中心
            ballLocation.clone().add(0, 0, -halfWidth)
        };
        
        Vector[] normals = {
            new Vector(0, 1, 0),   // 底面法线（向上）
            new Vector(0, -1, 0),  // 顶面法线（向下）
            new Vector(-1, 0, 0),  // 东面法线（向西）
            new Vector(1, 0, 0),   // 西面法线（向东）
            new Vector(0, 0, -1),  // 南面法线（向北）
            new Vector(0, 0, 1)    // 北面法线（向南）
        };
        
        for (int i = 0; i < checkPoints.length; i++) {
            Block block = checkPoints[i].getBlock();
            if (block.getType() != Material.AIR && block.getType().isSolid()) {
                Vector normal = normals[i];
                
                // 计算速度在法线方向上的分量
                double normalVelocity = velocity.dot(normal);
                
                // 只有当速度朝向表面且足够大时才反弹
                if (normalVelocity < -MIN_BOUNCE_VELOCITY) {
                    // 只反转法线方向的速度分量
                    Vector reflectedVelocity = velocity.subtract(normal.multiply(2 * normalVelocity));
                    
                    // 应用衰减
                    Vector normalComponent = normal.multiply(normalVelocity);
                    Vector tangentComponent = velocity.subtract(normalComponent);
                    
                    Vector newNormalComponent = normalComponent.multiply(-WALL_DAMPING);
                    Vector newVelocity = tangentComponent.add(newNormalComponent);
                    
                    String face = switch(i) {
                        case 0 -> "底面";
                        case 1 -> "顶面";
                        case 2 -> "东面";
                        case 3 -> "西面";
                        case 4 -> "南面";
                        case 5 -> "北面";
                        default -> "未知";
                    };
                    
                    plugin.getLogger().info(String.format("方块%s碰撞: 法线速度%.2f -> %.2f", 
                        face, normalVelocity, newNormalComponent.length()));
                    
                    return newVelocity;
                }
            }
        }
        
        return null; // 没有碰撞
    }
    
    
    // 重置速度状态（当球重新生成时调用）
    public void resetVelocity() {
        ballVelocity = new Vector(0, 0, 0);
    }
}