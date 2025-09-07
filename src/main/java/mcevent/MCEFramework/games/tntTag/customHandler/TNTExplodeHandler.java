package mcevent.MCEFramework.games.tntTag.customHandler;

import mcevent.MCEFramework.generalGameObject.MCEResumableEventHandler;
import static mcevent.MCEFramework.miscellaneous.Constants.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.entity.Player;

/*
TNTExplodeHandler: TNT爆炸相关事件监听器
*/
public class TNTExplodeHandler extends MCEResumableEventHandler implements Listener {

    public TNTExplodeHandler() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onEntityExplode(EntityExplodeEvent event) {
        if (this.isSuspended()) return;
        
        // 防止实际的方块破坏
        event.blockList().clear();
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageEvent event) {
        if (this.isSuspended()) return;
        
        if (event.getEntity() instanceof Player) {
            // 只允许玩家对玩家的伤害（PVP），禁用其他所有伤害源
            if (!(event instanceof EntityDamageByEntityEvent)) {
                event.setCancelled(true);
            } else {
                EntityDamageByEntityEvent pvpEvent = (EntityDamageByEntityEvent) event;
                // 如果不是玩家攻击玩家，则取消伤害
                if (!(pvpEvent.getDamager() instanceof Player)) {
                    event.setCancelled(true);
                }
            }
        }
    }
}