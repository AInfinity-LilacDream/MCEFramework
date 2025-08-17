package mcevent.MCEFramework.games.musicDodge.gameObject.attacks;

import org.bukkit.entity.Player;

/**
 * VoidAttack - 空攻击，用于模拟音乐中的休息间隔
 * 没有任何粒子效果，没有判定范围，仅用于占用时间
 */
public class VoidAttack extends MCEAttack {
    
    public VoidAttack(double attackDurationMeasures, int bpm) {
        super(0, attackDurationMeasures, bpm); // 预警时间为0
    }

    @Override
    public void toggle() {
        // 空攻击什么都不做，仅占用时间
        // 攻击时间会通过MCEMusicBPMPerformer的调度系统自动计算
    }

    @Override
    protected boolean isPlayerInAttackRange(Player player) {
        // 空攻击没有判定范围，永远不会伤害玩家
        return false;
    }
}