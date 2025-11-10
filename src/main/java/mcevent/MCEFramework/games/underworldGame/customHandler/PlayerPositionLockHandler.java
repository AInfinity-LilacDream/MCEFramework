package mcevent.MCEFramework.games.underworldGame.customHandler;

import mcevent.MCEFramework.generalGameObject.MCEResumableEventHandler;
import mcevent.MCEFramework.generalGameObject.MCEGame;
import mcevent.MCEFramework.MCEMainController;
import mcevent.MCEFramework.games.underworldGame.UnderworldGame;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import static mcevent.MCEFramework.miscellaneous.Constants.plugin;

/*
PlayerPositionLockHandler: 玩家位置锁定处理器（用于准备和intro阶段锁定玩家位置）
 */
public class PlayerPositionLockHandler extends MCEResumableEventHandler implements Listener {

    public PlayerPositionLockHandler() {
        setSuspended(true); // 默认不锁定
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (isSuspended()) return;
        if (!MCEMainController.isRunningGame()) return;
        
        MCEGame current = MCEMainController.getCurrentRunningGame();
        if (!(current instanceof UnderworldGame)) return;

        Player player = event.getPlayer();
        UnderworldGame game = (UnderworldGame) current;

        // 检查是否在游戏世界中
        if (!player.getWorld().getName().equals(game.getGeneratedWorldName()))
            return;

        // 检查是否有实际位置变化（忽略视角转动）
        if (event.getFrom().getX() != event.getTo().getX() ||
            event.getFrom().getY() != event.getTo().getY() ||
            event.getFrom().getZ() != event.getTo().getZ()) {

            // 取消移动，将玩家拉回原位置
            event.setCancelled(true);
        }
    }
}

