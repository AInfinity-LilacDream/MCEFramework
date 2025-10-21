package mcevent.MCEFramework.customHandler;

import mcevent.MCEFramework.generalGameObject.MCEResumableEventHandler;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import static mcevent.MCEFramework.miscellaneous.Constants.plugin;

/**
 * LobbyItemHandler: 仅负责在主城发放烈焰棒的轻量处理器（不包含饱和、二段跳等功能）
 */
public class LobbyItemHandler extends MCEResumableEventHandler implements Listener {

    public LobbyItemHandler() {
        setSuspended(false);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (isSuspended())
            return;
        Player player = event.getPlayer();
        if (isInLobby(player)) {
            giveBlazeRod(player);
        }
    }

    @EventHandler
    public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
        if (isSuspended())
            return;
        Player player = event.getPlayer();
        if (isInLobby(player)) {
            giveBlazeRod(player);
        }
    }

    private boolean isInLobby(Player player) {
        return "lobby".equals(player.getWorld().getName());
    }

    public void giveBlazeRod(Player player) {
        // 先清空物品栏后发放烈焰棒（保持原先行为）
        player.getInventory().clear();

        ItemStack blazeRod = new ItemStack(Material.BLAZE_ROD);
        ItemMeta meta = blazeRod.getItemMeta();
        if (meta != null) {
            meta.setDisplayName("§c§l风弹发射器");
            meta.setLore(java.util.Arrays.asList(
                    "§e右键发射风弹",
                    "§7击中玩家给予发光效果",
                    "§c冷却时间: 3秒"));
            blazeRod.setItemMeta(meta);
        }
        player.getInventory().setItem(0, blazeRod);
        player.updateInventory();
    }
}
