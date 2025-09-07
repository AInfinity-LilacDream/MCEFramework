package mcevent.MCEFramework.customHandler;

import mcevent.MCEFramework.generalGameObject.MCEResumableEventHandler;
import mcevent.MCEFramework.tools.MCETeamUtils;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.scoreboard.Team;

import static mcevent.MCEFramework.miscellaneous.Constants.*;

/*
FriendlyFireHandler: 友伤系统事件监听器
 */
public class FriendlyFireHandler extends MCEResumableEventHandler implements Listener {

    public FriendlyFireHandler() {
        setSuspended(false); // 默认关闭友伤（禁止同队伤害）
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onTeamMemberAttack(EntityDamageByEntityEvent event) {
        Player damager = null;
        Player victim = null;
        
        // 处理玩家对玩家的直接伤害
        if (event.getDamager() instanceof Player && event.getEntity() instanceof Player) {
            damager = (Player) event.getDamager();
            victim = (Player) event.getEntity();
        }
        // 处理投射物（如雪球）的伤害
        else if (event.getDamager() instanceof org.bukkit.entity.Projectile projectile && event.getEntity() instanceof Player) {
            if (projectile.getShooter() instanceof Player) {
                damager = (Player) projectile.getShooter();
                victim = (Player) event.getEntity();
                plugin.getLogger().info("调试 - FriendlyFireHandler检测到投射物伤害: " + damager.getName() + " -> " + victim.getName());
            }
        }
        
        if (damager == null || victim == null) {
            return;
        }

        // 获取攻击者和受害者的队伍
        Team damagerTeam = MCETeamUtils.getTeam(damager);
        Team victimTeam = MCETeamUtils.getTeam(victim);

        // 如果两个玩家在同一队伍
        if (damagerTeam != null && victimTeam != null && damagerTeam.equals(victimTeam)) {
            if (!isSuspended()) {
                // 友伤被关闭（suspended=false），阻止同队伤害
                event.setCancelled(true);
            }
            // 如果友伤被激活（suspended=true），则允许同队伤害，不做任何操作
        }
        
        // 不同队伍或无队伍的情况下，让全局PVP处理器来决定是否允许PVP
    }
}