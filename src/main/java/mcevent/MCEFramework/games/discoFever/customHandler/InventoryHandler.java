package mcevent.MCEFramework.games.discoFever.customHandler;

import mcevent.MCEFramework.games.discoFever.DiscoFever;
import mcevent.MCEFramework.generalGameObject.MCEResumableEventHandler;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;

import static mcevent.MCEFramework.miscellaneous.Constants.plugin;

/*
InventoryHandler: DiscoFever 专用物品栏监听器
禁止玩家操作物品栏内的物品
*/
public class InventoryHandler extends MCEResumableEventHandler implements Listener {

    private DiscoFever discoFever;

    public void register(DiscoFever game) {
        this.discoFever = game;
        setSuspended(true);
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
    public void onClick(InventoryClickEvent event) {
        if (!isSuspended()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (!isSuspended()) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        if (!isSuspended()) {
            event.setCancelled(true);
        }
    }
}
