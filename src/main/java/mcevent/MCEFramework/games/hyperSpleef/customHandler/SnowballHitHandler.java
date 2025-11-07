package mcevent.MCEFramework.games.hyperSpleef.customHandler;

import mcevent.MCEFramework.games.hyperSpleef.HyperSpleef;
import mcevent.MCEFramework.games.hyperSpleef.HyperSpleefFuncImpl;
import mcevent.MCEFramework.generalGameObject.MCEResumableEventHandler;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.scheduler.BukkitRunnable;

import static mcevent.MCEFramework.miscellaneous.Constants.plugin;

/*
SnowballHitHandler: 雪球触碰雪块处理器
雪球触碰雪块后, 雪块变成浮冰, 1s后浮冰变成冰
*/
public class SnowballHitHandler extends MCEResumableEventHandler implements Listener {

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

    @EventHandler(priority = EventPriority.HIGH)
    public void onProjectileHit(ProjectileHitEvent event) {
        if (isSuspended())
            return;

        // 只处理雪球
        if (!(event.getEntity() instanceof Snowball snowball)) {
            return;
        }

        // 检查是否是游戏中的雪球
        if (snowball.getCustomName() != null &&
                (snowball.getCustomName().contains("hyper_spleef") ||
                        snowball.getCustomName().contains("spleef_snowball"))) {

            // 检查是否击中玩家（暴雪法杖的雪球需要添加击退效果）
            if (event.getHitEntity() instanceof Player hitPlayer) {
                // 检查是否是暴雪法杖的雪球
                String customName = snowball.getCustomName();
                if (customName != null && customName.equals("hyper_spleef_blizzard_snowball")) {
                    // 计算击退方向：从雪球位置指向玩家位置
                    org.bukkit.util.Vector toPlayer = hitPlayer.getLocation()
                            .toVector()
                            .subtract(snowball.getLocation().toVector());
                    
                    // 如果距离太近，使用雪球的速度方向
                    org.bukkit.util.Vector knockbackDirection;
                    if (toPlayer.lengthSquared() < 0.01) {
                        org.bukkit.util.Vector velocity = snowball.getVelocity();
                        if (velocity.lengthSquared() > 0.01) {
                            knockbackDirection = velocity.normalize();
                        } else {
                            // 如果速度也为0，使用默认方向（向前）
                            knockbackDirection = new org.bukkit.util.Vector(0, 0, 1);
                        }
                    } else {
                        knockbackDirection = toPlayer.normalize();
                    }
                    
                    // 添加击退效果（水平方向0.5倍，垂直方向0.15倍）
                    org.bukkit.util.Vector knockback = knockbackDirection.clone();
                    knockback.setY(Math.max(0.05, knockback.getY())); // 确保有向上的分量
                    knockback.multiply(new org.bukkit.util.Vector(0.5, 0.15, 0.5)); // 水平方向0.5倍，垂直方向0.15倍
                    
                    // 应用击退效果
                    hitPlayer.setVelocity(hitPlayer.getVelocity().add(knockback));
                }
            }

            // 检查是否击中方块
            if (event.getHitBlock() != null) {
                Block hitBlock = event.getHitBlock();

                // 如果击中的是雪块
                if (hitBlock.getType() == Material.SNOW_BLOCK) {
                    // 将雪块变成浮冰
                    hitBlock.setType(Material.PACKED_ICE);

                    // 1秒后变成冰
                    BukkitRunnable task = new BukkitRunnable() {
                        @Override
                        public void run() {
                            if (hitBlock.getType() == Material.PACKED_ICE) {
                                hitBlock.setType(Material.ICE);
                            }
                        }
                    };
                    task.runTaskLater(plugin, 20L); // 1秒 = 20 ticks
                }
            }
        }
    }
}
