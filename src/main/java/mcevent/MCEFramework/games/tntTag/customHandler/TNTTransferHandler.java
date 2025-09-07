package mcevent.MCEFramework.games.tntTag.customHandler;

import mcevent.MCEFramework.generalGameObject.MCEResumableEventHandler;
import static mcevent.MCEFramework.miscellaneous.Constants.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

/*
TNTTransferHandler: TNT转移事件监听器
*/
public class TNTTransferHandler extends MCEResumableEventHandler implements Listener {

    public TNTTransferHandler() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerDamage(EntityDamageByEntityEvent event) {
        if (this.isSuspended()) return;
        if (tnttag.isInTransition()) return; // 过渡阶段不允许转移

        if (!(event.getEntity() instanceof Player target)) return;
        if (!(event.getDamager() instanceof Player attacker)) return;

        // 检查攻击者是否是TNT携带者
        if (!tnttag.getTntCarriers().contains(attacker)) return;
        
        // 检查被攻击者是否是存活玩家且不是TNT携带者
        if (!tnttag.getAlivePlayers().contains(target)) return;
        if (tnttag.getTntCarriers().contains(target)) return;

        // 不取消事件，让原版击退和伤害正常发生
        // 转移TNT
        tnttag.transferTNT(attacker, target);
    }
}