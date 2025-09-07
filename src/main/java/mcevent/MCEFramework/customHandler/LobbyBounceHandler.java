package mcevent.MCEFramework.customHandler;

import mcevent.MCEFramework.generalGameObject.MCEResumableEventHandler;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;

import static mcevent.MCEFramework.miscellaneous.Constants.plugin;

/**
 * LobbyBounceHandler: 处理主城中玩家Y轴过低时的弹射效果
 * 当玩家在主城（lobby世界）中Y轴降到190以下时，自动弹射到空中
 */
public class LobbyBounceHandler extends MCEResumableEventHandler implements Listener {
    
    private static final double MIN_Y_LEVEL = 190.0;
    private static final double BOUNCE_VELOCITY = 3.5; // 大约70格高度的弹射力度
    
    public LobbyBounceHandler() {
        setSuspended(false); // 始终启用
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
    
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (isSuspended()) return;
        
        Player player = event.getPlayer();
        Location location = player.getLocation();
        
        // 检查是否在主城世界
        if (location.getWorld() == null || !location.getWorld().getName().equals("lobby")) {
            return;
        }
        
        // 检查Y轴高度是否低于阈值
        if (location.getY() < MIN_Y_LEVEL) {
            // 检查玩家是否已经在向上移动，避免重复触发
            Vector velocity = player.getVelocity();
            if (velocity.getY() > 0) {
                return; // 玩家已经在向上移动，不再触发弹射
            }
            
            // 弹射玩家到空中
            bouncePlayer(player);
        }
    }
    
    /**
     * 弹射玩家到空中
     */
    private void bouncePlayer(Player player) {
        // 设置向上的速度向量
        Vector velocity = new Vector(0, BOUNCE_VELOCITY, 0);
        player.setVelocity(velocity);
        
        // 播放风爆音效
        player.playSound(player.getLocation(), Sound.ENTITY_WIND_CHARGE_THROW, 1.0f, 1.0f);
        
        plugin.getLogger().info("玩家 " + player.getName() + " 在主城Y=" + 
                               String.format("%.1f", player.getLocation().getY()) + " 被弹射到空中");
    }
}