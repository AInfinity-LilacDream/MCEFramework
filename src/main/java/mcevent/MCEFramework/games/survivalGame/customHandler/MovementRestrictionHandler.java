package mcevent.MCEFramework.games.survivalGame.customHandler;

import mcevent.MCEFramework.generalGameObject.MCEResumableEventHandler;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import static mcevent.MCEFramework.miscellaneous.Constants.*;

/*
MovementRestrictionHandler: 移动限制处理器（用于开始前10秒禁止移动）
 */
public class MovementRestrictionHandler extends MCEResumableEventHandler implements Listener {

    public MovementRestrictionHandler() {
        setSuspended(true); // 默认不限制移动
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (isSuspended()) return;

        Player player = event.getPlayer();

        // 检查是否有实际位置变化（忽略视角转动）
        if (event.getFrom().getX() != event.getTo().getX() ||
            event.getFrom().getY() != event.getTo().getY() ||
            event.getFrom().getZ() != event.getTo().getZ()) {

            // 取消移动，将玩家拉回原位置
            event.setCancelled(true);
        }
    }
}
