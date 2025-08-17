package mcevent.MCEFramework.games.musicDodge;

import mcevent.MCEFramework.games.musicDodge.gameObject.attacks.MCEAttack;

/**
 * 攻击配置项，包含攻击对象和同步标志
 */
public class AttackConfig {
    public final MCEAttack attack;
    public final boolean sync;
    
    public AttackConfig(MCEAttack attack, boolean sync) {
        this.attack = attack;
        this.sync = sync;
    }
}