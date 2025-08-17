package mcevent.MCEFramework.games.musicDodge.gameObject.attacks;

import mcevent.MCEFramework.games.musicDodge.AttackFactory;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Random;

public class RandomLaserAttack extends MCEAttack {
    private final World world;
    private final Plugin plugin;
    private final AttackFactory attackFactory;
    private final int laserCount; // 每次攻击生成几个LaserAttack
    private final double generationSpeedMeasures; // 生成速度（每多少小节生成一次）
    private final double laserAlertMeasures; // 生成的LaserAttack的预警时间
    private final Random random = new Random();
    
    // 场地范围：从(-7, -60, -4)到(35, -60, -46)，使用游戏坐标系(0,0)到(42,42)
    private static final int FIELD_SIZE = 43;

    public RandomLaserAttack(double attackDurationMeasures, World world, int laserCount, double generationSpeedMeasures, double laserAlertMeasures, int bpm, Plugin plugin) {
        super(0, attackDurationMeasures, bpm); // RandomLaserAttack本身无预警时间
        this.world = world;
        this.plugin = plugin;
        this.laserCount = laserCount;
        this.generationSpeedMeasures = generationSpeedMeasures;
        this.laserAlertMeasures = laserAlertMeasures;
        
        // 初始化攻击工厂（启用优化）
        this.attackFactory = new AttackFactory(plugin, true);
    }
    
    // Convert generation speed measures to ticks
    private int getGenerationSpeedTicks() {
        return (int) (generationSpeedMeasures * 4 * 60.0 / getBpm() * 20);
    }
    
    // Convert laser alert measures to ticks
    private int getLaserAlertTicks() {
        return (int) (laserAlertMeasures * 4 * 60.0 / getBpm() * 20);
    }

    @Override
    public void toggle() {
        // 直接开始攻击阶段，无预警时间
        startAttackPhase();
    }
    
    private void startAttackPhase() {
        // 播放攻击开始音效
        playAttackSound("block.note_block.pling");
        
        // 开始随机生成LaserAttack
        // 为了让攻击时间卡在拍子上，我们需要提前开始，提前的时间就是预警时间
        final int alertTicks = getLaserAlertTicks();
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
                
                // 按照指定速度生成LaserAttack
                if (ticks >= nextGenerationTick) {
                    generateRandomLasers();
                    nextGenerationTick = ticks + getGenerationSpeedTicks();
                }
                
                ticks++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }
    
    private void generateRandomLasers() {
        for (int i = 0; i < laserCount; i++) {
            // 生成两个在场地边缘的随机端点
            int[] startPoint = generateEdgePoint();
            int[] endPoint = generateEdgePoint();
            
            int startX = startPoint[0];
            int startY = startPoint[1];
            int endX = endPoint[0];
            int endY = endPoint[1];
            
            // 使用AttackFactory创建LaserAttack，自动处理优化注册
            LaserAttack laserAttack = attackFactory.createLaserAttack(
                laserAlertMeasures, // 使用预警时间
                0.125, // 短攻击时间（半拍）
                startX, startY,
                endX, endY,
                world,
                getBpm()
            );
            
            laserAttack.toggle();
        }
    }
    
    // 生成一个在场地边缘的随机点
    private int[] generateEdgePoint() {
        // 场地边缘有四条边：上(y=0), 下(y=42), 左(x=0), 右(x=42)
        int edge = random.nextInt(4); // 0=上, 1=下, 2=左, 3=右
        
        switch (edge) {
            case 0: // 上边缘 y=0
                return new int[]{random.nextInt(FIELD_SIZE), 0};
            case 1: // 下边缘 y=42
                return new int[]{random.nextInt(FIELD_SIZE), FIELD_SIZE - 1};
            case 2: // 左边缘 x=0
                return new int[]{0, random.nextInt(FIELD_SIZE)};
            case 3: // 右边缘 x=42
                return new int[]{FIELD_SIZE - 1, random.nextInt(FIELD_SIZE)};
            default:
                return new int[]{0, 0}; // 默认返回左上角
        }
    }

    @Override
    protected boolean isPlayerInAttackRange(Player player) {
        // RandomLaserAttack本身不直接造成伤害，伤害由生成的LaserAttack处理
        // 由于每个LaserAttack都是独立的，我们返回false
        return false;
    }
}