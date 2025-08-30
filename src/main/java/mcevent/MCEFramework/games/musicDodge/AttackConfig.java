package mcevent.MCEFramework.games.musicDodge;

import mcevent.MCEFramework.games.musicDodge.gameObject.attacks.MCEAttack;

/**
 * 攻击配置项，包含攻击对象和时间偏移
 */
public class AttackConfig {
    public final MCEAttack attack;
    public final double attackStartOffset; // 在上一个攻击从预警转为攻击之后多久，这个攻击从预警转为攻击（以拍为单位）
    
    public AttackConfig(MCEAttack attack, double attackStartOffset) {
        this.attack = attack;
        this.attackStartOffset = attackStartOffset;
    }
}