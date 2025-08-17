package mcevent.MCEFramework.games.musicDodge;

import mcevent.MCEFramework.games.musicDodge.gameObject.attacks.*;
import mcevent.MCEFramework.tools.MCETimerUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 攻击数据管理器 - 收集和分发攻击数据
 */
public class AttackDataManager {
    
    private static AttackDataManager instance;
    private final Plugin plugin;
    private final Set<TrackableAttack> activeAttacks = ConcurrentHashMap.newKeySet();
    private BukkitRunnable dataCollectionTask;
    private boolean isRunning = false;
    
    
    private AttackDataManager(Plugin plugin) {
        this.plugin = plugin;
    }
    
    public static AttackDataManager getInstance(Plugin plugin) {
        if (instance == null) {
            instance = new AttackDataManager(plugin);
        }
        return instance;
    }
    
    /**
     * 可追踪的攻击接口
     */
    public interface TrackableAttack {
        /**
         * 获取当前tick的攻击数据
         */
        List<AttackDataEncoder.AttackData> getCurrentAttackData();
        
        /**
         * 检查攻击是否仍然活跃
         */
        boolean isActive();
        
        /**
         * 获取攻击ID（用于去重）
         */
        String getAttackId();
        
        /**
         * 启动攻击（开始产生数据）
         */
        default void start() { };
    }
    
    /**
     * 注册攻击到管理器
     */
    public void registerAttack(TrackableAttack attack) {
        activeAttacks.add(attack);
    }
    
    /**
     * 启动指定攻击
     */
    public void startAttack(String attackId) {
        for (TrackableAttack attack : activeAttacks) {
            if (attack.getAttackId().equals(attackId)) {
                attack.start();
                break;
            }
        }
    }
    
    /**
     * 取消注册攻击
     */
    public void unregisterAttack(TrackableAttack attack) {
        activeAttacks.remove(attack);
    }
    
    /**
     * 开始数据收集和发送
     */
    public void start() {
        if (isRunning) return;
        
        // 注册Plugin Message频道
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, MusicDodgePayload.CHANNEL_NAME);
        
        isRunning = true;
        dataCollectionTask = new BukkitRunnable() {
            @Override
            public void run() {
                collectAndSendAttackData();
            }
        };
        dataCollectionTask.runTaskTimer(plugin, 0L, 1L); // 每1tick执行，与原始攻击系统同步
    }
    
    /**
     * 停止数据收集和发送
     */
    public void stop() {
        if (!isRunning) return;
        
        isRunning = false;
        if (dataCollectionTask != null && !dataCollectionTask.isCancelled()) {
            dataCollectionTask.cancel();
        }
        
        activeAttacks.clear();
        
        // 取消注册Plugin Message频道
        plugin.getServer().getMessenger().unregisterOutgoingPluginChannel(plugin, MusicDodgePayload.CHANNEL_NAME);
    }
    
    /**
     * 收集并发送攻击数据
     */
    private void collectAndSendAttackData() {
        // 清理已结束的攻击
        activeAttacks.removeIf(attack -> !attack.isActive());
        
        // 收集所有活跃攻击的数据
        List<AttackDataEncoder.AttackData> allAttackData = new ArrayList<>();
        for (TrackableAttack attack : activeAttacks) {
            try {
                List<AttackDataEncoder.AttackData> attackData = attack.getCurrentAttackData();
                if (attackData != null) {
                    allAttackData.addAll(attackData);
                }
            } catch (Exception e) {
                // 忽略单个攻击的错误，继续处理其他攻击
                plugin.getLogger().warning("Error processing attack " + attack.getAttackId() + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        // 编码并发送数据
        if (!allAttackData.isEmpty()) {
            String encodedData = AttackDataEncoder.encodeAttacks(allAttackData);
            sendToAllPlayers(encodedData);
        } else {
            // 发送空数据清除客户端显示
            sendToAllPlayers("");
        }
    }
    
    /**
     * 发送数据给所有玩家
     */
    private void sendToAllPlayers(String data) {
        MusicDodgePayload.sendToAllPlayers(data);
    }
    
    // ==================== 攻击包装器类 ====================
    
    /**
     * SpinAttack的包装器
     */
    public static class TrackableSpinAttack implements TrackableAttack {
        private final String attackId;
        private final Location location;
        private final int rayCount;
        private final int maxDistance;
        private boolean isActive = true;
        private boolean isStarted = false; // 添加启动标志
        private int currentTick = 0;
        
        // 通过反射或其他方式获取SpinAttack的内部状态
        private double currentRotation = 0;
        private final double rotationSpeed;
        private boolean isInAttackPhase = false;
        private int alertTicks, attackTicks;
        
        public TrackableSpinAttack(Location location, int rayCount, 
                                 double rotationSpeed, int maxDistance, double alertBeats, 
                                 double attackBeats, int bpm) {
            this.attackId = "spin_" + System.currentTimeMillis() + "_" + location.hashCode();
            this.location = location.clone();
            this.rayCount = rayCount;
            this.maxDistance = maxDistance;
            this.rotationSpeed = rotationSpeed;
            
            // 计算阶段持续时间
            this.alertTicks = (int) (alertBeats * 4 * 60.0 / bpm * 20);
            this.attackTicks = (int) (attackBeats * 4 * 60.0 / bpm * 20);
        }
        
        @Override
        public List<AttackDataEncoder.AttackData> getCurrentAttackData() {
            List<AttackDataEncoder.AttackData> data = new ArrayList<>();
            
            // 如果还未启动，返回空数据
            if (!isStarted) {
                return data;
            }
            
            // 更新旋转角度
            currentRotation += rotationSpeed;
            if (currentRotation >= 360) {
                currentRotation -= 360;
            }
            
            // 确定当前阶段
            AttackDataEncoder.AttackPhase phase;
            int ticksRemaining;
            
            if (currentTick < alertTicks) {
                phase = AttackDataEncoder.AttackPhase.ALERT;
                ticksRemaining = alertTicks - currentTick;
            } else if (currentTick < alertTicks + attackTicks) {
                phase = AttackDataEncoder.AttackPhase.ATTACK;
                ticksRemaining = alertTicks + attackTicks - currentTick;
                isInAttackPhase = true;
            } else {
                isActive = false;
                return data;
            }
            
            // 创建攻击数据
            data.add(AttackDataEncoder.createSpinAttack(
                location, rayCount, currentRotation, maxDistance, phase, ticksRemaining
            ));
            
            currentTick++;
            return data;
        }
        
        @Override
        public boolean isActive() {
            return isActive;
        }
        
        @Override
        public String getAttackId() {
            return attackId;
        }
        
        /**
         * 启动攻击（由服务端攻击调用）
         */
        public void start() {
            isStarted = true;
        }
    }
    
    /**
     * LaserAttack的包装器
     */
    public static class TrackableLaserAttack implements TrackableAttack {
        private final String attackId;
        private final Location start, end;
        private boolean isActive = true;
        private boolean isStarted = false; // 添加启动标志
        private int currentTick = 0;
        private int alertTicks, attackTicks;
        
        public TrackableLaserAttack(Location start, Location end, double alertBeats, 
                                  double attackBeats, int bpm) {
            this.attackId = "laser_" + System.currentTimeMillis() + "_" + start.hashCode();
            this.start = start.clone();
            this.end = end.clone();
            
            // 计算阶段持续时间
            this.alertTicks = (int) (alertBeats * 4 * 60.0 / bpm * 20);
            this.attackTicks = (int) (attackBeats * 4 * 60.0 / bpm * 20);
        }
        
        @Override
        public List<AttackDataEncoder.AttackData> getCurrentAttackData() {
            List<AttackDataEncoder.AttackData> data = new ArrayList<>();
            
            // 如果还未启动，返回空数据
            if (!isStarted) {
                return data;
            }
            
            // 确定当前阶段
            AttackDataEncoder.AttackPhase phase;
            int ticksRemaining;
            
            if (currentTick < alertTicks) {
                phase = AttackDataEncoder.AttackPhase.ALERT;
                ticksRemaining = alertTicks - currentTick;
            } else if (currentTick < alertTicks + attackTicks) {
                phase = AttackDataEncoder.AttackPhase.ATTACK;
                ticksRemaining = alertTicks + attackTicks - currentTick;
            } else {
                isActive = false;
                return data;
            }
            
            // 创建攻击数据
            data.add(AttackDataEncoder.createLaserAttack(start, end, phase, ticksRemaining));
            
            currentTick++;
            return data;
        }
        
        @Override
        public boolean isActive() {
            return isActive;
        }
        
        @Override
        public String getAttackId() {
            return attackId;
        }
        
        /**
         * 启动攻击（由服务端攻击调用）
         */
        public void start() {
            isStarted = true;
        }
    }
    
    /**
     * CircleAttack的包装器
     */
    public static class TrackableCircleAttack implements TrackableAttack {
        private final String attackId;
        private final Location center;
        private final double radius;
        private boolean isActive = true;
        private boolean isStarted = false; // 添加启动标志
        private int currentTick = 0;
        private int alertTicks, attackTicks;
        
        public TrackableCircleAttack(Location center, double radius,
                                   double alertBeats, double attackBeats, int bpm) {
            this.attackId = "circle_" + System.currentTimeMillis() + "_" + center.hashCode();
            this.center = center.clone();
            this.radius = radius;
            
            // 计算阶段持续时间
            this.alertTicks = (int) (alertBeats * 4 * 60.0 / bpm * 20);
            this.attackTicks = (int) (attackBeats * 4 * 60.0 / bpm * 20);
        }
        
        @Override
        public List<AttackDataEncoder.AttackData> getCurrentAttackData() {
            List<AttackDataEncoder.AttackData> data = new ArrayList<>();
            
            // 如果还未启动，返回空数据
            if (!isStarted) {
                return data;
            }
            
            // 确定当前阶段
            AttackDataEncoder.AttackPhase phase;
            int ticksRemaining;
            
            if (currentTick < alertTicks) {
                phase = AttackDataEncoder.AttackPhase.ALERT;
                ticksRemaining = alertTicks - currentTick;
            } else if (currentTick < alertTicks + attackTicks) {
                phase = AttackDataEncoder.AttackPhase.ATTACK;
                ticksRemaining = alertTicks + attackTicks - currentTick;
            } else {
                isActive = false;
                return data;
            }
            
            // 创建攻击数据
            data.add(AttackDataEncoder.createCircleAttack(center, radius, phase, ticksRemaining));
            
            currentTick++;
            return data;
        }
        
        @Override
        public boolean isActive() {
            return isActive;
        }
        
        @Override
        public String getAttackId() {
            return attackId;
        }
        
        /**
         * 启动攻击（由服务端攻击调用）
         */
        public void start() {
            isStarted = true;
        }
    }
    
    /**
     * SquareRingAttack的包装器
     */
    public static class TrackableSquareRingAttack implements TrackableAttack {
        private final String attackId;
        private final Location center;
        private final int innerRadius, outerRadius;
        private boolean isActive = true;
        private boolean isStarted = false; // 添加启动标志
        private int currentTick = 0;
        private int alertTicks, attackTicks;
        
        public TrackableSquareRingAttack(Location center, int innerRadius, int outerRadius,
                                       double alertBeats, double attackBeats, int bpm) {
            this.attackId = "squarering_" + System.currentTimeMillis() + "_" + center.hashCode();
            this.center = center.clone();
            this.innerRadius = innerRadius;
            this.outerRadius = outerRadius;
            
            // 计算阶段持续时间
            this.alertTicks = (int) (alertBeats * 4 * 60.0 / bpm * 20);
            this.attackTicks = (int) (attackBeats * 4 * 60.0 / bpm * 20);
        }
        
        @Override
        public List<AttackDataEncoder.AttackData> getCurrentAttackData() {
            List<AttackDataEncoder.AttackData> data = new ArrayList<>();
            
            // 如果还未启动，返回空数据
            if (!isStarted) {
                return data;
            }
            
            // 确定当前阶段
            AttackDataEncoder.AttackPhase phase;
            int ticksRemaining;
            
            if (currentTick < alertTicks) {
                phase = AttackDataEncoder.AttackPhase.ALERT;
                ticksRemaining = alertTicks - currentTick;
            } else if (currentTick < alertTicks + attackTicks) {
                phase = AttackDataEncoder.AttackPhase.ATTACK;
                ticksRemaining = alertTicks + attackTicks - currentTick;
            } else {
                isActive = false;
                return data;
            }
            
            // 创建攻击数据
            data.add(AttackDataEncoder.createSquareRingAttack(
                center, innerRadius, outerRadius, phase, ticksRemaining
            ));
            
            currentTick++;
            return data;
        }
        
        @Override
        public boolean isActive() {
            return isActive;
        }
        
        @Override
        public String getAttackId() {
            return attackId;
        }
        
        /**
         * 启动攻击（由服务端攻击调用）
         */
        public void start() {
            isStarted = true;
        }
    }
    
    /**
     * WallAttack的包装器
     */
    public static class TrackableWallAttack implements TrackableAttack {
        private final String attackId;
        private final String direction;
        private final double speed;
        private double currentPosition;
        private boolean isActive = true;
        private boolean isStarted = false; // 添加启动标志
        private int currentTick = 0;
        private int alertTicks, attackTicks;
        private static final int FIELD_SIZE = 43;
        
        public TrackableWallAttack(String direction, double alertBeats, double attackBeats, int bpm) {
            this.attackId = "wall_" + System.currentTimeMillis() + "_" + direction.hashCode();
            this.direction = direction;
            
            // 计算阶段持续时间
            this.alertTicks = (int) (alertBeats * 4 * 60.0 / bpm * 20);
            this.attackTicks = (int) (attackBeats * 4 * 60.0 / bpm * 20);
            
            // 计算速度：墙需要在攻击时间内穿越整个场地
            this.speed = (double) FIELD_SIZE / attackTicks;
            
            // 设置初始位置
            initializePosition();
        }
        
        private void initializePosition() {
            switch (direction.toLowerCase()) {
                case "x":
                    currentPosition = -1; // 从左边缘开始
                    break;
                case "-x":
                    currentPosition = FIELD_SIZE + 1; // 从右边缘开始
                    break;
                case "y":
                case "z":
                    currentPosition = -1; // 从下边缘开始
                    break;
                case "-y":
                case "-z":
                    currentPosition = FIELD_SIZE + 1; // 从上边缘开始
                    break;
            }
        }
        
        @Override
        public List<AttackDataEncoder.AttackData> getCurrentAttackData() {
            List<AttackDataEncoder.AttackData> data = new ArrayList<>();
            
            // 如果还未启动，返回空数据
            if (!isStarted) {
                return data;
            }
            
            // 确定当前阶段
            AttackDataEncoder.AttackPhase phase;
            int ticksRemaining;
            
            if (currentTick < alertTicks) {
                phase = AttackDataEncoder.AttackPhase.ALERT;
                ticksRemaining = alertTicks - currentTick;
            } else if (currentTick < alertTicks + attackTicks) {
                phase = AttackDataEncoder.AttackPhase.ATTACK;
                ticksRemaining = alertTicks + attackTicks - currentTick;
                
                // 在攻击阶段更新位置
                int attackTick = currentTick - alertTicks;
                currentPosition = getInitialPosition() + (speed * attackTick);
            } else {
                isActive = false;
                return data;
            }
            
            // 创建攻击数据
            data.add(AttackDataEncoder.createWallAttack(direction, currentPosition, phase, ticksRemaining));
            
            currentTick++;
            return data;
        }
        
        /**
         * 启动攻击（由服务端攻击调用）
         */
        public void start() {
            isStarted = true;
        }
        
        private double getInitialPosition() {
            switch (direction.toLowerCase()) {
                case "x":
                case "y":
                case "z":
                    return -1;
                case "-x":
                case "-y":
                case "-z":
                    return FIELD_SIZE + 1;
                default:
                    return 0;
            }
        }
        
        @Override
        public boolean isActive() {
            return isActive;
        }
        
        @Override
        public String getAttackId() {
            return attackId;
        }
    }
}