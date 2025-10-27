package mcevent.MCEFramework.customHandler;

import mcevent.MCEFramework.generalGameObject.MCEResumableEventHandler;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import static mcevent.MCEFramework.miscellaneous.Constants.plugin;

/*
GlobalWorldBorderHandler: 全局世界边界自定义伤害
- 取消原版世界边界伤害（将 damageAmount 设为 0）
- 自定义：玩家在边界外每0.5秒受到半颗心（1.0）的伤害
- 注意：放置/破坏在边界外的许可由 GlobalBlockInteractionHandler 处理
*/
public class GlobalWorldBorderHandler extends MCEResumableEventHandler {

    public GlobalWorldBorderHandler() {
        setSuspended(false);
        // 定时任务：每0.5秒检查一次
        new BukkitRunnable() {
            @Override
            public void run() {
                if (isSuspended())
                    return;
                // 确保所有世界的原版边界伤害为0
                for (World w : Bukkit.getWorlds()) {
                    try {
                        WorldBorder wb = w.getWorldBorder();
                        if (wb != null) {
                            // 取消原版边界伤害
                            if (wb.getDamageAmount() != 0.0d)
                                wb.setDamageAmount(0.0d);
                        }
                    } catch (Throwable ignored) {
                    }
                }

                for (Player p : Bukkit.getOnlinePlayers()) {
                    World w = p.getWorld();
                    if (w == null)
                        continue;
                    WorldBorder wb = w.getWorldBorder();
                    if (wb == null)
                        continue;
                    try {
                        if (!wb.isInside(p.getLocation())) {
                            // 半颗心伤害（1.0）
                            p.damage(1.0);
                        }
                    } catch (Throwable ignored) {
                    }
                }
            }
        }.runTaskTimer(plugin, 10L, 10L); // 0.5s 间隔
    }
}
