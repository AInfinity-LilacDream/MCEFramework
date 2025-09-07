package mcevent.MCEFramework.games.spleef.customHandler;

import mcevent.MCEFramework.games.spleef.Spleef;
import mcevent.MCEFramework.games.spleef.SpleefFuncImpl;
import mcevent.MCEFramework.generalGameObject.MCEResumableEventHandler;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

import static mcevent.MCEFramework.miscellaneous.Constants.plugin;

/*
SnowBreakHandler: 冰雪掘战雪块破坏处理器
*/
public class SnowBreakHandler extends MCEResumableEventHandler implements Listener {
    
    private Spleef spleef;
    
    public void register(Spleef game) {
        this.spleef = game;
        setSuspended(true); // 默认挂起，游戏开始时启动
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
    
    @Override
    public void start() {
        setSuspended(false);
    }
    
    @Override
    public void suspend() {
        setSuspended(true);
    }
    
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (isSuspended()) return;
        
        Player player = event.getPlayer();
        Material brokenBlock = event.getBlock().getType();
        
        // 检查玩家是否在游戏中
        if (!player.getScoreboardTags().contains("Active") || 
            player.getScoreboardTags().contains("dead")) {
            return;
        }
        
        // 检查是否在游戏世界中
        if (!player.getWorld().getName().equals(spleef.getWorldName())) {
            return;
        }
        
        // 只允许破坏雪块和雪层
        if (brokenBlock != Material.SNOW_BLOCK && brokenBlock != Material.SNOW) {
            event.setCancelled(true);
            return;
        }
        
        // 检查是否使用金铲子
        Material toolInHand = player.getInventory().getItemInMainHand().getType();
        if (toolInHand != Material.GOLDEN_SHOVEL) {
            event.setCancelled(true);
            player.sendMessage("§c只能使用金铲子挖雪！");
            return;
        }
        
        // 处理雪块破坏
        SpleefFuncImpl.handleSnowBreak(player, brokenBlock);
    }
}