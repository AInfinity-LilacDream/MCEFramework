package mcevent.MCEFramework.games.survivalGame.customHandler;

import mcevent.MCEFramework.MCEMainController;
import mcevent.MCEFramework.games.survivalGame.SurvivalGame;
import mcevent.MCEFramework.generalGameObject.MCEResumableEventHandler;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.AnvilInventory;

import static mcevent.MCEFramework.miscellaneous.Constants.*;

/*
AnvilDurabilityHandler: 在饥饿游戏中禁用铁砧损耗
- 当玩家完成附魔并取出成品时，强制保持铁砧不损坏
*/
public class AnvilDurabilityHandler extends MCEResumableEventHandler implements Listener {

    public AnvilDurabilityHandler() {
        setSuspended(true);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    private boolean isActiveInSG() {
        if (isSuspended())
            return false;
        if (!MCEMainController.isRunningGame())
            return false;
        return MCEMainController.getCurrentRunningGame() instanceof SurvivalGame;
    }

    // 记录最近一次 PrepareAnvilEvent 的铁砧方块与其原始类型
    private org.bukkit.block.Block lastAnvilBlock;
    private Material lastAnvilOriginalType;

    @EventHandler
    public void onPrepareAnvil(PrepareAnvilEvent event) {
        if (!isActiveInSG())
            return;
        Block b = event.getView().getTopInventory().getLocation() != null
                ? event.getView().getTopInventory().getLocation().getBlock()
                : null;
        if (b != null && (b.getType() == Material.ANVIL || b.getType() == Material.CHIPPED_ANVIL
                || b.getType() == Material.DAMAGED_ANVIL)) {
            lastAnvilBlock = b;
            lastAnvilOriginalType = b.getType();
        }
    }

    @EventHandler
    public void onTakeFromAnvil(InventoryClickEvent event) {
        if (!isActiveInSG())
            return;
        if (!(event.getInventory() instanceof AnvilInventory))
            return;
        if (event.getRawSlot() != 2)
            return; // 仅结果槽
        if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR)
            return;

        // 禁用铁砧损耗：强制恢复为原始类型（若被其他逻辑改变）
        if (lastAnvilBlock != null) {
            final org.bukkit.block.Block target = lastAnvilBlock;
            final Material original = (lastAnvilOriginalType == null ? Material.ANVIL : lastAnvilOriginalType);
            plugin.getServer().getScheduler().runTask(plugin, () -> {
                if (target.getType() != original) {
                    target.setType(original, false);
                }
            });
        }
    }
}
