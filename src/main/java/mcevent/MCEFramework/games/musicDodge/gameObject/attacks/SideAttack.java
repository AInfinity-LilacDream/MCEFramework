package mcevent.MCEFramework.games.musicDodge.gameObject.attacks;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import mcevent.MCEFramework.games.musicDodge.AttackFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * SideAttack - 由多个 SquareRingAttack 组成的从外向内收缩攻击
 */
public class SideAttack extends MCEAttack {
    private final String worldName;
    private final Plugin plugin;
    private final AttackFactory attackFactory;
    private final int ringThickness; // 每道正方形环的厚度
    private final double ringAlertMeasures; // 每个正方形环的预警时间
    private final double ringAttackMeasures; // 每个正方形环的攻击时间
    private final List<SquareRingAttack> activeRings = new ArrayList<>();
    
    // 场地中心位置和范围
    private static final double WORLD_LEFT_BOTTOM_X = -7.0;
    private static final double WORLD_LEFT_BOTTOM_Z = -46.0;
    private static final double WORLD_Y = -60.0;
    private static final int FIELD_SIZE = 43;
    private static final int MAX_RADIUS = FIELD_SIZE / 2; // 21

    public SideAttack(double attackDurationMeasures, String worldName, int ringThickness, 
                     double ringAlertMeasures, double ringAttackMeasures, int bpm, Plugin plugin, AttackFactory attackFactory) {
        super(0, attackDurationMeasures, bpm); // SideAttack 本身无预警时间
        this.worldName = worldName;
        this.plugin = plugin;
        this.attackFactory = attackFactory;
        this.ringThickness = ringThickness;
        this.ringAlertMeasures = ringAlertMeasures;
        this.ringAttackMeasures = ringAttackMeasures;
    }
    
    // 计算环生成间隔（tick）
    private int getRingGenerationIntervalTicks() {
        int totalRings = MAX_RADIUS / ringThickness;
        if (totalRings <= 0) totalRings = 1;
        return getAttackDurationTicks() / totalRings;
    }
    
    // 计算预警时间对应的 tick
    private int getRingAlertTicks() {
        return (int) (ringAlertMeasures * 4 * 60.0 / getBpm() * 20);
    }

    @Override
    public void toggle() {
        // 直接开始攻击阶段，无预警时间
        startAttackPhase();
    }
    
    private void startAttackPhase() {
        // 播放攻击开始音效
        playAttackSound("block.note_block.pling");
        
        // 获取场地中心位置
        Location centerLocation = getCenterLocation();
        
        // 计算时间参数
        final int alertTicks = getRingAlertTicks();
        final int generationInterval = getRingGenerationIntervalTicks();
        
        // 开始生成正方形环序列
        // 为了让攻击时间卡在拍子上，需要提前开始，提前的时间就是预警时间
        new BukkitRunnable() {
            int ticks = -alertTicks; // 从负的预警时间开始
            final int maxTicks = getAttackDurationTicks();
            int nextGenerationTick = 0;
            int currentRingIndex = 0;
            
            @Override
            public void run() {
                if (ticks >= maxTicks) {
                    this.cancel();
                    return;
                }
                
                // 按照指定间隔生成正方形环
                if (ticks >= nextGenerationTick && currentRingIndex < MAX_RADIUS / ringThickness) {
                    int remainingTicks = maxTicks - ticks;
                    generateSquareRing(centerLocation, currentRingIndex, remainingTicks);
                    nextGenerationTick = ticks + generationInterval;
                    currentRingIndex++;
                }
                
                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }
    
    /**
     * 生成一道正方形环
     * @param centerLocation 中心位置
     * @param ringIndex 环的索引（0表示最外层）
     * @param remainingTicks 剩余总时间（tick）
     */
    private void generateSquareRing(Location centerLocation, int ringIndex, int remainingTicks) {
        // 计算当前环的内外半径
        int outerRadius = MAX_RADIUS - (ringIndex * ringThickness);
        int innerRadius = Math.max(3, outerRadius - ringThickness); // 确保内半径至少为3，保留中心5x5空间
        
        // 如果外半径小于等于内半径，不生成（避免重叠）
        if (outerRadius <= innerRadius) {
            return;
        }
        
        // 计算该环的攻击持续时间：从现在开始到整个SideAttack结束
        double remainingMeasures = remainingTicks / (4.0 * 60.0 / getBpm() * 20);
        double ringTotalDuration = remainingMeasures - ringAlertMeasures; // 减去预警时间
        
        // 确保攻击时间不会为负数
        if (ringTotalDuration <= 0) {
            ringTotalDuration = 0.1; // 最小持续时间
        }
        
        // 创建 SquareRingAttack，攻击时间延长到整个攻击结束
        SquareRingAttack squareRing = attackFactory.createSquareRingAttack(
            ringAlertMeasures,
            ringTotalDuration,
            centerLocation,
            innerRadius,
            outerRadius,
            getBpm()
        );
        
        // 立即启动这个正方形环攻击
        squareRing.toggle();
        
        activeRings.add(squareRing);
    }
    
    /**
     * 获取场地中心位置
     * @return 中心位置
     */
    private Location getCenterLocation() {
        double centerX = WORLD_LEFT_BOTTOM_X + (FIELD_SIZE - 1) / 2.0;
        double centerZ = WORLD_LEFT_BOTTOM_Z + (FIELD_SIZE - 1) / 2.0;
        return new Location(Objects.requireNonNull(Bukkit.getWorld(worldName)), centerX, WORLD_Y, centerZ);
    }

    @Override
    public double getFirstInternalAttackStartOffset() {
        // SideAttack的第一个内部攻击（第一个SquareRingAttack）在SideAttack开始后立即生成
        // 第一个SquareRingAttack有自己的预警时间，所以总的偏移时间是：SideAttack的预警时间 + 第一个Ring的预警时间
        // 但是SideAttack本身没有预警时间（alertDurationBeats = 0），所以只需要返回第一个Ring的预警时间
        return ringAlertMeasures;
    }

    @Override
    protected boolean isPlayerInAttackRange(Player player) {
        // SideAttack 本身不直接造成伤害，伤害由生成的 SquareRingAttack 处理
        // 检查玩家是否在任何活动的正方形环范围内
        for (SquareRingAttack ring : activeRings) {
            if (ring.isPlayerInAttackRange(player)) {
                return true;
            }
        }
        return false;
    }
}