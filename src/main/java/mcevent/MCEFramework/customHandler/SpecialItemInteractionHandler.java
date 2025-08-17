package mcevent.MCEFramework.customHandler;

import mcevent.MCEFramework.generalGameObject.MCEResumableEventHandler;
import mcevent.MCEFramework.generalGameObject.MCESpecialItem;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

import static mcevent.MCEFramework.miscellaneous.Constants.plugin;

/**
 * SpecialItemInteractionHandler: 特殊物品交互事件监听器
 * 处理所有继承 MCESpecialItem 的物品的右键交互事件
 */
public class SpecialItemInteractionHandler extends MCEResumableEventHandler implements Listener {
    
    private final List<MCESpecialItem> registeredItems = new ArrayList<>();
    
    public SpecialItemInteractionHandler() {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }
    
    /**
     * 注册特殊物品
     * @param item 要注册的特殊物品
     */
    public void registerItem(MCESpecialItem item) {
        registeredItems.add(item);
    }
    
    /**
     * 移除特殊物品
     * @param item 要移除的特殊物品
     */
    public void unregisterItem(MCESpecialItem item) {
        registeredItems.remove(item);
    }
    
    /**
     * 清除所有注册的物品
     */
    public void clearAllItems() {
        registeredItems.clear();
    }
    
    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        // 如果监听器被暂停，不处理事件
        if (this.isSuspended()) return;
        
        Player player = event.getPlayer();
        
        // 尝试让所有注册的特殊物品处理这个交互事件
        for (MCESpecialItem item : registeredItems) {
            if (item.handleRightClick(event, player)) {
                // 如果有物品成功处理了事件，就停止处理
                break;
            }
        }
    }
}