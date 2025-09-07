package mcevent.MCEFramework.customHandler;

import mcevent.MCEFramework.generalGameObject.MCEResumableEventHandler;
import static mcevent.MCEFramework.miscellaneous.Constants.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/*
GlobalPVPHandler: 玩家PVP事件监听器
 */
public class GlobalPVPHandler extends MCEResumableEventHandler implements Listener {

    public GlobalPVPHandler() {
        setSuspended(true);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerAttack(EntityDamageByEntityEvent event) {
        if (isSuspended()) return;

        boolean isPlayerVsPlayer = false;
        
        // 检查直接玩家对玩家攻击
        if (event.getDamager() instanceof Player && event.getEntity() instanceof Player) {
            isPlayerVsPlayer = true;
        }
        // 检查投射物攻击（如雪球）
        else if (event.getDamager() instanceof org.bukkit.entity.Projectile projectile && event.getEntity() instanceof Player) {
            if (projectile.getShooter() instanceof Player) {
                // 检查是否是Spleef游戏中的雪球伤害，如果是则不干扰
                if (event.getEntity().hasMetadata("spleef_snowball_damage")) {
                    plugin.getLogger().info("调试 - GlobalPVPHandler检测到Spleef雪球伤害，不干扰");
                    return;
                }
                isPlayerVsPlayer = true;
                plugin.getLogger().info("调试 - GlobalPVPHandler检测到投射物攻击，PVP状态: suspended=" + isSuspended());
            }
        }
        
        if (isPlayerVsPlayer) {
            plugin.getLogger().info("调试 - GlobalPVPHandler取消PVP攻击");
            event.setCancelled(true);
        }
    }
}
