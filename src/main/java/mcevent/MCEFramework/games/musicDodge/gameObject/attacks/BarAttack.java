package mcevent.MCEFramework.games.musicDodge.gameObject.attacks;

import mcevent.MCEFramework.games.musicDodge.AttackFactory;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

public class BarAttack extends MCEAttack {
    private final String direction; // "x" for left-right, "y" for top-bottom
    private final World world;
    private final Plugin plugin;
    private final AttackFactory attackFactory;
    private final int spacing; // 每隔多少格生成一条激光
    private final List<LaserAttack> lasers = new ArrayList<>();
    
    // 场地范围：从(-7, -60, -4)到(35, -60, -46)
    private static final int FIELD_MIN_X = -7;
    private static final int FIELD_MAX_X = 35;
    private static final int FIELD_MIN_Z = -46;
    private static final int FIELD_MAX_Z = -4;
    private static final int FIELD_SIZE = 43;

    public BarAttack(double alertDurationMeasures, double attackDurationMeasures, String direction, int spacing, World world, int bpm, Plugin plugin, AttackFactory attackFactory) {
        super(alertDurationMeasures, attackDurationMeasures, bpm);
        this.direction = direction;
        this.world = world;
        this.plugin = plugin;
        this.spacing = spacing;
        this.attackFactory = attackFactory;
        
        generateLasers();
    }
    
    private void generateLasers() {
        if (direction.equals("x")) {
            // 从左到右：生成多条从上到下的激光
            for (int x = 0; x < FIELD_SIZE; x += spacing) {
                LaserAttack laser = attackFactory.createLaserAttack(
                    getAlertDurationBeats(), 
                    getAttackDurationBeats(),
                    x, 0,           // 起点：(x, 0) 场地顶部
                    x, FIELD_SIZE-1, // 终点：(x, 42) 场地底部
                    world, 
                    getBpm()
                );
                lasers.add(laser);
            }
        } else if (direction.equals("y")) {
            // 从上到下：生成多条从左到右的激光
            for (int y = 0; y < FIELD_SIZE; y += spacing) {
                LaserAttack laser = attackFactory.createLaserAttack(
                    getAlertDurationBeats(), 
                    getAttackDurationBeats(),
                    0, y,           // 起点：(0, y) 场地左侧
                    FIELD_SIZE-1, y, // 终点：(42, y) 场地右侧
                    world, 
                    getBpm()
                );
                lasers.add(laser);
            }
        }
    }

    @Override
    public void toggle() {
        // 同时启动所有激光
        for (LaserAttack laser : lasers) {
            laser.toggle();
        }
    }

    @Override
    protected boolean isPlayerInAttackRange(Player player) {
        // 检查玩家是否在任何一条激光的攻击范围内
        for (LaserAttack laser : lasers) {
            if (laser.isPlayerInAttackRange(player)) {
                return true;
            }
        }
        return false;
    }
}