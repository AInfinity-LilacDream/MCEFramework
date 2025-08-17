package mcevent.MCEFramework.games.musicDodge;

import org.bukkit.Color;
import org.bukkit.Location;
import java.util.ArrayList;
import java.util.List;

/**
 * 攻击数据编码器 - 将攻击信息编码为字符串发送给客户端
 * 格式：类型|参数1,参数2,...|颜色|相位
 */
public class AttackDataEncoder {
    
    /**
     * 基本攻击数据类型
     */
    public enum AttackType {
        LASER,      // 直线激光: x1,y1,z1,x2,y2,z2
        SQUARE_RING, // 正方形环: centerX,centerY,centerZ,innerRadius,outerRadius
        SPIN,       // 旋转激光: centerX,centerY,centerZ,rayCount,angleOffset,maxDistance
        CIRCLE,     // 圆形攻击: centerX,centerY,centerZ,radius
        WALL        // 墙攻击: direction,position
    }
    
    /**
     * 攻击相位
     */
    public enum AttackPhase {
        ALERT,      // 预警阶段（灰色）
        ATTACK      // 攻击阶段（红色）
    }
    
    /**
     * 单个攻击数据
     */
    public static class AttackData {
        public AttackType type;
        public String parameters;
        public AttackPhase phase;
        public int ticksRemaining;
        
        public AttackData(AttackType type, String parameters, AttackPhase phase, int ticksRemaining) {
            this.type = type;
            this.parameters = parameters;
            this.phase = phase;
            this.ticksRemaining = ticksRemaining;
        }
        
        public String encode() {
            String colorCode = (phase == AttackPhase.ALERT) ? "GRAY" : "RED";
            return type.name() + "|" + parameters + "|" + colorCode + "|" + ticksRemaining;
        }
    }
    
    /**
     * 编码多个攻击数据
     */
    public static String encodeAttacks(List<AttackData> attacks) {
        if (attacks.isEmpty()) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < attacks.size(); i++) {
            if (i > 0) sb.append("#");
            sb.append(attacks.get(i).encode());
        }
        return sb.toString();
    }
    
    // ==================== 具体攻击类型的编码器 ====================
    
    /**
     * 创建激光攻击数据
     */
    public static AttackData createLaserAttack(Location start, Location end, AttackPhase phase, int ticksRemaining) {
        String params = String.format("%.2f,%.2f,%.2f,%.2f,%.2f,%.2f", 
            start.getX(), start.getY(), start.getZ(),
            end.getX(), end.getY(), end.getZ());
        return new AttackData(AttackType.LASER, params, phase, ticksRemaining);
    }
    
    /**
     * 创建正方形环攻击数据
     */
    public static AttackData createSquareRingAttack(Location center, int innerRadius, int outerRadius, 
                                                   AttackPhase phase, int ticksRemaining) {
        String params = String.format("%.2f,%.2f,%.2f,%d,%d", 
            center.getX(), center.getY(), center.getZ(), innerRadius, outerRadius);
        return new AttackData(AttackType.SQUARE_RING, params, phase, ticksRemaining);
    }
    
    /**
     * 创建旋转攻击数据
     */
    public static AttackData createSpinAttack(Location center, int rayCount, double angleOffset, 
                                            int maxDistance, AttackPhase phase, int ticksRemaining) {
        String params = String.format("%.2f,%.2f,%.2f,%d,%.2f,%d", 
            center.getX(), center.getY(), center.getZ(), rayCount, angleOffset, maxDistance);
        return new AttackData(AttackType.SPIN, params, phase, ticksRemaining);
    }
    
    /**
     * 创建圆形攻击数据
     */
    public static AttackData createCircleAttack(Location center, double radius, AttackPhase phase, int ticksRemaining) {
        String params = String.format("%.2f,%.2f,%.2f,%.2f", 
            center.getX(), center.getY(), center.getZ(), radius);
        return new AttackData(AttackType.CIRCLE, params, phase, ticksRemaining);
    }
    
    /**
     * 创建墙攻击数据
     */
    public static AttackData createWallAttack(String direction, double position, AttackPhase phase, int ticksRemaining) {
        String params = direction + "," + String.format("%.2f", position);
        return new AttackData(AttackType.WALL, params, phase, ticksRemaining);
    }
    
    // ==================== 攻击数据解析器（客户端用） ====================
    
    /**
     * 解析激光攻击参数
     */
    public static class LaserParams {
        public double x1, y1, z1, x2, y2, z2;
        
        public LaserParams(String params) {
            String[] parts = params.split(",");
            if (parts.length == 6) {
                x1 = Double.parseDouble(parts[0]);
                y1 = Double.parseDouble(parts[1]);
                z1 = Double.parseDouble(parts[2]);
                x2 = Double.parseDouble(parts[3]);
                y2 = Double.parseDouble(parts[4]);
                z2 = Double.parseDouble(parts[5]);
            }
        }
    }
    
    /**
     * 解析正方形环攻击参数
     */
    public static class SquareRingParams {
        public double centerX, centerY, centerZ;
        public int innerRadius, outerRadius;
        
        public SquareRingParams(String params) {
            String[] parts = params.split(",");
            if (parts.length == 5) {
                centerX = Double.parseDouble(parts[0]);
                centerY = Double.parseDouble(parts[1]);
                centerZ = Double.parseDouble(parts[2]);
                innerRadius = Integer.parseInt(parts[3]);
                outerRadius = Integer.parseInt(parts[4]);
            }
        }
    }
    
    /**
     * 解析旋转攻击参数
     */
    public static class SpinParams {
        public double centerX, centerY, centerZ;
        public int rayCount, maxDistance;
        public double angleOffset;
        
        public SpinParams(String params) {
            String[] parts = params.split(",");
            if (parts.length == 6) {
                centerX = Double.parseDouble(parts[0]);
                centerY = Double.parseDouble(parts[1]);
                centerZ = Double.parseDouble(parts[2]);
                rayCount = Integer.parseInt(parts[3]);
                angleOffset = Double.parseDouble(parts[4]);
                maxDistance = Integer.parseInt(parts[5]);
            }
        }
    }
    
    /**
     * 解析圆形攻击参数
     */
    public static class CircleParams {
        public double centerX, centerY, centerZ, radius;
        
        public CircleParams(String params) {
            String[] parts = params.split(",");
            if (parts.length == 4) {
                centerX = Double.parseDouble(parts[0]);
                centerY = Double.parseDouble(parts[1]);
                centerZ = Double.parseDouble(parts[2]);
                radius = Double.parseDouble(parts[3]);
            }
        }
    }
    
    /**
     * 解析墙攻击参数
     */
    public static class WallParams {
        public String direction;
        public double position;
        
        public WallParams(String params) {
            String[] parts = params.split(",");
            if (parts.length == 2) {
                direction = parts[0];
                position = Double.parseDouble(parts[1]);
            }
        }
    }
}