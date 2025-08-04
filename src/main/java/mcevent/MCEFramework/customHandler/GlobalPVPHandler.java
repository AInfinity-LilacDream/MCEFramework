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

        if (event.getDamager() instanceof Player && event.getEntity() instanceof Player)
            event.setCancelled(true);
    }
}
