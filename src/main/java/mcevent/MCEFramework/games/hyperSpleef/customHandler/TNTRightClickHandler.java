package mcevent.MCEFramework.games.hyperSpleef.customHandler;

import mcevent.MCEFramework.games.hyperSpleef.HyperSpleef;
import mcevent.MCEFramework.generalGameObject.MCEResumableEventHandler;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import static mcevent.MCEFramework.miscellaneous.Constants.plugin;

/*
TNTRightClickHandler: TNT右键处理器
右键 TNT 会抛出一个已激活的 TNT
*/
public class TNTRightClickHandler extends MCEResumableEventHandler implements Listener {

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
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (isSuspended())
            return;

        Player player = event.getPlayer();

        // 仅参与者且未死亡
        if (!player.getScoreboardTags().contains("Participant") ||
                player.getScoreboardTags().contains("dead")) {
            return;
        }

        // 检查是否是右键
        if (event.getAction() != Action.RIGHT_CLICK_AIR &&
                event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        // 检查手持物品是否是TNT
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() != Material.TNT) {
            return;
        }

        // 取消事件，防止TNT被放置
        event.setCancelled(true);

        // 移除一个TNT
        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
        } else {
            player.getInventory().setItemInMainHand(null);
        }

        // 创建已激活的TNT
        Location spawnLoc = player.getEyeLocation();
        TNTPrimed tnt = spawnLoc.getWorld().spawn(spawnLoc, TNTPrimed.class);

        // 设置TNT的速度和方向
        Vector direction = player.getLocation().getDirection();
        tnt.setVelocity(direction.multiply(1.2));

        // 设置TNT的爆炸时间（默认80 ticks = 4秒）
        tnt.setFuseTicks(80);

        plugin.getLogger().info("HyperSpleef: " + player.getName() + " 右键抛出了已激活的TNT");
    }
}
