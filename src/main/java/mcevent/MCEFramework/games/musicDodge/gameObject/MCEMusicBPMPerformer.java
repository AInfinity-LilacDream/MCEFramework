package mcevent.MCEFramework.games.musicDodge.gameObject;

import lombok.Getter;
import lombok.Setter;
import mcevent.MCEFramework.games.musicDodge.gameObject.attacks.MCEAttack;
import mcevent.MCEFramework.tools.MCEPlayerUtils;
import mcevent.MCEFramework.tools.MCETimerUtils;
import mcevent.MCEFramework.tools.MCETimerFunction;

import java.util.ArrayList;
import java.util.List;

import mcevent.MCEFramework.games.musicDodge.AttackConfig;

/*
MCEMusicBPMPerformer: 按照小节划分节拍并播放谱面
 */
@Getter @Setter
public class MCEMusicBPMPerformer {
    private String musicPath;
    private int BPM;

    private ArrayList<MCEAttack> attacks = new ArrayList<>();

    public MCEMusicBPMPerformer(String musicPath, int BPM) {
        this.musicPath = musicPath;
        this.BPM = BPM;
    }

    public void play() {
        MCEPlayerUtils.globalPlaySound(musicPath);
        
        // 依次触发所有攻击
        scheduleAttacks();
    }
    
    public void playWithSync(List<AttackConfig> attackConfigs) {
        MCEPlayerUtils.globalPlaySound(musicPath);
        
        // 使用同步信息触发攻击
        scheduleAttacksWithSync(attackConfigs);
    }

    public void stop() {
        MCEPlayerUtils.globalStopMusic();
    }

    public void loadAttack(MCEAttack attack) {
        attacks.add(attack);
    }
    
    public void loadAttackConfigs(List<AttackConfig> attackConfigs) {
        attacks.clear();
        for (AttackConfig config : attackConfigs) {
            attacks.add(config.attack);
        }
        // 不在这里调度攻击，由playWithSync调用
    }
    
    /**
     * 按顺序调度所有攻击
     * 规则：忽略预警时间，在一个攻击的攻击时间结束后立即触发下一个
     * 预警转攻击的时刻卡在拍子上，需要提前触发
     */
    private void scheduleAttacks() {
        double currentDelay = 0.0; // 当前累积延迟（秒）
        
        for (int i = 0; i < attacks.size(); i++) {
            MCEAttack attack = attacks.get(i);
            
            // 计算这个攻击的预警时间和攻击时间（转换为秒）
            double alertDurationSeconds = attack.getAlertDurationBeats() * 4 * 60.0 / BPM;
            double attackDurationSeconds = attack.getAttackDurationBeats() * 4 * 60.0 / BPM;
            
            // 为了让攻击时间卡在拍子上，提前开始（提前预警时间）
            double triggerDelay = currentDelay - alertDurationSeconds;
            
            // 确保延迟不为负数
            if (triggerDelay < 0) {
                triggerDelay = 0;
            }
            
            // 设置定时任务触发攻击
            final MCEAttack currentAttack = attack;
            MCETimerUtils.setDelayedTask(triggerDelay, new MCETimerFunction() {
                @Override
                public void run() {
                    currentAttack.toggle();
                }
            });
            
            // 更新下一个攻击的开始时间：当前攻击的攻击时间结束后
            // 注意：这里只考虑攻击时间，忽略预警时间
            currentDelay += attackDurationSeconds;
        }
    }
    
    /**
     * 按顺序调度所有攻击（支持时间偏移）
     * 规则：每个攻击在上一个攻击从预警转为攻击之后指定的时间偏移后开始从预警转为攻击
     * @param attackConfigs 攻击配置列表
     */
    private void scheduleAttacksWithSync(List<AttackConfig> attackConfigs) {
        if (attackConfigs.isEmpty()) return;
        
        // 找到所有攻击的最早开始时间，确保没有负数延迟
        double earliestStartTime = 0.0;
        double lastAttackStartTime = 0.0;
        
        // 第一次遍历：计算所有时间点
        List<Double> attackStartTimes = new ArrayList<>();
        List<Double> triggerTimes = new ArrayList<>();
        
        for (int i = 0; i < attackConfigs.size(); i++) {
            AttackConfig config = attackConfigs.get(i);
            MCEAttack attack = config.attack;
            double attackStartOffsetBeats = config.attackStartOffset;
            double attackStartOffsetSeconds = attackStartOffsetBeats * 4 * 60.0 / BPM;
            double firstInternalAttackStartOffsetSeconds = attack.getFirstInternalAttackStartOffset() * 4 * 60.0 / BPM;
            
            double actualAttackStartTime;
            double triggerTime;
            
            if (i == 0) {
                // 第一个攻击：第一个内部攻击在指定时间转为攻击
                actualAttackStartTime = firstInternalAttackStartOffsetSeconds;
                triggerTime = 0.0;
            } else {
                // 后续攻击：在上一个攻击的第一个内部攻击转为攻击后，加上指定偏移时间
                actualAttackStartTime = lastAttackStartTime + attackStartOffsetSeconds;
                triggerTime = actualAttackStartTime - firstInternalAttackStartOffsetSeconds;
            }
            
            attackStartTimes.add(actualAttackStartTime);
            triggerTimes.add(triggerTime);
            lastAttackStartTime = actualAttackStartTime;
            
            // 更新最早开始时间
            if (triggerTime < earliestStartTime) {
                earliestStartTime = triggerTime;
            }
        }
        
        // 第二次遍历：调度所有攻击，调整负数延迟
        for (int i = 0; i < attackConfigs.size(); i++) {
            MCEAttack attack = attackConfigs.get(i).attack;
            double adjustedTriggerDelay = triggerTimes.get(i) - earliestStartTime;
            
            // 设置定时任务触发攻击
            final MCEAttack currentAttack = attack;
            MCETimerUtils.setDelayedTask(adjustedTriggerDelay, new MCETimerFunction() {
                @Override
                public void run() {
                    currentAttack.toggle();
                }
            });
        }
    }
    
}