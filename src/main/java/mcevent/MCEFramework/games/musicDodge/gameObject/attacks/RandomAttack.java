package mcevent.MCEFramework.games.musicDodge.gameObject.attacks;

import mcevent.MCEFramework.games.musicDodge.AttackFactory;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Random;

public class RandomAttack extends MCEAttack {
    private final World world;
    private final Plugin plugin;
    private final AttackFactory attackFactory;
    private final int circleCount; // 每次攻击生成几个CircleAttack
    private final double generationSpeedMeasures; // 生成速度（每多少小节生成一次）
    private final double circleRadius; // 生成的CircleAttack的半径
    private final double circleAlertMeasures; // 生成的CircleAttack的预警时间
    private final Random random = new Random();
    
    // 场地范围：从(-7, -60, -4)到(35, -60, -46)
    private final int minX = -7;
    private final int maxX = 35;
    private final int minZ = -46;
    private final int maxZ = -4;
    private final int fieldY = -60;

    public RandomAttack(double attackDurationMeasures, World world, int circleCount, double generationSpeedMeasures, double circleRadius, double circleAlertMeasures, int bpm, Plugin plugin) {
        super(0, attackDurationMeasures, bpm); // RandomAttack本身无预警时间
        this.world = world;
        this.plugin = plugin;
        this.circleCount = circleCount;
        this.generationSpeedMeasures = generationSpeedMeasures;
        this.circleRadius = circleRadius;
        this.circleAlertMeasures = circleAlertMeasures;
        
        // 初始化攻击工厂（启用优化）
        this.attackFactory = new AttackFactory(plugin, true);
    }
    
    // Convert generation speed measures to ticks
    private int getGenerationSpeedTicks() {
        return (int) (generationSpeedMeasures * 4 * 60.0 / getBpm() * 20);
    }
    
    // Convert circle alert measures to ticks
    private int getCircleAlertTicks() {
        return (int) (circleAlertMeasures * 4 * 60.0 / getBpm() * 20);
    }

    @Override
    public void toggle() {
        // 直接开始攻击阶段，无预警时间
        startAttackPhase();
    }
    
    private void startAttackPhase() {
        // 播放攻击开始音效
        playAttackSound("block.note_block.pling");
        
        // 开始随机生成CircleAttack
        // 为了让攻击时间卡在拍子上，我们需要提前开始，提前的时间就是预警时间
        final int alertTicks = getCircleAlertTicks();
        new BukkitRunnable() {
            int ticks = -alertTicks; // 从负的预警时间开始
            final int maxTicks = getAttackDurationTicks();
            int nextGenerationTick = 0;
            
            @Override
            public void run() {
                if (ticks >= maxTicks) {
                    this.cancel();
                    return;
                }
                
                // 按照指定速度生成CircleAttack
                if (ticks >= nextGenerationTick) {
                    generateRandomCircles();
                    nextGenerationTick = ticks + getGenerationSpeedTicks();
                }
                
                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }
    
    private void generateRandomCircles() {
        for (int i = 0; i < circleCount; i++) {
            // 在场地范围内随机生成位置
            double x = minX + random.nextDouble() * (maxX - minX);
            double z = minZ + random.nextDouble() * (maxZ - minZ);
            double y = fieldY; // 使用场地的固定Y坐标
            
            Location randomLocation = new Location(world, x, y, z);
            
            // 使用AttackFactory创建CircleAttack，自动处理优化注册
            CircleAttack circleAttack = attackFactory.createCircleAttack(
                circleAlertMeasures, // 使用预警时间
                0.125, // 短攻击时间（半拍）
                randomLocation,
                (int) circleRadius,
                getBpm()
            );
            
            circleAttack.toggle();
        }
    }
    
    @Override
    public double getFirstInternalAttackStartOffset() {
        // RandomAttack的第一个内部攻击（第一个CircleAttack）在RandomAttack开始后立即生成
        // 第一个CircleAttack有自己的预警时间，所以总的偏移时间是第一个Circle的预警时间
        return circleAlertMeasures;
    }

    @Override
    protected boolean isPlayerInAttackRange(Player player) {
        // RandomAttack本身不直接造成伤害，伤害由生成的CircleAttack处理
        // 由于每个CircleAttack都是独立的，我们返回false
        return false;
    }
}