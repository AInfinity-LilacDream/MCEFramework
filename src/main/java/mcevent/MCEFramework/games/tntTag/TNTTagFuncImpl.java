package mcevent.MCEFramework.games.tntTag;

import static mcevent.MCEFramework.miscellaneous.Constants.*;

import mcevent.MCEFramework.tools.MCEMessenger;
import mcevent.MCEFramework.tools.MCETimerUtils;
import org.bukkit.*;
import org.bukkit.entity.Player;

/*
TNTTagFuncImpl: 封装TNTTag游戏逻辑函数
*/
public class TNTTagFuncImpl {

    // 爆炸TNT携带者
    protected static void explodeTNTCarrier(Player player) {
        Location location = player.getLocation();
        World world = location.getWorld();
        
        if (world != null) {
            // 播放爆炸声音
            world.playSound(location, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 1.0f);
            
            // 生成爆炸粒子效果
            world.spawnParticle(Particle.EXPLOSION, location, 10, 1.0, 1.0, 1.0, 0.1);
            world.spawnParticle(Particle.LAVA, location, 20, 2.0, 2.0, 2.0, 0.1);
            world.spawnParticle(Particle.FLAME, location, 30, 1.5, 1.5, 1.5, 0.1);
        }
        
        // 清除头盔
        player.getInventory().setHelmet(null);
        
        MCEMessenger.sendInfoToPlayer("<red>[💥] 你爆炸了！</red>", player);
    }
    
    // 发送最终结果
    protected static void sendFinalResults() {
        if (tnttag.getAlivePlayers().size() == 1) {
            Player winner = tnttag.getAlivePlayers().get(0);
            MCEMessenger.sendGlobalTitle("<gold><bold>🎉 游戏结束！ 🎉</bold></gold>", 
                                       "<yellow>胜利者：" + winner.getName() + "</yellow>");
        } else {
            MCEMessenger.sendGlobalTitle("<gold><bold>🎉 游戏结束！ 🎉</bold></gold>", 
                                       "<yellow>没有胜利者</yellow>");
        }
        
        // 延迟显示详细统计
        MCETimerUtils.setDelayedTask(5, () -> {
            MCEMessenger.sendGlobalText("<newline><gold><bold>=== 丢锅大战 结果统计 ===</bold></gold>");
            
            if (tnttag.getAlivePlayers().size() == 1) {
                MCEMessenger.sendGlobalText("<green><bold>🏆 胜利者：" + tnttag.getAlivePlayers().get(0).getName() + "</bold></green>");
            }
            
            MCEMessenger.sendGlobalText("<newline><red><bold>💀 死亡顺序：</bold></red>");
            for (int i = 0; i < tnttag.getDeathOrder().size(); i++) {
                String playerName = tnttag.getDeathOrder().get(i);
                int position = tnttag.getDeathOrder().size() - i + 1; // 倒序排名
                MCEMessenger.sendGlobalText("<gray>" + position + ". " + playerName + "</gray>");
            }
            
            MCEMessenger.sendGlobalText("<newline><aqua><bold>📊 游戏统计：</bold></aqua>");
            MCEMessenger.sendGlobalText("<yellow>总阶段数：" + tnttag.getCurrentPhase() + "</yellow>");
            MCEMessenger.sendGlobalText("<yellow>参与玩家：" + (tnttag.getAlivePlayers().size() + tnttag.getDeathOrder().size()) + "</yellow>");
        });
    }
}