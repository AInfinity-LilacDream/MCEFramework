package mcevent.MCEFramework.games.hyperSpleef.customHandler;

import mcevent.MCEFramework.games.hyperSpleef.HyperSpleef;
import mcevent.MCEFramework.generalGameObject.MCEResumableEventHandler;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;

import static mcevent.MCEFramework.miscellaneous.Constants.plugin;

/*
CraftingDisableHandler: 超级掘一死战合成禁用处理器
*/
public class CraftingDisableHandler extends MCEResumableEventHandler implements Listener {

    private HyperSpleef hyperSpleef;

    public void register(HyperSpleef game) {
        this.hyperSpleef = game;
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
        if (isSuspended())
            return;

        ItemStack result = event.getRecipe().getResult();

        // 禁用雪球合成雪块
        if (result.getType() == Material.SNOW_BLOCK) {
            event.setCancelled(true);
            return;
        }
    }

    @EventHandler
    public void onPrepareItemCraft(PrepareItemCraftEvent event) {
        if (isSuspended())
            return;

        ItemStack result = event.getRecipe() != null ? event.getRecipe().getResult() : null;
        if (result == null)
            return;

        // 禁用雪球合成雪块的预览
        if (result.getType() == Material.SNOW_BLOCK) {
            event.getInventory().setResult(null);
        }
    }
}
