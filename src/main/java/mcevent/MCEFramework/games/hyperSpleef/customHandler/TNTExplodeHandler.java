package mcevent.MCEFramework.games.hyperSpleef.customHandler;

import mcevent.MCEFramework.games.hyperSpleef.HyperSpleef;
import mcevent.MCEFramework.generalGameObject.MCEResumableEventHandler;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityExplodeEvent;

import static mcevent.MCEFramework.miscellaneous.Constants.plugin;

/*
TNTExplodeHandler: TNT爆炸处理器
防止TNT爆炸时掉落物品
防止TNT对玩家造成伤害
*/
public class TNTExplodeHandler extends MCEResumableEventHandler implements Listener {

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
    public void onEntityExplode(EntityExplodeEvent event) {
        if (isSuspended())
            return;

        // 检查是否在游戏世界中
        if (hyperSpleef == null || event.getLocation().getWorld() == null ||
                !event.getLocation().getWorld().getName().equals(hyperSpleef.getWorldName())) {
            return;
        }

        // 防止掉落物品
        event.setYield(0.0f);

        // 减小TNT爆炸威力：限制被破坏的方块数量（原版TNT大约破坏50-60个方块，这里限制为约35个）
        java.util.List<org.bukkit.block.Block> blocks = event.blockList();
        if (blocks.size() > 35) {
            // 保留距离爆炸中心最近的35个方块，移除其余的
            org.bukkit.Location center = event.getLocation();

            // 按距离排序，保留最近的35个
            blocks.sort((b1, b2) -> {
                double dist1 = b1.getLocation().distanceSquared(center);
                double dist2 = b2.getLocation().distanceSquared(center);
                return Double.compare(dist1, dist2);
            });

            // 移除超出限制的方块（从后往前移除，避免索引问题）
            int toRemove = blocks.size() - 35;
            for (int i = 0; i < toRemove; i++) {
                blocks.remove(blocks.size() - 1);
            }
        }
    }

    @EventHandler
    public void onEntityDamage(EntityDamageEvent event) {
        if (isSuspended())
            return;

        // 只处理玩家受到的伤害
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }

        // 检查是否在游戏世界中
        if (hyperSpleef == null || player.getWorld() == null ||
                !player.getWorld().getName().equals(hyperSpleef.getWorldName())) {
            return;
        }

        // 检查伤害源是否是爆炸（TNT爆炸）
        if (event.getCause() == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION ||
            event.getCause() == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION) {
            // 取消TNT爆炸对玩家的伤害
            event.setCancelled(true);
        }
    }
}
