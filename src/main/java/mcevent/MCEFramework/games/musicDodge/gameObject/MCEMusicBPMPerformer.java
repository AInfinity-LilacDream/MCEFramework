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
        MCEPlayerUtils.globalPlaySound("minecraft.music_dodge_why_do_i");
        
        // 依次触发所有攻击
        scheduleAttacks();
    }
    
    public void playWithSync(List<AttackConfig> attackConfigs) {
        MCEPlayerUtils.globalPlaySound("minecraft.music_dodge_why_do_i");
        
        // 使用同步信息触发攻击
        scheduleAttacksWithSync(attackConfigs);
    }

    public void stop() {

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
     * 按顺序调度所有攻击（支持同步）
     * 规则：标记为sync的攻击与前一个攻击同时触发
     * @param attackConfigs 攻击配置列表
     */
    private void scheduleAttacksWithSync(List<AttackConfig> attackConfigs) {
        double currentDelay = 0.0; // 当前累积延迟（秒）
        double lastNonSyncStartTime = 0.0; // 最后一个非同步攻击的开始时间
        
        for (int i = 0; i < attackConfigs.size(); i++) {
            AttackConfig config = attackConfigs.get(i);
            MCEAttack attack = config.attack;
            boolean sync = config.sync;
            
            // 计算这个攻击的预警时间和攻击时间（转换为秒）
            double alertDurationSeconds = attack.getAlertDurationBeats() * 4 * 60.0 / BPM;
            double attackDurationSeconds = attack.getAttackDurationBeats() * 4 * 60.0 / BPM;
            
            double triggerDelay;
            
            if (!sync || i == 0) {
                // 非同步攻击或第一个攻击：在当前时间点开始预警
                triggerDelay = currentDelay;
                
                // 记录这个非同步攻击的预警开始时间
                lastNonSyncStartTime = triggerDelay;
                
                // 推进时间到此攻击完全结束后
                currentDelay = triggerDelay + alertDurationSeconds + attackDurationSeconds;
            } else {
                // 同步攻击：与最后一个非同步攻击同时开始预警阶段
                triggerDelay = lastNonSyncStartTime;
                
                // 如果同步攻击比主攻击长，需要延长总时间
                double syncAttackEndTime = triggerDelay + alertDurationSeconds + attackDurationSeconds;
                if (syncAttackEndTime > currentDelay) {
                    currentDelay = syncAttackEndTime;
                }
            }
            
            // 设置定时任务触发攻击
            final MCEAttack currentAttack = attack;
            MCETimerUtils.setDelayedTask(triggerDelay, new MCETimerFunction() {
                @Override
                public void run() {
                    currentAttack.toggle();
                }
            });
        }
    }
    
}