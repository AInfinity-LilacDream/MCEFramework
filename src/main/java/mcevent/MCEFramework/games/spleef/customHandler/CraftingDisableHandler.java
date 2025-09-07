package mcevent.MCEFramework.games.spleef.customHandler;

import mcevent.MCEFramework.games.spleef.Spleef;
import mcevent.MCEFramework.generalGameObject.MCEResumableEventHandler;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;

import static mcevent.MCEFramework.miscellaneous.Constants.plugin;

/*
CraftingDisableHandler: 冰雪掘战合成禁用处理器
*/
public class CraftingDisableHandler extends MCEResumableEventHandler implements Listener {
    
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
    public void onCraftItem(CraftItemEvent event) {
        if (isSuspended()) return;
        
        ItemStack result = event.getRecipe().getResult();
        
        // 禁用雪球合成雪块
        if (result.getType() == Material.SNOW_BLOCK) {
            event.setCancelled(true);
            plugin.getLogger().info("调试 - 禁用雪球合成雪块");
            return;
        }
        
        // 禁用旋风棒分解风弹（如果有这样的合成）
        if (result.getType() == Material.WIND_CHARGE) {
            event.setCancelled(true);
            plugin.getLogger().info("调试 - 禁用旋风棒分解风弹");
            return;
        }
    }
    
    @EventHandler
    public void onPrepareItemCraft(PrepareItemCraftEvent event) {
        if (isSuspended()) return;
        
        ItemStack result = event.getRecipe() != null ? event.getRecipe().getResult() : null;
        if (result == null) return;
        
        // 禁用雪球合成雪块的预览
        if (result.getType() == Material.SNOW_BLOCK) {
            event.getInventory().setResult(null);
            plugin.getLogger().info("调试 - 禁用雪球合成雪块预览");
        }
        
        // 禁用旋风棒分解风弹的预览
        if (result.getType() == Material.WIND_CHARGE) {
            event.getInventory().setResult(null);
            plugin.getLogger().info("调试 - 禁用旋风棒分解风弹预览");
        }
    }
}