package mcevent.MCEFramework.games.football.customHandler;

import mcevent.MCEFramework.games.football.Football;
import mcevent.MCEFramework.games.football.gameObject.FootballMatch;
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
    private FootballMatch match; // 可选：用于子对局解耦
    private boolean isActive = false;
    private boolean useSecondBall = false;

    // 球场边界（根据足球场地图调整）- 可配置，用于支持第二球场
    private double MIN_X = -19.0;
    private double MAX_X = 36.0;
    private double MIN_Z = -6.0;
    private double MAX_Z = 23.0;
    private double MIN_Y = -60.0;
    private double MAX_Y = -50.0; // 添加最大高度限制

    // 反弹衰减系数
    private static final double WALL_DAMPING = 0.8; // 墙壁反弹衰减
    private static final double GROUND_DAMPING = 0.5; // 地面反弹衰减
    private static final double MIN_BOUNCE_VELOCITY = 0.05; // 最小反弹速度阈值，低于此值不反弹
    private static final double STOP_VELOCITY_THRESHOLD = 0.03; // 完全停止的阈值
    private static final double GROUND_TOLERANCE = 0.2; // 地面检测容差

    // 维护球的速度状态
    private Vector ballVelocity = new Vector(0, 0, 0);

    public void start(Football football) {
        this.game = football;
        this.isActive = true;
        this.useSecondBall = false;

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

    /**
     * 使用自定义边界启动（用于第二球场）
     */
    public void start(Football football, double minX, double maxX, double minZ, double maxZ, double minY, double maxY) {
        this.MIN_X = minX;
        this.MAX_X = maxX;
        this.MIN_Z = minZ;
        this.MAX_Z = maxZ;
        this.MIN_Y = minY;
        this.MAX_Y = maxY;
        start(football);
    }

    /**
     * 第二球场使用的启动（操作 ball2）
     */
    public void startSecond(Football football, double minX, double maxX, double minZ, double maxZ, double minY,
            double maxY) {
        this.game = football;
        this.isActive = true;
        this.useSecondBall = true;
        this.MIN_X = minX;
        this.MAX_X = maxX;
        this.MIN_Z = minZ;
        this.MAX_Z = maxZ;
        this.MIN_Y = minY;
        this.MAX_Y = maxY;

        bounceTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!isActive || (useSecondBall ? (game.getBall2() == null || game.getBall2().isDead())
                        : (game.getBall() == null || game.getBall().isDead()))) {
                    return;
                }
                checkBounce();
            }
        };
        bounceTask.runTaskTimer(plugin, 0L, 1L);
    }

    // === 新增：以 FootballMatch 为上下文（第一球场）===
    public void start(FootballMatch match) {
        this.match = match;
        this.game = null;
        this.isActive = true;
        this.useSecondBall = match.isSecondField();
        bounceTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (!isActive || match.getBallEntity() == null || match.getBallEntity().isDead())
                    return;
                checkBounceForMatch();
            }
        };
        bounceTask.runTaskTimer(plugin, 0L, 1L);
    }

    // === 新增：以 FootballMatch 为上下文（第二球场，自定义边界）===
    public void startSecond(FootballMatch match, double minX, double maxX, double minZ, double maxZ, double minY,
            double maxY) {
        this.MIN_X = minX;
        this.MAX_X = maxX;
        this.MIN_Z = minZ;
        this.MAX_Z = maxZ;
        this.MIN_Y = minY;
        this.MAX_Y = maxY;
        start(match);
    }

    public void suspend() {
        this.isActive = false;
        if (bounceTask != null) {
            bounceTask.cancel();
        }
    }

    private void checkBounce() {
        Armadillo ball = useSecondBall ? game.getBall2() : game.getBall();
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

        // X轴边界碰撞 - 只有当球实际碰到墙壁且速度朝向墙壁时才反弹
        // 但在球门范围内不反弹，让球能够进门
        double ballWestEdge = ballLocation.getX() - entityWidth / 2;
        double ballEastEdge = ballLocation.getX() + entityWidth / 2;

        // 检查Z轴是否在球门范围内
        boolean inBlueGoalZRange;
        boolean inRedGoalZRange;
        // 对于第二球场，球门Z范围整体+59
        if (useSecondBall) {
            inBlueGoalZRange = ballLocation.getZ() >= (5 + 59) && ballLocation.getZ() <= (11 + 59);
            inRedGoalZRange = ballLocation.getZ() >= (5 + 59) && ballLocation.getZ() <= (11 + 59);
        } else {
            inBlueGoalZRange = ballLocation.getZ() >= 5 && ballLocation.getZ() <= 11;
            inRedGoalZRange = ballLocation.getZ() >= 5 && ballLocation.getZ() <= 11;
        }

        if (ballWestEdge <= MIN_X && vx < -MIN_BOUNCE_VELOCITY && !inBlueGoalZRange) {
            vx = -vx * WALL_DAMPING;
            bounced = true;
        } else if (ballEastEdge >= MAX_X && vx > MIN_BOUNCE_VELOCITY && !inRedGoalZRange) {
            vx = -vx * WALL_DAMPING;
            bounced = true;
        }

        // Z轴边界碰撞
        double ballNorthEdge = ballLocation.getZ() - entityWidth / 2;
        double ballSouthEdge = ballLocation.getZ() + entityWidth / 2;

        // 在球门X范围内不做Z轴反弹，避免球门反弹
        boolean inRedGoalXRange;
        boolean inBlueGoalXRange;
        if (useSecondBall) {
            // 青门在东（X=36..39），黄门在西（X=-23..-20），与红蓝一致
            inRedGoalXRange = (ballEastEdge >= (36 + 7) && ballWestEdge <= (39 + 7));
            inBlueGoalXRange = (ballWestEdge <= (-20 + 7) && ballEastEdge >= (-23 + 7));
        } else {
            inRedGoalXRange = (ballEastEdge >= 36 && ballWestEdge <= 39);
            inBlueGoalXRange = (ballWestEdge <= -20 && ballEastEdge >= -23);
        }

        if (ballNorthEdge <= MIN_Z && vz < -MIN_BOUNCE_VELOCITY && !inBlueGoalXRange) {
            vz = -vz * WALL_DAMPING;
            bounced = true;
        } else if (ballSouthEdge >= MAX_Z && vz > MIN_BOUNCE_VELOCITY && !inRedGoalXRange) {
            vz = -vz * WALL_DAMPING;
            bounced = true;
        }

        // Y轴边界碰撞
        double ballTopEdge = ballLocation.getY() + entityHeight / 2;
        double ballBottomEdge = ballLocation.getY() - entityHeight / 2;

        if (ballTopEdge >= MAX_Y && vy > MIN_BOUNCE_VELOCITY) {
            vy = -vy * WALL_DAMPING; // 天花板反弹
            bounced = true;
        }

        // 地面碰撞 - 只有向下运动且速度足够大才反弹
        if (ballBottomEdge <= MIN_Y && vy < -MIN_BOUNCE_VELOCITY) {
            vy = -vy * GROUND_DAMPING; // 地面反弹衰减更多
            bounced = true;
        }

        // 应用空气阻力/摩擦力
        boolean isNearGround = (ballLocation.getY() - entityHeight / 2) <= MIN_Y + GROUND_TOLERANCE;
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
        }

        // 更新球的速度
        Vector newVelocity = new Vector(vx, vy, vz);
        ball.setVelocity(newVelocity);
        ballVelocity = newVelocity;
    }

    // === 新增：Match 版本反弹检测 ===
    private void checkBounceForMatch() {
        Armadillo ball = match.getBallEntity();
        Location ballLocation = ball.getLocation();
        org.bukkit.util.Vector currentVelocity = ball.getVelocity();
        org.bukkit.util.Vector vel = currentVelocity.clone();

        double entityWidth = 0.7;
        double entityHeight = 0.65;
        double vx = vel.getX(), vy = vel.getY(), vz = vel.getZ();

        double ballWestEdge = ballLocation.getX() - entityWidth / 2;
        double ballEastEdge = ballLocation.getX() + entityWidth / 2;
        double ballNorthEdge = ballLocation.getZ() - entityWidth / 2;
        double ballSouthEdge = ballLocation.getZ() + entityWidth / 2;

        boolean bounced = false;

        // 与现有逻辑一致：球门范围不做外墙反弹（仅示例，具体范围由 match 持有的门框定义控制进球，
        // 这里仅避免边界反弹导致无法入门，采用与原始处理相同的近似判定）
        boolean inGoalZ = false; // 简化：不在此复刻门Z范围特判，交由进球检测处理

        if (ballWestEdge <= MIN_X && vx < -0.05 && !inGoalZ) {
            vx = -vx * 0.8;
            bounced = true;
        } else if (ballEastEdge >= MAX_X && vx > 0.05 && !inGoalZ) {
            vx = -vx * 0.8;
            bounced = true;
        }

        boolean inGoalX = false;
        if (ballNorthEdge <= MIN_Z && vz < -0.05 && !inGoalX) {
            vz = -vz * 0.8;
            bounced = true;
        } else if (ballSouthEdge >= MAX_Z && vz > 0.05 && !inGoalX) {
            vz = -vz * 0.8;
            bounced = true;
        }

        if ((ballLocation.getY() + entityHeight / 2) >= MAX_Y && vy > 0.05) {
            vy = -vy * 0.8;
            bounced = true;
        }
        if ((ballLocation.getY() - entityHeight / 2) <= MIN_Y && vy < -0.05) {
            vy = -vy * 0.5;
            bounced = true;
        }

        if (!bounced) {
            boolean nearGround = (ballLocation.getY() - entityHeight / 2) <= MIN_Y + 0.2;
            if (nearGround) {
                vx *= 0.98;
                vz *= 0.98;
            } else {
                vx *= 0.995;
                vz *= 0.995;
            }
            double speed = Math.sqrt(vx * vx + vy * vy + vz * vz);
            if (nearGround && speed < 0.03 && Math.abs(vy) < 0.01) {
                vx = vy = vz = 0;
            }
        }

        ball.setVelocity(new org.bukkit.util.Vector(vx, vy, vz));
    }

    private Vector checkBlockCollision(Armadillo ball, Location ballLocation, Vector velocity, double entityWidth,
            double entityHeight) {
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
                new Vector(0, 1, 0), // 底面法线（向上）
                new Vector(0, -1, 0), // 顶面法线（向下）
                new Vector(-1, 0, 0), // 东面法线（向西）
                new Vector(1, 0, 0), // 西面法线（向东）
                new Vector(0, 0, -1), // 南面法线（向北）
                new Vector(0, 0, 1) // 北面法线（向南）
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